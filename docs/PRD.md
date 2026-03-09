# MEGA (Monitoring & Error Gathering Agent) - Product Requirements Document

## 1. 개요 (Overview)
MEGA는 분산된 리눅스 서버 환경에서 시스템 자원(CPU, Memory, Disk) 및 애플리케이션 Exception 로그를 실시간으로 수집하고, 이를 중앙 웹 대시보드(Web Server)에서 통합하여 실시간으로 모니터링할 수 있는 에이전트 기반 모니터링 시스템입니다.

---

## 2. 시스템 아키텍처 (Architecture)

시스템은 **Agent** 모듈과 **Web Server** 모듈로 구성되며, 단방향 Pull & Push 방식으로 동작합니다.
1. **Agent (클라이언트)**: 각 리눅스 서버에 바이너리 형태로 독립 실행되며 데이터를 능동적으로 수집(Pull)하여 서버의 REST API로 전송(Push)합니다.
2. **Web Server (중앙 서버)**: 에이전트로부터 수집된 데이터를 MariaDB에 지속(Persist)시키고, 접속해 있는 대시보드 브라우저들에게 **SSE (Server-Sent Events)**를 이용해 실시간으로 브로드캐스팅합니다.

---

## 3. 구현 완료된 기능 (Implemented Features)

### 3.1. Agent (에이전트)
* **Linux 네이티브 명령어 기반 수집**: `top -bn1`, `free -m`, `df -h` 등 리눅스 표준 명령어를 자체 스케줄러를 통해 실행 후, 정규식 파싱으로 데이터를 추출하여 종속성을 최소화합니다.
  * **Memory**: 사용률(%), 가용량(MB)을 30초 단위로 수집.
  * **CPU**: 점유율(User %)을 30초 단위로 수집.
  * **Disk**: 파티션별 모든 볼륨 사용률(%)을 1분 단위로 수집.
* **에이전트 라이프사이클 관리**: `agent.sh` 스크립트를 통해 백그라운드 구동(start/stop/status), 깃허브 소스 반영 및 자동 빌드(rebuild)를 리눅스 터미널에서 손쉽게 수행합니다.
* **자동 식별 및 등록**: 부팅 시 `hostname` 및 IP 주소를 자동으로 탐지하여 서버에 등록 후 발급된 고유 난수를 로컬 `.agent_id` 파일에 캐싱하여 지속적인 연결을 유지합니다.

### 3.2. Web Server (중앙 모니터링 서버)
* **리액티브 비동기 스택**: Spring WebFlux, R2DBC 스택을 활용하여 C10K 레벨의 대규모 에이전트 연결 시에도 Non-blocking 기반으로 낮은 오버헤드를 유지하며 데이터 파이프라인을 구축합니다.
* **실시간 SSE (Server-Sent Events)**: Proxy 프론트엔드(ex. Nginx) 환경에서도 데이터가 버퍼링되지 않도록 `X-Accel-Buffering: no` 헤더 등을 주입하여 완전한 실시간(Real-time) 데이터 푸시를 실현합니다.
* **보안 계층**: 발급된 API Key 기반으로 에이전트를 인증하며, 웹 대시보드 접근자는 Spring Security의 로그인 세션을 통해 인가된 관리자만 열람할 수 있도록 제어합니다.

### 3.3. Dashboard (대시보드 화면)
* **넓은 뷰 (Full-Width View)**: 에이전트 상태 목록과 차트 영역을 세로(1열)로 꽉 차게 배치하여 넓은 공간에서 대량의 데이터를 시각적으로 표현합니다.
* **동적 차트 자동 생성 (Chart.js)**:
  * 에이전트가 쏘아올리는 **모든 메트릭 명(metricName)**을 동적으로 읽어들여 캔버스를 자동으로 무한 생성합니다.
  * **Memory Combined**: 메모리의 % 비율과 MB 용량을 **듀얼 Y축 (Dual-Y Axis)**을 사용하여 단일 차트에 겹쳐서 입체적으로 보여줍니다.
  * **Disk Combined**: 다양하게 마운트 되는 여러 개의 디스크 파티션들을 서로 다른 컬러 선을 할당하여 **다중 데이터셋(Multi-line)** 차트 한 곳에 모두 표기합니다.
* **과거 데이터(History) 시각화**: 브라우저 접속 즉시 DB에서 최신 500개(최대 보관 2,000 포인트)의 메트릭 데이터를 조회하여 1~2시간 분량의 차트 흐름을 즉시 선그리기로 완성합니다.

---

## 4. 차기 개발 계획 (Upcoming Features) 🚧

### 4.1. 서비스 관리 (Service Management) 메뉴 추가

#### 목표 (Goal)
사용자가 웹 UI에서 서비스별 모니터링 설정(경로, 로그, 수집 항목, 인터벌 등)을 직접 등록하면, Agent가 해당 설정을 주기적으로 내려받아 **실시간으로 반영하여 유연하게 모니터링 대상을 변경**할 수 있는 구조를 구현한다.
수집된 데이터를 기반으로 대시보드 그래프를 시각화한다.

#### 4.1.1. 등록 화면 항목 (UI Form Fields)
| 항목명 | 설명 | 예시 값 |
|---|---|---|
| 서비스명 | 식별 가능한 서비스 이름 | `Nginx-Docker`, `Spring-App` |
| 서비스 경로 | 서비스가 실행되는 루트 디렉토리 | `/home/user/apps/mega-api` |
| 로그 경로 | 모니터링할 로그 파일의 절대 경로 | `/var/log/nginx/access.log` |
| 수집 항목 | 모니터링할 항목 다중 선택 | CPU, Memory, Disk, Log Keyword (Error, 404 등) |
| 수집 인터벌 | 데이터 수집 주기 | `5s`, `10s`, `60s` |

#### 4.1.2. 데이터베이스 설계 (DB Schema)
신규 테이블: `monitoring_config`

```sql
CREATE TABLE monitoring_config (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    agent_id        BIGINT NOT NULL,              -- 연결된 에이전트 FK
    service_name    VARCHAR(100) NOT NULL,         -- 서비스명
    service_path    VARCHAR(255),                  -- 서비스 경로
    log_path        VARCHAR(255),                  -- 로그 파일 경로
    collect_items   VARCHAR(255),                  -- 수집 항목 (CSV: CPU,MEMORY,DISK,LOG)
    log_keywords    VARCHAR(255),                  -- 로그 키워드 (CSV: Error,404)
    interval_seconds INT DEFAULT 30,              -- 수집 인터벌 (초 단위)
    enabled         BOOLEAN DEFAULT TRUE,          -- 활성화 여부
    created_at      DATETIME DEFAULT NOW(),
    updated_at      DATETIME DEFAULT NOW() ON UPDATE NOW(),
    FOREIGN KEY (agent_id) REFERENCES agent(id)
);
```

#### 4.1.3. Agent 동적 설정 반영 방식
* **주기적 Config Pull**: 에이전트가 60초 등 주기로 서버로부터 자신에게 할당된 `monitoring_config` 목록을 REST API로 조회합니다.
* **동적 스케줄러 교체**: 기존 `TaskScheduler`에 등록된 작업을 내려받은 Config 변경 사항 기준으로 재등록(cancel → re-schedule)합니다.
* **로그 키워드 모니터링**: `log_path`에 지정된 파일을 tail 하면서 `log_keywords` 내 문자열이 포함된 라인이 발생하면 Exception 이벤트로 서버에 즉시 전송합니다.

#### 4.1.4. 구현 우선순위 (Priority)
1. **[P0] DB 테이블 및 Entity 생성** (`MonitoringConfig`)
2. **[P0] 서비스 관리 등록/수정/삭제 CRUD API** (WebFlux Reactive)
3. **[P0] 서비스 관리 웹 UI 화면** (Thymeleaf 기반 CRUD 폼)
4. **[P1] Agent Config 동적 Pull API** (`GET /api/agents/{agentId}/configs`)
5. **[P1] Agent 동적 스케줄러 재등록 구현**
6. **[P2] 로그 키워드 Tail 모니터링 구현**

---

## 5. 기술 스택 (Tech Stack)
* **Backend Framework**: Java 17, Spring Boot 3.x, Spring WebFlux
* **Database**: MariaDB, R2DBC, Spring Data R2DBC
* **Frontend**: Vanilla JS (ES6), HTML5/CSS, Thymeleaf, Chart.js (EventSource/SSE 기반)
* **Agent System**: Bash 쉘, Linux Standard tools (`top`, `free`, `df`)
