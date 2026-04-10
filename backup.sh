#!/bin/bash

# Clear Books 传统方式备份脚本
# 备份 MySQL 数据库和配置文件

set -e

# 配置
BACKUP_DIR="/opt/clear-books/backups"
PROJECT_DIR="/opt/clear-books"
DATE=$(date +%Y%m%d_%H%M%S)
KEEP_DAYS=30

# 读取环境变量
if [ -f "$PROJECT_DIR/.env" ]; then
    source "$PROJECT_DIR/.env"
fi

MYSQL_ROOT_PASSWORD="${MYSQL_ROOT_PASSWORD:-ClearBooksRoot2024!}"

mkdir -p "$BACKUP_DIR"

echo "[$(date)] 开始备份..."

# 1. 备份 MySQL 数据库
echo "备份数据库..."
mysqldump -u root -p"${MYSQL_ROOT_PASSWORD}" accounting > "$BACKUP_DIR/db_$DATE.sql"
gzip "$BACKUP_DIR/db_$DATE.sql"

# 2. 备份应用配置
echo "备份配置文件..."
tar -czf "$BACKUP_DIR/config_$DATE.tar.gz" -C "$PROJECT_DIR" .env backend/src/main/resources application-prod.yml 2>/dev/null || \
tar -czf "$BACKUP_DIR/config_$DATE.tar.gz" -C "$PROJECT_DIR" .env

# 3. 上传到腾讯云 COS（如果配置了）
if [ -n "${COS_BUCKET:-}" ] && command -v coscli &> /dev/null; then
    echo "上传到腾讯云 COS..."
    coscli cp "$BACKUP_DIR/db_$DATE.sql.gz" cos://"$COS_BUCKET"/backups/ 2>/dev/null || echo "COS 上传失败"
fi

# 4. 清理旧备份
echo "清理 $KEEP_DAYS 天前的备份..."
find "$BACKUP_DIR" -name "*.gz" -mtime +$KEEP_DAYS -delete 2>/dev/null || true

# 5. 输出备份信息
BACKUP_SIZE=$(du -sh "$BACKUP_DIR" | cut -f1)
echo "[$(date)] 备份完成"
echo "备份文件:"
ls -lh "$BACKUP_DIR"/*_$DATE* 2>/dev/null || echo "无备份文件"
echo "备份目录总大小: $BACKUP_SIZE"
