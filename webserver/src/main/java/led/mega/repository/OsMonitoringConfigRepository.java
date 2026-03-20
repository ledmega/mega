package led.mega.repository;

import led.mega.entity.OsMonitoringConfig;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

public interface OsMonitoringConfigRepository extends ReactiveCrudRepository<OsMonitoringConfig, String> {
    
    /**
     * 특정 에이전트의 활성화된 OS 설정 목록 조회
     */
    Flux<OsMonitoringConfig> findByAgentIdAndEnabledTrue(String agentId);
    
    /**
     * 전체 공통(agentId IS NULL) 활성화된 OS 설정 목록 조회
     */
    Flux<OsMonitoringConfig> findByAgentIdIsNullAndEnabledTrue();
}
