<template>
  <div class="min-h-screen bg-slate-100 flex flex-col pb-24">
    <NavBar />
    <main class="max-w-md w-full mx-auto p-4 space-y-4 flex-1">
      <section class="rounded-2xl bg-gradient-to-br from-blue-900 to-indigo-900 p-5 text-white shadow-md">
        <div class="flex items-center gap-3">
          <span class="flex h-11 w-11 items-center justify-center rounded-2xl bg-white/10 ring-1 ring-white/20"><AppIcon name="clipboard" :size="24" /></span>
          <div>
            <p class="text-[11px] text-blue-200">三证材料申报 · 平台人工审核</p>
            <h1 class="text-lg font-bold mt-0.5">申请小区停车位</h1>
          </div>
        </div>
        <p class="text-xs text-blue-100 mt-4 leading-relaxed">按步骤填写信息并上传证明材料。提交后由平台管理员人工核对原件，审核通过后进入公开轮候队列。人证核验与 OCR 接口暂未接入。</p>
      </section>

      <ol v-if="ready && !identityPending && !activeApplication" aria-label="三证申报步骤" class="grid grid-cols-3 rounded-2xl bg-white border border-slate-200 p-3 shadow-sm">
        <li v-for="(item, index) in steps" :key="item.label">
          <button type="button" @click="goToStep(index + 1)" :disabled="uploading || loading || !ready" :aria-current="step === index + 1 ? 'step' : undefined"
            class="flex w-full flex-col items-center gap-2 rounded-xl py-1 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-blue-500 disabled:opacity-50">
            <span class="flex h-9 w-9 items-center justify-center rounded-xl" :class="step === index + 1 ? 'bg-blue-600 text-white' : 'bg-slate-100 text-slate-500'"><AppIcon :name="item.icon" :size="20" /></span>
            <span class="text-[11px] font-semibold" :class="step === index + 1 ? 'text-blue-700' : 'text-slate-500'">{{ index + 1 }} · {{ item.label }}</span>
          </button>
        </li>
      </ol>

      <div v-if="activeApplication" class="rounded-xl border border-amber-200 bg-amber-50 p-3 text-xs text-amber-900 leading-relaxed">
        您已有正在办理的申请，请先查看当前进度；如需重新申报，可在首页撤销待审核或排队中的申请。
        <router-link to="/" class="block mt-2 text-blue-600 font-semibold">查看当前申请 <AppIcon name="arrow" :size="14" /></router-link>
      </div>
      <div v-if="identityPending" class="rounded-xl border border-blue-200 bg-blue-50 p-4 text-xs text-blue-900 leading-relaxed">
        身份资料尚待物业核实。请先联系物业完成核验，之后再上传三证材料并申请车位。
        <router-link to="/" class="block mt-2 font-semibold text-blue-700">返回首页 <AppIcon name="arrow" :size="14" /></router-link>
      </div>
      <p v-if="draftLoading" class="text-center text-xs text-slate-500 py-3"><AppIcon name="loading" class="animate-spin" :size="17" /> 正在读取材料草稿…</p>
      <div v-if="error" role="alert" class="rounded-xl border border-rose-200 bg-rose-50 p-3 text-xs text-rose-700 leading-relaxed">
        <AppIcon name="warning" :size="16" /> {{ error }}
        <button v-if="!ready" type="button" @click="loadDraft" class="block mt-2 font-semibold underline">重新读取</button>
      </div>

      <form v-if="ready && !identityPending && !activeApplication" @submit.prevent="step < 3 ? goToStep(step + 1) : submit()" class="rounded-2xl bg-white border border-slate-200 shadow-sm p-5 space-y-5" :aria-busy="loading || uploading">
        <section v-if="step === 1" class="space-y-4" aria-labelledby="identity-title">
          <div>
            <h2 id="identity-title" class="text-base font-bold text-slate-800">第一步：身份证明</h2>
            <p class="mt-1 text-xs text-slate-500 leading-relaxed">请核对注册信息，并上传清晰完整的证件材料。材料真实性由平台管理员人工核验。</p>
          </div>
          <dl class="rounded-xl bg-slate-50 border border-slate-200 p-3 space-y-2 text-xs">
            <div class="flex justify-between gap-3"><dt class="text-slate-500">申请人</dt><dd class="font-medium text-slate-800">{{ profile.applicant_name }}</dd></div>
            <div class="flex justify-between gap-3"><dt class="text-slate-500">手机号</dt><dd class="font-medium text-slate-800">{{ profile.phone }}</dd></div>
            <div class="flex justify-between gap-3"><dt class="text-slate-500">登记证件号</dt><dd class="font-mono text-slate-800">{{ profile.id_card_masked }}</dd></div>
            <div class="flex justify-between gap-3"><dt class="text-slate-500">所属小区</dt><dd class="font-medium text-slate-800">{{ userStore.communityName }}</dd></div>
          </dl>
          <ApplicationDocumentUpload type="ID_CARD" title="身份证明" icon="identity" v-model="documents.ID_CARD"
            hint="请将身份证正反面合成一张清晰图片，或合并为一个 PDF 后上传；每类材料只能保存一个文件。" @busy="setBusy('ID_CARD', $event)" />
        </section>

        <section v-else-if="step === 2" class="space-y-4" aria-labelledby="property-title">
          <div>
            <h2 id="property-title" class="text-base font-bold text-slate-800">第二步：房屋证明</h2>
            <p class="mt-1 text-xs text-slate-500 leading-relaxed">填写本小区实际房屋地址，上传对应的房屋权属或居住证明。</p>
          </div>
          <div>
            <label for="propertyNo" class="flex items-center gap-2 text-sm font-semibold text-slate-700 mb-2"><AppIcon name="building" :size="18" class="text-blue-600" /> 本小区房屋地址 <span class="text-rose-500">*</span></label>
            <input id="propertyNo" v-model.trim="form.property_no" required maxlength="100" placeholder="如：12号楼302室"
              class="w-full rounded-xl border border-slate-300 px-3.5 py-3 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500" />
          </div>
          <ApplicationDocumentUpload type="PROPERTY_CERT" title="房屋证明" icon="home" v-model="documents.PROPERTY_CERT"
            hint="上传产权证、公房租赁凭证或其他有效居住证明。是否符合本小区申请条件，以物业核验结果为准。" @busy="setBusy('PROPERTY_CERT', $event)" />
        </section>

        <section v-else class="space-y-4" aria-labelledby="vehicle-title">
          <div>
            <h2 id="vehicle-title" class="text-base font-bold text-slate-800">第三步：车辆申报</h2>
            <p class="mt-1 text-xs text-slate-500 leading-relaxed">填写行驶证上的完整车牌号码。非本人车辆还需提供关系证明。</p>
          </div>
          <div>
            <label for="plateNo" class="flex items-center gap-2 text-sm font-semibold text-slate-700 mb-2"><AppIcon name="car" :size="18" class="text-blue-600" /> 车牌号码 <span class="text-rose-500">*</span></label>
            <input id="plateNo" v-model.trim="form.plate_no" required maxlength="12" placeholder="如：沪A12345 或 沪AD12345"
              class="w-full rounded-xl border border-slate-300 px-3.5 py-3 text-sm uppercase focus:outline-none focus:ring-2 focus:ring-blue-500" />
            <p class="mt-1.5 text-[11px] text-slate-500">特殊号牌请联系物业办理。</p>
          </div>
          <div>
            <label for="relationship" class="flex items-center gap-2 text-sm font-semibold text-slate-700 mb-2"><AppIcon name="users" :size="18" class="text-blue-600" /> 车辆与申请人关系</label>
            <div class="relative">
              <select id="relationship" v-model="form.car_relationship" class="w-full appearance-none rounded-xl border border-slate-300 pl-3.5 pr-10 py-3 bg-white text-sm focus:outline-none focus:ring-2 focus:ring-blue-500">
                <option value="SELF">本人</option><option value="SPOUSE">配偶</option><option value="FAMILY">同住家人</option>
              </select>
              <AppIcon name="down" :size="17" class="pointer-events-none absolute right-3.5 top-1/2 -translate-y-1/2 text-slate-400" />
            </div>
          </div>
          <ApplicationDocumentUpload type="VEHICLE_LICENSE" title="车辆行驶证" icon="file-check" v-model="documents.VEHICLE_LICENSE"
            hint="上传能清楚显示车牌号码、所有人及车辆信息的行驶证材料。" @busy="setBusy('VEHICLE_LICENSE', $event)" />
          <ApplicationDocumentUpload v-if="form.car_relationship !== 'SELF'" type="RELATIONSHIP_PROOF" title="车辆关系证明" icon="relationship" v-model="documents.RELATIONSHIP_PROOF"
            :hint="form.car_relationship === 'SPOUSE' ? '配偶车辆请上传结婚证明等关系材料。' : '同住家人车辆请上传户口簿等关系材料。'" @busy="setBusy('RELATIONSHIP_PROOF', $event)" />
          <div class="rounded-xl border border-blue-100 bg-blue-50 p-3 text-xs text-slate-700 leading-relaxed space-y-1">
            <p class="font-semibold">提交前核对</p>
            <p>申请人：{{ profile.applicant_name }}</p>
            <p>房屋：{{ form.property_no || '尚未填写' }}</p>
            <p>材料：已选择 {{ selectedDocuments.length }} 份，提交后由平台管理员人工核验</p>
          </div>
          <label class="flex gap-2.5 text-xs text-slate-600 leading-relaxed">
            <input v-model="acknowledged" type="checkbox" required class="mt-0.5 h-4 w-4 shrink-0 accent-blue-600" />
            <span>我确认所填信息和上传材料真实，并同意按物业要求出示相关证明原件。</span>
          </label>
        </section>

        <div class="flex items-center gap-3 pt-1">
          <button v-if="step > 1" type="button" @click="goToStep(step - 1)" :disabled="uploading || loading"
            class="rounded-xl border border-slate-300 px-4 py-3 text-sm font-semibold text-slate-600 disabled:opacity-50">上一步</button>
          <button v-if="step < 3" type="button" @click="goToStep(step + 1)" :disabled="uploading || loading"
            class="flex flex-1 items-center justify-center gap-2 rounded-xl bg-blue-600 py-3 text-sm text-white font-bold hover:bg-blue-700 disabled:opacity-50">
            下一步 <AppIcon name="arrow" :size="17" />
          </button>
          <button v-else type="submit" :disabled="loading || uploading || !acknowledged || activeApplication"
            class="flex flex-1 items-center justify-center gap-2 rounded-xl bg-blue-600 py-3 text-sm text-white font-bold hover:bg-blue-700 disabled:opacity-50 disabled:cursor-not-allowed">
            <AppIcon :name="loading ? 'loading' : 'send'" :size="17" :class="loading ? 'animate-spin' : ''" />
            {{ loading ? '正在提交…' : '提交平台审核' }}
          </button>
        </div>
      </form>
    </main>
    <BottomNav />
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import api from '../api'
import { useUserStore } from '../store/user'
import NavBar from '../components/NavBar.vue'
import BottomNav from '../components/BottomNav.vue'
import AppIcon from '../components/AppIcon.vue'
import ApplicationDocumentUpload from '../components/ApplicationDocumentUpload.vue'
import { showToast } from '../utils/dialog'

const router = useRouter()
const userStore = useUserStore()
const steps = [{ icon: 'identity', label: '身份证明' }, { icon: 'home', label: '房屋证明' }, { icon: 'car', label: '车辆申报' }]
const step = ref(1)
const profile = ref({})
const form = reactive({ property_no: '', plate_no: '', car_relationship: 'SELF' })
const documents = reactive({ ID_CARD: null, PROPERTY_CERT: null, VEHICLE_LICENSE: null, RELATIONSHIP_PROOF: null })
const busyTypes = reactive({})
const acknowledged = ref(false)
const loading = ref(false)
const draftLoading = ref(false)
const ready = ref(false)
const error = ref('')
const uploading = computed(() => Object.values(busyTypes).some(Boolean))
const activeApplication = computed(() => ['PENDING_REVIEW', 'WAITING', 'ALLOCATED', 'OFFLINE_PENDING'].includes(userStore.homeData?.view_state))
const identityPending = computed(() => userStore.homeData?.user_info?.identity_pending === true)
const requiredTypes = computed(() => ['ID_CARD', 'PROPERTY_CERT', 'VEHICLE_LICENSE', ...(form.car_relationship === 'SELF' ? [] : ['RELATIONSHIP_PROOF'])])
const selectedDocuments = computed(() => requiredTypes.value.map(type => documents[type]).filter(Boolean))
const setBusy = (type, value) => { busyTypes[type] = value }
const normalizePlate = () => form.plate_no.toUpperCase().replace(/[\s·.\-]/g, '')
const platePattern = /^[京津沪渝冀豫云辽黑湘皖鲁新苏浙赣鄂桂甘晋蒙陕吉闽贵粤青藏川宁琼使领][A-HJ-NP-Z][A-HJ-NP-Z0-9]{4,5}[A-HJ-NP-Z0-9挂学警港澳]$/

const validateStep = (value) => {
  if (value === 1 && !documents.ID_CARD) return '请先上传身份证明'
  if (value === 2 && !form.property_no) return '请填写本小区房屋地址'
  if (value === 2 && !documents.PROPERTY_CERT) return '请上传房屋证明'
  if (value === 3 && !platePattern.test(normalizePlate())) return '请输入完整车牌号码，例如沪A12345或沪AD12345'
  if (value === 3 && !documents.VEHICLE_LICENSE) return '请上传车辆行驶证'
  if (value === 3 && form.car_relationship !== 'SELF' && !documents.RELATIONSHIP_PROOF) return '请上传配偶或同住家人的关系证明'
  return ''
}
const goToStep = (target) => {
  if (loading.value || uploading.value || !ready.value || identityPending.value || activeApplication.value) return
  error.value = ''
  if (target > step.value) {
    for (let index = 1; index < target; index++) {
      const message = validateStep(index)
      if (message) { error.value = message; step.value = index; return }
    }
  }
  step.value = target
}
const loadDraft = async () => {
  draftLoading.value = true
  error.value = ''
  try {
    const [response] = await Promise.all([api.get('/parking/application-documents'), userStore.fetchHomeData()])
    profile.value = response.data
    Object.keys(documents).forEach(type => { documents[type] = response.data.documents.find(doc => doc.type === type) || null })
    ready.value = true
  } catch (err) { error.value = err.message || '材料草稿读取失败，请重试' }
  finally { draftLoading.value = false }
}
const submit = async () => {
  if (loading.value || uploading.value || !acknowledged.value || activeApplication.value || identityPending.value) return
  error.value = ''
  for (let index = 1; index <= 3; index++) {
    const message = validateStep(index)
    if (message) { error.value = message; step.value = index; return }
  }
  loading.value = true
  try {
    const response = await api.post('/parking/apply', { ...form, plate_no: normalizePlate(), document_ids: selectedDocuments.value.map(doc => doc.id) })
    showToast(`申请 #${response.data.id} 已提交，等待平台人工审核`, 'success')
    try { await userStore.fetchHomeData() } catch { /* 首页会自行重试读取状态 */ }
    router.push('/')
  } catch (err) { error.value = err.message || '提交失败，请稍后重试' }
  finally { loading.value = false }
}
onMounted(loadDraft)
</script>
