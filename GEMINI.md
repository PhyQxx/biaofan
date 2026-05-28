# GEMINI.md

本项目是一个复杂的 Monorepo，包含后端（SpringBoot）、PC 前端（Vue3 TS）和移动端（UniApp）。在进行开发和维护时，请遵循以下准则。

## 开发原则

1. **类型安全 (PC 前端)**: 严格遵守 TypeScript 类型定义。新增功能时，优先在 `frontend/src/types/index.ts` 中定义接口。
2. **统一响应 (后端)**: 确保所有新的 Controller 接口返回 `com.biaofan.util.Result<T>`。
3. **游戏化逻辑**: 任何涉及用户操作的行为（如完成 SOP、提交模板）都应检查是否需要通过 `GamificationService` 触发积分或经验值更新。
4. **AI 适配**: 新增 AI 功能时，应通过 `AiModelFactory` 获取实例，以支持多模型切换。
5. **移动端一致性**: 移动端虽然使用 JS，但业务逻辑（如 Token 存储、API 路径）应与 PC 端保持逻辑一致。注意移动端 Token Key 为 `token`，PC 端为 `bf_token`。

## 常用命令

### 后端
- 启动：`cd backend && mvn spring-boot:run`
- 数据库迁移：通过 Flyway 自动执行，SQL 文件位于 `backend/src/main/resources/db/migration/`。

### PC 前端
- 启动：`cd frontend && npm run dev`
- 类型检查：`cd frontend && npm run type-check`
- 构建：`cd frontend && npm run build`

### 移动端
- H5 启动：`cd mobile && npm run dev:h5`
- 小程序编译：`cd mobile && npm run build:mp-weixin`

## 目录索引

- `/backend/src/main/java/com/biaofan/ai`: AI 适配层。
- `/frontend/src/components/gamification`: 游戏化 UI 组件库。
- `/frontend/src/views/admin`: 综合管理后台视图。
- `/docker`: 容器化部署脚本。

## 协作建议

- 在修改数据库结构时，必须新增 Flyway 迁移脚本。
- 在修改 API 接口时，同步更新 `frontend/src/api/` 下的对应文件。
