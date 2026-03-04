package led.mega.controller;

// [REACTIVE] ResponseEntity<List<T>> → Flux<T>
//            ResponseEntity<long>    → Mono<Long>

import led.mega.dto.ExceptionLogResponseDto;
import led.mega.service.ExceptionLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Slf4j
@RestController
@RequestMapping("/api/agents/{agentId}/exceptions")
@RequiredArgsConstructor
public class ExceptionApiController {

    private final ExceptionLogService exceptionLogService;

    // [CHANGED] ResponseEntity<List<T>> → Flux<T>
    @GetMapping
    public Flux<ExceptionLogResponseDto> getExceptions(@PathVariable Long agentId) {
        return exceptionLogService.getExceptionLogsByAgentId(agentId);
    }

    @GetMapping("/range")
    public Flux<ExceptionLogResponseDto> getExceptionsByTimeRange(
            @PathVariable Long agentId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        return exceptionLogService.getExceptionLogsByAgentIdAndTimeRange(agentId, startTime, endTime);
    }

    @GetMapping("/type/{exceptionType}")
    public Flux<ExceptionLogResponseDto> getExceptionsByType(@PathVariable String exceptionType) {
        return exceptionLogService.getExceptionLogsByType(exceptionType);
    }

    @GetMapping("/type/{exceptionType}/range")
    public Flux<ExceptionLogResponseDto> getExceptionsByTypeAndTimeRange(
            @PathVariable String exceptionType,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        return exceptionLogService.getExceptionLogsByTypeAndTimeRange(exceptionType, startTime, endTime);
    }

    // [CHANGED] ResponseEntity<Long> → Mono<Long>
    @GetMapping("/count")
    public Mono<Long> countExceptionsSince(
            @PathVariable Long agentId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime since) {
        return exceptionLogService.countExceptionLogsSince(agentId, since);
    }
}

