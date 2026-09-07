<template>
  <div class="min-h-screen bg-slate-900 text-slate-100 flex flex-col property-dashboard" :class="{ 'light-mode': userStore.propertyTheme === 'light' }">
    <!-- 顶部演示控制条 (随时一键免密切换任意角色，绝不迷路) -->
    <SeedAccountBar @fill="handleFastSwitch" />

    <!-- 顶部导航栏 -->
    <header class="bg-slate-800/90 backdrop-blur border-b border-slate-700 px-4 sm:px-6 py-3 flex flex-wrap items-center justify-between gap-x-3 gap-y-2 sticky top-0 z-30 shadow-md">
      <div class="flex items-center gap-3 min-w-0">
        <router-link
          to="/"
          class="flex items-center gap-1.5 px-3 py-1.5 rounded-xl bg-slate-700/80 hover:bg-slate-600 text-slate-200 text-xs font-semibold transition border border-slate-600 shrink-0"
          title="返回业主端首页"
        >
          <svg xmlns="http://www.w3.org/2000/svg" class="w-4 h-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="m15 18-6-6 6-6"/>
          </svg>
          <span>返回业主端</span>
        </router-link>

        <div class="w-9 h-9 rounded-xl bg-purple-600 flex items-center justify-center text-white font-bold shadow-md shadow-purple-600/30 shrink-0">
          🏢
        </div>
        <div class="min-w-0">
          <div class="flex items-center gap-2 flex-wrap">
            <h1 class="text-base font-black text-white tracking-wide">慧停管家 · 物业调度中台</h1>
            <span class="text-[10px] px-2 py-0.5 rounded-full bg-purple-900/60 text-purple-300 border border-purple-500/40 font-semibold whitespace-nowrap">
              物业管理 PC 端
            </span>
          </div>
          <p class="text-xs text-slate-400 break-words">服务小区：某某新村 (总车位 50 个) | 当前管理员：{{ userStore.realName }}</p>
        </div>
      </div>

      <div class="flex items-center gap-2 flex-wrap text-xs">
        <button
          @click="switchToOwnerH5"
          class="px-3.5 py-1.5 rounded-xl bg-blue-600 hover:bg-blue-500 text-white font-bold transition active:scale-95 flex items-center gap-1.5 shadow-md shadow-blue-600/30"
          title="一键切换为张欣欣并打开业主手机端 H5 界面"
        >
          <span>📱 一键切回业主手机端</span>
        </button>

        <button
          @click="handleResetDemo"
          class="px-3 py-1.5 rounded-xl bg-rose-700 hover:bg-rose-600 text-white font-semibold transition active:scale-95 flex items-center gap-1.5 shadow-sm"
          title="防翻车：一键将所有车位和账号重置为初始演示状态"
        >
          <span>🔄 一键重置演示</span>
        </button>

        <button
          @click="handleExcelTemplate"
          class="px-3 py-1.5 rounded-xl bg-slate-700 hover:bg-slate-600 text-slate-200 font-semibold transition flex items-center gap-1.5"
        >
          <span>📥 导出台账 Excel</span>
        </button>

        <button
          @click="userStore.togglePropertyTheme()"
          class="px-3 py-1.5 rounded-xl bg-slate-700 hover:bg-slate-600 text-slate-200 font-semibold transition flex items-center gap-1.5"
          :title="userStore.propertyTheme === 'dark' ? '切换为浅色模式' : '切换为深色大屏模式'"
        >
          <span>{{ userStore.propertyTheme === 'dark' ? '☀️ 浅色' : '🌙 深色' }}</span>
        </button>

        <button
          @click="handleLogout"
          class="px-3 py-1.5 rounded-xl bg-slate-800 hover:bg-slate-700 text-slate-400 hover:text-white transition border border-slate-700"
        >
          退出登录
        </button>
      </div>
    </header>

    <div class="flex-1 max-w-[1600px] w-full mx-auto p-4 sm:p-6 space-y-4">
      <!-- 三栏切换: 一次只展开一栏，避免手机端三栏叠成长页 -->
      <div class="bg-slate-800 p-1 rounded-2xl border border-slate-700 grid grid-cols-3 gap-1">
        <button
          v-for="tab in panels"
          :key="tab.id"
          @click="activePanel = tab.id"
          class="px-2 py-2.5 rounded-xl text-[11px] sm:text-xs font-bold transition active:scale-[0.98]"
          :class="activePanel === tab.id
            ? 'bg-purple-600 text-white shadow-md shadow-purple-600/30'
            : 'text-slate-400 hover:text-white hover:bg-slate-700'"
        >
          <span class="block text-base leading-none mb-0.5">{{ tab.icon }}</span>
          <span>{{ tab.label }}</span>
        </button>
      </div>

      <!-- 第 1 栏: 7 大关键大屏看板指标 -->
      <div v-show="activePanel === 'kpi'" class="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-4 xl:grid-cols-7 gap-3">
        <div class="bg-slate-800 p-4 rounded-2xl border border-slate-700">
          <span class="text-xs text-slate-400 block font-medium">总规划车位数</span>
          <span class="text-2xl font-black text-white mt-1 block font-mono">{{ stats.total_spaces ?? 50 }}</span>
        </div>
        <div class="bg-slate-800 p-4 rounded-2xl border border-slate-700">
          <span class="text-xs text-blue-400 block font-medium">已租车位 (出租率)</span>
          <div class="flex items-baseline gap-1 mt-1">
            <span class="text-2xl font-black text-blue-400 font-mono">{{ stats.rented_spaces ?? 0 }}</span>
            <span class="text-xs text-slate-400">({{ stats.occupancy_rate || '0%' }})</span>
          </div>
        </div>
        <div class="bg-slate-800 p-4 rounded-2xl border border-slate-700">
          <span class="text-xs text-emerald-400 block font-medium">空闲待分车位</span>
          <span class="text-2xl font-black text-emerald-400 mt-1 block font-mono">{{ stats.vacant_spaces ?? 0 }}</span>
        </div>
        <div class="bg-slate-800 p-4 rounded-2xl border border-slate-700">
          <span class="text-xs text-orange-400 block font-medium">维修停用车位</span>
          <span class="text-2xl font-black text-orange-400 mt-1 block font-mono">{{ stats.maintenance_spaces ?? 0 }}</span>
        </div>
        <div class="bg-slate-800 p-4 rounded-2xl border border-slate-700">
          <span class="text-xs text-amber-400 block font-medium">中签/线下确认中</span>
          <span class="text-2xl font-black text-amber-400 mt-1 block font-mono">{{ stats.allocated_spaces ?? 0 }}</span>
        </div>
        <div class="bg-slate-800 p-4 rounded-2xl border border-slate-700">
          <span class="text-xs text-purple-400 block font-medium">排队轮候总人数</span>
          <span class="text-2xl font-black text-purple-400 mt-1 block font-mono">{{ stats.waiting_count ?? 0 }}</span>
        </div>
        <div class="bg-slate-800 p-4 rounded-2xl border border-slate-700">
          <span class="text-xs text-rose-400 block font-medium">待办预警 (临期/待交割)</span>
          <span class="text-2xl font-black text-rose-400 mt-1 block font-mono">{{ stats.pending_warnings ?? 0 }}</span>
        </div>
      </div>

      <!-- 第 2 栏: 排队周转统计报表 -->
      <div v-show="activePanel === 'report'" class="bg-slate-800 rounded-2xl p-5 border border-slate-700">
        <div class="flex items-center justify-between mb-3 gap-2 flex-wrap">
          <h3 class="text-xs font-bold text-white flex items-center gap-1.5">📊 排队周转统计报表</h3>
          <span class="text-[10px] text-slate-400">基于存证流水与租约台账实时测算</span>
        </div>
        <div class="grid grid-cols-2 sm:grid-cols-4 gap-3 text-center">
          <div class="bg-slate-900/60 rounded-xl p-3 border border-slate-700/60">
            <span class="text-[10px] text-slate-400 block">平均周转周期</span>
            <span class="text-xl font-black text-cyan-400 font-mono mt-1 block">{{ stats.avg_turnover_days ?? 365 }}<span class="text-[10px] font-normal text-slate-400"> 天</span></span>
          </div>
          <div class="bg-slate-900/60 rounded-xl p-3 border border-slate-700/60">
            <span class="text-[10px] text-slate-400 block">轮候平均已等待</span>
            <span class="text-xl font-black text-amber-400 font-mono mt-1 block">{{ stats.avg_queue_wait_days ?? 0 }}<span class="text-[10px] font-normal text-slate-400"> 天</span></span>
          </div>
          <div class="bg-slate-900/60 rounded-xl p-3 border border-slate-700/60">
            <span class="text-[10px] text-slate-400 block">累计完成分配</span>
            <span class="text-xl font-black text-emerald-400 font-mono mt-1 block">{{ stats.completed_allocations ?? 0 }}<span class="text-[10px] font-normal text-slate-400"> 单</span></span>
          </div>
          <div class="bg-slate-900/60 rounded-xl p-3 border border-slate-700/60">
            <span class="text-[10px] text-slate-400 block">累计清退释放</span>
            <span class="text-xl font-black text-rose-400 font-mono mt-1 block">{{ stats.released_total ?? 0 }}<span class="text-[10px] font-normal text-slate-400"> 次</span></span>
          </div>
        </div>
      </div>

      <!-- 第 3 栏: 核心车位资产 50 矩阵网格 -->
      <div v-show="activePanel === 'spaces'" class="bg-slate-800 rounded-3xl p-3 sm:p-6 border border-slate-700 space-y-4">
        <div class="flex items-center justify-between flex-wrap gap-2">
          <div>
            <h2 class="text-sm font-bold text-white flex items-center gap-2 flex-wrap">
              <span>某某新村 50 车位资产实时运行矩阵</span>
              <span class="text-xs text-slate-400 font-normal hidden sm:inline">(点击任意车位卡片可发起管理操作)</span>
            </h2>
          </div>

          <!-- 状态图例说明 -->
          <div class="flex items-center gap-2 sm:gap-3 text-[10px] sm:text-xs flex-wrap">
            <span class="flex items-center gap-1.5">
              <span class="w-3 h-3 rounded bg-blue-600"></span>
              <span class="text-slate-300">已租</span>
            </span>
            <span class="flex items-center gap-1.5">
              <span class="w-3 h-3 rounded bg-amber-500"></span>
              <span class="text-slate-300">中签锁定</span>
            </span>
            <span class="flex items-center gap-1.5">
              <span class="w-3 h-3 rounded bg-emerald-500"></span>
              <span class="text-slate-300">空闲</span>
            </span>
            <span class="flex items-center gap-1.5">
              <span class="w-3 h-3 rounded bg-rose-600"></span>
              <span class="text-slate-300">维修</span>
            </span>
          </div>
        </div>

        <!-- 50 宫格矩阵: 手机 5 列 / 宽屏 10 列，避免 2 列把页面拉得很长 -->
        <div class="grid grid-cols-5 lg:grid-cols-10 gap-1.5 sm:gap-2.5">
          <div
            v-for="sp in spaceList"
            :key="sp.id"
            @click="selectSpace(sp)"
            class="p-1.5 sm:p-2.5 rounded-xl border cursor-pointer transition-all hover:scale-105 active:scale-95 flex flex-col justify-between h-[4.75rem] sm:h-24 text-xs select-none"
            :class="getSpaceColorClass(sp)"
          >
            <div class="flex items-center justify-between gap-0.5">
              <span class="font-mono font-black text-[11px] sm:text-sm">{{ sp.space_no }}</span>
              <span class="text-[9px] sm:text-[10px] opacity-75 hidden sm:inline">{{ sp.area_type === 'GROUND' ? '地面' : '地下' }}</span>
            </div>

            <div class="my-auto py-0.5 min-w-0">
              <span class="font-bold text-[10px] sm:text-[11px] block truncate">{{ sp.user_name || '待分' }}</span>
              <span class="text-[9px] sm:text-[10px] opacity-80 block truncate font-mono">{{ sp.plate_no || '--' }}</span>
            </div>

            <div class="flex items-center justify-between text-[9px] sm:text-[10px] opacity-90 border-t border-white/20 pt-0.5 sm:pt-1 gap-0.5">
              <span class="truncate">{{ getStatusLabel(sp.status) }}</span>
              <span class="hidden sm:inline">{{ sp.end_date ? sp.end_date.slice(5) : '--' }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 车位操作抽屉 / Modal -->
    <div v-if="selectedSpaceModal" class="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/70 backdrop-blur-sm animate-fade-in">
      <div class="bg-slate-800 rounded-3xl max-w-5xl w-full max-h-[90vh] overflow-y-auto p-3 sm:p-6 text-slate-100 shadow-2xl border border-slate-700 space-y-4">
        <div class="flex items-center justify-between border-b border-slate-700 pb-3">
          <div class="flex items-center gap-2">
            <span class="w-8 h-8 rounded-lg bg-purple-600 flex items-center justify-center font-black text-sm">
              {{ currentSpace.space_no }}
            </span>
            <div>
              <h3 class="text-sm font-bold text-white">车位资产管理操作</h3>
              <p class="text-xs text-slate-400">当前状态: {{ getStatusLabel(currentSpace.status) }}</p>
            </div>
          </div>
          <button @click="selectedSpaceModal = false" class="text-slate-400 hover:text-white p-1">✕</button>
        </div>

        <!-- 详细信息 | 车位档案 | 管理操作：手机单列，宽屏三列 -->
        <div class="grid grid-cols-1 md:grid-cols-3 gap-3 sm:gap-4 items-start">
        <div>
        <!-- 详细信息 -->
        <div class="bg-slate-900/80 p-3.5 rounded-xl border border-slate-700 text-xs space-y-2">
          <div class="flex justify-between">
            <span class="text-slate-400">当前承租/锁定业主:</span>
            <span class="font-bold text-white">{{ currentSpace.user_name || '无' }}</span>
          </div>
          <div class="flex justify-between">
            <span class="text-slate-400">联系电话:</span>
            <span class="font-mono text-slate-300">{{ currentSpace.user_phone || '无' }}</span>
          </div>
          <div class="flex justify-between">
            <span class="text-slate-400">绑定车辆车牌:</span>
            <span class="font-mono text-white font-bold">{{ currentSpace.plate_no || '无' }}</span>
          </div>
          <div class="flex justify-between">
            <span class="text-slate-400">租期截止日:</span>
            <span class="font-mono text-slate-300">{{ currentSpace.end_date || '无' }}</span>
          </div>
        </div>
        </div>

        <div>
        <!-- 车位档案: 历史承租记录与流转事件 -->
        <div class="bg-slate-900/80 p-3.5 rounded-xl border border-slate-700 text-xs space-y-2">
          <div class="flex items-center justify-between">
            <h4 class="font-bold text-white">📁 车位档案 · 历史承租记录</h4>
            <span class="text-[10px] text-slate-400">{{ spaceArchive ? spaceArchive.history.length + ' 条记录' : '加载中...' }}</span>
          </div>
          <div v-if="spaceArchive && spaceArchive.history.length === 0" class="text-slate-400 py-1">暂无承租记录</div>
          <div v-if="spaceArchive" class="space-y-1.5 max-h-32 overflow-y-auto">
            <div
              v-for="h in spaceArchive.history"
              :key="h.contract_no"
              class="flex items-center justify-between bg-slate-800/60 rounded-lg px-2.5 py-1.5"
            >
              <div>
                <span class="font-bold text-slate-200">{{ h.user_name }}</span>
                <span class="text-slate-400 ml-1.5 font-mono">{{ h.plate_no }}</span>
                <span class="text-slate-500 block text-[10px] font-mono">{{ h.start_date }} ~ {{ h.end_date }}</span>
              </div>
              <span
                class="text-[10px] px-1.5 py-0.5 rounded font-semibold"
                :class="h.status === 'TERMINATED' ? 'bg-slate-700 text-slate-300' : 'bg-emerald-900/50 text-emerald-300'"
              >{{ h.status_label }}</span>
            </div>
          </div>
          <div v-if="spaceArchive && spaceArchive.events.length > 0" class="pt-1.5 border-t border-slate-700/60 space-y-1">
            <span class="text-[10px] text-slate-500 font-semibold">上链流转事件:</span>
            <div v-for="(ev, i) in spaceArchive.events" :key="i" class="text-[10px] text-slate-400 flex justify-between gap-2">
              <span class="truncate">{{ ev.summary }}</span>
              <span class="font-mono shrink-0">{{ ev.time.slice(5, 16) }}</span>
            </div>
          </div>
        </div>
        </div>

        <div class="space-y-3">
        <!-- 操作项 1: 第三幕核心演示——物业强制代清退 (如 A-015) -->
        <div v-if="currentSpace.status === 'RENTED'" class="p-3 bg-rose-950/40 border border-rose-800/60 rounded-xl space-y-2">
          <h4 class="text-xs font-bold text-rose-300 flex items-center gap-1.5">
            <span>🚨 核心演示操作：物业强制代清退 (代办退租)</span>
          </h4>
          <p class="text-[11px] text-slate-400 leading-relaxed">
            用于老业主房屋买卖过户、失联违规占用等场景。清退后车位立即释放回池，<strong>系统排位引擎将秒级触发顺延，后续业主排位实时前进 1 位</strong>！
          </p>

          <div class="space-y-2 text-xs pt-1">
            <select v-model="terminateReason" class="w-full px-3 py-2 rounded-lg bg-slate-900 border border-slate-700 text-white">
              <option value="房屋买卖过户且原业主失联">房屋买卖过户且原业主失联</option>
              <option value="租约逾期未续租且车位被占">租约逾期未续租且车位被占</option>
              <option value="业主自愿委托物业代办退租">业主自愿委托物业代办退租</option>
            </select>

            <button
              @click="handleForceTerminate"
              :disabled="actionLoading"
              class="w-full py-2.5 rounded-xl bg-rose-600 hover:bg-rose-700 text-white font-bold text-xs shadow-md transition active:scale-[0.98] flex items-center justify-center gap-1"
            >
              <span>⚡ 执行强制代清退 (全流程存证上链)</span>
            </button>
          </div>
        </div>

        <!-- 操作项 2: 第四幕核心演示——线下签约完成交付 (如李雷中签 A-012) -->
        <div v-else-if="currentSpace.status === 'ALLOCATED' || currentSpace.status === 'OFFLINE_PENDING'" class="p-3 bg-amber-950/40 border border-amber-800/60 rounded-xl space-y-2">
          <h4 class="text-xs font-bold text-amber-300">📝 线下履约交付确认</h4>
          <p class="text-[11px] text-slate-400 leading-relaxed">
            已完成现场纸质合同签署并收缴租金，点击确认后正式转为“已租”，生成 1 年期正式租约与区块链存证。
          </p>
          <button
            @click="handleConfirmDelivery"
            :disabled="actionLoading"
            class="w-full py-2.5 rounded-xl bg-amber-600 hover:bg-amber-700 text-white font-bold text-xs shadow-md transition active:scale-[0.98]"
          >
            ✓ 线下签约已完成，确认正式交付
          </button>
        </div>

        <!-- 操作项 3: 维修停用 / 恢复可用 -->
        <div class="flex items-center gap-2 pt-2">
          <button
            v-if="currentSpace.status === 'MAINTENANCE'"
            @click="handleToggleMaintenance('RESTORE')"
            class="flex-1 py-2 rounded-xl bg-emerald-600 hover:bg-emerald-700 text-white font-bold text-xs transition"
          >
            恢复可用 (触发顺位分配)
          </button>
          <button
            v-else-if="currentSpace.status === 'VACANT'"
            @click="handleToggleMaintenance('SET_MAINTENANCE')"
            class="flex-1 py-2 rounded-xl bg-slate-700 hover:bg-rose-900 text-slate-200 font-bold text-xs transition"
          >
            设为维修停用
          </button>

          <button
            @click="selectedSpaceModal = false"
            class="px-4 py-2 rounded-xl bg-slate-700 hover:bg-slate-600 text-slate-300 text-xs font-semibold"
          >
            关闭
          </button>
        </div>
        </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../../store/user'
import api from '../../api'
import SeedAccountBar from '../../components/SeedAccountBar.vue'

const router = useRouter()
const userStore = useUserStore()

const activePanel = ref('spaces')
const panels = [
  { id: 'kpi', icon: '📈', label: '看板指标' },
  { id: 'report', icon: '📊', label: '周转报表' },
  { id: 'spaces', icon: '🅿️', label: '车位矩阵' }
]

const stats = ref({})
const spaceList = ref([])
const selectedSpaceModal = ref(false)
const currentSpace = ref({})
const terminateReason = ref('房屋买卖过户且原业主失联')
const actionLoading = ref(false)

const loadData = async () => {
  try {
    const sRes = await api.get('/property/stats')
    if (sRes.code === 200) stats.value = sRes.data

    const spRes = await api.get('/property/spaces')
    if (spRes.code === 200) spaceList.value = spRes.data
  } catch (err) {
    console.error('加载物业数据失败:', err)
  }
}

onMounted(() => {
  loadData()
})

const handleFastSwitch = async ({ identifier, password }) => {
  try {
    const res = await api.post('/auth/login', {
      identifier,
      password,
      login_type: 'PASSWORD'
    })
    userStore.setAuth(res.data.user, res.data.token)
    if (res.data.user.role === 'PROPERTY_ADMIN') {
      await loadData()
    } else {
      router.push('/')
    }
  } catch (err) {
    alert('切换失败: ' + err.message)
  }
}

const switchToOwnerH5 = async () => {
  // 快速切为张欣欣并跳转业主端
  await handleFastSwitch({ identifier: '13800000001', password: '123456' })
}

const spaceArchive = ref(null)

const selectSpace = async (sp) => {
  currentSpace.value = sp
  selectedSpaceModal.value = true
  spaceArchive.value = null
  try {
    const res = await api.get(`/property/space-history/${sp.id}`)
    if (res.code === 200) spaceArchive.value = res.data
  } catch (err) {
    console.error('加载车位档案失败:', err)
  }
}

const getStatusLabel = (st) => {
  switch (st) {
    case 'RENTED': return '已租'
    case 'ALLOCATED': return '48h确认'
    case 'OFFLINE_PENDING': return '线下办理'
    case 'VACANT': return '空闲待分'
    case 'MAINTENANCE': return '维修停用'
    default: return st
  }
}

const getSpaceColorClass = (sp) => {
  switch (sp.status) {
    case 'RENTED':
      return 'bg-blue-900/60 border-blue-500/50 text-blue-200 hover:border-blue-400'
    case 'ALLOCATED':
    case 'OFFLINE_PENDING':
      return 'bg-amber-900/60 border-amber-500/60 text-amber-200 hover:border-amber-400 ring-2 ring-amber-400/40 animate-pulse'
    case 'VACANT':
      return 'bg-emerald-900/60 border-emerald-500/50 text-emerald-200 hover:border-emerald-400'
    case 'MAINTENANCE':
      return 'bg-rose-950/70 border-rose-600/50 text-rose-300 hover:border-rose-400'
    default:
      return 'bg-slate-800 border-slate-700 text-slate-300'
  }
}

const handleForceTerminate = async () => {
  if (!confirm(`确认强制清退车位【${currentSpace.value.space_no}】吗？此操作将立即释放该车位，后序排队业主将秒级自动顺延前移！`)) return
  actionLoading.value = true
  try {
    const res = await api.post('/property/force-terminate', {
      space_id: currentSpace.value.id,
      reason: terminateReason.value,
      attachment_url: 'mock_property_deed_proof.jpg'
    })
    alert(res.message || '清退成功！后序排位已自动前移')
    selectedSpaceModal.value = false
    await loadData()
  } catch (err) {
    alert('清退失败: ' + err.message)
  } finally {
    actionLoading.value = false
  }
}

const handleConfirmDelivery = async () => {
  if (!currentSpace.value.user_id) {
    alert('该车位暂无关联中签用户')
    return
  }
  actionLoading.value = true
  try {
    const res = await api.post('/property/confirm-delivery', {
      space_id: currentSpace.value.id,
      user_id: currentSpace.value.user_id
    })
    alert(res.message || '交付成功，租约已生效！')
    selectedSpaceModal.value = false
    await loadData()
  } catch (err) {
    alert('交付失败: ' + err.message)
  } finally {
    actionLoading.value = false
  }
}

const handleToggleMaintenance = async (action) => {
  try {
    const res = await api.post('/property/set-maintenance', {
      space_id: currentSpace.value.id,
      action
    })
    alert(res.message || '操作成功')
    selectedSpaceModal.value = false
    await loadData()
  } catch (err) {
    alert('操作失败: ' + err.message)
  }
}

const handleResetDemo = async () => {
  if (!confirm('确认将全系统所有车位和账号数据一键重置为初始演示状态吗？')) return
  try {
    const res = await api.post('/dev/reset-seed')
    alert(res.message || '已成功重置为初始状态！')
    selectedSpaceModal.value = false
    await loadData()
  } catch (err) {
    alert('重置失败: ' + err.message)
  }
}

const handleExcelTemplate = async () => {
  try {
    const blob = await api.get('/property/export-spaces', { responseType: 'blob' })
    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.setAttribute('href', url)
    link.setAttribute('download', `${userStore.communityName}_车位资产台账_${new Date().toISOString().slice(0, 10)}.xlsx`)
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    URL.revokeObjectURL(url)
  } catch (err) {
    alert('导出失败: ' + err.message)
  }
}

const handleLogout = () => {
  userStore.logout()
  router.push('/login')
}
</script>
