#!/bin/bash

# Clear Books 更新脚本
# 本地构建 → 上传服务器 → 重启服务

set -e

echo "=========================================="
echo "Clear Books 更新脚本"
echo "=========================================="

# 检查参数
if [ $# -lt 1 ]; then
    echo "用法: $0 <服务器IP> [服务器用户名]"
    echo "示例: $0 192.168.1.100"
    echo "      $0 192.168.1.100 root"
    exit 1
fi

SERVER_IP=$1
SERVER_USER=${2:-root}
PROJECT_DIR="/opt/clear-books"

echo ""
echo "目标服务器: $SERVER_USER@$SERVER_IP"
echo ""

# 1. 本地构建
echo "[1/3] 本地构建..."
./build.sh

# 2. 上传 JAR
echo ""
echo "[2/3] 上传到服务器..."
scp backend/target/personal-accounting-1.0.0.jar $SERVER_USER@$SERVER_IP:$PROJECT_DIR/

# 3. 远程重启
echo ""
echo "[3/3] 重启服务..."
ssh $SERVER_USER@$SERVER_IP "cd $PROJECT_DIR && ./stop.sh && ./start.sh"

echo ""
echo "=========================================="
echo "更新完成！"
echo "=========================================="
echo "访问地址: http://$SERVER_IP:8080"
