# B2B SaaS Chatbot Builder Implementation Plan

## Goal Description
고객사가 고유한 프롬프트 및 RAG 학습 데이터를 기반으로 맞춤형 챗봇을 생성하고, 제공받은 JS 스니펫(Embed Code)을 자사 웹사이트에 손쉽게 연동하여 고객지원(CS) 챗봇을 서비스할 수 있는 B2B 통합 플랫폼을 구축합니다. 
가장 큰 특징은 고객(Tenant)별로 개별 제미나이(Gemini) API 키를 발급받을 필요 없이, 우리 메인 서버가 중앙에서 통합된 API 키를 활용하여 대리 호출(Proxy)하며 보안과 비용 효율을 달성하는 것입니다.

*   **[아키텍처 다이어그램 보기](file:///e:/ws/mega/docs/chatbot_builder/CHATBOT_BUILDER_DIAGRAM.md)**

## System Architecture

### 1. Frontend Widget (`chatbot-widget.js`)
* **역할**: 고객사 웹사이트 `<head>` 또는 `<body>` 영역에 삽입되어 브라우저 우측 하단에 채팅 UI를 렌더링.
* **통신**: 부여받은 고유 `Client ID`를 사용하여 백엔드 Proxy Server에 WebSocket / SSE 형태로 양방향 통신.
* **보안**: API Key가 클라이언트에 절대 노출되지 않으며, `Client ID`와 화이트리스트 지정 도메인(CORS) 조합으로 접근 제어.

### 2. Admin Dashboard
* **역할**: 챗봇 소유주(고객사)가 봇의 지능과 외형을 관리하는 관리자 화면.
* **기능**:
  * **프롬프트 관리**: 고객사가 직접 "페르소나(임무)"를 정의하고 시스템 프롬프트를 DB에 저장/수정.
  * **Knowledge Base (RAG 데이터)**: PDF, 매뉴얼, Q&A 텍스트 업로드 및 사이트 URL 크롤링 관리.
  * **배포 설정**: 챗봇 위젯 테마 색상, 웰컴 메시지 설정 및 스니펫 발급.
  * **Analytics & Billing**: 사용자 대화 이력 조회, 실시간 토큰 사용량 및 과금 통계 대시보드.
  * **Human Escalation**: AI가 답변 불가 시 실제 상담원 채팅창으로 전환 알림 기능.

### 3. Backend Proxy Server (Spring Boot / WebFlux)
* **역할**: 트래픽 라우팅, RAG 적용, LLM 호출의 중추.
* **기능**:
  * **Tenant 식별**: Inbound 요청의 `Client ID`를 파악하여 해당 고객사 설정값(DB) 조회.
  * **RAG 파이프라인**: 고객의 질문을 임베딩하여 Vector DB에서 고객사 전용 문서를 검색(Semantic Search).
  * **LLM Integration**: 검색된 문맥(Context)과 고객사 프롬프트를 융합해 Google Gemini API를 안전하게 호출.

### 4. Database Layer (Multi-tenant Structure)
* **RDB (PostgreSQL / MariaDB)**:
  * `TENANT`: 고객사 계정 및 API 인증 정보.
  * `BOT_CONFIG`: **테넌트별 시스템 프롬프트(Persona)**, 위젯 UI 설정(색상, 로고).
  * `CHAT_CONVERSATION`: 대화 세션 및 상태 관리.
  * `USAGE_LOG`: 토큰 사용량, API 호출 로그 (과금 및 통계용).
* **Vector DB (Milvus, pgvector 등)**: RAG 활용을 위한 임베딩 데이터. `Tenant ID` 컬럼을 통한 논리적 파티셔닝(Data Isolation) 필수 적용.

---

## Phased Implementation

### [Phase 1] 기반 아키텍처 및 데이터베이스 설계
* Multi-tenant RDB 스키마 구성 (Tenants, Prompts, WidgetConfig, ChatLogs).
* Vector DB 프로비저닝 및 RDB와의 연동 구조 정립.

### [Phase 2] Backend Core & LLM 통합
* Spring Boot 기반 API Gateway 및 LLM Proxy 기능 구축.
* 텍스트 임베딩 파이프라인 (문서 업로드 -> 청킹(Chunking) -> 임베딩 -> Vector DB 저장) 구현.
* **Web Ingestion Engine**: 특정 URL(사이트 주소) 입력 시 사이트 전체를 크롤링하여 지식화하는 자동 수집 파이프라인 구축.
* Client ID 기반의 RAG + Gemini 응답 생성 비즈니스 로직 작성.

### [Phase 3] Admin Dashboard 개발
* 고객사용 회원가입 / 로그인 시스템 구축.
* 프롬프트 에디터, 학습 문서 업로드 관리, 연동 스니펫 발급 UI 제공.

### [Phase 4] Embed Widget SDK 및 보안 적용
* 어떤 웹사이트에서든 깨지지 않고 구동되는 격리된(Iframe 또는 Shadow DOM 기반) 챗봇 UI 리액트/바닐라JS 개발.
* CORS 설정 도메인 검증 기능 및 Rate Limiter 추가.

---

## Verification Plan

### Test Scenarios
* **데이터 격리 테스트**: 두 고객사(A, B)가 업로드한 데이터가 서로 교차되어 검색되지 않는지 검증.
* **보안 테스트**: 스니펫 소스에서 어떤 방식으로든 중앙 API 키가 탈취 불가능한 구조인지, 인가되지 않은 도메인에서 `Client ID` 도용 호출 시 차단되는지 확인.
* **RAG 정확도**: 임의의 문서를 업로드했을 때, 제미나이가 정확히 해당 문서를 참고하여 할루시네이션(환각) 없는 답변을 도출하는지 품질점검.
