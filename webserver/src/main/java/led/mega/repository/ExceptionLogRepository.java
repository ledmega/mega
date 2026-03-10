package led.mega.repository;

// [REACTIVE] JpaRepository → ReactiveCrudRepository
// - findByAgent(Agent) 제거, findByAgentId(Long) 사용
// - JPQL (e.agent.id) → 네이티브 SQL (agent_id)
// - long → Mono<Long>

import led.mega.entity.ExceptionLog;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Repository
public interface ExceptionLogRepository extends ReactiveCrudRepository<ExceptionLog, Long> {

    // [CHANGED] findByAgent(Agent) 제거
    Flux<ExceptionLog> findByAgentId(Long agentId);

    Flux<ExceptionLog> findByExceptionType(String exceptionType);

    // [CHANGED] JPQL → 네이티브 SQL
    @Query("SELECT * FROM exception_log WHERE agent_id = :agentId AND occurred_at BETWEEN :startTime AND :endTime ORDER BY occurred_at DESC")
    Flux<ExceptionLog> findByAgentIdAndOccurredAtBetween(Long agentId, LocalDateTime startTime, LocalDateTime endTime);

    // [CHANGED] JPQL → 네이티브 SQL
    @Query("SELECT * FROM exception_log WHERE exception_type = :exceptionType AND occurred_at BETWEEN :startTime AND :endTime ORDER BY occurred_at DESC")
    Flux<ExceptionLog> findByExceptionTypeAndOccurredAtBetween(String exceptionType, LocalDateTime startTime, LocalDateTime endTime);

    // [CHANGED] long → Mono<Long>
    @Query("SELECT COUNT(*) FROM exception_log WHERE agent_id = :agentId AND occurred_at >= :since")
    Mono<Long> countByAgentIdSince(Long agentId, LocalDateTime since);

    @Query("SELECT COUNT(*) FROM exception_log WHERE task_id = :taskId AND occurred_at >= :since")
    Mono<Long> countByTaskIdSince(Long taskId, LocalDateTime since);

    @Query("SELECT COUNT(*) FROM exception_log WHERE monitoring_config_id = :configId AND occurred_at >= :since")
    Mono<Long> countByMonitoringConfigIdSince(Long configId, LocalDateTime since);

    @Query("SELECT * FROM exception_log ORDER BY occurred_at DESC LIMIT 10")
    Flux<ExceptionLog> findTop10ByOrderByOccurredAtDesc();

    /** 배치 cleanup: threshold 이전 데이터 일괄 삭제 */
    @org.springframework.data.r2dbc.repository.Modifying
    @Query("DELETE FROM exception_log WHERE occurred_at < :threshold")
    Mono<Integer> deleteByOccurredAtBefore(LocalDateTime threshold);
}
