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
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.LocalDateTime;
import java.util.List;

/**
 * CS 메시징 AI 자동화 엔진.
 * 모든 Spring AI 버전에서 호환되는 OpenAiChatModel 기반 호출 방식.
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
    private final OpenAiChatModel chatModel;

    public Mono<CsBotResponseDto> processInbound(CsInboundRequestDto request) {
        return inboundDataRepository.save(createInboundEntity(request))
                .flatMap(saved -> resolveOrCreateConversation(request))
                .flatMap(conversation -> processMessage(conversation, request));
    }

    private CsInboundData createInboundEntity(CsInboundRequestDto request) {
        return CsInboundData.builder()
                .csInboundId(IdGenerator.generate(IdGenerator.CS_INBOUND))
                .source(request.getSource())
                .externalRefId(request.getExternalRefId())
                .rawPayload(request.getContent())
                .status("RECEIVED")
                .receivedAt(LocalDateTime.now())
                .isNew(true)
                .build();
    }

    private Mono<CsConversation> resolveOrCreateConversation(CsInboundRequestDto request) {
        return conversationRepository.findByExternalIdAndStatusNot(request.getExternalId(), "COMPLETED")
                .switchIfEmpty(Mono.defer(() -> conversationRepository.save(CsConversation.builder()
                        .csConvId(IdGenerator.generate(IdGenerator.CS_CONV))
                        .externalId(request.getExternalId())
                        .channel(request.getSource())
                        .status("PENDING")
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .isNew(true)
                        .build())));
    }

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
                .doOnNext(result -> sseService.broadcastCsEvent(conversation.getCsConvId(), result));
    }

    private Mono<CsBotResponseDto> tryFaqMatch(CsConversation conversation, String question) {
        return faqRepository.searchFaq(extractPrimaryKeyword(question))
                .next()
                .flatMap(faq -> handleFaqMatch(conversation, question, faq))
                .switchIfEmpty(Mono.defer(() -> handleAiDraft(conversation, question)));
    }

    private Mono<CsBotResponseDto> handleFaqMatch(CsConversation conversation, String question, CsFaq faq) {
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

    private Mono<CsBotResponseDto> handleAiDraft(CsConversation conversation, String question) {
        log.info("[CS-BOT] AI 초안 생성 시작: convId={}", conversation.getCsConvId());

        return faqRepository.findByUseYn("Y")
                .collectList()
                .flatMap(faqList -> {
                    String systemPrompt = "당신은 CS 상담사 지원 AI입니다. FAQ를 참고해 답변 초안을 작성하세요.\n\n[FAQ]\n" + faqList;

                    return Mono.fromCallable(() -> {
                                Prompt prompt = new Prompt(List.of(new SystemMessage(systemPrompt), new UserMessage(question)));
                                return chatModel.call(prompt).getResult().getOutput().getContent();
                            })
                            .subscribeOn(Schedulers.boundedElastic())
                            .flatMap(aiDraft -> {
                                CsMessage draft = CsMessage.builder()
                                        .csMsgId(IdGenerator.generate(IdGenerator.CS_MSG))
                                        .csConvId(conversation.getCsConvId())
                                        .senderType("BOT")
                                        .content(aiDraft)
                                        .isDraft(true)
                                        .createdAt(LocalDateTime.now())
                                        .isNew(true)
                                        .build();

                                return messageRepository.save(draft)
                                        .then(updateConversationStatus(conversation, "PROCESSING"))
                                        .thenReturn(CsBotResponseDto.builder()
                                                .conversationId(conversation.getCsConvId())
                                                .resultType("DRAFT_CREATED")
                                                .botReply(aiDraft)
                                                .faqMatched(false)
                                                .aiProcessed(true)
                                                .statusMessage("AI 초안 생성 완료")
                                                .build());
                            })
                            .onErrorResume(e -> {
                                log.error("[CS-BOT] AI 호출 에러: {}", e.getMessage());
                                return Mono.just(CsBotResponseDto.builder()
                                        .conversationId(conversation.getCsConvId())
                                        .resultType("ESCALATED")
                                        .statusMessage("상담사 직접 배정").build());
                            });
                });
    }

    private Mono<CsConversation> updateConversationStatus(CsConversation conversation, String newStatus) {
        conversation.setStatus(newStatus);
        conversation.setUpdatedAt(LocalDateTime.now());
        conversation.setNew(false);
        return conversationRepository.save(conversation);
    }

    private String extractPrimaryKeyword(String question) {
        return (question == null || question.isBlank()) ? "" : question.substring(0, Math.min(question.length(), 5));
    }
}
