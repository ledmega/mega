-- CS 메시징 및 AI 자동화 관련 테이블

USE `ledmega`;

-- 1. FAQ 테이블: 자동 답변 및 RAG 데이터 엔진
CREATE TABLE IF NOT EXISTS cs_faq (
    cs_faq_id VARCHAR(50) PRIMARY KEY COMMENT 'FAQ ID (예: FAQ0000000001)',
    category VARCHAR(50) NOT NULL COMMENT '카테고리 (배정, 장애, 단순문의 등)',
    question VARCHAR(1000) NOT NULL COMMENT '질문 내용',
    answer TEXT NOT NULL COMMENT '답변 내용',
    use_yn CHAR(1) NOT NULL DEFAULT 'Y' COMMENT '사용 여부 (Y/N)',
    tags VARCHAR(500) COMMENT '검색용 태그 (쉼표 구분)',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성일시',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일시',
    FULLTEXT INDEX idx_faq_search (question, answer, tags) -- 자연어 검색용
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='CS FAQ 테이블';

-- 2. 상담 세션 테이블: 카톡, 메일, 포탈 문의 통합 관리
CREATE TABLE IF NOT EXISTS cs_conversation (
    cs_conv_id VARCHAR(50) PRIMARY KEY COMMENT '세션 ID (예: CON0000000001)',
    external_id VARCHAR(255) NOT NULL COMMENT '외부 식별자 (톡드림 ID, 이메일 주소 등)',
    channel VARCHAR(20) NOT NULL COMMENT '수신 채널 (TALKDREAM, EMAIL, PORTAL)',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '상태 (PENDING, PROCESSING, COMPLETED)',
    summary TEXT COMMENT 'AI가 요약한 문의 핵심 내용',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성일시',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일시',
    INDEX idx_external_id (external_id),
    INDEX idx_status (status),
    INDEX idx_channel (channel)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='CS 상담 세션 테이블';

-- 3. 메시지 내역 테이블: 개별 대화 상세 저장
CREATE TABLE IF NOT EXISTS cs_message (
    cs_msg_id VARCHAR(50) PRIMARY KEY COMMENT '메시지 ID (예: MSG0000000001)',
    cs_conv_id VARCHAR(50) NOT NULL COMMENT '세션 ID',
    sender_type VARCHAR(20) NOT NULL COMMENT '발신자 유형 (USER, BOT, ADMIN)',
    content TEXT NOT NULL COMMENT '메시지 본문',
    is_draft BOOLEAN NOT NULL DEFAULT FALSE COMMENT 'AI 추천 초안 여부',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성일시',
    FOREIGN KEY (cs_conv_id) REFERENCES cs_conversation(cs_conv_id) ON DELETE CASCADE,
    INDEX idx_conversation_id (cs_conv_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='CS 메시지 상세 테이블';

-- 4. 외부 수신 데이터 Staging 테이블: 이메일, 레드마인 웹훅 원본 데이터 저장
CREATE TABLE IF NOT EXISTS cs_inbound_data (
    cs_inbound_id VARCHAR(50) PRIMARY KEY COMMENT '수집 ID (예: INB0000000001)',
    source VARCHAR(20) NOT NULL COMMENT '데이터 출처 (EMAIL, REDMINE, TALKDREAM)',
    external_ref_id VARCHAR(100) COMMENT '외부 참조 ID (메일 UID, 레드마인 일감번호 등)',
    raw_payload LONGTEXT COMMENT '원본 JSON 또는 본문 데이터',
    status VARCHAR(20) NOT NULL DEFAULT 'RECEIVED' COMMENT '처리 상태 (RECEIVED, PROCESSED, FAILED)',
    error_message TEXT COMMENT '처리 실패 시 오류 메시지',
    received_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '수신 시간',
    processed_at DATETIME COMMENT '처리 완료 시간',
    INDEX idx_source_ref (source, external_ref_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='외부 수신 데이터 수집 테이블';

-- 5. CS 처리 리포트 테이블: 통계 데이터 자동 생성용
CREATE TABLE IF NOT EXISTS cs_report (
    cs_report_id VARCHAR(50) PRIMARY KEY COMMENT '리포트 ID (예: RPT0000000001)',
    report_type VARCHAR(20) NOT NULL COMMENT '리포트 유형 (DAILY, WEEKLY, MONTHLY)',
    target_date DATE NOT NULL COMMENT '대상 날짜/기준일',
    total_count INT NOT NULL DEFAULT 0 COMMENT '전체 문의 건수',
    auto_reply_count INT NOT NULL DEFAULT 0 COMMENT '자동 답변 건수',
    avg_process_time_sec INT DEFAULT 0 COMMENT '평균 처리 시간(초)',
    re_inquiry_rate DECIMAL(5, 2) DEFAULT 0.00 COMMENT '재문의율 (%)',
    report_data JSON COMMENT '상세 통계 데이터 (JSON 구조)',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성일시',
    UNIQUE KEY uk_report_type_date (report_type, target_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='CS 처리 통계 리포트 테이블';
