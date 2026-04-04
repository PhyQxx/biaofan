<template>
  <div class="stats-page">
    <div class="topbar">
      <div class="topbar-left">
        <div class="logo"><span class="logo-icon">🚀</span><span class="logo-text">标帆 SOP</span></div>
      </div>
      <div class="topbar-right">
        <button class="btn-new" @click="router.push('/sop/new')">+ 新建 SOP</button>
        <div class="avatar" @click="handleLogout">{{ user?.username?.charAt(0) || 'U' }}</div>
      </div>
    </div>

    <div class="main-layout">
      <div class="sidebar">
        <div class="sidebar-item" @click="router.push('/')"><span>📊</span><span>工作台</span></div>
        <div class="sidebar-item" @click="router.push('/execution')"><span>▶️</span><span>执行台</span></div>
        <div class="sidebar-item active"><span>📈</span><span>统计</span></div>
        <div class="sidebar-divider"></div>
        <div class="sidebar-item" @click="handleLogout"><span>🚪</span><span>退出登录</span></div>
      </div>

      <div class="main-content">
        <div class="page-header"><h1>统计概览</h1></div>

        <!-- Summary Cards -->
        <div class="summary-row">
          <div class="summary-card">
            <div class="summary-num">{{ totalExec }}</div>
            <div class="summary-label">总执行次数</div>
          </div>
          <div class="summary-card">
            <div class="summary-num success">{{ completedExec }}</div>
            <div class="summary-label">已完成</div>
          </div>
          <div class="summary-card">
            <div class="summary-num primary">{{ completionRate }}%</div>
            <div class="summary-label">完成率</div>
          </div>
        </div>

        <!-- SOP Stats List -->
        <div class="section-title">SOP 执行统计</div>
        <div class="stats-list" v-if="stats.length">
          <div v-for="s in stats" :key="s.id" class="stat-card">
            <div class="stat-sop-title">{{ s.sopTitle }}</div>
            <div class="stat-bar">
              <div class="stat-bar-fill" :style="{ width: completionRateOf(s) + '%' }"></div>
            </div>
            <div class="stat-meta">
              <span>执行 {{ s.totalCount }} 次</span>
              <span>完成 {{ s.completedCount }} 次</span>
              <span v-if="s.lastExecutedAt">最近 {{ formatDate(s.lastExecutedAt) }}</span>
            </div>
          </div>
        </div>
        <div v-else class="empty-state">
          <p>暂无统计数据</p>
          <p class="empty-sub">开始执行 SOP 后这里会显示统计</p>
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
const stats = ref<any[]>([])

const totalExec = computed(() => stats.value.reduce((a, s) => a + (s.totalCount || 0), 0))
const completedExec = computed(() => stats.value.reduce((a, s) => a + (s.completedCount || 0), 0))
const completionRate = computed(() => totalExec.value ? Math.round((completedExec.value / totalExec.value) * 100) : 0)
const completionRateOf = (s: any) => s.totalCount ? Math.round((s.completedCount / s.totalCount) * 100) : 0
const formatDate = (d: string) => d ? new Date(d).toLocaleDateString('zh-CN', { month: 'short', day: 'numeric' }) : '-'

const handleLogout = () => { authStore.logout(); router.push('/login') }

onMounted(async () => {
  await authStore.fetchMe()
  const res: any = await request.get('/stats/my')
  if (res.code === 200) {
    stats.value = res.data || []
    // Attach sop titles
    for (const s of stats.value) {
      try {
        const r: any = await request.get(`/sop/${s.sopId}`)
        s.sopTitle = r.code === 200 ? r.data.title : 'SOP'
      } catch { s.sopTitle = 'SOP' }
    }
  }
})
</script>

<style scoped>
.stats-page { min-height: 100vh; background: #F5F7FA; }
.topbar { height: 56px; background: #fff; border-bottom: 1px solid #E8E8E8; display: flex; align-items: center; justify-content: space-between; padding: 0 20px; position: sticky; top: 0; z-index: 100; }
.topbar-left { display: flex; align-items: center; gap: 24px; }
.logo { display: flex; align-items: center; gap: 8px; font-weight: 600; font-size: 16px; color: #212121; }
.logo-icon { font-size: 22px; }
.topbar-right { display: flex; align-items: center; gap: 12px; }
.btn-new { height: 36px; padding: 0 16px; background: #5B7FFF; color: white; border: none; border-radius: 8px; font-size: 14px; font-weight: 600; cursor: pointer; }
.avatar { width: 36px; height: 36px; background: #5B7FFF; color: white; border-radius: 50%; display: flex; align-items: center; justify-content: center; font-weight: 600; cursor: pointer; }
.main-layout { display: flex; min-height: calc(100vh - 56px); }
.sidebar { width: 200px; background: #fff; border-right: 1px solid #E8E8E8; padding: 12px 0; flex-shrink: 0; }
.sidebar-item { padding: 9px 16px; font-size: 14px; color: #666; cursor: pointer; display: flex; align-items: center; gap: 8px; }
.sidebar-item:hover { background: #F5F7FA; }
.sidebar-item.active { background: #E8ECFF; color: #5B7FFF; font-weight: 500; }
.sidebar-divider { height: 1px; background: #E8E8E8; margin: 8px 16px; }
.main-content { flex: 1; padding: 24px; overflow-y: auto; }
.page-header { margin-bottom: 20px; }
.page-header h1 { margin: 0; font-size: 22px; font-weight: 600; color: #212121; }
.summary-row { display: flex; gap: 14px; margin-bottom: 24px; }
.summary-card { flex: 1; background: #fff; border-radius: 12px; padding: 20px 24px; box-shadow: 0 1px 3px rgba(0,0,0,0.06); }
.summary-num { font-size: 32px; font-weight: 700; color: #333; }
.summary-num.success { color: #52C41A; }
.summary-num.primary { color: #5B7FFF; }
.summary-label { font-size: 13px; color: #999; margin-top: 4px; }
.section-title { font-size: 15px; font-weight: 600; color: #212121; margin-bottom: 14px; }
.stats-list { display: flex; flex-direction: column; gap: 12px; }
.stat-card { background: #fff; border-radius: 12px; padding: 16px; }
.stat-sop-title { font-size: 14px; font-weight: 600; color: #333; margin-bottom: 10px; }
.stat-bar { height: 8px; background: #F0F0F0; border-radius: 4px; overflow: hidden; margin-bottom: 8px; }
.stat-bar-fill { height: 100%; background: #5B7FFF; border-radius: 4px; transition: width 0.3s; }
.stat-meta { display: flex; gap: 16px; font-size: 12px; color: #999; }
.empty-state { text-align: center; padding: 40px; color: #999; }
.empty-sub { font-size: 13px; margin-top: 6px; }
</style>
