package led.mega.batch.task;

import led.mega.batch.entity.BatchJob;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

/**
 * 개별 배치 작업이 구현해야 할 인터페이스
 */
public interface BatchTask {
    
    /**
     * Job 유형 코드를 반환 (예: METRIC_DATA_CLEANUP)
     */
    String getJobType();

    /**
     * Job의 표시 이름을 반환 (UI용)
     */
    String getDisplayName();

    /**
     * 실제 배치 로직 실행
     */
    Mono<String> execute(BatchJob job, LocalDateTime threshold);
}
