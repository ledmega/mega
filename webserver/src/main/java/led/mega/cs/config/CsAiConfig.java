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
import org.springframework.web.client.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;

@Configuration
public class CsAiConfig {
    private static final Logger log = LoggerFactory.getLogger(CsAiConfig.class);

    @Bean
    public RestClient.Builder restClientBuilder() {
        return RestClient.builder()
                .requestInterceptor(new GeminiCompatibilityInterceptor());
    }

    @Bean
    @Primary
    public OpenAiChatModel openAiChatModel(
            @Value("${spring.ai.openai.api-key}") String apiKey,
            @Value("${spring.ai.openai.base-url}") String baseUrl,
            RestClient.Builder restClientBuilder) {

        log.info("[CS-BOT-CONFIG] Initializing Gemini with Hardcoded Fixes...");
        
        // OpenAiApi 생성 시 RestClient.Builder를 전달하여 인터셉터가 동작하게 함
        OpenAiApi openAiApi = new OpenAiApi(baseUrl, apiKey, restClientBuilder);

        OpenAiChatOptions options = new OpenAiChatOptions();
        options.setModel("gemini-1.5-flash");
        options.setTemperature(0.7f);

        return new OpenAiChatModel(openAiApi, options);
    }

    /**
     * 구글의 OpenAI 호환 레이어와 Spring AI 사이의 모든 불일치를 강제로 조정하는 인터셉터
     */
    static class GeminiCompatibilityInterceptor implements ClientHttpRequestInterceptor {
        @Override
        public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
            
            // 1. URL 강제 고정 (v1main 에러 방어)
            // 구글 공식: https://generativelanguage.googleapis.com/v1beta/openai/v1/chat/completions
            String targetUri = request.getURI().toString()
                    .replaceAll("/v1beta/openai/v1/v1/chat/completions", "/v1beta/openai/v1/chat/completions")
                    .replaceAll("/v1beta/openai/chat/completions", "/v1beta/openai/v1/chat/completions")
                    .replace("GoogleApis.com", "googleapis.com");
            
            HttpRequest redirectedRequest = new CustomHttpRequest(request, URI.create(targetUri));

            // 2. Body 변조 방어 (models/ 접두사 강제 제거)
            String bodyStr = new String(body, StandardCharsets.UTF_8);
            if (bodyStr.contains("\"model\":\"models/")) {
                log.info("[CS-BOT-CONFIG] Fixing body: removing 'models/' prefix...");
                bodyStr = bodyStr.replace("\"model\":\"models/", "\"model\":\"");
                body = bodyStr.getBytes(StandardCharsets.UTF_8);
            }

            log.info("[CS-BOT-CONFIG] Final Request URL: {}", targetUri);
            return execution.execute(redirectedRequest, body);
        }
    }

    static class CustomHttpRequest implements HttpRequest {
        private final HttpRequest original;
        private final URI newUri;
        public CustomHttpRequest(HttpRequest original, URI newUri) { this.original = original; this.newUri = newUri; }
        @Override public java.net.URI getURI() { return newUri; }
        @Override public org.springframework.http.HttpMethod getMethod() { return original.getMethod(); }
        @Override public org.springframework.http.HttpHeaders getHeaders() { return original.getHeaders(); }
    }
}
