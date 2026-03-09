package led.mega.controller;

// [REACTIVE] 단순 뷰 반환 컨트롤러
// - WebFlux에서 String 반환도 동작하지만, Mono<String>으로 통일하면 명시적으로 비동기임을 표현
// - DB 조회 없는 단순 뷰 반환은 String 그대로도 무방

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping("/")
    public String home() {
        return "redirect:/dashboard";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/dashboard")
    public String dashboard() {
        return "dashboard";
    }
}

