package led.mega.batch.task;

import led.mega.batch.entity.BatchJob;
import led.mega.repository.MetricDataRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class MetricCleanupTask implements BatchTask {

    private final MetricDataRepository metricDataRepository;

    @Override
    public String getJobType() {
        return "METRIC_DATA_CLEANUP";
    }

    @Override
    public String getDisplayName() {
        return "메트릭 데이터 정리 (DB 삭제)";
    }

    @Override
    public Mono<String> execute(BatchJob job, LocalDateTime threshold) {
        return metricDataRepository.deleteByCollectedAtBefore(threshold)
                .map(deleted -> String.format("Metric cleanup 완료: %d건 삭제 (기준: %s 이전)", 
                        deleted, threshold.toLocalDate()));
    }
}
