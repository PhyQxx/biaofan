import { defineStore } from 'pinia'
import { ref } from 'vue'
import request from '@/api'
import type { ApiResponse } from '@/types'

interface Notification {
  id: number
  isRead: boolean
  [key: string]: unknown
}

export const useNotificationStore = defineStore('notification', () => {
  const unreadCount = ref(0)

  async function fetchUnreadCount() {
    try {
      const res = await request.get<unknown, ApiResponse<Notification[]>>('/notifications')
      if (res.code === 200) {
        unreadCount.value = (res.data || []).filter((n) => !n.isRead).length
      }
    } catch (e) {
      console.error('[notification] fetchUnreadCount failed:', e)
    }
  }

  return { unreadCount, fetchUnreadCount }
})
