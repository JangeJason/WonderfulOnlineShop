#!/bin/bash

# Configuration
PROJECT_DIR="$(pwd)"
BACKEND_DIR="$PROJECT_DIR/backend"
ADMIN_WEB_DIR="$PROJECT_DIR/admin-web"
CLIENT_UNIAPP_DIR="$PROJECT_DIR/client-uniapp"

echo "==========================================="
echo "  Starting WonderfulOnlineShop Services"
echo "==========================================="

echo "Killing existing processes on ports 8080, 5174, 5173..."
lsof -ti:8080 | xargs kill -9 2>/dev/null
lsof -ti:5174 | xargs kill -9 2>/dev/null
lsof -ti:5173 | xargs kill -9 2>/dev/null

echo "Starting Backend (Port 8080)..."
cd "$BACKEND_DIR" || exit 1
mvn spring-boot:run > backend.log 2>&1 &
BACKEND_PID=$!
echo "Backend started with PID $BACKEND_PID (logs in backend/backend.log)"

echo "Starting Admin Web (Port 5174)..."
cd "$ADMIN_WEB_DIR" || exit 1
npm run dev > admin.log 2>&1 &
ADMIN_PID=$!
echo "Admin Web started with PID $ADMIN_PID (logs in admin-web/admin.log)"

echo "Starting Client UniApp (Port 5173)..."
cd "$CLIENT_UNIAPP_DIR" || exit 1
npm run dev:h5 > client.log 2>&1 &
CLIENT_PID=$!
echo "Client UniApp started with PID $CLIENT_PID (logs in client-uniapp/client.log)"

echo "==========================================="
echo "All services are starting up in the background!"
echo "- Backend API: http://localhost:8080"
echo "- Client Web:  http://localhost:5173"
echo "- Admin Web:   http://localhost:5174"
echo ""
echo "To stop all services, run: ./stop.sh (if created) or manually kill the processes."
echo "==========================================="
