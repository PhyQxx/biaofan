import request from './index'
import type { ApiResponse } from '@/types'

export interface Organization {
  id: number
  name: string
  ownerId: number
  inviteCode?: string
  description?: string
  logoUrl?: string
  createdAt?: string
  updatedAt?: string
}

export interface OrgMember {
  userId: number
  username: string
  role: 'owner' | 'admin' | 'member'
  joinedAt: string
}

/**
 * 获取我的组织列表
 */
export const getMyOrganizations = () =>
  request.get<unknown, ApiResponse<Organization[]>>('/organizations/my')

/**
 * 创建组织
 */
export const createOrganization = (data: { name: string; description?: string; logoUrl?: string }) =>
  request.post<any, ApiResponse<Organization>>('/organizations', data)

/**
 * 加入组织
 */
export const joinOrganization = (inviteCode: string) =>
  request.post<any, ApiResponse<void>>('/organizations/join', { inviteCode })

/**
 * 获取组织成员
 */
export const getOrgMembers = (orgId: number) =>
  request.get<unknown, ApiResponse<OrgMember[]>>(`/organizations/${orgId}/members`)

/**
 * 刷新邀请码
 */
export const refreshInviteCode = (orgId: number) =>
  request.post<unknown, ApiResponse<{ inviteCode: string }>>(`/organizations/${orgId}/invite-code/refresh`)
