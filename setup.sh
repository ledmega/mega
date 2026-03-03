#!/usr/bin/env bash
# =============================================================================
# mega 프로젝트 초기 설치 스크립트
# 실행: bash setup.sh
# 대상: Ubuntu (x86_64 / arm64)
# MariaDB: Docker 컨테이너로 별도 운영 중 (localhost:33306)
# =============================================================================
set -e

WS_DIR="/home/a86223/ws"
REPO_URL="https://github.com/ledmega/mega.git"
PROJECT_DIR="$WS_DIR/mega"
JDK_DIR="$PROJECT_DIR/jdk"
JDK_VERSION="21"
ADOPTIUM_API="https://api.adoptium.net/v3/assets/latest/${JDK_VERSION}/hotspot"

echo "=============================="
echo " mega 프로젝트 초기 설치 시작"
echo "=============================="

# ── 1. 작업 디렉터리 준비 ─────────────────────────────────────────────────────
mkdir -p "$WS_DIR"
cd "$WS_DIR"

# ── 2. git clone / pull ───────────────────────────────────────────────────────
if [ -d "$PROJECT_DIR/.git" ]; then
    echo "[git] 이미 clone 되어 있습니다. 최신 코드로 업데이트합니다..."
    cd "$PROJECT_DIR"
    git pull
else
    echo "[git] 소스 코드를 clone 합니다..."
    git clone "$REPO_URL" "$PROJECT_DIR"
    cd "$PROJECT_DIR"
fi

# ── 3. JDK 21 다운로드 (프로젝트 폴더 내) ────────────────────────────────────
if [ -d "$JDK_DIR" ]; then
    echo "[jdk] 이미 JDK가 설치되어 있습니다: $JDK_DIR"
else
    echo "[jdk] JDK ${JDK_VERSION} 다운로드 중..."

    ARCH=$(uname -m)
    case "$ARCH" in
        x86_64)  JDK_ARCH="x64" ;;
        aarch64) JDK_ARCH="aarch64" ;;
        *)       echo "지원하지 않는 아키텍처: $ARCH"; exit 1 ;;
    esac

    JDK_TAR="$PROJECT_DIR/jdk21.tar.gz"

    JDK_URL=$(curl -s "${ADOPTIUM_API}?architecture=${JDK_ARCH}&image_type=jdk&os=linux&vendor=eclipse" \
        | grep -o '"link":"[^"]*"' | head -1 | cut -d'"' -f4)

    if [ -z "$JDK_URL" ]; then
        echo "[jdk] API 조회 실패 → fallback URL 사용"
        JDK_URL="https://github.com/adoptium/temurin21-binaries/releases/download/jdk-21.0.6%2B7/OpenJDK21U-jdk_${JDK_ARCH}_linux_hotspot_21.0.6_7.tar.gz"
    fi

    echo "[jdk] 다운로드 URL: $JDK_URL"
    curl -L -o "$JDK_TAR" "$JDK_URL"

    mkdir -p "$JDK_DIR"
    tar -xzf "$JDK_TAR" -C "$JDK_DIR" --strip-components=1
    rm -f "$JDK_TAR"
    echo "[jdk] JDK 설치 완료: $JDK_DIR"
fi

export JAVA_HOME="$JDK_DIR"
export PATH="$JAVA_HOME/bin:$PATH"
echo "[jdk] 버전 확인: $(java -version 2>&1 | head -1)"

# ── 4. gradle.properties에 JAVA_HOME 지정 ────────────────────────────────────
if grep -q "^org.gradle.java.home=" gradle.properties 2>/dev/null; then
    sed -i "s|^org.gradle.java.home=.*|org.gradle.java.home=$JDK_DIR|" gradle.properties
else
    echo "org.gradle.java.home=$JDK_DIR" >> gradle.properties
fi
echo "[gradle] org.gradle.java.home=$JDK_DIR 설정 완료"

# ── 5. .env 파일 생성 (없을 때만) ────────────────────────────────────────────
ENV_FILE="$PROJECT_DIR/.env"
if [ ! -f "$ENV_FILE" ]; then
    echo ""
    echo "[env] .env 파일이 없습니다. DB 비밀번호를 입력하세요."
    read -rsp "MEGA_DB_PASSWORD: " INPUT_PW
    echo ""

    cat > "$ENV_FILE" <<EOF
# Docker MariaDB 접속 정보 (localhost:33306)
MEGA_DB_URL=jdbc:mariadb://localhost:33306/ledmega?createDatabaseIfNotExist=true&useUnicode=true&characterEncoding=utf8mb4&serverTimezone=Asia/Seoul
MEGA_DB_USER=ledmega
MEGA_DB_PASSWORD=${INPUT_PW}
EOF
    chmod 600 "$ENV_FILE"
    echo "[env] .env 생성 완료: $ENV_FILE"
else
    echo "[env] .env 파일이 이미 존재합니다. 건너뜁니다."
fi

# .env 로드
set -a
source "$ENV_FILE"
set +a

# ── 6. Docker MariaDB 접속 확인 ───────────────────────────────────────────────
echo "[db] Docker MariaDB 접속 확인 중 (localhost:33306)..."
if command -v mysql &>/dev/null || command -v mariadb &>/dev/null; then
    DB_CLI=$(command -v mariadb 2>/dev/null || command -v mysql)
    if "$DB_CLI" -h 127.0.0.1 -P 33306 -u ledmega -p"${MEGA_DB_PASSWORD}" -e "SELECT 1;" ledmega &>/dev/null; then
        echo "[db] ✓ DB 접속 성공"
    else
        echo "[db] ✗ DB 접속 실패. Docker 컨테이너 상태 및 비밀번호를 확인하세요."
        echo "     docker ps | grep mariadb"
        echo "     .env 파일: $ENV_FILE"
    fi
else
    echo "[db] mysql/mariadb 클라이언트가 없어 접속 확인을 건너뜁니다."
    echo "     앱 기동 후 로그로 확인하세요."
fi

# ── 7. 빌드 ──────────────────────────────────────────────────────────────────
echo "[build] webserver 모듈 빌드 중..."
chmod +x gradlew
./gradlew :webserver:bootJar -x test --no-daemon

JAR_PATH=$(find webserver/build/libs -name "*.jar" ! -name "*plain*" | head -1)
echo ""
echo "=============================="
echo " 설치 완료!"
echo " JAR: $JAR_PATH"
echo " 실행: bash run.sh start"
echo "=============================="
