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

        log.info("[CS-BOT-CONFIG] Initializing AI with Deep Inspection...");
        
        OpenAiApi openAiApi = new OpenAiApi(baseUrl, apiKey, restClientBuilder, WebClient.builder());

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
            
            // 1. 바디 실체 확인 (로그 출력)
            String bodyStr = new String(body, StandardCharsets.UTF_8);
            log.info("[CS-BOT-CONFIG] RAW BODY: {}", bodyStr);

            // 2. URL 수술 (이미지 참조: /v1 제거하고 API KEY 쿼리 파라미터로 강제 삽입)
            String targetUri = request.getURI().toString()
                    .replace("/v1/chat/completions", "/chat/completions")
                    .replace("Google-Apis.com", "googleapis.com");
            
            // API 키가 URL에 없으면 추가 (구글 v1beta 권장 방식)
            if (!targetUri.contains("key=")) {
                targetUri += (targetUri.contains("?") ? "&" : "?") + "key=" + apiKey;
            }
            
            HttpRequest redirectedRequest = new CustomHttpRequest(request, URI.create(targetUri));

            // 3. 모델명 강제 교정 (혹시라도 models/ 가 있으면 제거)
            String fixedBodyStr = bodyStr.replace("models/gemini-1.5-flash", "gemini-1.5-flash");
            if (!bodyStr.equals(fixedBodyStr)) {
                log.info("[CS-BOT-CONFIG] Found models/ prefix in body, stripping it...");
                body = fixedBodyStr.getBytes(StandardCharsets.UTF_8);
            }

            log.info("[CS-BOT-CONFIG] Final Request URL (with Key): {}", targetUri.split("key=")[0] + "key=***");
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
