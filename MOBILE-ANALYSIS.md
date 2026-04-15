# biaofan-sop 移动端深度分析报告

> 日期：2026-04-14 | 覆盖：mobile/src 全部代码（8个页面 + stores + api + utils）

---

## 一、技术栈与架构

### 1.1 技术选型

| 层级 | 技术 | 说明 |
|------|------|------|
| 框架 | UniApp + Vue 2 | **Options API**，非 Vue 3 |
| 状态管理 | Pinia | auth.js + draft.js |
| 构建 | Vite | 移动端 H5 模式 |
| 路由 | UniApp 声明式 | pages.json 配置 |
| 网络 | 原生 `uni.request` | 有封装，无 axios |
| 本地存储 | `uni.getStorageSync` | JSON 数组，无 SQLite |
| 工具 | dayjs | 日期时间处理 |

**重要**：移动端是 **Vue 2**，PC 端（frontend/）是 **Vue 3 + TypeScript**，两个前端技术栈完全不同。

### 1.2 页面结构

```
pages.json
├── pages/login/login            登录（custom导航栏）
├── pages/register/register      注册
├── pages/index/index           首页=执行台        ← Tab1
├── pages/execute/execute       执行详情（进入后）
├── pages/records/records       执行记录           ← Tab2
├── pages/notifications/notifications 通知       ← Tab3
├── pages/mine/mine              我的               ← Tab4
└── pages/mine/drafts           离线草稿（子页）

tabBar 4个：首页 / 记录 / 通知 / 我的
```

### 1.3 目录结构

```
mobile/src/
├── pages.json                  页面路由配置
├── App.vue
├── api/index.js                所有接口（uni.request封装）
├── common/utils.js             工具函数（dayjs封装）
├── store/
│   ├── auth.js                 认证状态（Pinia）
│   └── draft.js                离线草稿状态（Pinia）
└── pages/
    ├── login/login.vue
    ├── register/register.vue
    ├── index/index.vue         ← 核心：SOP实例列表
    ├── execute/execute.vue     ← 核心：步骤打卡
    ├── records/records.vue
    ├── notifications/notifications.vue
    └── mine/
        ├── mine.vue
        └── drafts.vue
```

---

## 二、核心业务流程

### 2.1 周期实例执行流程（主要路径）

```
index.vue onShow
    │
    ├── api.instance.myInstances()  获取所有周期实例
    │     → status: pending / in_progress / overdue / completed
    │
    ├── 并发请求每个SOP详情（Promise.all）
    │     → 填充 inst.sopTitle + inst.totalSteps
    │
    ├── computed 分组：pendingList / inProgressList / overdueList
    │
    └── 点击卡片 → navigateTo(/pages/execute/execute?instanceId=&sopId=)
            │
execute.vue（instance模式）
    │
    ├── loadInstanceDetail():
    │   ├── api.instance.detail(instanceId)      获取实例当前状态
    │   ├── api.sop.detail(sopId)               获取SOP步骤定义
    │   ├── api.instance.activate()              pending→in_progress（防重入锁）
    │   └── 合并 stepStatuses + currentStepIndex → 每步 status
    │
    ├── 打卡（handleCheckIn）:
    │   ├── 在线 → api.instance.completeStep() → status=completed
    │   ├── 在线请求失败 → 也落入草稿（draftStore.addDraft）
    │   └── 在线打卡成功 → 清空 note + photos
    │
    ├── 异常上报（showExceptionModal → submitException）:
    │   └── ⚠️ 用了 api.execution.reportException()（错的）
    │
    └── 完成执行（handleFinish）:
          └── api.instance.finish() → navigateBack()
```

### 2.2 离线草稿同步流程

```
drafts.vue / 首页网络恢复时
    │
    └── draftStore.syncAll()
          ├── 按 executionId 分组
          ├── 逐张上传图片 → api.upload.image()
          ├── 图片全成功 → api.draft.sync() 批量同步
          ├── 标记 synced=true
          └── 图片失败 → retryCount++，保留草稿
```

---

## 三、逐页分析

### 3.1 首页 index.vue ⭐⭐⭐⭐

**职责**：SOP 周期实例列表，三个 Tab：待执行 / 进行中 / 逾期

**亮点**：
- `Promise.all` 并发请求所有 SOP 详情，避免串行等待
- SOP ID 去重，只请求必要的详情
- 网络监听用 `_networkRegistered` 标志位解决 UniApp `onNetworkStatusChange` 泄漏问题
- `onPullDownRefresh` 下拉刷新
- `computed` 清晰分离四种状态列表

**问题**：
1. **SOP 详情无超时兜底**：某个 SOP 接口超时/删除会一直 pending，影响整体渲染
2. **所有 SOP 都失败时无提示**：列表为空但无错误提示
3. **`completedList` 定义但未使用**：Tab 只有三个，completed 被丢弃
4. **`draftStore.hasPendingDrafts` 每次 computed 都重新构造**：未缓存

---

### 3.2 执行详情 execute.vue ⭐⭐⭐⭐⭐

**职责**：单 SOP 步骤打卡，instance 模式（周期实例）

**亮点（工程质量很高）**：
1. **activate 防重入锁**（`activating` 标志位）：用户快速双击只触发一次激活
2. **步骤状态双重来源**：优先用 `stepStatuses`（后端精确状态），其次用 `currentStepIndex` 推算（兼容旧数据）
3. **打卡失败自动落入草稿**：在线请求 catch 后也保存草稿，数据不丢
4. **照片全上传**：`for...of` 串行上传所有照片（旧版只传第一张）
5. **SOP content 容错**：`raw !== 'null'` 防止 `JSON.parse(null)` 返回 'null'
6. **`relativeTime` 从 utils 引入**（而非直接在 methods 中定义）

**问题**：
1. **⚠️ 异常上报接口用错了**（第481行）：
   ```javascript
   // instance 模式下，this.executionId 实际是 instanceId
   // 但调用的是 api.execution.reportException（旧的 executions 接口）
   await api.execution.reportException(this.executionId, { ... })
   ```
   应该新增 `api.instance.reportException()` 或类似接口

2. **activate 后状态是前端伪造的**：`instData.status = 'in_progress'` 而非等后端返回一致

3. **缺少执行完成总结页**：完成后直接 `navigateBack()`，用户没有成就感反馈（PC 端有 GrowthCelebration）

---

### 3.3 执行记录 records.vue ⭐⭐⭐

**职责**：已完成 / 异常 执行记录

**亮点**：
- `Promise.allSettled` 并发请求 completed 和 abnormal，一个挂不影响另一个
- 去重逻辑（以 id 为准）

**问题**：
1. **无分页**：超过 20 条会显示不全
2. **`myInstances('abnormal')` 状态值待确认**：后端 abnormal 状态定义是否和 `hasException=true` 重叠？
3. **跳转时 `item.id` 作为 instanceId**：需确认后端返回的 `id` 究竟是 instanceId 还是 executionId

---

### 3.4 通知 notifications.vue ⭐⭐⭐

**职责**：通知列表 + 点击跳转

**亮点**：
- `item.instanceId || item.sourceId` 两层 fallback
- `item.sopId` 有则带，无则不带（execute.vue 内部会补全）

**问题**：
1. **⚠️ `relativeTime` 未在 methods 中声明**：直接在 methods 中引用 `relativeTime`，在 Vue 2 中可能报 undefined（但实际能工作是因为 Options API 的方法解析机制）
2. **`reminder` 类型通知点击无响应**：`handleNotification` 只处理 `execution` 和 `instance` 两种 sourceType，收到 reminder 通知点击后什么都不做
3. **TabBar 无未读数量 Badge**：通知 tab 图标没有小红点

---

### 3.5 我的 mine.vue ⭐⭐⭐

**职责**：用户信息 + 设置入口

**问题**：
1. **用户信息用原生 `uni.request`**：而非封装好的 `api`，401 处理逻辑与 api 层不一致
2. **编辑资料功能未实现**：`showToast('功能开发中')`
3. **关于我们硬编码**：`showModal` 内容写在代码里
4. **无待同步草稿的 Tab 入口**：草稿入口只在有待处理时显示，不够显著

---

### 3.6 离线草稿 drafts.vue ⭐⭐⭐⭐

**职责**：展示、同步、删除本地草稿

**亮点**：
- 单条同步 + 全部同步
- 重试计数显示（retryCount）
- 删除确认弹窗
- computed 实时分流 synced / pending

**问题**：
1. **`syncing` 状态覆盖所有操作**：点一条同步时，整个页面按钮都 disabled，体验略粗糙
2. **草稿按 executionId 分组展示，但没有 SOP 名称**：用户看到"执行单 #123"不知道是什么 SOP

---

## 四、共性问题

### 4.1 API 层（api/index.js）

| 问题 | 严重度 | 说明 |
|------|--------|------|
| BASE_URL 硬编码 `192.168.31.104:8013` | 🔴高 | 打包后必须改，应改用 `VITE_API_BASE` 环境变量 |
| 无统一错误处理 | 🟡中 | 每个接口失败只有 `console.error`，无 toast |
| 无响应拦截器 | 🟡中 | 没有 `res.data.data` 统一 unwrap |
| 401 跳转锁设计好 | ✅ 好 | `isRedirecting` 防重复跳转 |
| 无请求超时统一配置 | 🟢低 | 硬编码 30000ms |

### 4.2 网络状态管理（common/utils.js）

```javascript
// ⚠️ onNetworkChange 没有 off 方法，只能注册一次
export function onNetworkChange(callback) {
  uni.onNetworkStatusChange((res) => {
    callback(res.isConnected)
  })
}
```

index.vue 用标志位解决了这个问题（只在第一次加载时注册），但这是临时方案。长期需要全局 store 管理。

### 4.3 硬编码清单

| 位置 | 硬编码内容 |
|------|-----------|
| api/index.js:6 | `BASE_URL = 'http://192.168.31.104:8013'` |
| pages.json | `navigationBarBackgroundColor: '#4A90E2'`（5处）|
| pages.json | `enablePullDownRefresh: true`（4处）|
| mine.vue | 版本号 "v1.0.0" 硬编码在 showModal 里 |

---

## 五、关键待确认问题

### 5.1 需要问后端

| # | 问题 |
|---|------|
| 1 | `api.instance.activate()` 返回值是什么？`status` 和 `currentStep` 是多少？ |
| 2 | `api.instance/my` 返回的 `id` 字段：是 instanceId 还是 executionId？ |
| 3 | instance 模式下异常上报走哪个接口？后端有没有 `instance/{id}/exception`？ |
| 4 | `stepStatuses` 字段结构：`{"1": "completed", "2": "pending"}` 还是数组？ |
| 5 | records 页 `myInstances('abnormal')` 的 abnormal 状态值是什么？ |

### 5.2 前端待确认

| # | 问题 |
|---|------|
| 6 | `relativeTime` 在 notifications.vue 里直接用是否会报 lint 警告？ |
| 7 | TabBar 通知图标小红点：是要在 app.vue 手动实现还是用 UniApp 已读通知数？ |
| 8 | 草稿同步成功后，后端是否自动清理旧步骤数据防止重复打卡？ |

---

## 六、优化建议（按优先级）

### P0 — 影响核心流程

| 优先级 | 问题 | 解决方案 |
|--------|------|----------|
| P0 | BASE_URL 硬编码 | 改用 `import.meta.env.VITE_API_BASE`，开发/生产分离 |
| P0 | 异常上报接口错误 | 确认后端接口，补全 `api.instance.reportException` |
| P0 | records 跳转 ID 类型 | 确认 `item.id` 是 instanceId，避免 404 |

### P1 — 体验改进

| 优先级 | 问题 | 解决方案 |
|--------|------|----------|
| P1 | TabBar 通知无未读 Badge | 在 app.vue 或 mixin 中监听通知数，动态渲染 tabBar |
| P1 | records 无分页 | 增加 `pageNum`/`pageSize` 参数，滚动加载 |
| P1 | reminder 类型通知点击无响应 | 补全 `handleNotification` 中 reminder case |
| P1 | mine.vue 用原生 request | 统一走 api 层 |
| P1 | 草稿页无 SOP 名称 | 草稿结构增加 sopTitle 字段，execute.vue 填充 |

### P2 — 架构改进

| 优先级 | 问题 | 解决方案 |
|--------|------|----------|
| P2 | 网络监听泄漏隐患 | 新增全局 networkStore，提供 `on`/`off` 接口 |
| P2 | SOP 详情无超时兜底 | `Promise.all` 加 `race` 超时控制，单个失败不阻塞 |
| P2 | 无执行完成总结页 | 移植 PC 端 GrowthCelebration 到移动端 |
| P2 | 草稿无 SOP 名称 | store 增加 sopTitle 缓存 |
| P2 | 抽取全局错误处理 | 减少各页面 try/catch 重复代码 |
| P2 | 工具函数未做 tree-shaking | utils.js 按需引入，减少打包体积 |

---

## 七、架构图

```
┌─────────────────────────────────────────────────────────────┐
│              UniApp 移动端（H5 模式）                         │
│                                                             │
│  pages.json                                                 │
│  ┌─────────────────────────────────────────────────────┐   │
│  │ tabBar: 首页 | 记录 | 通知 | 我的                    │   │
│  │                                                     │   │
│  │  index.vue ──→ execute.vue ←─ draftStore           │   │
│  │  records.vue ←─ api.instance.my()                   │   │
│  │  notifications.vue ←─ api.notification.list()         │   │
│  │  mine.vue ←─ api.auth.me()                          │   │
│  │  drafts.vue ←─ draftStore                           │   │
│  └─────────────────────────────────────────────────────┘   │
│                    │                                        │
│  ┌─────────────────┼─────────────────┐                    │
│  │ store/auth.js   │ store/draft.js  │ api/index.js      │
│  │ (Pinia)         │ (Pinia+本地)     │ (uni.request)     │
│  │                 │                  │                   │
│  │ token/userId    │ drafts[]         │ 401锁(isRedirect) │
│  │ login/logout    │ syncAll/syncOne  │ BASE_URL(硬编码⚠️)│
│  └─────────────────┴─────────────────┴───────────────────┘  │
└─────────────────────────────────────────────────────────────┘
                         │ HTTP
┌─────────────────────────▼──────────────────────────────────┐
│              后端 Spring Boot                                │
│  SopInstanceService  SopExecutionService                     │
│  NotificationService  ExecutionStatService                   │
└─────────────────────────────────────────────────────────────┘
```

---

## 八、关键代码片段

### 8.1 激活防重入（execute.vue）

```javascript
data() {
  return {
    activating: false, // 防重入锁
  }
}

// loadInstanceDetail():
if (instData.status === 'pending' && !this.activating) {
  this.activating = true
  try {
    await api.instance.activate(this.instanceId)
    instData.status = 'in_progress'
    instData.currentStep = 1
  } finally {
    this.activating = false
  }
}
```

### 8.2 打卡失败兜底草稿（execute.vue）

```javascript
async handleCheckIn(step) {
  const isOnline = await checkNetwork()
  try {
    if (isOnline) {
      // 在线打卡
      await api.instance.completeStep(...)
      step.status = 'completed'
    }
  } catch (e) {
    // 在线打卡失败，也落入草稿兜底！
    const draftStore = useDraftStore()
    draftStore.addDraft(this.instanceId, step.index, photo, this.stepNote)
    step.status = 'pending_sync'
  }
}
```

### 8.3 并发 SOP 详情请求（index.vue）

```javascript
const sopMap = {}
await Promise.all(
  sopIds.map(async (sopId) => {
    try {
      const r = await api.sop.detail(sopId)
      if (r.code === 200) sopMap[sopId] = r.data
    } catch {}
  })
)
```

### 8.4 401 防重入跳转锁（api/index.js）

```javascript
let isRedirecting = false
function handle401() {
  if (isRedirecting) return
  isRedirecting = true
  uni.removeStorageSync('token')
  uni.reLaunch({
    url: '/pages/login/login',
    complete: () => { setTimeout(() => { isRedirecting = false }, 1500) }
  })
}
```

---

## 九、总结评分

| 维度 | 评分 | 说明 |
|------|------|------|
| 功能完整性 | ⭐⭐⭐⭐ | 核心打卡流程完整，离线草稿、异常上报、记录统计均有 |
| 工程质量 | ⭐⭐⭐⭐ | 防重入、草稿兜底、并发请求等工程细节处理较好 |
| 代码规范 | ⭐⭐⭐ | Options API + 部分硬编码 + 小部分重复代码 |
| 架构设计 | ⭐⭐⭐ | 简单直接，无分包，store 设计合理但缺少全局网络管理 |
| 移动端体验 | ⭐⭐⭐ | TabBar 无 Badge、草稿页缺 SOP 名称、无完成庆祝页 |
| 安全性 | ⭐⭐⭐ | BASE_URL 硬编码是最大隐患 |
| **综合** | **3.5/5** | **生产可用，工程亮点掩盖了一些细节问题** |
