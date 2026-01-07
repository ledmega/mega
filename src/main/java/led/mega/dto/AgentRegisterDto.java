package led.mega.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AgentRegisterDto {
    
    @NotBlank(message = "에이전트 ID는 필수입니다")
    @Size(max = 100, message = "에이전트 ID는 100자 이하여야 합니다")
    private String agentId;
    
    @NotBlank(message = "에이전트 이름은 필수입니다")
    @Size(max = 100, message = "에이전트 이름은 100자 이하여야 합니다")
    private String name;
    
    @Size(max = 255, message = "호스트명은 255자 이하여야 합니다")
    private String hostname;
    
    @Size(max = 50, message = "IP 주소는 50자 이하여야 합니다")
    private String ipAddress;
    
    @Size(max = 50, message = "OS 타입은 50자 이하여야 합니다")
    private String osType;
}

