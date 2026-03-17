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
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

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

    private Mono<CsBotResponseDto> handleAiDraft(CsConversation conversation, String question) {
        log.info("[CS-BOT] Native AI Draft Start: convId={}", conversation.getCsConvId());

        return Mono.fromCallable(() -> {
            RestClient restClient = RestClient.builder().build();
            
            // 1. OpenAI 호환 엔드포인트 URL
            String url = "https://generativelanguage.googleapis.com/v1beta/openai/v1/chat/completions";

            // 2. 모델 리스트에서 확인된 유효한 최신 모델명 사용
            Map<String, Object> requestBody = Map.of(
                "model", "gemini-flash-latest",
                "messages", List.of(
                    Map.of("role", "system", "content", "당신은 CS 상담사 지원 AI입니다. FAQ를 참고해 답변 초안을 작성하세요."),
                    Map.of("role", "user", "content", question)
                )
            );

            log.info("[CS-BOT] Requesting Gemini with model: gemini-flash-latest");
            if (apiKey == null || apiKey.isEmpty()) {
                log.error("[CS-BOT] API Key is missing! Please set GEMINI_API_KEY env variable.");
                throw new RuntimeException("API Key is missing");
            }
            log.info("[CS-BOT] Using API Key starting with: {}...", apiKey.substring(0, Math.min(apiKey.length(), 8)));

            // 3. Authorization 헤더와 x-goog-api-key 헤더를 모두 사용하여 인증 성공률을 극대화함
            Map response = restClient.post()
                    .uri(url)
                    .header("Authorization", "Bearer " + apiKey)
                    .header("x-goog-api-key", apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .body(Map.class);

            if (response == null || !response.containsKey("choices")) {
                throw new RuntimeException("Invalid response from Gemini API");
            }

            List choices = (List) response.get("choices");
            Map firstChoice = (Map) choices.get(0);
            Map message = (Map) firstChoice.get("message");
            return (String) message.get("content");
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
