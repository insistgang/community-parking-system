<template>
  <nav aria-label="主导航" class="fixed bottom-0 inset-x-0 z-40 mx-auto flex max-w-md items-center justify-around border-t border-slate-200 bg-white/95 px-3 pt-1.5 pb-[max(0.5rem,env(safe-area-inset-bottom))] backdrop-blur shadow-lg">
    <router-link v-for="item in items" :key="item.to" :to="item.to"
      :aria-current="isActive(item) ? 'page' : undefined"
      class="flex min-h-[54px] min-w-[72px] flex-col items-center justify-center gap-1 rounded-xl px-2 text-[11px] transition focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-blue-500"
      :class="isActive(item) ? 'text-blue-600 font-bold' : 'text-slate-500 font-medium hover:text-blue-600'">
      <span class="flex h-7 w-12 items-center justify-center rounded-full transition-colors"
        :class="isActive(item) ? 'bg-blue-50' : ''">
        <AppIcon :name="item.icon" :size="21" :stroke-width="isActive(item) ? 2 : 1.8" />
      </span>
      <span>{{ item.label }}</span>
    </router-link>
  </nav>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { useUserStore } from '../store/user'
import AppIcon from './AppIcon.vue'

const route = useRoute()
const userStore = useUserStore()
const items = computed(() => [
  { to: '/', label: '我的车辆', icon: 'car' },
  { to: '/queue', label: '顺位公示', icon: 'queue' },
  { to: '/evidence', label: '审计记录', icon: 'shield' },
  ...(userStore.isPropertyAdmin ? [{ to: '/property/dashboard', label: '物业管理', icon: 'building' }] : []),
])
const isActive = (item) => route.path === item.to || (item.to === '/' && route.path === '/apply')
</script>
