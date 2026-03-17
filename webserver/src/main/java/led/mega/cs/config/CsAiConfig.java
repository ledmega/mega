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
        OpenAiApi openAiApi = new OpenAiApi(baseUrl, apiKey);

        // 2. ChatOptions 설정 
        // 메서드 부재 에러를 방지하기 위해 빌더 대신 기본 생성자 + 세터를 사용합니다.
        OpenAiChatOptions options = new OpenAiChatOptions();
        options.setModel(modelName);
        options.setTemperature(0.7);

        // 3. ChatModel 생성
        return new OpenAiChatModel(openAiApi, options);
    }

    @Bean
    public ChatClient chatClient(OpenAiChatModel openAiChatModel) {
        return ChatClient.builder(openAiChatModel).build();
    }
}
