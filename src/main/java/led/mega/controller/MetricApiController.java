package led.mega.controller;

import led.mega.dto.MetricDataResponseDto;
import led.mega.entity.MetricType;
import led.mega.service.MetricDataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/agents/{agentId}/metrics")
@RequiredArgsConstructor
public class MetricApiController {

    private final MetricDataService metricDataService;

    /**
     * 에이전트별 메트릭 데이터 조회
     */
    @GetMapping
    public ResponseEntity<List<MetricDataResponseDto>> getMetrics(@PathVariable Long agentId) {
        List<MetricDataResponseDto> metrics = metricDataService.getMetricDataByAgentId(agentId);
        return ResponseEntity.ok(metrics);
    }

    /**
     * 메트릭 타입별 조회
     */
    @GetMapping("/type/{metricType}")
    public ResponseEntity<List<MetricDataResponseDto>> getMetricsByType(
            @PathVariable Long agentId,
            @PathVariable String metricType) {
        try {
            MetricType type = MetricType.valueOf(metricType.toUpperCase());
            List<MetricDataResponseDto> metrics = metricDataService.getMetricDataByAgentIdAndType(agentId, type);
            return ResponseEntity.ok(metrics);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(400).build();
        }
    }

    /**
     * 시간 범위별 메트릭 데이터 조회
     */
    @GetMapping("/range")
    public ResponseEntity<List<MetricDataResponseDto>> getMetricsByTimeRange(
            @PathVariable Long agentId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        List<MetricDataResponseDto> metrics = metricDataService.getMetricDataByAgentIdAndTimeRange(
                agentId, startTime, endTime);
        return ResponseEntity.ok(metrics);
    }

    /**
     * 메트릭 타입 및 시간 범위별 조회
     */
    @GetMapping("/type/{metricType}/range")
    public ResponseEntity<List<MetricDataResponseDto>> getMetricsByTypeAndTimeRange(
            @PathVariable Long agentId,
            @PathVariable String metricType,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        try {
            MetricType type = MetricType.valueOf(metricType.toUpperCase());
            List<MetricDataResponseDto> metrics = metricDataService.getMetricDataByAgentIdAndTypeAndTimeRange(
                    agentId, type, startTime, endTime);
            return ResponseEntity.ok(metrics);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(400).build();
        }
    }
}

