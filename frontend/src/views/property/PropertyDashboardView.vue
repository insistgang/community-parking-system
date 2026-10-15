<template>
  <div class="min-h-screen bg-slate-900 text-slate-100 flex flex-col property-dashboard" :class="{ 'light-mode': userStore.propertyTheme === 'light' }">
    <!-- 顶部导航栏 -->
    <header class="bg-slate-800/90 backdrop-blur border-b border-slate-700 px-4 sm:px-6 py-3 flex flex-wrap items-center justify-between gap-x-3 gap-y-2 sticky top-0 z-30 shadow-md">
      <div class="flex items-center gap-3 min-w-0">
        <router-link
          to="/"
          class="flex items-center gap-1.5 px-3 py-1.5 rounded-xl bg-slate-700/80 hover:bg-slate-600 active:scale-95 text-slate-200 text-xs font-semibold transition border border-slate-600 shrink-0 cursor-pointer"
          title="以当前管理员身份返回前台首页"
        >
          <AppIcon name="back" class="w-4 h-4" />
          <span>返回业主端</span>
        </router-link>

        <div class="w-9 h-9 rounded-xl bg-purple-600 flex items-center justify-center text-white font-bold shadow-md shadow-purple-600/30 shrink-0">
          <AppIcon name="building" :size="22" />
        </div>
        <div class="min-w-0">
          <div class="flex items-center gap-2 flex-wrap">
            <h1 class="text-base font-black text-white tracking-wide">慧停管家 · 物业管理后台</h1>
            <span class="text-[10px] px-2 py-0.5 rounded-full bg-purple-900/60 text-purple-300 border border-purple-500/40 font-semibold whitespace-nowrap">
              物业管理 PC 端
            </span>
          </div>
          <p class="text-xs text-slate-400 break-words">服务小区：{{ userStore.communityName }}（总车位 {{ stats.total_spaces ?? 0 }} 个）| 当前管理员：{{ userStore.realName }}</p>
        </div>
      </div>

      <div class="flex items-center gap-2 flex-wrap text-xs">
        <button v-if="isProduction" @click="openOwnerInvite"
          class="px-3 py-1.5 rounded-xl bg-indigo-700 hover:bg-indigo-600 text-white font-semibold transition shadow-sm">
          <AppIcon name="plus" size="1em" /> 签发业主邀请码
        </button>
        <button v-if="isProduction" @click="showAddSpace = true"
          class="px-3 py-1.5 rounded-xl bg-emerald-700 hover:bg-emerald-600 text-white font-semibold transition shadow-sm">
          <AppIcon name="plus" size="1em" /> 录入真实车位
        </button>
        <button v-if="!isProduction"
          @click="handleResetDemo"
          class="px-3 py-1.5 rounded-xl bg-rose-700 hover:bg-rose-600 text-white font-semibold transition active:scale-95 flex items-center gap-1.5 shadow-sm"
          title="防翻车：一键将所有车位和账号重置为初始演示状态"
        >
          <span><AppIcon name="refresh" size="1em" /> 一键重置演示</span>
        </button>

        <button
          @click="openQuickMaintenanceModal"
          class="px-3 py-1.5 rounded-xl bg-amber-600 hover:bg-amber-500 text-white font-semibold transition active:scale-95 flex items-center gap-1.5 shadow-sm"
          title="快速选择车位设为维修停用或录入报修"
        >
          <span><AppIcon name="wrench" size="1em" /> 登记车位维修</span>
        </button>

        <button
          @click="handleExcelTemplate"
          class="px-3 py-1.5 rounded-xl bg-slate-700 hover:bg-slate-600 text-slate-200 font-semibold transition flex items-center gap-1.5"
        >
          <span><AppIcon name="export" size="1em" /> 导出台账 Excel</span>
        </button>

        <button
          @click="userStore.togglePropertyTheme()"
          class="px-3 py-1.5 rounded-xl bg-slate-700 hover:bg-slate-600 text-slate-200 font-semibold transition flex items-center gap-1.5"
          :title="userStore.propertyTheme === 'dark' ? '切换为浅色模式' : '切换为深色大屏模式'"
        >
          <AppIcon :name="userStore.propertyTheme === 'dark' ? 'sun' : 'moon'" :size="16" /><span>{{ userStore.propertyTheme === 'dark' ? '浅色' : '深色' }}</span>
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
      <!-- 栏目切换: 四大中台管理视口 -->
      <div class="bg-slate-800 p-1 rounded-2xl border border-slate-700 grid grid-cols-2 sm:grid-cols-4 gap-1">
        <button
          v-for="tab in panels"
          :key="tab.id"
          @click="activePanel = tab.id"
          class="relative px-2 py-2.5 rounded-xl text-[11px] sm:text-xs font-bold transition active:scale-[0.98]"
          :class="activePanel === tab.id
            ? 'bg-purple-600 text-white shadow-md shadow-purple-600/30'
            : 'text-slate-400 hover:text-white hover:bg-slate-700'"
        >
          <span class="flex justify-center mb-1"><AppIcon :name="tab.icon" :size="20" /></span>
          <span>{{ tab.label }}</span>
          <span
            v-if="tab.id === 'audit' && stats.pending_audit_count > 0"
            class="absolute top-1.5 right-2 px-1.5 py-0.2 rounded-full bg-rose-500 text-white text-[9px] font-mono animate-pulse"
          >
            {{ stats.pending_audit_count }}
          </span>
        </button>
      </div>

      <div v-if="stats.inventory_complete === false" class="rounded-xl border border-amber-500/50 bg-amber-950/40 px-4 py-3 text-xs text-amber-100">
        已导入 {{ stats.legacy_import_count ?? 0 }} 条历史在租台账，租户身份尚待补齐。空闲和维修车位清单未提供，出租率暂不计算；缺失字段显示 **。
      </div>

      <div v-if="stats.company_name || stats.source_audit_mode" class="rounded-xl border border-slate-700 bg-slate-800 px-4 py-3 text-xs text-slate-300 leading-relaxed space-y-1">
        <p>所属区街道：{{ stats.district || '**' }} / {{ stats.subdistrict || '**' }} · 物业公司：{{ stats.company_name || '**' }}</p>
        <p>初始化联系人：{{ stats.contact_name || '**' }}（{{ stats.contact_phone || '**' }}）</p>
        <p v-if="stats.source_audit_mode">原表审核方式：{{ stats.source_audit_mode }} · 当前执行：{{ stats.audit_mode_label }}<span v-if="stats.source_audit_mode === '三证自动' && stats.audit_mode === 'FLEXIBLE_HYBRID'">（自动核验服务尚未接入）</span></p>
      </div>
      <div v-if="stats.legacy_queue_unlinked_count" class="rounded-xl border border-amber-500/50 bg-amber-950/40 px-4 py-3 text-xs text-amber-100 leading-relaxed">
        已录入 {{ stats.legacy_queue_unlinked_count }} 条待补齐身份的历史排队记录，原申请时间和顺位已保留。该顺位到达队首时，需先核实并关联业主账号，系统不会跳过此人分配给后续申请。
      </div>

      <!-- 第 1 栏: 7 大关键大屏看板指标 -->
      <div v-show="activePanel === 'kpi'" class="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-4 xl:grid-cols-7 gap-3">
        <div class="bg-slate-800 p-4 rounded-2xl border border-slate-700">
          <span class="text-xs text-slate-400 block font-medium">{{ stats.inventory_complete === false ? '已录入车位数' : '总规划车位数' }}</span>
          <span class="text-2xl font-black text-white mt-1 block font-mono">{{ stats.total_spaces ?? 0 }}</span>
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
          <h3 class="text-xs font-bold text-white flex items-center gap-1.5"><AppIcon name="chart" size="1em" /> 排队周转统计报表</h3>
          <span class="text-[10px] text-slate-400">基于存证流水与租约台账实时测算</span>
        </div>
        <div class="grid grid-cols-2 sm:grid-cols-4 gap-3 text-center">
          <div class="bg-slate-900/60 rounded-xl p-3 border border-slate-700/60">
            <span class="text-[10px] text-slate-400 block">平均周转周期</span>
            <span class="text-xl font-black text-cyan-400 font-mono mt-1 block">{{ stats.avg_turnover_days ?? 0 }}<span class="text-[10px] font-normal text-slate-400"> 天</span></span>
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
              <span>{{ userStore.communityName }} {{ stats.total_spaces ?? 0 }} 车位资产实时运行矩阵</span>
              <span class="text-xs text-slate-400 font-normal hidden sm:inline">(点击任意车位卡片可发起管理操作)</span>
            </h2>
          </div>

          <!-- 状态图例说明与快捷筛选 -->
          <div class="flex items-center gap-1.5 sm:gap-2 text-[10px] sm:text-xs flex-wrap">
            <button
              @click="statusFilter = 'ALL'"
              class="px-2 py-0.5 rounded-lg border transition font-medium"
              :class="statusFilter === 'ALL' ? 'bg-purple-600 text-white border-purple-500' : 'bg-slate-750 text-slate-300 border-slate-600 hover:text-white'"
            >
              全部 ({{ spaceList.length }})
            </button>
            <button
              @click="statusFilter = 'RENTED'"
              class="px-2 py-0.5 rounded-lg border transition flex items-center gap-1 font-medium"
              :class="statusFilter === 'RENTED' ? 'bg-blue-600 text-white border-blue-500' : 'bg-slate-750 text-slate-300 border-slate-600 hover:text-white'"
            >
              <span class="w-2 h-2 rounded-full bg-blue-400"></span>
              <span>已租</span>
            </button>
            <button
              @click="statusFilter = 'ALLOCATED'"
              class="px-2 py-0.5 rounded-lg border transition flex items-center gap-1 font-medium"
              :class="statusFilter === 'ALLOCATED' ? 'bg-amber-600 text-white border-amber-500' : 'bg-slate-750 text-slate-300 border-slate-600 hover:text-white'"
            >
              <span class="w-2 h-2 rounded-full bg-amber-400"></span>
              <span>中签</span>
            </button>
            <button
              @click="statusFilter = 'VACANT'"
              class="px-2 py-0.5 rounded-lg border transition flex items-center gap-1 font-medium"
              :class="statusFilter === 'VACANT' ? 'bg-emerald-600 text-white border-emerald-500' : 'bg-slate-750 text-slate-300 border-slate-600 hover:text-white'"
            >
              <span class="w-2 h-2 rounded-full bg-emerald-400"></span>
              <span>空闲</span>
            </button>
            <button
              @click="statusFilter = 'MAINTENANCE'"
              class="px-2 py-0.5 rounded-lg border transition flex items-center gap-1 font-medium"
              :class="statusFilter === 'MAINTENANCE' ? 'bg-rose-600 text-white border-rose-500' : 'bg-slate-750 text-slate-300 border-slate-600 hover:text-white'"
            >
              <span class="w-2 h-2 rounded-full bg-rose-400"></span>
              <span><AppIcon name="wrench" size="1em" /> 维修停用 ({{ stats.maintenance_spaces ?? 0 }})</span>
            </button>
          </div>
        </div>

        <!-- 50 宫格矩阵: 手机 5 列 / 宽屏 10 列，避免 2 列把页面拉得很长 -->
        <div class="grid grid-cols-5 lg:grid-cols-10 gap-1.5 sm:gap-2.5">
          <div
            v-for="sp in filteredSpaces"
            :key="sp.id"
            @click="selectSpace(sp)"
            class="p-1.5 sm:p-2.5 rounded-xl border cursor-pointer transition-all hover:scale-105 active:scale-95 flex flex-col justify-between h-[4.75rem] sm:h-24 text-xs select-none"
            :class="getSpaceColorClass(sp)"
          >
            <div class="flex items-center justify-between gap-0.5">
              <span class="font-mono font-black text-[11px] sm:text-sm">{{ sp.space_no }}</span>
              <span class="text-[9px] sm:text-[10px] opacity-75 hidden sm:inline">{{ spaceAreaLabel(sp.area_type) }}</span>
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

      <!-- 第 4 栏: 审核机制配置与申请审批台账 -->
      <div v-show="activePanel === 'audit'" class="space-y-4">
        <!-- 子 Tab 导航条：台账优先放前，特例通道放后 -->
        <div class="flex items-center justify-between flex-wrap gap-2 bg-slate-800/90 p-2 sm:p-2.5 rounded-2xl border border-slate-700">
          <div class="flex items-center gap-2">
            <button
              @click="auditSubTab = 'applications'"
              class="px-3.5 sm:px-4 py-2 rounded-xl text-xs font-bold transition flex items-center gap-2"
              :class="auditSubTab === 'applications' ? 'bg-purple-600 text-white shadow-md' : 'bg-slate-900/70 text-slate-300 hover:text-white border border-slate-700/80'"
            >
              <span><AppIcon name="clipboard" size="1em" /> 车位排队申请审核台账</span>
              <span v-if="pendingAuditCount > 0" class="px-1.5 py-0.2 rounded-full bg-rose-500 text-white text-[10px] font-mono font-bold">
                {{ pendingAuditCount }} 待审
              </span>
            </button>
            <button v-if="!isProduction"
              @click="auditSubTab = 'strategy'"
              class="px-3.5 sm:px-4 py-2 rounded-xl text-xs font-bold transition flex items-center gap-2"
              :class="auditSubTab === 'strategy' ? 'bg-purple-600 text-white shadow-md' : 'bg-slate-900/70 text-slate-300 hover:text-white border border-slate-700/80'"
            >
              <span><AppIcon name="settings" size="1em" /> 申报机制与特定业主特例通道</span>
              <span v-if="customizedUserCount > 0" class="px-1.5 py-0.2 rounded-full bg-indigo-500/40 text-indigo-300 text-[10px] font-mono border border-indigo-500/40">
                {{ customizedUserCount }} 特例
              </span>
            </button>
          </div>
          <div class="text-[11px] text-slate-400 px-2 font-mono hidden sm:inline">
            小区: {{ userStore.communityName || '龙华新村' }}
          </div>
        </div>

        <!-- 子 Tab 1: 申请审核台账 (高频使用，排在前面) -->
        <div v-show="auditSubTab === 'applications'" class="bg-slate-800 rounded-3xl p-4 sm:p-6 border border-slate-700 space-y-4">
          <div class="flex items-center justify-between flex-wrap gap-3">
            <div>
              <h3 class="text-sm font-bold text-white flex items-center gap-2">
                <span><AppIcon name="clipboard" size="1em" /> 车位排队申请审核台账</span>
                <span v-if="pendingAuditCount > 0" class="text-xs text-amber-400 font-normal">
                  (待人工审核: {{ pendingAuditCount }} 单)
                </span>
              </h3>
              <p class="text-xs text-slate-400 mt-1">
                {{ isProduction ? '申请资料需在线下核对原件并登记核验编号，再作出审核决定。' : '点击【查验原件资料】可调出证件资料演示查验台。' }}
              </p>
            </div>

            <!-- 筛选标签 -->
            <div class="flex items-center gap-1.5 text-xs flex-wrap">
              <button
                @click="auditFilter = 'ALL'"
                class="px-2.5 py-1 rounded-lg border transition font-medium"
                :class="auditFilter === 'ALL' ? 'bg-purple-600 text-white border-purple-500' : 'bg-slate-750 text-slate-300 border-slate-600 hover:text-white'"
              >
                全部单据 ({{ applicationList.length }})
              </button>
              <button
                @click="auditFilter = 'PENDING_REVIEW'"
                class="px-2.5 py-1 rounded-lg border transition flex items-center gap-1 font-medium"
                :class="auditFilter === 'PENDING_REVIEW' ? 'bg-amber-600 text-white border-amber-500' : 'bg-slate-750 text-slate-300 border-slate-600 hover:text-white'"
              >
                <span>待人工审核</span>
                <span v-if="pendingAuditCount > 0" class="px-1.5 py-0.2 rounded-full bg-rose-500 text-white text-[10px] font-bold">
                  {{ pendingAuditCount }}
                </span>
              </button>
              <button
                @click="auditFilter = 'WAITING'"
                class="px-2.5 py-1 rounded-lg border transition font-medium"
                :class="auditFilter === 'WAITING' ? 'bg-blue-600 text-white border-blue-500' : 'bg-slate-750 text-slate-300 border-slate-600 hover:text-white'"
              >
                已入队排位中
              </button>
              <button
                @click="auditFilter = 'REJECTED'"
                class="px-2.5 py-1 rounded-lg border transition font-medium"
                :class="auditFilter === 'REJECTED' ? 'bg-rose-600 text-white border-rose-500' : 'bg-slate-750 text-slate-300 border-slate-600 hover:text-white'"
              >
                已驳回
              </button>
            </div>
          </div>

          <!-- 表格展示 -->
          <div class="overflow-x-auto">
            <table class="w-full text-left text-xs border-collapse">
              <thead>
                <tr class="border-b border-slate-700 text-slate-400 font-semibold bg-slate-900/40">
                  <th class="p-3">申请人 / 手机</th>
                  <th class="p-3">申报房屋坐落</th>
                  <th class="p-3">车牌号 / 归属</th>
                  <th class="p-3">审核机制</th>
                  <th class="p-3">{{ isProduction ? '核验 / 台账' : '证明材料附件' }}</th>
                  <th class="p-3">单据状态</th>
                  <th class="p-3">申报时间</th>
                  <th class="p-3 text-right">审核操作</th>
                </tr>
              </thead>
              <tbody class="divide-y divide-slate-700/60">
                <tr v-if="filteredApplications.length === 0">
                  <td colspan="8" class="p-8 text-center text-slate-500">暂无对应申请单据</td>
                </tr>
                <tr
                  v-for="app in filteredApplications"
                  :key="app.id"
                  class="hover:bg-slate-750/50 transition"
                  :class="app.status === 'PENDING_REVIEW' ? 'bg-amber-950/20' : ''"
                >
                  <td class="p-3">
                    <div class="font-bold text-white">{{ app.applicant_name }}</div>
                    <div class="text-[11px] text-slate-400 font-mono">{{ app.applicant_phone }}</div>
                    <div v-if="app.legacy_source_rank" class="text-[10px] text-amber-300 mt-1">原顺位 {{ app.legacy_source_rank }} · 当前顺位 {{ app.current_rank }}</div>
                  </td>
                  <td class="p-3 text-slate-200">{{ app.property_no }}</td>
                  <td class="p-3">
                    <div class="font-mono font-bold text-white">{{ app.plate_no }}</div>
                    <div class="text-[10px] text-slate-400">
                      {{ app.source_car_relationship || (app.is_spouse_car ? '配偶车(结婚证)' : (app.car_relationship === 'FAMILY' ? '直系亲属(户口簿)' : '本人名下')) }}
                    </div>
                  </td>
                  <td class="p-3">
                    <span
                      class="px-2 py-0.5 rounded text-[10px] font-semibold border"
                      :class="app.audit_mode === 'FLEXIBLE_HYBRID' ? 'bg-purple-900/60 text-purple-300 border-purple-500/40' : 'bg-blue-900/60 text-blue-300 border-blue-500/40'"
                    >
                      {{ app.audit_mode_label }}
                    </span>
                  </td>
                  <td class="p-3">
                    <button
                      @click="openViewAttachmentsModal(app)"
                      class="text-blue-400 hover:text-blue-300 px-2.5 py-1 rounded-lg bg-blue-950/40 hover:bg-blue-900/40 border border-blue-800/50 transition text-xs flex items-center gap-1 font-medium"
                    >
                      <span><AppIcon name="review" :size="14" /> {{ isProduction ? (app.legacy_unverified ? '查看历史台账' : '打开审核') : `查验原件资料 (${app.attachments?.length || 3}份)` }}</span>
                    </button>
                  </td>
                  <td class="p-3">
                    <span
                      class="px-2 py-0.5 rounded text-[11px] font-semibold border"
                      :class="getAppStatusBadgeClass(app.status)"
                    >
                      {{ app.status_label }}
                    </span>
                    <div v-if="app.status === 'REJECTED' && app.reject_reason" class="text-[10px] text-rose-400 mt-0.5 truncate max-w-[150px]" :title="app.reject_reason">
                      理由: {{ app.reject_reason }}
                    </div>
                  </td>
                  <td class="p-3 text-slate-400 font-mono text-[11px]">{{ app.apply_time }}</td>
                  <td class="p-3 text-right">
                    <div v-if="app.status === 'PENDING_REVIEW' && !isProduction" class="flex items-center justify-end gap-1.5">
                      <button
                        @click="handleApproveApplication(app)"
                        :disabled="actionLoading"
                        class="px-2.5 py-1 rounded-lg bg-emerald-600 hover:bg-emerald-500 text-white font-bold text-xs transition shadow active:scale-95"
                      >
                        <AppIcon name="check" size="1em" /> 通过
                      </button>
                      <button
                        @click="openRejectModal(app)"
                        :disabled="actionLoading"
                        class="px-2.5 py-1 rounded-lg bg-rose-700 hover:bg-rose-600 text-white font-bold text-xs transition shadow active:scale-95"
                      >
                        <AppIcon name="close" size="1em" /> 驳回
                      </button>
                    </div>
                    <span v-else class="text-slate-500 text-[11px]">{{ isProduction && app.status === 'PENDING_REVIEW' ? '请打开审核' : '无需操作' }}</span>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>

        <!-- 子 Tab 2: 申报机制与特定业主特例通道 (低频配置，排在后面) -->
        <div v-if="!isProduction" v-show="auditSubTab === 'strategy'" class="space-y-4">
          <!-- 审核方式配置卡片 (全局基础策略) -->
          <div class="bg-slate-800 rounded-3xl p-4 sm:p-6 border border-slate-700 space-y-4">
            <div class="flex items-center justify-between flex-wrap gap-2 border-b border-slate-700 pb-3">
              <div>
                <h3 class="text-sm font-bold text-white flex items-center gap-2">
                  <span><AppIcon name="settings" size="1em" /> 小区车位申请审核方式配置 (全局基础策略)</span>
                  <span class="text-[10px] px-2.5 py-0.5 rounded-full font-bold border"
                    :class="currentAuditMode === 'FLEXIBLE_HYBRID' ? 'bg-purple-900/60 text-purple-300 border-purple-500/40' : 'bg-emerald-900/60 text-emerald-300 border-emerald-500/40'"
                  >
                    当前生效：{{ currentAuditMode === 'FLEXIBLE_HYBRID' ? '灵活配置（AI 自动化 + 人工审核）' : '三证合一（AI 自动化审核）' }}
                  </span>
                </h3>
                <p class="text-xs text-slate-400 mt-1">
                  支持小区维度自主配置。📌 <strong>重要规则：配置变更仅对新申请生效，存量申请保持原有审核模式不变。</strong>
                </p>
              </div>
              <span class="text-[11px] text-slate-400 font-mono">所属小区: {{ userStore.communityName || '龙华新村' }}</span>
            </div>

            <div class="grid grid-cols-1 md:grid-cols-2 gap-3 sm:gap-4">
              <!-- 模式一: 三证合一 (AI 自动化审核) -->
              <div
                @click="handleChangeAuditMode('THREE_CERT_AUTO')"
                class="p-4 rounded-2xl border-2 transition cursor-pointer flex flex-col justify-between space-y-3"
                :class="currentAuditMode === 'THREE_CERT_AUTO'
                  ? 'border-emerald-500 bg-emerald-950/30 shadow-lg shadow-emerald-950/40 ring-1 ring-emerald-500/30'
                  : 'border-slate-700 bg-slate-900/60 hover:border-slate-600'"
              >
                <div class="space-y-2">
                  <div class="flex items-center justify-between">
                    <div class="flex items-center gap-2">
                      <span class="text-xl"><AppIcon name="zap" size="1em" /></span>
                      <h4 class="font-bold text-white text-sm">三证合一（AI 自动化审核）</h4>
                    </div>
                    <span
                      class="w-5 h-5 rounded-full border-2 flex items-center justify-center text-[10px] font-bold"
                      :class="currentAuditMode === 'THREE_CERT_AUTO' ? 'border-emerald-400 bg-emerald-500 text-white' : 'border-slate-600 text-transparent'"
                    ><AppIcon name="check" size="1em" /></span>
                  </div>
                  <p class="text-xs text-slate-300 leading-relaxed">
                    业主提交申请沿用原有流程。系统自动通过政务跨库（公安/不动产/交管）完成三证合一智能核验，通过后<strong>单据直接进入 FIFO 阳光排位序列</strong>，无需人工介入。
                  </p>
                </div>
                <div class="pt-2 border-t border-slate-700/60 flex items-center justify-between text-[11px]">
                  <span class="text-emerald-400 font-medium">适合：产权清晰规范、追求高自动化小区</span>
                  <span v-if="currentAuditMode === 'THREE_CERT_AUTO'" class="text-emerald-300 font-bold">● 当前生效使用中</span>
                  <span v-else class="text-slate-400 hover:text-white">点击切换启用 <AppIcon name="arrow" size="1em" /></span>
                </div>
              </div>

              <!-- 模式二: 灵活配置 (AI 自动化 + 人工审核) -->
              <div
                @click="handleChangeAuditMode('FLEXIBLE_HYBRID')"
                class="p-4 rounded-2xl border-2 transition cursor-pointer flex flex-col justify-between space-y-3"
                :class="currentAuditMode === 'FLEXIBLE_HYBRID'
                  ? 'border-purple-500 bg-purple-950/30 shadow-lg shadow-purple-950/40 ring-1 ring-purple-500/30'
                  : 'border-slate-700 bg-slate-900/60 hover:border-slate-600'"
              >
                <div class="space-y-2">
                  <div class="flex items-center justify-between">
                    <div class="flex items-center gap-2">
                      <span class="text-xl"><AppIcon name="shield" size="1em" /></span>
                      <h4 class="font-bold text-white text-sm">灵活配置（AI 自动化 + 人工审核）</h4>
                    </div>
                    <span
                      class="w-5 h-5 rounded-full border-2 flex items-center justify-center text-[10px] font-bold"
                      :class="currentAuditMode === 'FLEXIBLE_HYBRID' ? 'border-purple-400 bg-purple-500 text-white' : 'border-slate-600 text-transparent'"
                    ><AppIcon name="check" size="1em" /></span>
                  </div>
                  <p class="text-xs text-slate-300 leading-relaxed">
                    业主提交申请需上传<strong>多张必填材料附件</strong>，先经 AI 基础校验，通过后<strong>流转至物业人工审核池</strong>。物业审核通过则单据进入排位序列；审核驳回时需填写驳回理由并通过站内消息通知业主修改重提。
                  </p>
                </div>
                <div class="pt-2 border-t border-slate-700/60 flex items-center justify-between text-[11px]">
                  <span class="text-purple-400 font-medium">适合：老旧小区历史遗留多、需物业人工复核</span>
                  <span v-if="currentAuditMode === 'FLEXIBLE_HYBRID'" class="text-purple-300 font-bold">● 当前生效使用中</span>
                  <span v-else class="text-slate-400 hover:text-white">点击切换启用 <AppIcon name="arrow" size="1em" /></span>
                </div>
              </div>
            </div>
          </div>

          <!-- 特定业主特例申报通道 (个性化特批名单，支持搜索) -->
          <div class="bg-slate-800 rounded-3xl p-4 sm:p-6 border border-slate-700 space-y-4">
            <div class="flex items-center justify-between flex-wrap gap-2 border-b border-slate-700 pb-3">
              <div class="flex items-center gap-2">
                <span class="text-base"><AppIcon name="target" size="1em" /></span>
                <div>
                  <h4 class="text-sm font-bold text-white flex items-center gap-1.5">
                    <span>特定业主特例申报通道 (个性化特批名单)</span>
                    <span class="text-[10px] bg-indigo-900/60 text-indigo-300 px-2 py-0.5 rounded-full border border-indigo-500/30">
                      支持单设
                    </span>
                  </h4>
                  <p class="text-xs text-slate-400 mt-0.5">
                    针对租户、借用车、单位派车等特殊情形业主，物业可单独为其特批开通【灵活申报通道】，其他业主继续遵循小区默认策略。
                  </p>
                </div>
              </div>
              <button
                type="button"
                @click="loadCommunityUsers"
                class="text-xs text-slate-300 hover:text-white px-3 py-1.5 rounded-xl bg-slate-900 border border-slate-700 hover:border-slate-600 flex items-center gap-1.5 transition shadow"
              >
                <span><AppIcon name="refresh" size="1em" /> 刷新业主名单</span>
              </button>
            </div>

            <!-- 搜索与筛选工具栏 -->
            <div class="flex items-center justify-between flex-wrap gap-2 bg-slate-900/80 p-2.5 rounded-2xl border border-slate-700/80">
              <!-- 实时搜索输入框 -->
              <div class="relative flex-1 min-w-[220px]">
                <span class="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400 text-xs"><AppIcon name="search" size="1em" /></span>
                <input
                  v-model="userSearchKeyword"
                  type="text"
                  placeholder="搜索业主姓名、手机号、房屋坐落 (如: 张欣欣 / 12号楼302室)..."
                  class="w-full pl-8 pr-8 py-2 bg-slate-800 border border-slate-700 rounded-xl text-xs text-white placeholder-slate-500 focus:outline-none focus:border-purple-500 focus:ring-1 focus:ring-purple-500"
                />
                <button
                  v-if="userSearchKeyword"
                  @click="userSearchKeyword = ''"
                  class="absolute right-2.5 top-1/2 -translate-y-1/2 text-slate-400 hover:text-white text-xs"
                 aria-label="关闭"><AppIcon name="close" size="1em" /></button>
              </div>

              <!-- 状态筛选胶囊 -->
              <div class="flex items-center gap-1.5 text-xs flex-wrap">
                <button
                  @click="userStatusFilter = 'ALL'"
                  class="px-2.5 py-1 rounded-lg border transition font-medium text-[11px]"
                  :class="userStatusFilter === 'ALL' ? 'bg-purple-600 text-white border-purple-500' : 'bg-slate-800 text-slate-300 border-slate-700 hover:text-white'"
                >
                  全部 ({{ communityUserList.length }})
                </button>
                <button
                  @click="userStatusFilter = 'CUSTOMIZED'"
                  class="px-2.5 py-1 rounded-lg border transition font-medium text-[11px] flex items-center gap-1"
                  :class="userStatusFilter === 'CUSTOMIZED' ? 'bg-indigo-600 text-white border-indigo-500' : 'bg-slate-800 text-slate-300 border-slate-700 hover:text-white'"
                >
                  <span>★ 仅看物业特批</span>
                  <span v-if="customizedUserCount > 0" class="px-1 rounded bg-white/20 text-white text-[9px] font-bold font-mono">{{ customizedUserCount }}</span>
                </button>
                <button
                  @click="userStatusFilter = 'PENDING'"
                  class="px-2.5 py-1 rounded-lg border transition font-medium text-[11px]"
                  :class="userStatusFilter === 'PENDING' ? 'bg-amber-600 text-white border-amber-500' : 'bg-slate-800 text-slate-300 border-slate-700 hover:text-white'"
                >
                  排队/审核中
                </button>
                <button
                  @click="userStatusFilter = 'RENTED'"
                  class="px-2.5 py-1 rounded-lg border transition font-medium text-[11px]"
                  :class="userStatusFilter === 'RENTED' ? 'bg-emerald-600 text-white border-emerald-500' : 'bg-slate-800 text-slate-300 border-slate-700 hover:text-white'"
                >
                  已签约租用
                </button>
                <button
                  @click="userStatusFilter = 'NO_APPLY'"
                  class="px-2.5 py-1 rounded-lg border transition font-medium text-[11px]"
                  :class="userStatusFilter === 'NO_APPLY' ? 'bg-slate-700 text-white border-slate-600' : 'bg-slate-800 text-slate-300 border-slate-700 hover:text-white'"
                >
                  未发起申请
                </button>
              </div>
            </div>

            <!-- 业主特例卡片列表 -->
            <div class="space-y-2 pt-1">
              <div v-if="filteredCommunityUsers.length === 0" class="p-8 text-center text-slate-500 text-xs bg-slate-900/40 rounded-2xl border border-dashed border-slate-700">
                未检索到符合条件的业主名单，请调整搜索关键词或状态筛选
              </div>
              <div
                v-for="u in filteredCommunityUsers"
                :key="u.id"
                class="p-3 bg-slate-900/70 hover:bg-slate-900/90 rounded-xl border border-slate-700/80 flex items-center justify-between flex-wrap gap-3 text-xs transition"
              >
                <div class="flex items-center gap-2.5 min-w-0">
                  <div class="w-8 h-8 rounded-lg flex items-center justify-center font-bold text-white text-xs shrink-0"
                    :class="u.is_customized ? 'bg-purple-600' : 'bg-slate-700'"
                  >
                    {{ u.real_name ? u.real_name.slice(0, 1) : '业' }}
                  </div>
                  <div class="min-w-0">
                    <div class="flex items-center gap-2 flex-wrap">
                      <span class="font-bold text-white">{{ u.real_name }}</span>
                      <span class="text-[10px] text-slate-400 font-mono">{{ u.phone }}</span>
                      <span v-if="u.property_no && u.property_no !== '--'" class="text-[10px] text-slate-300 bg-slate-800 px-1.5 py-0.2 rounded border border-slate-700">
                        {{ u.property_no }}
                      </span>
                      <span class="text-[10px] px-1.5 py-0.2 rounded font-medium border"
                        :class="u.status_tag === '已签约租用' ? 'bg-emerald-950/80 text-emerald-300 border-emerald-800/40' : (u.status_tag === '排队/审核中' ? 'bg-amber-950/80 text-amber-300 border-amber-800/40' : 'bg-slate-800 text-slate-400 border-slate-700')"
                      >
                        {{ u.status_tag }}
                      </span>
                    </div>
                    <div class="text-[10px] text-slate-400 flex items-center gap-2 mt-0.5">
                      <span>当前生效流程:</span>
                      <strong :class="u.effective_audit_mode === 'FLEXIBLE_HYBRID' ? 'text-purple-400' : 'text-emerald-400'">
                        {{ u.effective_audit_mode === 'FLEXIBLE_HYBRID' ? '灵活申报 (2步自主材料)' : '标准申报 (3步规范准入)' }}
                      </strong>
                      <span v-if="u.is_customized" class="text-amber-400 font-bold bg-amber-950/40 px-1 rounded border border-amber-800/30">
                        ★ 物业单独特批
                      </span>
                      <span v-else class="text-slate-500">
                        (继承小区策略)
                      </span>
                    </div>
                  </div>
                </div>

                <!-- 切换操作按钮 -->
                <div class="flex items-center gap-1.5 shrink-0">
                  <button
                    v-if="u.effective_audit_mode !== 'FLEXIBLE_HYBRID'"
                    type="button"
                    @click="handleSetUserAuditMode(u.id, 'FLEXIBLE_HYBRID')"
                    class="px-2.5 py-1 rounded-lg bg-purple-600/30 hover:bg-purple-600/50 text-purple-200 border border-purple-500/40 font-bold text-[10px] transition flex items-center gap-1"
                  >
                    <span><AppIcon name="zap" size="1em" /> 特批灵活申报</span>
                  </button>
                  <button
                    v-if="u.effective_audit_mode !== 'THREE_CERT_AUTO'"
                    type="button"
                    @click="handleSetUserAuditMode(u.id, 'THREE_CERT_AUTO')"
                    class="px-2.5 py-1 rounded-lg bg-blue-600/30 hover:bg-blue-600/50 text-blue-200 border border-blue-500/40 font-bold text-[10px] transition flex items-center gap-1"
                  >
                    <span><AppIcon name="shield" size="1em" /> 指定标准申报</span>
                  </button>
                  <button
                    v-if="u.is_customized"
                    type="button"
                    @click="handleSetUserAuditMode(u.id, null)"
                    class="px-2 py-1 rounded-lg bg-slate-800 hover:bg-slate-700 text-slate-400 hover:text-white border border-slate-700 text-[10px] transition"
                    title="清除特例设置，恢复随小区策略"
                  >
                    <span>恢复跟随小区</span>
                  </button>
                </div>
              </div>
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
          <button @click="selectedSpaceModal = false" class="text-slate-400 hover:text-white p-1" aria-label="关闭"><AppIcon name="close" size="1em" /></button>
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
          <div v-if="currentSpace.legacy_unverified" class="rounded-lg border border-amber-700/60 bg-amber-950/40 p-2 text-amber-200">
            历史在租台账 · 业主账号待核验关联；房屋坐落：{{ currentSpace.property_no || '**' }}；身份证号：{{ spaceArchive?.current?.id_card || '**' }}
          </div>
        </div>
        </div>

        <div>
        <!-- 车位档案: 历史承租记录与流转事件 -->
        <div class="bg-slate-900/80 p-3.5 rounded-xl border border-slate-700 text-xs space-y-2">
          <div class="flex items-center justify-between">
            <h4 class="font-bold text-white"><AppIcon name="folder" size="1em" /> 车位档案 · 历史承租记录</h4>
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
            <span class="text-[10px] text-slate-500 font-semibold">审计流转事件:</span>
            <div v-for="(ev, i) in spaceArchive.events" :key="i" class="text-[10px] text-slate-400 flex justify-between gap-2">
              <span class="truncate">{{ ev.summary }}</span>
              <span class="font-mono shrink-0">{{ ev.time.slice(5, 16) }}</span>
            </div>
          </div>
        </div>
        </div>

        <!-- 核心业务操作卡片区 -->
        <div class="space-y-3">
          <!-- 场景 A: 物业代清退 (仅在租时可用) -->
          <div v-if="currentSpace.status === 'RENTED'" class="p-3 bg-rose-950/40 border border-rose-800/60 rounded-xl space-y-2">
            <h4 class="text-xs font-bold text-rose-300 flex items-center gap-1.5">
              <span>物业清退与退租办理</span>
            </h4>
            <p class="text-[11px] text-slate-400 leading-relaxed">
              用于老业主房屋买卖过户、失联违规占用等场景。清退后车位立即释放回池，<strong>系统排位引擎将秒级触发顺延，后续业主排位实时前进 1 位</strong>！
            </p>

            <div class="space-y-2 text-xs pt-1">
              <CustomSelect
                v-model="terminateReason"
                :options="terminateReasonOptions"
                :dark="true"
              />
              <input v-if="isProduction" v-model.trim="terminationReference" maxlength="100"
                placeholder="线下清退办理凭据编号"
                class="w-full rounded-lg bg-slate-900 border border-slate-600 px-3 py-2.5 text-white" />

              <button
                @click="handleForceTerminate"
                :disabled="actionLoading"
                class="w-full py-2.5 rounded-xl bg-rose-600 hover:bg-rose-700 text-white font-bold text-xs shadow-md transition active:scale-[0.98] flex items-center justify-center gap-1"
              >
                <span>执行清退并记入审计记录</span>
              </button>
            </div>
          </div>

          <!-- 场景 B: 线下签约完成交付 (仅中签锁定/办理中时可用) -->
          <div v-if="currentSpace.status === 'ALLOCATED' || currentSpace.status === 'OFFLINE_PENDING'" class="p-3 bg-amber-950/40 border border-amber-800/60 rounded-xl space-y-2.5">
            <div class="flex items-center justify-between">
              <h4 class="text-xs font-bold text-amber-300 flex items-center gap-1.5">
                <span><AppIcon name="edit" size="1em" /> 线下履约交付确认</span>
              </h4>
              <span class="text-[10px] text-amber-400 bg-amber-900/60 px-2 py-0.5 rounded border border-amber-500/30">
                待签约交付
              </span>
            </div>
            <p class="text-[11px] text-slate-400 leading-relaxed">
              请先核对纸质合同与线下缴费凭据，确认后车位转为“已租”并生成系统审计记录。
            </p>

            <!-- 签约租期时长选择 -->
            <div>
              <div class="flex items-center justify-between mb-1.5">
                <label class="block text-[11px] text-slate-300 font-bold">签约租期时长：</label>
                <span class="text-[10px] text-amber-300 font-medium">默认半年 (支持自选)</span>
              </div>
              <div class="grid grid-cols-2 sm:grid-cols-4 gap-1.5">
                <button
                  v-for="term in deliveryTerms"
                  :key="term.months"
                  type="button"
                  @click="selectedDeliveryMonths = term.months"
                  class="p-2 rounded-lg border text-center transition cursor-pointer"
                  :class="selectedDeliveryMonths === term.months
                    ? 'border-amber-400 bg-amber-500/20 text-amber-200 font-bold ring-1 ring-amber-400/50'
                    : 'border-slate-700 bg-slate-900/80 text-slate-400 hover:text-slate-200'"
                >
                  <div class="text-xs">{{ term.label }}</div>
                  <div class="text-[9px] opacity-70">{{ term.desc }}</div>
                </button>
              </div>
            </div>

            <!-- 动态预览起止日期 -->
            <div class="p-2 bg-slate-900/90 border border-slate-700/80 rounded-lg flex items-center justify-between text-[11px]">
              <span class="text-slate-400">预计租期起止：</span>
              <span class="font-mono text-amber-300 font-bold">{{ todayStr }} ~ {{ calculatedDeliveryEndDate }}</span>
            </div>

            <div v-if="!isProduction">
              <label class="block text-[11px] text-slate-300 font-bold mb-1.5">合同签署证明：</label>
              <label class="flex items-center justify-between gap-2 w-full px-3 py-2 rounded-lg border border-dashed border-amber-500/40 bg-slate-900/80 cursor-pointer hover:border-amber-400 transition">
                <input type="file" accept="image/*,.pdf" class="hidden" @change="onPickDeliveryProof" />
                <span class="text-[11px] truncate" :class="deliveryProofName ? 'text-amber-200 font-medium' : 'text-slate-500'">
                  {{ deliveryProofName || '点击上传合同签署扫描件 / 照片' }}
                </span>
                <span class="text-[10px] text-amber-400 shrink-0">支持文件上传</span>
              </label>
            </div>
            <div v-if="isProduction" class="space-y-2 text-xs">
              <label class="block text-slate-300 font-bold">线下缴费凭据编号</label>
              <input v-model.trim="deliveryReceiptNo" required maxlength="100" placeholder="填写已核实的收据或流水编号"
                class="w-full rounded-lg bg-slate-900 border border-slate-600 px-3 py-2.5 text-white" />
              <label class="flex items-center gap-2 text-amber-200">
                <input v-model="deliveryPaymentConfirmed" type="checkbox" class="accent-amber-500" />
                已核对纸质合同及缴费凭据
              </label>
            </div>

            <button
              @click="handleConfirmDelivery"
              :disabled="actionLoading"
              class="w-full py-2.5 rounded-xl bg-amber-600 hover:bg-amber-700 text-white font-bold text-xs shadow-md transition active:scale-[0.98]"
            >
              <AppIcon name="check" size="1em" /> 线下签约已完成，确认正式交付
            </button>
          </div>

          <!-- 场景 C: 直接置为维修状态 (只要非维修中，无论当前是空闲、已租或中签，均可直接设为维修状态) -->
          <div v-if="currentSpace.status !== 'MAINTENANCE'" class="p-3 bg-slate-900/90 border border-amber-500/40 rounded-xl space-y-2.5">
            <div class="flex items-center justify-between">
              <h4 class="text-xs font-bold text-amber-300 flex items-center gap-1.5">
                <span><AppIcon name="wrench" size="1em" /> 车位报修维护 · 直接进入维修状态</span>
              </h4>
              <span class="text-[10px] text-amber-400 bg-amber-950/80 px-2 py-0.5 rounded border border-amber-500/30">
                当前: {{ getStatusLabel(currentSpace.status) }}
              </span>
            </div>
            <p class="text-[11px] text-slate-400 leading-relaxed">
              支持将该车位直接转入<strong>维修停用</strong>状态。若该车位当前存在中签锁定，系统将自动退回排队池调度，保障车位资产安全。
            </p>
            <div class="space-y-2 text-xs pt-1">
              <div>
                <label class="block text-[11px] text-slate-400 mb-1">报修维护原因：</label>
                <CustomSelect
                  v-model="maintenanceReason"
                  :options="maintenanceReasonOptions"
                  :dark="true"
                />
              </div>
              <div v-if="!isProduction">
                <label class="block text-[11px] text-slate-300 font-bold mb-1.5">证明：</label>
                <label class="flex items-center justify-between gap-2 w-full px-3 py-2 rounded-lg border border-dashed border-amber-500/40 bg-slate-900/80 cursor-pointer hover:border-amber-400 transition">
                  <input type="file" accept="image/*,.pdf" class="hidden" @change="onPickMaintenanceProof" />
                  <span class="text-[11px] truncate" :class="maintenanceProofName ? 'text-amber-200 font-medium' : 'text-slate-500'">
                    {{ maintenanceProofName || '点击上传维修现场 / 报修凭证' }}
                  </span>
                  <span class="text-[10px] text-amber-400 shrink-0">支持文件上传</span>
                </label>
              </div>
              <p v-if="isProduction" class="text-[11px] text-slate-400">维修照片和报修凭据请由物业在线下台账留存。</p>
              <button
                @click="handleToggleMaintenance('SET_MAINTENANCE')"
                :disabled="actionLoading"
                class="w-full py-2.5 rounded-xl bg-amber-600 hover:bg-amber-700 text-white font-bold text-xs shadow-md transition active:scale-[0.98] flex items-center justify-center gap-1.5"
              >
                <span><AppIcon name="wrench" size="1em" /> 确认：直接将该车位设为维修状态</span>
              </button>
            </div>
          </div>

          <!-- 场景 D: 维修验收恢复上线 (当前为维修中时可用) -->
          <div v-else class="p-3 bg-emerald-950/40 border border-emerald-800/60 rounded-xl space-y-2.5">
            <div class="flex items-center justify-between">
              <h4 class="text-xs font-bold text-emerald-300 flex items-center gap-1.5">
                <span><AppIcon name="success" size="1em" /> 维修验收完毕 · 恢复上线投入使用</span>
              </h4>
              <span class="text-[10px] text-rose-400 bg-rose-950/80 px-2 py-0.5 rounded border border-rose-500/30">维修停用中</span>
            </div>
            <p class="text-[11px] text-slate-400 leading-relaxed">
              车位施工检修已通过物业验收！点击恢复可用后，车位将重新释放回池，<strong>调度引擎将秒级触发自动排位，为轮候首位业主锁定该车位</strong>！
            </p>
            <button
              @click="handleToggleMaintenance('RESTORE')"
              :disabled="actionLoading"
              class="w-full py-2.5 rounded-xl bg-emerald-600 hover:bg-emerald-700 text-white font-bold text-xs shadow-md transition active:scale-[0.98] flex items-center justify-center gap-1.5"
            >
              <span><AppIcon name="zap" size="1em" /> 检修完成，恢复可用 (触发顺位自动分配)</span>
            </button>
          </div>

          <!-- 弹窗底部操作栏 -->
          <div class="flex items-center justify-end pt-2">
            <button
              @click="selectedSpaceModal = false"
              class="px-5 py-2 rounded-xl bg-slate-700 hover:bg-slate-600 text-slate-200 text-xs font-semibold transition"
            >
              关闭窗口
            </button>
          </div>
        </div>
        </div>
      </div>
    </div>

    <!-- 快捷登记车位维修 Modal (支持选择小区任意车位直接设为维修) -->
    <div v-if="quickMaintenanceModal" class="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/70 backdrop-blur-sm animate-fade-in">
      <div class="bg-slate-800 rounded-3xl max-w-md w-full p-6 text-slate-100 shadow-2xl border border-slate-700 space-y-4">
        <div class="flex items-center justify-between border-b border-slate-700 pb-3">
          <div class="flex items-center gap-2">
            <span class="w-8 h-8 rounded-lg bg-amber-600 text-white flex items-center justify-center font-bold text-base"><AppIcon name="wrench" size="1em" /></span>
            <div>
              <h3 class="text-sm font-bold text-white">选择车位直接进入维修状态</h3>
              <p class="text-xs text-slate-400">支持将小区任意车位一键置为维修停用</p>
            </div>
          </div>
          <button @click="quickMaintenanceModal = false" class="text-slate-400 hover:text-white p-1" aria-label="关闭"><AppIcon name="close" size="1em" /></button>
        </div>

        <div class="space-y-3 text-xs">
          <div>
            <label class="block text-slate-300 font-bold mb-1">选择车位：</label>
            <CustomSelect
              v-model="quickMaintenanceSpaceId"
              :options="quickMaintenanceSpaceOptions"
              placeholder="请选择需要设为维修状态的车位..."
              :dark="true"
              btn-class="py-2.5 rounded-xl font-mono"
            />
            <p class="text-[10px] text-slate-400 mt-1">支持选择本小区任意车位直接进入维修状态。修缮验收完成后可随时一键恢复上线。</p>
          </div>

          <div>
            <label class="block text-slate-300 font-bold mb-1">报修原因：</label>
            <CustomSelect
              v-model="quickMaintenanceReason"
              :options="quickMaintenanceReasonOptions"
              :dark="true"
              btn-class="py-2.5 rounded-xl"
            />
          </div>

          <div class="p-3 bg-amber-950/40 border border-amber-800/40 rounded-xl space-y-1">
            <span class="text-[11px] font-bold text-amber-300 block"><AppIcon name="info" size="1em" /> 物业运维说明</span>
            <p class="text-[10px] text-slate-400 leading-relaxed">
              车位设为维修停用后，系统排位引擎将暂停向该车位分配；修缮完毕后点击“恢复可用”，调度引擎将严格依据 FIFO 公正自动分发给轮候首位业主。
            </p>
          </div>

          <div class="flex items-center gap-2 pt-2">
            <button
              @click="quickMaintenanceModal = false"
              class="flex-1 py-2.5 rounded-xl bg-slate-700 hover:bg-slate-600 text-slate-300 font-semibold"
            >
              取消
            </button>
            <button
              @click="handleConfirmQuickMaintenance"
              :disabled="!quickMaintenanceSpaceId || actionLoading"
              class="flex-1 py-2.5 rounded-xl bg-amber-600 hover:bg-amber-700 text-white font-bold disabled:opacity-40"
            >
              <span v-if="actionLoading">提交中...</span>
              <span v-else>确认设为维修停用</span>
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- 驳回理由确认弹窗 Modal -->
    <div v-if="rejectModalVisible" class="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/70 backdrop-blur-sm animate-fade-in">
      <div class="bg-slate-800 rounded-3xl max-w-md w-full p-6 text-slate-100 shadow-2xl border border-slate-700 space-y-4">
        <div class="flex items-center justify-between border-b border-slate-700 pb-3">
          <div class="flex items-center gap-2">
            <span class="w-8 h-8 rounded-lg bg-rose-600 text-white flex items-center justify-center font-bold text-sm"><AppIcon name="close" size="1em" /></span>
            <div>
              <h3 class="text-sm font-bold text-white">审核驳回确认</h3>
              <p class="text-xs text-slate-400">驳回理由将以系统消息实时通知业主</p>
            </div>
          </div>
          <button @click="rejectModalVisible = false" class="text-slate-400 hover:text-white p-1" aria-label="关闭"><AppIcon name="close" size="1em" /></button>
        </div>

        <div v-if="rejectTargetApp" class="bg-slate-900/80 p-3 rounded-xl border border-slate-700 text-xs space-y-1.5">
          <div class="flex justify-between">
            <span class="text-slate-400">申请业主:</span>
            <span class="font-bold text-white">{{ rejectTargetApp.applicant_name }} ({{ rejectTargetApp.applicant_phone }})</span>
          </div>
          <div class="flex justify-between">
            <span class="text-slate-400">申报房屋:</span>
            <span class="text-slate-200">{{ rejectTargetApp.property_no }}</span>
          </div>
          <div class="flex justify-between">
            <span class="text-slate-400">申请车牌:</span>
            <span class="font-mono text-white font-bold">{{ rejectTargetApp.plate_no }}</span>
          </div>
        </div>

        <div class="space-y-2 text-xs">
          <label class="block text-slate-300 font-bold">快捷选择驳回理由：</label>
          <CustomSelect
            v-model="rejectReasonPreset"
            :options="rejectReasonOptions"
            :dark="true"
            btn-class="py-2 text-xs"
            @update:modelValue="(val) => { rejectReason = val }"
          />

          <label class="block text-slate-300 font-bold pt-1">驳回详细说明 (必填)：</label>
          <textarea
            v-model="rejectReason"
            rows="3"
            placeholder="请详细说明材料不符合的具体原因，方便业主重新调整后提交..."
            class="w-full px-3 py-2 bg-slate-900 border border-slate-700 rounded-xl text-white text-xs placeholder-slate-500 focus:outline-none focus:border-rose-500 focus:ring-1 focus:ring-rose-500 resize-none"
          ></textarea>
          <p class="text-[10px] text-slate-400">提示：驳回后业主可在移动端查看该理由，并修改材料后一键重新发起申报。</p>
        </div>

        <div class="flex items-center gap-3 pt-2">
          <button
            @click="rejectModalVisible = false"
            class="flex-1 py-2.5 rounded-xl bg-slate-700 hover:bg-slate-600 text-slate-200 text-xs font-semibold transition"
          >
            取消
          </button>
          <button
            @click="handleConfirmReject"
            :disabled="!rejectReason.trim() || actionLoading"
            class="flex-1 py-2.5 rounded-xl bg-rose-600 hover:bg-rose-700 text-white font-bold text-xs shadow-md transition disabled:opacity-40"
          >
            <span v-if="actionLoading">提交中...</span>
            <span v-else>确认驳回并通知</span>
          </button>
        </div>
      </div>
    </div>

    <!-- 查看与查验证明材料附件原件 Modal (大屏高保真政务与证照文档查验中心) -->
    <div v-if="!isProduction && viewAttachmentsModalVisible" class="fixed inset-0 z-50 flex items-center justify-center p-2 sm:p-4 bg-black/85 backdrop-blur-md animate-fade-in">
      <div class="bg-slate-900 rounded-3xl max-w-5xl w-full max-h-[92vh] flex flex-col text-slate-100 shadow-2xl border border-slate-700 overflow-hidden">
        <!-- 弹窗 Header -->
        <div class="flex items-center justify-between px-5 py-3.5 border-b border-slate-700/80 bg-slate-850">
          <div class="flex items-center gap-3">
            <span class="w-9 h-9 rounded-xl bg-gradient-to-br from-blue-600 to-indigo-600 text-white flex items-center justify-center font-bold text-lg shadow-md">
              <AppIcon name="folder" size="1em" />
            </span>
            <div>
              <div class="flex items-center gap-2 flex-wrap">
                <h3 class="text-sm font-bold text-white">申报证明材料原件在线查验台</h3>
                <span
                  class="px-2 py-0.5 rounded text-[10px] font-bold border"
                  :class="viewAttachmentsApp?.audit_mode === 'FLEXIBLE_HYBRID' ? 'bg-purple-900/60 text-purple-300 border-purple-500/40' : 'bg-emerald-900/60 text-emerald-300 border-emerald-500/40'"
                >
                  {{ isProduction ? '物业人工核验' : (viewAttachmentsApp?.audit_mode === 'FLEXIBLE_HYBRID' ? '灵活申报 · 物业人工核验' : '标准申报 · AI智能核验') }}
                </span>
                <span
                  class="px-2 py-0.5 rounded text-[10px] font-bold border"
                  :class="getAppStatusBadgeClass(viewAttachmentsApp?.status)"
                >
                  {{ viewAttachmentsApp?.status_label }}
                </span>
              </div>
              <p class="text-xs text-slate-400 mt-0.5 font-mono">
                申请人: <strong class="text-slate-200">{{ viewAttachmentsApp?.applicant_name }}</strong> ({{ viewAttachmentsApp?.applicant_phone }}) · 房屋: {{ viewAttachmentsApp?.property_no }} · 车牌: {{ viewAttachmentsApp?.plate_no }}
              </p>
            </div>
          </div>
          <button
            @click="viewAttachmentsModalVisible = false"
            class="w-8 h-8 rounded-full hover:bg-slate-800 text-slate-400 hover:text-white flex items-center justify-center transition"
           aria-label="关闭">
            <AppIcon name="close" size="1em" />
          </button>
        </div>

        <!-- 弹窗 Body: 双栏布局 (左: 材料清单导航, 右: 高保真图文文档展示视窗) -->
        <div class="flex-1 overflow-hidden grid grid-cols-1 md:grid-cols-12 min-h-0">
          <!-- 左侧: 证明材料导航清单 (md:col-span-4) -->
          <div class="md:col-span-4 border-r border-slate-700/80 bg-slate-950/60 p-3 sm:p-4 overflow-y-auto space-y-2">
            <div class="flex items-center justify-between text-xs text-slate-400 mb-2 px-1">
              <span class="font-bold text-slate-300">提交证明材料 ({{ currentAttachments.length }} 份)</span>
              <span class="text-[10px] text-emerald-400">● 验真通过</span>
            </div>

            <div
              v-for="(att, idx) in currentAttachments"
              :key="idx"
              @click="selectAttachment(idx)"
              class="p-3 rounded-xl border transition-all cursor-pointer select-none space-y-1"
              :class="activeAttachmentIndex === idx
                ? 'bg-purple-950/50 border-purple-500 shadow-md ring-1 ring-purple-500/50'
                : 'bg-slate-900/80 border-slate-800 hover:border-slate-700'"
            >
              <div class="flex items-center justify-between gap-1">
                <div class="flex items-center gap-2 min-w-0">
                  <span class="text-base">{{ getAttachmentIcon(att) }}</span>
                  <span class="font-bold text-xs truncate" :class="activeAttachmentIndex === idx ? 'text-white' : 'text-slate-300'">
                    {{ att.title || att.name }}
                  </span>
                </div>
                <span class="text-[9px] px-1.5 py-0.2 rounded bg-emerald-950 text-emerald-400 border border-emerald-800/40 shrink-0">
                  <AppIcon name="check" size="1em" /> 已验真
                </span>
              </div>
              <div class="text-[10px] text-slate-400 flex items-center justify-between font-mono pl-6">
                <span class="truncate">{{ att.file_name || '资料扫描件' }}</span>
                <span class="text-slate-500 shrink-0 ml-1">{{ att.file_size || '2.4 MB' }}</span>
              </div>
            </div>

            <div class="mt-4 p-3 rounded-xl bg-blue-950/30 border border-blue-900/40 text-[10px] text-slate-400 space-y-1">
              <div class="text-blue-300 font-bold flex items-center gap-1">
                <span><AppIcon name="shield" size="1em" /> 政务与隐私合规保障</span>
              </div>
              <p class="leading-relaxed">
                依据政务最小化原则，所有证件照片在端侧 1 秒完成拟真特征提取并生成防伪数字指纹，服务器存储加密结构化印鉴凭证，确保个人隐私合规。
              </p>
            </div>
          </div>

          <!-- 右侧: 高保真图文文档展示台 (md:col-span-8) -->
          <div class="md:col-span-8 bg-slate-900 p-4 sm:p-5 overflow-y-auto space-y-3">
            <!-- 顶栏工具条 -->
            <div class="flex items-center justify-between flex-wrap gap-2 pb-2 border-b border-slate-800 text-xs">
              <div class="flex items-center gap-2">
                <span class="text-base">{{ getAttachmentIcon(activeAttachment) }}</span>
                <span class="font-bold text-white text-sm">{{ activeAttachment?.title || activeAttachment?.name }}</span>
                <span class="text-[10px] px-2 py-0.5 rounded bg-slate-800 text-slate-300 border border-slate-700">
                  {{ activeAttachment?.type_label || '官方核验原件' }}
                </span>
              </div>
              <div class="flex items-center gap-2 text-[11px] text-slate-400 font-mono">
                <span class="text-emerald-400 flex items-center gap-1">
                  <span class="w-2 h-2 rounded-full bg-emerald-400 animate-pulse"></span>
                  <span>国家政务链上防伪存证通过</span>
                </span>
              </div>
            </div>

            <!-- 动态渲染证照原件图文卡片 -->
            <!-- 场景 1: 机动车行驶证 (正副双页 + 红色交警年检印章) -->
            <div
              v-if="isVehicleLicense(activeAttachment)"
              class="rounded-2xl border border-slate-700 bg-gradient-to-b from-slate-800 to-slate-850 p-4 sm:p-5 shadow-xl space-y-4"
            >
              <div class="flex items-center justify-between border-b border-slate-700/60 pb-2.5">
                <div class="flex items-center gap-2">
                  <span class="w-6 h-6 rounded-full bg-blue-600/30 border border-blue-500/50 flex items-center justify-center text-xs"><AppIcon name="car" size="1em" /></span>
                  <h4 class="font-bold text-white text-xs tracking-wider">中华人民共和国机动车行驶证</h4>
                </div>
                <span class="text-[10px] font-mono text-slate-400">交管电子印鉴证照 · 正副双页</span>
              </div>

              <!-- 行驶证卡片正副页双栏 -->
              <div class="grid grid-cols-1 lg:grid-cols-2 gap-3 text-xs">
                <!-- 正本卡片 -->
                <div class="p-3.5 rounded-xl bg-slate-900/90 border border-slate-700/80 space-y-2 relative overflow-hidden">
                  <div class="absolute -right-6 -bottom-6 w-24 h-24 rounded-full bg-blue-500/5 pointer-events-none"></div>
                  <div class="flex items-center justify-between border-b border-slate-800 pb-1.5">
                    <span class="text-[11px] font-bold text-slate-300">【行驶证 · 正页】</span>
                    <span class="px-2 py-0.5 rounded bg-blue-950 text-blue-300 border border-blue-800/40 text-[10px] font-mono font-bold">
                      {{ viewAttachmentsApp?.plate_no }}
                    </span>
                  </div>
                  <div class="space-y-1.5 text-[11px]">
                    <div class="flex justify-between"><span class="text-slate-400">号牌号码:</span><strong class="text-amber-300 font-mono text-xs">{{ viewAttachmentsApp?.plate_no }}</strong></div>
                    <div class="flex justify-between"><span class="text-slate-400">车辆类型:</span><span class="text-slate-200">小型普通客车</span></div>
                    <div class="flex justify-between"><span class="text-slate-400">所有人:</span><strong class="text-white">{{ viewAttachmentsApp?.applicant_name }}</strong></div>
                    <div class="flex justify-between"><span class="text-slate-400">住址:</span><span class="text-slate-300 truncate max-w-[170px]">上海市徐汇区{{ viewAttachmentsApp?.property_no }}</span></div>
                    <div class="flex justify-between"><span class="text-slate-400">使用性质:</span><span class="text-slate-200">非营运 · 家庭自用</span></div>
                    <div class="flex justify-between"><span class="text-slate-400">品牌型号:</span><span class="text-slate-300 font-mono">大众牌 SVW71413BR</span></div>
                    <div class="flex justify-between"><span class="text-slate-400">车辆识别代号:</span><span class="text-slate-300 font-mono">LSVAA2189MN829102</span></div>
                    <div class="flex justify-between"><span class="text-slate-400">发动机号码:</span><span class="text-slate-300 font-mono">EA211-928103</span></div>
                    <div class="flex justify-between"><span class="text-slate-400">注册日期:</span><span class="text-slate-300 font-mono">2023-05-18</span></div>
                  </div>
                </div>

                <!-- 副本卡片 (带红色合格印章) -->
                <div class="p-3.5 rounded-xl bg-slate-900/90 border border-slate-700/80 space-y-2 relative overflow-hidden flex flex-col justify-between">
                  <div>
                    <div class="flex items-center justify-between border-b border-slate-800 pb-1.5">
                      <span class="text-[11px] font-bold text-slate-300">【行驶证 · 副页】</span>
                      <span class="text-[10px] text-slate-400 font-mono">核定载客: 5人</span>
                    </div>
                    <div class="space-y-1.5 text-[11px] pt-1">
                      <div class="flex justify-between"><span class="text-slate-400">号牌号码:</span><span class="font-mono text-slate-200">{{ viewAttachmentsApp?.plate_no }}</span></div>
                      <div class="flex justify-between"><span class="text-slate-400">总质量:</span><span class="font-mono text-slate-300">1890 kg</span></div>
                      <div class="flex justify-between"><span class="text-slate-400">整备质量:</span><span class="font-mono text-slate-300">1420 kg</span></div>
                      <div class="flex justify-between"><span class="text-slate-400">外廓尺寸:</span><span class="font-mono text-slate-300">4670×1806×1474 mm</span></div>
                      <div class="flex justify-between"><span class="text-slate-400">检验有效期:</span><strong class="text-emerald-400 font-mono">至 2027年05月</strong></div>
                    </div>
                  </div>

                  <!-- 红色检验合格印章 Mockup -->
                  <div class="pt-2 flex items-center justify-between">
                    <div class="font-mono text-[9px] text-slate-500">
                      <div>公安部交管局防伪校验码</div>
                      <div class="tracking-widest font-bold text-slate-400">||| | |||| || ||||| |||</div>
                    </div>
                    <!-- 椭圆红章 -->
                    <div class="border-2 border-rose-500/80 text-rose-500 rounded-full px-2.5 py-1 text-center rotate-[-6deg] shadow-sm bg-rose-500/5 select-none">
                      <div class="text-[8px] font-bold">上海市公安局交通警察总队</div>
                      <div class="text-[9px] font-black tracking-wider">检验合格专用章</div>
                      <div class="text-[8px] font-mono">2027.05 有效</div>
                    </div>
                  </div>
                </div>
              </div>

              <!-- 权威核验底栏 -->
              <div class="p-2.5 rounded-xl bg-emerald-950/40 border border-emerald-800/40 text-emerald-300 text-xs flex items-center justify-between">
                <span class="flex items-center gap-1.5">
                  <span><AppIcon name="check" size="1em" /></span>
                  <span>全国公安交管车辆综合服务系统查验：号牌与车辆所有人完全一致，状态【正常】</span>
                </span>
                <span class="font-mono text-[10px] text-emerald-400">核验置信度: 99.8%</span>
              </div>
            </div>

            <!-- 场景 2: 不动产权证书 (红底金徽公文 + 徐汇规资局登记大红章) -->
            <div
              v-else-if="isPropertyCert(activeAttachment)"
              class="rounded-2xl border border-red-900/60 bg-gradient-to-b from-slate-900 via-stone-900/90 to-slate-900 p-4 sm:p-5 shadow-xl space-y-4 relative overflow-hidden"
            >
              <div class="flex items-center justify-between border-b border-amber-900/40 pb-2.5">
                <div class="flex items-center gap-2">
                  <span class="w-6 h-6 rounded-full bg-amber-600/30 border border-amber-500/50 flex items-center justify-center text-xs"><AppIcon name="building" size="1em" /></span>
                  <h4 class="font-bold text-amber-200 text-xs tracking-wider">中华人民共和国不动产权证书 (电子不动产证原件)</h4>
                </div>
                <span class="text-[10px] font-mono text-amber-400/80">徐汇不动产第 019823 号</span>
              </div>

              <div class="p-4 rounded-xl bg-slate-950/80 border border-amber-900/30 space-y-3 relative">
                <!-- 证书表头 -->
                <div class="text-center pb-2 border-b border-slate-800">
                  <div class="text-xs text-amber-400 font-bold tracking-widest">不动产权第 沪(2022)徐字不动产权第019823号</div>
                  <div class="text-[10px] text-slate-400 mt-0.5">上海市规划和自然资源局 统一制发</div>
                </div>

                <!-- 权属核心数据表 -->
                <div class="grid grid-cols-1 sm:grid-cols-2 gap-x-4 gap-y-2 text-xs">
                  <div class="flex justify-between border-b border-slate-800/80 py-1">
                    <span class="text-slate-400">权利人:</span>
                    <strong class="text-white">{{ viewAttachmentsApp?.applicant_name }}</strong>
                  </div>
                  <div class="flex justify-between border-b border-slate-800/80 py-1">
                    <span class="text-slate-400">共有情况:</span>
                    <span class="text-slate-200">单独所有 (产权100%)</span>
                  </div>
                  <div class="flex justify-between border-b border-slate-800/80 py-1 sm:col-span-2">
                    <span class="text-slate-400">房屋坐落:</span>
                    <strong class="text-emerald-300 font-medium">上海市徐汇区龙华新村 {{ viewAttachmentsApp?.property_no }}</strong>
                  </div>
                  <div class="flex justify-between border-b border-slate-800/80 py-1 sm:col-span-2">
                    <span class="text-slate-400">不动产单元号:</span>
                    <span class="font-mono text-slate-300">310104 002008 GB00012 F00030002</span>
                  </div>
                  <div class="flex justify-between border-b border-slate-800/80 py-1">
                    <span class="text-slate-400">权利类型:</span>
                    <span class="text-slate-200">国有建设用地使用权 / 房屋所有权</span>
                  </div>
                  <div class="flex justify-between border-b border-slate-800/80 py-1">
                    <span class="text-slate-400">权利性质:</span>
                    <span class="text-slate-200">出让 / 住宅商品房</span>
                  </div>
                  <div class="flex justify-between border-b border-slate-800/80 py-1">
                    <span class="text-slate-400">规划用途:</span>
                    <span class="text-slate-200">成套住宅</span>
                  </div>
                  <div class="flex justify-between border-b border-slate-800/80 py-1">
                    <span class="text-slate-400">建筑面积:</span>
                    <span class="font-mono font-bold text-slate-200">89.62 ㎡ (套内 76.35 ㎡)</span>
                  </div>
                </div>

                <!-- 附记与官方大红公章 -->
                <div class="pt-2 flex items-center justify-between flex-wrap gap-2">
                  <div class="text-[10px] text-slate-400 space-y-0.5 max-w-[280px]">
                    <div>附记: 住宅专项维修资金已结清；土地使用期限至2074年03月。</div>
                    <div class="text-emerald-400">状态: 现房有效，无司法查封、无涉案抵押。</div>
                  </div>

                  <!-- 红色公章 -->
                  <div class="border-2 border-rose-600/80 text-rose-500 rounded-full w-24 h-24 flex flex-col items-center justify-center p-1 text-center rotate-[-8deg] bg-rose-600/5 select-none shadow-sm shrink-0">
                    <span class="text-[7px] font-bold">上海市规划和自然资源局</span>
                    <span class="text-[9px] font-black my-0.5">不动产登记</span>
                    <span class="text-[8px] font-bold">专用章 (徐汇)</span>
                    <span class="text-[7px] font-mono mt-0.5">★ 310104 ★</span>
                  </div>
                </div>
              </div>

              <!-- 权威核验底栏 -->
              <div class="p-2.5 rounded-xl bg-emerald-950/40 border border-emerald-800/40 text-emerald-300 text-xs flex items-center justify-between">
                <span class="flex items-center gap-1.5">
                  <span><AppIcon name="check" size="1em" /></span>
                  <span>上海市自然资源与不动产登记数据库查验一致：房屋确属本小区产权住宅</span>
                </span>
                <span class="font-mono text-[10px] text-emerald-400">核验状态: 权属正常</span>
              </div>
            </div>

            <!-- 场景 3: 居民身份证 (国徽面 + 人像面 + CTID 实人核验) -->
            <div
              v-else-if="isIdCard(activeAttachment)"
              class="rounded-2xl border border-slate-700 bg-slate-800/90 p-4 sm:p-5 shadow-xl space-y-4"
            >
              <div class="flex items-center justify-between border-b border-slate-700 pb-2.5">
                <div class="flex items-center gap-2">
                  <span class="w-6 h-6 rounded-full bg-indigo-600/30 border border-indigo-500/50 flex items-center justify-center text-xs"><AppIcon name="identity" size="1em" /></span>
                  <h4 class="font-bold text-white text-xs tracking-wider">中华人民共和国居民身份证 (正反原件扫描件)</h4>
                </div>
                <span class="text-[10px] font-mono text-indigo-300">公安部 CTID 实人人脸比对认证</span>
              </div>

              <div class="grid grid-cols-1 sm:grid-cols-2 gap-3 text-xs">
                <!-- 人像面 -->
                <div class="p-4 rounded-xl bg-gradient-to-br from-slate-900 to-slate-950 border border-slate-700 space-y-2 relative overflow-hidden flex justify-between">
                  <div class="space-y-1.5 min-w-0 flex-1 pr-2 text-[11px]">
                    <div class="text-[10px] text-indigo-400 font-bold">【身份证 · 人像面】</div>
                    <div><span class="text-slate-400">姓名: </span><strong class="text-white text-sm">{{ viewAttachmentsApp?.applicant_name }}</strong></div>
                    <div><span class="text-slate-400">性别: </span><span class="text-slate-200">男</span><span class="text-slate-400 ml-3">民族: </span><span class="text-slate-200">汉</span></div>
                    <div><span class="text-slate-400">出生: </span><span class="text-slate-200 font-mono">1988年10月12日</span></div>
                    <div><span class="text-slate-400">住址: </span><span class="text-slate-300">上海市徐汇区{{ viewAttachmentsApp?.property_no }}</span></div>
                    <div class="pt-1"><span class="text-slate-400 block text-[10px]">公民身份号码:</span><strong class="font-mono text-amber-300 text-xs tracking-wider">31010419881012****</strong></div>
                  </div>
                  <!-- 证件照片模拟 -->
                  <div class="w-20 h-24 rounded-lg bg-slate-800 border border-slate-700 flex flex-col items-center justify-center p-1 shrink-0 self-center">
                    <span class="text-3xl"><AppIcon name="user" size="1em" /></span>
                    <span class="text-[9px] text-emerald-400 mt-1 font-bold"><AppIcon name="check" size="1em" /> 实人已核验</span>
                  </div>
                </div>

                <!-- 国徽面 -->
                <div class="p-4 rounded-xl bg-gradient-to-br from-slate-900 to-slate-950 border border-slate-700 space-y-2 relative overflow-hidden flex flex-col justify-between">
                  <div class="space-y-2">
                    <div class="flex items-center gap-2">
                      <span class="text-lg">🇨🇳</span>
                      <div>
                        <div class="text-xs font-bold text-white">中华人民共和国</div>
                        <div class="text-xs font-black tracking-widest text-slate-200">居民身份证</div>
                      </div>
                    </div>
                    <div class="space-y-1 text-[11px] pt-1">
                      <div><span class="text-slate-400">签发机关: </span><span class="text-slate-200">上海市公安局徐汇分局</span></div>
                      <div><span class="text-slate-400">有效期限: </span><span class="text-emerald-300 font-mono">2018.06.20 - 2038.06.20</span></div>
                    </div>
                  </div>
                  <div class="text-[9px] text-slate-500 font-mono border-t border-slate-800 pt-2 flex items-center justify-between">
                    <span>二代身份证内置射频防伪芯片</span>
                    <span class="text-emerald-400">● 芯片验签有效</span>
                  </div>
                </div>
              </div>

              <!-- 权威核验底栏 -->
              <div class="p-2.5 rounded-xl bg-emerald-950/40 border border-emerald-800/40 text-emerald-300 text-xs flex items-center justify-between">
                <span class="flex items-center gap-1.5">
                  <span><AppIcon name="check" size="1em" /></span>
                  <span>全国公民身份信息系统 (CTID) 真实性校验核验一致，证件处于法定有效期内</span>
                </span>
                <span class="font-mono text-[10px] text-emerald-400">状态: 有效证件</span>
              </div>
            </div>

            <!-- 场景 4: 夫妻关系证明 / 结婚证 -->
            <div
              v-else-if="isMarriageCert(activeAttachment)"
              class="rounded-2xl border border-rose-900/60 bg-gradient-to-b from-slate-900 via-rose-950/30 to-slate-900 p-4 sm:p-5 shadow-xl space-y-4"
            >
              <div class="flex items-center justify-between border-b border-rose-900/40 pb-2.5">
                <div class="flex items-center gap-2">
                  <span class="w-6 h-6 rounded-full bg-rose-600/30 border border-rose-500/50 flex items-center justify-center text-xs"><AppIcon name="relationship" size="1em" /></span>
                  <h4 class="font-bold text-rose-200 text-xs tracking-wider">中华人民共和国结婚证 (电子婚姻登记证明)</h4>
                </div>
                <span class="text-[10px] font-mono text-rose-400">字号: J310104-2020-006782</span>
              </div>

              <div class="p-4 rounded-xl bg-slate-950/80 border border-rose-900/40 space-y-3">
                <div class="text-center pb-2 border-b border-slate-800">
                  <div class="text-xs text-rose-400 font-bold tracking-widest">民政部全国婚姻登记系统 权威联网出具</div>
                </div>
                <div class="grid grid-cols-1 sm:grid-cols-2 gap-3 text-xs">
                  <div class="space-y-1.5">
                    <div class="flex justify-between"><span class="text-slate-400">持证人:</span><strong class="text-white">{{ viewAttachmentsApp?.applicant_name }}</strong></div>
                    <div class="flex justify-between"><span class="text-slate-400">登记日期:</span><span class="text-slate-200 font-mono">2020年08月18日</span></div>
                    <div class="flex justify-between"><span class="text-slate-400">结婚证字号:</span><span class="text-amber-300 font-mono">J310104-2020-006782</span></div>
                  </div>
                  <div class="flex items-center justify-end">
                    <div class="border-2 border-rose-600 text-rose-500 rounded-full w-20 h-20 flex flex-col items-center justify-center text-center rotate-[-5deg] bg-rose-600/5">
                      <span class="text-[7px] font-bold">上海市徐汇区民政局</span>
                      <span class="text-[8px] font-black">婚姻登记专用章</span>
                      <span class="text-[7px] font-mono">★ (01) ★</span>
                    </div>
                  </div>
                </div>
              </div>

              <div class="p-2.5 rounded-xl bg-emerald-950/40 border border-emerald-800/40 text-emerald-300 text-xs flex items-center justify-between">
                <span><AppIcon name="check" size="1em" /> 全国婚姻登记系统比对属实：夫妻共有车辆准入条件成立，享有配偶优先排位权利</span>
                <span class="font-mono text-[10px] text-emerald-400">配偶核验合格</span>
              </div>
            </div>

            <!-- 场景 5: 房屋租赁合同网签备案证明 -->
            <div
              v-else-if="isRentalCert(activeAttachment)"
              class="rounded-2xl border border-blue-900/60 bg-slate-850 p-4 sm:p-5 shadow-xl space-y-4"
            >
              <div class="flex items-center justify-between border-b border-slate-700 pb-2.5">
                <div class="flex items-center gap-2">
                  <span class="w-6 h-6 rounded-full bg-blue-600/30 border border-blue-500/50 flex items-center justify-center text-xs"><AppIcon name="file-check" size="1em" /></span>
                  <h4 class="font-bold text-white text-xs tracking-wider">上海市住房租赁合同网签备案通知书</h4>
                </div>
                <span class="text-[10px] font-mono text-blue-300">备案编号: SH-RENT-2025-091823</span>
              </div>

              <div class="p-4 rounded-xl bg-slate-900/90 border border-slate-700/80 space-y-2 text-xs">
                <div class="grid grid-cols-1 sm:grid-cols-2 gap-2 text-[11px]">
                  <div class="flex justify-between"><span class="text-slate-400">承租人:</span><strong class="text-white">{{ viewAttachmentsApp?.applicant_name }}</strong></div>
                  <div class="flex justify-between"><span class="text-slate-400">出租人 (房东):</span><span class="text-slate-200">王建国 (产权人)</span></div>
                  <div class="flex justify-between sm:col-span-2"><span class="text-slate-400">租赁房屋坐落:</span><span class="text-slate-200">上海市徐汇区龙华新村 {{ viewAttachmentsApp?.property_no }}</span></div>
                  <div class="flex justify-between"><span class="text-slate-400">租赁期限:</span><span class="text-emerald-300 font-mono font-bold">2025-01-01 至 2026-12-31</span></div>
                  <div class="flex justify-between"><span class="text-slate-400">月租金标准:</span><span class="font-mono text-slate-200">4,500 元/月</span></div>
                </div>
                <div class="pt-2 flex justify-end">
                  <div class="border-2 border-rose-600 text-rose-500 rounded-full px-3 py-1 text-center rotate-[-4deg]">
                    <span class="text-[8px] font-bold block">上海市徐汇区住房保障和房屋管理局</span>
                    <span class="text-[9px] font-black block">住房租赁网签备案专用章</span>
                  </div>
                </div>
              </div>

              <div class="p-2.5 rounded-xl bg-emerald-950/40 border border-emerald-800/40 text-emerald-300 text-xs flex items-center justify-between">
                <span><AppIcon name="check" size="1em" /> 上海市住房租赁公共服务平台电子备案验证一致：租约有效，具备申报资格</span>
                <span class="font-mono text-[10px] text-emerald-400">租约验真有效</span>
              </div>
            </div>

            <!-- 场景 6: 通用规范证明文件 / 自治承诺书 / 单位派车函 (高保真公文扫描件) -->
            <div
              v-else
              class="rounded-2xl border border-slate-700 bg-slate-850 p-4 sm:p-5 shadow-xl space-y-4"
            >
              <div class="flex items-center justify-between border-b border-slate-700 pb-2.5">
                <div class="flex items-center gap-2">
                  <span class="w-6 h-6 rounded-full bg-purple-600/30 border border-purple-500/50 flex items-center justify-center text-xs"><AppIcon name="file" size="1em" /></span>
                  <h4 class="font-bold text-white text-xs tracking-wider">{{ activeAttachment?.title || activeAttachment?.name || '申报证明材料原件' }}</h4>
                </div>
                <span class="text-[10px] font-mono text-purple-300">电子公文扫描件</span>
              </div>

              <!-- 公文正文卡片 -->
              <div class="p-5 rounded-xl bg-slate-900 border border-slate-700/90 text-xs space-y-3">
                <div class="text-center border-b border-slate-800 pb-2">
                  <h5 class="font-bold text-white text-sm tracking-wide">{{ activeAttachment?.title || activeAttachment?.name }}</h5>
                  <span class="text-[10px] text-slate-400 font-mono">关于龙华新村内部车位申报与停放使用合规事项</span>
                </div>
                <div class="space-y-2 text-slate-300 text-[11px] leading-relaxed">
                  <p>
                    申请人 <strong>{{ viewAttachmentsApp?.applicant_name }}</strong> 就申报坐落于 <strong>{{ viewAttachmentsApp?.property_no }}</strong>、车牌号码为 <strong>{{ viewAttachmentsApp?.plate_no }}</strong> 之车位申请，特此提交本项材料并作出如下确认：
                  </p>
                  <p>
                    一、本项材料真实合法有效，不存在任何涂改伪造或虚假借名情形；<br/>
                    二、服从社区自治管理公约及物业管理处关于车位流转排位的统筹安排，服从一车一位规范停放；<br/>
                    三、若房屋发生交易转让或车辆权属变更，承诺在 3 个工作日内主动向物业申报清退。
                  </p>
                </div>
                <div class="pt-4 flex items-center justify-between border-t border-slate-800">
                  <div class="text-[10px] text-slate-400">
                    <div>申报提交时间: <span class="font-mono text-slate-300">{{ viewAttachmentsApp?.apply_time }}</span></div>
                    <div>数字防伪指纹: <span class="font-mono text-indigo-300">0x7d9f...4a12</span></div>
                  </div>
                  <!-- 模拟电子签名与红色指印 -->
                  <div class="flex items-center gap-2">
                    <div class="text-right">
                      <div class="text-[10px] text-slate-400">签署承诺人:</div>
                      <div class="font-serif font-black text-white text-sm italic">{{ viewAttachmentsApp?.applicant_name }}</div>
                    </div>
                    <!-- 红色印泥指纹 -->
                    <div class="w-8 h-10 rounded-full border border-rose-600 bg-rose-600/20 flex items-center justify-center text-rose-500 text-[8px] font-mono rotate-6 select-none shadow-sm">
                      捺印
                    </div>
                  </div>
                </div>
              </div>

              <!-- 权威核验底栏 -->
              <div class="p-2.5 rounded-xl bg-emerald-950/40 border border-emerald-800/40 text-emerald-300 text-xs flex items-center justify-between">
                <span><AppIcon name="check" size="1em" /> 申请材料格式规范、承诺人签名及电子防伪捺印比对完备，符合受理要求</span>
                <span class="font-mono text-[10px] text-emerald-400">待材料核验</span>
              </div>
            </div>
          </div>
        </div>

        <!-- 弹窗 Footer: 查验结论与就地审核快捷操作 -->
        <div class="px-5 py-3.5 border-t border-slate-700/80 bg-slate-850 flex items-center justify-between flex-wrap gap-3">
          <div class="flex items-center gap-2 text-xs text-slate-300">
            <span class="w-2 h-2 rounded-full bg-emerald-400"></span>
            <span v-if="isProduction">请线下核对身份证、房屋与车辆证明原件，再作出审核决定。</span>
            <span v-else>已查验该单据全量证明材料 ({{ currentAttachments.length }}/{{ currentAttachments.length }})，可就地审批：</span>
          </div>

          <!-- 审批操作按钮组 -->
          <div class="flex items-center gap-2">
            <!-- 若该单据处于待审核状态，直接提供审核通过与驳回 -->
            <template v-if="viewAttachmentsApp?.status === 'PENDING_REVIEW'">
              <button
                @click="handleInlineReject(viewAttachmentsApp)"
                :disabled="actionLoading"
                class="px-4 py-2 rounded-xl bg-rose-700 hover:bg-rose-600 text-white font-bold text-xs transition shadow active:scale-95 flex items-center gap-1"
              >
                <span><AppIcon name="close" size="1em" /> 审核驳回并通知</span>
              </button>
              <button
                @click="handleInlineApprove(viewAttachmentsApp)"
                :disabled="actionLoading"
                class="px-5 py-2 rounded-xl bg-emerald-600 hover:bg-emerald-500 text-white font-bold text-xs transition shadow active:scale-95 flex items-center gap-1"
              >
                <span><AppIcon name="check" size="1em" /> 查验通过 · 准予入队</span>
              </button>
            </template>
            <!-- 否则仅展示关闭按钮 -->
            <button
              @click="viewAttachmentsModalVisible = false"
              class="px-5 py-2 rounded-xl bg-slate-700 hover:bg-slate-600 text-slate-200 text-xs font-semibold transition"
            >
              关闭查验窗口
            </button>
          </div>
        </div>
      </div>
    </div>
    <div v-if="showOwnerInvite" class="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/70">
      <form @submit.prevent="handleIssueOwnerInvite" class="w-full max-w-md rounded-2xl bg-slate-800 border border-slate-600 p-5 space-y-4 text-sm shadow-2xl">
        <div class="flex items-center justify-between">
          <h3 class="font-bold text-white">签发业主注册邀请码</h3>
          <button type="button" @click="closeOwnerInvite" class="text-slate-400 hover:text-white" aria-label="关闭"><AppIcon name="close" size="1em" /></button>
        </div>
        <p class="text-xs text-slate-300 leading-relaxed">请先在线下核对业主身份证明和所属小区，再填写与证件一致的信息。邀请码仅显示一次，有效期 7 天。</p>
        <template v-if="!issuedOwnerInvite">
          <div>
            <label class="block text-xs text-slate-300 mb-1">业主手机号</label>
            <input v-model.trim="invitePhone" required maxlength="11" placeholder="11位手机号"
              class="w-full rounded-xl bg-slate-900 border border-slate-600 px-3 py-2.5 text-white" />
          </div>
          <div>
            <label class="block text-xs text-slate-300 mb-1">业主身份证号</label>
            <input v-model.trim="inviteIdCard" required maxlength="18" placeholder="18位身份证号"
              class="w-full rounded-xl bg-slate-900 border border-slate-600 px-3 py-2.5 text-white" />
          </div>
          <button type="submit" :disabled="actionLoading" class="w-full rounded-xl bg-indigo-600 px-4 py-2.5 font-bold text-white disabled:opacity-50">确认签发</button>
        </template>
        <div v-else class="space-y-2">
          <label class="block text-xs text-emerald-300 font-bold">一次性邀请码，请线下交给对应业主</label>
          <input :value="issuedOwnerInvite" readonly class="w-full rounded-xl bg-slate-900 border border-emerald-500 px-3 py-2.5 text-emerald-200 font-mono select-all" />
          <p class="text-xs text-slate-400">绑定手机号 {{ invitePhone }}；关闭后系统不再展示原码。</p>
        </div>
      </form>
    </div>

    <div v-if="productionReviewVisible && productionReviewApp" class="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/70">
      <div class="w-full max-w-lg max-h-[90vh] overflow-y-auto rounded-2xl bg-slate-800 border border-slate-600 p-5 space-y-4 text-sm shadow-2xl">
        <div class="flex items-center justify-between">
          <h3 class="font-bold text-white">{{ productionReviewApp.legacy_unverified ? '历史排队台账' : '三证材料人工审核' }}</h3>
          <button type="button" @click="productionReviewVisible = false" class="text-slate-400 hover:text-white" aria-label="关闭"><AppIcon name="close" size="1em" /></button>
        </div>
        <div class="rounded-xl bg-slate-900 p-3 text-xs text-slate-300 space-y-1">
          <p>申请人：{{ productionReviewApp.applicant_name }}（{{ productionReviewApp.applicant_phone }}）</p>
          <p>房屋：{{ productionReviewApp.property_no }}</p>
          <p>车牌：{{ productionReviewApp.plate_no }}</p>
          <p>状态：{{ productionReviewApp.status_label }}</p>
          <template v-if="productionReviewApp.legacy_source_rank">
            <p>原排队序号：{{ productionReviewApp.legacy_source_rank }} · 当前顺位：{{ productionReviewApp.current_rank }}</p>
            <p>原申请时间：{{ productionReviewApp.apply_time }}</p>
            <p>原表车辆关系：{{ productionReviewApp.source_car_relationship || '**' }}</p>
            <p>来源：04已有排队，第 {{ productionReviewApp.legacy_source_row }} 行</p>
            <p>备注：{{ productionReviewApp.legacy_notes || '未填写' }}</p>
          </template>
        </div>
        <p v-if="productionReviewApp.legacy_unverified" class="rounded-xl bg-amber-950/40 border border-amber-800/60 p-3 text-xs text-amber-200 leading-relaxed">已按历史在排台账保留顺位，尚未生成可登录账号。请补齐并核实身份后关联账号；不会使用不合规证件号自动创建账号或绕过本人分配车位。</p>
        <ApplicationDocumentList v-else-if="productionReviewApp.attachments?.length" :documents="productionReviewApp.attachments" dark />
        <p v-else class="rounded-xl bg-amber-950/40 border border-amber-800/60 p-3 text-xs text-amber-200 leading-relaxed">此申请未上传线上材料。可按原线下流程核验，或请申请人撤销后重新提交三证材料。</p>
        <p v-if="!productionReviewApp.legacy_unverified" class="text-xs text-slate-300 leading-relaxed">请查看申请材料并核对身份证明、房屋及车辆证明原件，填写核验登记编号后再给出审核结果。材料上传成功不代表核验通过。</p>
        <template v-if="productionReviewApp.status === 'PENDING_REVIEW'">
          <div>
            <label class="block text-xs text-slate-300 mb-1">线下核验登记编号</label>
            <input v-model.trim="auditReference" maxlength="100" placeholder="如 CHECK-2026-001"
              class="w-full rounded-xl bg-slate-900 border border-slate-600 px-3 py-2.5 text-white" />
          </div>
          <label class="flex items-center gap-2 text-xs text-slate-200">
            <input v-model="documentsVerified" type="checkbox" class="accent-emerald-500" />
            我已核对申请人的证明材料原件
          </label>
          <div class="flex gap-2">
            <button type="button" @click="handleProductionReject" class="flex-1 rounded-xl bg-rose-700 px-3 py-2.5 text-white font-semibold">驳回</button>
            <button type="button" @click="handleProductionApprove" :disabled="actionLoading || !documentsVerified || !auditReference"
              class="flex-1 rounded-xl bg-emerald-600 px-3 py-2.5 text-white font-semibold disabled:opacity-50">核验通过并入队</button>
          </div>
        </template>
      </div>
    </div>

    <div v-if="showAddSpace" class="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/70">
      <form @submit.prevent="handleAddSpace" class="w-full max-w-sm rounded-2xl bg-slate-800 border border-slate-600 p-5 space-y-4 text-sm shadow-2xl">
        <div class="flex items-center justify-between">
          <h3 class="font-bold text-white">录入真实车位</h3>
          <button type="button" @click="showAddSpace = false" class="text-slate-400 hover:text-white" aria-label="关闭"><AppIcon name="close" size="1em" /></button>
        </div>
        <div>
          <label class="block text-xs text-slate-300 mb-1">车位编号</label>
          <input v-model.trim="newSpaceNo" required maxlength="32" placeholder="如 A-001"
            class="w-full rounded-xl bg-slate-900 border border-slate-600 px-3 py-2.5 text-white" />
        </div>
        <div>
          <label class="block text-xs text-slate-300 mb-1">车位位置</label>
          <select v-model="newSpaceArea" class="w-full rounded-xl bg-slate-900 border border-slate-600 px-3 py-2.5 text-white">
            <option value="GROUND">地面</option>
            <option value="UNDERGROUND">地下</option>
          </select>
        </div>
        <p class="text-xs text-slate-400">仅录入已核实存在的车位，新增后将进入可分配车位池。</p>
        <button type="submit" :disabled="actionLoading" class="w-full rounded-xl bg-emerald-600 px-4 py-2.5 font-bold text-white disabled:opacity-50">确认录入</button>
      </form>
    </div>
  </div>
</template>

<script setup>
import AppIcon from '../../components/AppIcon.vue'
import ApplicationDocumentList from '../../components/ApplicationDocumentList.vue'
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../../store/user'
import api from '../../api'
import { showConfirm, showToast } from '../../utils/dialog'
import CustomSelect from '../../components/CustomSelect.vue'

const router = useRouter()
const userStore = useUserStore()
const isProduction = import.meta.env.VITE_APP_MODE === 'production'
const showOwnerInvite = ref(false)
const invitePhone = ref('')
const inviteIdCard = ref('')
const issuedOwnerInvite = ref('')

const openOwnerInvite = () => {
  invitePhone.value = ''
  inviteIdCard.value = ''
  issuedOwnerInvite.value = ''
  showOwnerInvite.value = true
}

const closeOwnerInvite = () => {
  showOwnerInvite.value = false
  issuedOwnerInvite.value = ''
  inviteIdCard.value = ''
}

const handleIssueOwnerInvite = async () => {
  actionLoading.value = true
  try {
    const res = await api.post('/property/owner-invites', {
      phone: invitePhone.value,
      id_card: inviteIdCard.value
    })
    issuedOwnerInvite.value = res.data?.invite_code || ''
    if (!issuedOwnerInvite.value) throw new Error('服务器未返回邀请码')
  } catch (err) {
    showToast('签发失败: ' + err.message, 'error')
  } finally {
    actionLoading.value = false
  }
}
const productionReviewVisible = ref(false)
const productionReviewApp = ref(null)
const auditReference = ref('')
const documentsVerified = ref(false)

const handleProductionApprove = async () => {
  if (!productionReviewApp.value || !documentsVerified.value || !auditReference.value.trim()) return
  actionLoading.value = true
  try {
    await api.post('/property/audit-application', {
      app_id: productionReviewApp.value.id,
      action: 'APPROVE',
      documents_verified: true,
      audit_reference: auditReference.value.trim()
    })
    showToast('审核通过，申请已进入轮候队列', 'success')
    productionReviewVisible.value = false
    await loadData()
  } catch (err) {
    showToast('审核失败: ' + err.message, 'error')
  } finally {
    actionLoading.value = false
  }
}

const handleProductionReject = () => {
  productionReviewVisible.value = false
  openRejectModal(productionReviewApp.value)
}
const showAddSpace = ref(false)
const newSpaceNo = ref('')
const newSpaceArea = ref('GROUND')

const handleAddSpace = async () => {
  actionLoading.value = true
  try {
    await api.post('/property/spaces', { space_no: newSpaceNo.value, area_type: newSpaceArea.value })
    showToast('车位录入成功', 'success')
    showAddSpace.value = false
    newSpaceNo.value = ''
    await loadData()
  } catch (err) {
    showToast('录入失败: ' + err.message, 'error')
  } finally {
    actionLoading.value = false
  }
}

const activePanel = ref('spaces')
const panels = [
  { id: 'kpi', icon: 'trend', label: '看板指标' },
  { id: 'report', icon: 'chart', label: '周转报表' },
  { id: 'spaces', icon: 'parking', label: '车位矩阵' },
  { id: 'audit', icon: 'clipboard-check', label: '审核管理' }
]

// 审核管理子 Tab: 'applications' (台账) | 'strategy' (机制与特定业主特例通道)
const auditSubTab = ref('applications')

// 审核机制与申请审批相关变量
const currentAuditMode = ref('THREE_CERT_AUTO')
const applicationList = ref([])
const auditFilter = ref('ALL')

const pendingAuditCount = computed(() => {
  return applicationList.value.filter(a => a.status === 'PENDING_REVIEW').length
})

// 特定业主特例申报通道相关变量
const userSearchKeyword = ref('')
const userStatusFilter = ref('ALL') // ALL, CUSTOMIZED, PENDING, RENTED, NO_APPLY

const customizedUserCount = computed(() => {
  return communityUserList.value.filter(u => u.is_customized).length
})

const filteredCommunityUsers = computed(() => {
  let list = communityUserList.value || []
  const kw = userSearchKeyword.value.trim().toLowerCase()
  if (kw) {
    list = list.filter(u => {
      const nameMatch = u.real_name && u.real_name.toLowerCase().includes(kw)
      const phoneMatch = u.phone && u.phone.includes(kw)
      const propMatch = u.property_no && u.property_no.toLowerCase().includes(kw)
      return nameMatch || phoneMatch || propMatch
    })
  }
  if (userStatusFilter.value === 'CUSTOMIZED') {
    list = list.filter(u => u.is_customized)
  } else if (userStatusFilter.value === 'PENDING') {
    list = list.filter(u => u.status_tag === '排队/审核中')
  } else if (userStatusFilter.value === 'RENTED') {
    list = list.filter(u => u.status_tag === '已签约租用')
  } else if (userStatusFilter.value === 'NO_APPLY') {
    list = list.filter(u => u.status_tag === '未发起申请')
  }
  return list
})

// 证明材料查验中心相关变量与高保真计算属性
const viewAttachmentsModalVisible = ref(false)
const viewAttachmentsApp = ref(null)
const activeAttachmentIndex = ref(0)

const selectAttachment = (idx) => {
  activeAttachmentIndex.value = idx
}

const currentAttachments = computed(() => {
  if (!viewAttachmentsApp.value) return []
  const app = viewAttachmentsApp.value
  if (app.attachments && app.attachments.length > 0) {
    return app.attachments.map((att, idx) => ({
      ...att,
      id: att.type || `att_${idx}`,
      title: att.title || att.name || '证明附件材料',
      name: att.name || att.title || '证明附件材料',
      file_name: att.file_name || `${att.name || '材料'}.pdf`,
      type: att.type || 'CUSTOM',
      type_label: att.type_label || '证明附件',
      file_size: att.file_size || '2.4 MB'
    }))
  }
  // 智能补全三证合一标准原件供直观查验
  const list = [
    {
      id: 'VEHICLE_LICENSE',
      name: '机动车行驶证正副页扫描件',
      title: '机动车行驶证 (正副双页原件)',
      type: 'VEHICLE_LICENSE',
      type_label: '车辆行驶证',
      file_name: `${app.applicant_name}_行驶证_${app.plate_no}.pdf`,
      file_size: '2.1 MB'
    },
    {
      id: 'PROPERTY_CERT',
      name: '不动产权证书原件扫描件',
      title: '中华人民共和国不动产权证书',
      type: 'PROPERTY_CERT',
      type_label: '不动产权证',
      file_name: `${app.applicant_name}_不动产权证书_${app.property_no}.pdf`,
      file_size: '3.4 MB'
    },
    {
      id: 'ID_CARD',
      name: '居民身份证原件扫描件',
      title: '中华人民共和国居民身份证 (正反原件)',
      type: 'ID_CARD',
      type_label: '居民身份证',
      file_name: `${app.applicant_name}_二代居民身份证.pdf`,
      file_size: '1.8 MB'
    }
  ]
  if (app.is_spouse_car) {
    list.push({
      id: 'MARRIAGE_CERT',
      name: '夫妻关系结婚证明原件',
      title: '中华人民共和国结婚证 (婚姻关系原件)',
      type: 'MARRIAGE_CERT',
      type_label: '婚姻关系证明',
      file_name: `${app.applicant_name}_结婚证_民政核验.pdf`,
      file_size: '1.5 MB'
    })
  } else if (app.car_relationship === 'FAMILY') {
    list.push({
      id: 'FAMILY_CERT',
      name: '常住人口直系亲属居民户口簿',
      title: '居民户口簿 (直系亲属证明)',
      type: 'FAMILY_CERT',
      type_label: '户籍证明',
      file_name: `${app.applicant_name}_常住户口簿.pdf`,
      file_size: '2.0 MB'
    })
  }
  return list
})

const activeAttachment = computed(() => {
  const list = currentAttachments.value
  if (!list.length) return null
  return list[activeAttachmentIndex.value] || list[0]
})

const getAttachmentIcon = (att) => {
  if (!att) return '📄'
  const t = (att.type || '').toUpperCase()
  const n = (att.name || att.title || '').toLowerCase()
  if (t === 'VEHICLE_LICENSE' || n.includes('行驶证') || n.includes('车辆') || n.includes('车牌')) return '🚗'
  if (t === 'PROPERTY_CERT' || n.includes('不动产') || n.includes('房产') || n.includes('产证') || n.includes('产权')) return '🏠'
  if (t === 'ID_CARD' || n.includes('身份证') || n.includes('身份')) return '🪪'
  if (t === 'MARRIAGE_CERT' || n.includes('结婚') || n.includes('配偶')) return '💍'
  if (n.includes('租赁') || n.includes('租房') || n.includes('合同') || n.includes('备案')) return '📑'
  return '📄'
}

const isVehicleLicense = (att) => {
  if (!att) return false
  const t = (att.type || '').toUpperCase()
  const n = (att.name || att.title || '').toLowerCase()
  return t === 'VEHICLE_LICENSE' || n.includes('行驶证') || n.includes('车辆')
}

const isPropertyCert = (att) => {
  if (!att) return false
  const t = (att.type || '').toUpperCase()
  const n = (att.name || att.title || '').toLowerCase()
  return t === 'PROPERTY_CERT' || n.includes('不动产') || n.includes('房产') || n.includes('产权') || n.includes('产证')
}

const isIdCard = (att) => {
  if (!att) return false
  const t = (att.type || '').toUpperCase()
  const n = (att.name || att.title || '').toLowerCase()
  return t === 'ID_CARD' || n.includes('身份证') || n.includes('身份')
}

const isMarriageCert = (att) => {
  if (!att) return false
  const t = (att.type || '').toUpperCase()
  const n = (att.name || att.title || '').toLowerCase()
  return t === 'MARRIAGE_CERT' || n.includes('结婚') || n.includes('配偶')
}

const isRentalCert = (att) => {
  if (!att) return false
  const n = (att.name || att.title || '').toLowerCase()
  return n.includes('租赁') || n.includes('租房') || n.includes('合同') || n.includes('备案')
}

const rejectModalVisible = ref(false)
const rejectTargetApp = ref(null)
const rejectReasonPreset = ref('行驶证照片模糊不清，无法辨识车辆所有人与车牌号')
const rejectReason = ref('行驶证照片模糊不清，无法辨识车辆所有人与车牌号')

const rejectReasonOptions = [
  '行驶证照片模糊不清，无法辨识车辆所有人与车牌号',
  '提交的房屋坐落与不动产登记信息不一致',
  '缺少配偶车辆关系证明（结婚证或同户户籍证明）',
  '车辆所有人非直系亲属，不符合本小区申报标准',
  '附件资料缺失或非有效合法证件'
]

const stats = ref({})
const spaceList = ref([])
const statusFilter = ref('ALL')
const selectedSpaceModal = ref(false)
const currentSpace = ref({})
const terminateReason = ref('房屋买卖过户且原业主失联')
const terminationReference = ref('')
const maintenanceReason = ref('地面沉降破损与路砖平整修补')
const actionLoading = ref(false)

const terminateReasonOptions = [
  '房屋买卖过户且原业主失联',
  '租约逾期未续租且车位被占',
  '业主自愿委托物业代办退租'
]

const maintenanceReasonOptions = [
  '地面沉降破损与路砖平整修补',
  '车位标线模糊翻新与反光漆重涂',
  '智能地锁检修与慢充充电桩线路维护',
  '绿化树木修剪与雨污管道清淤施工',
  '突发管道漏水应急维修抢修'
]

const quickMaintenanceReasonOptions = [
  '地面沉降破损与路砖平整修补',
  '车位标线模糊翻新与反光漆重涂',
  '智能地锁检修与慢充充电桩线路维护',
  '绿化树木修剪与雨污管道清淤施工',
  '突发管道漏水应急维修抢修'
]

// 快捷报修弹窗变量
const quickMaintenanceModal = ref(false)
const quickMaintenanceSpaceId = ref(null)
const quickMaintenanceReason = ref('地面沉降破损与路砖平整修补')

const quickMaintenanceSpaceOptions = computed(() => {
  return spaceList.value.map(sp => ({
    label: `${sp.space_no} (${getStatusLabel(sp.status)}) ${spaceAreaLabel(sp.area_type)}${sp.user_name ? ' [' + sp.user_name + ']' : ''}`,
    value: sp.id
  }))
})

// 线下交付签约租期选项
const deliveryProofName = ref('')
const deliveryReceiptNo = ref('')
const deliveryPaymentConfirmed = ref(false)
const maintenanceProofName = ref('')

const onPickDeliveryProof = (e) => {
  const f = e.target.files && e.target.files[0]
  deliveryProofName.value = f ? f.name : ''
}

const onPickMaintenanceProof = (e) => {
  const f = e.target.files && e.target.files[0]
  maintenanceProofName.value = f ? f.name : ''
}

const selectedDeliveryMonths = ref(6) // 默认半年
const deliveryTerms = [
  { months: 6, label: '半年 (6个月)', desc: '180 天' },
  { months: 12, label: '一年 (12个月)', desc: '365 天' },
  { months: 3, label: '一季度 (3个月)', desc: '90 天' },
  { months: 1, label: '一个月 (短期)', desc: '30 天' }
]

const calculatedDeliveryEndDate = computed(() => {
  const base = new Date()
  const daysMap = { 12: 365, 6: 180, 3: 90, 1: 30 }
  const days = daysMap[selectedDeliveryMonths.value] || (selectedDeliveryMonths.value * 30)
  base.setDate(base.getDate() + days)
  const y = base.getFullYear()
  const m = String(base.getMonth() + 1).padStart(2, '0')
  const d = String(base.getDate()).padStart(2, '0')
  return `${y}-${m}-${d}`
})

const todayStr = computed(() => {
  const d = new Date()
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
})

const filteredSpaces = computed(() => {
  if (statusFilter.value === 'ALL') return spaceList.value
  return spaceList.value.filter(s => s.status === statusFilter.value)
})

const filteredApplications = computed(() => {
  if (auditFilter.value === 'ALL') return applicationList.value
  return applicationList.value.filter(a => a.status === auditFilter.value)
})

const getAppStatusBadgeClass = (status) => {
  switch (status) {
    case 'PENDING_REVIEW':
      return 'bg-amber-900/60 text-amber-300 border-amber-500/40'
    case 'WAITING':
      return 'bg-blue-900/60 text-blue-300 border-blue-500/40'
    case 'ALLOCATED':
      return 'bg-emerald-900/60 text-emerald-300 border-emerald-500/40'
    case 'REJECTED':
      return 'bg-rose-900/60 text-rose-300 border-rose-500/40'
    case 'COMPLETED':
      return 'bg-purple-900/60 text-purple-300 border-purple-500/40'
    default:
      return 'bg-slate-700 text-slate-300 border-slate-600'
  }
}

const loadAuditConfig = async () => {
  try {
    const res = await api.get('/property/audit-config')
    if (res.code === 200 && res.data) {
      currentAuditMode.value = res.data.audit_mode || 'THREE_CERT_AUTO'
    }
  } catch (err) {
    console.error('加载审核配置失败:', err)
  }
}

const communityUserList = ref([])

const loadCommunityUsers = async () => {
  try {
    const res = await api.get('/property/community-users')
    if (res.code === 200 && res.data) {
      communityUserList.value = res.data.users || []
    }
  } catch (err) {
    console.error('加载小区业主名单失败:', err)
  }
}

const handleSetUserAuditMode = async (userId, mode) => {
  actionLoading.value = true
  try {
    const res = await api.post('/property/set-user-audit-mode', {
      user_id: userId,
      audit_mode: mode
    })
    showToast(res.message || '业主申报模式设置成功！', 'success')
    await loadCommunityUsers()
  } catch (err) {
    showToast('设置失败: ' + err.message, 'error')
  } finally {
    actionLoading.value = false
  }
}

const handleChangeAuditMode = async (newMode) => {
  if (newMode === currentAuditMode.value) return
  const modeName = newMode === 'FLEXIBLE_HYBRID' ? '灵活配置（AI 自动化 + 人工审核）' : '三证合一（AI 自动化审核）'
  const confirmed = await showConfirm({
    title: '变更小区审核方式',
    content: `确认将小区车位申请审核方式变更为【${modeName}】吗？\n\n📌 重要规则：本次变更仅对新发起的申请生效，存量排队及在审单据保持原有审核模式不变。`,
    type: 'warning',
    confirmText: '确认变更',
    cancelText: '取消'
  })
  if (!confirmed) return

  actionLoading.value = true
  try {
    const res = await api.post('/property/audit-config', { audit_mode: newMode })
    if (res.code === 200) {
      currentAuditMode.value = newMode
      showToast(res.message || '审核方式配置已更新并上链存证！', 'success')
      await loadData()
    }
  } catch (err) {
    showToast('更新配置失败: ' + err.message, 'error')
  } finally {
    actionLoading.value = false
  }
}

const loadApplications = async () => {
  try {
    const res = await api.get('/property/applications')
    if (res.code === 200) {
      applicationList.value = res.data || []
    }
  } catch (err) {
    console.error('加载申请单据列表失败:', err)
  }
}

const handleApproveApplication = async (app) => {
  const confirmed = await showConfirm({
    title: '人工审核通过确认',
    content: `确认通过业主【${app.applicant_name}】针对房屋【${app.property_no}】、车牌【${app.plate_no}】的车位排队申请吗？\n\n通过后该单据将正式进入车位 FIFO 轮候队列，若当前有空闲车位将自动触发顺位匹配。`,
    type: 'info',
    confirmText: '确认通过',
    cancelText: '取消'
  })
  if (!confirmed) return

  actionLoading.value = true
  try {
    const res = await api.post('/property/audit-application', {
      app_id: app.id,
      action: 'APPROVE'
    })
    if (res.code === 200) {
      showToast(res.message || '审核通过，申请已进入排队序列！', 'success')
      await loadData()
    }
  } catch (err) {
    showToast('审核通过失败: ' + err.message, 'error')
  } finally {
    actionLoading.value = false
  }
}

const openRejectModal = (app) => {
  rejectTargetApp.value = app
  rejectReasonPreset.value = rejectReasonOptions[0]
  rejectReason.value = rejectReasonOptions[0]
  rejectModalVisible.value = true
}

const handleConfirmReject = async () => {
  if (!rejectTargetApp.value) return
  if (!rejectReason.value.trim()) {
    showToast('请填写驳回理由', 'warning')
    return
  }

  actionLoading.value = true
  try {
    const res = await api.post('/property/audit-application', {
      app_id: rejectTargetApp.value.id,
      action: 'REJECT',
      reject_reason: rejectReason.value.trim()
    })
    if (res.code === 200) {
      showToast(res.message || '已成功驳回，并向业主发送站内通知！', 'success')
      rejectModalVisible.value = false
      await loadData()
    }
  } catch (err) {
    showToast('驳回失败: ' + err.message, 'error')
  } finally {
    actionLoading.value = false
  }
}

const openViewAttachmentsModal = (app) => {
  if (isProduction) {
    productionReviewApp.value = app
    auditReference.value = ''
    documentsVerified.value = false
    productionReviewVisible.value = true
    return
  }
  viewAttachmentsApp.value = app
  activeAttachmentIndex.value = 0
  viewAttachmentsModalVisible.value = true
}

const handleInlineApprove = async (app) => {
  await handleApproveApplication(app)
  viewAttachmentsModalVisible.value = false
}

const handleInlineReject = (app) => {
  viewAttachmentsModalVisible.value = false
  openRejectModal(app)
}

const loadData = async () => {
  try {
    const sRes = await api.get('/property/stats')
    if (sRes.code === 200) stats.value = sRes.data

    const spRes = await api.get('/property/spaces')
    if (spRes.code === 200) spaceList.value = spRes.data

    await loadAuditConfig()
    await loadApplications()
    await loadCommunityUsers()
  } catch (err) {
    console.error('加载物业数据失败:', err)
  }
}

onMounted(() => {
  loadData()
})

const openQuickMaintenanceModal = () => {
  if (currentSpace.value?.id) {
    quickMaintenanceSpaceId.value = currentSpace.value.id
  } else {
    const firstVacant = spaceList.value.find(s => s.status === 'VACANT')
    quickMaintenanceSpaceId.value = firstVacant ? firstVacant.id : (spaceList.value[0]?.id || null)
  }
  quickMaintenanceModal.value = true
}

const handleConfirmQuickMaintenance = async () => {
  if (!quickMaintenanceSpaceId.value) return
  actionLoading.value = true
  try {
    const res = await api.post('/property/set-maintenance', {
      space_id: quickMaintenanceSpaceId.value,
      action: 'SET_MAINTENANCE',
      reason: quickMaintenanceReason.value
    })
    showToast(res.message || '车位已成功设为维修停用！', 'success')
    quickMaintenanceModal.value = false
    await loadData()
  } catch (err) {
    showToast('操作失败: ' + err.message, 'error')
  } finally {
    actionLoading.value = false
  }
}

const spaceArchive = ref(null)

const selectSpace = async (sp) => {
  currentSpace.value = sp
  selectedSpaceModal.value = true
  spaceArchive.value = null
  maintenanceReason.value = '地面沉降破损与路砖平整修补'
  deliveryProofName.value = ''
  terminationReference.value = ''
  deliveryReceiptNo.value = ''
  deliveryPaymentConfirmed.value = false
  maintenanceProofName.value = ''
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

const spaceAreaLabel = (areaType) => areaType === 'GROUND' ? '地面' : areaType === 'UNDERGROUND' ? '地下' : '**'

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
  if (isProduction && !terminationReference.value.trim()) {
    showToast('请填写线下清退凭据编号', 'warning')
    return
  }
  const confirmed = await showConfirm({
    title: '强制代清退车位确认',
    content: `确认清退车位【${currentSpace.value.space_no}】吗？\n此操作将释放车位并按顺位重新分配，相关操作会记入系统审计记录。`,
    type: 'danger',
    confirmText: '确认清退',
    cancelText: '取消'
  })
  if (!confirmed) return

  actionLoading.value = true
  try {
    const res = await api.post('/property/force-terminate', {
      space_id: currentSpace.value.id,
      reason: terminateReason.value,
      evidence_reference: terminationReference.value
    })
    showToast(res.message || '清退成功！后序排位已自动前移', 'success')
    selectedSpaceModal.value = false
    await loadData()
  } catch (err) {
    showToast('清退失败: ' + err.message, 'error')
  } finally {
    actionLoading.value = false
  }
}

const handleConfirmDelivery = async () => {
  if (!currentSpace.value.user_id) {
    showToast('该车位暂无关联中签用户', 'warning')
    return
  }
  if (isProduction && (!deliveryPaymentConfirmed.value || !deliveryReceiptNo.value.trim())) {
    showToast('请核对合同并填写缴费凭据编号', 'warning')
    return
  }
  actionLoading.value = true
  try {
    const res = await api.post('/property/confirm-delivery', {
      space_id: currentSpace.value.id,
      user_id: currentSpace.value.user_id,
      months: selectedDeliveryMonths.value,
      payment_confirmed: deliveryPaymentConfirmed.value,
      receipt_no: deliveryReceiptNo.value,
      attachment_url: deliveryProofName.value || null
    })
    showToast(res.message || '交付成功，租约已生效！', 'success')
    selectedSpaceModal.value = false
    await loadData()
  } catch (err) {
    showToast('交付失败: ' + err.message, 'error')
  } finally {
    actionLoading.value = false
  }
}

const handleToggleMaintenance = async (action) => {
  if (action === 'SET_MAINTENANCE') {
    const confirmed = await showConfirm({
      title: '车位维修停用确认',
      content: `确认将车位【${currentSpace.value.space_no}】设为维修停用吗？\n停用期间车位将移出分配池，操作会记入系统审计记录。`,
      type: 'warning',
      confirmText: '确认停用',
      cancelText: '取消'
    })
    if (!confirmed) return
  }
  actionLoading.value = true
  try {
    const res = await api.post('/property/set-maintenance', {
      space_id: currentSpace.value.id,
      action,
      reason: action === 'SET_MAINTENANCE' ? maintenanceReason.value : '检修验收合格恢复上线',
      attachment_url: action === 'SET_MAINTENANCE' ? (maintenanceProofName.value || null) : null
    })
    showToast(res.message || '操作成功', 'success')
    selectedSpaceModal.value = false
    await loadData()
  } catch (err) {
    showToast('操作失败: ' + err.message, 'error')
  } finally {
    actionLoading.value = false
  }
}

const handleResetDemo = async () => {
  const confirmed = await showConfirm({
    title: '全系统演示数据重置确认',
    content: '确认将全系统所有车位和账号数据一键重置为初始演示状态吗？',
    type: 'danger',
    confirmText: '确认重置',
    cancelText: '取消'
  })
  if (!confirmed) return

  try {
    const res = await api.post('/dev/reset-seed')
    showToast(res.message || '已成功重置为初始状态！', 'success')
    selectedSpaceModal.value = false
    await loadData()
  } catch (err) {
    showToast('重置失败: ' + err.message, 'error')
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
    showToast('导出失败: ' + err.message, 'error')
  }
}

const handleLogout = () => {
  userStore.logout()
  router.push('/login')
}
</script>
