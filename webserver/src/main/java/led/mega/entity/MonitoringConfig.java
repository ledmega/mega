package led.mega.entity;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
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
    private Long id;

    private Long agentId;              // agent 테이블 FK

    private String serviceName;        // 서비스 이름 (예: Nginx-Docker)
    @Builder.Default
    private String targetType = "HOST"; // HOST, PROCESS, DOCKER
    private String targetName;         // 타겟 식별자 (프로세스명 또는 컨테이너명)
    private String servicePath;        // 서비스 경로 (예: /home/user/apps/mega-api)
    private String logPath;            // 로그 파일 경로 (예: /var/log/nginx/access.log)

    @Builder.Default
    private String collectItems = "CPU,MEMORY,DISK"; // 수집 항목 CSV (CPU,MEMORY,DISK,LOG)

    private String logKeywords;        // 로그 감시 키워드 CSV (Error,404,Exception)

    @Builder.Default
    private Integer intervalSeconds = 30; // 수집 주기 (초)

    @Builder.Default
    private Boolean enabled = true;    // 활성화 여부

    private String description;        // 비고/설명

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}
