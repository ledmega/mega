-- 데이터베이스 캐릭터셋 확인 및 설정

-- 1. 현재 데이터베이스 캐릭터셋 확인
SHOW CREATE DATABASE ledmega;

-- 2. 데이터베이스 캐릭터셋을 utf8mb4로 변경 (필요한 경우)
ALTER DATABASE ledmega CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 3. 테이블 캐릭터셋 확인
SHOW CREATE TABLE member;

-- 4. 연결 캐릭터셋 확인
SHOW VARIABLES LIKE 'character_set%';
SHOW VARIABLES LIKE 'collation%';

GRANT CREATE ON *.* TO 'ledmega'@'%';
FLUSH PRIVILEGES;

