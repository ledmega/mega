package led.mega.entity;

// [REACTIVE] JPA → R2DBC 전환
// - @ManyToOne Agent agent → Long agentId (FK를 Long ID로 단순화)
// - R2DBC에서 관계 엔티티 참조 불가 → 순수 Long FK만 보관

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Table("agent_heartbeat")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AgentHeartbeat {

    @Id
    private Long id;

    // [CHANGED] @ManyToOne Agent agent → Long agentId (FK 컬럼 직접 매핑)
    private Long agentId;

    private AgentStatus status;
    private LocalDateTime heartbeatAt;

    @CreatedDate
    private LocalDateTime createdAt;
}

