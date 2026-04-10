#!/bin/bash
# Clear Books Server Startup Script
# Usage: ./start.sh [jar-file]
# Example: ./start.sh
# Example: ./start.sh personal-accounting-1.0.0.jar

# Get script directory as project root
PROJECT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$PROJECT_DIR"

# Create required directories
mkdir -p "logs"
mkdir -p "backups"

# Load environment variables
if [ -f ".env" ]; then
    set -a
    source ".env"
    set +a
fi

# Find JAR file
if [ -n "$1" ]; then
    # Use provided JAR file
    JAR_FILE="$1"
    if [ ! -f "$JAR_FILE" ]; then
        echo "Error: JAR file not found: $JAR_FILE"
        exit 1
    fi
else
    # Auto-detect latest JAR file
    JAR_FILE=$(ls -t personal-accounting-*.jar 2>/dev/null | head -1)
    if [ -z "$JAR_FILE" ]; then
        echo "Error: JAR file not found"
        echo "Usage: $0 [jar-file]"
        echo "Example: $0 personal-accounting-1.0.0.jar"
        exit 1
    fi
fi

# Check if already running
if [ -f "app.pid" ]; then
    PID=$(cat "app.pid")
    if ps -p "$PID" > /dev/null 2>&1; then
        echo "Already running (PID: $PID, JAR: $JAR_FILE)"
        exit 0
    fi
fi

echo "Starting $JAR_FILE ..."
nohup java -jar "$JAR_FILE" --spring.profiles.active=prod > /dev/null 2>&1 &
echo $! > "app.pid"

# Wait for confirmation
sleep 2
if ps -p $(cat "app.pid") > /dev/null 2>&1; then
    echo "Started successfully! Access: http://$(curl -s ip.sb 2>/dev/null || echo 'SERVER_IP'):6173"
else
    echo "Failed to start"
    rm -f "app.pid"
    exit 1
fi
