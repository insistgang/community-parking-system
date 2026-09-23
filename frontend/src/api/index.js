import axios from 'axios'

const api = axios.create({
  baseURL: '/api',
  timeout: 10000
})

api.interceptors.request.use((config) => {
  const userJson = localStorage.getItem('parking_user')
  if (userJson) {
    try {
      const user = JSON.parse(userJson)
      if (user && user.id) {
        config.headers['X-User-Id'] = user.id
      }
    } catch (e) {
      console.error(e)
    }
  }
  const token = localStorage.getItem('parking_token')
  if (token) {
    config.headers['Authorization'] = `Bearer ${token}`
  }
  return config
})

api.interceptors.response.use(
  (res) => res.data,
  (err) => {
    const msg = err.response?.data?.detail || err.response?.data?.message || err.message || '网络请求失败'
    return Promise.reject(new Error(msg))
  }
)

export default api
