<template>
  <header class="sticky top-0 z-40 bg-white/95 border-b border-slate-200 shadow-sm">
    <div class="max-w-md mx-auto px-3 py-2.5 flex items-center justify-between gap-2">
      <div class="flex items-center gap-2 min-w-0">
        <span class="w-9 h-9 shrink-0 rounded-xl bg-blue-600 flex items-center justify-center text-white shadow-sm">
          <AppIcon name="parking" :size="23" />
        </span>
        <div class="min-w-0">
          <h1 class="text-[13px] font-bold text-slate-800 truncate" :title="userStore.communityName">{{ userStore.communityName }}</h1>
          <p class="text-[11px] text-slate-500 truncate">
            {{ userStore.realName }} · {{ userStore.isPropertyAdmin ? '物业管理员' : userStore.isSuperAdmin ? '平台管理员' : '业主' }}
          </p>
        </div>
      </div>
      <div class="flex shrink-0 items-center">
        <button type="button" @click="openChangePwdModal" class="icon-button text-slate-600 hover:text-blue-600" title="修改密码" aria-label="修改密码">
          <AppIcon name="key" :size="19" />
        </button>
        <button type="button" @click="userStore.toggleTheme()" class="icon-button text-slate-600"
          :title="userStore.isDark ? '切换为浅色模式' : '切换为深色模式'" :aria-label="userStore.isDark ? '切换为浅色模式' : '切换为深色模式'">
          <AppIcon :name="userStore.isDark ? 'sun' : 'moon'" :size="19" />
        </button>
        <router-link to="/notifications" class="icon-button relative text-slate-600" title="消息通知" :aria-label="unreadCount ? '消息通知，' + unreadCount + '条未读' : '消息通知'">
          <AppIcon name="bell" :size="19" />
          <span v-if="unreadCount > 0" class="absolute top-0.5 right-0 min-w-4 h-4 px-0.5 rounded-full bg-rose-500 text-white text-[9px] font-bold flex items-center justify-center">{{ unreadCount > 99 ? '99+' : unreadCount }}</span>
        </router-link>
        <button type="button" @click="handleLogout" class="icon-button text-slate-500 hover:text-rose-600" title="退出登录" aria-label="退出登录">
          <AppIcon name="logout" :size="19" />
        </button>
      </div>
    </div>

    <!-- 修改密码弹窗 -->
    <div v-if="showChangePwdModal" class="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/60 backdrop-blur-sm animate-fade-in text-slate-800">
      <div class="bg-white rounded-3xl max-w-sm w-full p-6 shadow-2xl border border-slate-100 space-y-4">
        <div class="flex items-center justify-between border-b border-slate-100 pb-3">
          <div class="flex items-center gap-2">
            <span class="w-8 h-8 rounded-xl bg-purple-100 text-purple-600 flex items-center justify-center font-bold text-sm">
              <AppIcon name="key" size="1em" />
            </span>
            <div>
              <h3 class="text-sm font-bold text-slate-900">修改登录密码</h3>
              <p class="text-[11px] text-slate-400">账号：{{ userStore.realName }} (需验证原密码)</p>
            </div>
          </div>
          <button @click="showChangePwdModal = false" class="text-slate-400 hover:text-slate-600 p-1 text-base" aria-label="关闭"><AppIcon name="close" size="1em" /></button>
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
            <AppIcon name="warning" size="1em" /> {{ pwdError }}
          </div>

          <div v-if="pwdSuccess" class="p-2 bg-emerald-50 border border-emerald-200 rounded-xl text-emerald-700 text-xs font-medium">
            <AppIcon name="check" size="1em" /> {{ pwdSuccess }}
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
import AppIcon from './AppIcon.vue'
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
