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

/**
 * 에이전트(Agent) 관련 외부 API를 처리하는 컨트롤러입니다.
 * 에이전트의 등록, 하트비트 수신, 메트릭 데이터 및 예외 로그 수집을 담당합니다.
 * 모든 에이전트 통신은 비동기 Non-blocking 방식으로 처리됩니다.
 */
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

    /**
     * 등록된 모든 에이전트 목록을 조회합니다.
     * 대시보드에서 에이전트 현황을 파악하기 위해 사용됩니다.
     * @return 에이전트 정보 목록 (Flux)
     */
    @GetMapping
    public Flux<AgentResponseDto> getAllAgents() {
        return agentService.getAllAgents();
    }

    /**
     * 새로운 에이전트를 시스템에 등록합니다.
     * 처음 설치된 에이전트가 서버와 통신하기 위해 최조 1회 호출합니다.
     * @param registerDto 등록할 에이전트 정보 (이름, 호스트명 등)
     * @return 등록 완료된 에이전트 정보 및 API 키
     */
    @PostMapping("/register")
    public Mono<ResponseEntity<AgentRegisterResponseDto>> registerAgent(
            @Valid @RequestBody AgentRegisterDto registerDto) {
        log.info("[API] 신규 에이전트 등록 요청: {}", registerDto.getName());
        return agentService.registerAgent(registerDto)
                .<ResponseEntity<AgentRegisterResponseDto>>map(response -> ResponseEntity.status(HttpStatus.CREATED).body(response))
                .onErrorResume(IllegalArgumentException.class, e -> 
                        Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST).<AgentRegisterResponseDto>build()));
    }

    /**
     * 에이전트의 생존 신호(Heartbeat)를 수신합니다.
     * 에이전트가 일정 간격으로 호출하여 서버에 자신의 온라인 상태를 알립니다.
     * @param agentId 에이전트 식별자
     * @param requestDto 현재 상태 정보
     * @param authentication 인증된 에이전트 정보
     * @return 성공 여부 (200 OK)
     */
    @PostMapping("/{agentId}/heartbeat")
    public Mono<ResponseEntity<Void>> sendHeartbeat(
            @PathVariable String agentId,
            @Valid @RequestBody HeartbeatRequestDto requestDto,
            Authentication authentication) {
        // 1. 인증된 에이전트인지, 요청한 ID와 일치하는지 먼저 검증합니다.
        return getAuthenticatedAgentMono(authentication, agentId)
                // 2. 검증 통과 시 하트비트 정보를 DB에 저장하고 마지막 통신 시간을 갱신합니다.
                .flatMap(agent -> heartbeatService.saveHeartbeat(agent.getAgentId(), requestDto))
                .<ResponseEntity<Void>>thenReturn(ResponseEntity.ok().build())
                .doOnError(e -> log.error("[API] 하트비트 처리 중 오류 발생: {}", e.getMessage(), e))
                .onErrorResume(e -> Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).<Void>build()));
    }

    /**
     * 서버의 일반 시스템 메트릭(CPU, Memory, Disk 등)을 수집합니다.
     * 에이전트가 주기적으로 수집한 OS 지표를 서버로 전송할 때 사용합니다.
     * @param agentId 에이전트 식별자
     * @param requestDto 메트릭 데이터 (타입, 값 등)
     * @param authentication 인증된 에이전트 정보
     * @return 저장 완료된 메트릭 응답 정보
     */
    @PostMapping("/{agentId}/metrics")
    public Mono<ResponseEntity<MetricDataResponseDto>> sendMetricData(
            @PathVariable String agentId,
            @Valid @RequestBody MetricDataRequestDto requestDto,
            Authentication authentication) {
        return getAuthenticatedAgentMono(authentication, agentId)
                // 수집된 데이터를 비동기로 저장하고, 실시간 대시보드(SSE)로 즉시 전파합니다.
                .flatMap(agent -> metricDataService.saveMetricData(agent.getAgentId(), requestDto))
                .<ResponseEntity<MetricDataResponseDto>>map(response -> ResponseEntity.status(HttpStatus.CREATED).body(response))
                .doOnError(e -> log.error("[API] 일반 메트릭 수집 처리 중 오류 발생: {}", e.getMessage(), e))
                .onErrorResume(e -> Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()));
    }

    /**
     * 특정 서비스(Docker, Nginx 등)에 특화된 정밀 메트릭을 수집합니다.
     * 에이전트 설정(monitoring_config)에 따라 감시 중인 서비스별 지표를 전송합니다.
     * @param agentId 에이전트 식별자
     * @param requestDto 서비스 메트릭 정보 (프로세스 사용량 등)
     * @param authentication 인증된 에이전트 정보
     * @return 성공 여부 (201 Created)
     */
    @PostMapping("/{agentId}/service-metrics")
    public Mono<ResponseEntity<Void>> sendServiceMetricData(
            @PathVariable String agentId,
            @Valid @RequestBody led.mega.dto.ServiceMetricDataRequestDto requestDto,
            Authentication authentication) {
        return getAuthenticatedAgentMono(authentication, agentId)
                // 특정 애플리케이션 서비스 단위의 성능 지표를 저장합니다.
                .flatMap(agent -> serviceMetricDataService.saveServiceMetric(agent.getAgentId(), requestDto))
                .<ResponseEntity<Void>>thenReturn(ResponseEntity.status(HttpStatus.CREATED).build())
                .doOnError(e -> log.error("[API] 서비스 메트릭 수집 처리 중 오류 발생: {}", e.getMessage(), e))
                .onErrorResume(e -> Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()));
    }

    /**
     * 에이전트에서 감지된 애플리케이션 예외(Exception) 로그를 수집합니다.
     * 에러 발생 시의 로그 메시지와 상세 스택 트레이스를 서버로 전송합니다.
     * @param agentId 에이전트 식별자
     * @param requestDto 예외 정보 (타입, 메시지, 스택트레이스 등)
     * @param authentication 인증된 에이전트 정보
     * @return 저장된 예외 로그 응답 정보
     */
    @PostMapping("/{agentId}/exceptions")
    public Mono<ResponseEntity<ExceptionLogResponseDto>> sendExceptionLog(
            @PathVariable String agentId,
            @Valid @RequestBody ExceptionLogRequestDto requestDto,
            Authentication authentication) {
        return getAuthenticatedAgentMono(authentication, agentId)
                // 발생한 오류 정보를 DB에 기록하고, 관리자 알림 트리거를 발생시킬 준비를 합니다.
                .flatMap(agent -> exceptionLogService.saveExceptionLog(agent.getAgentId(), requestDto))
                .<ResponseEntity<ExceptionLogResponseDto>>map(response -> ResponseEntity.status(HttpStatus.CREATED).body(response))
                .onErrorResume(IllegalArgumentException.class, e -> 
                        Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST).build()));
    }

    /**
     * 현재 API를 호출한 주체가 인증된 에이전트인지, 
     * 그리고 URL의 ID가 본인의 ID가 맞는지 검증하는 내부 헬퍼 메서드입니다.
     * 이는 타 에이전트의 ID로 허위 데이터를 보내는 것을 방지하는 보안 로직입니다.
     * @param authentication Spring Security 인증 객체
     * @param agentId 검증할 에이전트 ID
     * @return 검증 통과 시 에이전트 엔티티 (Mono)
     */
    private Mono<Agent> getAuthenticatedAgentMono(Authentication authentication, String agentId) {
        // 1. 인증 객체가 유효한지 체크합니다.
        if (authentication == null || !(authentication.getPrincipal() instanceof Agent authenticatedAgent)) {
            log.warn("[AUTH] 인증 정보가 없거나 Principal이 Agent가 아닙니다. type: {}", 
                    (authentication != null ? authentication.getPrincipal().getClass().getName() : "null"));
            return Mono.error(new IllegalArgumentException("인증이 필요합니다."));
        }
        
        // 2. 에이전트의 PK(UUID) 또는 RefId(Business ID)가 요청 파라미터와 일치하는지 대조합니다.
        String principalRefId = authenticatedAgent.getAgentRefId();
        String principalId = authenticatedAgent.getAgentId();
        
        if (!principalRefId.equals(agentId) && !principalId.equals(agentId)) {
            log.error("[AUTH] 에이전트 ID 불일치! URL_ID: {}, DB_RefId: {}, DB_PK: {}", 
                    agentId, principalRefId, principalId);
            return Mono.error(new IllegalArgumentException("에이전트 ID가 일치하지 않습니다."));
        }
        
        // 검증 성공 시 인증된 에이전트 정보를 반환합니다.
        return Mono.just(authenticatedAgent);
    }
}
