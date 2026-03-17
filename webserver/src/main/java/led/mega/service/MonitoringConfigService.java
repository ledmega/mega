package led.mega.service;

import led.mega.dto.MonitoringConfigDto;
import led.mega.entity.MonitoringConfig;
import led.mega.repository.AgentRepository;
import led.mega.repository.ExceptionLogRepository;
import led.mega.repository.MonitoringConfigRepository;
import led.mega.repository.ServiceMetricDataRepository;
import led.mega.util.IdGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public class MonitoringConfigService {

    private final MonitoringConfigRepository configRepository;
    private final AgentRepository agentRepository;
    private final ExceptionLogRepository exceptionLogRepository;
    private final ServiceMetricDataRepository serviceMetricDataRepository;

    /** 전체 설정 목록 + 에이전트 이름 채움 */
    public Flux<MonitoringConfigDto> getAll() {
        return configRepository.findAllOrderByCreatedAtDesc()
                .flatMap(this::toDto);
    }

    /** 특정 에이전트의 설정 목록 */
    public Flux<MonitoringConfigDto> getByAgentId(String agentId) {
        return configRepository.findByAgentId(agentId)
                .flatMap(this::toDto);
    }

    /** 단건 조회 */
    public Mono<MonitoringConfigDto> getById(String id) {
        return configRepository.findById(id)
                .flatMap(this::toDto);
    }

    /** 활성화된 설정만 조회 (Agent가 Pull할 때 사용) */
    public Flux<MonitoringConfigDto> getActiveByAgentId(String agentId) {
        return configRepository.findActiveByAgentId(agentId)
                .flatMap(this::toDto);
    }

    /** 생성 */
    public Mono<MonitoringConfigDto> create(MonitoringConfigDto dto) {
        MonitoringConfig entity = MonitoringConfig.builder()
                .configId(IdGenerator.generate(IdGenerator.CONFIG))
                .agentId(dto.getAgentId())
                .serviceName(dto.getServiceName())
                .targetType(dto.getTargetType() != null ? dto.getTargetType() : "HOST")
                .targetName(dto.getTargetName())
                .servicePath(dto.getServicePath())
                .logPath(dto.getLogPath())
                .collectItems(dto.getCollectItems() != null ? dto.getCollectItems() : "CPU,MEMORY,DISK")
                .logKeywords(dto.getLogKeywords())
                .intervalSeconds(dto.getIntervalSeconds() != null ? dto.getIntervalSeconds() : 30)
                .enabled(dto.getEnabled() != null ? dto.getEnabled() : true)
                .description(dto.getDescription())
                .isNew(true)
                .build();
        return configRepository.save(entity).flatMap(this::toDto);
    }

    /** 수정 */
    public Mono<MonitoringConfigDto> update(String id, MonitoringConfigDto dto) {
        return configRepository.findById(id)
                .flatMap(entity -> {
                    entity.setServiceName(dto.getServiceName());
                    entity.setTargetType(dto.getTargetType() != null ? dto.getTargetType() : "HOST");
                    entity.setTargetName(dto.getTargetName());
                    entity.setServicePath(dto.getServicePath());
                    entity.setLogPath(dto.getLogPath());
                    entity.setCollectItems(dto.getCollectItems());
                    entity.setLogKeywords(dto.getLogKeywords());
                    entity.setIntervalSeconds(dto.getIntervalSeconds());
                    entity.setEnabled(dto.getEnabled());
                    entity.setDescription(dto.getDescription());
                    return configRepository.save(entity);
                })
                .flatMap(this::toDto);
    }

    /** 삭제 */
    public Mono<Void> delete(String id) {
        return configRepository.deleteById(id);
    }

    /** 활성화/비활성화 토글 */
    public Mono<MonitoringConfigDto> toggleEnabled(String id) {
        return configRepository.findById(id)
                .flatMap(entity -> {
                    entity.setEnabled(!entity.getEnabled());
                    return configRepository.save(entity);
                })
                .flatMap(this::toDto);
    }

    private Mono<MonitoringConfigDto> toDto(MonitoringConfig entity) {
        java.time.LocalDateTime since = java.time.LocalDateTime.now().minusDays(1);

        Mono<MonitoringConfigDto> baseDto = agentRepository.findById(entity.getAgentId())
                .map(agent -> MonitoringConfigDto.builder()
                        .id(entity.getConfigId())
                        .agentId(entity.getAgentId())
                        .agentName(agent.getName() != null ? agent.getName() : agent.getAgentId())
                        .serviceName(entity.getServiceName())
                        .targetType(entity.getTargetType())
                        .targetName(entity.getTargetName())
                        .servicePath(entity.getServicePath())
                        .logPath(entity.getLogPath())
                        .collectItems(entity.getCollectItems())
                        .logKeywords(entity.getLogKeywords())
                        .intervalSeconds(entity.getIntervalSeconds())
                        .enabled(entity.getEnabled())
                        .description(entity.getDescription())
                        .createdAt(entity.getCreatedAt())
                        .updatedAt(entity.getUpdatedAt())
                        .build())
                .defaultIfEmpty(MonitoringConfigDto.builder()
                        .id(entity.getConfigId())
                        .agentId(entity.getAgentId())
                        .agentName("(삭제된 에이전트)")
                        .serviceName(entity.getServiceName())
                        .targetType(entity.getTargetType())
                        .targetName(entity.getTargetName())
                        .servicePath(entity.getServicePath())
                        .logPath(entity.getLogPath())
                        .collectItems(entity.getCollectItems())
                        .logKeywords(entity.getLogKeywords())
                        .intervalSeconds(entity.getIntervalSeconds())
                        .enabled(entity.getEnabled())
                        .description(entity.getDescription())
                        .createdAt(entity.getCreatedAt())
                        .updatedAt(entity.getUpdatedAt())
                        .build());

        Mono<Long> recentCount = exceptionLogRepository
                .countByMonitoringConfigIdSince(entity.getConfigId(), since)
                .defaultIfEmpty(0L);

        return baseDto
                .zipWith(recentCount)
                .flatMap(tuple -> {
                    MonitoringConfigDto dto = tuple.getT1();
                    dto.setRecentExceptionCount(tuple.getT2());

                    return serviceMetricDataRepository
                            .findTopByMonitoringConfigIdOrderByCollectedAtDesc(entity.getConfigId())
                            .map(m -> {
                                dto.setRecentCpu(m.getCpuUsagePercent());
                                dto.setRecentMemory(m.getMemoryUsagePercent() != null ? m.getMemoryUsagePercent() : m.getMemoryUsageMb());
                                dto.setRecentDisk(m.getDiskUsagePercent());
                                return dto;
                            })
                            .switchIfEmpty(Mono.just(dto));
                });
    }
}
