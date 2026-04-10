#!/bin/bash

# Clear Books 启动脚本

PROJECT_DIR="/opt/clear-books"
JAR_FILE="$PROJECT_DIR/personal-accounting-1.0.0.jar"
APP_USER="clearbooks"
PID_FILE="$PROJECT_DIR/app.pid"

cd $PROJECT_DIR

# 检查 JAR 文件是否存在
if [ ! -f "$JAR_FILE" ]; then
    echo "错误: 未找到 JAR 文件: $JAR_FILE"
    echo "请先上传 JAR 文件到 $PROJECT_DIR/"
    exit 1
fi

# 检查 .env 文件
if [ ! -f "$PROJECT_DIR/.env" ]; then
    echo "警告: 未找到 .env 文件，将使用默认配置"
fi

# 检查是否已在运行
if [ -f "$PID_FILE" ]; then
    PID=$(cat "$PID_FILE")
    if ps -p "$PID" > /dev/null 2>&1; then
        echo "应用已在运行 (PID: $PID)"
        echo "访问: http://$(curl -s ip.sb 2>/dev/null || echo '服务器IP'):8080"
        exit 0
    else
        rm -f "$PID_FILE"
    fi
fi

echo "正在启动 Clear Books..."

# 启动应用
nohup java -jar "$JAR_FILE" \
    --spring.profiles.active=prod \
    > /dev/null 2>&1 &
    
# 保存 PID
NEW_PID=$!
echo $NEW_PID > "$PID_FILE"

sleep 3

# 检查是否启动成功
if ps -p "$NEW_PID" > /dev/null 2>&1; then
    echo "启动成功！"
    echo ""
    echo "访问地址: http://$(curl -s ip.sb 2>/dev/null || echo '服务器IP'):8080"
    echo "进程 PID: $NEW_PID"
    echo ""
    echo "查看日志: tail -f $PROJECT_DIR/logs/app.log"
    echo "停止服务: ./stop.sh"
else
    echo "启动失败，请检查日志"
    rm -f "$PID_FILE"
    exit 1
fi
