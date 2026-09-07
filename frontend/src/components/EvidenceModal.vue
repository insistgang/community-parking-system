<template>
  <div v-if="visible" class="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/70 backdrop-blur-sm animate-fade-in">
    <div class="bg-white rounded-2xl max-w-lg w-full p-6 shadow-2xl border-2 border-blue-400/50 relative overflow-hidden text-slate-800">
      <!-- 水印背景 -->
      <div class="absolute inset-0 flex items-center justify-center opacity-5 pointer-events-none select-none">
        <span class="text-7xl font-black text-blue-900 rotate-[-25deg]">BLOCKCHAIN</span>
      </div>

      <!-- 顶部 Header -->
      <div class="flex items-center justify-between border-b border-slate-200 pb-3 mb-4">
        <div class="flex items-center gap-2">
          <div class="w-7 h-7 rounded bg-blue-600 flex items-center justify-center text-white">
            <svg xmlns="http://www.w3.org/2000/svg" class="w-4 h-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
              <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10"/>
              <path d="m9 12 2 2 4-4"/>
            </svg>
          </div>
          <div>
            <h3 class="text-sm font-bold text-slate-900">区块链电子存证防伪凭据</h3>
            <p class="text-[10px] text-slate-400">分布式账本存证与不可篡改审计链路</p>
          </div>
        </div>
        <button @click="$emit('close')" class="text-slate-400 hover:text-slate-600 p-1 rounded-full hover:bg-slate-100 transition">
          ✕
        </button>
      </div>

      <!-- 证书主体内容 -->
      <div class="space-y-3 text-xs bg-slate-50 p-4 rounded-xl border border-slate-200 relative">
        <div class="flex items-center justify-between">
          <span class="text-slate-500">存证凭据流水号:</span>
          <span class="font-mono font-bold text-blue-700 select-all">{{ record.evidence_id }}</span>
        </div>

        <div class="flex items-center justify-between">
          <span class="text-slate-500">虚拟区块高度:</span>
          <span class="font-mono font-bold text-indigo-600">#{{ record.block_height }}</span>
        </div>

        <div class="flex items-center justify-between">
          <span class="text-slate-500">存证事件类型:</span>
          <span class="px-2 py-0.5 rounded bg-blue-100 text-blue-800 font-semibold text-[11px]">{{ record.event_type }}</span>
        </div>

        <div>
          <span class="text-slate-500 block mb-1">交易哈希 (SHA-256):</span>
          <div class="bg-white p-2 rounded border border-slate-200 font-mono text-[11px] text-slate-700 break-all flex items-center justify-between gap-1">
            <span>{{ record.tx_hash }}</span>
            <button @click="copyHash(record.tx_hash)" class="text-blue-600 hover:text-blue-800 whitespace-nowrap text-[11px] font-semibold underline">
              {{ copied ? '已复制' : '复制' }}
            </button>
          </div>
        </div>

        <div>
          <span class="text-slate-500 block mb-1">前序区块哈希 (Prev Hash):</span>
          <div class="bg-white p-2 rounded border border-slate-200 font-mono text-[10px] text-slate-400 break-all">
            {{ record.prev_hash }}
          </div>
        </div>

        <div class="flex items-center justify-between">
          <span class="text-slate-500">存证时间戳:</span>
          <span class="text-slate-700 font-mono">{{ record.created_at || record.timestamp }}</span>
        </div>

        <div class="pt-2 border-t border-slate-200">
          <span class="text-slate-500 block mb-1">存证业务中文摘要:</span>
          <p class="font-medium text-slate-800 leading-relaxed bg-white p-2 rounded border border-slate-200">
            {{ record.event_summary }}
          </p>
        </div>

        <!-- 官方防伪验真电子印章 -->
        <div class="pt-4 flex items-center justify-end">
          <div class="w-32 h-32 border-2 border-emerald-600 rounded-full flex flex-col items-center justify-center text-emerald-700 p-2 text-center rotate-[-12deg] shadow-inner opacity-90 select-none bg-emerald-50/40">
            <span class="text-[9px] font-bold tracking-tighter">上海智慧社区服务中心</span>
            <span class="text-sm font-black tracking-widest my-0.5">区块链验真</span>
            <span class="text-[8px] font-mono">VERIFIED OK</span>
          </div>
        </div>
      </div>

      <!-- 底部关闭 -->
      <div class="mt-4 flex justify-end">
        <button
          @click="$emit('close')"
          class="px-5 py-2 rounded-xl bg-slate-800 hover:bg-slate-700 text-white text-xs font-semibold shadow transition active:scale-95"
        >
          关闭凭据
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'

const props = defineProps({
  visible: Boolean,
  record: {
    type: Object,
    default: () => ({})
  }
})

defineEmits(['close'])

const copied = ref(false)

const copyHash = (text) => {
  if (!text) return
  navigator.clipboard.writeText(text).then(() => {
    copied.value = true
    setTimeout(() => {
      copied.value = false
    }, 2000)
  })
}
</script>
