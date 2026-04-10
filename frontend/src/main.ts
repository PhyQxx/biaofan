
/**
 * Vue 应用入口
 * - createApp(App)
 * - 全局注册 Element Plus
 * - 全局注册 ECharts（vue-echarts）
 * - 挂载 Pinia（状态管理）
 * - 挂载 Vue Router
 * - 挂载 App 组件到 #app
 */

// 引入 Vue 3 的 createApp 函数，用于创建应用实例
import { createApp } from 'vue'
// 引入 Element Plus UI 组件库
import ElementPlus from 'element-plus'
// 引入 Element Plus 的默认样式文件
import 'element-plus/dist/index.css'
// 引入路由配置
import router from './router'
// 引入 Pinia 状态管理库（Vue 3 推荐的全局状态管理方案）
import { createPinia } from 'pinia'
// 引入根组件 App.vue
import App from './App.vue'

// 创建 Vue 应用实例
const app = createApp(App)
// 注册 Pinia 状态管理插件
app.use(createPinia())
// 注册路由插件
app.use(router)
// 注册 Element Plus UI 组件库插件
app.use(ElementPlus)
// 将应用挂载到 #app 元素上
app.mount('#app')
