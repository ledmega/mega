package led.mega.service;

import led.mega.dto.AgentRegisterDto;
import led.mega.dto.AgentRegisterResponseDto;
import led.mega.dto.AgentResponseDto;
import led.mega.entity.Agent;
import led.mega.entity.AgentStatus;
import led.mega.repository.AgentRepository;
import led.mega.util.IdGenerator;
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
        return agentRepository.findByAgentRefId(registerDto.getAgentId())
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
                            .agentId(IdGenerator.generate(IdGenerator.AGENT))
                            .agentRefId(registerDto.getAgentId())
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
                        .id(saved.getAgentId())
                        .agentRefId(saved.getAgentRefId())
                        .status(saved.getStatus())
                        .apiKey(saved.getApiKey())
                        .build())
                .doOnNext(r -> log.info("에이전트 등록/업데이트 완료: agentRefId={}", r.getAgentRefId()));
    }

    public Mono<AgentResponseDto> getAgent(String id) {
        return agentRepository.findById(id)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("에이전트를 찾을 수 없습니다. id: " + id)))
                .map(this::toResponseDto);
    }

    public Mono<AgentResponseDto> getAgentByAgentRefId(String agentRefId) {
        return agentRepository.findByAgentRefId(agentRefId)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("에이전트를 찾을 수 없습니다. agentRefId: " + agentRefId)))
                .map(this::toResponseDto);
    }

    public Flux<AgentResponseDto> getAllAgents() {
        return agentRepository.findAll().map(this::toResponseDto);
    }

    public Flux<AgentResponseDto> getAgentsByStatus(AgentStatus status) {
        return agentRepository.findByStatus(status).map(this::toResponseDto);
    }

    public Mono<Agent> findByApiKey(String apiKey) {
        return agentRepository.findAll()
                .filter(agent -> apiKey.equals(agent.getApiKey()))
                .next()
                .switchIfEmpty(Mono.error(new IllegalArgumentException("유효하지 않은 API 키입니다.")));
    }

    @Transactional
    public Mono<Void> updateHeartbeat(String agentId, LocalDateTime heartbeatAt) {
        return agentRepository.findById(agentId)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("에이전트를 찾을 수 없습니다. id: " + agentId)))
                .flatMap(agent -> {
                    agent.setLastHeartbeat(heartbeatAt);
                    agent.setStatus(AgentStatus.ONLINE);
                    return agentRepository.save(agent);
                })
                .then();
    }

    @Transactional
    public Mono<Void> updateStatus(String agentId, AgentStatus status) {
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
                .id(agent.getAgentId())
                .agentRefId(agent.getAgentRefId())
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
