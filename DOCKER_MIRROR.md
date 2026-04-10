# Docker 国内镜像源配置指南

由于 Docker Hub 在国内访问速度较慢，建议使用国内镜像源加速下载。

## 方法一：配置 Docker Desktop（Windows/Mac）

1. 打开 Docker Desktop
2. 点击 **Settings**（齿轮图标）
3. 选择 **Docker Engine**
4. 在配置中添加 `registry-mirrors`：

```json
{
  "registry-mirrors": [
    "https://docker.mirrors.ustc.edu.cn",
    "https://hub-mirror.c.163.com",
    "https://mirror.baidubce.com",
    "https://docker.m.daocloud.io"
  ]
}
```

5. 点击 **Apply & Restart**

## 方法二：配置 Linux 服务器

### 创建/编辑配置文件

```bash
# 创建配置目录（如果不存在）
sudo mkdir -p /etc/docker

# 编辑配置文件
sudo vim /etc/docker/daemon.json
```

### 添加以下内容

```json
{
  "registry-mirrors": [
    "https://docker.mirrors.ustc.edu.cn",
    "https://hub-mirror.c.163.com",
    "https://mirror.baidubce.com",
    "https://docker.m.daocloud.io"
  ]
}
```

### 重启 Docker 服务

```bash
# 重载配置
sudo systemctl daemon-reload

# 重启 Docker
sudo systemctl restart docker

# 验证配置是否生效
docker info | grep -A 10 "Registry Mirrors"
```

## 可用的国内镜像源

| 镜像源 | 地址 | 备注 |
|--------|------|------|
| 中科大镜像 | `https://docker.mirrors.ustc.edu.cn` | 推荐，稳定快速 |
| 网易云镜像 | `https://hub-mirror.c.163.com` | 推荐，速度快 |
| 百度云镜像 | `https://mirror.baidubce.com` | 百度出品 |
| DaoCloud | `https://docker.m.daocloud.io` | 国内可用 |
| 阿里云镜像 | `https://<你的ID>.mirror.aliyuncs.com` | 需要注册阿里云账号 |

## 阿里云镜像源配置（推荐，最稳定）

1. 登录 [阿里云控制台](https://cr.console.aliyun.com/)
2. 左侧菜单选择 **镜像工具** → **镜像加速器**
3. 复制你的专属加速器地址（格式如 `https://xxxxxx.mirror.aliyuncs.com`）
4. 添加到配置中

## 验证镜像源是否生效

```bash
# 查看 Docker 信息
docker info

# 应该能看到类似输出：
# Registry Mirrors:
#  https://docker.mirrors.ustc.edu.cn/
#  https://hub-mirror.c.163.com/

# 测试拉取速度
time docker pull mysql:8.0
```

## 常见问题

### 配置后不生效
- 确保 JSON 格式正确（注意逗号、引号）
- 必须重启 Docker 服务
- 检查配置文件路径是否正确

### 某些镜像无法拉取
- 国内镜像源可能同步有延迟
- 可以尝试多个镜像源（配置数组中的多个地址）
- 紧急情况下可以暂时删除配置使用官方源

### 公司网络有代理
如果公司网络需要代理，还需要配置：
```json
{
  "registry-mirrors": ["https://docker.mirrors.ustc.edu.cn"],
  "proxies": {
    "default": {
      "httpProxy": "http://proxy.company.com:8080",
      "httpsProxy": "http://proxy.company.com:8080"
    }
  }
}
```
