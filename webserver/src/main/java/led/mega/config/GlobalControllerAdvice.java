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
        return Mono.just(auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")));
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
