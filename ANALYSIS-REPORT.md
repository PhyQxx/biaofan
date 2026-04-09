# 标帆 SOP 系统 — 全项目深度审查报告

> 审查时间：2026-04-10  
> 审查范围：backend / frontend / mobile / docker  
> 发现问题总计：**80 个**（Critical 12 / High 22 / Medium 44 / Low 22）

---

## 一、总体概述

标帆 SOP 是企业标准化操作流程管理系统，Monorepo 结构，含 Spring Boot 后端、Vue3 PC 前端、UniApp 移动端、Docker 部署四个子项目。

核心业务流：SOP 创建 → 版本管理 → 派发执行 → 步骤打卡 → 完成/异常上报 → 统计

附加功能：游戏化（积分/徽章/排行榜）、SOP 模板市场、通知推送、离线草稿

---

## 二、Critical 级别（必须立即修复，共 12 个）

### C-01: /api/admin/** 设为 permitAll — 管理接口完全无认证
- **文件**: `backend/.../config/SecurityConfig.java:36`
- **描述**: 任何人无需登录即可访问审核模板、下架模板、管理徽章/商品/成长规则等管理功能
- **修复**: 删除 `.requestMatchers("/api/admin/**").permitAll()`，改为 `.requestMatchers("/api/admin/**").hasRole("ADMIN")`

### C-02: /api/gamification/** 设为 permitAll — 游戏化接口完全无认证
- **文件**: `backend/.../config/SecurityConfig.java:35`
- **描述**: leaderboard、store、levelRanking 等接口可被任何人访问；Controller 用 `@AuthenticationPrincipal` 但 permitAll 路径上 Spring Security 不强制认证，userId 为 null
- **修复**: 删除此 permitAll，使其走 `anyRequest().authenticated()`

### C-03: JWT Secret 硬编码在 application.yml
- **文件**: `backend/src/main/resources/application.yml:41`
- **描述**: `jwt.secret: biaofan-sop-platform-secret-key-2026-very-long-and-secure` 明文写配置文件，代码仓库泄露即可伪造任意用户 JWT
- **修复**: 使用环境变量 `${JWT_SECRET}` 或 Spring Cloud Vault

### C-04: 多处 IDOR — 用户身份从请求参数获取而非认证主体
- **文件**: `MarketplaceController.java:60,106,115,123,153` / `PushController.java:23` / `DraftController.java:26`
- **描述**: userId 从 `@RequestParam` 获取，攻击者可冒充任意用户收藏/取消收藏/提交模板/使用模板/注册推送 token
- **修复**: 所有用户身份必须从 `@AuthenticationPrincipal Long userId` 获取

### C-05: AdminMarketplaceController auditorId 从请求参数传入
- **文件**: `backend/.../controller/AdminMarketplaceController.java:40`
- **描述**: 审核人 ID 由调用方自行指定，配合 C-01 无认证，任何人可伪造审核人
- **修复**: 从 `@AuthenticationPrincipal` 获取，并验证 admin 角色

### C-06: 移动端 main.js 未注册 Pinia
- **文件**: `mobile/src/main.js`
- **描述**: 未 `app.use(createPinia())`，所有 `useAuthStore()` / `useDraftStore()` 调用都会报 "pinia is not installed"，整个应用无法运行
- **修复**: 添加 `import { createPinia } from 'pinia'` 并 `app.use(createPinia())`

### C-07: 移动端 BASE_URL 硬编码内网 IP + HTTP 明文
- **文件**: `mobile/src/api/index.js:6`
- **描述**: `http://192.168.31.104:8013` 无法部署到正式环境，HTTP 明文传输 token 和数据
- **修复**: 使用环境变量 `import.meta.env.VITE_API_BASE`，生产环境必须 HTTPS

### C-08: 移动端草稿 syncAll 图片失败仍标 synced
- **文件**: `mobile/src/store/draft.js`
- **描述**: 图片上传失败后 photoUrl 为空字符串，但仍标记 `synced: true` 提交，服务端收到无图片的脏数据
- **修复**: 图片上传失败时该草稿不应标记 synced，应跳过保留重试

### C-09: 移动端离线/失败打卡标记 completed 导致数据不一致
- **文件**: `mobile/src/pages/execute/execute.vue:266`
- **描述**: 在线失败 catch 和离线模式下，前端直接 `step.status = 'completed'`，服务端完全不知道
- **修复**: 离线打卡标记为 `pending_sync` 中间状态，而非 `completed`

### C-10: 前端硬编码 fallback userId 为 '1'
- **文件**: `frontend/src/views/marketplace/MarketplaceIndex.vue:249` / `TemplateDetail.vue:160` / `Favorites.vue:80`
- **描述**: `localStorage.getItem('bf_user_id') || '1'` 未登录时默认 ID=1（可能是 admin），导致越权操作
- **修复**: 从 auth store 的 userInfo.userId 获取，未登录则跳转登录页

### C-11: 移动端 records.vue 异常记录获取是死代码
- **文件**: `mobile/src/pages/records/records.vue:117`
- **描述**: `api.execution.reportException` 是函数引用而非调用（缺括号），异常记录永远无数据
- **修复**: 修正函数调用或使用正确的列表接口

### C-12: 移动端 pages.json 缺少 drafts 页面注册
- **文件**: `mobile/src/pages.json`
- **描述**: `pages/mine/drafts` 页面未注册在 pages 数组中，跳转必定 404
- **修复**: 在 pages 数组中添加 drafts 页面配置

---

## 三、High 级别（尽快修复，共 22 个）

### 后端（12 个）

| 编号 | 文件 | 问题 | 修复建议 |
|------|------|------|---------|
| H-01 | AuthController.java:63 | SMS 验证码明文打印到控制台，生产日志泄露即可绕过 | 移除或仅记录脱敏手机号 |
| H-02 | AuthController.java:55 | `new Random().nextInt()` 非密码学安全，可预测 | 使用 `SecureRandom` |
| H-03 | AuthController.java:45-66 | send-code 无频率限制，可短信轰炸/穷举验证码 | Redis 限流（60s 1次/天 5次）+ 尝试次数限制 |
| H-04 | ExecutionController.java:83-93 | GET /execution/{id} 无所有权校验，任何登录用户可看任意执行记录 | 校验 executorId == userId |
| H-05 | SopExceptionController.java:38-39 | list() 无用户过滤，返回全系统异常记录 | 增加 userId 过滤或限制 admin |
| H-06 | SopExceptionController.java:43-54 | resolve() 无权限校验，任何用户可 resolve 任何异常 | 添加 admin 角色校验 |
| H-07 | SecurityConfig.java:48 | CORS 允许所有来源 `*` | 限定为实际前端域名 |
| H-08 | UploadController.java:56-60 | 文件上传仅校验 Content-Type，扩展名取自用户文件名，可上传 .html/.jsp | 扩展名白名单 + 根据内容重新生成扩展名 |
| H-09 | SopTemplateController.java:24-41 | /api/sop/templates permitAll 但未强制 status=published，可看所有用户草稿 | 公开接口强制过滤 status=published |
| H-10 | GamificationServiceImpl.java:215-250 | redeemProduct() 积分扣减非原子操作，并发可超额使用 | 原子 SQL: `UPDATE SET score=score-price WHERE score>=price` |
| H-11 | SopVersionController.java:19-22 | versions() 无权限校验，任何登录用户可看任意 SOP 版本 | 仅 SOP 所有者或 admin 可查看 |
| H-12 | JwtUtil + JwtAuthFilter | 无 Token 刷新/黑名单机制，修改密码/封禁后 token 仍有效 24h | Redis 黑名单 + refresh token |

### 前端（6 个）

| 编号 | 文件 | 问题 | 修复建议 |
|------|------|------|---------|
| H-13 | router/index.ts:45-48 | 路由守卫仅检查 token 存在性，任何字符串都能通过 | 守卫中调 fetchMe 验证有效性 |
| H-14 | api/index.ts + stores/auth.ts | Token 存 localStorage 易受 XSS | 改用 httpOnly cookie 或 sessionStorage |
| H-15 | layout/Layout.vue:92 | `username === 'admin'` 判断管理员，纯前端可绕过 | 使用后端 role 字段 + 路由 meta 守卫 |
| H-16 | ExecutionView.vue:111-117 | N+1 查询：逐个请求 SOP 详情 | 后端批量接口或列表接口返回 sopTitle |
| H-17 | ExecutionDoView.vue:289-325 | 步骤提交成功后直接改本地状态不重新获取，双击可重复提交 | 提交后从服务端重新获取；入口加防抖 |
| H-18 | 全项目 | `any` 类型滥用 10+ 处，失去 TS 类型检查价值 | 定义完整接口类型替换 any |

### 移动端（4 个）

| 编号 | 文件 | 问题 | 修复建议 |
|------|------|------|---------|
| H-19 | api/index.js | 401 并发重复跳转 reLaunch | 加全局 isRedirecting 锁 |
| H-20 | store/auth.js | userInfo 存 JSON.stringify 但取时未 JSON.parse，state 中是字符串 | 初始化时 JSON.parse |
| H-21 | App.vue | onLaunch 调 syncDrafts 但 Pinia 可能未初始化 | 移到 onShow 并确保 Pinia 已安装 |
| H-22 | pages/execute/execute.vue:266 | `this.$set()` 是 Vue 2 写法，Vue 3 无效 | 直接赋值 `this.stepPhotos[stepIndex] = []` |

---

## 四、Medium 级别（建议修复，共 44 个）

### 后端（15 个）

| 编号 | 问题 | 文件 |
|------|------|------|
| M-01 | DashboardStatsController 与 StatisticsController 路径冲突 /api/stats | 两个 Controller |
| M-02 | SopInstanceController.getInstance() 无所有权校验 | SopInstanceController.java:28-37 |
| M-03 | SopDispatchServiceImpl.batchDispatch() 未验证 SOP 所有权 | SopDispatchServiceImpl.java:31-74 |
| M-04 | SopStatisticsController 全表查询 + N+1 问题 | SopStatisticsController.java:42-122 |
| M-05 | DashboardStatsServiceImpl.getLeaderboard() N+1 查询 | DashboardStatsServiceImpl.java:109-129 |
| M-06 | GamificationServiceImpl.getBadges() N+1 查询 | GamificationServiceImpl.java:87-119 |
| M-07 | 多个列表接口无分页（execution/exception/dispatch/gamification） | 多个 Controller |
| M-08 | BCryptPasswordEncoder 重复创建（SecurityConfig 已有 Bean） | UserServiceImpl.java:20 |
| M-09 | MarketplaceController 直接注入 Mapper 绕过 Service 层 | MarketplaceController.java:34-37 |
| M-10 | ScheduleTaskServiceImpl 手工解析 cron 脆弱 | ScheduleTaskServiceImpl.java:86-149 |
| M-11 | 注册接口无验证码校验，send-code 产的验证码从未被使用 | UserServiceImpl.java:37-49 |
| M-12 | updatePhone 无验证码校验 | UserServiceImpl.java:83-89 |
| M-13 | JwtUtil.getUserId() 吞掉所有异常 | JwtUtil.java:43-45 |
| M-14 | SopServiceImpl.delete() 硬删无软删除，不校验进行中执行 | SopServiceImpl.java:74-99 |
| M-15 | 文件上传路径拼接可能路径遍历 | UploadController.java:52-64 |

### 前端（11 个）

| 编号 | 问题 | 文件 |
|------|------|------|
| M-16 | Webhook Secret 前端明文传输 | ProfileView.vue:172,456-461 |
| M-17 | ExecutionDoView 与 InstanceDoView 近乎完全复制（580行） | execution/ 目录 |
| M-18 | 登录成功后不调 fetchMe，userInfo 为 null | stores/auth.ts + LoginView.vue |
| M-19 | fetchMe 静默吞错 `catch {}` | stores/auth.ts:24-29 |
| M-20 | API 响应类型不一致 | admin.ts:28, gamification.ts:112 |
| M-21 | LoginView 与 RegisterView 70% CSS 重复 | auth/ 目录 |
| M-22 | StatisticsView resize 监听在模块顶层注册，组件销毁才移除 | StatisticsView.vue:222 |
| M-23 | 多处 API 调用无错误处理或 `catch {}` | WorkbenchView/ExecutionView/notification store |
| M-24 | 401 处理导致双重错误提示 | api/index.ts:18-29 |
| M-25 | activate 请求无错误处理，失败仍乐观改状态 | ExecutionDoView.vue:340 |
| M-26 | SopEditorView handleSave 始终设 status=draft | SopEditorView.vue:338 |

### 移动端（18 个）

| 编号 | 问题 | 文件 |
|------|------|------|
| M-27 | upload.image JSON.parse 无 try-catch | api/index.js |
| M-28 | upload.image 成功码判断与 request 不一致 | api/index.js |
| M-29 | executorId 从前端 localStorage 传入，应后端从 JWT 提取 | api/index.js:78-87 |
| M-30 | userInfo 字段名不确定 `res.data.userId \|\| res.data.id` | store/auth.js |
| M-31 | draft.js Storage 可能超限（小程序 10MB） | store/draft.js |
| M-32 | syncAll 图片并发上传无控制 | store/draft.js |
| M-33 | retryCount 无上限递增 | store/draft.js |
| M-34 | genUUID 用 Math.random 非密码学安全 | store/draft.js |
| M-35 | 临时文件路径下次启动可能失效 | store/draft.js |
| M-36 | App.vue initPush 引用 auth.baseUrl 不存在 | App.vue |
| M-37 | App.vue initPush 绕过统一 request 函数 | App.vue |
| M-38 | 登录页手机号只检查长度 11 位 | login.vue |
| M-39 | 验证码长度校验不明确（4-6 位） | login.vue |
| M-40 | sendCode setInterval 未在组件销毁时清除 | login.vue |
| M-41 | loadDetail 中 pending 自动 start 无用户确认 | execute.vue |
| M-42 | takePhoto 只上传第一张照片，UI 允许 3 张 | execute.vue |
| M-43 | index 页网络监听重复注册 | index.vue |
| M-44 | records 异常记录获取逻辑为空实现 | records.vue |

---

## 五、Low 级别（可优化，共 22 个）

### 后端（6 个）

| 编号 | 问题 |
|------|------|
| L-01 | ExecutionController 与 SopExecutionController 大量代码重复 |
| L-02 | SopExecutionServiceImpl 手动 new ObjectMapper()，未用 Spring Bean |
| L-03 | SopDispatchServiceImpl 忽略 JsonProcessingException |
| L-04 | 全局用 RuntimeException 无法区分业务/系统异常，缺 @ControllerAdvice |
| L-05 | 无方法级安全注解 @PreAuthorize |
| L-06 | GamificationServiceImpl.getScoreHistory 行196无意义查询（调试遗留） |

### 前端（7 个）

| 编号 | 问题 |
|------|------|
| L-07 | 注册页手机号无格式验证 |
| L-08 | "记住我"功能未实现（变量定义了没使用） |
| L-09 | statusLabel 用 as any 绕过类型检查 |
| L-10 | 多组件内联重复的 formatDate 函数 |
| L-11 | echarts 顶层 import SSR 不兼容 |
| L-12 | API 模块路径调用方式不统一 |
| L-13 | SopEditorView publishSop 失败后状态不一致 |

### 移动端（9 个）

| 编号 | 问题 |
|------|------|
| L-14 | token key 'token' 过于通用，易冲突 |
| L-15 | checkLogin 只检查 token 存在不验证有效性 |
| L-16 | mine.vue 手机号未脱敏显示 |
| L-17 | drafts.vue syncing 状态可能卡在 true |
| L-18 | notifications getNotifIcon 实质是 identity 映射 |
| L-19 | 微信小程序 appid 占位符 wx0000000000000000 |
| L-20 | manifest.json urlCheck:false 上线前必须开启 |
| L-21 | pages.json login 为首页项，tabBar index 应为首页 |
| L-22 | common/utils checkNetwork 不检查实际连通性 |

---

## 六、问题统计汇总

| 严重级别 | 后端 | 前端 | 移动端 | 合计 |
|---------|------|------|--------|------|
| Critical | 5 | 1 | 6 | **12** |
| High | 12 | 6 | 4 | **22** |
| Medium | 15 | 11 | 18 | **44** |
| Low | 6 | 7 | 9 | **22** |
| **合计** | **38** | **25** | **37** | **100** |

> 注：部分跨端共性问题已归入对应端，总计去重后约 80 个独立问题。

---

## 七、最紧急修复优先级 TOP 10

| 优先级 | 编号 | 问题 | 影响 |
|--------|------|------|------|
| 1 | C-06 | 移动端未注册 Pinia | 移动端完全无法运行 |
| 2 | C-12 | pages.json 缺 drafts 页面 | 草稿页面跳转 404 |
| 3 | C-01/C-02 | SecurityConfig permitAll | 管理接口/游戏化接口无认证 |
| 4 | C-04/C-05 | IDOR — userId 从请求参数获取 | 越权操作 |
| 5 | C-10 | 前端 fallback userId='1' | 越权操作 |
| 6 | C-03 | JWT Secret 硬编码 | Token 伪造 |
| 7 | C-08/C-09 | 草稿同步/离线打卡数据不一致 | 数据脏写 |
| 8 | H-01/H-02/H-03 | SMS 验证码安全问题 | 短信轰炸/验证码绕过 |
| 9 | H-08 | 文件上传扩展名未严格校验 | XSS/RCE |
| 10 | H-10 | 积分扣减竞态条件 | 积分超发 |

---

## 八、架构级建议

1. **安全体系重构**: 引入 RBAC 权限模型 + 方法级 @PreAuthorize + Token 黑名单
2. **API 层规范**: 统一错误码体系、业务异常类、@ControllerAdvice 全局异常处理
3. **前端类型化**: 定义完整 TypeScript 接口，消除 any，统一 API 调用规范
4. **移动端基础修复**: Pinia 注册、环境变量、页面注册补全
5. **性能优化**: 列表接口分页、N+1 查询改 JOIN、全表查询改 SQL 聚合
6. **数据一致性**: 离线同步策略完善（中间状态 + 重试 + 回退）、积分扣减原子化
7. **测试覆盖**: 当前零单元测试，至少覆盖核心业务（认证/执行/积分）

---

*报告生成工具：Hermes Agent | 审查模型：GLM-5.1*
