package led.mega.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;
import java.time.LocalDateTime;

@Table("cs_conversation")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CsConversation {

    @Id
    @Column("cs_conv_id")
    private String csConvId;

    @Column("external_id")
    private String externalId; // 톡드림 ID, 이메일 주소 등 발신 식별자

    private String channel; // TALKDREAM, EMAIL, PORTAL

    private String status; // PENDING(대기), PROCESSING(처리중), COMPLETED(완료)

    private String summary; // AI가 요약한 내용

    @Column("created_at")
    private LocalDateTime createdAt;

    @Column("updated_at")
    private LocalDateTime updatedAt;
}
