#!/usr/bin/env bash
set -euo pipefail

BACKEND_PORT=8080
ADMIN_PORT=5174
CLIENT_PORT=5173

timestamp() {
  date '+%Y-%m-%d %H:%M:%S'
}

stop_port_graceful() {
  local port="$1"
  local name="$2"
  local pids
  pids="$(lsof -ti tcp:"$port" || true)"
  if [ -z "$pids" ]; then
    echo "[$(timestamp)] $name: no process on port $port"
    return
  fi
  echo "[$(timestamp)] Stopping $name on port $port (PID: $pids)"
  kill $pids 2>/dev/null || true
  sleep 1
  local remain
  remain="$(lsof -ti tcp:"$port" || true)"
  if [ -n "$remain" ]; then
    echo "[$(timestamp)] Force killing $name remaining PID(s): $remain"
    kill -9 $remain 2>/dev/null || true
  fi
}

echo "==========================================="
echo "  Stopping WonderfulOnlineShop Services"
echo "==========================================="

stop_port_graceful "$BACKEND_PORT" "Backend"
stop_port_graceful "$ADMIN_PORT" "Admin Web"
stop_port_graceful "$CLIENT_PORT" "Client UniApp"

echo "==========================================="
echo "All services stopped."
echo "==========================================="
