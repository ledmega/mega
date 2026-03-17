package led.mega.cs.controller;

import led.mega.cs.dto.CsBotResponseDto;
import led.mega.cs.dto.CsInboundRequestDto;
import led.mega.cs.service.CsBotService;
import led.mega.entity.CsFaq;
import led.mega.repository.CsConversationRepository;
import led.mega.repository.CsFaqRepository;
import led.mega.repository.CsMessageRepository;
import led.mega.util.IdGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

/**
 * CS 자동화 개발/테스트용 시뮬레이터 컨트롤러.
 * 실제 카카오톡(톡드림), 이메일 없이도 문의 유입부터 AI 처리까지 테스트할 수 있습니다.
 *
 * POST /api/cs/simulate/inbound  - 가상 문의를 발생시켜 CsBotService 전체 흐름 테스트
 * POST /api/cs/faq               - FAQ 데이터 등록
 * GET  /api/cs/faq               - FAQ 목록 조회
 * GET  /api/cs/conversations     - 상담 세션 목록 조회
 * GET  /api/cs/conversations/{id}/messages - 특정 세션의 메시지 조회
 */
@Slf4j
@RestController
@RequestMapping("/api/cs")
@RequiredArgsConstructor
public class CsSimulationController {

    private final CsBotService csBotService;
    private final CsFaqRepository faqRepository;
    private final CsConversationRepository conversationRepository;
    private final CsMessageRepository messageRepository;

    // -------------------------------------------------------------------------
    // 시뮬레이터: 가상 문의 발생
    // -------------------------------------------------------------------------

    /**
     * 가상 문의를 주입하여 CsBotService 전체 흐름을 테스트합니다.
     *
     * 예시 요청 Body:
     * {
     *   "source": "EMAIL",
     *   "externalId": "test@example.com",
     *   "content": "배정 관련 문의드립니다."
     * }
     */
    @PostMapping("/simulate/inbound")
    public Mono<ResponseEntity<CsBotResponseDto>> simulateInbound(
            @RequestBody CsInboundRequestDto request) {
        log.info("[CS-SIM] 시뮬레이션 문의 수신: source={}, externalId={}", request.getSource(), request.getExternalId());
        return csBotService.processInbound(request)
                .map(result -> ResponseEntity.status(HttpStatus.CREATED).body(result))
                .onErrorResume(e -> {
                    log.error("[CS-SIM] 시뮬레이션 처리 오류: {}", e.getMessage(), e);
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
                });
    }

    // -------------------------------------------------------------------------
    // FAQ 관리 API
    // -------------------------------------------------------------------------

    @GetMapping("/faq")
    public Flux<CsFaq> listFaq() {
        return faqRepository.findByUseYn("Y");
    }

    @PostMapping("/faq")
    public Mono<ResponseEntity<CsFaq>> createFaq(@RequestBody CsFaq faq) {
        faq.setCsFaqId(IdGenerator.generate(IdGenerator.CS_FAQ));
        faq.setUseYn("Y");
        faq.setCreatedAt(LocalDateTime.now());
        faq.setUpdatedAt(LocalDateTime.now());
        faq.setNew(true);
        return faqRepository.save(faq)
                .map(saved -> ResponseEntity.status(HttpStatus.CREATED).body(saved));
    }

    @PutMapping("/faq/{id}")
    public Mono<ResponseEntity<CsFaq>> updateFaq(@PathVariable String id, @RequestBody CsFaq faq) {
        return faqRepository.findById(id)
                .flatMap(existing -> {
                    existing.setCategory(faq.getCategory());
                    existing.setQuestion(faq.getQuestion());
                    existing.setAnswer(faq.getAnswer());
                    existing.setTags(faq.getTags());
                    existing.setUseYn(faq.getUseYn());
                    existing.setUpdatedAt(LocalDateTime.now());
                    return faqRepository.save(existing);
                })
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/faq/{id}")
    public Mono<ResponseEntity<Void>> deleteFaq(@PathVariable String id) {
        return faqRepository.findById(id)
                .flatMap(faq -> {
                    faq.setUseYn("N");
                    faq.setUpdatedAt(LocalDateTime.now());
                    return faqRepository.save(faq);
                })
                .map(v -> ResponseEntity.<Void>ok().build())
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    // -------------------------------------------------------------------------
    // 상담 세션 & 메시지 조회 API
    // -------------------------------------------------------------------------

    @GetMapping("/conversations")
    public Flux<?> listConversations() {
        return conversationRepository.findAll();
    }

    @GetMapping("/conversations/{id}/messages")
    public Flux<?> listMessages(@PathVariable String id) {
        return messageRepository.findByCsConvIdOrderByCreatedAtAsc(id);
    }
}
