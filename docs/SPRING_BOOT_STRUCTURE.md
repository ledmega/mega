# MEGA Spring Boot 프로젝트 구조 가이드 (Architecture Guide)

이 문서는 MEGA(Monitoring & Error Gathering Agent) 프로젝트의 서버(Web Server) 아키텍처와 주요 패키지 구성을 설명합니다. 프로젝트 소스 코드를 분석하고 수정하기 전에 이 구조를 먼저 파악하는 것이 중요합니다.

---

## 🏗 전체 아키텍처 (Top-Level View)

이 프로젝트는 **Spring Boot 3.x** 기반의 **Reactive Stack(WebFlux)**으로 구성되어 있습니다. 비동기/논블로킹(Non-blocking) 방식으로 수천 개의 에이전트로부터 유입되는 데이터를 효율적으로 처리합니다.

---

## 📂 패키지별 상세 역할 (Package Roles)

가장 핵심적인 `webserver/src/main/java/led/mega` 폴더 아래의 패키지 구성입니다.

### 1. `MegaApplication.java`
*   **역할**: 프로젝트의 시작점(Entry Point)입니다. `@SpringBootApplication` 어노테이션을 통해 설정을 로드하고 서버를 구동합니다.

### 2. `controller` (Web Layer)
*   **역할**: 사용자 및 외부 시스템의 API 요청을 처리합니다.
*   **특징**: WebFlux를 사용하여 리턴 타입이 `Mono<T>` 또는 `Flux<T>`인 경우가 많습니다.
*   **예시**: 대시보드 화면 전환, 데이터 조회 API 등.

### 3. `service` (Business Layer)
*   **역할**: 핵심 비즈니스 로직을 수행합니다. 데이터 가공, AI 연동, 복합적인 데이터 처리 등을 담당합니다.
*   **예시**: `CsBotService.java` (AI 챗봇 로직), `SseService.java` (실시간 SSE 통신).

### 4. `repository` (Data Access Layer)
*   **역할**: 데이터베이스(DB)와의 통신을 담당하는 인터페이스입니다.
*   **특징**: `R2DBC`를 사용하여 데이터베이스 I/O도 논블로킹으로 처리합니다. 에이전트 정보나 로그 등을 SQL로 조회하거나 저장합니다.

### 5. `entity` (Persistence Domain)
*   **역할**: 데이터베이스 테이블과 1:1로 매핑되는 객체입니다.
*   **예시**: `CsFaq.java`, `CsMessage.java` 등. 테이블의 각 컬럼이 필드로 정의되어 있습니다.

### 6. `dto` (Data Transfer Object)
*   **역할**: 계층 간 데이터 전송을 위해 사용되는 객체입니다. API 요청 바디(Request)나 응답 바디(Response)를 정의할 때 주로 사용합니다. (Entity와 직접 통신하지 않고 보안/유연성을 위해 DTO를 사용)

### 7. `config` (Infrastructure Configuration)
*   **역할**: 시스템 전반의 설정을 담당합니다.
*   **주요 구성**:
    *   `SecurityConfig.java`: 인증/인가 및 API 접근 권한 설정.
    *   `R2dbcConfig.java`: 데이터베이스 연결 설정.
    *   `CsAiConfig.java`: AI(Gemini) 연동 관련 설정.

### 8. `cs` (Co-Support Automation)
*   **역할**: 최근 추가된 **CS AI 자동화** 기능을 모아둔 세부 도메인 패키지입니다. 챗봇, 시뮬레이터 등이 포함되어 있습니다.

### 9. `batch` (Job Scheduling)
*   **역할**: 정기적으로 수행되어야 하는 작업(예: 오래된 로그 삭제, 일일 리포트 생성 등)을 관리합니다.

---

## 🎨 화면 및 리소스 구성 (Resources)

`webserver/src/main/resources` 하위 구성입니다.

*   **`static/`**: 이미지, CSS, 공용 자바스크립트 파일.
*   **`templates/`**: Thymeleaf 템플릿 엔진이 사용하는 HTML 파일. (서버 사이드 렌더링)
*   **`application.properties`**: 포트 번호, DB 접속 정보, API 키 등 핵심 설정 값 관리.

---

## 💡 개발 팁: 리액티브 스택 이해하기
이 프로젝트는 일반적인 MVC와 달리 `WebFlux`를 사용합니다.
*   `Mono<T>`: 0개 또는 1개의 데이터를 비동기로 반환.
*   `Flux<T>`: 0개 또는 N개의 데이터(리스트 등)를 비동기로 반환.
*   작업 시 항상 **논블로킹**을 염두에 두고, 외부 API 호출 시 `WebClient`를 사용하는 것이 기본입니다.
