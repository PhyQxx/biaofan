/**
 * AI 模块接口
 * 策略模式多模型支持：DeepSeek / GLM(智谱) / MiniMax
 */
import type { ApiResponse } from '@/types'
import request from './index'

// ========== AI 配置 ==========

export interface AiModelConfig {
  id?: number
  userId?: number
  modelType: string          // 'deepseek' | 'glm' | 'minimax'
  apiUrl?: string
  apiKey?: string
  modelName?: string
  systemPrompt?: string
  enabled?: boolean
  temperature?: number
}

export const getAiConfig = () => request.get<any, ApiResponse<AiModelConfig>>('/ai/config')

export const saveAiConfig = (config: AiModelConfig) =>
  request.post<any, ApiResponse<any>>('/ai/config', config)

export const deleteAiConfig = () => request.delete<any, ApiResponse<any>>('/ai/config')

// ========== SOP AI 三大功能 ==========

/** AI 辅助创建 SOP */
export interface AiGenerateSopRequest {
  goal: string               // 用户描述的目标
  title?: string
  category?: string
  tags?: string
  modelType?: string
}
export const generateSopByAi = (data: AiGenerateSopRequest) =>
  request.post<any, ApiResponse<string>>('/ai/sop/generate', data)

/** AI 执行指导 */
export interface AiExecuteGuidanceRequest {
  stepTitle: string
  stepDescription?: string
  stepIndex: number
  totalSteps: number
  checkData?: Record<string, unknown>
  notes?: string
  modelType?: string
}
export const getExecuteGuidance = (data: AiExecuteGuidanceRequest) =>
  request.post<any, ApiResponse<string>>('/ai/sop/execute/guidance', data)

/** AI 发布审核 */
export interface AiReviewRequest {
  sopId: number
}
export interface AiReviewResponse {
  id: number
  sopId: number
  sopVersion: number
  reviewMode: string
  verdict: 'pass' | 'warning' | 'reject'
  issues: string   // JSON 数组
  suggestions: string  // JSON 数组
  score?: number
  rawResponse: string
  modelType: string
  costMs: number
  createdAt: string
}
export const reviewSopByAi = (data: AiReviewRequest) =>
  request.post<any, ApiResponse<AiReviewResponse>>('/ai/sop/review', data)

// ========== 通用对话 ==========

export interface AiChatRequest {
  modelType?: string
  modelName?: string
  apiUrl?: string
  apiKey?: string
  systemPrompt?: string
  content: string
  temperature?: number
}
export const aiChat = (data: AiChatRequest) =>
  request.post<any, ApiResponse<string>>('/ai/chat', data)
