/**
 * 认证状态管理
 */
import { defineStore } from 'pinia'
import api from '../api'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: uni.getStorageSync('token') || '',
    userId: uni.getStorageSync('userId') || '',
    userInfo: (() => {
      try {
        const raw = uni.getStorageSync('userInfo')
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

          uni.setStorageSync('token', this.token)
          uni.setStorageSync('userId', this.userId)
          uni.setStorageSync('userInfo', JSON.stringify(this.userInfo))

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

      uni.removeStorageSync('token')
      uni.removeStorageSync('userId')
      uni.removeStorageSync('userInfo')

      // 调用后端登出
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
