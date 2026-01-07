package led.mega.dto;

import led.mega.entity.TaskType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskResponseDto {
    
    private Long id;
    private Long agentId;
    private String taskName;
    private TaskType taskType;
    private String command;
    private String logPath;
    private String logPattern;
    private Integer intervalSeconds;
    private Boolean enabled;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

