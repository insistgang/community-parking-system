<template>
  <!-- 顶部演示浮层: 默认收起，鼠标移入展开，移出隐藏 -->
  <div
    class="fixed top-0 inset-x-0 z-[70]"
    @mouseenter="open"
    @mouseleave="scheduleClose"
  >
    <!-- 始终可命中的细热区 + 收起时的提示条 -->
    <button
      type="button"
      class="w-full flex justify-center items-start h-3 group"
      @click="toggle"
      title="演示控制台（悬停展开）"
    >
      <span
        class="h-1.5 w-20 rounded-b-full transition"
        :class="visible ? 'bg-transparent' : 'bg-indigo-500/80 group-hover:w-28 group-hover:bg-indigo-400'"
      />
    </button>

    <div
      class="overflow-hidden transition-all duration-200 ease-out"
      :class="visible ? 'max-h-40 opacity-100' : 'max-h-0 opacity-0 pointer-events-none'"
    >
      <div class="bg-gradient-to-r from-blue-900 via-indigo-900 to-slate-900 text-white px-3 py-2 text-xs shadow-lg demo-console border-b border-indigo-400/20">
        <div class="max-w-4xl mx-auto flex flex-wrap items-center justify-between gap-2">
          <div class="flex items-center gap-1.5 font-semibold text-blue-200">
            <span class="inline-block w-2 h-2 rounded-full bg-emerald-400 animate-pulse"></span>
            <span>演示控制台 (一键免密填入):</span>
          </div>
          <div class="flex flex-wrap items-center gap-1.5">
            <button
              @click="fillAccount('310104199201011111', '123456', '张欣欣')"
              class="px-2.5 py-1 rounded bg-blue-700/80 hover:bg-blue-600 active:scale-95 transition text-white border border-blue-400/30 flex items-center gap-1"
              title="第一幕：未申请新客，测试外区房拦截与夫妻车"
            >
              <span>👤 张欣欣</span>
              <span class="text-[10px] text-blue-200 bg-blue-900/60 px-1 rounded">未申请</span>
            </button>

            <button
              @click="fillAccount('310104199002022222', '123456', '李雷')"
              class="px-2.5 py-1 rounded bg-amber-700/80 hover:bg-amber-600 active:scale-95 transition text-white border border-amber-400/30 flex items-center gap-1"
              title="第四幕：队首中签者，测试48h倒计时与签约存证"
            >
              <span>⭐ 李雷</span>
              <span class="text-[10px] text-amber-200 bg-amber-900/60 px-1 rounded">中签48h</span>
            </button>

            <button
              @click="fillAccount('310104199103033333', '123456', '韩梅梅')"
              class="px-2.5 py-1 rounded bg-emerald-700/80 hover:bg-emerald-600 active:scale-95 transition text-white border border-emerald-400/30 flex items-center gap-1"
              title="已有车位，剩余25天临期催缴"
            >
              <span>🚗 韩梅梅</span>
              <span class="text-[10px] text-emerald-200 bg-emerald-900/60 px-1 rounded">已有车位</span>
            </button>

            <button
              @click="fillAccount('310104198005055555', '123456', '王建国')"
              class="px-2.5 py-1 rounded bg-purple-700/80 hover:bg-purple-600 active:scale-95 transition text-white border border-purple-400/30 flex items-center gap-1"
              title="第三幕：物业大盘中台与强制代清退"
            >
              <span>🏢 王建国</span>
              <span class="text-[10px] text-purple-200 bg-purple-900/60 px-1 rounded">物业管理员</span>
            </button>

            <button
              @click="handleResetSeed"
              class="px-2 py-1 rounded bg-rose-700/80 hover:bg-rose-600 active:scale-95 transition text-white border border-rose-400/30 flex items-center gap-1 ml-1"
              title="重置所有演示数据至初始状态"
            >
              <span>🔄 重置演示</span>
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onUnmounted } from 'vue'
import api from '../api'

const emit = defineEmits(['fill'])

const visible = ref(false)
let hideTimer = null

const open = () => {
  if (hideTimer) clearTimeout(hideTimer)
  visible.value = true
}

const scheduleClose = () => {
  if (hideTimer) clearTimeout(hideTimer)
  hideTimer = setTimeout(() => {
    visible.value = false
  }, 200)
}

const toggle = () => {
  if (hideTimer) clearTimeout(hideTimer)
  visible.value = !visible.value
}

const fillAccount = (idCard, pwd, name) => {
  emit('fill', { identifier: idCard, password: pwd, name })
}

const handleResetSeed = async () => {
  if (!confirm('确定要将系统演示数据一键重置回初始状态吗？')) return
  try {
    const res = await api.post('/dev/reset-seed')
    alert(res.message || '重置成功！')
    window.location.reload()
  } catch (err) {
    alert('重置失败: ' + err.message)
  }
}

onUnmounted(() => {
  if (hideTimer) clearTimeout(hideTimer)
})
</script>
