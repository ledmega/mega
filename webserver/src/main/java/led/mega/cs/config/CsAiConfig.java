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
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * Spring AI ChatClient 빈 설정.
 * Spring AI의 자동 모델명 변조(models/ 접두사 추가) 로직을 우회하기 위해
 * 가짜 도메인으로 초기화한 후 인터셉터에서 실제 도메인으로 교체합니다.
 */
@Configuration
public class CsAiConfig {

    @Bean
    @Primary
    public OpenAiChatModel openAiChatModel(
            @Value("${spring.ai.openai.api-key}") String apiKey,
            @Value("${spring.ai.openai.chat.options.model}") String modelName) {

        // 1. Spring AI를 속이기 위한 가짜 베이스 URL
        // googleapis.com을 포함하지 않아야 모델명 앞에 'models/'가 붙지 않습니다.
        String dummyBaseUrl = "https://gemini-proxy.internal/v1beta/openai";
        String realHost = "generativelanguage.googleapis.com";

        // 2. 가짜 URL을 실제 URL로 바꿔주는 인터셉터 설정
        RestClient.Builder restClientBuilder = RestClient.builder()
                .requestInterceptor(new ClientHttpRequestInterceptor() {
                    @Override
                    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
                        URI uri = request.getURI();
                        if (uri.getHost().equals("gemini-proxy.internal")) {
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
                                request = new CustomHttpRequest(request, newUri);
                            } catch (URISyntaxException e) {
                                throw new IOException(e);
                            }
                        }
                        return execution.execute(request, body);
                    }
                });

        // 3. OpenAiApi 생성 (커스텀 RestClient.Builder 사용)
        OpenAiApi openAiApi = new OpenAiApi(dummyBaseUrl, apiKey, restClientBuilder);

        // 4. ChatOptions 설정
        OpenAiChatOptions options = new OpenAiChatOptions();
        options.setModel(modelName); // gemini-1.5-flash (접두사 없음!)
        options.setTemperature(0.7);

        // 5. ChatModel 생성
        return new OpenAiChatModel(openAiApi, options);
    }

    @Bean
    public ChatClient chatClient(OpenAiChatModel openAiChatModel) {
        return ChatClient.builder(openAiChatModel).build();
    }

    /**
     * URI를 변경하기 위한 래퍼 클래스
     */
    private static class CustomHttpRequest implements HttpRequest {
        private final HttpRequest delegate;
        private final URI uri;

        public CustomHttpRequest(HttpRequest delegate, URI uri) {
            this.delegate = delegate;
            this.uri = uri;
        }

        @Override
        public String getMethodValue() { return delegate.getMethodValue(); }
        @Override
        public URI getURI() { return uri; }
        @Override
        public org.springframework.http.HttpHeaders getHeaders() { return delegate.getHeaders(); }
    }
}
