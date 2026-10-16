<template>
  <section v-if="history.length" class="space-y-2" aria-label="申请处理记录">
    <h4 class="text-xs font-bold" :class="dark ? 'text-slate-200' : 'text-slate-700'">申请处理记录</h4>
    <ol class="space-y-2">
      <li v-for="entry in history" :key="entry.id" class="rounded-xl border p-3 text-xs"
        :class="dark ? 'border-slate-700 bg-slate-950 text-slate-300' : 'border-slate-200 bg-slate-50 text-slate-700'">
        <div class="flex flex-wrap items-center justify-between gap-1 font-semibold">
          <span>第 {{ entry.version }} 版 · {{ labels[entry.event_type] || entry.event_type }}</span>
          <time class="font-normal opacity-70">{{ entry.created_at }}</time>
        </div>
        <p class="mt-1 leading-relaxed">{{ entry.summary }}</p>
        <p class="mt-1 text-[11px] opacity-70">操作人：{{ entry.actor_name || '系统' }}</p>
        <ApplicationDocumentList v-if="['SUBMITTED', 'SUBMITTED_LEGACY', 'RESUBMITTED'].includes(entry.event_type) && entry.snapshot?.attachments?.length"
          :documents="entry.snapshot.attachments" :dark="dark" class="mt-2" />
      </li>
    </ol>
  </section>
</template>

<script setup>
import ApplicationDocumentList from './ApplicationDocumentList.vue'
defineProps({ history: { type: Array, default: () => [] }, dark: { type: Boolean, default: false } })
const labels = {
  SUBMITTED: '材料提交', SUBMITTED_LEGACY: '历史材料', CORRECTION_REQUESTED: '要求补正',
  RESUBMITTED: '材料补正', APPROVED: '审核通过', REJECTED: '审核驳回',
  APPEALED: '申请复审', APPEAL_APPROVED: '复审通过', APPEAL_UPHELD: '维持驳回', CANCELLED: '申请撤销'
}
</script>
