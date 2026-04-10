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

- **Account Management**: Manage bank cards, WeChat Wallet, Alipay, and other accounts / 管理银行卡、微信钱包、支付宝等账户
- **Payment Methods**: Manage WeChat Pay, Alipay, and other payment methods / 管理微信支付、支付宝支付等支付方式
- **Category Management**: Custom income and expense categories with tree structure / 自定义收入和支出分类，支持两级树形结构
- **Transaction Records**: Record detailed information for each transaction / 记录每笔交易的详细信息
- **Quick Add**: Quickly add accounts/categories/payment methods during transaction entry / 录入流水时可快速添加账户、分类、支付方式
- **Statistics & Reports**: Weekly, monthly, quarterly, and yearly statistics with charts / 按周、月、季度、年统计收支情况，支持图表展示
- **Multi-user Support**: Support for multiple user registration and login / 支持多用户注册和登录

### Tech Stack

#### Backend
- JDK 8+ (Compatible with JDK 17)
- Spring Boot 2.7.x (Compatible with Spring Boot 3.x)
- Spring Security + JWT
- Spring Data JPA
- H2 Database (Embedded, no additional installation required)

#### Frontend
- Vue 3 + Composition API
- Element Plus UI Component Library
- ECharts Chart Library
- Pinia State Management
- Vite Build Tool

### Quick Start

#### 1. Clone Project
```bash
git clone <repository-url>
cd clear-books
```

#### 2. Start Backend Service
```bash
cd backend
# Windows
mvnw spring-boot:run
# Linux/Mac
./mvnw spring-boot:run
```
Or if Maven is installed:
```bash
mvn spring-boot:run
```
Backend runs at http://localhost:8080

H2 Database Console: http://localhost:8080/h2-console
- JDBC URL: `jdbc:h2:file:./data/accountingdb`
- Username: `sa`
- Password: (empty)

#### 3. Start Frontend Service
```bash
cd frontend
npm install
npm run dev
```
Frontend runs at http://localhost:5173

#### 4. Access Application
Open http://localhost:5173 in browser. Register an account on first use.

### Project Structure
```
clear-books/
├── backend/                    # Backend project
│   ├── src/main/java/com/accounting/
│   │   ├── config/             # Configuration classes
│   │   ├── controller/         # Controller layer
│   │   ├── service/            # Service layer
│   │   ├── repository/         # Data access layer
│   │   ├── entity/             # Entity classes
│   │   ├── dto/                # DTOs
│   │   ├── security/           # JWT security config
│   │   └── exception/          # Exception handling
│   ├── src/main/resources/
│   │   ├── application.yml     # Configuration file
│   │   └── data.sql            # Initial data
│   └── pom.xml                 # Maven config
│
├── frontend/                   # Frontend project
│   ├── src/
│   │   ├── api/                # API interfaces
│   │   ├── components/         # Common components
│   │   ├── views/              # Page views
│   │   ├── router/             # Router config
│   │   ├── stores/             # Pinia state management
│   │   └── utils/              # Utility functions
│   ├── package.json
│   └── vite.config.js
│
├── LICENSE                     # MIT License
└── README.md                   # This file
```

### Default Ports
| Service | Port | URL |
|---------|------|-----|
| Frontend | 5173 | http://localhost:5173 |
| Backend | 8080 | http://localhost:8080 |
| H2 Console | 8080 | http://localhost:8080/h2-console |

### Configuration

#### Backend (backend/src/main/resources/application.yml)
```yaml
# Database configuration
spring.datasource.url: jdbc:h2:file:./data/accountingdb

# JWT configuration
jwt.secret: your-secret-key
jwt.expiration: 86400000  # 24 hours

# CORS configuration
cors.allowed-origins: http://localhost:5173
```

#### Frontend (frontend/vite.config.js)
```javascript
server: {
  port: 5173,
  proxy: {
    '/api': {
      target: 'http://localhost:8080',
      changeOrigin: true
    }
  }
}
```

### Development Notes
- **API Docs**: Visit http://localhost:8080/swagger-ui.html after starting backend
- **Data Backup**: H2 database file is at `backend/data/accountingdb.mv.db`

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
- JDK 8+ (兼容 JDK 17)
- Spring Boot 2.7.x (兼容 Spring Boot 3.x)
- Spring Security + JWT 认证
- Spring Data JPA
- H2 嵌入式数据库（无需额外安装）

#### 前端
- Vue 3 + Composition API
- Element Plus UI 组件库
- ECharts 图表库
- Pinia 状态管理
- Vite 构建工具

### 快速开始

#### 1. 克隆项目
```bash
git clone <repository-url>
cd clear-books
```

#### 2. 启动后端服务
```bash
cd backend
# Windows
mvnw spring-boot:run
# Linux/Mac
./mvnw spring-boot:run
```
或者使用已安装的 Maven：
```bash
mvn spring-boot:run
```
后端服务将运行在 http://localhost:8080

H2 数据库控制台：http://localhost:8080/h2-console
- JDBC URL: `jdbc:h2:file:./data/accountingdb`
- 用户名: `sa`
- 密码: （留空）

#### 3. 启动前端服务
```bash
cd frontend
npm install
npm run dev
```
前端服务将运行在 http://localhost:5173

#### 4. 访问应用
在浏览器中打开 http://localhost:5173，首次使用需要注册账号。

### 界面预览

系统采用左侧菜单栏 + 右侧内容区域的后台管理布局：

- **首页仪表盘**：展示本月收支概览、近6个月趋势图、支出分类饼图、账户余额
- **流水管理**：记录每日流水，支持筛选、分页、新增、编辑、删除
- **账户管理**：管理各类账户，支持余额调整，可设置账户颜色
- **分类管理**：自定义收入和支出分类，支持拖拽排序、调整层级
- **支付方式**：管理支付方式，支持拖拽排序
- **统计报表**：多维度图表分析，支持自定义时间范围

### 项目结构
```
clear-books/
├── backend/                    # 后端项目
│   ├── src/main/java/com/accounting/
│   │   ├── config/             # 配置类
│   │   ├── controller/         # 控制器层
│   │   ├── service/            # 业务层
│   │   ├── repository/         # 数据访问层
│   │   ├── entity/             # 实体类
│   │   ├── dto/                # 数据传输对象
│   │   ├── security/           # JWT安全配置
│   │   └── exception/          # 异常处理
│   ├── src/main/resources/
│   │   ├── application.yml     # 配置文件
│   │   └── data.sql            # 初始化数据
│   └── pom.xml                 # Maven配置
│
├── frontend/                   # 前端项目
│   ├── src/
│   │   ├── api/                # API接口
│   │   ├── components/         # 公共组件
│   │   ├── views/              # 页面视图
│   │   ├── router/             # 路由配置
│   │   ├── stores/             # Pinia状态管理
│   │   └── utils/              # 工具函数
│   ├── package.json
│   └── vite.config.js
│
├── LICENSE                     # MIT许可证
└── README.md                   # 本文件
```

### 默认端口

| 服务 | 端口 | 地址 |
|------|------|------|
| 前端 | 5173 | http://localhost:5173 |
| 后端 | 8080 | http://localhost:8080 |
| H2 Console | 8080 | http://localhost:8080/h2-console |

### 配置说明

#### 后端配置 (backend/src/main/resources/application.yml)
```yaml
# 数据库配置
spring.datasource.url: jdbc:h2:file:./data/accountingdb

# JWT配置
jwt.secret: your-secret-key
jwt.expiration: 86400000  # 24小时

# CORS配置
cors.allowed-origins: http://localhost:5173
```

#### 前端配置 (frontend/vite.config.js)
```javascript
server: {
  port: 5173,
  proxy: {
    '/api': {
      target: 'http://localhost:8080',
      changeOrigin: true
    }
  }
}
```

### 开发说明

- **API文档**：启动后端服务后，访问 http://localhost:8080/swagger-ui.html 查看API文档
- **数据备份**：H2数据库文件存储在 `backend/data/accountingdb.mv.db`，可直接备份该文件

---

## 🐳 Docker 部署（生产环境）

### 快速部署

```bash
# 1. 克隆项目
git clone <repository-url>
cd clear-books

# 2. 配置环境变量
cp .env.example .env
vim .env  # 修改密码和配置

# 3. 运行部署脚本
sudo ./deploy.sh
```

部署完成后访问 `https://服务器IP` 即可。

### 主要脚本

| 脚本 | 用途 |
|------|------|
| `deploy.sh` | 首次部署，自动安装 Docker、生成 SSL 证书、启动服务 |
| `update.sh` | 更新应用，自动备份、拉取代码、重建容器 |
| `backup.sh` | 备份数据，支持本地备份和腾讯云 COS 上传 |

### 手动管理

```bash
# 查看日志
docker-compose logs -f

# 重启服务
docker-compose restart

# 停止服务
docker-compose down

# 更新（代码变更后）
docker-compose build --no-cache
docker-compose up -d
```

---

### 后续可扩展功能

- [ ] 预算管理：设置月度/年度预算，超支提醒
- [ ] 账单导入：支持微信、支付宝账单CSV导入
- [ ] 数据导出：导出Excel/JSON
- [ ] 多币种：支持外币记账和汇率转换
- [ ] 记账提醒：定期记账提醒
- [ ] 数据同步：云端同步备份

---

## License / 许可证

[MIT License](LICENSE)

Copyright (c) 2026 Clear Books Contributors

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
