# 멀티 모듈 프로젝트 구조

## 프로젝트 구조

```
mega/                              # 루트 프로젝트
├── settings.gradle                # 멀티 모듈 설정
├── build.gradle                   # 루트 빌드 설정 (공통 설정)
│
├── webserver/                     # 웹서버 모듈 (Java 21, Spring Boot)
│   ├── build.gradle              # 웹서버 빌드 설정
│   └── src/
│       ├── main/
│       │   ├── java/led/mega/    # 기존 웹서버 코드
│       │   └── resources/        # 웹서버 리소스
│       └── test/
│
└── agent/                         # 에이전트 모듈 (Java 1.8)
    ├── build.gradle              # 에이전트 빌드 설정
    └── src/
        ├── main/
        │   ├── java/led/mega/agent/  # 에이전트 코드
        │   └── resources/            # 에이전트 설정 파일
        └── test/
```

## 모듈별 상세 정보

### 1. webserver 모듈

**기술 스택:**
- Java 21
- Spring Boot 3.5.5
- Spring Security
- Spring Data JPA
- Thymeleaf
- WebSocket (STOMP)
- MariaDB

**주요 기능:**
- REST API 엔드포인트 제공
- 대시보드 웹 페이지
- 에이전트 데이터 수신 및 저장
- 실시간 모니터링 (WebSocket)

**실행 방법:**
```bash
# 웹서버만 실행
cd webserver
../gradlew bootRun

# 또는 루트에서
./gradlew :webserver:bootRun
```

**빌드:**
```bash
./gradlew :webserver:build
```

### 2. agent 모듈

**기술 스택:**
- Java 1.8 (JDK 8)
- OkHttp 4.12.0 (HTTP Client)
- Gson 2.10.1 (JSON 처리)
- SLF4J + Logback (로깅)
- Lombok

**주요 기능:**
- 주기적 명령어 실행 (free -m, df -h, CPU 등)
- 로그 파일 파싱 (Exception 추출)
- 웹서버로 데이터 전송 (REST API)
- 하트비트 전송
- 작업 스케줄 관리

**실행 방법:**
```bash
# 에이전트 실행
cd agent
../gradlew run

# 또는 루트에서
./gradlew :agent:run
```

**빌드 (실행 가능한 JAR):**
```bash
# Fat JAR 생성 (모든 의존성 포함)
./gradlew :agent:fatJar

# 생성된 JAR 위치: agent/build/libs/mega-agent-0.0.1-SNAPSHOT.jar
```

**배포:**
```bash
# 리모트 리눅스 서버에 배포
scp agent/build/libs/mega-agent-0.0.1-SNAPSHOT.jar user@server:/opt/mega-agent/
ssh user@server "java -jar /opt/mega-agent/mega-agent-0.0.1-SNAPSHOT.jar"
```

## 빌드 명령어

### 전체 빌드
```bash
./gradlew build
```

### 특정 모듈만 빌드
```bash
./gradlew :webserver:build
./gradlew :agent:build
```

### 웹서버 실행
```bash
./gradlew :webserver:bootRun
```

### 에이전트 실행
```bash
./gradlew :agent:run
```

### 에이전트 Fat JAR 생성
```bash
./gradlew :agent:fatJar
```

## 모듈 간 관계

현재는 **독립적인 모듈**로 구성되어 있습니다:
- 웹서버와 에이전트는 REST API로 통신
- 공통 코드가 필요한 경우 `common` 모듈을 추가할 수 있음
- 각 모듈은 독립적으로 빌드 및 배포 가능

## 향후 확장 가능성

### common 모듈 추가 (선택사항)
```
mega/
├── webserver/
├── agent/
└── common/              # 공통 코드 모듈
    ├── build.gradle
    └── src/main/java/led/mega/common/
        ├── dto/        # 공통 DTO (선택사항)
        └── util/       # 공통 유틸리티
```

공통 DTO나 유틸리티가 필요한 경우 `common` 모듈을 추가하여 두 모듈에서 공유할 수 있습니다.

## 설정 파일

### 웹서버 설정
- `webserver/src/main/resources/application.properties`
- `webserver/src/main/resources/logback-spring.xml`

### 에이전트 설정
- `agent/src/main/resources/application.properties`
- `agent/src/main/resources/logback.xml`

## 주의사항

1. **Java 버전**: 웹서버는 Java 21, 에이전트는 Java 1.8을 사용합니다.
2. **독립 빌드**: 각 모듈은 독립적으로 빌드 및 배포할 수 있습니다.
3. **의존성 관리**: 각 모듈의 `build.gradle`에서 필요한 의존성을 관리합니다.

