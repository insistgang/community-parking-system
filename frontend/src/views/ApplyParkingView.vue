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
        <h1 class="text-sm font-bold text-slate-900">老旧小区车位三证合一申报认证</h1>
      </div>
      <span class="text-[11px] text-emerald-700 bg-emerald-50 px-2 py-0.5 rounded-full border border-emerald-200 font-bold flex items-center gap-1">
        <span class="w-1.5 h-1.5 rounded-full bg-emerald-500 animate-pulse"></span>
        <span>三证合一刚性准入</span>
      </span>
    </header>

    <main class="max-w-md w-full mx-auto p-4 space-y-4 flex-1">
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
            {{ idCardScanned ? '✓ 证一:身份证' : '证一:身份证' }}
          </div>
          <div
            @click="selectedProperty ? step = 2 : null"
            class="rounded-lg py-1 px-1 border transition cursor-pointer"
            :class="selectedProperty ? 'bg-emerald-500/20 text-emerald-300 border-emerald-400/40 font-bold' : 'bg-white/10 text-slate-300 border-white/10'"
          >
            {{ selectedProperty ? '✓ 证二:房产证' : '证二:房产证' }}
          </div>
          <div
            @click="licenseScanned || inputPlate ? step = 3 : null"
            class="rounded-lg py-1 px-1 border transition cursor-pointer"
            :class="licenseScanned || inputPlate ? 'bg-emerald-500/20 text-emerald-300 border-emerald-400/40 font-bold' : 'bg-white/10 text-slate-300 border-white/10'"
          >
            {{ licenseScanned || inputPlate ? '✓ 证三:行驶证' : '证三:行驶证' }}
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
            <span class="w-5 h-5 rounded-full flex items-center justify-center text-[10px]" :class="step === 1 ? 'bg-blue-600 text-white' : (idCardScanned ? 'bg-emerald-100 text-emerald-700' : 'bg-slate-200 text-slate-500')">
              {{ idCardScanned && step !== 1 ? '✓' : '1' }}
            </span>
            <span>① 身份核验</span>
          </button>
          <span class="text-slate-300">──</span>
          <button
            @click="idCardScanned ? step = 2 : null"
            class="flex items-center gap-1.5 transition"
            :class="step === 2 ? 'text-blue-600 font-black' : (selectedProperty ? 'text-emerald-600' : 'text-slate-400')"
          >
            <span class="w-5 h-5 rounded-full flex items-center justify-center text-[10px]" :class="step === 2 ? 'bg-blue-600 text-white' : (selectedProperty ? 'bg-emerald-100 text-emerald-700' : 'bg-slate-200 text-slate-500')">
              {{ selectedProperty && step !== 2 ? '✓' : '2' }}
            </span>
            <span>② 房产核定</span>
          </button>
          <span class="text-slate-300">──</span>
          <button
            @click="idCardScanned && selectedProperty ? step = 3 : null"
            class="flex items-center gap-1.5 transition"
            :class="step === 3 ? 'text-blue-600 font-black' : (licenseScanned ? 'text-emerald-600' : 'text-slate-400')"
          >
            <span class="w-5 h-5 rounded-full flex items-center justify-center text-[10px]" :class="step === 3 ? 'bg-blue-600 text-white' : (licenseScanned ? 'bg-emerald-100 text-emerald-700' : 'bg-slate-200 text-slate-500')">
              {{ licenseScanned && step !== 3 ? '✓' : '3' }}
            </span>
            <span>③ 车辆填报</span>
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
            <span class="text-slate-500">政务人证比对:</span>
            <span class="font-semibold text-emerald-600 flex items-center gap-1">
              <span>✓ 比对通过 (公安实人认证库一致)</span>
            </span>
          </div>
        </div>

        <button
          @click="step = 2"
          :disabled="!idCardScanned"
          class="w-full py-3 rounded-xl bg-blue-600 hover:bg-blue-700 text-white font-bold text-xs shadow-md transition disabled:opacity-40 disabled:pointer-events-none flex items-center justify-center gap-1.5"
        >
          <span>第一证已核验，下一步核验第二证(房产证)</span>
          <span>➔</span>
        </button>
      </div>

      <!-- ================= 步骤 2: 证二·房产核定 (不动产权属证明) ================= -->
      <div v-else-if="step === 2" class="bg-white rounded-2xl p-5 shadow-sm border border-slate-200 space-y-4 animate-fade-in">
        <div class="flex items-center justify-between">
          <div>
            <h3 class="text-sm font-black text-slate-900">第二证：本小区不动产权属证明</h3>
            <p class="text-[11px] text-slate-400 mt-0.5">调取市大数据不动产电子证照，严禁外区房蹭停</p>
          </div>
          <span class="text-[11px] text-emerald-600 font-bold bg-emerald-50 px-2 py-0.5 rounded-full border border-emerald-200">
            电子证照调取
          </span>
        </div>

        <!-- 调取出的名下房产列表 -->
        <div class="space-y-2.5">
          <!-- 房产 1: 本小区 (可选) -->
          <div
            @click="selectedProperty = '某某新村12号楼302室'"
            class="p-3.5 rounded-xl border-2 cursor-pointer transition flex items-start justify-between"
            :class="selectedProperty === '某某新村12号楼302室' ? 'border-blue-600 bg-blue-50/50 shadow-sm' : 'border-slate-200 bg-white hover:border-slate-300'"
          >
            <div>
              <div class="flex items-center gap-1.5 font-bold text-xs text-slate-800">
                <span class="text-blue-600">🏠</span>
                <span>某某新村 12号楼 302室</span>
                <span class="text-[10px] text-emerald-700 bg-emerald-100 px-1.5 py-0.2 rounded font-semibold">本小区产权房</span>
              </div>
              <span class="text-[11px] text-slate-500 mt-1 block">产证所有人: {{ userStore.realName }} (与第一证完全一致)</span>
              <span class="text-[10px] text-slate-400 block font-mono mt-0.5">产证编号: 沪(2022)徐字不动产权第019823号</span>
            </div>
            <span class="w-5 h-5 rounded-full border-2 flex items-center justify-center mt-0.5" :class="selectedProperty === '某某新村12号楼302室' ? 'border-blue-600 bg-blue-600 text-white text-[11px] font-bold' : 'border-slate-300'">
              ✓
            </span>
          </div>

          <!-- 房产 2: 外区房 (置灰 + 拦截测试) -->
          <div
            @click="handleSelectExternalProperty"
            class="p-3.5 rounded-xl border-2 border-slate-200 bg-slate-100 opacity-60 cursor-not-allowed flex items-start justify-between relative group"
          >
            <div>
              <div class="flex items-center gap-1.5 font-bold text-xs text-slate-500">
                <span>🏢</span>
                <span class="line-through">浦东新区世纪大道 888弄 501室</span>
                <span class="text-[10px] text-rose-700 bg-rose-100 px-1.5 py-0.2 rounded font-semibold not-italic">非本小区·不可选</span>
              </div>
              <span class="text-[11px] text-slate-400 mt-1 block">产权人: {{ userStore.realName }} | 产证编号: 沪房地浦字(2021)第033412号</span>
            </div>
            <span class="text-[10px] text-rose-600 font-bold bg-rose-50 border border-rose-200 px-1.5 py-0.5 rounded">
              禁止申请
            </span>
          </div>

          <!-- 拦截警告提示 -->
          <div v-if="externalWarning" class="p-2.5 rounded-xl bg-rose-50 border border-rose-200 text-rose-700 text-xs font-semibold flex items-center gap-1.5 animate-bounce">
            <span>🚫 拦截警告：仅限本小区产权房屋可申请车位，外区房产无法申报！</span>
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
            :disabled="!selectedProperty"
            class="flex-1 py-3 rounded-xl bg-blue-600 hover:bg-blue-700 text-white font-bold text-xs shadow-md transition disabled:opacity-40 disabled:pointer-events-none flex items-center justify-center gap-1.5"
          >
            <span>第二证已核定，下一步核验第三证(行驶证)</span>
            <span>➔</span>
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
            <span class="text-[11px] text-blue-600 font-bold bg-blue-50 px-2 py-0.5 rounded-full border border-blue-200">
              OCR/自填均可
            </span>
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
              <div class="flex items-center gap-1 text-[11px]">
                <span class="text-slate-400">快速填入:</span>
                <button
                  type="button"
                  @click="quickFillPlate('沪A·12345', false)"
                  class="px-2 py-0.5 rounded bg-slate-100 hover:bg-blue-100 text-blue-700 font-mono font-bold border border-slate-200 transition"
                >
                  沪A·12345 (本人车)
                </button>
                <button
                  type="button"
                  @click="quickFillPlate('沪A·67890', true)"
                  class="px-2 py-0.5 rounded bg-slate-100 hover:bg-amber-100 text-amber-700 font-mono font-bold border border-slate-200 transition"
                >
                  沪A·67890 (配偶车)
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

          <!-- 车辆权属关系单选 -->
          <div class="pt-1">
            <label class="block font-bold text-xs text-slate-700 mb-2">机动车行驶证所有人权属关系</label>
            <div class="grid grid-cols-2 gap-2.5">
              <label
                @click="handleCarTypeChange(false)"
                class="p-3 rounded-xl border-2 cursor-pointer transition flex items-center gap-2 text-xs"
                :class="!isSpouseCar ? 'border-blue-600 bg-blue-50/60 font-bold text-blue-900' : 'border-slate-200 bg-white text-slate-600 hover:border-slate-300'"
              >
                <input type="radio" name="car_type" :checked="!isSpouseCar" class="text-blue-600" />
                <div>
                  <span class="block">本人名下车辆</span>
                  <span class="text-[10px] text-slate-400 font-normal">车主 == 房产所有人</span>
                </div>
              </label>

              <label
                @click="handleCarTypeChange(true)"
                class="p-3 rounded-xl border-2 cursor-pointer transition flex items-center gap-2 text-xs"
                :class="isSpouseCar ? 'border-amber-500 bg-amber-50/60 font-bold text-amber-900' : 'border-slate-200 bg-white text-slate-600 hover:border-slate-300'"
              >
                <input type="radio" name="car_type" :checked="isSpouseCar" class="text-amber-600" />
                <div>
                  <span class="block">配偶共有车辆 (夫妻车)</span>
                  <span class="text-[10px] text-slate-400 font-normal">需补充上传结婚证</span>
                </div>
              </label>
            </div>
          </div>

          <!-- 夫妻车辆结婚证上传区域 (当选择夫妻车辆时展示) -->
          <div v-if="isSpouseCar" class="bg-amber-50/90 rounded-2xl p-4 border-2 border-amber-300 space-y-3 animate-fade-in">
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
              <span class="text-emerald-400 font-bold flex items-center gap-1" v-if="!isSpouseCar">
                <span>{{ inputPlate }} (车主: {{ userStore.realName }})</span>
                <span>✓ 完全一致</span>
              </span>
              <span class="text-amber-300 font-bold flex items-center gap-1" v-else>
                <span>{{ inputPlate }} (车主: {{ spouseName }})</span>
                <span>{{ marriageCertUploaded ? '✓ 夫妻共有认定' : '⚠️ 待传结婚证' }}</span>
              </span>
            </div>
          </div>

          <div class="pt-1 flex items-center justify-between text-xs">
            <span class="text-[11px] text-teal-200 font-medium">三证综合交叉判定:</span>
            <span
              class="font-black px-2.5 py-1 rounded-lg text-xs"
              :class="isThreeCertValid ? 'bg-emerald-500 text-white shadow-md' : 'bg-rose-900/60 text-rose-200 border border-rose-400/40'"
            >
              {{ isThreeCertValid ? '🌟 三证合一核验全部通过 (准予入队)' : '等待材料补齐' }}
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
            <span v-if="submitting">正在上链存证并计算排位顺位...</span>
            <span v-else>确认提交三证申请 (进入 FIFO 阳光排位)</span>
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
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../store/user'
import api from '../api'
import OcrUploadBox from '../components/OcrUploadBox.vue'

const router = useRouter()
const userStore = useUserStore()

const step = ref(1)
const idCardScanned = ref(false)
const selectedProperty = ref('某某新村12号楼302室')
const externalWarning = ref(false)

// 车牌填报与行驶证逻辑
const licenseScanned = ref(false)
const inputPlate = ref('沪A·12345')
const isSpouseCar = ref(false)
const spouseName = ref('李玉霞')
const showSpouseConfirmModal = ref(false)
const marriageCertUploaded = ref(false)
const submitting = ref(false)

// 综合三证合一判定
const isThreeCertValid = computed(() => {
  if (!idCardScanned.value) return false
  if (!selectedProperty.value || selectedProperty.value.includes('浦东')) return false
  if (!inputPlate.value || !inputPlate.value.trim()) return false
  if (isSpouseCar.value && !marriageCertUploaded.value) return false
  return true
})

const handleIdCardScanned = () => {
  idCardScanned.value = true
}

const handleLicenseScanned = () => {
  licenseScanned.value = true
  if (!isSpouseCar.value) {
    inputPlate.value = '沪A·12345'
  }
}

const handleSelectExternalProperty = () => {
  externalWarning.value = true
  setTimeout(() => {
    externalWarning.value = false
  }, 4000)
}

const quickFillPlate = (plate, isSpouse) => {
  inputPlate.value = plate
  licenseScanned.value = true
  if (isSpouse) {
    showSpouseConfirmModal.value = true
  } else {
    isSpouseCar.value = false
  }
}

const handleCarTypeChange = (isSpouse) => {
  if (isSpouse) {
    showSpouseConfirmModal.value = true
  } else {
    isSpouseCar.value = false
  }
}

const confirmSpouseMode = () => {
  isSpouseCar.value = true
  showSpouseConfirmModal.value = false
}

const handleSubmitApplication = async () => {
  if (!isThreeCertValid.value) {
    alert('三证核验未完全通过，请核对材料！')
    return
  }

  submitting.value = true
  try {
    const res = await api.post('/parking/apply', {
      property_no: selectedProperty.value,
      plate_no: inputPlate.value.trim().toUpperCase(),
      is_spouse_car: isSpouseCar.value,
      marriage_cert_no: isSpouseCar.value ? 'J310104-2022-0089' : null
    })
    alert(res.message || '三证合一核验完全通过！已成功进入排队队列')
    await userStore.fetchHomeData()
    router.push('/')
  } catch (err) {
    alert('提交失败: ' + err.message)
  } finally {
    submitting.value = false
  }
}
</script>
