package led.mega.cs.controller;

import led.mega.entity.CsInboundData;
import led.mega.repository.CsInboundDataRepository;
import led.mega.cs.service.CsBotService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/cs/inbound")
@RequiredArgsConstructor
public class CsInboundApiController {

    private final CsInboundDataRepository inboundDataRepository;
    private final CsBotService csBotService;

    @GetMapping("/{id}/ai-solution")
    public Mono<Map<String, String>> getAiSolution(@PathVariable String id) {
        return inboundDataRepository.findById(id)
                .flatMap(inbound -> csBotService.generateAiSolution(inbound.getRawPayload())
                        .flatMap(solution -> {
                            inbound.setAiSuggestion(solution);
                            inbound.setNew(false);
                            return inboundDataRepository.save(inbound).thenReturn(solution);
                        }))
                .map(solution -> Map.of("solution", solution));
    }

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
