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

router.beforeEach((to, from, next) => {
  const userStore = useUserStore()
  if (!to.meta.public && !userStore.isLoggedIn) {
    return next('/login')
  }
  if (to.meta.requiresAdmin && !userStore.isPropertyAdmin) {
    return next('/')
  }
  next()
})

export default router
