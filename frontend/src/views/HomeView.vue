<template>
  <div class="min-h-screen bg-slate-100 flex flex-col pb-20">
    <!-- 顶部导航栏 -->
    <NavBar />

    <main class="max-w-md w-full mx-auto p-4 space-y-4 flex-1">
      <!-- 物业管理员身份专属巡检横幅 (物业人员返回前台时显示) -->
      <div v-if="userStore.isPropertyAdmin" class="bg-gradient-to-r from-purple-900 via-indigo-900 to-slate-900 text-white rounded-2xl p-3.5 shadow-md border border-purple-500/30 flex items-center justify-between gap-3 animate-fade-in">
        <div class="flex items-center gap-2.5 min-w-0">
          <span class="text-2xl">🏢</span>
          <div class="min-w-0">
            <span class="text-xs font-bold text-purple-200 block truncate">物业管理巡检视角 · {{ userStore.realName }}</span>
            <span class="text-[10px] text-slate-300 block">当前正在以管理员身份浏览前台大盘与公示</span>
          </div>
        </div>
        <router-link
          to="/property/dashboard"
          class="px-3 py-1.5 rounded-xl bg-purple-600 hover:bg-purple-500 text-white font-bold text-xs shrink-0 shadow transition active:scale-95 flex items-center gap-1"
        >
          <span>回中台 ➔</span>
        </router-link>
      </div>

      <!-- 视图 D: 队首中签 48h 确认倒计时卡片 -->
      <div v-if="viewState === 'ALLOCATED' || viewState === 'OFFLINE_PENDING'">
        <CountdownCard
          :spaceNo="myQueue?.allocated_space_no || 'A-012'"
          :initialSeconds="myQueue?.countdown_seconds ?? 172800"
          :isOfflinePending="viewState === 'OFFLINE_PENDING'"
          @refresh="loadData"
        />
      </div>

      <!-- 视图 A: 已有车位视图 (仅展示当前业主本人的专属车位信息) -->
      <div v-else-if="viewState === 'RENTED' && mySpace" class="bg-gradient-to-br from-blue-700 via-blue-800 to-indigo-900 text-white rounded-2xl p-5 shadow-lg relative overflow-hidden">
        <div class="flex items-center justify-between mb-4">
          <div class="flex items-center gap-2">
            <span class="p-2 rounded-xl bg-white/10 text-emerald-300">
              <svg xmlns="http://www.w3.org/2000/svg" class="w-5 h-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M19 17h2c.6 0 1-.4 1-1v-3c0-.9-.7-1.7-1.5-1.9C18.7 10.6 16 10 16 10s-1.3-1.4-2.2-2.3c-.5-.4-1.1-.7-1.8-.7H5c-.6 0-1.1.4-1.4.9l-1.5 2.8C2.1 11 2 11.5 2 12v4c0 .6.4 1 1 1h2"/><circle cx="7" cy="17" r="2"/><circle cx="17" cy="17" r="2"/>
              </svg>
            </span>
            <div>
              <span class="text-xs text-blue-200">您名下的承租车位</span>
              <h2 class="text-2xl font-black tracking-wide">{{ mySpace.space_no }}</h2>
            </div>
          </div>
          <span class="px-2.5 py-1 rounded-full text-xs font-semibold" :class="mySpace.days_left <= 30 ? 'bg-amber-500/30 text-amber-200 border border-amber-400/40' : 'bg-emerald-500/30 text-emerald-200 border border-emerald-400/40'">
            {{ mySpace.days_left <= 30 ? '临期待续' : '生效中' }}
          </span>
        </div>

        <div class="grid grid-cols-2 gap-3 bg-black/20 backdrop-blur rounded-xl p-3 text-xs mb-4">
          <div>
            <span class="text-blue-200 block text-[11px]">绑定车辆车牌</span>
            <span class="font-bold text-sm tracking-wider">{{ mySpace.plate_no }}</span>
          </div>
          <div>
            <span class="text-blue-200 block text-[11px]">租期剩余</span>
            <span class="font-bold text-sm" :class="mySpace.days_left <= 30 ? 'text-amber-300' : 'text-emerald-300'">
              {{ mySpace.days_left }} 天
            </span>
          </div>
          <div class="col-span-2 pt-1 border-t border-white/10 text-[11px] text-blue-200">
            合同有效至: {{ mySpace.end_date }}
          </div>
        </div>

        <div class="flex items-center gap-2">
          <button
            v-if="mySpace.days_left <= 30"
            @click="handleRenew"
            class="flex-1 py-2.5 rounded-xl bg-emerald-500 hover:bg-emerald-600 text-white font-bold text-xs shadow-md transition"
          >
            申请续租 (延期1年)
          </button>
          <button
            @click="handleTerminate"
            class="px-3 py-2.5 rounded-xl bg-white/10 hover:bg-white/20 text-blue-100 text-xs font-semibold transition border border-white/20"
          >
            申请退租
          </button>
        </div>
      </div>

      <!-- 视图 C: 排队轮候中视图 (严格仅展示本人的排位进度与AI预估) -->
      <div v-else-if="viewState === 'WAITING' && myQueue" class="space-y-3">
        <!-- 核心排位卡片 -->
        <div
          @click="$router.push('/queue')"
          class="bg-white rounded-2xl p-5 shadow-sm border border-slate-200 cursor-pointer hover:border-blue-400 transition relative overflow-hidden"
        >
          <div class="flex items-center justify-between mb-3">
            <span class="text-xs font-bold text-slate-500 flex items-center gap-1.5">
              <span class="w-2 h-2 rounded-full bg-blue-600 animate-pulse"></span>
              <span>您的车位排队轮候状态</span>
            </span>
          </div>

          <div class="flex items-baseline gap-3 my-2">
            <div>
              <span class="text-xs text-slate-400 block">我的实时排位</span>
              <div class="flex items-baseline gap-1">
                <span class="text-4xl font-black text-blue-600 tracking-tight">{{ myQueue.current_rank }}</span>
                <span class="text-xs text-slate-400">/ 共 {{ myQueue.total_queue }} 人排队</span>
              </div>
            </div>

            <div class="ml-auto text-right">
              <span class="px-2.5 py-1 rounded-full text-xs font-bold" :class="myQueue.priority_level === 1 ? 'bg-blue-50 text-blue-700 border border-blue-200' : 'bg-slate-100 text-slate-600'">
                {{ myQueue.priority_label }}
              </span>
              <span class="text-[11px] text-slate-400 block mt-1">
                前面还有 {{ Math.max(0, myQueue.current_rank - 1) }} 人
              </span>
            </div>
          </div>

          <!-- 候补首位特殊提示 -->
          <div v-if="myQueue.current_rank === 1" class="mt-3 p-2.5 rounded-xl bg-amber-50 border border-amber-200 text-amber-800 text-xs font-medium flex items-center gap-2">
            <span class="text-base">⭐</span>
            <span>您已升至候补首位！一旦有空闲车位释放，系统将第一时间为您锁定并推送 48h 确认通知！</span>
          </div>

          <div class="mt-3 pt-3 border-t border-slate-100 flex items-center justify-between text-xs text-slate-400">
            <span>绑定车辆: {{ myQueue.plate_no }}</span>
            <span>申报时间: {{ myQueue.apply_time }}</span>
          </div>
        </div>

        <!-- AI 大模型预估等待时长卡片 -->
        <AiWaitingCard
          :durationText="myQueue.ai_waiting_time"
          :basisText="myQueue.ai_estimation_basis"
        />

        <!-- 撤销申请按钮 -->
        <div class="pt-1">
          <button
            @click="handleCancelApply"
            class="w-full py-2.5 rounded-xl bg-white hover:bg-rose-50 text-rose-600 font-semibold text-xs border border-rose-200 shadow-sm transition active:scale-[0.99] flex items-center justify-center gap-1.5"
          >
            <svg xmlns="http://www.w3.org/2000/svg" class="w-4 h-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <circle cx="12" cy="12" r="10"/><path d="m15 9-6 6"/><path d="m9 9 6 6"/>
            </svg>
            <span>撤销车位申请 (放弃当前排位)</span>
          </button>
        </div>
      </div>

      <!-- 视图 B: 新客无车位视图 / 物业管理员巡检视图 -->
      <div v-else class="bg-white rounded-2xl p-6 shadow-sm border border-slate-200 text-center space-y-4">
        <div class="w-16 h-16 rounded-full flex items-center justify-center mx-auto shadow-inner" :class="userStore.isPropertyAdmin ? 'bg-purple-50 text-purple-600' : 'bg-blue-50 text-blue-600'">
          <span v-if="userStore.isPropertyAdmin" class="text-3xl">🏢</span>
          <svg v-else xmlns="http://www.w3.org/2000/svg" class="w-8 h-8" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8">
            <rect width="18" height="18" x="3" y="3" rx="2"/><path d="M9 17V7h4a3 3 0 0 1 0 6H9"/>
          </svg>
        </div>
        <div v-if="userStore.isPropertyAdmin">
          <h3 class="text-base font-black text-slate-800">物业管理员前台巡检模式</h3>
          <p class="text-xs text-slate-500 mt-1 leading-relaxed max-w-xs mx-auto">
            当前登录身份：【{{ userStore.realName }}】(物业管理员)。您可在此查验车位公示大盘、顺位全量脱敏队列与存证链，随时可切回调度中台。
          </p>
        </div>
        <div v-else>
          <h3 class="text-base font-black text-slate-800">尚未在本小区申请车位</h3>
          <p class="text-xs text-slate-500 mt-1 leading-relaxed max-w-xs mx-auto">
            老旧小区车位资源紧张，系统实行“三证合一严审、按序阳光轮候、全流程存证”的公平管理机制。
          </p>
        </div>

        <router-link
          v-if="userStore.isPropertyAdmin"
          to="/property/dashboard"
          class="inline-flex items-center justify-center gap-2 w-full py-3 rounded-xl bg-purple-600 hover:bg-purple-700 text-white font-bold text-sm shadow-md shadow-purple-500/20 active:scale-[0.98] transition"
        >
          <span>🏢 进入物业资产调度中台</span>
          <svg xmlns="http://www.w3.org/2000/svg" class="w-4 h-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
            <path d="m9 18 6-6-6-6"/>
          </svg>
        </router-link>
        <router-link
          v-else
          to="/apply"
          class="inline-flex items-center justify-center gap-2 w-full py-3 rounded-xl bg-blue-600 hover:bg-blue-700 text-white font-bold text-sm shadow-md shadow-blue-500/20 active:scale-[0.98] transition"
        >
          <span>立即申请排位 (三证合一认证)</span>
          <svg xmlns="http://www.w3.org/2000/svg" class="w-4 h-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
            <path d="m9 18 6-6-6-6"/>
          </svg>
        </router-link>
      </div>

      <!-- 小区车位宏观总览指标 (仅展示宏观统计数据，绝不暴露邻居姓名或门牌隐私) -->
      <div class="bg-white rounded-2xl p-4 shadow-sm border border-slate-200">
        <div class="flex items-center justify-between mb-3">
          <h3 class="text-xs font-bold text-slate-800 flex items-center gap-1.5">
            <span class="w-1.5 h-3.5 bg-blue-600 rounded-full"></span>
            <span>小区车位宏观概况</span>
          </h3>
          <span class="text-[11px] text-slate-400">总车位 {{ overview?.total_spaces ?? 50 }} 个</span>
        </div>

        <div class="grid grid-cols-4 gap-2 text-center text-xs">
          <div class="bg-slate-50 p-2.5 rounded-xl border border-slate-100">
            <span class="text-slate-400 text-[10px] block">已租车位</span>
            <span class="text-base font-extrabold text-slate-800 mt-0.5 block">{{ overview?.rented_spaces ?? 46 }}</span>
          </div>
          <div class="bg-emerald-50 p-2.5 rounded-xl border border-emerald-100">
            <span class="text-emerald-600 text-[10px] block font-medium">空闲可用</span>
            <span class="text-base font-extrabold text-emerald-700 mt-0.5 block">{{ overview?.vacant_spaces ?? 0 }}</span>
          </div>
          <div class="bg-amber-50 p-2.5 rounded-xl border border-amber-100">
            <span class="text-amber-600 text-[10px] block font-medium">中签待签</span>
            <span class="text-base font-extrabold text-amber-700 mt-0.5 block">{{ overview?.allocated_spaces ?? 0 }}</span>
          </div>
          <div class="bg-orange-50 p-2.5 rounded-xl border border-orange-100">
            <span class="text-orange-600 text-[10px] block font-medium">维修停用</span>
            <span class="text-base font-extrabold text-orange-700 mt-0.5 block">{{ overview?.maintenance_spaces ?? 1 }}</span>
          </div>
        </div>

        <div class="mt-3 pt-2.5 border-t border-slate-100 flex items-center justify-between text-[11px] text-slate-400">
          <span>🔒 业主隐私安全：个人车位信息互不可见</span>
          <router-link to="/queue" class="text-blue-600 font-semibold hover:underline">
            查看排队情况 ➔
          </router-link>
        </div>
      </div>
    </main>

    <BottomNav />
  </div>
</template>

<script setup>
import { computed, onMounted, onUnmounted } from 'vue'
import { useUserStore } from '../store/user'
import api from '../api'
import NavBar from '../components/NavBar.vue'
import CountdownCard from '../components/CountdownCard.vue'
import AiWaitingCard from '../components/AiWaitingCard.vue'
import BottomNav from '../components/BottomNav.vue'

const userStore = useUserStore()

let pollTimer = null

const viewState = computed(() => userStore.homeData?.view_state || 'NO_APPLY')
const mySpace = computed(() => userStore.homeData?.my_space)
const myQueue = computed(() => userStore.homeData?.my_queue)
const overview = computed(() => userStore.homeData?.community_overview)

const loadData = async () => {
  await userStore.fetchHomeData()
}

onMounted(() => {
  loadData()
  // 每 3 秒轻量轮询，实现物业端清退时，业主端排位秒级跳变联动
  pollTimer = setInterval(() => {
    loadData()
  }, 3000)
})

onUnmounted(() => {
  if (pollTimer) clearInterval(pollTimer)
})

const handleCancelApply = async () => {
  if (!confirm('警告：撤销排队申请后，您当前的顺位将立即释放给后续业主，重新申请需重新核验并排至队尾。确认撤销吗？')) return
  try {
    const res = await api.post('/parking/cancel-apply', { reason: '自愿放弃申请' })
    alert(res.message || '申请已撤销')
    await loadData()
  } catch (err) {
    alert('撤销失败: ' + err.message)
  }
}

const handleRenew = async () => {
  if (!confirm('确认为当前车位申请续租 1 年吗？续租成功后租约自动顺延并生成区块链存证。')) return
  try {
    const res = await api.post('/parking/renew')
    alert(res.message || '续租成功')
    await loadData()
  } catch (err) {
    alert('续租失败: ' + err.message)
  }
}

const handleTerminate = async () => {
  if (!confirm('确认申请退租吗？车位将立即释放并由系统自动顺延分配给排队业主，操作实时上链不可撤销。')) return
  try {
    const res = await api.post('/parking/terminate-rental')
    alert(res.message || '退租成功')
    await loadData()
  } catch (err) {
    alert('退租失败: ' + err.message)
  }
}
</script>
