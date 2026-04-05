<template>
  <view id="app"></view>
</template>

<script>
import { onLaunch, onShow, onHide } from '@dcloudio/uni-app'
import { useAuthStore } from './store/auth'
import { useDraftStore } from './store/draft'

export default {
  onLaunch() {
    console.log('App Launch')
    // 初始化推送
    this.initPush()
    // 同步离线草稿
    this.syncDrafts()
  },
  onShow() {
    console.log('App Show')
    // 检查登录状态
    const auth = useAuthStore()
    if (!auth.isLoggedIn) {
      uni.reLaunch({ url: '/pages/login/login' })
    }
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
          // 将 CID 注册到后端
          const auth = useAuthStore()
          if (auth.token) {
            uni.request({
              url: `${auth.baseUrl}/api/push/register`,
              method: 'POST',
              header: { Authorization: `Bearer ${auth.token}` },
              data: { clientId: cid }
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
