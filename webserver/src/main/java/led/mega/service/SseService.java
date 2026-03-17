package led.mega.service;

import led.mega.dto.WebSocketMessageDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.time.LocalDateTime;

@Slf4j
@Service
public class SseService {

    private final Sinks.Many<WebSocketMessageDto> sink =
            Sinks.many().multicast().onBackpressureBuffer();

    public Flux<WebSocketMessageDto> getStream() {
        return Flux.merge(
                sink.asFlux(),
                Flux.interval(java.time.Duration.ofSeconds(30))
                        .map(tick -> WebSocketMessageDto.builder()
                                .type("PING")
                                .timestamp(LocalDateTime.now())
                                .build())
        );
    }

    public void publish(WebSocketMessageDto message) {
        Sinks.EmitResult result = sink.tryEmitNext(message);
        if (result.isFailure()) {
            log.warn("SSE 이벤트 발행 실패: type={}, result={}", message.getType(), result);
        }
    }

    public void broadcastMetric(String agentId, Object metricData) {
        publish(WebSocketMessageDto.builder()
                .type("METRIC").agentId(agentId).data(metricData)
                .timestamp(LocalDateTime.now()).build());
    }

    public void broadcastException(String agentId, Object exceptionData) {
        publish(WebSocketMessageDto.builder()
                .type("EXCEPTION").agentId(agentId).data(exceptionData)
                .timestamp(LocalDateTime.now()).build());
    }

    public void broadcastHeartbeat(String agentId, Object heartbeatData) {
        publish(WebSocketMessageDto.builder()
                .type("HEARTBEAT").agentId(agentId).data(heartbeatData)
                .timestamp(LocalDateTime.now()).build());
    }

    public void broadcastAgentStatus(String agentId, Object statusData) {
        publish(WebSocketMessageDto.builder()
                .type("AGENT_STATUS").agentId(agentId).data(statusData)
                .timestamp(LocalDateTime.now()).build());
    }

    public void broadcastCsEvent(String conversationId, Object csEventData) {
        publish(WebSocketMessageDto.builder()
                .type("CS_EVENT").agentId(conversationId).data(csEventData)
                .timestamp(LocalDateTime.now()).build());
    }
}
