package led.mega.repository;

// [REACTIVE] JpaRepository → ReactiveCrudRepository
// - Optional<T>   → Mono<T>
// - List<T>       → Flux<T>
// - boolean       → Mono<Boolean>
// - long          → Mono<Long>
// - JPQL (@Query) → 네이티브 SQL (@Query)

import led.mega.entity.Agent;
import led.mega.entity.AgentStatus;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Repository
public interface AgentRepository extends ReactiveCrudRepository<Agent, Long> {

    // [CHANGED] Optional<Agent> → Mono<Agent>
    Mono<Agent> findByAgentId(String agentId);

    // [CHANGED] boolean → Mono<Boolean>
    Mono<Boolean> existsByAgentId(String agentId);

    // [CHANGED] List<Agent> → Flux<Agent>
    Flux<Agent> findByStatus(AgentStatus status);

    // [CHANGED] JPQL → 네이티브 SQL
    @Query("SELECT * FROM agent WHERE last_heartbeat < :threshold")
    Flux<Agent> findOfflineAgents(LocalDateTime threshold);

    // [CHANGED] long → Mono<Long>, JPQL → 네이티브 SQL
    @Query("SELECT COUNT(*) FROM agent WHERE status = :status")
    Mono<Long> countByStatus(String status);
}

