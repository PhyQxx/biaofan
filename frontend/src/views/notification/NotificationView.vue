<template>
  <!-- Filter Tabs -->
  <div class="notif-toolbar">
    <div class="filter-tabs">
      <button class="filter-tab" :class="{ active: filter === null }" @click="filter = null">全部</button>
      <button class="filter-tab" :class="{ active: filter === 0 }" @click="filter = 0">未读 ({{ unreadCount }})</button>
      <button class="filter-tab" :class="{ active: filter === 1 }" @click="filter = 1">已读</button>
    </div>
    <button v-if="notifications.length" class="btn-read-all" @click="markAllRead">全部已读</button>
  </div>

  <!-- Notification List -->
  <div class="notif-list" v-if="filteredNotifications.length">
    <div
      v-for="n in filteredNotifications"
      :key="n.id"
      class="notif-card"
      :class="{ unread: !n.isRead }"
      @click="handleClick(n)"
    >
      <div class="notif-icon">{{ typeIcon(n.type) }}</div>
      <div class="notif-body">
        <div class="notif-header">
          <div class="notif-title">{{ n.title }}</div>
          <div class="notif-time">{{ formatDate(n.createdAt) }}</div>
        </div>
        <div class="notif-content" v-if="n.content">{{ n.content }}</div>
        <div class="notif-tag" :class="typeClass(n.type)">{{ typeLabel(n.type) }}</div>
      </div>
      <div class="notif-unread-dot" v-if="!n.isRead"></div>
    </div>
  </div>
  <div v-else class="empty-state">
    <div class="empty-icon">🔔</div>
    <p>暂无通知</p>
    <p class="empty-sub">执行 SOP 后会收到提醒</p>
  </div>
</template>

<script setup lang="ts">


/**
 * PC 端通知列表页
 * - 全部 / 未读 筛选
 * - 通知卡片（类型图标、标题、内容、时间）
 * - 标为已读 / 全部已读
 */
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useNotificationStore } from '@/stores/notification'
import request from '@/api'

const router = useRouter()
const notifStore = useNotificationStore()
const notifications = ref<any[]>([])
const filter = ref<number | null>(null)
const unreadCount = computed(() => notifications.value.filter(n => !n.isRead).length)

const filteredNotifications = computed(() => {
  if (filter.value === null) return notifications.value
  return notifications.value.filter(n => n.isRead === filter.value)
})

const typeIcon = (type: string) => ({
  execution_reminder: '⏰', execution_overdue: '🔴', sop_published: '📤',
  version_updated: '🔄', exception_reported: '⚠️', team_invitation: '👥',
} [type] || '📋')

const typeLabel = (type: string) => ({
  execution_reminder: '执行提醒', execution_overdue: '超时提醒', sop_published: 'SOP发布',
  version_updated: '版本更新', exception_reported: '异常上报', team_invitation: '团队邀请',
} [type] || '通知')

const typeClass = (type: string) => ({
  execution_reminder: 'tag-blue', execution_overdue: 'tag-red', sop_published: 'tag-green',
  version_updated: 'tag-yellow', exception_reported: 'tag-red', team_invitation: 'tag-blue',
} [type] || 'tag-gray')

const formatDate = (d: string) => {
  if (!d) return ''
  const date = new Date(d)
  const now = new Date()
  const diffMs = now.getTime() - date.getTime()
  const diffMin = Math.floor(diffMs / 60000)
  if (diffMin < 1) return '刚刚'
  if (diffMin < 60) return `${diffMin} 分钟前`
  if (diffMin < 1440) return `${Math.floor(diffMin / 60)} 小时前`
  return date.toLocaleDateString('zh-CN', { month: 'short', day: 'numeric' })
}

const loadNotifications = async () => {
  const res: any = await request.get('/notifications')
  if (res.code === 200) {
    notifications.value = res.data || []
  }
}

const handleClick = async (n: any) => {
  if (!n.isRead) {
    await request.put(`/notifications/${n.id}/read`)
    n.isRead = 1
    notifStore.fetchUnreadCount()
  }
  if (n.sourceType === 'execution' && n.sourceId) {
    router.push(`/execution/${n.sourceId}`)
  } else if (n.sourceType === 'sop' && n.sourceId) {
    router.push(`/sop/${n.sourceId}/edit`)
  }
}

const markAllRead = async () => {
  await request.put('/notifications/read-all')
  for (const n of notifications.value) n.isRead = 1
  notifStore.fetchUnreadCount()
  ElMessage.success('已全部标记为已读')
}

onMounted(loadNotifications)
</script>

<style scoped>
.notif-toolbar {
  display: flex; align-items: center; justify-content: space-between;
  padding: 12px 0; margin-bottom: 4px;
}
.filter-tabs { display: flex; gap: 0; background: #fff; padding: 8px 12px; border-radius: 12px; }
.filter-tab { padding: 6px 16px; border: none; background: transparent; border-radius: 20px; font-size: 13px; color: #666; cursor: pointer; transition: all 0.15s; }
.filter-tab.active { background: #E8ECFF; color: #5B7FFF; font-weight: 600; }
.btn-read-all { height: 30px; padding: 0 12px; background: #fff; color: #5B7FFF; border: 1px solid #5B7FFF; border-radius: 6px; font-size: 12px; cursor: pointer; flex-shrink: 0; }

.notif-list { padding: 12px 0; display: flex; flex-direction: column; gap: 8px; }
.notif-card {
  background: #fff; border-radius: 12px; padding: 14px 16px;
  display: flex; align-items: flex-start; gap: 12px;
  cursor: pointer; transition: all 0.15s; position: relative;
  border: 1px solid transparent;
}
.notif-card:hover { background: #FAFBFF; border-color: #E8ECFF; }
.notif-card.unread { border-left: 3px solid #5B7FFF; }
.notif-icon { font-size: 22px; flex-shrink: 0; margin-top: 2px; }
.notif-body { flex: 1; }
.notif-header { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 4px; }
.notif-title { font-size: 14px; font-weight: 600; color: #333; flex: 1; }
.notif-time { font-size: 11px; color: #999; flex-shrink: 0; margin-left: 8px; }
.notif-content { font-size: 13px; color: #666; line-height: 1.5; margin-bottom: 6px; }
.notif-tag { display: inline-block; font-size: 11px; padding: 1px 8px; border-radius: 10px; }
.tag-blue { background: #E8F3FF; color: #1890FF; }
.tag-red { background: #FFF1F0; color: #F5222D; }
.tag-yellow { background: #FFFBE6; color: #FAAD14; }
.tag-green { background: #F6FFED; color: #52C41A; }
.tag-gray { background: #F5F5F5; color: #999; }
.notif-unread-dot { width: 8px; height: 8px; background: #5B7FFF; border-radius: 50%; flex-shrink: 0; margin-top: 6px; }

.empty-state { text-align: center; padding: 80px 0; color: #999; }
.empty-icon { font-size: 56px; margin-bottom: 16px; }
.empty-state p { margin: 0 0 6px; font-size: 15px; }
.empty-sub { font-size: 13px; }
</style>
