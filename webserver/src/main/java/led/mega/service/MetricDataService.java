package led.mega.service;

// [REACTIVE] 핵심 변경점
// - .agent(agent).task(task) → .agentId(agentId).taskId(taskId)
// - TaskRepository 제거 (taskId Long 직접 사용)
// - MetricType 파싱 오류: throw → Mono.error
// - @Query에서 MetricType enum → .name() 문자열로 전달

import led.mega.dto.MetricDataRequestDto;
import led.mega.dto.MetricDataResponseDto;
import led.mega.entity.MetricData;
import led.mega.entity.MetricType;
import led.mega.repository.AgentRepository;
import led.mega.repository.MetricDataRepository;
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
    // [REMOVED] TaskRepository

    @Transactional
    public Mono<MetricDataResponseDto> saveMetricData(Long agentId, MetricDataRequestDto requestDto) {
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
                            .build();
                    return metricDataRepository.save(metricData);
                })
                .map(this::toResponseDto)
                .doOnNext(r -> {
                    log.debug("메트릭 데이터 저장 완료: agentId={}, metricType={}", agentId, r.getMetricType());
                    sseService.broadcastMetric(agentId, r);
                });
    }

    public Flux<MetricDataResponseDto> getMetricDataByAgentId(Long agentId) {
        return metricDataRepository.findByAgentId(agentId).map(this::toResponseDto);
    }

    public Flux<MetricDataResponseDto> getMetricDataByAgentIdAndTimeRange(
            Long agentId, LocalDateTime startTime, LocalDateTime endTime) {
        return metricDataRepository.findByAgentIdAndCollectedAtBetween(agentId, startTime, endTime)
                .map(this::toResponseDto);
    }

    public Flux<MetricDataResponseDto> getMetricDataByAgentIdAndType(Long agentId, MetricType metricType) {
        return metricDataRepository.findByAgentIdAndMetricType(agentId, metricType).map(this::toResponseDto);
    }

    public Flux<MetricDataResponseDto> getMetricDataByAgentIdAndTypeAndTimeRange(
            Long agentId, MetricType metricType, LocalDateTime startTime, LocalDateTime endTime) {
        // [CHANGED] MetricType enum → .name() 문자열 전달 (@Query 네이티브 SQL 파라미터)
        return metricDataRepository.findByAgentIdAndMetricTypeAndCollectedAtBetween(
                agentId, metricType.name(), startTime, endTime).map(this::toResponseDto);
    }

    private MetricDataResponseDto toResponseDto(MetricData metricData) {
        return MetricDataResponseDto.builder()
                .id(metricData.getId())
                .agentId(metricData.getAgentId())  // [CHANGED] .getAgent().getId() → .getAgentId()
                .taskId(metricData.getTaskId())    // [CHANGED] .getTask().getId()  → .getTaskId()
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

    /**
     * 오늘 00:00 이후 수집된 메트릭 개수 반환.
     */
    public Mono<Long> getTodayMetricCount() {
        LocalDateTime startOfDay = LocalDateTime.now().toLocalDate().atStartOfDay();
        return metricDataRepository.countByCollectedAtAfter(startOfDay);
    }
}

