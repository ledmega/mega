package led.mega.cs.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Spring AI ChatClient 빈 설정.
 */
@Configuration
public class CsAiConfig {
    private static final Logger log = LoggerFactory.getLogger(CsAiConfig.class);

    @Bean
    @Primary
    public OpenAiChatModel openAiChatModel(
            @Value("${spring.ai.openai.api-key}") String apiKey,
            @Value("${spring.ai.openai.chat.options.model}") String modelName) {

        // ✅ Spring AI의 "google" 감지 로직을 우회하기 위해 'a'를 '%61'로 인코딩합니다.
        // Spring AI는 'googleapis.com' 문자열이 없다고 판단하여 모델명을 변조하지 않습니다.
        String sneakyBaseUrl = "https://generativelanguage.google%61pis.com/v1beta/openai";
        
        log.info("[CS-BOT-CONFIG] Gemini OpenAI-compatible mode initialize - model={}, url={}", 
                modelName, sneakyBaseUrl);

        // 1. OpenAiApi 생성
        OpenAiApi openAiApi = new OpenAiApi(sneakyBaseUrl, apiKey);

        // 2. ChatOptions 설정
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
