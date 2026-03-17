package led.mega.repository;

import led.mega.entity.MetricData;
import led.mega.entity.MetricType;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;

@Repository
public interface MetricDataRepository extends ReactiveCrudRepository<MetricData, String> {

    Flux<MetricData> findByAgentId(String agentId);

    Flux<MetricData> findByMetricType(MetricType metricType);

    Flux<MetricData> findByAgentIdAndMetricType(String agentId, MetricType metricType);

    @Query("SELECT * FROM metric_data WHERE agent_id = :agentId AND collected_at BETWEEN :startTime AND :endTime ORDER BY collected_at DESC")
    Flux<MetricData> findByAgentIdAndCollectedAtBetween(String agentId, LocalDateTime startTime, LocalDateTime endTime);

    @Query("SELECT * FROM metric_data WHERE agent_id = :agentId AND metric_type = :metricType AND collected_at BETWEEN :startTime AND :endTime ORDER BY collected_at DESC")
    Flux<MetricData> findByAgentIdAndMetricTypeAndCollectedAtBetween(String agentId, String metricType, LocalDateTime startTime, LocalDateTime endTime);

    @Query("SELECT * FROM metric_data WHERE collected_at < :threshold")
    Flux<MetricData> findOldMetrics(LocalDateTime threshold);

    @Query("SELECT * FROM metric_data ORDER BY collected_at DESC LIMIT 500")
    Flux<MetricData> findTop500ByOrderByCollectedAtDesc();

    @Query("SELECT COUNT(*) FROM metric_data WHERE collected_at >= :startOfDay")
    Mono<Long> countByCollectedAtAfter(LocalDateTime startOfDay);

    @Query("SELECT * FROM metric_data WHERE collected_at >= :startTime ORDER BY collected_at DESC LIMIT 2000")
    Flux<MetricData> findByCollectedAtAfterOrderByCollectedAtDesc(LocalDateTime startTime);

    @Query("SELECT * FROM metric_data WHERE monitoring_config_id = :configId AND metric_type = :metricType ORDER BY collected_at DESC LIMIT 1")
    Flux<MetricData> findLatestByMonitoringConfigIdAndMetricType(String configId, String metricType);

    @org.springframework.data.r2dbc.repository.Modifying
    @Query("DELETE FROM metric_data WHERE collected_at < :threshold")
    Mono<Integer> deleteByCollectedAtBefore(LocalDateTime threshold);
}
