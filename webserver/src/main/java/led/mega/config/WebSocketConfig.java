package led.mega.config;

// WebSocket 의존성이 제대로 로드되지 않을 경우를 대비해 임시로 비활성화
// IDE에서 Gradle 프로젝트를 새로고침한 후 다시 활성화하세요
/*
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@ConditionalOnClass(name = "org.springframework.messaging.simp.config.MessageBrokerRegistry")
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // 클라이언트가 구독할 수 있는 메시지 브로커 설정
        config.enableSimpleBroker("/topic", "/queue");
        
        // 클라이언트가 메시지를 보낼 때 사용하는 prefix
        config.setApplicationDestinationPrefixes("/app");
        
        // 특정 사용자에게 메시지를 보낼 때 사용하는 prefix
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // WebSocket 연결 엔드포인트 등록
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")  // 개발 환경에서는 모든 origin 허용 (운영 환경에서는 제한 필요)
                .withSockJS();  // SockJS 지원 (fallback 옵션)
        
        // SockJS 없이 순수 WebSocket만 사용하는 경우
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*");
    }
}
*/

