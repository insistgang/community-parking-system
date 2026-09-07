<template>
  <!-- 底部固定导航栏 (业主 H5 全局 TabBar) -->
  <nav class="fixed bottom-0 inset-x-0 bg-white/95 backdrop-blur border-t border-slate-200 py-2 px-6 flex items-center justify-around text-xs z-40 max-w-md mx-auto shadow-lg">
    <router-link to="/" class="flex flex-col items-center gap-0.5 font-bold transition" :class="$route.path === '/' ? 'text-blue-600' : 'text-slate-400 hover:text-blue-600 font-medium'">
      <svg xmlns="http://www.w3.org/2000/svg" class="w-5 h-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
        <path d="m3 9 9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"/><polyline points="9 22 9 12 15 12 15 22"/>
      </svg>
      <span class="text-[10px]">我的车位</span>
    </router-link>

    <router-link to="/queue" class="flex flex-col items-center gap-0.5 transition" :class="$route.path === '/queue' ? 'text-blue-600 font-bold' : 'text-slate-400 hover:text-blue-600 font-medium'">
      <svg xmlns="http://www.w3.org/2000/svg" class="w-5 h-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
        <line x1="8" y1="6" x2="21" y2="6"/><line x1="8" y1="12" x2="21" y2="12"/><line x1="8" y1="18" x2="21" y2="18"/><line x1="3" y1="6" x2="3.01" y2="6"/><line x1="3" y1="12" x2="3.01" y2="12"/><line x1="3" y1="18" x2="3.01" y2="18"/>
      </svg>
      <span class="text-[10px]">顺位公示</span>
    </router-link>

    <router-link to="/evidence" class="flex flex-col items-center gap-0.5 transition" :class="$route.path === '/evidence' ? 'text-blue-600 font-bold' : 'text-slate-400 hover:text-blue-600 font-medium'">
      <svg xmlns="http://www.w3.org/2000/svg" class="w-5 h-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
        <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10"/>
      </svg>
      <span class="text-[10px]">区块链存证</span>
    </router-link>

    <router-link v-if="userStore.isPropertyAdmin" to="/property/dashboard" class="flex flex-col items-center gap-0.5 text-purple-600 hover:text-purple-700 font-bold transition">
      <span class="text-base leading-none">🏢</span>
      <span class="text-[10px]">物业中台</span>
    </router-link>
  </nav>
</template>

<script setup>
import { useUserStore } from '../store/user'
const userStore = useUserStore()
</script>
