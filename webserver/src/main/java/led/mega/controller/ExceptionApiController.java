package led.mega.controller;

import led.mega.dto.ExceptionLogResponseDto;
import led.mega.service.ExceptionLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/agents/{agentId}/exceptions")
@RequiredArgsConstructor
public class ExceptionApiController {

    private final ExceptionLogService exceptionLogService;

    /**
     * 에이전트별 Exception 로그 조회
     */
    @GetMapping
    public ResponseEntity<List<ExceptionLogResponseDto>> getExceptions(@PathVariable Long agentId) {
        List<ExceptionLogResponseDto> exceptions = exceptionLogService.getExceptionLogsByAgentId(agentId);
        return ResponseEntity.ok(exceptions);
    }

    /**
     * 시간 범위별 Exception 로그 조회
     */
    @GetMapping("/range")
    public ResponseEntity<List<ExceptionLogResponseDto>> getExceptionsByTimeRange(
            @PathVariable Long agentId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        List<ExceptionLogResponseDto> exceptions = exceptionLogService.getExceptionLogsByAgentIdAndTimeRange(
                agentId, startTime, endTime);
        return ResponseEntity.ok(exceptions);
    }

    /**
     * Exception 타입별 조회
     */
    @GetMapping("/type/{exceptionType}")
    public ResponseEntity<List<ExceptionLogResponseDto>> getExceptionsByType(
            @PathVariable String exceptionType) {
        List<ExceptionLogResponseDto> exceptions = exceptionLogService.getExceptionLogsByType(exceptionType);
        return ResponseEntity.ok(exceptions);
    }

    /**
     * Exception 타입 및 시간 범위별 조회
     */
    @GetMapping("/type/{exceptionType}/range")
    public ResponseEntity<List<ExceptionLogResponseDto>> getExceptionsByTypeAndTimeRange(
            @PathVariable String exceptionType,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        List<ExceptionLogResponseDto> exceptions = exceptionLogService.getExceptionLogsByTypeAndTimeRange(
                exceptionType, startTime, endTime);
        return ResponseEntity.ok(exceptions);
    }

    /**
     * 최근 Exception 발생 횟수 조회
     */
    @GetMapping("/count")
    public ResponseEntity<Long> countExceptionsSince(
            @PathVariable Long agentId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime since) {
        long count = exceptionLogService.countExceptionLogsSince(agentId, since);
        return ResponseEntity.ok(count);
    }
}

