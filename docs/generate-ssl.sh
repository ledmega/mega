#!/usr/bin/env bash
# =============================================================================
# nginx용 자체 서명(self-signed) SSL 인증서 생성 스크립트
# 실행: bash generate-ssl.sh
# 생성 위치: /home/a86223/nginx/ssl/
# =============================================================================

SSL_DIR="/home/a86223/nginx/ssl"
mkdir -p "$SSL_DIR"

echo "[ssl] 인증서 생성 중..."

openssl req -x509 -nodes -days 3650 \
    -newkey rsa:2048 \
    -keyout "$SSL_DIR/server.key" \
    -out    "$SSL_DIR/server.crt" \
    -subj "/C=KR/ST=Seoul/L=Seoul/O=Mega/OU=Dev/CN=192.168.0.77"

# 권한 설정 (key는 nginx만 읽기)
chmod 600 "$SSL_DIR/server.key"
chmod 644 "$SSL_DIR/server.crt"

echo "[ssl] 인증서 생성 완료!"
echo "  인증서: $SSL_DIR/server.crt"
echo "  개인키: $SSL_DIR/server.key"
echo "  유효기간: 10년"
echo ""
echo "[ssl] nginx docker-compose.yml 에 볼륨 마운트 확인:"
echo "  - /home/a86223/nginx/ssl:/etc/nginx/ssl:ro"
