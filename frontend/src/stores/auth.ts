import { defineStore } from 'pinia'
import request from '@/api'
import { ref } from 'vue'

export const useAuthStore = defineStore('auth', () => {
  const token = ref(localStorage.getItem('bf_token') || '')
  const userInfo = ref<any>(null)

  async function login(phone: string, password: string) {
    const res: any = await request.post('/auth/login', { phone, password })
    if (res.code === 200) {
      token.value = res.data.token
      localStorage.setItem('bf_token', res.data.token)
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
      const res: any = await request.get('/auth/me')
      if (res.code === 200) userInfo.value = res.data
    } catch {}
  }

  function logout() {
    token.value = ''
    userInfo.value = null
    localStorage.removeItem('bf_token')
  }

  return { token, userInfo, login, register, fetchMe, logout }
})
