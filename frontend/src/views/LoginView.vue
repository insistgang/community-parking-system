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

          <div v-if="loginType === 'PASSWORD'" class="space-y-1">
            <div class="flex items-center justify-between">
              <label class="block font-bold text-slate-700 mb-1">登录密码</label>
              <button
                type="button"
                @click="openForgotModal"
                class="text-[11px] text-blue-600 hover:text-blue-800 font-bold hover:underline"
              >
                忘记密码？
              </button>
            </div>
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
                  <span>收到来自【上海政务云/停车便民服务】短信</span>
                </span>
                <span class="text-[10px] text-amber-600 font-normal">刚刚</span>
              </div>
              <p class="text-[11px] text-amber-800 leading-relaxed font-mono">
                您的登录动态验证码为：<span class="font-extrabold text-blue-700 bg-white px-1.5 py-0.5 rounded border border-amber-300">888888</span>，5分钟内有效。
              </p>
              <div class="pt-1 flex justify-end">
                <button
                  type="button"
                  @click="form.verify_code = '888888'"
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

        <!-- 注册与其它通道入口 -->
        <div class="mt-6 pt-4 border-t border-slate-100 flex items-center justify-between text-xs text-slate-500">
          <router-link to="/register" class="text-blue-600 hover:underline font-semibold">
            新业主注册 ➔
          </router-link>
          <div class="flex items-center gap-3">
            <router-link to="/property/login" class="text-purple-600 hover:underline font-semibold">
              物业登录
            </router-link>
            <span class="text-slate-300">|</span>
            <router-link to="/admin/login" class="text-slate-600 hover:text-slate-900 hover:underline font-semibold">
              管理员登录 ➔
            </router-link>
          </div>
        </div>
      </div>
    </div>

    <!-- 忘记密码 / 手机验证码重设密码 弹窗 -->
    <div v-if="showForgotModal" class="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/60 backdrop-blur-sm animate-fade-in">
      <div class="bg-white rounded-3xl max-w-sm w-full p-6 text-slate-800 shadow-2xl border border-slate-100 space-y-4">
        <div class="flex items-center justify-between border-b border-slate-100 pb-3">
          <div class="flex items-center gap-2">
            <span class="w-8 h-8 rounded-xl bg-blue-100 text-blue-600 flex items-center justify-center font-bold text-sm">
              🔑
            </span>
            <div>
              <h3 class="text-sm font-bold text-slate-900">重设业主登录密码</h3>
              <p class="text-[11px] text-slate-400">通过注册绑定的手机号接收验证码找回</p>
            </div>
          </div>
          <button @click="showForgotModal = false" class="text-slate-400 hover:text-slate-600 p-1 text-base">✕</button>
        </div>

        <form @submit.prevent="handleResetPassword" class="space-y-3 text-xs">
          <div>
            <label class="block font-bold text-slate-700 mb-1">手机号码</label>
            <input
              v-model="forgotForm.phone"
              type="text"
              required
              maxlength="11"
              placeholder="请输入您注册的11位手机号"
              class="w-full px-3 py-2 rounded-xl border border-slate-200 focus:outline-none focus:ring-2 focus:ring-blue-500 font-mono font-medium"
            />
          </div>

          <div>
            <label class="block font-bold text-slate-700 mb-1">短信验证码</label>
            <div class="flex gap-2">
              <input
                v-model="forgotForm.verify_code"
                type="text"
                required
                maxlength="6"
                placeholder="6位数字验证码"
                class="flex-1 px-3 py-2 rounded-xl border border-slate-200 focus:outline-none focus:ring-2 focus:ring-blue-500 font-mono font-bold tracking-wider"
              />
              <button
                type="button"
                @click="handleForgotSendSms"
                :disabled="forgotSendingSms || forgotCountdown > 0"
                class="px-3 py-2 bg-blue-50 hover:bg-blue-100 text-blue-700 border border-blue-200 rounded-xl font-bold text-[11px] whitespace-nowrap transition disabled:opacity-50"
              >
                <span v-if="forgotSendingSms">发送中...</span>
                <span v-else-if="forgotCountdown > 0">{{ forgotCountdown }}s</span>
                <span v-else>获取验证码</span>
              </button>
            </div>
          </div>

          <!-- 拟真短信卡片 (快捷填入) -->
          <div v-if="forgotSmsToast" class="p-2.5 bg-amber-50 border border-amber-200 rounded-xl text-[11px] text-amber-800 space-y-1">
            <div class="flex items-center justify-between font-bold">
              <span>📱 收到重置密码短信</span>
              <button
                type="button"
                @click="forgotForm.verify_code = '888888'"
                class="px-1.5 py-0.5 rounded bg-amber-200 hover:bg-amber-300 text-amber-900 font-bold text-[10px]"
              >
                点此自动填入: 888888
              </button>
            </div>
          </div>

          <div>
            <label class="block font-bold text-slate-700 mb-1">设定新密码</label>
            <input
              v-model="forgotForm.new_password"
              type="password"
              required
              minlength="6"
              placeholder="请输入不少于6位的新密码"
              class="w-full px-3 py-2 rounded-xl border border-slate-200 focus:outline-none focus:ring-2 focus:ring-blue-500 font-medium"
            />
          </div>

          <div>
            <label class="block font-bold text-slate-700 mb-1">确认新密码</label>
            <input
              v-model="forgotForm.confirm_password"
              type="password"
              required
              minlength="6"
              placeholder="请再次输入新密码"
              class="w-full px-3 py-2 rounded-xl border border-slate-200 focus:outline-none focus:ring-2 focus:ring-blue-500 font-medium"
            />
          </div>

          <div v-if="forgotError" class="p-2 bg-rose-50 border border-rose-200 rounded-xl text-rose-600 text-xs font-medium">
            ⚠️ {{ forgotError }}
          </div>

          <div v-if="forgotSuccess" class="p-2 bg-emerald-50 border border-emerald-200 rounded-xl text-emerald-700 text-xs font-medium">
            ✓ {{ forgotSuccess }}
          </div>

          <div class="pt-2 flex gap-2">
            <button
              type="button"
              @click="showForgotModal = false"
              class="flex-1 py-2.5 rounded-xl border border-slate-200 text-slate-600 font-bold hover:bg-slate-50 transition"
            >
              取消
            </button>
            <button
              type="submit"
              :disabled="forgotLoading"
              class="flex-1 py-2.5 rounded-xl bg-blue-600 hover:bg-blue-700 text-white font-bold shadow-md shadow-blue-500/20 transition disabled:opacity-50"
            >
              <span v-if="forgotLoading">正在重设...</span>
              <span v-else>确认重置密码</span>
            </button>
          </div>
        </form>
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
let countdownTimer = null

const handleSendSms = async () => {
  if (!form.value.identifier) {
    errorMsg.value = '请先输入接收短信验证码的手机号！'
    return
  }

  sendingSms.value = true
  errorMsg.value = ''
  try {
    const res = await api.post('/auth/send-sms', { phone: form.value.identifier })
    smsToast.value = true
    form.value.verify_code = res.data?.demo_code || '888888'
    
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

// 忘记密码弹窗状态与逻辑
const showForgotModal = ref(false)
const forgotLoading = ref(false)
const forgotSendingSms = ref(false)
const forgotCountdown = ref(0)
const forgotSmsToast = ref(false)
const forgotError = ref('')
const forgotSuccess = ref('')
let forgotTimer = null

const forgotForm = ref({
  phone: '13800000001',
  verify_code: '',
  new_password: '',
  confirm_password: ''
})

const openForgotModal = () => {
  forgotError.value = ''
  forgotSuccess.value = ''
  forgotForm.value.phone = form.value.identifier.length === 11 ? form.value.identifier : '13800000001'
  forgotForm.value.verify_code = ''
  forgotForm.value.new_password = ''
  forgotForm.value.confirm_password = ''
  showForgotModal.value = true
}

const handleForgotSendSms = async () => {
  if (!forgotForm.value.phone || forgotForm.value.phone.length !== 11) {
    forgotError.value = '请输入有效的11位手机号码'
    return
  }
  forgotSendingSms.value = true
  forgotError.value = ''
  try {
    const res = await api.post('/auth/send-sms', { phone: forgotForm.value.phone })
    forgotSmsToast.value = true
    forgotForm.value.verify_code = res.data?.demo_code || '888888'
    forgotCountdown.value = 60
    if (forgotTimer) clearInterval(forgotTimer)
    forgotTimer = setInterval(() => {
      if (forgotCountdown.value > 0) {
        forgotCountdown.value--
      } else {
        clearInterval(forgotTimer)
      }
    }, 1000)
  } catch (err) {
    forgotError.value = err.message || '短信发送失败'
  } finally {
    forgotSendingSms.value = false
  }
}

const handleResetPassword = async () => {
  if (forgotForm.value.new_password !== forgotForm.value.confirm_password) {
    forgotError.value = '两次输入的新密码不一致，请核对'
    return
  }
  if (forgotForm.value.new_password.length < 6) {
    forgotError.value = '新密码长度至少需要6位'
    return
  }
  forgotLoading.value = true
  forgotError.value = ''
  forgotSuccess.value = ''
  try {
    const res = await api.post('/auth/reset-password', {
      phone: forgotForm.value.phone,
      verify_code: forgotForm.value.verify_code,
      new_password: forgotForm.value.new_password
    })
    forgotSuccess.value = res.message || '密码重置成功！'
    setTimeout(() => {
      showForgotModal.value = false
      loginType.value = 'PASSWORD'
      form.value.identifier = forgotForm.value.phone
      form.value.password = forgotForm.value.new_password
    }, 1200)
  } catch (err) {
    forgotError.value = err.message || '密码重置失败'
  } finally {
    forgotLoading.value = false
  }
}

onUnmounted(() => {
  if (countdownTimer) clearInterval(countdownTimer)
  if (forgotTimer) clearInterval(forgotTimer)
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
      } else if (res.data.user.role === 'SUPER_ADMIN') {
        router.push('/admin/dashboard')
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
