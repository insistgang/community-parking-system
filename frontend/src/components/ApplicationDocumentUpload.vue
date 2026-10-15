<template>
  <div class="rounded-xl border border-slate-200 bg-slate-50 p-4 space-y-3">
    <div class="flex items-center gap-2">
      <AppIcon :name="icon" :size="19" class="text-blue-600" />
      <h3 class="text-sm font-semibold text-slate-800">{{ title }} <span class="text-rose-500">*</span></h3>
    </div>
    <p class="text-xs text-slate-500 leading-relaxed">{{ hint }}</p>
    <div v-if="modelValue" class="rounded-xl bg-white border border-slate-200 p-3 space-y-2">
      <div class="flex items-start gap-2">
        <AppIcon name="file-check" :size="18" class="text-blue-600 mt-0.5" />
        <div class="min-w-0 flex-1">
          <p class="text-xs font-medium text-slate-800 break-all">{{ modelValue.name }}</p>
          <p class="text-[11px] text-slate-500 mt-1">{{ formatSize(modelValue.size_bytes) }} · 已上传，待物业人工核验</p>
        </div>
      </div>
      <div class="flex items-center gap-4 text-xs">
        <button type="button" :disabled="busy" @click="download" class="text-blue-600 font-semibold disabled:opacity-50">
          <AppIcon name="download" :size="14" /> 下载查看
        </button>
        <button type="button" :disabled="busy" @click="remove" class="text-rose-600 font-semibold disabled:opacity-50">
          <AppIcon name="close" :size="14" /> 移除
        </button>
      </div>
    </div>
    <input ref="fileInput" :id="`upload-${type}`" type="file" accept=".jpg,.jpeg,.png,.pdf" :disabled="busy"
      class="sr-only" @change="upload" />
    <button type="button" :disabled="busy" @click="fileInput?.click()" :aria-controls="`upload-${type}`"
      class="flex w-full items-center justify-center gap-2 rounded-xl border border-dashed border-blue-300 bg-blue-50 px-3 py-3 text-xs font-semibold text-blue-700 hover:bg-blue-100 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-blue-500 disabled:opacity-50">
      <AppIcon :name="busy ? 'loading' : 'upload'" :size="17" :class="busy ? 'animate-spin' : ''" />
      {{ busy ? '正在处理…' : modelValue ? `更换${title}` : `上传${title}` }}
    </button>
    <p class="text-[11px] text-slate-400">支持 JPG、PNG、PDF，单份不超过 10MB；多页材料可合并为 PDF。</p>
    <p v-if="error" role="alert" class="text-xs text-rose-600 leading-relaxed">{{ error }}</p>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import api from '../api'
import AppIcon from './AppIcon.vue'
import { downloadApplicationDocument } from '../utils/applicationDocuments'

const props = defineProps({
  type: { type: String, required: true }, title: { type: String, required: true },
  hint: { type: String, default: '' }, icon: { type: String, default: 'file' },
  modelValue: { type: Object, default: null }
})
const emit = defineEmits(['update:modelValue', 'busy'])
const fileInput = ref(null)
const busy = ref(false)
const error = ref('')
const setBusy = (value) => { busy.value = value; emit('busy', value) }
const formatSize = (value) => value >= 1024 * 1024 ? `${(value / 1024 / 1024).toFixed(1)} MB` : `${Math.max(1, Math.ceil(value / 1024))} KB`

const upload = async (event) => {
  const file = event.target.files?.[0]
  event.target.value = ''
  if (!file || busy.value) return
  error.value = ''
  if (!/\.(jpe?g|png|pdf)$/i.test(file.name)) { error.value = '请选择 JPG、PNG 或 PDF 文件'; return }
  if (file.size > 10 * 1024 * 1024) { error.value = '每份材料不能超过 10MB'; return }
  setBusy(true)
  try {
    const previous = props.modelValue
    const body = new FormData()
    body.append('document_type', props.type)
    body.append('file', file)
    const response = await api.post('/parking/application-documents', body, { timeout: 60000 })
    emit('update:modelValue', response.data)
    if (previous?.id) {
      try { await api.delete(`/parking/application-documents/${encodeURIComponent(previous.id)}`) }
      catch { error.value = '新材料已保存，旧草稿暂未清理，您可继续申请。' }
    }
  } catch (err) { error.value = err.message || '上传失败，请重试' }
  finally { setBusy(false) }
}
const remove = async () => {
  if (!props.modelValue?.id || busy.value) return
  error.value = ''
  setBusy(true)
  try {
    await api.delete(`/parking/application-documents/${encodeURIComponent(props.modelValue.id)}`)
    emit('update:modelValue', null)
  } catch (err) { error.value = err.message || '移除失败，请重试' }
  finally { setBusy(false) }
}
const download = async () => {
  error.value = ''
  setBusy(true)
  try { await downloadApplicationDocument(props.modelValue) }
  catch (err) { error.value = err.message || '下载失败，请重试' }
  finally { setBusy(false) }
}
</script>
