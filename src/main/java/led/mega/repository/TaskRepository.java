package led.mega.repository;

import led.mega.entity.Agent;
import led.mega.entity.Task;
import led.mega.entity.TaskType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {
    
    List<Task> findByAgent(Agent agent);
    
    List<Task> findByAgentId(Long agentId);
    
    List<Task> findByAgentIdAndEnabled(Long agentId, Boolean enabled);
    
    List<Task> findByTaskType(TaskType taskType);
    
    List<Task> findByEnabled(Boolean enabled);
}

