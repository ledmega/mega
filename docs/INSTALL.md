# mega 프로젝트 설치 가이드

## 환경 정보

| 항목 | 값 |
|------|-----|
| 서버 OS | Ubuntu (x86_64 / arm64) |
| 서버 IP | 192.168.0.77 |
| 설치 경로 | `/home/a86223/ws/mega` |
| JDK | Eclipse Temurin 21 (프로젝트 폴더 내 자동 설치) |
| DB | MariaDB (Docker 컨테이너, 포트 `33306`) |
| 앱 포트 | `8080` |

---

## 사전 조건

- Ubuntu 서버에 `git`, `curl` 설치되어 있어야 함
- Docker MariaDB 컨테이너가 실행 중이어야 함

```bash
# git, curl 설치 (없을 경우)
sudo apt-get update && sudo apt-get install -y git curl

# Docker MariaDB 컨테이너 상태 확인
docker ps | grep mariadb
```

### Docker MariaDB 접속 정보

| 항목 | 값 |
|------|-----|
| 호스트 | `localhost` (127.0.0.1) |
| 포트 | `33306` |
| DB명 | `ledmega` |
| 계정 | `ledmega` |
| 비밀번호 | _(별도 관리)_ |

---

## 최초 설치

### 1. 서버 SSH 접속

```bash
ssh a86223@192.168.0.77
```

### 2. setup.sh 실행

```bash
cd /home/a86223/ws
git clone https://github.com/ledmega/mega.git
cd mega
bash setup.sh
```

`setup.sh`가 자동으로 처리하는 항목:

1. 소스코드 clone (이미 있으면 `git pull`)
2. JDK 21 다운로드 → `jdk/` 폴더에 설치
3. `gradle.properties`에 JDK 경로 자동 등록
4. `.env` 파일 생성 (DB 비밀번호 입력 프롬프트)
5. Docker MariaDB 접속 확인
6. `bootJar` 빌드

### 3. DB 비밀번호 입력

`setup.sh` 실행 중 아래 프롬프트가 나타나면 Docker MariaDB 비밀번호를 입력합니다.

```
[env] .env 파일이 없습니다. DB 비밀번호를 입력하세요.
MEGA_DB_PASSWORD: ●●●●●●●●
```

입력 후 `.env` 파일이 자동 생성됩니다.

```
/home/a86223/ws/mega/.env
```

```dotenv
MEGA_DB_URL=jdbc:mariadb://localhost:33306/ledmega
MEGA_DB_USER=ledmega
MEGA_DB_PASSWORD=입력한_비밀번호
```

> `.env` 파일은 `.gitignore`에 등록되어 있어 git에 올라가지 않습니다.

---

## 앱 실행 / 관리

```bash
# 서버 시작
bash run.sh start

# 서버 종료
bash run.sh stop

# 서버 재시작
bash run.sh restart

# 실행 상태 확인
bash run.sh status

# 실시간 로그 확인
bash run.sh log

# 최신 코드 반영 (git pull → 재빌드 → 재시작)
bash run.sh rebuild
```

### 로그 파일 위치

```
/home/a86223/ws/mega/logs/app.log
```

---

## 서버 재부팅 시 자동 시작 설정 (선택)

systemd 서비스를 등록하면 서버가 재부팅되어도 앱이 자동으로 시작됩니다.

```bash
# 서비스 파일 복사
sudo cp /home/a86223/ws/mega/service/mega-webserver.service /etc/systemd/system/

# 서비스 등록 및 활성화
sudo systemctl daemon-reload
sudo systemctl enable mega-webserver
sudo systemctl start mega-webserver

# 상태 확인
sudo systemctl status mega-webserver
```

### systemd 서비스 관리 명령

```bash
sudo systemctl start mega-webserver    # 시작
sudo systemctl stop mega-webserver     # 종료
sudo systemctl restart mega-webserver  # 재시작
sudo systemctl status mega-webserver   # 상태 확인

# 실시간 로그 (journald)
sudo journalctl -u mega-webserver -f
```

---

## 코드 업데이트 (배포)

```bash
cd /home/a86223/ws/mega
bash run.sh rebuild
```

또는 systemd 사용 중인 경우:

```bash
cd /home/a86223/ws/mega
git pull
./gradlew :webserver:bootJar -x test --no-daemon
sudo systemctl restart mega-webserver
```

---

## 디렉터리 구조

```
/home/a86223/ws/mega/
├── jdk/                          # JDK 21 (setup.sh가 자동 설치, git 제외)
├── logs/
│   └── app.log                   # 애플리케이션 로그
├── webserver/
│   └── build/libs/
│       └── webserver-*.jar       # 빌드된 JAR (run.sh가 실행)
├── service/
│   └── mega-webserver.service    # systemd 서비스 파일
├── .env                          # DB 접속정보 (git 제외, 직접 생성)
├── setup.sh                      # 최초 설치 스크립트
└── run.sh                        # 실행/종료/재시작 스크립트
```

---

## 문제 해결

### DB 접속 실패

```bash
# Docker 컨테이너 실행 중인지 확인
docker ps | grep mariadb

# 컨테이너가 없으면 재시작
docker start <컨테이너명>

# .env 파일 비밀번호 확인/수정
nano /home/a86223/ws/mega/.env
```

### 포트 8080 충돌

```bash
# 8080 포트 사용 중인 프로세스 확인
sudo ss -tlnp | grep 8080

# 기존 프로세스 종료
bash run.sh stop
```

### JAR 파일 없음 오류

```bash
# 빌드 재실행
cd /home/a86223/ws/mega
export JAVA_HOME=/home/a86223/ws/mega/jdk
./gradlew :webserver:bootJar -x test --no-daemon
```
