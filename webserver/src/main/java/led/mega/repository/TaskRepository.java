package led.mega.repository;

import led.mega.entity.Task;
import led.mega.entity.TaskType;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

@Repository
public interface TaskRepository extends ReactiveCrudRepository<Task, String> {

    Flux<Task> findByAgentId(String agentId);

    Flux<Task> findByAgentIdAndEnabled(String agentId, Boolean enabled);

    Flux<Task> findByTaskType(TaskType taskType);

    Flux<Task> findByEnabled(Boolean enabled);
}
