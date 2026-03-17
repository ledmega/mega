package led.mega.batch.service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import led.mega.batch.entity.BatchJob;
import led.mega.batch.repository.BatchJobRepository;
import led.mega.batch.task.BatchTask;
import led.mega.util.IdGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class BatchJobService {

    private final BatchJobRepository batchJobRepository;

    // Job 유형 상수
    public static final String TYPE_METRIC_CLEANUP    = "METRIC_DATA_CLEANUP";
    public static final String TYPE_EXCEPTION_CLEANUP = "EXCEPTION_LOG_CLEANUP";

    // 실행 중인 스케줄 Future 관리
    private final Map<String, ScheduledFuture<?>> scheduledFutures = new ConcurrentHashMap<>();
    
    // 주입된 BatchTask 구현체들을 저장하는 맵
    private final Map<String, BatchTask> tasks = new ConcurrentHashMap<>();

    private final TaskScheduler taskScheduler;

    /** Spring에 등록된 모든 BatchTask를 자동 주입받아 맵에 저장 */
    @Autowired
    public void setBatchTasks(java.util.List<BatchTask> taskList) {
        taskList.forEach(task -> {
            tasks.put(task.getJobType(), task);
            log.info("[BatchJob] Task 등록: type={}, class={}", task.getJobType(), task.getClass().getSimpleName());
        });
    }

    /** UI용 Job 유형 목록 반환 */
    public Map<String, String> getAvailableJobTypes() {
        Map<String, String> result = new java.util.LinkedHashMap<>();
        tasks.values().stream()
                .sorted(java.util.Comparator.comparing(BatchTask::getJobType))
                .forEach(t -> result.put(t.getJobType(), t.getDisplayName()));
        return result;
    }

    @PostConstruct
    public void init() {
        log.info("[BatchJob] 배치 스케줄러 초기화 시작...");
        seedDefaultJobsIfEmpty()
                .thenMany(batchJobRepository.findByEnabledTrueOrderByJobNameAsc())
                .doOnNext(this::scheduleJob)
                .doOnComplete(() -> log.info("[BatchJob] 배치 스케줄러 초기화 완료. 등록된 Job 수: {}", scheduledFutures.size()))
                .subscribe();
    }

    @PreDestroy
    public void destroy() {
        scheduledFutures.values().forEach(f -> f.cancel(false));
        log.info("[BatchJob] 배치 스케줄러 종료");
    }

    public Flux<BatchJob> findAll() {
        return batchJobRepository.findAllByOrderByCreatedAtAsc();
    }

    public Mono<BatchJob> findById(String id) {
        return batchJobRepository.findById(id);
    }

    public Mono<BatchJob> create(BatchJob job) {
        job.setBatchJobId(IdGenerator.generate(IdGenerator.BATCH_JOB));
        job.setCreatedAt(LocalDateTime.now());
        job.setUpdatedAt(LocalDateTime.now());
        if (job.getEnabled() == null) job.setEnabled(true);
        return batchJobRepository.save(job)
                .doOnSuccess(saved -> {
                    if (Boolean.TRUE.equals(saved.getEnabled())) {
                        scheduleJob(saved);
                    }
                    log.info("[BatchJob] Job 생성: id={}, name={}", saved.getBatchJobId(), saved.getJobName());
                });
    }

    public Mono<BatchJob> update(String id, BatchJob req) {
        return batchJobRepository.findById(id)
                .flatMap(existing -> {
                    existing.setJobName(req.getJobName());
                    existing.setJobType(req.getJobType());
                    existing.setDescription(req.getDescription());
                    existing.setIntervalMinutes(req.getIntervalMinutes());
                    existing.setCronExpression(req.getCronExpression());
                    existing.setRetentionDays(req.getRetentionDays());
                    existing.setJobConfig(req.getJobConfig());
                    existing.setEnabled(req.getEnabled());
                    existing.setUpdatedAt(LocalDateTime.now());
                    return batchJobRepository.save(existing);
                })
                .doOnSuccess(saved -> {
                    cancelSchedule(saved.getBatchJobId());
                    if (Boolean.TRUE.equals(saved.getEnabled())) {
                        scheduleJob(saved);
                    }
                    log.info("[BatchJob] Job 수정: id={}, name={}", saved.getBatchJobId(), saved.getJobName());
                });
    }

    public Mono<Void> delete(String id) {
        cancelSchedule(id);
        return batchJobRepository.deleteById(id)
                .doOnSuccess(v -> log.info("[BatchJob] Job 삭제: id={}", id));
    }

    public Mono<BatchJob> toggleEnabled(String id) {
        return batchJobRepository.findById(id)
                .flatMap(job -> {
                    boolean newEnabled = !Boolean.TRUE.equals(job.getEnabled());
                    job.setEnabled(newEnabled);
                    job.setUpdatedAt(LocalDateTime.now());
                    return batchJobRepository.save(job);
                })
                .doOnSuccess(job -> {
                    cancelSchedule(job.getBatchJobId());
                    if (Boolean.TRUE.equals(job.getEnabled())) {
                        scheduleJob(job);
                        log.info("[BatchJob] Job 활성화: id={}, name={}", job.getBatchJobId(), job.getJobName());
                    } else {
                        log.info("[BatchJob] Job 비활성화: id={}, name={}", job.getBatchJobId(), job.getJobName());
                    }
                });
    }

    public Mono<String> runNow(String id) {
        return batchJobRepository.findById(id)
                .flatMap(job -> {
                    log.info("[BatchJob] 즉시 실행 요청: id={}, name={}", id, job.getJobName());
                    return executeJob(job);
                });
    }

    private void scheduleJob(BatchJob job) {
        ScheduledFuture<?> future;
        
        if (job.getCronExpression() != null && !job.getCronExpression().trim().isEmpty()) {
            try {
                future = taskScheduler.schedule(
                    () -> executeJob(job).subscribe(),
                    new CronTrigger(job.getCronExpression())
                );
                log.info("[BatchJob] Job 스케줄 등록 (Cron): id={}, name={}, cron={}", 
                         job.getBatchJobId(), job.getJobName(), job.getCronExpression());
            } catch (Exception e) {
                log.error("[BatchJob] Cron 표현식 오류: id={}, expr={}", job.getBatchJobId(), job.getCronExpression(), e);
                return;
            }
        } else if (job.getIntervalMinutes() != null && job.getIntervalMinutes() > 0) {
            Duration duration = Duration.ofMinutes(job.getIntervalMinutes());
            future = taskScheduler.scheduleAtFixedRate(
                () -> executeJob(job).subscribe(),
                duration
            );
            log.info("[BatchJob] Job 스케줄 등록 (Interval): id={}, name={}, interval={}분", 
                     job.getBatchJobId(), job.getJobName(), job.getIntervalMinutes());
        } else {
            log.warn("[BatchJob] 스케줄 설정이 없는 Job입니다. 건너뜁니다: id={}, name={}", job.getBatchJobId(), job.getJobName());
            return;
        }
        
        scheduledFutures.put(job.getBatchJobId(), future);
    }

    private void cancelSchedule(String jobId) {
        ScheduledFuture<?> future = scheduledFutures.remove(jobId);
        if (future != null) {
            future.cancel(false);
            log.info("[BatchJob] Job 스케줄 취소: id={}", jobId);
        }
    }

    private Mono<String> executeJob(BatchJob job) {
        int retentionDays = job.getRetentionDays() != null ? job.getRetentionDays() : 0;
        LocalDateTime threshold = LocalDateTime.now().minusDays(retentionDays);
        log.info("[BatchJob] 실행 시작: type={}, name={}", job.getJobType(), job.getJobName());

        BatchTask task = tasks.get(job.getJobType());
        if (task == null) {
            return Mono.just("미지원 Job 유형: " + job.getJobType());
        }

        return task.execute(job, threshold)
                .flatMap(msg -> {
                    log.info("[BatchJob] {}", msg);
                    return batchJobRepository.updateRunResult(job.getBatchJobId(), LocalDateTime.now(), "SUCCESS", msg)
                            .thenReturn(msg);
                })
                .onErrorResume(e -> {
                    String errMsg = "실행 실패: " + e.getMessage();
                    log.error("[BatchJob] {} 실행 오류", job.getJobName(), e);
                    return batchJobRepository.updateRunResult(job.getBatchJobId(), LocalDateTime.now(), "FAILED", errMsg)
                            .thenReturn(errMsg);
                });
    }

    private Mono<Void> seedDefaultJobsIfEmpty() {
        return batchJobRepository.count()
                .flatMap(count -> {
                    if (count > 0) return Mono.empty();
                    log.info("[BatchJob] 기본 Job 생성 중...");
                    BatchJob metricCleanup = BatchJob.builder()
                            .batchJobId(IdGenerator.generate(IdGenerator.BATCH_JOB))
                            .jobName("메트릭 데이터 정리")
                            .jobType(TYPE_METRIC_CLEANUP)
                            .description("수집된 metric_data에서 보존 기간이 지난 데이터를 자동 삭제합니다.")
                            .intervalMinutes(60)
                            .retentionDays(7)
                            .enabled(true)
                            .createdAt(LocalDateTime.now())
                            .updatedAt(LocalDateTime.now())
                            .build();
                    BatchJob exceptionCleanup = BatchJob.builder()
                            .batchJobId(IdGenerator.generate(IdGenerator.BATCH_JOB))
                            .jobName("Exception 로그 정리")
                            .jobType(TYPE_EXCEPTION_CLEANUP)
                            .description("exception_log에서 보존 기간이 지난 로그를 자동 삭제합니다.")
                            .intervalMinutes(60)
                            .retentionDays(30)
                            .enabled(true)
                            .createdAt(LocalDateTime.now())
                            .updatedAt(LocalDateTime.now())
                            .build();
                    return batchJobRepository.saveAll(Flux.just(metricCleanup, exceptionCleanup)).collectList().then();
                });
    }
}
