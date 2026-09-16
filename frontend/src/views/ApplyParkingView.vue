<template>
  <div class="min-h-screen bg-slate-100 flex flex-col pb-12">
    <!-- 顶部导航 -->
    <header class="sticky top-0 z-40 bg-white border-b border-slate-200 px-4 py-3.5 flex items-center justify-between shadow-sm">
      <div class="flex items-center gap-2">
        <button @click="$router.push('/')" class="text-slate-500 hover:text-slate-800 p-1">
          <svg xmlns="http://www.w3.org/2000/svg" class="w-5 h-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
            <path d="m15 18-6-6 6-6"/>
          </svg>
        </button>
        <h1 class="text-sm font-bold text-slate-900">
          {{ currentAuditMode === 'FLEXIBLE_HYBRID' ? '老旧小区车位申请申报 (AI+人工审核)' : '老旧小区车位三证合一申报认证' }}
        </h1>
      </div>
      <span
        class="text-[11px] px-2 py-0.5 rounded-full border font-bold flex items-center gap-1"
        :class="currentAuditMode === 'FLEXIBLE_HYBRID' ? 'text-purple-700 bg-purple-50 border-purple-200' : 'text-emerald-700 bg-emerald-50 border-emerald-200'"
      >
        <span class="w-1.5 h-1.5 rounded-full animate-pulse" :class="currentAuditMode === 'FLEXIBLE_HYBRID' ? 'bg-purple-500' : 'bg-emerald-500'"></span>
        <span>{{ currentAuditMode === 'FLEXIBLE_HYBRID' ? 'AI初验 + 人工复核' : '三证合一刚性准入' }}</span>
      </span>
    </header>

    <main class="max-w-md w-full mx-auto p-4 space-y-4 flex-1">
      <!-- 驳回整改提示横幅 (若之前有被驳回单据) -->
      <div v-if="lastRejectReason" class="p-3.5 rounded-2xl bg-rose-50 border-2 border-rose-300 text-xs text-rose-800 space-y-1 animate-fade-in shadow-sm">
        <div class="font-bold flex items-center gap-1.5">
          <span class="text-base">⚠️</span>
          <span>前次申请驳回意见，请根据提示调整材料后重新提交：</span>
        </div>
        <p class="text-xs bg-white/90 p-2.5 rounded-xl border border-rose-200 text-rose-700 font-medium">
          {{ lastRejectReason }}
        </p>
        <p class="text-[10px] text-slate-500 pt-0.5">
          修改相关信息并补正附件后点击下方按钮重新发起，物业审核通过后将正式进入轮候序列。
        </p>
      </div>

      <!-- 申报审核方式选择卡片（三证合一 vs 灵活配置） -->
      <div class="bg-white rounded-2xl p-4 shadow-sm border border-slate-200 space-y-2.5 animate-fade-in">
        <div class="flex items-center justify-between">
          <div class="flex items-center gap-1.5 font-black text-xs text-slate-800">
            <span>📋</span>
            <span>申报审核通道选择</span>
          </div>
          <span class="text-[10px] px-2 py-0.5 rounded-full font-medium"
            :class="communityAuditMode === 'FLEXIBLE_HYBRID' ? 'bg-purple-100 text-purple-700' : 'bg-slate-100 text-slate-500'"
          >
            小区管理配置: {{ communityAuditMode === 'FLEXIBLE_HYBRID' ? '灵活配置(已启用)' : '三证合一直审' }}
          </span>
        </div>

        <div class="grid grid-cols-2 gap-2 text-left">
          <!-- 选项 1: 三证合一 -->
          <button
            type="button"
            @click="currentAuditMode = 'THREE_CERT_AUTO'"
            class="p-2.5 rounded-xl border-2 transition relative flex flex-col justify-between"
            :class="currentAuditMode === 'THREE_CERT_AUTO'
              ? 'border-emerald-500 bg-emerald-50/70 shadow-sm ring-1 ring-emerald-400/30'
              : 'border-slate-200 hover:border-slate-300 bg-slate-50/50'"
          >
            <div>
              <div class="flex items-center justify-between mb-1">
                <span class="font-bold text-xs" :class="currentAuditMode === 'THREE_CERT_AUTO' ? 'text-emerald-900' : 'text-slate-700'">
                  ⚡ 三证合一
                </span>
                <span v-if="currentAuditMode === 'THREE_CERT_AUTO'" class="w-4 h-4 rounded-full bg-emerald-500 text-white text-[10px] flex items-center justify-center font-bold">✓</span>
              </div>
              <p class="text-[10px] leading-tight" :class="currentAuditMode === 'THREE_CERT_AUTO' ? 'text-emerald-700 font-medium' : 'text-slate-400'">
                AI 自动化极速审核
              </p>
            </div>
            <span class="text-[9px] mt-1.5 block font-medium" :class="currentAuditMode === 'THREE_CERT_AUTO' ? 'text-emerald-600' : 'text-slate-400'">
              三证同名 · 秒级直入排队
            </span>
          </button>

          <!-- 选项 2: 灵活配置 -->
          <button
            type="button"
            @click="currentAuditMode = 'FLEXIBLE_HYBRID'"
            class="p-2.5 rounded-xl border-2 transition relative flex flex-col justify-between"
            :class="currentAuditMode === 'FLEXIBLE_HYBRID'
              ? 'border-purple-500 bg-purple-50/70 shadow-sm ring-1 ring-purple-400/30'
              : 'border-slate-200 hover:border-slate-300 bg-slate-50/50'"
          >
            <div>
              <div class="flex items-center justify-between mb-1">
                <span class="font-bold text-xs" :class="currentAuditMode === 'FLEXIBLE_HYBRID' ? 'text-purple-900' : 'text-slate-700'">
                  ⚙️ 灵活配置
                </span>
                <span v-if="currentAuditMode === 'FLEXIBLE_HYBRID'" class="w-4 h-4 rounded-full bg-purple-600 text-white text-[10px] flex items-center justify-center font-bold">✓</span>
              </div>
              <p class="text-[10px] leading-tight" :class="currentAuditMode === 'FLEXIBLE_HYBRID' ? 'text-purple-700 font-medium' : 'text-slate-400'">
                AI 初验 + 人工复核
              </p>
            </div>
            <span class="text-[9px] mt-1.5 block font-medium" :class="currentAuditMode === 'FLEXIBLE_HYBRID' ? 'text-purple-600' : 'text-slate-400'">
              多附件/承诺书 · 专人审验
            </span>
          </button>
        </div>
      </div>

      <!-- 灵活配置模式提示卡片 -->
      <div v-if="currentAuditMode === 'FLEXIBLE_HYBRID'" class="bg-gradient-to-r from-purple-900 via-indigo-950 to-slate-900 text-white p-3.5 rounded-2xl shadow-md border border-purple-500/30 flex items-start gap-2.5 animate-fade-in">
        <span class="text-xl">⚙️</span>
        <div class="space-y-0.5 text-xs">
          <div class="font-bold text-purple-200">当前申报通道：灵活配置（AI 自动化 + 人工审核）</div>
          <p class="text-[11px] text-slate-300 leading-relaxed">
            除完成身份证、房产证、行驶证基础比对外，需上传多张必要证明附件与承诺书。先经 AI 基础校验，校验通过后自动流转物业人工复核。
          </p>
        </div>
      </div>


      <!-- 三证合一概念科普卡片 -->
      <div class="bg-gradient-to-r from-blue-900 via-indigo-900 to-slate-900 text-white p-4 rounded-2xl shadow-md space-y-2">
        <div class="flex items-center gap-2">
          <span class="text-lg">🛡️</span>
          <div>
            <h2 class="text-xs font-black tracking-wide text-blue-200">老旧小区停车“三证合一”刚性准入准则</h2>
            <p class="text-[10px] text-slate-300">杜绝外来车蹭停 · 杜绝私相授受 · 唯有一致方可进入队列</p>
          </div>
        </div>
        <div class="grid grid-cols-3 gap-1.5 pt-1 text-[10px] text-center font-semibold">
          <div
            @click="idCardScanned ? step = 1 : null"
            class="rounded-lg py-1 px-1 border transition cursor-pointer"
            :class="idCardScanned ? 'bg-emerald-500/20 text-emerald-300 border-emerald-400/40 font-bold' : 'bg-white/10 text-slate-300 border-white/10'"
          >
            证一:身份证
          </div>
          <div
            @click="propertyVerified ? step = 2 : null"
            class="rounded-lg py-1 px-1 border transition cursor-pointer"
            :class="propertyVerified ? 'bg-emerald-500/20 text-emerald-300 border-emerald-400/40 font-bold' : 'bg-white/10 text-slate-300 border-white/10'"
          >
            证二:房产证
          </div>
          <div
            @click="licenseScanned || inputPlate ? step = 3 : null"
            class="rounded-lg py-1 px-1 border transition cursor-pointer"
            :class="licenseScanned || inputPlate ? 'bg-emerald-500/20 text-emerald-300 border-emerald-400/40 font-bold' : 'bg-white/10 text-slate-300 border-white/10'"
          >
            证三:行驶证
          </div>
        </div>
      </div>

      <!-- 步骤指示器 -->
      <div class="bg-white rounded-2xl p-4 shadow-sm border border-slate-200">
        <div class="flex items-center justify-between text-xs font-bold text-slate-700">
          <button
            @click="step = 1"
            class="flex items-center gap-1.5 transition"
            :class="step === 1 ? 'text-blue-600 font-black' : (idCardScanned ? 'text-emerald-600' : 'text-slate-400')"
          >
            <span>身份核验</span>
          </button>
          <span class="text-slate-300">──</span>
          <button
            @click="idCardScanned ? step = 2 : null"
            class="flex items-center gap-1.5 transition"
            :class="step === 2 ? 'text-blue-600 font-black' : (propertyVerified ? 'text-emerald-600' : 'text-slate-400')"
          >
            <span>房产核定</span>
          </button>
          <span class="text-slate-300">──</span>
          <button
            @click="idCardScanned && propertyVerified ? step = 3 : null"
            class="flex items-center gap-1.5 transition"
            :class="step === 3 ? 'text-blue-600 font-black' : (licenseScanned ? 'text-emerald-600' : 'text-slate-400')"
          >
            <span>车辆填报</span>
          </button>
        </div>
      </div>

      <!-- ================= 步骤 1: 证一·实人认证 (身份证上传与拟真 OCR) ================= -->
      <div v-if="step === 1" class="bg-white rounded-2xl p-5 shadow-sm border border-slate-200 space-y-4 animate-fade-in">
        <div>
          <div class="flex items-center justify-between">
            <h3 class="text-sm font-black text-slate-900">第一证：申请人居民身份证核验</h3>
            <span class="text-[10px] bg-blue-50 text-blue-700 font-bold px-2 py-0.5 rounded-full border border-blue-200">实人认证</span>
          </div>
          <p class="text-xs text-slate-400 mt-0.5">确立申请人法律主体身份，系统仅存编号与Hash（隐私合规）</p>
        </div>

        <OcrUploadBox
          label="拍摄 / 上传居民身份证原件照片"
          tip="系统自动 OCR 识别姓名与身份证号 (数据脱敏留痕)"
          mockType="id_card"
          @scanned="handleIdCardScanned"
        />

        <div v-if="idCardScanned" class="bg-slate-50 p-3.5 rounded-xl border border-slate-200 space-y-2 text-xs">
          <div class="flex justify-between">
            <span class="text-slate-500">申请人姓名:</span>
            <span class="font-bold text-slate-800">{{ userStore.realName }}</span>
          </div>
          <div class="flex justify-between">
            <span class="text-slate-500">证件号码:</span>
            <span class="font-mono font-bold text-slate-800">310104********1111</span>
          </div>
          <div class="flex justify-between">
            <span class="text-slate-500">OCR 证号核验:</span>
            <span class="font-semibold text-emerald-600">✓ 公安实人认证库一致</span>
          </div>
        </div>

        <FaceCaptureBox
          v-if="idCardScanned"
          :name="userStore.realName"
          persona="zhang"
          title="实人认证 · 采集申请人人脸"
          hint="采集完成后自动与身份证照片完成人证比对"
          @matched="faceMatched = true"
        />

        <button
          @click="step = 2"
          :disabled="!idCardScanned || !faceMatched"
          class="w-full py-3 rounded-xl bg-blue-600 hover:bg-blue-700 text-white font-bold text-xs shadow-md transition disabled:opacity-40 disabled:pointer-events-none flex items-center justify-center gap-1.5"
        >
          <span v-if="!idCardScanned">请先上传身份证</span>
          <span v-else-if="!faceMatched">请先完成人脸人证比对</span>
          <span v-else>人证比对通过，下一步核验第二证(房产证) ➔</span>
        </button>
      </div>

      <!-- ================= 步骤 2: 证二·房产核定 (不动产权属证明) ================= -->
      <div v-else-if="step === 2" class="bg-white rounded-2xl p-5 shadow-sm border border-slate-200 space-y-4 animate-fade-in">
        <div class="flex items-center justify-between">
          <div>
            <h3 class="text-sm font-black text-slate-900">第二证：本小区不动产权属证明</h3>
            <p class="text-[11px] text-slate-400 mt-0.5">依据政务隐私合规要求，请上传产证原件由系统提取坐落并反向比对</p>
          </div>
          <span class="text-[11px] font-bold px-2 py-0.5 rounded-full border transition" :class="propertyVerified ? 'text-emerald-600 bg-emerald-50 border-emerald-200' : 'text-blue-600 bg-blue-50 border-blue-200'">
            {{ propertyVerified ? '✓ 核验通过' : '政务隐私合规' }}
          </span>
        </div>

        <!-- 强化“拍摄/上传不动产权证书原件”按钮与 OCR 识别 -->
        <OcrUploadBox
          label="拍摄 / 上传不动产权证书 (房产证) 原件"
          tip="系统自动 OCR 提取产证编号、权利人与房屋坐落，并与市不动产中心联网反向比对 (图片不落盘)"
          mockType="property_cert"
          @scanned="handlePropertyScanned"
        />

        <!-- 未上传时的政务隐私合规引导说明 -->
        <div v-if="!propertyCertScanned" class="bg-blue-50/80 border border-blue-200/70 rounded-2xl p-4 text-xs text-blue-900 space-y-1.5 animate-fade-in">
          <div class="flex items-center gap-1.5 font-bold text-blue-950">
            <span class="text-sm">🔒</span>
            <span>政务数据隐私保护合规规范</span>
          </div>
          <p class="text-[11px] text-blue-800 leading-relaxed">
            为严格保障居民不动产隐私，系统遵循政务最小必要原则，<strong>严禁预先枚举用户名下全量房产</strong>。请先拍照或上传不动产权证，系统将基于证照信息与本小区产权数据库进行反向验真。
          </p>
        </div>

        <!-- 上传/模拟后：展示扫描带出的本小区核验房产及反向比对通过结果 -->
        <div v-if="propertyCertScanned" class="space-y-3 animate-fade-in">
          <div class="flex items-center justify-between">
            <span class="text-xs font-bold text-slate-700">市不动产登记中心反向比对结果</span>
            <span class="text-[10px] text-emerald-600 font-semibold flex items-center gap-1">
              <span class="w-1.5 h-1.5 rounded-full bg-emerald-500 animate-ping"></span>
              联网比对完成
            </span>
          </div>

          <!-- 本小区核验房产卡片 -->
          <div
            @click="selectedProperty = '龙华新村 12号302室'"
            class="p-4 rounded-2xl border-2 transition cursor-pointer flex items-start justify-between shadow-sm"
            :class="selectedProperty === '龙华新村 12号302室' ? 'border-emerald-500 bg-emerald-50/70 ring-2 ring-emerald-500/20' : 'border-slate-200 bg-white hover:border-slate-300'"
          >
            <div class="space-y-1.5 flex-1 pr-2">
              <div class="flex items-center gap-1.5 font-bold text-xs text-slate-800">
                <span class="text-blue-600 text-sm">🏠</span>
                <span class="text-sm font-black text-slate-900">本小区核验房产：龙华新村 12号302室</span>
                <span class="text-[10px] text-emerald-700 bg-emerald-100 px-2 py-0.5 rounded-full font-semibold border border-emerald-300">
                  本小区产权房
                </span>
              </div>
              <div class="text-[11px] text-slate-600 space-y-0.5 pt-0.5">
                <div>产证所有人: <strong class="text-slate-800">{{ userStore.realName || '张欣欣' }}</strong> (与第一证完全一致)</div>
                <div class="font-mono text-slate-500 text-[10px]">产证编号: 沪(2022)徐字不动产权第019823号</div>
              </div>
              <div class="pt-1 text-[11px] font-bold text-emerald-700 flex items-center gap-1.5">
                <span class="w-4 h-4 rounded-full bg-emerald-500 text-white flex items-center justify-center text-[10px]">✓</span>
                <span>已与市不动产登记中心完成核验比对通过</span>
              </div>
            </div>
            <span
              class="w-5 h-5 rounded-full border-2 flex items-center justify-center mt-1 border-emerald-600 bg-emerald-600 text-white text-[11px] font-bold"
            >
              ✓
            </span>
          </div>

          <!-- 外区房产拦截演示卡片 (满足演示指南与 AGENTS.md 红线3) -->
          <div
            @click="handleSelectExternalProperty"
            class="p-4 rounded-2xl border-2 border-dashed border-rose-200 bg-rose-50/40 opacity-75 hover:opacity-100 transition cursor-pointer flex items-start justify-between shadow-sm"
          >
            <div class="space-y-1.5 flex-1 pr-2">
              <div class="flex items-center gap-1.5 font-bold text-xs text-slate-800">
                <span class="text-rose-500 text-sm">🏢</span>
                <span class="text-sm font-bold text-slate-600 line-through">浦东新区世纪大道 888弄 201室</span>
                <span class="text-[10px] text-rose-700 bg-rose-100 px-2 py-0.5 rounded-full font-semibold border border-rose-300">
                  非本小区房产 (不可选)
                </span>
              </div>
              <div class="text-[11px] text-slate-500 space-y-0.5 pt-0.5">
                <div>产证所有人: {{ userStore.realName || '张欣欣' }}</div>
                <div class="font-mono text-[10px]">产证编号: 沪(2021)浦字不动产权第088219号</div>
              </div>
              <div class="pt-1 text-[11px] font-bold text-rose-600 flex items-center gap-1.5">
                <span class="w-4 h-4 rounded-full bg-rose-500 text-white flex items-center justify-center text-[10px]">✕</span>
                <span>跨区校验拦截：非本小区房屋，不可申报车位</span>
              </div>
            </div>
            <span class="text-[10px] text-rose-500 bg-rose-100/80 px-2 py-1 rounded-lg font-bold shrink-0 mt-1">
              点击测试拦截
            </span>
          </div>
        </div>

        <!-- 按钮组 -->
        <div class="flex items-center gap-2 pt-2">
          <button
            @click="step = 1"
            class="py-3 px-4 rounded-xl bg-slate-100 hover:bg-slate-200 text-slate-700 font-semibold text-xs transition"
          >
            ⬅️ 返回身份证
          </button>
          <button
            @click="step = 3"
            :disabled="!propertyVerified"
            class="flex-1 py-3 rounded-xl bg-blue-600 hover:bg-blue-700 text-white font-bold text-xs shadow-md transition disabled:opacity-40 disabled:pointer-events-none flex items-center justify-center gap-1.5"
          >
            <span v-if="!propertyVerified">请先上传不动产权证书完成核验</span>
            <span v-else>第二证已核定，下一步核验第三证(行驶证) ➔</span>
          </button>
        </div>
      </div>

      <!-- ================= 步骤 3: 证三·行驶证与车牌申报 + 三证合一综合看板 ================= -->
      <div v-else-if="step === 3" class="space-y-4 animate-fade-in">
        <!-- 车辆与车牌号填报模块 (证三: 行驶证) -->
        <div class="bg-white rounded-2xl p-5 shadow-sm border border-slate-200 space-y-4">
          <div class="flex items-center justify-between">
            <div>
              <h3 class="text-sm font-black text-slate-900">第三证：机动车行驶证与车牌填报</h3>
              <p class="text-[11px] text-slate-400 mt-0.5">支持拍摄识别行驶证原件，也可自主填报车牌</p>
            </div>

          </div>

          <!-- 行驶证拍照上传 / OCR 扫描识别 -->
          <div>
            <OcrUploadBox
              label="拍摄 / 上传机动车行驶证正本原件"
              tip="系统自动 OCR 提取号牌号码、车辆类型与所有人姓名"
              mockType="license"
              @scanned="handleLicenseScanned"
            />
          </div>

          <!-- 车牌号输入框 -->
          <div>
            <div class="flex items-center justify-between mb-1">
              <label class="block font-bold text-xs text-slate-700">
                申报号牌号码 <span class="text-rose-500">*</span>
              </label>
              <!-- 快捷从名下带入 -->
              <div class="flex flex-wrap items-center gap-1.5 text-[11px]">
                <span class="text-slate-400">快速填入:</span>
                <button
                  type="button"
                  @click="quickFillPlate('沪A·12345', 'self')"
                  class="px-2 py-0.5 rounded bg-slate-100 hover:bg-blue-100 text-blue-700 font-mono font-bold border border-slate-200 transition"
                >
                  沪A·12345 (本人车)
                </button>
                <button
                  type="button"
                  @click="quickFillPlate('沪A·67890', 'spouse')"
                  class="px-2 py-0.5 rounded bg-slate-100 hover:bg-amber-100 text-amber-700 font-mono font-bold border border-slate-200 transition"
                >
                  沪A·67890 (配偶车)
                </button>
                <button
                  type="button"
                  @click="quickFillPlate('沪A·88888', 'family')"
                  class="px-2 py-0.5 rounded bg-slate-100 hover:bg-indigo-100 text-indigo-700 font-mono font-bold border border-slate-200 transition"
                >
                  沪A·88888 (家人车)
                </button>
              </div>
            </div>

            <div class="relative">
              <input
                v-model="inputPlate"
                type="text"
                required
                maxlength="10"
                placeholder="例如: 沪A·12345 或 沪AD12345"
                class="w-full px-4 py-3 rounded-xl border-2 border-slate-200 focus:border-blue-600 focus:outline-none font-mono font-bold text-base tracking-wider text-slate-800 uppercase placeholder:font-sans placeholder:text-xs placeholder:font-normal"
              />
              <span class="absolute right-3 top-3 text-xs text-slate-400 font-medium">
                蓝牌 / 绿牌均可
              </span>
            </div>
          </div>

          <!-- 车辆权属关系单选 (本人、配偶、家人) -->
          <div class="pt-1">
            <label class="block font-bold text-xs text-slate-700 mb-2">机动车行驶证所有人权属关系</label>
            <div class="grid grid-cols-1 sm:grid-cols-3 gap-2">
              <label
                @click="handleCarTypeChange('self')"
                class="p-2.5 rounded-xl border-2 cursor-pointer transition flex items-center gap-2 text-xs"
                :class="carRelationship === 'self' ? 'border-blue-600 bg-blue-50/60 font-bold text-blue-900' : 'border-slate-200 bg-white text-slate-600 hover:border-slate-300'"
              >
                <input type="radio" name="car_type" :checked="carRelationship === 'self'" class="text-blue-600" />
                <div>
                  <span class="block">本人名下车辆</span>
                  <span class="text-[10px] text-slate-400 font-normal">车主 == 房产人</span>
                </div>
              </label>

              <label
                @click="handleCarTypeChange('spouse')"
                class="p-2.5 rounded-xl border-2 cursor-pointer transition flex items-center gap-2 text-xs"
                :class="carRelationship === 'spouse' ? 'border-amber-500 bg-amber-50/60 font-bold text-amber-900' : 'border-slate-200 bg-white text-slate-600 hover:border-slate-300'"
              >
                <input type="radio" name="car_type" :checked="carRelationship === 'spouse'" class="text-amber-600" />
                <div>
                  <span class="block">配偶车辆 (夫妻车)</span>
                  <span class="text-[10px] text-slate-400 font-normal">需补充结婚证</span>
                </div>
              </label>

              <label
                @click="handleCarTypeChange('family')"
                class="p-2.5 rounded-xl border-2 cursor-pointer transition flex items-center gap-2 text-xs"
                :class="carRelationship === 'family' ? 'border-indigo-600 bg-indigo-50/60 font-bold text-indigo-900' : 'border-slate-200 bg-white text-slate-600 hover:border-slate-300'"
              >
                <input type="radio" name="car_type" :checked="carRelationship === 'family'" class="text-indigo-600" />
                <div>
                  <span class="block">直系亲属 / 家人车</span>
                  <span class="text-[10px] text-slate-400 font-normal">需补充户口本</span>
                </div>
              </label>
            </div>
          </div>

          <!-- 夫妻车辆结婚证上传区域 (当选择夫妻车辆时展示) -->
          <div v-if="carRelationship === 'spouse'" class="bg-amber-50/90 rounded-2xl p-4 border-2 border-amber-300 space-y-3 animate-fade-in">
            <div class="flex items-center justify-between">
              <div class="flex items-center gap-2">
                <span class="text-xl">💍</span>
                <div>
                  <h4 class="text-xs font-bold text-amber-900">夫妻关系证明 (结婚证)</h4>
                  <p class="text-[11px] text-amber-700">证明产权人【{{ userStore.realName }}】与车主具有合法婚姻共有权</p>
                </div>
              </div>
              <span class="text-[10px] bg-amber-200 text-amber-900 px-2 py-0.5 rounded font-bold">必传</span>
            </div>

            <div>
              <label class="block text-xs font-bold text-amber-900 mb-1">配偶 (行驶证登记车主) 姓名</label>
              <input
                v-model="spouseName"
                type="text"
                placeholder="请输入配偶真实姓名，如: 李玉霞"
                class="w-full px-3 py-2 rounded-xl border border-amber-300 bg-white text-xs font-medium focus:ring-2 focus:ring-amber-500"
              />
            </div>

            <OcrUploadBox
              label="上传中华人民共和国结婚证照片"
              tip="系统自动 OCR 比对双方婚姻证明"
              mockType="marriage"
              @scanned="marriageCertUploaded = true"
            />

            <div v-if="marriageCertUploaded" class="p-2.5 rounded-xl bg-white text-xs text-emerald-700 font-bold border border-emerald-300 flex items-center justify-between">
              <span>✓ 结婚证比对一致 (核验夫妻关系真实有效)</span>
              <span class="text-[10px] text-slate-400 font-mono">J310104-2022-0089</span>
            </div>
          </div>

          <!-- 家人车辆户口本上传区域 (当选择直系亲属/家人车辆时展示) -->
          <div v-if="carRelationship === 'family'" class="bg-indigo-50/90 rounded-2xl p-4 border-2 border-indigo-300 space-y-3 animate-fade-in">
            <div class="flex items-center justify-between">
              <div class="flex items-center gap-2">
                <span class="text-xl">👨‍👩‍👧</span>
                <div>
                  <h4 class="text-xs font-bold text-indigo-900">直系亲属/家庭成员关系证明 (居民户口簿)</h4>
                  <p class="text-[11px] text-indigo-700">证明产权人【{{ userStore.realName }}】与车主具有合法直系亲属共有权</p>
                </div>
              </div>
              <span class="text-[10px] bg-indigo-200 text-indigo-900 px-2 py-0.5 rounded font-bold">必传</span>
            </div>

            <div class="space-y-2.5">
              <div>
                <label class="block text-xs font-bold text-indigo-900 mb-1">家人 (车主) 姓名</label>
                <input
                  v-model="familyMemberName"
                  type="text"
                  placeholder="请输入家人车主姓名，如: 张建国"
                  class="w-full px-3 py-2.5 rounded-xl border border-indigo-200 bg-white text-xs font-bold text-slate-800 focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500"
                />
              </div>

              <div>
                <label class="block text-xs font-bold text-indigo-900 mb-1.5">与房产所有权人直系关系</label>
                <div class="grid grid-cols-3 gap-2">
                  <button
                    type="button"
                    v-for="item in [
                      { value: 'PARENT', label: '父亲 / 母亲', icon: '👴' },
                      { value: 'CHILD', label: '子女 (同住)', icon: '🧒' },
                      { value: 'OTHER', label: '其他同户直系', icon: '👥' }
                    ]"
                    :key="item.value"
                    @click="familyRelationType = item.value"
                    class="py-2.5 px-2 rounded-xl border text-xs transition flex flex-col items-center justify-center gap-0.5 shadow-sm"
                    :class="familyRelationType === item.value 
                      ? 'bg-indigo-600 text-white font-bold border-indigo-600 ring-2 ring-indigo-500/30' 
                      : 'bg-white text-slate-600 border-indigo-200 hover:border-indigo-300 hover:bg-indigo-50/50'"
                  >
                    <span class="text-sm">{{ item.icon }}</span>
                    <span class="text-[11px]">{{ item.label }}</span>
                  </button>
                </div>
              </div>
            </div>

            <OcrUploadBox
              label="上传中华人民共和国居民户口簿照片"
              tip="系统自动 OCR 比对公安常住人口常住登记卡直系亲属关系"
              mockType="family_book"
              @scanned="familyCertUploaded = true"
            />

            <div v-if="familyCertUploaded" class="p-2.5 rounded-xl bg-white text-xs text-emerald-700 font-bold border border-emerald-300 flex items-center justify-between">
              <span>✓ 户口本比对一致 (常住人口登记卡直系亲属关系真实有效)</span>
              <span class="text-[10px] text-slate-400 font-mono">310104-HK-982103</span>
            </div>
          </div>
        </div>

        <!-- 灵活配置模式: 多张必填证明附件材料清单与上传 -->
        <div v-if="currentAuditMode === 'FLEXIBLE_HYBRID'" class="bg-white rounded-2xl p-5 shadow-sm border border-slate-200 space-y-3 animate-fade-in">
          <div class="flex items-center justify-between">
            <div>
              <h3 class="text-sm font-black text-slate-900 flex items-center gap-1.5">
                <span>📎 必要审核证明附件 (多张必填)</span>
                <span class="text-[10px] bg-purple-100 text-purple-800 font-bold px-2 py-0.5 rounded-full border border-purple-200">
                  人工审核材料
                </span>
              </h3>
              <p class="text-[11px] text-slate-400 mt-0.5">附件将流转物业专人逐项审验，请确保材料真实有效</p>
            </div>
            <button
              type="button"
              @click="quickLoadDefaultAttachments"
              class="text-[10px] px-2 py-1 rounded-lg bg-purple-50 hover:bg-purple-100 text-purple-700 font-bold border border-purple-200 transition"
            >
              ⚡ 一键齐备示例附件
            </button>
          </div>

          <div class="space-y-2 pt-1">
            <div
              v-for="(item, idx) in allActiveAttachmentItems"
              :key="idx"
              class="p-3 rounded-xl border transition flex items-center justify-between gap-3 text-xs"
              :class="item.file_name ? 'bg-purple-50/50 border-purple-200' : 'bg-slate-50 border-slate-200'"
            >
              <div class="flex items-center gap-2.5 min-w-0">
                <span class="text-lg shrink-0">{{ item.icon || '📄' }}</span>
                <div class="min-w-0">
                  <div class="font-bold text-slate-800 truncate flex items-center gap-1.5">
                    <span>{{ item.title }}</span>
                    <span v-if="item.required" class="text-[10px] text-rose-500 font-normal">*必填</span>
                  </div>
                  <div class="text-[10px] truncate" :class="item.file_name ? 'text-purple-700 font-mono' : 'text-slate-400'">
                    {{ item.file_name ? `已就绪: ${item.file_name}` : '未上传文件' }}
                  </div>
                </div>
              </div>

              <div class="shrink-0 flex items-center gap-2">
                <label class="cursor-pointer px-2.5 py-1 rounded-lg text-[11px] font-bold transition border"
                  :class="item.file_name ? 'bg-white text-purple-700 border-purple-300 hover:bg-purple-50' : 'bg-purple-600 text-white border-purple-600 hover:bg-purple-700'"
                >
                  <input type="file" accept="image/*,.pdf" class="hidden" @change="(e) => onUploadAttachment(e, idx)" />
                  <span>{{ item.file_name ? '重新上传' : '上传附件' }}</span>
                </label>
                <span
                  v-if="item.file_name"
                  class="w-5 h-5 rounded-full bg-emerald-100 text-emerald-700 flex items-center justify-center font-bold text-xs"
                >
                  ✓
                </span>
              </div>
            </div>
          </div>

          <button
            type="button"
            @click="addCustomAttachment"
            class="w-full py-2.5 rounded-xl border border-dashed border-purple-300 hover:border-purple-500 bg-purple-50/50 hover:bg-purple-100/50 text-purple-700 text-xs font-bold transition flex items-center justify-center gap-1.5"
          >
            <span>➕ 自主增加其他证明材料 (如租赁合同、共有说明、代管凭证等)</span>
          </button>

          <div class="p-2.5 bg-slate-50 rounded-xl border border-slate-200 text-[10px] text-slate-500 leading-relaxed">
            🛡️ <strong>合规说明</strong>：根据政务最小化原则，附件结构与哈希凭据经 AI 预验后即时上链存证，服务端不落地保存个人高清原图。
          </div>
        </div>


        <!-- 🌟 核心高光看板：三证合一智能比对与核验报告 🌟 -->
        <div class="bg-gradient-to-br from-emerald-900 via-teal-900 to-slate-900 text-white rounded-2xl p-5 shadow-xl border-2 border-emerald-400/40 space-y-3 relative overflow-hidden">
          <div class="flex items-center justify-between">
            <div class="flex items-center gap-2">
              <div class="w-7 h-7 rounded-lg bg-emerald-500 text-white flex items-center justify-center font-black text-xs shadow-md">
                ✓
              </div>
              <div>
                <h3 class="text-xs font-black text-emerald-200">三证合一智能比对与核验报告</h3>
                <p class="text-[10px] text-teal-300">公安/不动产/交管大数据自动跨库交叉稽核</p>
              </div>
            </div>
            <span class="px-2 py-0.5 rounded-full text-[10px] font-extrabold bg-emerald-500/30 text-emerald-200 border border-emerald-400/40">
              跨库交叉核验
            </span>
          </div>

          <div class="bg-black/30 rounded-xl p-3 text-xs space-y-2 font-mono divide-y divide-white/10">
            <div class="flex items-center justify-between pt-1">
              <span class="text-slate-300 font-sans">证一·身份证核验:</span>
              <span class="text-emerald-400 font-bold flex items-center gap-1">
                <span>{{ userStore.realName }} (310104********1111)</span>
                <span>✓ 通过</span>
              </span>
            </div>
            <div class="flex items-center justify-between pt-2">
              <span class="text-slate-300 font-sans">证二·房产证核验:</span>
              <span class="text-emerald-400 font-bold flex items-center gap-1">
                <span>{{ selectedProperty }}</span>
                <span>✓ 本小区产权属实</span>
              </span>
            </div>
            <div class="flex items-center justify-between pt-2">
              <span class="text-slate-300 font-sans">证三·行驶证核验:</span>
              <span class="text-emerald-400 font-bold flex items-center gap-1" v-if="carRelationship === 'self'">
                <span>{{ inputPlate }} (车主: {{ userStore.realName }})</span>
                <span>✓ 完全一致</span>
              </span>
              <span class="text-amber-300 font-bold flex items-center gap-1" v-else-if="carRelationship === 'spouse'">
                <span>{{ inputPlate }} (车主: {{ spouseName }})</span>
                <span>{{ marriageCertUploaded ? '✓ 夫妻共有认定' : '⚠️ 待传结婚证' }}</span>
              </span>
              <span class="text-indigo-300 font-bold flex items-center gap-1" v-else-if="carRelationship === 'family'">
                <span>{{ inputPlate }} (车主: {{ familyMemberName }})</span>
                <span>{{ familyCertUploaded ? '✓ 户口本直系亲属共有认定' : '⚠️ 待传户口本' }}</span>
              </span>
            </div>
          </div>

          <div class="pt-1 flex items-center justify-between text-xs">
            <span class="text-[11px] text-teal-200 font-medium">三证综合交叉判定:</span>
            <span
              class="font-black px-2.5 py-1 rounded-lg text-xs"
              :class="isThreeCertValid ? 'bg-emerald-500 text-white shadow-md' : 'bg-rose-900/60 text-rose-200 border border-rose-400/40'"
            >
              {{ isThreeCertValid ? (currentAuditMode === 'FLEXIBLE_HYBRID' ? '🌟 AI初审预验通过 (待流转人工审核)' : '🌟 三证合一核验全部通过 (准予入队)') : '等待材料补齐' }}
            </span>
          </div>
        </div>

        <!-- 按钮组 -->
        <div class="flex items-center gap-2 pt-2">
          <button
            @click="step = 2"
            class="py-3 px-4 rounded-2xl bg-white hover:bg-slate-50 text-slate-700 font-semibold text-xs border border-slate-200 transition"
          >
            ⬅️ 返回房产核定
          </button>
          <button
            @click="handleSubmitApplication"
            :disabled="submitting || !isThreeCertValid"
            class="flex-1 py-3.5 rounded-2xl bg-blue-600 hover:bg-blue-700 text-white font-bold text-sm shadow-lg shadow-blue-500/25 active:scale-[0.98] transition disabled:opacity-40 disabled:pointer-events-none flex items-center justify-center gap-2"
          >
            <span v-if="submitting">正在上链存证并提交申请...</span>
            <span v-else>
              {{ currentAuditMode === 'FLEXIBLE_HYBRID' ? '确认提交 (AI初验后流转物业人工复核)' : '确认提交三证申请 (进入阳光排位)' }}
            </span>
          </button>
        </div>
      </div>
    </main>

    <!-- 夫妻车切换弹窗引导 -->
    <div v-if="showSpouseConfirmModal" class="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/60 backdrop-blur-sm animate-fade-in">
      <div class="bg-white rounded-3xl max-w-sm w-full p-6 text-center space-y-4 shadow-2xl border border-slate-100">
        <div class="w-14 h-14 rounded-2xl bg-amber-100 text-amber-600 flex items-center justify-center mx-auto text-2xl shadow-inner">
          💍
        </div>
        <div>
          <h3 class="text-base font-extrabold text-slate-900">申报配偶共有车辆 (夫妻车)</h3>
          <p class="text-xs text-slate-500 mt-2 leading-relaxed">
            若所申报车辆行驶证所有人为配偶，依据管理规定需上传结婚证明，系统将自动核验并认定为合法夫妻共有车辆。
          </p>
        </div>

        <div class="flex items-center gap-2 pt-2">
          <button
            @click="showSpouseConfirmModal = false"
            class="flex-1 py-2.5 rounded-xl bg-slate-100 hover:bg-slate-200 text-slate-600 font-semibold text-xs transition"
          >
            取消
          </button>
          <button
            @click="confirmSpouseMode"
            class="flex-1 py-2.5 rounded-xl bg-amber-600 hover:bg-amber-700 text-white font-bold text-xs shadow-md transition"
          >
            确认开启 (上传结婚证)
          </button>
        </div>
      </div>
    </div>

    <!-- 直系亲属/家人车切换弹窗引导 -->
    <div v-if="showFamilyConfirmModal" class="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/60 backdrop-blur-sm animate-fade-in">
      <div class="bg-white rounded-3xl max-w-sm w-full p-6 text-center space-y-4 shadow-2xl border border-slate-100">
        <div class="w-14 h-14 rounded-2xl bg-indigo-100 text-indigo-600 flex items-center justify-center mx-auto text-2xl shadow-inner">
          👨‍👩‍👧
        </div>
        <div>
          <h3 class="text-base font-extrabold text-slate-900">申报直系亲属 / 家人车辆</h3>
          <p class="text-xs text-slate-500 mt-2 leading-relaxed">
            若所申报车辆为父母、子女等同住直系亲属所有，依据规定需补充核验居民户口簿，系统将自动核验常住人口直系亲属共有权。
          </p>
        </div>

        <div class="flex items-center gap-2 pt-2">
          <button
            @click="showFamilyConfirmModal = false"
            class="flex-1 py-2.5 rounded-xl bg-slate-100 hover:bg-slate-200 text-slate-600 font-semibold text-xs transition"
          >
            取消
          </button>
          <button
            @click="confirmFamilyMode"
            class="flex-1 py-2.5 rounded-xl bg-indigo-600 hover:bg-indigo-700 text-white font-bold text-xs shadow-md transition"
          >
            确认开启 (上传户口本)
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../store/user'
import api from '../api'
import OcrUploadBox from '../components/OcrUploadBox.vue'
import FaceCaptureBox from '../components/FaceCaptureBox.vue'
import { showToast, showAlert } from '../utils/dialog'

const router = useRouter()
const userStore = useUserStore()

const currentAuditMode = ref('THREE_CERT_AUTO')
const communityAuditMode = ref('THREE_CERT_AUTO')
const lastRejectReason = ref('')

const loadAuditMode = async () => {
  try {
    const res = await api.get('/parking/audit-mode')
    if (res.code === 200 && res.data) {
      communityAuditMode.value = res.data.audit_mode || 'THREE_CERT_AUTO'
      currentAuditMode.value = res.data.audit_mode || 'THREE_CERT_AUTO'
    }
  } catch (err) {
    console.error('加载小区审核模式失败:', err)
  }
}


onMounted(async () => {
  if (userStore.isPropertyAdmin) {
    showToast('当前为物业企业管理账号，无权发起车位申请！请切换为业主账号（如张欣欣）。', 'warning')
    router.replace('/property/dashboard')
    return
  }
  await loadAuditMode()
  if (userStore.homeData?.view_state === 'REJECTED' && userStore.homeData?.my_queue?.reject_reason) {
    lastRejectReason.value = userStore.homeData.my_queue.reject_reason
  }
})

const step = ref(1)
const idCardScanned = ref(false)
const faceMatched = ref(false)
const propertyCertScanned = ref(false)
const selectedProperty = ref('')

// 车牌填报与行驶证逻辑
const licenseScanned = ref(false)
const inputPlate = ref('沪A·12345')
const carRelationship = ref('self') // 'self' | 'spouse' | 'family'
const isSpouseCar = computed(() => carRelationship.value === 'spouse')

// 配偶车状态
const spouseName = ref('李玉霞')
const showSpouseConfirmModal = ref(false)
const marriageCertUploaded = ref(false)

// 家人车状态 (户口本)
const familyMemberName = ref('张建国')
const familyRelationType = ref('PARENT')
const showFamilyConfirmModal = ref(false)
const familyCertUploaded = ref(false)

// 灵活配置模式: 多张必填证明附件材料
const requiredAttachmentItems = ref([
  {
    type: 'LICENSE',
    type_label: '机动车行驶证正副页',
    title: '机动车行驶证正副页彩色扫描件',
    file_name: '机动车行驶证正副本扫描件_沪A12345.jpg',
    required: true,
    icon: '🚗'
  },
  {
    type: 'PROPERTY',
    type_label: '不动产权证书',
    title: '不动产权证书坐落与权利人页',
    file_name: '不动产权证书_沪(2022)徐字不动产权第019823号.jpg',
    required: true,
    icon: '🏢'
  },
  {
    type: 'COMMITMENT',
    type_label: '车位合规使用承诺书',
    title: '老旧小区车位规范停放承诺书 (签字确认)',
    file_name: '车位合规停放与安全管理承诺书.pdf',
    required: true,
    icon: '📝'
  }
])

const dynamicSpouseAttachment = computed(() => {
  if (carRelationship.value === 'spouse') {
    return {
      type: 'MARRIAGE',
      type_label: '夫妻共有证明',
      title: '中华人民共和国结婚证 (双方信息页)',
      file_name: marriageCertUploaded.value ? '结婚证_J310104-2022-0089.jpg' : '',
      required: true,
      icon: '💍'
    }
  }
  if (carRelationship.value === 'family') {
    return {
      type: 'FAMILY',
      type_label: '直系亲属共有证明',
      title: '居民户口簿 (户口本首页与常住人口登记页)',
      file_name: familyCertUploaded.value ? '居民户口簿_310104-HK-982103.jpg' : '',
      required: true,
      icon: '👨‍👩‍👧'
    }
  }
  return null
})

const allActiveAttachmentItems = computed(() => {
  const list = [...requiredAttachmentItems.value]
  if (dynamicSpouseAttachment.value) {
    list.push(dynamicSpouseAttachment.value)
  }
  return list
})

const areAllRequiredAttachmentsReady = computed(() => {
  return allActiveAttachmentItems.value.every(item => !item.required || !!item.file_name)
})

const onUploadAttachment = (e, idx) => {
  const f = e.target.files && e.target.files[0]
  if (f) {
    requiredAttachmentItems.value[idx].file_name = f.name
    showToast(`已成功载入附件：${f.name}`, 'success')
  }
}

const quickLoadDefaultAttachments = () => {
  requiredAttachmentItems.value[0].file_name = '机动车行驶证正副本扫描件_沪A12345.jpg'
  requiredAttachmentItems.value[1].file_name = '不动产权证书_沪(2022)徐字不动产权第019823号.jpg'
  requiredAttachmentItems.value[2].file_name = '车位合规停放与安全管理承诺书.pdf'
  showToast('已一键齐备标准申报附件材料！', 'success')
}

const addCustomAttachment = () => {
  const customIndex = requiredAttachmentItems.value.filter(i => i.type === 'OTHER').length + 1
  requiredAttachmentItems.value.push({
    type: 'OTHER',
    type_label: '补充材料',
    title: `其他证明凭证材料 #${customIndex}`,
    file_name: '',
    required: false,
    icon: '📑'
  })
  showToast(`已增设补充材料上传位 #${customIndex}`, 'info')
}


const submitting = ref(false)

// 第二证完成判定: 完成产证上传/识别并锁定本小区产权
const propertyVerified = computed(() => propertyCertScanned.value && !!selectedProperty.value)

// 综合三证合一判定
const isThreeCertValid = computed(() => {
  if (!idCardScanned.value || !faceMatched.value) return false
  if (!propertyVerified.value) return false
  if (!inputPlate.value || !inputPlate.value.trim()) return false
  if (carRelationship.value === 'spouse' && !marriageCertUploaded.value) return false
  if (carRelationship.value === 'family' && !familyCertUploaded.value) return false
  if (currentAuditMode.value === 'FLEXIBLE_HYBRID' && !areAllRequiredAttachmentsReady.value) return false
  return true
})

const handleIdCardScanned = () => {
  idCardScanned.value = true
}

const handlePropertyScanned = () => {
  propertyCertScanned.value = true
  selectedProperty.value = '龙华新村 12号302室'
}

const handleSelectExternalProperty = () => {
  showToast('🚫 拦截警告：仅限本小区产权房屋可申请车位，外区房产无法申报！', 'error')
}

const handleLicenseScanned = () => {
  licenseScanned.value = true
  if (carRelationship.value === 'self') {
    inputPlate.value = '沪A·12345'
  }
}

const quickFillPlate = (plate, type) => {
  inputPlate.value = plate
  licenseScanned.value = true
  if (type === 'spouse') {
    showSpouseConfirmModal.value = true
  } else if (type === 'family') {
    showFamilyConfirmModal.value = true
  } else {
    carRelationship.value = 'self'
  }
}

const handleCarTypeChange = (type) => {
  if (type === 'spouse') {
    showSpouseConfirmModal.value = true
  } else if (type === 'family') {
    showFamilyConfirmModal.value = true
  } else {
    carRelationship.value = 'self'
  }
}

const confirmSpouseMode = () => {
  carRelationship.value = 'spouse'
  showSpouseConfirmModal.value = false
}

const confirmFamilyMode = () => {
  carRelationship.value = 'family'
  showFamilyConfirmModal.value = false
}

const handleSubmitApplication = async () => {
  if (!isThreeCertValid.value) {
    showToast('申报材料未完全齐备，请核对！', 'warning')
    return
  }

  submitting.value = true
  try {
    const res = await api.post('/parking/apply', {
      property_no: selectedProperty.value,
      plate_no: inputPlate.value.trim().toUpperCase(),
      is_spouse_car: carRelationship.value === 'spouse',
      car_relationship: carRelationship.value.toUpperCase(),
      property_cert_no: '沪(2022)徐字不动产权第019823号',
      marriage_cert_no: carRelationship.value === 'spouse' ? 'J310104-2022-0089' : null,
      family_cert_no: carRelationship.value === 'family' ? '310104-HK-982103' : null,
      attachments: currentAuditMode.value === 'FLEXIBLE_HYBRID' ? allActiveAttachmentItems.value : [],
      audit_mode: currentAuditMode.value
    })

    showToast(
      res.message || (currentAuditMode.value === 'FLEXIBLE_HYBRID' ? '申报材料已提交！AI初审通过，已流转物业人工审核' : '三证合一核验完全通过！已成功进入排队队列'),
      'success'
    )
    await userStore.fetchHomeData()
    router.push('/')
  } catch (err) {
    showToast('提交失败: ' + err.message, 'error')
  } finally {
    submitting.value = false
  }
}
</script>
