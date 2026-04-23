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
        <div class="avatar">{{ authStore.userInfo?.username?.charAt(0) || 'A' }}</div>
      </div>
    </div>

    <!-- Admin Body -->
    <div class="admin-body">
      <!-- Sidebar -->
      <div class="admin-sidebar">
        <div class="sidebar-section-title">🏪 模板市场</div>
        <div class="sidebar-item" :class="{ active: activeMenu === 'audit' }" @click="go('/admin/marketplace')">
          <span>📋</span><span>审核列表</span>
          <span v-if="pendingCount > 0" class="pending-badge">{{ pendingCount }}</span>
        </div>
        <div class="sidebar-item" :class="{ active: activeMenu === 'manage' }" @click="go('/admin/marketplace/manage')">
          <span>⚙️</span><span>上下架管理</span>
        </div>
        <div class="sidebar-item" :class="{ active: activeMenu === 'categories' }" @click="go('/admin/marketplace/categories')">
          <span>🏷️</span><span>分类管理</span>
        </div>

        <div class="sidebar-divider"></div>
        <div class="sidebar-section-title">游戏化配置</div>
        <div class="sidebar-item" :class="{ active: activeMenu === 'badges' }" @click="go('/admin/badges')">
          <span>🏅</span><span>徽章管理</span>
        </div>
        <div class="sidebar-item" :class="{ active: activeMenu === 'products' }" @click="go('/admin/products')">
          <span>🎁</span><span>商品管理</span>
        </div>
        <div class="sidebar-item" :class="{ active: activeMenu === 'rules' }" @click="go('/admin/rules')">
          <span>📈</span><span>成长规则</span>
        </div>
      </div>

      <!-- Content -->
      <div class="admin-content">
        <slot />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">


/**
 * 管理后台 - 市场管理布局
 * - 子菜单：审核 / 分类
 */
import { computed, onMounted, ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { getAuditList } from '@/api/marketplace'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()
const pendingCount = ref(0)

const activeMenu = computed(() => {
  const path = route.path
  if (path.includes('/marketplace/categories')) return 'categories'
  if (path.includes('/marketplace/manage')) return 'manage'
  if (path.includes('/marketplace')) return 'audit'
  if (path.includes('/badges')) return 'badges'
  if (path.includes('/products')) return 'products'
  if (path.includes('/rules')) return 'rules'
  return 'audit'
})

const go = (path: string) => router.push(path)

const fetchPendingCount = async () => {
  try {
    const res = await getAuditList({ status: 'pending' })
    if (res.success) pendingCount.value = res.data?.total || 0
  } catch (e) {
    console.error('[MarketplaceLayout] fetchPendingCount failed:', e)
  }
}

onMounted(() => {
  fetchPendingCount()
})
</script>

<style scoped>
.admin-layout {
  display: flex;
  flex-direction: column;
  height: 100vh;
  background: #0F1117;
  color: var(--color-text-primary);
}

.admin-topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 56px;
  padding: 0 var(--space-2xl);
  background: var(--color-bg-elevated);
  border-bottom: 1px solid var(--color-border);
  flex-shrink: 0;
}

.topbar-left { display: flex; align-items: center; }
.logo { display: flex; align-items: center; gap: var(--space-sm); }
.logo-icon { font-size: 22px; }
.logo-text { font-size: var(--font-size-xl); font-weight: 600; color: var(--color-text-primary); }
.admin-badge {
  background: linear-gradient(135deg, var(--color-primary), #3B5FDF);
  color: #fff;
  font-size: var(--font-size-xs);
  font-weight: 600;
  padding: 2px 8px;
  border-radius: 10px;
}

.topbar-right { display: flex; align-items: center; gap: var(--space-lg); }
.back-link { color: var(--color-text-secondary); font-size: var(--font-size-sm); text-decoration: none; transition: color var(--transition-normal); }
.back-link:hover { color: var(--color-primary); }
.avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: var(--color-primary);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: var(--font-size-sm);
  font-weight: 600;
  cursor: pointer;
}

.admin-body { display: flex; flex: 1; overflow: hidden; }

.admin-sidebar {
  width: 200px;
  background: var(--color-bg-elevated);
  border-right: 1px solid var(--color-border);
  padding: var(--space-xl) 0;
  flex-shrink: 0;
  overflow-y: auto;
}

.sidebar-section-title {
  font-size: var(--font-size-xs);
  font-weight: 600;
  color: var(--color-text-muted);
  text-transform: uppercase;
  letter-spacing: 0.5px;
  padding: 0 var(--space-lg) var(--space-sm);
}

.sidebar-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px var(--space-lg);
  font-size: var(--font-size-base);
  color: var(--color-text-secondary);
  cursor: pointer;
  transition: all var(--transition-normal);
  border-left: 3px solid transparent;
  position: relative;
}

.sidebar-item:hover { background: var(--color-bg-surface); color: var(--color-text-primary); }

.sidebar-item.active {
  background: rgba(91, 127, 255, 0.1);
  color: var(--color-primary);
  border-left-color: var(--color-primary);
}

.pending-badge {
  margin-left: auto;
  background: #EF4444;
  color: #fff;
  font-size: var(--font-size-xs);
  font-weight: 600;
  padding: 1px 6px;
  border-radius: 10px;
}

.sidebar-divider {
  height: 1px;
  background: var(--color-border);
  margin: var(--space-lg) 0;
}

.admin-content {
  flex: 1;
  overflow-y: auto;
  padding: var(--space-2xl);
}
</style>
