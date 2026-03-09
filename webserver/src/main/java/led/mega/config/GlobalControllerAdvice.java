package led.mega.config;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import reactor.core.publisher.Mono;

@ControllerAdvice
public class GlobalControllerAdvice {

    @ModelAttribute("isAdmin")
    public Mono<Boolean> isAdmin(Mono<Authentication> authMono) {
        return authMono
                .map(auth -> auth.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN")))
                .defaultIfEmpty(false);
    }

    @ModelAttribute("isLoggedIn")
    public Mono<Boolean> isLoggedIn(Mono<Authentication> authMono) {
        return authMono
                .map(Authentication::isAuthenticated)
                .defaultIfEmpty(false);
    }
}
