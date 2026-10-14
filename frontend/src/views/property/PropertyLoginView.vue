<template>
  <div class="min-h-screen bg-slate-900 text-slate-100 flex flex-col justify-center py-12 sm:px-6 lg:px-8">
    <div class="sm:mx-auto sm:w-full sm:max-w-md text-center">
      <div class="inline-flex items-center justify-center w-14 h-14 rounded-2xl bg-purple-600 text-white shadow-lg shadow-purple-500/30 mb-3">
        <svg xmlns="http://www.w3.org/2000/svg" class="w-8 h-8" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <path d="M3 21h18"/><path d="M5 21V5a2 2 0 0 1 2-2h10a2 2 0 0 1 2 2v16"/><path d="M9 9h1"/><path d="M9 13h1"/><path d="M9 17h1"/><path d="M14 9h1"/><path d="M14 13h1"/><path d="M14 17h1"/>
        </svg>
      </div>
      <h2 class="text-xl font-black tracking-tight text-white">慧停管家 · 物业登录</h2>
      <p class="mt-1 text-xs text-slate-400">车位管理 · 申请审核 · 租约管理</p>
    </div>

    <div class="mt-6 sm:mx-auto sm:w-full sm:max-w-md px-4">
      <div class="bg-slate-800 py-7 px-6 shadow-2xl rounded-3xl sm:px-10 border border-slate-700 space-y-5">
        <!-- 一键快捷填入条 -->
        <div v-if="!isProduction" class="p-3 bg-purple-950/60 border border-purple-800/60 rounded-2xl flex items-center justify-between text-xs">
          <div class="flex items-center gap-2 text-purple-200">
            <span>👤 管理员: 王建国</span>
            <span class="text-[10px] text-purple-400 font-mono">13900000001</span>
          </div>
          <button
            type="button"
            @click="handleQuickLogin"
            :disabled="loading"
            class="px-2.5 py-1 rounded-lg bg-purple-600 hover:bg-purple-500 text-white font-bold text-xs shadow transition active:scale-95 disabled:opacity-50"
          >
            ⚡ 一键免密进入
          </button>
        </div>

        <!-- 账号密码表单 -->
        <form @submit.prevent="handleLogin" class="space-y-4 text-xs">
          <div>
            <label class="block font-bold text-slate-300 mb-1.5">管理员手机号 / 账号</label>
            <input
              v-model="identifier"
              type="text"
              required
              placeholder="请输入管理员手机号"
              class="w-full px-3.5 py-2.5 rounded-xl bg-slate-900 border border-slate-600 text-white focus:outline-none focus:ring-2 focus:ring-purple-500 font-medium text-xs"
            />
          </div>

          <div>
            <label class="block font-bold text-slate-300 mb-1.5">登录密码</label>
            <input
              v-model="password"
              type="password"
              required
              placeholder="请输入登录密码"
              class="w-full px-3.5 py-2.5 rounded-xl bg-slate-900 border border-slate-600 text-white focus:outline-none focus:ring-2 focus:ring-purple-500 font-medium text-xs"
            />
          </div>

          <div v-if="errorMsg" class="p-2.5 bg-rose-900/50 border border-rose-600 rounded-xl text-rose-300 text-xs font-medium">
            ⚠️ {{ errorMsg }}
          </div>

          <button
            type="submit"
            :disabled="loading"
            class="w-full py-3 rounded-xl bg-purple-600 hover:bg-purple-700 text-white font-bold text-sm shadow-lg shadow-purple-500/30 transition active:scale-[0.98] disabled:opacity-50"
          >
            <span v-if="loading">正在验证登录...</span>
            <span v-else>立即登录 ➔</span>
          </button>
        </form>

        <!-- 备查: 企业资质认证信息(默认折叠，不阻断直接登录) -->
        <div v-if="!isProduction" class="pt-2 border-t border-slate-700">
          <button
            type="button"
            @click="showLicenseDetails = !showLicenseDetails"
            class="text-[11px] text-slate-400 hover:text-slate-200 flex items-center justify-between w-full"
          >
            <span>📜 企业资质与法人备案信息 (已通过工商核验)</span>
            <span>{{ showLicenseDetails ? '▲ 收起' : '▼ 展开' }}</span>
          </button>

          <div v-if="showLicenseDetails" class="mt-2.5 p-3 bg-slate-900/80 rounded-xl border border-slate-700 text-[11px] space-y-1 text-slate-300 animate-fade-in">
            <div class="flex justify-between"><span class="text-slate-500">企业名称:</span><span class="font-bold text-white">{{ company.name }}</span></div>
            <div class="flex justify-between"><span class="text-slate-500">统一代码:</span><span class="font-mono">{{ company.creditCode }}</span></div>
            <div class="flex justify-between"><span class="text-slate-500">法定代表:</span><span class="text-purple-300 font-bold">{{ company.legalName }} (王建国)</span></div>
            <div class="flex justify-between"><span class="text-slate-500">核验状态:</span><span class="text-emerald-400 font-bold">✓ 市住建委与工商信息库比对属实</span></div>
          </div>
        </div>

        <div class="pt-3 border-t border-slate-700/60 flex items-center justify-between text-xs">
          <router-link to="/property/register" class="text-purple-400 hover:text-purple-300 font-semibold underline flex items-center gap-1">
            <span>✨ 注册新物业账号 / 入驻新小区</span>
          </router-link>
          <router-link to="/login" class="text-slate-400 hover:text-white underline">
            切换为业主端 H5 登录
          </router-link>
        </div>
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
const isProduction = import.meta.env.VITE_APP_MODE === 'production'

const company = {
  name: '上海安居物业管理服务有限公司',
  creditCode: '91310104MA1FR8992X',
  legalName: '王建国'
}

const identifier = ref(isProduction ? '' : '13900000001')
const password = ref(isProduction ? '' : '123456')
const loading = ref(false)
const errorMsg = ref('')
const showLicenseDetails = ref(false)

const handleQuickLogin = async () => {
  identifier.value = '13900000001'
  password.value = '123456'
  await handleLogin()
}

const handleLogin = async () => {
  loading.value = true
  errorMsg.value = ''
  try {
    const res = await api.post('/auth/login', {
      identifier: identifier.value,
      password: password.value,
      login_type: 'PASSWORD'
    })
    if (res.code === 200) {
      if (res.data.user.role !== 'PROPERTY_ADMIN') {
        throw new Error('该账号非物业管理员账号，请核对手机号')
      }
      userStore.setAuth(res.data.user, res.data.token)
      router.push('/property/dashboard')
    }
  } catch (err) {
    errorMsg.value = err.message || '登录失败'
  } finally {
    loading.value = false
  }
}
</script>
