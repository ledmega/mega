package led.mega.entity;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
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
    @Column("ex_log_id")
    private String exLogId;

    @Column("agent_id")
    private String agentId;
    @Column("task_id")
    private String taskId;
    @Column("monitoring_config_id")
    private String monitoringConfigId;

    @Column("log_file_path")
    private String logFilePath;
    @Column("exception_type")
    private String exceptionType;
    @Column("exception_message")
    private String exceptionMessage;
    @Column("context_before")
    private String contextBefore;
    @Column("context_after")
    private String contextAfter;
    @Column("full_stack_trace")
    private String fullStackTrace;
    @Column("occurred_at")
    private LocalDateTime occurredAt;

    @CreatedDate
    @Column("created_at")
    private LocalDateTime createdAt;
}
