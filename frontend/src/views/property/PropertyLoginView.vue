<template>
  <div class="min-h-screen bg-slate-900 text-slate-100 flex flex-col justify-center py-12 sm:px-6 lg:px-8">
    <SeedAccountBar @fill="handleFastFill" />

    <div class="sm:mx-auto sm:w-full sm:max-w-md text-center">
      <div class="inline-flex items-center justify-center w-14 h-14 rounded-2xl bg-purple-600 text-white shadow-lg shadow-purple-500/30 mb-3">
        <svg xmlns="http://www.w3.org/2000/svg" class="w-8 h-8" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <path d="M3 21h18"/><path d="M5 21V5a2 2 0 0 1 2-2h10a2 2 0 0 1 2 2v16"/><path d="M9 9h1"/><path d="M9 13h1"/><path d="M9 17h1"/><path d="M14 9h1"/><path d="M14 13h1"/><path d="M14 17h1"/>
        </svg>
      </div>
      <h2 class="text-xl font-black tracking-tight text-white">物业服务企业工作台认证登录</h2>
      <p class="mt-1 text-xs text-slate-400">营业执照核验 · 法人身份证 · 实人人脸认证</p>
    </div>

    <div class="mt-6 sm:mx-auto sm:w-full sm:max-w-md px-4">
      <div class="bg-slate-800 py-7 px-6 shadow-2xl rounded-3xl sm:px-10 border border-slate-700 space-y-5">
        <!-- 步骤指示 -->
        <div class="grid grid-cols-3 gap-1.5 text-[10px] font-bold">
          <div
            class="rounded-lg py-1.5 text-center border"
            :class="authStep === 1 ? 'bg-purple-600 text-white border-purple-400' : (licenseScanned ? 'bg-emerald-900/40 text-emerald-300 border-emerald-600/40' : 'bg-slate-900 text-slate-500 border-slate-700')"
          >① 营业执照</div>
          <div
            class="rounded-lg py-1.5 text-center border"
            :class="authStep === 2 ? 'bg-purple-600 text-white border-purple-400' : (legalIdScanned ? 'bg-emerald-900/40 text-emerald-300 border-emerald-600/40' : 'bg-slate-900 text-slate-500 border-slate-700')"
          >② 法人身份证</div>
          <div
            class="rounded-lg py-1.5 text-center border"
            :class="authStep === 3 ? 'bg-purple-600 text-white border-purple-400' : (faceMatched ? 'bg-emerald-900/40 text-emerald-300 border-emerald-600/40' : 'bg-slate-900 text-slate-500 border-slate-700')"
          >③ 人脸认证</div>
        </div>

        <!-- 第一步: 营业执照 -->
        <div v-if="authStep === 1" class="space-y-3 animate-fade-in">
          <label class="block text-xs font-bold text-slate-300">第一步：上传企业营业执照，系统自动 OCR 识别</label>
          <OcrUploadBox
            label="上传物业公司营业执照"
            tip="自动提取企业名称、统一社会信用代码与法定代表人"
            mockType="business_license"
            @scanned="handleLicenseScanned"
          />
          <div v-if="licenseScanned" class="bg-slate-900/80 p-3 rounded-xl border border-slate-600 text-xs space-y-1.5">
            <div class="flex justify-between">
              <span class="text-slate-400">认证企业:</span>
              <span class="font-bold text-slate-200">{{ company.name }}</span>
            </div>
            <div class="flex justify-between">
              <span class="text-slate-400">统一信用代码:</span>
              <span class="font-mono text-slate-300">{{ company.creditCode }}</span>
            </div>
            <div class="flex justify-between">
              <span class="text-slate-400">法定代表人:</span>
              <span class="font-bold text-purple-300">{{ company.legalName }}</span>
            </div>
          </div>
          <button
            type="button"
            :disabled="!licenseScanned"
            @click="authStep = 2"
            class="w-full py-3 rounded-xl bg-purple-600 hover:bg-purple-500 text-white font-bold text-xs shadow-md transition disabled:opacity-40 disabled:pointer-events-none"
          >
            执照已识别，下一步上传法人身份证 ➔
          </button>
        </div>

        <!-- 第二步: 法人身份证 -->
        <div v-else-if="authStep === 2" class="space-y-3 animate-fade-in">
          <label class="block text-xs font-bold text-slate-300">第二步：上传企业法人身份证照片</label>
          <OcrUploadBox
            label="拍摄 / 上传法定代表人居民身份证"
            tip="OCR 提取姓名与身份证号，并与营业执照法人交叉比对"
            mockType="id_card"
            @scanned="handleLegalIdScanned"
          />
          <div v-if="legalIdScanned" class="bg-slate-900/80 p-3 rounded-xl border border-slate-600 text-xs space-y-1.5">
            <div class="flex justify-between">
              <span class="text-slate-400">证件姓名:</span>
              <span class="font-bold text-slate-200">{{ company.legalName }}</span>
            </div>
            <div class="flex justify-between">
              <span class="text-slate-400">证件号码:</span>
              <span class="font-mono text-slate-300">310104********5555</span>
            </div>
            <div class="flex justify-between">
              <span class="text-slate-400">与执照法人比对:</span>
              <span class="font-semibold text-emerald-400">✓ 姓名一致（{{ company.legalName }}）</span>
            </div>
          </div>
          <div class="flex gap-2">
            <button type="button" @click="authStep = 1" class="px-4 py-3 rounded-xl bg-slate-700 text-slate-200 text-xs font-semibold">返回</button>
            <button
              type="button"
              :disabled="!legalIdScanned"
              @click="authStep = 3"
              class="flex-1 py-3 rounded-xl bg-purple-600 hover:bg-purple-500 text-white font-bold text-xs shadow-md transition disabled:opacity-40 disabled:pointer-events-none"
            >
              身份证已核验，下一步法人人脸认证 ➔
            </button>
          </div>
        </div>

        <!-- 第三步: 人脸 + 登录 -->
        <div v-else class="space-y-4 animate-fade-in">
          <label class="block text-xs font-bold text-slate-300">第三步：法人实人认证（采集法人人脸）</label>
          <p class="text-[11px] text-slate-400 leading-relaxed">
            系统将交叉核验：营业执照法人姓名、身份证姓名、证件人像与现场采集人脸，确认系同一法人。
          </p>
          <FaceCaptureBox
            :name="company.legalName"
            persona="wang"
            title="法人活体人脸采集"
            hint="现场人脸将与身份证头像自动比对"
            dark
            :pre-matched="faceMatched"
            @matched="handleFaceMatched"
          />

          <div v-if="faceMatched" class="bg-emerald-950/40 border border-emerald-700/50 rounded-xl p-3 text-[11px] text-emerald-300 space-y-1">
            <div class="font-bold text-emerald-200">法人身份认证全部通过</div>
            <div class="flex justify-between"><span>执照法人</span><span>{{ company.legalName }} ✓</span></div>
            <div class="flex justify-between"><span>身份证姓名</span><span>{{ company.legalName }} ✓</span></div>
            <div class="flex justify-between"><span>证件人脸 ↔ 现场人脸</span><span>同一人 ✓</span></div>
          </div>

          <form v-if="faceMatched" @submit.prevent="handleLogin" class="space-y-3 text-xs pt-1">
            <div>
              <label class="block font-bold text-slate-300 mb-1">法人手机号 / 管理员账号</label>
              <input
                v-model="identifier"
                type="text"
                required
                class="w-full px-3.5 py-2.5 rounded-xl bg-slate-900 border border-slate-600 text-white focus:outline-none focus:ring-2 focus:ring-purple-500 font-medium"
              />
            </div>
            <div>
              <label class="block font-bold text-slate-300 mb-1">登录密码</label>
              <input
                v-model="password"
                type="password"
                required
                class="w-full px-3.5 py-2.5 rounded-xl bg-slate-900 border border-slate-600 text-white focus:outline-none focus:ring-2 focus:ring-purple-500 font-medium"
              />
            </div>
            <div v-if="errorMsg" class="p-2.5 bg-rose-900/50 border border-rose-600 rounded-xl text-rose-300 text-xs font-medium">
              ⚠️ {{ errorMsg }}
            </div>
            <button
              type="submit"
              :disabled="loading"
              class="w-full py-3 rounded-xl bg-purple-600 hover:bg-purple-700 text-white font-bold shadow-lg shadow-purple-500/30 transition active:scale-[0.98]"
            >
              <span v-if="loading">正在验证权限...</span>
              <span v-else>进入物业管理中台 ➔</span>
            </button>
          </form>

          <button type="button" @click="authStep = 2" class="w-full py-2 text-[11px] text-slate-400 hover:text-white">
            返回上一步
          </button>
        </div>

        <div class="pt-1 flex items-center justify-between text-xs text-slate-400">
          <router-link to="/login" class="text-slate-400 hover:text-white underline">
            切换为业主端 H5 登录
          </router-link>
          <button
            @click="fillAdmin"
            class="text-purple-400 hover:text-purple-300 font-semibold underline"
          >
            一键填入王建国账号
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../../store/user'
import api from '../../api'
import OcrUploadBox from '../../components/OcrUploadBox.vue'
import FaceCaptureBox from '../../components/FaceCaptureBox.vue'
import SeedAccountBar from '../../components/SeedAccountBar.vue'

const router = useRouter()
const userStore = useUserStore()

const company = {
  name: '上海安居物业管理服务有限公司',
  creditCode: '91310104MA1FR8992X',
  legalName: '王建国'
}

const authStep = ref(1)
const identifier = ref('13900000001')
const password = ref('123456')
const licenseScanned = ref(false)
const legalIdScanned = ref(false)
const faceMatched = ref(false)
const loading = ref(false)
const errorMsg = ref('')

const handleLicenseScanned = () => {
  licenseScanned.value = true
}

const handleLegalIdScanned = () => {
  legalIdScanned.value = true
}

const handleFaceMatched = () => {
  faceMatched.value = true
}

const completeAllSteps = () => {
  licenseScanned.value = true
  legalIdScanned.value = true
  faceMatched.value = true
  authStep.value = 3
  identifier.value = '13900000001'
  password.value = '123456'
}

const fillAdmin = () => {
  completeAllSteps()
}

const handleFastFill = ({ identifier: id, password: pwd, name }) => {
  identifier.value = id
  password.value = pwd
  if (name === '王建国') completeAllSteps()
}

const handleLogin = async () => {
  if (!faceMatched.value) {
    errorMsg.value = '请先完成法人实人认证'
    return
  }
  loading.value = true
  errorMsg.value = ''
  try {
    const res = await api.post('/auth/login', {
      identifier: identifier.value,
      password: password.value,
      login_type: 'PASSWORD'
    })
    if (res.code === 200) {
      if (res.data.user.role !== 'PROPERTY_ADMIN') {
        throw new Error('该账号非物业管理员账号，无法登录中台')
      }
      userStore.setAuth(res.data.user, res.data.token)
      router.push('/property/dashboard')
    }
  } catch (err) {
    errorMsg.value = err.message
  } finally {
    loading.value = false
  }
}
</script>
