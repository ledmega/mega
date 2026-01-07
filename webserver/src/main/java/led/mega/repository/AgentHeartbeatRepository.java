package led.mega.repository;

import led.mega.entity.Agent;
import led.mega.entity.AgentHeartbeat;
import led.mega.entity.AgentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AgentHeartbeatRepository extends JpaRepository<AgentHeartbeat, Long> {
    
    List<AgentHeartbeat> findByAgent(Agent agent);
    
    List<AgentHeartbeat> findByAgentId(Long agentId);
    
    List<AgentHeartbeat> findByStatus(AgentStatus status);
    
    @Query("SELECT h FROM AgentHeartbeat h WHERE h.agent.id = :agentId ORDER BY h.heartbeatAt DESC")
    List<AgentHeartbeat> findByAgentIdOrderByHeartbeatAtDesc(@Param("agentId") Long agentId);
    
    @Query("SELECT h FROM AgentHeartbeat h WHERE h.agent.id = :agentId AND h.heartbeatAt BETWEEN :startTime AND :endTime ORDER BY h.heartbeatAt DESC")
    List<AgentHeartbeat> findByAgentIdAndHeartbeatAtBetween(
            @Param("agentId") Long agentId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );
    
    @Query("SELECT h FROM AgentHeartbeat h WHERE h.agent.id = :agentId ORDER BY h.heartbeatAt DESC")
    List<AgentHeartbeat> findByAgentIdOrderByHeartbeatAtDescList(@Param("agentId") Long agentId);
    
    default Optional<AgentHeartbeat> findLatestByAgentId(Long agentId) {
        List<AgentHeartbeat> list = findByAgentIdOrderByHeartbeatAtDescList(agentId);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }
}

