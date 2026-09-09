import { reactive } from 'vue'

export const dialogState = reactive({
  visible: false,
  title: '',
  content: '',
  type: 'info', // 'danger' | 'warning' | 'info' | 'success'
  confirmText: '确定',
  cancelText: '取消',
  showCancel: true,
  resolve: null
})

export const toastState = reactive({
  toasts: []
})

let toastCounter = 0

/**
 * 弹出内部美化 Toast 提示（替代原生 alert / 简短反馈）
 */
export function showToast(message, type = 'info', duration = 2800) {
  const id = ++toastCounter
  toastState.toasts.push({ id, message, type })
  setTimeout(() => {
    const idx = toastState.toasts.findIndex(t => t.id === id)
    if (idx !== -1) toastState.toasts.splice(idx, 1)
  }, duration)
}

/**
 * 弹出内部美化 Confirm 确认弹窗（替代原生 window.confirm）
 * @returns {Promise<boolean>} 用户点击确定返回 true，取消返回 false
 */
export function showConfirm(options) {
  return new Promise((resolve) => {
    if (typeof options === 'string') {
      dialogState.content = options
      dialogState.title = '操作确认'
      dialogState.type = (options.includes('清退') || options.includes('重置') || options.includes('警告') || options.includes('放弃') || options.includes('退租')) ? 'danger' : 'info'
      dialogState.confirmText = options.includes('清退') ? '确认清退' : (options.includes('重置') ? '确认重置' : '确定')
      dialogState.cancelText = '取消'
    } else {
      dialogState.title = options.title || '操作确认'
      dialogState.content = options.content || ''
      dialogState.type = options.type || 'info'
      dialogState.confirmText = options.confirmText || '确定'
      dialogState.cancelText = options.cancelText || '取消'
    }
    dialogState.showCancel = true
    dialogState.visible = true
    dialogState.resolve = resolve
  })
}

/**
 * 弹出内部美化 Alert 弹窗（替代原生 window.alert）
 * @returns {Promise<void>}
 */
export function showAlert(options, type = 'info') {
  return new Promise((resolve) => {
    if (typeof options === 'string') {
      dialogState.content = options
      dialogState.title = '系统提示'
      dialogState.type = (options.includes('失败') || options.includes('错误') || options.includes('警告')) ? 'danger' : type
      dialogState.confirmText = '我知道了'
    } else {
      dialogState.title = options.title || '系统提示'
      dialogState.content = options.content || ''
      dialogState.type = options.type || type
      dialogState.confirmText = options.confirmText || '我知道了'
    }
    dialogState.showCancel = false
    dialogState.visible = true
    dialogState.resolve = resolve
  })
}

export function handleDialogConfirm() {
  dialogState.visible = false
  if (dialogState.resolve) {
    dialogState.resolve(true)
    dialogState.resolve = null
  }
}

export function handleDialogCancel() {
  dialogState.visible = false
  if (dialogState.resolve) {
    dialogState.resolve(false)
    dialogState.resolve = null
  }
}

export function removeToast(id) {
  const idx = toastState.toasts.findIndex(t => t.id === id)
  if (idx !== -1) toastState.toasts.splice(idx, 1)
}

// 自动挂载至全局 window，并优雅拦截原生 alert
if (typeof window !== 'undefined') {
  window.$confirm = showConfirm
  window.$alert = showAlert
  window.$toast = showToast
  // 彻底阻断原生丑陋浏览器弹窗，无缝自动路由至内部美化 Toast
  window.alert = (msg) => {
    const text = String(msg)
    const isError = text.includes('失败') || text.includes('错误') || text.includes('禁止') || text.includes('警告')
    showToast(text, isError ? 'error' : 'success')
  }
}
