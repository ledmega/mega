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

        log.info("[CS-BOT-CONFIG] Initializing Gemini with Constructor Fix...");
        
        // OpenAiApi 생성 시 RestClient.Builder와 WebClient.Builder를 모두 전달
        OpenAiApi openAiApi = new OpenAiApi(baseUrl, apiKey, restClientBuilder, WebClient.builder());

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
            
            // 1. URL 정규화 (v1main 에러 방지)
            String originalUri = request.getURI().toString();
            String targetUri = originalUri
                    .replaceAll("/v1/v1/", "/v1/") // 중복 v1 제거
                    .replace("GoogleApis.com", "googleapis.com"); // 도메인 정규화
            
            // 만약 v1이 아예 없다면 추가 (v1beta/openai/chat -> v1beta/openai/v1/chat)
            if (targetUri.contains("/v1beta/openai/") && !targetUri.contains("/v1beta/openai/v1/")) {
                targetUri = targetUri.replace("/v1beta/openai/", "/v1beta/openai/v1/");
            }

            HttpRequest redirectedRequest = new CustomHttpRequest(request, URI.create(targetUri));

            // 2. Body 변조 방어 (models/ 접두사 제거)
            String bodyStr = new String(body, StandardCharsets.UTF_8);
            if (bodyStr.contains("\"model\":\"models/")) {
                log.info("[CS-BOT-CONFIG] Stripping 'models/' prefix from request body");
                bodyStr = bodyStr.replace("\"model\":\"models/", "\"model\":\"");
                body = bodyStr.getBytes(StandardCharsets.UTF_8);
            }

            log.info("[CS-BOT-CONFIG] Executing request to: {}", targetUri);
            return execution.execute(redirectedRequest, body);
        }
    }

    /**
     * 안전하게 URI만 교체하기 위해 HttpRequestWrapper를 사용합니다.
     */
    static class CustomHttpRequest extends HttpRequestWrapper {
        private final URI newUri;
        public CustomHttpRequest(HttpRequest original, URI newUri) {
            super(original);
            this.newUri = newUri;
        }
        @Override
        public URI getURI() {
            return newUri;
        }
    }
}
