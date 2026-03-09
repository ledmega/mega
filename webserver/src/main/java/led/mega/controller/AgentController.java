package led.mega.controller;

// [REACTIVE] Thymeleaf Controller 전환
//
// MVC (기존):                          WebFlux (reactive):
// String 반환 (동기)                 → Mono<String> 반환 (비동기)
// model.addAttribute("x", list)     → model.addAttribute("x", flux)
//                                     (Thymeleaf-WebFlux가 Flux를 자동 subscribe)
// try-catch return redirect          → .onErrorReturn("redirect:...")
// service.getX() [블로킹]            → service.getX() [Mono/Flux] + flatMap

import led.mega.service.AgentService;
import led.mega.service.TaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@Slf4j
@Controller
@RequestMapping("/agents")
@RequiredArgsConstructor
public class AgentController {

    private final AgentService agentService;
    private final TaskService taskService;

    // [CHANGED] String → Mono<String>
    // [CHANGED] model.addAttribute("agents", list) → model.addAttribute("agents", flux)
    // Thymeleaf-WebFlux: 모델에 Flux를 넣으면 렌더링 시 자동 subscribe
    @GetMapping
    public Mono<String> agentList(Model model) {
        model.addAttribute("agents", agentService.getAllAgents()); // Flux<AgentResponseDto>
        return Mono.just("agents/list");
    }

    // [CHANGED] try-catch return "redirect" → Mono 체이닝 + .onErrorReturn("redirect")
    @GetMapping("/{id}")
    public Mono<String> agentDetail(@PathVariable Long id, Model model) {
        return agentService.getAgent(id)
                .flatMap(agent -> {
                    model.addAttribute("agent", agent);
                    model.addAttribute("tasks", taskService.getTasksByAgentId(id)); // Flux
                    return Mono.just("agents/detail");
                })
                .onErrorReturn("redirect:/agents");
    }
}

