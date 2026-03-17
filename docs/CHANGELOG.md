# MEGA Project - Changelog

## 2026-03-17 - CS AI 자동화 시스템 구축 및 버그 수정

### 🤖 신규: CS 자동화 시스템 (AI 고객 상담 자동화)
* **`CsBotService`** 구현: FAQ 키워드 매칭 → OpenAI RAG 기반 답변 초안 생성 전체 흐름 완성.
* **CS 어드민 대시보드** (`/cs/dashboard`): 상담 내역(채팅 뷰), FAQ 관리 CRUD, 시뮬레이터 3탭 UI 구현.
* **`CsSimulationController`**: 외부 연동 없이 문의 유입부터 AI 처리까지 E2E 테스트 가능한 시뮬레이터 API.
* **`CsAdminController`**: CS 대시보드 페이지 뷰 컨트롤러.
* **`CsAiConfig`**: Spring AI `ChatClient` 빈 등록.
* **`SseService`**: `CS_EVENT` 타입 실시간 브로드캐스팅 메서드 추가.

### 🐛 버그 수정
* **`AgentHeartbeat` HTTP 500 에러**: `Persistable` 인터페이스 미구현으로 R2DBC가 신규 하트비트를 UPDATE로 처리하던 문제 수정. `isNew=true` 설정 추가.
* **에이전트 ID 불일치 (Agent ID Mismatch)**: `ApiClient.RegisterResponse`의 `agentId` 필드가 서버 응답 필드명(`agentRefId`)과 달라 null이 저장되던 문제 수정. `@SerializedName` 어노테이션 추가.

---

## 2026-03-12 ~ 2026-03-13 - 배치 스케줄러 고도화 및 로그 분리


## 1. 배치 스케줄러 고도화 (Batch Scheduling)
*   **Cron 표현식 지원**: 기존의 고정 주기(Interval) 방식 외에 복잡한 스케줄링이 가능한 **Cron 표현식**을 지원합니다. (예: 매일 밤 11시 수행 등)
*   **Spring TaskScheduler 도입**: `ScheduledExecutorService` 대신 스프링 프레임워크와 통합된 `TaskScheduler`를 사용하여 보다 안정적이고 정밀한 스케줄링 제어가 가능해졌습니다.
*   **동적 파라미터 관리**: 배치 작업 유형별로 `Retention Days`(보관 주기) 등 복잡한 설정값을 DB에서 동적으로 관리할 수 있도록 구조를 개선했습니다.

## 2. 대시보드 시각화 개선 (Dashboard & Chart.js)
*   **기간별 필터링 기능 추가**: 실시간 대시보드에서 최근 1시간, 12시간, 1일, 5일, 10일 단위로 메트릭을 조회할 수 있는 필터 버튼을 추가했습니다.
*   **데이터 다운샘플링 (Downsampling)**: 장기 데이터 조회 시 차트가 너무 촘촘해지는 문제를 해결하기 위해, 기간에 따라 데이터 포인트를 적절히 축소하는 알고리즘을 도입했습니다.
*   **지능형 차트 렌더링**: 데이터 밀도에 따라 점(Point)의 크기를 자동으로 조절(`pointRadius`)하며, 기간이 길어질 경우 X축 라벨에 날짜 정보를 자동으로 포함하여 가독성을 높였습니다.

## 3. 로그 시스템 고도화 및 최적화 (Logging & Operations)
*   **업무 도메인별 로그 분리**: 하나의 거대한 로그 파일에서 벗어나 업무 성격에 따라 4가지 전용 로그로 분리하였습니다.
    *   `mega-batch.log`: 배치 스케줄러 전용
    *   `mega-agent.log`: 에이전트 통신 및 상태 전용
    *   `mega-security.log`: 보안 및 인증 전용
    *   `mega-alert.log`: 주요 경고 및 알림 전용
*   **관리 스크립트 개선 (`run.sh`, `agent.sh`)**:
    *   분리된 로그를 실시간으로 모니터링할 수 있는 전용 명령어 추가 (`log-batch`, `log-agent`, `log-sec`, `log-alert`).
    *   `app.log` 무한 증식 문제 해결: 부팅 로그(`boot.log`) 분리 및 덮어쓰기 방식으로 변경하여 수 기가바이트(GB) 규모의 로그 비대화 문제를 원천 차단했습니다.
    *   자동 권한 관리: `gradlew` 실행 권한이 없을 경우 스크립트가 자동으로 `chmod`를 수행하도록 보완했습니다.
*   **운영 안정성**: `git pull` 과정에서 발생하는 스크립트 충돌 문제를 해결하기 위해 업데이트 프로세스를 수동 `git pull` 후 `rebuild`로 정립하였습니다.

---
**MEGA Team - Advanced Agentic Coding**
