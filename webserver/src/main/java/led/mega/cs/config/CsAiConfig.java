package led.mega.cs.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.google.GoogleAiGeminiChatModel;
import org.springframework.ai.google.GoogleAiGeminiChatOptions;
import org.springframework.ai.google.api.GoogleAiGeminiApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Spring AI ChatClient 빈 설정.
 * OpenAI 호환 모드 대신 Native Gemini API를 사용하여 404 에러를 해결합니다.
 */
@Configuration
public class CsAiConfig {
    private static final Logger log = LoggerFactory.getLogger(CsAiConfig.class);

    @Bean
    @Primary
    public GoogleAiGeminiChatModel googleAiGeminiChatModel(
            @Value("${spring.ai.google.api-key}") String apiKey,
            @Value("${spring.ai.google.chat.options.model}") String modelName) {

        log.info("[CS-BOT-CONFIG] Initializing Native Google Gemini API...");
        log.info("[CS-BOT-CONFIG] Target Model: {}", modelName);

        // Native Gemini API 생성
        GoogleAiGeminiApi api = new GoogleAiGeminiApi(apiKey);

        // Options 설정
        GoogleAiGeminiChatOptions options = GoogleAiGeminiChatOptions.builder()
                .withModel(modelName)
                .withTemperature(0.7)
                .build();

        return new GoogleAiGeminiChatModel(api, options);
    }

    @Bean
    public ChatClient chatClient(GoogleAiGeminiChatModel googleAiGeminiChatModel) {
        return ChatClient.builder(googleAiGeminiChatModel).build();
    }
}
