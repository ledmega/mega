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

        log.info("[CS-BOT-CONFIG] Applying final URL fix from Gemini's advice...");
        
        // Base URL은 인터셉터에서 완전히 무시하고 재정의할 예정이므로 기본값만 유지
        OpenAiApi openAiApi = new OpenAiApi("https://generativelanguage.googleapis.com", apiKey, restClientBuilder, WebClient.builder());

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
            
            // 제미나이 답변(v1beta 또는 v1 안정화 버전)을 적용한 최종 엔드포인트
            // 경로에서 /openai 를 완전히 제거함
            String finalUrl = "https://generativelanguage.googleapis.com/v1beta/chat/completions?key=" + apiKey;
            
            HttpRequest redirectedRequest = new CustomHttpRequest(request, URI.create(finalUrl));

            // 바디 로그 및 모델명 체크
            String bodyStr = new String(body, StandardCharsets.UTF_8);
            
            // 혹시라도 models/ 가 붙어있다면 제거 (제미나이 가이드 준수)
            String fixedBodyStr = bodyStr.replace("models/gemini-1.5-flash", "gemini-1.5-flash");

            log.info("[CS-BOT-CONFIG] Sending request to Gemini's suggested URL: {}", finalUrl.split("key=")[0] + "key=***");
            
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
