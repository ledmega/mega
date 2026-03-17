package led.mega.entity;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Table("agent_heartbeat")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AgentHeartbeat implements org.springframework.data.domain.Persistable<String> {

    @Id
    @Column("hb_id")
    private String hbId;

    @Column("agent_id")
    private String agentId;

    @org.springframework.data.annotation.Transient
    @Builder.Default
    private boolean isNew = false;

    private AgentStatus status;
    @Column("heartbeat_at")
    private LocalDateTime heartbeatAt;

    @CreatedDate
    @Column("created_at")
    private LocalDateTime createdAt;

    @Override
    public String getId() {
        return hbId;
    }

    @Override
    public boolean isNew() {
        return isNew || hbId == null;
    }
}
