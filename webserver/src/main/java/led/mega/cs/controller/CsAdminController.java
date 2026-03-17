package led.mega.cs.controller;

import led.mega.service.MenuService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import reactor.core.publisher.Mono;

/**
 * CS 어드민 대시보드 페이지 컨트롤러.
 * /cs/dashboard -> templates/cs/dashboard.html 렌더링
 */
@Controller
@RequestMapping("/cs")
@RequiredArgsConstructor
public class CsAdminController {

    private final MenuService menuService;

    @GetMapping("/dashboard")
    public Mono<String> csDashboard(Model model, Authentication auth) {
        model.addAttribute("isLoggedIn", auth != null && auth.isAuthenticated());
        model.addAttribute("username", auth != null ? auth.getName() : "");
        return menuService.getEnabledMenus()
                .collectList()
                .doOnNext(menus -> model.addAttribute("menus", menus))
                .thenReturn("cs/dashboard");
    }
}
