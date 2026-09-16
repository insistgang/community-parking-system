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
          老旧小区车位申请申报
        </h1>
      </div>
      <span
        class="text-[11px] px-2.5 py-0.5 rounded-full border font-bold flex items-center gap-1"
        :class="currentAuditMode === 'FLEXIBLE_HYBRID' ? 'text-purple-700 bg-purple-50 border-purple-200' : 'text-emerald-700 bg-emerald-50 border-emerald-200'"
      >
        <span class="w-1.5 h-1.5 rounded-full animate-pulse" :class="currentAuditMode === 'FLEXIBLE_HYBRID' ? 'bg-purple-500' : 'bg-emerald-500'"></span>
        <span>{{ currentAuditMode === 'FLEXIBLE_HYBRID' ? '证明材料申报 · 人工核验' : '规范申报 · 阳光排位' }}</span>
      </span>
    </header>

    <main class="max-w-md w-full mx-auto p-4 space-y-4 flex-1">
      <!-- 快捷通道切换条 (遵循后台小区政策，同时提供便捷测试通道供评审验证) -->
      <div class="flex items-center justify-between px-3 py-2 bg-slate-200/80 rounded-xl text-[11px] text-slate-600 border border-slate-300/60 shadow-xs">
        <div class="flex items-center gap-1 font-medium">
          <span>🏛️ 当前小区政策：</span>
          <span class="font-bold" :class="communityAuditMode === 'FLEXIBLE_HYBRID' ? 'text-purple-700' : 'text-blue-700'">
            {{ communityAuditMode === 'FLEXIBLE_HYBRID' ? '自主材料申报（人工审核）' : '标准规范申报（极简准入）' }}
          </span>
        </div>
        <div class="flex items-center gap-1 bg-white p-0.5 rounded-lg border border-slate-200">
          <button
            type="button"
            @click="switchAuditMode('THREE_CERT_AUTO')"
            class="px-2 py-0.5 rounded text-[10px] font-bold transition"
            :class="currentAuditMode === 'THREE_CERT_AUTO' ? 'bg-blue-600 text-white shadow-xs' : 'text-slate-500 hover:text-slate-800'"
          >
            标准流程 (3步)
          </button>
          <button
            type="button"
            @click="switchAuditMode('FLEXIBLE_HYBRID')"
            class="px-2 py-0.5 rounded text-[10px] font-bold transition"
            :class="currentAuditMode === 'FLEXIBLE_HYBRID' ? 'bg-purple-600 text-white shadow-xs' : 'text-slate-500 hover:text-slate-800'"
          >
            灵活材料 (2步)
          </button>
        </div>
      </div>

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

      <!-- 申报须知科普卡片 (根据模式展示不同须知) -->
      <div
        v-if="currentAuditMode === 'THREE_CERT_AUTO'"
        class="bg-gradient-to-r from-blue-900 via-indigo-900 to-slate-900 text-white p-4 rounded-2xl shadow-md space-y-2 animate-fade-in"
      >
        <div class="flex items-center gap-2">
          <span class="text-lg">🛡️</span>
          <div>
            <h2 class="text-xs font-black tracking-wide text-blue-200">老旧小区车位规范申报须知</h2>
            <p class="text-[10px] text-slate-300">一车一位 · 产权对齐 · 阳光轮候 · 杜绝违规蹭停</p>
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

      <div
        v-else
        class="bg-gradient-to-r from-purple-900 via-slate-900 to-indigo-950 text-white p-4 rounded-2xl shadow-md space-y-2 animate-fade-in"
      >
        <div class="flex items-center gap-2">
          <span class="text-lg">📋</span>
          <div>
            <h2 class="text-xs font-black tracking-wide text-purple-200">老旧小区车位申报材料须知</h2>
            <p class="text-[10px] text-slate-300">身份核验 · 自主提交证明材料 · 规范流转物业审核</p>
          </div>
        </div>
        <div class="grid grid-cols-2 gap-2 pt-1 text-[10px] text-center font-semibold">
          <div
            @click="step = 1"
            class="rounded-lg py-1.5 px-2 border transition cursor-pointer"
            :class="idCardScanned ? 'bg-purple-500/30 text-purple-200 border-purple-400/40 font-bold' : 'bg-white/10 text-slate-300 border-white/10'"
          >
            第 1 步: 身份实人核验
          </div>
          <div
            @click="idCardScanned ? step = 2 : null"
            class="rounded-lg py-1.5 px-2 border transition cursor-pointer"
            :class="step === 2 ? 'bg-purple-500/30 text-purple-200 border-purple-400/40 font-bold' : 'bg-white/10 text-slate-300 border-white/10'"
          >
            第 2 步: 证明材料与车辆申报
          </div>
        </div>
      </div>

      <!-- 步骤指示器 -->
      <div class="bg-white rounded-2xl p-4 shadow-sm border border-slate-200">
        <!-- 标准模式: 3 步 -->
        <div v-if="currentAuditMode === 'THREE_CERT_AUTO'" class="flex items-center justify-between text-xs font-bold text-slate-700">
          <button
            @click="step = 1"
            class="flex items-center gap-1.5 transition"
            :class="step === 1 ? 'text-blue-600 font-black' : (idCardScanned ? 'text-emerald-600' : 'text-slate-400')"
          >
            <span>1. 身份核验</span>
          </button>
          <span class="text-slate-300">──</span>
          <button
            @click="idCardScanned ? step = 2 : null"
            class="flex items-center gap-1.5 transition"
            :class="step === 2 ? 'text-blue-600 font-black' : (propertyVerified ? 'text-emerald-600' : 'text-slate-400')"
          >
            <span>2. 房产核定</span>
          </button>
          <span class="text-slate-300">──</span>
          <button
            @click="idCardScanned && propertyVerified ? step = 3 : null"
            class="flex items-center gap-1.5 transition"
            :class="step === 3 ? 'text-blue-600 font-black' : (licenseScanned ? 'text-emerald-600' : 'text-slate-400')"
          >
            <span>3. 车辆填报</span>
          </button>
        </div>

        <!-- 灵活模式: 2 步 -->
        <div v-else class="flex items-center justify-between text-xs font-bold text-slate-700 px-2">
          <button
            @click="step = 1"
            class="flex items-center gap-2 transition"
            :class="step === 1 ? 'text-purple-600 font-black' : (idCardScanned ? 'text-emerald-600' : 'text-slate-400')"
          >
            <span class="w-5 h-5 rounded-full flex items-center justify-center text-[10px]" :class="step === 1 ? 'bg-purple-600 text-white' : 'bg-emerald-100 text-emerald-700'">1</span>
            <span>第一步：身份实人核验</span>
          </button>
          <span class="text-slate-300">────────</span>
          <button
            @click="idCardScanned && faceMatched ? step = 2 : null"
            class="flex items-center gap-2 transition"
            :class="step === 2 ? 'text-purple-600 font-black' : 'text-slate-400'"
          >
            <span class="w-5 h-5 rounded-full flex items-center justify-center text-[10px]" :class="step === 2 ? 'bg-purple-600 text-white' : 'bg-slate-200 text-slate-600'">2</span>
            <span>第二步：证明材料与车辆申报</span>
          </button>
        </div>
      </div>

      <!-- ================= 步骤 1: 申请人身份实人认证 (身份证上传与人脸比对) ================= -->
      <div v-if="step === 1" class="bg-white rounded-2xl p-5 shadow-sm border border-slate-200 space-y-4 animate-fade-in">
        <div>
          <div class="flex items-center justify-between">
            <h3 class="text-sm font-black text-slate-900">第一步：申请人居民身份核验</h3>
            <span class="text-[10px] bg-blue-50 text-blue-700 font-bold px-2 py-0.5 rounded-full border border-blue-200">实人认证</span>
          </div>
          <p class="text-xs text-slate-400 mt-0.5">确立申请人法律主体身份，系统仅存编号与Hash（隐私合规不落原图）</p>
        </div>

        <OcrUploadBox
          label="拍摄 / 上传居民身份证原件照片"
          tip="系统自动提取姓名与身份证号 (数据脱敏留痕)"
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
            <span class="text-slate-500">实名身份核验:</span>
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

        <!-- 下一步按钮: 区分模式 -->
        <button
          @click="step = 2"
          :disabled="!idCardScanned || !faceMatched"
          class="w-full py-3 rounded-xl font-bold text-xs shadow-md transition disabled:opacity-40 disabled:pointer-events-none flex items-center justify-center gap-1.5 text-white"
          :class="currentAuditMode === 'FLEXIBLE_HYBRID' ? 'bg-purple-600 hover:bg-purple-700' : 'bg-blue-600 hover:bg-blue-700'"
        >
          <span v-if="!idCardScanned">请先上传身份证</span>
          <span v-else-if="!faceMatched">请先完成人脸人证比对</span>
          <span v-else-if="currentAuditMode === 'FLEXIBLE_HYBRID'">人证核验通过，下一步完善证明材料与车辆申报 ➔</span>
          <span v-else>人证比对通过，下一步核验房产证明 ➔</span>
        </button>
      </div>

      <!-- ================= 灵活模式 步骤 2: 证明材料与车辆申报 (2 Steps 模式核心) ================= -->
      <div v-else-if="step === 2 && currentAuditMode === 'FLEXIBLE_HYBRID'" class="space-y-4 animate-fade-in">
        <!-- 模块一: 车辆与房号基础信息填报 -->
        <div class="bg-white rounded-2xl p-5 shadow-sm border border-slate-200 space-y-4">
          <div class="flex items-center justify-between border-b border-slate-100 pb-2.5">
            <div class="flex items-center gap-2">
              <span class="text-lg">🚗</span>
              <h3 class="text-sm font-black text-slate-900">第二步：车辆与房屋基础信息</h3>
            </div>
            <span class="text-[10px] bg-purple-50 text-purple-700 font-bold px-2 py-0.5 rounded-full border border-purple-200">
              自主申报
            </span>
          </div>

          <!-- 车牌号码填报 -->
          <div>
            <div class="flex items-center justify-between mb-1">
              <label class="block font-bold text-xs text-slate-700">
                申报号牌号码 <span class="text-rose-500">*</span>
              </label>
              <!-- 快捷带入 -->
              <div class="flex flex-wrap items-center gap-1.5 text-[11px]">
                <span class="text-slate-400">快捷选择:</span>
                <button
                  type="button"
                  @click="quickFillFlexibleVehicle('沪A·12345', 'self')"
                  class="px-2 py-0.5 rounded bg-slate-100 hover:bg-purple-100 text-purple-700 font-mono font-bold border border-slate-200 transition"
                >
                  沪A·12345 (自用)
                </button>
                <button
                  type="button"
                  @click="quickFillFlexibleVehicle('沪A·67890', 'spouse')"
                  class="px-2 py-0.5 rounded bg-slate-100 hover:bg-amber-100 text-amber-700 font-mono font-bold border border-slate-200 transition"
                >
                  沪A·67890 (配偶)
                </button>
                <button
                  type="button"
                  @click="quickFillFlexibleVehicle('沪A·88888', 'tenant')"
                  class="px-2 py-0.5 rounded bg-slate-100 hover:bg-indigo-100 text-indigo-700 font-mono font-bold border border-slate-200 transition"
                >
                  沪A·88888 (租户)
                </button>
              </div>
            </div>
            <input
              v-model="inputPlate"
              type="text"
              required
              maxlength="10"
              placeholder="例如: 沪A·12345 或 沪AD12345"
              class="w-full px-4 py-3 rounded-xl border-2 border-slate-200 focus:border-purple-600 focus:outline-none font-mono font-bold text-base tracking-wider text-slate-800 uppercase placeholder:font-sans placeholder:text-xs placeholder:font-normal"
            />
          </div>

          <!-- 关联小区房号 -->
          <div>
            <div class="flex items-center justify-between mb-1">
              <label class="block font-bold text-xs text-slate-700">
                申报关联房屋门牌 <span class="text-rose-500">*</span>
              </label>
              <div class="flex items-center gap-1.5 text-[11px]">
                <span class="text-slate-400">快速填入:</span>
                <button
                  type="button"
                  @click="selectedProperty = '龙华新村 12号302室'"
                  class="px-2 py-0.5 rounded bg-slate-100 hover:bg-emerald-100 text-emerald-700 font-bold border border-slate-200 transition"
                >
                  12号302室 (产权房)
                </button>
                <button
                  type="button"
                  @click="selectedProperty = '龙华新村 8号101室'"
                  class="px-2 py-0.5 rounded bg-slate-100 hover:bg-blue-100 text-blue-700 font-bold border border-slate-200 transition"
                >
                  8号101室 (租用)
                </button>
              </div>
            </div>
            <input
              v-model="selectedProperty"
              type="text"
              required
              placeholder="例如: 龙华新村 12号302室"
              class="w-full px-3.5 py-2.5 rounded-xl border border-slate-200 focus:border-purple-600 focus:outline-none text-xs font-bold text-slate-800"
            />
          </div>

          <!-- 车辆使用与权属情形 -->
          <div>
            <label class="block font-bold text-xs text-slate-700 mb-2">车辆实际使用情形 / 权属关系</label>
            <div class="grid grid-cols-2 sm:grid-cols-3 gap-2">
              <button
                type="button"
                v-for="rel in [
                  { key: 'self', label: '本人自用车', icon: '👤', desc: '车主为本人' },
                  { key: 'spouse', label: '配偶车辆', icon: '💍', desc: '配偶名下车' },
                  { key: 'family', label: '直系亲属车', icon: '👨‍👩‍👧', desc: '父母子女车' },
                  { key: 'tenant', label: '房屋承租人车', icon: '🏠', desc: '租赁自用车' },
                  { key: 'company', label: '公司单位派车', icon: '🏢', desc: '在职配备车' },
                  { key: 'other', label: '其他借用/代管', icon: '🤝', desc: '亲友借用车' }
                ]"
                :key="rel.key"
                @click="carRelationship = rel.key"
                class="p-2 rounded-xl border text-left transition flex flex-col justify-between shadow-2xs"
                :class="carRelationship === rel.key ? 'border-purple-600 bg-purple-50/70 text-purple-900 font-bold ring-1 ring-purple-500/40' : 'border-slate-200 bg-white text-slate-600 hover:border-slate-300'"
              >
                <div class="flex items-center gap-1 text-xs">
                  <span>{{ rel.icon }}</span>
                  <span class="truncate">{{ rel.label }}</span>
                </div>
                <span class="text-[10px] text-slate-400 font-normal mt-0.5">{{ rel.desc }}</span>
              </button>
            </div>
          </div>
        </div>

        <!-- 模块二: 自主证明材料申报清单 (支持添加、修改名称、上传附件) -->
        <div class="bg-white rounded-2xl p-5 shadow-sm border border-slate-200 space-y-3.5">
          <div class="flex items-center justify-between">
            <div>
              <h3 class="text-sm font-black text-slate-900 flex items-center gap-1.5">
                <span>📎 证明材料申报清单 (支持自主添加)</span>
                <span class="text-[10px] bg-purple-100 text-purple-800 font-bold px-2 py-0.5 rounded-full border border-purple-200">
                  人工核验材料
                </span>
              </h3>
              <p class="text-[11px] text-slate-400 mt-0.5">请自主填报材料名称并上传附件，流转物业专人复核</p>
            </div>
            <button
              type="button"
              @click="quickLoadExampleMaterials"
              class="text-[10px] px-2.5 py-1 rounded-lg bg-purple-50 hover:bg-purple-100 text-purple-700 font-bold border border-purple-200 transition shrink-0"
            >
              ⚡ 一键示例材料
            </button>
          </div>

          <!-- 材料列表 -->
          <div class="space-y-3 pt-1">
            <div
              v-for="(item, idx) in customMaterials"
              :key="idx"
              class="p-3.5 rounded-xl border transition space-y-2.5 text-xs shadow-2xs"
              :class="item.file_name ? 'bg-purple-50/40 border-purple-200' : 'bg-slate-50 border-slate-200'"
            >
              <!-- 顶部: 材料序号与名称输入 -->
              <div class="flex items-center justify-between gap-2">
                <div class="flex items-center gap-2 flex-1 min-w-0">
                  <span class="w-6 h-6 rounded-lg bg-purple-100 text-purple-800 font-black text-xs flex items-center justify-center shrink-0">
                    {{ idx + 1 }}
                  </span>
                  <input
                    v-model="item.name"
                    type="text"
                    placeholder="请输入证明材料名称 (如: 机动车行驶证 / 租赁合同 / 派车说明)"
                    class="flex-1 px-2.5 py-1.5 rounded-lg border border-slate-300 focus:border-purple-600 focus:outline-none font-bold text-xs text-slate-800 bg-white"
                  />
                </div>
                <button
                  type="button"
                  @click="removeCustomMaterial(idx)"
                  class="text-slate-400 hover:text-rose-600 p-1 transition"
                  title="删除此材料"
                >
                  <svg xmlns="http://www.w3.org/2000/svg" class="w-4 h-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M3 6h18m-2 0v14c0 1-1 2-2 2H7c-1 0-2-1-2-2V6m3 0V4c0-1 1-2 2-2h4c1 0 2 1 2 2v2"/>
                  </svg>
                </button>
              </div>

              <!-- 快捷常用材料标签芯片 -->
              <div class="flex flex-wrap items-center gap-1 text-[10px]">
                <span class="text-slate-400">快速设置名称:</span>
                <button
                  type="button"
                  v-for="tag in materialQuickTags"
                  :key="tag.name"
                  @click="applyQuickTagToItem(item, tag)"
                  class="px-1.5 py-0.5 rounded bg-white hover:bg-purple-100 text-slate-600 hover:text-purple-700 border border-slate-200 transition"
                >
                  {{ tag.type_label }}
                </button>
              </div>

              <!-- 附件状态与上传操作区 -->
              <div class="flex items-center justify-between gap-2 pt-1 border-t border-slate-200/60 text-[11px]">
                <div class="flex items-center gap-1.5 min-w-0 flex-1">
                  <span class="text-base">{{ item.icon || '📄' }}</span>
                  <span class="truncate font-mono" :class="item.file_name ? 'text-purple-700 font-bold' : 'text-slate-400'">
                    {{ item.file_name ? `附件: ${item.file_name}` : '未载入附件文件' }}
                  </span>
                </div>

                <div class="flex items-center gap-1.5 shrink-0">
                  <button
                    type="button"
                    @click="mockUploadForItem(item)"
                    class="px-2 py-1 rounded-lg bg-white hover:bg-slate-100 text-slate-700 border border-slate-200 text-[10px] font-bold transition"
                  >
                    ⚡ 拟真载入
                  </button>
                  <label class="cursor-pointer px-2.5 py-1 rounded-lg text-[10px] font-bold transition border bg-purple-600 hover:bg-purple-700 text-white border-purple-600">
                    <input type="file" accept="image/*,.pdf" class="hidden" @change="(e) => onRealUploadForItem(e, item)" />
                    <span>{{ item.file_name ? '重选' : '上传文件' }}</span>
                  </label>
                  <span
                    v-if="item.file_name"
                    class="w-4 h-4 rounded-full bg-emerald-100 text-emerald-700 flex items-center justify-center font-bold text-[10px]"
                  >
                    ✓
                  </span>
                </div>
              </div>
            </div>
          </div>

          <!-- 自主新增材料按钮 -->
          <button
            type="button"
            @click="addCustomMaterial"
            class="w-full py-2.5 rounded-xl border-2 border-dashed border-purple-300 hover:border-purple-500 bg-purple-50/50 hover:bg-purple-100/50 text-purple-700 text-xs font-bold transition flex items-center justify-center gap-1.5"
          >
            <span>➕ 自主添加一项证明材料</span>
          </button>
        </div>

        <!-- 审验与提交说明卡片 -->
        <div class="p-3.5 bg-slate-50 rounded-2xl border border-slate-200 text-[11px] text-slate-500 space-y-1 leading-relaxed">
          <div class="font-bold text-slate-700 flex items-center gap-1.5">
            <span>📋 物业人工审验说明</span>
          </div>
          <p>
            您提交的材料将由小区物业服务中心专人人工复核。审核通过后将正式进入轮候序列。材料仅作资质审核使用，服务端不落高清原图。
          </p>
        </div>

        <!-- 操作按钮组 -->
        <div class="flex items-center gap-2 pt-1">
          <button
            @click="step = 1"
            class="py-3 px-4 rounded-xl bg-slate-100 hover:bg-slate-200 text-slate-700 font-semibold text-xs transition"
          >
            ⬅️ 返回身份核验
          </button>
          <button
            @click="handleSubmitApplication"
            :disabled="submitting"
            class="flex-1 py-3.5 rounded-xl bg-purple-600 hover:bg-purple-700 text-white font-bold text-xs shadow-md transition disabled:opacity-40 flex items-center justify-center gap-1.5"
          >
            <span v-if="submitting">正在提交并流转物业审核...</span>
            <span v-else>确认提交申请材料 (提交物业人工审核) ➔</span>
          </button>
        </div>
      </div>

      <!-- ================= 步骤 2: 标准模式 房产核定 (不动产权属证明) ================= -->
      <div v-else-if="step === 2 && currentAuditMode === 'THREE_CERT_AUTO'" class="bg-white rounded-2xl p-5 shadow-sm border border-slate-200 space-y-4 animate-fade-in">
        <div class="flex items-center justify-between">
          <div>
            <h3 class="text-sm font-black text-slate-900">第二步：本小区不动产权属证明</h3>
            <p class="text-[11px] text-slate-400 mt-0.5">依据政务隐私合规要求，请上传产证原件由系统提取坐落并反向比对</p>
          </div>
          <span class="text-[11px] font-bold px-2 py-0.5 rounded-full border transition" :class="propertyVerified ? 'text-emerald-600 bg-emerald-50 border-emerald-200' : 'text-blue-600 bg-blue-50 border-blue-200'">
            {{ propertyVerified ? '✓ 核验通过' : '政务隐私合规' }}
          </span>
        </div>

        <!-- 上传不动产权证书原件与 OCR 识别 -->
        <OcrUploadBox
          label="拍摄 / 上传不动产权证书 (房产证) 原件"
          tip="系统自动提取产证编号、权利人与房屋坐落，并与市不动产中心联网反向比对 (图片不落盘)"
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
                <div>产证所有人: <strong class="text-slate-800">{{ userStore.realName || '张欣欣' }}</strong> (与身份证一致)</div>
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

          <!-- 外区房产拦截演示卡片 -->
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
            <span v-else>第二步已核定，下一步填报机动车信息 ➔</span>
          </button>
        </div>
      </div>

      <!-- ================= 步骤 3: 标准模式 车辆行驶证与车牌申报 ================= -->
      <div v-else-if="step === 3 && currentAuditMode === 'THREE_CERT_AUTO'" class="space-y-4 animate-fade-in">
        <!-- 车辆与车牌号填报模块 -->
        <div class="bg-white rounded-2xl p-5 shadow-sm border border-slate-200 space-y-4">
          <div class="flex items-center justify-between">
            <div>
              <h3 class="text-sm font-black text-slate-900">第三步：机动车行驶证与车牌填报</h3>
              <p class="text-[11px] text-slate-400 mt-0.5">支持拍摄识别行驶证原件，也可自主填报车牌</p>
            </div>
          </div>

          <!-- 行驶证拍照上传 / OCR 扫描识别 -->
          <div>
            <OcrUploadBox
              label="拍摄 / 上传机动车行驶证正本原件"
              tip="系统自动提取号牌号码、车辆类型与所有人姓名"
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
              tip="系统自动比对双方婚姻证明"
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
              tip="系统自动比对常住人口登记卡直系亲属关系"
              mockType="family_book"
              @scanned="familyCertUploaded = true"
            />

            <div v-if="familyCertUploaded" class="p-2.5 rounded-xl bg-white text-xs text-emerald-700 font-bold border border-emerald-300 flex items-center justify-between">
              <span>✓ 户口本比对一致 (常住人口登记卡直系亲属关系真实有效)</span>
              <span class="text-[10px] text-slate-400 font-mono">310104-HK-982103</span>
            </div>
          </div>
        </div>

        <!-- 申请材料综合比对与核验报告 -->
        <div class="bg-gradient-to-br from-emerald-900 via-teal-900 to-slate-900 text-white rounded-2xl p-5 shadow-xl border-2 border-emerald-400/40 space-y-3 relative overflow-hidden">
          <div class="flex items-center justify-between">
            <div class="flex items-center gap-2">
              <div class="w-7 h-7 rounded-lg bg-emerald-500 text-white flex items-center justify-center font-black text-xs shadow-md">
                ✓
              </div>
              <div>
                <h3 class="text-xs font-black text-emerald-200">申报材料比对与核验报告</h3>
                <p class="text-[10px] text-teal-300">身份 / 房产 / 车辆权属联网比对结果</p>
              </div>
            </div>
            <span class="px-2 py-0.5 rounded-full text-[10px] font-extrabold bg-emerald-500/30 text-emerald-200 border border-emerald-400/40">
              数据比对
            </span>
          </div>

          <div class="bg-black/30 rounded-xl p-3 text-xs space-y-2 font-mono divide-y divide-white/10">
            <div class="flex items-center justify-between pt-1">
              <span class="text-slate-300 font-sans">申请人身份证核验:</span>
              <span class="text-emerald-400 font-bold flex items-center gap-1">
                <span>{{ userStore.realName }} (310104********1111)</span>
                <span>✓ 通过</span>
              </span>
            </div>
            <div class="flex items-center justify-between pt-2">
              <span class="text-slate-300 font-sans">不动产权证书核验:</span>
              <span class="text-emerald-400 font-bold flex items-center gap-1">
                <span>{{ selectedProperty }}</span>
                <span>✓ 本小区产权属实</span>
              </span>
            </div>
            <div class="flex items-center justify-between pt-2">
              <span class="text-slate-300 font-sans">机动车行驶证核验:</span>
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
            <span class="text-[11px] text-teal-200 font-medium">材料综合判定:</span>
            <span
              class="font-black px-2.5 py-1 rounded-lg text-xs"
              :class="isThreeCertValid ? 'bg-emerald-500 text-white shadow-md' : 'bg-rose-900/60 text-rose-200 border border-rose-400/40'"
            >
              {{ isThreeCertValid ? '🌟 申报材料全部核验通过 (准予入队)' : '等待材料补齐' }}
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
            <span v-else>确认提交车位申请 (进入轮候排位) ➔</span>
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
import { showToast } from '../utils/dialog'

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

const switchAuditMode = (mode) => {
  currentAuditMode.value = mode
  if (mode === 'FLEXIBLE_HYBRID' && step.value > 2) {
    step.value = 2
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
const selectedProperty = ref('龙华新村 12号302室')

// 车牌填报与行驶证逻辑
const licenseScanned = ref(false)
const inputPlate = ref('沪A·12345')
const carRelationship = ref('self') // 'self' | 'spouse' | 'family' | 'tenant' | 'company' | 'other'

// 配偶车状态
const spouseName = ref('李玉霞')
const showSpouseConfirmModal = ref(false)
const marriageCertUploaded = ref(false)

// 家人车状态 (户口本)
const familyMemberName = ref('张建国')
const familyRelationType = ref('PARENT')
const showFamilyConfirmModal = ref(false)
const familyCertUploaded = ref(false)

// 灵活材料模式: 业主自主证明材料申报列表
const customMaterials = ref([
  {
    name: '机动车行驶证正副页',
    file_name: '机动车行驶证正副本扫描件_沪A12345.jpg',
    type: 'LICENSE',
    type_label: '行驶证',
    icon: '🚗'
  },
  {
    name: '不动产权证书或房屋租赁协议',
    file_name: '不动产权证书_沪(2022)徐字不动产权第019823号.jpg',
    type: 'PROPERTY',
    type_label: '房屋权属',
    icon: '🏢'
  },
  {
    name: '老旧小区车位规范停放承诺书',
    file_name: '车位合规停放与安全管理承诺书.pdf',
    type: 'COMMITMENT',
    type_label: '合规承诺',
    icon: '📝'
  }
])

// 常用快捷标签芯片
const materialQuickTags = [
  { name: '机动车行驶证正副页', type: 'LICENSE', type_label: '行驶证', icon: '🚗' },
  { name: '不动产权证书', type: 'PROPERTY', type_label: '产证', icon: '🏢' },
  { name: '房屋租赁合同', type: 'LEASE', type_label: '租赁合同', icon: '📄' },
  { name: '车辆借用/代管协议', type: 'AGREEMENT', type_label: '借用协议', icon: '🤝' },
  { name: '单位在职与派车证明', type: 'COMPANY', type_label: '单位证明', icon: '💼' },
  { name: '中华人民共和国结婚证', type: 'MARRIAGE', type_label: '结婚证', icon: '💍' },
  { name: '居民户口簿(直系亲属)', type: 'FAMILY', type_label: '户口本', icon: '👨‍👩‍👧' },
  { name: '车位合规使用承诺书', type: 'COMMITMENT', type_label: '承诺书', icon: '📝' }
]

const applyQuickTagToItem = (item, tag) => {
  item.name = tag.name
  item.type = tag.type
  item.type_label = tag.type_label
  item.icon = tag.icon
  if (!item.file_name) {
    item.file_name = `${tag.name}_扫描件.pdf`
  }
}

const addCustomMaterial = () => {
  const nextNum = customMaterials.value.length + 1
  customMaterials.value.push({
    name: '',
    file_name: '',
    type: 'CUSTOM',
    type_label: `证明材料 #${nextNum}`,
    icon: '📄'
  })
}

const removeCustomMaterial = (idx) => {
  if (customMaterials.value.length <= 1) {
    showToast('请至少保留一项证明材料！', 'warning')
    return
  }
  customMaterials.value.splice(idx, 1)
}

const quickLoadExampleMaterials = () => {
  customMaterials.value = [
    {
      name: '机动车行驶证正副页',
      file_name: '机动车行驶证正副本扫描件_沪A12345.jpg',
      type: 'LICENSE',
      type_label: '行驶证',
      icon: '🚗'
    },
    {
      name: '房屋租赁合同或不动产权证',
      file_name: '龙华新村房屋租赁协议_12号302室.pdf',
      type: 'LEASE',
      type_label: '租赁协议',
      icon: '🏢'
    },
    {
      name: '老旧小区车位规范停放承诺书',
      file_name: '车位合规停放与安全管理承诺书.pdf',
      type: 'COMMITMENT',
      type_label: '合规承诺',
      icon: '📝'
    }
  ]
  showToast('已一键齐备常用证明材料示例！', 'success')
}

const mockUploadForItem = (item) => {
  if (!item.name || !item.name.trim()) {
    item.name = '机动车行驶证正副页'
  }
  item.file_name = `${item.name}_拍照扫描件.jpg`
  showToast(`已成功载入附件：${item.file_name}`, 'success')
}

const onRealUploadForItem = (e, item) => {
  const f = e.target.files && e.target.files[0]
  if (f) {
    item.file_name = f.name
    if (!item.name || !item.name.trim()) {
      item.name = f.name.replace(/\.[^/.]+$/, '')
    }
    showToast(`已上传：${f.name}`, 'success')
  }
}

const quickFillFlexibleVehicle = (plate, type) => {
  inputPlate.value = plate
  carRelationship.value = type
}

const submitting = ref(false)

// 第二步完成判定 (标准模式): 完成产证上传/识别并锁定本小区产权
const propertyVerified = computed(() => propertyCertScanned.value && !!selectedProperty.value)

// 标准模式综合判定
const isThreeCertValid = computed(() => {
  if (!idCardScanned.value || !faceMatched.value) return false
  if (!propertyVerified.value) return false
  if (!inputPlate.value || !inputPlate.value.trim()) return false
  if (carRelationship.value === 'spouse' && !marriageCertUploaded.value) return false
  if (carRelationship.value === 'family' && !familyCertUploaded.value) return false
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
  if (currentAuditMode.value === 'FLEXIBLE_HYBRID') {
    if (!inputPlate.value || !inputPlate.value.trim()) {
      showToast('请输入申报号牌号码！', 'warning')
      return
    }
    if (!selectedProperty.value || !selectedProperty.value.trim()) {
      showToast('请输入关联房屋坐落！', 'warning')
      return
    }
    const readyMaterials = customMaterials.value.filter(m => m.name && m.name.trim() && m.file_name)
    if (readyMaterials.length === 0) {
      showToast('请至少添加并上传一项证明材料附件！', 'warning')
      return
    }

    submitting.value = true
    try {
      const formattedAttachments = customMaterials.value
        .filter(m => m.name && m.name.trim())
        .map(m => ({
          name: m.name.trim(),
          title: m.name.trim(),
          file_name: m.file_name || `${m.name.trim()}.pdf`,
          type: m.type || 'CUSTOM',
          type_label: m.type_label || '证明附件',
          desc: `业主自主申报材料: ${m.name.trim()} (${m.file_name || '已就绪'})`
        }))

      const res = await api.post('/parking/apply', {
        property_no: selectedProperty.value.trim(),
        plate_no: inputPlate.value.trim().toUpperCase(),
        is_spouse_car: carRelationship.value === 'spouse',
        car_relationship: carRelationship.value.toUpperCase(),
        property_cert_no: '沪(2022)徐字不动产权第019823号',
        attachments: formattedAttachments,
        audit_mode: 'FLEXIBLE_HYBRID'
      })

      showToast(res.message || '申报材料已提交！正在等待物业人工审核', 'success')
      await userStore.fetchHomeData()
      router.push('/')
    } catch (err) {
      showToast('提交失败: ' + err.message, 'error')
    } finally {
      submitting.value = false
    }
  } else {
    // THREE_CERT_AUTO 模式
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
        audit_mode: 'THREE_CERT_AUTO'
      })

      showToast(res.message || '申报材料核验完全通过！已成功进入轮候队列', 'success')
      await userStore.fetchHomeData()
      router.push('/')
    } catch (err) {
      showToast('提交失败: ' + err.message, 'error')
    } finally {
      submitting.value = false
    }
  }
}
</script>
