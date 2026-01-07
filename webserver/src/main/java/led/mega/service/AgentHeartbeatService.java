package led.mega.service;

import led.mega.dto.HeartbeatRequestDto;
import led.mega.entity.Agent;
import led.mega.entity.AgentHeartbeat;
import led.mega.entity.AgentStatus;
import led.mega.repository.AgentHeartbeatRepository;
import led.mega.repository.AgentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AgentHeartbeatService {

    private final AgentHeartbeatRepository heartbeatRepository;
    private final AgentRepository agentRepository;

    /**
     * 하트비트 저장
     */
    @Transactional
    public AgentHeartbeat saveHeartbeat(Long agentId, HeartbeatRequestDto requestDto) {
        Agent agent = agentRepository.findById(agentId)
                .orElseThrow(() -> new IllegalArgumentException("에이전트를 찾을 수 없습니다. id: " + agentId));

        AgentStatus status;
        try {
            status = AgentStatus.valueOf(requestDto.getStatus().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("유효하지 않은 상태입니다: " + requestDto.getStatus());
        }

        // 하트비트 저장
        AgentHeartbeat heartbeat = AgentHeartbeat.builder()
                .agent(agent)
                .status(status)
                .heartbeatAt(requestDto.getHeartbeatAt() != null ? requestDto.getHeartbeatAt() : LocalDateTime.now())
                .build();

        AgentHeartbeat savedHeartbeat = heartbeatRepository.save(heartbeat);

        // 에이전트 상태 및 하트비트 시간 업데이트
        agent.setStatus(status);
        agent.setLastHeartbeat(savedHeartbeat.getHeartbeatAt());
        agentRepository.save(agent);

        log.debug("하트비트 저장 완료: agentId={}, status={}", agentId, status);

        // WebSocket으로 실시간 전송 (WebSocketService가 활성화되면 자동으로 작동)
        // TODO: WebSocketService 활성화 시 주석 해제
        // webSocketService.ifPresent(ws -> {
        //     ws.broadcastHeartbeat(agentId, savedHeartbeat);
        //     ws.broadcastAgentStatus(agentId, agent);
        // });

        return savedHeartbeat;
    }

    /**
     * 에이전트별 하트비트 조회
     */
    public List<AgentHeartbeat> getHeartbeatsByAgentId(Long agentId) {
        return heartbeatRepository.findByAgentId(agentId);
    }

    /**
     * 최신 하트비트 조회
     */
    public AgentHeartbeat getLatestHeartbeat(Long agentId) {
        return heartbeatRepository.findLatestByAgentId(agentId)
                .orElse(null);
    }

    /**
     * 시간 범위별 하트비트 조회
     */
    public List<AgentHeartbeat> getHeartbeatsByAgentIdAndTimeRange(
            Long agentId, LocalDateTime startTime, LocalDateTime endTime) {
        return heartbeatRepository.findByAgentIdAndHeartbeatAtBetween(agentId, startTime, endTime);
    }
}

