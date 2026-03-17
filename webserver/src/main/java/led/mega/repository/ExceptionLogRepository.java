package led.mega.repository;

import led.mega.entity.ExceptionLog;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Repository
public interface ExceptionLogRepository extends ReactiveCrudRepository<ExceptionLog, String> {

    Flux<ExceptionLog> findByAgentId(String agentId);

    Flux<ExceptionLog> findByExceptionType(String exceptionType);

    @Query("SELECT * FROM exception_log WHERE agent_id = :agentId AND occurred_at BETWEEN :startTime AND :endTime ORDER BY occurred_at DESC")
    Flux<ExceptionLog> findByAgentIdAndOccurredAtBetween(String agentId, LocalDateTime startTime, LocalDateTime endTime);

    @Query("SELECT * FROM exception_log WHERE exception_type = :exceptionType AND occurred_at BETWEEN :startTime AND :endTime ORDER BY occurred_at DESC")
    Flux<ExceptionLog> findByExceptionTypeAndOccurredAtBetween(String exceptionType, LocalDateTime startTime, LocalDateTime endTime);

    @Query("SELECT COUNT(*) FROM exception_log WHERE agent_id = :agentId AND occurred_at >= :since")
    Mono<Long> countByAgentIdSince(String agentId, LocalDateTime since);

    @Query("SELECT COUNT(*) FROM exception_log WHERE task_id = :taskId AND occurred_at >= :since")
    Mono<Long> countByTaskIdSince(String taskId, LocalDateTime since);

    @Query("SELECT COUNT(*) FROM exception_log WHERE monitoring_config_id = :configId AND occurred_at >= :since")
    Mono<Long> countByMonitoringConfigIdSince(String configId, LocalDateTime since);

    @Query("SELECT * FROM exception_log ORDER BY occurred_at DESC LIMIT 10")
    Flux<ExceptionLog> findTop10ByOrderByOccurredAtDesc();

    @org.springframework.data.r2dbc.repository.Modifying
    @Query("DELETE FROM exception_log WHERE occurred_at < :threshold")
    Mono<Integer> deleteByOccurredAtBefore(LocalDateTime threshold);
}
