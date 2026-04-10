import { defineStore } from 'pinia'
import { ref } from 'vue'
import request from '@/api'
import type { Sop, ApiResponse } from '@/types'

interface SopListData {
  records: Sop[]
  total: number
}

export const useSopStore = defineStore('sop', () => {
  const sops = ref<Sop[]>([])
  const total = ref(0)

  async function fetchMySops(page = 1, size = 20) {
    const res = await request.get<unknown, ApiResponse<SopListData>>('/sop/my', { params: { page, size } })
    if (res.code === 200) {
      sops.value = res.data.records || []
      total.value = res.data.total || 0
    }
    return res
  }

  async function getSopById(id: number) {
    const res = await request.get<unknown, ApiResponse<Sop>>(`/sop/${id}`)
    return res
  }

  async function createSop(data: Record<string, unknown>) {
    return request.post('/sop', data)
  }

  async function updateSop(id: number, data: Record<string, unknown>) {
    return request.put(`/sop/${id}`, data)
  }

  async function deleteSop(id: number) {
    return request.delete(`/sop/${id}`)
  }

  async function publishSop(id: number) {
    return request.post(`/sop/${id}/publish`)
  }

  // 定时任务 API
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
    getScheduleTask, saveScheduleTask, deleteScheduleTask, toggleScheduleTask, getMyScheduleTasks
  }
})
