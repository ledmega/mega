# MEGA (Monitoring & Error Gathering Agent) - Product Requirements Document

## 1. 개요 (Overview)
MEGA는 분산된 리눅스 서버 환경에서 시스템 자원(CPU, Memory, Disk) 및 애플리케이션 Exception 로그를 실시간으로 수집하고, 이를 중앙 웹 대시보드(Web Server)에서 통합하여 실시간으로 모니터링할 수 있는 에이전트 기반 모니터링 시스템입니다.

## 2. 시스템 아키텍처 (Architecture)

시스템은 **Agent** 모듈과 **Web Server** 모듈로 구성되며, 단방향 Pull & Push 방식으로 동작합니다.
1. **Agent (클라이언트)**: 각 리눅스 서버에 바이너리 형태로 독립 실행되며 데이터를 능동적으로 수집(Pull)하여 서버의 REST API로 전송(Push)합니다.
2. **Web Server (중앙 서버)**: 에이전트로부터 수집된 데이터를 MariaDB에 지속(Persist)시키고, 접속해 있는 대시보드 브라우저들에게 **SSE (Server-Sent Events)**를 이용해 실시간으로 브로드캐스팅합니다.

## 3. 주요 기능 (Key Features)

### 3.1. Agent (에이전트)
* **Linux 네이티브 명령어 기반 수집**: `top -bn1`, `free -m`, `df -h` 등 리눅스 표준 명령어를 자체 스케줄러를 통해 실행 후, 정규식 파싱으로 데이터를 추출하여 종속성을 최소화합니다.
  * **Memory**: 사용률(%), 가용량(MB)을 30초 단위로 수집.
  * **CPU**: 점유율(User %)을 30초 단위로 수집.
  * **Disk**: 파티션별 루트 볼륨 사용률(%)을 1분 단위로 수집.
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

## 4. 기술 스택 (Tech Stack)
* **Backend Framework**: Java 17, Spring Boot 3.x, Spring WebFlux
* **Database**: MariaDB, R2DBC, Spring Data R2DBC
* **Frontend**: Vanilla JS (ES6), HTML5/CSS, Thymeleaf, Chart.js (EventSource/SSE 기반)
* **Agent System**: Bash 쉘, Linux Standard tools (`top`, `free`, `df`)
