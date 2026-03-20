package led.mega.controller;

import led.mega.entity.OsMonitoringConfig;
import led.mega.service.OsMonitoringConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
@RequestMapping("/api/os-configs")
@RequiredArgsConstructor
public class OsMonitoringConfigApiController {

    private final OsMonitoringConfigService osMonitoringConfigService;

    /**
     * 공통 OS 설정 목록 조회
     */
    @GetMapping("/common")
    public Flux<OsMonitoringConfig> getCommonConfigs() {
        return osMonitoringConfigService.getCommonConfigs();
    }

    /**
     * 특정 에이전트의 OS 설정 목록 조회 (공통 포함)
     */
    @GetMapping("/agent/{agentId}")
    public Flux<OsMonitoringConfig> getConfigsForAgent(@PathVariable String agentId) {
        return osMonitoringConfigService.getMergedConfigsForAgent(agentId);
    }

    /**
     * OS 설정 저장 (등록/수정)
     */
    @PostMapping
    public Mono<ResponseEntity<OsMonitoringConfig>> saveConfig(@RequestBody OsMonitoringConfig config) {
        return osMonitoringConfigService.saveConfig(config)
                .map(saved -> ResponseEntity.status(HttpStatus.CREATED).body(saved))
                .doOnError(e -> log.error("OS 설정 저장 중 오류 발생: {}", e.getMessage(), e))
                .onErrorResume(e -> Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()));
    }

    /**
     * OS 설정 삭제 (비활성화)
     */
    @DeleteMapping("/{configId}")
    public Mono<ResponseEntity<Void>> deleteConfig(@PathVariable String configId) {
        return osMonitoringConfigService.deleteConfig(configId)
                .then(Mono.just(ResponseEntity.noContent().<Void>build()))
                .doOnError(e -> log.error("OS 설정 삭제 중 오류 발생: {}", e.getMessage(), e))
                .onErrorResume(e -> Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()));
    }
}
