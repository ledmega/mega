package led.mega.repository;

// [REACTIVE] JpaRepository → ReactiveCrudRepository
// - findByAgent(Agent) 제거, findByAgentId(Long) 사용

import led.mega.entity.Task;
import led.mega.entity.TaskType;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

@Repository
public interface TaskRepository extends ReactiveCrudRepository<Task, Long> {

    // [CHANGED] findByAgent(Agent) 제거
    Flux<Task> findByAgentId(Long agentId);

    Flux<Task> findByAgentIdAndEnabled(Long agentId, Boolean enabled);

    Flux<Task> findByTaskType(TaskType taskType);

    Flux<Task> findByEnabled(Boolean enabled);
}

