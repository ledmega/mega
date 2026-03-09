package led.mega.entity;

// [REACTIVE] JPA → R2DBC 전환
// - @ManyToOne Agent agent → Long agentId
// - @ManyToOne Task task   → Long taskId
// - 서비스에서 agentId/taskId로 별도 조회하면 됨

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Table("exception_log")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExceptionLog {

    @Id
    private Long id;

    // [CHANGED] @ManyToOne Agent agent → Long agentId
    private Long agentId;
    // [CHANGED] @ManyToOne Task task   → Long taskId
    private Long taskId;
    /** 서비스 모니터링 설정 ID (서비스별 예외 시 사용) */
    private Long monitoringConfigId;

    private String logFilePath;
    private String exceptionType;
    private String exceptionMessage;
    private String contextBefore;
    private String contextAfter;
    private String fullStackTrace;
    private LocalDateTime occurredAt;

    @CreatedDate
    private LocalDateTime createdAt;
}

