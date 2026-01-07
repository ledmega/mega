package led.mega.repository;

import led.mega.entity.Agent;
import led.mega.entity.ExceptionLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ExceptionLogRepository extends JpaRepository<ExceptionLog, Long> {
    
    List<ExceptionLog> findByAgent(Agent agent);
    
    List<ExceptionLog> findByAgentId(Long agentId);
    
    List<ExceptionLog> findByExceptionType(String exceptionType);
    
    @Query("SELECT e FROM ExceptionLog e WHERE e.agent.id = :agentId AND e.occurredAt BETWEEN :startTime AND :endTime ORDER BY e.occurredAt DESC")
    List<ExceptionLog> findByAgentIdAndOccurredAtBetween(
            @Param("agentId") Long agentId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );
    
    @Query("SELECT e FROM ExceptionLog e WHERE e.exceptionType = :exceptionType AND e.occurredAt BETWEEN :startTime AND :endTime ORDER BY e.occurredAt DESC")
    List<ExceptionLog> findByExceptionTypeAndOccurredAtBetween(
            @Param("exceptionType") String exceptionType,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );
    
    @Query("SELECT COUNT(e) FROM ExceptionLog e WHERE e.agent.id = :agentId AND e.occurredAt >= :since")
    long countByAgentIdSince(@Param("agentId") Long agentId, @Param("since") LocalDateTime since);
}

