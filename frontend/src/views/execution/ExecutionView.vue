<template>
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
  <!-- Empty State -->
  <div v-else class="empty-state">
    <div class="empty-illustration">
      <svg width="80" height="80" viewBox="0 0 80 80" fill="none" xmlns="http://www.w3.org/2000/svg">
        <circle cx="40" cy="40" r="36" fill="#EEF2FF" stroke="#C7D0FF" stroke-width="2"/>
        <path d="M28 40h24M40 28v24" stroke="#8B95C7" stroke-width="3" stroke-linecap="round"/>
        <circle cx="40" cy="40" r="10" fill="#C7D0FF"/>
        <circle cx="40" cy="40" r="4" fill="#8B95C7"/>
      </svg>
    </div>
    <p class="empty-title" v-if="tab === 'pending'">暂无待执行的 SOP</p>
    <p class="empty-title" v-else-if="tab === 'in_progress'">暂无正在进行的任务</p>
    <p class="empty-title" v-else>暂无已完成记录</p>
    <p class="empty-sub" v-if="tab === 'pending'">去创建一个 SOP 开始执行吧</p>
    <p class="empty-sub" v-else-if="tab === 'in_progress'">从待执行标签页选择一个 SOP 开始</p>
    <p class="empty-sub" v-else>完成 SOP 执行后会自动显示在这里</p>
    <button class="btn-primary-sm" v-if="tab !== 'completed'" @click="tab = 'pending'">查看待执行</button>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import request from '@/api'

const router = useRouter()
const authStore = useAuthStore()

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

onMounted(async () => {
  await authStore.fetchMe()
  const res: any = await request.get('/execution/my')
  if (res.code === 200) {
    executions.value = res.data || []
    const sopIds = [...new Set(executions.value.map(e => e.sopId))]
    for (const sopId of sopIds) {
      try {
        const r: any = await request.get(`/sop/${sopId}`)
        if (r.code === 200) sopMap.value[sopId] = r.data
      } catch {}
    }
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
.empty-illustration { margin-bottom: 20px; }
.empty-title { font-size: 16px; font-weight: 600; color: #333; margin: 0 0 8px; }
.empty-sub { font-size: 13px; color: #999; margin: 0 0 16px; }
.btn-primary-sm { height: 36px; padding: 0 20px; background: #5B7FFF; color: white; border: none; border-radius: 8px; font-size: 14px; font-weight: 500; cursor: pointer; }
</style>
