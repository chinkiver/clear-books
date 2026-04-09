#!/bin/bash

# Clear Books Docker 生产部署脚本
# 使用方法: ./deploy.sh

set -e  # 遇到错误立即退出

echo "=========================================="
echo "Clear Books 生产部署脚本"
echo "=========================================="

# 检查是否以 root 运行
if [ "$EUID" -ne 0 ]; then 
    echo "请使用 sudo 运行此脚本"
    exit 1
fi

# 1. 检查 Docker 和 Docker Compose
if ! command -v docker &> /dev/null; then
    echo "安装 Docker..."
    curl -fsSL https://get.docker.com | sh
    systemctl enable docker
    systemctl start docker
fi

if ! command -v docker-compose &> /dev/null; then
    echo "安装 Docker Compose..."
    curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
    chmod +x /usr/local/bin/docker-compose
fi

# 2. 创建必要的目录
echo "创建目录结构..."
mkdir -p /opt/clear-books
mkdir -p /opt/clear-books/logs
mkdir -p /opt/clear-books/data/mysql
mkdir -p /opt/clear-books/nginx/ssl
mkdir -p /opt/clear-books/backups

# 3. 复制项目文件（假设脚本在项目根目录运行）
echo "复制项目文件..."
PROJECT_DIR="$(cd "$(dirname "$0")" && pwd)"
cp -r "$PROJECT_DIR"/* /opt/clear-books/

# 4. 检查 .env 文件
if [ ! -f /opt/clear-books/.env ]; then
    echo "警告: 未找到 .env 文件，使用默认配置"
    echo "请复制 .env.example 为 .env 并修改配置"
    cp /opt/clear-books/.env.example /opt/clear-books/.env
fi

# 5. 生成 SSL 证书（如果没有）
SSL_DIR="/opt/clear-books/nginx/ssl"
if [ ! -f "$SSL_DIR/cert.pem" ] || [ ! -f "$SSL_DIR/key.pem" ]; then
    echo "生成自签名 SSL 证书..."
    openssl req -x509 -nodes -days 365 -newkey rsa:2048 \
        -keyout "$SSL_DIR/key.pem" \
        -out "$SSL_DIR/cert.pem" \
        -subj "/C=CN/ST=Beijing/L=Beijing/O=ClearBooks/CN=localhost"
    echo "注意: 使用的是自签名证书，浏览器会有安全警告"
    echo "建议后续使用 Let's Encrypt 替换为正式证书"
fi

# 6. 设置权限
chmod 600 /opt/clear-books/.env
chmod -R 755 /opt/clear-books/logs

# 7. 构建并启动服务
cd /opt/clear-books
echo "构建 Docker 镜像..."
docker-compose build --no-cache

echo "启动服务..."
docker-compose up -d

# 8. 等待服务启动
echo "等待服务启动..."
sleep 10

# 9. 检查服务状态
echo "检查服务状态..."
if docker-compose ps | grep -q "Up"; then
    echo "=========================================="
    echo "部署成功！"
    echo "=========================================="
    echo "应用地址: https://$(curl -s ip.sb)"
    echo ""
    echo "查看日志: docker-compose logs -f"
    echo "停止服务: docker-compose down"
    echo "重启服务: docker-compose restart"
    echo "=========================================="
else
    echo "部署可能失败，请检查日志:"
    docker-compose logs
    exit 1
fi
