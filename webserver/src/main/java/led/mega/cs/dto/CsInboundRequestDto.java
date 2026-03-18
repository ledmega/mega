package led.mega.cs.dto;

import lombok.Data;

/**
 * 외부(또는 시뮬레이터)로부터 CS 문의가 유입될 때 사용하는 공통 요청 DTO.
 * source: EMAIL | TALKDREAM | PORTAL
 */
@Data
public class CsInboundRequestDto {

    /** 수신 채널  예) EMAIL, TALKDREAM, PORTAL */
    private String source;

    /** 외부 식별자: 이메일 주소, 카카오 유저 ID 등 */
    private String externalId;

    /** 외부 참조 ID: 메일 UID, 레드마인 이슈 번호 등 (없으면 null) */
    private String externalRefId;

    /** 문의 본문 */
    private String content;

    /** 첨부파일 목록 (메일 첨부파일명 등) */
    private String attachments;

    /** 원본 JSON payload (웹훅 수신 시 그대로 저장) */
    private String rawPayload;
}
