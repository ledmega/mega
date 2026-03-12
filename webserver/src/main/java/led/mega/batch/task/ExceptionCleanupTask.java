package led.mega.batch.task;

import led.mega.batch.entity.BatchJob;
import led.mega.repository.ExceptionLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class ExceptionCleanupTask implements BatchTask {

    private final ExceptionLogRepository exceptionLogRepository;

    @Override
    public String getJobType() {
        return "EXCEPTION_LOG_CLEANUP";
    }

    @Override
    public String getDisplayName() {
        return "Exception 로그 정리 (DB 삭제)";
    }

    @Override
    public Mono<String> execute(BatchJob job, LocalDateTime threshold) {
        return exceptionLogRepository.deleteByOccurredAtBefore(threshold)
                .map(deleted -> String.format("Exception cleanup 완료: %d건 삭제 (기준: %s 이전)", 
                        deleted, threshold.toLocalDate()));
    }
}
