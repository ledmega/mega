package led.mega.cs.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.googleai.gemini.GoogleAiGeminiChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring AI ChatClient 빈 설정.
 * application.properties 의 spring.ai.google.gemini.api-key 를 사용합니다.
 * Google AI Studio (https://aistudio.google.com/app/apikey) 에서 발급한 API 키를 환경변수로 주입합니다.
 */
@Configuration
public class CsAiConfig {

    @Bean
    public ChatClient chatClient(GoogleAiGeminiChatModel geminiChatModel) {
        return ChatClient.builder(geminiChatModel).build();
    }
}
