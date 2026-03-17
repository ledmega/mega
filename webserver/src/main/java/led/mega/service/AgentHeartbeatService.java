package led.mega.service;

import led.mega.dto.HeartbeatRequestDto;
import led.mega.entity.AgentHeartbeat;
import led.mega.entity.AgentStatus;
import led.mega.repository.AgentHeartbeatRepository;
import led.mega.repository.AgentRepository;
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
public class AgentHeartbeatService {

    private final AgentHeartbeatRepository heartbeatRepository;
    private final AgentRepository agentRepository;
    private final SseService sseService;

    @Transactional
    public Mono<AgentHeartbeat> saveHeartbeat(String agentId, HeartbeatRequestDto requestDto) {
        AgentStatus status;
        try {
            status = AgentStatus.valueOf(requestDto.getStatus().toUpperCase());
        } catch (IllegalArgumentException e) {
            return Mono.error(new IllegalArgumentException("유효하지 않은 상태입니다: " + requestDto.getStatus()));
        }

        final AgentStatus finalStatus = status;

        return agentRepository.findById(agentId)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("에이전트를 찾을 수 없습니다. id: " + agentId)))
                .flatMap(agent -> {
                    AgentHeartbeat heartbeat = AgentHeartbeat.builder()
                            .hbId(IdGenerator.generate(IdGenerator.HEARTBEAT))
                            .agentId(agentId)
                            .status(finalStatus)
                            .heartbeatAt(requestDto.getHeartbeatAt() != null
                                    ? requestDto.getHeartbeatAt() : LocalDateTime.now())
                            .build();

                    return heartbeatRepository.save(heartbeat)
                            .flatMap(saved -> {
                                agent.setStatus(finalStatus);
                                agent.setLastHeartbeat(saved.getHeartbeatAt());
                                return agentRepository.save(agent)
                                        .doOnNext(updatedAgent -> sseService.broadcastAgentStatus(agentId, updatedAgent))
                                        .thenReturn(saved);
                            });
                })
                .doOnNext(h -> {
                    log.debug("하트비트 저장 완료: agentId={}, status={}", agentId, finalStatus);
                    sseService.broadcastHeartbeat(agentId, h);
                });
    }

    public Flux<AgentHeartbeat> getHeartbeatsByAgentId(String agentId) {
        return heartbeatRepository.findByAgentId(agentId);
    }

    public Mono<AgentHeartbeat> getLatestHeartbeat(String agentId) {
        return heartbeatRepository.findByAgentIdOrderByHeartbeatAtDesc(agentId).next();
    }

    public Flux<AgentHeartbeat> getHeartbeatsByAgentIdAndTimeRange(
            String agentId, LocalDateTime startTime, LocalDateTime endTime) {
        return heartbeatRepository.findByAgentIdAndHeartbeatAtBetween(agentId, startTime, endTime);
    }
}
