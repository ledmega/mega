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
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class DashboardApiController {

    private final MetricDataService metricDataService;
    private final ExceptionLogService exceptionLogService;

    @GetMapping("/metrics/recent")
    public Flux<MetricDataResponseDto> getRecentMetrics(
            @org.springframework.web.bind.annotation.RequestParam(required = false, defaultValue = "0") int hours,
            @org.springframework.web.bind.annotation.RequestParam(required = false) String metricType,
            @org.springframework.web.bind.annotation.RequestParam(required = false) String metricName) {
        
        if (metricName != null && !metricName.isEmpty()) {
            return metricDataService.getRecentMetricsByName(metricName, hours > 0 ? hours : 1);
        }
        
        if (metricType != null && !metricType.isEmpty()) {
            return metricDataService.getRecentMetricsByType(metricType, hours > 0 ? hours : 1);
        }
        
        if (hours > 0) {
            return metricDataService.getRecentMetrics(hours);
        }
        return metricDataService.getRecentMetrics();
    }

    @GetMapping("/metrics/today-count")
    public Mono<Long> getTodayMetricCount() {
        return metricDataService.getTodayMetricCount();
    }

    @GetMapping("/exceptions/recent")
    public Flux<ExceptionLogResponseDto> getRecentExceptions() {
        return exceptionLogService.getRecentExceptions();
    }
}
