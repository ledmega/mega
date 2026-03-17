-- 모든 테이블 삭제 스크립트 (초기화용)
USE `ledmega`;

-- 외래키 체크 일시 중지
SET FOREIGN_KEY_CHECKS = 0;

-- 기존 테이블 삭제
DROP TABLE IF EXISTS service_metric_data;
DROP TABLE IF EXISTS batch_job;
DROP TABLE IF EXISTS monitoring_config;
DROP TABLE IF EXISTS menu;
DROP TABLE IF EXISTS agent_heartbeat;
DROP TABLE IF EXISTS exception_log;
DROP TABLE IF EXISTS metric_data;
DROP TABLE IF EXISTS task;
DROP TABLE IF EXISTS agent;
DROP TABLE IF EXISTS member;

-- CS 관련 테이블 삭제
DROP TABLE IF EXISTS cs_message;
DROP TABLE IF EXISTS cs_conversation;
DROP TABLE IF EXISTS cs_faq;
DROP TABLE IF EXISTS cs_inbound_data;
DROP TABLE IF EXISTS cs_report;

-- 외래키 체크 다시 활성화
SET FOREIGN_KEY_CHECKS = 1;

SELECT 'All tables have been dropped. Now run schema.sql and cs_ai_tables.sql to recreate them.' AS result;
