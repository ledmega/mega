package led.mega.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExceptionLogResponseDto {
    
    private String id;
    private String agentId;
    private String taskId;
    private String logFilePath;
    private String exceptionType;
    private String exceptionMessage;
    private String contextBefore;
    private String contextAfter;
    private String fullStackTrace;
    private LocalDateTime occurredAt;
    private LocalDateTime createdAt;
}
