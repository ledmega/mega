package led.mega.service;

// [REACTIVE] 핵심 변경점
// - ExceptionLogResponseDto → Mono<ExceptionLogResponseDto>
// - List<ExceptionLogResponseDto> → Flux<ExceptionLogResponseDto>
// - long → Mono<Long>
// - .agent(agent).task(task) → .agentId(agentId).taskId(taskId)  (엔티티 직접 참조 제거)
// - Task 조회 불필요: requestDto.getTaskId()를 바로 사용

import led.mega.dto.ExceptionLogRequestDto;
import led.mega.dto.ExceptionLogResponseDto;
import led.mega.entity.ExceptionLog;
import led.mega.repository.AgentRepository;
import led.mega.repository.ExceptionLogRepository;
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
    // [REMOVED] TaskRepository: task 엔티티 조회 불필요 (taskId Long 직접 사용)

    @Transactional
    public Mono<ExceptionLogResponseDto> saveExceptionLog(Long agentId, ExceptionLogRequestDto requestDto) {
        return agentRepository.findById(agentId)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("에이전트를 찾을 수 없습니다. id: " + agentId)))
                .flatMap(agent -> {
                    // 서비스별 예외는 monitoring_config_id에 저장, task_id는 FK 위반 방지로 null
                    Long configId = requestDto.getMonitoringConfigId() != null
                            ? requestDto.getMonitoringConfigId() : requestDto.getTaskId();
                    ExceptionLog exceptionLog = ExceptionLog.builder()
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
                .doOnNext(r -> log.warn("Exception 로그 저장 완료: agentId={}, exceptionType={}",
                        agentId, r.getExceptionType()));
    }

    // [CHANGED] List → Flux, .stream().map().collect() → .map()
    public Flux<ExceptionLogResponseDto> getExceptionLogsByAgentId(Long agentId) {
        return exceptionLogRepository.findByAgentId(agentId).map(this::toResponseDto);
    }

    public Flux<ExceptionLogResponseDto> getExceptionLogsByAgentIdAndTimeRange(
            Long agentId, LocalDateTime startTime, LocalDateTime endTime) {
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

    // [CHANGED] long → Mono<Long>
    public Mono<Long> countExceptionLogsSince(Long agentId, LocalDateTime since) {
        return exceptionLogRepository.countByAgentIdSince(agentId, since);
    }

    private ExceptionLogResponseDto toResponseDto(ExceptionLog exceptionLog) {
        return ExceptionLogResponseDto.builder()
                .id(exceptionLog.getId())
                .agentId(exceptionLog.getAgentId())   // [CHANGED] .getAgent().getId() → .getAgentId()
                .taskId(exceptionLog.getTaskId())     // [CHANGED] .getTask().getId()  → .getTaskId()
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

