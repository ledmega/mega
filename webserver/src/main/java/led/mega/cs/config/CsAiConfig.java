package led.mega.cs.config;

import org.springframework.ai.chat.client.ChatClient;
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

/**
 * Spring AI ChatClient 빈 설정.
 * Spring AI의 자동 변조 로직을 완벽하게 차단하기 위한 최종 인터셉터 설정입니다.
 */
@Configuration
public class CsAiConfig {
    private static final Logger log = LoggerFactory.getLogger(CsAiConfig.class);

    @Bean
    @Primary
    public OpenAiChatModel openAiChatModel(
            @Value("${spring.ai.openai.api-key}") String apiKey,
            @Value("${spring.ai.openai.chat.options.model}") String modelName,
            WebClient.Builder webClientBuilder) {

        log.info("[CS-BOT-CONFIG] Initializing Gemini Last-mile Guard...");

        // 1. Spring AI가 구글임을 눈치채지 못하게 하는 도메인 (OpenAI인 척 함)
        String dummyBaseUrl = "https://api.openai.com/v1"; 
        // 실제 구글 엔드포인트
        String realTargetUrl = "https://generativelanguage.googleapis.com/v1beta/openai/chat/completions";

        // 2. 인터셉터: 나가는 모든 요청을 감시하고 구글에 맞게 강제 변환
        RestClient.Builder restClientBuilder = RestClient.builder()
                .requestInterceptor(new ClientHttpRequestInterceptor() {
                    @Override
                    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
                        
                        // 바디 확인 및 'models/' 강제 제거
                        String originalBody = new String(body, StandardCharsets.UTF_8);
                        String fixedBody = originalBody;
                        if (originalBody.contains("models/")) {
                            fixedBody = originalBody.replace("models/", "");
                            log.info("[CS-BOT-CONFIG] Found and removed 'models/' prefix from request body");
                        }

                        // 로그 출력 (디버깅용)
                        log.info("[CS-BOT-CONFIG] Intercepting request: {} -> {}", request.getURI(), realTargetUrl);
                        log.info("[CS-BOT-CONFIG] Request Body (cleansed): {}", fixedBody);

                        // URL 및 바디 교체
                        HttpRequest secureRequest = new HttpRequestWrapper(request) {
                            @Override
                            public URI getURI() {
                                return URI.create(realTargetUrl);
                            }
                        };
                        
                        return execution.execute(secureRequest, fixedBody.getBytes(StandardCharsets.UTF_8));
                    }
                });

        // 3. OpenAiApi 생성 (RestClient.Builder 주입)
        OpenAiApi openAiApi = new OpenAiApi(dummyBaseUrl, apiKey, restClientBuilder, webClientBuilder);

        // 4. ChatOptions 설정
        OpenAiChatOptions options = new OpenAiChatOptions();
        options.setModel(modelName); 
        options.setTemperature(0.7);

        return new OpenAiChatModel(openAiApi, options);
    }

    @Bean
    public ChatClient chatClient(OpenAiChatModel openAiChatModel) {
        return ChatClient.builder(openAiChatModel).build();
    }
}
