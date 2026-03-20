package led.mega.repository;

import led.mega.entity.OsMonitoringConfig;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface OsMonitoringConfigRepository extends ReactiveCrudRepository<OsMonitoringConfig, String> {
    
    /**
     * 특정 에이전트의 활성화된 OS 설정 목록 조회
     */
    Flux<OsMonitoringConfig> findByAgentIdAndEnabledTrue(String agentId);
    
    /**
     * 전체 공통(agentId IS NULL) 활성화된 OS 설정 목록 조회
     */
    Flux<OsMonitoringConfig> findByAgentIdIsNullAndEnabledTrue();

    /**
     * 특정 에이전트의 메트릭 타입별 활성화된 설정 조회 (중복 방지용)
     */
    Mono<led.mega.entity.OsMonitoringConfig> findByAgentIdAndMetricTypeAndEnabledTrue(String agentId, String metricType);

    /**
     * 공통 설정 중 특정 메트릭 타입 활성화된 설정 조회 (중복 방지용)
     */
    Mono<led.mega.entity.OsMonitoringConfig> findByAgentIdIsNullAndMetricTypeAndEnabledTrue(String metricType);
}
