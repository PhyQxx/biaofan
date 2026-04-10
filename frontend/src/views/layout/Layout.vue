<template>
  <div class="app-layout" :class="{ 'dark-mode': isDarkRoute }">
    <!-- Header -->
    <header class="layout-header">
      <div class="header-left">
        <div class="logo" @click="router.push('/')">
          <span class="logo-icon">🚀</span>
          <span class="logo-text">标帆 SOP</span>
        </div>
      </div>
      <div class="header-right">
        <button v-if="isAdmin" class="btn-admin" @click="router.push('/admin/badges')">⚙️ 管理后台</button>
        <button class="btn-new" @click="router.push('/sop/new')">+ 新建 SOP</button>
        <div class="notif-bell" @click="router.push('/notification')">
          <span class="bell-icon">🔔</span>
          <span class="notif-badge" v-if="unreadNotif > 0">{{ unreadNotif > 9 ? '9+' : unreadNotif }}</span>
        </div>
        <div class="avatar-wrapper">
          <div class="avatar" @click="showUserMenu = !showUserMenu">{{ userInitial }}</div>
          <div class="user-menu" v-if="showUserMenu" @mouseleave="showUserMenu = false">
            <div class="menu-item" @click="showUserMenu = false; router.push('/profile')">
              <span>👤</span> 个人中心
            </div>
            <div class="menu-divider"></div>
            <div class="menu-item menu-item-danger" @click="showUserMenu = false; handleLogout()">
              <span>🚪</span> 退出登录
            </div>
          </div>
        </div>
      </div>
    </header>

    <!-- Body -->
    <div class="layout-body">
      <!-- Sidebar -->
      <aside class="layout-sidebar" :class="{ dark: isDarkRoute }">
        <div class="sidebar-item" :class="{ active: isActive('/') }" @click="router.push('/')">
          <span class="sidebar-icon">📊</span>
          <span>工作台</span>
        </div>
        <div class="sidebar-item" :class="{ active: isActive('/execution') }" @click="router.push('/execution')">
          <span class="sidebar-icon">▶️</span>
          <span>执行台</span>
        </div>
        <div class="sidebar-item" :class="{ active: isActive('/stats') }" @click="router.push('/stats')">
          <span class="sidebar-icon">📈</span>
          <span>统计</span>
        </div>
        <div class="sidebar-item" :class="{ active: isActive('/notification') }" @click="router.push('/notification')">
          <span class="sidebar-icon">🔔</span>
          <span>通知</span>
          <span class="sidebar-badge" v-if="unreadNotif > 0">{{ unreadNotif }}</span>
        </div>
        <div class="sidebar-divider"></div>
        <div class="sidebar-group-label">🏆 游戏化</div>
        <div class="sidebar-item" :class="{ active: isActive('/profile') }" @click="router.push('/profile')">
          <span class="sidebar-icon">👤</span>
          <span>个人中心</span>
        </div>
        <div class="sidebar-item" :class="{ active: isActive('/leaderboard') }" @click="router.push('/leaderboard')">
          <span class="sidebar-icon">🏆</span>
          <span>排行榜</span>
        </div>
        <div class="sidebar-divider"></div>
        <div class="sidebar-item logout-item" @click="handleLogout">
          <span class="sidebar-icon">🚪</span>
          <span>退出登录</span>
        </div>
      </aside>

      <!-- Main Content -->
      <main class="layout-main" :class="{ 'dark-theme': isDarkRoute, 'admin-dark': isAdminDarkRoute }">
        <RouterView />
      </main>
    </div>
  </div>
</template>

<script setup lang="ts">


/**
 * PC 端主布局组件
 * - 顶部 Header：Logo、新建 SOP 按钮、通知铃铛、用户头像
 * - 左侧 Sidebar：工作台 / 执行台 / 统计 / 通知 / 个人中心 / 排行榜
 * - 主内容区：<RouterView />
 * - 根据路由切换亮色/暗色主题（/admin、/profile、/leaderboard 走暗色）
 * - 未读通知数量实时显示
 */
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useNotificationStore } from '@/stores/notification'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()
const notifStore = useNotificationStore()
const showUserMenu = ref(false)

const user = computed(() => authStore.userInfo)
const isAdmin = computed(() => user.value?.role === 'admin')
const userInitial = computed(() => user.value?.username?.charAt(0)?.toUpperCase() || 'U')
const unreadNotif = computed(() => notifStore.unreadCount)
const isDarkRoute = computed(() =>
  route.path.startsWith('/profile') ||
  route.path.startsWith('/leaderboard') ||
  route.path.startsWith('/admin')
)
const isAdminDarkRoute = computed(() => route.path.startsWith('/admin'))

function isActive(path: string) {
  if (path === '/') return route.path === '/'
  return route.path.startsWith(path)
}

function handleLogout() {
  authStore.logout()
  router.push('/login')
}

onMounted(notifStore.fetchUnreadCount)
watch(() => route.path, notifStore.fetchUnreadCount)
</script>

<style scoped>
.app-layout {
  display: flex;
  flex-direction: column;
  height: 100vh;
  background: #f5f6fa;
}

.app-layout.dark-mode {
  background: #0f1117;
}

/* Header */
.layout-header {
  height: 56px;
  background: #fff;
  border-bottom: 1px solid #eee;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 100;
  box-shadow: 0 1px 3px rgba(0,0,0,0.05);
  transition: background 0.2s, border-color 0.2s;
}

.app-layout.dark-mode .layout-header {
  background: #1a1d27;
  border-bottom-color: #2d3348;
}

.header-left {
  display: flex;
  align-items: center;
}

.logo {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  user-select: none;
}

.logo-icon {
  font-size: 22px;
}

.logo-text {
  font-size: 16px;
  font-weight: 700;
  color: #222;
  letter-spacing: 0.5px;
  transition: color 0.2s;
}

.app-layout.dark-mode .logo-text {
  color: #e8eaf0;
}

.app-layout.dark-mode .btn-admin {
  background: #22263a;
  border-color: #2d3348;
  color: #8b90a0;
}
.app-layout.dark-mode .btn-admin:hover {
  background: #2d3348;
  color: #e8eaf0;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.btn-admin {
  padding: 6px 14px;
  background: #f0f0f0;
  border: 1px solid #ddd;
  border-radius: 6px;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-admin:hover {
  background: #e0e0e0;
}

.btn-new {
  padding: 7px 16px;
  background: #4f46e5;
  color: #fff;
  border: none;
  border-radius: 6px;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.2s;
}

.btn-new:hover {
  background: #4338ca;
}

.notif-bell {
  position: relative;
  cursor: pointer;
  font-size: 18px;
  padding: 4px;
}

.notif-badge {
  position: absolute;
  top: -2px;
  right: -4px;
  background: #ef4444;
  color: #fff;
  font-size: 10px;
  font-weight: 700;
  min-width: 16px;
  height: 16px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 3px;
}

.avatar {
  width: 32px;
  height: 32px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;
  transition: opacity 0.2s;
}

.avatar:hover {
  opacity: 0.85;
}

.avatar-wrapper {
  position: relative;
}

.user-menu {
  position: absolute;
  top: 40px;
  right: 0;
  background: #fff;
  border-radius: 10px;
  box-shadow: 0 4px 20px rgba(0,0,0,0.12);
  min-width: 140px;
  padding: 6px 0;
  z-index: 200;
}

.menu-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 16px;
  font-size: 14px;
  color: #333;
  cursor: pointer;
  transition: background 0.15s;
}

.menu-item:hover {
  background: #F5F7FA;
}

.menu-item-danger {
  color: #FF4D4F;
}

.menu-item-danger:hover {
  background: #FFF1F0;
}

.menu-divider {
  height: 1px;
  background: #F0F0F0;
  margin: 4px 0;
}

/* Body */
.layout-body {
  display: flex;
  flex: 1;
  margin-top: 56px;
}

/* Sidebar */
.layout-sidebar {
  width: 200px;
  background: #fff;
  border-right: 1px solid #eee;
  position: fixed;
  top: 56px;
  left: 0;
  bottom: 0;
  overflow-y: auto;
  padding: 12px 0;
  transition: background 0.2s, border-color 0.2s;
}

.layout-sidebar.dark {
  background: #1a1d27;
  border-right-color: #2d3348;
}

.sidebar-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 20px;
  font-size: 14px;
  color: #555;
  cursor: pointer;
  transition: all 0.15s;
  border-left: 3px solid transparent;
  position: relative;
}

.sidebar-item:hover {
  background: #f5f6fa;
  color: #333;
}

.sidebar-item.active {
  background: #eef2ff;
  color: #4f46e5;
  border-left-color: #4f46e5;
  font-weight: 600;
}

.sidebar-icon {
  font-size: 16px;
  width: 20px;
  text-align: center;
  flex-shrink: 0;
}

.sidebar-badge {
  margin-left: auto;
  background: #ef4444;
  color: #fff;
  font-size: 10px;
  font-weight: 700;
  min-width: 18px;
  height: 18px;
  border-radius: 9px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 4px;
}

.sidebar-divider {
  height: 1px;
  background: #eee;
  margin: 8px 16px;
}

.sidebar-group-label {
  padding: 6px 20px;
  font-size: 11px;
  font-weight: 700;
  color: #aaa;
  text-transform: uppercase;
  letter-spacing: 0.8px;
}

.logout-item {
  color: #999;
}

.logout-item:hover {
  color: #ef4444;
  background: #fef2f2;
}

/* Dark sidebar */
.layout-sidebar.dark {
  background: #1a1d27;
  border-right-color: #2d3348;
}
.layout-sidebar.dark .sidebar-item {
  color: #8b90a0;
}
.layout-sidebar.dark .sidebar-item:hover {
  background: #22263a;
  color: #e8eaf0;
}
.layout-sidebar.dark .sidebar-item.active {
  background: rgba(91, 127, 255, 0.1);
  color: #5b7fff;
  border-left-color: #5b7fff;
}
.layout-sidebar.dark .sidebar-divider {
  background: #2d3348;
}
.layout-sidebar.dark .sidebar-group-label {
  color: #555a6e;
}

/* Main */
.layout-main {
  flex: 1;
  margin-left: 200px;
  padding: 24px;
  overflow-y: auto;
  min-height: calc(100vh - 56px);
  transition: background 0.2s;
}

.layout-main.dark-theme {
  background: #0f1117;
  margin-left: 200px;
  padding-left: 24px;
  padding-right: 24px;
}

.layout-main.admin-dark {
  margin-left: 0;
}
</style>
