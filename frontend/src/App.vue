<template>
  <div class="min-h-screen bg-slate-100 text-slate-800">
    <SeedAccountBar @fill="handleFill" />
    <router-view />
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { useUserStore } from './store/user'
import api from './api'
import SeedAccountBar from './components/SeedAccountBar.vue'

const router = useRouter()
const userStore = useUserStore()

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
    alert('切换失败: ' + err.message)
  }
}
</script>
