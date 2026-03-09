-- 기존 DB에 monitoring_config_id 컬럼 추가 (신규 설치 시 schema.sql 사용)
-- 컬럼이 이미 있으면 에러 나므로 한 번만 실행
USE `ledmega`;

ALTER TABLE metric_data
    ADD COLUMN monitoring_config_id BIGINT NULL COMMENT '서비스 모니터링 설정 ID' AFTER task_id;
ALTER TABLE metric_data ADD INDEX idx_md_monitoring_config_id (monitoring_config_id);

ALTER TABLE exception_log
    ADD COLUMN monitoring_config_id BIGINT NULL COMMENT '서비스 모니터링 설정 ID' AFTER task_id;
ALTER TABLE exception_log ADD INDEX idx_el_monitoring_config_id (monitoring_config_id);
