package led.mega.cs.dto;

import lombok.Builder;
import lombok.Data;

/**
 * CsBotService 처리 결과를 담는 응답 DTO.
 */
@Data
@Builder
public class CsBotResponseDto {

    /** 상담 세션 ID */
    private String conversationId;

    /** 처리 결과 타입: AUTO_REPLIED(자동 답변), DRAFT_CREATED(초안 생성), ESCALATED(인계) */
    private String resultType;

    /** 봇이 생성한 답변 또는 초안 내용 */
    private String botReply;

    /** FAQ 매칭 여부 */
    private boolean faqMatched;

    /** 매칭된 FAQ ID (없으면 null) */
    private String matchedFaqId;

    /** AI 처리 여부 */
    private boolean aiProcessed;

    /** 처리 상태 메시지 */
    private String statusMessage;
}
