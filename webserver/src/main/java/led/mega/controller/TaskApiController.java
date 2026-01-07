package led.mega.controller;

import jakarta.validation.Valid;
import led.mega.dto.TaskRequestDto;
import led.mega.dto.TaskResponseDto;
import led.mega.service.TaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/agents/{agentId}/tasks")
@RequiredArgsConstructor
public class TaskApiController {

    private final TaskService taskService;

    /**
     * 작업 목록 조회
     */
    @GetMapping
    public ResponseEntity<List<TaskResponseDto>> getTasks(@PathVariable Long agentId) {
        List<TaskResponseDto> tasks = taskService.getTasksByAgentId(agentId);
        return ResponseEntity.ok(tasks);
    }

    /**
     * 활성화된 작업 목록 조회
     */
    @GetMapping("/enabled")
    public ResponseEntity<List<TaskResponseDto>> getEnabledTasks(@PathVariable Long agentId) {
        List<TaskResponseDto> tasks = taskService.getEnabledTasksByAgentId(agentId);
        return ResponseEntity.ok(tasks);
    }

    /**
     * 작업 생성
     */
    @PostMapping
    public ResponseEntity<TaskResponseDto> createTask(
            @PathVariable Long agentId,
            @Valid @RequestBody TaskRequestDto requestDto) {
        try {
            TaskResponseDto response = taskService.createTask(agentId, requestDto);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    /**
     * 작업 조회
     */
    @GetMapping("/{taskId}")
    public ResponseEntity<TaskResponseDto> getTask(@PathVariable Long taskId) {
        try {
            TaskResponseDto response = taskService.getTask(taskId);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    /**
     * 작업 수정
     */
    @PutMapping("/{taskId}")
    public ResponseEntity<TaskResponseDto> updateTask(
            @PathVariable Long taskId,
            @Valid @RequestBody TaskRequestDto requestDto) {
        try {
            TaskResponseDto response = taskService.updateTask(taskId, requestDto);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    /**
     * 작업 삭제
     */
    @DeleteMapping("/{taskId}")
    public ResponseEntity<Void> deleteTask(@PathVariable Long taskId) {
        try {
            taskService.deleteTask(taskId);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    /**
     * 작업 활성화/비활성화
     */
    @PatchMapping("/{taskId}/toggle")
    public ResponseEntity<TaskResponseDto> toggleTask(
            @PathVariable Long taskId,
            @RequestParam Boolean enabled) {
        try {
            TaskResponseDto response = taskService.toggleTask(taskId, enabled);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }
}

