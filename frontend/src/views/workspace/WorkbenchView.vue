<template>
  <div class="workbench">
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

    <!-- Main -->
    <div class="main-layout">
      <!-- Sidebar -->
      <div class="sidebar">
        <div class="sidebar-item active">
          <span>📊</span><span>工作台</span>
        </div>
        <div class="sidebar-item" @click="router.push('/sops')">
          <span>📋</span><span>我的 SOP</span>
        </div>
        <div class="sidebar-divider"></div>
        <div class="sidebar-item" @click="handleLogout">
          <span>🚪</span><span>退出登录</span>
        </div>
      </div>

      <!-- Content -->
      <div class="main-content">
        <!-- Welcome -->
        <div class="welcome-card">
          <h2>{{ greeting }}，{{ user?.username || '朋友' }} 👋</h2>
          <p>开始管理你的 SOP 流程，让每一步都有标准</p>
          <button class="btn-primary" @click="router.push('/sop/new')">创建第一个 SOP</button>
        </div>

        <!-- Stats -->
        <div class="stats-row">
          <div class="stat-card">
            <div class="stat-num">{{ total }}</div>
            <div class="stat-label">我的 SOP</div>
          </div>
        </div>

        <!-- SOP List -->
        <div class="section-header">
          <h3>最近编辑</h3>
          <button class="btn-secondary" @click="router.push('/sops')">查看全部</button>
        </div>

        <div class="sop-grid" v-if="sops.length">
          <div v-for="sop in sops" :key="sop.id" class="sop-card" @click="router.push(`/sop/${sop.id}/edit`)">
            <div class="sop-category">{{ sop.category }}</div>
            <h4 class="sop-title">{{ sop.title }}</h4>
            <p class="sop-desc">{{ sop.description || '暂无描述' }}</p>
            <div class="sop-footer">
              <span class="sop-date">{{ formatDate(sop.updatedAt) }}</span>
              <span class="sop-status" :class="sop.status">{{ sop.status === 'published' ? '已发布' : '草稿' }}</span>
            </div>
          </div>
        </div>
        <div v-else class="empty-state">
          <p>还没有 SOP，创建一个开始吧！</p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useSopStore } from '@/stores/sop'

const router = useRouter()
const authStore = useAuthStore()
const sopStore = useSopStore()

const user = authStore.userInfo
const sops = ref<any[]>([])
const total = ref(0)

const greeting = computed(() => {
  const h = new Date().getHours()
  if (h < 12) return '上午好'
  if (h < 18) return '下午好'
  return '晚上好'
})

const formatDate = (d: string) => {
  const date = new Date(d)
  return `${date.getMonth()+1}/${date.getDate()} ${date.getHours()}:${String(date.getMinutes()).padStart(2,'0')}`
}

const handleLogout = () => {
  authStore.logout()
  router.push('/login')
}

onMounted(async () => {
  await authStore.fetchMe()
  const res: any = await sopStore.fetchMySops(1, 6)
  if (res.code === 200) {
    sops.value = res.data.records || []
    total.value = res.data.total || 0
  }
})
</script>

<style scoped>
.workbench { min-height: 100vh; background: #F5F7FA; }
.topbar {
  height: 56px; background: #fff;
  border-bottom: 1px solid #E8E8E8;
  display: flex; align-items: center; justify-content: space-between;
  padding: 0 20px; position: sticky; top: 0; z-index: 100;
}
.topbar-left { display: flex; align-items: center; gap: 24px; }
.logo { display: flex; align-items: center; gap: 8px; font-weight: 600; font-size: 16px; color: #212121; }
.logo-icon { font-size: 22px; }
.topbar-right { display: flex; align-items: center; gap: 12px; }
.btn-new {
  height: 36px; padding: 0 16px;
  background: #5B7FFF; color: white;
  border: none; border-radius: 8px;
  font-size: 14px; font-weight: 600; cursor: pointer;
}
.btn-new:hover { background: #7994FF; }
.avatar {
  width: 36px; height: 36px; background: #5B7FFF; color: white;
  border-radius: 50%; display: flex; align-items: center; justify-content: center;
  font-weight: 600; cursor: pointer;
}
.main-layout { display: flex; min-height: calc(100vh - 56px); }
.sidebar {
  width: 200px; background: #fff; border-right: 1px solid #E8E8E8;
  padding: 12px 0; flex-shrink: 0;
}
.sidebar-item {
  padding: 9px 16px; font-size: 14px; color: #666;
  cursor: pointer; display: flex; align-items: center; gap: 8px;
}
.sidebar-item:hover { background: #F5F7FA; color: #333; }
.sidebar-item.active { background: #E8ECFF; color: #5B7FFF; font-weight: 500; }
.sidebar-divider { height: 1px; background: #E8E8E8; margin: 8px 16px; }
.main-content { flex: 1; padding: 24px; overflow-y: auto; }
.welcome-card {
  background: linear-gradient(135deg, #5B7FFF, #7994FF);
  border-radius: 16px; padding: 32px; color: white; margin-bottom: 24px;
}
.welcome-card h2 { margin: 0 0 8px 0; font-size: 22px; font-weight: 600; }
.welcome-card p { margin: 0 0 20px 0; font-size: 14px; opacity: 0.9; }
.btn-primary {
  height: 40px; padding: 0 24px;
  background: white; color: #5B7FFF;
  border: none; border-radius: 8px;
  font-size: 15px; font-weight: 600; cursor: pointer;
}
.stats-row { display: flex; gap: 14px; margin-bottom: 24px; }
.stat-card {
  background: #fff; border-radius: 12px; padding: 20px 24px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.06);
}
.stat-num { font-size: 28px; font-weight: 700; color: #5B7FFF; }
.stat-label { font-size: 13px; color: #999; margin-top: 4px; }
.section-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.section-header h3 { margin: 0; font-size: 16px; font-weight: 600; color: #212121; }
.btn-secondary {
  height: 32px; padding: 0 14px;
  background: #fff; color: #333;
  border: 1.5px solid #E8E8E8; border-radius: 8px;
  font-size: 13px; cursor: pointer;
}
.btn-secondary:hover { background: #F5F7FA; }
.sop-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(280px, 1fr)); gap: 14px; }
.sop-card {
  background: #fff; border-radius: 12px; padding: 16px;
  cursor: pointer; transition: all 0.15s;
  border: 1px solid transparent;
}
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
</style>
