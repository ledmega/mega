package led.mega.cs.controller;

import led.mega.entity.CsInboundData;
import led.mega.repository.CsInboundDataRepository;
import led.mega.cs.service.CsBotService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/cs/inbound")
@RequiredArgsConstructor
public class CsInboundApiController {

    private final CsInboundDataRepository inboundDataRepository;
    private final CsBotService csBotService;

    @Value("${mega.storage.path}")
    private String storagePath;

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
    public Mono<Map<String, Object>> getAllInboundData(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size) {
        return inboundDataRepository.findAllByOrderByReceivedAtDesc(PageRequest.of(page, size))
                .collectList()
                .zipWith(inboundDataRepository.count())
                .map(tuple -> Map.of(
                        "content", tuple.getT1(),
                        "totalElements", tuple.getT2(),
                        "page", page,
                        "size", size,
                        "totalPages", (int) Math.ceil((double) tuple.getT2() / size)
                ));
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
                    existing.setAttachments(request.getAttachments());
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

    @GetMapping("/{id}/download")
    public Mono<ResponseEntity<Resource>> downloadAttachment(@PathVariable String id, @RequestParam String filename) {
        return Mono.fromCallable(() -> {
            try {
                Path path = Paths.get(storagePath).resolve(filename).normalize();
                if (!Files.exists(path)) {
                    log.error("[CS-API] File not found: {}", path);
                    return ResponseEntity.notFound().<Resource>build();
                }
                Resource resource = new UrlResource(path.toUri());
                return ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                        .body(resource);
            } catch (MalformedURLException e) {
                log.error("[CS-API] Download error: {}", e.getMessage());
                return ResponseEntity.internalServerError().<Resource>build();
            }
        }).subscribeOn(Schedulers.boundedElastic());
    }
}
