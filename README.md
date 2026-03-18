# MEGA (Monitoring & Error Gathering Agent) 🚀

![Java](https://img.shields.io/badge/Java-21-orange?logo=java)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.5.5-brightgreen?logo=springboot)
![Reactive](https://img.shields.io/badge/Stack-Reactive-blue?logo=reactivex)
![License](https://img.shields.io/badge/License-MIT-blue.svg)

**MEGA**는 분산 환경의 서버 자원과 애플리케이션 예외 로그를 실시간으로 통합 관리하는 **경량형 모니터링 시스템**입니다. 
리액티브 스택(WebFlux, R2DBC)을 활용하여 수많은 에이전트로부터의 데이터 유입을 낮은 오버헤드로 처리하며, SSE(Server-Sent Events)를 통해 대시보드에 깜박임 없는 실시간 지표를 제공합니다.

---

## 🏗 아키텍처 (Architecture)

MEGA는 **경량 에이전트(Agent)**와 **중앙 관리 서버(Web Server)**로 구성됩니다.

```mermaid
graph TD
    subgraph "Production Servers"
        A1[Agent 1 - Java 8]
        A2[Agent 2 - Java 8]
    end

    subgraph "MEGA Infrastructure"
        WS[Web Server - WebFlux]
        DB[(MariaDB - R2DBC)]
    end

    subgraph "Clients"
        BR[Web Dashboard - Vanilla JS/Chart.js]
    end

    A1 -- "Push Metrics & Logs (HTTP REST)" --> WS
    A2 -- "Push Metrics & Logs (HTTP REST)" --> WS
    WS -- "Async Store" --> DB
    WS -- "Real-time Push (SSE)" --> BR
    BR -- "Config Control" --> WS
    WS -- "Configuration Pull" --> A1
```

---

## ✨ 주요 기능 (Key Features)

### 🛰 Lightweight Agent
- **무설치형 구동**: Java 8 이상 환경이라면 어디서든 즉시 실행 가능한 Fat-JAR 및 Shell 스크립트 제공.
- **네이티브 수집**: 리눅스 표준 명령어(`top`, `/proc/stat`, `free`, `df`)를 직접 파싱하여 가장 정확한 OS 지표 수집.
- **로그 키워드 모니터링**: 애플리케이션 로그를 Real-time Tail 방식으로 감시하여 특정 키워드(Exception, Error 등) 발생 시 즉시 서버 전송.
- **동적 스케줄링**: 중앙 서버에서 설정한 모니터링 대상(Nginx, Docker 등)을 실시간으로 전달받아 수집 대상을 동적으로 변경.

### 🖥 Central Web Server
- **Full-Reactive Stack**: Spring WebFlux와 R2DBC(MariaDB)를 사용하여 대량의 스트리밍 데이터를 효율적으로 처리.
- **실시간 대시보드**: SSE 기술을 적용해 브라우저 새로고침 없이 차트와 지표가 실시간으로 업데이트.
- **차트 자동 생성**: 등록된 에이전트와 서비스별로 `Chart.js` 기반의 전용 대시보드가 자동으로 구성.
- **보안 및 인증**: API Key 기반 에이전트 인증 및 Spring Security를 통한 유저 접근 제어.

---

## 🛠 기술 스택 (Tech Stack)

| 구분 | 기술 |
| :--- | :--- |
| **Backend** | Java 21, Spring Boot 3.5.5, Spring WebFlux, Spring Security |
| **Database** | MariaDB, R2DBC, JDBC (for setup) |
| **Agent** | Java 8 (Compatibility), OkHttp 4, Gson, Logback |
| **Frontend** | Vanilla JS (ES6), Tailwind CSS (Optional), Chart.js, Thymeleaf |
| **Deployment** | Linux Shell Script, Nginx (Reverse Proxy) |

---

## 🚀 시작하기 (Quick Start)

### 1. 전제 조건 (Prerequisites)
- **Java**: Web Server(JDK 21+), Agent(JDK 1.8+)
- **Database**: MariaDB 10.x+
- **Database Setup**: [설치 가이드](./docs/INSTALL_AND_USE.md) 확인

### 2. 프로젝트 빌드 (Build)
```bash
# 전체 프로젝트 빌드
./gradlew build

# 에이전트 실행 가능 JAR 생성
./gradlew :agent:fatJar
```

### 3. 서버 및 에이전트 실행
- **Web Server**: `webserver/build/libs/` 내 JAR 파일 실행 또는 IDE에서 `WebServerApplication` 실행.
- **Agent**: `agent/` 디렉토리로 이동하여 설정 파일 수정 후 `./agent.sh start` 실행. (자세한 내용은 [Agent README](./agent/README.md) 참고)

---

## 📄 상세 문서 (Documentation)
궁금한 점이 있다면 상세 기술 문서를 참고하세요.
- 📘 [PRD (기획 문서)](./docs/PRD.md)
- 🏗 [Architecture (설계 구조)](./docs/ARCHITECTURE.md)
- 🛠 [Installation & Usage (설치 및 사용법)](./docs/INSTALL_AND_USE.md)
- 📝 [Logging Policy (로그 운영 정책)](./docs/LOGGING_POLICY.md)
- 🤖 [Agent 가이드](./agent/README.md)

---

## 🤝 기여하기 (Contributing)
1. Fork the Project
2. Create your Feature Branch (`git checkout -b feature/AmazingFeature`)
3. Commit your Changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the Branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

---
**MEGA**로 서버 관리를 더 스마트하고 실시간으로 경험해 보세요! 📈
