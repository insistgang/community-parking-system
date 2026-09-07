<template>
  <div
    class="rounded-2xl border-2 p-4 transition"
    :class="dark
      ? 'border-slate-600 bg-slate-900/70'
      : 'border-slate-200 bg-slate-50'"
  >
    <div class="flex items-center justify-between mb-3">
      <div>
        <h4 class="text-xs font-black" :class="dark ? 'text-white' : 'text-slate-900'">
          {{ title }}
        </h4>
        <p class="text-[11px] mt-0.5" :class="dark ? 'text-slate-400' : 'text-slate-500'">
          {{ hint }}
        </p>
      </div>
      <span
        class="text-[10px] font-bold px-2 py-0.5 rounded-full border"
        :class="matched
          ? 'bg-emerald-50 text-emerald-700 border-emerald-200'
          : (dark ? 'bg-purple-900/40 text-purple-200 border-purple-500/40' : 'bg-blue-50 text-blue-700 border-blue-200')"
      >
        {{ matched ? '人证比对通过' : '活体检测' }}
      </span>
    </div>

    <div class="grid grid-cols-2 gap-3">
      <!-- 证件头像 -->
      <div class="text-center space-y-1.5">
        <div
          class="relative mx-auto w-28 h-36 rounded-xl overflow-hidden border"
          :class="dark ? 'border-slate-600 bg-slate-800' : 'border-slate-200 bg-white'"
        >
          <img :src="idPortrait" alt="证件人像" class="w-full h-full object-cover" />
          <span
            class="absolute bottom-0 inset-x-0 text-[10px] py-0.5 font-semibold"
            :class="dark ? 'bg-black/60 text-slate-200' : 'bg-slate-900/70 text-white'"
          >证件人像</span>
        </div>
        <span class="block text-[10px]" :class="dark ? 'text-slate-400' : 'text-slate-500'">{{ name }} · 身份证</span>
      </div>

      <!-- 现场采集框 -->
      <div class="text-center space-y-1.5">
        <div
          class="relative mx-auto w-28 h-36 rounded-xl overflow-hidden border-2"
          :class="frameClass"
        >
          <img v-if="phase === 'matched' || phase === 'comparing'" :src="livePortrait" alt="现场人脸" class="w-full h-full object-cover" />
          <div v-else class="absolute inset-0 flex items-center justify-center">
            <div class="w-20 h-24 rounded-full border-2 border-dashed border-emerald-400/80 animate-pulse" />
          </div>
          <div v-if="phase === 'capturing'" class="absolute inset-0 bg-emerald-400/10 pointer-events-none">
            <div class="absolute inset-x-0 h-0.5 bg-emerald-400 shadow-[0_0_12px_#34d399] animate-scan" />
          </div>
          <span
            class="absolute bottom-0 inset-x-0 text-[10px] py-0.5 font-semibold"
            :class="dark ? 'bg-black/60 text-slate-200' : 'bg-slate-900/70 text-white'"
          >{{ liveLabel }}</span>
        </div>
        <span class="block text-[10px]" :class="dark ? 'text-slate-400' : 'text-slate-500'">现场采集</span>
      </div>
    </div>

    <div v-if="phase === 'capturing'" class="mt-3 text-center text-[11px] font-bold text-emerald-500 font-mono">
      {{ captureHint }}
    </div>

    <div v-if="phase === 'comparing'" class="mt-3 text-center text-[11px] font-bold text-amber-500 font-mono">
      正在与证件照进行人证比对…
    </div>

    <div
      v-if="matched"
      class="mt-3 rounded-xl p-2.5 text-[11px] space-y-1"
      :class="dark ? 'bg-emerald-950/40 border border-emerald-700/50 text-emerald-300' : 'bg-emerald-50 border border-emerald-200 text-emerald-800'"
    >
      <div class="flex items-center justify-between font-bold">
        <span>✓ 人证比对一致</span>
        <span class="font-mono">相似度 {{ score }}%</span>
      </div>
      <div class="flex items-center justify-between opacity-80">
        <span>活体检测（眨眼 / 转头）</span>
        <span>通过</span>
      </div>
      <div class="flex items-center justify-between opacity-80">
        <span>证件人像 ↔ 现场人脸</span>
        <span>同一人</span>
      </div>
    </div>

    <button
      v-if="!matched"
      type="button"
      @click="startCapture"
      :disabled="phase === 'capturing' || phase === 'comparing'"
      class="mt-3 w-full py-2.5 rounded-xl font-bold text-xs shadow-md transition active:scale-[0.98] disabled:opacity-50"
      :class="dark
        ? 'bg-purple-600 hover:bg-purple-500 text-white'
        : 'bg-blue-600 hover:bg-blue-700 text-white'"
    >
      {{ phase === 'idle' ? '开始活体人脸采集' : '采集核验中…' }}
    </button>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'

const props = defineProps({
  name: { type: String, default: '申请人' },
  title: { type: String, default: '法人 / 业主实人认证' },
  hint: { type: String, default: '采集人脸并与身份证照片自动比对' },
  persona: { type: String, default: 'zhang' }, // zhang | wang
  dark: { type: Boolean, default: false },
  preMatched: { type: Boolean, default: false }
})

const emit = defineEmits(['matched'])

const phase = ref(props.preMatched ? 'matched' : 'idle') // idle | capturing | comparing | matched

watch(() => props.preMatched, (v) => {
  if (v) phase.value = 'matched'
})
const captureHint = ref('请正视镜头')
const score = ref('99.7')
const matched = computed(() => phase.value === 'matched')

const portraitSvg = (persona, live) => {
  const female = persona !== 'wang'
  const skin = female ? '%23f1c7b0' : '%23e0b089'
  const hair = female ? '%231e293b' : '%23334155'
  const shirt = live ? (female ? '%233b82f6' : '%236d28d9') : (female ? '%231e40af' : '%234c1d95')
  const bg = live ? '%230f172a' : '%23e2e8f0'
  return `data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" width="160" height="200" viewBox="0 0 160 200"><rect width="160" height="200" fill="${bg}"/><ellipse cx="80" cy="170" rx="58" ry="40" fill="${shirt}"/><circle cx="80" cy="88" r="38" fill="${skin}"/><path d="M42 80 C42 42, 118 42, 118 80 L118 70 C118 28, 42 28, 42 70 Z" fill="${hair}"/><ellipse cx="66" cy="90" rx="5" ry="6" fill="%231e293b"/><ellipse cx="94" cy="90" rx="5" ry="6" fill="%231e293b"/><path d="M70 108 Q80 116 90 108" fill="none" stroke="%239f1239" stroke-width="2"/></svg>`
}

const idPortrait = computed(() => portraitSvg(props.persona, false))
const livePortrait = computed(() => portraitSvg(props.persona, true))

const liveLabel = computed(() => {
  if (phase.value === 'capturing') return '采集中'
  if (phase.value === 'comparing') return '比对中'
  if (phase.value === 'matched') return '现场人脸'
  return '待采集'
})

const frameClass = computed(() => {
  if (phase.value === 'matched') return 'border-emerald-500'
  if (phase.value === 'capturing') return 'border-emerald-400'
  return props.dark ? 'border-slate-600 bg-slate-800' : 'border-dashed border-slate-300 bg-slate-100'
})

const startCapture = () => {
  phase.value = 'capturing'
  const hints = ['请正视镜头', '请眨眼一次', '请缓慢左右转头']
  let i = 0
  captureHint.value = hints[0]
  const tick = setInterval(() => {
    i += 1
    if (i < hints.length) {
      captureHint.value = hints[i]
    } else {
      clearInterval(tick)
      phase.value = 'comparing'
      setTimeout(() => {
        phase.value = 'matched'
        emit('matched', { score: Number(score.value), name: props.name })
      }, 900)
    }
  }, 700)
}
</script>
