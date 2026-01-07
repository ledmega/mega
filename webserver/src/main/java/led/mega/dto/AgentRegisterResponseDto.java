package led.mega.dto;

import led.mega.entity.AgentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentRegisterResponseDto {
    
    private Long id;
    private String agentId;
    private AgentStatus status;
    private String apiKey;
}

