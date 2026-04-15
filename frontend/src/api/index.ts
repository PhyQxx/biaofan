
/**
 * Axios 实例封装
 * - baseURL: /api
 * - 请求拦截器：自动附加 Authorization: Bearer {token}
 * - 响应拦截器：
 *   - 2xx: 解包 data，直接返回
 *   - 401: 清除 bf_token，跳转 /login
 *   - 其他: 弹出错误提示
 * - 各模块 API 函数：auth / sop / execution / gamification / admin / stats / marketplace
 */

// 引入 axios HTTP 请求库
import axios from 'axios'
// 引入 Element Plus 的 Message 组件，用于显示提示信息
import { ElMessage } from 'element-plus'
// 引入路由实例，用于处理 401 未授权时跳转到登录页
import router from '@/router'

// 创建 axios 请求实例，配置基础路径和超时时间
const request = axios.create({
  baseURL: '/api',  // API 请求的基础 URL
  timeout: 100000,  // 请求超时时间：10秒
})

// 请求拦截器：在每个请求发送前执行
request.interceptors.request.use((config) => {
  // 从 localStorage 中获取 token
  const token = localStorage.getItem('bf_token')
  if (token) {
    // 如果存在 token，将其添加到请求头的 Authorization 字段
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// 响应拦截器：在收到响应后执行
request.interceptors.response.use(
  // 成功响应：直接返回 response.data
  (response) => response.data,
  // 错误响应：统一处理错误逻辑
  (error) => {
    // 如果是 401 未授权错误（token 过期或无效）
    if (error.response?.status === 401) {
      // 清除本地存储的 token 和 user_id
      localStorage.removeItem('bf_token')
      localStorage.removeItem('bf_user_id')
      // 跳转到登录页面
      router.push('/login')
    } else {
      // 其他错误：显示错误提示信息
      ElMessage.error(error.response?.data?.message || '请求失败')
    }
    // 返回被拒绝的 Promise，便于调用方捕获错误
    return Promise.reject(error)
  }
)

// 导出配置好的 axios 实例，供项目中发起 API 请求使用
export default request
