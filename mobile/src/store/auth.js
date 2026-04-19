/**
 * 认证状态管理
 */
import { defineStore } from 'pinia'
import api from '../api'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: uni.getStorageSync('bf_token') || '',
    userId: uni.getStorageSync('bf_userId') || '',
    userInfo: (() => {
      try {
        // Load safe minimal user info only — never expose sensitive fields
        const raw = uni.getStorageSync('bf_safeUserInfo')
        return raw ? (typeof raw === 'string' ? JSON.parse(raw) : raw) : null
      } catch { return null }
    })()
  }),

  getters: {
    isLoggedIn: (state) => !!state.token
  },

  actions: {
    async login(phone, password) {
      try {
        const res = await api.auth.login({ phone, password })
        if (res.data && res.data.token) {
          this.token = res.data.token
          this.userId = res.data.userId || res.data.id
          this.userInfo = res.data.userInfo || {}

          uni.setStorageSync('bf_token', this.token)
          uni.setStorageSync('bf_userId', this.userId)
          // Store only minimal safe info (id, nickname, avatar) — NOT email/phone/sensitive fields
          const safeInfo = {
            id: this.userInfo?.id,
            nickname: this.userInfo?.nickname,
            avatar: this.userInfo?.avatar,
          }
          uni.setStorageSync('bf_safeUserInfo', JSON.stringify(safeInfo))

          return res
        }
        throw new Error('登录失败')
      } catch (e) {
        throw e
      }
    },

    async sendCode(phone) {
      return await api.auth.sendCode(phone)
    },

    logout() {
      this.token = ''
      this.userId = ''
      this.userInfo = null

      uni.removeStorageSync('bf_token')
      uni.removeStorageSync('bf_userId')
      uni.removeStorageSync('bf_safeUserInfo')
      uni.removeStorageSync('bf_userInfo')

      // 调用后端登出（忽略错误——登出是非关键操作）
      api.auth.logout().catch(() => {})

      uni.reLaunch({ url: '/pages/login/login' })
    },

    checkLogin() {
      if (!this.token) {
        uni.reLaunch({ url: '/pages/login/login' })
        return false
      }
      return true
    }
  }
})
