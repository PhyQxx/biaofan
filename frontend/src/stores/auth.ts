/**
 * 认证状态管理模块
 * 负责用户登录、注册、登出、获取用户信息等认证相关功能
 *
 * [安全说明 - JWT 存储方式]
 * 当前实现将 JWT 存储在 localStorage 中（key: 'bf_token'）。
 * 风险：localStorage 可被任何注入页面的 JavaScript 访问，存在 XSS 窃取 token 的风险。
 * 替代方案：使用 httpOnly Cookie 存储 JWT，由后端通过 Set-Cookie 设置。
 * 这需要后端较大改动（将 JWT 从 Authorization header 移到 Cookie）。
 * 当前为已知的架构限制，适合内部工具类应用使用。
 */
import { defineStore } from 'pinia'
import request from '@/api'
import { ref } from 'vue'
import type { UserInfo } from '@/types'

export const useAuthStore = defineStore('auth', () => {
  // token: 存储用户登录凭证，从 localStorage 初始化
  // 注意：此存储方式存在 XSS 风险，见上方安全说明
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
      const res = await request.get<unknown, { code: number; message?: string; data: UserInfo }>('/auth/me')
      if (res.code === 200) {
        userInfo.value = res.data
        // 将 userId 持久化到 localStorage，供其他组件使用
        if (res.data?.id) {
          localStorage.setItem('bf_user_id', String(res.data.id))
        }
      } else {
        // Non-200 responses (including 401 handled by interceptor) are re-thrown
        throw new Error(`fetchMe failed: ${res.message ?? 'unknown error'}`)
      }
    } catch (e: unknown) {
      // Re-throw 401 so router guard can distinguish invalid token from network error
      const err = e as { response?: { status?: number }; message?: string }
      const is401 = err?.response?.status === 401 || err?.message?.includes('401')
      if (is401) throw e
      // For other errors (network failure), log but don't throw — guard will handle
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
