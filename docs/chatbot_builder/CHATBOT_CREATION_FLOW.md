# 챗봇 커스터마이징 및 생성 흐름도 (관리자용)

고객(테넌트)이 우리 사이트(Admin Dashboard)에 접속하여 자신만의 맞춤형 챗봇을 설계하고 배포하는 구체적인 과정입니다.

## 1. 단계별 생성 및 학습 흐름 (Sequence Diagram)

```mermaid
sequenceDiagram
    autonumber
    participant A as 고객 (Tenant Admin)
    participant UI as 관리자 대시보드 (React)
    participant Ctrl as Tenant/Bot Controller
    participant K as Knowledge Service (RAG)
    participant V as 벡터 DB (Milvus/pgvector)
    participant R as RDB (Postgres/Maria)

    Note over A, UI: 1. 기본 설정 및 페르소나 설계
    A->>UI: 챗봇 이름 및 페르소나(프롬프트) 입력
    UI->>Ctrl: POST /api/admin/bot/config
    Ctrl->>R: BOT_CONFIG 테이블에 페르소나 저장
    R-->>Ctrl: 저장 완료
    Ctrl-->>UI: 설정 반영 완료

    Note over A, UI: 2. 지식 데이터 학습 (파일/URL)
    alt 파일 업로드 방식
        A->>UI: 메뉴얼 PDF 등 파일 업로드
        UI->>Ctrl: POST /api/admin/knowledge/upload
        Ctrl->>K: 파일 텍스트 추출 및 청킹(Chunking)
    else 웹사이트 크롤링 방식
        A->>UI: 회사 웹사이트 URL 입력
        UI->>Ctrl: POST /api/admin/knowledge/crawl
        Ctrl->>K: 웹 크롤러(Scraper) 구동 및 텍스트 추출
    end

    K->>K: 텍스트 벡터화 (Embedding)
    K->>V: 벡터 DB에 Tenant ID와 함께 지식 저장(Indexing)
    V-->>K: 인덱싱 완료
    K-->>UI: 학습 완료 및 지식 베이스 활성화

    Note over A, UI: 3. 배포 및 연동
    A->>UI: 위젯 디자인 설정 (색상, 로고)
    UI->>Ctrl: PATCH /api/admin/bot/design
    Ctrl->>R: 위젯 테마 정보 업데이트
    UI->>UI: 자바스크립트 스니펫(Snippet) 생성
    UI->>A: 스크립트 코드 복사 제공 (<script src="..."></script>)
```

## 2. 주요 단계별 처리 데이터

| 단계 | 입력 데이터 (Input) | 결과 데이터 (Output) | 저장 장소 |
| :--- | :--- | :--- | :--- |
| **페르소나 설정** | 시스템 프롬프트 문구 | `system_prompt` 필드 업데이트 | RDB (`BOT_CONFIG`) |
| **지식 데이터 수집** | PDF, 텍스트, 웹사이트 URL | 원문 데이터 추출 및 정제 | 임시 저장소/DB |
| **지식 데이터 학습** | 정제된 텍스트 | 1536차원 이상의 **수치형 벡터** | **벡터 DB** |
| **위젯 배포** | 고객사 웹사이트 도메인 | `Client ID` 기반 JS 코드 | 관리자 화면 |

---
**설명**:
이 과정이 완료되면, 앞서 살펴본 `CHATBOT_SOURCE_FLOW.md`의 흐름에 따라 사용자의 질문을 받았을 때 **여기서 저장된 페르소나와 지식 데이터**를 조합하여 답변하게 됩니다.
