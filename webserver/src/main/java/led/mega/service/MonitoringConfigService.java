package led.mega.service;

import led.mega.dto.MonitoringConfigDto;
import led.mega.entity.MetricData;
import led.mega.entity.MonitoringConfig;
import led.mega.repository.AgentRepository;
import led.mega.repository.ExceptionLogRepository;
import led.mega.repository.MetricDataRepository;
import led.mega.repository.MonitoringConfigRepository;
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
    private final MetricDataRepository metricDataRepository;

    /** 전체 설정 목록 + 에이전트 이름 채움 */
    public Flux<MonitoringConfigDto> getAll() {
        return configRepository.findAllOrderByCreatedAtDesc()
                .flatMap(this::toDto);
    }

    /** 특정 에이전트의 설정 목록 */
    public Flux<MonitoringConfigDto> getByAgentId(Long agentId) {
        return configRepository.findByAgentId(agentId)
                .flatMap(this::toDto);
    }

    /** 단건 조회 */
    public Mono<MonitoringConfigDto> getById(Long id) {
        return configRepository.findById(id)
                .flatMap(this::toDto);
    }

    /** 활성화된 설정만 조회 (Agent가 Pull할 때 사용) */
    public Flux<MonitoringConfigDto> getActiveByAgentId(Long agentId) {
        return configRepository.findActiveByAgentId(agentId)
                .flatMap(this::toDto);
    }

    /** 생성 */
    public Mono<MonitoringConfigDto> create(MonitoringConfigDto dto) {
        MonitoringConfig entity = MonitoringConfig.builder()
                .agentId(dto.getAgentId())
                .serviceName(dto.getServiceName())
                .servicePath(dto.getServicePath())
                .logPath(dto.getLogPath())
                .collectItems(dto.getCollectItems() != null ? dto.getCollectItems() : "CPU,MEMORY,DISK")
                .logKeywords(dto.getLogKeywords())
                .intervalSeconds(dto.getIntervalSeconds() != null ? dto.getIntervalSeconds() : 30)
                .enabled(dto.getEnabled() != null ? dto.getEnabled() : true)
                .description(dto.getDescription())
                .build();
        return configRepository.save(entity).flatMap(this::toDto);
    }

    /** 수정 */
    public Mono<MonitoringConfigDto> update(Long id, MonitoringConfigDto dto) {
        return configRepository.findById(id)
                .flatMap(entity -> {
                    entity.setServiceName(dto.getServiceName());
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
    public Mono<Void> delete(Long id) {
        return configRepository.deleteById(id);
    }

    /** 활성화/비활성화 토글 */
    public Mono<MonitoringConfigDto> toggleEnabled(Long id) {
        return configRepository.findById(id)
                .flatMap(entity -> {
                    entity.setEnabled(!entity.getEnabled());
                    return configRepository.save(entity);
                })
                .flatMap(this::toDto);
    }

    // ------------------------------------------------------------------
    // Private helper
    // ------------------------------------------------------------------
    private Mono<MonitoringConfigDto> toDto(MonitoringConfig entity) {
        // 최근 24시간 예외 건수
        java.time.LocalDateTime since = java.time.LocalDateTime.now().minusDays(1);

        Mono<MonitoringConfigDto> baseDto = agentRepository.findById(entity.getAgentId())
                .map(agent -> MonitoringConfigDto.builder()
                        .id(entity.getId())
                        .agentId(entity.getAgentId())
                        .agentName(agent.getName() != null ? agent.getName() : agent.getAgentId())
                        .serviceName(entity.getServiceName())
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
                        .id(entity.getId())
                        .agentId(entity.getAgentId())
                        .agentName("(삭제된 에이전트)")
                        .serviceName(entity.getServiceName())
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
                .countByMonitoringConfigIdSince(entity.getId(), since)
                .defaultIfEmpty(0L);

        // 예외 건수까지 채운 후, CPU/MEM/DISK 메트릭은 각각 존재할 때만 DTO에 채운다.
        return baseDto
                .zipWith(recentCount)
                .flatMap(tuple -> {
                    MonitoringConfigDto dto = tuple.getT1();
                    dto.setRecentExceptionCount(tuple.getT2());

                    Mono<MonitoringConfigDto> withCpu = metricDataRepository
                            .findLatestByMonitoringConfigIdAndMetricType(entity.getId(), "CPU")
                            .next()
                            .map(m -> {
                                dto.setRecentCpu(m.getMetricValue());
                                return dto;
                            })
                            .switchIfEmpty(Mono.just(dto));

                    Mono<MonitoringConfigDto> withMem = withCpu.flatMap(d ->
                            metricDataRepository
                                    .findLatestByMonitoringConfigIdAndMetricType(entity.getId(), "MEMORY")
                                    .next()
                                    .map(m -> {
                                        d.setRecentMemory(m.getMetricValue());
                                        return d;
                                    })
                                    .switchIfEmpty(Mono.just(d))
                    );

                    return withMem.flatMap(d ->
                            metricDataRepository
                                    .findLatestByMonitoringConfigIdAndMetricType(entity.getId(), "DISK")
                                    .next()
                                    .map(m -> {
                                        d.setRecentDisk(m.getMetricValue());
                                        return d;
                                    })
                                    .switchIfEmpty(Mono.just(d))
                    );
                });
    }
}
