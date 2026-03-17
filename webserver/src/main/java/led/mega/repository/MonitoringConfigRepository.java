package led.mega.repository;

import led.mega.entity.MonitoringConfig;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

@Repository
public interface MonitoringConfigRepository extends ReactiveCrudRepository<MonitoringConfig, String> {

    Flux<MonitoringConfig> findByAgentId(String agentId);

    Flux<MonitoringConfig> findByEnabled(Boolean enabled);

    @Query("SELECT * FROM monitoring_config WHERE agent_id = :agentId AND enabled = true ORDER BY created_at DESC")
    Flux<MonitoringConfig> findActiveByAgentId(String agentId);

    @Query("SELECT * FROM monitoring_config ORDER BY created_at DESC")
    Flux<MonitoringConfig> findAllOrderByCreatedAtDesc();
}
