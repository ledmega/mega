# 챗봇 빌더 시스템 아키텍처 다이어그램

## 1. 전체 시스템 데이터 흐름도

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

## 2. 실시간 RAG 시퀀스 다이어그램

```mermaid
sequenceDiagram
    participant U as 사용자 (위젯)
    participant P as 프록시 서버
    participant DB as RDB (테넌트 설정)
    participant V as 벡터 DB
    participant AI as Gemini API (제미나이)

    U->>P: 질문 전송 (Tenant ID 포함)
    P->>DB: 페르소나 및 위젯 설정 조회
    DB-->>P: 성공 (봇 페르소나 로드 완료)
    
    P->>P: 검색 키워드 추출
    P->>V: 시맨틱 유도사 검색 (KNN)
    V-->>P: 가장 유사한 지식 3~5건 반환
    
    P->>P: 최종 프롬프트 구성 (페르소나 + 지식)
    P->>AI: Gemini API 호출 (답변 생성 요청)
    AI-->>P: AI가 생성한 초안 답변
    
    P->>DB: 대화 이력 및 토큰 사용량 로그 기록
    P->>U: 최종 답변 제공 (위젯 출력)
```

## 3. 아키텍처 주요 구성 요소 설명

- **프론트엔드 위젯 (JS Snippet)**: 고객사 웹사이트에 삽입되는 가벼운 임베드 스크립트입니다.
- **프록시 서버 (Spring Boot)**: API 키 보안 관리 및 RAG 워크플로우를 관장하는 중앙 컨트롤러입니다.
- **RDB (Postgres/Maria)**: 테넌트 설정, 시스템 프롬프트(페르소나), 대화 통계 데이터를 저장합니다.
- **벡터 DB**: 벡터화된 문서 데이터의 고속 유사도 검색을 위한 특화 데이터베이스입니다.
- **웹 크롤러**: 고객사 웹사이트 URL로부터 지식을 주기적/실시간으로 수집하는 엔진입니다.
