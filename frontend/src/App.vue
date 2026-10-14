<template>
  <div class="min-h-screen bg-slate-100 text-slate-800">
    <SeedAccountBar v-if="!isProduction" @fill="handleFill" />
    <router-view />
    <!-- 全局现代拟真模态弹窗与 Toast 容器 -->
    <GlobalDialog />
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { useUserStore } from './store/user'
import api from './api'
import SeedAccountBar from './components/SeedAccountBar.vue'
import GlobalDialog from './components/GlobalDialog.vue'
import { showToast } from './utils/dialog'

const router = useRouter()
const userStore = useUserStore()
const isProduction = import.meta.env.VITE_APP_MODE === 'production'

const handleFill = async ({ identifier, password }) => {
  try {
    const res = await api.post('/auth/login', {
      identifier,
      password,
      login_type: 'PASSWORD'
    })
    if (res.code !== 200) throw new Error(res.message || '登录失败')
    userStore.setAuth(res.data.user, res.data.token)
    if (res.data.user.role === 'PROPERTY_ADMIN') {
      router.push('/property/dashboard')
    } else {
      await userStore.fetchHomeData()
      if (router.currentRoute.value.path !== '/') {
        router.push('/')
      }
    }
  } catch (err) {
    showToast('切换失败: ' + err.message, 'error')
  }
}
</script>
