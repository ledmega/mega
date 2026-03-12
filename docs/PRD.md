# MEGA (Monitoring & Error Gathering Agent) - Product Requirements Document

## 1. 개요 (Overview)
MEGA는 분산된 리눅스 서버 환경에서 시스템 자원(CPU, Memory, Disk) 및 애플리케이션 Exception 로그를 실시간으로 수집하고, 이를 중앙 웹 대시보드(Web Server)에서 통합하여 실시간으로 모니터링할 수 있는 에이전트 기반 모니터링 시스템입니다.

---

## 2. 구현 완료된 기능 (Implemented Features)

### 2.1. Agent (에이전트)
* **Linux 네이티브 명령어 기반 수집**: `top -bn1`, `cat /proc/stat`, `free -m`, `df -h` 등 리눅스 표준 명령어를 자체 스케줄러를 통해 실행 후, 파싱하여 데이터를 추출.
  * **유연한 수집 주기 설정**: `application.properties`를 통해 메모리, CPU, 디스크, Exception 수집 주기를 독립적으로 설정 가능.
  * **Memory**: 사용률(%), 가용량(MB) 수집.
  * **CPU**: 점유율(User %) 수집.
  * **Disk**: 파티션별 모든 볼륨 사용률(%) 수집.
* **에이전트 라이프사이클 관리**: `agent.sh` 스크립트를 통해 백그라운드 구동(start/stop/status) 가능.
* **자동 식별 및 등록**: 부팅 시 `hostname` 및 IP 주소를 자동으로 탐지하여 서버에 등록 후 발급된 API키를 로컬 `.agent_id` 파일에 캐싱.
* **동적 서비스 스케줄러 (P1)**: 서버에서 `monitoring_config` 목록을 Pull하여 신규/변경/삭제된 서비스 모니터링(예: Nginx, Docker) 작업을 실시간/동적으로 병렬 처리.
* **로그 키워드 및 다중 경로 모니터링 (P2)**: 설정된 여러 경로(`task.exception.log.paths`) 파일 포지션(Offset) 기준으로 추가될 때마다 즉각 파싱하여 Exception 이벤트로 발송.

### 2.2. Web Server (중앙 모니터링 서버)
* **리액티브 비동기 스택**: Spring WebFlux, R2DBC 스택을 활용하여 서버 오버헤드 최소화.
* **실시간 SSE (Server-Sent Events) 통합**: 에이전트로부터 수집된 데이터(`Metric`, `Exception`, `Heartbeat`)를 저장하자마자 `SseService`를 통해 대시보드로 즉시 브로드캐스팅.
* **보안 계층**: API Key 기반으로 에이전트를 인증, 웹 접근자는 Spring Security Form Login(CustomUserDetailsService)을 통해 인가된 사용자만 접근. (인증정보 분리)
* **서비스 관리 CRUD API**: `MonitoringConfig` 엔티티 기반으로 서비스별 모니터링 설정(경로, 로그, 수집 항목, 인터벌)을 동적으로 등록/수정/삭제하는 환경 구축.
* **시간대 픽스**: OS 기본 타임존과 독립적으로 KST(Asia/Seoul)를 강제 설정해 로그/메트릭 기록 일관성 도모.

### 2.3. Dashboard (대시보드 화면)
* **넓은 뷰 (Full-Width View)**: 에이전트 상태 목록과 차트 영역을 세로/가로 뷰로 배치.
* **동적 차트 자동 생성 (Chart.js)**: Host 단일 차트 및 개별 서비스 `monitoring_config`별 차트를 탭 없이 한 화면에 다중 생성.
* **과거 데이터(History) 시각화**: 브라우저 접속 즉시 DB에서 최신 메트릭을 불러들여 기존 히스토리 복원 (KST 변환).
* **깜박임 없는 실시간 업데이트 (Flicker-free)**: `Chart.js`의 `.update()` 및 `DOM` Text Node 교체 방식을 통해 렌더링 부하 없는 10초 주기 매끄러운 Refresh 반영.
* **Service 목록 UI 풀 폴링**: 대시보드 뿐만 아니라 `/services` 전체 목록 페이지에서도 SSE 또는 AJAX Polling을 통해 상태 배지 및 최신 지표 실시간 감시 가능.

## 3. 남은 작업 (Backlog)
- [x] 전역 Navigation UI 통일 (Dashboard, Admin Pages 공통 디자인 적용 요소 구축)
- [ ] 현재 대부분의 코어 시스템 완료. 추가 권한 체계 정교화 및 알럿(Slack/이메일) 트리거 시스템.
- [ ] 시스템 메모리 누수 방어를 위한 Agent/Webserver 자원 사용량 최적화 (Profiling).
