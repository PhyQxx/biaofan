<template>
  <view id="app"></view>
</template>

<script>


/**
 * UniApp 应用根组件
 * - createSSRApp 兼容 SSR 写法
 * - 渲染 pages.json 中声明的页面
 */
import { onLaunch, onShow, onHide } from '@dcloudio/uni-app'
import { useAuthStore } from './store/auth'
import { useDraftStore } from './store/draft'
import api from './api'

export default {
  onLaunch() {
    console.log('App Launch')
    // 初始化推送
    this.initPush()
  },
  onShow() {
    console.log('App Show')
    // 检查登录状态
    const auth = useAuthStore()
    if (!auth.isLoggedIn) {
      uni.reLaunch({ url: '/pages/login/login' })
      return
    }
    // 同步离线草稿（移到 onShow，确保 Pinia 已初始化）
    this.syncDrafts()
  },
  onHide() {
    console.log('App Hide')
  },
  methods: {
    initPush() {
      // #ifdef APP-PLUS
      const push = uni.requireNativePlugin('uni-push')
      push.init({
        appid: '__UNI__SOPMOBILE',
        debug: false,
        sound: 'default',
        vibrate: true
      }, (res) => {
        console.log('Push init result:', res)
        // 获取客户端 cid
        push.getClientId((cid) => {
          console.log('Push CID:', cid)
          // 将 CID 注册到后端（使用统一 api 模块）
          const auth = useAuthStore()
          if (auth.token) {
            api.push.registerCid(cid).catch((e) => {
              console.error('CID 注册失败:', e)
            })
          }
        })
        // 监听消息
        push.on('notification', (notif) => {
          console.log('Push notification:', notif)
          uni.showToast({ title: notif.title || '新通知', icon: 'none' })
        })
      })
      // #endif
    },
    syncDrafts() {
      const draftStore = useDraftStore()
      if (draftStore.hasPendingDrafts) {
        draftStore.syncAll()
      }
    }
  }
}
</script>

<style>
@import './uni.scss';
page {
  background-color: #F5F5F5;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
}
</style>
