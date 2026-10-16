<template>
  <div v-if="visible" class="fixed inset-0 z-50 flex items-center justify-center p-3 bg-slate-900/70 backdrop-blur-sm">
    <div class="bg-white rounded-2xl max-w-xl w-full max-h-[92vh] overflow-y-auto p-5 sm:p-7 shadow-2xl border-2 border-blue-300 text-slate-800">
      <div class="flex items-center justify-between gap-3 border-b border-slate-200 pb-4 mb-4">
        <div class="flex items-center gap-3">
          <div class="w-9 h-9 rounded-lg bg-blue-600 flex items-center justify-center text-white"><AppIcon name="shield" :size="20" /></div>
          <div>
            <h3 class="text-sm font-bold text-slate-900">电子存证凭据</h3>
            <p class="text-[11px] text-slate-500 mt-0.5">本地数据库存证与 SHA-256 哈希链校验</p>
          </div>
        </div>
        <button type="button" @click="$emit('close')" class="text-slate-400 hover:text-slate-700" aria-label="关闭"><AppIcon name="close" size="1em" /></button>
      </div>
      <div class="rounded-2xl border border-slate-200 bg-slate-50 p-4 sm:p-5 text-xs space-y-3">
        <dl class="space-y-3">
          <div class="flex justify-between gap-3"><dt class="text-slate-500">存证凭据流水号</dt><dd class="font-mono font-bold text-blue-700 break-all text-right">{{ record.evidence_id }}</dd></div>
          <div class="flex justify-between gap-3"><dt class="text-slate-500">本地记录序号</dt><dd class="font-mono font-bold text-indigo-600">#{{ record.block_height }}</dd></div>
          <div class="flex justify-between gap-3"><dt class="text-slate-500">存证事件类型</dt><dd class="rounded bg-blue-100 px-2 py-0.5 font-bold text-blue-800">{{ record.event_type }}</dd></div>
          <div><dt class="text-slate-500 mb-1">记录哈希（SHA-256）</dt><dd class="flex items-center gap-2 rounded-lg border border-slate-200 bg-white p-2 font-mono break-all"><span class="min-w-0 flex-1">{{ record.tx_hash }}</span><button type="button" @click="copyHash" class="shrink-0 font-sans font-bold text-blue-600">{{ copied ? '已复制' : '复制' }}</button></dd></div>
          <div><dt class="text-slate-500 mb-1">前序记录哈希</dt><dd class="rounded-lg border border-slate-200 bg-white p-2 font-mono text-[11px] text-slate-500 break-all">{{ record.prev_hash }}</dd></div>
          <div class="flex justify-between gap-3"><dt class="text-slate-500">存证时间戳</dt><dd class="font-mono text-right">{{ record.created_at || record.timestamp }}</dd></div>
          <div class="border-t border-slate-200 pt-3"><dt class="text-slate-500 mb-1">存证业务中文摘要</dt><dd class="rounded-xl border border-slate-200 bg-white p-3 font-medium leading-relaxed">{{ record.event_summary }}</dd></div>
        </dl>
        <div class="flex items-center justify-between gap-3 border-t border-slate-200 pt-3">
        <div class="min-w-0 space-y-1 text-xs">
          <p class="font-bold text-slate-800"><span class="text-rose-600">●</span> 慧停管家本地电子存证</p>
          <p class="leading-relaxed text-slate-500">核验底座：系统数据库 · SHA-256 哈希链</p>
          <p class="leading-relaxed text-slate-500">浦江数链接口待接入，当前记录尚未上链。</p>
        </div>
        <svg viewBox="0 0 160 160" role="img" aria-label="慧停管家本地电子存证展示章，浦江数链待接入"
          class="h-32 w-32 shrink-0 -rotate-12 text-rose-600 sm:h-36 sm:w-36">
          <defs><path id="local-seal-arc" d="M 19,80 A 61,61 0 1,1 141,80" fill="none" /></defs>
          <circle cx="80" cy="80" r="75" fill="none" stroke="currentColor" stroke-width="3" />
          <circle cx="80" cy="80" r="69" fill="none" stroke="currentColor" stroke-width="1" />
          <circle cx="80" cy="80" r="48" fill="none" stroke="currentColor" stroke-width="1" stroke-dasharray="3 3" />
          <text fill="currentColor" font-size="11" font-weight="700" letter-spacing="2">
            <textPath href="#local-seal-arc" startOffset="50%" text-anchor="middle">慧停管家本地电子存证</textPath>
          </text>
          <path d="M80 54l7 17h18L91 82l5 18-16-11-16 11 5-18-14-11h18z" fill="currentColor" />
          <text x="80" y="117" text-anchor="middle" fill="currentColor" font-size="13" font-weight="900">本地存证</text>
          <text x="80" y="135" text-anchor="middle" fill="currentColor" font-size="9" font-weight="700">浦江数链待接入</text>
        </svg>
        </div>
      </div>
      <div class="mt-4 flex items-center justify-between gap-3">
        <p v-if="verified === true" class="text-xs font-semibold text-emerald-700">本地完整性校验通过</p>
        <p v-else-if="verified === false" class="text-xs font-semibold text-rose-700">本地完整性校验失败</p>
        <p v-else class="text-xs text-slate-500">{{ checking ? '正在核验…' : '可核验本地记录完整性' }}</p>
        <button type="button" @click="verify" :disabled="checking" class="rounded-xl bg-blue-600 px-4 py-2 text-white text-xs font-semibold disabled:opacity-50">核验记录</button>
      </div>
      <div class="mt-4 flex justify-end"><button type="button" @click="$emit('close')" class="rounded-xl bg-slate-800 px-5 py-2 text-xs font-semibold text-white">关闭凭据</button></div>
    </div>
  </div>
</template>

<script setup>
import AppIcon from './AppIcon.vue'
import { ref, watch } from 'vue'
import api from '../api'

const props = defineProps({ visible: Boolean, record: { type: Object, default: () => ({}) } })
defineEmits(['close'])
const checking = ref(false)
const verified = ref(null)
const copied = ref(false)

watch(() => props.record?.evidence_id, () => { verified.value = null; copied.value = false })

const copyHash = async () => {
  if (!props.record?.tx_hash) return
  try {
    if (navigator.clipboard?.writeText) await navigator.clipboard.writeText(props.record.tx_hash)
    else {
      const input = document.createElement('textarea')
      input.value = props.record.tx_hash
      document.body.appendChild(input)
      input.select()
      const success = document.execCommand('copy')
      input.remove()
      if (!success) throw new Error('copy failed')
    }
    copied.value = true
    setTimeout(() => { copied.value = false }, 2000)
  } catch { copied.value = false }
}

const verify = async () => {
  if (!props.record?.evidence_id) return
  checking.value = true
  try {
    const res = await api.get(`/evidence/verify/${encodeURIComponent(props.record.evidence_id)}`)
    verified.value = res.data?.is_valid === true
  } catch {
    verified.value = false
  } finally {
    checking.value = false
  }
}
</script>
