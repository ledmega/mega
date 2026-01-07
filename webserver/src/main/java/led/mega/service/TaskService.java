package led.mega.service;

import led.mega.dto.TaskRequestDto;
import led.mega.dto.TaskResponseDto;
import led.mega.entity.Agent;
import led.mega.entity.Task;
import led.mega.repository.AgentRepository;
import led.mega.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;
    private final AgentRepository agentRepository;

    /**
     * 작업 생성
     */
    @Transactional
    public TaskResponseDto createTask(Long agentId, TaskRequestDto requestDto) {
        Agent agent = agentRepository.findById(agentId)
                .orElseThrow(() -> new IllegalArgumentException("에이전트를 찾을 수 없습니다. id: " + agentId));

        Task task = Task.builder()
                .agent(agent)
                .taskName(requestDto.getTaskName())
                .taskType(requestDto.getTaskType())
                .command(requestDto.getCommand())
                .logPath(requestDto.getLogPath())
                .logPattern(requestDto.getLogPattern())
                .intervalSeconds(requestDto.getIntervalSeconds())
                .enabled(requestDto.getEnabled() != null ? requestDto.getEnabled() : true)
                .build();

        Task savedTask = taskRepository.save(task);
        log.info("작업 생성 완료: taskId={}, taskName={}, agentId={}", 
                savedTask.getId(), savedTask.getTaskName(), agentId);

        return toResponseDto(savedTask);
    }

    /**
     * 작업 조회
     */
    public TaskResponseDto getTask(Long id) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("작업을 찾을 수 없습니다. id: " + id));
        return toResponseDto(task);
    }

    /**
     * 에이전트별 작업 목록 조회
     */
    public List<TaskResponseDto> getTasksByAgentId(Long agentId) {
        return taskRepository.findByAgentId(agentId).stream()
                .map(this::toResponseDto)
                .collect(Collectors.toList());
    }

    /**
     * 활성화된 작업 목록 조회
     */
    public List<TaskResponseDto> getEnabledTasksByAgentId(Long agentId) {
        return taskRepository.findByAgentIdAndEnabled(agentId, true).stream()
                .map(this::toResponseDto)
                .collect(Collectors.toList());
    }

    /**
     * 작업 수정
     */
    @Transactional
    public TaskResponseDto updateTask(Long id, TaskRequestDto requestDto) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("작업을 찾을 수 없습니다. id: " + id));

        task.setTaskName(requestDto.getTaskName());
        task.setTaskType(requestDto.getTaskType());
        task.setCommand(requestDto.getCommand());
        task.setLogPath(requestDto.getLogPath());
        task.setLogPattern(requestDto.getLogPattern());
        task.setIntervalSeconds(requestDto.getIntervalSeconds());
        if (requestDto.getEnabled() != null) {
            task.setEnabled(requestDto.getEnabled());
        }

        Task updatedTask = taskRepository.save(task);
        log.info("작업 수정 완료: taskId={}", id);

        return toResponseDto(updatedTask);
    }

    /**
     * 작업 삭제
     */
    @Transactional
    public void deleteTask(Long id) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("작업을 찾을 수 없습니다. id: " + id));
        
        taskRepository.delete(task);
        log.info("작업 삭제 완료: taskId={}", id);
    }

    /**
     * 작업 활성화/비활성화
     */
    @Transactional
    public TaskResponseDto toggleTask(Long id, Boolean enabled) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("작업을 찾을 수 없습니다. id: " + id));
        
        task.setEnabled(enabled);
        Task updatedTask = taskRepository.save(task);
        log.info("작업 상태 변경: taskId={}, enabled={}", id, enabled);

        return toResponseDto(updatedTask);
    }

    /**
     * Entity를 DTO로 변환
     */
    private TaskResponseDto toResponseDto(Task task) {
        return TaskResponseDto.builder()
                .id(task.getId())
                .agentId(task.getAgent().getId())
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

