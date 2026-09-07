import { defineStore } from 'pinia'
import api from '../api'

export const useUserStore = defineStore('user', {
  state: () => ({
    user: JSON.parse(localStorage.getItem('parking_user') || 'null'),
    token: localStorage.getItem('parking_token') || '',
    homeData: null
  }),
  getters: {
    isLoggedIn: (state) => !!state.token && !!state.user,
    isPropertyAdmin: (state) => state.user?.role === 'PROPERTY_ADMIN',
    realName: (state) => state.user?.real_name || '业主',
    communityName: (state) => state.user?.community_name || '某某新村'
  },
  actions: {
    setAuth(userData, token) {
      this.user = userData
      this.token = token
      localStorage.setItem('parking_user', JSON.stringify(userData))
      localStorage.setItem('parking_token', token)
    },
    logout() {
      this.user = null
      this.token = ''
      this.homeData = null
      localStorage.removeItem('parking_user')
      localStorage.removeItem('parking_token')
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
