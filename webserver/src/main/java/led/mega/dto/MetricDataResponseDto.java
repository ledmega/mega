package led.mega.dto;

import led.mega.entity.MetricType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MetricDataResponseDto {
    
    private String id;
    private String agentId;
    private String taskId;
    private MetricType metricType;
    private String metricName;
    private BigDecimal metricValue;
    private String unit;
    private String rawData;
    private LocalDateTime collectedAt;
    private LocalDateTime createdAt;
}
