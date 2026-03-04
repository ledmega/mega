package led.mega.config;

// [REMOVED] WebSocketConfig - WebFlux에서 STOMP/SockJS 미지원
//
// 기존:
//   @EnableWebSocketMessageBroker
//   STOMP 메시지 브로커 설정 (/topic, /queue, /app)
//   SockJS 폴백 엔드포인트 (/ws)
//
// [REACTIVE] 대체:
//   SseService    - Sinks.Many 기반 Hot Publisher (이벤트 발행)
//   SseController - GET /api/sse/events (text/event-stream 응답)
//   클라이언트   - new EventSource('/api/sse/events') (JS 표준 API)
//
// 이 파일은 비어 있습니다. 모든 실시간 통신은 SSE로 처리합니다.
