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

    // 최근 24시간 기준 서비스 예외 건수 (서비스 목록 화면용)
    private Long   recentExceptionCount;
    // 서비스별 최근 메트릭 (목록/상세 표시용)
    private java.math.BigDecimal recentCpu;     // %
    private java.math.BigDecimal recentMemory; // MB 또는 %
    private java.math.BigDecimal recentDisk;   // % 또는 GB

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
