<template>
  <div ref="containerRef" class="relative select-none text-xs w-full" :class="customClass">
    <!-- 触发器按钮 -->
    <button
      type="button"
      ref="triggerRef"
      @click="toggleOpen"
      :disabled="disabled"
      class="w-full flex items-center justify-between gap-2 px-3 py-2 rounded-lg border transition-all duration-150 text-left outline-none"
      :class="[
        dark
          ? 'bg-slate-900 border-slate-700 text-slate-100 hover:border-slate-500 focus:border-blue-500'
          : 'bg-white border-slate-200 text-slate-800 hover:border-slate-400 focus:border-blue-500',
        isOpen ? (dark ? 'border-blue-500 ring-1 ring-blue-500' : 'border-blue-500 ring-1 ring-blue-500') : '',
        disabled ? 'opacity-50 cursor-not-allowed' : 'cursor-pointer',
        btnClass
      ]"
    >
      <span class="truncate block font-medium">
        {{ currentLabel || placeholder || '请选择...' }}
      </span>
      <svg
        class="w-4 h-4 shrink-0 transition-transform duration-200 text-slate-400"
        :class="{ 'rotate-180 text-blue-400': isOpen }"
        fill="none"
        stroke="currentColor"
        viewBox="0 0 24 24"
      >
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7" />
      </svg>
    </button>

    <!-- 下拉弹出菜单 (纯 DOM 渲染，绝不触发操作系统的脱靶弹窗) -->
    <transition
      enter-active-class="transition duration-150 ease-out"
      enter-from-class="transform opacity-0 scale-95"
      enter-to-class="transform opacity-100 scale-100"
      leave-active-class="transition duration-100 ease-in"
      leave-from-class="transform opacity-100 scale-100"
      leave-to-class="transform opacity-0 scale-95"
    >
      <div
        v-if="isOpen"
        class="absolute left-0 right-0 z-50 rounded-xl shadow-2xl border overflow-hidden"
        :class="[
          openUpward ? 'bottom-full mb-1.5 origin-bottom' : 'top-full mt-1.5 origin-top',
          dark
            ? 'bg-slate-900 border-slate-700 text-slate-200 shadow-black/80 divide-y divide-slate-800/80'
            : 'bg-white border-slate-200 text-slate-800 shadow-slate-400/40 divide-y divide-slate-100',
          dropdownClass
        ]"
      >
        <div class="max-h-56 overflow-y-auto py-1 custom-scrollbar">
          <button
            v-for="(opt, idx) in normalizedOptions"
            :key="opt.value ?? idx"
            type="button"
            @click="selectOption(opt)"
            :disabled="opt.disabled"
            class="w-full text-left px-3 py-2 flex items-center justify-between gap-2 text-xs transition-colors duration-100"
            :class="[
              opt.disabled
                ? 'opacity-40 cursor-not-allowed'
                : (dark ? 'hover:bg-slate-800 hover:text-white cursor-pointer' : 'hover:bg-slate-100 cursor-pointer'),
              isSelected(opt)
                ? (dark ? 'bg-blue-600/20 text-blue-400 font-semibold' : 'bg-blue-50 text-blue-600 font-semibold')
                : ''
            ]"
          >
            <div class="truncate flex items-center gap-1.5 flex-1">
              <span v-if="opt.icon">{{ opt.icon }}</span>
              <span class="truncate">{{ opt.label }}</span>
              <span v-if="opt.sublabel" class="text-[10px] text-slate-400 font-mono ml-1">{{ opt.sublabel }}</span>
            </div>
            <svg
              v-if="isSelected(opt)"
              class="w-4 h-4 shrink-0 text-blue-400"
              fill="none"
              stroke="currentColor"
              viewBox="0 0 24 24"
            >
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2.5" d="M5 13l4 4L19 7" />
            </svg>
          </button>
        </div>
      </div>
    </transition>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'

const props = defineProps({
  modelValue: {
    type: [String, Number, Boolean, null],
    default: null
  },
  options: {
    type: Array,
    default: () => []
  },
  placeholder: {
    type: String,
    default: '请选择...'
  },
  disabled: {
    type: Boolean,
    default: false
  },
  dark: {
    type: Boolean,
    default: true
  },
  customClass: {
    type: String,
    default: ''
  },
  btnClass: {
    type: String,
    default: ''
  },
  dropdownClass: {
    type: String,
    default: ''
  }
})

const emit = defineEmits(['update:modelValue', 'change'])

const containerRef = ref(null)
const triggerRef = ref(null)
const isOpen = ref(false)
const openUpward = ref(false)

const normalizedOptions = computed(() => {
  return props.options.map((opt) => {
    if (typeof opt === 'object' && opt !== null) {
      return {
        label: opt.label ?? opt.name ?? String(opt.value),
        value: opt.value,
        disabled: !!opt.disabled,
        icon: opt.icon,
        sublabel: opt.sublabel
      }
    }
    return {
      label: String(opt),
      value: opt,
      disabled: false
    }
  })
})

const currentLabel = computed(() => {
  const match = normalizedOptions.value.find((opt) => opt.value === props.modelValue)
  return match ? match.label : ''
})

const isSelected = (opt) => {
  return props.modelValue === opt.value
}

const toggleOpen = () => {
  if (props.disabled) return
  if (!isOpen.value && triggerRef.value) {
    const rect = triggerRef.value.getBoundingClientRect()
    const spaceBelow = window.innerHeight - rect.bottom
    openUpward.value = spaceBelow < 220 && rect.top > 220
  }
  isOpen.value = !isOpen.value
}

const selectOption = (opt) => {
  if (opt.disabled) return
  emit('update:modelValue', opt.value)
  emit('change', opt.value)
  isOpen.value = false
}

const handleDocClick = (e) => {
  if (containerRef.value && !containerRef.value.contains(e.target)) {
    isOpen.value = false
  }
}

const handleKeyDown = (e) => {
  if (e.key === 'Escape' && isOpen.value) {
    isOpen.value = false
  }
}

onMounted(() => {
  document.addEventListener('pointerdown', handleDocClick)
  document.addEventListener('keydown', handleKeyDown)
})

onUnmounted(() => {
  document.removeEventListener('pointerdown', handleDocClick)
  document.removeEventListener('keydown', handleKeyDown)
})
</script>

<style scoped>
.custom-scrollbar::-webkit-scrollbar {
  width: 4px;
}
.custom-scrollbar::-webkit-scrollbar-thumb {
  background: rgba(148, 163, 184, 0.3);
  border-radius: 9999px;
}
.custom-scrollbar::-webkit-scrollbar-thumb:hover {
  background: rgba(148, 163, 184, 0.5);
}
</style>
