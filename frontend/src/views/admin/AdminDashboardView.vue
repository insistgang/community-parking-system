<template>
  <div class="min-h-screen bg-slate-950 text-slate-100 flex flex-col font-sans">
    <!-- 顶部导航栏 -->
    <header class="bg-slate-900/90 border-b border-slate-800 px-6 py-3.5 sticky top-0 z-30 flex flex-wrap items-center justify-between gap-4 backdrop-blur shadow-lg">
      <div class="flex items-center gap-3">
        <div class="w-9 h-9 rounded-xl bg-indigo-600 flex items-center justify-center text-white font-bold shadow-md shadow-indigo-600/30">
          <AppIcon name="shield" :size="22" />
        </div>
        <div>
          <div class="flex items-center gap-2">
            <h1 class="text-base font-black text-white tracking-wide">慧停管家 · 管理员后台</h1>
            <span class="text-[10px] px-2 py-0.5 rounded-full bg-indigo-950 text-indigo-300 border border-indigo-500/40 font-bold">
              SUPER_ADMIN
            </span>
          </div>
          <p class="text-xs text-slate-400">运行环境：本地生产节点 | 当前管理员：{{ userStore.realName }} ({{ userStore.user?.phone }})</p>
        </div>
      </div>

      <div class="flex items-center gap-2 flex-wrap text-xs">
        <button v-if="isProduction"
          @click="openPropertyInvite"
          class="px-3.5 py-1.5 rounded-xl bg-purple-700 hover:bg-purple-600 text-white font-bold transition"
        >
          签发物业入驻邀请码
        </button>
        <!-- 24小时未读短信催办快捷测试按钮 -->
        <button v-if="!isProduction"
          @click="handleTriggerSmsUpgrade"
          :disabled="smsTriggering"
          class="px-3.5 py-1.5 rounded-xl bg-emerald-700 hover:bg-emerald-600 active:scale-95 text-white font-bold transition flex items-center gap-1.5 shadow-sm disabled:opacity-50"
          title="手动触发未读通知短信升级扫描（测试验证）"
        >
          <span><AppIcon name="phone" size="1em" /></span>
          <span v-if="smsTriggering">正在扫描下发...</span>
          <span v-else>测试 24h 未读短信升级</span>
        </button>

        <button
          @click="loadAllData"
          class="px-3 py-1.5 rounded-xl bg-slate-800 hover:bg-slate-700 text-slate-300 font-semibold transition flex items-center gap-1 border border-slate-700"
        >
          <span><AppIcon name="refresh" size="1em" /> 刷新数据</span>
        </button>

        <router-link
          to="/"
          class="px-3 py-1.5 rounded-xl bg-slate-800 hover:bg-slate-700 text-blue-400 font-semibold transition border border-slate-700"
        >
          前台业主端 <AppIcon name="arrow" size="1em" />
        </router-link>

        <router-link
          to="/property/dashboard"
          class="px-3 py-1.5 rounded-xl bg-slate-800 hover:bg-slate-700 text-purple-400 font-semibold transition border border-slate-700"
        >
          物业工作台 <AppIcon name="arrow" size="1em" />
        </router-link>

        <button
          @click="handleLogout"
          class="px-3 py-1.5 rounded-xl bg-rose-950/80 hover:bg-rose-900 text-rose-300 transition border border-rose-800"
        >
          退出平台
        </button>
      </div>
    </header>

    <main class="flex-1 max-w-[1600px] w-full mx-auto p-4 sm:p-6 space-y-5">
      <!-- 平台统计大盘指标 -->
      <div class="grid grid-cols-2 sm:grid-cols-4 lg:grid-cols-7 gap-3 text-xs">
        <div class="bg-slate-900 p-4 rounded-2xl border border-slate-800">
          <span class="text-slate-400 block">注册用户总数</span>
          <span class="text-2xl font-black text-white mt-1 block font-mono">{{ stats.total_users ?? 0 }}</span>
        </div>
        <div class="bg-slate-900 p-4 rounded-2xl border border-slate-800">
          <span class="text-blue-400 block">社区业主账号</span>
          <span class="text-2xl font-black text-blue-400 mt-1 block font-mono">{{ stats.owner_count ?? 0 }}</span>
        </div>
        <div class="bg-slate-900 p-4 rounded-2xl border border-slate-800">
          <span class="text-purple-400 block">物业管理账号</span>
          <span class="text-2xl font-black text-purple-400 mt-1 block font-mono">{{ stats.property_count ?? 0 }}</span>
        </div>
        <div class="bg-slate-900 p-4 rounded-2xl border border-slate-800">
          <span class="text-emerald-400 block">纳管车位总量</span>
          <span class="text-2xl font-black text-emerald-400 mt-1 block font-mono">{{ stats.total_spaces ?? 0 }}</span>
        </div>
        <div class="bg-slate-900 p-4 rounded-2xl border border-slate-800">
          <span class="text-amber-400 block">车位申请总量</span>
          <span class="text-2xl font-black text-amber-400 mt-1 block font-mono">{{ stats.total_applications ?? 0 }}</span>
        </div>
        <div class="bg-slate-900 p-4 rounded-2xl border border-slate-800">
          <span class="text-cyan-400 block">累计登录人次</span>
          <span class="text-2xl font-black text-cyan-400 mt-1 block font-mono">{{ stats.total_logins ?? 0 }}</span>
        </div>
        <div class="bg-slate-900 p-4 rounded-2xl border border-slate-800">
          <span class="text-rose-400 block">外部接口核验</span>
          <div class="flex items-baseline gap-1 mt-1">
            <span class="text-2xl font-black text-rose-400 font-mono">{{ stats.total_api_calls ?? 0 }}</span>
            <span class="text-[10px] text-slate-500">次</span>
          </div>
        </div>
      </div>

      <!-- 选项卡导航栏 -->
      <div class="bg-slate-900 p-1.5 rounded-2xl border border-slate-800 flex items-center gap-1.5 flex-wrap text-xs font-bold">
        <button
          v-for="t in tabs"
          :key="t.id"
          @click="activeTab = t.id"
          class="px-4 py-2.5 rounded-xl transition flex items-center gap-2"
          :class="activeTab === t.id ? 'bg-indigo-600 text-white shadow-md shadow-indigo-600/30' : 'text-slate-400 hover:text-white hover:bg-slate-800'"
        >
          <AppIcon :name="t.icon" :size="17" />
          <span>{{ t.label }}</span>
          <span v-if="t.count !== undefined" class="px-1.5 py-0.2 rounded-full bg-slate-800 text-[10px] font-mono">
            {{ t.count }}
          </span>
        </button>
      </div>

      <!-- ================= Tab 1: 用户全量档案与管理 (功能 18) ================= -->
      <div v-show="activeTab === 'users'" class="bg-slate-900 rounded-3xl p-5 border border-slate-800 space-y-4">
        <div class="flex items-center justify-between flex-wrap gap-2 text-xs">
          <div class="flex items-center gap-2">
            <h2 class="text-sm font-bold text-white flex items-center gap-2">
              <span><AppIcon name="users" size="1em" /> 用户档案与登录状态汇总</span>
            </h2>
            <span class="text-[11px] text-slate-400">包含业主注册信息、物业注册信息及最后活跃登录记录</span>
          </div>
          <!-- 角色过滤 -->
          <div class="flex items-center gap-1.5 text-xs">
            <button
              v-for="r in ['ALL', 'OWNER', 'PROPERTY_ADMIN', 'SUPER_ADMIN']"
              :key="r"
              @click="userRoleFilter = r"
              class="px-2.5 py-1 rounded-lg border text-[11px] font-semibold transition"
              :class="userRoleFilter === r ? 'bg-indigo-600 text-white border-indigo-500' : 'bg-slate-800 text-slate-400 border-slate-700 hover:text-white'"
            >
              {{ r === 'ALL' ? '全部角色' : (r === 'OWNER' ? '社区业主' : (r === 'PROPERTY_ADMIN' ? '物业人员' : '系统总管')) }}
            </button>
          </div>
        </div>

        <div class="overflow-x-auto rounded-2xl border border-slate-800">
          <table class="w-full text-left text-xs divide-y divide-slate-800">
            <thead class="bg-slate-950/60 text-slate-400 font-bold">
              <tr>
                <th class="p-3">ID</th>
                <th class="p-3">真实姓名</th>
                <th class="p-3">手机号码</th>
                <th class="p-3">身份证号</th>
                <th class="p-3">系统角色</th>
                <th class="p-3">职务 / 备注</th>
                <th class="p-3">所属小区</th>
                <th class="p-3">注册时间</th>
                <th class="p-3">登录次数</th>
                <th class="p-3">最后登录时间</th>
                <th class="p-3">最后登录 IP</th>
              </tr>
            </thead>
            <tbody class="divide-y divide-slate-800/60 text-slate-300">
              <tr v-for="u in filteredUsers" :key="u.id" class="hover:bg-slate-800/50 transition">
                <td class="p-3 font-mono text-slate-400">{{ u.id }}</td>
                <td class="p-3 font-bold text-white">{{ u.real_name }}</td>
                <td class="p-3 font-mono text-indigo-300">{{ u.phone }}</td>
                <td class="p-3 font-mono text-slate-400">{{ u.id_card_masked }}</td>
                <td class="p-3">
                  <span
                    class="px-2 py-0.5 rounded-full text-[10px] font-bold border"
                    :class="u.role === 'SUPER_ADMIN' ? 'bg-rose-950 text-rose-300 border-rose-800' : (u.role === 'PROPERTY_ADMIN' ? 'bg-purple-950 text-purple-300 border-purple-800' : 'bg-blue-950 text-blue-300 border-blue-800')"
                  >
                    {{ u.role_label }}
                  </span>
                </td>
                <td class="p-3 text-slate-300"><div>{{ u.job_title || '—' }}</div><div class="mt-1 text-[11px] text-slate-400">{{ u.notes || '—' }}</div></td>
                <td class="p-3 text-slate-300">{{ u.community_name }}</td>
                <td class="p-3 text-[11px] text-slate-400 font-mono">{{ u.created_at }}</td>
                <td class="p-3 font-mono font-bold text-emerald-400">{{ u.login_count }} 次</td>
                <td class="p-3 text-[11px] text-slate-400 font-mono">{{ u.last_login_at }}</td>
                <td class="p-3 text-[11px] text-slate-400 font-mono">{{ u.last_login_ip }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- ================= Tab 2: 登录记录审计日志 (功能 18 & 19) ================= -->
      <div v-show="activeTab === 'login_logs'" class="bg-slate-900 rounded-3xl p-5 border border-slate-800 space-y-4">
        <div class="flex items-center justify-between flex-wrap gap-2 text-xs">
          <div>
            <h2 class="text-sm font-bold text-white flex items-center gap-2">
              <span><AppIcon name="key" size="1em" /> 用户与物业登录记录流水</span>
            </h2>
            <span class="text-[11px] text-slate-400">实时记录全平台业主、物业及管理员的登录时间、IP与客户端环境</span>
          </div>
          <span class="text-slate-400">最近 50 条登录记录</span>
        </div>

        <div class="overflow-x-auto rounded-2xl border border-slate-800">
          <table class="w-full text-left text-xs divide-y divide-slate-800">
            <thead class="bg-slate-950/60 text-slate-400 font-bold">
              <tr>
                <th class="p-3">日志ID</th>
                <th class="p-3">登录时间</th>
                <th class="p-3">登录账号</th>
                <th class="p-3">姓名</th>
                <th class="p-3">角色身份</th>
                <th class="p-3">认证方式</th>
                <th class="p-3">客户端 IP</th>
                <th class="p-3">客户端终端环境</th>
                <th class="p-3">结果状态</th>
              </tr>
            </thead>
            <tbody class="divide-y divide-slate-800/60 text-slate-300">
              <tr v-for="l in loginLogs" :key="l.id" class="hover:bg-slate-800/50 transition">
                <td class="p-3 font-mono text-slate-500">#{{ l.id }}</td>
                <td class="p-3 text-[11px] text-slate-300 font-mono">{{ l.login_at }}</td>
                <td class="p-3 font-mono font-bold text-white">{{ l.account }}</td>
                <td class="p-3 text-slate-200">{{ l.real_name }}</td>
                <td class="p-3">
                  <span class="text-[11px] text-slate-400 font-semibold">{{ l.role_label }}</span>
                </td>
                <td class="p-3">
                  <span class="px-2 py-0.5 rounded text-[10px] font-bold" :class="l.login_type === 'PASSWORD' ? 'bg-slate-800 text-slate-300' : 'bg-amber-950 text-amber-300'">
                    {{ l.login_type === 'PASSWORD' ? '账号密码' : '短信验证码' }}
                  </span>
                </td>
                <td class="p-3 font-mono text-indigo-300">{{ l.ip }}</td>
                <td class="p-3 text-[11px] text-slate-400 truncate max-w-xs" :title="l.user_agent">{{ l.user_agent }}</td>
                <td class="p-3">
                  <span
                    class="px-2 py-0.5 rounded-full text-[10px] font-bold"
                    :class="l.status === 'SUCCESS' ? 'bg-emerald-950 text-emerald-300 border border-emerald-800' : 'bg-rose-950 text-rose-300 border border-rose-800'"
                  >
                    {{ l.status === 'SUCCESS' ? '✓ 登录成功' : '✕ 认证失败' }}
                  </span>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- ================= Tab 3: 用户关键操作日志 (功能 19) ================= -->
      <div v-show="activeTab === 'operation_logs'" class="bg-slate-900 rounded-3xl p-5 border border-slate-800 space-y-4">
        <div class="flex items-center justify-between flex-wrap gap-2 text-xs">
          <div>
            <h2 class="text-sm font-bold text-white flex items-center gap-2">
              <span><AppIcon name="clipboard" size="1em" /> 用户与管理员关键操作安全日志</span>
            </h2>
            <span class="text-[11px] text-slate-400">覆盖申请申报、资格审核、车位清退、密码修改、撤销排位等所有敏感操作流水</span>
          </div>
          <span class="text-slate-400">最近 50 条操作记录</span>
        </div>

        <div class="overflow-x-auto rounded-2xl border border-slate-800">
          <table class="w-full text-left text-xs divide-y divide-slate-800">
            <thead class="bg-slate-950/60 text-slate-400 font-bold">
              <tr>
                <th class="p-3">ID</th>
                <th class="p-3">操作时间</th>
                <th class="p-3">操作人</th>
                <th class="p-3">业务模块</th>
                <th class="p-3">动作类型</th>
                <th class="p-3">操作事项摘要</th>
                <th class="p-3">IP 地址</th>
                <th class="p-3">操作参数</th>
              </tr>
            </thead>
            <tbody class="divide-y divide-slate-800/60 text-slate-300">
              <tr v-for="op in operationLogs" :key="op.id" class="hover:bg-slate-800/50 transition">
                <td class="p-3 font-mono text-slate-500">#{{ op.id }}</td>
                <td class="p-3 text-[11px] text-slate-300 font-mono">{{ op.created_at }}</td>
                <td class="p-3">
                  <span class="font-bold text-white block">{{ op.operator_name }}</span>
                  <span class="text-[10px] text-slate-500 font-mono">{{ op.operator_role }}</span>
                </td>
                <td class="p-3">
                  <span class="px-2 py-0.5 rounded text-[10px] font-bold bg-slate-800 text-indigo-300 border border-slate-700">
                    {{ op.module }}
                  </span>
                </td>
                <td class="p-3 font-mono font-semibold text-slate-300">{{ op.action }}</td>
                <td class="p-3 text-slate-200 max-w-sm">{{ op.summary }}</td>
                <td class="p-3 font-mono text-slate-400">{{ op.ip }}</td>
                <td class="p-3">
                  <button
                    @click="viewJsonDetail('操作参数详情', op.details_json)"
                    class="px-2 py-0.5 rounded bg-slate-800 hover:bg-slate-700 text-indigo-300 text-[11px] font-mono transition"
                  >
                    查看 JSON
                  </button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- ================= Tab 4: 外部接口调用日志监控 (功能 20) ================= -->
      <div v-show="activeTab === 'external_api_logs'" class="bg-slate-900 rounded-3xl p-5 border border-slate-800 space-y-4">
        <div class="flex items-center justify-between flex-wrap gap-2 text-xs">
          <div>
            <h2 class="text-sm font-bold text-white flex items-center gap-2">
              <span><AppIcon name="globe" size="1em" /> 外部第三方接口调用追踪日志</span>
            </h2>
            <span class="text-[11px] text-slate-400">当前尚未接入外部核验服务；接入后将在这里查看实际调用记录。</span>
          </div>
          <span class="text-slate-400">最近 50 次外部调用记录</span>
        </div>

        <div class="overflow-x-auto rounded-2xl border border-slate-800">
          <table class="w-full text-left text-xs divide-y divide-slate-800">
            <thead class="bg-slate-950/60 text-slate-400 font-bold">
              <tr>
                <th class="p-3">ID</th>
                <th class="p-3">调用时间</th>
                <th class="p-3">核验接口名称</th>
                <th class="p-3">对接机构/服务商</th>
                <th class="p-3">请求方式</th>
                <th class="p-3">耗时 (ms)</th>
                <th class="p-3">HTTP 码</th>
                <th class="p-3">状态</th>
                <th class="p-3">报文详情</th>
              </tr>
            </thead>
            <tbody class="divide-y divide-slate-800/60 text-slate-300">
              <tr v-for="apiItem in externalApiLogs" :key="apiItem.id" class="hover:bg-slate-800/50 transition">
                <td class="p-3 font-mono text-slate-500">#{{ apiItem.id }}</td>
                <td class="p-3 text-[11px] text-slate-300 font-mono">{{ apiItem.created_at }}</td>
                <td class="p-3 font-bold text-white">{{ apiItem.api_name }}</td>
                <td class="p-3 text-slate-300">{{ apiItem.provider }}</td>
                <td class="p-3 font-mono text-[11px] text-amber-300">{{ apiItem.method }}</td>
                <td class="p-3 font-mono text-emerald-400 font-bold">{{ apiItem.duration_ms }} ms</td>
                <td class="p-3 font-mono text-slate-300">{{ apiItem.status_code }}</td>
                <td class="p-3">
                  <span
                    class="px-2 py-0.5 rounded-full text-[10px] font-bold"
                    :class="apiItem.status === 'SUCCESS' ? 'bg-emerald-950 text-emerald-300 border border-emerald-800' : 'bg-rose-950 text-rose-300 border border-rose-800'"
                  >
                    {{ apiItem.status === 'SUCCESS' ? '✓ 成功' : '✕ 异常' }}
                  </span>
                </td>
                <td class="p-3">
                  <button
                    @click="viewApiDetail(apiItem)"
                    class="px-2.5 py-0.5 rounded bg-indigo-950 hover:bg-indigo-900 text-indigo-300 text-[11px] font-mono border border-indigo-800 transition"
                  >
                    报文详情 <AppIcon name="arrow" size="1em" />
                  </button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- ================= Tab 5: 24小时未读升级短信调度控制台 (功能 7) ================= -->
      <div v-if="!isProduction" v-show="activeTab === 'sms_remind'" class="bg-slate-900 rounded-3xl p-6 border border-slate-800 space-y-6">
        <div>
          <h2 class="text-sm font-bold text-white flex items-center gap-2">
            <span><AppIcon name="phone" size="1em" /> 24 小时未读消息短信催办调度服务</span>
          </h2>
          <p class="text-xs text-slate-400 mt-1 leading-relaxed">
            方案规则约束：当车位中签通知（48小时倒计时）、审核结果等关键站内信超过 24 小时仍未被业主点击查阅时，后台定时调度引擎将自动触发移动政务云短信网关下发备用短信通知，确保业主不错过签约时限。
          </p>
        </div>

        <div class="grid grid-cols-1 md:grid-cols-3 gap-4 text-xs">
          <div class="bg-slate-950/70 p-4 rounded-2xl border border-slate-800 space-y-2">
            <span class="text-slate-400 block font-bold"><AppIcon name="clock" size="1em" /> 调度执行周期</span>
            <p class="text-emerald-400 font-mono font-bold text-sm">每 60 秒轮询扫描一次</p>
            <p class="text-slate-500 text-[11px]">后台由 Spring Scheduled 自动轮询执行，无需人工干预。</p>
          </div>

          <div class="bg-slate-950/70 p-4 rounded-2xl border border-slate-800 space-y-2">
            <span class="text-slate-400 block font-bold"><AppIcon name="target" size="1em" /> 覆盖核心业务类型</span>
            <p class="text-amber-400 font-bold text-sm">中签锁定(48h) / 审核通过 / 顺位变动</p>
            <p class="text-slate-500 text-[11px]">对中签通知特别设置多渠道保障，防止因业主未上线导致中签超时收回。</p>
          </div>

          <div class="bg-slate-950/70 p-4 rounded-2xl border border-slate-800 space-y-2">
            <span class="text-slate-400 block font-bold"><AppIcon name="chart" size="1em" /> 累计已升级短信条数</span>
            <p class="text-2xl font-black text-indigo-400 font-mono">{{ stats.sms_upgraded_count ?? 0 }} <span class="text-xs font-normal text-slate-500">条</span></p>
            <p class="text-slate-500 text-[11px]">全部短信下发均已自动记录进“外部接口日志”与“操作审计日志”。</p>
          </div>
        </div>

        <!-- 手动触发与测试工具区 -->
        <div v-if="!isProduction" class="p-5 rounded-2xl bg-indigo-950/40 border border-indigo-500/30 flex items-center justify-between flex-wrap gap-4 text-xs">
          <div>
            <h3 class="font-bold text-white text-sm"><AppIcon name="review" size="1em" /> 验收与演示快捷测试</h3>
            <p class="text-slate-400 mt-0.5">系统已预置 1 条超过 24 小时未读的中签通知（李雷）。点击右侧按钮可立即强制扫描并下发催办短信。</p>
          </div>
          <button
            @click="handleTriggerSmsUpgrade"
            :disabled="smsTriggering"
            class="px-5 py-2.5 rounded-xl bg-indigo-600 hover:bg-indigo-500 active:scale-95 text-white font-bold transition shadow-lg shadow-indigo-600/30 flex items-center gap-2"
          >
            <span><AppIcon name="trend" size="1em" /> 立即执行短信催办扫描</span>
          </button>
        </div>

        <div v-if="smsScanResult" class="p-4 rounded-2xl bg-emerald-950/60 border border-emerald-500/40 text-xs text-emerald-300 space-y-1 animate-fade-in">
          <p class="font-bold"><AppIcon name="check" size="1em" /> {{ smsScanResult }}</p>
          <p class="text-[11px] text-slate-400">已自动向目标业主手机下发模拟政务短信，并在“外部接口调用监控”中生成调用流水记录。</p>
        </div>
      </div>
    </main>

    <!-- JSON 详情模态弹窗 -->
    <div v-if="showJsonModal" class="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/70 backdrop-blur-sm animate-fade-in text-xs">
      <div class="bg-slate-900 rounded-3xl max-w-2xl w-full p-6 text-slate-200 shadow-2xl border border-slate-700 space-y-4">
        <div class="flex items-center justify-between border-b border-slate-800 pb-3">
          <h3 class="text-sm font-bold text-white">{{ jsonModalTitle }}</h3>
          <button @click="showJsonModal = false" class="text-slate-400 hover:text-white p-1 text-base" aria-label="关闭"><AppIcon name="close" size="1em" /></button>
        </div>
        <div class="max-h-[60vh] overflow-auto bg-slate-950 p-4 rounded-xl border border-slate-800">
          <pre class="font-mono text-emerald-400 text-xs leading-relaxed whitespace-pre-wrap">{{ jsonModalContent }}</pre>
        </div>
        <div class="flex justify-end pt-2">
          <button
            @click="showJsonModal = false"
            class="px-4 py-2 rounded-xl bg-slate-800 hover:bg-slate-700 text-slate-200 font-bold transition"
          >
            关闭
          </button>
        </div>
      </div>
    </div>

    <!-- 外部接口调用详情弹窗 -->
    <div v-if="showApiModal && currentApiItem" class="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/70 backdrop-blur-sm animate-fade-in text-xs">
      <div class="bg-slate-900 rounded-3xl max-w-3xl w-full p-6 text-slate-200 shadow-2xl border border-slate-700 space-y-4">
        <div class="flex items-center justify-between border-b border-slate-800 pb-3">
          <div>
            <h3 class="text-sm font-bold text-white flex items-center gap-2">
              <span>{{ currentApiItem.api_name }}</span>
              <span class="px-2 py-0.5 rounded text-[10px] bg-indigo-950 text-indigo-300 border border-indigo-700 font-mono">
                {{ currentApiItem.method }}
              </span>
            </h3>
            <p class="text-[11px] text-slate-400 mt-0.5">对接服务商：{{ currentApiItem.provider }} | URL: {{ currentApiItem.url }}</p>
          </div>
          <button @click="showApiModal = false" class="text-slate-400 hover:text-white p-1 text-base" aria-label="关闭"><AppIcon name="close" size="1em" /></button>
        </div>

        <div class="grid grid-cols-2 gap-4">
          <div>
            <label class="block font-bold text-slate-400 mb-1"><AppIcon name="upload" size="1em" /> 请求入参报文 (Request Payload)</label>
            <div class="max-h-64 overflow-auto bg-slate-950 p-3 rounded-xl border border-slate-800">
              <pre class="font-mono text-indigo-300 text-xs whitespace-pre-wrap">{{ formatJson(currentApiItem.request_json) }}</pre>
            </div>
          </div>

          <div>
            <label class="block font-bold text-slate-400 mb-1"><AppIcon name="export" size="1em" /> 响应结果报文 (Response Data)</label>
            <div class="max-h-64 overflow-auto bg-slate-950 p-3 rounded-xl border border-slate-800">
              <pre class="font-mono text-emerald-400 text-xs whitespace-pre-wrap">{{ formatJson(currentApiItem.response_json) }}</pre>
            </div>
          </div>
        </div>

        <div class="flex items-center justify-between pt-2 border-t border-slate-800 text-[11px] text-slate-400">
          <span>调用耗时：<strong class="text-emerald-400 font-mono">{{ currentApiItem.duration_ms }} ms</strong> | HTTP Code: {{ currentApiItem.status_code }}</span>
          <button
            @click="showApiModal = false"
            class="px-4 py-2 rounded-xl bg-slate-800 hover:bg-slate-700 text-slate-200 font-bold transition"
          >
            关闭窗口
          </button>
        </div>
      </div>
    </div>
    <div v-if="showPropertyInvite" class="fixed inset-0 z-50 flex items-center justify-center bg-black/75 p-4">
      <div class="w-full max-w-md rounded-2xl border border-slate-700 bg-slate-900 p-5 space-y-4 text-sm shadow-2xl">
        <div class="flex items-center justify-between">
          <h2 class="font-bold text-white">签发物业入驻邀请码</h2>
          <button type="button" @click="closePropertyInvite" class="text-slate-400 hover:text-white">关闭</button>
        </div>
        <p class="text-xs text-slate-400">先线下核验负责人身份。邀请码绑定手机号、身份证号及小区名称，七天内只能使用一次。</p>
        <form v-if="!propertyInviteCode" @submit.prevent="handleIssuePropertyInvite" class="space-y-3">
          <label class="block space-y-1">小区名称
            <input v-model.trim="propertyInviteForm.community_name" required maxlength="100"
              class="w-full rounded-lg border border-slate-600 bg-slate-950 px-3 py-2 text-white" />
          </label>
          <label class="block space-y-1">负责人手机号
            <input v-model.trim="propertyInviteForm.phone" required inputmode="numeric" maxlength="11"
              class="w-full rounded-lg border border-slate-600 bg-slate-950 px-3 py-2 text-white" />
          </label>
          <label class="block space-y-1">负责人身份证号
            <input v-model.trim="propertyInviteForm.id_card" required maxlength="18"
              class="w-full rounded-lg border border-slate-600 bg-slate-950 px-3 py-2 text-white" />
          </label>
          <p v-if="propertyInviteError" role="alert" class="text-rose-300 text-xs">{{ propertyInviteError }}</p>
          <button type="submit" :disabled="propertyInviteLoading"
            class="w-full rounded-lg bg-purple-600 px-4 py-2 font-bold text-white disabled:opacity-50">
            {{ propertyInviteLoading ? '正在签发…' : '签发一次性邀请码' }}
          </button>
        </form>
        <div v-else class="space-y-3">
          <p class="text-emerald-300 text-xs">邀请码仅显示这一次。请通过线下方式交给对应负责人。</p>
          <input :value="propertyInviteCode" readonly @click="$event.target.select()"
            class="w-full rounded-lg border border-emerald-600 bg-slate-950 px-3 py-2 font-mono text-emerald-200" />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import AppIcon from '../../components/AppIcon.vue'
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../../store/user'
import api from '../../api'

const router = useRouter()
const userStore = useUserStore()
const isProduction = import.meta.env.VITE_APP_MODE === 'production'

const stats = ref({})
const userList = ref([])
const loginLogs = ref([])
const operationLogs = ref([])
const externalApiLogs = ref([])

const activeTab = ref('users')
const userRoleFilter = ref('ALL')

const tabs = computed(() => [
  { id: 'users', label: '用户档案管理', icon: 'users', count: userList.value.length },
  { id: 'login_logs', label: '登录流水审计', icon: 'key', count: loginLogs.value.length },
  { id: 'operation_logs', label: '操作审计日志', icon: 'clipboard', count: operationLogs.value.length },
  { id: 'external_api_logs', label: '外部接口监控', icon: 'globe', count: externalApiLogs.value.length },
  ...(!isProduction ? [{ id: 'sms_remind', label: '24h 未读短信催办', icon: 'phone' }] : [])
])

const filteredUsers = computed(() => {
  if (userRoleFilter.value === 'ALL') return userList.value
  return userList.value.filter(u => u.role === userRoleFilter.value)
})

// 弹窗状态
const showJsonModal = ref(false)
const jsonModalTitle = ref('')
const jsonModalContent = ref('')

const showApiModal = ref(false)
const currentApiItem = ref(null)

const smsTriggering = ref(false)
const smsScanResult = ref('')
const showPropertyInvite = ref(false)
const propertyInviteForm = ref({ community_name: '', phone: '', id_card: '' })
const propertyInviteCode = ref('')
const propertyInviteError = ref('')
const propertyInviteLoading = ref(false)

const openPropertyInvite = () => {
  propertyInviteForm.value = { community_name: '', phone: '', id_card: '' }
  propertyInviteCode.value = ''
  propertyInviteError.value = ''
  showPropertyInvite.value = true
}

const closePropertyInvite = () => {
  showPropertyInvite.value = false
  propertyInviteCode.value = ''
  propertyInviteForm.value = { community_name: '', phone: '', id_card: '' }
}

const handleIssuePropertyInvite = async () => {
  propertyInviteLoading.value = true
  propertyInviteError.value = ''
  try {
    const res = await api.post('/admin/property-invites', propertyInviteForm.value)
    propertyInviteCode.value = res.data.invite_code
  } catch (err) {
    propertyInviteError.value = err.message || '邀请码签发失败'
  } finally {
    propertyInviteLoading.value = false
  }
}

const formatJson = (raw) => {
  if (!raw) return '{}'
  try {
    const parsed = typeof raw === 'string' ? JSON.parse(raw) : raw
    return JSON.stringify(parsed, null, 2)
  } catch (e) {
    return raw
  }
}

const viewJsonDetail = (title, jsonStr) => {
  jsonModalTitle.value = title
  jsonModalContent.value = formatJson(jsonStr)
  showJsonModal.value = true
}

const viewApiDetail = (item) => {
  currentApiItem.value = item
  showApiModal.value = true
}

const loadAllData = async () => {
  try {
    const [sRes, uRes, lRes, opRes, apiRes] = await Promise.all([
      api.get('/admin/stats'),
      api.get('/admin/users'),
      api.get('/admin/login-logs'),
      api.get('/admin/operation-logs'),
      api.get('/admin/external-api-logs')
    ])
    if (sRes.code === 200) stats.value = sRes.data
    if (uRes.code === 200) userList.value = uRes.data
    if (lRes.code === 200) loginLogs.value = lRes.data
    if (opRes.code === 200) operationLogs.value = opRes.data
    if (apiRes.code === 200) externalApiLogs.value = apiRes.data
  } catch (err) {
    console.error('加载后台管理数据失败:', err)
  }
}

const handleTriggerSmsUpgrade = async () => {
  smsTriggering.value = true
  smsScanResult.value = ''
  try {
    const res = await api.post('/admin/trigger-sms-upgrade')
    smsScanResult.value = res.message || '催办扫描完成'
    await loadAllData()
  } catch (err) {
    smsScanResult.value = '触发失败: ' + err.message
  } finally {
    smsTriggering.value = false
  }
}

const handleLogout = () => {
  userStore.logout()
  router.push('/admin/login')
}

onMounted(() => {
  loadAllData()
})
</script>
