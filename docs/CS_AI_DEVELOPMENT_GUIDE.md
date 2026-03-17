### CS업무관련 문제및 개선방안 
현 CS인원 4 및 메인업무 구조 - CS 업무처리 검수관리자 1 / 보안업무 포탈관련 주처리자 1 / 메일 문의접수 주처리, 유선보조대응 1 
/ 메일 문의접수 보조처리, 유선주대응, 주간OP일부지원 1

CS 관련 업무 문제점 및 자동화 방식 제안 정리
문제점
업무 접수 부터 처리까지 자동화 내지 시스템화 되어져있는 부분이 없음
유선 접수시 대표메일로 접수하도록 유도하며, 대표메일 단순문의경우 반복접수 처리시에는 대표메일로 발송한 기존 답변을 고정기능 통해서 남겨두었다가 
참고하여 수동으로 작성 처리 중
그외 특수경우나 시스템등 프로그램관련 문의시 레드마인 통한 운영자 확인 요청 진행하여 답변 받아서 회신 처리함


CS업무에대한 제안
유선 노출을 줄임 - 당장 끊어버리기는 힘들며 단순 엔드사용자와 포탈사용자들에 대한 대응방법 강구 이후 공지후 차단 - 대표메일이나 별도 접수 시스템구축후 유도
현재는 대표메일 통한 접수 처리 및 포탈 1대1문의 기능등을 통한 시스템 등록 접수 하고있음 - 시스템통한 자동 등록 및 처리후 답변 자동발송 회신을 목표로 
하며 진행에 대한 요청자에게 SMS/알림톡/이메일등 통한 상태 알람 제공
현재 레드마인 통해 처리된 업무처리 분류 및 처리 규격화 진행 포함하여 FAQ 데이터 등록 및 현행화 진행하여 반복 문의건과 그외 건들 분류 작업 진행
현재 포탈사이트는 발송등 사용자 위주로의 기능으로 노출되어야 하고 기본적인 어드민 기능 사이트 분리 필요하며 CS 업무처리 관리 사이트 필요


자동화 기능에 대한 제안
FAQ 등록된 데이터 기반으로 단순 문의 사항에 대한 사용자 직접 문의 검색 기능 추가 혹은 최종적으로 답변 자동화 필요 - 반복 문의에 대한 업무 자동화 우선 진행
- 24시간 챗봇통한 기본적인 반복문의 답변 자동화 / 그외 업무시간내 전문적이거나 특수 문의 요청시 CS에서 답변작성시 FAQ 데이터를 기초로한 AI가 답변 초안 작성 지원
- 처리된 CS건들에 대한 일/주/월 단위 리포트 자동생성 필요 - 평균 처리시간 / 재문의율 / 고객사별 문의내역 / 처리자들의 능률평가 등
- 자동화 진행 순서 : FAQ등 데이터 정리, 챗봇도입, 문의내용 자동 분류(키워드 및 AI분석), 답변초안 AI적용, CS처리 리포트 자동화
- 자동화 기능에 대한 상황 모니터링 및 예외상황에 CS가 직접 대응하는 기능 필요






# CS 메시징 AI 시스템 개발 가이드

이 문서는 AI 기반 CS 자동화 및 지원 시스템의 개발 방향과 상세 Java 클래스 구조를 설명합니다.

## 1. 개발 전략: "인터페이스 우선 & 시뮬레이션 기반"
현재 외부 망과의 직접적인 연결이 어려운 상황을 고려하여 다음과 같은 단계로 개발합니다.

1.  **규격화 (Mocking)**: 이메일, 레드마인, 톡드림에서 들어올 데이터를 JSON 스펙으로 정의합니다.
2.  **시뮬레이터 구축**: 외부 시스템 없이도 내부 API(`CsSimulationController`)를 통해 메시지 유입부터 AI 처리까지의 전 과정을 테스트합니다.
3.  **RAG (현행화 학습)**: 모델을 새로 만드는 것이 아니라, 질문이 들어왔을 때 관련 로그와 FAQ를 검색하여 AI에게 "참고 문헌"으로 제공하는 방식을 사용합니다.

---

## 2. 주요 Java 클래스 및 역할

### [Entity 계층] - 데이터 구조 정의
*   `CsConversation`: 채팅 상담의 세션 관리 (상태: 진행중, 완료, 대기).
*   `CsMessage`: 개별 메시지 저장 (발신자, 내용, 타입: USER/BOT/ADMIN).
*   `CsFaq`: 자동 답변용 지식 베이스.
*   `CsReport`: 일/주/월 단위 통계 데이터.

### [Service 계층] - 비즈니스 로직
*   `CsBotService`: **이 프로젝트의 핵심 엔진**.
    *   질문을 받아 FAQ 매칭 시도.
    *   FAQ가 없으면 OpenAI API를 호출하여 요약 및 답변 초안 생성.
    *   `ExceptionLogRepository` 등을 참조하여 시스템 장애 데이터 수집.
*   `TalkDreamClient`: LG CNS 톡드림 API와 통신하여 실제 카톡/SMS 발송.
*   `SseService`: (기존 활용) 상담사 어드민 웹 페이지에 실시간 메시지 도착 알림.

### [Controller 계층] - 외부 접점
*   `TalkDreamWebhookController`: 실제 서비스 오픈 시 톡드림 서버로부터 메시지를 받는 창구.
*   `CsAdminController`: 상담사 전용 대시보드 API (사용자 포탈과 분리).
*   `CsSimulationController`: **개발 전용**. 외부 연동 없이 HTTP 호출로 가상의 문의를 발생시키는 도구.

---

## 3. 전체 흐름도 (Data Flow)

1.  **문의 유입**: (시뮬레이터 or 외부 웹훅) -> `Controller`
2.  **분석**: `CsBotService`가 OpenAI를 활용하여 문의 분류 (단순 FAQ vs 복잡 문의).
3.  **처리**:
    *   **단순**: `CsFaq` 검색 후 즉시 `TalkDreamClient`로 자동 회신.
    *   **복잡**: 최근 로그/통계 수집 -> AI가 요약 및 초안 작성 -> `SseService`로 상담사에게 알림.
4.  **완료**: 상담사가 확인/수정 후 발송 -> `TalkDreamClient` 발송.

---

## 5. 현재 구현 완료 사항 (Status)

### 5.1 로그 시스템 분리
*   **로그 파일**: `logs/mega-cs-ai.log`
*   **대상**: AI 추론 과정, 톡드림 웹훅 수신 로그, 시뮬레이션 데이터 흐름.
*   **설정 파일**: [`logback-spring.xml`](file:///e:/ws/mega/webserver/src/main/resources/logback-spring.xml)

### 5.2 데이터 모델링 및 인프라 (R2DBC)
*   **엔티티**: `CsFaq`, `CsConversation`, `CsMessage`, `CsInboundData` 생성 완료.
*   **레포지토리**: `CsFaqRepository`, `CsConversationRepository`, `CsMessageRepository`, `CsInboundDataRepository` 생성 완료.
*   **DB 테이블**: `cs_ai_tables.sql` 실행 완료. (cs_faq, cs_conversation, cs_message, cs_inbound_data, cs_report)
*   **의존성**: `Spring AI OpenAI` 라이브러리 추가 완료.
*   **ID 체계 개편**: 모든 테이블의 ID를 `Long`에서 고유 Prefix를 가진 `String`으로 변경하였습니다.
    *   `CsFaq`: `FAQ0000000001`
    *   `CsConversation`: `CON0000000001`
    *   `CsMessage`: `MSG0000000001`
    *   `CsInboundData`: `INB0000000001`
    *   상세 내용은 [REF_ID_OVERHAUL_GUIDE.md](./REF_ID_OVERHAUL_GUIDE.md)를 참고하세요.

### 5.3 CsBotService 구현 완료 (2026-03-17)
*   **파일**: `led.mega.cs.service.CsBotService`
*   **처리 흐름**:
    1.  수신 원본을 `cs_inbound_data` 에 Raw 저장 (감사 추적)
    2.  기존 열린 상담 세션 재사용 또는 신규 세션(`cs_conversation`) 생성
    3.  사용자 메시지를 `cs_message` 에 저장
    4.  FAQ 키워드 매칭 (`CsFaqRepository.searchFaq`) 시도
        *   **매칭 성공**: BOT 메시지 저장 → `AUTO_REPLIED` 반환 (즉시 응대)
        *   **매칭 실패 (RAG 도입)**: 
            *   사용자 질문 키워드로 관련 FAQ 리스트 검색.
            *   검색된 FAQ 데이터를 **System Prompt의 Context**로 주입.
            *   OpenAI API(Gemini) 호출하여 우리 데이터 기반 답변 초안(`isDraft=true`) 생성.
            *   **성능 최적화**: `WebClient`를 이용한 비동기 논블로킹 통신 및 60초 타임아웃 적용 (ReadTimeout 방지).
            *   `DRAFT_CREATED` 반환 및 상담사 알림.
    5.  SSE(`CS_EVENT` 이벤트)를 통해 관리자에게 실시간 알림
*   **AI 설정**: `led.mega.cs.config.CsAiConfig` - `ChatClient` 빈 등록 완료

### 5.4 시뮬레이터 컨트롤러 구현 완료 (2026-03-17)
*   **파일**: `led.mega.cs.controller.CsSimulationController`
*   **역할**: 실제 외부 시스템(카카오, 이메일) 없이 전체 CS 흐름 테스트 가능
*   **주요 API**:

    | Method | Path | 설명 |
    |--------|------|------|
    | `POST` | `/api/cs/simulate/inbound` | 가상 문의를 발생시켜 봇 처리 흐름 테스트 |
    | `GET`  | `/api/cs/faq` | 활성 FAQ 목록 조회 |
    | `POST` | `/api/cs/faq` | FAQ 등록 |
    | `PUT`  | `/api/cs/faq/{id}` | FAQ 수정 |
    | `DELETE` | `/api/cs/faq/{id}` | FAQ 비활성화 (soft delete) |
    | `GET`  | `/api/cs/conversations` | 상담 세션 전체 조회 |
    | `GET`  | `/api/cs/conversations/{id}/messages` | 특정 세션의 메시지 히스토리 조회 |

---

## 6. 향후 확장성
*   **Redmine 연동**: 추후 엔지니어의 확인이 필요한 건은 자동으로 Redmine Issue를 생성하는 기능을 추가할 수 있습니다.
*   **Vector DB 도입**: FAQ 데이터가 수천 건 이상으로 많아지면 단순 텍스트 검색 대신 벡터 검색(Vector Search)으로 품질을 높일 수 있습니다.
*   **TalkDreamWebhookController**: 실제 서비스 오픈 시 톡드림 서버로부터 메시지를 받는 창구 구현 예정.
*   **CsAdminController + 어드민 화면**: 상담사가 초안을 확인·수정·발송할 수 있는 전용 대시보드 구현 예정.
*   **CsReportService**: 일/주/월 단위 처리 통계 자동 생성 배치 구현 예정.

---

## 7. 테스트 방법 (시뮬레이터 사용)

### Step 1: FAQ 데이터 등록
```bash
curl -X POST http://localhost:8080/api/cs/faq \
  -H "Content-Type: application/json" \
  -H "Cookie: SESSION=<로그인_세션>" \
  -d '{
    "category": "배정",
    "question": "배정이 어떻게 되나요?",
    "answer": "배정은 영업일 기준 1~2일 이내에 처리됩니다. 배정 완료 시 이메일로 안내드립니다.",
    "tags": "배정,처리,이메일"
  }'
```

### Step 2: 가상 문의 발생 (봇 처리 테스트)
```bash
curl -X POST http://localhost:8080/api/cs/simulate/inbound \
  -H "Content-Type: application/json" \
  -H "Cookie: SESSION=<로그인_세션>" \
  -d '{
    "source": "EMAIL",
    "externalId": "test@example.com",
    "content": "배정 관련해서 문의드립니다. 언제쯤 처리되나요?"
  }'
```

### Step 3: 결과 확인
```bash
# 상담 세션 확인
curl http://localhost:8080/api/cs/conversations -H "Cookie: SESSION=<로그인_세션>"

# 특정 세션의 메시지 히스토리
curl http://localhost:8080/api/cs/conversations/CON0000000001/messages -H "Cookie: SESSION=<로그인_세션>"
```
