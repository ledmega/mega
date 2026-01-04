# 웹서버 + 에이전트 시스템 설계 계획서

## 1. 시스템 아키텍처 개요

```
┌─────────────────┐         HTTP/REST API        ┌──────────────────┐
│                 │◄─────────────────────────────┤                  │
│  웹서버         │                               │   에이전트       │
│  (Spring Boot)  │         WebSocket            │   (Java 1.8)     │
│                 │◄─────────────────────────────┤                  │
│  - 대시보드     │                               │  - 명령어 실행   │
│  - 실시간 모니터│                               │  - 로그 파싱     │
│  - 통계 조회    │                               │  - 데이터 전송   │
└─────────────────┘                               └──────────────────┘
         │                                                  │
         │                                                  │
         └──────────────────┬──────────────────────────────┘
                            │
                    ┌───────▼────────┐
                    │   MariaDB      │
                    │   (ledmega)    │
                    └────────────────┘
```

## 2. 웹서버 (Spring Boot) 스펙

### 2.1 기술 스택

#### 백엔드
- **Framework**: Spring Boot 3.5.5
- **Language**: Java 21
- **Build Tool**: Gradle
- **Database**: MariaDB (JPA/Hibernate)
- **Security**: Spring Security
- **WebSocket**: Spring WebSocket (STOMP)
- **REST API**: Spring Web MVC
- **Validation**: Bean Validation

#### 프론트엔드
- **Template Engine**: Thymeleaf 3.x
- **JavaScript**: Vanilla JS 또는 Vue.js 3.x (선택)
- **CSS Framework**: Bootstrap 5.x 또는 Tailwind CSS (선택)
- **Chart Library**: Chart.js 또는 ECharts (대시보드용)
- **WebSocket Client**: SockJS + STOMP.js

#### 추가 라이브러리
- **JSON 처리**: Jackson (기본 포함)
- **HTTP Client**: RestTemplate 또는 WebClient
- **스케줄링**: Spring @Scheduled (에이전트 상태 체크용)
- **로깅**: Logback (이미 설정됨)

### 2.2 주요 기능 모듈

#### 2.2.1 대시보드 모듈
- **실시간 모니터링**
  - 서버 목록 및 상태 표시
  - CPU, 메모리, 디스크 사용률 실시간 차트
  - 네트워크 트래픽 모니터링
  - 서비스 상태 표시

- **통계 대시보드**
  - 시간대별 리소스 사용 통계
  - 서버별 비교 차트
  - 알람/이벤트 로그
  - Exception 발생 통계

#### 2.2.2 에이전트 관리 모듈
- **에이전트 등록/관리**
  - 에이전트 등록 (서버 정보, IP, 호스트명)
  - 에이전트 상태 모니터링 (온라인/오프라인)
  - 에이전트 설정 관리

- **작업 스케줄 관리**
  - 주기적 작업 설정 (5초, 10초, 30초, 1분, 10분 등)
  - 명령어 실행 작업 설정
  - 로그 파싱 작업 설정
  - 작업 활성화/비활성화

#### 2.2.3 데이터 조회 모듈
- **통계 데이터 조회**
  - 서버별 리소스 사용 이력
  - 시간대별 통계 조회
  - Exception 로그 조회 및 검색

#### 2.2.4 WebSocket 모듈
- **실시간 데이터 전송**
  - 에이전트 → 서버: 실시간 메트릭 전송
  - 서버 → 클라이언트: 실시간 대시보드 업데이트
  - STOMP 프로토콜 사용

### 2.3 API 설계

#### REST API 엔드포인트
```
POST   /api/agents/register          # 에이전트 등록
POST   /api/agents/{id}/metrics      # 메트릭 데이터 수신
GET    /api/agents                   # 에이전트 목록 조회
GET    /api/agents/{id}/status       # 에이전트 상태 조회
GET    /api/metrics/history          # 통계 데이터 조회
GET    /api/exceptions               # Exception 로그 조회
POST   /api/tasks                    # 작업 스케줄 등록
GET    /api/tasks                    # 작업 목록 조회
```

#### WebSocket 엔드포인트
```
/topic/metrics/{agentId}             # 실시간 메트릭 구독
/topic/agents/status                 # 에이전트 상태 변경 구독
/app/metrics/send                    # 메트릭 전송 (에이전트용)
```

## 3. 에이전트 (Java 1.8) 스펙

### 3.1 기술 스택

#### 핵심 라이브러리
- **Java Version**: Java 1.8 (JDK 8)
- **HTTP Client**: Apache HttpClient 4.5+ 또는 OkHttp 3.x
- **JSON 처리**: Gson 2.8+ 또는 Jackson 2.10+
- **스케줄링**: ScheduledExecutorService (Java 표준)
- **로깅**: Log4j 2.x 또는 SLF4J + Logback
- **설정 관리**: Properties 파일 또는 JSON 설정 파일

#### 선택적 라이브러리
- **SSH 연결**: JSch (원격 서버 접근용, 필요시)
- **프로세스 실행**: Java ProcessBuilder (기본 포함)
- **파일 모니터링**: Apache Commons IO (로그 파일 모니터링)

### 3.2 주요 기능 모듈

#### 3.2.1 스케줄러 모듈
- **작업 스케줄 관리**
  - 다중 주기 지원 (5초, 10초, 30초, 1분, 10분 등)
  - 작업별 독립적인 스레드 풀
  - 작업 실행 상태 관리
  - 에러 발생 시 재시도 로직

#### 3.2.2 명령어 실행 모듈
- **시스템 명령어 실행**
  - `free -m`: 메모리 사용량
  - `df -h`: 디스크 사용량
  - `top -bn1`: CPU 사용률
  - `netstat`, `ss`: 네트워크 상태
  - 커스텀 명령어 실행

- **결과 파싱**
  - 명령어 출력 파싱
  - JSON 형식으로 변환
  - 서버로 전송

#### 3.2.3 로그 파싱 모듈
- **로그 파일 모니터링**
  - 파일 변경 감지 (tail -f 방식)
  - 정규식 패턴 매칭 (Exception 등)
  - 컨텍스트 추출 (위 5줄, 아래 5줄)
  - DB 저장 형식으로 변환

#### 3.2.4 통신 모듈
- **HTTP 통신**
  - REST API 호출 (메트릭 전송)
  - 인증 토큰 관리
  - 재연결 로직
  - 배치 전송 (성능 최적화)

- **WebSocket 통신** (선택)
  - 실시간 데이터 전송
  - 연결 유지 관리

#### 3.2.5 설정 관리 모듈
- **설정 파일 구조**
  ```json
  {
    "server": {
      "url": "http://webserver:8080",
      "apiKey": "agent-api-key"
    },
    "agent": {
      "id": "agent-001",
      "name": "Server-01"
    },
    "tasks": [
      {
        "id": "task-001",
        "type": "COMMAND",
        "command": "free -m",
        "interval": 60,
        "enabled": true
      }
    ]
  }
  ```

## 4. 데이터베이스 설계

### 4.1 테이블 구조

#### 4.1.1 agent 테이블
```sql
CREATE TABLE agent (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    agent_id VARCHAR(100) UNIQUE NOT NULL,  -- 고유 에이전트 ID
    name VARCHAR(100) NOT NULL,             -- 에이전트 이름
    hostname VARCHAR(255),                  -- 호스트명
    ip_address VARCHAR(50),                 -- IP 주소
    os_type VARCHAR(50),                    -- OS 타입
    status VARCHAR(20) DEFAULT 'OFFLINE',   -- ONLINE/OFFLINE
    last_heartbeat DATETIME,                -- 마지막 하트비트
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_agent_id (agent_id),
    INDEX idx_status (status)
);
```

#### 4.1.2 task 테이블 (작업 스케줄)
```sql
CREATE TABLE task (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    agent_id BIGINT NOT NULL,
    task_name VARCHAR(100) NOT NULL,
    task_type VARCHAR(50) NOT NULL,         -- COMMAND, LOG_PARSE
    command VARCHAR(500),                   -- 실행 명령어
    log_path VARCHAR(500),                  -- 로그 파일 경로
    log_pattern VARCHAR(500),               -- 로그 파싱 패턴
    interval_seconds INT NOT NULL,          -- 실행 주기 (초)
    enabled BOOLEAN DEFAULT TRUE,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (agent_id) REFERENCES agent(id),
    INDEX idx_agent_id (agent_id),
    INDEX idx_enabled (enabled)
);
```

#### 4.1.3 metric_data 테이블 (통계 데이터)
```sql
CREATE TABLE metric_data (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    agent_id BIGINT NOT NULL,
    task_id BIGINT,
    metric_type VARCHAR(50) NOT NULL,       -- CPU, MEMORY, DISK, NETWORK
    metric_name VARCHAR(100),               -- 메트릭 이름
    metric_value DECIMAL(20, 4),            -- 메트릭 값
    unit VARCHAR(20),                       -- 단위 (%, MB, GB 등)
    raw_data TEXT,                          -- 원본 데이터 (JSON)
    collected_at DATETIME NOT NULL,         -- 수집 시간
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (agent_id) REFERENCES agent(id),
    FOREIGN KEY (task_id) REFERENCES task(id),
    INDEX idx_agent_collected (agent_id, collected_at),
    INDEX idx_metric_type (metric_type),
    INDEX idx_collected_at (collected_at)
);
```

#### 4.1.4 exception_log 테이블 (Exception 로그)
```sql
CREATE TABLE exception_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    agent_id BIGINT NOT NULL,
    task_id BIGINT,
    log_file_path VARCHAR(500),
    exception_type VARCHAR(200),
    exception_message TEXT,
    context_before TEXT,                    -- 위 5줄
    context_after TEXT,                     -- 아래 5줄
    full_stack_trace TEXT,
    occurred_at DATETIME NOT NULL,          -- 발생 시간
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (agent_id) REFERENCES agent(id),
    FOREIGN KEY (task_id) REFERENCES task(id),
    INDEX idx_agent_occurred (agent_id, occurred_at),
    INDEX idx_exception_type (exception_type),
    INDEX idx_occurred_at (occurred_at)
);
```

#### 4.1.5 agent_heartbeat 테이블 (하트비트 로그)
```sql
CREATE TABLE agent_heartbeat (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    agent_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL,            -- ONLINE, OFFLINE
    heartbeat_at DATETIME NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (agent_id) REFERENCES agent(id),
    INDEX idx_agent_heartbeat (agent_id, heartbeat_at)
);
```

## 5. 통신 프로토콜

### 5.1 에이전트 등록
```json
POST /api/agents/register
{
  "agentId": "agent-001",
  "name": "Server-01",
  "hostname": "server01.example.com",
  "ipAddress": "192.168.1.100",
  "osType": "Linux"
}

Response:
{
  "id": 1,
  "agentId": "agent-001",
  "status": "ONLINE",
  "apiKey": "generated-api-key"
}
```

### 5.2 메트릭 데이터 전송
```json
POST /api/agents/{agentId}/metrics
Headers: Authorization: Bearer {apiKey}

{
  "taskId": "task-001",
  "metricType": "MEMORY",
  "metricName": "free_memory",
  "metricValue": 2048.5,
  "unit": "MB",
  "rawData": {
    "total": 8192,
    "used": 6144,
    "free": 2048
  },
  "collectedAt": "2026-01-03T22:30:00"
}
```

### 5.3 Exception 로그 전송
```json
POST /api/agents/{agentId}/exceptions
Headers: Authorization: Bearer {apiKey}

{
  "taskId": "task-002",
  "logFilePath": "/var/log/app/application.log",
  "exceptionType": "java.lang.NullPointerException",
  "exceptionMessage": "Cannot invoke method on null",
  "contextBefore": "line1\nline2\n...",
  "contextAfter": "line1\nline2\n...",
  "fullStackTrace": "...",
  "occurredAt": "2026-01-03T22:30:00"
}
```

### 5.4 하트비트 전송
```json
POST /api/agents/{agentId}/heartbeat
Headers: Authorization: Bearer {apiKey}

{
  "status": "ONLINE",
  "heartbeatAt": "2026-01-03T22:30:00"
}
```

## 6. 구현 단계

### Phase 1: 웹서버 기본 구조
1. 대시보드 페이지 생성
2. 에이전트 관리 페이지
3. REST API 기본 구조
4. 데이터베이스 테이블 생성

### Phase 2: 에이전트 기본 구조
1. 에이전트 프로젝트 생성 (Java 1.8)
2. 설정 파일 구조
3. HTTP 통신 모듈
4. 에이전트 등록 기능

### Phase 3: 스케줄러 및 명령어 실행
1. 스케줄러 모듈 구현
2. 명령어 실행 모듈
3. 결과 파싱 모듈
4. 메트릭 데이터 전송

### Phase 4: 로그 파싱
1. 로그 파일 모니터링
2. 정규식 패턴 매칭
3. Exception 추출 및 저장

### Phase 5: 실시간 대시보드
1. WebSocket 설정
2. 실시간 데이터 전송
3. 대시보드 실시간 업데이트

### Phase 6: 통계 및 조회
1. 통계 데이터 조회 API
2. 차트 데이터 제공
3. Exception 로그 조회

## 7. 보안 고려사항

- **인증/인가**: API Key 기반 인증
- **HTTPS**: 프로덕션 환경에서는 HTTPS 사용
- **Rate Limiting**: API 호출 제한
- **입력 검증**: 모든 입력 데이터 검증
- **SQL Injection 방지**: PreparedStatement 사용

## 8. 성능 최적화

- **배치 전송**: 메트릭 데이터 배치 처리
- **인덱싱**: 자주 조회되는 컬럼 인덱스
- **데이터 보관 정책**: 오래된 데이터 아카이빙
- **캐싱**: 자주 조회되는 데이터 캐싱 (Redis 선택)

## 9. 모니터링 및 알람

- **에이전트 상태 모니터링**: 오프라인 감지
- **리소스 임계값 알람**: CPU/메모리 사용률 경고
- **Exception 발생 알람**: 실시간 알림

