package led.mega.config;

// [REACTIVE] OncePerRequestFilter → WebFilter
//
// MVC (기존):                          WebFlux (reactive):
// OncePerRequestFilter              → WebFilter (함수형 인터페이스)
// HttpServletRequest/Response       → ServerWebExchange (요청+응답 통합)
// FilterChain.doFilter(req, res)    → WebFilterChain.filter(exchange)
// SecurityContextHolder.setAuth()   → ReactiveSecurityContextHolder.withAuthentication()
//                                     (context를 체이닝으로 주입)
// response.setStatus(401)           → exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED)
// response.getWriter().write(...)   → exchange.getResponse().writeWith(Mono.just(...))
// throws IOException/ServletException → Mono<Void> 반환 (논블로킹)

import led.mega.service.AgentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.Collections;

@Slf4j
@Component
@RequiredArgsConstructor
public class ApiKeyAuthenticationFilter implements WebFilter { // [CHANGED] OncePerRequestFilter → WebFilter

    private final AgentService agentService;
    private static final String API_KEY_HEADER = "Authorization";
    private static final String API_KEY_PREFIX = "Bearer ";

    // [CHANGED] doFilterInternal(req, res, chain) throws ... → filter(exchange, chain) : Mono<Void>
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        // [CHANGED] request.getHeader(...) → exchange.getRequest().getHeaders().getFirst(...)
        String authHeader = exchange.getRequest().getHeaders().getFirst(API_KEY_HEADER);

        if (authHeader == null || !authHeader.startsWith(API_KEY_PREFIX)) {
            return chain.filter(exchange); // API 키 없으면 다음 필터로
        }

        String apiKey = authHeader.substring(API_KEY_PREFIX.length());

        return agentService.findByApiKey(apiKey)
                .flatMap(agent -> {
                    log.debug("API 키 인증 성공: agentId={}", agent.getAgentId());
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(
                                    agent, null,
                                    Collections.singletonList(new SimpleGrantedAuthority("ROLE_AGENT")));

                    // [CHANGED] SecurityContextHolder.getContext().setAuthentication(auth)
                    //         → contextWrite(ReactiveSecurityContextHolder.withAuthentication(auth))
                    return chain.filter(exchange)
                            .contextWrite(ReactiveSecurityContextHolder.withAuthentication(authentication));
                })
                .onErrorResume(IllegalArgumentException.class, e -> {
                    log.warn("유효하지 않은 API 키");
                    // [CHANGED] response.setStatus(401), response.getWriter().write(...)
                    //         → exchange.getResponse().setStatusCode(...) + writeWith(Mono.just(...))
                    exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                    byte[] bytes = "{\"error\":\"Invalid API Key\"}".getBytes();
                    return exchange.getResponse().writeWith(
                            Mono.just(exchange.getResponse().bufferFactory().wrap(bytes)));
                });
    }
}

