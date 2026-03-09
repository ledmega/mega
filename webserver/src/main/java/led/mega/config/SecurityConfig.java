package led.mega.config;

// [REACTIVE] SecurityConfig 전환 핵심 요약
//
// MVC (기존):                          WebFlux (reactive):
// @EnableWebSecurity                → @EnableWebFluxSecurity
// HttpSecurity                      → ServerHttpSecurity
// SecurityFilterChain               → SecurityWebFilterChain
// authorizeHttpRequests             → authorizeExchange
// .requestMatchers(...)             → .pathMatchers(...)
// .anyRequest()                     → .anyExchange()
// AuthenticationManager             → ReactiveAuthenticationManager
// UserDetailsService                → ReactiveUserDetailsService (CustomUserDetailsService)
// OncePerRequestFilter              → WebFilter (ApiKeyAuthenticationFilter)
// UsernamePasswordAuthenticationFilter → SecurityWebFiltersOrder.AUTHENTICATION
// .addFilterBefore(filter, Clazz)   → .addFilterBefore(filter, SecurityWebFiltersOrder.X)
// RedirectServerAuthenticationSuccessHandler (ServerHttpSecurity formLogin용)

import led.mega.service.CustomUserDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UserDetailsRepositoryReactiveAuthenticationManager;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authentication.RedirectServerAuthenticationFailureHandler;
import org.springframework.security.web.server.authentication.RedirectServerAuthenticationSuccessHandler;
import org.springframework.security.web.server.authentication.logout.RedirectServerLogoutSuccessHandler;
import org.springframework.security.web.server.util.matcher.NegatedServerWebExchangeMatcher;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatchers;
import org.springframework.security.web.server.authentication.RedirectServerAuthenticationEntryPoint;
import org.springframework.http.HttpStatus;
import java.net.URI;

@Configuration
@EnableWebFluxSecurity  // [CHANGED] @EnableWebSecurity → @EnableWebFluxSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final ApiKeyAuthenticationFilter apiKeyAuthenticationFilter;
    private final CustomUserDetailsService customUserDetailsService;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // [CHANGED] AuthenticationManager → ReactiveAuthenticationManager
    // UserDetailsRepositoryReactiveAuthenticationManager: ReactiveUserDetailsService + PasswordEncoder 조합
    @Bean
    public ReactiveAuthenticationManager reactiveAuthenticationManager() {
        UserDetailsRepositoryReactiveAuthenticationManager manager =
                new UserDetailsRepositoryReactiveAuthenticationManager(customUserDetailsService);
        manager.setPasswordEncoder(passwordEncoder());
        return manager;
    }

    // [CHANGED] SecurityFilterChain → SecurityWebFilterChain
    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        RedirectServerLogoutSuccessHandler logoutSuccessHandler = new RedirectServerLogoutSuccessHandler();
        logoutSuccessHandler.setLogoutSuccessUrl(URI.create("/"));

        return http
            .csrf(csrf -> csrf.disable())  // [DEBUG] CSRF 잠시 완전 비활성화
            .authorizeExchange(auth -> auth
                .pathMatchers("/", "/public/**", "/css/**", "/js/**", "/images/**",
                        "/favicon.ico", "/signup", "/login", "/dashboard", "/agents", "/error").permitAll()
                .pathMatchers("/api/agents/register").permitAll()
                .pathMatchers("/api/**").authenticated()
                .anyExchange().authenticated()
            )
            // [CHANGED] .addFilterBefore(filter, UsernamePasswordAuthenticationFilter.class)
            //         → .addFilterBefore(filter, SecurityWebFiltersOrder.AUTHENTICATION)
            .addFilterBefore(apiKeyAuthenticationFilter, SecurityWebFiltersOrder.AUTHENTICATION)
            .exceptionHandling(exceptionHandling -> exceptionHandling
                .authenticationEntryPoint((exchange, e) -> {
                    if (exchange.getRequest().getPath().value().startsWith("/api/")) {
                        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                        return exchange.getResponse().setComplete();
                    }
                    return new RedirectServerAuthenticationEntryPoint("/login").commence(exchange, e);
                })
            )
            // [CHANGED] .formLogin(form -> form.loginPage(...).defaultSuccessUrl(...).failureUrl(...))
            //         → ServerHttpSecurity.formLogin + RedirectServerAuthenticationSuccessHandler
            .formLogin(form -> form
                .loginPage("/login")
                .authenticationManager(reactiveAuthenticationManager())
                .authenticationSuccessHandler(new RedirectServerAuthenticationSuccessHandler("/dashboard"))
                .authenticationFailureHandler(new RedirectServerAuthenticationFailureHandler("/login?error=true"))
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessHandler(logoutSuccessHandler)
            )
            .build();
    }
}

