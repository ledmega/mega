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
 * Spring AI의 강제 모델명 변조 및 URL 오류를 최종 단계에서 강제 교정합니다.
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

        log.info("[CS-BOT-CONFIG] Gemini Last-mile Guard Interceptor Initializing...");

        // 1. 가짜 도메인 (Spring AI의 자동 감지를 피함)
        String dummyBaseUrl = "https://gemini-proxy.internal/v1beta/openai";
        String realBaseUrl = "https://generativelanguage.googleapis.com/v1beta/openai/chat/completions";

        // 2. 최종 병기 인터셉터: URL과 JSON 바디를 직접 수정
        RestClient.Builder restClientBuilder = RestClient.builder()
                .requestInterceptor(new ClientHttpRequestInterceptor() {
                    @Override
                    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
                        
                        // ✅ URL 보정: Spring AI가 만든 엉뚱한 URL을 버리고 우리가 원하는 정확한 URL로 교체
                        HttpRequest secureRequest = new HttpRequestWrapper(request) {
                            @Override
                            public URI getURI() {
                                return URI.create(realBaseUrl);
                            }
                        };

                        // ✅ 바디 보정: JSON 내의 "models/" 접두사를 강제로 삭제
                        String jsonBody = new String(body, StandardCharsets.UTF_8);
                        if (jsonBody.contains("models/")) {
                            String fixedBody = jsonBody.replace("models/", "");
                            log.debug("[CS-BOT-CONFIG] Stripped 'models/' from JSON body");
                            body = fixedBody.getBytes(StandardCharsets.UTF_8);
                        }

                        log.info("[CS-BOT-CONFIG] Sending request to: {}", realBaseUrl);
                        return execution.execute(secureRequest, body);
                    }
                });

        // 3. OpenAiApi 생성 (가짜 도메인 전달)
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
