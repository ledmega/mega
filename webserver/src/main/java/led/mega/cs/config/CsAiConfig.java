package led.mega.cs.config;

import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.support.HttpRequestWrapper;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;

@Configuration
public class CsAiConfig {
    private static final Logger log = LoggerFactory.getLogger(CsAiConfig.class);

    @Bean
    public RestClient.Builder restClientBuilder(
            @Value("${spring.ai.openai.api-key}") String apiKey) {
        return RestClient.builder()
                .requestInterceptor(new GeminiCompatibilityInterceptor(apiKey));
    }

    @Bean
    @Primary
    public OpenAiChatModel openAiChatModel(
            @Value("${spring.ai.openai.api-key}") String apiKey,
            @Value("${spring.ai.openai.base-url}") String baseUrl,
            RestClient.Builder restClientBuilder) {

        log.info("[CS-BOT-CONFIG] Final Configuration for Gemini Integration...");
        
        OpenAiApi openAiApi = new OpenAiApi("https://generativelanguage.googleapis.com/v1beta/openai", apiKey, restClientBuilder, WebClient.builder());

        OpenAiChatOptions options = new OpenAiChatOptions();
        options.setModel("gemini-1.5-flash");
        options.setTemperature(0.7f);

        return new OpenAiChatModel(openAiApi, options);
    }

    static class GeminiCompatibilityInterceptor implements ClientHttpRequestInterceptor {
        private final String apiKey;

        public GeminiCompatibilityInterceptor(String apiKey) {
            this.apiKey = apiKey;
        }

        @Override
        public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
            
            // 1. 구글이 요구하는 '완벽한' 엔드포인트 URL 생성
            // 주안점: /v1/ 을 포함하고 쿼리 파라미터로 key를 전달함
            String finalUrl = "https://generativelanguage.googleapis.com/v1beta/openai/v1/chat/completions?key=" + apiKey;
            
            HttpRequest redirectedRequest = new CustomHttpRequest(request, URI.create(finalUrl));

            // 2. 바디 수술: 모델명 필드의 어떤 방해물도 제거하고 순수하게 전달
            String bodyStr = new String(body, StandardCharsets.UTF_8);
            String fixedBodyStr = bodyStr.replace("models/gemini-1.5-flash", "gemini-1.5-flash");
            
            // 3. 로그 출력 (보안을 위해 Key 제외)
            log.info("[CS-BOT-CONFIG] Target URL: https://generativelanguage.googleapis.com/v1beta/openai/v1/chat/completions?key=***");
            log.info("[CS-BOT-CONFIG] Corrected Body Check: {}", fixedBodyStr.contains("gemini-1.5-flash"));

            return execution.execute(redirectedRequest, fixedBodyStr.getBytes(StandardCharsets.UTF_8));
        }
    }

    static class CustomHttpRequest extends HttpRequestWrapper {
        private final URI newUri;
        public CustomHttpRequest(HttpRequest original, URI newUri) {
            super(original);
            this.newUri = newUri;
        }
        @Override public URI getURI() { return newUri; }
    }
}
