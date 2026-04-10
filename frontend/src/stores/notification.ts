/**
 * 通知状态管理模块
 * 负责获取和管理用户的未读通知数量
 */
import { defineStore } from 'pinia'
import { ref } from 'vue'
import request from '@/api'
import type { ApiResponse } from '@/types'

// 通知消息数据结构
interface Notification {
  id: number
  isRead: boolean
  [key: string]: unknown
}

export const useNotificationStore = defineStore('notification', () => {
  // unreadCount: 未读通知数量
  const unreadCount = ref(0)

  /**
   * 获取未读通知数量
   * 从服务器获取通知列表，统计未读消息的数量并存储到 unreadCount
   */
  async function fetchUnreadCount() {
    try {
      const res = await request.get<unknown, ApiResponse<Notification[]>>('/notifications')
      if (res.code === 200) {
        // 过滤出未读通知并更新数量
        unreadCount.value = (res.data || []).filter((n) => !n.isRead).length
      }
    } catch (e) {
      console.error('[notification] fetchUnreadCount failed:', e)
    }
  }

  return { unreadCount, fetchUnreadCount }
})
