# MEGA Coding Convention & Instruction (AI 전용 지침)

이 문서는 AI(Antigravity)가 이 프로젝트를 개발하고 수정할 때 반드시 준수해야 하는 **글로벌 코딩 규칙**을 정의합니다.

---

## 🏗 공통 코딩 규칙 (General Rules)

### 1. 상세한 주석 작성 (Detailed Documentation)
*   **Javadoc & Commenting**: 모든 클래스, 인터페이스, 공용 메서드 상단에 반드시 Javadoc 형식의 주석을 작성한다.
*   **Inline Comments**: 복잡한 비즈니스 로직, 조건문, 정규식 등은 그 의미와 **"왜 이렇게 작성했는지(Rationale)"**를 한글로 아주 상세하게 설명한다.
*   **CSS/JS Rules**: UI 인터랙션이나 스타일 정의 시, 특정 수치(px, transition 등)를 사용한 이유를 주석으로 명시한다.

### 2. 가독성 및 명명 규칙 (Naming & Readability)
*   변수명과 메서드명은 의미가 명확하도록 충분히 길고 서술적으로 작성한다.
*   매직 넘버(Magic Number)나 리터럴 문자열은 상수로 정의하거나 상세 주석을 달아 설명한다.

---

## 🤖 AI 업무 영역 특화 (Domain Specific)

### 톡드림(TalkDream) 메시징 비즈니스
*   알림톡, SMS, LMS, MMS, RCS 및 부달(재발송) 서비스 관련 로직 수정 시, 해당 도메인의 비즈니스 흐름(규격, 심사, 실패 처리 등)을 고려하여 주석을 단다.
*   Gemini AI 프롬프트나 RAG 연동 로직 수정 시, "메시징 도메인 전문가"의 관점에서 답변을 생성하도록 유지한다.

---

## 🚀 개발 프로세스 (Workflow)

1.  **항상 `git push`**: 모든 수정 사항은 작업 완료 즉시 `origin` 브랜치에 푸시한다.
2.  **문서 동기화**: 소스 코드 수정 시 관련 설계 문서(`docs/*.md`)나 `README.md`에 변경 사항이 있다면 함께 최신화한다.
3.  **오류 검증**: 수정 후에는 가능한 한 프로젝트 빌드나 문법 검증을 거친 후 푸시한다.
