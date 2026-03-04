package led.mega.service;

// [REACTIVE] 핵심 변경점
// - AgentHeartbeat → Mono<AgentHeartbeat>
// - 동기 예외 throw → Mono.error(...)
// - heartbeat 저장 후 agent 업데이트: flatMap 체이닝
// - .orElse(null) → .next() (Flux의 첫 번째 요소, 없으면 empty Mono)

import led.mega.dto.HeartbeatRequestDto;
import led.mega.entity.AgentHeartbeat;
import led.mega.entity.AgentStatus;
import led.mega.repository.AgentHeartbeatRepository;
import led.mega.repository.AgentRepository;
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

    // [CHANGED] AgentHeartbeat → Mono<AgentHeartbeat>
    @Transactional
    public Mono<AgentHeartbeat> saveHeartbeat(Long agentId, HeartbeatRequestDto requestDto) {
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
                            .agentId(agentId) // [CHANGED] .agent(agent) → .agentId(agentId)
                            .status(finalStatus)
                            .heartbeatAt(requestDto.getHeartbeatAt() != null
                                    ? requestDto.getHeartbeatAt() : LocalDateTime.now())
                            .build();

                    // [CHANGED] 동기 순차 저장 → flatMap으로 비동기 체이닝
                    return heartbeatRepository.save(heartbeat)
                            .flatMap(saved -> {
                                agent.setStatus(finalStatus);
                                agent.setLastHeartbeat(saved.getHeartbeatAt());
                                return agentRepository.save(agent).thenReturn(saved);
                            });
                })
                .doOnNext(h -> log.debug("하트비트 저장 완료: agentId={}, status={}", agentId, finalStatus));
    }

    // [CHANGED] List<AgentHeartbeat> → Flux<AgentHeartbeat>
    public Flux<AgentHeartbeat> getHeartbeatsByAgentId(Long agentId) {
        return heartbeatRepository.findByAgentId(agentId);
    }

    // [CHANGED] AgentHeartbeat → Mono<AgentHeartbeat>
    // [CHANGED] findLatestByAgentId(default 메서드) → findByAgentIdOrderByHeartbeatAtDesc().next()
    public Mono<AgentHeartbeat> getLatestHeartbeat(Long agentId) {
        return heartbeatRepository.findByAgentIdOrderByHeartbeatAtDesc(agentId).next();
    }

    // [CHANGED] List → Flux
    public Flux<AgentHeartbeat> getHeartbeatsByAgentIdAndTimeRange(
            Long agentId, LocalDateTime startTime, LocalDateTime endTime) {
        return heartbeatRepository.findByAgentIdAndHeartbeatAtBetween(agentId, startTime, endTime);
    }
}

