<template>
  <div class="min-h-screen bg-slate-100 flex items-center justify-center p-4">
    <div class="max-w-md w-full bg-white rounded-3xl p-6 sm:p-8 shadow-xl border border-slate-200">
      <div class="flex items-center justify-between mb-6">
        <div>
          <h2 class="text-lg font-black text-slate-900">业主账号注册</h2>
          <p class="text-xs text-slate-400 mt-0.5">请选择您所属的上海区划/街道并填写真实信息</p>
        </div>
        <router-link to="/login" class="text-xs text-blue-600 hover:underline font-semibold">
          返回登录
        </router-link>
      </div>

      <form @submit.prevent="handleRegister" class="space-y-3.5 text-xs">
        <!-- 完善的全量十六区与街道级联选择 -->
        <div class="grid grid-cols-2 gap-2">
          <div>
            <div class="flex items-center justify-between mb-1">
              <label class="block font-bold text-slate-700">所属行政区</label>
              <span class="text-[10px] text-slate-400">16区全量</span>
            </div>
            <select
              v-model="form.district"
              @change="handleDistrictChange"
              class="w-full px-3 py-2 rounded-xl border border-slate-200 focus:ring-2 focus:ring-blue-500 font-medium bg-white cursor-pointer"
            >
              <option v-for="item in districtsData" :key="item.district" :value="item.district">
                {{ item.district }}
              </option>
            </select>
          </div>
          <div>
            <div class="flex items-center justify-between mb-1">
              <label class="block font-bold text-slate-700">所属街道/镇</label>
              <span class="text-[10px] text-emerald-600 font-bold">级联自动联动</span>
            </div>
            <select
              v-model="form.subdistrict"
              class="w-full px-3 py-2 rounded-xl border border-slate-200 focus:ring-2 focus:ring-blue-500 font-medium bg-white cursor-pointer"
            >
              <option v-for="sub in currentSubdistricts" :key="sub" :value="sub">
                {{ sub }}
              </option>
            </select>
          </div>
        </div>

        <div>
          <label class="block font-bold text-slate-700 mb-1">小区名称</label>
          <input
            v-model="form.community_name"
            type="text"
            required
            placeholder="如: 龙华新村"
            class="w-full px-3.5 py-2.5 rounded-xl border border-slate-200 focus:ring-2 focus:ring-blue-500 font-medium"
          />
        </div>

        <div>
          <label class="block font-bold text-slate-700 mb-1">真实姓名 (需与身份证及房产证一致)</label>
          <input
            v-model="form.real_name"
            type="text"
            required
            placeholder="请输入您的真实姓名"
            class="w-full px-3.5 py-2.5 rounded-xl border border-slate-200 focus:ring-2 focus:ring-blue-500 font-medium"
          />
        </div>

        <div>
          <label class="block font-bold text-slate-700 mb-1">居民身份证号码 (仅用于政务比对与存证哈希)</label>
          <input
            v-model="form.id_card"
            type="text"
            required
            placeholder="18位居民身份证号码"
            class="w-full px-3.5 py-2.5 rounded-xl border border-slate-200 focus:ring-2 focus:ring-blue-500 font-medium font-mono"
          />
        </div>

        <div>
          <label class="block font-bold text-slate-700 mb-1">手机号码 (支持短信快捷验证码登录)</label>
          <input
            v-model="form.phone"
            type="tel"
            required
            placeholder="11位常用手机号码"
            class="w-full px-3.5 py-2.5 rounded-xl border border-slate-200 focus:ring-2 focus:ring-blue-500 font-medium font-mono"
          />
        </div>

        <div class="grid grid-cols-2 gap-2">
          <div>
            <label class="block font-bold text-slate-700 mb-1">登录密码</label>
            <input
              v-model="form.password"
              type="password"
              required
              placeholder="6-20位密码"
              class="w-full px-3 py-2.5 rounded-xl border border-slate-200 focus:ring-2 focus:ring-blue-500 font-medium"
            />
          </div>
          <div>
            <label class="block font-bold text-slate-700 mb-1">确认密码</label>
            <input
              v-model="form.confirm_pwd"
              type="password"
              required
              placeholder="再次输入密码"
              class="w-full px-3 py-2.5 rounded-xl border border-slate-200 focus:ring-2 focus:ring-blue-500 font-medium"
            />
          </div>
        </div>

        <p class="text-[11px] text-slate-400 leading-relaxed">
          实人认证（身份证核验与人脸比对）将在申请车位时进行，注册阶段仅需填写账号资料。
        </p>

        <div v-if="errorMsg" class="p-2.5 bg-rose-50 border border-rose-200 rounded-xl text-rose-600 text-xs font-medium">
          ⚠️ {{ errorMsg }}
        </div>

        <button
          type="submit"
          :disabled="loading"
          class="w-full py-3 rounded-xl bg-blue-600 hover:bg-blue-700 text-white font-bold shadow-md transition active:scale-[0.98] mt-2 disabled:opacity-40 disabled:pointer-events-none"
        >
          <span v-if="loading">正在注册并接入存证...</span>
          <span v-else>确认注册进入系统</span>
        </button>
      </form>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import api from '../api'

const router = useRouter()
const loading = ref(false)
const errorMsg = ref('')

// 完整的上海市 16 个行政区与全量街道/镇字典
const defaultDistricts = [
  {
    district: '徐汇区',
    subdistricts: ['湖南路街道', '天平路街道', '斜土路街道', '枫林路街道', '徐家汇街道', '康健新村街道', '虹梅路街道', '田林街道', '长桥街道', '龙华街道', '华泾镇', '漕河泾街道', '凌云路街道']
  },
  {
    district: '黄浦区',
    subdistricts: ['外滩街道', '南京东路街道', '半淞园路街道', '小东门街道', '老西门街道', '五里桥街道', '打浦桥街道', '淮海中路街道', '瑞金二路街道', '豫园街道']
  },
  {
    district: '静安区',
    subdistricts: ['江宁路街道', '静安寺街道', '南京西路街道', '曹家渡街道', '天目西路街道', '北站街道', '宝山路街道', '芷江西路街道', '共和新路街道', '大宁路街道', '彭浦新村街道', '临汾路街道', '彭浦镇']
  },
  {
    district: '长宁区',
    subdistricts: ['华阳路街道', '江苏路街道', '新华路街道', '周家桥街道', '天山路街道', '仙霞新村街道', '虹桥街道', '程家桥街道', '北新泾街道', '新泾镇']
  },
  {
    district: '普陀区',
    subdistricts: ['曹杨新村街道', '长风新村街道', '长寿路街道', '石泉路街道', '真如镇街道', '万里街道', '甘泉路街道', '宜川路街道', '桃浦镇']
  },
  {
    district: '虹口区',
    subdistricts: ['四川北路街道', '欧阳路街道', '嘉兴路街道', '曲阳路街道', '广中路街道', '凉城新村街道', '江湾镇街道', '北外滩街道']
  },
  {
    district: '杨浦区',
    subdistricts: ['定海路街道', '平凉路街道', '江浦路街道', '四平路街道', '控江路街道', '延吉新村街道', '长白新村街道', '殷行街道', '大桥街道', '五角场街道', '新江湾城街道', '长海路街道']
  },
  {
    district: '闵行区',
    subdistricts: ['江川路街道', '新虹街道', '古美街道', '浦锦街道', '莘庄镇', '七宝镇', '颛桥镇', '华漕镇', '虹桥镇', '梅陇镇', '吴泾镇', '马桥镇', '浦江镇']
  },
  {
    district: '浦东新区',
    subdistricts: ['潍坊新村街道', '陆家嘴街道', '周家渡街道', '塘桥街道', '上钢新村街道', '南码头路街道', '花木街道', '金杨新村街道', '洋泾街道', '浦兴路街道', '川沙新镇', '高桥镇', '北蔡镇', '三林镇', '张江镇', '金桥镇', '康桥镇', '周浦镇']
  },
  {
    district: '宝山区',
    subdistricts: ['友谊路街道', '吴淞街道', '张庙街道', '罗店镇', '大场镇', '杨行镇', '月浦镇', '罗泾镇', '顾村镇', '高境镇', '庙行镇', '淞南镇']
  },
  {
    district: '嘉定区',
    subdistricts: ['新成路街道', '真新街道', '嘉定镇街道', '南翔镇', '安亭镇', '马陆镇', '江桥镇', '徐行镇', '外冈镇', '华亭镇']
  },
  {
    district: '松江区',
    subdistricts: ['岳阳街道', '永丰街道', '方松街道', '广富林街道', '九里亭街道', '泗泾镇', '佘山镇', '车墩镇', '新桥镇', '洞泾镇', '九亭镇']
  },
  {
    district: '青浦区',
    subdistricts: ['夏阳街道', '盈浦街道', '香花桥街道', '朱家角镇', '练塘镇', '金泽镇', '赵巷镇', '徐泾镇', '华新镇', '重固镇', '白鹤镇']
  },
  {
    district: '奉贤区',
    subdistricts: ['西渡街道', '奉浦街道', '金海街道', '南桥镇', '奉城镇', '庄行镇', '金汇镇', '四团镇', '青村镇', '柘林镇', '海湾镇']
  },
  {
    district: '金山区',
    subdistricts: ['石化街道', '朱泾镇', '枫泾镇', '亭林镇', '漕泾镇', '山阳镇', '金山卫镇', '张堰镇', '廊下镇', '吕巷镇']
  },
  {
    district: '崇明区',
    subdistricts: ['城桥镇', '堡镇', '新河镇', '庙镇', '竖新镇', '向化镇', '三星镇', '港沿镇', '中兴镇', '陈家镇', '绿华镇', '港西镇', '建设镇', '新海镇', '东平镇', '长兴镇', '横沙乡']
  }
]

const districtsData = ref(defaultDistricts)

const form = ref({
  district: '徐汇区',
  subdistrict: '龙华街道',
  community_name: '龙华新村',
  real_name: '',
  id_card: '',
  phone: '',
  password: '',
  confirm_pwd: ''
})

const currentSubdistricts = computed(() => {
  const found = districtsData.value.find(d => d.district === form.value.district)
  return found ? found.subdistricts : []
})

const handleDistrictChange = () => {
  if (currentSubdistricts.value && currentSubdistricts.value.length > 0) {
    form.value.subdistrict = currentSubdistricts.value[0]
  }
}

onMounted(async () => {
  try {
    const res = await api.get('/common/districts')
    if (res.data && res.data.length > 0) {
      districtsData.value = res.data
    }
  } catch (err) {
    // 离线环境使用预置 defaultDistricts
  }
})

const handleRegister = async () => {
  if (form.value.password !== form.value.confirm_pwd) {
    errorMsg.value = '两次输入的密码不一致'
    return
  }
  loading.value = true
  errorMsg.value = ''
  try {
    const res = await api.post('/auth/register', {
      district: form.value.district,
      subdistrict: form.value.subdistrict,
      community_name: form.value.community_name,
      real_name: form.value.real_name,
      id_card: form.value.id_card,
      phone: form.value.phone,
      password: form.value.password
    })
    alert(res.message || '注册成功！请前往登录')
    router.push('/login')
  } catch (err) {
    errorMsg.value = err.message
  } finally {
    loading.value = false
  }
}
</script>
