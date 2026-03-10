package led.mega.batch.controller;

import led.mega.batch.entity.BatchJob;
import led.mega.batch.service.BatchJobService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
@RequestMapping("/api/batch/jobs")
@RequiredArgsConstructor
public class BatchJobApiController {

    private final BatchJobService batchJobService;

    /** 전체 목록 조회 */
    @GetMapping
    public Flux<BatchJob> findAll() {
        return batchJobService.findAll();
    }

    /** 단건 조회 */
    @GetMapping("/{id}")
    public Mono<ResponseEntity<BatchJob>> findById(@PathVariable Long id) {
        return batchJobService.findById(id)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    /** 생성 */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<BatchJob> create(@RequestBody BatchJob job) {
        return batchJobService.create(job);
    }

    /** 수정 */
    @PutMapping("/{id}")
    public Mono<ResponseEntity<BatchJob>> update(@PathVariable Long id, @RequestBody BatchJob job) {
        return batchJobService.update(id, job)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    /** 삭제 */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> delete(@PathVariable Long id) {
        return batchJobService.delete(id);
    }

    /** 활성화/비활성화 토글 */
    @PatchMapping("/{id}/toggle")
    public Mono<ResponseEntity<BatchJob>> toggle(@PathVariable Long id) {
        return batchJobService.toggleEnabled(id)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    /** 즉시 실행 */
    @PostMapping("/{id}/run")
    public Mono<ResponseEntity<Map<String, String>>> runNow(@PathVariable Long id) {
        return batchJobService.runNow(id)
                .map(msg -> ResponseEntity.ok(Map.of("message", msg)))
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }
}
