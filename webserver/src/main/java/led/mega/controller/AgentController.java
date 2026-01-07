package led.mega.controller;

import led.mega.dto.AgentResponseDto;
import led.mega.dto.TaskResponseDto;
import led.mega.service.AgentService;
import led.mega.service.TaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@Controller
@RequestMapping("/agents")
@RequiredArgsConstructor
public class AgentController {

    private final AgentService agentService;
    private final TaskService taskService;

    /**
     * 에이전트 목록 페이지
     */
    @GetMapping
    public String agentList(Model model) {
        List<AgentResponseDto> agents = agentService.getAllAgents();
        model.addAttribute("agents", agents);
        return "agents/list";
    }

    /**
     * 에이전트 상세 페이지
     */
    @GetMapping("/{id}")
    public String agentDetail(@PathVariable Long id, Model model) {
        try {
            AgentResponseDto agent = agentService.getAgent(id);
            List<TaskResponseDto> tasks = taskService.getTasksByAgentId(id);
            
            model.addAttribute("agent", agent);
            model.addAttribute("tasks", tasks);
            return "agents/detail";
        } catch (IllegalArgumentException e) {
            return "redirect:/agents";
        }
    }
}

