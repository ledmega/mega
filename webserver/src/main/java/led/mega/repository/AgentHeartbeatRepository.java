package led.mega.repository;

import led.mega.entity.AgentHeartbeat;
import led.mega.entity.AgentStatus;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;

@Repository
public interface AgentHeartbeatRepository extends ReactiveCrudRepository<AgentHeartbeat, String> {

    Flux<AgentHeartbeat> findByAgentId(String agentId);

    Flux<AgentHeartbeat> findByStatus(AgentStatus status);

    @Query("SELECT * FROM agent_heartbeat WHERE agent_id = :agentId ORDER BY heartbeat_at DESC")
    Flux<AgentHeartbeat> findByAgentIdOrderByHeartbeatAtDesc(String agentId);

    @Query("SELECT * FROM agent_heartbeat WHERE agent_id = :agentId AND heartbeat_at BETWEEN :startTime AND :endTime ORDER BY heartbeat_at DESC")
    Flux<AgentHeartbeat> findByAgentIdAndHeartbeatAtBetween(String agentId, LocalDateTime startTime, LocalDateTime endTime);
}
