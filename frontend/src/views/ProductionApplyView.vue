<template>
  <div class="min-h-screen bg-slate-100 flex flex-col pb-20">
    <NavBar />
    <main class="max-w-md w-full mx-auto p-4 space-y-4 flex-1">
      <div class="rounded-2xl bg-gradient-to-r from-blue-900 to-indigo-900 p-5 text-white shadow-md">
        <p class="text-[11px] font-bold text-blue-200">慧停管家 · 正式版</p>
        <h1 class="text-lg font-extrabold mt-1">申请小区停车位</h1>
        <p class="text-xs text-blue-100 mt-2 leading-relaxed">提交房屋和车辆信息后，由物业核对证明材料。审核通过后进入公开轮候队列。</p>
      </div>

      <div class="rounded-2xl bg-amber-50 border border-amber-200 p-4 text-xs text-amber-900 leading-relaxed">
        请准备身份证明、房屋权属或居住证明、车辆行驶证。物业将联系您核验原件；本页面不进行自动人脸或证照识别。
      </div>

      <form @submit.prevent="submit" class="rounded-2xl bg-white border border-slate-200 shadow-sm p-5 space-y-4 text-sm">
        <div>
          <label for="propertyNo" class="block font-bold text-slate-700 mb-1.5">本小区房屋地址</label>
          <input id="propertyNo" v-model.trim="form.property_no" required maxlength="100" placeholder="如：12号楼302室"
            class="w-full rounded-xl border border-slate-300 px-3.5 py-3 focus:outline-none focus:ring-2 focus:ring-blue-500" />
        </div>
        <div>
          <label for="plateNo" class="block font-bold text-slate-700 mb-1.5">车牌号码</label>
          <input id="plateNo" v-model.trim="form.plate_no" required maxlength="32" placeholder="请输入车辆号牌"
            class="w-full rounded-xl border border-slate-300 px-3.5 py-3 focus:outline-none focus:ring-2 focus:ring-blue-500" />
        </div>
        <div>
          <label for="relationship" class="block font-bold text-slate-700 mb-1.5">车辆与申请人关系</label>
          <select id="relationship" v-model="form.car_relationship"
            class="w-full rounded-xl border border-slate-300 px-3.5 py-3 bg-white focus:outline-none focus:ring-2 focus:ring-blue-500">
            <option value="SELF">本人</option>
            <option value="SPOUSE">配偶</option>
            <option value="FAMILY">同住家人</option>
          </select>
        </div>
        <label class="flex gap-2.5 text-xs text-slate-600 leading-relaxed">
          <input v-model="acknowledged" type="checkbox" required class="mt-0.5 accent-blue-600" />
          <span>我确认填写的信息真实，并同意在物业审核时出示相关证明材料。</span>
        </label>
        <p v-if="error" role="alert" class="rounded-xl bg-rose-50 border border-rose-200 p-3 text-xs text-rose-700">{{ error }}</p>
        <button type="submit" :disabled="loading || !acknowledged"
          class="w-full rounded-xl bg-blue-600 py-3 text-white font-bold shadow-lg shadow-blue-500/20 disabled:opacity-50">
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

const router = useRouter()
const userStore = useUserStore()
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
