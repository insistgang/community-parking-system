<template>
  <teleport to="body">
    <!-- ================= 1. 全局顶部浮动 Toast 消息通知条 ================= -->
    <div
      v-if="toastState.toasts.length > 0"
      class="fixed top-5 left-1/2 -translate-x-1/2 z-[100000] flex flex-col items-center gap-2 pointer-events-none w-full max-w-sm px-4"
    >
      <div
        v-for="t in toastState.toasts"
        :key="t.id"
        class="pointer-events-auto w-full px-4 py-3 rounded-2xl shadow-xl border flex items-center justify-between gap-3 text-xs font-bold transition-all duration-300 animate-slide-down backdrop-blur-md"
        :class="getToastClass(t.type)"
      >
        <div class="flex items-center gap-2.5 min-w-0">
          <span class="text-base leading-none shrink-0">{{ getToastIcon(t.type) }}</span>
          <span class="truncate leading-relaxed">{{ t.message }}</span>
        </div>
        <button
          @click="removeToast(t.id)"
          class="shrink-0 opacity-70 hover:opacity-100 transition p-1 text-sm leading-none"
        >
          ✕
        </button>
      </div>
    </div>

    <!-- ================= 2. 全局现代拟真模态确认弹窗 (替代丑陋原生 confirm/alert) ================= -->
    <transition name="dialog-fade">
      <div
        v-if="dialogState.visible"
        class="fixed inset-0 z-[99999] flex items-center justify-center p-4 bg-black/60 backdrop-blur-sm"
        @click.self="handleDialogCancel"
      >
        <div
          class="bg-white dark:bg-slate-900 text-slate-800 dark:text-slate-100 rounded-3xl max-w-sm w-full p-6 shadow-2xl border border-slate-200 dark:border-slate-800 space-y-4 animate-scale-up"
        >
          <!-- 图标徽章 -->
          <div class="flex items-center gap-3">
            <div
              class="w-12 h-12 rounded-2xl flex items-center justify-center text-xl shrink-0 shadow-inner"
              :class="getBadgeClass(dialogState.type)"
            >
              {{ getBadgeIcon(dialogState.type) }}
            </div>
            <div>
              <h3 class="text-base font-black text-slate-900 dark:text-white">
                {{ dialogState.title }}
              </h3>
              <span class="text-[10px] text-slate-400">慧停管家 · 官方综合管理系统</span>
            </div>
          </div>

          <!-- 内容正文 -->
          <div class="text-xs text-slate-600 dark:text-slate-300 leading-relaxed py-1 whitespace-pre-line">
            {{ dialogState.content }}
          </div>

          <!-- 底部操作按钮 -->
          <div class="flex items-center justify-end gap-2.5 pt-2">
            <button
              v-if="dialogState.showCancel"
              type="button"
              @click="handleDialogCancel"
              class="px-4 py-2.5 rounded-xl font-bold text-xs bg-slate-100 hover:bg-slate-200 dark:bg-slate-800 dark:hover:bg-slate-700 text-slate-600 dark:text-slate-300 transition active:scale-95"
            >
              {{ dialogState.cancelText }}
            </button>
            <button
              type="button"
              @click="handleDialogConfirm"
              class="px-5 py-2.5 rounded-xl font-bold text-xs text-white shadow-md transition active:scale-95 flex items-center gap-1.5"
              :class="getConfirmBtnClass(dialogState.type)"
            >
              <span>{{ dialogState.confirmText }}</span>
            </button>
          </div>
        </div>
      </div>
    </transition>
  </teleport>
</template>

<script setup>
import {
  dialogState,
  toastState,
  handleDialogConfirm,
  handleDialogCancel,
  removeToast
} from '../utils/dialog'

const getToastClass = (type) => {
  switch (type) {
    case 'success':
      return 'bg-emerald-600/95 text-white border-emerald-500 shadow-emerald-900/20'
    case 'error':
      return 'bg-rose-600/95 text-white border-rose-500 shadow-rose-900/20'
    case 'warning':
      return 'bg-amber-600/95 text-white border-amber-500 shadow-amber-900/20'
    default:
      return 'bg-slate-900/95 text-white border-slate-700 shadow-black/30'
  }
}

const getToastIcon = (type) => {
  switch (type) {
    case 'success': return '✓'
    case 'error': return '✕'
    case 'warning': return '⚠️'
    default: return 'ℹ️'
  }
}

const getBadgeClass = (type) => {
  switch (type) {
    case 'danger':
      return 'bg-rose-100 dark:bg-rose-950/70 text-rose-600 border border-rose-200 dark:border-rose-800'
    case 'warning':
      return 'bg-amber-100 dark:bg-amber-950/70 text-amber-600 border border-amber-200 dark:border-amber-800'
    case 'success':
      return 'bg-emerald-100 dark:bg-emerald-950/70 text-emerald-600 border border-emerald-200 dark:border-emerald-800'
    default:
      return 'bg-blue-100 dark:bg-blue-950/70 text-blue-600 border border-blue-200 dark:border-blue-800'
  }
}

const getBadgeIcon = (type) => {
  switch (type) {
    case 'danger': return '⚡'
    case 'warning': return '⚠️'
    case 'success': return '✓'
    default: return '🛡️'
  }
}

const getConfirmBtnClass = (type) => {
  switch (type) {
    case 'danger':
      return 'bg-rose-600 hover:bg-rose-700 shadow-rose-600/30'
    case 'warning':
      return 'bg-amber-600 hover:bg-amber-700 shadow-amber-600/30'
    case 'success':
      return 'bg-emerald-600 hover:bg-emerald-700 shadow-emerald-600/30'
    default:
      return 'bg-blue-600 hover:bg-blue-700 shadow-blue-600/30'
  }
}
</script>

<style scoped>
.dialog-fade-enter-active,
.dialog-fade-leave-active {
  transition: opacity 0.2s ease;
}
.dialog-fade-enter-from,
.dialog-fade-leave-to {
  opacity: 0;
}

@keyframes slideDown {
  from {
    opacity: 0;
    transform: translateY(-20px) scale(0.95);
  }
  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

@keyframes scaleUp {
  from {
    opacity: 0;
    transform: scale(0.92);
  }
  to {
    opacity: 1;
    transform: scale(1);
  }
}

.animate-slide-down {
  animation: slideDown 0.25s cubic-bezier(0.16, 1, 0.3, 1) forwards;
}

.animate-scale-up {
  animation: scaleUp 0.22s cubic-bezier(0.16, 1, 0.3, 1) forwards;
}
</style>
