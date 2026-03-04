package led.mega.entity;

// [REACTIVE] JPA → R2DBC 전환
// - @ManyToOne Agent agent → Long agentId

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Table("task")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Task {

    @Id
    private Long id;

    // [CHANGED] @ManyToOne Agent agent → Long agentId
    private Long agentId;

    private String taskName;
    private TaskType taskType;
    private String command;
    private String logPath;
    private String logPattern;
    private Integer intervalSeconds;

    @Builder.Default
    private Boolean enabled = true;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}

