package led.mega.cs.config;

import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.client.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Spring AI 빈 설정.
 */
@Configuration
public class CsAiConfig {
    private static final Logger log = LoggerFactory.getLogger(CsAiConfig.class);

    @Bean
    public RestClient.Builder restClientBuilder() {
        return RestClient.builder();
    }

    @Bean
    @Primary
    public OpenAiChatModel openAiChatModel(
            @Value("${spring.ai.openai.api-key}") String apiKey,
            @Value("${spring.ai.openai.base-url}") String baseUrl,
            @Value("${spring.ai.openai.chat.options.model}") String modelName) {

        log.info("[CS-BOT-CONFIG] Initializing Gemini via OpenAI Compatibility Mode...");
        log.info("[CS-BOT-CONFIG] Target URL Base: {}", baseUrl);

        // OpenAiApi 생성 (URL 뒤에 /v1이 붙지 않도록 properties 설정을 따름)
        OpenAiApi openAiApi = new OpenAiApi(baseUrl, apiKey);

        // Options 설정
        OpenAiChatOptions options = new OpenAiChatOptions();
        options.setModel(modelName);
        options.setTemperature(0.7f);

        return new OpenAiChatModel(openAiApi, options);
    }
}
