package led.mega.repository;

// [REACTIVE] JpaRepository → ReactiveCrudRepository
// - findByAgent(Agent) 제거: 엔티티 참조 불가 → findByAgentId(Long) 사용
// - JPQL (h.agent.id = :agentId) → 네이티브 SQL (agent_id = :agentId)
// - 별도 findLatestByAgentId 헬퍼 불필요: findByAgentIdOrderByHeartbeatAtDesc().next()

import led.mega.entity.AgentHeartbeat;
import led.mega.entity.AgentStatus;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;

@Repository
public interface AgentHeartbeatRepository extends ReactiveCrudRepository<AgentHeartbeat, Long> {

    // [CHANGED] findByAgent(Agent) 제거, findByAgentId 만 사용
    Flux<AgentHeartbeat> findByAgentId(Long agentId);

    Flux<AgentHeartbeat> findByStatus(AgentStatus status);

    // [CHANGED] JPQL → 네이티브 SQL
    @Query("SELECT * FROM agent_heartbeat WHERE agent_id = :agentId ORDER BY heartbeat_at DESC")
    Flux<AgentHeartbeat> findByAgentIdOrderByHeartbeatAtDesc(Long agentId);

    // [CHANGED] JPQL → 네이티브 SQL
    @Query("SELECT * FROM agent_heartbeat WHERE agent_id = :agentId AND heartbeat_at BETWEEN :startTime AND :endTime ORDER BY heartbeat_at DESC")
    Flux<AgentHeartbeat> findByAgentIdAndHeartbeatAtBetween(Long agentId, LocalDateTime startTime, LocalDateTime endTime);

    // [REMOVED] findLatestByAgentId(default) → 서비스에서 .next() 호출로 대체
}

