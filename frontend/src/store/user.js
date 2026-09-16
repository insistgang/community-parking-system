import { defineStore } from 'pinia'
import api from '../api'

export const useUserStore = defineStore('user', {
  state: () => ({
    user: JSON.parse(localStorage.getItem('parking_user') || 'null'),
    token: localStorage.getItem('parking_token') || '',
    homeData: null,
    theme: localStorage.getItem('parking_theme') || 'light',
    propertyTheme: localStorage.getItem('parking_property_theme_v2') || 'dark'
  }),
  getters: {
    isLoggedIn: (state) => !!state.token && !!state.user,
    isPropertyAdmin: (state) => state.user?.role === 'PROPERTY_ADMIN',
    realName: (state) => state.user?.real_name || '业主',
    communityName: (state) => state.user?.community_name || '龙华新村',
    isDark: (state) => state.theme === 'dark'
  },
  actions: {
    setAuth(userData, token) {
      this.user = userData
      this.token = token
      localStorage.setItem('parking_user', JSON.stringify(userData))
      localStorage.setItem('parking_token', token)
    },
    initTheme() {
      document.documentElement.classList.toggle('dark', this.theme === 'dark')
    },
    toggleTheme() {
      this.theme = this.theme === 'dark' ? 'light' : 'dark'
      localStorage.setItem('parking_theme', this.theme)
      this.initTheme()
    },
    togglePropertyTheme() {
      this.propertyTheme = this.propertyTheme === 'dark' ? 'light' : 'dark'
      localStorage.setItem('parking_property_theme_v2', this.propertyTheme)
    },
    logout() {
      this.user = null
      this.token = ''
      this.homeData = null
      localStorage.removeItem('parking_user')
      localStorage.removeItem('parking_token')
    },
    async quickLogin(identifier, password) {
      const res = await api.post('/auth/login', {
        identifier,
        password,
        login_type: 'PASSWORD'
      })
      if (res.code === 200 && res.data) {
        this.setAuth(res.data.user, res.data.token)
        return res.data.user
      }
      throw new Error(res.message || '快捷登录失败')
    },
    async fetchHomeData() {
      if (!this.user?.id) return null
      try {
        const res = await api.get('/user/home')
        if (res.code === 200) {
          this.homeData = res.data
          return res.data
        }
      } catch (err) {
        console.error('获取主页数据失败:', err)
      }
      return null
    }
  }
})
