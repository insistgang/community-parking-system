<template>
  <div class="min-h-screen bg-slate-950 flex flex-col justify-center items-center p-4 text-slate-100">
    <div class="max-w-md w-full bg-slate-900/90 rounded-3xl p-6 sm:p-8 shadow-2xl border border-slate-800 backdrop-blur">
      <div class="text-center mb-6">
        <div class="inline-flex items-center justify-center w-14 h-14 rounded-2xl bg-indigo-600 text-white mb-3 shadow-lg shadow-indigo-600/30">
          <svg xmlns="http://www.w3.org/2000/svg" class="w-8 h-8" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10"/>
            <path d="m9 12 2 2 4-4"/>
          </svg>
        </div>
        <h2 class="text-xl font-black text-white tracking-wide">慧停管家 · 平台运维管理中台</h2>
        <p class="text-xs text-slate-400 mt-1">系统全景监控 · 用户全量档案 · 操作安全审计 · 外部接口追踪</p>
      </div>

      <div class="p-3 bg-indigo-950/60 border border-indigo-500/30 rounded-xl text-xs text-indigo-300 mb-6 flex items-center justify-between">
        <span class="flex items-center gap-1.5">
          <span>🛡️</span>
          <span>系统最高管理特权通道</span>
        </span>
        <button
          type="button"
          @click="fillDemoAdmin"
          class="px-2 py-0.5 rounded bg-indigo-600/50 hover:bg-indigo-600 text-white text-[10px] font-bold transition"
        >
          一键填入演示账号
        </button>
      </div>

      <form @submit.prevent="handleLogin" class="space-y-4 text-xs">
        <div>
          <label class="block font-bold text-slate-300 mb-1">系统管理员账号</label>
          <input
            v-model="form.username"
            type="text"
            required
            placeholder="请输入管理员账号 (默认: admin)"
            class="w-full px-3.5 py-2.5 rounded-xl bg-slate-800 border border-slate-700 focus:outline-none focus:ring-2 focus:ring-indigo-500 text-white font-medium"
          />
        </div>

        <div>
          <label class="block font-bold text-slate-300 mb-1">管理员密码</label>
          <input
            v-model="form.password"
            type="password"
            required
            placeholder="请输入登录密码 (默认: admin123)"
            class="w-full px-3.5 py-2.5 rounded-xl bg-slate-800 border border-slate-700 focus:outline-none focus:ring-2 focus:ring-indigo-500 text-white font-medium"
          />
        </div>

        <div v-if="errorMsg" class="p-2.5 bg-rose-950/80 border border-rose-500/40 rounded-xl text-rose-300 text-xs font-medium">
          ⚠️ {{ errorMsg }}
        </div>

        <button
          type="submit"
          :disabled="loading"
          class="w-full py-3 rounded-xl bg-indigo-600 hover:bg-indigo-500 text-white font-bold shadow-lg shadow-indigo-600/30 active:scale-[0.98] transition flex items-center justify-center gap-2"
        >
          <span v-if="loading">正在验证平台权限...</span>
          <span v-else>立即登录运维中台</span>
        </button>
      </form>

      <div class="mt-6 pt-4 border-t border-slate-800 flex items-center justify-between text-xs text-slate-500">
        <router-link to="/login" class="text-blue-400 hover:underline">
          ➔ 返回业主端
        </router-link>
        <router-link to="/property/login" class="text-purple-400 hover:underline">
          ➔ 前往物业工作台
        </router-link>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../../store/user'
import api from '../../api'

const router = useRouter()
const userStore = useUserStore()

const loading = ref(false)
const errorMsg = ref('')

const form = ref({
  username: 'admin',
  password: 'admin123'
})

const fillDemoAdmin = () => {
  form.value.username = 'admin'
  form.value.password = 'admin123'
}

const handleLogin = async () => {
  loading.value = true
  errorMsg.value = ''
  try {
    const res = await api.post('/admin/login', {
      username: form.value.username,
      password: form.value.password
    })
    if (res.code === 200) {
      userStore.setAuth(res.data.user, res.data.token)
      router.push('/admin/dashboard')
    }
  } catch (err) {
    errorMsg.value = err.message || '管理员登录认证失败'
  } finally {
    loading.value = false
  }
}
</script>
