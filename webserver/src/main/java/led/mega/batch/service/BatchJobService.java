package led.mega.batch.service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import led.mega.batch.entity.BatchJob;
import led.mega.batch.repository.BatchJobRepository;
import led.mega.repository.ExceptionLogRepository;
import led.mega.repository.MetricDataRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class BatchJobService {

    private final BatchJobRepository batchJobRepository;
    private final MetricDataRepository metricDataRepository;
    private final ExceptionLogRepository exceptionLogRepository;

    // Job 유형 상수
    public static final String TYPE_METRIC_CLEANUP    = "METRIC_DATA_CLEANUP";
    public static final String TYPE_EXCEPTION_CLEANUP = "EXCEPTION_LOG_CLEANUP";

    // 실행 중인 스케줄 Future 관리
    private final Map<Long, ScheduledFuture<?>> scheduledFutures = new ConcurrentHashMap<>();
    private final ScheduledExecutorService executor =
            Executors.newScheduledThreadPool(4, r -> {
                Thread t = new Thread(r, "batch-job-" + System.currentTimeMillis());
                t.setDaemon(true);
                return t;
            });

    // ───────────────────────────────────────────────────────────────────────
    // 서버 시작 시 초기화
    // ───────────────────────────────────────────────────────────────────────

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
        executor.shutdownNow();
        log.info("[BatchJob] 배치 스케줄러 종료");
    }

    // ───────────────────────────────────────────────────────────────────────
    // CRUD
    // ───────────────────────────────────────────────────────────────────────

    public Flux<BatchJob> findAll() {
        return batchJobRepository.findAllByOrderByCreatedAtAsc();
    }

    public Mono<BatchJob> findById(Long id) {
        return batchJobRepository.findById(id);
    }

    public Mono<BatchJob> create(BatchJob job) {
        job.setCreatedAt(LocalDateTime.now());
        job.setUpdatedAt(LocalDateTime.now());
        if (job.getEnabled() == null) job.setEnabled(true);
        return batchJobRepository.save(job)
                .doOnSuccess(saved -> {
                    if (Boolean.TRUE.equals(saved.getEnabled())) {
                        scheduleJob(saved);
                    }
                    log.info("[BatchJob] Job 생성: id={}, name={}", saved.getId(), saved.getJobName());
                });
    }

    public Mono<BatchJob> update(Long id, BatchJob req) {
        return batchJobRepository.findById(id)
                .flatMap(existing -> {
                    existing.setJobName(req.getJobName());
                    existing.setJobType(req.getJobType());
                    existing.setDescription(req.getDescription());
                    existing.setIntervalMinutes(req.getIntervalMinutes());
                    existing.setRetentionDays(req.getRetentionDays());
                    existing.setEnabled(req.getEnabled());
                    existing.setUpdatedAt(LocalDateTime.now());
                    return batchJobRepository.save(existing);
                })
                .doOnSuccess(saved -> {
                    // 기존 스케줄 취소 후 재등록
                    cancelSchedule(saved.getId());
                    if (Boolean.TRUE.equals(saved.getEnabled())) {
                        scheduleJob(saved);
                    }
                    log.info("[BatchJob] Job 수정: id={}, name={}", saved.getId(), saved.getJobName());
                });
    }

    public Mono<Void> delete(Long id) {
        cancelSchedule(id);
        return batchJobRepository.deleteById(id)
                .doOnSuccess(v -> log.info("[BatchJob] Job 삭제: id={}", id));
    }

    public Mono<BatchJob> toggleEnabled(Long id) {
        return batchJobRepository.findById(id)
                .flatMap(job -> {
                    boolean newEnabled = !Boolean.TRUE.equals(job.getEnabled());
                    job.setEnabled(newEnabled);
                    job.setUpdatedAt(LocalDateTime.now());
                    return batchJobRepository.save(job);
                })
                .doOnSuccess(job -> {
                    cancelSchedule(job.getId());
                    if (Boolean.TRUE.equals(job.getEnabled())) {
                        scheduleJob(job);
                        log.info("[BatchJob] Job 활성화: id={}, name={}", job.getId(), job.getJobName());
                    } else {
                        log.info("[BatchJob] Job 비활성화: id={}, name={}", job.getId(), job.getJobName());
                    }
                });
    }

    /** 즉시 실행 (UI의 '지금 실행' 버튼) */
    public Mono<String> runNow(Long id) {
        return batchJobRepository.findById(id)
                .flatMap(job -> {
                    log.info("[BatchJob] 즉시 실행 요청: id={}, name={}", id, job.getJobName());
                    return executeJob(job);
                });
    }

    // ───────────────────────────────────────────────────────────────────────
    // 스케줄링
    // ───────────────────────────────────────────────────────────────────────

    private void scheduleJob(BatchJob job) {
        long intervalMs = (long) job.getIntervalMinutes() * 60 * 1000;
        ScheduledFuture<?> future = executor.scheduleAtFixedRate(
                () -> executeJob(job).subscribe(),
                intervalMs,   // 첫 실행은 interval 후 (즉시 실행은 runNow로)
                intervalMs,
                TimeUnit.MILLISECONDS
        );
        scheduledFutures.put(job.getId(), future);
        log.info("[BatchJob] Job 스케줄 등록: id={}, name={}, interval={}분", job.getId(), job.getJobName(), job.getIntervalMinutes());
    }

    private void cancelSchedule(Long jobId) {
        ScheduledFuture<?> future = scheduledFutures.remove(jobId);
        if (future != null) {
            future.cancel(false);
            log.info("[BatchJob] Job 스케줄 취소: id={}", jobId);
        }
    }

    // ───────────────────────────────────────────────────────────────────────
    // 실제 Job 실행
    // ───────────────────────────────────────────────────────────────────────

    private Mono<String> executeJob(BatchJob job) {
        LocalDateTime threshold = LocalDateTime.now().minusDays(job.getRetentionDays());
        log.info("[BatchJob] 실행 시작: type={}, threshold={}", job.getJobType(), threshold);

        Mono<Integer> action;
        switch (job.getJobType()) {
            case TYPE_METRIC_CLEANUP:
                action = metricDataRepository.deleteByCollectedAtBefore(threshold);
                break;
            case TYPE_EXCEPTION_CLEANUP:
                action = exceptionLogRepository.deleteByOccurredAtBefore(threshold);
                break;
            default:
                return Mono.just("알 수 없는 Job 유형: " + job.getJobType());
        }

        return action
                .flatMap(deleted -> {
                    String msg = String.format("%s 완료: %d건 삭제 (기준: %s 이전)", job.getJobType(), deleted, threshold.toLocalDate());
                    log.info("[BatchJob] {}", msg);
                    return batchJobRepository.updateRunResult(job.getId(), LocalDateTime.now(), "SUCCESS", msg)
                            .thenReturn(msg);
                })
                .onErrorResume(e -> {
                    String errMsg = "실행 실패: " + e.getMessage();
                    log.error("[BatchJob] {} 실행 오류", job.getJobName(), e);
                    return batchJobRepository.updateRunResult(job.getId(), LocalDateTime.now(), "FAILED", errMsg)
                            .thenReturn(errMsg);
                });
    }

    // ───────────────────────────────────────────────────────────────────────
    // 기본 Job 시딩
    // ───────────────────────────────────────────────────────────────────────

    private Mono<Void> seedDefaultJobsIfEmpty() {
        return batchJobRepository.count()
                .flatMap(count -> {
                    if (count > 0) return Mono.empty();
                    log.info("[BatchJob] 기본 Job 생성 중...");
                    BatchJob metricCleanup = BatchJob.builder()
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
                            .jobName("Exception 로그 정리")
                            .jobType(TYPE_EXCEPTION_CLEANUP)
                            .description("exception_log에서 보존 기간이 지난 로그를 자동 삭제합니다.")
                            .intervalMinutes(60)
                            .retentionDays(30)
                            .enabled(true)
                            .createdAt(LocalDateTime.now())
                            .updatedAt(LocalDateTime.now())
                            .build();
                    return batchJobRepository.saveAll(Flux.just(metricCleanup, exceptionCleanup)).then();
                });
    }
}
