<template>
  <div class="min-h-screen bg-slate-100 flex flex-col pb-12">
    <!-- 顶部导航 -->
    <header class="sticky top-0 z-40 bg-white border-b border-slate-200 px-4 py-3.5 flex items-center justify-between shadow-sm">
      <div class="flex items-center gap-2">
        <button @click="$router.push('/')" class="icon-button text-slate-500 hover:text-slate-800" aria-label="返回首页">
          <AppIcon name="back" class="w-5 h-5" />
        </button>
        <h1 class="text-sm font-bold text-slate-900">消息通知中心</h1>
      </div>
      <span class="text-xs text-slate-400 font-medium">全部已读</span>
    </header>

    <main class="max-w-md w-full mx-auto p-4 space-y-3 flex-1">
      <div
        v-for="item in notifList"
        :key="item.id"
        class="bg-white rounded-2xl p-4 shadow-sm border border-slate-200 space-y-2.5 transition"
        :class="item.type === 'ALLOCATION' ? 'border-amber-300 bg-amber-50/40' : item.type === 'RANK_CHANGE' ? 'border-blue-200' : item.type === 'SYSTEM' ? 'border-indigo-200' : ''"
      >
        <div class="flex items-center justify-between">
          <div class="flex items-center gap-1.5 font-bold text-xs">
            <span v-if="item.type === 'ALLOCATION'" class="text-amber-600"><AppIcon name="success" size="1em" /></span>
            <span v-else-if="item.type === 'RANK_CHANGE'" class="text-blue-600"><AppIcon name="trend" size="1em" /></span>
            <span v-else-if="item.type === 'EXPIRATION'" class="text-rose-600"><AppIcon name="clock" size="1em" /></span>
            <span v-else class="text-slate-600"><AppIcon name="announcement" size="1em" /></span>
            <span class="text-slate-900">{{ displayTitle(item) }}</span>
          </div>

          <span class="text-[10px] text-slate-400 font-mono">{{ item.created_at }}</span>
        </div>

        <p class="text-xs text-slate-600 leading-relaxed bg-white/70 p-2.5 rounded-xl border border-slate-100">
          {{ displayContent(item) }}
        </p>

        <!-- 关联快捷操作 -->
        <div v-if="item.action_type" class="pt-1 flex justify-end">
          <button
            v-if="item.action_type === 'GO_CONFIRM'"
            @click="$router.push('/')"
            class="px-3 py-1.5 rounded-xl bg-amber-600 hover:bg-amber-700 text-white font-bold text-xs shadow-sm transition active:scale-95 flex items-center gap-1"
          >
            <span>立即办理 / 查看倒计时 <AppIcon name="arrow" size="1em" /></span>
          </button>
          <button
            v-else-if="item.action_type === 'GO_QUEUE'"
            @click="$router.push('/queue')"
            class="px-3 py-1.5 rounded-xl bg-blue-600 hover:bg-blue-700 text-white font-semibold text-xs shadow-sm transition active:scale-95 flex items-center gap-1"
          >
            <span>查看顺位 <AppIcon name="arrow" size="1em" /></span>
          </button>
        </div>
      </div>

      <div v-if="notifList.length === 0" class="bg-white rounded-2xl p-8 text-center text-slate-400 text-xs shadow-sm">
        暂无消息通知
      </div>
    </main>
  </div>
</template>

<script setup>
import AppIcon from '../components/AppIcon.vue'
import { ref, onMounted } from 'vue'
import api from '../api'

const notifList = ref([])

const displayTitle = (item) => {
  if (item.type === 'RANK_CHANGE') {
    return (item.title || '').replace('前移', '')
  }
  return item.title
}

const displayContent = (item) => {
  if (item.type === 'RANK_CHANGE') {
    const m = String(item.content || '').match(/第【(\d+)】/)
    if (m) return `您的实时排位是第【${m[1]}】位`
  }
  return item.content
}

const loadNotifications = async () => {
  try {
    const res = await api.get('/notifications')
    if (res.code === 200) {
      notifList.value = res.data
    }
  } catch (err) {
    console.error('获取通知失败:', err)
  }
}

onMounted(() => {
  loadNotifications()
})
</script>
