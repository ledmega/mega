package led.mega.controller;

import led.mega.dto.MetricDataResponseDto;
import led.mega.entity.MetricType;
import led.mega.service.MetricDataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;

@Slf4j
@RestController
@RequestMapping("/api/agents/{agentId}/metrics")
@RequiredArgsConstructor
public class MetricApiController {

    private final MetricDataService metricDataService;

    @GetMapping
    public Flux<MetricDataResponseDto> getMetrics(@PathVariable String agentId) {
        return metricDataService.getMetricDataByAgentId(agentId);
    }

    @GetMapping("/type/{metricType}")
    public Flux<MetricDataResponseDto> getMetricsByType(
            @PathVariable String agentId,
            @PathVariable String metricType) {
        try {
            MetricType type = MetricType.valueOf(metricType.toUpperCase());
            return metricDataService.getMetricDataByAgentIdAndType(agentId, type);
        } catch (IllegalArgumentException e) {
            return Flux.empty();
        }
    }

    @GetMapping("/range")
    public Flux<MetricDataResponseDto> getMetricsByTimeRange(
            @PathVariable String agentId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        return metricDataService.getMetricDataByAgentIdAndTimeRange(agentId, startTime, endTime);
    }

    @GetMapping("/type/{metricType}/range")
    public Flux<MetricDataResponseDto> getMetricsByTypeAndTimeRange(
            @PathVariable String agentId,
            @PathVariable String metricType,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        try {
            MetricType type = MetricType.valueOf(metricType.toUpperCase());
            return metricDataService.getMetricDataByAgentIdAndTypeAndTimeRange(agentId, type, startTime, endTime);
        } catch (IllegalArgumentException e) {
            return Flux.empty();
        }
    }
}
