package led.mega.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ServiceMetricDataRequestDto {
    @NotNull(message = "모니터링 설정 ID는 필수입니다")
    private String monitoringConfigId;
    
    // 수집된 단위 지표들
    private BigDecimal cpuUsagePercent;
    private BigDecimal memoryUsageMb;
    private BigDecimal memoryUsagePercent;
    private BigDecimal diskUsagePercent;
    private Long networkRxBytes;
    private Long networkTxBytes;
    
    @NotNull(message = "수집 시간은 필수입니다")
    private LocalDateTime collectedAt;
}
