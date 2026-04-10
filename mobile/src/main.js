
/**
 * UniApp 应用入口
 * - createSSRApp(App)
 * - 挂载全局 store（Pinia）
 */

import { createSSRApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'

export function createApp() {
  const app = createSSRApp(App)
  const pinia = createPinia()
  app.use(pinia)
  return {
    app
  }
}
