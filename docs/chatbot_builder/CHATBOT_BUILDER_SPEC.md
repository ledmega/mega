# B2B SaaS 챗봇 빌더 통합 상세 설계서 (Technical Spec)

본 문서는 고객사 맞춤형 RAG 기반 챗봇 서비스를 위한 B2B SaaS 플랫폼의 전체 아키텍처, 데이터 흐름, DB 설계 및 구현 계획을 통합하여 기술합니다.

---

## 1. 개요 및 목표
*   **목표**: 고객사가 고유한 프롬프트 및 RAG 학습 데이터를 기반으로 맞춤형 챗봇을 생성하고, JS 스니펫(Embed Code)을 자사 웹사이트에 연동하는 통합 플랫폼 구축.
*   **핵심 특징**: 중앙 집중형 API Key 관리(Proxy 서버), 테넌트별 데이터 격리, 실시간 RAG 지식 업데이트.

---

## 2. 시스템 아키텍처 및 데이터 흐름

### 2.1 전체 시스템 아키텍처
```mermaid
graph TD
    subgraph "외부 사이트 (고객사)"
        Widget[프론트엔드 임베드 위젯]
    end

    subgraph "관리자 환경"
        Admin[고객 전용 관리자 대시보드]
        Crawler[웹 데이터 수집 / 크롤러]
    end

    subgraph "챗봇 플랫폼 (백엔드)"
        Proxy[Spring Boot 프록시 서버]
        RAG[RAG 로직 / 임베딩]
    end

    subgraph "데이터베이스 계층"
        RDB[(RDB - Postgres/Maria)]
        VDB[(벡터 DB - Milvus/pgvector)]
    end

    subgraph "외부 AI 서비스"
        Gemini[Google Gemini API]
    end

    %% 사용자 상호작용 흐름
    Widget -- "질문 + Client ID" --> Proxy
    Proxy -- "페르소나 및 설정 로드" --> RDB
    Proxy -- "지식 검색 요청" --> RAG
    RAG -- "시맨틱 유사도 검색" --> VDB
    Proxy -- "지식 결합 프롬프트 전달" --> Gemini
    Gemini -- "AI 생성 답변" --> Proxy
    Proxy -- "정제된 최종 답변" --> Widget

    %% 지식 베이스 구축 흐름 (관리자)
    Admin -- "PDF/매뉴얼 업로드" --> RAG
    Admin -- "웹사이트 주소 입력" --> Crawler
    Crawler -- "수집된 콘텐츠 데이터" --> RAG
    RAG -- "인덱싱 (벡터화)" --> VDB
    
    %% 설정 관리 흐름
    Admin -- "페르소나 / 테마 설정" --> RDB
```

### 2.2 실시간 RAG 시퀀스 (고객 문의 처리)
```mermaid
sequenceDiagram
    autonumber
    participant C as 사용자 (Client/Widget)
    participant P as 프록시 서버 (Spring Boot)
    participant R as RDB (테넌트 성격/설정)
    participant V as 벡터 DB (지식 베이스)
    participant AI as Gemini API

    C->>P: 질문 전송 (with Tenant ID)
    P->>R: 테넌트별 페르소나(System Prompt) 조회
    R-->>P: 페르소나 데이터 로드 완료
    
    P->>V: 질문 키워드 기반 시맨틱 유사도 검색
    V-->>P: 매칭된 지식 문맥(Context) 3~5건 반환
    
    P->>AI: [페르소나 + 지식 + 질문] 결합하여 API 호출
    AI-->>P: AI 생성 답변 (Draft)
    
    P->>R: 대화 로그 및 사용량(Token) 기록
    P->>C: 위젯에 최종 답변 출력
```

---

## 3. 데이터베이스 설계 (ERD)

모든 테이블은 데이터 추적을 위해 **감사(Auditing) 컬럼**(`created_at`, `updated_at`, `created_by`, `updated_by`)을 공통으로 포함합니다.

```mermaid
erDiagram
    TENANT ||--o{ BOT_CONFIG : "manages"
    TENANT ||--o{ USAGE_LOG : "has"
    BOT_CONFIG ||--o{ KNOWLEDGE_BASE : "references"
    BOT_CONFIG ||--o{ CONVERSATION : "owns"
    CONVERSATION ||--o{ MESSAGE : "contains"

    TENANT {
        string tenant_id PK "UUID"
        string name "회사/고객명"
        string api_token "인증용 토큰"
        string status "ACTIVE/SUSPENDED"
        datetime created_at
    }

    BOT_CONFIG {
        string bot_id PK "UUID"
        string tenant_id FK "고객사 ID"
        string bot_name "챗봇 이름"
        string system_prompt "페르소나(임무)"
        string theme_color "위젯 포인트 색상"
        string welcome_message "첫 인사말"
    }

    KNOWLEDGE_BASE {
        string item_id PK "UUID"
        string bot_id FK "봇 ID"
        string source_type "FILE/URL/TEXT"
        longtext content_text "추출된 지식 원문"
        vector embedding "벡터 임베딩 데이터"
    }
```

---

## 4. 챗봇 생성 및 학습 흐름 (관리자용)

고객이 직접 지식을 학습시키고 챗봇을 커스터마이징하는 백엔드 내부 흐름입니다.

```mermaid
sequenceDiagram
    autonumber
    participant A as 관리자 (Tenant Admin)
    participant Ctrl as 백엔드 컨트롤러
    participant C as 크롤러 / 파서
    participant E as 임베딩 엔진
    participant V as 벡터 DB

    alt 파일 업로드
        A->>Ctrl: 도메인 지식 파일(PDF/TXT) 업로드
        Ctrl->>C: 텍스트 추출 및 청킹(Chunking)
    else 웹사이트 등록
        A->>Ctrl: 웹사이트 URL 주소 입력
        Ctrl->>C: 웹 크롤러 가동 및 콘텐츠 수집
    end

    C->>E: 정제된 텍스트 뭉치 전달
    E->>E: 고차원 벡터로 변환 (Embedding)
    E->>V: 벡터 DB에 테넌트 ID와 함께 인덱싱
    V-->>A: 학습 완료 및 서비스 준비 완료 알림
```

---

## 5. 단계별 구현 계획

### [Phase 1] 아키텍처 기초 및 DB 구축
- 멀티테넌트 지원을 위한 RDB/벡터 DB 스키마 생성 및 연동 구조 수립.
- 테넌트 식별을 위한 API 키 프록시 구조 설계.

### [Phase 2] 백엔드 코어 및 리얼타임 RAG 개발
- Spring Boot 기반의 Gemini API 대리 호출(Proxy) 모듈 구축.
- **Web Ingestion Engine**: 특정 URL 입력 시 자동으로 크롤링하여 지식화하는 파이프라인.
- 테넌트별 동적 프롬프트 로딩 및 지식 기반 답변 생성 로직.

### [Phase 3] 관리자 대시보드 및 위젯 개발
- 고객사용 챗봇 생성 UI, 프롬프트 에디터, 지식 관리 화면 개발.
- 외부 웹사이트 임베드용 `chatbot-widget.js` SDK 및 보안 통신 적용.
- 사용량(Token) 집계 및 과금 관리 통계 제공.

---
**설명**:
이 통합 문서는 `CHATBOT_BUILDER_ARCHITECTURE`, `DIAGRAM`, `SOURCE_FLOW`, `ER_DIAGRAM` 등을 하나로 합친 최종 스펙 문서입니다. 이후 작업은 이 문서를 기준으로 진행됩니다.
