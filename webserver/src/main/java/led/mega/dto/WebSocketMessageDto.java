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
public class WebSocketMessageDto {
    
    private String type;  // METRIC, EXCEPTION, HEARTBEAT, AGENT_STATUS
    private Long agentId;
    private String agentIdStr;
    private Object data;
    private LocalDateTime timestamp;
}

