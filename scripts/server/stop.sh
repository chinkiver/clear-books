#!/bin/bash
# Clear Books Server Stop Script

# Get script directory
PROJECT_DIR="$(cd "$(dirname "$0")" && pwd)"
PID_FILE="$PROJECT_DIR/app.pid"

if [ ! -f "$PID_FILE" ]; then
    echo "Not running"
    exit 0
fi

PID=$(cat "$PID_FILE")
if ! ps -p "$PID" > /dev/null 2>&1; then
    echo "Not running"
    rm -f "$PID_FILE"
    exit 0
fi

echo "Stopping (PID: $PID)..."
kill "$PID"

# Wait for process to end (max 10 seconds)
for i in {1..10}; do
    if ! ps -p "$PID" > /dev/null 2>&1; then
        echo "Stopped"
        rm -f "$PID_FILE"
        exit 0
    fi
    sleep 1
done

# Force kill
echo "Force stopping..."
kill -9 "$PID" 2>/dev/null || true
rm -f "$PID_FILE"
echo "Force stopped"
