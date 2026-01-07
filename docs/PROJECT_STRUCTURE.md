# 프로젝트 구조 문서

> **참고**: 이 프로젝트는 멀티 모듈 구조로 구성되어 있습니다.
> 자세한 내용은 [MULTI_MODULE_STRUCTURE.md](./MULTI_MODULE_STRUCTURE.md)를 참조하세요.

## 프로젝트 전체 구조

```
mega/
├── src/main/java/led/mega/          # Java 소스 코드
│   ├── MegaApplication.java         # 메인 애플리케이션 진입점
│   │
│   ├── config/                      # 설정 클래스들
│   │   ├── SecurityConfig.java      # Spring Security 설정 (로그인/로그아웃)
│   │   ├── DatabaseInitializer.java # DB 초기화 (테이블 자동 생성)
│   │   ├── ApiKeyAuthenticationFilter.java  # API 키 인증 필터
│   │   └── WebSocketConfig.java     # WebSocket 설정 (현재 비활성화)
│   │
│   ├── entity/                      # 데이터베이스 테이블과 매핑되는 엔티티
│   │   ├── Member.java              # 회원 테이블
│   │   ├── Agent.java               # 에이전트 테이블
│   │   ├── Task.java                # 작업 테이블
│   │   ├── MetricData.java          # 메트릭 데이터 테이블
│   │   ├── ExceptionLog.java        # 예외 로그 테이블
│   │   ├── AgentHeartbeat.java      # 에이전트 하트비트 테이블
│   │   ├── AgentStatus.java         # 에이전트 상태 Enum
│   │   ├── TaskType.java            # 작업 타입 Enum
│   │   ├── MetricType.java          # 메트릭 타입 Enum
│   │   ├── MemberRole.java          # 회원 역할 Enum
│   │   └── MemberStatus.java        # 회원 상태 Enum
│   │
│   ├── repository/                  # 데이터베이스 접근 계층 (JPA Repository)
│   │   ├── MemberRepository.java    # 회원 데이터 접근
│   │   ├── AgentRepository.java     # 에이전트 데이터 접근
│   │   ├── TaskRepository.java      # 작업 데이터 접근
│   │   ├── MetricDataRepository.java # 메트릭 데이터 접근
│   │   ├── ExceptionLogRepository.java # 예외 로그 데이터 접근
│   │   └── AgentHeartbeatRepository.java # 하트비트 데이터 접근
│   │
│   ├── dto/                         # 데이터 전송 객체 (API 요청/응답)
│   │   ├── SignupDto.java           # 회원가입 요청
│   │   ├── AgentRegisterDto.java    # 에이전트 등록 요청
│   │   ├── AgentRegisterResponseDto.java # 에이전트 등록 응답
│   │   ├── AgentResponseDto.java    # 에이전트 정보 응답
│   │   ├── MetricDataRequestDto.java # 메트릭 데이터 요청
│   │   ├── MetricDataResponseDto.java # 메트릭 데이터 응답
│   │   ├── ExceptionLogRequestDto.java # 예외 로그 요청
│   │   ├── ExceptionLogResponseDto.java # 예외 로그 응답
│   │   ├── HeartbeatRequestDto.java # 하트비트 요청
│   │   ├── TaskRequestDto.java      # 작업 요청
│   │   ├── TaskResponseDto.java     # 작업 응답
│   │   └── WebSocketMessageDto.java # WebSocket 메시지
│   │
│   ├── service/                     # 비즈니스 로직 계층
│   │   ├── CustomUserDetailsService.java  # Spring Security 사용자 인증
│   │   ├── MemberService.java       # 회원 관련 로직
│   │   ├── AgentService.java        # 에이전트 관리 로직
│   │   ├── TaskService.java         # 작업 관리 로직
│   │   ├── MetricDataService.java   # 메트릭 데이터 처리
│   │   ├── ExceptionLogService.java # 예외 로그 처리
│   │   ├── AgentHeartbeatService.java # 하트비트 처리
│   │   └── WebSocketService.java    # WebSocket 메시지 전송 (현재 비활성화)
│   │
│   └── controller/                  # 웹 요청 처리 계층
│       ├── HomeController.java      # 홈, 로그인 페이지
│       ├── MemberController.java    # 회원가입
│       ├── AgentController.java     # 에이전트 웹 페이지
│       ├── AgentApiController.java  # 에이전트 REST API
│       ├── TaskApiController.java   # 작업 REST API
│       ├── MetricApiController.java # 메트릭 REST API
│       └── ExceptionApiController.java # 예외 로그 REST API
│
├── src/main/resources/              # 리소스 파일들
│   ├── application.properties       # 애플리케이션 설정 (DB 연결 등)
│   ├── logback-spring.xml           # 로깅 설정
│   │
│   ├── sql/                         # SQL 스크립트
│   │   ├── schema.sql               # 테이블 생성 스크립트
│   │   ├── member_table.sql         # 회원 테이블 (레거시)
│   │   └── check_charset.sql        # 문자셋 확인 스크립트
│   │
│   └── templates/                   # Thymeleaf 템플릿 (HTML)
│       ├── index.html               # 홈 페이지
│       ├── login.html               # 로그인 페이지
│       ├── signup.html              # 회원가입 페이지
│       └── dashboard.html           # 대시보드 페이지
│
├── build.gradle                     # Gradle 빌드 설정 및 의존성
├── gradle.properties                # Gradle 프로퍼티 (인코딩 설정 등)
├── settings.gradle                   # Gradle 프로젝트 설정
├── docs/                             # 문서 폴더
│   ├── SYSTEM_DESIGN.md              # 시스템 설계 문서
│   └── PROJECT_STRUCTURE.md          # 프로젝트 구조 문서 (본 문서)
└── logs/                             # 로그 파일 저장 폴더
```

## 계층 구조 설명

### 1. Controller 계층 (`controller/`)
**역할**: HTTP 요청을 받아 처리하고 응답을 반환

#### Web Controller
- `HomeController`: 홈 페이지(`/`), 로그인 페이지(`/login`), 대시보드(`/dashboard`) 처리
- `MemberController`: 회원가입(`/signup`) 처리
- `AgentController`: 에이전트 목록(`/agents`), 에이전트 상세(`/agents/{id}`) 페이지 처리

#### API Controller
- `AgentApiController`: 에이전트 등록(`POST /api/agents/register`), 하트비트(`POST /api/agents/{id}/heartbeat`) 처리
- `TaskApiController`: 작업 목록 조회(`GET /api/agents/{agentId}/tasks`), 작업 생성(`POST /api/agents/{agentId}/tasks`) 처리
- `MetricApiController`: 메트릭 데이터 전송(`POST /api/agents/{agentId}/metrics`) 처리
- `ExceptionApiController`: 예외 로그 전송(`POST /api/agents/{agentId}/exceptions`) 처리

### 2. Service 계층 (`service/`)
**역할**: 비즈니스 로직 처리

- `CustomUserDetailsService`: Spring Security에서 사용자 인증을 위한 사용자 정보 로드
- `MemberService`: 회원가입, 비밀번호 암호화 등 회원 관련 로직
- `AgentService`: 에이전트 등록, 조회, 상태 업데이트 등 에이전트 관리 로직
- `TaskService`: 작업 생성, 조회, 업데이트 등 작업 관리 로직
- `MetricDataService`: 메트릭 데이터 저장 및 통계 처리
- `ExceptionLogService`: 예외 로그 저장 및 조회 처리
- `AgentHeartbeatService`: 하트비트 수신 및 에이전트 상태 업데이트
- `WebSocketService`: 실시간 데이터 전송을 위한 WebSocket 메시지 발송 (현재 비활성화)

### 3. Repository 계층 (`repository/`)
**역할**: 데이터베이스 CRUD 작업

- JPA Repository 인터페이스로 자동 구현
- 각 Entity에 대응하는 Repository 인터페이스 제공
- 커스텀 쿼리 메서드 지원 (예: `findByAgentId`, `findLatestByAgentId`)

### 4. Entity 계층 (`entity/`)
**역할**: 데이터베이스 테이블과 1:1 매핑

- JPA 어노테이션(`@Entity`, `@Table`, `@Id` 등)으로 테이블 구조 정의
- 관계 매핑(`@OneToMany`, `@ManyToOne` 등)으로 테이블 간 관계 정의
- Enum 클래스로 상태값, 타입값 관리

### 5. DTO 계층 (`dto/`)
**역할**: API 요청/응답 데이터 전송

- Entity와 분리하여 보안 및 유연성 확보
- Validation 어노테이션으로 데이터 검증
- Request DTO: API 요청 데이터
- Response DTO: API 응답 데이터

### 6. Config 계층 (`config/`)
**역할**: 애플리케이션 전역 설정

- `SecurityConfig`: Spring Security 설정 (인증/인가, 로그인/로그아웃, CSRF 등)
- `DatabaseInitializer`: 애플리케이션 시작 시 데이터베이스 및 테이블 자동 생성
- `ApiKeyAuthenticationFilter`: REST API 요청에 대한 API 키 인증 필터
- `WebSocketConfig`: WebSocket(STOMP) 설정 (현재 비활성화)

## 데이터 흐름

### 웹 요청 흐름
```
브라우저 
  → Web Controller (HomeController, MemberController, AgentController)
    → Service (MemberService, AgentService 등)
      → Repository (MemberRepository, AgentRepository 등)
        → Database (MariaDB)
      ← Entity (Member, Agent 등)
    ← DTO 또는 Model
  ← Thymeleaf 템플릿 (HTML)
브라우저
```

### API 요청 흐름
```
에이전트 (Java 1.8)
  → REST API Controller (AgentApiController, TaskApiController 등)
    → ApiKeyAuthenticationFilter (API 키 인증)
      → Service (AgentService, TaskService 등)
        → Repository (AgentRepository, TaskRepository 등)
          → Database (MariaDB)
        ← Entity (Agent, Task 등)
      ← DTO (ResponseDto)
    ← JSON 응답
에이전트
```

### 실시간 데이터 흐름 (WebSocket, 현재 비활성화)
```
Service (MetricDataService, ExceptionLogService 등)
  → WebSocketService
    → WebSocketConfig (STOMP)
      → 브라우저 (dashboard.html)
        → JavaScript WebSocket Client
```

## 주요 기술 스택

### 프레임워크 및 라이브러리
- **Spring Boot 3.5.5**: 메인 프레임워크
- **Spring Security**: 인증/인가
- **Spring Data JPA**: 데이터베이스 접근
- **Thymeleaf**: 서버 사이드 템플릿 엔진
- **WebSocket (STOMP)**: 실시간 통신 (현재 비활성화)
- **Lombok**: 보일러플레이트 코드 제거
- **Validation**: 데이터 검증

### 데이터베이스
- **MariaDB 12**: 관계형 데이터베이스
- **JPA/Hibernate**: ORM (Object-Relational Mapping)

### 빌드 도구
- **Gradle**: 빌드 자동화 도구
- **Java 21**: 개발 환경 (에이전트는 Java 1.8 호환 필요)

## 주요 기능

### 1. 회원 관리
- 회원가입 (`/signup`)
- 로그인/로그아웃 (`/login`, `/logout`)
- Spring Security 기반 인증

### 2. 에이전트 관리
- 에이전트 등록 (REST API: `POST /api/agents/register`)
- 에이전트 목록 조회 (웹: `/agents`)
- 에이전트 상세 조회 (웹: `/agents/{id}`)
- 하트비트 수신 (REST API: `POST /api/agents/{id}/heartbeat`)

### 3. 작업 관리
- 작업 생성 (REST API: `POST /api/agents/{agentId}/tasks`)
- 작업 목록 조회 (REST API: `GET /api/agents/{agentId}/tasks`)
- 작업 타입: COMMAND, LOG_PARSE

### 4. 메트릭 데이터 수집
- 메트릭 데이터 전송 (REST API: `POST /api/agents/{agentId}/metrics`)
- 메트릭 타입: CPU, MEMORY, DISK, NETWORK, CUSTOM

### 5. 예외 로그 수집
- 예외 로그 전송 (REST API: `POST /api/agents/{agentId}/exceptions`)
- 로그 파일에서 Exception 파싱 결과 저장

### 6. 대시보드
- 실시간 에이전트 모니터링 (웹: `/dashboard`)
- 통계 정보 표시
- WebSocket을 통한 실시간 데이터 업데이트 (현재 비활성화)

## 데이터베이스 스키마

### 주요 테이블
1. **member**: 회원 정보
2. **agent**: 에이전트 정보
3. **task**: 작업 정의
4. **metric_data**: 메트릭 데이터
5. **exception_log**: 예외 로그
6. **agent_heartbeat**: 에이전트 하트비트

자세한 스키마 정보는 `docs/SYSTEM_DESIGN.md` 참조

## 설정 파일

### `application.properties`
- 데이터베이스 연결 정보
- JPA 설정
- 서버 포트 등

### `logback-spring.xml`
- 로깅 레벨 설정
- 로그 파일 출력 설정
- 콘솔 출력 설정

### `build.gradle`
- 프로젝트 의존성 관리
- 빌드 설정
- JVM 인코딩 설정

## 주의사항

1. **WebSocket 기능**: 현재 `WebSocketConfig`, `WebSocketService`가 비활성화되어 있습니다. 활성화하려면 관련 클래스의 주석을 해제하고 의존성을 확인해야 합니다.

2. **데이터베이스 초기화**: `DatabaseInitializer`가 애플리케이션 시작 시 자동으로 데이터베이스와 테이블을 생성합니다. `schema.sql` 파일을 수정하면 테이블 구조가 변경됩니다.

3. **인코딩**: 한글 지원을 위해 여러 설정 파일에서 UTF-8 인코딩을 설정했습니다. (`gradle.properties`, `build.gradle`, `.vscode/settings.json` 등)

4. **API 인증**: REST API는 API 키 기반 인증을 사용합니다. `ApiKeyAuthenticationFilter`에서 처리합니다.

## 향후 개발 계획

1. WebSocket 기능 활성화
2. 에이전트 Java 프로젝트 생성 (Java 1.8 호환)
3. 대시보드 실시간 데이터 시각화 강화
4. 알림 기능 추가
5. 통계 및 리포트 기능 추가

