package led.mega.service;

// [REACTIVE] WebSocket(STOMP/SockJS) → SSE(Server-Sent Events) 완전 교체
//
// 기존 WebSocketService:
//   - SimpMessagingTemplate.convertAndSend("/topic/...") 으로 브로드캐스트
//   - SockJS + STOMP 클라이언트 필요
//   - 서블릿 기반 (블로킹)
//
// [NEW] SseService:
//   - Sinks.Many<T> : Hot Publisher (구독자 없어도 이벤트 발행 가능)
//   - Sinks.Many.multicast() : 여러 SSE 클라이언트에게 동일 이벤트 방출
//   - .asFlux() : 클라이언트가 연결할 때마다 새 구독 생성
//   - 클라이언트는 단순 EventSource('/api/sse/events') 로 연결 (JS 표준 API)

import led.mega.dto.WebSocketMessageDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.time.LocalDateTime;

@Slf4j
@Service
public class SseService {

    // [NEW] Sinks.Many: 여러 구독자에게 이벤트를 멀티캐스트하는 Hot Publisher
    //   - multicast()        : 현재 연결된 모든 구독자에게 전달
    //   - onBackpressureBuffer() : 구독자가 느릴 경우 버퍼에 보관
    private final Sinks.Many<WebSocketMessageDto> sink =
            Sinks.many().multicast().onBackpressureBuffer();

    // [NEW] getStream(): 클라이언트 연결 시 호출 → 이 Flux를 SSE 응답 스트림으로 사용
    public Flux<WebSocketMessageDto> getStream() {
        return sink.asFlux();
    }

    // [NEW] publish(): 내부에서 이벤트 발행 (서비스 레이어에서 호출)
    public void publish(WebSocketMessageDto message) {
        Sinks.EmitResult result = sink.tryEmitNext(message);
        if (result.isFailure()) {
            log.warn("SSE 이벤트 발행 실패: type={}, result={}", message.getType(), result);
        }
    }

    public void broadcastMetric(Long agentId, Object metricData) {
        publish(WebSocketMessageDto.builder()
                .type("METRIC").agentId(agentId).data(metricData)
                .timestamp(LocalDateTime.now()).build());
    }

    public void broadcastException(Long agentId, Object exceptionData) {
        publish(WebSocketMessageDto.builder()
                .type("EXCEPTION").agentId(agentId).data(exceptionData)
                .timestamp(LocalDateTime.now()).build());
    }

    public void broadcastHeartbeat(Long agentId, Object heartbeatData) {
        publish(WebSocketMessageDto.builder()
                .type("HEARTBEAT").agentId(agentId).data(heartbeatData)
                .timestamp(LocalDateTime.now()).build());
    }

    public void broadcastAgentStatus(Long agentId, Object statusData) {
        publish(WebSocketMessageDto.builder()
                .type("AGENT_STATUS").agentId(agentId).data(statusData)
                .timestamp(LocalDateTime.now()).build());
    }
}
