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

        log.info("[CS-BOT-CONFIG] Applying settings based on user provided reference...");
        
        OpenAiApi openAiApi = new OpenAiApi(baseUrl, apiKey, restClientBuilder, WebClient.builder());

        OpenAiChatOptions options = new OpenAiChatOptions();
        options.setModel("gemini-1.5-flash"); // 순수 모델명만 지정
        options.setTemperature(0.7f);

        return new OpenAiChatModel(openAiApi, options);
    }

    /**
     * 이미지 참조에 따라 /v1/을 제거하고 모델명을 정규화하는 인터셉터
     */
    static class GeminiCompatibilityInterceptor implements ClientHttpRequestInterceptor {
        @Override
        public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
            
            // 1. URL 수술: 이미지 설명에 따라 /v1/ 을 제거함
            String targetUri = request.getURI().toString()
                    .replace("/v1/chat/completions", "/chat/completions") // /v1/ 제거
                    .replace("Google-Apis.com", "googleapis.com");       // 도메인 복구
            
            HttpRequest redirectedRequest = new CustomHttpRequest(request, URI.create(targetUri));

            // 2. Body 수술: 모델명에 어떤 접두사가 붙어있든 강제로 떼어냄
            String bodyStr = new String(body, StandardCharsets.UTF_8);
            
            // "model":"ANYTHING/gemini-1.5-flash" -> "model":"gemini-1.5-flash"
            String fixedBodyStr = bodyStr.replaceAll("\"model\"\\s*:\\s*\"[^\"]*gemini-1.5-flash[^\"]*\"", "\"model\":\"gemini-1.5-flash\"");
            
            if (!bodyStr.equals(fixedBodyStr)) {
                log.info("[CS-BOT-CONFIG] Stripped potential prefixes from model name in body");
                body = fixedBodyStr.getBytes(StandardCharsets.UTF_8);
            }

            log.info("[CS-BOT-CONFIG] Corrected URL (removed /v1/): {}", targetUri);
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
