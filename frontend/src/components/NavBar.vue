<template>
  <header class="sticky top-0 z-40 bg-white/95 backdrop-blur border-b border-slate-200 px-4 py-3 flex items-center justify-between shadow-sm">
    <div class="flex items-center gap-2">
      <div class="w-8 h-8 rounded-lg bg-blue-600 flex items-center justify-center text-white font-bold shadow-sm">
        <svg xmlns="http://www.w3.org/2000/svg" class="w-5 h-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <rect width="18" height="18" x="3" y="3" rx="2"/><path d="M9 17V7h4a3 3 0 0 1 0 6H9"/>
        </svg>
      </div>
      <div>
        <h1 class="text-sm font-bold text-slate-800 flex items-center gap-1.5">
          <span>{{ userStore.communityName }}</span>
          <span class="text-[10px] px-1.5 py-0.5 rounded bg-blue-50 text-blue-600 font-medium border border-blue-200">公房停车</span>
        </h1>
        <p class="text-[11px] text-slate-500 flex items-center gap-1">
          <span>{{ userStore.realName }}</span>
          <span v-if="userStore.isPropertyAdmin" class="text-purple-600 font-semibold">(物业管理员)</span>
          <span v-else-if="userStore.isSuperAdmin" class="text-rose-600 font-semibold">(系统总管)</span>
          <span v-else class="text-slate-400">业主</span>
        </p>
      </div>
    </div>

    <div class="flex items-center gap-1 sm:gap-2">
      <!-- 修改密码按钮 -->
      <button
        @click="openChangePwdModal"
        class="p-2 rounded-full hover:bg-slate-100 text-slate-600 hover:text-blue-600 transition"
        title="修改密码"
      >
        <svg xmlns="http://www.w3.org/2000/svg" class="w-4 h-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <rect width="18" height="11" x="3" y="11" rx="2" ry="2"/><path d="M7 11V7a5 5 0 0 1 10 0v4"/>
        </svg>
      </button>

      <!-- 明暗主题切换 -->
      <button
        @click="userStore.toggleTheme()"
        class="p-2 rounded-full hover:bg-slate-100 text-slate-600 transition"
        :title="userStore.isDark ? '切换为浅色模式' : '切换为暗夜模式'"
      >
        <svg v-if="userStore.isDark" xmlns="http://www.w3.org/2000/svg" class="w-4 h-4 text-amber-400" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <circle cx="12" cy="12" r="4"/><path d="M12 2v2"/><path d="M12 20v2"/><path d="m4.93 4.93 1.41 1.41"/><path d="m17.66 17.66 1.41 1.41"/><path d="M2 12h2"/><path d="M20 12h2"/><path d="m6.34 17.66-1.41 1.41"/><path d="m19.07 4.93-1.41 1.41"/>
        </svg>
        <svg v-else xmlns="http://www.w3.org/2000/svg" class="w-4 h-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <path d="M12 3a6 6 0 0 0 9 9 9 9 0 1 1-9-9Z"/>
        </svg>
      </button>

      <!-- 消息通知入口 -->
      <router-link
        to="/notifications"
        class="relative p-2 rounded-full hover:bg-slate-100 text-slate-600 transition"
        title="消息通知"
      >
        <svg xmlns="http://www.w3.org/2000/svg" class="w-4 h-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <path d="M6 8a6 6 0 0 1 12 0c0 7 3 9 3 9H3s3-2 3-9"/><path d="M10.3 21a1.94 1.94 0 0 0 3.4 0"/>
        </svg>
        <span
          v-if="unreadCount > 0"
          class="absolute top-1 right-1 w-4 h-4 rounded-full bg-rose-500 text-white text-[10px] font-bold flex items-center justify-center animate-pulse"
        >
          {{ unreadCount }}
        </span>
      </router-link>

      <!-- 退出登录 -->
      <button
        @click="handleLogout"
        class="p-2 rounded-full hover:bg-slate-100 text-slate-500 hover:text-rose-600 transition text-xs flex items-center gap-1"
        title="退出登录"
      >
        <svg xmlns="http://www.w3.org/2000/svg" class="w-4 h-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"/><polyline points="16 17 21 12 16 7"/><line x1="21" x2="9" y1="12" y2="12"/>
        </svg>
      </button>
    </div>

    <!-- 修改密码弹窗 -->
    <div v-if="showChangePwdModal" class="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/60 backdrop-blur-sm animate-fade-in text-slate-800">
      <div class="bg-white rounded-3xl max-w-sm w-full p-6 shadow-2xl border border-slate-100 space-y-4">
        <div class="flex items-center justify-between border-b border-slate-100 pb-3">
          <div class="flex items-center gap-2">
            <span class="w-8 h-8 rounded-xl bg-purple-100 text-purple-600 flex items-center justify-center font-bold text-sm">
              🔐
            </span>
            <div>
              <h3 class="text-sm font-bold text-slate-900">修改登录密码</h3>
              <p class="text-[11px] text-slate-400">账号：{{ userStore.realName }} (需验证原密码)</p>
            </div>
          </div>
          <button @click="showChangePwdModal = false" class="text-slate-400 hover:text-slate-600 p-1 text-base">✕</button>
        </div>

        <form @submit.prevent="handleChangePassword" class="space-y-3 text-xs">
          <div>
            <label class="block font-bold text-slate-700 mb-1">原登录密码</label>
            <input
              v-model="pwdForm.old_password"
              type="password"
              required
              placeholder="请输入当前生效的原密码"
              class="w-full px-3 py-2 rounded-xl border border-slate-200 focus:outline-none focus:ring-2 focus:ring-purple-500 font-medium"
            />
          </div>

          <div>
            <label class="block font-bold text-slate-700 mb-1">设置新密码</label>
            <input
              v-model="pwdForm.new_password"
              type="password"
              required
              minlength="6"
              placeholder="请输入不少于6位的新密码"
              class="w-full px-3 py-2 rounded-xl border border-slate-200 focus:outline-none focus:ring-2 focus:ring-purple-500 font-medium"
            />
          </div>

          <div>
            <label class="block font-bold text-slate-700 mb-1">确认新密码</label>
            <input
              v-model="pwdForm.confirm_password"
              type="password"
              required
              minlength="6"
              placeholder="请再次输入新密码"
              class="w-full px-3 py-2 rounded-xl border border-slate-200 focus:outline-none focus:ring-2 focus:ring-purple-500 font-medium"
            />
          </div>

          <div v-if="pwdError" class="p-2 bg-rose-50 border border-rose-200 rounded-xl text-rose-600 text-xs font-medium">
            ⚠️ {{ pwdError }}
          </div>

          <div v-if="pwdSuccess" class="p-2 bg-emerald-50 border border-emerald-200 rounded-xl text-emerald-700 text-xs font-medium">
            ✓ {{ pwdSuccess }}
          </div>

          <div class="pt-2 flex gap-2">
            <button
              type="button"
              @click="showChangePwdModal = false"
              class="flex-1 py-2.5 rounded-xl border border-slate-200 text-slate-600 font-bold hover:bg-slate-50 transition"
            >
              取消
            </button>
            <button
              type="submit"
              :disabled="pwdLoading"
              class="flex-1 py-2.5 rounded-xl bg-purple-600 hover:bg-purple-700 text-white font-bold shadow-md shadow-purple-500/20 transition disabled:opacity-50"
            >
              <span v-if="pwdLoading">正在提交...</span>
              <span v-else>确认修改密码</span>
            </button>
          </div>
        </form>
      </div>
    </div>
  </header>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../store/user'
import { showConfirm } from '../utils/dialog'
import api from '../api'

const router = useRouter()
const userStore = useUserStore()

const unreadCount = computed(() => {
  return userStore.homeData?.unread_notifications ?? 0
})

const showChangePwdModal = ref(false)
const pwdLoading = ref(false)
const pwdError = ref('')
const pwdSuccess = ref('')
const pwdForm = ref({
  old_password: '',
  new_password: '',
  confirm_password: ''
})

const openChangePwdModal = () => {
  pwdError.value = ''
  pwdSuccess.value = ''
  pwdForm.value.old_password = ''
  pwdForm.value.new_password = ''
  pwdForm.value.confirm_password = ''
  showChangePwdModal.value = true
}

const handleChangePassword = async () => {
  if (pwdForm.value.new_password !== pwdForm.value.confirm_password) {
    pwdError.value = '两次输入的新密码不一致，请核对'
    return
  }
  if (pwdForm.value.new_password.length < 6) {
    pwdError.value = '新密码长度至少需要6位'
    return
  }
  pwdLoading.value = true
  pwdError.value = ''
  pwdSuccess.value = ''
  try {
    const res = await api.post('/user/change-password', {
      old_password: pwdForm.value.old_password,
      new_password: pwdForm.value.new_password
    })
    pwdSuccess.value = res.message || '密码修改成功！'
    setTimeout(() => {
      showChangePwdModal.value = false
    }, 1200)
  } catch (err) {
    pwdError.value = err.message || '修改密码失败，请核对原密码'
  } finally {
    pwdLoading.value = false
  }
}

const handleLogout = async () => {
  const ok = await showConfirm({
    title: '退出登录确认',
    message: '确认退出当前账号登录吗？',
    confirmText: '退出登录',
    type: 'warning'
  })
  if (ok) {
    userStore.logout()
    router.push('/login')
  }
}
</script>
