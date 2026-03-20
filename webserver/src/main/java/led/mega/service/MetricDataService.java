package led.mega.service;

import led.mega.dto.MetricDataRequestDto;
import led.mega.dto.MetricDataResponseDto;
import led.mega.entity.MetricData;
import led.mega.entity.MetricType;
import led.mega.repository.AgentRepository;
import led.mega.repository.MetricDataRepository;
import led.mega.util.IdGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class MetricDataService {

    private final MetricDataRepository metricDataRepository;
    private final AgentRepository agentRepository;
    private final SseService sseService;

    @Transactional
    public Mono<MetricDataResponseDto> saveMetricData(String agentId, MetricDataRequestDto requestDto) {
        MetricType metricType;
        try {
            metricType = MetricType.valueOf(requestDto.getMetricType().toUpperCase());
        } catch (IllegalArgumentException e) {
            return Mono.error(new IllegalArgumentException("유효하지 않은 메트릭 타입입니다: " + requestDto.getMetricType()));
        }

        final MetricType finalMetricType = metricType;

        return agentRepository.findById(agentId)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("에이전트를 찾을 수 없습니다. id: " + agentId)))
                .flatMap(agent -> {
                    MetricData metricData = MetricData.builder()
                            .metricId(IdGenerator.generate(IdGenerator.METRIC))
                            .agentId(agentId)
                            .taskId(requestDto.getTaskId())
                            .monitoringConfigId(requestDto.getMonitoringConfigId())
                            .metricType(finalMetricType)
                            .metricName(requestDto.getMetricName())
                            .metricValue(requestDto.getMetricValue())
                            .unit(requestDto.getUnit())
                            .rawData(requestDto.getRawData())
                            .collectedAt(requestDto.getCollectedAt() != null
                                    ? requestDto.getCollectedAt() : LocalDateTime.now())
                            .isNew(true)
                            .build();
                    return metricDataRepository.save(metricData);
                })
                .map(this::toResponseDto)
                .doOnNext(r -> {
                    log.debug("메트릭 데이터 저장 완료: agentId={}, metricType={}", agentId, r.getMetricType());
                    sseService.broadcastMetric(agentId, r);
                });
    }

    public Flux<MetricDataResponseDto> getMetricDataByAgentId(String agentId) {
        return metricDataRepository.findByAgentId(agentId).map(this::toResponseDto);
    }

    public Flux<MetricDataResponseDto> getMetricDataByAgentIdAndTimeRange(
            String agentId, LocalDateTime startTime, LocalDateTime endTime) {
        return metricDataRepository.findByAgentIdAndCollectedAtBetween(agentId, startTime, endTime)
                .map(this::toResponseDto);
    }

    public Flux<MetricDataResponseDto> getMetricDataByAgentIdAndType(String agentId, MetricType metricType) {
        return metricDataRepository.findByAgentIdAndMetricType(agentId, metricType).map(this::toResponseDto);
    }

    public Flux<MetricDataResponseDto> getMetricDataByAgentIdAndTypeAndTimeRange(
            String agentId, MetricType metricType, LocalDateTime startTime, LocalDateTime endTime) {
        return metricDataRepository.findByAgentIdAndMetricTypeAndCollectedAtBetween(
                agentId, metricType.name(), startTime, endTime).map(this::toResponseDto);
    }

    private MetricDataResponseDto toResponseDto(MetricData metricData) {
        return MetricDataResponseDto.builder()
                .id(metricData.getMetricId())
                .agentId(metricData.getAgentId())
                .taskId(metricData.getTaskId())
                .metricType(metricData.getMetricType())
                .metricName(metricData.getMetricName())
                .metricValue(metricData.getMetricValue())
                .unit(metricData.getUnit())
                .rawData(metricData.getRawData())
                .collectedAt(metricData.getCollectedAt())
                .createdAt(metricData.getCreatedAt())
                .build();
    }

    public Flux<MetricDataResponseDto> getRecentMetrics() {
        return metricDataRepository.findTop500ByOrderByCollectedAtDesc()
                .map(this::toResponseDto);
    }

    public Flux<MetricDataResponseDto> getRecentMetrics(int hours) {
        LocalDateTime startTime = LocalDateTime.now().minusHours(hours);
        return metricDataRepository.findByCollectedAtAfterOrderByCollectedAtDesc(startTime)
                .map(this::toResponseDto);
    }

    public Flux<MetricDataResponseDto> getRecentMetricsByType(String metricType, int hours) {
        LocalDateTime startTime = LocalDateTime.now().minusHours(hours);
        return metricDataRepository.findByMetricTypeAndCollectedAtAfterOrderByCollectedAtDesc(metricType, startTime)
                .map(this::toResponseDto);
    }

    public Mono<Long> getTodayMetricCount() {
        LocalDateTime startOfDay = LocalDateTime.now().toLocalDate().atStartOfDay();
        return metricDataRepository.countByCollectedAtAfter(startOfDay);
    }
}
