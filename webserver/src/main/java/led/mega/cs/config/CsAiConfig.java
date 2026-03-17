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
    @Primary
    public OpenAiChatModel openAiChatModel(
            @Value("${spring.ai.openai.api-key}") String apiKey) {

        log.info("[CS-BOT-CONFIG] Applying Gemini Advisor's 'Method B' Strategy...");
        
        // 1. 격리된 빌더와 인터셉터 설정
        RestClient.Builder isolatedBuilder = RestClient.builder()
                .requestInterceptor(new GeminiFinalInterceptor(apiKey));

        // 2. 가짜 주소를 사용하여 Spring AI의 자동 모델명 접두사(models/) 추가 로직을 차단함
        // 이 주소는 인터셉터에서 제미나이가 알려준 진짜 주소로 교체됩니다.
        OpenAiApi openAiApi = new OpenAiApi("https://Internal-Proxy.Custom", apiKey, isolatedBuilder, WebClient.builder());

        OpenAiChatOptions options = new OpenAiChatOptions();
        options.setModel("gemini-1.5-flash");
        options.setTemperature(0.7f);

        return new OpenAiChatModel(openAiApi, options);
    }

    static class GeminiFinalInterceptor implements ClientHttpRequestInterceptor {
        private final String apiKey;

        public GeminiFinalInterceptor(String apiKey) {
            this.apiKey = apiKey;
        }

        @Override
        public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
            
            // 3. 제미나이 답변에 따른 최적의 URL 구성
            // /openai 경로나 중복된 v1을 제거하고 구글이 모델을 찾을 수 있는 정석 경로로 유도함
            String finalUrl = "https://generativelanguage.googleapis.com/v1beta/chat/completions?key=" + apiKey;
            
            HttpRequest redirectedRequest = new CustomHttpRequest(request, URI.create(finalUrl));

            // 4. 모델명 강제 정규화 (models/ 접두사 제거)
            String bodyStr = new String(body, StandardCharsets.UTF_8);
            String fixedBodyStr = bodyStr.replace("models/gemini-1.5-flash", "gemini-1.5-flash");
            
            log.info("[CS-BOT-CONFIG] Redirecting to Gemini Advisor's Fixed URL: {}", finalUrl.split("key=")[0] + "key=***");
            if (!bodyStr.equals(fixedBodyStr)) {
                log.info("[CS-BOT-CONFIG] Successfully stripped 'models/' prefix from body.");
            }

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
