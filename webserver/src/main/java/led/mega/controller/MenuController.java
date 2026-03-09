package led.mega.controller;

import led.mega.entity.Menu;
import led.mega.service.MenuService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@Controller
@RequestMapping("/menu")
@RequiredArgsConstructor
public class MenuController {

    private final MenuService menuService;

    @GetMapping
    public Mono<String> list(Model model, Authentication auth) {
        if (!isAdmin(auth)) return Mono.just("redirect:/dashboard");
        
        model.addAttribute("menuList", menuService.getAllMenus());
        return Mono.just("menu/list");
    }

    @PostMapping("/add")
    public Mono<String> add(@ModelAttribute Menu menu, Authentication auth) {
        if (!isAdmin(auth)) return Mono.just("redirect:/dashboard");
        
        return menuService.saveMenu(menu)
                .thenReturn("redirect:/menu");
    }

    @PostMapping("/delete/{id}")
    public Mono<String> delete(@PathVariable Long id, Authentication auth) {
        if (!isAdmin(auth)) return Mono.just("redirect:/dashboard");
        
        return menuService.deleteMenu(id)
                .thenReturn("redirect:/menu");
    }

    private boolean isAdmin(Authentication auth) {
        if (auth == null) return false;
        return auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equalsIgnoreCase("ROLE_ADMIN"));
    }
}
