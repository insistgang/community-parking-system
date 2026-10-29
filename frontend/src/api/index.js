import axios from 'axios'

const api = axios.create({
  baseURL: '/api',
  timeout: 10000
})

api.interceptors.request.use((config) => {
  const token = localStorage.getItem('parking_token')
  if (token) {
    config.headers['Authorization'] = `Bearer ${token}`
  }
  return config
})

api.interceptors.response.use(
  (res) => res.data,
  (err) => {
    if (err.response?.status === 401) {
      localStorage.removeItem('parking_token')
      localStorage.removeItem('parking_user')
      if (window.location.pathname !== '/login' && window.location.pathname !== '/property/login') {
        window.location.replace('/login')
      }
    }
    const msg = err.response?.data?.detail || err.response?.data?.message || err.message || '网络请求失败'
    return Promise.reject(new Error(msg))
  }
)

export default api
