<template>
  <div v-if="loading" class="loading-state">
    <div class="welcome-card">
      <el-skeleton variant="text" width="200px" height="32px" style="margin-bottom:8px" />
      <el-skeleton variant="text" width="280px" height="20px" />
    </div>
    <div class="stats-row">
      <div class="stat-card" v-for="i in 4" :key="i">
        <el-skeleton variant="text" width="60px" height="32px" style="margin-bottom:8px" />
        <el-skeleton variant="text" width="80px" height="16px" />
      </div>
    </div>
    <el-skeleton :rows="6" animated />
  </div>
  <div v-else>
    <div class="welcome-card">
      <h2>{{ greeting }}，{{ user?.username || '朋友' }} 👋</h2>
      <p>开始管理你的 SOP 流程，让每一步都有标准</p>
      <button class="btn-primary" @click="router.push('/sop/new')">创建第一个 SOP</button>
    </div>

    <div class="stats-row">
      <div class="stat-card">
        <div class="stat-num">{{ total }}</div>
        <div class="stat-label">我的 SOP</div>
      </div>
      <div class="stat-card">
        <div class="stat-num blue">{{ pendingExec }}</div>
        <div class="stat-label">待执行</div>
      </div>
      <div class="stat-card">
        <div class="stat-num orange">{{ inProgressExec }}</div>
        <div class="stat-label">执行中</div>
      </div>
      <div class="stat-card">
        <div class="stat-num green">{{ completedRate }}%</div>
        <div class="stat-label">完成率</div>
      </div>
    </div>

    <div class="section-header">
      <h3>最近编辑</h3>
      <button class="btn-secondary" @click="router.push('/sops')">查看全部</button>
    </div>

    <div class="sop-grid" v-if="sops.length">
      <div v-for="sop in sops" :key="sop.id" class="sop-card">
        <div class="sop-category">{{ ({daily:'日SOP',weekly:'周SOP',monthly:'月SOP',yearly:'年SOP'} as any)[sop.category] || sop.category }}</div>
        <h4 class="sop-title" @click="router.push(`/sop/${sop.id}/edit`)" style="cursor:pointer">{{ sop.title }}</h4>
        <p class="sop-desc">{{ sop.description || '暂无描述' }}</p>
        <div class="sop-footer">
          <span class="sop-date">{{ formatDate(sop.updatedAt) }}</span>
          <span class="sop-status" :class="sop.status">{{ sop.status === 'published' ? '已发布' : '草稿' }}</span>
        </div>
        <div class="sop-actions">
          <button class="btn-xs" @click.stop="router.push(`/sop/${sop.id}/versions`)">📋 版本历史</button>
          <button class="btn-xs" @click.stop="router.push(`/sop/${sop.id}/edit`)">✏️ 编辑</button>
          <button class="btn-xs btn-start" v-if="sop.status === 'published'" @click.stop="router.push('/execution')">▶️ 去执行</button>
        </div>
      </div>
    </div>
    <div v-else class="empty-state">
      <p>还没有 SOP，创建一个开始吧！</p>
    </div>
  </div>
</template>

<script setup lang="ts">


/**
 * PC 端工作台首页
 * - 今日任务统计卡片
 * - 最近执行的 SOP 列表
 * - 快捷操作入口
 */
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useSopStore } from '@/stores/sop'
import request from '@/api'

const router = useRouter()
const authStore = useAuthStore()
const sopStore = useSopStore()

const loading = ref(true)
const user = computed(() => authStore.userInfo)
const sops = ref<any[]>([])
const total = ref(0)
const pendingExec = ref(0)
const inProgressExec = ref(0)
const completedRate = ref(0)

const greeting = computed(() => {
  const h = new Date().getHours()
  if (h < 12) return '上午好'
  if (h < 18) return '下午好'
  return '晚上好'
})

const formatDate = (d: string) => {
  if (!d) return '-'
  const date = new Date(d)
  return `${date.getMonth()+1}/${date.getDate()} ${date.getHours()}:${String(date.getMinutes()).padStart(2,'0')}`
}

onMounted(async () => {
  try {
    await authStore.fetchMe()
    const res: any = await sopStore.fetchMySops(1, 6)
    if (res.code === 200) {
      sops.value = res.data.records || []
      total.value = res.data.total || 0
    }
    try {
      const instRes: any = await request.get('/instance/my')
      if (instRes.code === 200) {
        const insts = Array.isArray(instRes.data?.records) ? instRes.data.records : []
        pendingExec.value = insts.filter((e: any) => e.status === 'pending').length
        inProgressExec.value = insts.filter((e: any) => e.status === 'in_progress').length
        const completed = insts.filter((e: any) => e.status === 'completed').length
        completedRate.value = insts.length ? Math.round((completed / insts.length) * 100) : 0
      }
    } catch (e) {
      console.error('[WorkbenchView] fetchStats failed:', e)
    }
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.welcome-card { background: linear-gradient(135deg, var(--color-primary), #7994FF); border-radius: var(--radius-lg); padding: 32px; color: white; margin-bottom: var(--space-xl); }
.welcome-card h2 { margin: 0 0 8px 0; font-size: var(--font-size-4xl); font-weight: 600; }
.welcome-card p { margin: 0 0 20px 0; font-size: var(--font-size-base); opacity: 0.9; }
.btn-primary { height: 40px; padding: 0 24px; background: white; color: var(--color-primary); border: none; border-radius: var(--radius-md); font-size: var(--font-size-lg); font-weight: 600; cursor: pointer; }
.stats-row { display: flex; gap: 14px; margin-bottom: var(--space-xl); }
.stat-card { flex: 1; background: var(--color-bg-light-elevated); border-radius: var(--radius-lg); padding: 20px 24px; box-shadow: 0 1px 3px rgba(0,0,0,0.06); }
.stat-num { font-size: 28px; font-weight: 700; color: var(--color-text-light-primary); }
.stat-num.blue { color: var(--color-primary); }
.stat-num.orange { color: var(--color-warning); }
.stat-num.green { color: var(--color-success); }
.stat-label { font-size: var(--font-size-sm); color: var(--color-text-light-muted); margin-top: 4px; }
.section-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: var(--space-lg); }
.section-header h3 { margin: 0; font-size: var(--font-size-xl); font-weight: 600; color: var(--color-text-light-primary); }
.btn-secondary { height: 32px; padding: 0 14px; background: var(--color-bg-light-elevated); color: var(--color-text-light-primary); border: 1.5px solid var(--color-border-light); border-radius: var(--radius-md); font-size: var(--font-size-sm); cursor: pointer; }
.btn-secondary:hover { background: var(--color-bg-light); }
.sop-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(280px, 1fr)); gap: 14px; }
.sop-card { background: var(--color-bg-light-elevated); border-radius: var(--radius-lg); padding: 16px; cursor: pointer; transition: var(--transition-fast); border: 1px solid transparent; }
.sop-card:hover { border-color: #D0D8FF; box-shadow: 0 6px 16px rgba(91,127,255,0.12); }
.sop-category { font-size: var(--font-size-xs); color: var(--color-primary); font-weight: 500; margin-bottom: 6px; }
.sop-title { margin: 0 0 6px 0; font-size: var(--font-size-lg); font-weight: 600; color: var(--color-text-light-primary); }
.sop-desc { margin: 0 0 12px 0; font-size: var(--font-size-sm); color: var(--color-text-light-muted); display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; }
.sop-footer { display: flex; justify-content: space-between; align-items: center; padding-top: 10px; border-top: 1px solid #F0F0F0; }
.sop-date { font-size: var(--font-size-xs); color: var(--color-text-light-muted); }
.sop-status { font-size: var(--font-size-xs); padding: 2px 8px; border-radius: var(--radius-sm); }
.sop-status.published { background: #E8F3FF; color: var(--color-primary); }
.sop-status.draft { background: #F5F5F5; color: var(--color-text-light-muted); }
.empty-state { text-align: center; padding: 40px; color: var(--color-text-light-muted); }
.sop-actions { display: flex; gap: 6px; margin-top: 10px; padding-top: 10px; border-top: 1px solid #F0F0F0; }
.btn-xs { height: 26px; padding: 0 10px; background: var(--color-bg-light-elevated); color: var(--color-text-light-secondary); border: 1px solid var(--color-border-light); border-radius: var(--radius-sm); font-size: var(--font-size-xs); cursor: pointer; }
.btn-xs:hover { background: var(--color-bg-light); }
.btn-primary-xs { background: #E8ECFF; color: var(--color-primary); border-color: #D0D8FF; }
.btn-primary-xs:hover { background: #D0D8FF; }
.btn-start { background: #E8F3FF; color: var(--color-primary); border-color: #D0D8FF; }
.btn-start:hover { background: #D0D8FF; }
</style>
