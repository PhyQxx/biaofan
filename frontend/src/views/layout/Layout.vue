<template>
  <div class="app-layout" :class="{ 'dark-mode': isDark }">
    <!-- Header -->
    <header class="layout-header">
      <div class="header-left">
        <div class="logo" @click="router.push('/')">
          <span class="logo-icon">🚀</span>
          <span class="logo-text">标帆 SOP</span>
        </div>

        <!-- Space Switcher -->
        <div class="space-switcher" v-if="user">
          <div class="current-space" @click="showSpaceMenu = !showSpaceMenu">
            <span class="space-icon">{{ currentOrg ? '🏢' : '👤' }}</span>
            <span class="space-name">{{ currentOrg ? currentOrg.name : '个人空间' }}</span>
            <span class="chevron">▾</span>
          </div>
          <div class="space-menu" v-if="showSpaceMenu" @mouseleave="showSpaceMenu = false">
            <div class="menu-label">切换空间</div>
            <div 
              class="space-item" 
              :class="{ active: !currentOrgId }"
              @click="handleSwitchOrg(null)"
            >
              <span class="item-icon">👤</span> 个人私有空间
            </div>
            <div class="menu-divider" v-if="userOrganizations.length > 0"></div>
            <div 
              v-for="org in userOrganizations" 
              :key="org.id" 
              class="space-item"
              :class="{ active: currentOrgId === org.id }"
              @click="handleSwitchOrg(org.id)"
            >
              <span class="item-icon">🏢</span> {{ org.name }}
            </div>
            <div class="menu-divider"></div>
            <div class="space-item join-org" @click="router.push('/org/join'); showSpaceMenu = false">
              <span class="item-icon">➕</span> 加入/创建组织
            </div>
          </div>
        </div>
      </div>
      <div class="header-right">
        <!-- Theme Toggle -->
        <button class="btn-icon theme-toggle" @click="toggleTheme" :title="isDark ? '切换到明亮模式' : '切换到暗黑模式'">
          <span v-if="isDark">☀️</span>
          <span v-else>🌙</span>
        </button>

        <button class="btn-new" @click="router.push('/sop/new')" aria-label="新建 SOP">+ 新建 SOP</button>
        
        <div class="notif-bell" @click="router.push('/notification')" role="button" aria-label="查看通知">
          <span class="bell-icon">🔔</span>
          <span class="notif-badge" v-if="unreadNotif > 0">{{ unreadNotif > 9 ? '9+' : unreadNotif }}</span>
        </div>

        <div class="avatar-wrapper">
          <div class="avatar" @click="showUserMenu = !showUserMenu" role="button" aria-label="用户菜单">{{ userInitial }}</div>
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
      <aside class="layout-sidebar">
        <div class="sidebar-scroll">
          <div class="sidebar-item" :class="{ active: isActive('/') }" @click="router.push('/')" role="button">
            <span class="sidebar-icon">📊</span>
            <span>工作台</span>
          </div>
          <div class="sidebar-item" :class="{ active: isActive('/execution') }" @click="router.push('/execution')" role="button">
            <span class="sidebar-icon">▶️</span>
            <span>执行台</span>
          </div>
          <div class="sidebar-item" :class="{ active: isActive('/stats') }" @click="router.push('/stats')" role="button">
            <span class="sidebar-icon">📈</span>
            <span>统计</span>
          </div>
          <div class="sidebar-item" v-if="currentOrgId" :class="{ active: isActive('/org/approvals') }" @click="router.push('/org/approvals')" role="button">
            <span class="sidebar-icon">📝</span>
            <span>审核中心</span>
          </div>
          <div class="sidebar-item" :class="{ active: isActive('/notification') }" @click="router.push('/notification')" role="button">
            <span class="sidebar-icon">🔔</span>
            <span>通知</span>
            <span class="sidebar-badge" v-if="unreadNotif > 0">{{ unreadNotif }}</span>
          </div>
          
          <div class="sidebar-divider"></div>
          <div class="sidebar-group-label">🏆 游戏化</div>
          <div class="sidebar-item" :class="{ active: isActive('/profile') }" @click="router.push('/profile')" role="button">
            <span class="sidebar-icon">👤</span>
            <span>个人中心</span>
          </div>
          <div class="sidebar-item" :class="{ active: isActive('/leaderboard') }" @click="router.push('/leaderboard')" role="button">
            <span class="sidebar-icon">🏆</span>
            <span>排行榜</span>
          </div>

          <!-- 管理后台入口 (仅管理员可见，放在最下面) -->
          <template v-if="isAdmin">
            <div class="sidebar-divider"></div>
            <div class="sidebar-group-label">⚙️ 管理后台</div>
            <div class="sidebar-item" :class="{ active: isActive('/admin/badges') }" @click="router.push('/admin/badges')" role="button">
              <span class="sidebar-icon">🏅</span>
              <span>徽章管理</span>
            </div>
            <div class="sidebar-item" :class="{ active: isActive('/admin/products') }" @click="router.push('/admin/products')" role="button">
              <span class="sidebar-icon">📦</span>
              <span>商品管理</span>
            </div>
            <div class="sidebar-item" :class="{ active: isActive('/admin/rules') }" @click="router.push('/admin/rules')" role="button">
              <span class="sidebar-icon">📏</span>
              <span>规则管理</span>
            </div>
            <div class="sidebar-item" :class="{ active: isActive('/admin/marketplace') }" @click="router.push('/admin/marketplace')" role="button">
              <span class="sidebar-icon">🛒</span>
              <span>市场审核</span>
            </div>
            <div class="sidebar-item" :class="{ active: isActive('/admin/ai-config') }" @click="router.push('/admin/ai-config')" role="button">
              <span class="sidebar-icon">🤖</span>
              <span>AI 配置</span>
            </div>
            <div class="sidebar-item" :class="{ active: isActive('/admin/email-config') }" @click="router.push('/admin/email-config')" role="button">
              <span class="sidebar-icon">📧</span>
              <span>邮件配置</span>
            </div>
          </template>
        </div>
      </aside>

      <!-- Main Content -->
      <main class="layout-main">
        <RouterView />
      </main>
    </div>

    <!-- AI Knowledge Assistant: Only visible in Org Space -->
    <OrgKnowledgeAssistant v-if="user && currentOrgId" />
  </div>
</template>

<script setup lang="ts">


/**
 * PC 端主布局组件 - 统一风格并支持主题切换
 */
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useNotificationStore } from '@/stores/notification'
import OrgKnowledgeAssistant from '@/components/ai/OrgKnowledgeAssistant.vue'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()
const notifStore = useNotificationStore()
const showUserMenu = ref(false)
const showSpaceMenu = ref(false)

// 主题管理
const isDark = ref(localStorage.getItem('bf_theme') === 'dark')

function applyTheme(dark: boolean) {
  if (dark) {
    document.documentElement.classList.add('dark-mode')
  } else {
    document.documentElement.classList.remove('dark-mode')
  }
}

function toggleTheme() {
  isDark.value = !isDark.value
  localStorage.setItem('bf_theme', isDark.value ? 'dark' : 'light')
  applyTheme(isDark.value)
}

const user = computed(() => authStore.userInfo)
const userOrganizations = computed(() => authStore.userOrganizations)
const currentOrgId = computed(() => authStore.currentOrgId)
const currentOrg = computed(() => authStore.currentOrg)

const isAdmin = computed(() => user.value?.role === 'admin')
const userInitial = computed(() => user.value?.username?.charAt(0)?.toUpperCase() || 'U')
const unreadNotif = computed(() => notifStore.unreadCount)

function isActive(path: string) {
  if (path === '/') return route.path === '/'
  return route.path.startsWith(path)
}

function handleLogout() {
  authStore.logout()
  router.push('/login')
}

function handleSwitchOrg(id: number | null) {
  authStore.switchOrg(id)
  showSpaceMenu.value = false
  router.replace({ path: route.path, query: { ...route.query, _t: Date.now() } })
}

onMounted(() => {
  applyTheme(isDark.value)
  notifStore.fetchUnreadCount()
  if (authStore.token) {
    authStore.fetchMyOrgs()
  }
})

watch(() => route.path, (newPath) => {
  if (['/execution', '/instance', '/notification'].some(p => newPath.startsWith(p))) {
    notifStore.fetchUnreadCount()
  }
}, { immediate: false })
</script>

<style scoped>
.app-layout {
  display: flex;
  flex-direction: column;
  height: 100vh;
  background: var(--color-bg-base);
  color: var(--color-text-primary);
  transition: background 0.3s, color 0.3s;
}

/* Header */
.layout-header {
  height: 56px;
  background: var(--color-bg-elevated);
  border-bottom: 1px solid var(--color-border);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 100;
  box-shadow: var(--shadow-sm);
}

.header-left { display: flex; align-items: center; gap: 24px; }
.logo { display: flex; align-items: center; gap: 8px; cursor: pointer; user-select: none; }
.logo-icon { font-size: 22px; }
.logo-text { font-size: 16px; font-weight: 700; color: var(--color-text-primary); letter-spacing: 0.5px; }

/* Space Switcher */
.space-switcher { position: relative; margin-left: 8px; padding-left: 16px; border-left: 1px solid var(--color-border); }
.current-space {
  display: flex; align-items: center; gap: 8px; padding: 6px 12px;
  background: var(--color-bg-surface); border-radius: 8px; cursor: pointer;
}
.space-name { font-size: 13px; font-weight: 600; max-width: 120px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.chevron { font-size: 12px; color: var(--color-text-muted); }

.space-menu {
  position: absolute; top: 42px; left: 16px;
  background: var(--color-bg-elevated); border-radius: 10px;
  box-shadow: var(--shadow-lg); min-width: 200px; padding: 8px 0;
  z-index: 200; border: 1px solid var(--color-border);
}
.menu-label { padding: 8px 16px; font-size: 11px; font-weight: 700; color: var(--color-text-muted); text-transform: uppercase; }
.space-item {
  display: flex; align-items: center; gap: 10px; padding: 10px 16px;
  font-size: 14px; color: var(--color-text-primary); cursor: pointer;
}
.space-item:hover { background: var(--color-bg-surface); }
.space-item.active { color: var(--color-primary); background: var(--color-primary-subtle); font-weight: 600; }

.header-right { display: flex; align-items: center; gap: 16px; }

.btn-icon {
  background: none; border: none; font-size: 18px; padding: 6px;
  border-radius: 50%; cursor: pointer; transition: background 0.2s;
  display: flex; align-items: center; justify-content: center;
}
.btn-icon:hover { background: var(--color-bg-surface); }

.btn-new {
  padding: 8px 16px; background: var(--color-primary); color: #fff;
  border: none; border-radius: 8px; font-size: 13px; font-weight: 600;
}
.btn-new:hover { background: var(--color-primary-hover); }

.notif-bell { position: relative; cursor: pointer; font-size: 20px; }
.notif-badge {
  position: absolute; top: -4px; right: -6px;
  background: var(--color-error); color: #fff; font-size: 10px;
  font-weight: 700; min-width: 16px; height: 16px; border-radius: 8px;
  display: flex; align-items: center; justify-content: center; padding: 0 3px;
}

.avatar {
  width: 32px; height: 32px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff; border-radius: 50%; display: flex; align-items: center;
  justify-content: center; font-size: 13px; font-weight: 700; cursor: pointer;
}
.avatar-wrapper { position: relative; }
.user-menu {
  position: absolute; top: 40px; right: 0;
  background: var(--color-bg-elevated); border-radius: 10px;
  box-shadow: var(--shadow-md); min-width: 140px; padding: 6px 0; z-index: 200;
  border: 1px solid var(--color-border);
}
.menu-item {
  display: flex; align-items: center; gap: 8px; padding: 10px 16px;
  font-size: 14px; color: var(--color-text-primary); cursor: pointer;
}
.menu-item:hover { background: var(--color-bg-surface); }
.menu-item-danger { color: var(--color-error); }
.menu-divider { height: 1px; background: var(--color-border); margin: 4px 0; }

/* Body */
.layout-body { display: flex; flex: 1; margin-top: 56px; }

/* Sidebar */
.layout-sidebar {
  width: 210px; background: var(--color-bg-elevated);
  border-right: 1px solid var(--color-border);
  position: fixed; top: 56px; left: 0; bottom: 0;
  overflow-y: hidden;
}
.sidebar-scroll { height: 100%; overflow-y: auto; padding: 12px 0; }
.sidebar-item {
  display: flex; align-items: center; gap: 12px; padding: 10px 20px;
  font-size: 14px; color: var(--color-text-secondary); cursor: pointer;
  transition: all 0.2s; border-left: 3px solid transparent;
}
.sidebar-item:hover { background: var(--color-bg-surface); color: var(--color-text-primary); }
.sidebar-item.active {
  background: var(--color-primary-subtle); color: var(--color-primary);
  border-left-color: var(--color-primary); font-weight: 600;
}
.sidebar-icon { font-size: 16px; width: 20px; text-align: center; }
.sidebar-badge {
  margin-left: auto; background: var(--color-error); color: #fff;
  font-size: 10px; font-weight: 700; min-width: 18px; height: 18px;
  border-radius: 9px; display: flex; align-items: center; justify-content: center;
}
.sidebar-divider { height: 1px; background: var(--color-border); margin: 8px 16px; }
.sidebar-group-label { padding: 12px 20px 6px; font-size: 11px; font-weight: 700; color: var(--color-text-muted); text-transform: uppercase; letter-spacing: 1px; }

/* Main */
.layout-main { flex: 1; margin-left: 210px; padding: 24px; min-height: calc(100vh - 56px); transition: background 0.3s; }
</style>
