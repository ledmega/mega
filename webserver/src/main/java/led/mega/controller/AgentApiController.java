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
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
@RequestMapping("/api/agents")
@RequiredArgsConstructor
public class AgentApiController {

    private final AgentService agentService;
    private final MetricDataService metricDataService;
    private final ServiceMetricDataService serviceMetricDataService;
    private final ExceptionLogService exceptionLogService;
    private final AgentHeartbeatService heartbeatService;

    @GetMapping
    public Flux<AgentResponseDto> getAllAgents() {
        return agentService.getAllAgents();
    }

    @PostMapping("/register")
    public Mono<ResponseEntity<AgentRegisterResponseDto>> registerAgent(
            @Valid @RequestBody AgentRegisterDto registerDto) {
        return agentService.registerAgent(registerDto)
                .<ResponseEntity<AgentRegisterResponseDto>>map(response -> ResponseEntity.status(HttpStatus.CREATED).body(response))
                .onErrorResume(IllegalArgumentException.class, e -> 
                        Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST).<AgentRegisterResponseDto>build()));
    }

    @PostMapping("/{agentId}/heartbeat")
    public Mono<ResponseEntity<Void>> sendHeartbeat(
            @PathVariable String agentId,
            @Valid @RequestBody HeartbeatRequestDto requestDto,
            Authentication authentication) {
        return getAuthenticatedAgentMono(authentication, agentId)
                .flatMap(agent -> heartbeatService.saveHeartbeat(agent.getAgentId(), requestDto))
                .<ResponseEntity<Void>>thenReturn(ResponseEntity.<Void>ok().build())
                .doOnError(e -> log.error("[API] 하트비트 처리 중 오류 발생: {}", e.getMessage(), e))
                .onErrorResume(e -> Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).<Void>build()));
    }

    @PostMapping("/{agentId}/metrics")
    public Mono<ResponseEntity<MetricDataResponseDto>> sendMetricData(
            @PathVariable String agentId,
            @Valid @RequestBody MetricDataRequestDto requestDto,
            Authentication authentication) {
        return getAuthenticatedAgentMono(authentication, agentId)
                .flatMap(agent -> metricDataService.saveMetricData(agent.getAgentId(), requestDto))
                .<ResponseEntity<MetricDataResponseDto>>map(response -> ResponseEntity.status(HttpStatus.CREATED).body(response))
                .doOnError(e -> log.error("[API] 일반 메트릭 수집 처리 중 오류 발생: {}", e.getMessage(), e))
                .onErrorResume(e -> Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).<MetricDataResponseDto>build()));
    }

    @PostMapping("/{agentId}/service-metrics")
    public Mono<ResponseEntity<Void>> sendServiceMetricData(
            @PathVariable String agentId,
            @Valid @RequestBody led.mega.dto.ServiceMetricDataRequestDto requestDto,
            Authentication authentication) {
        return getAuthenticatedAgentMono(authentication, agentId)
                .flatMap(agent -> serviceMetricDataService.saveServiceMetric(agent.getAgentId(), requestDto))
                .<ResponseEntity<Void>>thenReturn(ResponseEntity.status(HttpStatus.CREATED).build())
                .doOnError(e -> log.error("[API] 서비스 메트릭 수집 처리 중 오류 발생: {}", e.getMessage(), e))
                .onErrorResume(e -> Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).<Void>build()));
    }

    @PostMapping("/{agentId}/exceptions")
    public Mono<ResponseEntity<ExceptionLogResponseDto>> sendExceptionLog(
            @PathVariable String agentId,
            @Valid @RequestBody ExceptionLogRequestDto requestDto,
            Authentication authentication) {
        return getAuthenticatedAgentMono(authentication, agentId)
                .flatMap(agent -> exceptionLogService.saveExceptionLog(agent.getAgentId(), requestDto))
                .<ResponseEntity<ExceptionLogResponseDto>>map(response -> ResponseEntity.status(HttpStatus.CREATED).body(response))
                .onErrorResume(IllegalArgumentException.class, e -> 
                        Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST).<ExceptionLogResponseDto>build()));
    }

    private Mono<Agent> getAuthenticatedAgentMono(Authentication authentication, String agentId) {
        if (authentication == null || !(authentication.getPrincipal() instanceof Agent authenticatedAgent)) {
            return Mono.error(new IllegalArgumentException("인증이 필요합니다."));
        }
        // agentId URL 파라미터가 agentRefId(기존 biz ID)일 가능성이 높으므로 agentRefId와 비교
        if (!authenticatedAgent.getAgentRefId().equals(agentId) && !authenticatedAgent.getAgentId().equals(agentId)) {
            return Mono.error(new IllegalArgumentException("에이전트 ID가 일치하지 않습니다."));
        }
        return Mono.just(authenticatedAgent);
    }
}
