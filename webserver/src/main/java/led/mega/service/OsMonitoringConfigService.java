package led.mega.service;

import led.mega.entity.OsMonitoringConfig;
import led.mega.repository.OsMonitoringConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OsMonitoringConfigService {

    private final OsMonitoringConfigRepository osMonitoringConfigRepository;

    /**
     * 모든 에이전트에 적용되는 공통 OS 설정 조회
     */
    public Flux<OsMonitoringConfig> getCommonConfigs() {
        return osMonitoringConfigRepository.findByAgentIdIsNullAndEnabledTrue();
    }

    /**
     * 특정 에이전트 전용 OS 설정 조회
     */
    public Flux<OsMonitoringConfig> getAgentSpecificConfigs(String agentId) {
        return osMonitoringConfigRepository.findByAgentIdAndEnabledTrue(agentId);
    }

    /**
     * 에이전트에게 전달할 최종 OS 설정 목록 (공통 + 에이전트 전용)
     * 에이전트 전용 설정이 공통 설정보다 우선순위를 가집니다 (MetricType 기준).
     */
    public Flux<OsMonitoringConfig> getMergedConfigsForAgent(String agentId) {
        // 실제 구현 시에는 에이전트 전용 설정으로 공통 설정을 덮어쓰는 로직이 들어갈 수 있습니다.
        // 현재는 단순 통합하여 반환합니다.
        return Flux.concat(getCommonConfigs(), getAgentSpecificConfigs(agentId));
    }

    /**
     * OS 설정 저장 (등록/수정)
     */
    public Mono<OsMonitoringConfig> saveConfig(OsMonitoringConfig config) {
        if (config.getConfigId() == null || config.getConfigId().isEmpty()) {
            config.setConfigId("OSC" + UUID.randomUUID().toString().substring(0, 10).toUpperCase());
            config.setNew(true);
            config.setCreatedAt(LocalDateTime.now());
        }
        config.setUpdatedAt(LocalDateTime.now());
        return osMonitoringConfigRepository.save(config);
    }

    /**
     * OS 설정 삭제 (비활성화)
     */
    public Mono<Void> deleteConfig(String configId) {
        return osMonitoringConfigRepository.findById(configId)
                .flatMap(config -> {
                    config.setEnabled(false);
                    return osMonitoringConfigRepository.save(config);
                })
                .then();
    }
}
