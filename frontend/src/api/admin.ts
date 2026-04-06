import request from './index'

// ============ Badge Management ============
export interface Badge {
  id?: number
  badge_id: string
  name: string
  icon: string
  rarity: 'common' | 'rare' | 'legendary'
  condition_type: string
  condition_param: string
  exp_reward: number
  score_reward: number
  status: number
  created_at?: string
  updated_at?: string
}

export interface BadgeListResp {
  list: Badge[]
  total: number
}

export const getBadgeList = () =>
  request.get<any, BadgeListResp>('/admin/gamification/badges')

export const createBadge = (data: Badge) =>
  request.post<any, any>('/admin/gamification/badges', data)

export const updateBadge = (id: number, data: Badge) =>
  request.put<any, any>(`/admin/gamification/badges/${id}`, data)

export const deleteBadge = (id: number) =>
  request.delete<any, any>(`/admin/gamification/badges/${id}`)

// ============ Product Management ============
export interface Product {
  id?: number
  name: string
  category: 'title' | 'avatar_frame' | 'background' | 'emotion'
  icon: string
  price: number
  stock: number
  valid_days: number
  description: string
  status: number
  created_at?: string
  updated_at?: string
}

export interface ProductListResp {
  list: Product[]
  total: number
}

export const getProductList = () =>
  request.get<any, ProductListResp>('/admin/gamification/products')

export const createProduct = (data: Product) =>
  request.post<any, any>('/admin/gamification/products', data)

export const updateProduct = (id: number, data: Product) =>
  request.put<any, any>(`/admin/gamification/products/${id}`, data)

export const deleteProduct = (id: number) =>
  request.delete<any, any>(`/admin/gamification/products/${id}`)

// ============ Growth Rules ============
export interface LevelRule {
  id?: number
  rule_key: string
  rule_name: string
  level: number
  exp_required: number
  segment: string
}

export interface SegmentRule {
  id?: number
  rule_key: string
  rule_name: string
  segment: string
  min_exp: number
  max_exp: number
}

export interface ScoreRule {
  id?: number
  rule_key: string
  rule_name: string
  badge_id: string
  exp_reward: number
  score_reward: number
}

export interface GrowthRulesResp {
  level_exp: LevelRule[]
  segment_threshold: SegmentRule[]
  score_rule: ScoreRule[]
}

export const getGrowthRules = () =>
  request.get<any, GrowthRulesResp>('/admin/gamification/rules')

export const updateGrowthRules = (data: GrowthRulesResp) =>
  request.put<any, any>('/admin/gamification/rules', data)
