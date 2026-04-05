# 标帆 SOP 系统

标帆 SOP — 企业标准化操作流程管理系统，同时支持 PC Web 和移动端（UniApp）。

## 项目结构

```
biaofan/
├── backend/          # SpringBoot 3.2 后端（Java 17）
│   ├── src/
│   └── pom.xml
├── frontend/         # Vue3 + Vite PC 前端
│   ├── src/
│   └── package.json
├── mobile/          # UniApp 移动端（支持 H5 / 小程序 / Android）
│   ├── src/
│   └── package.json
└── docker/          # Docker 部署配置
```

## 技术栈

| 端 | 技术 |
|----|------|
| 后端 | SpringBoot 3.2 / MySQL / Redis / JWT |
| PC 前端 | Vue3 + Vite + Element Plus |
| 移动端 | UniApp + Vue3 + Pinia |

## 快速启动

### 后端

```bash
cd backend
mvn spring-boot:run
# 端口：8013
```

### PC 前端

```bash
cd frontend
npm install
npm run dev
# 端口：8011
```

### 移动端 H5

```bash
cd mobile
npm install
npm run dev:h5
# 端口：8080
```

### 移动端小程序

```bash
cd mobile
npm run build:mp-weixin
# 产物在 dist/build/mp-weixin/
# 导入微信开发者工具
```

## API 地址

- 开发环境后端：http://localhost:8013
- PC 前端：http://localhost:8011
- 移动端 H5：http://localhost:8080

## 测试账号

- 手机号：`13800138000`
- 密码：`test123`

## GitHub

https://github.com/PhyQxx/biaofan
