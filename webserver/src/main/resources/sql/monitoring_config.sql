-- 서비스 모니터링 설정 테이블
CREATE TABLE IF NOT EXISTS monitoring_config (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '설정 ID',
    agent_id         BIGINT NOT NULL                   COMMENT '연결된 에이전트 ID',
    service_name     VARCHAR(100) NOT NULL              COMMENT '서비스 이름 (예: Nginx-Docker)',
    service_path     VARCHAR(500)                       COMMENT '서비스 설치 경로 (예: /home/user/apps/mega-api)',
    log_path         VARCHAR(500)                       COMMENT '모니터링 로그 파일 경로 (예: /var/log/nginx/access.log)',
    collect_items    VARCHAR(255) NOT NULL DEFAULT 'CPU,MEMORY,DISK' COMMENT '수집 항목 목록 (CSV: CPU,MEMORY,DISK,LOG)',
    log_keywords     VARCHAR(500)                       COMMENT '로그 감시 키워드 (CSV: Error,404,Exception)',
    interval_seconds INT          NOT NULL DEFAULT 30   COMMENT '수집 주기 (초 단위)',
    enabled          BOOLEAN      NOT NULL DEFAULT TRUE  COMMENT '활성화 여부',
    description      VARCHAR(500)                       COMMENT '설명 / 비고',
    created_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성일시',
    updated_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일시',
    FOREIGN KEY (agent_id) REFERENCES agent(id) ON DELETE CASCADE,
    INDEX idx_agent_id  (agent_id),
    INDEX idx_enabled   (enabled),
    INDEX idx_service_name (service_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='서비스 모니터링 설정 테이블';
