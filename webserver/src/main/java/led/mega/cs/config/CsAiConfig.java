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
 * OpenAI 호환 모드로 Gemini를 연동합니다.
 */
@Configuration
public class CsAiConfig {
    private static final Logger log = LoggerFactory.getLogger(CsAiConfig.class);

    @Bean
    @Primary
    public OpenAiChatModel openAiChatModel(
            @Value("${spring.ai.openai.api-key}") String apiKey,
            @Value("${spring.ai.openai.base-url}") String baseUrl,
            @Value("${spring.ai.openai.chat.completions-path}") String completionsPath,
            @Value("${spring.ai.openai.chat.options.model}") String modelName) {

        log.info("[CS-BOT-CONFIG] Initializing Gemini via OpenAI Compatibility Mode...");
        log.info("[CS-BOT-CONFIG] Target URL: {}{}", baseUrl, completionsPath);
        log.info("[CS-BOT-CONFIG] Target Model: {}", modelName);

        // OpenAiApi 생성
        // 핵심: baseUrl을 그대로 넘겨주고, properties에서 정의한 completionsPath가 반영되도록 함.
        OpenAiApi openAiApi = new OpenAiApi(baseUrl, apiKey);

        // Options 설정
        OpenAiChatOptions options = new OpenAiChatOptions();
        options.setModel(modelName);
        options.setTemperature(0.7);

        return new OpenAiChatModel(openAiApi, options);
    }

    @Bean
    public ChatClient chatClient(OpenAiChatModel openAiChatModel) {
        return ChatClient.builder(openAiChatModel).build();
    }
}
