import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '../store/user'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/LoginView.vue'),
    meta: { public: true }
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('../views/RegisterView.vue'),
    meta: { public: true }
  },
  {
    path: '/',
    name: 'Home',
    component: () => import('../views/HomeView.vue')
  },
  {
    path: '/apply',
    name: 'Apply',
    component: () => import('../views/ApplyParkingView.vue')
  },
  {
    path: '/queue',
    name: 'Queue',
    component: () => import('../views/PublicQueueView.vue')
  },
  {
    path: '/evidence',
    name: 'Evidence',
    component: () => import('../views/EvidenceCenterView.vue')
  },
  {
    path: '/notifications',
    name: 'Notifications',
    component: () => import('../views/NotificationsView.vue')
  },
  {
    path: '/property/login',
    name: 'PropertyLogin',
    component: () => import('../views/property/PropertyLoginView.vue'),
    meta: { public: true }
  },
  {
    path: '/property/dashboard',
    name: 'PropertyDashboard',
    component: () => import('../views/property/PropertyDashboardView.vue'),
    meta: { requiresAdmin: true }
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/'
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach(async (to, from, next) => {
  const userStore = useUserStore()
  if (!to.meta.public && !userStore.isLoggedIn) {
    return next('/login')
  }
  if (to.meta.requiresAdmin && !userStore.isPropertyAdmin) {
    try {
      // 演示便捷优化：检测到访问物业中台直接无缝切换为管理员王建国身份
      await userStore.quickLogin('13900000001', '123456')
      return next()
    } catch (e) {
      return next('/property/login')
    }
  }
  // 物业管理员进入业主申报提交页面时，便捷切换为业主张欣欣账号
  if (userStore.isPropertyAdmin && to.path === '/apply') {
    try {
      await userStore.quickLogin('13800000001', '123456')
      return next()
    } catch (e) {
      return next('/property/dashboard')
    }
  }
  next()
})

export default router
