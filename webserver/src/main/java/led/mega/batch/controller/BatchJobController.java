package led.mega.batch.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class BatchJobController {

    @GetMapping("/scheduler")
    public String schedulerPage() {
        return "scheduler/list";
    }
}
