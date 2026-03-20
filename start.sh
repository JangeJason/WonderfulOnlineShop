#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$SCRIPT_DIR"
BACKEND_DIR="$PROJECT_DIR/backend"
ADMIN_WEB_DIR="$PROJECT_DIR/admin-web"
CLIENT_UNIAPP_DIR="$PROJECT_DIR/client-uniapp"
LOG_DIR="$PROJECT_DIR/.logs"

BACKEND_PORT=8080
ADMIN_PORT=5174
CLIENT_PORT=5173

mkdir -p "$LOG_DIR"

timestamp() {
  date '+%Y-%m-%d %H:%M:%S'
}

stop_port_graceful() {
  local port="$1"
  local pids
  pids="$(lsof -ti tcp:"$port" || true)"
  if [ -z "$pids" ]; then
    return
  fi
  echo "[$(timestamp)] Port $port occupied by PID(s): $pids, stopping..."
  kill $pids 2>/dev/null || true
  sleep 1
  local remain
  remain="$(lsof -ti tcp:"$port" || true)"
  if [ -n "$remain" ]; then
    echo "[$(timestamp)] Force killing remaining PID(s) on $port: $remain"
    kill -9 $remain 2>/dev/null || true
  fi
}

ensure_cmd() {
  local cmd="$1"
  if ! command -v "$cmd" >/dev/null 2>&1; then
    echo "[$(timestamp)] Missing command: $cmd"
    exit 1
  fi
}

echo "==========================================="
echo "  Starting WonderfulOnlineShop Services"
echo "==========================================="

ensure_cmd lsof
ensure_cmd mvn
ensure_cmd npm

echo "[$(timestamp)] Cleaning old processes..."
stop_port_graceful "$BACKEND_PORT"
stop_port_graceful "$ADMIN_PORT"
stop_port_graceful "$CLIENT_PORT"

echo "[$(timestamp)] Starting Backend (Port $BACKEND_PORT)..."
cd "$BACKEND_DIR"
nohup mvn spring-boot:run > "$LOG_DIR/backend.log" 2>&1 &
BACKEND_PID=$!
echo "[$(timestamp)] Backend PID: $BACKEND_PID"

echo "[$(timestamp)] Starting Admin Web (Port $ADMIN_PORT)..."
cd "$ADMIN_WEB_DIR"
nohup npm run dev > "$LOG_DIR/admin-web.log" 2>&1 &
ADMIN_PID=$!
echo "[$(timestamp)] Admin Web PID: $ADMIN_PID"

echo "[$(timestamp)] Starting Client UniApp H5 (Port $CLIENT_PORT)..."
cd "$CLIENT_UNIAPP_DIR"
nohup npm run dev:h5 > "$LOG_DIR/client-uniapp.log" 2>&1 &
CLIENT_PID=$!
echo "[$(timestamp)] Client UniApp PID: $CLIENT_PID"

sleep 2

echo "==========================================="
echo "Startup requested."
echo "- Backend API: http://localhost:$BACKEND_PORT"
echo "- Client Web:  http://localhost:$CLIENT_PORT"
echo "- Admin Web:   http://localhost:$ADMIN_PORT"
echo ""
echo "Health check:"
lsof -i tcp:"$BACKEND_PORT" | sed -n '1,3p' || true
lsof -i tcp:"$CLIENT_PORT" | sed -n '1,3p' || true
lsof -i tcp:"$ADMIN_PORT" | sed -n '1,3p' || true
echo ""
echo "Logs:"
echo "- $LOG_DIR/backend.log"
echo "- $LOG_DIR/client-uniapp.log"
echo "- $LOG_DIR/admin-web.log"
echo ""
echo "Stop with: ./stop.sh"
echo "==========================================="
