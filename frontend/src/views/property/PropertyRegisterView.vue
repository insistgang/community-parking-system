<template>
  <div class="min-h-screen bg-slate-900 text-slate-100 flex flex-col justify-center py-12 sm:px-6 lg:px-8">
    <div class="sm:mx-auto sm:w-full sm:max-w-md text-center">
      <div class="inline-flex items-center justify-center w-14 h-14 rounded-2xl bg-purple-600 text-white shadow-lg shadow-purple-500/30 mb-3">
        <svg xmlns="http://www.w3.org/2000/svg" class="w-8 h-8" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <path d="M16 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"></path>
          <circle cx="8.5" cy="7" r="4"></circle>
          <line x1="20" y1="8" x2="20" y2="14"></line>
          <line x1="23" y1="11" x2="17" y2="11"></line>
        </svg>
      </div>
      <h2 class="text-xl font-black tracking-tight text-white">物业服务企业 · 管理员注册与入驻</h2>
      <p class="mt-1 text-xs text-slate-400">开通小区车位管理</p>
    </div>

    <div class="mt-6 sm:mx-auto sm:w-full sm:max-w-lg px-4">
      <div class="bg-slate-800 py-7 px-6 shadow-2xl rounded-3xl sm:px-10 border border-slate-700 space-y-4">
        <form @submit.prevent="handleRegister" class="space-y-3.5 text-xs">
          <!-- 物业企业名称 -->
          <div>
            <label class="block font-bold text-slate-300 mb-1">物业服务企业名称 (选填)</label>
            <input
              v-model="form.company_name"
              type="text"
              placeholder="如: 上海新纪元物业管理服务有限公司"
              class="w-full px-3.5 py-2.5 rounded-xl bg-slate-900 border border-slate-600 text-white focus:outline-none focus:ring-2 focus:ring-purple-500 font-medium"
            />
          </div>

          <!-- 管辖区域 -->
          <div class="grid grid-cols-2 gap-2">
            <div>
              <label class="block font-bold text-slate-300 mb-1">所属行政区</label>
              <input
                v-model="form.district"
                type="text"
                required
                placeholder="如: 徐汇区"
                class="w-full px-3.5 py-2.5 rounded-xl bg-slate-900 border border-slate-600 text-white focus:outline-none focus:ring-2 focus:ring-purple-500 font-medium"
              />
            </div>
            <div>
              <label class="block font-bold text-slate-300 mb-1">所属街道/镇</label>
              <input
                v-model="form.subdistrict"
                type="text"
                required
                placeholder="如: 湖南路街道"
                class="w-full px-3.5 py-2.5 rounded-xl bg-slate-900 border border-slate-600 text-white focus:outline-none focus:ring-2 focus:ring-purple-500 font-medium"
              />
            </div>
          </div>

          <!-- 管辖小区 -->
          <div>
            <div class="flex items-center justify-between mb-1">
              <label class="block font-bold text-slate-300">管辖小区名称</label>
              <span class="text-[10px] text-purple-400">新小区建档后由物业录入真实车位</span>
            </div>
            <input
              v-model="form.community_name"
              type="text"
              required
              placeholder="如: 龙华新村 (若为新小区将自动建档)"
              class="w-full px-3.5 py-2.5 rounded-xl bg-slate-900 border border-slate-600 text-white focus:outline-none focus:ring-2 focus:ring-purple-500 font-medium"
            />
          </div>

          <!-- 管理员信息 -->
          <div>
            <label class="block font-bold text-slate-300 mb-1">物业入驻邀请码</label>
            <input v-model.trim="form.invite_code" type="password" required autocomplete="off"
              placeholder="请向平台管理员获取邀请码"
              class="w-full px-3.5 py-2.5 rounded-xl bg-slate-900 border border-slate-600 text-white focus:outline-none focus:ring-2 focus:ring-purple-500 font-medium" />
          </div>
          <div class="grid grid-cols-2 gap-2">
            <div>
              <label class="block font-bold text-slate-300 mb-1">管理员真实姓名</label>
              <input
                v-model="form.real_name"
                type="text"
                required
                placeholder="如: 李主管"
                class="w-full px-3.5 py-2.5 rounded-xl bg-slate-900 border border-slate-600 text-white focus:outline-none focus:ring-2 focus:ring-purple-500 font-medium"
              />
            </div>
            <div>
              <label class="block font-bold text-slate-300 mb-1">管理员身份证号</label>
              <input
                v-model="form.id_card"
                type="text"
                required
                placeholder="18位身份证号"
                class="w-full px-3.5 py-2.5 rounded-xl bg-slate-900 border border-slate-600 text-white focus:outline-none focus:ring-2 focus:ring-purple-500 font-medium"
              />
            </div>
          </div>

          <!-- 手机号（登录账号） -->
          <div>
            <label class="block font-bold text-slate-300 mb-1">管理员手机号 (作为系统登录账号)</label>
            <input
              v-model="form.phone"
              type="tel"
              required
              placeholder="请输入11位手机号"
              class="w-full px-3.5 py-2.5 rounded-xl bg-slate-900 border border-slate-600 text-white focus:outline-none focus:ring-2 focus:ring-purple-500 font-medium"
            />
          </div>

          <!-- 密码与确认密码 -->
          <div class="grid grid-cols-2 gap-2">
            <div>
              <label class="block font-bold text-slate-300 mb-1">登录密码</label>
              <input
                v-model="form.password"
                type="password"
                required
                placeholder="请输入密码"
                class="w-full px-3.5 py-2.5 rounded-xl bg-slate-900 border border-slate-600 text-white focus:outline-none focus:ring-2 focus:ring-purple-500 font-medium"
              />
            </div>
            <div>
              <label class="block font-bold text-slate-300 mb-1">确认密码</label>
              <input
                v-model="form.confirm_pwd"
                type="password"
                required
                placeholder="请再次确认密码"
                class="w-full px-3.5 py-2.5 rounded-xl bg-slate-900 border border-slate-600 text-white focus:outline-none focus:ring-2 focus:ring-purple-500 font-medium"
              />
            </div>
          </div>

          <div v-if="errorMsg" class="p-2.5 bg-rose-900/50 border border-rose-600 rounded-xl text-rose-300 text-xs font-medium">
            <AppIcon name="warning" size="1em" /> {{ errorMsg }}
          </div>

          <button
            type="submit"
            :disabled="loading"
            class="w-full py-3 rounded-xl bg-purple-600 hover:bg-purple-700 text-white font-bold text-sm shadow-lg shadow-purple-500/30 transition active:scale-[0.98] disabled:opacity-50 mt-1"
          >
            <span v-if="loading">正在建档并初始化...</span>
            <span v-else>立即注册并进入管理 <AppIcon name="arrow" size="1em" /></span>
          </button>
        </form>

        <div class="pt-3 border-t border-slate-700 flex items-center justify-between text-xs text-slate-400">
          <router-link to="/property/login" class="text-purple-400 hover:text-purple-300 font-semibold underline">
            已有物业账号？返回登录
          </router-link>
          <router-link to="/login" class="text-slate-400 hover:text-white underline">
            返回业主端
          </router-link>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import AppIcon from '../../components/AppIcon.vue'
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../../store/user'
import api from '../../api'

const router = useRouter()
const userStore = useUserStore()

const loading = ref(false)
const errorMsg = ref('')

const form = reactive({
  company_name: '',
  district: '徐汇区',
  subdistrict: '湖南路街道',
  community_name: '',
  real_name: '',
  id_card: '',
  phone: '',
  invite_code: '',
  password: '',
  confirm_pwd: ''
})

const handleRegister = async () => {
  errorMsg.value = ''

  if (form.password !== form.confirm_pwd) {
    errorMsg.value = '两次输入的密码不一致'
    return
  }

  if (form.password.length < 8) {
    errorMsg.value = '密码长度不能少于8位'
    return
  }

  if (!/^1\d{10}$/.test(form.phone)) {
    errorMsg.value = '请输入有效的11位手机号码'
    return
  }

  loading.value = true
  try {
    const res = await api.post('/auth/register', {
      district: form.district,
      subdistrict: form.subdistrict,
      community_name: form.community_name,
      real_name: form.real_name,
      id_card: form.id_card,
      phone: form.phone,
      password: form.password,
      invite_code: form.invite_code,
      role: 'PROPERTY_ADMIN',
      company_name: form.company_name
    })

    if (res.code === 200) {
      // 注册成功，若返回了 token 和 user 直接完成登录
      if (res.data && res.data.token) {
        userStore.setAuth(res.data.user, res.data.token)
        router.push('/property/dashboard')
      } else {
        // 自动调用登录接口
        const loginRes = await api.post('/auth/login', {
          identifier: form.phone,
          password: form.password,
          login_type: 'PASSWORD'
        })
        if (loginRes.code === 200) {
          userStore.setAuth(loginRes.data.user, loginRes.data.token)
          router.push('/property/dashboard')
        }
      }
    }
  } catch (err) {
    errorMsg.value = err.message || '注册失败，请核对信息后重试'
  } finally {
    loading.value = false
  }
}
</script>
