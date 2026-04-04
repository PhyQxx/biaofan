import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/login',
      name: 'Login',
      component: () => import('@/views/auth/LoginView.vue'),
    },
    {
      path: '/register',
      name: 'Register',
      component: () => import('@/views/auth/RegisterView.vue'),
    },
    {
      path: '/',
      name: 'Workbench',
      component: () => import('@/views/workspace/WorkbenchView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/sop/new',
      name: 'SopNew',
      component: () => import('@/views/sop/SopEditorView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/sop/:id/edit',
      name: 'SopEdit',
      component: () => import('@/views/sop/SopEditorView.vue'),
      meta: { requiresAuth: true },
    },
  ],
})

router.beforeEach((to) => {
  const token = localStorage.getItem('bf_token')
  if (to.meta.requiresAuth && !token) {
    return '/login'
  }
})

export default router
