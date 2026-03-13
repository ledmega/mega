#!/usr/bin/env bash
# =============================================================================
# mega webserver 실행 스크립트
# 실행: bash run.sh [start|stop|restart|status|log]
# =============================================================================
set -e

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
JDK_DIR="$PROJECT_DIR/jdk"
PID_FILE="$PROJECT_DIR/mega.pid"
LOG_DIR="$PROJECT_DIR/logs"
LOG_FILE="$LOG_DIR/app.log"

export JAVA_HOME="$JDK_DIR"
export PATH="$JAVA_HOME/bin:$PATH"

# .env 파일이 있으면 자동 로드
if [ -f "$PROJECT_DIR/.env" ]; then
    set -a
    source "$PROJECT_DIR/.env"
    set +a
fi

JAR=$(find "$PROJECT_DIR/webserver/build/libs" -name "*.jar" ! -name "*plain*" 2>/dev/null | head -1)

# ─── 함수 ────────────────────────────────────────────────────────────────────

is_running() {
    if [ -f "$PID_FILE" ]; then
        local pid
        pid=$(cat "$PID_FILE")
        kill -0 "$pid" 2>/dev/null && return 0
    fi
    return 1
}

do_start() {
    if is_running; then
        echo "[mega] 이미 실행 중입니다. (PID: $(cat "$PID_FILE"))"
        return
    fi

    if [ -z "$JAR" ]; then
        echo "[mega] JAR 파일을 찾을 수 없습니다. 먼저 setup.sh를 실행하세요."
        exit 1
    fi

    mkdir -p "$LOG_DIR"
    echo "[mega] 서버 시작 중..."
    echo "[mega] JAR: $JAR"

    nohup "$JAVA_HOME/bin/java" \
        -Dfile.encoding=UTF-8 \
        -Duser.language=ko \
        -Duser.country=KR \
        -jar "$JAR" \
        >> "$LOG_FILE" 2>&1 &

    echo $! > "$PID_FILE"
    echo "[mega] 서버가 시작되었습니다. (PID: $(cat "$PID_FILE"))"
    echo "[mega] 로그: tail -f $LOG_FILE"
}

do_stop() {
    if ! is_running; then
        echo "[mega] 실행 중인 서버가 없습니다."
        rm -f "$PID_FILE"
        return
    fi

    local pid
    pid=$(cat "$PID_FILE")
    echo "[mega] 서버 종료 중... (PID: $pid)"
    kill "$pid"

    local count=0
    while kill -0 "$pid" 2>/dev/null; do
        sleep 1
        count=$((count + 1))
        if [ $count -ge 30 ]; then
            echo "[mega] 강제 종료합니다."
            kill -9 "$pid"
            break
        fi
    done

    rm -f "$PID_FILE"
    echo "[mega] 서버가 종료되었습니다."
}

do_status() {
    if is_running; then
        echo "[mega] 실행 중 (PID: $(cat "$PID_FILE"))"
    else
        echo "[mega] 정지 상태"
    fi
}

do_log() {
    if [ -f "$LOG_FILE" ]; then
        tail -f "$LOG_FILE"
    else
        echo "[mega] 로그 파일이 없습니다: $LOG_FILE"
    fi
}

do_log_batch() {
    local batch_log="$LOG_DIR/mega-batch.log"
    if [ -f "$batch_log" ]; then
        tail -f "$batch_log"
    else
        echo "[mega] 배치 로그 파일이 없습니다: $batch_log"
    fi
}

do_log_agent() {
    local agent_log="$LOG_DIR/mega-agent.log"
    if [ -f "$agent_log" ]; then
        tail -f "$agent_log"
    else
        echo "[mega] 에이전트 로그 파일이 없습니다: $agent_log"
    fi
}

do_log_security() {
    local security_log="$LOG_DIR/mega-security.log"
    if [ -f "$security_log" ]; then
        tail -f "$security_log"
    else
        echo "[mega] 보안 로그 파일이 없습니다: $security_log"
    fi
}

do_log_alert() {
    local alert_log="$LOG_DIR/mega-alert.log"
    if [ -f "$alert_log" ]; then
        tail -f "$alert_log"
    else
        echo "[mega] 알림 로그 파일이 없습니다: $alert_log"
    fi
}

do_rebuild() {
    echo "[mega] 재빌드 및 서버 적용 중..."
    cd "$PROJECT_DIR"
    [ -x gradlew ] || chmod +x gradlew
    ./gradlew :webserver:bootJar -x test --no-daemon
    JAR=$(find "$PROJECT_DIR/webserver/build/libs" -name "*.jar" ! -name "*plain*" | head -1)
    echo "[mega] 빌드 완료: $JAR"
}

# ─── 명령 분기 ────────────────────────────────────────────────────────────────

CMD="${1:-start}"

case "$CMD" in
    start)   do_start ;;
    stop)    do_stop ;;
    restart) do_stop; sleep 2; do_start ;;
    status)  do_status ;;
    log)        do_log ;;
    log-batch)  do_log_batch ;;
    log-agent)  do_log_agent ;;
    log-sec)    do_log_security ;;
    log-alert)  do_log_alert ;;
    rebuild)    do_rebuild; do_stop; sleep 2; do_start ;;
    update)  do_rebuild; do_stop; sleep 2; do_start ;;
    *)
        echo "사용법: $0 [start|stop|restart|status|log|log-batch|log-agent|log-sec|log-alert|rebuild|update]"
        echo ""
        echo "  start      - 서버 시작"
        echo "  stop       - 서버 종료"
        echo "  restart    - 서버 재시작"
        echo "  status     - 실행 상태 확인"
        echo "  log        - 전체 실시간 로그 출력"
        echo "  log-batch  - 배치 스케줄러 로그만 출력"
        echo "  log-agent  - 에이전트 통신 로그만 출력"
        echo "  log-sec    - 보안 및 인증 로그만 출력"
        echo "  log-alert  - 시스템 알림 및 경고 로그만 출력"
        echo "  rebuild    - 재빌드 → 서버 재시작 (git pull은 수동으로 먼저 수행)"
        echo "  update     - rebuild와 동일 (rebuild의 별칭)"
        exit 1
        ;;
esac
