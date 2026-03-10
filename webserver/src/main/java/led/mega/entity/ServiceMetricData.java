package led.mega.entity;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Table("service_metric_data")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ServiceMetricData {

    @Id
    private Long id;
    private Long agentId;
    private Long monitoringConfigId;
    
    private BigDecimal cpuUsagePercent;
    private BigDecimal memoryUsageMb;
    private BigDecimal memoryUsagePercent;
    private BigDecimal diskUsagePercent;
    private Long networkRxBytes;
    private Long networkTxBytes;
    
    private LocalDateTime collectedAt;
    
    @CreatedDate
    private LocalDateTime createdAt;
}
