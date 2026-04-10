#!/bin/bash
# Clear Books Local Build Script (Linux/macOS)
# Usage: ./build.sh
# Note: Can be executed from any directory

set -e

# Get project root (two levels up from script location)
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_ROOT="$(cd "$(dirname "$SCRIPT_DIR")/.." && pwd)"

# Switch to project root
cd "$PROJECT_ROOT"

echo "=== Building Clear Books ==="
echo "Project: $PROJECT_ROOT"

# 1. Build frontend
echo "[1/3] Building frontend..."
cd frontend
npm install
npm run build
cd ..

# 2. Copy to backend static
echo "[2/3] Merging frontend & backend..."
mkdir -p backend/src/main/resources/static
rm -rf backend/src/main/resources/static/*
cp -r frontend/dist/* backend/src/main/resources/static/

# 3. Package JAR
echo "[3/3] Packaging JAR..."
cd backend
mvn clean package -DskipTests
cd ..

echo ""
echo "=== Build Successful ==="
ls -lh backend/target/personal-accounting-*.jar
echo ""
echo "Upload these files to your server:"
echo "  - backend/target/personal-accounting-*.jar"
echo "  - scripts/server/start.sh"
echo "  - scripts/server/stop.sh"
echo "  - .env (config file)"
