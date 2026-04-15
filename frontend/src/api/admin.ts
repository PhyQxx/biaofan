import request from './index'

// ============ 徽章管理 ============
/**
 * 徽章实体
 * 用于用户成就系统中的徽章定义
 */
export interface Badge {
  id?: number
  badge_id: string          // 徽章唯一标识
  name: string              // 徽章名称
  icon: string              // 徽章图标URL
  rarity: 'common' | 'rare' | 'legendary'  // 稀有度等级
  condition_type: string    // 解锁条件类型
  condition_param: string   // 解锁条件参数
  exp_reward: number        // 经验奖励
  score_reward: number      // 积分奖励
  status: number            // 状态：0-禁用 1-启用
  created_at?: string       // 创建时间
  updated_at?: string       // 更新时间
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
  request.get<any, BadgeListResp>('/admin/gamification/badges')

/**
 * 创建徽章
 * @param data 徽章信息
 */
export const createBadge = (data: Badge) =>
  request.post<any, any>('/admin/gamification/badges', data)

/**
 * 更新徽章
 * @param id 徽章ID
 * @param data 徽章信息
 */
export const updateBadge = (id: number, data: Badge) =>
  request.put<any, any>(`/admin/gamification/badges/${id}`, data)

/**
 * 删除徽章
 * @param id 徽章ID
 */
export const deleteBadge = (id: number) =>
  request.delete<any, any>(`/admin/gamification/badges/${id}`)

// ============ 商品管理 ============
/**
 * 商品实体
 * 用于商城系统中的商品定义
 */
export interface Product {
  id?: number
  name: string                                // 商品名称
  category: 'title' | 'avatar_frame' | 'background' | 'emotion'  // 商品类别：称号、头像框、背景、表情
  icon: string                                // 商品图标URL
  price: number                               // 价格（积分）
  stock: number                               // 库存
  valid_days: number                          // 有效期（天）
  description: string                          // 商品描述
  status: number                              // 状态：0-禁用 1-启用
  created_at?: string                          // 创建时间
  updated_at?: string                          // 更新时间
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
  request.get<any, ProductListResp>('/admin/gamification/products')

/**
 * 创建商品
 * @param data 商品信息
 */
export const createProduct = (data: Product) =>
  request.post<any, any>('/admin/gamification/products', data)

/**
 * 更新商品
 * @param id 商品ID
 * @param data 商品信息
 */
export const updateProduct = (id: number, data: Product) =>
  request.put<any, any>(`/admin/gamification/products/${id}`, data)

/**
 * 删除商品
 * @param id 商品ID
 */
export const deleteProduct = (id: number) =>
  request.delete<any, any>(`/admin/gamification/products/${id}`)

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
  request.get<any, { list: AiModelConfig[]; total: number }>('/admin/ai-config/global')

export const getUserAiConfigs = () =>
  request.get<any, { list: AiModelConfig[]; total: number }>('/admin/ai-config/users')

export const saveGlobalAiConfig = (data: AiModelConfig) =>
  request.post<any, any>('/admin/ai-config/global', data)

export const deleteGlobalAiConfig = (id: number) =>
  request.delete<any, any>(`/admin/ai-config/global/${id}`)

// ============ 成长规则 ============
/**
 * 等级规则
 * 定义用户升级所需经验值
 */
export interface LevelRule {
  id?: number
  rule_key: string       // 规则key
  rule_name: string       // 规则名称
  level: number           // 等级
  exp_required: number    // 所需经验
  segment: string         // 阶段
}

/**
 * 段位规则
 * 定义用户段位划分
 */
export interface SegmentRule {
  id?: number
  rule_key: string       // 规则key
  rule_name: string       // 规则名称
  segment: string         // 段位
  min_exp: number         // 最小经验
  max_exp: number         // 最大经验
}

/**
 * 积分规则
 * 定义徽章对应积分奖励
 */
export interface ScoreRule {
  id?: number
  rule_key: string       // 规则key
  rule_name: string       // 规则名称
  badge_id: string        // 徽章ID
  exp_reward: number      // 经验奖励
  score_reward: number    // 积分奖励
}

/**
 * 成长规则响应
 * 包含所有成长相关规则
 */
export interface GrowthRulesResp {
  level_exp: LevelRule[]       // 等级经验规则
  segment_threshold: SegmentRule[]  // 段位阈值规则
  score_rule: ScoreRule[]      // 积分规则
}

/**
 * 获取成长规则
 * @returns 等级、段位、积分规则配置
 */
export const getGrowthRules = () =>
  request.get<any, GrowthRulesResp>('/admin/gamification/rules')

/**
 * 更新成长规则
 * @param data 成长规则配置
 */
export const updateGrowthRules = (data: GrowthRulesResp) =>
  request.put<any, any>('/admin/gamification/rules', data)

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
  request.get<any, EmailConfig>('/admin/email-config')

export const saveEmailConfig = (data: Partial<EmailConfig>) =>
  request.post<any, any>('/admin/email-config', data)

export const testEmailConfig = (to: string) =>
  request.post<any, any>('/admin/email-config/test', { to })
