package led.mega.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;
import java.time.LocalDateTime;

@Table("cs_faq")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CsFaq {

    @Id
    @Column("cs_faq_id")
    private String csFaqId;

    private String category; // 배정, 장애, 단순문의 등

    private String question;

    private String answer;

    @Column("use_yn")
    private String useYn; // Y, N

    private String tags; // 검색용 태그 (쉼표 구분)

    @Column("created_at")
    private LocalDateTime createdAt;

    @Column("updated_at")
    private LocalDateTime updatedAt;
}
