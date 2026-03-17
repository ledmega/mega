package led.mega.controller;

import led.mega.dto.MonitoringConfigDto;
import led.mega.service.MonitoringConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** 서비스 관리 REST API Controller */
@Slf4j
@RestController
@RequestMapping("/api/configs")
@RequiredArgsConstructor
public class MonitoringConfigApiController {

    private final MonitoringConfigService configService;

    /** 전체 목록 */
    @GetMapping
    public Flux<MonitoringConfigDto> getAll() {
        return configService.getAll();
    }

    /** 에이전트 별 목록 */
    @GetMapping("/agent/{agentId}")
    public Flux<MonitoringConfigDto> getByAgent(@PathVariable String agentId) {
        return configService.getByAgentId(agentId);
    }

    /** 에이전트 별 활성 설정 목록 (Agent가 주기적으로 Pull) */
    @GetMapping("/agent/{agentId}/active")
    public Flux<MonitoringConfigDto> getActiveByAgent(@PathVariable String agentId) {
        return configService.getActiveByAgentId(agentId);
    }

    /** 단건 조회 */
    @GetMapping("/{id}")
    public Mono<ResponseEntity<MonitoringConfigDto>> getById(@PathVariable String id) {
        return configService.getById(id)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    /** 생성 */
    @PostMapping
    public Mono<ResponseEntity<MonitoringConfigDto>> create(@RequestBody MonitoringConfigDto dto) {
        return configService.create(dto)
                .map(resp -> ResponseEntity.ok(resp))
                .onErrorReturn(ResponseEntity.badRequest().build());
    }

    /** 수정 */
    @PutMapping("/{id}")
    public Mono<ResponseEntity<MonitoringConfigDto>> update(@PathVariable String id,
                                                            @RequestBody MonitoringConfigDto dto) {
        return configService.update(id, dto)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    /** 삭제 */
    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<Void>> delete(@PathVariable String id) {
        return configService.delete(id)
                .then(Mono.just(ResponseEntity.<Void>noContent().build()));
    }

    /** 활성화/비활성화 토글 */
    @PatchMapping("/{id}/toggle")
    public Mono<ResponseEntity<MonitoringConfigDto>> toggle(@PathVariable String id) {
        return configService.toggleEnabled(id)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }
}
