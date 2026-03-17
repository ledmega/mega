package led.mega.cs.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.googleai.GoogleAiChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring AI ChatClient 빈 설정.
 * Google AI Studio Gemini 전용 스타터를 사용합니다.
 */
@Configuration
public class CsAiConfig {

    @Bean
    public ChatClient chatClient(GoogleAiChatModel googleAiChatModel) {
        return ChatClient.builder(googleAiChatModel).build();
    }
}
