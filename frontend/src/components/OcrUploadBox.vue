<template>
  <div class="border-2 border-dashed border-slate-300 hover:border-blue-500 rounded-2xl p-4 bg-white transition relative overflow-hidden group">
    <div v-if="!previewUrl" class="flex flex-col items-center justify-center py-4 text-center">
      <div class="w-12 h-12 rounded-full bg-blue-50 text-blue-600 flex items-center justify-center mb-2 group-hover:scale-110 transition cursor-pointer" @click="triggerUpload">
        <svg xmlns="http://www.w3.org/2000/svg" class="w-6 h-6" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <rect width="18" height="18" x="3" y="3" rx="2" ry="2"/><circle cx="9" cy="9" r="2"/><path d="m21 15-3.086-3.086a2 2 0 0 0-2.828 0L6 21"/>
        </svg>
      </div>
      <span class="text-xs font-bold text-slate-700 block cursor-pointer" @click="triggerUpload">{{ label }}</span>
      <span class="text-[11px] text-slate-400 mt-0.5 block">{{ tip }}</span>

      <!-- 快捷操作按钮条 -->
      <div class="mt-3 flex items-center gap-2">
        <button
          type="button"
          @click="triggerUpload"
          class="px-2.5 py-1 rounded-lg bg-slate-100 hover:bg-slate-200 text-slate-700 text-[11px] font-semibold transition"
        >
          📁 本地选图
        </button>
        <button
          type="button"
          @click="simulateScan"
          class="px-2.5 py-1 rounded-lg bg-blue-50 hover:bg-blue-100 text-blue-700 border border-blue-200 text-[11px] font-bold transition flex items-center gap-1"
        >
          <span>⚡ 演示拟真识别</span>
        </button>
      </div>

      <input type="file" ref="fileInput" accept="image/*" class="hidden" @change="handleFileChange" />
    </div>

    <!-- 上传/拟真后扫描识别动效 -->
    <div v-else class="relative rounded-xl overflow-hidden bg-slate-900 border border-slate-200">
      <img :src="previewUrl" alt="预览图" class="w-full h-36 object-contain bg-slate-950/80 opacity-90" />

      <!-- 绿色激光雷达动态扫描线 -->
      <div v-if="isScanning" class="absolute inset-0 bg-blue-950/40 flex flex-col justify-between pointer-events-none">
        <div class="w-full h-1 bg-gradient-to-r from-emerald-400 via-teal-300 to-emerald-400 shadow-[0_0_15px_#10B981] animate-bounce"></div>
        <div class="bg-black/75 backdrop-blur text-emerald-400 text-[11px] py-1.5 text-center font-mono flex items-center justify-center gap-1.5">
          <span class="w-2 h-2 rounded-full bg-emerald-400 animate-ping"></span>
          <span>🔍 政务公安/车管所 OCR 电子核验中...</span>
        </div>
      </div>

      <!-- 识别完成状态 -->
      <div v-else class="absolute bottom-0 inset-x-0 bg-emerald-700/90 text-white text-[11px] py-1.5 px-3 flex items-center justify-between backdrop-blur">
        <span class="flex items-center gap-1 font-semibold">
          <svg xmlns="http://www.w3.org/2000/svg" class="w-3.5 h-3.5 text-emerald-200" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3">
            <polyline points="20 6 9 17 4 12"/>
          </svg>
          官方证照验真通过 (数据已脱敏留痕)
        </span>
        <button @click.stop="triggerUpload" class="text-emerald-100 hover:text-white underline text-[10px]">
          重新上传
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'

const props = defineProps({
  label: {
    type: String,
    default: '上传证件照片'
  },
  tip: {
    type: String,
    default: '点击拍摄或选择照片 (系统自动OCR识别)'
  },
  mockType: {
    type: String,
    default: 'id_card' // 'id_card' | 'property_cert' | 'license' | 'marriage' | 'business_license'
  }
})

const emit = defineEmits(['scanned'])

const fileInput = ref(null)
const previewUrl = ref('')
const isScanning = ref(false)

const triggerUpload = () => {
  fileInput.value?.click()
}

const getMockSvg = (type) => {
  if (type === 'business_license') {
    return 'data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" width="400" height="250" viewBox="0 0 400 250"><rect width="100%" height="100%" rx="16" fill="%23fff7ed"/><rect x="12" y="12" width="376" height="226" rx="10" fill="%23fff1f2" stroke="%23b91c1c" stroke-width="3"/><text x="200" y="48" font-family="sans-serif" font-size="16" fill="%239f1239" font-weight="bold" text-anchor="middle">营业执照</text><text x="200" y="68" font-family="sans-serif" font-size="10" fill="%23be123c" text-anchor="middle">（副本）统一社会信用代码 91310104MA1FR8992X</text><rect x="28" y="84" width="344" height="130" rx="8" fill="%23ffffff" stroke="%23fecaca"/><text x="40" y="110" font-family="sans-serif" font-size="12" fill="%231e293b" font-weight="bold">名称: 上海安居物业管理服务有限公司</text><text x="40" y="134" font-family="sans-serif" font-size="12" fill="%23475569">类型: 有限责任公司</text><text x="40" y="158" font-family="sans-serif" font-size="12" fill="%23475569">法定代表人: 王建国</text><text x="40" y="182" font-family="sans-serif" font-size="11" fill="%23475569">住所: 上海市徐汇区龙华街道龙华新村</text><circle cx="330" cy="175" r="28" fill="none" stroke="%23dc2626" stroke-width="3"/><text x="330" y="179" font-family="sans-serif" font-size="9" fill="%23dc2626" font-weight="bold" text-anchor="middle">登记专用</text></svg>'
  } else if (type === 'property_cert') {
    return 'data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" width="400" height="250" viewBox="0 0 400 250"><rect width="100%" height="100%" rx="16" fill="%23b91c1c"/><rect x="15" y="15" width="370" height="220" rx="12" fill="%23dc2626" stroke="%23fbbf24" stroke-width="2"/><text x="200" y="58" font-family="sans-serif" font-size="17" fill="%23fde68a" font-weight="bold" text-anchor="middle">中华人民共和国不动产权证书</text><text x="200" y="82" font-family="sans-serif" font-size="10" fill="%23fecaca" text-anchor="middle">自然资源部监制</text><rect x="35" y="100" width="330" height="110" rx="8" fill="%23fef2f2"/><text x="50" y="128" font-family="monospace" font-size="11" fill="%231e293b" font-weight="bold">沪(2022)徐字不动产权第019823号</text><text x="50" y="152" font-family="sans-serif" font-size="11" fill="%23475569">权利人: 张欣欣</text><text x="50" y="174" font-family="sans-serif" font-size="11" fill="%23475569">坐落: 龙华新村12号楼302室</text><text x="50" y="196" font-family="sans-serif" font-size="11" fill="%23475569">用途: 城镇住宅用地/住宅</text></svg>'
  } else if (type === 'license') {
    return 'data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" width="400" height="250" viewBox="0 0 400 250"><rect width="100%" height="100%" rx="16" fill="%23fef3c7"/><rect x="20" y="20" width="360" height="210" rx="10" fill="%23fffbeb" stroke="%23f59e0b" stroke-width="2"/><text x="100" y="52" font-family="sans-serif" font-size="14" fill="%23b45309" font-weight="bold">中华人民共和国机动车行驶证</text><rect x="35" y="80" width="70" height="8" rx="4" fill="%23d97706"/><rect x="115" y="80" width="120" height="10" rx="4" fill="%231e293b"/><rect x="35" y="105" width="70" height="8" rx="4" fill="%23d97706"/><rect x="115" y="105" width="90" height="10" rx="4" fill="%231e293b"/><rect x="35" y="145" width="330" height="65" rx="6" fill="%23fef3c7" stroke="%23fde68a"/><text x="45" y="172" font-family="monospace" font-size="13" fill="%231e293b" font-weight="bold">号牌号码: 沪A·12345</text><text x="45" y="195" font-family="sans-serif" font-size="11" fill="%23475569">车辆类型: 小型轿车 | 所有人: 张欣欣</text></svg>'
  } else if (type === 'marriage') {
    return 'data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" width="400" height="250" viewBox="0 0 400 250"><rect width="100%" height="100%" rx="16" fill="%23881337"/><rect x="15" y="15" width="370" height="220" rx="12" fill="%239f1239" stroke="%23fbbf24" stroke-width="2"/><circle cx="200" cy="75" r="28" fill="%23fbbf24" opacity="0.9"/><text x="200" y="135" font-family="sans-serif" font-size="18" fill="%23fbbf24" font-weight="bold" text-anchor="middle">结 婚 证</text><text x="200" y="165" font-family="sans-serif" font-size="11" fill="%23fecdd3" text-anchor="middle">中华人民共和国民政部监制</text><text x="200" y="195" font-family="monospace" font-size="11" fill="%23fde047" text-anchor="middle">字号: J310104-2022-0089 (夫妻共有)</text></svg>'
  } else {
    return 'data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" width="400" height="250" viewBox="0 0 400 250"><rect width="100%" height="100%" rx="16" fill="%23dbeafe"/><rect x="24" y="30" width="40" height="40" rx="20" fill="%232563eb" opacity="0.8"/><rect x="80" y="38" width="120" height="12" rx="6" fill="%231e3a8a"/><rect x="80" y="58" width="80" height="8" rx="4" fill="%2364748b"/><rect x="24" y="95" width="200" height="10" rx="5" fill="%2394a3b8"/><rect x="24" y="120" width="160" height="10" rx="5" fill="%2394a3b8"/><rect x="24" y="175" width="240" height="16" rx="6" fill="%231e293b"/><rect x="280" y="40" width="95" height="125" rx="8" fill="%2393c5fd"/><circle cx="327" cy="85" r="24" fill="%233b82f6"/><path d="M300 150 c0 -20, 54 -20, 54 0 Z" fill="%233b82f6"/><text x="24" y="220" font-family="sans-serif" font-size="11" fill="%232563eb" font-weight="bold">居民身份证 (官方人证核验通过)</text></svg>'
  }
}

const simulateScan = () => {
  previewUrl.value = getMockSvg(props.mockType)
  isScanning.value = true

  setTimeout(() => {
    isScanning.value = false
    emit('scanned', { fileName: `mock_${props.mockType}.jpg` })
  }, 1200)
}

const handleFileChange = (e) => {
  const file = e.target.files?.[0]
  if (!file) return

  previewUrl.value = URL.createObjectURL(file)
  isScanning.value = true

  // 1.2 秒拟真扫描后通知父组件
  setTimeout(() => {
    isScanning.value = false
    emit('scanned', { fileName: file.name })
  }, 1200)
}
</script>
