package led.mega.cs.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring AI ChatClient 빈 설정.
 *
 * Gemini OpenAI 호환 엔드포인트를 사용합니다.
 * application.properties 에서 base-url을 Gemini 엔드포인트로 설정하면
 * OpenAiChatModel이 자동으로 Gemini API를 호출합니다.
 *
 * 참고: https://ai.google.dev/gemini-api/docs/openai
 * Base URL: https://generativelanguage.googleapis.com/v1beta/openai/
 */
@Configuration
public class CsAiConfig {

    @Bean
    public ChatClient chatClient(OpenAiChatModel openAiChatModel) {
        return ChatClient.builder(openAiChatModel).build();
    }
}
