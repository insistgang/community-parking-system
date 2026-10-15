<template>
  <section class="space-y-2" aria-label="已提交的证明材料">
    <h3 class="text-xs font-semibold" :class="dark ? 'text-slate-200' : 'text-slate-700'">已提交材料 · {{ documents.length }} 份</h3>
    <div v-for="document in documents" :key="document.id || document.name"
      class="flex items-center gap-2 rounded-xl border px-3 py-2.5"
      :class="dark ? 'bg-slate-900 border-slate-700' : 'bg-slate-50 border-slate-200'">
      <AppIcon name="file" :size="18" :class="dark ? 'text-blue-400' : 'text-blue-600'" />
      <div class="flex-1 min-w-0">
        <p class="text-xs font-medium" :class="dark ? 'text-slate-200' : 'text-slate-700'">{{ document.type_label || documentTypeLabels[document.type] || '证明材料' }}</p>
        <p class="text-[11px] truncate mt-0.5" :class="dark ? 'text-slate-400' : 'text-slate-500'" :title="document.name">{{ document.name }}</p>
      </div>
      <button v-if="document.id" type="button" @click="download(document)" :disabled="!!busyId"
        :aria-label="`下载查看${document.type_label || documentTypeLabels[document.type] || '材料'}`"
        class="shrink-0 rounded-lg px-2 py-1.5 text-xs font-semibold disabled:opacity-50" :class="dark ? 'text-blue-400' : 'text-blue-600'">
        <AppIcon :name="busyId === document.id ? 'loading' : 'download'" :size="15" :class="busyId === document.id ? 'animate-spin' : ''" /> 下载查看
      </button>
      <span v-else class="text-[11px] text-slate-400">线下记录</span>
    </div>
    <p v-if="error" role="alert" class="text-xs text-rose-500">{{ error }}</p>
  </section>
</template>

<script setup>
import { ref } from 'vue'
import AppIcon from './AppIcon.vue'
import { documentTypeLabels, downloadApplicationDocument } from '../utils/applicationDocuments'
defineProps({ documents: { type: Array, default: () => [] }, dark: { type: Boolean, default: false } })
const busyId = ref('')
const error = ref('')
const download = async (document) => {
  busyId.value = document.id
  error.value = ''
  try { await downloadApplicationDocument(document) }
  catch (err) { error.value = err.message || '材料下载失败，请重试' }
  finally { busyId.value = '' }
}
</script>
