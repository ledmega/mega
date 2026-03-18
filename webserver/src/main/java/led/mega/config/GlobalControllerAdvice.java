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
        // [CHANGED] 비로그인 사용자도 권한이 필요 없는 메뉴는 보이도록 수정
        boolean isLoggedIn = auth != null && auth.isAuthenticated();
        boolean admin = isLoggedIn && auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equalsIgnoreCase("ROLE_ADMIN"));
        
        return menuService.getEnabledMenus()
                .filter(menu -> {
                    // 권한 제한이 없는 메뉴는 누구나 볼 수 있음
                    if (menu.getRequiredRole() == null || menu.getRequiredRole().isBlank()) return true;
                    
                    // 권한 제한이 있는 메뉴는 로그인 상태여야 함
                    if (!isLoggedIn) return false;
                    
                    // 관리자이거나 필요한 권한을 가진 경우
                    if (admin) return true;
                    return auth.getAuthorities().stream()
                            .anyMatch(a -> a.getAuthority().equalsIgnoreCase(menu.getRequiredRole()));
                });
    }
}
