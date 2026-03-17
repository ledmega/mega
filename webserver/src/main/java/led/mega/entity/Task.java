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

@Table("task")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Task implements Persistable<String> {

    @Id
    @Column("task_id")
    private String taskId;

    @Transient
    @Builder.Default
    private boolean isNew = false;

    @Column("agent_id")
    private String agentId;

    @Column("task_name")
    private String taskName;
    @Column("task_type")
    private TaskType taskType;
    private String command;
    @Column("log_path")
    private String logPath;
    @Column("log_pattern")
    private String logPattern;
    @Column("interval_seconds")
    private Integer intervalSeconds;

    @Builder.Default
    private Boolean enabled = true;

    @CreatedDate
    @Column("created_at")
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column("updated_at")
    private LocalDateTime updatedAt;

    @Override
    public String getId() {
        return taskId;
    }

    @Override
    public boolean isNew() {
        return isNew || taskId == null;
    }
}
