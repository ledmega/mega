package led.mega.repository;

import led.mega.entity.ServiceMetricData;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface ServiceMetricDataRepository extends ReactiveCrudRepository<ServiceMetricData, String> {

    @Query("SELECT * FROM service_metric_data WHERE monitoring_config_id = :configId ORDER BY collected_at DESC LIMIT 1")
    Mono<ServiceMetricData> findTopByMonitoringConfigIdOrderByCollectedAtDesc(String configId);
}
