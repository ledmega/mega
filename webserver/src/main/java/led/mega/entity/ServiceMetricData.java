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

@Table("service_metric_data")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ServiceMetricData implements Persistable<String> {

    @Id
    @Column("svc_metric_id")
    private String svcMetricId;

    @Transient
    @Builder.Default
    private boolean isNew = false;
    
    @Column("agent_id")
    private String agentId;
    @Column("monitoring_config_id")
    private String monitoringConfigId;
    
    @Column("cpu_usage_percent")
    private BigDecimal cpuUsagePercent;
    @Column("memory_usage_mb")
    private BigDecimal memoryUsageMb;
    @Column("memory_usage_percent")
    private BigDecimal memoryUsagePercent;
    @Column("disk_usage_percent")
    private BigDecimal diskUsagePercent;
    @Column("network_rx_bytes")
    private Long networkRxBytes;
    @Column("network_tx_bytes")
    private Long networkTxBytes;
    
    @Column("collected_at")
    private LocalDateTime collectedAt;
    
    @CreatedDate
    @Column("created_at")
    private LocalDateTime createdAt;

    @Override
    public String getId() {
        return svcMetricId;
    }

    @Override
    public boolean isNew() {
        return isNew || svcMetricId == null;
    }
}
