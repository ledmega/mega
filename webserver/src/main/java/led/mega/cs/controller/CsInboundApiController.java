package led.mega.cs.controller;

import led.mega.entity.CsInboundData;
import led.mega.repository.CsInboundDataRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Slf4j
@RestController
@RequestMapping("/api/cs/inbound")
@RequiredArgsConstructor
public class CsInboundApiController {

    private final CsInboundDataRepository inboundDataRepository;

    @GetMapping
    public Flux<CsInboundData> getAllInboundData() {
        return inboundDataRepository.findAllByOrderByReceivedAtDesc();
    }

    @GetMapping("/{id}")
    public Mono<CsInboundData> getInboundData(@PathVariable String id) {
        return inboundDataRepository.findById(id);
    }

    @PutMapping("/{id}")
    public Mono<CsInboundData> updateInboundData(@PathVariable String id, @RequestBody CsInboundData request) {
        return inboundDataRepository.findById(id)
                .flatMap(existing -> {
                    existing.setStatus(request.getStatus());
                    existing.setRawPayload(request.getRawPayload());
                    existing.setResolvedPayload(request.getResolvedPayload());
                    existing.setProcessingHistory(request.getProcessingHistory());
                    existing.setExternalRefId(request.getExternalRefId());
                    existing.setProcessedAt(LocalDateTime.now());
                    existing.setNew(false);
                    return inboundDataRepository.save(existing);
                });
    }

    @DeleteMapping("/{id}")
    public Mono<Void> deleteInboundData(@PathVariable String id) {
        return inboundDataRepository.deleteById(id);
    }
}
