<template>
  <div class="bg-gradient-to-br from-indigo-50 via-sky-50 to-blue-50 border border-indigo-100 rounded-2xl p-4 shadow-sm relative overflow-hidden">
    <div class="flex items-center justify-between">
      <div class="flex items-center gap-2">
        <div class="w-7 h-7 rounded-lg bg-indigo-600 text-white flex items-center justify-center font-bold text-xs shadow-sm">
          排
        </div>
        <div>
          <h4 class="text-xs font-bold text-slate-800 flex items-center gap-1.5">
            <span>轮候进度</span>
            <span class="text-[10px] px-1.5 py-0.2 rounded-full bg-indigo-100 text-indigo-700 font-semibold">
              实际释放为准
            </span>
          </h4>
          <span class="text-base font-extrabold text-indigo-900 tracking-tight block mt-0.5">
            {{ durationText }}
          </span>
        </div>
      </div>

      <button
        @click="expanded = !expanded"
        class="text-[11px] text-indigo-600 hover:text-indigo-800 font-medium flex items-center gap-0.5 bg-white/80 px-2 py-1 rounded-lg border border-indigo-200/60 shadow-2xl transition"
      >
        <span>{{ expanded ? '收起依据' : '测算依据' }}</span>
        <svg xmlns="http://www.w3.org/2000/svg" class="w-3.5 h-3.5 transition-transform" :class="{'rotate-180': expanded}" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <polyline points="6 9 12 15 18 9"/>
        </svg>
      </button>
    </div>

    <!-- 展开详情 -->
    <div v-if="expanded" class="mt-3 pt-3 border-t border-indigo-100 text-xs text-slate-600 space-y-1.5 animate-fade-in">
      <p class="leading-relaxed text-[11px] text-slate-700 font-medium bg-white/70 p-2.5 rounded-xl border border-indigo-100">
        <AppIcon name="chart" size="1em" /> <strong>说明：</strong>{{ basisText }}
      </p>
      <p class="text-[10px] text-slate-400 italic">
        * 实际分配以小区车位释放和系统消息通知为准。
      </p>
    </div>
  </div>
</template>

<script setup>
import AppIcon from './AppIcon.vue'
import { ref } from 'vue'

defineProps({
  durationText: {
    type: String,
    default: '等待实际车位释放'
  },
  basisText: {
    type: String,
    default: '分配时间取决于真实车位释放，暂无法准确预估。'
  }
})

const expanded = ref(false)
</script>
