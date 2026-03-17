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
    
    private String id;
    private String agentRefId;
    private AgentStatus status;
    private String apiKey;
}
