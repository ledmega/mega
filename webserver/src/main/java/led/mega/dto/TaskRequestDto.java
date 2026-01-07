package led.mega.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import led.mega.entity.TaskType;
import lombok.Data;

@Data
public class TaskRequestDto {
    
    @NotBlank(message = "작업 이름은 필수입니다")
    @Size(max = 100, message = "작업 이름은 100자 이하여야 합니다")
    private String taskName;
    
    @NotNull(message = "작업 타입은 필수입니다")
    private TaskType taskType;
    
    @Size(max = 500, message = "명령어는 500자 이하여야 합니다")
    private String command;
    
    @Size(max = 500, message = "로그 파일 경로는 500자 이하여야 합니다")
    private String logPath;
    
    @Size(max = 500, message = "로그 패턴은 500자 이하여야 합니다")
    private String logPattern;
    
    @NotNull(message = "실행 주기는 필수입니다")
    @Positive(message = "실행 주기는 양수여야 합니다")
    private Integer intervalSeconds;
    
    private Boolean enabled = true;
}

