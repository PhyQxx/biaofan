/**
 * 认证状态管理模块
 * 负责用户登录、注册、登出、获取用户信息等认证相关功能
 */
import { defineStore } from 'pinia'
import request from '@/api'
import { ref } from 'vue'
import type { UserInfo } from '@/types'

export const useAuthStore = defineStore('auth', () => {
  // token: 存储用户登录凭证，从 localStorage 初始化
  const token = ref(localStorage.getItem('bf_token') || '')
  // userInfo: 存储当前登录用户的信息
  const userInfo = ref<UserInfo | null>(null)

  /**
   * 用户登录
   * @param phone 手机号
   * @param password 密码
   * @returns 登录结果，包含 token 和用户信息
   */
  async function login(phone: string, password: string) {
    const res = await request.post<unknown, { code: number; message: string; data: { token: string } }>('/auth/login', { phone, password })
    if (res.code === 200) {
      token.value = res.data.token
      localStorage.setItem('bf_token', res.data.token)
      // 登录成功后获取当前用户信息 (M-18)
      await fetchMe()
    } else {
      throw new Error(res.message || '登录失败')
    }
    return res
  }

  /**
   * 用户注册
   * @param data 包含 phone、password、username 的注册信息
   * @returns 注册结果
   */
  async function register(data: { phone: string; password: string; username: string }) {
    return request.post('/auth/register', data)
  }

  /**
   * 获取当前登录用户信息
   * 将用户信息存储到 store 和 localStorage 中
   */
  async function fetchMe() {
    try {
      const res = await request.get<unknown, { code: number; data: UserInfo }>('/auth/me')
      if (res.code === 200) {
        userInfo.value = res.data
        // 将 userId 持久化到 localStorage，供其他组件使用
        if (res.data?.id) {
          localStorage.setItem('bf_user_id', String(res.data.id))
        }
      }
    } catch (e) {
      // M-19: 添加错误日志而非静默捕获
      console.error('[auth] fetchMe failed:', e)
    }
  }

  /**
   * 用户登出
   * 清除 token、用户信息以及 localStorage 中的相关数据
   */
  function logout() {
    token.value = ''
    userInfo.value = null
    localStorage.removeItem('bf_token')
    localStorage.removeItem('bf_user_id')
  }

  /**
   * 获取当前用户 ID
   * 优先从 store 获取，若无则从 localStorage 降级获取
   * @returns 用户 ID 字符串，若无则为空字符串
   */
  function getUserId(): string {
    if (userInfo.value?.id) return String(userInfo.value.id)
    return localStorage.getItem('bf_user_id') || ''
  }

  /**
   * 获取当前用户 ID，若无则返回 null
   * @returns 用户 ID 或 null
   */
  function requireUserId(): string | null {
    const id = getUserId()
    if (!id) return null
    return id
  }

  return { token, userInfo, login, register, fetchMe, logout, getUserId, requireUserId }
})
