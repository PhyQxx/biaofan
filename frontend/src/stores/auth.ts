import { defineStore } from 'pinia'
import request from '@/api'
import { ref } from 'vue'
import type { UserInfo } from '@/types'

export const useAuthStore = defineStore('auth', () => {
  const token = ref(localStorage.getItem('bf_token') || '')
  const userInfo = ref<UserInfo | null>(null)

  async function login(phone: string, password: string) {
    const res = await request.post<unknown, { code: number; message: string; data: { token: string } }>('/auth/login', { phone, password })
    if (res.code === 200) {
      token.value = res.data.token
      localStorage.setItem('bf_token', res.data.token)
      // Fetch user info after login (M-18)
      await fetchMe()
    } else {
      throw new Error(res.message || '登录失败')
    }
    return res
  }

  async function register(data: { phone: string; password: string; username: string }) {
    return request.post('/auth/register', data)
  }

  async function fetchMe() {
    try {
      const res = await request.get<unknown, { code: number; data: UserInfo }>('/auth/me')
      if (res.code === 200) {
        userInfo.value = res.data
        // Persist userId for components that need it
        if (res.data?.id) {
          localStorage.setItem('bf_user_id', String(res.data.id))
        }
      }
    } catch (e) {
      // M-19: Add logging instead of silently swallowing
      console.error('[auth] fetchMe failed:', e)
    }
  }

  function logout() {
    token.value = ''
    userInfo.value = null
    localStorage.removeItem('bf_token')
    localStorage.removeItem('bf_user_id')
  }

  /** Get current userId from store or localStorage fallback */
  function getUserId(): string {
    if (userInfo.value?.id) return String(userInfo.value.id)
    return localStorage.getItem('bf_user_id') || ''
  }

  /** Require userId - redirect to login if not available */
  function requireUserId(): string | null {
    const id = getUserId()
    if (!id) return null
    return id
  }

  return { token, userInfo, login, register, fetchMe, logout, getUserId, requireUserId }
})
