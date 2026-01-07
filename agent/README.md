# Mega Agent

리모트 리눅스 서버에서 실행되는 모니터링 에이전트입니다.

## 기능

- **시스템 메트릭 수집**
  - 메모리 사용량 (`free -m`) - 1분마다
  - 디스크 사용량 (`df -h`) - 10분마다
  - CPU 사용률 - 30초마다

- **로그 모니터링**
  - Exception 로그 파싱 - 10분마다
  - 위아래 5줄 컨텍스트 포함

- **하트비트 전송**
  - 주기적으로 서버에 상태 전송 (기본: 30초)

## 요구사항

- Java 1.8 이상
- Linux 운영체제
- 네트워크 연결 (웹서버 접근 가능)

## 빌드

```bash
# 프로젝트 루트에서
./gradlew :agent:build

# 실행 가능한 JAR 생성
./gradlew :agent:fatJar
```

빌드된 JAR 파일은 `agent/build/libs/mega-agent-0.0.1-SNAPSHOT.jar`에 생성됩니다.

## 실행

### 1. 설정 파일 수정

`agent/src/main/resources/application.properties` 파일을 수정하세요:

```properties
# 웹서버 연결 정보
webserver.url=http://your-server:8080
webserver.api.key=your-api-key-here

# 에이전트 정보
agent.name=my-server-01
```

### 2. JAR 파일 실행

```bash
java -jar mega-agent-0.0.1-SNAPSHOT.jar
```

### 3. 백그라운드 실행 (systemd 서비스 예시)

`/etc/systemd/system/mega-agent.service` 파일 생성:

```ini
[Unit]
Description=Mega Agent
After=network.target

[Service]
Type=simple
User=your-user
WorkingDirectory=/opt/mega-agent
ExecStart=/usr/bin/java -jar /opt/mega-agent/mega-agent-0.0.1-SNAPSHOT.jar
Restart=always
RestartSec=10

[Install]
WantedBy=multi-user.target
```

서비스 시작:

```bash
sudo systemctl daemon-reload
sudo systemctl enable mega-agent
sudo systemctl start mega-agent
```

## 설정 옵션

### application.properties

| 속성 | 설명 | 기본값 |
|------|------|--------|
| `webserver.url` | 웹서버 URL | `http://localhost:8080` |
| `webserver.api.key` | API 키 (등록 후 자동 설정) | `your-api-key-here` |
| `agent.name` | 에이전트 이름 | `default-agent` |
| `agent.hostname` | 호스트명 (자동 감지 가능) | `${HOSTNAME:unknown}` |
| `agent.ip` | IP 주소 (자동 감지 가능) | `${HOST_IP:unknown}` |
| `heartbeat.interval.seconds` | 하트비트 전송 주기 | `30` |
| `task.memory.interval.seconds` | 메모리 수집 주기 | `60` |
| `task.disk.interval.seconds` | 디스크 수집 주기 | `600` |
| `task.cpu.interval.seconds` | CPU 수집 주기 | `30` |
| `task.exception.interval.seconds` | Exception 로그 수집 주기 | `600` |
| `task.exception.log.paths` | Exception 로그 파일 경로 (쉼표로 구분) | `/var/log/app/application.log,...` |

## 동작 방식

1. **에이전트 등록**: 시작 시 웹서버에 자동 등록하고 API 키를 받습니다.
2. **주기적 작업 실행**: 설정된 주기로 시스템 메트릭을 수집하고 서버에 전송합니다.
3. **하트비트 전송**: 주기적으로 서버에 상태를 전송하여 온라인 상태를 유지합니다.
4. **로그 모니터링**: 설정된 로그 파일에서 Exception을 찾아 서버에 전송합니다.

## 로그

로그는 콘솔에 출력되며, Logback을 사용합니다.

로그 레벨 설정:

```properties
logging.level.led.mega.agent=INFO
```

## 문제 해결

### 에이전트 등록 실패

- 웹서버 URL이 올바른지 확인
- 네트워크 연결 확인
- 웹서버가 실행 중인지 확인

### 메트릭 전송 실패

- API 키가 올바른지 확인
- 네트워크 연결 확인
- 웹서버 로그 확인

### 명령어 실행 실패

- 필요한 명령어가 설치되어 있는지 확인 (`free`, `df`, `top`)
- 실행 권한 확인

## 개발

### 프로젝트 구조

```
agent/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── led/mega/agent/
│   │   │       ├── AgentApplication.java      # 메인 애플리케이션
│   │   │       ├── config/
│   │   │       │   └── AgentConfig.java       # 설정 관리
│   │   │       ├── client/
│   │   │       │   └── ApiClient.java         # HTTP 클라이언트
│   │   │       ├── executor/
│   │   │       │   └── CommandExecutor.java  # 명령어 실행
│   │   │       ├── parser/
│   │   │       │   ├── MetricParser.java     # 메트릭 파싱
│   │   │       │   └── LogParser.java        # 로그 파싱
│   │   │       └── scheduler/
│   │   │           └── TaskScheduler.java    # 작업 스케줄러
│   │   └── resources/
│   │       ├── application.properties        # 설정 파일
│   │       └── logback.xml                   # 로깅 설정
│   └── test/
└── build.gradle                              # 빌드 설정
```

## 라이선스

이 프로젝트는 Mega 프로젝트의 일부입니다.

