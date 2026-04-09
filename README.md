# 个人记账系统

一个基于 Spring Boot + Vue3 + H2 的个人记账系统，帮助您记录每日的开销和收入，按周、月、季度、年来统计整体收支情况。

## 功能特性

- **账户管理**：管理银行卡、微信钱包、支付宝等账户
- **支付方式管理**：管理微信支付、支付宝支付、京东支付等支付方式
- **分类管理**：自定义收入和支出分类
- **每日流水**：记录每笔交易的详细信息
- **统计报表**：按周、月、季度、年统计收支情况，支持图表展示
- **多用户支持**：支持多用户注册和登录

## 技术架构

### 后端
- JDK 8+ (兼容JDK 17)
- Spring Boot 2.7.x (兼容Spring Boot 3.x)
- Spring Security + JWT
- Spring Data JPA
- H2 Database（嵌入式，无需额外安装）

### 前端
- Vue 3 + Composition API
- Element Plus UI组件库
- ECharts 图表库
- Pinia 状态管理
- Vite 构建工具

## 快速开始

### 1. 克隆项目

```bash
git clone <repository-url>
cd personal-accounting
```

### 2. 启动后端服务

```bash
cd backend
# Windows
mvnw spring-boot:run
# 或 Linux/Mac
./mvnw spring-boot:run
```

如果系统中已安装 Maven，也可以使用：
```bash
mvn spring-boot:run
```

后端服务将运行在 http://localhost:8080

H2数据库控制台：http://localhost:8080/h2-console
- JDBC URL: `jdbc:h2:file:./data/accountingdb`
- 用户名: `sa`
- 密码: （留空）

### 3. 启动前端服务

```bash
cd frontend
npm install
npm run dev
```

前端服务将运行在 http://localhost:5173

### 4. 访问应用

在浏览器中打开 http://localhost:5173

首次使用需要注册账号，然后即可开始使用记账功能。

## 项目结构

```
personal-accounting/
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
└── README.md
```

## 界面预览

系统采用左侧菜单栏 + 右侧内容区域的后台管理布局：

- **首页仪表盘**：展示本月收支概览、近6个月趋势图、支出分类饼图、账户余额
- **流水管理**：记录每日流水，支持筛选、分页、新增、编辑、删除
- **账户管理**：管理各类账户，支持余额调整
- **分类管理**：自定义收入和支出分类
- **支付方式**：管理支付方式
- **统计报表**：多维度图表分析，支持自定义时间范围

## 默认端口

| 服务 | 端口 | 地址 |
|------|------|------|
| 前端 | 5173 | http://localhost:5173 |
| 后端 | 8080 | http://localhost:8080 |
| H2 Console | 8080 | http://localhost:8080/h2-console |

## 配置说明

### 后端配置 (backend/src/main/resources/application.yml)

```yaml
# 数据库配置
spring.datasource.url: jdbc:h2:file:./data/accountingdb

# JWT配置
jwt.secret: your-secret-key
jwt.expiration: 86400000  # 24小时

# CORS配置
cors.allowed-origins: http://localhost:5173
```

### 前端配置 (frontend/vite.config.js)

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

## 开发说明

### API文档

启动后端服务后，访问 http://localhost:8080/swagger-ui.html 查看API文档。

### 数据备份

H2数据库文件存储在项目根目录的 `data/accountingdb.mv.db`，可以直接备份该文件。

## 后续可扩展功能

- [ ] 预算管理：设置月度/年度预算，超支提醒
- [ ] 账单导入：支持微信、支付宝账单CSV导入
- [ ] 数据导出：导出Excel/JSON
- [ ] 多币种：支持外币记账和汇率转换
- [ ] 记账提醒：定期记账提醒
- [ ] 数据同步：云端同步备份

## License

MIT
