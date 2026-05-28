# 标帆 SOP 前端 (PC Web)

> 基于 Vue 3 + TypeScript + Vite + Element Plus 构建的企业 SOP 管理系统。

## 🌟 核心特性

- **响应式布局**: 适配不同屏幕尺寸的工作台。
- **SOP 编辑器**: 支持多级步骤、附件上传、富文本描述。
- **版本控制**: SOP 版本对比、发布与回滚。
- **执行看板**: 实时监控 SOP 执行进度与异常。
- **游戏化中心**: 个人成长等级、积分商城、荣誉徽章。
- **模板市场**: 行业标准 SOP 模板一键导入。
- **管理后台**: 全面的系统配置与内容审计。

## 🛠 技术栈

- **框架**: [Vue 3](https://vuejs.org/) (Composition API)
- **开发语言**: [TypeScript](https://www.typescriptlang.org/)
- **构建工具**: [Vite](https://vitejs.dev/)
- **UI 组件库**: [Element Plus](https://element-plus.org/)
- **状态管理**: [Pinia](https://pinia.vuejs.org/)
- **路由**: [Vue Router](https://router.vuejs.org/)
- **网络请求**: [Axios](https://axios-http.com/)
- **图标**: [Element Plus Icons](https://element-plus.org/en-US/component/icon.html)

## 📁 目录结构

```
frontend/
├── src/
│   ├── api/            # 接口定义 (Axios 封装)
│   ├── assets/         # 静态资源 (CSS, 图片, SVG)
│   ├── components/     # 公共组件 (common, gamification, ai 等)
│   ├── composables/    # 组合式函数 (hooks)
│   ├── router/         # 路由配置
│   ├── stores/         # Pinia 状态管理 (auth, sop, notification)
│   ├── types/          # TypeScript 类型声明
│   ├── views/          # 视图页面 (admin, sop, marketplace, profile 等)
│   ├── App.vue         # 根组件
│   ├── main.ts         # 入口文件
│   └── style.css       # 全局样式
├── tsconfig.json       # TypeScript 配置
├── vite.config.ts      # Vite 配置
└── package.json        # 项目依赖
```

## 🚀 快速启动

### 1. 安装依赖

```bash
npm install
```

### 2. 启动开发服务器

```bash
npm run dev
```

### 3. 类型检查与构建

```bash
# 类型检查
npm run type-check

# 构建生产产物
npm run build
```

## 🔌 API 配置

项目使用 Vite 的代理功能将 API 请求转发到后端服务器。

配置位于 `vite.config.ts`:

```typescript
server: {
  proxy: {
    '/api': {
      target: 'http://localhost:8013',
      changeOrigin: true,
    }
  }
}
```

## 📝 贡献指南

1. 遵循 TypeScript 严格模式。
2. 组件命名采用 PascalCase，文件命名采用 PascalCase 或 kebab-case。
3. 新增通用 UI 组件请放置于 `src/components/common`。
4. 业务逻辑优先封装在 `src/composables` 中以实现复用。

## 📄 License

Private - All Rights Reserved
