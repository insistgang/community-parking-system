<template>
  <div class="min-h-screen bg-slate-100 flex flex-col pb-20">
    <NavBar />

    <main class="max-w-md w-full mx-auto p-4 space-y-4 flex-1">
      <div class="bg-gradient-to-r from-blue-900 to-indigo-900 rounded-2xl p-5 text-white shadow-md relative overflow-hidden">
        <div class="flex items-center gap-3">
          <div class="w-10 h-10 rounded-xl bg-white/10 backdrop-blur flex items-center justify-center text-xl shadow-inner">
            ⛓️
          </div>
          <div>
            <h2 class="text-base font-extrabold tracking-wide">个人区块链数字存证中心</h2>
            <p class="text-xs text-blue-200 mt-0.5">SHA-256 分布式哈希存证 · 基层治理不可篡改证据链</p>
          </div>
        </div>
      </div>

      <!-- 存证流水列表 -->
      <div class="space-y-3">
        <div class="flex items-center justify-between text-xs font-bold text-slate-700 px-1">
          <span>存证流水明细 (倒序排列)</span>
          <span class="text-[11px] text-slate-400 font-normal">点击单条查看防伪证书</span>
        </div>

        <div class="space-y-2.5">
          <div
            v-for="item in evidenceList"
            :key="item.evidence_id"
            @click="openDetail(item)"
            class="bg-white rounded-2xl p-4 shadow-sm border border-slate-200 hover:border-blue-400 cursor-pointer transition active:scale-[0.99]"
          >
            <div class="flex items-center justify-between mb-2">
              <span class="px-2 py-0.5 rounded-full text-[10px] font-bold bg-blue-50 text-blue-700 border border-blue-200">
                {{ item.event_type }}
              </span>
              <span class="font-mono text-xs font-bold text-indigo-600">
                #{{ item.block_height }}
              </span>
            </div>

            <p class="text-xs font-bold text-slate-800 leading-snug mb-2">
              {{ item.event_summary }}
            </p>

            <div class="bg-slate-50 p-2 rounded-xl text-[11px] font-mono text-slate-500 break-all mb-2 flex items-center justify-between">
              <span>{{ item.tx_hash.slice(0, 20) }}...{{ item.tx_hash.slice(-16) }}</span>
              <span class="text-blue-600 font-sans font-semibold text-[10px]">查看证书 ➔</span>
            </div>

            <div class="flex items-center justify-between text-[10px] text-slate-400">
              <span>编号: {{ item.evidence_id }}</span>
              <span>{{ item.created_at }}</span>
            </div>
          </div>

          <div v-if="evidenceList.length === 0" class="bg-white rounded-2xl p-8 text-center text-slate-400 text-xs shadow-sm">
            暂无上链存证记录
          </div>
        </div>
      </div>
    </main>

    <!-- 弹窗证书 -->
    <EvidenceModal
      :visible="modalVisible"
      :record="selectedRecord"
      @close="modalVisible = false"
    />

    <!-- 底部 TabBar -->
    <nav class="fixed bottom-0 inset-x-0 bg-white/95 backdrop-blur border-t border-slate-200 py-2 px-6 flex items-center justify-around text-xs z-40 max-w-md mx-auto shadow-lg">
      <router-link to="/" class="flex flex-col items-center gap-0.5 text-slate-400 hover:text-blue-600 font-medium transition">
        <svg xmlns="http://www.w3.org/2000/svg" class="w-5 h-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <path d="m3 9 9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"/><polyline points="9 22 9 12 15 12 15 22"/>
        </svg>
        <span class="text-[10px]">我的车位</span>
      </router-link>

      <router-link to="/queue" class="flex flex-col items-center gap-0.5 text-slate-400 hover:text-blue-600 font-medium transition">
        <svg xmlns="http://www.w3.org/2000/svg" class="w-5 h-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <line x1="8" y1="6" x2="21" y2="6"/><line x1="8" y1="12" x2="21" y2="12"/><line x1="8" y1="18" x2="21" y2="18"/><line x1="3" y1="6" x2="3.01" y2="6"/><line x1="3" y1="12" x2="3.01" y2="12"/><line x1="3" y1="18" x2="3.01" y2="18"/>
        </svg>
        <span class="text-[10px]">顺位公示</span>
      </router-link>

      <router-link to="/evidence" class="flex flex-col items-center gap-0.5 font-bold text-blue-600">
        <svg xmlns="http://www.w3.org/2000/svg" class="w-5 h-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10"/>
        </svg>
        <span class="text-[10px]">区块链存证</span>
      </router-link>
    </nav>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import api from '../api'
import NavBar from '../components/NavBar.vue'
import EvidenceModal from '../components/EvidenceModal.vue'

const evidenceList = ref([])
const modalVisible = ref(false)
const selectedRecord = ref({})

const loadEvidence = async () => {
  try {
    const res = await api.get('/evidence/list')
    if (res.code === 200) {
      evidenceList.value = res.data
    }
  } catch (err) {
    console.error('加载存证列表失败:', err)
  }
}

const openDetail = (item) => {
  selectedRecord.value = item
  modalVisible.value = true
}

onMounted(() => {
  loadEvidence()
})
</script>
