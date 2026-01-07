package led.mega.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class HeartbeatRequestDto {
    
    @NotBlank(message = "상태는 필수입니다")
    @Size(max = 20, message = "상태는 20자 이하여야 합니다")
    private String status;  // ONLINE, OFFLINE
    
    @NotNull(message = "하트비트 시간은 필수입니다")
    private LocalDateTime heartbeatAt;
}

