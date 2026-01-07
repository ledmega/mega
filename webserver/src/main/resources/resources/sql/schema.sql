-- 회원 테이블 생성
CREATE TABLE IF NOT EXISTS member (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '회원 ID (시퀀스 키)',
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
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '에이전트 ID',
    agent_id VARCHAR(100) NOT NULL UNIQUE COMMENT '고유 에이전트 ID',
    name VARCHAR(100) NOT NULL COMMENT '에이전트 이름',
    hostname VARCHAR(255) COMMENT '호스트명',
    ip_address VARCHAR(50) COMMENT 'IP 주소',
    os_type VARCHAR(50) COMMENT 'OS 타입',
    status VARCHAR(20) NOT NULL DEFAULT 'OFFLINE' COMMENT '상태 (ONLINE, OFFLINE)',
    last_heartbeat DATETIME COMMENT '마지막 하트비트',
    api_key VARCHAR(255) COMMENT 'API 인증 키',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성일시',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일시',
    INDEX idx_agent_id (agent_id),
    INDEX idx_status (status),
    INDEX idx_last_heartbeat (last_heartbeat)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='에이전트 테이블';

-- 작업 스케줄 테이블 생성
CREATE TABLE IF NOT EXISTS task (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '작업 ID',
    agent_id BIGINT NOT NULL COMMENT '에이전트 ID',
    task_name VARCHAR(100) NOT NULL COMMENT '작업 이름',
    task_type VARCHAR(50) NOT NULL COMMENT '작업 타입 (COMMAND, LOG_PARSE)',
    command VARCHAR(500) COMMENT '실행 명령어',
    log_path VARCHAR(500) COMMENT '로그 파일 경로',
    log_pattern VARCHAR(500) COMMENT '로그 파싱 패턴 (정규식)',
    interval_seconds INT NOT NULL COMMENT '실행 주기 (초)',
    enabled BOOLEAN NOT NULL DEFAULT TRUE COMMENT '활성화 여부',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성일시',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일시',
    FOREIGN KEY (agent_id) REFERENCES agent(id) ON DELETE CASCADE,
    INDEX idx_agent_id (agent_id),
    INDEX idx_enabled (enabled),
    INDEX idx_task_type (task_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='작업 스케줄 테이블';

-- 통계 데이터 테이블 생성
CREATE TABLE IF NOT EXISTS metric_data (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '메트릭 ID',
    agent_id BIGINT NOT NULL COMMENT '에이전트 ID',
    task_id BIGINT COMMENT '작업 ID',
    metric_type VARCHAR(50) NOT NULL COMMENT '메트릭 타입 (CPU, MEMORY, DISK, NETWORK)',
    metric_name VARCHAR(100) COMMENT '메트릭 이름',
    metric_value DECIMAL(20, 4) COMMENT '메트릭 값',
    unit VARCHAR(20) COMMENT '단위 (%, MB, GB 등)',
    raw_data TEXT COMMENT '원본 데이터 (JSON)',
    collected_at DATETIME NOT NULL COMMENT '수집 시간',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성일시',
    FOREIGN KEY (agent_id) REFERENCES agent(id) ON DELETE CASCADE,
    FOREIGN KEY (task_id) REFERENCES task(id) ON DELETE SET NULL,
    INDEX idx_agent_collected (agent_id, collected_at),
    INDEX idx_metric_type (metric_type),
    INDEX idx_collected_at (collected_at),
    INDEX idx_task_id (task_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='통계 데이터 테이블';

-- Exception 로그 테이블 생성
CREATE TABLE IF NOT EXISTS exception_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT 'Exception 로그 ID',
    agent_id BIGINT NOT NULL COMMENT '에이전트 ID',
    task_id BIGINT COMMENT '작업 ID',
    log_file_path VARCHAR(500) COMMENT '로그 파일 경로',
    exception_type VARCHAR(200) COMMENT 'Exception 타입',
    exception_message TEXT COMMENT 'Exception 메시지',
    context_before TEXT COMMENT '위 5줄 컨텍스트',
    context_after TEXT COMMENT '아래 5줄 컨텍스트',
    full_stack_trace TEXT COMMENT '전체 스택 트레이스',
    occurred_at DATETIME NOT NULL COMMENT '발생 시간',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성일시',
    FOREIGN KEY (agent_id) REFERENCES agent(id) ON DELETE CASCADE,
    FOREIGN KEY (task_id) REFERENCES task(id) ON DELETE SET NULL,
    INDEX idx_agent_occurred (agent_id, occurred_at),
    INDEX idx_exception_type (exception_type),
    INDEX idx_occurred_at (occurred_at),
    INDEX idx_task_id (task_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Exception 로그 테이블';

-- 에이전트 하트비트 테이블 생성
CREATE TABLE IF NOT EXISTS agent_heartbeat (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '하트비트 ID',
    agent_id BIGINT NOT NULL COMMENT '에이전트 ID',
    status VARCHAR(20) NOT NULL COMMENT '상태 (ONLINE, OFFLINE)',
    heartbeat_at DATETIME NOT NULL COMMENT '하트비트 시간',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성일시',
    FOREIGN KEY (agent_id) REFERENCES agent(id) ON DELETE CASCADE,
    INDEX idx_agent_heartbeat (agent_id, heartbeat_at),
    INDEX idx_heartbeat_at (heartbeat_at),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='에이전트 하트비트 테이블';

