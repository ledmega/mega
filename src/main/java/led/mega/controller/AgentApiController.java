package led.mega.controller;

import jakarta.validation.Valid;
import led.mega.dto.*;
import led.mega.entity.Agent;
import led.mega.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/agents")
@RequiredArgsConstructor
public class AgentApiController {

    private final AgentService agentService;
    private final MetricDataService metricDataService;
    private final ExceptionLogService exceptionLogService;
    private final AgentHeartbeatService heartbeatService;

    /**
     * 에이전트 목록 조회 (웹 대시보드용)
     */
    @GetMapping
    public ResponseEntity<List<AgentResponseDto>> getAllAgents() {
        List<AgentResponseDto> agents = agentService.getAllAgents();
        return ResponseEntity.ok(agents);
    }

    /**
     * 에이전트 등록 (공개 API)
     */
    @PostMapping("/register")
    public ResponseEntity<AgentRegisterResponseDto> registerAgent(@Valid @RequestBody AgentRegisterDto registerDto) {
        try {
            AgentRegisterResponseDto response = agentService.registerAgent(registerDto);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            log.error("에이전트 등록 실패: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    /**
     * 하트비트 전송 (인증 필요)
     */
    @PostMapping("/{agentId}/heartbeat")
    public ResponseEntity<Void> sendHeartbeat(
            @PathVariable String agentId,
            @Valid @RequestBody HeartbeatRequestDto requestDto,
            Authentication authentication) {
        
        try {
            Agent agent = getAuthenticatedAgent(authentication, agentId);
            heartbeatService.saveHeartbeat(agent.getId(), requestDto);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            log.error("하트비트 전송 실패: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    /**
     * 메트릭 데이터 전송 (인증 필요)
     */
    @PostMapping("/{agentId}/metrics")
    public ResponseEntity<MetricDataResponseDto> sendMetricData(
            @PathVariable String agentId,
            @Valid @RequestBody MetricDataRequestDto requestDto,
            Authentication authentication) {
        
        try {
            Agent agent = getAuthenticatedAgent(authentication, agentId);
            MetricDataResponseDto response = metricDataService.saveMetricData(agent.getId(), requestDto);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            log.error("메트릭 데이터 전송 실패: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    /**
     * Exception 로그 전송 (인증 필요)
     */
    @PostMapping("/{agentId}/exceptions")
    public ResponseEntity<ExceptionLogResponseDto> sendExceptionLog(
            @PathVariable String agentId,
            @Valid @RequestBody ExceptionLogRequestDto requestDto,
            Authentication authentication) {
        
        try {
            Agent agent = getAuthenticatedAgent(authentication, agentId);
            ExceptionLogResponseDto response = exceptionLogService.saveExceptionLog(agent.getId(), requestDto);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            log.error("Exception 로그 전송 실패: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    /**
     * 인증된 에이전트 확인
     */
    private Agent getAuthenticatedAgent(Authentication authentication, String agentId) {
        if (authentication == null || !(authentication.getPrincipal() instanceof Agent)) {
            throw new IllegalArgumentException("인증이 필요합니다.");
        }
        
        Agent authenticatedAgent = (Agent) authentication.getPrincipal();
        
        // 요청한 agentId와 인증된 에이전트의 agentId가 일치하는지 확인
        if (!authenticatedAgent.getAgentId().equals(agentId)) {
            throw new IllegalArgumentException("에이전트 ID가 일치하지 않습니다.");
        }
        
        return authenticatedAgent;
    }
}

