package led.mega.cs.service;

import led.mega.cs.dto.CsBotResponseDto;
import led.mega.cs.dto.CsInboundRequestDto;
import led.mega.entity.CsConversation;
import led.mega.entity.CsFaq;
import led.mega.entity.CsInboundData;
import led.mega.entity.CsMessage;
import led.mega.repository.CsConversationRepository;
import led.mega.repository.CsFaqRepository;
import led.mega.repository.CsInboundDataRepository;
import led.mega.repository.CsMessageRepository;
import led.mega.service.SseService;
import led.mega.util.IdGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.LocalDateTime;

/**
 * CS 메시징 AI 자동화의 핵심 엔진.
 * <p>
 * 처리 흐름:
 * 1. 수신 데이터를 cs_inbound_data 에 Raw 저장 (감사 추적)
 * 2. 기존 열린 상담 세션이 있으면 재사용, 없으면 신규 세션 생성
 * 3. 사용자 메시지를 cs_message 에 저장
 * 4. FAQ 키워드 매칭 시도
 *    - 매칭 성공: 즉시 BOT 답변 저장 → AUTO_REPLIED
 *    - 매칭 실패: OpenAI 호출 → 초안 메시지 저장 → DRAFT_CREATED
 * 5. SSE를 통해 관리자 페이지에 실시간 알림
 *
 * [중요] Spring AI의 ChatClient.call().content()는 blocking 호출이므로
 * Reactor 스레드(epoll)에서 직접 호출하면 IllegalStateException 발생.
 * → Mono.fromCallable(...).subscribeOn(Schedulers.boundedElastic()) 으로 오프로드.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CsBotService {

    private final CsFaqRepository faqRepository;
    private final CsConversationRepository conversationRepository;
    private final CsMessageRepository messageRepository;
    private final CsInboundDataRepository inboundDataRepository;
    private final SseService sseService;
    private final ChatClient chatClient;

    /**
     * 외부 문의 유입 시 호출되는 메인 엔트리포인트.
     */
    public Mono<CsBotResponseDto> processInbound(CsInboundRequestDto request) {
        log.info("[CS-BOT] 문의 수신 - source={}, externalId={}", request.getSource(), request.getExternalId());

        CsInboundData inboundData = CsInboundData.builder()
                .csInboundId(IdGenerator.generate(IdGenerator.CS_INBOUND))
                .source(request.getSource())
                .externalRefId(request.getExternalRefId())
                .rawPayload(request.getRawPayload() != null ? request.getRawPayload() : request.getContent())
                .status("RECEIVED")
                .receivedAt(LocalDateTime.now())
                .isNew(true)
                .build();

        return inboundDataRepository.save(inboundData)
                .flatMap(saved -> resolveOrCreateConversation(request))
                .flatMap(conversation -> processMessage(conversation, request));
    }

    /**
     * 기존 열린 상담 세션을 찾거나 신규 세션을 생성합니다.
     */
    private Mono<CsConversation> resolveOrCreateConversation(CsInboundRequestDto request) {
        return conversationRepository.findByExternalIdAndStatusNot(request.getExternalId(), "COMPLETED")
                .switchIfEmpty(
                        Mono.defer(() -> {
                            CsConversation newConv = CsConversation.builder()
                                    .csConvId(IdGenerator.generate(IdGenerator.CS_CONV))
                                    .externalId(request.getExternalId())
                                    .channel(request.getSource())
                                    .status("PENDING")
                                    .createdAt(LocalDateTime.now())
                                    .updatedAt(LocalDateTime.now())
                                    .isNew(true)
                                    .build();
                            log.info("[CS-BOT] 신규 상담 세션 생성: convId={}", newConv.getCsConvId());
                            return conversationRepository.save(newConv);
                        })
                );
    }

    /**
     * 사용자 메시지를 저장하고, FAQ 매칭 또는 AI 초안을 생성합니다.
     */
    private Mono<CsBotResponseDto> processMessage(CsConversation conversation, CsInboundRequestDto request) {
        CsMessage userMessage = CsMessage.builder()
                .csMsgId(IdGenerator.generate(IdGenerator.CS_MSG))
                .csConvId(conversation.getCsConvId())
                .senderType("USER")
                .content(request.getContent())
                .isDraft(false)
                .createdAt(LocalDateTime.now())
                .isNew(true)
                .build();

        return messageRepository.save(userMessage)
                .flatMap(saved -> tryFaqMatch(conversation, request.getContent()))
                .doOnNext(result -> {
                    sseService.broadcastCsEvent(conversation.getCsConvId(), result);
                    log.info("[CS-BOT] 처리 완료: convId={}, resultType={}, faqMatched={}",
                            conversation.getCsConvId(), result.getResultType(), result.isFaqMatched());
                });
    }

    /**
     * FAQ 키워드 매칭을 먼저 시도하고, 실패하면 OpenAI에 요청합니다.
     */
    private Mono<CsBotResponseDto> tryFaqMatch(CsConversation conversation, String question) {
        String keyword = extractPrimaryKeyword(question);

        return faqRepository.searchFaq(keyword)
                .next()
                .flatMap(faq -> handleFaqMatch(conversation, question, faq))
                .switchIfEmpty(Mono.defer(() -> handleAiDraft(conversation, question)));
    }

    /**
     * FAQ 매칭 성공 시: BOT 메시지 저장 후 AUTO_REPLIED 반환
     */
    private Mono<CsBotResponseDto> handleFaqMatch(CsConversation conversation, String question, CsFaq faq) {
        log.info("[CS-BOT] FAQ 매칭 성공: faqId={}, category={}", faq.getCsFaqId(), faq.getCategory());

        CsMessage botMessage = CsMessage.builder()
                .csMsgId(IdGenerator.generate(IdGenerator.CS_MSG))
                .csConvId(conversation.getCsConvId())
                .senderType("BOT")
                .content(faq.getAnswer())
                .isDraft(false)
                .createdAt(LocalDateTime.now())
                .isNew(true)
                .build();

        return messageRepository.save(botMessage)
                .then(updateConversationStatus(conversation, "COMPLETED"))
                .thenReturn(CsBotResponseDto.builder()
                        .conversationId(conversation.getCsConvId())
                        .resultType("AUTO_REPLIED")
                        .botReply(faq.getAnswer())
                        .faqMatched(true)
                        .matchedFaqId(faq.getCsFaqId())
                        .aiProcessed(false)
                        .statusMessage("FAQ 자동 답변이 완료되었습니다.")
                        .build());
    }

    /**
     * FAQ 매칭 실패 시: OpenAI 호출로 답변 초안 생성.
     *
     * ✅ chatClient.call().content()는 blocking 호출이므로
     *    Mono.fromCallable + subscribeOn(Schedulers.boundedElastic())으로
     *    Reactor epoll 스레드 밖에서 실행합니다.
     */
    private Mono<CsBotResponseDto> handleAiDraft(CsConversation conversation, String question) {
        log.info("[CS-BOT] FAQ 매칭 실패. AI 초안 생성 시작: convId={}", conversation.getCsConvId());

        return faqRepository.findByUseYn("Y")
                .collectList()
                .flatMap(faqList -> {
                    StringBuilder faqContext = new StringBuilder();
                    faqList.stream().limit(10).forEach(faq ->
                            faqContext.append("Q: ").append(faq.getQuestion())
                                    .append("\nA: ").append(faq.getAnswer())
                                    .append("\n\n")
                    );

                    String systemPrompt = """
                            당신은 CS(고객 서비스) 상담사를 지원하는 AI 어시스턴트입니다.
                            다음은 우리 서비스의 FAQ 데이터입니다. 이를 참고하여 고객 문의에 대한 답변 초안을 작성해주세요.
                            답변은 친절하고 전문적으로 작성하되, 확실하지 않은 내용은 "담당자 확인 후 안내드리겠습니다."로 처리하세요.

                            [FAQ 참고 데이터]
                            %s
                            """.formatted(faqContext.toString().isBlank() ? "현재 등록된 FAQ가 없습니다." : faqContext);

                    // ✅ blocking 호출 → boundedElastic 스레드 풀에서 실행
                    return Mono.fromCallable(() ->
                                    chatClient.prompt()
                                            .system(systemPrompt)
                                            .user(question)
                                            .call()
                                            .content()
                            )
                            .subscribeOn(Schedulers.boundedElastic())
                            .flatMap(aiDraft -> {
                                CsMessage draftMessage = CsMessage.builder()
                                        .csMsgId(IdGenerator.generate(IdGenerator.CS_MSG))
                                        .csConvId(conversation.getCsConvId())
                                        .senderType("BOT")
                                        .content(aiDraft)
                                        .isDraft(true)
                                        .createdAt(LocalDateTime.now())
                                        .isNew(true)
                                        .build();

                                return messageRepository.save(draftMessage)
                                        .then(updateConversationStatus(conversation, "PROCESSING"))
                                        .thenReturn(CsBotResponseDto.builder()
                                                .conversationId(conversation.getCsConvId())
                                                .resultType("DRAFT_CREATED")
                                                .botReply(aiDraft)
                                                .faqMatched(false)
                                                .aiProcessed(true)
                                                .statusMessage("AI 초안이 생성되었습니다. 상담사 검토 후 발송해주세요.")
                                                .build());
                            })
                            .onErrorResume(e -> {
                                log.error("[CS-BOT] OpenAI 호출 실패: {}", e.getMessage());
                                return updateConversationStatus(conversation, "PROCESSING")
                                        .thenReturn(CsBotResponseDto.builder()
                                                .conversationId(conversation.getCsConvId())
                                                .resultType("ESCALATED")
                                                .botReply(null)
                                                .faqMatched(false)
                                                .aiProcessed(false)
                                                .statusMessage("AI 처리 실패. 상담사에게 직접 배정되었습니다.")
                                                .build());
                            });
                });
    }

    /**
     * 상담 세션 상태를 업데이트합니다.
     * isNew=false → R2DBC가 UPDATE 쿼리로 처리
     */
    private Mono<CsConversation> updateConversationStatus(CsConversation conversation, String newStatus) {
        conversation.setStatus(newStatus);
        conversation.setUpdatedAt(LocalDateTime.now());
        conversation.setNew(false); // 기존 레코드 → UPDATE
        return conversationRepository.save(conversation);
    }

    /**
     * 질문에서 핵심 키워드를 추출합니다.
     */
    private String extractPrimaryKeyword(String question) {
        if (question == null || question.isBlank()) return "";
        String[] tokens = question.split("[\\s\\p{Punct}]+");
        for (String token : tokens) {
            if (token.length() >= 2) return token;
        }
        return question.substring(0, Math.min(question.length(), 10));
    }
}
