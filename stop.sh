#!/bin/bash

# Clear Books 停止脚本

PROJECT_DIR="/opt/clear-books"
PID_FILE="$PROJECT_DIR/app.pid"

if [ ! -f "$PID_FILE" ]; then
    echo "应用未运行"
    exit 0
fi

PID=$(cat "$PID_FILE")

if ! ps -p "$PID" > /dev/null 2>&1; then
    echo "应用未运行 (PID 文件已失效)"
    rm -f "$PID_FILE"
    exit 0
fi

echo "正在停止 Clear Books (PID: $PID)..."

# 先尝试优雅停止
kill "$PID"

# 等待最多 10 秒
for i in {1..10}; do
    if ! ps -p "$PID" > /dev/null 2>&1; then
        echo "已停止"
        rm -f "$PID_FILE"
        exit 0
    fi
    sleep 1
done

# 强制停止
echo "强制停止..."
kill -9 "$PID" 2>/dev/null || true
rm -f "$PID_FILE"
echo "已停止"
