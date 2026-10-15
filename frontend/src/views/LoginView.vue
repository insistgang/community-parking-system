<template>
  <div class="min-h-screen bg-slate-100 flex items-center justify-center p-4">
    <div class="max-w-md w-full bg-white rounded-3xl p-6 sm:p-8 shadow-xl border border-slate-200">
      <div class="text-center mb-7">
        <div class="inline-flex items-center justify-center w-14 h-14 rounded-2xl bg-blue-600 text-white mb-3 shadow-lg shadow-blue-500/30">
          <AppIcon name="parking" class="w-8 h-8" />
        </div>
        <h1 class="text-xl font-black text-slate-900 tracking-tight">慧停管家 · 老旧小区停车位管理系统</h1>
        <p class="text-xs text-slate-500 mt-1">正式版 · 公平排位 · 物业人工审核</p>
      </div>

      <form @submit.prevent="login" class="space-y-4 text-sm">
        <div>
          <label for="identifier" class="block font-bold text-slate-700 mb-1">手机号或身份证号</label>
          <input id="identifier" v-model.trim="identifier" required autocomplete="username"
            placeholder="请输入注册时使用的手机号或身份证号"
            class="w-full px-3.5 py-3 rounded-xl border border-slate-200 focus:outline-none focus:ring-2 focus:ring-blue-500" />
        </div>
        <div>
          <label for="password" class="block font-bold text-slate-700 mb-1">登录密码</label>
          <input id="password" v-model="password" type="password" required autocomplete="current-password"
            placeholder="请输入登录密码"
            class="w-full px-3.5 py-3 rounded-xl border border-slate-200 focus:outline-none focus:ring-2 focus:ring-blue-500" />
        </div>
        <p v-if="error" role="alert" class="p-3 bg-rose-50 border border-rose-200 rounded-xl text-rose-700 text-xs">{{ error }}</p>
        <button type="submit" :disabled="loading"
          class="w-full py-3 rounded-xl bg-blue-600 hover:bg-blue-700 text-white font-bold shadow-lg shadow-blue-500/25 disabled:opacity-50">
          {{ loading ? '正在验证…' : '立即登录' }}
        </button>
      </form>

      <p class="text-[11px] text-slate-500 mt-4 leading-relaxed">短信找回密码尚未开通。如忘记密码，请联系物业或平台管理员核实身份后处理。</p>
      <div class="mt-6 pt-4 border-t border-slate-100 flex items-center justify-between text-xs">
        <router-link to="/register" class="text-blue-600 hover:underline font-semibold">新业主注册 <AppIcon name="arrow" size="1em" /></router-link>
        <div class="flex gap-3">
          <router-link to="/property/login" class="text-purple-600 hover:underline font-semibold">物业登录</router-link>
          <router-link to="/admin/login" class="text-slate-600 hover:underline font-semibold">平台登录</router-link>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import AppIcon from '../components/AppIcon.vue'
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../store/user'
import api from '../api'

const router = useRouter()
const userStore = useUserStore()
const identifier = ref('')
const password = ref('')
const loading = ref(false)
const error = ref('')

const login = async () => {
  error.value = ''
  loading.value = true
  try {
    const res = await api.post('/auth/login', {
      identifier: identifier.value,
      password: password.value,
      login_type: 'PASSWORD'
    })
    if (res.code !== 200) throw new Error(res.message || '登录失败')
    userStore.setAuth(res.data.user, res.data.token)
    await userStore.fetchHomeData()
    router.push(res.data.user.role === 'PROPERTY_ADMIN' ? '/property/dashboard' : '/')
  } catch (err) {
    error.value = err.message || '登录失败'
  } finally {
    loading.value = false
  }
}
</script>
