package led.mega.controller;

// [REACTIVE] REST API Controller 전환 핵심 요약
//
// MVC (기존):                          WebFlux (reactive):
// ResponseEntity<T>                 → Mono<ResponseEntity<T>>
// ResponseEntity<List<T>>           → Flux<T>  (WebFlux가 Flux를 JSON 배열로 직렬화)
// try-catch + return                → .onErrorReturn(ResponseEntity.badRequest().build())
// ResponseEntity.ok(service.get())  → service.get().map(ResponseEntity::ok)
// 동기 인증 확인                      → Mono 체이닝으로 비동기 처리

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
    private final ExceptionLogService exceptionLogService;
    private final AgentHeartbeatService heartbeatService;

    // [CHANGED] ResponseEntity<List<T>> → Flux<T>
    // WebFlux는 Flux를 자동으로 JSON 배열로 직렬화
    @GetMapping
    public Flux<AgentResponseDto> getAllAgents() {
        return agentService.getAllAgents();
    }

    // [CHANGED] ResponseEntity<T> → Mono<ResponseEntity<T>>
    // [CHANGED] try-catch → .onErrorReturn(badRequest)
    @PostMapping("/register")
    public Mono<ResponseEntity<AgentRegisterResponseDto>> registerAgent(
            @Valid @RequestBody AgentRegisterDto registerDto) {
        return agentService.registerAgent(registerDto)
                .<ResponseEntity<AgentRegisterResponseDto>>map(response -> ResponseEntity.status(HttpStatus.CREATED).body(response))
                .onErrorResume(IllegalArgumentException.class, e -> 
                        Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST).<AgentRegisterResponseDto>build()));
    }

    // [CHANGED] void → Mono<ResponseEntity<Void>>
    // [CHANGED] 동기 에이전트 인증 → Mono 체이닝
    @PostMapping("/{agentId}/heartbeat")
    public Mono<ResponseEntity<Void>> sendHeartbeat(
            @PathVariable String agentId,
            @Valid @RequestBody HeartbeatRequestDto requestDto,
            Authentication authentication) {
        return getAuthenticatedAgentMono(authentication, agentId)
                .flatMap(agent -> heartbeatService.saveHeartbeat(agent.getId(), requestDto))
                .<ResponseEntity<Void>>thenReturn(ResponseEntity.<Void>ok().build())
                .onErrorResume(IllegalArgumentException.class, e -> 
                        Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST).<Void>build()));
    }

    @PostMapping("/{agentId}/metrics")
    public Mono<ResponseEntity<MetricDataResponseDto>> sendMetricData(
            @PathVariable String agentId,
            @Valid @RequestBody MetricDataRequestDto requestDto,
            Authentication authentication) {
        return getAuthenticatedAgentMono(authentication, agentId)
                .flatMap(agent -> metricDataService.saveMetricData(agent.getId(), requestDto))
                .<ResponseEntity<MetricDataResponseDto>>map(response -> ResponseEntity.status(HttpStatus.CREATED).body(response))
                .onErrorResume(IllegalArgumentException.class, e -> 
                        Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST).<MetricDataResponseDto>build()));
    }

    @PostMapping("/{agentId}/exceptions")
    public Mono<ResponseEntity<ExceptionLogResponseDto>> sendExceptionLog(
            @PathVariable String agentId,
            @Valid @RequestBody ExceptionLogRequestDto requestDto,
            Authentication authentication) {
        return getAuthenticatedAgentMono(authentication, agentId)
                .flatMap(agent -> exceptionLogService.saveExceptionLog(agent.getId(), requestDto))
                .<ResponseEntity<ExceptionLogResponseDto>>map(response -> ResponseEntity.status(HttpStatus.CREATED).body(response))
                .onErrorResume(IllegalArgumentException.class, e -> 
                        Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST).<ExceptionLogResponseDto>build()));
    }

    // [CHANGED] 동기 메서드 → Mono 반환 (에러도 Mono.error로)
    private Mono<Agent> getAuthenticatedAgentMono(Authentication authentication, String agentId) {
        if (authentication == null || !(authentication.getPrincipal() instanceof Agent authenticatedAgent)) {
            return Mono.error(new IllegalArgumentException("인증이 필요합니다."));
        }
        if (!authenticatedAgent.getAgentId().equals(agentId)) {
            return Mono.error(new IllegalArgumentException("에이전트 ID가 일치하지 않습니다."));
        }
        return Mono.just(authenticatedAgent);
    }
}
