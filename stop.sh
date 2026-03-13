#!/bin/bash

echo "==========================================="
echo "  Stopping WonderfulOnlineShop Services"
echo "==========================================="

echo "Killing process on Port 8080 (Backend)..."
lsof -ti:8080 | xargs kill -9 2>/dev/null

echo "Killing process on Port 5174 (Admin Web)..."
lsof -ti:5174 | xargs kill -9 2>/dev/null

echo "Killing process on Port 5173 (Client UniApp)..."
lsof -ti:5173 | xargs kill -9 2>/dev/null

echo "All services stopped."
echo "==========================================="
