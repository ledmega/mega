-- 챗봇 빌더(B2B SaaS) 시스템 스킴 설계 (Auditing 컬럼 보완)
USE `ledmega`;

-- 1. 테넌트(고객사) 테이블
CREATE TABLE IF NOT EXISTS cb_tenant (
    tenant_id VARCHAR(50) PRIMARY KEY COMMENT '테넌트 고유 ID (UUID)',
    name VARCHAR(100) NOT NULL COMMENT '고객사/회사명',
    api_token VARCHAR(255) UNIQUE NOT NULL COMMENT 'API 인증용 토큰',
    status VARCHAR(20) DEFAULT 'ACTIVE' COMMENT '상태 (ACTIVE, SUSPENDED)',
    -- [공통 컬럼]
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '생성일시',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일시',
    created_by VARCHAR(50) COMMENT '생성자 ID',
    updated_by VARCHAR(50) COMMENT '수정자 ID'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='챗봇 빌더 고객사 정보';

-- 2. 봇 설정 테이블 (페르소나 및 UI)
CREATE TABLE IF NOT EXISTS cb_bot_config (
    bot_id VARCHAR(50) PRIMARY KEY COMMENT '봇 고유 ID (UUID)',
    tenant_id VARCHAR(50) NOT NULL COMMENT '소속 테넌트 ID',
    bot_name VARCHAR(100) NOT NULL COMMENT '챗봇 이름',
    system_prompt TEXT NOT NULL COMMENT '페르소나(시스템 프롬프트/임무)',
    welcome_message VARCHAR(1000) COMMENT '위젯 첫 인사말',
    theme_color VARCHAR(20) DEFAULT '#007AFF' COMMENT '위젯 포인트 색상',
    logo_url VARCHAR(500) COMMENT '챗봇 로고 이미지 경로',
    -- [공통 컬럼]
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by VARCHAR(50),
    updated_by VARCHAR(50),
    FOREIGN KEY (tenant_id) REFERENCES cb_tenant(tenant_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='챗봇별 개별 설정 정보';

-- 3. 지식 베이스 테이블 (RAG 데이터)
CREATE TABLE IF NOT EXISTS cb_knowledge_base (
    item_id VARCHAR(50) PRIMARY KEY COMMENT '지식 아이템 ID',
    tenant_id VARCHAR(50) NOT NULL COMMENT '소속 테넌트 ID',
    bot_id VARCHAR(50) NOT NULL COMMENT '연결된 봇 ID',
    source_type VARCHAR(20) NOT NULL COMMENT '출처 유형 (FILE, URL, TEXT)',
    source_url VARCHAR(500) COMMENT '크롤링 주소 또는 파일명',
    content_text LONGTEXT NOT NULL COMMENT '추출된 원문 텍스트',
    -- [공통 컬럼]
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by VARCHAR(50),
    updated_by VARCHAR(50),
    INDEX idx_tenant_bot (tenant_id, bot_id),
    FOREIGN KEY (bot_id) REFERENCES cb_bot_config(bot_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='RAG 학습용 지식 데이터';

-- 4. API 사용량 로그 테이블 (과금용)
CREATE TABLE IF NOT EXISTS cb_usage_log (
    log_id VARCHAR(50) PRIMARY KEY COMMENT '로그 ID',
    tenant_id VARCHAR(50) NOT NULL COMMENT '고객사 ID',
    bot_id VARCHAR(50) NOT NULL COMMENT '챗봇 ID',
    request_tokens INT DEFAULT 0 COMMENT '질문에 사용된 토큰 수',
    response_tokens INT DEFAULT 0 COMMENT '답변에 사용된 토큰 수',
    called_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_tenant_date (tenant_id, called_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='API 사용량 및 과금 통계 로그';
