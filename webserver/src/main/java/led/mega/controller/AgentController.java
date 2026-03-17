package led.mega.controller;

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

    @GetMapping
    public Mono<String> agentList(Model model) {
        model.addAttribute("agents", agentService.getAllAgents());
        return Mono.just("agents/list");
    }

    @GetMapping("/{id}")
    public Mono<String> agentDetail(@PathVariable String id, Model model) {
        return agentService.getAgent(id)
                .flatMap(agent -> {
                    model.addAttribute("agent", agent);
                    model.addAttribute("tasks", taskService.getTasksByAgentId(id));
                    return Mono.just("agents/detail");
                })
                .onErrorReturn("redirect:/agents");
    }
}
