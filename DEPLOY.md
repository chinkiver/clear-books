# Clear Books Docker 生产部署指南

本文档介绍如何使用 Docker 在腾讯云服务器上部署 Clear Books 记账系统。

## 目录

1. [环境要求](#环境要求)
2. [快速部署](#快速部署)
3. [详细步骤](#详细步骤)
4. [配置 HTTPS](#配置-https)
5. [日常运维](#日常运维)
6. [故障排查](#故障排查)

---

## 环境要求

- **服务器**: 腾讯云 CVM（推荐配置：2核4G 或以上）
- **操作系统**: Ubuntu 20.04/22.04 LTS 或 CentOS 7/8
- **域名**: 已备案的域名（可选，用于 HTTPS）
- **端口**: 服务器安全组需开放 80、443 端口

---

## 快速部署

### 方式一：使用自动部署脚本（推荐）

```bash
# 1. 登录腾讯云服务器
ssh root@你的服务器IP

# 2. 克隆项目
git clone https://github.com/yourusername/clear-books.git
cd clear-books

# 3. 复制并修改环境变量（密码自己设置，用于初始化服务）
cp .env.example .env
vim .env  # 修改为你的配置

# ⚠️ 注意：MYSQL_ROOT_PASSWORD、MYSQL_PASSWORD、JWT_SECRET 等
# 都是你自己设置的密码，不是已有的密钥！
# 建议修改成你自己的强密码

# 4. 运行部署脚本
chmod +x deploy.sh
sudo ./deploy.sh
```

部署完成后，访问 `https://你的服务器IP` 即可。

---

## 详细步骤

### 步骤 1：准备服务器

#### 1.1 购买并配置腾讯云服务器

1. 登录 [腾讯云控制台](https://console.cloud.tencent.com/)
2. 购买云服务器 CVM（推荐 2核4G 或以上配置）
3. 选择操作系统：Ubuntu 22.04 LTS
4. 配置安全组，开放端口：
   - 22 (SSH)
   - 80 (HTTP)
   - 443 (HTTPS)

#### 1.2 连接服务器

```bash
ssh root@你的服务器IP
```

#### 1.3 系统更新

```bash
apt update && apt upgrade -y
```

---

### 步骤 2：安装 Docker（如未安装）

```bash
# 安装 Docker
curl -fsSL https://get.docker.com | sh

# 启动 Docker
systemctl enable docker
systemctl start docker

# 验证
docker --version
docker-compose --version
```

#### 2.1 配置 Docker 国内镜像源（推荐）

由于 Docker Hub 国内访问慢，建议配置国内镜像源加速：

```bash
# 创建配置
sudo mkdir -p /etc/docker
sudo tee /etc/docker/daemon.json <<-'EOF'
{
  "registry-mirrors": [
    "https://docker.mirrors.ustc.edu.cn",
    "https://hub-mirror.c.163.com",
    "https://mirror.baidubce.com"
  ]
}
EOF

# 重启 Docker
sudo systemctl daemon-reload
sudo systemctl restart docker
```

更多镜像源配置参考：[DOCKER_MIRROR.md](./DOCKER_MIRROR.md)

---

### 步骤 3：上传项目代码

#### 方式 A：使用 Git

```bash
cd /opt
git clone https://github.com/yourusername/clear-books.git
cd clear-books
```

#### 方式 B：本地打包上传

在本地执行：
```bash
tar -czf clear-books.tar.gz clear-books/
scp clear-books.tar.gz root@服务器IP:/opt/
```

在服务器执行：
```bash
cd /opt
tar -xzf clear-books.tar.gz
cd clear-books
```

---

### 步骤 4：配置环境变量

```bash
cp .env.example .env
vim .env
```

修改以下配置（**必须修改密码**）：

```bash
# MySQL 密码（必须修改）
MYSQL_ROOT_PASSWORD=YourStrongRootPassword123
MYSQL_PASSWORD=YourStrongDBPassword123

# Redis 密码
REDIS_PASSWORD=YourStrongRedisPassword123

# JWT 密钥（必须修改，至少 32 位随机字符串）
JWT_SECRET=your-super-secret-jwt-key-change-this-now-32chars

# 允许的域名
CORS_ORIGINS=http://localhost,https://your-domain.com
```

---

### 步骤 5：配置 SSL 证书

#### 方式 A：使用 Let's Encrypt（推荐，有域名时）

```bash
# 安装 Certbot
apt install -y certbot

# 申请证书（替换为你的域名）
certbot certonly --standalone -d your-domain.com

# 复制证书到项目目录
cp /etc/letsencrypt/live/your-domain.com/fullchain.pem nginx/ssl/cert.pem
cp /etc/letsencrypt/live/your-domain.com/privkey.pem nginx/ssl/key.pem
```

#### 方式 B：使用自签名证书（无域名时）

```bash
# 自动生成自签名证书
mkdir -p nginx/ssl
openssl req -x509 -nodes -days 365 -newkey rsa:2048 \
    -keyout nginx/ssl/key.pem \
    -out nginx/ssl/cert.pem \
    -subj "/C=CN/ST=Beijing/L=Beijing/O=ClearBooks/CN=localhost"
```

---

### 步骤 6：构建并启动服务

```bash
# 构建镜像
docker-compose build

# 启动服务
docker-compose up -d

# 查看状态
docker-compose ps

# 查看日志
docker-compose logs -f
```

---

### 步骤 7：验证部署

1. 访问 `https://你的服务器IP` 或 `https://你的域名`
2. 注册一个测试账号
3. 尝试添加账户、分类和流水记录

---

## 配置 HTTPS

### 使用 Let's Encrypt 自动续期

```bash
# 安装 Certbot 的 Nginx 插件
apt install -y certbot python3-certbot-nginx

# 自动配置 HTTPS 并重载 Nginx
certbot --nginx -d your-domain.com

# 测试自动续期
certbot renew --dry-run

# 添加到定时任务（会自动添加）
crontab -l | grep certbot
```

### 手动更新证书

```bash
# 停止 Nginx
docker-compose stop frontend

# 更新证书
certbot renew

# 复制新证书
cp /etc/letsencrypt/live/your-domain.com/fullchain.pem nginx/ssl/cert.pem
cp /etc/letsencrypt/live/your-domain.com/privkey.pem nginx/ssl/key.pem

# 重启服务
docker-compose start frontend
```

---

## 日常运维

### 查看日志

```bash
# 查看所有服务日志
docker-compose logs -f

# 查看特定服务日志
docker-compose logs -f backend
docker-compose logs -f frontend
docker-compose logs -f mysql

# 查看最近的 100 行日志
docker-compose logs --tail=100 backend
```

### 重启服务

```bash
# 重启所有服务
docker-compose restart

# 重启特定服务
docker-compose restart backend

# 停止并重新创建容器
docker-compose down
docker-compose up -d
```

### 备份数据

#### 手动备份

```bash
# 运行备份脚本
chmod +x backup.sh
./backup.sh

# 备份文件位置
ls -lh /opt/clear-books/backups/
```

#### 自动备份（推荐）

```bash
# 编辑 crontab
crontab -e

# 添加以下行（每天凌晨 2 点备份）
0 2 * * * /opt/clear-books/backup.sh >> /var/log/clear-books-backup.log 2>&1

# 查看定时任务
crontab -l
```

### 更新应用

```bash
# 使用更新脚本
chmod +x update.sh
./update.sh

# 或者手动更新
git pull
docker-compose down
docker-compose build --no-cache
docker-compose up -d
```

### 监控资源使用

```bash
# 查看容器资源使用
docker stats

# 查看磁盘使用
docker system df

# 清理未使用的镜像和容器
docker system prune -f
```

---

## 故障排查

### 问题 1：服务无法启动

```bash
# 查看详细日志
docker-compose logs --no-color > logs.txt

# 检查端口占用
netstat -tlnp | grep 80
netstat -tlnp | grep 443

# 停止占用端口的服务
systemctl stop nginx  # 如果宿主机有 Nginx
```

### 问题 2：数据库连接失败

```bash
# 检查 MySQL 容器状态
docker-compose ps mysql
docker-compose logs mysql

# 进入 MySQL 容器检查
docker exec -it clear-books-mysql mysql -u root -p
```

### 问题 3：前端无法访问后端

```bash
# 检查后端服务
curl http://localhost:8080/api/health

# 检查 Nginx 配置
docker exec clear-books-web nginx -t

# 查看 Nginx 错误日志
docker-compose logs frontend
```

### 问题 4：SSL 证书错误

```bash
# 检查证书文件
ls -la nginx/ssl/

# 验证证书
openssl x509 -in nginx/ssl/cert.pem -text -noout

# 重新生成自签名证书
rm nginx/ssl/*.pem
openssl req -x509 -nodes -days 365 -newkey rsa:2048 \
    -keyout nginx/ssl/key.pem \
    -out nginx/ssl/cert.pem \
    -subj "/C=CN/ST=Beijing/L=Beijing/O=ClearBooks/CN=localhost"

docker-compose restart frontend
```

### 问题 5：内存不足

```bash
# 查看内存使用
free -h

# 添加 Swap 分区（如果服务器没有）
fallocate -l 2G /swapfile
chmod 600 /swapfile
mkswap /swapfile
swapon /swapfile
echo '/swapfile none swap sw 0 0' >> /etc/fstab

# 限制容器内存使用（修改 docker-compose.yml）
# 在 services 下添加:
# deploy:
#   resources:
#     limits:
#       memory: 1G
```

---

## 安全建议

1. **修改默认密码**: 必须修改 `JWT_SECRET`、`MYSQL_ROOT_PASSWORD` 等默认密码
2. **限制 MySQL 访问**: MySQL 端口 3306 不应暴露在公网
3. **定期更新**: 定期执行 `docker-compose pull` 更新基础镜像
4. **防火墙配置**: 仅开放必要的端口（80、443、22）
5. **日志审计**: 定期检查 `/opt/clear-books/logs/` 中的日志文件

---

## 性能优化

### MySQL 性能调优

编辑 `mysql/conf.d/my.cnf`，根据服务器内存调整：

```ini
[mysqld]
# 根据服务器内存调整（例如 4G 内存服务器）
innodb_buffer_pool_size=2G
innodb_log_file_size=512M
max_connections=300
```

### 启用 Gzip 压缩

已在 `nginx/nginx.conf` 中配置，无需额外操作。

### 静态资源缓存

已在 Nginx 配置中设置静态资源缓存 1 年。

---

## 联系支持

如有问题，请通过以下方式联系：

- GitHub Issues: https://github.com/yourusername/clear-books/issues
- Email: your-email@example.com

---

**部署完成！** 🎉

现在你可以开始使用 Clear Books 记账系统了。
