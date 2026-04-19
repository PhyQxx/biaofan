<template>
  <div class="page-header">
    <h1>执行台</h1>
  </div>

  <div class="tab-bar">
    <button class="tab-item" :class="{ active: tab === 'pending' }" @click="tab = 'pending'">待执行 ({{ pendingList.length }})</button>
    <button class="tab-item" :class="{ active: tab === 'in_progress' }" @click="tab = 'in_progress'">执行中 ({{ inProgressList.length }})</button>
    <button class="tab-item" :class="{ active: tab === 'completed' }" @click="tab = 'completed'">已完成</button>
    <button class="tab-item" :class="{ active: tab === 'overdue' }" @click="tab = 'overdue'">已逾期 ({{ overdueList.length }})</button>
  </div>

  <div v-if="loading" class="exec-list">
    <div v-for="i in 5" :key="i" class="exec-card skeleton-card">
      <el-skeleton variant="text" width="60%" height="20px" style="margin-bottom:10px" />
      <el-skeleton variant="text" width="40%" height="16px" style="margin-bottom:10px" />
      <el-skeleton variant="text" width="30%" height="16px" />
    </div>
  </div>
  <div v-else class="exec-list">
    <div v-if="currentList.length" class="card-list">
      <div v-for="inst in currentList" :key="inst.id" class="exec-card">
        <div class="exec-sop-title" @click="goExecution(inst)">{{ inst.sopTitle }}</div>
        <div class="exec-meta">
          <span class="exec-status" :class="inst.status">{{ statusLabel(inst.status) }}</span>
          <span class="exec-period">{{ formatPeriod(inst) }}</span>
          <span class="exec-time" v-if="inst.startedAt">开始于 {{ formatDate(inst.startedAt) }}</span>
        </div>
        <div class="exec-progress" v-if="inst.status === 'in_progress'">
          <div class="progress-bar">
            <div class="progress-fill" :style="{ width: progressWidth(inst) }"></div>
          </div>
          <span class="progress-text">第 {{ inst.currentStep }} / {{ inst.totalSteps }} 步</span>
        </div>
        <button class="btn-execute" @click="goExecution(inst)">
          {{ inst.status === 'completed' ? '查看' : inst.status === 'overdue' ? '继续执行' : inst.status === 'in_progress' ? '继续执行' : '开始执行' }}
        </button>
      </div>
    </div>
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
      <p class="empty-title" v-else-if="tab === 'overdue'">暂无逾期任务</p>
      <p class="empty-title" v-else>暂无已完成记录</p>
      <p class="empty-sub" v-if="tab === 'pending'">发布 SOP 后会按周期自动生成执行实例</p>
      <p class="empty-sub" v-else-if="tab === 'in_progress'">从待执行标签页选择一个 SOP 开始</p>
      <p class="empty-sub" v-else-if="tab === 'overdue'">按时完成可避免逾期</p>
      <p class="empty-sub" v-else>完成 SOP 执行后会自动显示在这里</p>
      <button class="btn-primary-sm" v-if="tab === 'pending'" @click="router.push('/sop/new')">创建 SOP</button>
      <button class="btn-primary-sm" v-else-if="tab === 'in_progress'" @click="tab = 'pending'">查看待执行</button>
    </div>
  </div>
</template>

<script setup lang="ts">


/**
 * PC 端执行台列表页
 * - 我的待执行 / 进行中 / 已完成的执行单列表
 * - 支持状态筛选
 * - 点击进入执行详情
 */
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import request from '@/api'

const router = useRouter()
const authStore = useAuthStore()

const loading = ref(true)
const instances = ref<any[]>([])
const tab = ref('pending')

const pendingList = computed(() => instances.value.filter(e => e.status === 'pending'))
const inProgressList = computed(() => instances.value.filter(e => e.status === 'in_progress'))
const completedList = computed(() => instances.value.filter(e => e.status === 'completed'))
const overdueList = computed(() => instances.value.filter(e => e.status === 'overdue'))

const currentList = computed(() => {
  if (tab.value === 'pending') return pendingList.value
  if (tab.value === 'in_progress') return inProgressList.value
  if (tab.value === 'overdue') return overdueList.value
  return completedList.value
})

const statusLabel = (s: string) => ({
  pending: '待执行', in_progress: '执行中', completed: '已完成', overdue: '已逾期'
} as any)[s] || s

// Safe date parser - validates input before parsing, returns empty string on invalid dates
const parseDate = (dateStr: string | null | undefined): Date | null => {
  if (!dateStr || dateStr === 'null' || dateStr === 'undefined') return null
  const d = new Date(dateStr)
  if (isNaN(d.getTime())) {
    console.warn('[ExecutionView] Invalid date:', dateStr)
    return null
  }
  return d
}

const formatDate = (d: string) => {
  const parsed = parseDate(d)
  if (!parsed) return ''
  return parsed.toLocaleDateString('zh-CN', { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' })
}

const formatPeriod = (inst: any) => {
  if (!inst.periodStart || !inst.periodEnd) return ''
  const s = parseDate(inst.periodStart)
  const e = parseDate(inst.periodEnd)
  if (!s || !e) return ''
  const fmt = (d: Date) => `${d.getMonth() + 1}/${d.getDate()}`
  return `${fmt(s)} ~ ${fmt(e)}`
}

const progressWidth = (inst: any) => {
  if (!inst.totalSteps) return '0%'
  return Math.round((inst.currentStep / inst.totalSteps) * 100) + '%'
}

const goExecution = (inst: any) => {
  router.push(`/instance/${inst.id}`)
}

onMounted(async () => {
  try {
    await authStore.fetchMe()
    const res: any = await request.get('/instance/my')
    if (res.code === 200) {
      instances.value = Array.isArray(res.data?.records) ? res.data.records : []
      const sopIds = [...new Set(instances.value.map((e: any) => e.sopId))]
      const sopMap: Record<number, any> = {}
      const sopPromises = sopIds.map(sopId =>
        request.get(`/sop/${sopId}`).then((r: any) => ({ sopId, data: r.data })).catch((e: any) => ({ sopId, data: null, error: e }))
      )
      const results = await Promise.all(sopPromises)
      for (const { sopId, data } of results) {
        if (data) sopMap[sopId] = data
      }
      for (const inst of instances.value) {
        const sop = sopMap[inst.sopId]
        if (sop) {
          inst.sopTitle = sop.title
          try {
            const raw = sop.content
            const steps = (raw && raw !== 'null' && raw !== 'undefined') ? JSON.parse(raw) : []
            inst.totalSteps = steps.length
          } catch { inst.totalSteps = 0 }
        } else {
          inst.sopTitle = 'SOP'
          inst.totalSteps = 0
        }
      }
    }
  } finally {
    loading.value = false
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
.exec-card { background: #fff; border-radius: 12px; padding: 16px; display: flex; flex-direction: column; gap: 10px;
  margin-bottom: 16px;}
.exec-sop-title { font-size: 15px; font-weight: 600; color: #212121; cursor: pointer; }
.exec-sop-title:hover { color: #5B7FFF; }
.exec-meta { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; }
.exec-status { font-size: 12px; padding: 2px 8px; border-radius: 4px; }
.exec-status.pending { background: #F5F5F5; color: #999; }
.exec-status.in_progress { background: #E8F3FF; color: #5B7FFF; }
.exec-status.completed { background: #F6FFED; color: #52C41A; }
.exec-status.overdue { background: #FFF1F0; color: #FF4D4F; }
.exec-period { font-size: 12px; color: #5B7FFF; background: #E8ECFF; padding: 1px 8px; border-radius: 4px; }
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
