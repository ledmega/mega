package led.mega.dto;

import led.mega.entity.AgentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentResponseDto {
    
    private String id;
    private String agentRefId; // 기존 agentId (고유 식별코드)
    private String name;
    private String hostname;
    private String ipAddress;
    private String osType;
    private AgentStatus status;
    private LocalDateTime lastHeartbeat;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
