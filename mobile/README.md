# 标帆 SOP 移动端 (sop-mobile)

> 标帆 SOP 移动执行台 - UniApp + Vue3 实现

## 📱 项目概述

基于 UniApp + Vue3 开发的 SOP 移动端应用，让执行人员在手机端完成 SOP 任务接收、步骤打卡、异常上报。

## 🛠 技术栈

- **框架**: UniApp + Vue3
- **状态管理**: Pinia
- **HTTP**: uni.request
- **离线存储**: uni.setStorageSync (MVP)
- **推送**: uniPush (基于个推)

## 📁 项目结构

```
sop-mobile/
├── src/
│   ├── pages/              # 页面目录
│   │   ├── login/         # 登录页
│   │   ├── index/         # 首页（待办列表）
│   │   ├── execute/       # 执行台（步骤打卡）
│   │   ├── records/       # 执行记录
│   │   ├── notifications/ # 通知列表
│   │   └── mine/          # 个人中心
│   ├── store/             # Pinia 状态管理
│   │   ├── auth.js        # 认证状态
│   │   └── draft.js       # 离线草稿状态
│   ├── api/               # API 层
│   │   └── index.js       # 接口封装
│   ├── common/            # 公共模块
│   │   └── utils.js       # 工具函数
│   ├── static/            # 静态资源（tabBar图标）
│   ├── pages.json         # UniApp 页面配置
│   ├── manifest.json      # UniApp 应用配置
│   ├── App.vue            # 应用入口
│   ├── main.js            # 主入口
│   └── uni.scss           # 全局样式
├── index.html             # Vite 入口
├── vite.config.js         # Vite 配置
├── package.json           # 依赖配置
├── docker-compose.yml     # Docker 开发环境
└── README.md
```

## 🚀 快速开始

### 环境要求

- Node.js >= 18
- HBuilderX (可选，用于 App 打包)
- Android Studio / Xcode (用于原生 App 打包)

### 安装依赖

```bash
npm install
```

### 开发模式

```bash
# H5 开发预览
npm run dev:h5

# 微信小程序
npm run dev:mp-weixin

# App 开发（需要 HBuilderX）
npm run dev:app
```

### 生产构建

```bash
# H5 构建
npm run build:h5

# App 构建（输出到 unpackage 目录）
npm run build:app
```

## 📦 Docker 部署

### 开发环境

```bash
docker-compose up -d
```

访问 http://localhost:8080

## 🔌 API 配置

后端 API 地址在以下位置配置：

- `src/api/index.js` - BASE_URL
- `src/manifest.json` - h5.devServer.proxy (开发代理)
- `vite.config.js` - server.proxy

默认后端地址：`http://192.168.31.104:8013`

## 📲 功能列表

### P0 必须功能 ✅ 已实现

- [x] 手机号 + 验证码登录
- [x] 待执行 SOP 列表（首页）
- [x] 进行中 SOP 列表（首页 Tab 切换）
- [x] SOP 执行台（步骤展示 + 打卡）
- [x] 步骤打卡（支持拍照 + 备注）
- [x] 执行记录查看
- [x] 消息通知列表
- [x] 离线草稿（网络恢复自动同步）
- [x] 异常上报（拍照 + 描述）
- [x] 个人中心

### P1 可选功能 🔲 待开发

- [ ] GPS 打卡
- [ ] 拍照上传附件
- [ ] 个人资料编辑
- [ ] iOS TestFlight

## 🔒 认证

移动端使用与 Web 端相同的 JWT Token 认证，Token 存储在 localStorage 中。

## 📝 开发注意事项

1. **API 调用**: 所有接口调用通过 `src/api/index.js` 封装
2. **离线处理**: 打卡操作会优先检查网络状态，离线时自动保存草稿
3. **图片上传**: 使用 `uni.uploadFile` 进行图片上传
4. **推送**: uniPush 需要在 manifest.json 中配置 AppID

## 🔗 相关链接

- GitHub: https://github.com/PhyQxx/sop-mobile
- 后端 API: http://192.168.31.104:8013

## 📄 License

Private - All Rights Reserved
