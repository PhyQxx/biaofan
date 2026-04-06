<template>
  <div class="admin-layout">
    <!-- Admin Topbar -->
    <div class="admin-topbar">
      <div class="topbar-left">
        <div class="logo">
          <span class="logo-icon">🚀</span>
          <span class="logo-text">标帆 SOP</span>
          <span class="admin-badge">管理后台</span>
        </div>
      </div>
      <div class="topbar-right">
        <router-link to="/" class="back-link">← 返回工作台</router-link>
        <div class="avatar" @click="handleLogout">{{ authStore.userInfo?.username?.charAt(0) || 'A' }}</div>
      </div>
    </div>

    <!-- Admin Body -->
    <div class="admin-body">
      <!-- Admin Sidebar -->
      <div class="admin-sidebar">
        <div class="sidebar-section-title">游戏化配置</div>
        <div
          class="sidebar-item"
          :class="{ active: activeMenu === 'badges' }"
          @click="go('/admin/badges')"
        >
          <span>🏅</span><span>徽章管理</span>
        </div>
        <div
          class="sidebar-item"
          :class="{ active: activeMenu === 'products' }"
          @click="go('/admin/products')"
        >
          <span>🎁</span><span>商品管理</span>
        </div>
        <div
          class="sidebar-item"
          :class="{ active: activeMenu === 'rules' }"
          @click="go('/admin/rules')"
        >
          <span>📈</span><span>成长规则</span>
        </div>
      </div>

      <!-- Admin Content -->
      <div class="admin-content">
        <slot />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

const activeMenu = computed(() => {
  const path = route.path
  if (path.includes('/badges')) return 'badges'
  if (path.includes('/products')) return 'products'
  if (path.includes('/rules')) return 'rules'
  return 'badges'
})

const go = (path: string) => router.push(path)

const handleLogout = () => {
  localStorage.removeItem('bf_token')
  router.push('/login')
}
</script>

<style scoped>
.admin-layout {
  display: flex;
  flex-direction: column;
  height: 100vh;
  background: #0F1117;
  color: #E8EAF0;
}

.admin-topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 56px;
  padding: 0 24px;
  background: #1A1D27;
  border-bottom: 1px solid #2D3348;
  flex-shrink: 0;
}

.topbar-left {
  display: flex;
  align-items: center;
}

.logo {
  display: flex;
  align-items: center;
  gap: 8px;
}

.logo-icon {
  font-size: 22px;
}

.logo-text {
  font-size: 16px;
  font-weight: 600;
  color: #E8EAF0;
}

.admin-badge {
  background: linear-gradient(135deg, #5B7FFF, #3B5FDF);
  color: #fff;
  font-size: 11px;
  font-weight: 600;
  padding: 2px 8px;
  border-radius: 10px;
}

.topbar-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.back-link {
  color: #8B90A0;
  font-size: 13px;
  transition: color 0.2s;
}

.back-link:hover {
  color: #5B7FFF;
}

.avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: #5B7FFF;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.2s;
}

.avatar:hover {
  background: #7B9FFF;
}

.admin-body {
  display: flex;
  flex: 1;
  overflow: hidden;
}

.admin-sidebar {
  width: 200px;
  background: #1A1D27;
  border-right: 1px solid #2D3348;
  padding: 20px 0;
  flex-shrink: 0;
  overflow-y: auto;
}

.sidebar-section-title {
  font-size: 11px;
  font-weight: 600;
  color: #555A6E;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  padding: 0 16px 8px;
}

.sidebar-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 16px;
  font-size: 14px;
  color: #8B90A0;
  cursor: pointer;
  transition: all 0.2s;
  border-left: 3px solid transparent;
}

.sidebar-item:hover {
  background: #22263A;
  color: #E8EAF0;
}

.sidebar-item.active {
  background: rgba(91, 127, 255, 0.1);
  color: #5B7FFF;
  border-left-color: #5B7FFF;
}

.admin-content {
  flex: 1;
  overflow-y: auto;
  padding: 24px;
}
</style>
