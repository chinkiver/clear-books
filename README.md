# Personal Accounting System / 个人记账系统

<div align="center">

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-2.7.x-green.svg)](https://spring.io/projects/spring-boot)
[![Vue 3](https://img.shields.io/badge/Vue-3.x-4FC08D.svg)](https://vuejs.org/)
[![JDK](https://img.shields.io/badge/JDK-8%2B-orange.svg)](https://www.oracle.com/java/)

A personal accounting system based on Spring Boot + Vue3 + H2, helping you record daily expenses and income, with statistics by week, month, quarter, and year.

基于 Spring Boot + Vue3 + H2 的个人记账系统，帮助您记录每日的开销和收入，按周、月、季度、年来统计整体收支情况。

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
- JDK 8+ (Compatible with JDK 17)
- Spring Boot 2.7.x
- Spring Security + JWT
- Spring Data JPA
- H2 Database (Development)
- MySQL 8.0 (Production)

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

# 2. Start Backend
cd backend
./mvnw spring-boot:run
# or: mvn spring-boot:run

# 3. Start Frontend
cd ../frontend
npm install
npm run dev
```

Access http://localhost:5173

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
- H2 嵌入式数据库（开发环境）
- MySQL 8.0（生产环境）

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

# 2. 启动后端服务
cd backend
# Windows
mvnw spring-boot:run
# Linux/Mac
./mvnw spring-boot:run

# 或使用已安装的 Maven
mvn spring-boot:run

# 3. 启动前端服务
cd ../frontend
npm install
npm run dev
```

在浏览器中打开 http://localhost:5173

---

## 🚀 生产环境部署

### 部署流程

```
本地打包 → 上传服务器 → 运行
```

### 1. 本地打包（开发机）

```bash
# 进入项目目录
cd clear-books

# 配置生产环境变量
cp .env.example .env
vim .env  # 修改 MySQL 密码、JWT 密钥

# 构建 JAR 包（前端+后端合并）
./build.sh
```

构建完成后，JAR 文件位于 `backend/target/personal-accounting-1.0.0.jar`

### 2. 上传到服务器

```bash
# SSH 到服务器，创建目录
ssh root@服务器IP "mkdir -p /opt/clear-books"

# 上传 JAR 包
scp backend/target/personal-accounting-1.0.0.jar root@服务器IP:/opt/clear-books/

# 上传 .env 配置文件
scp .env root@服务器IP:/opt/clear-books/

# 上传脚本
scp start.sh stop.sh install.sh root@服务器IP:/opt/clear-books/
```

### 3. 服务器配置（仅首次）

```bash
# SSH 登录服务器
ssh root@服务器IP

cd /opt/clear-books

# 安装环境（JDK、MySQL）- 只需执行一次
chmod +x install.sh
./install.sh

# 启动服务
chmod +x start.sh stop.sh
./start.sh
```

访问 `http://服务器IP:8080` 即可使用。

---

### 脚本说明

| 脚本 | 运行位置 | 用途 |
|------|---------|------|
| `build.sh` | 本地开发机 | 构建前端 + 复制到后端 + 打包 JAR |
| `install.sh` | 服务器（仅一次）| 安装 JDK、MySQL、创建数据库 |
| `start.sh` | 服务器 | 启动应用 |
| `stop.sh` | 服务器 | 停止应用 |
| `update.sh` | 本地开发机 | 完整更新流程 |
| `backup.sh` | 服务器 | 备份 MySQL 数据 |

---

### 日常更新流程

```bash
# 方式 1：全自动更新
./update.sh

# 方式 2：手动分步
# 1. 本地重新构建
./build.sh

# 2. 上传到服务器
scp backend/target/personal-accounting-1.0.0.jar root@服务器IP:/opt/clear-books/

# 3. SSH 到服务器重启
ssh root@服务器IP "cd /opt/clear-books && ./stop.sh && ./start.sh"
```

---

### 服务器管理命令

```bash
# SSH 登录服务器
cd /opt/clear-books

# 启动应用
./start.sh

# 停止应用
./stop.sh

# 查看日志
tail -f logs/app.log

# 查看运行状态
ps aux | grep personal-accounting
```

---

### 目录结构

**本地开发机：**
```
clear-books/
├── backend/
│   └── target/
│       └── personal-accounting-1.0.0.jar   # ← 生成的JAR
├── frontend/
├── build.sh                                 # 本地构建
└── update.sh                                # 更新脚本
```

**服务器：**
```
/opt/clear-books/
├── personal-accounting-1.0.0.jar   # ← JAR 包
├── .env                            # 环境变量
├── start.sh                        # 启动脚本
├── stop.sh                         # 停止脚本
├── logs/
│   └── app.log                     # 应用日志
└── backups/                        # 备份目录
```

---

### 手动构建（不使用脚本）

```bash
# 1. 构建前端
cd frontend
npm install
npm run build

# 2. 复制到后端
cp -r dist/* ../backend/src/main/resources/static/

# 3. 构建 JAR
cd ../backend
mvn clean package -DskipTests
```

---

### 数据库管理

```bash
# 备份数据库
mysqldump -u root -p accounting > backup.sql

# 恢复数据库
mysql -u root -p accounting < backup.sql
```

---

## License / 许可证

[MIT License](LICENSE)

Copyright (c) 2026 Clear Books Contributors
