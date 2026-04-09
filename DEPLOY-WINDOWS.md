# Clear Books Windows 本地 Docker 验证指南

本文档介绍如何在 Windows 11 上使用 Docker Desktop 本地验证部署方案。

## 前置要求

- Windows 11
- Docker Desktop for Windows（已安装并运行）
- Git for Windows
- PowerShell 或命令提示符

## 快速开始

### 1. 克隆项目

```powershell
# 打开 PowerShell
cd D:\Projects  # 或你喜欢的目录
git clone https://github.com/yourusername/clear-books.git
cd clear-books
```

### 2. 配置环境变量

```powershell
# 复制环境变量模板
copy .env.example .env

# 使用记事本或 VS Code 编辑
notepad .env
```

最小配置（使用默认值即可）：
```bash
# MySQL 密码
MYSQL_ROOT_PASSWORD=root123456
MYSQL_PASSWORD=db123456

# JWT 密钥（至少 32 位）
JWT_SECRET=your-local-test-secret-key-32chars

# 允许本地访问
CORS_ORIGINS=http://localhost,http://localhost:8080
```

### 3. 构建并启动（Windows 专用配置）

```powershell
# 使用 Windows 专用配置
docker-compose -f docker-compose.windows.yml up -d --build
```

### 4. 验证部署

```powershell
# 查看运行状态
docker-compose -f docker-compose.windows.yml ps

# 查看日志
docker-compose -f docker-compose.windows.yml logs -f
```

访问 http://localhost 即可使用系统。

## Windows 注意事项

### 端口冲突检查

如果 80 端口被占用（如 IIS、WAMP、XAMPP）：

```powershell
# 检查端口占用
netstat -ano | findstr :80

# 修改 docker-compose.windows.yml，更改端口映射
# 例如改为 8080:80
```

### 文件路径问题

Windows 使用反斜杠 `\`，但 Docker 支持正斜杠 `/`：

```yaml
# 正确 - 使用正斜杠
volumes:
  - ./mysql/conf.d:/etc/mysql/conf.d:ro

# 避免 - Windows 反斜杠
volumes:
  - .\mysql\conf.d:/etc/mysql/conf.d:ro
```

### 权限问题

Windows 下 Docker 挂载卷可能遇到权限问题，解决方案：

1. **使用 Docker 命名卷**（推荐）
   ```yaml
   volumes:
     - mysql_data:/var/lib/mysql  # 使用命名卷，非绑定挂载
   ```

2. **共享驱动器设置**
   - 打开 Docker Desktop
   - Settings → Resources → File sharing
   - 确保项目所在磁盘已共享

### 内存配置

Docker Desktop 默认内存可能不足：

1. 打开 Docker Desktop
2. Settings → Resources → Advanced
3. 建议分配至少 4GB 内存
4. 点击 Apply & Restart

## 常用命令

### 查看日志

```powershell
# 所有服务日志
docker-compose -f docker-compose.windows.yml logs -f

# 特定服务
docker-compose -f docker-compose.windows.yml logs -f backend
docker-compose -f docker-compose.windows.yml logs -f mysql
```

### 重启服务

```powershell
# 重启所有
docker-compose -f docker-compose.windows.yml restart

# 重启单个
docker-compose -f docker-compose.windows.yml restart backend
```

### 进入容器

```powershell
# 进入 MySQL 容器
docker exec -it clear-books-mysql mysql -u root -p

# 进入后端容器
docker exec -it clear-books-api sh

# 进入前端容器
docker exec -it clear-books-web sh
```

### 停止和清理

```powershell
# 停止服务
docker-compose -f docker-compose.windows.yml down

# 停止并删除数据卷（彻底清理）
docker-compose -f docker-compose.windows.yml down -v

# 删除所有未使用的镜像
docker system prune -f
```

## 数据持久化

Windows 下的数据存储：

- **MySQL 数据**: 存储在 Docker 命名卷 `clear-books_mysql_data`
- **应用日志**: 存储在 Docker 命名卷 `clear-books_app_logs`

查看卷位置：
```powershell
docker volume ls
docker volume inspect clear-books_mysql_data
```

## 故障排查

### 问题 1: 端口被占用

错误: `bind: address already in use`

解决:
```powershell
# 修改 docker-compose.windows.yml
ports:
  - "8080:80"  # 使用 8080 代替 80

# 然后访问 http://localhost:8080
```

### 问题 2: 内存不足

错误: `container exited with code 137`

解决:
1. 增加 Docker Desktop 内存分配（建议 4GB+）
2. 或减少 MySQL 内存配置:
   ```ini
   # mysql/conf.d/my.cnf
   innodb_buffer_pool_size=128M
   ```

### 问题 3: 构建失败

错误: `failed to solve: rpc error`

解决:
```powershell
# 清理构建缓存
docker builder prune -f

# 重新构建
docker-compose -f docker-compose.windows.yml build --no-cache
```

### 问题 4: 无法访问 localhost

检查:
```powershell
# 1. 确认容器运行
docker ps

# 2. 查看端口映射
docker port clear-books-web

# 3. 检查防火墙
# Windows 防火墙可能需要允许 Docker 端口
```

### 问题 5: 前端无法连接后端

检查 Nginx 配置:
```powershell
# 查看前端容器日志
docker-compose -f docker-compose.windows.yml logs frontend

# 测试后端是否可访问
docker exec clear-books-web wget -qO- http://backend:8080/api/health
```

## 与 Linux 部署的区别

| 项目 | Windows 本地验证 | Linux 生产部署 |
|------|------------------|----------------|
| 配置文件 | `docker-compose.windows.yml` | `docker-compose.yml` |
| SSL/HTTPS | ❌ 禁用 | ✅ 启用 |
| 端口 | 可能需改为 8080 | 标准 80/443 |
| 数据卷 | Docker 命名卷 | 绑定挂载 |
| 备份脚本 | ❌ 不适用 | ✅ 可用 |
| 性能 | 适合验证 | 适合生产 |

## 验证完成后

Windows 本地验证通过后，部署到腾讯云服务器：

```powershell
# 1. 推送代码到 GitHub
git add .
git commit -m "ready for production"
git push origin main

# 2. 在服务器上执行（参考 DEPLOY.md）
# ssh root@你的服务器IP
# git clone ...
# ./deploy.sh
```

## 提示

- Windows 验证主要用于**功能测试**，性能不代表生产环境
- 数据存储在 Docker 卷中，删除容器会保留数据
- 如需彻底清理数据，使用 `docker-compose down -v`
