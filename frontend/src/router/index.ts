import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/login', name: 'Login', component: () => import('@/views/auth/LoginView.vue') },
    { path: '/register', name: 'Register', component: () => import('@/views/auth/RegisterView.vue') },

    // Standalone route: ExecutionDoView (fullscreen, no Layout)
    {
      path: '/execution/:id',
      name: 'ExecutionDo',
      component: () => import('@/views/execution/ExecutionDoView.vue'),
      meta: { requiresAuth: true }
    },

    // Layout-wrapped routes
    {
      path: '/',
      component: () => import('@/views/layout/Layout.vue'),
      meta: { requiresAuth: true },
      children: [
        { path: '', name: 'Workbench', component: () => import('@/views/workspace/WorkbenchView.vue') },
        { path: 'execution', name: 'Execution', component: () => import('@/views/execution/ExecutionView.vue') },
        { path: 'stats', name: 'Statistics', component: () => import('@/views/stats/StatisticsView.vue') },
        { path: 'notification', name: 'Notification', component: () => import('@/views/notification/NotificationView.vue') },
        { path: 'profile', name: 'Profile', component: () => import('@/views/profile/ProfileView.vue') },
        { path: 'leaderboard', name: 'Leaderboard', component: () => import('@/views/profile/LeaderboardView.vue') },
        { path: 'user/favorites', name: 'Favorites', component: () => import('@/views/user/Favorites.vue') },
        { path: 'sop/new', name: 'SopNew', component: () => import('@/views/sop/SopEditorView.vue') },
        { path: 'sop/:id/edit', name: 'SopEdit', component: () => import('@/views/sop/SopEditorView.vue') },
        { path: 'sop/:id/versions', name: 'SopVersions', component: () => import('@/views/sop/VersionHistoryView.vue') },
        { path: 'marketplace', name: 'Marketplace', component: () => import('@/views/marketplace/MarketplaceIndex.vue') },
        { path: 'marketplace/:template_id', name: 'TemplateDetail', component: () => import('@/views/marketplace/TemplateDetail.vue') },
        { path: 'admin/badges', name: 'AdminBadges', component: () => import('@/views/admin/AdminBadges.vue') },
        { path: 'admin/products', name: 'AdminProducts', component: () => import('@/views/admin/AdminProducts.vue') },
        { path: 'admin/rules', name: 'AdminRules', component: () => import('@/views/admin/AdminRules.vue') },
        { path: 'admin/marketplace', name: 'AdminMarketplace', component: () => import('@/views/admin/MarketplaceAudit.vue') },
        { path: 'admin/marketplace/categories', name: 'AdminMarketplaceCategories', component: () => import('@/views/admin/MarketplaceCategories.vue') },
      ]
    },
  ],
})

router.beforeEach((to) => {
  const token = localStorage.getItem('bf_token')
  if (to.meta.requiresAuth && !token) return '/login'
})

export default router
