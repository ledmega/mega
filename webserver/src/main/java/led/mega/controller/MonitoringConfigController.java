package led.mega.controller;

import led.mega.service.AgentService;
import led.mega.service.MonitoringConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

/** 서비스 관리 Thymeleaf 화면 Controller */
@Slf4j
@Controller
@RequestMapping("/services")
@RequiredArgsConstructor
public class MonitoringConfigController {

    private final MonitoringConfigService configService;
    private final AgentService agentService;

    /** 서비스 관리 목록 화면 */
    @GetMapping
    public Mono<String> list(Model model, Authentication auth) {
        boolean isAdmin = auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        model.addAttribute("configs", configService.getAll());   // Flux — Thymeleaf가 자동 subscribe
        model.addAttribute("agents",  agentService.getAllAgents()); // 등록 폼 셀렉트박스용
        model.addAttribute("isLoggedIn", auth != null && auth.isAuthenticated());
        model.addAttribute("isAdmin", isAdmin);
        model.addAttribute("username", auth != null ? auth.getName() : "");
        return Mono.just("services/list");
    }
}
