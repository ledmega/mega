package led.mega.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import led.mega.entity.Agent;
import led.mega.service.AgentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Slf4j
@Component
@RequiredArgsConstructor
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {

    private final AgentService agentService;
    private static final String API_KEY_HEADER = "Authorization";
    private static final String API_KEY_PREFIX = "Bearer ";

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, 
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        
        String authHeader = request.getHeader(API_KEY_HEADER);
        
        if (authHeader != null && authHeader.startsWith(API_KEY_PREFIX)) {
            String apiKey = authHeader.substring(API_KEY_PREFIX.length());
            
            try {
                Agent agent = agentService.findByApiKey(apiKey);
                
                // API 키로 인증된 에이전트를 SecurityContext에 설정
                UsernamePasswordAuthenticationToken authentication = 
                    new UsernamePasswordAuthenticationToken(
                        agent, 
                        null, 
                        Collections.singletonList(new SimpleGrantedAuthority("ROLE_AGENT"))
                    );
                
                SecurityContextHolder.getContext().setAuthentication(authentication);
                log.debug("API 키 인증 성공: agentId={}", agent.getAgentId());
                
            } catch (IllegalArgumentException e) {
                log.warn("유효하지 않은 API 키: {}", apiKey);
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("{\"error\":\"Invalid API Key\"}");
                return;
            }
        }
        
        filterChain.doFilter(request, response);
    }
}

