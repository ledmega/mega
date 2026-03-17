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

        log.info("[CS-BOT-CONFIG] Initializing Gemini with Extreme Fixes...");
        
        // OpenAiApi 생성 시 RestClient.Builder 전달
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
            
            // 1. URL 수술 (v1main 에러 방지)
            String originalUri = request.getURI().toString();
            // Google-Apis -> googleapis.com 으로 정규화
            String targetUri = originalUri
                    .replace("Google-Apis.com", "googleapis.com")
                    .replaceAll("/v1/v1/", "/v1/"); // 중복 v1 방어
            
            // v1이 누락된 경우 강제로 추가 (구글 OpenAI 호환 레이어 필수 경로)
            if (!targetUri.contains("/v1/chat/completions")) {
                targetUri = targetUri.replace("/chat/completions", "/v1/chat/completions");
            }

            HttpRequest redirectedRequest = new CustomHttpRequest(request, URI.create(targetUri));

            // 2. Body 정밀 수술 (어떤 접두사가 붙어있든 강제로 떼어냄)
            String bodyStr = new String(body, StandardCharsets.UTF_8);
            if (bodyStr.contains("\"model\":\"")) {
                // 모델명 부분을 정규식으로 찾아서 gemini-1.5-flash 로 고정
                // 예: "model":"models/gemini-1.5-flash" -> "model":"gemini-1.5-flash"
                String newBodyStr = bodyStr.replaceAll("\"model\":\"[^\"]*gemini-1.5-flash[^\"]*\"", "\"model\":\"gemini-1.5-flash\"");
                
                if (!bodyStr.equals(newBodyStr)) {
                    log.info("[CS-BOT-CONFIG] Correcting model name in request body...");
                    bodyStr = newBodyStr;
                    body = bodyStr.getBytes(StandardCharsets.UTF_8);
                }
            }

            log.info("[CS-BOT-CONFIG] Request Final State: URL={}, BodyLen={}", targetUri, body.length);
            return execution.execute(redirectedRequest, body);
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
