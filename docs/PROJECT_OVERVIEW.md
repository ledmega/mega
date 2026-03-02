# MEGA 프로젝트 전체 구조 개요

## 📁 프로젝트 구조 다이어그램

```
mega/ (루트 프로젝트)
│
├── 📄 build.gradle              # 루트 빌드 설정 (공통 설정)
├── 📄 settings.gradle            # 멀티 모듈 설정 (webserver, agent)
├── 📄 gradle.properties          # Gradle 프로퍼티 (인코딩 등)
│
├── 📂 webserver/                 # 웹서버 모듈 (Spring Boot, Java 21)
│   ├── 📄 build.gradle           # 웹서버 의존성 및 빌드 설정
│   │
│   └── 📂 src/main/
│       ├── 📂 java/led/mega/
│       │   ├── 🚀 MegaApplication.java      # 메인 진입점
│       │   │
│       │   ├── 📂 config/                   # 설정 클래스
│       │   │   ├── SecurityConfig.java      # Spring Security 설정
│       │   │   ├── DatabaseInitializer.java # DB 자동 초기화
│       │   │   ├── ApiKeyAuthenticationFilter.java # API 키 인증
│       │   │   └── WebSocketConfig.java     # WebSocket 설정 (비활성화)
│       │   │
│       │   ├── 📂 controller/               # 웹 요청 처리
│       │   │   ├── HomeController.java      # 홈, 로그인, 대시보드
│       │   │   ├── MemberController.java   # 회원가입
│       │   │   ├── AgentController.java    # 에이전트 웹 페이지
│       │   │   ├── AgentApiController.java # 에이전트 REST API
│       │   │   ├── TaskApiController.java  # 작업 REST API
│       │   │   ├── MetricApiController.java # 메트릭 REST API
│       │   │   └── ExceptionApiController.java # 예외 로그 REST API
│       │   │
│       │   ├── 📂 service/                  # 비즈니스 로직
│       │   │   ├── CustomUserDetailsService.java # 사용자 인증
│       │   │   ├── MemberService.java      # 회원 관리
│       │   │   ├── AgentService.java       # 에이전트 관리
│       │   │   ├── TaskService.java        # 작업 관리
│       │   │   ├── MetricDataService.java  # 메트릭 처리
│       │   │   ├── ExceptionLogService.java # 예외 로그 처리
│       │   │   ├── AgentHeartbeatService.java # 하트비트 처리
│       │   │   └── WebSocketService.java   # WebSocket (비활성화)
│       │   │
│       │   ├── 📂 repository/               # 데이터베이스 접근
│       │   │   ├── MemberRepository.java
│       │   │   ├── AgentRepository.java
│       │   │   ├── TaskRepository.java
│       │   │   ├── MetricDataRepository.java
│       │   │   ├── ExceptionLogRepository.java
│       │   │   └── AgentHeartbeatRepository.java
│       │   │
│       │   ├── 📂 entity/                   # 데이터베이스 엔티티
│       │   │   ├── Member.java              # 회원
│       │   │   ├── Agent.java               # 에이전트
│       │   │   ├── Task.java                # 작업
│       │   │   ├── MetricData.java          # 메트릭 데이터
│       │   │   ├── ExceptionLog.java        # 예외 로그
│       │   │   ├── AgentHeartbeat.java      # 하트비트
│       │   │   └── [Enum 클래스들]           # 상태, 타입 Enum
│       │   │
│       │   └── 📂 dto/                      # 데이터 전송 객체
│       │       ├── SignupDto.java
│       │       ├── AgentRegisterDto.java
│       │       ├── MetricDataRequestDto.java
│       │       └── [기타 DTO들]
│       │
│       └── 📂 resources/
│           ├── 📄 application.properties    # DB 연결, JPA 설정
│           ├── 📄 logback-spring.xml        # 로깅 설정
│           │
│           ├── 📂 sql/
│           │   └── schema.sql              # 테이블 생성 스크립트
│           │
│           └── 📂 templates/                # Thymeleaf 템플릿
│               ├── index.html               # 홈 페이지
│               ├── login.html               # 로그인
│               ├── signup.html              # 회원가입
│               └── dashboard.html           # 대시보드
│
├── 📂 agent/                      # 에이전트 모듈 (Java 1.8)
│   ├── 📄 build.gradle            # 에이전트 의존성 (OkHttp, Gson 등)
│   │
│   └── 📂 src/main/
│       ├── 📂 java/led/mega/agent/
│       │   ├── 🚀 AgentApplication.java    # 메인 진입점
│       │   │
│       │   ├── 📂 config/
│       │   │   └── AgentConfig.java        # 설정 로드
│       │   │
│       │   ├── 📂 client/
│       │   │   └── ApiClient.java          # 웹서버와 HTTP 통신
│       │   │
│       │   ├── 📂 executor/
│       │   │   └── CommandExecutor.java   # 명령어 실행
│       │   │
│       │   ├── 📂 parser/
│       │   │   ├── MetricParser.java      # 메트릭 파싱
│       │   │   └── LogParser.java         # 로그 파싱
│       │   │
│       │   └── 📂 scheduler/
│       │       └── TaskScheduler.java      # 작업 스케줄링
│       │
│       └── 📂 resources/
│           ├── 📄 application.properties   # 웹서버 URL, API 키 등
│           └── 📄 logback.xml              # 로깅 설정
│
└── 📂 docs/                       # 문서
    ├── SYSTEM_DESIGN.md            # 시스템 설계
    ├── PROJECT_STRUCTURE.md        # 프로젝트 구조 상세
    └── PROJECT_OVERVIEW.md         # 전체 개요 (본 문서)
```

## 🏗️ 시스템 아키텍처

```
┌─────────────────────────────────────────────────────────────────┐
│                        웹서버 (webserver)                        │
│                    Spring Boot 3.5.5 (Java 21)                  │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐        │
│  │   Web UI     │  │  REST API    │  │  WebSocket   │        │
│  │ (Thymeleaf)  │  │  (JSON)      │  │  (STOMP)     │        │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘        │
│         │                 │                  │                 │
│         └─────────────────┼──────────────────┘                 │
│                           │                                    │
│                  ┌────────▼────────┐                           │
│                  │   Controller    │                           │
│                  │   (Web/API)     │                           │
│                  └────────┬────────┘                           │
│                           │                                    │
│                  ┌────────▼────────┐                           │
│                  │    Service      │                           │
│                  │  (비즈니스 로직) │                           │
│                  └────────┬────────┘                           │
│                           │                                    │
│                  ┌────────▼────────┐                           │
│                  │   Repository    │                           │
│                  │   (JPA/Hibernate)│                          │
│                  └────────┬────────┘                           │
│                           │                                    │
└───────────────────────────┼────────────────────────────────────┘
                            │
                            │ JDBC
                            │
                ┌───────────▼───────────┐
                │     MariaDB 12        │
                │   (ledmega 스키마)     │
                │                       │
                │  - member             │
                │  - agent              │
                │  - task               │
                │  - metric_data        │
                │  - exception_log      │
                │  - agent_heartbeat    │
                └───────────────────────┘
                            ▲
                            │
                            │ HTTP/REST API
                            │ (JSON)
                            │
                ┌───────────┴───────────┐
                │                       │
                │   에이전트 (agent)      │
                │   Java 1.8            │
                │                       │
                │  ┌─────────────────┐  │
                │  │ TaskScheduler  │  │
                │  │ (주기적 작업)   │  │
                │  └────────┬────────┘  │
                │           │           │
                │  ┌────────▼────────┐  │
                │  │ CommandExecutor│  │
                │  │ LogParser      │  │
                │  │ MetricParser   │  │
                │  └────────┬────────┘  │
                │           │           │
                │  ┌────────▼────────┐  │
                │  │   ApiClient    │  │
                │  │  (OkHttp)      │  │
                │  └────────────────┘  │
                │                       │
                └───────────────────────┘
```

## 🔄 데이터 흐름

### 1. 웹 브라우저 → 웹서버 (사용자 접근)

```
브라우저
  ↓ HTTP 요청
Web Controller (HomeController, MemberController)
  ↓
Service (MemberService, AgentService)
  ↓
Repository (JPA Repository)
  ↓
MariaDB
  ↑
Entity (Member, Agent 등)
  ↑
DTO 또는 Model
  ↑
Thymeleaf 템플릿 (HTML)
  ↑
브라우저 (렌더링)
```

### 2. 에이전트 → 웹서버 (데이터 전송)

```
에이전트 (리눅스 서버)
  ↓
TaskScheduler (주기적 실행: 5초, 10초, 1분, 10분 등)
  ↓
CommandExecutor (free -m, df -h, top 등 실행)
  ↓
MetricParser / LogParser (결과 파싱)
  ↓
ApiClient (OkHttp)
  ↓ HTTP POST /api/agents/{id}/metrics
REST API Controller (MetricApiController)
  ↓
ApiKeyAuthenticationFilter (API 키 인증)
  ↓
Service (MetricDataService)
  ↓
Repository (MetricDataRepository)
  ↓
MariaDB (저장)
  ↓
JSON 응답
  ↑
에이전트
```

### 3. 웹서버 → 브라우저 (실시간 업데이트, 현재 비활성화)

```
Service (MetricDataService)
  ↓
WebSocketService
  ↓
WebSocketConfig (STOMP)
  ↓
브라우저 (dashboard.html)
  ↓
JavaScript WebSocket Client
  ↓
실시간 차트 업데이트
```

## 📊 주요 컴포넌트 역할

### 웹서버 모듈 (webserver)

| 컴포넌트 | 역할 |
|---------|------|
| **MegaApplication** | Spring Boot 애플리케이션 진입점 |
| **SecurityConfig** | 로그인/로그아웃, API 인증 설정 |
| **DatabaseInitializer** | 애플리케이션 시작 시 DB/테이블 자동 생성 |
| **HomeController** | 홈, 로그인, 대시보드 페이지 |
| **MemberController** | 회원가입 처리 |
| **AgentApiController** | 에이전트 등록, 하트비트 수신 |
| **MetricApiController** | 메트릭 데이터 수신 및 저장 |
| **ExceptionApiController** | 예외 로그 수신 및 저장 |
| **AgentService** | 에이전트 관리 비즈니스 로직 |
| **MetricDataService** | 메트릭 데이터 처리 및 통계 |

### 에이전트 모듈 (agent)

| 컴포넌트 | 역할 |
|---------|------|
| **AgentApplication** | 에이전트 메인 진입점 |
| **AgentConfig** | 설정 파일 로드 (웹서버 URL, API 키 등) |
| **TaskScheduler** | 주기적 작업 스케줄링 (5초, 10초, 1분, 10분 등) |
| **CommandExecutor** | 시스템 명령어 실행 (free -m, df -h, top 등) |
| **MetricParser** | 명령어 출력 파싱 (메모리, CPU, 디스크 등) |
| **LogParser** | 로그 파일에서 Exception 파싱 |
| **ApiClient** | 웹서버와 HTTP 통신 (OkHttp) |

## 🗄️ 데이터베이스 테이블

```
MariaDB (ledmega 스키마)
│
├── member              # 회원 정보
├── agent               # 에이전트 정보
├── task                # 작업 정의 (명령어, 로그 파싱 등)
├── metric_data         # 메트릭 데이터 (CPU, 메모리, 디스크 등)
├── exception_log       # 예외 로그
└── agent_heartbeat     # 에이전트 하트비트 (온라인/오프라인 상태)
```

## 🔌 API 엔드포인트

### 웹 페이지
- `GET /` → 홈 페이지
- `GET /login` → 로그인 페이지
- `GET /signup` → 회원가입 페이지
- `GET /dashboard` → 대시보드 (로그인 필요)
- `GET /agents` → 에이전트 목록 (로그인 필요)

### REST API (에이전트용)
- `POST /api/agents/register` → 에이전트 등록
- `POST /api/agents/{id}/heartbeat` → 하트비트 전송
- `GET /api/agents/{agentId}/tasks` → 작업 목록 조회
- `POST /api/agents/{agentId}/tasks` → 작업 생성
- `POST /api/agents/{agentId}/metrics` → 메트릭 데이터 전송
- `POST /api/agents/{agentId}/exceptions` → 예외 로그 전송

## 🛠️ 기술 스택

### 웹서버
- **Java**: 21
- **Framework**: Spring Boot 3.5.5
- **Security**: Spring Security
- **Database**: MariaDB 12 (JPA/Hibernate)
- **Template**: Thymeleaf
- **Build**: Gradle

### 에이전트
- **Java**: 1.8 (JDK 8)
- **HTTP Client**: OkHttp 4.12.0
- **JSON**: Gson 2.10.1
- **Logging**: Logback 1.2.12
- **Build**: Gradle

## 🚀 실행 방법

### 웹서버 실행
```bash
cd webserver
../gradlew.bat bootRun
```
또는 루트에서:
```bash
./gradlew.bat :webserver:bootRun
```

### 에이전트 실행
```bash
cd agent
../gradlew.bat run
```
또는 루트에서:
```bash
./gradlew.bat :agent:run
```

## 📝 주요 기능

1. **회원 관리**: 회원가입, 로그인/로그아웃
2. **에이전트 관리**: 에이전트 등록, 상태 모니터링
3. **작업 스케줄링**: 주기적 명령어 실행, 로그 파싱
4. **메트릭 수집**: CPU, 메모리, 디스크 사용률 수집 및 저장
5. **예외 로그 수집**: 로그 파일에서 Exception 파싱 및 저장
6. **대시보드**: 에이전트 상태 및 통계 정보 표시

## ⚠️ 현재 상태

- ✅ 웹서버 기본 구조 완성
- ✅ 에이전트 기본 구조 완성
- ✅ 데이터베이스 스키마 완성
- ✅ REST API 구현 완료
- ⚠️ WebSocket 기능 비활성화 (의존성 문제)
- ⚠️ 대시보드 실시간 업데이트 미구현 (WebSocket 필요)
