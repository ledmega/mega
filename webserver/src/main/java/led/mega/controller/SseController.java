package led.mega.controller;

// [NEW] SseController - WebSocketConfig 대체
//
// 기존 WebSocket 구조:
//   WebSocketConfig → /ws 엔드포인트 등록 (STOMP over SockJS)
//   클라이언트: new SockJS('/ws') → Stomp.over(socket) → stompClient.subscribe('/topic/...')
//
// [REACTIVE] SSE 구조:
//   SseController → GET /api/sse/events (text/event-stream)
//   클라이언트: new EventSource('/api/sse/events') → addEventListener('METRIC', handler)
//   SseService.getStream() Flux를 ServerSentEvent 스트림으로 변환하여 응답

import led.mega.dto.WebSocketMessageDto;
import led.mega.service.SseService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api/sse")
@RequiredArgsConstructor
public class SseController {

    private final SseService sseService;

    // [NEW] SSE 스트림 엔드포인트
    // produces = text/event-stream : WebFlux가 Flux를 SSE 형식으로 자동 변환
    // ServerSentEvent<T> : event 이름, data, id 등을 명시적으로 지정
    // [FIX] X-Accel-Buffering, Cache-Control 등을 추가하여 Nginx나 프록시에서 버퍼링되어 지연되는 현상 방지
    @GetMapping(value = "/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<Flux<ServerSentEvent<WebSocketMessageDto>>> streamEvents() {
        Flux<ServerSentEvent<WebSocketMessageDto>> flux = sseService.getStream()
                .map(msg -> ServerSentEvent.<WebSocketMessageDto>builder()
                        .event(msg.getType())  // JS: eventSource.addEventListener('METRIC', ...)
                        .data(msg)
                        .build());

        return ResponseEntity.ok()
                .header("X-Accel-Buffering", "no")
                .header("Cache-Control", "no-cache, no-store, max-age=0, must-revalidate")
                .header("Pragma", "no-cache")
                .body(flux);
    }
}
