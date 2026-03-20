package led.mega.entity;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Table("os_monitoring_config")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class OsMonitoringConfig implements Persistable<String> {

    @Id
    @Column("config_id")
    private String configId;

    @Transient
    @Builder.Default
    private boolean isNew = false;

    @Column("agent_id")
    private String agentId;

    @Column("metric_type")
    private String metricType;

    @Column("metric_name")
    @NonNull
    private String metricName;

    @Builder.Default
    @Column("interval_seconds")
    private Integer intervalSeconds = 60;

    @Builder.Default
    @Column("collect_yn")
    private String collectYn = "Y";

    @Builder.Default
    @Column("dashboard_yn")
    private String dashboardYn = "Y";

    @Column("threshold_value")
    private BigDecimal thresholdValue;

    @Column("threshold_type")
    private String thresholdType;

    @Builder.Default
    @Column("alert_yn")
    private String alertYn = "N";

    @Column("options")
    private String options;

    @Builder.Default
    @Column("enabled")
    private Boolean enabled = true;

    @Column("description")
    private String description;

    @CreatedDate
    @Column("created_at")
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column("updated_at")
    private LocalDateTime updatedAt;

    @Override
    public String getId() {
        return configId;
    }

    @Override
    public boolean isNew() {
        return isNew || configId == null;
    }
}
