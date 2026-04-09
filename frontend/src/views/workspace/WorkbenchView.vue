<template>
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
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useSopStore } from '@/stores/sop'
import request from '@/api'

const router = useRouter()
const authStore = useAuthStore()
const sopStore = useSopStore()

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
  await authStore.fetchMe()
  const res: any = await sopStore.fetchMySops(1, 6)
  if (res.code === 200) {
    sops.value = res.data.records || []
    total.value = res.data.total || 0
  }
  try {
    const instRes: any = await request.get('/instance/my')
    if (instRes.code === 200) {
      const insts = instRes.data || []
      pendingExec.value = insts.filter((e: any) => e.status === 'pending').length
      inProgressExec.value = insts.filter((e: any) => e.status === 'in_progress').length
      const completed = insts.filter((e: any) => e.status === 'completed').length
      completedRate.value = insts.length ? Math.round((completed / insts.length) * 100) : 0
    }
  } catch {}
})
</script>

<style scoped>
.welcome-card { background: linear-gradient(135deg, #5B7FFF, #7994FF); border-radius: 16px; padding: 32px; color: white; margin-bottom: 24px; }
.welcome-card h2 { margin: 0 0 8px 0; font-size: 22px; font-weight: 600; }
.welcome-card p { margin: 0 0 20px 0; font-size: 14px; opacity: 0.9; }
.btn-primary { height: 40px; padding: 0 24px; background: white; color: #5B7FFF; border: none; border-radius: 8px; font-size: 15px; font-weight: 600; cursor: pointer; }
.stats-row { display: flex; gap: 14px; margin-bottom: 24px; }
.stat-card { flex: 1; background: #fff; border-radius: 12px; padding: 20px 24px; box-shadow: 0 1px 3px rgba(0,0,0,0.06); }
.stat-num { font-size: 28px; font-weight: 700; color: #333; }
.stat-num.blue { color: #5B7FFF; }
.stat-num.orange { color: #FA8C16; }
.stat-num.green { color: #52C41A; }
.stat-label { font-size: 13px; color: #999; margin-top: 4px; }
.section-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.section-header h3 { margin: 0; font-size: 16px; font-weight: 600; color: #212121; }
.btn-secondary { height: 32px; padding: 0 14px; background: #fff; color: #333; border: 1.5px solid #E8E8E8; border-radius: 8px; font-size: 13px; cursor: pointer; }
.btn-secondary:hover { background: #F5F7FA; }
.sop-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(280px, 1fr)); gap: 14px; }
.sop-card { background: #fff; border-radius: 12px; padding: 16px; cursor: pointer; transition: all 0.15s; border: 1px solid transparent; }
.sop-card:hover { border-color: #D0D8FF; box-shadow: 0 6px 16px rgba(91,127,255,0.12); }
.sop-category { font-size: 11px; color: #5B7FFF; font-weight: 500; margin-bottom: 6px; }
.sop-title { margin: 0 0 6px 0; font-size: 15px; font-weight: 600; color: #212121; }
.sop-desc { margin: 0 0 12px 0; font-size: 13px; color: #999; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; }
.sop-footer { display: flex; justify-content: space-between; align-items: center; padding-top: 10px; border-top: 1px solid #F0F0F0; }
.sop-date { font-size: 12px; color: #999; }
.sop-status { font-size: 11px; padding: 2px 8px; border-radius: 4px; }
.sop-status.published { background: #E8F3FF; color: #5B7FFF; }
.sop-status.draft { background: #F5F5F5; color: #999; }
.empty-state { text-align: center; padding: 40px; color: #999; }
.sop-actions { display: flex; gap: 6px; margin-top: 10px; padding-top: 10px; border-top: 1px solid #F0F0F0; }
.btn-xs { height: 26px; padding: 0 10px; background: #fff; color: #666; border: 1px solid #E8E8E8; border-radius: 6px; font-size: 11px; cursor: pointer; }
.btn-xs:hover { background: #F5F7FA; }
.btn-primary-xs { background: #E8ECFF; color: #5B7FFF; border-color: #D0D8FF; }
.btn-primary-xs:hover { background: #D0D8FF; }
.btn-start { background: #E8F3FF; color: #5B7FFF; border-color: #D0D8FF; }
.btn-start:hover { background: #D0D8FF; }
</style>
