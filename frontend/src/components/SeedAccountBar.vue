<template>
  <div class="fixed top-0 inset-x-0 z-[70]">
    <div class="demo-hover-root" :class="{ 'is-open': pinned }">
      <!-- 热区始终占位，避免展开瞬间高度塌掉导致闪烁 -->
      <button
        type="button"
        class="demo-handle"
        aria-label="快捷账号"
        @click="toggle"
      />

      <div class="demo-panel">
        <div class="bg-gradient-to-r from-blue-900 via-indigo-900 to-slate-900 text-white px-3 py-2 text-xs shadow-lg demo-console border-b border-indigo-400/20">
          <div class="max-w-4xl mx-auto flex flex-wrap items-center justify-between gap-2">
            <div class="flex items-center gap-1.5 font-semibold text-blue-200">
              <span class="inline-block w-2 h-2 rounded-full bg-emerald-400"></span>
              <span>快捷账号 (一键免密填入):</span>
            </div>
            <div class="flex flex-wrap items-center gap-1.5">
              <button
                type="button"
                @click="fillAccount('310104199201011111', '123456', '张欣欣')"
                class="px-2.5 py-1 rounded bg-blue-700/80 hover:bg-blue-600 active:scale-95 transition text-white border border-blue-400/30 flex items-center gap-1"
                title="第一幕：未申请新客，测试外区房拦截与夫妻车"
              >
                <span>👤 张欣欣</span>
                <span class="text-[10px] text-blue-200 bg-blue-900/60 px-1 rounded">未申请</span>
              </button>

              <button
                type="button"
                @click="fillAccount('310104199002022222', '123456', '李雷')"
                class="px-2.5 py-1 rounded bg-amber-700/80 hover:bg-amber-600 active:scale-95 transition text-white border border-amber-400/30 flex items-center gap-1"
                title="第四幕：队首中签者，测试48h倒计时与签约存证"
              >
                <span>⭐ 李雷</span>
                <span class="text-[10px] text-amber-200 bg-amber-900/60 px-1 rounded">中签48h</span>
              </button>

              <button
                type="button"
                @click="fillAccount('310104199103033333', '123456', '韩梅梅')"
                class="px-2.5 py-1 rounded bg-emerald-700/80 hover:bg-emerald-600 active:scale-95 transition text-white border border-emerald-400/30 flex items-center gap-1"
                title="已有车位，剩余25天临期催缴"
              >
                <span>🚗 韩梅梅</span>
                <span class="text-[10px] text-emerald-200 bg-emerald-900/60 px-1 rounded">已有车位</span>
              </button>

              <button
                type="button"
                @click="fillAccount('310104198005055555', '123456', '王建国')"
                class="px-2.5 py-1 rounded bg-purple-700/80 hover:bg-purple-600 active:scale-95 transition text-white border border-purple-400/30 flex items-center gap-1"
                title="第三幕：物业大盘中台与强制代清退"
              >
                <span>🏢 王建国</span>
                <span class="text-[10px] text-purple-200 bg-purple-900/60 px-1 rounded">物业管理员</span>
              </button>

              <button
                type="button"
                @click="handleResetSeed"
                class="px-2 py-1 rounded bg-rose-700/80 hover:bg-rose-600 active:scale-95 transition text-white border border-rose-400/30 flex items-center gap-1 ml-1"
                title="重置所有演示数据至初始状态"
              >
                <span>🔄 重置数据</span>
              </button>
              <button
                type="button"
                class="md:hidden px-2 py-1 rounded bg-slate-700/80 text-white border border-slate-500/40"
                @click.stop="pinned = false"
              >
                收起
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import api from '../api'

const emit = defineEmits(['fill'])
const pinned = ref(false)

const isCoarsePointer = () => {
  if (typeof window === 'undefined') return false
  return window.matchMedia('(hover: none)').matches
}

const toggle = () => {
  // 电脑只用 CSS :hover，移出即收起；点按热区不会钉住
  if (!isCoarsePointer()) return
  pinned.value = !pinned.value
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
</script>

<style scoped>
.demo-handle {
  display: flex;
  width: 100%;
  height: 10px;
  align-items: flex-start;
  justify-content: center;
  background: transparent;
  border: 0;
  padding: 0;
  cursor: pointer;
}
.demo-handle::after {
  content: '';
  width: 2.75rem;
  height: 3px;
  margin-top: 3px;
  border-radius: 999px;
  background: rgb(99 102 241 / 0.55);
}
.demo-panel {
  max-height: 0;
  opacity: 0;
  overflow: hidden;
  pointer-events: none;
  transform: translateY(-6px);
  transition: max-height 0.2s ease, opacity 0.16s ease, transform 0.16s ease;
}
/* 整块作为悬停区域：热区不卸载，展开后鼠标仍在同一容器内 */
.demo-hover-root:hover .demo-panel,
.demo-hover-root.is-open .demo-panel {
  max-height: 220px;
  opacity: 1;
  pointer-events: auto;
  transform: translateY(0);
}
.demo-hover-root:hover .demo-handle::after,
.demo-hover-root.is-open .demo-handle::after {
  opacity: 0;
}

@media (hover: none) {
  .demo-handle {
    height: 14px;
  }
}
</style>
