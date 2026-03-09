package led.mega.dto;

import lombok.*;
import java.time.LocalDateTime;

/** 서비스 모니터링 설정 요청/응답 DTO */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MonitoringConfigDto {

    private Long   id;
    private Long   agentId;
    private String agentName;      // 화면 표시용 (조인 없이 서비스에서 채움)

    private String serviceName;
    private String servicePath;
    private String logPath;
    private String collectItems;   // CSV: CPU,MEMORY,DISK,LOG
    private String logKeywords;    // CSV: Error,404,Exception
    private Integer intervalSeconds;
    private Boolean enabled;
    private String description;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
