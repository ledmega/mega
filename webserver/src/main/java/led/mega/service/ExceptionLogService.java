package led.mega.service;

import led.mega.dto.ExceptionLogRequestDto;
import led.mega.dto.ExceptionLogResponseDto;
import led.mega.entity.ExceptionLog;
import led.mega.repository.AgentRepository;
import led.mega.repository.ExceptionLogRepository;
import led.mega.util.IdGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExceptionLogService {

    private final ExceptionLogRepository exceptionLogRepository;
    private final AgentRepository agentRepository;
    private final SseService sseService;

    @Transactional
    public Mono<ExceptionLogResponseDto> saveExceptionLog(String agentId, ExceptionLogRequestDto requestDto) {
        return agentRepository.findById(agentId)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("에이전트를 찾을 수 없습니다. id: " + agentId)))
                .flatMap(agent -> {
                    // 서비스별 예외는 monitoring_config_id에 저장, task_id는 FK 위반 방지로 null
                    String configId = requestDto.getMonitoringConfigId() != null
                            ? requestDto.getMonitoringConfigId() : requestDto.getTaskId();
                    ExceptionLog exceptionLog = ExceptionLog.builder()
                            .exLogId(IdGenerator.generate(IdGenerator.EXCEPTION))
                            .agentId(agentId)
                            .taskId(null)
                            .monitoringConfigId(configId)
                            .logFilePath(requestDto.getLogFilePath())
                            .exceptionType(requestDto.getExceptionType())
                            .exceptionMessage(requestDto.getExceptionMessage())
                            .contextBefore(requestDto.getContextBefore())
                            .contextAfter(requestDto.getContextAfter())
                            .fullStackTrace(requestDto.getFullStackTrace())
                            .occurredAt(requestDto.getOccurredAt() != null
                                    ? requestDto.getOccurredAt() : LocalDateTime.now())
                            .build();
                    return exceptionLogRepository.save(exceptionLog);
                })
                .map(this::toResponseDto)
                .doOnNext(r -> {
                    log.warn("Exception 로그 저장 완료: agentId={}, exceptionType={}", agentId, r.getExceptionType());
                    sseService.broadcastException(agentId, r);
                });
    }

    public Flux<ExceptionLogResponseDto> getExceptionLogsByAgentId(String agentId) {
        return exceptionLogRepository.findByAgentId(agentId).map(this::toResponseDto);
    }

    public Flux<ExceptionLogResponseDto> getExceptionLogsByAgentIdAndTimeRange(
            String agentId, LocalDateTime startTime, LocalDateTime endTime) {
        return exceptionLogRepository.findByAgentIdAndOccurredAtBetween(agentId, startTime, endTime)
                .map(this::toResponseDto);
    }

    public Flux<ExceptionLogResponseDto> getExceptionLogsByType(String exceptionType) {
        return exceptionLogRepository.findByExceptionType(exceptionType).map(this::toResponseDto);
    }

    public Flux<ExceptionLogResponseDto> getExceptionLogsByTypeAndTimeRange(
            String exceptionType, LocalDateTime startTime, LocalDateTime endTime) {
        return exceptionLogRepository.findByExceptionTypeAndOccurredAtBetween(exceptionType, startTime, endTime)
                .map(this::toResponseDto);
    }

    public Mono<Long> countExceptionLogsSince(String agentId, LocalDateTime since) {
        return exceptionLogRepository.countByAgentIdSince(agentId, since);
    }

    private ExceptionLogResponseDto toResponseDto(ExceptionLog exceptionLog) {
        return ExceptionLogResponseDto.builder()
                .id(exceptionLog.getExLogId())
                .agentId(exceptionLog.getAgentId())
                .taskId(exceptionLog.getTaskId())
                .logFilePath(exceptionLog.getLogFilePath())
                .exceptionType(exceptionLog.getExceptionType())
                .exceptionMessage(exceptionLog.getExceptionMessage())
                .contextBefore(exceptionLog.getContextBefore())
                .contextAfter(exceptionLog.getContextAfter())
                .fullStackTrace(exceptionLog.getFullStackTrace())
                .occurredAt(exceptionLog.getOccurredAt())
                .createdAt(exceptionLog.getCreatedAt())
                .build();
    }

    public Flux<ExceptionLogResponseDto> getRecentExceptions() {
        return exceptionLogRepository.findTop10ByOrderByOccurredAtDesc()
                .map(this::toResponseDto);
    }
}
