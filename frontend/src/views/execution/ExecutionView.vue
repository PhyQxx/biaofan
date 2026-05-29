<template>
  <div class="execution-list-page">
    <!-- Header -->
    <header class="page-header">
      <div class="header-left">
        <h1>🛠 执行台</h1>
        <p class="subtitle">监控并完成您的所有标准操作流程</p>
      </div>
      <div class="header-right">
        <div class="stats-mini">
          <div class="stat-item">
            <span class="val">{{ inProgressList.length }}</span>
            <span class="label">进行中</span>
          </div>
          <div class="stat-item">
            <span class="val">{{ pendingList.length }}</span>
            <span class="label">待开始</span>
          </div>
        </div>
      </div>
    </header>

    <!-- Navigation Tabs -->
    <nav class="tab-nav">
      <button 
        v-for="t in tabs" 
        :key="t.key"
        class="tab-btn"
        :class="{ active: tab === t.key }"
        @click="tab = t.key"
      >
        <span class="icon">{{ t.icon }}</span>
        <span class="text">{{ t.label }}</span>
        <span class="count" v-if="t.count !== undefined">{{ t.count }}</span>
      </button>
    </nav>

    <!-- Main Content -->
    <main class="content-area">
      <!-- Loading Skeleton -->
      <div v-if="loading" class="exec-grid skeleton">
        <div v-for="i in 6" :key="i" class="glass-card skeleton-card">
          <el-skeleton animated>
            <template #template>
              <el-skeleton-item variant="text" style="width: 30%; height: 12px; margin-bottom: 12px" />
              <el-skeleton-item variant="h3" style="width: 80%; height: 24px; margin-bottom: 20px" />
              <el-skeleton-item variant="text" style="width: 100%; height: 8px; margin-bottom: 8px" />
              <div style="display: flex; justify-content: space-between; margin-top: 20px">
                <el-skeleton-item variant="button" style="width: 80px; height: 32px" />
                <el-skeleton-item variant="circle" style="width: 32px; height: 32px" />
              </div>
            </template>
          </el-skeleton>
        </div>
      </div>

      <!-- Card Grid -->
      <div v-else-if="currentList.length" class="exec-grid">
        <div 
          v-for="inst in currentList" 
          :key="inst.id" 
          class="glass-card exec-card"
          :class="inst.status"
          @click="goExecution(inst)"
        >
          <div class="card-top">
            <span class="category-tag">{{ inst.category || '通用' }}</span>
            <div class="status-indicator">
              <span class="dot"></span>
              {{ statusLabel(inst.status) }}
            </div>
          </div>
          
          <h3 class="sop-title">{{ inst.sopTitle }}</h3>
          
          <div class="card-meta">
            <div class="meta-item">
              <span class="icon">📅</span>
              <span class="text">{{ formatPeriod(inst) }}</span>
            </div>
            <div class="meta-item" v-if="inst.startedAt">
              <span class="icon">⏱️</span>
              <span class="text">已开始 {{ formatDate(inst.startedAt) }}</span>
            </div>
          </div>

          <!-- Progress (Only for In Progress) -->
          <div class="card-footer">
            <div class="progress-section" v-if="inst.status === 'in_progress'">
              <div class="progress-header">
                <span class="label">完成度</span>
                <span class="val">{{ inst.currentStep }} / {{ inst.totalSteps }}</span>
              </div>
              <div class="progress-bar-wrap">
                <div class="progress-bar-fill" :style="{ width: progressWidth(inst) }"></div>
              </div>
            </div>
            <div class="progress-section" v-else-if="inst.status === 'completed'">
              <div class="success-seal">已达成 ✓</div>
            </div>
            <div class="progress-section" v-else>
              <div class="pending-hint">等待开始执行...</div>
            </div>

            <button class="btn-action">
              {{ inst.status === 'completed' ? '回顾记录' : '立即处理' }}
              <span class="arrow">→</span>
            </button>
          </div>
        </div>
      </div>

      <!-- Empty State -->
      <div v-else class="empty-state-modern">
        <div class="empty-glow"></div>
        <div class="empty-content">
          <div class="empty-icon-wrap">
            <span class="icon">✨</span>
          </div>
          <h3>当前列表空空如也</h3>
          <p>{{ emptySubText }}</p>
          <button v-if="tab === 'pending'" class="btn-cta" @click="router.push('/sop/new')">
            创建我的第一个 SOP
          </button>
          <button v-else class="btn-cta-secondary" @click="tab = 'pending'">
            查看待处理任务
          </button>
        </div>
      </div>
    </main>
  </div>
</template>

<script setup lang="ts">


/**
 * 现代化执行台列表
 * 采用“玻璃态”设计与卡片式布局，突出执行状态与优先级
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

const tabs = computed(() => [
  { key: 'pending', label: '待执行', icon: '📥', count: pendingList.value.length },
  { key: 'in_progress', label: '进行中', icon: '⚡', count: inProgressList.value.length },
  { key: 'overdue', label: '已逾期', icon: '🚨', count: overdueList.value.length },
  { key: 'completed', label: '已完成', icon: '✅' }
])

const pendingList = computed(() => instances.value.filter(e => e.status === 'pending'))
const inProgressList = computed(() => instances.value.filter(e => e.status === 'in_progress'))
const completedList = computed(() => instances.value.filter(e => e.status === 'completed'))
const overdueList = computed(() => instances.value.filter(e => e.status === 'overdue'))

const currentList = computed(() => {
  switch(tab.value) {
    case 'pending': return pendingList.value
    case 'in_progress': return inProgressList.value
    case 'overdue': return overdueList.value
    case 'completed': return completedList.value
    default: return []
  }
})

const emptySubText = computed(() => {
  const map: any = {
    pending: '暂无待执行任务。发布 SOP 后，系统将根据周期自动为您生成。',
    in_progress: '当前没有正在执行中的 SOP，开启一个新的流程吧。',
    overdue: '太棒了！目前没有任何逾期任务。',
    completed: '您还没有完成过任何 SOP 执行记录。'
  }
  return map[tab.value]
})

const statusLabel = (s: string) => ({
  pending: '等待激活', in_progress: '执行中', completed: '已圆满', overdue: '已逾期'
} as any)[s] || s

const parseDate = (dateStr: string | null | undefined): Date | null => {
  if (!dateStr || dateStr === 'null' || dateStr === 'undefined') return null
  const d = new Date(dateStr)
  return isNaN(d.getTime()) ? null : d
}

const formatDate = (d: string) => {
  const parsed = parseDate(d)
  if (!parsed) return ''
  return parsed.toLocaleDateString('zh-CN', { month: '2-digit', day: '2-digit' })
}

const formatPeriod = (inst: any) => {
  if (!inst.periodStart || !inst.periodEnd) return '单次执行'
  const s = parseDate(inst.periodStart)
  const e = parseDate(inst.periodEnd)
  if (!s || !e) return '单次执行'
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
      // Bulk fetch SOP info logic (simplified)
      const sopIds = [...new Set(instances.value.map((e: any) => e.sopId))]
      const sopMap: Record<number, any> = {}
      const results = await Promise.all(sopIds.map(id => request.get(`/sop/${id}`).catch(() => null)))
      results.forEach((r: any) => {
        if (r?.data) sopMap[r.data.id] = r.data
      })
      
      instances.value.forEach(inst => {
        const sop = sopMap[inst.sopId]
        inst.sopTitle = sop?.title || '未命名 SOP'
        try {
          const raw = sop?.content
          inst.totalSteps = JSON.parse(raw || '[]').length
        } catch { inst.totalSteps = 0 }
      })
    }
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.execution-list-page { padding: 0; max-width: 1200px; margin: 0; min-height: auto; }

/* Page Header */
.page-header { display: flex; justify-content: space-between; align-items: flex-end; margin-bottom: var(--space-xl); }
.page-header h1 { font-size: var(--font-size-4xl); font-weight: 800; margin: 0 0 4px 0; color: var(--color-text-primary); }
.subtitle { color: var(--color-text-secondary); font-size: var(--font-size-base); }

.stats-mini { display: flex; gap: 24px; }
.stat-item { display: flex; flex-direction: column; align-items: flex-end; }
.stat-item .val { font-size: 20px; font-weight: 800; color: var(--color-primary); }
.stat-item .label { font-size: 11px; font-weight: 600; color: var(--color-text-muted); text-transform: uppercase; }

/* Tabs */
.tab-nav { 
  display: flex; gap: 8px; margin-bottom: 32px; 
  padding: 6px; background: var(--color-bg-surface); 
  border-radius: 16px; width: fit-content;
}
.tab-btn {
  display: flex; align-items: center; gap: 8px; padding: 10px 18px;
  border: none; background: transparent; border-radius: 12px;
  font-size: 14px; font-weight: 600; color: var(--color-text-secondary);
  cursor: pointer; transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
}
.tab-btn:hover { background: rgba(255,255,255,0.5); }
.tab-btn.active {
  background: white; color: var(--color-primary);
  box-shadow: 0 4px 12px rgba(0,0,0,0.05);
}
.tab-btn .count {
  background: var(--color-primary-subtle); color: var(--color-primary);
  padding: 2px 6px; border-radius: 6px; font-size: 11px;
}

/* Grid */
.exec-grid {
  display: grid; grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 14px;
}

/* Glass Card */
.glass-card {
  background: var(--color-bg-elevated); border: 1px solid var(--color-border);
  border-radius: var(--radius-lg); padding: var(--space-xl); transition: all 0.3s ease;
  position: relative; overflow: hidden; cursor: pointer;
}
.glass-card:hover { 
  transform: translateY(-6px); 
  box-shadow: 0 20px 40px rgba(0,0,0,0.08); 
  border-color: var(--color-primary);
}

.card-top { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.category-tag { font-size: 10px; font-weight: 800; text-transform: uppercase; color: var(--color-primary); background: var(--color-primary-subtle); padding: 4px 10px; border-radius: 8px; }

.status-indicator { display: flex; align-items: center; gap: 6px; font-size: 12px; font-weight: 600; }
.status-indicator .dot { width: 8px; height: 8px; border-radius: 50%; background: #ccc; }
.in_progress .status-indicator { color: var(--color-primary); }
.in_progress .status-indicator .dot { background: var(--color-primary); box-shadow: 0 0 10px var(--color-primary); animation: pulse 2s infinite; }
.completed .status-indicator { color: var(--color-success); }
.completed .status-indicator .dot { background: var(--color-success); }
.overdue .status-indicator { color: var(--color-error); }
.overdue .status-indicator .dot { background: var(--color-error); }

.sop-title { font-size: 20px; font-weight: 700; margin: 0 0 12px 0; line-height: 1.3; }

.card-meta { display: flex; flex-direction: column; gap: 8px; margin-bottom: 24px; }
.meta-item { display: flex; align-items: center; gap: 8px; font-size: 13px; color: var(--color-text-secondary); }

.card-footer { margin-top: auto; border-top: 1px solid var(--color-border); padding-top: 20px; }
.progress-section { margin-bottom: 16px; }
.progress-header { display: flex; justify-content: space-between; margin-bottom: 8px; font-size: 12px; font-weight: 600; }
.progress-bar-wrap { height: 6px; background: var(--color-bg-surface); border-radius: 3px; overflow: hidden; }
.progress-bar-fill { height: 100%; background: linear-gradient(90deg, var(--color-primary), #7c3aed); border-radius: 3px; }

.success-seal { color: var(--color-success); font-weight: 700; font-style: italic; }
.pending-hint { color: var(--color-text-muted); font-size: 12px; }

.btn-action {
  width: 100%; height: 44px; border-radius: 12px; border: none;
  background: var(--color-bg-surface); color: var(--color-text-primary);
  font-weight: 700; font-size: 14px; display: flex; align-items: center;
  justify-content: center; gap: 10px; transition: all 0.2s;
}
.exec-card:hover .btn-action { background: var(--color-primary); color: white; }
.btn-action .arrow { opacity: 0; transition: transform 0.2s, opacity 0.2s; }
.exec-card:hover .btn-action .arrow { opacity: 1; transform: translateX(4px); }

/* Empty State Modern */
.empty-state-modern {
  position: relative; height: 400px; display: flex; align-items: center;
  justify-content: center; text-align: center; overflow: hidden;
  background: var(--color-bg-elevated); border-radius: 32px; border: 1px dashed var(--color-border);
}
.empty-glow {
  position: absolute; width: 300px; height: 300px; background: var(--color-primary-subtle);
  filter: blur(80px); border-radius: 50%; opacity: 0.5;
}
.empty-content { position: relative; z-index: 1; }
.empty-icon-wrap { font-size: 48px; margin-bottom: 20px; }
.empty-state-modern h3 { font-size: 22px; font-weight: 700; margin-bottom: 8px; }
.empty-state-modern p { color: var(--color-text-secondary); max-width: 400px; margin: 0 auto 24px; line-height: 1.6; }
.btn-cta { 
  background: var(--color-primary); color: white; border: none; padding: 12px 28px;
  border-radius: 16px; font-weight: 700; box-shadow: 0 10px 20px var(--color-primary-subtle);
}
.btn-cta-secondary {
  background: white; border: 1px solid var(--color-border); padding: 12px 28px;
  border-radius: 16px; font-weight: 700;
}

@keyframes pulse {
  0% { transform: scale(1); opacity: 1; }
  50% { transform: scale(1.5); opacity: 0.5; }
  100% { transform: scale(1); opacity: 1; }
}
@keyframes spin { to { transform: rotate(360deg); } }
</style>
