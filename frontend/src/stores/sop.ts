/**
 * SOP（标准操作程序）状态管理模块
 * 负责 SOP 文档的增删改查、分页获取、定时任务管理等功能
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
  // sops: 当前用户的所有 SOP 列表
  const sops = ref<Sop[]>([])
  // total: SOP 总数，用于分页
  const total = ref(0)

  /**
   * 获取当前用户的 SOP 列表（分页）
   * @param page 页码，默认第 1 页
   * @param size 每页数量，默认 20 条
   * @returns API 响应结果
   */
  async function fetchMySops(page = 1, size = 20) {
    const res = await request.get<unknown, ApiResponse<SopListData>>('/sop/my', { params: { page, size } })
    if (res.code === 200) {
      sops.value = res.data.records || []
      total.value = res.data.total || 0
    }
    return res
  }

  /**
   * 根据 ID 获取单个 SOP 详情
   * @param id SOP ID
   * @returns API 响应结果，包含 SOP 详情
   */
  async function getSopById(id: number) {
    const res = await request.get<unknown, ApiResponse<Sop>>(`/sop/${id}`)
    return res
  }

  /**
   * 创建新的 SOP
   * @param data SOP 数据（标题、内容、配置等）
   * @returns API 响应结果
   */
  async function createSop(data: Record<string, unknown>) {
    return request.post('/sop', data)
  }

  /**
   * 更新指定 SOP
   * @param id SOP ID
   * @param data 更新的 SOP 数据
   * @returns API 响应结果
   */
  async function updateSop(id: number, data: Record<string, unknown>) {
    return request.put(`/sop/${id}`, data)
  }

  /**
   * 删除指定 SOP
   * @param id SOP ID
   * @returns API 响应结果
   */
  async function deleteSop(id: number) {
    return request.delete(`/sop/${id}`)
  }

  /**
   * 发布指定 SOP
   * @param id SOP ID
   * @returns API 响应结果
   */
  async function publishSop(id: number) {
    return request.post(`/sop/${id}/publish`)
  }

  // ==================== 定时任务管理 API ====================

  /**
   * 获取指定 SOP 的定时任务配置
   * @param sopId SOP ID
   * @returns 定时任务详情
   */
  async function getScheduleTask(sopId: number) {
    return request.get(`/schedule/tasks/${sopId}`)
  }

  /**
   * 保存/创建 SOP 的定时任务
   * @param sopId SOP ID
   * @param cronExpression Cron 表达式
   * @returns API 响应结果
   */
  async function saveScheduleTask(sopId: number, cronExpression: string) {
    return request.post('/schedule/tasks', { sopId, cronExpression })
  }

  /**
   * 删除指定 SOP 的定时任务
   * @param sopId SOP ID
   * @returns API 响应结果
   */
  async function deleteScheduleTask(sopId: number) {
    return request.delete(`/schedule/tasks/${sopId}`)
  }

  /**
   * 启用/禁用定时任务
   * @param id 定时任务 ID
   * @param enabled 是否启用（1: 启用, 0: 禁用）
   * @returns API 响应结果
   */
  async function toggleScheduleTask(id: number, enabled: number) {
    return request.put(`/schedule/tasks/${id}/enable`, { enabled })
  }

  /**
   * 获取当前用户的所有定时任务
   * @returns 定时任务列表
   */
  async function getMyScheduleTasks() {
    return request.get('/schedule/tasks')
  }

  return {
    sops, total, fetchMySops, getSopById, createSop, updateSop, deleteSop, publishSop,
    getScheduleTask, saveScheduleTask, deleteScheduleTask, toggleScheduleTask, getMyScheduleTasks
  }
})
