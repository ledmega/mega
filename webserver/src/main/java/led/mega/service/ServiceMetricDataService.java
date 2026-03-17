package led.mega.service;

import led.mega.dto.ServiceMetricDataRequestDto;
import led.mega.entity.ServiceMetricData;
import led.mega.repository.ServiceMetricDataRepository;
import led.mega.util.IdGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public class ServiceMetricDataService {

    private final ServiceMetricDataRepository serviceMetricDataRepository;

    public Mono<Void> saveServiceMetric(String agentId, ServiceMetricDataRequestDto dto) {
        ServiceMetricData entity = ServiceMetricData.builder()
                .svcMetricId(IdGenerator.generate(IdGenerator.SVC_METRIC))
                .agentId(agentId)
                .monitoringConfigId(dto.getMonitoringConfigId())
                .cpuUsagePercent(dto.getCpuUsagePercent())
                .memoryUsageMb(dto.getMemoryUsageMb())
                .memoryUsagePercent(dto.getMemoryUsagePercent())
                .diskUsagePercent(dto.getDiskUsagePercent())
                .networkRxBytes(dto.getNetworkRxBytes())
                .networkTxBytes(dto.getNetworkTxBytes())
                .collectedAt(dto.getCollectedAt())
                .build();
                
        return serviceMetricDataRepository.save(entity)
                .doOnSuccess(saved -> log.debug("Saved service metric for configId: {}", dto.getMonitoringConfigId()))
                .then();
    }
}
