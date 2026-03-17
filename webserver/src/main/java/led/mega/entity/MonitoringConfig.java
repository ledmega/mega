package led.mega.entity;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Table("monitoring_config")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MonitoringConfig {

    @Id
    @Column("config_id")
    private String configId;

    @Column("agent_id")
    private String agentId;

    @Column("service_name")
    private String serviceName;
    @Builder.Default
    @Column("target_type")
    private String targetType = "HOST";
    @Column("target_name")
    private String targetName;
    @Column("service_path")
    private String servicePath;
    @Column("log_path")
    private String logPath;

    @Builder.Default
    @Column("collect_items")
    private String collectItems = "CPU,MEMORY,DISK";

    @Column("log_keywords")
    private String logKeywords;

    @Builder.Default
    @Column("interval_seconds")
    private Integer intervalSeconds = 30;

    @Builder.Default
    private Boolean enabled = true;

    private String description;

    @CreatedDate
    @Column("created_at")
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column("updated_at")
    private LocalDateTime updatedAt;
}
