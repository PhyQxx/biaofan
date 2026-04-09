import { defineStore } from 'pinia'
import { ref } from 'vue'
import request from '@/api'

export const useNotificationStore = defineStore('notification', () => {
  const unreadCount = ref(0)

  async function fetchUnreadCount() {
    try {
      const res: any = await request.get('/notifications')
      if (res.code === 200) {
        unreadCount.value = (res.data || []).filter((n: any) => !n.isRead).length
      }
    } catch {}
  }

  return { unreadCount, fetchUnreadCount }
})
