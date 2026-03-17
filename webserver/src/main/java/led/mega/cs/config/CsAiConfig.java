package led.mega.cs.config;

import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Spring AI의 자동 변조를 피하기 위해 가장 순수한 형태로 설정합니다.
 */
@Configuration
public class CsAiConfig {
    private static final Logger log = LoggerFactory.getLogger(CsAiConfig.class);

    @Bean
    public RestClient.Builder restClientBuilder() {
        return RestClient.builder();
    }

    @Bean
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder();
    }

    @Bean
    @Primary
    public OpenAiChatModel openAiChatModel(
            @Value("${spring.ai.openai.api-key}") String apiKey) {

        log.info("[CS-BOT-CONFIG] Switching to Ultra-Stable Legacy Mode...");

        // Spring AI에게 'OpenAI 호환 사이트'가 아님을 인지시키기 위해 로컬 호스트 주소(가짜)를 줍니다.
        // 이렇게 하면 라이브러리의 모든 '구글 전용 변조 로직'이 작동하지 않습니다. 
        OpenAiApi openAiApi = new OpenAiApi("http://localhost:11111", apiKey, RestClient.builder(), WebClient.builder());

        // 실제 호출은 서비스 단에서 우리가 가로챌 예정이거나, 
        // 여기서 아예 가짜 모델을 주고 서비스에서 수동 호출로 바꿀 수도 있습니다.
        // 하지만 일단은 가장 '무해한' 설정을 유지합니다.
        OpenAiChatOptions options = new OpenAiChatOptions();
        options.setModel("gemini-1.5-flash");
        
        return new OpenAiChatModel(openAiApi, options);
    }
}
