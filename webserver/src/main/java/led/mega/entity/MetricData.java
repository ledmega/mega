package led.mega.entity;

// [REACTIVE] JPA → R2DBC 전환
// - @ManyToOne Agent agent → Long agentId
// - @ManyToOne Task task   → Long taskId

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Table("metric_data")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MetricData {

    @Id
    private Long id;

    // [CHANGED] @ManyToOne Agent agent → Long agentId
    private Long agentId;
    // [CHANGED] @ManyToOne Task task   → Long taskId
    private Long taskId;

    private MetricType metricType;
    private String metricName;
    private BigDecimal metricValue;
    private String unit;
    private String rawData;
    private LocalDateTime collectedAt;

    @CreatedDate
    private LocalDateTime createdAt;
}

