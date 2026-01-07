package led.mega.service;

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

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AgentService {

    private final AgentRepository agentRepository;

    /**
     * 에이전트 등록
     */
    @Transactional
    public AgentRegisterResponseDto registerAgent(AgentRegisterDto registerDto) {
        // 에이전트 ID 중복 확인
        if (agentRepository.existsByAgentId(registerDto.getAgentId())) {
            throw new IllegalArgumentException("이미 등록된 에이전트 ID입니다: " + registerDto.getAgentId());
        }

        // API 키 생성
        String apiKey = generateApiKey();

        // 에이전트 생성
        Agent agent = Agent.builder()
                .agentId(registerDto.getAgentId())
                .name(registerDto.getName())
                .hostname(registerDto.getHostname())
                .ipAddress(registerDto.getIpAddress())
                .osType(registerDto.getOsType())
                .status(AgentStatus.ONLINE)
                .apiKey(apiKey)
                .lastHeartbeat(LocalDateTime.now())
                .build();

        Agent savedAgent = agentRepository.save(agent);
        log.info("에이전트 등록 완료: agentId={}, name={}", savedAgent.getAgentId(), savedAgent.getName());

        return AgentRegisterResponseDto.builder()
                .id(savedAgent.getId())
                .agentId(savedAgent.getAgentId())
                .status(savedAgent.getStatus())
                .apiKey(savedAgent.getApiKey())
                .build();
    }

    /**
     * 에이전트 조회
     */
    public AgentResponseDto getAgent(Long id) {
        Agent agent = agentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("에이전트를 찾을 수 없습니다. id: " + id));
        return toResponseDto(agent);
    }

    /**
     * 에이전트 ID로 조회
     */
    public AgentResponseDto getAgentByAgentId(String agentId) {
        Agent agent = agentRepository.findByAgentId(agentId)
                .orElseThrow(() -> new IllegalArgumentException("에이전트를 찾을 수 없습니다. agentId: " + agentId));
        return toResponseDto(agent);
    }

    /**
     * 모든 에이전트 조회
     */
    public List<AgentResponseDto> getAllAgents() {
        return agentRepository.findAll().stream()
                .map(this::toResponseDto)
                .collect(Collectors.toList());
    }

    /**
     * 상태별 에이전트 조회
     */
    public List<AgentResponseDto> getAgentsByStatus(AgentStatus status) {
        return agentRepository.findByStatus(status).stream()
                .map(this::toResponseDto)
                .collect(Collectors.toList());
    }

    /**
     * API 키로 에이전트 조회 (인증용)
     */
    public Agent findByApiKey(String apiKey) {
        return agentRepository.findAll().stream()
                .filter(agent -> apiKey.equals(agent.getApiKey()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 API 키입니다."));
    }

    /**
     * 하트비트 업데이트
     */
    @Transactional
    public void updateHeartbeat(Long agentId, LocalDateTime heartbeatAt) {
        Agent agent = agentRepository.findById(agentId)
                .orElseThrow(() -> new IllegalArgumentException("에이전트를 찾을 수 없습니다. id: " + agentId));
        
        agent.setLastHeartbeat(heartbeatAt);
        agent.setStatus(AgentStatus.ONLINE);
        agentRepository.save(agent);
    }

    /**
     * 에이전트 상태 업데이트
     */
    @Transactional
    public void updateStatus(Long agentId, AgentStatus status) {
        Agent agent = agentRepository.findById(agentId)
                .orElseThrow(() -> new IllegalArgumentException("에이전트를 찾을 수 없습니다. id: " + agentId));
        
        agent.setStatus(status);
        agentRepository.save(agent);
    }

    /**
     * API 키 생성
     */
    private String generateApiKey() {
        return "mega-" + UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * Entity를 DTO로 변환
     */
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

