package led.mega.batch.task;

import led.mega.batch.entity.BatchJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

/**
 * DB 삭제가 아닌, 시스템 상태를 점검하는 별개 유형의 배치 작업 예시
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SystemHealthCheckTask implements BatchTask {

    @Override
    public String getJobType() {
        return "SYSTEM_HEALTH_CHECK";
    }

    @Override
    public String getDisplayName() {
        return "시스템 상태 점검 (Disk/CPU 부하 체크)";
    }

    @Override
    public Mono<String> execute(BatchJob job, LocalDateTime threshold) {
        // 여기서는 예시로 로직을 구성 (실제로는 에이전트들의 상태를 체크하는 등 다양한 로직 가능)
        log.info("[SystemHealthCheck] 배치 실행 중...");
        
        return Mono.just("시스템 상태 정상 (Check 완료: " + LocalDateTime.now() + ")");
    }
}
