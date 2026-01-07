package led.mega.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final ApiKeyAuthenticationFilter apiKeyAuthenticationFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf
                .ignoringRequestMatchers("/api/**")  // API는 CSRF 비활성화
            )
            .authorizeHttpRequests(auth -> auth
                // 공개 접근 허용 경로
                .requestMatchers("/", "/public/**", "/css/**", "/js/**", "/images/**", "/favicon.ico", 
                               "/signup", "/login", "/dashboard", "/agents").permitAll()
                // API 에이전트 등록은 공개
                .requestMatchers("/api/agents/register").permitAll()
                // 나머지 API는 API 키 인증 필요
                .requestMatchers("/api/**").authenticated()
                // 나머지 모든 요청은 웹 로그인 필요
                .anyRequest().authenticated()
            )
            .addFilterBefore(apiKeyAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/login")  // 로그인 처리 URL
                .usernameParameter("username")  // 로그인 폼의 사용자명 필드명 (이메일)
                .passwordParameter("password")   // 로그인 폼의 비밀번호 필드명
                .defaultSuccessUrl("/dashboard", true)  // 로그인 성공 시 대시보드로 이동
                .failureUrl("/login?error=true")  // 로그인 실패 시
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/")
                .permitAll()
            );
        
        return http.build();
    }
}

