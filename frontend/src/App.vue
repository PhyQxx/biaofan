/**
 * 根组件
 * - RouterView 渲染路由页面
 * - Error boundary 捕获子组件渲染错误，防止整页崩溃
 */

<template>
  <router-view v-slot="{ Component }">
    <error-boundary>
      <component :is="Component" />
    </error-boundary>
  </router-view>
</template>

<script setup lang="ts">
import { ref, h, onErrorCaptured } from 'vue'

// Error boundary component using Vue 3 Composition API
// Catches errors from child components and displays fallback UI
const errorBoundary = {
  setup(_props: any, { slots }: any) {
    const error = ref<Error | null>(null)
    onErrorCaptured((e: Error) => {
      error.value = e
      console.error('[ErrorBoundary] Component error:', e)
      return false // Prevent error propagation
    })
    return () => error.value
      ? h('div', {
          class: 'error-fallback',
          style: 'padding: 40px; text-align: center; color: #666;'
        }, [
          h('div', { style: 'font-size: 48px; margin-bottom: 16px;' }, '😵'),
          h('div', { style: 'font-size: 18px; margin-bottom: 8px;' }, '页面加载失败'),
          h('div', { style: 'font-size: 14px; color: #999;' }, '请刷新页面重试'),
          h('button', {
            style: 'margin-top: 16px; padding: 8px 24px; background: #5B7FFF; color: #fff; border: none; border-radius: 6px; cursor: pointer;',
            onClick: () => { error.value = null; window.location.reload() }
          }, '刷新页面')
        ])
      : slots.default?.()
  }
}
</script>
