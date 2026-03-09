package led.mega.config;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@ControllerAdvice
@RequiredArgsConstructor
public class GlobalControllerAdvice {

    private final led.mega.service.MenuService menuService;

    @ModelAttribute("isAdmin")
    public Mono<Boolean> isAdmin(Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) return Mono.just(false);
        boolean admin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equalsIgnoreCase("ROLE_ADMIN"));
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

    @ModelAttribute("menus")
    public Flux<led.mega.entity.Menu> menus(Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) return Flux.empty();
        
        boolean admin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equalsIgnoreCase("ROLE_ADMIN"));
        
        return menuService.getEnabledMenus()
                .filter(menu -> {
                    if (menu.getRequiredRole() == null || menu.getRequiredRole().isBlank()) return true;
                    if (admin) return true;
                    return auth.getAuthorities().stream()
                            .anyMatch(a -> a.getAuthority().equalsIgnoreCase(menu.getRequiredRole()));
                });
    }
}
