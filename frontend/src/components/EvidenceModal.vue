<template>
  <div v-if="visible" class="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/70 backdrop-blur-sm">
    <div class="bg-white rounded-2xl max-w-lg w-full max-h-[90vh] overflow-y-auto p-6 shadow-2xl border border-blue-200 text-slate-800">
      <div class="flex items-start justify-between gap-3 border-b border-slate-200 pb-3 mb-4">
        <div>
          <h3 class="text-sm font-bold text-slate-900">浦江数链模拟存证</h3>
          <p class="text-[11px] text-slate-500 mt-1">记录存于本地数据库，可校验哈希链完整性；未调用真实区块链接口。</p>
        </div>
        <button type="button" @click="$emit('close')" class="text-slate-400 hover:text-slate-700" aria-label="关闭"><AppIcon name="close" size="1em" /></button>
      </div>
      <dl class="space-y-3 text-xs">
        <div class="flex justify-between gap-3"><dt class="text-slate-500">流水号</dt><dd class="font-mono break-all">{{ record.evidence_id }}</dd></div>
        <div class="flex justify-between gap-3"><dt class="text-slate-500">记录序号</dt><dd>#{{ record.block_height }}</dd></div>
        <div class="flex justify-between gap-3"><dt class="text-slate-500">事件类型</dt><dd>{{ record.event_type }}</dd></div>
        <div class="flex justify-between gap-3"><dt class="text-slate-500">记录时间</dt><dd>{{ record.created_at || record.timestamp }}</dd></div>
        <div><dt class="text-slate-500 mb-1">事件摘要</dt><dd class="rounded-xl bg-slate-50 p-3 leading-relaxed">{{ record.event_summary }}</dd></div>
        <div><dt class="text-slate-500 mb-1">SHA-256 哈希</dt><dd class="font-mono break-all rounded-xl bg-slate-50 p-3">{{ record.tx_hash }}</dd></div>
      </dl>
      <div class="mt-4 flex items-center justify-between gap-3 rounded-xl border border-rose-200 bg-rose-50/50 p-3">
        <div class="min-w-0 space-y-1 text-xs">
          <p class="font-bold text-rose-800">本地模拟存证标识</p>
          <p class="leading-relaxed text-slate-600">申请、审核和排位记录已写入本地数据库，可用哈希链检查完整性。</p>
          <p class="leading-relaxed text-rose-700">此章仅用于页面展示；未连接浦江数链，不是官方电子印章或链上凭证。</p>
        </div>
        <svg viewBox="0 0 160 160" role="img" aria-label="浦江数链模拟存证展示章，未上真实链"
          class="h-28 w-28 shrink-0 -rotate-12 text-rose-600 sm:h-32 sm:w-32">
          <defs><path id="local-seal-arc" d="M 19,80 A 61,61 0 1,1 141,80" fill="none" /></defs>
          <circle cx="80" cy="80" r="75" fill="none" stroke="currentColor" stroke-width="3" />
          <circle cx="80" cy="80" r="69" fill="none" stroke="currentColor" stroke-width="1" />
          <circle cx="80" cy="80" r="48" fill="none" stroke="currentColor" stroke-width="1" stroke-dasharray="3 3" />
          <text fill="currentColor" font-size="11" font-weight="700" letter-spacing="2">
            <textPath href="#local-seal-arc" startOffset="50%" text-anchor="middle">浦江数链模拟存证</textPath>
          </text>
          <text x="80" y="79" text-anchor="middle" fill="currentColor" font-size="24" font-weight="900">模拟</text>
          <text x="80" y="99" text-anchor="middle" fill="currentColor" font-size="12" font-weight="700">本地记录</text>
          <text x="80" y="123" text-anchor="middle" fill="currentColor" font-size="8" font-weight="700" letter-spacing="1">NOT ON CHAIN</text>
        </svg>
      </div>
      <div class="mt-5 flex items-center justify-between gap-3">
        <p v-if="verified === true" class="text-xs font-semibold text-emerald-700">本地完整性校验通过</p>
        <p v-else-if="verified === false" class="text-xs font-semibold text-rose-700">本地完整性校验失败</p>
        <p v-else class="text-xs text-slate-500">{{ checking ? '正在核验…' : '可核验本地记录完整性' }}</p>
        <button type="button" @click="verify" :disabled="checking" class="rounded-xl bg-blue-600 px-4 py-2 text-white text-xs font-semibold disabled:opacity-50">核验记录</button>
      </div>
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

watch(() => props.record?.evidence_id, () => { verified.value = null })

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
