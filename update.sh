#!/bin/bash

# Clear Books 更新脚本
# 用于更新应用到最新版本

set -e

PROJECT_DIR="/opt/clear-books"
BACKUP_DIR="/opt/clear-books/backups"
DATE=$(date +%Y%m%d_%H%M%S)

echo "=========================================="
echo "Clear Books 更新脚本"
echo "=========================================="

cd "$PROJECT_DIR"

# 1. 备份当前版本
echo "备份当前版本..."
tar -czf "$BACKUP_DIR/pre_update_$DATE.tar.gz" -C "$PROJECT_DIR" .

# 2. 拉取最新代码（如果使用 git）
if [ -d ".git" ]; then
    echo "拉取最新代码..."
    git pull origin master
fi

# 3. 停止服务
echo "停止当前服务..."
docker-compose down

# 4. 重新构建
echo "重新构建镜像..."
docker-compose build --no-cache

# 5. 启动服务
echo "启动服务..."
docker-compose up -d

# 6. 清理旧镜像
echo "清理旧镜像..."
docker image prune -f

# 7. 检查状态
sleep 5
if docker-compose ps | grep -q "Up"; then
    echo "=========================================="
    echo "更新成功！"
    echo "=========================================="
    echo "如需回滚，使用备份: $BACKUP_DIR/pre_update_$DATE.tar.gz"
else
    echo "更新可能失败，正在回滚..."
    docker-compose down
    # 解压备份并恢复
    tar -xzf "$BACKUP_DIR/pre_update_$DATE.tar.gz" -C "$PROJECT_DIR"
    docker-compose up -d
    echo "已回滚到之前的版本"
    exit 1
fi
