package led.mega.service;

// [REACTIVE] 블로킹 → 논블로킹 전환 핵심 패턴
// - T           → Mono<T>      : 단건 결과
// - List<T>     → Flux<T>      : 다건 결과
// - void        → Mono<Void>   : 반환값 없는 비동기
// - orElseThrow → switchIfEmpty(Mono.error(...))
// - if 체크     → flatMap 내부 Mono.error(...)
// - .stream()   → .map()  (Flux는 스트림 연산 내장)
// - @Transactional: R2dbcTransactionManager 통해 반응형 트랜잭션 적용

import led.mega.dto.AgentRegisterDto;
import led.mega.dto.AgentRegisterResponseDto;
import led.mega.dto.AgentResponseDto;
import led.mega.entity.Agent;
import led.mega.entity.AgentStatus;
import led.mega.repository.AgentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AgentService {

    private final AgentRepository agentRepository;

    @Transactional
    public Mono<AgentRegisterResponseDto> registerAgent(AgentRegisterDto registerDto) {
        return agentRepository.findByAgentId(registerDto.getAgentId())
                .flatMap(existingAgent -> {
                    // 이미 존재하면 정보 업데이트
                    existingAgent.setName(registerDto.getName());
                    existingAgent.setHostname(registerDto.getHostname());
                    existingAgent.setIpAddress(registerDto.getIpAddress());
                    existingAgent.setOsType(registerDto.getOsType());
                    existingAgent.setStatus(AgentStatus.ONLINE);
                    existingAgent.setLastHeartbeat(LocalDateTime.now());
                    return agentRepository.save(existingAgent);
                })
                .switchIfEmpty(Mono.defer(() -> {
                    // 존재하지 않으면 신규 생성
                    Agent agent = Agent.builder()
                            .agentId(registerDto.getAgentId())
                            .name(registerDto.getName())
                            .hostname(registerDto.getHostname())
                            .ipAddress(registerDto.getIpAddress())
                            .osType(registerDto.getOsType())
                            .status(AgentStatus.ONLINE)
                            .apiKey(generateApiKey())
                            .lastHeartbeat(LocalDateTime.now())
                            .build();
                    return agentRepository.save(agent);
                }))
                .map(saved -> AgentRegisterResponseDto.builder()
                        .id(saved.getId())
                        .agentId(saved.getAgentId())
                        .status(saved.getStatus())
                        .apiKey(saved.getApiKey())
                        .build())
                .doOnNext(r -> log.info("에이전트 등록/업데이트 완료: agentId={}", r.getAgentId()));
    }

    // [CHANGED] AgentResponseDto → Mono<AgentResponseDto>
    // [CHANGED] orElseThrow → switchIfEmpty(Mono.error(...))
    public Mono<AgentResponseDto> getAgent(Long id) {
        return agentRepository.findById(id)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("에이전트를 찾을 수 없습니다. id: " + id)))
                .map(this::toResponseDto);
    }

    public Mono<AgentResponseDto> getAgentByAgentId(String agentId) {
        return agentRepository.findByAgentId(agentId)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("에이전트를 찾을 수 없습니다. agentId: " + agentId)))
                .map(this::toResponseDto);
    }

    // [CHANGED] List<AgentResponseDto> → Flux<AgentResponseDto>
    // [CHANGED] .stream().map().collect() → .map() (Flux는 스트림 연산 내장)
    public Flux<AgentResponseDto> getAllAgents() {
        return agentRepository.findAll().map(this::toResponseDto);
    }

    public Flux<AgentResponseDto> getAgentsByStatus(AgentStatus status) {
        return agentRepository.findByStatus(status).map(this::toResponseDto);
    }

    // [CHANGED] Agent → Mono<Agent>
    // [CHANGED] .stream().filter().findFirst().orElseThrow → .filter().next().switchIfEmpty
    public Mono<Agent> findByApiKey(String apiKey) {
        return agentRepository.findAll()
                .filter(agent -> apiKey.equals(agent.getApiKey()))
                .next()
                .switchIfEmpty(Mono.error(new IllegalArgumentException("유효하지 않은 API 키입니다.")));
    }

    // [CHANGED] void → Mono<Void>
    // [CHANGED] 동기 orElseThrow → flatMap 체이닝
    @Transactional
    public Mono<Void> updateHeartbeat(Long agentId, LocalDateTime heartbeatAt) {
        return agentRepository.findById(agentId)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("에이전트를 찾을 수 없습니다. id: " + agentId)))
                .flatMap(agent -> {
                    agent.setLastHeartbeat(heartbeatAt);
                    agent.setStatus(AgentStatus.ONLINE);
                    return agentRepository.save(agent);
                })
                .then(); // [CHANGED] save 결과 버리고 Mono<Void> 반환
    }

    @Transactional
    public Mono<Void> updateStatus(Long agentId, AgentStatus status) {
        return agentRepository.findById(agentId)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("에이전트를 찾을 수 없습니다. id: " + agentId)))
                .flatMap(agent -> {
                    agent.setStatus(status);
                    return agentRepository.save(agent);
                })
                .then();
    }

    private String generateApiKey() {
        return "mega-" + UUID.randomUUID().toString().replace("-", "");
    }

    private AgentResponseDto toResponseDto(Agent agent) {
        return AgentResponseDto.builder()
                .id(agent.getId())
                .agentId(agent.getAgentId())
                .name(agent.getName())
                .hostname(agent.getHostname())
                .ipAddress(agent.getIpAddress())
                .osType(agent.getOsType())
                .status(agent.getStatus())
                .lastHeartbeat(agent.getLastHeartbeat())
                .createdAt(agent.getCreatedAt())
                .updatedAt(agent.getUpdatedAt())
                .build();
    }
}

