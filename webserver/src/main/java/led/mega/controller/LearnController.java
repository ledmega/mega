package led.mega.controller;

import led.mega.service.LearnService;
import led.mega.service.MenuService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import reactor.core.publisher.Mono;

@Controller
@RequestMapping("/learn")
@RequiredArgsConstructor
public class LearnController {

    private final LearnService learnService;
    private final MenuService menuService;

    @GetMapping
    public Mono<String> list(@RequestParam(required = false) String keyword, Model model, Authentication auth) {
        model.addAttribute("isLoggedIn", auth != null && auth.isAuthenticated());
        model.addAttribute("username", auth != null ? auth.getName() : "");
        model.addAttribute("keyword", keyword);

        return menuService.getEnabledMenus()
                .collectList()
                .doOnNext(menus -> model.addAttribute("menus", menus))
                .then(
                    (keyword == null || keyword.isEmpty())
                        ? learnService.getReactiveLearningContents().collectList()
                        : learnService.searchReactiveLearning(keyword).collectList()
                )
                .doOnNext(list -> model.addAttribute("learnings", list))
                .thenReturn("learn/list");
    }
}
