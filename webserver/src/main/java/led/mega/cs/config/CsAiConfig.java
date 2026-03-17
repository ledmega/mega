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

        log.info("[CS-BOT-CONFIG] Initializing AI with Full Isolation Strategy...");
        
        // 1. 자동 설정된 빌더 대신 완전히 새로운 빌더를 생성하여 '숨겨진 인터셉터'의 간섭을 차단함
        RestClient.Builder isolatedBuilder = RestClient.builder()
                .requestInterceptor(new GeminiFinalInterceptor(apiKey));

        // 2. 가짜 URL을 전달하여 OpenAiApi 내부의 '도메인 감지(Google 감지 시 models/ 접두사 추가)' 로직을 무력화함
        OpenAiApi openAiApi = new OpenAiApi("https://Internal-Proxy.Custom", apiKey, isolatedBuilder, WebClient.builder());

        OpenAiChatOptions options = new OpenAiChatOptions();
        options.setModel("gemini-1.5-flash");
        options.setTemperature(0.7f);

        return new OpenAiChatModel(openAiApi, options);
    }

    /**
     * 모든 자동화를 거부하고 구글이 요구하는 정석 경로와 형식으로 요청을 강제 재조립하는 인터셉터
     */
    static class GeminiFinalInterceptor implements ClientHttpRequestInterceptor {
        private final String apiKey;

        public GeminiFinalInterceptor(String apiKey) {
            this.apiKey = apiKey;
        }

        @Override
        public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
            
            // 1. URL 강제 고정: 제미나이가 알려준 'openai' 경로가 없는 가장 표준적인 주소 사용
            // 쿼리 파라미터로 키를 전달하는 것이 v1beta에서 가장 오류가 적음
            String finalUrl = "https://generativelanguage.googleapis.com/v1beta/chat/completions?key=" + apiKey;
            
            HttpRequest redirectedRequest = new CustomHttpRequest(request, URI.create(finalUrl));

            // 2. 바디 정규화: Spring AI가 몰래 붙였을지 모르는 모든 'models/' 접두사 박멸
            String bodyStr = new String(body, StandardCharsets.UTF_8);
            String fixedBodyStr = bodyStr.replace("models/gemini-1.5-flash", "gemini-1.5-flash");
            
            // 3. 최종 상태 로그 (보안을 위해 Key만 가림)
            log.info("[CS-BOT-CONFIG] Final Push -> URL: {}, ModelFixed: {}", 
                    finalUrl.split("key=")[0] + "key=***", 
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
