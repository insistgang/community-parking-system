<template>
  <div v-if="visible" class="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/70 backdrop-blur-sm">
    <div class="bg-white rounded-2xl max-w-lg w-full p-6 shadow-2xl border border-blue-200 text-slate-800">
      <div class="flex items-start justify-between gap-3 border-b border-slate-200 pb-3 mb-4">
        <div>
          <h3 class="text-sm font-bold text-slate-900">系统审计记录</h3>
          <p class="text-[11px] text-slate-500 mt-1">本地哈希链用于发现记录改动；浦江数链尚未接入。</p>
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
