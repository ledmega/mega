package led.mega.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ExceptionLogRequestDto {
    
    private Long taskId;
    
    @Size(max = 500, message = "로그 파일 경로는 500자 이하여야 합니다")
    private String logFilePath;
    
    @Size(max = 200, message = "Exception 타입은 200자 이하여야 합니다")
    private String exceptionType;
    
    private String exceptionMessage;
    
    private String contextBefore;  // 위 5줄
    
    private String contextAfter;    // 아래 5줄
    
    private String fullStackTrace;
    
    @NotNull(message = "발생 시간은 필수입니다")
    private LocalDateTime occurredAt;
}

