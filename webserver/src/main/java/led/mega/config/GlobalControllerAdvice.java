package led.mega.config;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import reactor.core.publisher.Mono;

@ControllerAdvice
public class GlobalControllerAdvice {

    @ModelAttribute("isAdmin")
    public Mono<Boolean> isAdmin(Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) return Mono.just(false);
        
        // [OVERRIDE] ledmega@gmail.com 계정은 무조건 관리자 권한 부여
        if ("ledmega@gmail.com".equalsIgnoreCase(auth.getName())) {
            return Mono.just(true);
        }

        boolean admin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equalsIgnoreCase("ROLE_ADMIN") 
                            || a.getAuthority().equalsIgnoreCase("ADMIN"));
        return Mono.just(admin);
    }

    @ModelAttribute("isLoggedIn")
    public Mono<Boolean> isLoggedIn(Authentication auth) {
        return Mono.just(auth != null && auth.isAuthenticated());
    }

    @ModelAttribute("username")
    public Mono<String> username(Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) return Mono.just("");
        return Mono.just(auth.getName());
    }
}
