package led.mega.entity;

// [REACTIVE] JPA → R2DBC 전환
// - @Entity, @GeneratedValue, @Column, @Enumerated 제거 (jakarta.persistence.*)
// - @Table, @Id → Spring Data R2DBC 어노테이션 사용
// - @CreationTimestamp, @UpdateTimestamp → @CreatedDate, @LastModifiedDate (Spring Data)
// - @OneToMany 컬렉션 전부 제거 (R2DBC는 관계 매핑 미지원 - 서비스 레이어에서 별도 조회)

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Table("agent")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Agent {

    @Id
    private Long id;

    private String agentId;
    private String name;
    private String hostname;
    private String ipAddress;
    private String osType;

    @Builder.Default
    private AgentStatus status = AgentStatus.OFFLINE;

    private LocalDateTime lastHeartbeat;
    private String apiKey;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    // [REMOVED] @OneToMany tasks, metricDataList, exceptionLogs, heartbeats
    // R2DBC는 관계 매핑 지원 안 함 → 필요 시 Repository에서 별도 조회
}

