#!/bin/bash

# Clear Books 本地构建脚本
# 在开发机/本地运行，生成生产环境 JAR 包

set -e

echo "=========================================="
echo "Clear Books 构建脚本"
echo "=========================================="

# 检查依赖
if ! command -v mvn &> /dev/null; then
    echo "错误: 未找到 Maven，请先安装 Maven"
    exit 1
fi

if ! command -v npm &> /dev/null; then
    echo "错误: 未找到 npm，请先安装 Node.js"
    exit 1
fi

# 1. 构建前端
echo "[1/3] 构建前端..."
cd frontend
npm install
npm run build

# 2. 复制到后端
echo "[2/3] 复制前端到后端..."
cd ..
mkdir -p backend/src/main/resources/static
rm -rf backend/src/main/resources/static/*
cp -r frontend/dist/* backend/src/main/resources/static/

# 3. 构建后端 JAR
echo "[3/3] 构建 JAR 包..."
cd backend
mvn clean package -DskipTests

echo ""
echo "=========================================="
echo "构建完成！"
echo "=========================================="
echo "JAR 文件: backend/target/personal-accounting-1.0.0.jar"
echo ""
echo "下一步:"
echo "  1. 上传 JAR 到服务器:"
echo "     scp backend/target/personal-accounting-1.0.0.jar root@服务器IP:/opt/clear-books/"
echo ""
echo "  2. 上传 .env 配置文件:"
echo "     scp .env root@服务器IP:/opt/clear-books/"
echo ""
echo "  3. 在服务器上运行:"
echo "     ssh root@服务器IP 'cd /opt/clear-books && ./start.sh'"
echo "=========================================="
