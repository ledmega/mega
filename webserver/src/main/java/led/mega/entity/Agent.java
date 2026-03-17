package led.mega.entity;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Table("agent")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Agent implements Persistable<String> {

    @Id
    @Column("agent_id")
    private String agentId;

    @Transient
    @Builder.Default
    private boolean isNew = false;

    @Column("agent_ref_id")
    private String agentRefId; // 고유 에이전트 식별코드

    private String name;
    private String hostname;
    @Column("ip_address")
    private String ipAddress;
    @Column("os_type")
    private String osType;

    @Builder.Default
    private AgentStatus status = AgentStatus.OFFLINE;

    @Column("last_heartbeat")
    private LocalDateTime lastHeartbeat;
    @Column("api_key")
    private String apiKey;

    @CreatedDate
    @Column("created_at")
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column("updated_at")
    private LocalDateTime updatedAt;

    @Override
    public String getId() {
        return agentId;
    }

    @Override
    public boolean isNew() {
        return isNew || agentId == null;
    }
}
