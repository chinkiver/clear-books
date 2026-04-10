# Personal Accounting System / 个人记账系统

<div align="center">

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-2.7.x-green.svg)](https://spring.io/projects/spring-boot)
[![Vue 3](https://img.shields.io/badge/Vue-3.x-4FC08D.svg)](https://vuejs.org/)
[![JDK](https://img.shields.io/badge/JDK-17%2B-orange.svg)](https://www.oracle.com/java/)

A personal accounting system based on Spring Boot + Vue3 + MySQL, helping you record daily expenses and income, with statistics by week, month, quarter, and year.

基于 Spring Boot + Vue3 + MySQL 的个人记账系统，帮助您记录每日的开销和收入，按周、月、季度、年来统计整体收支情况。

[English](#english) | [中文](#中文)

</div>

---

<a name="english"></a>
## 🇬🇧 English

### Features

- **Account Management**: Manage bank cards, WeChat Wallet, Alipay, and other accounts
- **Payment Methods**: Manage WeChat Pay, Alipay, and other payment methods
- **Category Management**: Custom income and expense categories with tree structure
- **Transaction Records**: Record detailed information for each transaction
- **Quick Add**: Quickly add accounts/categories/payment methods during transaction entry
- **Statistics & Reports**: Weekly, monthly, quarterly, and yearly statistics with charts
- **Multi-user Support**: Support for multiple user registration and login

### Tech Stack

#### Backend
- JDK 17+
- Spring Boot 2.7.x
- Spring Security + JWT
- Spring Data JPA
- MySQL 8.0

#### Frontend
- Vue 3 + Composition API
- Element Plus UI Component Library
- ECharts Chart Library
- Pinia State Management
- Vite Build Tool

### Quick Start (Development)

```bash
# 1. Clone Project
git clone <repository-url>
cd clear-books

# 2. Configure Environment
cp .env.example .env
# Edit .env with your MySQL credentials

# 3. Start Backend
cd backend
./mvnw spring-boot:run
# or: mvn spring-boot:run

# 4. Start Frontend
cd ../frontend
npm install
npm run dev
```

Access http://localhost:5173

### Production Deployment

```bash
# 1. Build (Local)
./scripts/local/build.sh        # Linux/Mac
.\scripts\local\build.ps1      # Windows

# 2. Upload to Server
scp backend/target/personal-accounting-*.jar user@server:/opt/clear-books/
scp scripts/server/start.sh user@server:/opt/clear-books/
scp scripts/server/stop.sh user@server:/opt/clear-books/
scp scripts/server/backup.sh user@server:/opt/clear-books/
scp .env user@server:/opt/clear-books/

# 3. Start (Server)
ssh user@server "cd /opt/clear-books && chmod +x start.sh stop.sh backup.sh && ./start.sh"

# 4. Setup Daily Backup (2:00 AM)
ssh user@server "crontab -l | { cat; echo \"0 2 * * * /opt/clear-books/backup.sh >> /opt/clear-books/logs/backup.log 2>&1\"; } | crontab -"
```

---

<a name="中文"></a>
## 🇨🇳 中文

### 功能特性

- **账户管理**：管理银行卡、微信钱包、支付宝等账户，支持余额调整
- **支付方式管理**：管理微信支付、支付宝支付等支付方式，支持拖拽排序
- **分类管理**：自定义收入和支出分类，支持两级树形结构，支持拖拽排序
- **每日流水**：记录每笔交易的详细信息，支持收入、支出、转账三种类型
- **快速添加**：录入流水时可直接在弹窗中快速添加账户、分类、支付方式
- **统计报表**：按周、月、季度、年统计收支情况，支持图表展示
- **多用户支持**：支持多用户注册和登录，数据相互隔离

### 技术架构

#### 后端
- JDK 17 (生产环境推荐)
- Spring Boot 2.7.x
- Spring Security + JWT 认证
- Spring Data JPA
- MySQL 8.0

#### 前端
- Vue 3 + Composition API
- Element Plus UI 组件库
- ECharts 图表库
- Pinia 状态管理
- Vite 构建工具

### 开发环境快速开始

```bash
# 1. 克隆项目
git clone <repository-url>
cd clear-books

# 2. 配置环境变量
cp .env.example .env
# 编辑 .env 填入数据库信息

# 3. 启动后端服务
cd backend
# Windows
mvnw spring-boot:run
# Linux/Mac
./mvnw spring-boot:run

# 或使用已安装的 Maven
mvn spring-boot:run

# 4. 启动前端服务
cd ../frontend
npm install
npm run dev
```

在浏览器中打开 http://localhost:5173

---

## 🚀 生产环境部署

### 部署流程概览

```
本地构建 → 上传服务器 → 启动服务
```

### 1. 本地构建

#### Windows (PowerShell)

```powershell
.\scripts\local\build.ps1
```

#### Linux / macOS

```bash
./scripts/local/build.sh
```

构建完成后，JAR 文件位于 `backend/target/personal-accounting-*.jar`

### 2. 准备部署文件

将以下文件上传到服务器的任意目录（例如 `/opt/clear-books`）：

| 文件 | 来源 | 说明 |
|------|------|------|
| `personal-accounting-*.jar` | `backend/target/` | 应用主程序 |
| `start.sh` | `scripts/server/` | 启动脚本 |
| `stop.sh` | `scripts/server/` | 停止脚本 |
| `backup.sh` | `scripts/server/` | 数据库备份脚本 |
| `.env` | 项目根目录 | 数据库配置 |

#### .env 配置文件示例

```bash
# 数据库配置（由系统管理提供）
MYSQL_URL=jdbc:mysql://localhost:3306/accounting?useSSL=false&serverTimezone=Asia/Shanghai
MYSQL_USER=your_username
MYSQL_PASSWORD=your_password

# JWT 密钥（建议使用 openssl rand -hex 32 生成）
JWT_SECRET=your_random_secret_key_here
```

### 3. 服务器启动应用

```bash
# 进入部署目录（你上传文件的目录）
cd /opt/clear-books

# 添加执行权限（首次）
chmod +x start.sh stop.sh

# 启动服务
./start.sh

# 停止服务
./stop.sh

# 重启服务
./stop.sh && ./start.sh
```

访问 `http://服务器IP:6173` 即可使用。

---

### 脚本说明

| 脚本 | 位置 | 运行环境 | 用途 |
|------|------|---------|------|
| `build.sh` / `build.ps1` | `scripts/local/` | 本地 | 编译前端 + 打包 JAR |
| `start.sh` | `scripts/server/` | 服务器 | 启动应用 |
| `stop.sh` | `scripts/server/` | 服务器 | 停止应用 |
| `backup.sh` | `scripts/server/` | 服务器 | MySQL 数据库备份 |

---

### 日常更新/升级流程

```bash
# 1. 本地重新构建
./scripts/local/build.sh

# 2. 上传新的 JAR 到服务器（覆盖或保留旧版本）
scp backend/target/personal-accounting-*.jar user@server:/opt/clear-books/

# 3. 服务器上重启
ssh user@server "cd /opt/clear-books && ./stop.sh && ./start.sh"
```

---

### 服务器管理命令

```bash
cd /opt/clear-books    # 进入部署目录

# 启动应用
./start.sh

# 停止应用
./stop.sh

# 查看运行状态
ps aux | grep personal-accounting

# 查看实时日志
tail -f logs/app.log

# 手动备份数据库
./backup.sh
```

#### 配置定时自动备份

设置每天凌晨2点自动备份数据库（保留最近7天）：

```bash
# 编辑 crontab
crontab -e

# 添加以下行
0 2 * * * /opt/clear-books/backup.sh >> /opt/clear-books/logs/backup.log 2>&1

# 验证设置
crontab -l
```

---

### 目录结构

**本地开发机：**
```
clear-books/
├── backend/
│   └── target/
│       └── personal-accounting-*.jar   # ← 生成的JAR
├── frontend/
├── scripts/
│   ├── local/
│   │   ├── build.sh                    # Linux/Mac 构建
│   │   └── build.ps1                   # Windows 构建
│   └── server/
│       ├── start.sh                    # 服务器启动脚本
│       └── stop.sh                     # 服务器停止脚本
├── .env                                # 环境变量配置
└── .env.example                        # 配置模板
```

**服务器：**
```
/opt/clear-books/                      # 部署目录（可自定义）
├── personal-accounting-1.0.0.jar      # JAR 文件
├── start.sh                           # 启动脚本
├── stop.sh                            # 停止脚本
├── backup.sh                          # 数据库备份脚本
├── .env                               # 环境变量配置
├── logs/                              # 日志目录（自动创建，保留7天）
│   ├── app.log                        # 当前日志
│   └── app.2024-01-15.log             # 历史日志（自动清理）
├── backups/                           # 备份目录（自动创建，保留7天）
│   └── backup_accounting_20240115_020000.sql.gz
└── app.pid                            # 进程ID文件（运行时生成）
```

---

### 手动构建（不使用脚本）

```bash
# 1. 构建前端
cd frontend
npm install
npm run build

# 2. 复制到后端 static
cp -r dist/* ../backend/src/main/resources/static/

# 3. 构建 JAR
cd ../backend
mvn clean package -DskipTests
```

---

### 修改版本号

编辑 `backend/pom.xml`：

```xml
<version>1.0.0</version>    <!-- 修改这里 -->
```

然后重新构建即可生成对应版本的 JAR 文件。

---

### 数据库管理

```bash
# 备份数据库
mysqldump -u root -p accounting > backup.sql

# 恢复数据库
mysql -u root -p accounting < backup.sql
```

---

### 常见问题排查

#### 1. 更新系统图标/Logo 报错 "Data truncation: Data too long"

**问题原因**：`system_settings` 表的 `setting_value` 字段默认为 VARCHAR(255)，无法存储 Base64 编码的图片数据。

**解决方法**：

```bash
# 在服务器上执行修复脚本
cd /opt/clear-books
chmod +x scripts/server/fix-setting-value.sh
./scripts/server/fix-setting-value.sh
```

或者手动执行 SQL：

```sql
ALTER TABLE system_settings MODIFY COLUMN setting_value TEXT;
```

**预防措施**：
- 图标文件建议不超过 50KB
- Logo 文件建议不超过 100KB
- 使用 PNG 或 SVG 格式，避免 BMP 等大体积格式

---

## License / 许可证

[MIT License](LICENSE)

Copyright (c) 2026 Clear Books Contributors
