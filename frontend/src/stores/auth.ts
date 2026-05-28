/**
 * 认证状态管理模块
 * 负责用户登录、注册、登出、获取用户信息等认证相关功能
 */
import { defineStore } from 'pinia'
import request from '@/api'
import { ref, computed } from 'vue'
import type { UserInfo } from '@/types'
import { getMyOrganizations, type Organization } from '@/api/org'

export const useAuthStore = defineStore('auth', () => {
  const token = ref(localStorage.getItem('bf_token') || '')
  const userInfo = ref<UserInfo | null>(null)
  
  // 组织架构状态
  const userOrganizations = ref<Organization[]>([])
  const currentOrgId = ref<number | null>(
    localStorage.getItem('bf_curr_org_id') ? Number(localStorage.getItem('bf_curr_org_id')) : null
  )

  const currentOrg = computed(() => 
    userOrganizations.value.find(org => org.id === currentOrgId.value) || null
  )

  /**
   * 用户登录
   */
  async function login(phone: string, password: string) {
    const res = await request.post<unknown, { code: number; message: string; data: { token: string } }>('/auth/login', { phone, password })
    if (res.code === 200) {
      token.value = res.data.token
      localStorage.setItem('bf_token', res.data.token)
      await fetchMe()
      await fetchMyOrgs()
    } else {
      throw new Error(res.message || '登录失败')
    }
    return res
  }

  /**
   * 用户注册
   */
  async function register(data: { phone: string; password: string; username: string }) {
    return request.post('/auth/register', data)
  }

  /**
   * 获取当前登录用户信息
   */
  async function fetchMe() {
    try {
      const res = await request.get<unknown, { code: number; message?: string; data: UserInfo }>('/auth/me')
      if (res.code === 200) {
        userInfo.value = res.data
        if (res.data?.id) {
          localStorage.setItem('bf_user_id', String(res.data.id))
        }
      }
    } catch (e) {
      console.error('[auth] fetchMe failed:', e)
    }
  }

  /**
   * 获取我的组织列表
   */
  async function fetchMyOrgs() {
    try {
      const res = await getMyOrganizations()
      if (res.code === 200) {
        userOrganizations.value = res.data
        if (currentOrgId.value && !res.data.some(o => o.id === currentOrgId.value)) {
          switchOrg(null)
        }
      }
    } catch (e) {
      console.error('Failed to fetch orgs:', e)
    }
  }

  /**
   * 切换当前组织空间
   */
  function switchOrg(orgId: number | null) {
    currentOrgId.value = orgId
    if (orgId) {
      localStorage.setItem('bf_curr_org_id', String(orgId))
    } else {
      localStorage.removeItem('bf_curr_org_id')
    }
  }

  /**
   * 用户登出
   */
  function logout() {
    token.value = ''
    userInfo.value = null
    userOrganizations.value = []
    currentOrgId.value = null
    localStorage.removeItem('bf_token')
    localStorage.removeItem('bf_user_id')
    localStorage.removeItem('bf_curr_org_id')
  }

  function getUserId(): string {
    if (userInfo.value?.id) return String(userInfo.value.id)
    return localStorage.getItem('bf_user_id') || ''
  }

  function requireUserId(): string | null {
    const id = getUserId()
    if (!id) return null
    return id
  }

  return { 
    token, userInfo, userOrganizations, currentOrgId, currentOrg,
    login, register, fetchMe, fetchMyOrgs, switchOrg, logout, getUserId, requireUserId 
  }
})
