package led.mega.service;

import led.mega.dto.MetricDataRequestDto;
import led.mega.dto.MetricDataResponseDto;
import led.mega.entity.Agent;
import led.mega.entity.MetricData;
import led.mega.entity.MetricType;
import led.mega.entity.Task;
import led.mega.repository.AgentRepository;
import led.mega.repository.MetricDataRepository;
import led.mega.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MetricDataService {

    private final MetricDataRepository metricDataRepository;
    private final AgentRepository agentRepository;
    private final TaskRepository taskRepository;

    /**
     * 메트릭 데이터 저장
     */
    @Transactional
    public MetricDataResponseDto saveMetricData(Long agentId, MetricDataRequestDto requestDto) {
        Agent agent = agentRepository.findById(agentId)
                .orElseThrow(() -> new IllegalArgumentException("에이전트를 찾을 수 없습니다. id: " + agentId));

        Task task = null;
        if (requestDto.getTaskId() != null) {
            task = taskRepository.findById(requestDto.getTaskId())
                    .orElse(null);  // 작업이 없어도 계속 진행
        }

        MetricType metricType;
        try {
            metricType = MetricType.valueOf(requestDto.getMetricType().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("유효하지 않은 메트릭 타입입니다: " + requestDto.getMetricType());
        }

        MetricData metricData = MetricData.builder()
                .agent(agent)
                .task(task)
                .metricType(metricType)
                .metricName(requestDto.getMetricName())
                .metricValue(requestDto.getMetricValue())
                .unit(requestDto.getUnit())
                .rawData(requestDto.getRawData())
                .collectedAt(requestDto.getCollectedAt() != null ? requestDto.getCollectedAt() : LocalDateTime.now())
                .build();

        MetricData savedMetric = metricDataRepository.save(metricData);
        log.debug("메트릭 데이터 저장 완료: agentId={}, metricType={}, metricName={}", 
                agentId, metricType, requestDto.getMetricName());

        // WebSocket으로 실시간 전송 (WebSocketService가 활성화되면 자동으로 작동)
        MetricDataResponseDto responseDto = toResponseDto(savedMetric);
        // TODO: WebSocketService 활성화 시 주석 해제
        // webSocketService.ifPresent(ws -> ws.broadcastMetric(agentId, responseDto));

        return responseDto;
    }

    /**
     * 에이전트별 메트릭 데이터 조회
     */
    public List<MetricDataResponseDto> getMetricDataByAgentId(Long agentId) {
        return metricDataRepository.findByAgentId(agentId).stream()
                .map(this::toResponseDto)
                .collect(Collectors.toList());
    }

    /**
     * 시간 범위별 메트릭 데이터 조회
     */
    public List<MetricDataResponseDto> getMetricDataByAgentIdAndTimeRange(
            Long agentId, LocalDateTime startTime, LocalDateTime endTime) {
        return metricDataRepository.findByAgentIdAndCollectedAtBetween(agentId, startTime, endTime).stream()
                .map(this::toResponseDto)
                .collect(Collectors.toList());
    }

    /**
     * 메트릭 타입별 조회
     */
    public List<MetricDataResponseDto> getMetricDataByAgentIdAndType(Long agentId, MetricType metricType) {
        return metricDataRepository.findByAgentIdAndMetricType(agentId, metricType).stream()
                .map(this::toResponseDto)
                .collect(Collectors.toList());
    }

    /**
     * 메트릭 타입 및 시간 범위별 조회
     */
    public List<MetricDataResponseDto> getMetricDataByAgentIdAndTypeAndTimeRange(
            Long agentId, MetricType metricType, LocalDateTime startTime, LocalDateTime endTime) {
        return metricDataRepository.findByAgentIdAndMetricTypeAndCollectedAtBetween(
                agentId, metricType, startTime, endTime).stream()
                .map(this::toResponseDto)
                .collect(Collectors.toList());
    }

    /**
     * Entity를 DTO로 변환
     */
    private MetricDataResponseDto toResponseDto(MetricData metricData) {
        return MetricDataResponseDto.builder()
                .id(metricData.getId())
                .agentId(metricData.getAgent().getId())
                .taskId(metricData.getTask() != null ? metricData.getTask().getId() : null)
                .metricType(metricData.getMetricType())
                .metricName(metricData.getMetricName())
                .metricValue(metricData.getMetricValue())
                .unit(metricData.getUnit())
                .rawData(metricData.getRawData())
                .collectedAt(metricData.getCollectedAt())
                .createdAt(metricData.getCreatedAt())
                .build();
    }
}

