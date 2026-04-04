<template>
  <div class="execution-page">
    <!-- Topbar -->
    <div class="topbar">
      <div class="topbar-left">
        <div class="logo">
          <span class="logo-icon">🚀</span>
          <span class="logo-text">标帆 SOP</span>
        </div>
      </div>
      <div class="topbar-right">
        <button class="btn-new" @click="router.push('/sop/new')">+ 新建 SOP</button>
        <div class="avatar" @click="handleLogout">{{ user?.username?.charAt(0) || 'U' }}</div>
      </div>
    </div>

    <div class="main-layout">
      <!-- Sidebar -->
      <div class="sidebar">
        <div class="sidebar-item" @click="router.push('/')">
          <span>📊</span><span>工作台</span>
        </div>
        <div class="sidebar-item active">
          <span>▶️</span><span>执行台</span>
        </div>
        <div class="sidebar-item" @click="router.push('/stats')">
          <span>📈</span><span>统计</span>
        </div>
        <div class="sidebar-divider"></div>
        <div class="sidebar-item" @click="handleLogout">
          <span>🚪</span><span>退出登录</span>
        </div>
      </div>

      <!-- Content -->
      <div class="main-content">
        <div class="page-header">
          <h1>执行台</h1>
        </div>

        <!-- Tabs -->
        <div class="tab-bar">
          <button class="tab-item" :class="{ active: tab === 'pending' }" @click="tab = 'pending'">待执行 ({{ pendingExecutions.length }})</button>
          <button class="tab-item" :class="{ active: tab === 'in_progress' }" @click="tab = 'in_progress'">进行中 ({{ inProgressExecutions.length }})</button>
          <button class="tab-item" :class="{ active: tab === 'completed' }" @click="tab = 'completed'">已完成</button>
        </div>

        <!-- List -->
        <div class="exec-list" v-if="currentList.length">
          <div v-for="e in currentList" :key="e.id" class="exec-card">
            <div class="exec-sop-title" @click="goExecution(e)">{{ e.sopTitle }}</div>
            <div class="exec-meta">
              <span class="exec-status" :class="e.status">{{ statusLabel(e.status) }}</span>
              <span class="exec-time" v-if="e.startedAt">开始于 {{ formatDate(e.startedAt) }}</span>
            </div>
            <div class="exec-progress" v-if="e.status === 'in_progress'">
              <div class="progress-bar">
                <div class="progress-fill" :style="{ width: progressWidth(e) }"></div>
              </div>
              <span class="progress-text">第 {{ e.currentStep }} / {{ e.totalSteps }} 步</span>
            </div>
            <button class="btn-execute" @click="goExecution(e)">
              {{ e.status === 'completed' ? '查看' : e.status === 'in_progress' ? '继续执行' : '开始执行' }}
            </button>
          </div>
        </div>
        <div v-else class="empty-state">
          <p>🎉 暂无执行任务</p>
          <p class="empty-sub">去创建一个 SOP 开始执行吧</p>
          <button class="btn-primary-sm" @click="router.push('/')">去工作台</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import request from '@/api'

const router = useRouter()
const authStore = useAuthStore()
const user = authStore.userInfo

const executions = ref<any[]>([])
const tab = ref('pending')
const sopMap = ref<Record<number, any>>({})

const pendingExecutions = computed(() => executions.value.filter(e => e.status === 'pending'))
const inProgressExecutions = computed(() => executions.value.filter(e => e.status === 'in_progress'))
const completedExecutions = computed(() => executions.value.filter(e => e.status === 'completed'))

const currentList = computed(() => {
  if (tab.value === 'pending') return pendingExecutions.value
  if (tab.value === 'in_progress') return inProgressExecutions.value
  return completedExecutions.value
})

const statusLabel = (s: string) => ({ pending: '待执行', in_progress: '进行中', completed: '已完成', abnormal: '异常' }[s] || s)

const formatDate = (d: string) => {
  if (!d) return ''
  return new Date(d).toLocaleDateString('zh-CN', { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' })
}

const progressWidth = (e: any) => {
  if (!e.totalSteps) return '0%'
  return Math.round((e.currentStep / e.totalSteps) * 100) + '%'
}

const goExecution = (e: any) => {
  router.push(`/execution/${e.id}`)
}

const handleLogout = () => { authStore.logout(); router.push('/login') }

onMounted(async () => {
  await authStore.fetchMe()
  const res: any = await request.get('/execution/my')
  if (res.code === 200) {
    executions.value = res.data || []
    // Fetch sop titles
    const sopIds = [...new Set(executions.value.map(e => e.sopId))]
    for (const sopId of sopIds) {
      try {
        const r: any = await request.get(`/sop/${sopId}`)
        if (r.code === 200) sopMap.value[sopId] = r.data
      } catch {}
    }
    // Attach sop info to executions
    for (const e of executions.value) {
      const sop = sopMap.value[e.sopId]
      if (sop) {
        e.sopTitle = sop.title
        try {
          const steps = JSON.parse(sop.content || '[]')
          e.totalSteps = steps.length
        } catch { e.totalSteps = 0 }
      } else {
        e.sopTitle = 'SOP'
        e.totalSteps = 0
      }
    }
  }
})
</script>

<style scoped>
.execution-page { min-height: 100vh; background: #F5F7FA; }
.topbar { height: 56px; background: #fff; border-bottom: 1px solid #E8E8E8; display: flex; align-items: center; justify-content: space-between; padding: 0 20px; position: sticky; top: 0; z-index: 100; }
.topbar-left { display: flex; align-items: center; gap: 24px; }
.logo { display: flex; align-items: center; gap: 8px; font-weight: 600; font-size: 16px; color: #212121; }
.logo-icon { font-size: 22px; }
.topbar-right { display: flex; align-items: center; gap: 12px; }
.btn-new { height: 36px; padding: 0 16px; background: #5B7FFF; color: white; border: none; border-radius: 8px; font-size: 14px; font-weight: 600; cursor: pointer; }
.btn-new:hover { background: #7994FF; }
.avatar { width: 36px; height: 36px; background: #5B7FFF; color: white; border-radius: 50%; display: flex; align-items: center; justify-content: center; font-weight: 600; cursor: pointer; }
.main-layout { display: flex; min-height: calc(100vh - 56px); }
.sidebar { width: 200px; background: #fff; border-right: 1px solid #E8E8E8; padding: 12px 0; flex-shrink: 0; }
.sidebar-item { padding: 9px 16px; font-size: 14px; color: #666; cursor: pointer; display: flex; align-items: center; gap: 8px; }
.sidebar-item:hover { background: #F5F7FA; color: #333; }
.sidebar-item.active { background: #E8ECFF; color: #5B7FFF; font-weight: 500; }
.sidebar-divider { height: 1px; background: #E8E8E8; margin: 8px 16px; }
.main-content { flex: 1; padding: 24px; overflow-y: auto; }
.page-header { margin-bottom: 20px; }
.page-header h1 { margin: 0; font-size: 22px; font-weight: 600; color: #212121; }
.tab-bar { display: flex; gap: 4px; background: #fff; padding: 8px 12px; border-radius: 12px; margin-bottom: 16px; }
.tab-item { padding: 8px 16px; border: none; background: transparent; border-radius: 8px; font-size: 14px; color: #666; cursor: pointer; transition: all 0.15s; }
.tab-item.active { background: #E8ECFF; color: #5B7FFF; font-weight: 500; }
.tab-item:hover:not(.active) { background: #F5F7FA; }
.exec-list { display: flex; flex-direction: column; gap: 12px; }
.exec-card { background: #fff; border-radius: 12px; padding: 16px; display: flex; flex-direction: column; gap: 10px; }
.exec-sop-title { font-size: 15px; font-weight: 600; color: #212121; cursor: pointer; }
.exec-sop-title:hover { color: #5B7FFF; }
.exec-meta { display: flex; align-items: center; gap: 10px; }
.exec-status { font-size: 12px; padding: 2px 8px; border-radius: 4px; }
.exec-status.pending { background: #F5F5F5; color: #999; }
.exec-status.in_progress { background: #E8F3FF; color: #5B7FFF; }
.exec-status.completed { background: #F6FFED; color: #52C41A; }
.exec-time { font-size: 12px; color: #999; }
.exec-progress { display: flex; align-items: center; gap: 10px; }
.progress-bar { flex: 1; height: 6px; background: #F0F0F0; border-radius: 3px; overflow: hidden; }
.progress-fill { height: 100%; background: #5B7FFF; border-radius: 3px; transition: width 0.3s; }
.progress-text { font-size: 12px; color: #999; white-space: nowrap; }
.btn-execute { height: 32px; padding: 0 14px; background: #5B7FFF; color: white; border: none; border-radius: 8px; font-size: 13px; font-weight: 500; cursor: pointer; align-self: flex-start; }
.btn-execute:hover { background: #7994FF; }
.empty-state { text-align: center; padding: 60px 0; color: #999; }
.empty-state p { margin: 0 0 8px; }
.empty-sub { font-size: 13px; }
.btn-primary-sm { height: 36px; padding: 0 20px; background: #5B7FFF; color: white; border: none; border-radius: 8px; font-size: 14px; font-weight: 500; cursor: pointer; margin-top: 12px; }
</style>
