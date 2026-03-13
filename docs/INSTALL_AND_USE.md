# 설치 및 기동 가이드 (Installation & Run Guide)

본 프로젝트는 리눅스 서버 모니터링을 위한 Agent 모듈과 Web Server 모듈로 구성됩니다. 아래 가이드를 따라 설치 및 실행할 수 있습니다.

## 1. 사전 요구사항
* **Java**: JDK 17 이상 (Web Server), JDK 8 이상 (Agent)
* **Database**: MariaDB 12+ (Web Server 노드만 필요)
* **OS**: Linux/Unix 권장 (Agent 호스트 서버), Windows도 개발용으로 지원.

## 2. 데이터베이스 설정 (Web Server 전용)
Web Server가 구동되기 전, MariaDB에 접근할 수 있도록 스키마와 계정을 구성해야 합니다.
```sql
CREATE DATABASE ledmega DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'ledmega'@'localhost' IDENTIFIED BY 'your_password';
GRANT ALL PRIVILEGES ON ledmega.* TO 'ledmega'@'localhost';
FLUSH PRIVILEGES;
```
*(참고: `.env` 또는 `application.properties` 에서 DB 정보(`r2dbc:mariadb...`)를 매칭시켜야 합니다.)*

## 3. 모듈 빌드 및 실행

### 3.1 Web Server (Spring Boot WebFlux)
웹서버 루트 디렉토리에서 아래 명령어로 서버를 관리합니다.
* **빌드 및 실행**: `./run.sh update` (또는 `rebuild`)
* **로그 모니터링**: 
    - 전체 로그: `./run.sh log`
    - 배치 로그: `./run.sh log-batch`
    - 에이전트 로그: `./run.sh log-agent`
* **접속 URL**: `http://localhost:8080/`

기본 관리자 계정은 회원가입(`/signup`) 화면을 통해 최초 1회 생성 후 웹 UI에 로그인할 수 있습니다.

### 3.2 Agent (Java Core)
모니터링 대상이 되는 리눅스 서버 터미널에서 에이전트를 수행합니다.
* **빌드 및 실행**: `./agent.sh rebuild`
* **로그 모니터링**:
    - 데이터 로그: `./agent.sh log data`
    - 네트워크 로그: `./agent.sh log net`
    - 테스크 로그: `./agent.sh log task`

에이전트는 기동 즉시 웹서버를 목적지로 하여 스스로를 등록하며 API Key를 발급 받습니다. Web UI 대시보드 새로고침 시 이 에이전트 목록이 실시간으로 확인됩니다.
또한, `agent/src/main/resources/application.properties` 파일 내의 `task.*` 설정을 통해 수집 주기와 모니터링할 로그 경로를 커스터마이징할 수 있습니다.

---
## 4. 로깅 및 타임존 (Troubleshooting)
시간 차이 이슈를 겪을 시:
에이전트 및 웹서버는 내장 로거(Logback) 설정과 Bootstrapping 코드 선언으로 **KST(Asia/Seoul)**를 명시적으로 오버라이드하게 설계되어 있습니다. KST 외의 타임존이 강제되어 DB 입력/차트 출력에 문제가 있다면 JVM 실행 옵션에 `-Duser.timezone=Asia/Seoul` 변수를 추가하여 기동하십시오.
