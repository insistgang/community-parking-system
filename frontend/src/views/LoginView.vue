<template>
  <div class="min-h-screen bg-slate-100 flex flex-col">
    <div class="flex-1 flex items-center justify-center p-4">
      <div class="max-w-md w-full bg-white rounded-3xl p-6 sm:p-8 shadow-xl border border-slate-200">
        <!-- 头部 Logo 与标题 -->
        <div class="text-center mb-6">
          <div class="inline-flex items-center justify-center w-14 h-14 rounded-2xl bg-blue-600 text-white mb-3 shadow-lg shadow-blue-500/30">
            <svg xmlns="http://www.w3.org/2000/svg" class="w-8 h-8" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <rect width="18" height="18" x="3" y="3" rx="2"/><path d="M9 17V7h4a3 3 0 0 1 0 6H9"/>
            </svg>
          </div>
          <h2 class="text-xl font-black text-slate-900 tracking-tight">慧停管家 · 老旧小区停车位管理系统</h2>
          <p class="text-xs text-slate-500 mt-1">公平排位 · 实名核验 · 阳光公示 · 存证溯源</p>
        </div>

        <!-- 登录方式 Tab -->
        <div class="flex border-b border-slate-200 mb-6">
          <button
            @click="switchLoginType('PASSWORD')"
            class="flex-1 pb-3 text-xs font-bold transition border-b-2"
            :class="loginType === 'PASSWORD' ? 'border-blue-600 text-blue-600' : 'border-transparent text-slate-400 hover:text-slate-600'"
          >
            身份证号密码登录
          </button>
          <button
            @click="switchLoginType('SMS')"
            class="flex-1 pb-3 text-xs font-bold transition border-b-2"
            :class="loginType === 'SMS' ? 'border-blue-600 text-blue-600' : 'border-transparent text-slate-400 hover:text-slate-600'"
          >
            手机验证码快捷登录
          </button>
        </div>

        <!-- 登录表单 -->
        <form @submit.prevent="handleLogin" class="space-y-4 text-xs">
          <div>
            <label class="block font-bold text-slate-700 mb-1">{{ loginType === 'PASSWORD' ? '身份证号' : '手机号' }}</label>
            <input
              v-model="form.identifier"
              type="text"
              required
              :maxlength="loginType === 'PASSWORD' ? 18 : 11"
              :placeholder="loginType === 'PASSWORD' ? '请输入18位居民身份证号' : '请输入11位手机号码'"
              class="w-full px-3.5 py-2.5 rounded-xl border border-slate-200 focus:outline-none focus:ring-2 focus:ring-blue-500 font-medium"
            />
          </div>

          <div v-if="loginType === 'PASSWORD'">
            <label class="block font-bold text-slate-700 mb-1">登录密码</label>
            <input
              v-model="form.password"
              type="password"
              required
              placeholder="请输入密码 (演示默认 123456)"
              class="w-full px-3.5 py-2.5 rounded-xl border border-slate-200 focus:outline-none focus:ring-2 focus:ring-blue-500 font-medium"
            />
          </div>

          <div v-else class="space-y-2">
            <div>
              <label class="block font-bold text-slate-700 mb-1">手机短信验证码</label>
              <div class="flex gap-2">
                <input
                  v-model="form.verify_code"
                  type="text"
                  required
                  placeholder="6位数字验证码"
                  maxlength="6"
                  class="flex-1 px-3.5 py-2.5 rounded-xl border border-slate-200 focus:outline-none focus:ring-2 focus:ring-blue-500 font-mono font-bold text-sm tracking-wider"
                />
                <button
                  type="button"
                  @click="handleSendSms"
                  :disabled="sendingSms || smsCountdown > 0"
                  class="px-3.5 py-2.5 bg-blue-50 hover:bg-blue-100 text-blue-700 border border-blue-200 rounded-xl font-bold text-xs whitespace-nowrap transition disabled:opacity-50 disabled:pointer-events-none"
                >
                  <span v-if="sendingSms">发送中...</span>
                  <span v-else-if="smsCountdown > 0">{{ smsCountdown }}s 后重新获取</span>
                  <span v-else>获取验证码</span>
                </button>
              </div>
            </div>

            <!-- 拟真短信通知弹卡 (发送后高亮展示) -->
            <div v-if="smsToast" class="p-3 bg-amber-50 border border-amber-200 rounded-xl text-xs space-y-1.5 animate-fade-in shadow-sm">
              <div class="flex items-center justify-between text-amber-900 font-bold">
                <span class="flex items-center gap-1">
                  <span>📱</span>
                  <span>本地演示验证码（未发送真实短信）</span>
                </span>
                <span class="text-[10px] text-amber-600 font-normal">刚刚</span>
              </div>
              <p class="text-[11px] text-amber-800 leading-relaxed font-mono">
                您的登录动态验证码为：<span class="font-extrabold text-blue-700 bg-white px-1.5 py-0.5 rounded border border-amber-300">{{ smsCode }}</span>，5分钟内有效。
              </p>
              <div class="pt-1 flex justify-end">
                <button
                  type="button"
                  @click="fillSmsCode"
                  class="px-2 py-0.5 rounded bg-amber-200 hover:bg-amber-300 text-amber-900 font-bold text-[10px] transition"
                >
                  ⚡ 一键填入验证码
                </button>
              </div>
            </div>
          </div>

          <div v-if="errorMsg" class="p-2.5 bg-rose-50 border border-rose-200 rounded-xl text-rose-600 text-xs font-medium">
            ⚠️ {{ errorMsg }}
          </div>

          <button
            type="submit"
            :disabled="loading"
            class="w-full py-3 rounded-xl bg-blue-600 hover:bg-blue-700 text-white font-bold shadow-lg shadow-blue-500/25 active:scale-[0.98] transition flex items-center justify-center gap-2"
          >
            <span v-if="loading">正在验证登录...</span>
            <span v-else>立即登录</span>
          </button>
        </form>

        <!-- 注册与物业登录通道 -->
        <div class="mt-6 pt-4 border-t border-slate-100 flex items-center justify-between text-xs text-slate-500">
          <router-link to="/register" class="text-blue-600 hover:underline font-semibold">
            新业主注册 ➔
          </router-link>
          <router-link to="/property/login" class="text-purple-600 hover:underline font-semibold flex items-center gap-1">
            <span>物业企业认证通道</span>
          </router-link>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../store/user'
import api from '../api'

const router = useRouter()
const userStore = useUserStore()

const DEMO_ID_CARD = '310104199201011111'
const DEMO_PHONE = '13800000001'

const loginType = ref('PASSWORD')
const loading = ref(false)
const errorMsg = ref('')

const form = ref({
  identifier: DEMO_ID_CARD,
  password: '123456',
  verify_code: ''
})

const switchLoginType = (t) => {
  loginType.value = t
  if (t === 'SMS') {
    form.value.identifier = DEMO_PHONE
  } else {
    form.value.identifier = DEMO_ID_CARD
  }
  errorMsg.value = ''
}

const sendingSms = ref(false)
const smsCountdown = ref(0)
const smsToast = ref(false)
const smsCode = ref('')
const fillSmsCode = () => {
  form.value.verify_code = smsCode.value
}
let countdownTimer = null

const handleSendSms = async () => {
  if (!form.value.identifier) {
    errorMsg.value = '请先输入接收短信验证码的手机号！'
    return
  }

  sendingSms.value = true
  errorMsg.value = ''
  smsToast.value = false
  smsCode.value = ''
  form.value.verify_code = ''
  try {
    const res = await api.post('/auth/send-sms', { phone: form.value.identifier })
    if (typeof res.data?.demo_code !== 'string' || !/^\d{6}$/.test(res.data.demo_code)) {
      throw new Error('未收到有效的演示验证码，请重新获取')
    }
    smsCode.value = res.data.demo_code
    smsToast.value = true
    fillSmsCode()
    
    // 启动 60 秒倒计时
    smsCountdown.value = 60
    if (countdownTimer) clearInterval(countdownTimer)
    countdownTimer = setInterval(() => {
      if (smsCountdown.value > 0) {
        smsCountdown.value--
      } else {
        clearInterval(countdownTimer)
      }
    }, 1000)
  } catch (err) {
    errorMsg.value = err.message || '短信发送失败，请稍后重试'
  } finally {
    sendingSms.value = false
  }
}

onUnmounted(() => {
  if (countdownTimer) clearInterval(countdownTimer)
})

const handleLogin = async () => {
  loading.value = true
  errorMsg.value = ''
  try {
    const res = await api.post('/auth/login', {
      identifier: form.value.identifier,
      password: form.value.password,
      verify_code: form.value.verify_code,
      login_type: loginType.value
    })
    if (res.code === 200) {
      userStore.setAuth(res.data.user, res.data.token)
      if (res.data.user.role === 'PROPERTY_ADMIN') {
        router.push('/property/dashboard')
      } else {
        router.push('/')
      }
    }
  } catch (err) {
    errorMsg.value = err.message
  } finally {
    loading.value = false
  }
}
</script>
