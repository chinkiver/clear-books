#!/bin/bash

# Clear Books 自动备份脚本
# 可以添加到 crontab 定时执行

set -e

# 配置
BACKUP_DIR="/opt/clear-books/backups"
DATE=$(date +%Y%m%d_%H%M%S)
KEEP_DAYS=30
PROJECT_DIR="/opt/clear-books"

# 腾讯云 COS 配置（可选）
COS_BUCKET="${COS_BUCKET:-}"
COS_REGION="${COS_REGION:-ap-guangzhou}"

mkdir -p "$BACKUP_DIR"

echo "[$(date)] 开始备份..."

# 1. 备份 MySQL 数据库
echo "备份 MySQL 数据库..."
docker exec clear-books-mysql mysqldump -u root -p"${MYSQL_ROOT_PASSWORD}" accounting > "$BACKUP_DIR/db_$DATE.sql"
gzip "$BACKUP_DIR/db_$DATE.sql"

# 2. 备份应用数据（包括上传的文件等）
echo "备份应用数据..."
tar -czf "$BACKUP_DIR/data_$DATE.tar.gz" -C "$PROJECT_DIR" logs

# 3. 备份配置文件
echo "备份配置文件..."
tar -czf "$BACKUP_DIR/config_$DATE.tar.gz" -C "$PROJECT_DIR" .env docker-compose.yml nginx

# 4. 上传到腾讯云 COS（如果配置了）
if [ -n "$COS_BUCKET" ] && command -v coscli &> /dev/null; then
    echo "上传到腾讯云 COS..."
    coscli cp "$BACKUP_DIR/db_$DATE.sql.gz" cos://"$COS_BUCKET"/backups/
    coscli cp "$BACKUP_DIR/config_$DATE.tar.gz" cos://"$COS_BUCKET"/backups/
fi

# 5. 清理旧备份
echo "清理 $KEEP_DAYS 天前的备份..."
find "$BACKUP_DIR" -name "*.gz" -mtime +$KEEP_DAYS -delete

# 6. 输出备份信息
BACKUP_SIZE=$(du -sh "$BACKUP_DIR" | cut -f1)
echo "[$(date)] 备份完成"
echo "备份文件:"
ls -lh "$BACKUP_DIR"/*_$DATE*
echo "备份目录总大小: $BACKUP_SIZE"

# 可选：发送通知（微信/钉钉/邮件等）
# curl -s "https://sc.ftqq.com/{SCKEY}.send" -d "text=Clear Books 备份完成" -d "desp=备份时间: $DATE, 大小: $BACKUP_SIZE"
