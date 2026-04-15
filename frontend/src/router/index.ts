
/**
 * Vue Router 路由配置
 * - /login: 登录页（无需认证）
 * - /register: 注册页
 * - /: 首页/工作台（需要认证）
 * - /execution: 执行台列表
 * - /execution/:id/do: 逐步执行页
 * - /sop/new: 新建 SOP
 * - /sop/:id/edit: 编辑 SOP
 * - /sop/:id/versions: 版本历史
 * - /sop/:id/history/:versionId: 版本详情
 * - /stats: 统计页
 * - /notification: 通知页
 * - /profile: 个人中心
 * - /leaderboard: 排行榜
 * - /favorites: 我的收藏
 * - /marketplace: 模板市场
 * - /marketplace/:id: 模板详情
 * - /admin/*: 管理后台（需 admin 角色）
 * - 路由守卫：未登录跳转 /login
 */

// 引入 vue-router 的路由创建函数
import { createRouter, createWebHistory } from 'vue-router'
// 引入认证状态管理 store
import { useAuthStore } from '@/stores/auth'

// 创建路由实例，配置 history 模式和路由表
const router = createRouter({
  // 使用 HTML5 History 模式，URL 不带 # 号
  history: createWebHistory(),
  routes: [
    // 游客可访问的路由：登录和注册页面
    { path: '/login', name: 'Login', component: () => import('@/views/auth/LoginView.vue'), meta: { guest: true } },
    { path: '/register', name: 'Register', component: () => import('@/views/auth/RegisterView.vue'), meta: { guest: true } },

    // 独立路由：ExecutionDoView（全屏模式，不使用 Layout 布局）
    {
      path: '/execution/:id',
      name: 'ExecutionDo',
      component: () => import('@/views/execution/ExecutionDoView.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/instance/:id',
      name: 'InstanceDo',
      component: () => import('@/views/execution/InstanceDoView.vue'),
      meta: { requiresAuth: true }
    },

    // 使用 Layout 布局包裹的路由（需登录）
    {
      path: '/',
      component: () => import('@/views/layout/Layout.vue'),
      meta: { requiresAuth: true },
      children: [
        // 工作台首页
        { path: '', name: 'Workbench', component: () => import('@/views/workspace/WorkbenchView.vue') },
        // 执行记录页面
        { path: 'execution', name: 'Execution', component: () => import('@/views/execution/ExecutionView.vue') },
        // 统计页面
        { path: 'stats', name: 'Statistics', component: () => import('@/views/stats/StatisticsView.vue') },
        // 通知页面
        { path: 'notification', name: 'Notification', component: () => import('@/views/notification/NotificationView.vue') },
        // 个人资料页面
        { path: 'profile', name: 'Profile', component: () => import('@/views/profile/ProfileView.vue') },
        // 排行榜页面
        { path: 'leaderboard', name: 'Leaderboard', component: () => import('@/views/profile/LeaderboardView.vue') },
        // 用户收藏页面
        { path: 'user/favorites', name: 'Favorites', component: () => import('@/views/user/Favorites.vue') },
        // 新建 SOP 页面
        { path: 'sop/new', name: 'SopNew', component: () => import('@/views/sop/SopEditorView.vue') },
        // 编辑 SOP 页面
        { path: 'sop/:id/edit', name: 'SopEdit', component: () => import('@/views/sop/SopEditorView.vue') },
        // SOP 版本历史页面
        { path: 'sop/:id/versions', name: 'SopVersions', component: () => import('@/views/sop/VersionHistoryView.vue') },
        // 模板市场首页
        { path: 'marketplace', name: 'Marketplace', component: () => import('@/views/marketplace/MarketplaceIndex.vue') },
        // 模板详情页面
        { path: 'marketplace/:template_id', name: 'TemplateDetail', component: () => import('@/views/marketplace/TemplateDetail.vue') },
        // 管理后台：徽章管理
        { path: 'admin/badges', name: 'AdminBadges', component: () => import('@/views/admin/AdminBadges.vue') },
        // 管理后台：产品管理
        { path: 'admin/products', name: 'AdminProducts', component: () => import('@/views/admin/AdminProducts.vue') },
        // 管理后台：规则管理
        { path: 'admin/rules', name: 'AdminRules', component: () => import('@/views/admin/AdminRules.vue') },
        // 管理后台：市场审核
        { path: 'admin/marketplace', name: 'AdminMarketplace', component: () => import('@/views/admin/MarketplaceAudit.vue') },
        // 管理后台：市场分类管理
        { path: 'admin/marketplace/categories', name: 'AdminMarketplaceCategories', component: () => import('@/views/admin/MarketplaceCategories.vue') },
        // 管理后台：AI 模型配置
        { path: 'admin/ai-config', name: 'AdminAiConfig', component: () => import('@/views/admin/AdminAiConfig.vue') },
        // 管理后台：邮件配置
        { path: 'admin/email-config', name: 'AdminEmailConfig', component: () => import('@/views/admin/AdminEmailConfig.vue') },
      ]
    },
  ],
})

// 路由全局前置守卫：每次路由跳转前执行
router.beforeEach(async (to) => {
  const authStore = useAuthStore()
  const token = localStorage.getItem('bf_token')

  // 如果本地存在 token 但用户信息未加载，则尝试获取用户信息
  if (token && !authStore.userInfo) {
    await authStore.fetchMe()
  }

  // 已登录用户访问游客页面（如登录、注册）时，重定向到首页
  if (to.meta.guest && authStore.token && authStore.userInfo) {
    return '/'
  }

  // 需要登录的路由：检查 token 和用户信息是否完整
  if (to.meta.requiresAuth) {
    if (!token) return '/login'
    // 如果 fetchMe 失败（userInfo 仍为空），说明 token 可能已失效
    if (!authStore.userInfo) {
      authStore.logout()
      return '/login'
    }
  }
})

// 导出路由实例，供 main.ts 注册使用
export default router
