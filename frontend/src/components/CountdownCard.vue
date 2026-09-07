<template>
  <div class="rounded-2xl p-4 bg-gradient-to-br from-amber-500 via-amber-600 to-orange-600 text-white shadow-lg border border-amber-300/40 relative overflow-hidden">
    <!-- 装饰光影 -->
    <div class="absolute -right-6 -bottom-6 w-32 h-32 bg-white/10 rounded-full blur-xl pointer-events-none"></div>

    <div class="flex items-start justify-between relative z-10">
      <div class="flex items-center gap-2">
        <span class="inline-flex items-center justify-center w-8 h-8 rounded-full bg-white/20 backdrop-blur text-lg">
          🎉
        </span>
        <div>
          <h2 class="text-base font-extrabold tracking-wide">恭喜！车位已中签锁定</h2>
          <p class="text-xs text-amber-100">请在 48 小时内确认前往物业办理线下手续</p>
        </div>
      </div>
      <span class="px-2 py-0.5 rounded-full bg-amber-900/40 text-amber-200 text-[11px] font-bold border border-amber-300/30">
        48h 限时确认
      </span>
    </div>

    <!-- 车位信息 -->
    <div class="my-3.5 bg-black/15 backdrop-blur rounded-xl p-3 flex items-center justify-between border border-white/15">
      <div>
        <span class="text-[11px] text-amber-200 block">分配锁定车位号</span>
        <span class="text-xl font-black tracking-wider text-white">{{ spaceNo }}</span>
      </div>
      <div class="text-right">
        <span class="text-[11px] text-amber-200 block">办理剩余倒计时</span>
        <div class="font-mono text-base font-black tracking-widest text-yellow-300">
          {{ formattedTime }}
        </div>
      </div>
    </div>

    <!-- 状态判断与操作 -->
    <div v-if="isOfflinePending" class="bg-amber-800/60 rounded-xl p-3 text-xs text-amber-100 border border-amber-400/40">
      <div class="flex items-center gap-1.5 font-bold text-white mb-1">
        <span class="w-2 h-2 rounded-full bg-emerald-400 animate-ping"></span>
        <span>已进入“线下办理中”状态</span>
      </div>
      <p class="leading-relaxed">请携带车主本人身份证件前往小区物业服务中心签署纸质租赁合同并缴纳租金，物业审核确认后车位正式生效。</p>
    </div>

    <div v-else class="flex items-center gap-2.5 pt-1">
      <button
        @click="handleAccept"
        :disabled="loading"
        class="flex-1 py-2.5 rounded-xl bg-white text-amber-900 hover:bg-amber-50 active:scale-[0.98] transition font-bold text-sm shadow-md flex items-center justify-center gap-1.5"
      >
        <span>确认前往办理</span>
        <svg xmlns="http://www.w3.org/2000/svg" class="w-4 h-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
          <path d="m9 18 6-6-6-6"/>
        </svg>
      </button>

      <button
        @click="handleGiveUp"
        :disabled="loading"
        class="px-3 py-2.5 rounded-xl bg-amber-900/40 hover:bg-amber-900/60 text-amber-100 text-xs font-semibold border border-white/20 active:scale-95 transition"
      >
        放弃机会
      </button>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import api from '../api'

const props = defineProps({
  spaceNo: {
    type: String,
    default: 'A-012'
  },
  initialSeconds: {
    type: Number,
    default: 172800 // 48h
  },
  isOfflinePending: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['refresh'])

const secondsLeft = ref(props.initialSeconds)
const loading = ref(false)
let timer = null

const formattedTime = computed(() => {
  if (secondsLeft.value <= 0) return '00:00:00 (已超时)'
  const h = Math.floor(secondsLeft.value / 3600)
  const m = Math.floor((secondsLeft.value % 3600) / 60)
  const s = secondsLeft.value % 60
  return `${String(h).padStart(2, '0')}:${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`
})

onMounted(() => {
  timer = setInterval(() => {
    if (secondsLeft.value > 0) {
      secondsLeft.value--
    }
  }, 1000)
})

onUnmounted(() => {
  if (timer) clearInterval(timer)
})

const handleAccept = async () => {
  if (!confirm('确认接受本次车位分配，并前往物业办公室办理线下签约缴费吗？')) return
  loading.value = true
  try {
    const res = await api.post('/parking/accept-allocation')
    alert(res.message || '确认成功！')
    emit('refresh')
  } catch (err) {
    alert('操作失败: ' + err.message)
  } finally {
    loading.value = false
  }
}

const handleGiveUp = async () => {
  if (!confirm('确定要放弃本次车位分配机会吗？放弃后该车位将自动顺延给下一位业主，且不可撤销！')) return
  loading.value = true
  try {
    const res = await api.post('/parking/give-up-allocation')
    alert(res.message || '已放弃分配')
    emit('refresh')
  } catch (err) {
    alert('操作失败: ' + err.message)
  } finally {
    loading.value = false
  }
}
</script>
