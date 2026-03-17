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

    /**
     * Spring AI 자동 설정 및 기타 컴포넌트가 요구하는 RestClient.Builder 빈을 제공합니다.
     */
    @Bean
    public RestClient.Builder restClientBuilder() {
        return RestClient.builder();
    }

    @Bean
    @Primary
    public OpenAiChatModel openAiChatModel(
            @Value("${spring.ai.openai.api-key}") String apiKey) {

        log.info("[CS-BOT-CONFIG] Initializing AI with Full Isolation & Context Support...");
        
        // 격리된 전략을 위해 'new' 빌더를 사용 (컨텍스트 빈과 분리)
        RestClient.Builder isolatedBuilder = RestClient.builder()
                .requestInterceptor(new GeminiFinalInterceptor(apiKey));

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
            
            // 제미나이 가이드에 따른 정석 경로
            String finalUrl = "https://generativelanguage.googleapis.com/v1beta/chat/completions?key=" + apiKey;
            
            HttpRequest redirectedRequest = new CustomHttpRequest(request, URI.create(finalUrl));

            String bodyStr = new String(body, StandardCharsets.UTF_8);
            String fixedBodyStr = bodyStr.replace("models/gemini-1.5-flash", "gemini-1.5-flash");
            
            log.info("[CS-BOT-CONFIG] Sending to: {}, BodyFixed: {}", 
                    "https://generativelanguage.googleapis.com/v1beta/chat/completions?key=***", 
                    !bodyStr.equals(fixedBodyStr));

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
