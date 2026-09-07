<template>
  <div class="min-h-screen bg-slate-100 flex flex-col pb-12">
    <!-- 顶部导航 -->
    <header class="sticky top-0 z-40 bg-white border-b border-slate-200 px-4 py-3.5 flex items-center justify-between shadow-sm">
      <div class="flex items-center gap-2">
        <button @click="$router.push('/')" class="text-slate-500 hover:text-slate-800 p-1">
          <svg xmlns="http://www.w3.org/2000/svg" class="w-5 h-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
            <path d="m15 18-6-6 6-6"/>
          </svg>
        </button>
        <h1 class="text-sm font-bold text-slate-900">排队顺位阳光脱敏公示</h1>
      </div>
      <span class="text-[11px] text-emerald-600 font-semibold bg-emerald-50 px-2 py-0.5 rounded-full border border-emerald-200 flex items-center gap-1">
        <span class="w-1.5 h-1.5 rounded-full bg-emerald-500 animate-pulse"></span>
        <span>全小区公开监督</span>
      </span>
    </header>

    <main class="max-w-md w-full mx-auto p-4 space-y-4 flex-1">
      <!-- Tab 切换 -->
      <div class="flex bg-slate-200/70 p-1 rounded-xl text-xs font-bold">
        <button
          @click="tab = 'QUEUE'"
          class="flex-1 py-2 rounded-lg transition"
          :class="tab === 'QUEUE' ? 'bg-white text-blue-600 shadow-sm' : 'text-slate-500 hover:text-slate-800'"
        >
          全队列公示大盘
        </button>
        <button
          @click="tab = 'TIMELINE'"
          class="flex-1 py-2 rounded-lg transition"
          :class="tab === 'TIMELINE' ? 'bg-white text-blue-600 shadow-sm' : 'text-slate-500 hover:text-slate-800'"
        >
          本人顺位变动轨迹
        </button>
      </div>

      <!-- Tab 1: 全队列公示列表 -->
      <div v-if="tab === 'QUEUE'" class="space-y-3">
        <div class="bg-blue-50/60 p-3 rounded-xl border border-blue-100 text-[11px] text-blue-800 flex items-start gap-2">
          <span>ℹ️</span>
          <p class="leading-relaxed">
            公示声明：依据基层隐私合规准则，对姓名和手机号已做星号脱敏。申请时间毫秒级精确，确保排队次序公信力与绝对防插队。
          </p>
        </div>

        <div class="bg-white rounded-2xl shadow-sm border border-slate-200 overflow-hidden">
          <div class="divide-y divide-slate-100 text-xs">
            <div
              v-for="item in queueList"
              :key="item.rank"
              class="p-3.5 flex items-center justify-between transition"
              :class="item.is_me ? 'bg-amber-50/70 border-l-4 border-amber-500' : 'hover:bg-slate-50'"
            >
              <div class="flex items-center gap-3">
                <span
                  class="w-7 h-7 rounded-lg flex items-center justify-center font-black text-xs"
                  :class="item.rank === 1 ? 'bg-amber-500 text-white shadow-sm' : item.rank === 2 ? 'bg-slate-700 text-white' : item.rank === 3 ? 'bg-amber-700 text-white' : 'bg-slate-100 text-slate-600'"
                >
                  {{ item.rank }}
                </span>
                <div>
                  <div class="flex items-center gap-1.5 font-bold text-slate-800">
                    <span>{{ item.masked_name }}</span>
                    <span v-if="item.is_me" class="text-[10px] bg-amber-500 text-white px-1.5 py-0.2 rounded font-semibold">本人</span>
                    <span class="text-[10px] text-slate-400 font-normal">({{ item.masked_phone }})</span>
                  </div>
                  <span class="text-[10px] text-slate-400 block mt-0.5">申报时间: {{ item.apply_time }}</span>
                </div>
              </div>

              <div>
                <span
                  class="text-[10px] px-2 py-0.5 rounded-full font-semibold"
                  :class="item.priority_type === '家庭一车' ? 'bg-blue-50 text-blue-700 border border-blue-200' : 'bg-slate-100 text-slate-600'"
                >
                  {{ item.priority_type }}
                </span>
              </div>
            </div>

            <div v-if="queueList.length === 0" class="p-8 text-center text-slate-400 text-xs">
              暂无排队中的申请单
            </div>
          </div>
        </div>
      </div>

      <!-- Tab 2: 本人排位变动时间线 -->
      <div v-else class="bg-white rounded-2xl p-5 shadow-sm border border-slate-200 space-y-4">
        <h3 class="text-xs font-bold text-slate-700">本人排位历程动态轨迹</h3>

        <div class="relative pl-6 space-y-6 before:absolute before:left-2.5 before:top-2 before:bottom-2 before:w-0.5 before:bg-blue-200">
          <div
            v-for="ev in timelineList"
            :key="ev.id"
            class="relative text-xs"
          >
            <span class="absolute -left-6 top-1 w-3 h-3 rounded-full bg-blue-600 ring-4 ring-blue-100"></span>
            <div>
              <span class="font-bold text-slate-800 block">{{ ev.title }}</span>
              <p class="text-slate-600 mt-1 leading-relaxed bg-slate-50 p-2.5 rounded-xl border border-slate-100">{{ ev.content }}</p>
              <span class="text-[10px] text-slate-400 mt-1 block font-mono">{{ ev.time }}</span>
            </div>
          </div>

          <div v-if="timelineList.length === 0" class="text-slate-400 text-xs py-4">
            暂无顺位变动记录
          </div>
        </div>
      </div>
    </main>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import api from '../api'

const tab = ref('QUEUE')
const queueList = ref([])
const timelineList = ref([])

const loadData = async () => {
  try {
    const qRes = await api.get('/parking/queue')
    if (qRes.code === 200) {
      queueList.value = qRes.data
    }
    const tRes = await api.get('/parking/my-timeline')
    if (tRes.code === 200) {
      timelineList.value = tRes.data
    }
  } catch (err) {
    console.error('加载公示失败:', err)
  }
}

onMounted(() => {
  loadData()
})
</script>
