# 챗봇 빌더 데이터베이스 ER 다이어그램 (ERD)

멀티테넌트(Multi-tenant) 지원을 위해 설계된 챗봇 빌더용 DB 테이블 간의 관계도입니다.

## 1. 개체 관계도 (ER Diagram)

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
        datetime created_at
    }

    BOT_CONFIG {
        string bot_id PK "UUID"
        string tenant_id FK "고객사 ID"
        string system_prompt "페르소나(임무)"
        string welcome_message "첫 인사말"
        string theme_color "위젯 색상"
        string logo_url "로고 경로"
    }

    KNOWLEDGE_BASE {
        string item_id PK "UUID"
        string bot_id FK "봇 ID"
        string tenant_id FK "고객사 ID (인덱싱 필터용)"
        string source_type "FILE / URL"
        string source_url "수집 주소"
        string content_text "원본 텍스트"
        vector embedding "벡터 데이터 (1536차원)"
    }

    CONVERSATION {
        string conv_id PK "UUID"
        string bot_id FK "봇 ID"
        string external_user_id "사용자 식별값"
        string status "ACTIVE / CLOSED"
        datetime updated_at
    }

    MESSAGE {
        string message_id PK "UUID"
        string conv_id FK "세션 ID"
        string sender_type "USER / BOT"
        string content "메시지 내용"
        boolean is_ai_solution "AI 생성 답변 여부"
        datetime created_at
    }

    USAGE_LOG {
        string log_id PK "UUID"
        string tenant_id FK "고객사 ID"
        string bot_id FK "봇 ID"
        integer request_tokens "질문 토큰"
        integer response_tokens "답변 토큰"
        float cost "계산된 비용"
        datetime called_at
    }
```

## 2. 주요 제약 조건 및 설계 특징

1.  **데이터 격리 (Data Isolation)**: 모든 주요 테이블(`BOT_CONFIG`, `KNOWLEDGE_BASE`, `CONVERSATION`)은 `tenant_id` 또는 `bot_id`를 외래키로 가집니다. 특히 벡터 검색을 수행하는 `KNOWLEDGE_BASE` 테이블은 `tenant_id` 필터를 통해 다른 고객사의 지식이 노출되지 않도록 강제합니다.
2.  **페르소나의 동적 관리**: `BOT_CONFIG` 테이블의 `system_prompt` 컬럼에 각 챗봇만의 고유한 정체성(예: 컴퓨터 전문가, 화난 친구 등)을 저장하며, 백엔드에서 런타임에 이를 로드합니다.
3.  **지식의 벡터화**: `KNOWLEDGE_BASE` 테이블의 `embedding` 컬럼은 실제 벡터 전용 DB(pgvector 등)에서 유사도 검색을 수행하는 핵심 필드입니다.
4.  **과금 데이터 추적**: `USAGE_LOG`를 통해 고객사별로 실시간 API 사용량을 집계하여 관리자 대시보드에서 통계를 제공합니다.
