package led.mega.batch.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("batch_job")
public class BatchJob {

    @Id
    @Column("batch_job_id")
    private String batchJobId;

    @Column("job_name")
    private String jobName;

    @Column("job_type")
    private String jobType;

    @Column("description")
    private String description;

    @Column("interval_minutes")
    private Integer intervalMinutes;

    @Column("retention_days")
    private Integer retentionDays;

    @Column("enabled")
    private Boolean enabled;

    @Column("last_run_at")
    private LocalDateTime lastRunAt;

    @Column("last_run_status")
    private String lastRunStatus;  // SUCCESS, FAILED, RUNNING

    @Column("last_run_message")
    private String lastRunMessage;

    @Column("cron_expression")
    private String cronExpression; // 예: "0 0 23 * * ?" (매일 23시)

    @Column("job_config")
    private String jobConfig;      // 실행할 SQL이나 스크립트 내용

    @Column("created_at")
    private LocalDateTime createdAt;

    @Column("updated_at")
    private LocalDateTime updatedAt;
}
