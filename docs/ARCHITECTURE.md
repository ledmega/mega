# MEGA (Monitoring & Error Gathering Agent) - Architecture

## 1. 개요 (Overview)
MEGA는 분산된 리눅스 서버 환경에서 시스템 자원(CPU, Memory, Disk) 및 애플리케이션 Exception 로그를 실시간으로 수집하고, 이를 중앙 웹 대시보드(Web Server)에서 통합 처리하여 실시간으로 모니터링할 수 있는 에이전트 기반 모니터링 시스템입니다.

## 2. 시스템 아키텍처 (Architecture)

시스템은 **Agent** 모듈과 **Web Server** 모듈로 구성됩니다.
1. **Agent**: 각 리눅스 서버에 바이너리 형태로 독립 실행되며 시스템 명령어를 통해 데이터를 수집하여 Web Server의 REST API로 Push합니다. Java 8 기반으로 최소한의 의존성만 가집니다 (가벼움을 유지).
2. **Web Server**: 에이전트로부터 수집된 데이터를 R2DBC 기반 비동기 Non-blocking으로 MariaDB에 적재하며, 접속해 있는 대시보드 브라우저들에게 **SSE (Server-Sent Events)**를 이용해 실시간으로 브로드캐스팅합니다. Java 17+ 및 Spring WebFlux 기반.

### 2.1 주요 기술 스택 (Tech Stack)
* **Backend Framework**: Java 21 (Web), Java 8 (Agent), Spring Boot 3.x, Spring WebFlux
* **Database**: MariaDB, R2DBC (Reactive Relational Database Connectivity)
* **Frontend**: Vanilla JS (ES6), HTML5/CSS, Thymeleaf, Chart.js, SSE (Server-Sent Events)
* **Agent System**: Bash 쉘 스크립트 기반, Linux Standard tools (`top`, `/proc/stat`, `free`, `df`), 퓨어 자바 스레드 스케줄러.

## 3. 데이터 흐름도

### 3.1 수집 흐름
```text
[ Agent ] 
 ├──> 10초마다 host 정보 (/proc/stat, free, df) 파싱
 ├──> 60초마다 서버 설정(MonitoringConfig) Pull
 ├──> 등록된 서비스별 CPU/MEM/DISK 추가 메트릭 수집
 └──> 로그 파일 (logPath) 키워드 매칭 Tail 모니터링
      |
      v (HTTP POST /api/agents/{id}/metrics 또는 exceptions)
[ Web Server ]
 ├──> Spring WebFlux REST Controller (Reactor Netty)
 ├──> ApiKey 인증 필터 (WebFilter)
 ├──> R2DBC 비동기 DB 인서트 (MariaDB)
 └──> Sinks.Many 브로드캐스트 (Flux) 통과
      |
      v (SSE Event Stream /api/sse/events)
[ Dashboard ]
 └──> 브라우저 수신 후 Chart.js 동적 렌더링 (깜박임 없는 실시간 업데이트)
```

## 4. 데이터베이스 스키마
`schema.sql` 기반으로 R2DBC에서 구동됩니다.
* `agent`: 연결된 서버/에이전트 노드 인벤토리.
* `metric_data`: CPU, MEMORY, DISK의 시계열성 통계 지표.
* `exception_log`: 애플리케이션 구동 중 발생한 에러 라인 스니펫.
* `monitoring_config`: 각 서버별로 모니터링해야 할 타겟 애플리케이션(경로, 수집 항목, 키워드 등).
* `menu`: 사용자 UI용 메뉴 및 권한 관리 테이블.
* `member`: 시스템 접속자(관리자) 계정 관리.
