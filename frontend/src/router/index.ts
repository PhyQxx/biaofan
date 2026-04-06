import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/login', name: 'Login', component: () => import('@/views/auth/LoginView.vue') },
    { path: '/register', name: 'Register', component: () => import('@/views/auth/RegisterView.vue') },
    { path: '/', name: 'Workbench', component: () => import('@/views/workspace/WorkbenchView.vue'), meta: { requiresAuth: true } },
    { path: '/execution', name: 'Execution', component: () => import('@/views/execution/ExecutionView.vue'), meta: { requiresAuth: true } },
    { path: '/execution/:id', name: 'ExecutionDo', component: () => import('@/views/execution/ExecutionDoView.vue'), meta: { requiresAuth: true } },
    { path: '/stats', name: 'Statistics', component: () => import('@/views/stats/StatisticsView.vue'), meta: { requiresAuth: true } },
    { path: '/sop/new', name: 'SopNew', component: () => import('@/views/sop/SopEditorView.vue'), meta: { requiresAuth: true } },
    { path: '/sop/:id/edit', name: 'SopEdit', component: () => import('@/views/sop/SopEditorView.vue'), meta: { requiresAuth: true } },
    { path: '/sop/:id/versions', name: 'SopVersions', component: () => import('@/views/sop/VersionHistoryView.vue'), meta: { requiresAuth: true } },
    { path: '/notification', name: 'Notification', component: () => import('@/views/notification/NotificationView.vue'), meta: { requiresAuth: true } },
    // Admin routes
    { path: '/admin/badges', name: 'AdminBadges', component: () => import('@/views/admin/AdminBadges.vue'), meta: { requiresAuth: true } },
    { path: '/admin/products', name: 'AdminProducts', component: () => import('@/views/admin/AdminProducts.vue'), meta: { requiresAuth: true } },
    { path: '/admin/rules', name: 'AdminRules', component: () => import('@/views/admin/AdminRules.vue'), meta: { requiresAuth: true } },
    // Gamification routes
    { path: '/profile', name: 'Profile', component: () => import('@/views/profile/ProfileView.vue'), meta: { requiresAuth: true } },
    { path: '/leaderboard', name: 'Leaderboard', component: () => import('@/views/profile/LeaderboardView.vue'), meta: { requiresAuth: true } },
  ],
})

router.beforeEach((to) => {
  const token = localStorage.getItem('bf_token')
  if (to.meta.requiresAuth && !token) return '/login'
})

export default router
