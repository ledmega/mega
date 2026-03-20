package led.mega.controller;

import led.mega.service.AgentService;
import led.mega.service.OsMonitoringConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import reactor.core.publisher.Mono;

/** OS 모니터링 설정 Thymeleaf 화면 Controller */
@Slf4j
@Controller
@RequestMapping("/os-configs")
@RequiredArgsConstructor
public class OsMonitoringConfigController {

    private final OsMonitoringConfigService osMonitoringConfigService;
    private final AgentService agentService;

    /** OS 모니터링 설정 관리 화면 */
    @GetMapping
    public Mono<String> list(Model model, Authentication auth) {
        boolean isAdmin = auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        model.addAttribute("commonConfigs", osMonitoringConfigService.getCommonConfigs());
        model.addAttribute("agents", agentService.getAllAgents());
        model.addAttribute("isLoggedIn", auth != null && auth.isAuthenticated());
        model.addAttribute("isAdmin", isAdmin);
        model.addAttribute("username", auth != null ? auth.getName() : "");
        
        return Mono.just("os-config/list");
    }
}
