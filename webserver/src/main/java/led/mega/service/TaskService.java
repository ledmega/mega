package led.mega.service;

import led.mega.dto.TaskRequestDto;
import led.mega.dto.TaskResponseDto;
import led.mega.entity.Task;
import led.mega.repository.AgentRepository;
import led.mega.repository.TaskRepository;
import led.mega.util.IdGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;
    private final AgentRepository agentRepository;

    @Transactional
    public Mono<TaskResponseDto> createTask(String agentId, TaskRequestDto requestDto) {
        return agentRepository.findById(agentId)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("에이전트를 찾을 수 없습니다. id: " + agentId)))
                .flatMap(agent -> {
                    Task task = Task.builder()
                            .taskId(IdGenerator.generate(IdGenerator.TASK))
                            .agentId(agentId)
                            .taskName(requestDto.getTaskName())
                            .taskType(requestDto.getTaskType())
                            .command(requestDto.getCommand())
                            .logPath(requestDto.getLogPath())
                            .logPattern(requestDto.getLogPattern())
                            .intervalSeconds(requestDto.getIntervalSeconds())
                            .enabled(requestDto.getEnabled() != null ? requestDto.getEnabled() : true)
                            .isNew(true)
                            .build();
                    return taskRepository.save(task);
                })
                .map(this::toResponseDto)
                .doOnNext(r -> log.info("작업 생성 완료: taskId={}, taskName={}", r.getId(), r.getTaskName()));
    }

    public Mono<TaskResponseDto> getTask(String id) {
        return taskRepository.findById(id)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("작업을 찾을 수 없습니다. id: " + id)))
                .map(this::toResponseDto);
    }

    public Flux<TaskResponseDto> getTasksByAgentId(String agentId) {
        return taskRepository.findByAgentId(agentId).map(this::toResponseDto);
    }

    public Flux<TaskResponseDto> getEnabledTasksByAgentId(String agentId) {
        return taskRepository.findByAgentIdAndEnabled(agentId, true).map(this::toResponseDto);
    }

    @Transactional
    public Mono<TaskResponseDto> updateTask(String id, TaskRequestDto requestDto) {
        return taskRepository.findById(id)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("작업을 찾을 수 없습니다. id: " + id)))
                .flatMap(task -> {
                    task.setTaskName(requestDto.getTaskName());
                    task.setTaskType(requestDto.getTaskType());
                    task.setCommand(requestDto.getCommand());
                    task.setLogPath(requestDto.getLogPath());
                    task.setLogPattern(requestDto.getLogPattern());
                    task.setIntervalSeconds(requestDto.getIntervalSeconds());
                    if (requestDto.getEnabled() != null) task.setEnabled(requestDto.getEnabled());
                    return taskRepository.save(task);
                })
                .map(this::toResponseDto)
                .doOnNext(r -> log.info("작업 수정 완료: taskId={}", r.getId()));
    }

    @Transactional
    public Mono<Void> deleteTask(String id) {
        return taskRepository.findById(id)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("작업을 찾을 수 없습니다. id: " + id)))
                .flatMap(task -> taskRepository.delete(task))
                .doOnSuccess(v -> log.info("작업 삭제 완료: taskId={}", id));
    }

    @Transactional
    public Mono<TaskResponseDto> toggleTask(String id, Boolean enabled) {
        return taskRepository.findById(id)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("작업을 찾을 수 없습니다. id: " + id)))
                .flatMap(task -> {
                    task.setEnabled(enabled);
                    return taskRepository.save(task);
                })
                .map(this::toResponseDto)
                .doOnNext(r -> log.info("작업 상태 변경: taskId={}, enabled={}", r.getId(), enabled));
    }

    private TaskResponseDto toResponseDto(Task task) {
        return TaskResponseDto.builder()
                .id(task.getTaskId())
                .agentId(task.getAgentId())
                .taskName(task.getTaskName())
                .taskType(task.getTaskType())
                .command(task.getCommand())
                .logPath(task.getLogPath())
                .logPattern(task.getLogPattern())
                .intervalSeconds(task.getIntervalSeconds())
                .enabled(task.getEnabled())
                .createdAt(task.getCreatedAt())
                .updatedAt(task.getUpdatedAt())
                .build();
    }
}
