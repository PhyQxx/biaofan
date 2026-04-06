import request from './index'

// ============ Types ============
export interface Template {
  id?: number
  templateId?: string
  sopId?: string
  title: string
  description?: string
  category: string
  subCategory?: string
  coverUrl?: string
  authorId: string
  authorName?: string
  stepCount?: number
  estimatedDuration?: string
  avgRating?: number
  ratingCount?: number
  useCount?: number
  status: string
  rejectReason?: string
  auditedBy?: string
  auditedAt?: string
  createdAt?: string
  updatedAt?: string
  // 额外字段（详情页）
  steps?: TemplateStep[]
  isFavorited?: boolean
  hasReviewed?: boolean
}

export interface TemplateStep {
  stepOrder: number
  title: string
  description?: string
  estimatedDuration?: string
}

export interface Review {
  id?: number
  templateId: string
  userId: string
  userName?: string
  rating: number
  comment?: string
  createdAt?: string
}

// Full API response wrapper
export interface ApiResponse<T = any> {
  code: number
  message: string
  data: T
  timestamp: number
  success: boolean
}

export interface TemplateListData {
  total: number
  templates: Template[]
  page: number
  page_size: number
}

export interface ReviewListData {
  total: number
  reviews: Review[]
  page: number
  page_size: number
}

// ============ Marketplace APIs (User) ============

/** 模板列表（分类/搜索/排序/分页） */
export const getTemplates = (params: {
  category?: string
  keyword?: string
  sort?: string
  page?: number
  page_size?: number
}) =>
  request.get<any, ApiResponse<TemplateListData>>('/marketplace/templates', { params })

/** 模板详情（含步骤、收藏状态） */
export const getTemplateDetail = (templateId: string) =>
  request.get<any, ApiResponse<any>>(`/marketplace/templates/${templateId}`)

/** 使用模板（复制为用户 SOP） */
export const useTemplate = (templateId: string, data: { user_id: string; sop_name?: string }) =>
  request.post<any, ApiResponse<any>>(`/marketplace/templates/${templateId}/use`, data)

/** 收藏模板 */
export const favoriteTemplate = (templateId: string, data: { user_id: string }) =>
  request.post<any, ApiResponse<any>>(`/marketplace/templates/${templateId}/favorite`, data)

/** 取消收藏 */
export const unfavoriteTemplate = (templateId: string, data: { user_id: string }) =>
  request.delete<any, ApiResponse<any>>(`/marketplace/templates/${templateId}/favorite`, { data })

/** 我的收藏列表 */
export const getFavorites = (params: { user_id: string; page?: number; page_size?: number }) =>
  request.get<any, ApiResponse<TemplateListData>>('/marketplace/templates/favorites', { params })

/** 提交模板审核 */
export const submitTemplate = (data: {
  sop_id: string
  title: string
  description?: string
  category: string
  sub_category?: string
}) =>
  request.post<any, ApiResponse<any>>('/marketplace/templates', data)

/** 获取评价列表 */
export const getReviews = (templateId: string, params?: { page?: number; page_size?: number }) =>
  request.get<any, ApiResponse<ReviewListData>>(`/marketplace/templates/${templateId}/reviews`, { params })

/** 提交评价 */
export const submitReview = (templateId: string, data: { user_id: string; rating: number; comment?: string }) =>
  request.post<any, ApiResponse<any>>(`/marketplace/templates/${templateId}/reviews`, data)

// ============ Admin APIs ============

/** 审核列表 */
export const getAuditList = (params?: { status?: string; page?: number; page_size?: number }) =>
  request.get<any, ApiResponse<TemplateListData>>('/admin/marketplace/templates', { params })

/** 审核操作（通过/驳回） */
export const auditTemplate = (templateId: string, data: { status: string; reject_reason?: string }) =>
  request.put<any, ApiResponse<any>>(`/admin/marketplace/templates/${templateId}/audit`, data)

/** 下架模板 */
export const offlineTemplate = (templateId: string) =>
  request.delete<any, ApiResponse<any>>(`/admin/marketplace/templates/${templateId}`)
