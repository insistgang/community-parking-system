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
          <span v-else class="text-slate-400">业主</span>
        </p>
      </div>
    </div>

    <div class="flex items-center gap-2">
      <!-- 明暗主题切换 -->
      <button
        @click="userStore.toggleTheme()"
        class="p-2 rounded-full hover:bg-slate-100 text-slate-600 transition"
        :title="userStore.isDark ? '切换为浅色模式' : '切换为暗夜模式'"
      >
        <svg v-if="userStore.isDark" xmlns="http://www.w3.org/2000/svg" class="w-5 h-5 text-amber-400" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <circle cx="12" cy="12" r="4"/><path d="M12 2v2"/><path d="M12 20v2"/><path d="m4.93 4.93 1.41 1.41"/><path d="m17.66 17.66 1.41 1.41"/><path d="M2 12h2"/><path d="M20 12h2"/><path d="m6.34 17.66-1.41 1.41"/><path d="m19.07 4.93-1.41 1.41"/>
        </svg>
        <svg v-else xmlns="http://www.w3.org/2000/svg" class="w-5 h-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <path d="M12 3a6 6 0 0 0 9 9 9 9 0 1 1-9-9Z"/>
        </svg>
      </button>

      <!-- 消息通知入口 -->
      <router-link
        to="/notifications"
        class="relative p-2 rounded-full hover:bg-slate-100 text-slate-600 transition"
        title="消息通知"
      >
        <svg xmlns="http://www.w3.org/2000/svg" class="w-5 h-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
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
  </header>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../store/user'

const router = useRouter()
const userStore = useUserStore()

const unreadCount = computed(() => {
  return userStore.homeData?.unread_notifications ?? 0
})

const handleLogout = () => {
  if (confirm('确认退出当前账号吗？')) {
    userStore.logout()
    router.push('/login')
  }
}
</script>
