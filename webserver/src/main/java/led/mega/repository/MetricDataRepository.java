package led.mega.repository;

// [REACTIVE] JpaRepository → ReactiveCrudRepository
// - findByAgent(Agent) 제거, findByAgentId(Long) 사용
// - MetricType enum: 파생 쿼리는 그대로, @Query는 name() 문자열로 처리

import led.mega.entity.MetricData;
import led.mega.entity.MetricType;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;

@Repository
public interface MetricDataRepository extends ReactiveCrudRepository<MetricData, Long> {

    // [CHANGED] findByAgent(Agent) 제거
    Flux<MetricData> findByAgentId(Long agentId);

    Flux<MetricData> findByMetricType(MetricType metricType);

    Flux<MetricData> findByAgentIdAndMetricType(Long agentId, MetricType metricType);

    // [CHANGED] JPQL → 네이티브 SQL
    @Query("SELECT * FROM metric_data WHERE agent_id = :agentId AND collected_at BETWEEN :startTime AND :endTime ORDER BY collected_at DESC")
    Flux<MetricData> findByAgentIdAndCollectedAtBetween(Long agentId, LocalDateTime startTime, LocalDateTime endTime);

    // [CHANGED] JPQL → 네이티브 SQL, MetricType → String :metricType 전달 시 .name() 호출
    @Query("SELECT * FROM metric_data WHERE agent_id = :agentId AND metric_type = :metricType AND collected_at BETWEEN :startTime AND :endTime ORDER BY collected_at DESC")
    Flux<MetricData> findByAgentIdAndMetricTypeAndCollectedAtBetween(Long agentId, String metricType, LocalDateTime startTime, LocalDateTime endTime);

    @Query("SELECT * FROM metric_data WHERE collected_at < :threshold")
    Flux<MetricData> findOldMetrics(LocalDateTime threshold);

    @Query("SELECT * FROM metric_data ORDER BY collected_at DESC LIMIT 500")
    Flux<MetricData> findTop500ByOrderByCollectedAtDesc();

    @Query("SELECT COUNT(*) FROM metric_data WHERE collected_at >= :startOfDay")
    Mono<Long> countByCollectedAtAfter(LocalDateTime startOfDay);

    /** 서비스(monitoring_config)별 최근 메트릭 1건 조회 */
    @Query("SELECT * FROM metric_data WHERE monitoring_config_id = :configId AND metric_type = :metricType ORDER BY collected_at DESC LIMIT 1")
    Flux<MetricData> findLatestByMonitoringConfigIdAndMetricType(Long configId, String metricType);

    /** 배치 cleanup: threshold 이전 데이터 일괄 삭제 */
    @org.springframework.data.r2dbc.repository.Modifying
    @Query("DELETE FROM metric_data WHERE collected_at < :threshold")
    Mono<Integer> deleteByCollectedAtBefore(LocalDateTime threshold);
}
