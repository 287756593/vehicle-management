# 企业公务车辆管理系统

<div align="center">

![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.0-green)
![Vue 3](https://img.shields.io/badge/Vue-3.4-blue)
![MyBatis Plus](https://img.shields.io/badge/MyBatis%20Plus-3.5.5-orange)
![Element Plus](https://img.shields.io/badge/Element%20Plus-2.5-7B68EE)
![Vant](https://img.shields.io/badge/Vant-4.8-1989FA)
![JDK](https://img.shields.io/badge/JDK-17-brightgreen)
![MySQL](https://img.shields.io/badge/MySQL-8.0-blue)

一套面向企业单位的公务车辆综合管理平台，包含车辆档案管理、驾驶员管理、借还车登记、加油记录、维修工单、待办中心、统计报表等完整业务功能。

**管理员后台** 基于 Vue 3 + Element Plus 构建，适用于 PC 端管理操作。  
**驾驶员端** 基于 Vue 3 + Vant 4 构建，适用于移动端 App，随时随地完成借车、还车、加油登记等操作。

</div>

---

## 功能特性

### 核心业务模块

| 模块 | 说明 |
|------|------|
| **车辆档案管理** | 车辆 CRUD、尾号限行规则（周一至周五按尾号限行）、保险/年检到期提醒、手动放行、今日可用性判断 |
| **驾驶员管理** | 驾驶员 CRUD、部门关联、自动创建系统账号、登录排序 |
| **借还车登记** | 取车/还车全流程、必填照片记录（车况+里程）、异常上报、自动生成"需处理事项"、编辑日志全程留痕 |
| **加油记录管理** | 加油登记、现金加油审批流程、报销状态管理、年度油耗统计、加油量补录（7天内） |
| **维修工单** | 状态流转（草稿→待审批→已批准→维修中→待验收→已完成）、从借还车异常自动创建工单 |
| **待办中心** | 汇总全系统待办事项，支持按类型/优先级筛选，一键跳转到对应业务页面处理 |
| **统计报表** | 首页概览、半年用车报告（ECharts 图表）、出车趋势、驾驶员活跃度、燃油分析 |

### 角色权限体系

| 角色 | 说明 | 主要权限 |
|------|------|----------|
| `SUPER_ADMIN` | 超级管理员 | 全部权限，包括永久删除 |
| `OFFICE_ADMIN` | 综合办公室 | 车辆管理、驾驶员管理、借还车审批、维修审批、加油审批 |
| `DRIVER` | 驾驶人 | 借还车、加油登记、查看车辆动态 |
| `FINANCE` | 财务人员 | 查看统计、加油报销管理 |
| `DEPT_APPROVER` | 部门审批人 | 查看驾驶员和车辆信息 |

---

## 技术架构

```
┌─────────────────────────────────────────────────────────┐
│                     用户浏览器 / App                      │
├────────────┬──────────────────┬─────────────────────────┤
│            │                  │                         │
│   PC 端    │    移动端 App    │      移动端 App         │
│  (管理后台) │    (驾驶员端)    │      (驾驶员端)         │
│            │                  │                         │
│  Vue 3     │    Vue 3        │    Vue 3               │
│  Element Plus│    Vant 4      │    Vant 4             │
│  Pinia     │    Pinia        │    Pinia               │
│  ECharts   │                 │                         │
│  Vite      │    Vite         │    Vite                │
└─────┬──────┴────────┬────────┴────────────┬────────────┘
      │               │                     │
      │  http://前端地址  │  http://驾驶员端地址  │
      └───────────────┼─────────────────────┘
                      │
                      │ /api/
                      ▼
            ┌─────────────────────┐
            │  Spring Boot 3.2.0  │
            │       端口: 18231   │
            │                     │
            │  Spring Security    │
            │  JWT 认证            │
            │  MyBatis Plus 3.5.5 │
            │  文件上传/预览       │
            └──────────┬──────────┘
                       │
                       ▼
            ┌─────────────────────┐
            │     MySQL 8.0       │
            │   vehicle_management │
            └─────────────────────┘
```

### 技术栈详情

**后端**

- 框架：Spring Boot 3.2.0
- Java 版本：JDK 17
- 持久层：MyBatis Plus 3.5.5
- 安全：Spring Security + JWT (jjwt 0.12.3)
- 数据库：MySQL 8.0
- 工具库：Hutool 5.8.23、EasyExcel 3.3.4、Commons-IO 2.11.0
- 构建：Maven

**前端管理后台**

- 框架：Vue 3.4 (Composition API)
- 构建工具：Vite 5.4.21
- UI 组件库：Element Plus 2.5
- 状态管理：Pinia 2.1.7
- 路由：Vue Router 4.2.5
- 图表：ECharts 5.5

**驾驶员端移动端**

- 框架：Vue 3.4 (Composition API)
- 构建工具：Vite 5.4.21
- 移动端 UI：Vant 4.8
- 状态管理：Pinia 2.1
- 路由：Vue Router 4.2

---

## 系统截图

### 管理员后台

- 首页仪表盘：统计概览 + 待办提醒 + 最近借还车
- 车辆管理：车牌号搜索、状态筛选、今日放行、预算执行进度
- 借还车记录：主表/回收站切换、详情弹窗、编辑日志时间线、闭环处理
- 加油管理：审批流程、报销状态、年度油费卡片
- 维修工单：状态流转面板、详情抽屉
- 待办中心：类型汇总卡片、优先级标签、一键处理
- 半年报告：ECharts 多图表、趋势分析、PDF 导出打印

### 驾驶员端

- 首页工作台：可借数量、本月统计、当前借车状态
- 借还车双模式：借车必填照片+里程，还车异常记录
- 加油登记：借车记录关联、油品智能推荐、时间范围校验
- 车辆动态总览：状态分类卡片、超期徽章、搜索过滤
- 用车记录 / 加油记录：列表 + 详情弹窗 + 分页加载

---

## 项目结构

```
vehicle-management/
├── backend/                      # Spring Boot 后端
│   ├── src/main/java/com/company/vehicle/
│   │   ├── controller/           # REST API 控制器
│   │   ├── service/              # 业务逻辑层
│   │   ├── mapper/               # 数据访问层 (MyBatis Plus)
│   │   ├── entity/               # 数据库实体
│   │   ├── dto/                  # 数据传输对象
│   │   ├── config/               # 配置类 (CORS, MyBatis Plus)
│   │   └── security/             # JWT 认证过滤器
│   ├── src/main/resources/
│   │   ├── application.yml       # 配置文件
│   │   └── schema.sql            # 数据库初始化脚本
│   ├── pom.xml
│   ├── mvnw                      # Maven Wrapper
│   ├── uploads/                  # 上传文件存储目录
│   └── upload-previews/          # 缩略图目录
│
├── frontend/                     # Vue 3 管理后台前端
│   ├── src/
│   │   ├── views/                # 页面组件
│   │   ├── components/           # 公共组件
│   │   ├── api/                  # API 调用封装
│   │   ├── store/                # Pinia 状态管理
│   │   ├── router/               # 路由配置
│   │   ├── utils/                # 工具函数（请求封装、图片压缩）
│   │   └── styles/               # 全局样式（CSS 变量主题）
│   ├── vite.config.js
│   └── package.json
│
└── driver-app/                   # Vue 3 驾驶员端移动端
    ├── src/
    │   ├── views/                # 页面组件
    │   ├── api/                  # API 调用封装
    │   ├── store/                # Pinia 状态管理
    │   ├── router/               # 路由配置
    │   ├── utils/                # 工具函数（图片压缩）
    │   └── styles/               # 全局样式（深色模式支持）
    ├── vite.config.js
    └── package.json
```

---

## 快速部署

### 环境要求

| 环境 | 版本 |
|------|------|
| JDK | >= 17 |
| Node.js | >= 18 |
| npm | >= 9 |
| MySQL | >= 8.0 |
| Maven | >= 3.8（项目自带 mvnw 可不装） |

### 1. 克隆项目

```bash
git clone https://github.com/287756593/vehicle-management.git
cd vehicle-management
```

### 2. 初始化数据库

创建数据库（执行 `backend/src/main/resources/schema.sql`）：

```bash
mysql -u root -p < backend/src/main/resources/schema.sql
```

初始化数据包括：

- 12+ 张业务表（车辆、驾驶员、借还车、加油、维修工单等）
- 系统参数配置（保养阈值、燃油预算等）
- 陕西省内城市距离数据
- **默认管理员账号**

### 3. 修改配置

**后端配置** — 修改 `backend/src/main/resources/application.yml`：

```yaml
spring:
  datasource:
    url: jdbc:mysql://你的数据库IP:3306/vehicle_management
    username: 你的数据库用户名
    password: 你的数据库密码

jwt:
  secret: 替换为你的随机密钥（至少32字符）   # ⚠️ 生产环境必须修改！
  expiration: 1296000000                     # 15天
```

**前端代理配置** — 修改 `frontend/vite.config.js` 和 `driver-app/vite.config.js` 中的 `target` 为后端实际地址：

```javascript
proxy: {
  '/api': {
    target: 'http://你的服务器IP:18231',   # 修改为后端地址
    changeOrigin: true
  },
  '/uploads': {
    target: 'http://你的服务器IP:18231',
    changeOrigin: true
  }
}
```

### 4. 构建后端

```bash
cd backend

# 使用 Maven Wrapper（推荐，无需安装 Maven）
./mvnw clean package -DskipTests

# 或使用系统 Maven
mvn clean package -DskipTests
```

启动后端：

```bash
java -jar target/vehicle-management.jar
```

后端启动后会监听 `18231` 端口。

### 5. 构建前端

```bash
# 管理后台
cd frontend
npm install
npm run build       # 生产构建
npm run dev:hot     # 开发模式预览

# 驾驶员端
cd ../driver-app
npm install
npm run build       # 生产构建
npm run dev:hot     # 开发模式预览
```

构建产物分别在 `frontend/dist/` 和 `driver-app/dist/` 目录下。

### 6. Nginx 部署（生产环境）

**管理后台** `/etc/nginx/sites-available/vehicle-admin`：

```nginx
server {
    listen 80;
    server_name admin.your-domain.com;

    root /path/to/frontend/dist;
    index index.html;

    location / {
        try_files $uri $uri/ /index.html;
    }

    location /api/ {
        proxy_pass http://127.0.0.1:18231;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }

    location /uploads/ {
        proxy_pass http://127.0.0.1:18231;
    }
}
```

**驾驶员端** `/etc/nginx/sites-available/vehicle-driver`：

```nginx
server {
    listen 80;
    server_name driver.your-domain.com;

    root /path/to/driver-app/dist;
    index index.html;

    location / {
        try_files $uri $uri/ /index.html;
    }

    location /api/ {
        proxy_pass http://127.0.0.1:18231;
        proxy_set_header Host $host;
    }
}
```

```bash
# 启用站点
sudo ln -s /etc/nginx/sites-available/vehicle-admin /etc/nginx/sites-enabled/
sudo ln -s /etc/nginx/sites-available/vehicle-driver /etc/nginx/sites-enabled/

# 测试并重载 Nginx
sudo nginx -t && sudo systemctl reload nginx
```

### 7. Systemd 后端服务（推荐）

创建服务文件 `/etc/systemd/system/vehicle-backend.service`：

```ini
[Unit]
Description=Vehicle Management Backend
After=network.target mysql.service

[Service]
Type=simple
User=your-user
WorkingDirectory=/path/to/vehicle-management/backend
ExecStart=/usr/bin/java -jar target/vehicle-management.jar
Restart=always
RestartSec=10

[Install]
WantedBy=multi-user.target
```

```bash
sudo systemctl daemon-reload
sudo systemctl enable vehicle-backend
sudo systemctl start vehicle-backend
```

---

## 默认账号

| 用户名 | 密码 | 角色 | 说明 |
|--------|------|------|------|
| `admin` | `admin123` | SUPER_ADMIN | 超级管理员，拥有全部权限 |
| `office_admin` | `admin123` | OFFICE_ADMIN | 综合办公室管理员 |
| `dept_approver` | `admin123` | DEPT_APPROVER | 部门审批人 |
| `driver1` | `admin123` | DRIVER | 驾驶员账号（张三） |
| `driver2` | `admin123` | DRIVER | 驾驶员账号（李四） |
| `finance` | `admin123` | FINANCE | 财务人员 |

> ⚠️ 生产环境部署前请务必修改默认密码和 JWT 密钥。

---

## API 端口速查

| 服务 | 端口 | 说明 |
|------|------|------|
| 后端 API | `18231` | REST API，所有业务接口 |
| 管理后台（开发） | `3592` | Vite 开发服务器 |
| 驾驶员端（开发） | `3593` | Vite 开发服务器 |

---

## 数据库表结构

系统使用 12+ 张 MySQL 表，完整结构见 `backend/src/main/resources/schema.sql`：

| 表名 | 说明 |
|------|------|
| `sys_user` | 系统用户表 |
| `sys_dept` | 部门表 |
| `sys_config` | 系统参数配置表 |
| `vehicle` | 车辆档案表 |
| `driver` | 驾驶人信息表 |
| `vehicle_borrow_record` | 借还车记录表 |
| `vehicle_borrow_record_edit_log` | 借还车编辑日志表 |
| `fuel_record` | 加油记录表 |
| `maintenance_work_order` | 维修工单表（新版） |
| `maintenance_work_order_attachment` | 维修工单附件表 |
| `insurance_record` | 保险记录表 |
| `inspection_record` | 年检记录表 |
| `violation_record` | 违章记录表 |
| `system_reminder` | 系统提醒记录表 |
| `operation_log` | 操作日志表 |
| `city_distance` | 城市间距离配置表 |

---

## 核心业务逻辑

### 尾号限行规则

| 星期 | 限行尾号 |
|------|----------|
| 周一 | 1 和 6 |
| 周二 | 2 和 7 |
| 周三 | 3 和 8 |
| 周四 | 4 和 9 |
| 周五 | 5 和 0 |
| 周末 | 不限行 |

管理员可手动为特定车辆在当天放行（突破限行限制）。

### 借还车流程

```
取车 → 验证（驾驶员状态 / 车辆状态 / 限行） → 上传照片（车况+里程）
  ↓
使用中 (IN_USE)
  ↓
还车 → 验证里程 → 上传照片（停车+仪表）→ 自动生成"需处理事项"
  ↓
待闭环 (FOLLOW_UP_PENDING)
  ↓
闭环处理 → 恢复正常 或 转入维修
```

关键约束：

- 还车里程必须 >= 取车里程
- 取车/还车必传照片（车况照 + 里程照）
- 每次修改记录生成编辑日志快照
- 使用 MySQL `GET_LOCK` 防止同一车辆/驾驶员并发操作

### 维修工单状态流转

```
DRAFT ──提交──→ PENDING_APPROVAL ──审批通过──→ APPROVED
                      │                            │
                  审批驳回                        开始维修
                      │                            ↓
                      └──→ REJECTED            IN_REPAIR
                                                 │
                                              完工
                                                 ↓
                                         WAIT_ACCEPTANCE
                                                 │
                                              验收
                                                 ↓
                                            COMPLETED

任意状态（除 COMPLETED/CANCELED）───取消──→ CANCELED
```

---

## 安全建议

部署生产环境前请检查以下项目：

- [ ] **JWT 密钥**：替换为随机生成的 32+ 字符字符串
- [ ] **数据库密码**：使用强密码，不要使用默认值
- [ ] **数据库访问**：不要将 MySQL 3306 端口对外网开放
- [ ] **CORS 配置**：当前允许所有来源，生产环境应限制具体域名
- [ ] **HTTPS**：务必配置 SSL 证书（Let's Encrypt 免费）
- [ ] **Nginx 安全**：添加请求体大小限制、速率限制
- [ ] **日志级别**：将 `logging.level.com.company.vehicle` 从 `debug` 改为 `info`
- [ ] **文件存储**：确保 `uploads/` 目录不在公网可随意访问的路径下

---

## 开发指南

### 本地前后端联调

1. 启动后端：`cd backend && ./mvnw spring-boot:run`（默认端口 18231）
2. 启动管理后台：`cd frontend && npm run dev:hot`（端口 3592）
3. 启动驾驶员端：`cd driver-app && npm run dev:hot`（端口 3593）

Vite 已配置代理，前端发往 `/api` 的请求自动转发到后端。

### 图片压缩

前端实现了图片自动压缩上传：

- 最大尺寸：1600 × 1600 px
- 压缩质量：0.78
- 小于 350KB 的图片不压缩
- 输出格式：JPEG

压缩工具位于 `frontend/src/utils/imageUpload.js` 和 `driver-app/src/utils/imageUpload.js`。

### API 请求封装

前后端均使用 Axios 封装请求：

- 自动携带 JWT Token
- 统一错误处理（中文提示）
- 401/403 自动跳转登录页
- 文件上传使用 `multipart/form-data`

---

## 开源协议

本项目仅供学习交流和技术研究使用。

---

## 联系方式

如有问题或建议，欢迎提交 Issue 或联系开发者。