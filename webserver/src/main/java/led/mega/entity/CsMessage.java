package led.mega.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;
import java.time.LocalDateTime;

@Table("cs_message")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CsMessage {

    @Id
    @Column("cs_msg_id")
    private String csMsgId;

    @Column("cs_conv_id")
    private String csConvId;

    @Column("sender_type")
    private String senderType; // USER, BOT, ADMIN

    private String content;

    @Column("is_draft")
    private boolean isDraft; // 관리자용 추천 답변 초안 여부

    @Column("created_at")
    private LocalDateTime createdAt;
}
