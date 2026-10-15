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
          <span>回物业端 ➔</span>
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

        <div v-if="isProduction" class="rounded-xl bg-white/10 p-3 text-xs text-blue-100">
          续租或退租请联系物业服务中心，完成合同及费用核对后由物业办理。
        </div>
        <div v-else class="flex items-center gap-2">
          <button
            v-if="mySpace.days_left <= 30"
            @click="handleRenew"
            class="flex-1 py-2.5 rounded-xl bg-emerald-500 hover:bg-emerald-600 text-white font-bold text-xs shadow-md transition"
          >
            申请续租
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
              <span v-if="myQueue.priority_level === 2" class="px-2.5 py-0.5 rounded-full text-[11px] font-bold bg-amber-50 text-amber-700 border border-amber-200 inline-block mb-1">
                同户二车 (顺延轮候)
              </span>
              <span class="text-xs text-slate-500 font-medium block">
                前面还有 <span class="text-blue-600 font-bold font-mono text-sm">{{ Math.max(0, myQueue.current_rank - 1) }}</span> 人
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

      <!-- 视图 E: 待人工审核视图 (灵活配置模式下 AI 初审通过，待物业人工审核) -->
      <div v-else-if="viewState === 'PENDING_REVIEW'" class="space-y-3 animate-fade-in">
        <div class="bg-gradient-to-br from-amber-500/10 via-amber-600/5 to-slate-900/5 bg-white rounded-2xl p-5 shadow-sm border-2 border-amber-300 relative overflow-hidden">
          <div class="flex items-center justify-between mb-3">
            <span class="text-xs font-bold text-amber-700 flex items-center gap-1.5">
              <span class="w-2 h-2 rounded-full bg-amber-500 animate-ping"></span>
              <span>车位申请审核中 (待物业人工复核)</span>
            </span>
            <span class="px-2.5 py-0.5 rounded-full text-[10px] font-bold bg-amber-100 text-amber-800 border border-amber-300">
              待人工审核
            </span>
          </div>

          <div class="space-y-3 my-2">
            <div>
              <span class="text-xs text-slate-400 block">申报车辆号牌</span>
              <span class="text-2xl font-black font-mono text-slate-800 tracking-wider">
                {{ myQueue?.plate_no || '审核中' }}
              </span>
            </div>

            <!-- 审核流程进度条 -->
            <div class="bg-amber-50/80 rounded-xl p-3 border border-amber-200/80 space-y-2 text-xs">
              <div class="flex items-center justify-between">
                <span class="font-bold text-amber-900">当前审核机制：</span>
                <span class="text-[11px] font-bold text-purple-700 bg-purple-100 px-2 py-0.5 rounded-md border border-purple-200">
                  物业人工审核
                </span>
              </div>
              <div class="space-y-1.5 pt-1 text-[11px]">
                <div class="flex items-center gap-2 text-emerald-700 font-semibold">
                  <span>✓ 步骤 1：申请信息已提交</span>
                </div>
                <div class="flex items-center gap-2 text-amber-800 font-bold">
                  <span class="animate-spin">⏳</span>
                  <span>步骤 2：等待物业核对证明材料原件</span>
                </div>
                <div class="flex items-center gap-2 text-slate-400">
                  <span>⚪ 步骤 3: 人工审核通过后系统自动并入车位 FIFO 轮候序列</span>
                </div>
              </div>
            </div>

            <div class="p-2.5 rounded-xl bg-slate-50 border border-slate-200 text-slate-500 text-[11px] flex items-center justify-between">
              <span>申报坐落：{{ myQueue?.property_no || '本小区房屋' }}</span>
              <span class="font-mono text-[10px]">{{ myQueue?.apply_time }}</span>
            </div>
          </div>

          <!-- 撤销申请按钮 -->
          <div class="pt-2 border-t border-slate-100">
            <button
              @click="handleCancelApply"
              class="w-full py-2.5 rounded-xl bg-white hover:bg-rose-50 text-rose-600 font-semibold text-xs border border-rose-200 shadow-sm transition active:scale-[0.99] flex items-center justify-center gap-1.5"
            >
              <svg xmlns="http://www.w3.org/2000/svg" class="w-4 h-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <circle cx="12" cy="12" r="10"/><path d="m15 9-6 6"/><path d="m9 9 6 6"/>
              </svg>
              <span>撤销本次申请</span>
            </button>
          </div>
        </div>
      </div>

      <!-- 视图 F: 申请被驳回视图 (提示驳回理由，支持一键重新调整申报) -->
      <div v-else-if="viewState === 'REJECTED'" class="space-y-3 animate-fade-in">
        <div class="bg-white rounded-2xl p-5 shadow-sm border-2 border-rose-300 relative overflow-hidden">
          <div class="flex items-center justify-between mb-3">
            <span class="text-xs font-bold text-rose-600 flex items-center gap-1.5">
              <span class="w-2 h-2 rounded-full bg-rose-500"></span>
              <span>车位申请未通过审核</span>
            </span>
            <span class="px-2.5 py-0.5 rounded-full text-[10px] font-bold bg-rose-100 text-rose-700 border border-rose-200">
              已驳回
            </span>
          </div>

          <div class="bg-rose-50 rounded-xl p-4 border border-rose-200 space-y-2 my-2">
            <div class="flex items-center gap-2 text-rose-800 font-bold text-xs">
              <span>⚠️ 物业人工审核驳回意见：</span>
            </div>
            <p class="text-xs text-rose-700 font-medium leading-relaxed bg-white/80 p-3 rounded-lg border border-rose-200">
              {{ myQueue?.reject_reason || '材料不齐或车辆权属凭证不符合规定要求，请核对后重新提交。' }}
            </p>
            <p class="text-[11px] text-slate-500 pt-1">
              您可根据物业驳回意见，重新补充或调整相关车辆与房产证明附件后再次提交。
            </p>
          </div>

          <div class="pt-2 flex items-center gap-2">
            <router-link
              to="/apply"
              class="flex-1 py-3 rounded-xl bg-blue-600 hover:bg-blue-700 text-white font-bold text-xs text-center shadow-md shadow-blue-500/20 transition active:scale-[0.98]"
            >
              ✏️ 修改材料，重新发起申报 ➔
            </router-link>
          </div>
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
            当前登录身份：【{{ userStore.realName }}】(物业管理员)。您可在此查验车位公示大盘、顺位全量脱敏队列与存证链，随时可切回物业管理。
          </p>
        </div>
        <div v-else>
          <h3 class="text-base font-black text-slate-800">尚未在本小区申请车位</h3>
          <p class="text-xs text-slate-500 mt-1 leading-relaxed max-w-xs mx-auto">
            老旧小区车位资源紧张，系统实行“实名规范申报、按序阳光轮候、全流程存证”的公平管理机制。
          </p>
        </div>

        <router-link
          v-if="userStore.isPropertyAdmin"
          to="/property/dashboard"
          class="inline-flex items-center justify-center gap-2 w-full py-3 rounded-xl bg-purple-600 hover:bg-purple-700 text-white font-bold text-sm shadow-md shadow-purple-500/20 active:scale-[0.98] transition"
        >
          <span>🏢 进入物业管理后台</span>
          <svg xmlns="http://www.w3.org/2000/svg" class="w-4 h-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
            <path d="m9 18 6-6-6-6"/>
          </svg>
        </router-link>
        <router-link
          v-else
          to="/apply"
          class="inline-flex items-center justify-center gap-2 w-full py-3 rounded-xl bg-blue-600 hover:bg-blue-700 text-white font-bold text-sm shadow-md shadow-blue-500/20 active:scale-[0.98] transition"
        >
          <span>立即申请车位排位 ➔</span>
          <svg xmlns="http://www.w3.org/2000/svg" class="w-4 h-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
            <path d="m9 18 6-6-6-6"/>
          </svg>
        </router-link>
      </div>

      <!-- 小区车辆概况指标 (仅展示统计数据，绝不暴露邻居姓名或门牌隐私) -->
      <div class="bg-white rounded-2xl p-4 shadow-sm border border-slate-200">
        <div class="flex items-center justify-between mb-3">
          <h3 class="text-xs font-bold text-slate-800 flex items-center gap-1.5">
            <span class="w-1.5 h-3.5 bg-blue-600 rounded-full"></span>
            <span>小区车辆概况</span>
          </h3>
          <span class="text-[11px] text-slate-400">总车位 {{ overview?.total_spaces ?? 0 }} 个</span>
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
          <span>🔒 业主隐私安全：个人车辆信息互不可见</span>
          <router-link to="/queue" class="text-blue-600 font-semibold hover:underline">
            查看排队情况 ➔
          </router-link>
        </div>
      </div>
    </main>

    <BottomNav />

    <!-- 续租周期选择弹窗 (支持半年/一年/季度/月度，默认半年) -->
    <div v-if="showRenewModal && mySpace" class="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/60 backdrop-blur-sm animate-fade-in">
      <div class="bg-white rounded-3xl max-w-sm w-full p-5 text-slate-800 shadow-2xl border border-slate-100 space-y-4">
        <div class="flex items-center justify-between border-b border-slate-100 pb-3">
          <div class="flex items-center gap-2">
            <span class="w-8 h-8 rounded-xl bg-emerald-100 text-emerald-600 flex items-center justify-center font-bold text-sm">
              ⏳
            </span>
            <div>
              <h3 class="text-sm font-bold text-slate-800">车位续租申请</h3>
              <p class="text-xs text-slate-400">车位：{{ mySpace.space_no }} ({{ mySpace.plate_no }})</p>
            </div>
          </div>
          <button @click="showRenewModal = false" class="text-slate-400 hover:text-slate-600 p-1 text-base">✕</button>
        </div>

        <div class="space-y-3 text-xs">
          <div class="bg-slate-50 p-3 rounded-xl border border-slate-100 space-y-1">
            <div class="flex justify-between text-slate-500">
              <span>当前到期日：</span>
              <span class="font-mono text-slate-700 font-semibold">{{ mySpace.end_date }}</span>
            </div>
            <div class="flex justify-between text-slate-500">
              <span>剩余有效期：</span>
              <span class="font-bold text-amber-600">{{ mySpace.days_left }} 天</span>
            </div>
          </div>

          <div>
            <div class="flex items-center justify-between mb-1.5">
              <label class="block font-bold text-slate-700">选择续租周期：</label>
              <span class="text-[10px] text-emerald-600 font-medium">默认半年 (可依需自选)</span>
            </div>
            <div class="grid grid-cols-2 gap-2">
              <button
                v-for="term in renewTerms"
                :key="term.months"
                type="button"
                @click="selectedRenewMonths = term.months"
                class="p-2.5 rounded-xl border text-left transition relative"
                :class="selectedRenewMonths === term.months
                  ? 'border-emerald-500 bg-emerald-50/70 text-emerald-800 font-bold ring-2 ring-emerald-400/40'
                  : 'border-slate-200 hover:border-slate-300 text-slate-600'"
              >
                <div class="flex items-center justify-between">
                  <span>{{ term.label }}</span>
                  <span v-if="term.recommended" class="text-[9px] bg-emerald-600 text-white px-1 py-0.2 rounded font-normal">默认</span>
                </div>
                <span class="text-[10px] text-slate-400 block mt-0.5">{{ term.desc }}</span>
              </button>
            </div>
          </div>

          <!-- 预计顺延新到期日 -->
          <div class="p-3 bg-emerald-50 border border-emerald-200/60 rounded-xl flex items-center justify-between text-xs">
            <span class="text-emerald-700 font-medium">顺延后新到期日：</span>
            <span class="font-mono font-bold text-emerald-800 text-sm">{{ calculatedNewEndDate }}</span>
          </div>

          <p class="text-[11px] text-slate-400 leading-relaxed">
            💡 提示：续租和缴费请先到物业服务中心办理，系统会记录经确认的租约变更。
          </p>
        </div>

        <div class="flex items-center gap-2 pt-2">
          <button
            type="button"
            @click="showRenewModal = false"
            class="flex-1 py-2.5 rounded-xl bg-slate-100 hover:bg-slate-200 text-slate-600 font-semibold text-xs transition"
          >
            取消
          </button>
          <button
            type="button"
            @click="submitRenew"
            :disabled="renewLoading"
            class="flex-1 py-2.5 rounded-xl bg-emerald-600 hover:bg-emerald-700 text-white font-bold text-xs shadow-md transition disabled:opacity-50"
          >
            {{ renewLoading ? '提交中...' : '确认续租' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useUserStore } from '../store/user'
import api from '../api'
import NavBar from '../components/NavBar.vue'
import CountdownCard from '../components/CountdownCard.vue'
import AiWaitingCard from '../components/AiWaitingCard.vue'
import BottomNav from '../components/BottomNav.vue'
import { showConfirm, showToast } from '../utils/dialog'

const userStore = useUserStore()
const isProduction = import.meta.env.VITE_APP_MODE === 'production'

let pollTimer = null

const showRenewModal = ref(false)
const renewLoading = ref(false)
const selectedRenewMonths = ref(6)

const renewTerms = [
  { months: 6, label: '半年 (6个月)', desc: '顺延 180 天', recommended: true },
  { months: 12, label: '一年 (12个月)', desc: '顺延 365 天', recommended: false },
  { months: 3, label: '一季度 (3个月)', desc: '顺延 90 天', recommended: false },
  { months: 1, label: '一个月 (短期)', desc: '顺延 30 天', recommended: false }
]

const calculatedNewEndDate = computed(() => {
  if (!mySpace.value?.end_date) return '--'
  const base = new Date(mySpace.value.end_date)
  if (isNaN(base.getTime())) return '--'
  const daysMap = { 12: 365, 6: 180, 3: 90, 1: 30 }
  const days = daysMap[selectedRenewMonths.value] || (selectedRenewMonths.value * 30)
  base.setDate(base.getDate() + days)
  const y = base.getFullYear()
  const m = String(base.getMonth() + 1).padStart(2, '0')
  const d = String(base.getDate()).padStart(2, '0')
  return `${y}-${m}-${d}`
})

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
  const ok = await showConfirm({
    title: '撤销排队申请确认',
    message: '撤销申请后，您当前的顺位将立即释放给后续业主。重新申请需重新核验并排至队尾。确认撤销吗？',
    confirmText: '确认撤销',
    type: 'danger'
  })
  if (!ok) return
  try {
    const res = await api.post('/parking/cancel-apply', { reason: '自愿放弃申请' })
    showToast(res.message || '申请已成功撤销', 'success')
    await loadData()
  } catch (err) {
    showToast('撤销失败: ' + err.message, 'error')
  }
}

const handleRenew = () => {
  selectedRenewMonths.value = 6 // 默认半年
  showRenewModal.value = true
}

const submitRenew = async () => {
  renewLoading.value = true
  try {
    const res = await api.post('/parking/renew', { months: selectedRenewMonths.value })
    showToast(res.message || '续租成功', 'success')
    showRenewModal.value = false
    await loadData()
  } catch (err) {
    showToast('续租失败: ' + err.message, 'error')
  } finally {
    renewLoading.value = false
  }
}

const handleTerminate = async () => {
  const ok = await showConfirm({
    title: '车位主动退租确认',
    message: '确认申请退租吗？车位将立即释放并由系统按排队顺位分配，请谨慎操作。',
    confirmText: '确认退租',
    type: 'danger'
  })
  if (!ok) return
  try {
    const res = await api.post('/parking/terminate-rental')
    showToast(res.message || '退租成功，车位已释放并自动顺延', 'success')
    await loadData()
  } catch (err) {
    showToast('退租失败: ' + err.message, 'error')
  }
}
</script>
