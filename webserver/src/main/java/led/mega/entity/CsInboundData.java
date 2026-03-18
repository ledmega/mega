package led.mega.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;
import java.time.LocalDateTime;

@Table("cs_inbound_data")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CsInboundData implements Persistable<String> {

    @Id
    @Column("cs_inbound_id")
    private String csInboundId;

    @Transient
    @Builder.Default
    private boolean isNew = false;

    private String source; // EMAIL, REDMINE, TALKDREAM

    @Column("external_ref_id")
    private String externalRefId; // 메일 UID, 레드마인 번호 등

    @Column("raw_payload")
    private String rawPayload; // 원본 데이터

    @Column("resolved_payload")
    private String resolvedPayload; // 최종 답변 또는 처리 결과

    @Column("processing_history")
    private String processingHistory; // 처리 과정 이력

    private String status; // RECEIVED, PROCESSED, FAILED

    @Column("error_message")
    private String errorMessage;

    @Column("received_at")
    private LocalDateTime receivedAt;

    @Column("processed_at")
    private LocalDateTime processedAt;

    @Override
    public String getId() {
        return csInboundId;
    }

    @Override
    public boolean isNew() {
        return isNew || csInboundId == null;
    }
}
