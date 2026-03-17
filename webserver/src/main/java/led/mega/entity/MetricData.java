package led.mega.entity;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Table("metric_data")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MetricData implements Persistable<String> {

    @Id
    @Column("metric_id")
    private String metricId;

    @Transient
    @Builder.Default
    private boolean isNew = false;

    @Column("agent_id")
    private String agentId;
    @Column("task_id")
    private String taskId;
    @Column("monitoring_config_id")
    private String monitoringConfigId;

    @Column("metric_type")
    private MetricType metricType;
    @Column("metric_name")
    private String metricName;
    @Column("metric_value")
    private BigDecimal metricValue;
    private String unit;
    @Column("raw_data")
    private String rawData;
    @Column("collected_at")
    private LocalDateTime collectedAt;

    @CreatedDate
    @Column("created_at")
    private LocalDateTime createdAt;

    @Override
    public String getId() {
        return metricId;
    }

    @Override
    public boolean isNew() {
        return isNew || metricId == null;
    }
}
