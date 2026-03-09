package led.mega.controller;

import led.mega.dto.ExceptionLogResponseDto;
import led.mega.dto.MetricDataResponseDto;
import led.mega.service.ExceptionLogService;
import led.mega.service.MetricDataService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class DashboardApiController {

    private final MetricDataService metricDataService;
    private final ExceptionLogService exceptionLogService;

    @GetMapping("/metrics/recent")
    public Flux<MetricDataResponseDto> getRecentMetrics() {
        return metricDataService.getRecentMetrics();
    }

    @GetMapping("/exceptions/recent")
    public Flux<ExceptionLogResponseDto> getRecentExceptions() {
        return exceptionLogService.getRecentExceptions();
    }
}
