#!/bin/bash

# ==========================================
# MEGA Agent 관리 스크립트
# ==========================================

MEGA_DIR="/home/a86223/ws/mega"
AGENT_DIR="/home/a86223/ws/agent"
JAVA_HOME="/home/a86223/ws/mega/jdk"
JAVA_CMD="$JAVA_HOME/bin/java"
JAR_FILE="mega-agent-0.0.1-SNAPSHOT.jar"
PID_FILE="$AGENT_DIR/agent.pid"
LOG_FILE="$AGENT_DIR/logs/agent_sys.log"

export JAVA_HOME
export PATH=$JAVA_HOME/bin:$PATH

function check_status() {
    if [ -f "$PID_FILE" ]; then
        PID=$(cat "$PID_FILE")
        if ps -p $PID > /dev/null; then
            return 0 # Running
        else
            # PID 파일은 있는데 프로세스가 없는 경우 (비정상 종료)
            rm -f "$PID_FILE"
            return 1 # Not running
        fi
    fi
    return 1 # Not running
}

function start() {
    if check_status; then
        echo "⚠️  Agent가 이미 실행 중입니다. (PID: $(cat $PID_FILE))"
        exit 1
    fi

    echo "🚀 Agent 시작 중..."
    mkdir -p "$AGENT_DIR/logs"
    
    cd "$AGENT_DIR" || exit 1
    nohup $JAVA_CMD -jar "$JAR_FILE" > "$LOG_FILE" 2>&1 &
    PID=$!
    echo $PID > "$PID_FILE"
    
    echo "✅ Agent가 실행되었습니다. (PID: $PID)"
    echo "📄 로그 확인: tail -f $LOG_FILE"
}

function stop() {
    if check_status; then
        PID=$(cat "$PID_FILE")
        echo "🛑 Agent 종료 중... (PID: $PID)"
        kill -15 $PID
        
        # 정상 종료 대기
        for i in {1..5}; do
            if ps -p $PID > /dev/null; then
                sleep 1
            else
                break
            fi
        done
        
        # 그래도 살아있으면 강제 종료
        if ps -p $PID > /dev/null; then
            echo "⚠️  정상 종료되지 않아 강제 종료(kill -9) 합니다."
            kill -9 $PID
        fi
        
        rm -f "$PID_FILE"
        echo "✅ Agent가 종료되었습니다."
    else
        echo "ℹ️  Agent가 현재 실행 중이 아닙니다."
    fi
}

function rebuild() {
    echo "🔄 최신 소스코드 다운로드 및 빌드 시작..."
    
    cd "$MEGA_DIR" || exit 1
    
    # 1. 최신 코드 가져오기
    echo "▶️ git pull..."
    git pull origin feature/reactive
    
    # 2. 에이전트 빌드
    echo "▶️ gradle build..."
    ./gradlew :agent:fatJar
    
    if [ $? -eq 0 ]; then
        echo "✅ 빌드 성공!"
        # 3. 새 JAR 파일 복사
        echo "▶️ 새로운 JAR 파일을 $AGENT_DIR 로 복사합니다."
        cp -f "$MEGA_DIR/agent/build/libs/$JAR_FILE" "$AGENT_DIR/"
    else
        echo "❌ 빌드 실패! 스크립트를 중단합니다."
        exit 1
    fi
}

function status() {
    if check_status; then
        echo "🟢 Agent 상태: [ON] 실행 중 (PID: $(cat $PID_FILE))"
    else
        echo "🔴 Agent 상태: [OFF] 정지됨"
    fi
}

# 파라미터에 따른 분기
case "$1" in
    start)
        start
        ;;
    stop)
        stop
        ;;
    restart)
        stop
        sleep 2
        start
        ;;
    rebuild)
        stop
        rebuild
        start
        ;;
    status)
        status
        ;;
    *)
        echo "=========================================="
        echo "사용법: ./agent.sh {start|stop|restart|rebuild|status}"
        echo "=========================================="
        echo "  start   : 에이전트 실행"
        echo "  stop    : 에이전트 종료"
        echo "  restart : 에이전트 재시작"
        echo "  rebuild : Git 최신화 + 재빌드 후 에이전트 실행"
        echo "  status  : 에이전트 실행 상태 확인"
        echo "=========================================="
        exit 1
        ;;
esac

exit 0
