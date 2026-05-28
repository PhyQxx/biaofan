import type { ApiResponse } from '@/types'
import request from './index'

// ============ 徽章管理 ============
/**
 * 徽章实体
 * 用于用户成就系统中的徽章定义
 */
export interface Badge {
  id?: number
  badgeKey: string          // 徽章唯一标识
  name: string              // 徽章名称
  icon: string              // 徽章图标URL
  rarity: 'bronze' | 'silver' | 'gold'  // 稀有度等级
  description: string          // 徽章描述
  condition: string            // 解锁条件
  rewardExp: number            // 经验奖励
  rewardScore: number          // 积分奖励
  createdAt?: string           // 创建时间
}

/**
 * 徽章列表响应
 */
export interface BadgeListResp {
  list: Badge[]
  total: number
}

/**
 * 获取徽章列表
 * @returns 徽章列表及总数
 */
export const getBadgeList = () =>
  request.get<unknown, ApiResponse<BadgeListResp>>('/admin/gamification/badges')

/**
 * 创建徽章
 * @param data 徽章信息
 */
export const createBadge = (data: Badge) =>
  request.post<Badge, ApiResponse<void>>('/admin/gamification/badges', data)

/**
 * 更新徽章
 * @param id 徽章ID
 * @param data 徽章信息
 */
export const updateBadge = (id: number, data: Badge) =>
  request.put<Badge, ApiResponse<void>>(`/admin/gamification/badges/${id}`, data)

/**
 * 删除徽章
 * @param id 徽章ID
 */
export const deleteBadge = (id: number) =>
  request.delete<number, ApiResponse<void>>(`/admin/gamification/badges/${id}`)

// ============ 商品管理 ============
/**
 * 商品实体
 * 用于商城系统中的商品定义
 */
export interface Product {
  id?: number
  name: string                                // 商品名称
  icon: string                                // 商品图标URL
  description: string                          // 商品描述
  price: number                               // 价格（积分）
  stock: number                               // 库存
  active: boolean                              // 状态：是否启用
  createdAt?: string                          // 创建时间
}

/**
 * 商品列表响应
 */
export interface ProductListResp {
  list: Product[]
  total: number
}

/**
 * 获取商品列表
 * @returns 商品列表及总数
 */
export const getProductList = () =>
  request.get<unknown, ApiResponse<ProductListResp>>('/admin/gamification/products')

/**
 * 创建商品
 * @param data 商品信息
 */
export const createProduct = (data: Product) =>
  request.post<Product, ApiResponse<void>>('/admin/gamification/products', data)

/**
 * 更新商品
 * @param id 商品ID
 * @param data 商品信息
 */
export const updateProduct = (id: number, data: Product) =>
  request.put<Product, ApiResponse<void>>(`/admin/gamification/products/${id}`, data)

/**
 * 删除商品
 * @param id 商品ID
 */
export const deleteProduct = (id: number) =>
  request.delete<number, ApiResponse<void>>(`/admin/gamification/products/${id}`)

// ============ AI 模型配置 ============
export interface AiModelConfig {
  id?: number
  userId?: number | null
  modelType: string
  apiUrl?: string
  apiKey?: string
  modelName?: string
  systemPrompt?: string
  enabled?: boolean
  temperature?: number
  createdAt?: string
  updatedAt?: string
}

export const getGlobalAiConfigs = () =>
  request.get<any, ApiResponse<{ list: AiModelConfig[]; total: number }>>('/admin/ai-config/global')

export const getUserAiConfigs = () =>
  request.get<any, ApiResponse<{ list: AiModelConfig[]; total: number }>>('/admin/ai-config/users')

export const saveGlobalAiConfig = (data: AiModelConfig) =>
  request.post<AiModelConfig, ApiResponse<void>>('/admin/ai-config/global', data)

export const deleteGlobalAiConfig = (id: number) =>
  request.delete<number, ApiResponse<void>>(`/admin/ai-config/global/${id}`)

// ============ 成长规则 ============
/**
 * 成长规则实体
 */
export interface GrowthRule {
  id?: number
  ruleType: string
  ruleKey: string
  ruleValue: string
  version?: number
  isActive?: boolean
  comment?: string
  createdAt?: string
  updatedAt?: string
}

/**
 * 获取成长规则列表
 * @returns 规则列表
 */
export const getGrowthRules = () =>
  request.get<any, ApiResponse<GrowthRule[]>>('/admin/gamification/rules')

/**
 * 更新成长规则
 * @param data 成长规则配置列表
 */
export const updateGrowthRules = (data: GrowthRule[]) =>
  request.put<GrowthRule[], ApiResponse<void>>('/admin/gamification/rules', data)


// ============ 邮件配置 ============
export interface EmailConfig {
  id?: number
  host: string
  port: number
  username: string
  password?: string
  fromAddress?: string
  smtpAuth?: boolean
  starttlsEnable?: boolean
  enabled?: boolean
  createdAt?: string
  updatedAt?: string
}

export const getEmailConfig = () =>
  request.get<any, ApiResponse<EmailConfig>>('/admin/email-config')

export const saveEmailConfig = (data: Partial<EmailConfig>) =>
  request.post<Partial<EmailConfig>, ApiResponse<void>>('/admin/email-config', data)

export const testEmailConfig = (to: string) =>
  request.post<{ to: string }, ApiResponse<void>>('/admin/email-config/test', { to })
