-- OS 자원 모니터링 설정 테이블 (CPU, 메모리, 디스크 등)
CREATE TABLE IF NOT EXISTS os_monitoring_config (
    config_id        VARCHAR(50) PRIMARY KEY COMMENT '설정 ID (예: OSC0000000001)',
    agent_id         VARCHAR(50)                             COMMENT '에이전트 ID (NULL이면 전체 공통)',
    metric_type      VARCHAR(30) NOT NULL                    COMMENT '메트릭 타입 (CPU, MEMORY, DISK, NETWORK, PROCESS)',
    metric_name      VARCHAR(100) NOT NULL                   COMMENT '화면/차트에 표시할 이름',
    interval_seconds INT          NOT NULL DEFAULT 60        COMMENT '수집 주기 (초 단위)',
    collect_yn       CHAR(1)      NOT NULL DEFAULT 'Y'       COMMENT '수집 여부 (Y/N)',
    dashboard_yn     CHAR(1)      NOT NULL DEFAULT 'Y'       COMMENT '대시보드 표시 여부 (Y/N)',
    threshold_value  DECIMAL(10,2)                           COMMENT '임계값 (이 값 초과 시만 전송/알림)',
    threshold_type   VARCHAR(20)                             COMMENT '임계 방향 (ABOVE, BELOW)',
    alert_yn         CHAR(1)      NOT NULL DEFAULT 'N'       COMMENT '임계 초과 시 알림 여부 (Y/N)',
    options          TEXT                                    COMMENT 'JSON 형태 추가 옵션',
    enabled          BOOLEAN      NOT NULL DEFAULT TRUE      COMMENT '활성화 여부',
    description      VARCHAR(500)                            COMMENT '설명 / 비고',
    created_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성일시',
    updated_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일시',
    FOREIGN KEY (agent_id) REFERENCES agent(agent_id) ON DELETE CASCADE,
    INDEX idx_osc_agent_id (agent_id),
    INDEX idx_osc_type     (metric_type),
    INDEX idx_osc_enabled  (enabled),
    UNIQUE INDEX idx_osc_agent_metric (agent_id, metric_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='OS 자원 모니터링 설정 테이블';
