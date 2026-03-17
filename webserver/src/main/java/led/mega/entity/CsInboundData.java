package led.mega.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;
import java.time.LocalDateTime;

@Table("cs_inbound_data")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CsInboundData {

    @Id
    @Column("cs_inbound_id")
    private String csInboundId;

    private String source; // EMAIL, REDMINE, TALKDREAM

    @Column("external_ref_id")
    private String externalRefId; // 메일 UID, 레드마인 번호 등

    @Column("raw_payload")
    private String rawPayload; // 원본 데이터

    private String status; // RECEIVED, PROCESSED, FAILED

    @Column("error_message")
    private String errorMessage;

    @Column("received_at")
    private LocalDateTime receivedAt;

    @Column("processed_at")
    private LocalDateTime processedAt;
}
