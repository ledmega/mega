package led.mega.repository;

import led.mega.entity.Agent;
import led.mega.entity.AgentStatus;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Repository
public interface AgentRepository extends ReactiveCrudRepository<Agent, String> {

    Mono<Agent> findByAgentRefId(String agentRefId);

    Mono<Boolean> existsByAgentRefId(String agentRefId);

    Flux<Agent> findByStatus(AgentStatus status);

    @Query("SELECT * FROM agent WHERE last_heartbeat < :threshold")
    Flux<Agent> findOfflineAgents(LocalDateTime threshold);

    @Query("SELECT COUNT(*) FROM agent WHERE status = :status")
    Mono<Long> countByStatus(String status);
}
