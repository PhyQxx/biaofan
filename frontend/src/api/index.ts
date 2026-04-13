
/**
 * Axios 实例封装
 * - baseURL: /api
 * - 请求拦截器：自动附加 Authorization: Bearer ***
 * - 响应拦截器：
 *   - 2xx: 解包 data，直接返回
 *   - 401: 尝试自动续期（token 可解析出 userId）
 *   - 其他: 弹出错误提示
 * - 各模块 API 函数：auth / sop / execution / gamification / admin / stats / marketplace
 */

// 引入 axios HTTP 请求库
import axios from 'axios'
// 引入 Element Plus 的 Message 组件，用于显示提示信息
import { ElMessage } from 'element-plus'
// 引入路由实例，用于处理 401 未授权时跳转到登录页
import router from '@/router'
// 引入 token 工具函数
import { getToken, setToken, removeToken, isTokenValidatable } from '@/utils/auth'

// 创建 axios 请求实例，配置基础路径和超时时间
const request = axios.create({
  baseURL: '/api',  // API 请求的基础 URL
  timeout: 10000,  // 请求超时时间：10秒
})

// 标记是否正在刷新 token，避免并发重复刷新
let isRefreshing = false
// 等待刷新完成的请求队列
let refreshSubscribers: Array<(token: string) => void> = []

function subscribeTokenRefresh(cb: (token: string) => void) {
  refreshSubscribers.push(cb)
}

function onTokenRefreshed(newToken: string) {
  refreshSubscribers.forEach(cb => cb(newToken))
  refreshSubscribers = []
}

// 请求拦截器：在每个请求发送前执行
request.interceptors.request.use((config) => {
  // 从 localStorage 中获取 token
  const token = getToken()
  if (token) {
    // 如果存在 token，将其添加到请求头的 Authorization 字段
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// 响应拦截器
request.interceptors.response.use(
  // 成功响应：直接返回 response.data
  (response) => response.data,
  // 错误响应：统一处理错误逻辑
  async (error) => {
    const originalRequest = error.config

    // 尝试自动续期：401 且 token 仍可解析出 userId
    if (
      error.response?.status === 401 &&
      !originalRequest._retry &&
      isTokenValidatable()
    ) {
      if (isRefreshing) {
        // 已有刷新请求进行中，将当前请求加入队列
        return new Promise(resolve => {
          subscribeTokenRefresh(newToken => {
            originalRequest.headers.Authorization = `Bearer ${newToken}`
            resolve(request(originalRequest))
          })
        })
      }

      originalRequest._retry = true
      isRefreshing = true

      try {
        // 调用后端 /api/auth/refresh 接口换新 token
        const res = await request.post<{ code: number; data?: { token: string }; message?: string }>(
          '/auth/refresh',
          {},
          { headers: { Authorization: `Bearer ${getToken()}` } }
        ) as any as { code: number; data?: { token: string }; message?: string }
        const newToken = res?.data?.token
        if (newToken) {
          setToken(newToken)
          originalRequest.headers.Authorization = `Bearer ${newToken}`
          onTokenRefreshed(newToken)
          // 重试当前请求
          return request(originalRequest)
        } else {
          removeToken()
          router.push('/login')
          return Promise.reject(error)
        }
      } catch {
        removeToken()
        router.push('/login')
        return Promise.reject(error)
      } finally {
        isRefreshing = false
      }
    }

    // 非自动续期路径的 401：清除 token 并跳转登录
    if (error.response?.status === 401) {
      removeToken()
      localStorage.removeItem('bf_user_id')
      router.push('/login')
    } else {
      ElMessage.error(error.response?.data?.message || '请求失败')
    }
    return Promise.reject(error)
  }
)

// 导出配置好的 axios 实例，供项目中发起 API 请求使用
export default request
