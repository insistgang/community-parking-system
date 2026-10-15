<template>
  <div class="min-h-screen bg-slate-100 flex flex-col pb-24">
    <NavBar />
    <main class="max-w-md w-full mx-auto p-4 space-y-4 flex-1">
      <section class="rounded-2xl bg-gradient-to-br from-blue-900 to-indigo-900 p-5 text-white shadow-md" aria-labelledby="apply-title">
        <div class="flex items-center gap-3">
          <span class="flex h-11 w-11 items-center justify-center rounded-2xl bg-white/10 ring-1 ring-white/20">
            <AppIcon name="clipboard" :size="24" />
          </span>
          <div>
            <p class="text-[11px] font-medium text-blue-200">慧停管家 · 车位服务</p>
            <h1 id="apply-title" class="text-lg font-bold mt-0.5">申请小区停车位</h1>
          </div>
        </div>
        <p class="text-xs text-blue-100 mt-4 leading-relaxed">填写房屋和车辆信息，物业核对材料后，按审核结果进入公开轮候队列。</p>
      </section>

      <ol aria-label="申请办理流程" class="grid grid-cols-3 rounded-2xl bg-white border border-slate-200 p-4 shadow-sm">
        <li v-for="(step, index) in steps" :key="step.label" class="relative flex flex-col items-center gap-2 text-center">
          <span v-if="index < steps.length - 1" class="absolute top-4 left-[calc(50%+1.25rem)] right-[calc(-50%+1.25rem)] h-px bg-slate-200" aria-hidden="true"></span>
          <span class="relative flex h-8 w-8 items-center justify-center rounded-xl" :class="index === 0 ? 'bg-blue-600 text-white' : 'bg-slate-100 text-slate-500'">
            <AppIcon :name="step.icon" :size="18" />
          </span>
          <span class="text-[11px] font-semibold" :class="index === 0 ? 'text-blue-700' : 'text-slate-500'">{{ step.label }}</span>
        </li>
      </ol>

      <section class="rounded-2xl bg-amber-50 border border-amber-200 p-4" aria-labelledby="materials-title">
        <h2 id="materials-title" class="flex items-center gap-2 text-xs font-bold text-amber-900">
          <AppIcon name="folder" :size="17" /> 请准备证明材料
        </h2>
        <div class="grid grid-cols-3 gap-2 mt-3 text-center text-[11px] text-amber-900">
          <div v-for="item in materials" :key="item.label" class="flex flex-col items-center gap-1.5">
            <AppIcon :name="item.icon" :size="21" />
            <span>{{ item.label }}</span>
          </div>
        </div>
        <p class="mt-3 text-[11px] leading-relaxed text-amber-800">提交后，按物业通知出示相关原件，完成现场核验。</p>
      </section>

      <form @submit.prevent="submit" class="rounded-2xl bg-white border border-slate-200 shadow-sm p-5 space-y-5 text-sm" :aria-busy="loading">
        <div>
          <label for="propertyNo" class="flex items-center gap-2 font-semibold text-slate-700 mb-2">
            <AppIcon name="building" :size="18" class="text-blue-600" /> 本小区房屋地址 <span class="text-rose-500" aria-hidden="true">*</span>
          </label>
          <input id="propertyNo" v-model.trim="form.property_no" required maxlength="100" placeholder="如：12号楼302室"
            class="w-full rounded-xl border border-slate-300 px-3.5 py-3 focus:outline-none focus:ring-2 focus:ring-blue-500" />
        </div>
        <div>
          <label for="plateNo" class="flex items-center gap-2 font-semibold text-slate-700 mb-2">
            <AppIcon name="car" :size="18" class="text-blue-600" /> 车牌号码 <span class="text-rose-500" aria-hidden="true">*</span>
          </label>
          <input id="plateNo" v-model.trim="form.plate_no" required maxlength="32" placeholder="请输入车辆号牌"
            class="w-full rounded-xl border border-slate-300 px-3.5 py-3 focus:outline-none focus:ring-2 focus:ring-blue-500" />
        </div>
        <div>
          <label for="relationship" class="flex items-center gap-2 font-semibold text-slate-700 mb-2">
            <AppIcon name="users" :size="18" class="text-blue-600" /> 车辆与申请人关系
          </label>
          <div class="relative">
            <select id="relationship" v-model="form.car_relationship"
              class="w-full appearance-none rounded-xl border border-slate-300 pl-3.5 pr-10 py-3 bg-white focus:outline-none focus:ring-2 focus:ring-blue-500">
              <option value="SELF">本人</option>
              <option value="SPOUSE">配偶</option>
              <option value="FAMILY">同住家人</option>
            </select>
            <AppIcon name="down" :size="17" class="pointer-events-none absolute right-3.5 top-1/2 -translate-y-1/2 text-slate-400" />
          </div>
        </div>
        <label class="flex gap-2.5 text-xs text-slate-600 leading-relaxed">
          <input v-model="acknowledged" type="checkbox" required class="mt-0.5 h-4 w-4 shrink-0 accent-blue-600" />
          <span>我确认填写的信息真实，并同意在物业审核时出示相关证明材料。</span>
        </label>
        <p v-if="error" role="alert" class="flex items-start gap-2 rounded-xl bg-rose-50 border border-rose-200 p-3 text-xs text-rose-700">
          <AppIcon name="warning" :size="16" /> <span>{{ error }}</span>
        </p>
        <button type="submit" :disabled="loading || !acknowledged"
          class="flex w-full items-center justify-center gap-2 rounded-xl bg-blue-600 py-3 text-white font-bold shadow-lg shadow-blue-500/20 transition hover:bg-blue-700 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-blue-500 focus-visible:ring-offset-2 disabled:opacity-50 disabled:cursor-not-allowed">
          <AppIcon :name="loading ? 'loading' : 'send'" :size="18" :class="loading ? 'animate-spin' : ''" />
          {{ loading ? '正在提交…' : '提交申请，等待物业审核' }}
        </button>
      </form>
    </main>
    <BottomNav />
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import api from '../api'
import { useUserStore } from '../store/user'
import NavBar from '../components/NavBar.vue'
import BottomNav from '../components/BottomNav.vue'
import AppIcon from '../components/AppIcon.vue'

const router = useRouter()
const userStore = useUserStore()
const steps = [
  { icon: 'edit', label: '1 · 填写资料' },
  { icon: 'review', label: '2 · 物业核验' },
  { icon: 'queue', label: '3 · 进入轮候' },
]
const materials = [
  { icon: 'identity', label: '身份证明' },
  { icon: 'home', label: '房屋或居住证明' },
  { icon: 'file-check', label: '车辆行驶证' },
]
const form = reactive({ property_no: '', plate_no: '', car_relationship: 'SELF' })
const acknowledged = ref(false)
const loading = ref(false)
const error = ref('')

const submit = async () => {
  error.value = ''
  loading.value = true
  try {
    await api.post('/parking/apply', { ...form })
    await userStore.fetchHomeData()
    router.push('/')
  } catch (err) {
    error.value = err.message || '提交失败，请稍后重试'
  } finally {
    loading.value = false
  }
}
</script>
