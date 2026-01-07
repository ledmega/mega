package led.mega.repository;

import led.mega.entity.Agent;
import led.mega.entity.AgentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AgentRepository extends JpaRepository<Agent, Long> {
    
    Optional<Agent> findByAgentId(String agentId);
    
    boolean existsByAgentId(String agentId);
    
    List<Agent> findByStatus(AgentStatus status);
    
    @Query("SELECT a FROM Agent a WHERE a.lastHeartbeat < :threshold")
    List<Agent> findOfflineAgents(@Param("threshold") LocalDateTime threshold);
    
    @Query("SELECT COUNT(a) FROM Agent a WHERE a.status = :status")
    long countByStatus(@Param("status") AgentStatus status);
}

