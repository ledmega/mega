package led.mega.service;

import led.mega.dto.ExceptionLogRequestDto;
import led.mega.dto.ExceptionLogResponseDto;
import led.mega.entity.Agent;
import led.mega.entity.ExceptionLog;
import led.mega.entity.Task;
import led.mega.repository.AgentRepository;
import led.mega.repository.ExceptionLogRepository;
import led.mega.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExceptionLogService {

    private final ExceptionLogRepository exceptionLogRepository;
    private final AgentRepository agentRepository;
    private final TaskRepository taskRepository;

    /**
     * Exception 로그 저장
     */
    @Transactional
    public ExceptionLogResponseDto saveExceptionLog(Long agentId, ExceptionLogRequestDto requestDto) {
        Agent agent = agentRepository.findById(agentId)
                .orElseThrow(() -> new IllegalArgumentException("에이전트를 찾을 수 없습니다. id: " + agentId));

        Task task = null;
        if (requestDto.getTaskId() != null) {
            task = taskRepository.findById(requestDto.getTaskId())
                    .orElse(null);  // 작업이 없어도 계속 진행
        }

        ExceptionLog exceptionLog = ExceptionLog.builder()
                .agent(agent)
                .task(task)
                .logFilePath(requestDto.getLogFilePath())
                .exceptionType(requestDto.getExceptionType())
                .exceptionMessage(requestDto.getExceptionMessage())
                .contextBefore(requestDto.getContextBefore())
                .contextAfter(requestDto.getContextAfter())
                .fullStackTrace(requestDto.getFullStackTrace())
                .occurredAt(requestDto.getOccurredAt() != null ? requestDto.getOccurredAt() : LocalDateTime.now())
                .build();

        ExceptionLog savedLog = exceptionLogRepository.save(exceptionLog);
        log.warn("Exception 로그 저장 완료: agentId={}, exceptionType={}", 
                agentId, requestDto.getExceptionType());

        // WebSocket으로 실시간 전송 (WebSocketService가 활성화되면 자동으로 작동)
        ExceptionLogResponseDto responseDto = toResponseDto(savedLog);
        // TODO: WebSocketService 활성화 시 주석 해제
        // webSocketService.ifPresent(ws -> ws.broadcastException(agentId, responseDto));

        return responseDto;
    }

    /**
     * 에이전트별 Exception 로그 조회
     */
    public List<ExceptionLogResponseDto> getExceptionLogsByAgentId(Long agentId) {
        return exceptionLogRepository.findByAgentId(agentId).stream()
                .map(this::toResponseDto)
                .collect(Collectors.toList());
    }

    /**
     * 시간 범위별 Exception 로그 조회
     */
    public List<ExceptionLogResponseDto> getExceptionLogsByAgentIdAndTimeRange(
            Long agentId, LocalDateTime startTime, LocalDateTime endTime) {
        return exceptionLogRepository.findByAgentIdAndOccurredAtBetween(agentId, startTime, endTime).stream()
                .map(this::toResponseDto)
                .collect(Collectors.toList());
    }

    /**
     * Exception 타입별 조회
     */
    public List<ExceptionLogResponseDto> getExceptionLogsByType(String exceptionType) {
        return exceptionLogRepository.findByExceptionType(exceptionType).stream()
                .map(this::toResponseDto)
                .collect(Collectors.toList());
    }

    /**
     * Exception 타입 및 시간 범위별 조회
     */
    public List<ExceptionLogResponseDto> getExceptionLogsByTypeAndTimeRange(
            String exceptionType, LocalDateTime startTime, LocalDateTime endTime) {
        return exceptionLogRepository.findByExceptionTypeAndOccurredAtBetween(
                exceptionType, startTime, endTime).stream()
                .map(this::toResponseDto)
                .collect(Collectors.toList());
    }

    /**
     * 최근 Exception 발생 횟수 조회
     */
    public long countExceptionLogsSince(Long agentId, LocalDateTime since) {
        return exceptionLogRepository.countByAgentIdSince(agentId, since);
    }

    /**
     * Entity를 DTO로 변환
     */
    private ExceptionLogResponseDto toResponseDto(ExceptionLog exceptionLog) {
        return ExceptionLogResponseDto.builder()
                .id(exceptionLog.getId())
                .agentId(exceptionLog.getAgent().getId())
                .taskId(exceptionLog.getTask() != null ? exceptionLog.getTask().getId() : null)
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
}

