import request from './index'
import type { ApiResponse } from '@/types'

// ============ 类型定义 ============

/**
 * 模板实体
 * SOP模板，包含模板基本信息
 */
export interface Template {
  id?: number
  templateId?: string         // 模板ID
  sopId?: string               // 关联SOP ID
  title: string               // 模板标题
  description?: string        // 模板描述
  category: string            // 类别
  subCategory?: string         // 子类别
  coverUrl?: string           // 封面URL
  authorId: string            // 作者ID
  authorName?: string         // 作者名称
  stepCount?: number          // 步骤数量
  estimatedDuration?: string  // 预计时长
  avgRating?: number          // 平均评分
  ratingCount?: number        // 评分次数
  useCount?: number           // 使用次数
  status: string              // 状态：pending/approved/rejected
  rejectReason?: string       // 驳回原因
  auditedBy?: string           // 审核人
  auditedAt?: string           // 审核时间
  createdAt?: string          // 创建时间
  updatedAt?: string           // 更新时间
  // 额外字段（详情页）
  steps?: TemplateStep[]      // 模板步骤列表
  isFavorited?: boolean       // 是否已收藏
  hasReviewed?: boolean       // 是否已评价
}

/**
 * 模板步骤
 * 模板的单个步骤信息
 */
export interface TemplateStep {
  stepOrder: number          // 步骤序号
  title: string             // 步骤标题
  description?: string       // 步骤描述
  estimatedDuration?: string // 预计时长
}

/**
 * 评价实体
 * 用户对模板的评价
 */
export interface Review {
  id?: number
  templateId: string        // 模板ID
  userId: string             // 用户ID
  userName?: string          // 用户名
  rating: number             // 评分（1-5）
  comment?: string           // 评论内容
  createdAt?: string         // 评论时间
}

/**
 * 模板列表数据
 * 包含分页信息的模板列表
 */
export interface TemplateListData {
  total: number              // 总数
  templates: Template[]      // 模板列表
  page: number               // 当前页
  page_size: number          // 每页数量
}

/**
 * 评价列表数据
 * 包含分页信息的评价列表
 */
export interface ReviewListData {
  total: number              // 总数
  reviews: Review[]           // 评价列表
  page: number               // 当前页
  page_size: number           // 每页数量
}

// ============ 商城 API（用户端） ============

/**
 * 获取模板列表（支持分类/搜索/排序/分页）
 * @param params 查询参数：category分类、keyword关键词、sort排序、page页码、page_size每页数量
 */
export const getTemplates = (params: {
  category?: string
  keyword?: string
  sort?: string
  page?: number
  page_size?: number
}) =>
  request.get<unknown, ApiResponse<TemplateListData>>('/marketplace/templates', { params })

/**
 * 获取模板详情（含步骤、收藏状态）
 * @param templateId 模板ID
 */
export const getTemplateDetail = (templateId: string) =>
  request.get<unknown, ApiResponse<{ template?: Template; steps?: TemplateStep[]; isFavorited?: boolean; hasReviewed?: boolean } & Template>>((`/marketplace/templates/${templateId}`))

/**
 * 使用模板（复制为用户 SOP）
 * @param templateId 模板ID
 * @param data user_id用户ID、sop_name新的SOP名称
 */
export const useTemplate = (templateId: string, data: { user_id: string; sop_name?: string }) =>
  request.post<unknown, ApiResponse<unknown>>(`/marketplace/templates/${templateId}/use`, data)

/**
 * 收藏模板
 * @param templateId 模板ID
 * @param data user_id用户ID
 */
export const favoriteTemplate = (templateId: string, data: { user_id: string }) =>
  request.post<unknown, ApiResponse<unknown>>(`/marketplace/templates/${templateId}/favorite`, data)

/**
 * 取消收藏
 * @param templateId 模板ID
 * @param data user_id用户ID
 */
export const unfavoriteTemplate = (templateId: string, data: { user_id: string }) =>
  request.delete<unknown, ApiResponse<unknown>>(`/marketplace/templates/${templateId}/favorite`, { data })

/**
 * 获取我的收藏列表
 * @param params user_id用户ID、page页码、page_size每页数量
 */
export const getFavorites = (params: { user_id: string; page?: number; page_size?: number }) =>
  request.get<unknown, ApiResponse<TemplateListData>>('/marketplace/templates/favorites', { params })

/**
 * 提交模板审核
 * @param data sop_id关联SOP ID、title标题、description描述、category类别、sub_category子类别
 */
export const submitTemplate = (data: {
  sop_id: string
  title: string
  description?: string
  category: string
  sub_category?: string
}) =>
  request.post<unknown, ApiResponse<unknown>>('/marketplace/templates', data)

/**
 * 获取评价列表
 * @param templateId 模板ID
 * @param params page页码、page_size每页数量
 */
export const getReviews = (templateId: string, params?: { page?: number; page_size?: number }) =>
  request.get<unknown, ApiResponse<ReviewListData>>(`/marketplace/templates/${templateId}/reviews`, { params })

/**
 * 提交评价
 * @param templateId 模板ID
 * @param data user_id用户ID、rating评分、comment评论内容
 */
export const submitReview = (templateId: string, data: { user_id: string; rating: number; comment?: string }) =>
  request.post<unknown, ApiResponse<unknown>>(`/marketplace/templates/${templateId}/reviews`, data)

// ============ 管理端 API ============

/**
 * 获取审核列表
 * @param params status审核状态、page页码、page_size每页数量
 */
export const getAuditList = (params?: { status?: string; page?: number; page_size?: number }) =>
  request.get<unknown, ApiResponse<TemplateListData>>('/admin/marketplace/templates', { params })

/**
 * 审核操作（通过/驳回）
 * @param templateId 模板ID
 * @param data status状态、reject_reason驳回原因
 */
export const auditTemplate = (templateId: string, data: { status: string; reject_reason?: string }) =>
  request.put<unknown, ApiResponse<unknown>>(`/admin/marketplace/templates/${templateId}/audit`, data)

/**
 * 下架模板
 * @param templateId 模板ID
 */
export const offlineTemplate = (templateId: string) =>
  request.delete<unknown, ApiResponse<unknown>>(`/admin/marketplace/templates/${templateId}`)
