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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import java.time.Duration;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class CsBotService {

    private final CsFaqRepository faqRepository;
    private final CsConversationRepository conversationRepository;
    private final CsMessageRepository messageRepository;
    private final CsInboundDataRepository inboundDataRepository;
    private final SseService sseService;
    private final WebClient.Builder webClientBuilder;

    @Value("${spring.ai.openai.api-key}")
    private String apiKey;

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
                .attachments(request.getAttachments())
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
        CsMessage userMsg = CsMessage.builder()
                .csMsgId(IdGenerator.generate(IdGenerator.CS_MSG))
                .csConvId(conversation.getCsConvId())
                .senderType("USER")
                .content(request.getContent())
                .isDraft(false)
                .createdAt(LocalDateTime.now())
                .isNew(true)
                .build();

        return messageRepository.save(userMsg)
                .flatMap(saved -> tryFaqMatch(conversation, request.getContent()))
                .doOnNext(result -> sseService.broadcastCsEvent(conversation.getCsConvId(), result));
    }

    private Mono<CsBotResponseDto> tryFaqMatch(CsConversation conversation, String question) {
        return faqRepository.searchFaq(question.substring(0, Math.min(question.length(), 5)))
                .next()
                .flatMap(faq -> handleFaqMatch(conversation, question, faq))
                .switchIfEmpty(Mono.defer(() -> handleAiDraft(conversation, question)));
    }

    private Mono<CsBotResponseDto> handleFaqMatch(CsConversation conversation, String question, CsFaq faq) {
        CsMessage botMsg = CsMessage.builder()
                .csMsgId(IdGenerator.generate(IdGenerator.CS_MSG))
                .csConvId(conversation.getCsConvId())
                .senderType("BOT")
                .content(faq.getAnswer())
                .isDraft(false)
                .createdAt(LocalDateTime.now())
                .isNew(true).build();

        return messageRepository.save(botMsg)
                .then(updateConversationStatus(conversation, "COMPLETED"))
                .thenReturn(CsBotResponseDto.builder()
                        .conversationId(conversation.getCsConvId())
                        .resultType("AUTO_REPLIED")
                        .botReply(faq.getAnswer())
                        .faqMatched(true).build());
    }

    public Mono<String> generateAiSolution(String question) {
        log.info("[CS-BOT] AI Solution Generation Start: question={}", question);
        String searchKeyword = question.substring(0, Math.min(question.length(), 10));

        return Mono.zip(
                faqRepository.searchFaq(searchKeyword).take(5).collectList(),
                inboundDataRepository.searchInbound(searchKeyword).take(5).collectList()
        ).flatMap(tuple -> {
            List<CsFaq> faqs = tuple.getT1();
            List<CsInboundData> inbounds = tuple.getT2();

            StringBuilder contextBuilder = new StringBuilder();
            if (!faqs.isEmpty()) {
                contextBuilder.append("[공식 FAQ 데이터]\n");
                for (CsFaq f : faqs) {
                    contextBuilder.append(String.format("질문: %s\n답변: %s\n---\n", f.getQuestion(), f.getAnswer()));
                }
            }
            if (!inbounds.isEmpty()) {
                contextBuilder.append("\n[과거 상담/처리 상세 히스토리]\n");
                for (CsInboundData inb : inbounds) {
                    contextBuilder.append(String.format("### [사례: %s]\n", inb.getExternalRefId()));
                    contextBuilder.append(String.format("- 최초 문의 내용: %s\n", inb.getRawPayload()));
                    if (inb.getProcessingHistory() != null && !inb.getProcessingHistory().isBlank()) {
                        contextBuilder.append("- 처리 및 대화 이력:\n").append(inb.getProcessingHistory()).append("\n");
                    }
                    if (inb.getResolvedPayload() != null && !inb.getResolvedPayload().isBlank()) {
                        contextBuilder.append(String.format("- 최종 해결 내용: %s\n", inb.getResolvedPayload()));
                    }
                    if (inb.getAttachments() != null && !inb.getAttachments().isBlank()) {
                        contextBuilder.append(String.format("- 첨부파일 목록: %s\n", inb.getAttachments()));
                    }
                    contextBuilder.append("---\n");
                }
            }

            String systemPrompt = "당신은 CS 상담 시스템 AI 지원 엔진입니다. 제공된 데이터(FAQ, 과거 이력)를 참고하여 답변 초안을 작성하세요.\n" +
                    "1. 공식 FAQ 우선. 2. 과거 히스토리의 해결 흐름 참고. 3. 답변하기 모호하면 상담사 개입 권유.\n\n" +
                    "[참고 지식 베이스]\n" + (contextBuilder.length() == 0 ? "관련 정보 없음" : contextBuilder.toString());

            String url = "https://generativelanguage.googleapis.com/v1beta/openai/v1/chat/completions";
            Map<String, Object> requestBody = Map.of(
                "model", "gemini-flash-latest",
                "messages", List.of(
                    Map.of("role", "system", "content", systemPrompt),
                    Map.of("role", "user", "content", question)
                )
            );

            if (apiKey == null || apiKey.isEmpty()) {
                log.error("[CS-BOT] API Key is missing! .env 파일이나 환경변수(GEMINI_API_KEY)를 확인해주세요.");
                return Mono.error(new RuntimeException("GEMINI_API_KEY is not set"));
            }

            log.info("[CS-BOT] Gemini OpenAI-Compatible API Call start... (model: gemini-flash-latest)");

            return webClientBuilder.build().post().uri(url)
                    .header("Authorization", "Bearer " + apiKey)
                    .header("x-goog-api-key", apiKey)
                    .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody).retrieve().bodyToMono(Map.class)
                    .timeout(Duration.ofSeconds(60))
                    .retryWhen(reactor.util.retry.Retry.backoff(3, Duration.ofSeconds(2))
                            .filter(t -> t instanceof org.springframework.web.reactive.function.client.WebClientResponseException &&
                                    ((org.springframework.web.reactive.function.client.WebClientResponseException) t).getStatusCode().value() == 503))
                    .map(response -> {
                        if (response == null || !response.containsKey("choices")) {
                            log.error("[CS-BOT] 부적절한 응답: {}", response);
                            throw new RuntimeException("Invalid response from Gemini API");
                        }
                        List choices = (List) response.get("choices");
                        Map firstChoice = (Map) choices.get(0);
                        Map messageBody = (Map) firstChoice.get("message");
                        return (String) messageBody.get("content");
                    });
        });
    }

    private Mono<CsBotResponseDto> handleAiDraft(CsConversation conversation, String question) {
        log.info("[CS-BOT] RAG-based AI Draft Start: convId={}", conversation.getCsConvId());

        return generateAiSolution(question)
                .flatMap(aiDraft -> {
                    CsMessage draft = CsMessage.builder()
                            .csMsgId(IdGenerator.generate(IdGenerator.CS_MSG))
                            .csConvId(conversation.getCsConvId())
                            .senderType("BOT")
                            .content(aiDraft)
                            .isDraft(true)
                            .createdAt(LocalDateTime.now())
                            .isNew(true).build();

                    return messageRepository.save(draft)
                            .then(updateConversationStatus(conversation, "PROCESSING"))
                            .thenReturn(CsBotResponseDto.builder()
                                    .conversationId(conversation.getCsConvId())
                                    .resultType("DRAFT_CREATED")
                                    .botReply(aiDraft)
                                    .aiProcessed(true).build());
                })
                .onErrorResume(e -> {
                    log.error("[CS-BOT] AI 호출 실패: {}", e.getMessage());
                    return Mono.just(CsBotResponseDto.builder()
                            .conversationId(conversation.getCsConvId())
                            .resultType("ESCALATED").build());
                });
    }

    private Mono<CsConversation> updateConversationStatus(CsConversation conversation, String newStatus) {
        conversation.setStatus(newStatus);
        conversation.setUpdatedAt(LocalDateTime.now());
        conversation.setNew(false);
        return conversationRepository.save(conversation);
    }
}
