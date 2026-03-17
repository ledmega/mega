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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * Spring AI ChatClient 빈 설정.
 */
@Configuration
public class CsAiConfig {
    private static final Logger log = LoggerFactory.getLogger(CsAiConfig.class);

    @Bean
    @Primary
    public OpenAiChatModel openAiChatModel(
            @Value("${spring.ai.openai.api-key}") String apiKey,
            @Value("${spring.ai.openai.chat.options.model}") String modelName) {

        log.info("==========================================================");
        log.info("[CS-BOT-CONFIG] Gemini Ultimate Interceptor Initializing...");
        log.info("[CS-BOT-CONFIG] Target Model: {}", modelName);
        log.info("==========================================================");

        // 1. Spring AI를 완전히 속이기 위한 가짜 내부 도메인
        String dummyBaseUrl = "https://internal-proxy.local/v1beta/openai";
        String realHost = "generativelanguage.googleapis.com";

        // 2. 도메인 스왑 인터셉터
        RestClient.Builder restClientBuilder = RestClient.builder()
                .requestInterceptor(new ClientHttpRequestInterceptor() {
                    @Override
                    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
                        URI uri = request.getURI();
                        if (uri.getHost().equals("internal-proxy.local")) {
                            try {
                                URI newUri = new URI(
                                        uri.getScheme(),
                                        uri.getUserInfo(),
                                        realHost,
                                        uri.getPort(),
                                        uri.getPath(),
                                        uri.getQuery(),
                                        uri.getFragment()
                                );
                                // HttpRequestWrapper를 사용하여 URI만 교체
                                request = new HttpRequestWrapper(request) {
                                    @Override
                                    public URI getURI() {
                                        return newUri;
                                    }
                                };
                            } catch (URISyntaxException e) {
                                throw new IOException("URI Swap Failed", e);
                            }
                        }
                        return execution.execute(request, body);
                    }
                });

        // 3. OpenAiApi 생성
        OpenAiApi openAiApi = new OpenAiApi(dummyBaseUrl, apiKey, restClientBuilder);

        // 4. ChatOptions 설정 (접두사 'models/' 강제 추가를 피하기 위해 가짜 도메인 사용 상태임)
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
