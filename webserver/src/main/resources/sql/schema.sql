-- 데이터베이스 생성
CREATE DATABASE IF NOT EXISTS `ledmega`
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE `ledmega`;

-- 회원 테이블 생성
CREATE TABLE IF NOT EXISTS member (
    member_id VARCHAR(50) PRIMARY KEY COMMENT '회원 ID (예: MBR0000000001)',
    email VARCHAR(100) NOT NULL UNIQUE COMMENT '이메일',
    password VARCHAR(255) NOT NULL COMMENT '비밀번호 (암호화)',
    name VARCHAR(50) NOT NULL COMMENT '이름',
    nickname VARCHAR(50) COMMENT '닉네임',
    phone VARCHAR(20) COMMENT '전화번호',
    role VARCHAR(20) NOT NULL DEFAULT 'ROLE_USER' COMMENT '권한 (ROLE_USER, ROLE_ADMIN)',
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT '상태 (ACTIVE, INACTIVE, DELETED)',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '가입일시',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일시',
    last_login_at DATETIME COMMENT '마지막 로그인 일시',
    INDEX idx_email (email),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='회원 테이블';

-- 에이전트 테이블 생성
CREATE TABLE IF NOT EXISTS agent (
    agent_id VARCHAR(50) PRIMARY KEY COMMENT '에이전트 ID (예: AGT0000000001)',
    agent_ref_id VARCHAR(100) NOT NULL UNIQUE COMMENT '고유 에이전트 식별코드',
    name VARCHAR(100) NOT NULL COMMENT '에이전트 이름',
    hostname VARCHAR(255) COMMENT '호스트명',
    ip_address VARCHAR(50) COMMENT 'IP 주소',
    os_type VARCHAR(50) COMMENT 'OS 타입',
    status VARCHAR(20) NOT NULL DEFAULT 'OFFLINE' COMMENT '상태 (ONLINE, OFFLINE)',
    last_heartbeat DATETIME COMMENT '마지막 하트비트',
    api_key VARCHAR(255) COMMENT 'API 인증 키',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성일시',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일시',
    INDEX idx_agent_ref_id (agent_ref_id),
    INDEX idx_status (status),
    INDEX idx_last_heartbeat (last_heartbeat)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='에이전트 테이블';

-- 작업 스케줄 테이블 생성
CREATE TABLE IF NOT EXISTS task (
    task_id VARCHAR(50) PRIMARY KEY COMMENT '작업 ID (예: TSK0000000001)',
    agent_id VARCHAR(50) NOT NULL COMMENT '에이전트 ID',
    task_name VARCHAR(100) NOT NULL COMMENT '작업 이름',
    task_type VARCHAR(50) NOT NULL COMMENT '작업 타입 (COMMAND, LOG_PARSE)',
    command VARCHAR(500) COMMENT '실행 명령어',
    log_path VARCHAR(500) COMMENT '로그 파일 경로',
    log_pattern VARCHAR(500) COMMENT '로그 파싱 패턴 (정규식)',
    interval_seconds INT NOT NULL COMMENT '실행 주기 (초)',
    enabled BOOLEAN NOT NULL DEFAULT TRUE COMMENT '활성화 여부',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성일시',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일시',
    FOREIGN KEY (agent_id) REFERENCES agent(agent_id) ON DELETE CASCADE,
    INDEX idx_agent_id (agent_id),
    INDEX idx_enabled (enabled),
    INDEX idx_task_type (task_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='작업 스케줄 테이블';

-- 통계 데이터 테이블 생성
CREATE TABLE IF NOT EXISTS metric_data (
    metric_id VARCHAR(50) PRIMARY KEY COMMENT '메트릭 ID (예: MET0000000001)',
    agent_id VARCHAR(50) NOT NULL COMMENT '에이전트 ID',
    task_id VARCHAR(50) COMMENT '작업 ID',
    monitoring_config_id VARCHAR(50) COMMENT '서비스 모니터링 설정 ID',
    metric_type VARCHAR(50) NOT NULL COMMENT '메트릭 타입 (CPU, MEMORY, DISK, NETWORK)',
    metric_name VARCHAR(100) COMMENT '메트릭 이름',
    metric_value DECIMAL(20, 4) COMMENT '메트릭 값',
    unit VARCHAR(20) COMMENT '단위 (%, MB, GB 등)',
    raw_data TEXT COMMENT '원본 데이터 (JSON)',
    collected_at DATETIME NOT NULL COMMENT '수집 시간',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성일시',
    FOREIGN KEY (agent_id) REFERENCES agent(agent_id) ON DELETE CASCADE,
    FOREIGN KEY (task_id) REFERENCES task(task_id) ON DELETE SET NULL,
    INDEX idx_agent_collected (agent_id, collected_at),
    INDEX idx_metric_type (metric_type),
    INDEX idx_collected_at (collected_at),
    INDEX idx_task_id (task_id),
    INDEX idx_monitoring_config_id (monitoring_config_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='통계 데이터 테이블';

-- Exception 로그 테이블 생성
CREATE TABLE IF NOT EXISTS exception_log (
    ex_log_id VARCHAR(50) PRIMARY KEY COMMENT 'Exception 로그 ID (예: EXL0000000001)',
    agent_id VARCHAR(50) NOT NULL COMMENT '에이전트 ID',
    task_id VARCHAR(50) COMMENT '작업 ID',
    monitoring_config_id VARCHAR(50) COMMENT '서비스 모니터링 설정 ID',
    log_file_path VARCHAR(500) COMMENT '로그 파일 경로',
    exception_type VARCHAR(200) COMMENT 'Exception 타입',
    exception_message TEXT COMMENT 'Exception 메시지',
    context_before TEXT COMMENT '위 5줄 컨텍스트',
    context_after TEXT COMMENT '아래 5줄 컨텍스트',
    full_stack_trace TEXT COMMENT '전체 스택 트레이스',
    occurred_at DATETIME NOT NULL COMMENT '발생 시간',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성일시',
    FOREIGN KEY (agent_id) REFERENCES agent(agent_id) ON DELETE CASCADE,
    FOREIGN KEY (task_id) REFERENCES task(task_id) ON DELETE SET NULL,
    INDEX idx_agent_occurred (agent_id, occurred_at),
    INDEX idx_exception_type (exception_type),
    INDEX idx_occurred_at (occurred_at),
    INDEX idx_task_id (task_id),
    INDEX idx_monitoring_config_id (monitoring_config_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Exception 로그 테이블';

-- 에이전트 하트비트 테이블 생성
CREATE TABLE IF NOT EXISTS agent_heartbeat (
    hb_id VARCHAR(50) PRIMARY KEY COMMENT '하트비트 ID (예: HBT0000000001)',
    agent_id VARCHAR(50) NOT NULL COMMENT '에이전트 ID',
    status VARCHAR(20) NOT NULL COMMENT '상태 (ONLINE, OFFLINE)',
    heartbeat_at DATETIME NOT NULL COMMENT '하트비트 시간',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성일시',
    FOREIGN KEY (agent_id) REFERENCES agent(agent_id) ON DELETE CASCADE,
    INDEX idx_agent_heartbeat (agent_id, heartbeat_at),
    INDEX idx_heartbeat_at (heartbeat_at),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='에이전트 하트비트 테이블';

-- 메뉴 테이블 생성
CREATE TABLE IF NOT EXISTS menu (
    menu_id VARCHAR(50) PRIMARY KEY COMMENT '메뉴 ID (예: MNU0000000001)',
    name VARCHAR(100) NOT NULL COMMENT '메뉴 이름',
    url VARCHAR(255) NOT NULL COMMENT '메뉴 URL',
    icon VARCHAR(100) COMMENT '아이콘 (FontAwesome 클래스명)',
    sort_order INT NOT NULL DEFAULT 0 COMMENT '정렬 순서',
    parent_id VARCHAR(50) COMMENT '부모 메뉴 ID',
    required_role VARCHAR(50) COMMENT '필요 권한 (null이면 전체)',
    is_enabled BOOLEAN NOT NULL DEFAULT TRUE COMMENT '활성화 여부',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성일시',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일시',
    FOREIGN KEY (parent_id) REFERENCES menu(menu_id) ON DELETE SET NULL,
    INDEX idx_sort_order (sort_order),
    INDEX idx_is_enabled (is_enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='메뉴 관리 테이블';

-- 서비스 모니터링 설정 테이블 생성
CREATE TABLE IF NOT EXISTS monitoring_config (
    config_id        VARCHAR(50) PRIMARY KEY COMMENT '설정 ID (예: CFG0000000001)',
    agent_id         VARCHAR(50) NOT NULL                   COMMENT '연결된 에이전트 ID',
    service_name     VARCHAR(100) NOT NULL              COMMENT '서비스 이름 (예: Nginx-Docker)',
    target_type      VARCHAR(20) NOT NULL DEFAULT 'HOST' COMMENT '타겟 유형 (HOST, PROCESS, DOCKER)',
    target_name      VARCHAR(100)                       COMMENT '타겟 식별자 (프로세스명 또는 컨테이너명)',
    service_path     VARCHAR(500)                       COMMENT '서비스 설치 경로',
    log_path         VARCHAR(500)                       COMMENT '모니터링 로그 파일 경로',
    collect_items    VARCHAR(255) NOT NULL DEFAULT 'CPU,MEMORY,DISK' COMMENT '수집 항목 목록 (CSV: CPU,MEMORY,DISK,LOG)',
    log_keywords     VARCHAR(500)                       COMMENT '로그 감시 키워드 (CSV: Error,404)',
    interval_seconds INT          NOT NULL DEFAULT 30   COMMENT '수집 주기 (초 단위)',
    enabled          BOOLEAN      NOT NULL DEFAULT TRUE  COMMENT '활성화 여부',
    description      VARCHAR(500)                       COMMENT '설명 / 비고',
    created_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성일시',
    updated_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일시',
    FOREIGN KEY (agent_id) REFERENCES agent(agent_id) ON DELETE CASCADE,
    INDEX idx_mc_agent_id   (agent_id),
    INDEX idx_mc_enabled    (enabled),
    INDEX idx_mc_service    (service_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='서비스 모니터링 설정 테이블';

-- 배치 스케줄러 Job 설정 테이블
CREATE TABLE IF NOT EXISTS batch_job (
    batch_job_id     VARCHAR(50) PRIMARY KEY COMMENT 'Job ID (예: BTJ0000000001)',
    job_name         VARCHAR(100) NOT NULL UNIQUE       COMMENT 'Job 이름 (고유)',
    job_type         VARCHAR(50)  NOT NULL               COMMENT 'Job 유형 (METRIC_DATA_CLEANUP, EXCEPTION_LOG_CLEANUP)',
    description      VARCHAR(500)                        COMMENT 'Job 설명',
    interval_minutes INT          NOT NULL DEFAULT 60    COMMENT '실행 주기 (분 단위)',
    retention_days   INT          NOT NULL DEFAULT 7     COMMENT '데이터 보존 일수 (이 이전 데이터 삭제)',
    enabled          BOOLEAN      NOT NULL DEFAULT TRUE   COMMENT '활성화 여부',
    last_run_at      DATETIME                            COMMENT '마지막 실행 시간',
    last_run_status  VARCHAR(20)                         COMMENT '마지막 실행 결과 (SUCCESS, FAILED, RUNNING)',
    last_run_message VARCHAR(500)                        COMMENT '마지막 실행 메시지 또는 오류',
    cron_expression  VARCHAR(100)                        COMMENT 'Cron 표현식 (예: 0 0 23 * * ?)',
    job_config       TEXT                                COMMENT '실행할 SQL이나 스크립트 내용',
    created_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성일시',
    updated_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일시',
    INDEX idx_bj_enabled  (enabled),
    INDEX idx_bj_job_type (job_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='배치 스케줄러 Job 설정 테이블';

-- 서비스 전용 메트릭 데이터 테이블
CREATE TABLE IF NOT EXISTS service_metric_data (
    svc_metric_id VARCHAR(50) PRIMARY KEY COMMENT '메트릭 ID (예: SMT0000000001)',
    agent_id VARCHAR(50) NOT NULL COMMENT '에이전트 ID',
    monitoring_config_id VARCHAR(50) NOT NULL COMMENT '연결된 서비스 모니터링 설정 ID',
    cpu_usage_percent DECIMAL(10, 4) COMMENT '서비스 CPU 사용량 (%)',
    memory_usage_mb DECIMAL(10, 4) COMMENT '서비스 메모리 사용량 (MB)',
    memory_usage_percent DECIMAL(10, 4) COMMENT '서비스 메모리 제한 대비 사용량 (%)',
    disk_usage_percent DECIMAL(10, 4) COMMENT '서비스 디스크 사용률 (%)',
    network_rx_bytes BIGINT COMMENT '서비스 네트워크 다운로드 바이트',
    network_tx_bytes BIGINT COMMENT '서비스 네트워크 업로드 바이트',
    collected_at DATETIME NOT NULL COMMENT '수집 시간',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성일시',
    FOREIGN KEY (agent_id) REFERENCES agent(agent_id) ON DELETE CASCADE,
    FOREIGN KEY (monitoring_config_id) REFERENCES monitoring_config(config_id) ON DELETE CASCADE,
    INDEX idx_smd_agent_collected (agent_id, collected_at),
    INDEX idx_smd_config (monitoring_config_id),
    INDEX idx_smd_collected_at (collected_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='서비스 전용 통계 데이터 테이블';

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
    INDEX idx_osc_enabled  (enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='OS 자원 모니터링 설정 테이블';
