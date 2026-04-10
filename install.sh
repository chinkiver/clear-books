#!/bin/bash

# Clear Books 服务器环境安装脚本
# 在服务器上运行，安装 MySQL、JDK 等依赖

set -e

echo "=========================================="
echo "Clear Books 环境安装脚本"
echo "=========================================="

if [ "$EUID" -ne 0 ]; then 
    echo "请使用 sudo 运行此脚本"
    exit 1
fi

# 1. 安装依赖
echo "[1/4] 安装系统依赖..."
if command -v apt-get &> /dev/null; then
    # Ubuntu/Debian
    apt-get update
    apt-get install -y openjdk-17-jdk mysql-server curl
elif command -v yum &> /dev/null; then
    # CentOS/RHEL
    yum install -y java-17-openjdk mysql-server curl
else
    echo "不支持的系统，请手动安装 JDK 17 和 MySQL 8.0"
    exit 1
fi

# 2. 配置 MySQL
echo "[2/4] 配置 MySQL..."
systemctl enable mysql
systemctl start mysql

# 读取 .env 配置
PROJECT_DIR="/opt/clear-books"
if [ -f "$PROJECT_DIR/.env" ]; then
    source "$PROJECT_DIR/.env"
else
    echo "警告: 未找到 .env 文件，使用默认密码"
    MYSQL_ROOT_PASSWORD="ClearBooksRoot2024!"
    MYSQL_PASSWORD="ClearBooksDB2024!"
fi

# 创建数据库
echo "[3/4] 创建数据库..."
mysql -u root -e "CREATE DATABASE IF NOT EXISTS accounting CHARACTER SET utf8mb4;" 2>/dev/null || true
mysql -u root -e "CREATE USER IF NOT EXISTS 'accounting'@'localhost' IDENTIFIED BY '${MYSQL_PASSWORD}';" 2>/dev/null || true
mysql -u root -e "GRANT ALL ON accounting.* TO 'accounting'@'localhost';" 2>/dev/null || true
mysql -u root -e "FLUSH PRIVILEGES;" 2>/dev/null || true

# 4. 创建应用目录和用户
echo "[4/4] 创建应用目录..."
APP_USER="clearbooks"
id -u $APP_USER &>/dev/null || useradd -r -s /bin/false $APP_USER

mkdir -p $PROJECT_DIR/logs
mkdir -p $PROJECT_DIR/backups
chown -R $APP_USER:$APP_USER $PROJECT_DIR

echo ""
echo "=========================================="
echo "环境安装完成！"
echo "=========================================="
echo ""
echo "下一步:"
echo "  1. 上传 JAR 文件: scp personal-accounting-1.0.0.jar root@服务器IP:$PROJECT_DIR/"
echo "  2. 上传 .env 文件: scp .env root@服务器IP:$PROJECT_DIR/"
echo "  3. 启动服务: cd $PROJECT_DIR && ./start.sh"
echo ""
echo "MySQL 数据库已创建:"
echo "  数据库: accounting"
echo "  用户名: accounting"
echo "  密码: $MYSQL_PASSWORD"
echo "=========================================="
