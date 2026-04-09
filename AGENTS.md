# AGENTS.md — 标帆 SOP 系统

## 项目概述

企业标准化操作流程管理系统。Monorepo 结构，三个独立子项目 + Docker 部署。

```
backend/    → Spring Boot 3.2 / Java 17 / MyBatis-Plus / MySQL / Redis / JWT
frontend/   → Vue 3 + TypeScript + Vite + Element Plus + Pinia（PC 端）
mobile/     → UniApp + Vue 3 + Pinia（移动端，H5 / 微信小程序 / Android）
docker/     → Docker Compose 部署（host 网络模式，Nginx 反代）
```

## 启动命令

```bash
# 后端（需要本地 MySQL + Redis）
cd backend && mvn spring-boot:run          # → http://localhost:8013

# PC 前端
cd frontend && npm install && npm run dev  # → http://localhost:8011

# 移动端 H5
cd mobile && npm install && npm run dev:h5 # → http://localhost:8080
```

测试账号：手机号 `13800138000`，密码 `test123`

## 端口映射

| 服务 | 端口 |
|------|------|
| 后端 API | 8013 |
| PC 前端（Vite dev） | 8011 |
| 移动端 H5（Vite dev） | 8080 |

## 后端（backend/）

**包结构**：`com.biaofan.{controller, service, service.impl, mapper, entity, dto, util, job}`

**关键约定**：
- ORM：MyBatis-Plus，实体用 `@TableName` + `@Data`（Lombok），ID 策略 `IdType.AUTO`
- 统一响应：所有 Controller 返回 `Result<T>`（`code` / `message` / `data` / `success` / `timestamp`）
- API 路径统一前缀 `/api/`（如 `@RequestMapping("/api/auth")`）
- 认证：自定义 `JwtAuthFilter`（在 util 包），Spring Security + JWT，token TTL 24h
- 验证码：存 Redis，key `sms:code:{phone}`，5 分钟有效（开发环境验证码打印到控制台）
- 复杂 SQL 用 XML mapper（`resources/mapper/*.xml`），简单 CRUD 靠 MyBatis-Plus 自动生成
- 定时任务：`@EnableScheduling`，job 类在 `com.biaofan.job`
- 文件上传：`/www/sop-uploads`，最大 10MB
- **无 src/test 目录**——暂无单元测试
- 数据库连接信息在 `application.yml`（远程 MySQL `mysql.pnkx.top:13306`）

**构建产物**：`target/biaofan-backend-1.0.0.jar`

## PC 前端（frontend/）

**TypeScript 项目**，`tsconfig.app.json` 开启 strict + noUnusedLocals + noUnusedParameters。

**路径别名**：`@/` → `src/`

**关键约定**：
- UI 库：Element Plus（全局注册，直接用 `<el-*>` 组件）
- 状态管理：Pinia Composition API（`defineStore('name', () => {...})`）
- HTTP：axios 实例在 `src/api/index.ts`，baseURL `/api`，自动附加 `Authorization: Bearer {token}`
- Token 存储：`localStorage.getItem('bf_token')`
- 路由守卫：`router.beforeEach` 检查 `requiresAuth` meta
- 图表：ECharts via `vue-echarts`
- 构建流程：`vue-tsc -b && vite build`（先类型检查再打包）

**目录**：
- `views/` — 页面（auth/ workspace/ execution/ sop/ stats/ notification/ admin/ profile/）
- `components/gamification/` — 游戏化组件（徽章、排行榜、积分）
- `stores/` — auth.ts, sop.ts
- `api/` — index.ts（基础 axios）, gamification.ts, admin.ts, stats.ts

## 移动端（mobile/）

**纯 JavaScript 项目**（无 TypeScript）。UniApp + Vue 3，用 `@dcloudio/vite-plugin-uni`。

**关键约定**：
- 页面路由：`src/pages.json`（UniApp 声明式路由，不是 vue-router）
- TabBar：4 个 tab（首页/记录/通知/我的）
- HTTP：`uni.request` 封装在 `src/api/index.js`，**BASE_URL 硬编码为 `http://192.168.31.104:8013`**——切换环境需改此值
- Token 存储：`uni.getStorageSync('token')`（注意：PC 端用 `bf_token`，移动端用 `token`，key 不同）
- 图片上传：`uni.uploadFile`
- 离线草稿：`src/store/draft.js`，网络恢复自动同步
- H5 路由模式：hash（见 manifest.json `h5.router.mode`）
- App 入口：`createSSRApp`（SSR 兼容写法）

**构建目标**：
- `npm run build:h5` → `dist/build/h5/`
- `npm run build:mp-weixin` → `dist/build/mp-weixin/`（导入微信开发者工具）
- `npm run build:app` → `unpackage/`（需 HBuilderX）

## Docker 部署

- **网络模式：host**（容器直接使用宿主机网络，无端口映射）
- `Dockerfile.backend`：基于 `amazoncorretto:17-alpine`，覆盖数据库地址为 `127.0.0.1`
- `Dockerfile.frontend`：多阶段构建——先构建 mobile H5，再用 Nginx 同时服务 PC（`/usr/share/nginx/html/`）和移动端（`/usr/share/nginx/mobile/`）
- Nginx 配置：`/api/` 反代到 `127.0.0.1:8013`，`/mobile/` 服务 UniApp H5，`/` 服务 PC 前端（Vue history mode）
- 构建前需先 `cd frontend && npm run build` 生成 `frontend/dist/`

**部署顺序**：先启动 MySQL + Redis → `docker compose up biaofan-backend` → `docker compose up biaofan-frontend`

## API 约定

- 所有接口前缀 `/api/`
- 响应格式：`{ code: 200, message: "操作成功", data: T, success: true, timestamp: long }`
- 认证 header：`Authorization: Bearer {jwt_token}`
- 401 响应自动清除 token 并跳转登录页

## 注意事项

- 移动端 `vite.config.js` 和 `manifest.json` 中的后端代理地址 `192.168.31.104` 是开发机 IP，部署前需改
- `application.yml` 中的 MySQL 连接指向远程服务器 `mysql.pnkx.top:13306`，Docker 部署时会被环境变量覆盖
- 微信小程序 appid `wx0000000000000000` 是占位符
- `JwtUtil` secret 硬编码在 `application.yml`，生产环境应通过环境变量注入
