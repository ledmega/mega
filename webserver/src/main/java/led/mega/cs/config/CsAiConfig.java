package led.mega.cs.config;

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
 * Spring AI 빈 설정.
 * M2 버전에서도 임포트 에러가 없도록 가장 안정적인 클래스들만 사용합니다.
 */
@Configuration
public class CsAiConfig {
    private static final Logger log = LoggerFactory.getLogger(CsAiConfig.class);

    @Bean
    @Primary
    public OpenAiChatModel openAiChatModel(
            @Value("${spring.ai.openai.api-key}") String apiKey,
            @Value("${spring.ai.openai.base-url}") String baseUrl,
            @Value("${spring.ai.openai.chat.options.model}") String modelName) {

        log.info("[CS-BOT-CONFIG] Initializing Gemini via OpenAI Compatibility Mode (M2 Stable)...");
        log.info("[CS-BOT-CONFIG] Target URL: {}", baseUrl);

        // OpenAiApi 생성
        OpenAiApi openAiApi = new OpenAiApi(baseUrl, apiKey);

        // Options 설정 (가장 안전한 직접 생성 방식)
        OpenAiChatOptions options = new OpenAiChatOptions();
        options.setModel(modelName);
        options.setTemperature(0.7f);

        return new OpenAiChatModel(openAiApi, options);
    }
}
