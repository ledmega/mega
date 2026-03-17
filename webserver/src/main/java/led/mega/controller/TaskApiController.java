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
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
@RequestMapping("/api/agents/{agentId}/tasks")
@RequiredArgsConstructor
public class TaskApiController {

    private final TaskService taskService;

    @GetMapping
    public Flux<TaskResponseDto> getTasks(@PathVariable String agentId) {
        return taskService.getTasksByAgentId(agentId);
    }

    @GetMapping("/enabled")
    public Flux<TaskResponseDto> getEnabledTasks(@PathVariable String agentId) {
        return taskService.getEnabledTasksByAgentId(agentId);
    }

    @PostMapping
    public Mono<ResponseEntity<TaskResponseDto>> createTask(
            @PathVariable String agentId,
            @Valid @RequestBody TaskRequestDto requestDto) {
        return taskService.createTask(agentId, requestDto)
                .<ResponseEntity<TaskResponseDto>>map(r -> ResponseEntity.status(HttpStatus.CREATED).body(r))
                .onErrorResume(IllegalArgumentException.class, e -> 
                        Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST).<TaskResponseDto>build()));
    }

    @GetMapping("/{taskId}")
    public Mono<ResponseEntity<TaskResponseDto>> getTask(@PathVariable String taskId) {
        return taskService.getTask(taskId)
                .<ResponseEntity<TaskResponseDto>>map(ResponseEntity::ok)
                .onErrorResume(IllegalArgumentException.class, e -> 
                        Mono.just(ResponseEntity.status(HttpStatus.NOT_FOUND).<TaskResponseDto>build()));
    }

    @PutMapping("/{taskId}")
    public Mono<ResponseEntity<TaskResponseDto>> updateTask(
            @PathVariable String taskId,
            @Valid @RequestBody TaskRequestDto requestDto) {
        return taskService.updateTask(taskId, requestDto)
                .<ResponseEntity<TaskResponseDto>>map(ResponseEntity::ok)
                .onErrorResume(IllegalArgumentException.class, e -> 
                        Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST).<TaskResponseDto>build()));
    }

    @DeleteMapping("/{taskId}")
    public Mono<ResponseEntity<Void>> deleteTask(@PathVariable String taskId) {
        return taskService.deleteTask(taskId)
                .<ResponseEntity<Void>>thenReturn(ResponseEntity.<Void>noContent().build())
                .onErrorResume(IllegalArgumentException.class, e -> 
                        Mono.just(ResponseEntity.status(HttpStatus.NOT_FOUND).<Void>build()));
    }

    @PatchMapping("/{taskId}/toggle")
    public Mono<ResponseEntity<TaskResponseDto>> toggleTask(
            @PathVariable String taskId,
            @RequestParam Boolean enabled) {
        return taskService.toggleTask(taskId, enabled)
                .<ResponseEntity<TaskResponseDto>>map(ResponseEntity::ok)
                .onErrorResume(IllegalArgumentException.class, e -> 
                        Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST).<TaskResponseDto>build()));
    }
}
