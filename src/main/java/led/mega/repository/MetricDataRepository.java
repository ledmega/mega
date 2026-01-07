package led.mega.repository;

import led.mega.entity.Agent;
import led.mega.entity.MetricData;
import led.mega.entity.MetricType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MetricDataRepository extends JpaRepository<MetricData, Long> {
    
    List<MetricData> findByAgent(Agent agent);
    
    List<MetricData> findByAgentId(Long agentId);
    
    List<MetricData> findByMetricType(MetricType metricType);
    
    List<MetricData> findByAgentIdAndMetricType(Long agentId, MetricType metricType);
    
    @Query("SELECT m FROM MetricData m WHERE m.agent.id = :agentId AND m.collectedAt BETWEEN :startTime AND :endTime ORDER BY m.collectedAt DESC")
    List<MetricData> findByAgentIdAndCollectedAtBetween(
            @Param("agentId") Long agentId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );
    
    @Query("SELECT m FROM MetricData m WHERE m.agent.id = :agentId AND m.metricType = :metricType AND m.collectedAt BETWEEN :startTime AND :endTime ORDER BY m.collectedAt DESC")
    List<MetricData> findByAgentIdAndMetricTypeAndCollectedAtBetween(
            @Param("agentId") Long agentId,
            @Param("metricType") MetricType metricType,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );
    
    @Query("SELECT m FROM MetricData m WHERE m.collectedAt < :threshold")
    List<MetricData> findOldMetrics(@Param("threshold") LocalDateTime threshold);
}

