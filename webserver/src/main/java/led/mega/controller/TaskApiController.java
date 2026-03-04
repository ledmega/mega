package led.mega.controller;

// [REACTIVE] 전환 패턴 정리
// - ResponseEntity<List<T>> → Flux<T>
// - ResponseEntity<T>       → Mono<ResponseEntity<T>>
// - ResponseEntity<Void>    → Mono<ResponseEntity<Void>>
// - try-catch               → .onErrorReturn(ResponseEntity.badRequest().build())

import jakarta.validation.Valid;
import led.mega.dto.TaskRequestDto;
import led.mega.dto.TaskResponseDto;
import led.mega.service.TaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
@RequestMapping("/api/agents/{agentId}/tasks")
@RequiredArgsConstructor
public class TaskApiController {

    private final TaskService taskService;

    @GetMapping
    public Flux<TaskResponseDto> getTasks(@PathVariable Long agentId) {
        return taskService.getTasksByAgentId(agentId);
    }

    @GetMapping("/enabled")
    public Flux<TaskResponseDto> getEnabledTasks(@PathVariable Long agentId) {
        return taskService.getEnabledTasksByAgentId(agentId);
    }

    @PostMapping
    public Mono<ResponseEntity<TaskResponseDto>> createTask(
            @PathVariable Long agentId,
            @Valid @RequestBody TaskRequestDto requestDto) {
        return taskService.createTask(agentId, requestDto)
                .map(r -> ResponseEntity.status(HttpStatus.CREATED).body(r))
                .onErrorReturn(IllegalArgumentException.class,
                        ResponseEntity.status(HttpStatus.BAD_REQUEST).build());
    }

    @GetMapping("/{taskId}")
    public Mono<ResponseEntity<TaskResponseDto>> getTask(@PathVariable Long taskId) {
        return taskService.getTask(taskId)
                .map(ResponseEntity::ok)
                .onErrorReturn(IllegalArgumentException.class,
                        ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    @PutMapping("/{taskId}")
    public Mono<ResponseEntity<TaskResponseDto>> updateTask(
            @PathVariable Long taskId,
            @Valid @RequestBody TaskRequestDto requestDto) {
        return taskService.updateTask(taskId, requestDto)
                .map(ResponseEntity::ok)
                .onErrorReturn(IllegalArgumentException.class,
                        ResponseEntity.status(HttpStatus.BAD_REQUEST).build());
    }

    // [CHANGED] ResponseEntity<Void> → Mono<ResponseEntity<Void>>
    @DeleteMapping("/{taskId}")
    public Mono<ResponseEntity<Void>> deleteTask(@PathVariable Long taskId) {
        return taskService.deleteTask(taskId)
                .thenReturn(ResponseEntity.<Void>noContent().build())
                .onErrorReturn(IllegalArgumentException.class,
                        ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    @PatchMapping("/{taskId}/toggle")
    public Mono<ResponseEntity<TaskResponseDto>> toggleTask(
            @PathVariable Long taskId,
            @RequestParam Boolean enabled) {
        return taskService.toggleTask(taskId, enabled)
                .map(ResponseEntity::ok)
                .onErrorReturn(IllegalArgumentException.class,
                        ResponseEntity.status(HttpStatus.BAD_REQUEST).build());
    }
}

