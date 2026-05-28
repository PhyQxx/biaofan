# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 项目概述

企业标准化操作流程（SOP）管理系统。Monorepo 结构：

```
backend/    → Spring Boot 3.2 / Java 17 / MyBatis-Plus / MySQL / Redis / JWT
frontend/   → Vue 3 + TypeScript + Vite + Element Plus + Pinia（PC 端）
mobile/     → UniApp + Vue 3 + Pinia（移动端 H5 / 微信小程序 / Android，纯 JS，无 TypeScript）
docker/     → Docker Compose 部署（host 网络模式，Nginx 反代）
```

## 构建与运行

```bash
# 后端（需要 MySQL + Redis 可用）
cd backend && mvn spring-boot:run          # → localhost:8013

# PC 前端
cd frontend && npm install && npm run dev  # → localhost:8011，/api 代理到 8013

# 移动端 H5
cd mobile && npm install && npm run dev:h5 # → localhost:8080

# 移动端小程序
cd mobile && npm run build:mp-weixin       # → dist/build/mp-weixin/

# Docker 部署
cd docker && docker compose up biaofan-backend && docker compose up biaofan-frontend
```

测试账号：`13800138000` / `test123`

端口：后端 8013，PC 前端 8011，移动端 H5 8080

## 后端关键约定

- **包路径**：`com.biaofan.{controller, service, service.impl, mapper, entity, dto, util, job, ai}`
- **统一响应**：所有 Controller 返回 `Result<T>`，字段：`code / message / data / success / timestamp`
- **AI 模块**：
  - `AiModelFactory`: 处理多模型（GPT/Claude/DeepSeek）适配。
  - `SopAiAssistService`: 提供 SOP 辅助生成与优化接口。
  - 配置表：`ai_model_config`。
- **游戏化模块**：
  - 核心：`Score / Level / Badge / Rank`。
  - 规则引擎：`AdminRules` 配置触发条件（如：完成 SOP +10 分）。
- **市场模块**：
  - `MarketplaceController`: 模板浏览、购买、收藏。
  - 审核流程：用户上传 → 管理员审核 → 上架。
- **API 前缀**：所有路径以 `/api/` 开头。
- **认证**：Spring Security + JWT。Controller 中用 `@AuthenticationPrincipal Long userId` 获取。
- **ORM**：MyBatis-Plus。简单 CRUD 靠自动生成，复杂 SQL 用 `resources/mapper/*.xml`。
- **无单元测试**：目前无 src/test 目录。

## PC 前端关键约定

- **TypeScript strict 模式**，`noUnusedLocals` + `noUnusedParameters` 已开启。
- **模块化路由**：
  - `/execution`: 逐步执行。
  - `/sop`: 编辑与版本控制。
  - `/marketplace`: 模板市场。
  - `/admin`: 后台管理（AI/Email/游戏化/市场）。
- **组件规范**：
  - `src/components/common`: 通用 UI。
  - `src/components/gamification`: 游戏化专用组件。
  - `src/views/layout`: 侧边栏与导航。
- **UI**：Element Plus。
- **状态管理**：Pinia Composition API。
- **HTTP**：axios 实例在 `src/api/index.ts`，自动附加 `Authorization`。
- **构建**：`vue-tsc -b && vite build`。

## 移动端关键约定

- **纯 JavaScript**，无 TypeScript
- **路由**：`src/pages.json`（UniApp 声明式路由），TabBar 4 个 tab
- **HTTP**：`uni.request` 封装在 `src/api/index.js`，BASE_URL 硬编码 `http://192.168.31.104:8013`（切换环境需改此值）
- **Token 存储**：`uni.getStorageSync('token')`（注意 key 与 PC 端 `bf_token` 不同）
- **离线草稿**：`src/store/draft.js`，网络恢复自动同步
- **H5 路由模式**：hash（manifest.json 配置）

## 注意事项

- 移动端 `vite.config.js` 和 `api/index.js` 中后端地址 `192.168.31.104` 是开发机 IP，部署前需修改
- `application.yml` 中 MySQL 指向 `mysql.pnkx.top:13306`，Docker 部署时环境变量覆盖
- 微信小程序 appid `wx0000000000000000` 是占位符
- JWT secret 硬编码在 `application.yml`，生产环境应通过环境变量注入

# gstack

Use the `/browse` skill from gstack for all web browsing. Never use `mcp__claude-in-chrome__*` tools.

Available gstack skills:
`/office-hours`, `/plan-ceo-review`, `/plan-eng-review`, `/plan-design-review`, `/design-consultation`, `/design-shotgun`, `/design-html`, `/review`, `/ship`, `/land-and-deploy`, `/canary`, `/benchmark`, `/browse`, `/connect-chrome`, `/qa`, `/qa-only`, `/design-review`, `/setup-browser-cookies`, `/setup-deploy`, `/retro`, `/investigate`, `/document-release`, `/codex`, `/cso`, `/autoplan`, `/plan-devex-review`, `/devex-review`, `/careful`, `/freeze`, `/guard`, `/unfreeze`, `/gstack-upgrade`, `/learn`
# currentDate
Todayʹs date is 2026/04/10.