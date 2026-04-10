#!/bin/bash

# Clear Books - Docker 国内镜像安装脚本
# 适用于腾讯云轻量应用服务器等国内环境

set -e

echo "=========================================="
echo "开始安装 Docker（使用国内镜像源）"
echo "=========================================="

# 1. 卸载旧版本（如果有）
echo "[1/6] 卸载旧版本 Docker..."
apt-get remove -y docker docker-engine docker.io containerd runc 2>/dev/null || true

# 2. 更新 apt 并安装依赖
echo "[2/6] 安装必要依赖..."
apt-get update
apt-get install -y \
    apt-transport-https \
    ca-certificates \
    curl \
    gnupg \
    lsb-release

# 3. 添加 Docker 国内镜像源（中科大）
echo "[3/6] 添加 Docker 国内镜像源..."
if [ ! -f /etc/apt/keyrings/docker.gpg ]; then
    mkdir -p /etc/apt/keyrings
    # 使用国内镜像的 GPG key
    curl -fsSL https://mirrors.ustc.edu.cn/docker-ce/linux/ubuntu/gpg | gpg --dearmor -o /etc/apt/keyrings/docker.gpg
    chmod a+r /etc/apt/keyrings/docker.gpg
fi

# 4. 添加软件源
echo "[4/6] 添加软件源..."
echo \
  "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://mirrors.ustc.edu.cn/docker-ce/linux/ubuntu \
  $(lsb_release -cs) stable" | tee /etc/apt/sources.list.d/docker.list > /dev/null

# 5. 安装 Docker
echo "[5/6] 安装 Docker..."
apt-get update
apt-get install -y docker-ce docker-ce-cli containerd.io docker-compose-plugin

# 6. 启动并配置 Docker
echo "[6/6] 配置 Docker..."
systemctl enable docker
systemctl start docker

# 配置国内镜像源加速
cat > /etc/docker/daemon.json << 'EOF'
{
  "registry-mirrors": [
    "https://docker.mirrors.ustc.edu.cn",
    "https://hub-mirror.c.163.com",
    "https://mirror.baidubce.com",
    "https://docker.m.daocloud.io"
  ]
}
EOF

# 重启 Docker 使配置生效
systemctl restart docker

# 验证安装
echo ""
echo "=========================================="
echo "Docker 安装完成！"
echo "=========================================="
docker --version
docker-compose --version
echo ""
echo "国内镜像源已配置，拉取镜像会更快"
echo "如需验证：docker run hello-world"
