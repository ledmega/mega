package led.mega.service;

// WebSocket 의존성이 제대로 로드되지 않을 경우를 대비해 임시로 비활성화
// IDE에서 Gradle 프로젝트를 새로고침한 후 다시 활성화하세요
// 
// 사용 방법:
// 1. Cursor/IntelliJ에서 Gradle 탭 열기
// 2. 새로고침 버튼 클릭 (또는 우클릭 > Reload Gradle Project)
// 3. 이 파일의 주석을 제거하고 WebSocketConfig도 활성화

/*
import led.mega.dto.WebSocketMessageDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnBean(SimpMessagingTemplate.class)
public class WebSocketService {

    private final SimpMessagingTemplate messagingTemplate;

    public void broadcast(String destination, WebSocketMessageDto message) {
        messagingTemplate.convertAndSend(destination, message);
        log.debug("WebSocket 브로드캐스트: destination={}, type={}", destination, message.getType());
    }

    public void sendToAgent(String agentId, WebSocketMessageDto message) {
        String destination = "/topic/agent/" + agentId;
        messagingTemplate.convertAndSend(destination, message);
        log.debug("WebSocket 전송: destination={}, type={}", destination, message.getType());
    }

    public void sendToUser(String username, String destination, WebSocketMessageDto message) {
        messagingTemplate.convertAndSendToUser(username, destination, message);
        log.debug("WebSocket 사용자 전송: user={}, destination={}, type={}", username, destination, message.getType());
    }

    public void broadcastMetric(Long agentId, Object metricData) {
        WebSocketMessageDto message = WebSocketMessageDto.builder()
                .type("METRIC")
                .agentId(agentId)
                .data(metricData)
                .timestamp(java.time.LocalDateTime.now())
                .build();
        broadcast("/topic/metrics", message);
        sendToAgent(String.valueOf(agentId), message);
    }

    public void broadcastException(Long agentId, Object exceptionData) {
        WebSocketMessageDto message = WebSocketMessageDto.builder()
                .type("EXCEPTION")
                .agentId(agentId)
                .data(exceptionData)
                .timestamp(java.time.LocalDateTime.now())
                .build();
        broadcast("/topic/exceptions", message);
        sendToAgent(String.valueOf(agentId), message);
    }

    public void broadcastHeartbeat(Long agentId, Object heartbeatData) {
        WebSocketMessageDto message = WebSocketMessageDto.builder()
                .type("HEARTBEAT")
                .agentId(agentId)
                .data(heartbeatData)
                .timestamp(java.time.LocalDateTime.now())
                .build();
        broadcast("/topic/heartbeat", message);
        sendToAgent(String.valueOf(agentId), message);
    }

    public void broadcastAgentStatus(Long agentId, Object statusData) {
        WebSocketMessageDto message = WebSocketMessageDto.builder()
                .type("AGENT_STATUS")
                .agentId(agentId)
                .data(statusData)
                .timestamp(java.time.LocalDateTime.now())
                .build();
        broadcast("/topic/agent-status", message);
        sendToAgent(String.valueOf(agentId), message);
    }
}
*/
