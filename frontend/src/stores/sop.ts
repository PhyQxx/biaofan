/**
 * SOP（标准操作程序）状态管理模块
 * 负责 SOP 文档的增删改查、分页获取、定时任务管理及审核流等功能
 */
import { defineStore } from 'pinia'
import { ref } from 'vue'
import request from '@/api'
import type { Sop, ApiResponse } from '@/types'

// SOP 列表数据结构，包含记录列表和总数
interface SopListData {
  records: Sop[]
  total: number
}

export const useSopStore = defineStore('sop', () => {
  const sops = ref<Sop[]>([])
  const total = ref(0)

  /**
   * 获取当前用户的 SOP 列表（分页）
   */
  async function fetchMySops(page = 1, size = 20, orgId?: number | null) {
    const res = await request.get<unknown, ApiResponse<SopListData>>('/sop/my', { params: { page, size, orgId } })
    if (res.code === 200) {
      sops.value = res.data.records || []
      total.value = res.data.total || 0
    }
    return res
  }

  /**
   * 根据 ID 获取单个 SOP 详情
   */
  async function getSopById(id: number) {
    return request.get<unknown, ApiResponse<Sop>>(`/sop/${id}`)
  }

  /**
   * 创建新的 SOP
   */
  async function createSop(data: Record<string, unknown>, orgId?: number | null) {
    return request.post('/sop', data, { params: { orgId } })
  }

  /**
   * 更新指定 SOP
   */
  async function updateSop(id: number, data: Record<string, unknown>) {
    return request.put(`/sop/${id}`, data)
  }

  /**
   * 删除指定 SOP
   */
  async function deleteSop(id: number) {
    return request.delete(`/sop/${id}`)
  }

  /**
   * 发布指定 SOP (仅限个人空间)
   */
  async function publishSop(id: number) {
    return request.post(`/sop/${id}/publish`)
  }

  /**
   * 提交审核 (组织 SOP)
   */
  async function submitSopReview(id: number) {
    return request.post(`/sop/${id}/submit`)
  }

  /**
   * 审核通过 (管理员)
   */
  async function approveSop(id: number, comment?: string) {
    return request.post(`/sop/${id}/approve`, { comment })
  }

  /**
   * 审核驳回 (管理员)
   */
  async function rejectSop(id: number, comment?: string) {
    return request.post(`/sop/${id}/reject`, { comment })
  }

  /**
   * 获取组织下待审核列表
   */
  async function fetchPendingApprovals(orgId: number) {
    return request.get<unknown, ApiResponse<any[]>>('/sop/approvals/pending', { params: { orgId } })
  }

  // ==================== 定时任务管理 API ====================

  async function getScheduleTask(sopId: number) {
    return request.get(`/schedule/tasks/${sopId}`)
  }

  async function saveScheduleTask(sopId: number, cronExpression: string) {
    return request.post('/schedule/tasks', { sopId, cronExpression })
  }

  async function deleteScheduleTask(sopId: number) {
    return request.delete(`/schedule/tasks/${sopId}`)
  }

  async function toggleScheduleTask(id: number, enabled: number) {
    return request.put(`/schedule/tasks/${id}/enable`, { enabled })
  }

  async function getMyScheduleTasks() {
    return request.get('/schedule/tasks')
  }

  return {
    sops, total, fetchMySops, getSopById, createSop, updateSop, deleteSop, publishSop,
    submitSopReview, approveSop, rejectSop, fetchPendingApprovals,
    getScheduleTask, saveScheduleTask, deleteScheduleTask, toggleScheduleTask, getMyScheduleTasks
  }
})
