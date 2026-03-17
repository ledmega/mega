package led.mega.cs.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Spring AI ChatClient 빈 설정.
 * Spring AI의 자동 모델명 변조(models/ 접두사 추가)를 방지하기 위해 
 * OpenAiChatModel을 수동으로 빌드합니다.
 */
@Configuration
public class CsAiConfig {

    @Bean
    @Primary
    public OpenAiChatModel openAiChatModel(
            @Value("${spring.ai.openai.api-key}") String apiKey,
            @Value("${spring.ai.openai.base-url}") String baseUrl,
            @Value("${spring.ai.openai.chat.options.model}") String modelName) {

        // 1. OpenAiApi 생성 (Google을 속이기 위해 하드코딩된 정확한 엔드포인트 사용)
        // baseUrl: https://generativelanguage.googleapis.com/v1beta/openai
        OpenAiApi openAiApi = new OpenAiApi(baseUrl, apiKey);

        // 2. ChatModel 생성
        return new OpenAiChatModel(openAiApi, OpenAiChatOptions.builder()
                .withModel(modelName) // gemini-1.5-flash
                .withTemperature(0.7f)
                .build());
    }

    @Bean
    public ChatClient chatClient(OpenAiChatModel openAiChatModel) {
        return ChatClient.builder(openAiChatModel).build();
    }
}
