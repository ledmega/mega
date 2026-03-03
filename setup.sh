#!/usr/bin/env bash
# =============================================================================
# mega 프로젝트 초기 설치 스크립트
# 실행: bash setup.sh
# 대상: Ubuntu (x86_64 / arm64)
# =============================================================================
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
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

    # 아키텍처 감지
    ARCH=$(uname -m)
    case "$ARCH" in
        x86_64)  JDK_ARCH="x64" ;;
        aarch64) JDK_ARCH="aarch64" ;;
        *)       echo "지원하지 않는 아키텍처: $ARCH"; exit 1 ;;
    esac

    JDK_TAR="$PROJECT_DIR/jdk21.tar.gz"

    # Adoptium (Eclipse Temurin) JDK 21 LTS 다운로드
    JDK_URL=$(curl -s "${ADOPTIUM_API}?architecture=${JDK_ARCH}&image_type=jdk&os=linux&vendor=eclipse" \
        | grep -o '"link":"[^"]*"' | head -1 | cut -d'"' -f4)

    if [ -z "$JDK_URL" ]; then
        # fallback: 직접 URL 구성
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

# ── 5. application.properties 확인 ───────────────────────────────────────────
APP_PROPS="$PROJECT_DIR/webserver/src/main/resources/application.properties"
if grep -q "MEGA_DB_PASSWORD:}" "$APP_PROPS" 2>/dev/null; then
    echo ""
    echo "┌──────────────────────────────────────────────────────────────┐"
    echo "│  ⚠  DB 비밀번호를 환경변수로 설정해야 합니다.               │"
    echo "│                                                              │"
    echo "│  방법 1) 실행 전 export:                                     │"
    echo "│    export MEGA_DB_URL=jdbc:mariadb://localhost:3306/ledmega  │"
    echo "│    export MEGA_DB_USER=ledmega                               │"
    echo "│    export MEGA_DB_PASSWORD=your_password                     │"
    echo "│                                                              │"
    echo "│  방법 2) run.sh 옆에 .env 파일 생성 (setup.sh가 자동 로드)   │"
    echo "└──────────────────────────────────────────────────────────────┘"
fi

# ── 6. .env 파일이 있으면 로드 ────────────────────────────────────────────────
if [ -f "$PROJECT_DIR/.env" ]; then
    echo "[env] .env 파일을 로드합니다..."
    set -a
    source "$PROJECT_DIR/.env"
    set +a
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
echo " 실행: bash run.sh"
echo "=============================="
