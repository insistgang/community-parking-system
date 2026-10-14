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
    component: import.meta.env.VITE_APP_MODE === 'production'
      ? () => import('../views/ProductionApplyView.vue')
      : () => import('../views/ApplyParkingView.vue')
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
    path: '/property/register',
    name: 'PropertyRegister',
    component: () => import('../views/property/PropertyRegisterView.vue'),
    meta: { public: true }
  },
  {
    path: '/property/dashboard',
    name: 'PropertyDashboard',
    component: () => import('../views/property/PropertyDashboardView.vue'),
    meta: { requiresAdmin: true }
  },
  {
    path: '/admin/login',
    name: 'AdminLogin',
    component: () => import('../views/admin/AdminLoginView.vue'),
    meta: { public: true }
  },
  {
    path: '/admin/dashboard',
    name: 'AdminDashboard',
    component: () => import('../views/admin/AdminDashboardView.vue'),
    meta: { requiresSuperAdmin: true }
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

router.beforeEach((to, from, next) => {
  const userStore = useUserStore()
  if (!to.meta.public && !userStore.isLoggedIn) {
    if (to.path.startsWith('/admin')) {
      return next('/admin/login')
    }
    return next('/login')
  }
  if (to.meta.requiresSuperAdmin && !userStore.isSuperAdmin) {
    return next('/admin/login')
  }
  if (to.meta.requiresAdmin && !userStore.isPropertyAdmin) {
    return next('/')
  }
  // 物业管理员禁止进入业主申报提交页面，避免误操作；但允许浏览前台主页、公示大盘与存证
  if (userStore.isPropertyAdmin && to.path === '/apply') {
    return next('/property/dashboard')
  }
  next()
})

export default router
