<template>
  <div class="min-h-screen bg-slate-900 text-slate-100 flex flex-col justify-center py-12 sm:px-6 lg:px-8">
    <div class="sm:mx-auto sm:w-full sm:max-w-md text-center">
      <div class="inline-flex items-center justify-center w-14 h-14 rounded-2xl bg-purple-600 text-white shadow-lg shadow-purple-500/30 mb-3">
        <svg xmlns="http://www.w3.org/2000/svg" class="w-8 h-8" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <path d="M3 21h18"/><path d="M5 21V5a2 2 0 0 1 2-2h10a2 2 0 0 1 2 2v16"/><path d="M9 9h1"/><path d="M9 13h1"/><path d="M9 17h1"/><path d="M14 9h1"/><path d="M14 13h1"/><path d="M14 17h1"/>
        </svg>
      </div>
      <h2 class="text-xl font-black tracking-tight text-white">物业服务企业工作台认证登录</h2>
      <p class="mt-1 text-xs text-slate-400">老旧小区车位资产台账与线下履约交付中台</p>
    </div>

    <div class="mt-6 sm:mx-auto sm:w-full sm:max-w-md px-4">
      <div class="bg-slate-800 py-8 px-6 shadow-2xl rounded-3xl sm:px-10 border border-slate-700 space-y-5">
        <!-- 营业执照扫描认证模块 (根据 PRD 4.1 规范) -->
        <div class="space-y-2">
          <label class="block text-xs font-bold text-slate-300">企业资质认证：营业执照 OCR 自动核验</label>
          <OcrUploadBox
            label="上传物业公司营业执照"
            tip="系统自动核验法人姓名与统一社会信用代码"
            @scanned="handleLicenseScanned"
          />

          <div v-if="licenseScanned" class="bg-slate-900/80 p-3 rounded-xl border border-slate-600 text-xs space-y-1.5 animate-fade-in">
            <div class="flex justify-between">
              <span class="text-slate-400">认证企业:</span>
              <span class="font-bold text-slate-200">上海安居物业管理服务有限公司</span>
            </div>
            <div class="flex justify-between">
              <span class="text-slate-400">统一信用代码:</span>
              <span class="font-mono text-slate-300">91310104MA1FR8992X</span>
            </div>
            <div class="flex justify-between">
              <span class="text-slate-400">法定代表人:</span>
              <span class="font-bold text-purple-400">王建国 (已实名核验)</span>
            </div>
          </div>
        </div>

        <form @submit.prevent="handleLogin" class="space-y-4 text-xs">
          <div>
            <label class="block font-bold text-slate-300 mb-1">法人手机号 / 管理员账号</label>
            <input
              v-model="identifier"
              type="text"
              required
              class="w-full px-3.5 py-2.5 rounded-xl bg-slate-900 border border-slate-600 text-white focus:outline-none focus:ring-2 focus:ring-purple-500 font-medium"
            />
          </div>

          <div>
            <label class="block font-bold text-slate-300 mb-1">登录密码</label>
            <input
              v-model="password"
              type="password"
              required
              class="w-full px-3.5 py-2.5 rounded-xl bg-slate-900 border border-slate-600 text-white focus:outline-none focus:ring-2 focus:ring-purple-500 font-medium"
            />
          </div>

          <div v-if="errorMsg" class="p-2.5 bg-rose-900/50 border border-rose-600 rounded-xl text-rose-300 text-xs font-medium">
            ⚠️ {{ errorMsg }}
          </div>

          <button
            type="submit"
            :disabled="loading"
            class="w-full py-3 rounded-xl bg-purple-600 hover:bg-purple-700 text-white font-bold shadow-lg shadow-purple-500/30 transition active:scale-[0.98] flex items-center justify-center gap-2"
          >
            <span v-if="loading">正在验证权限...</span>
            <span v-else>进入物业管理中台 ➔</span>
          </button>
        </form>

        <div class="pt-2 flex items-center justify-between text-xs text-slate-400">
          <router-link to="/login" class="text-slate-400 hover:text-white underline">
            切换为业主端 H5 登录
          </router-link>
          <button
            @click="fillAdmin"
            class="text-purple-400 hover:text-purple-300 font-semibold underline"
          >
            一键填入王建国账号
          </button>
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
import OcrUploadBox from '../../components/OcrUploadBox.vue'

const router = useRouter()
const userStore = useUserStore()

const identifier = ref('13900000001')
const password = ref('123456')
const licenseScanned = ref(false)
const loading = ref(false)
const errorMsg = ref('')

const handleLicenseScanned = () => {
  licenseScanned.value = true
}

const fillAdmin = () => {
  identifier.value = '13900000001'
  password.value = '123456'
  licenseScanned.value = true
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
        throw new Error('该账号非物业管理员账号，无法登录中台')
      }
      userStore.setAuth(res.data.user, res.data.token)
      router.push('/property/dashboard')
    }
  } catch (err) {
    errorMsg.value = err.message
  } finally {
    loading.value = false
  }
}
</script>
