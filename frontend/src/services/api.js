import axios from 'axios'
import { useAuthStore } from '@/stores/auth'
import router from '@/router'

// 创建axios实例
const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api',
  timeout: 15000,
  headers: {
    'Content-Type': 'application/json'
  }
})

// 是否正在刷新token
let isRefreshing = false
// 重试队列
let requests = []

// 请求拦截器
api.interceptors.request.use(
  config => {
    // 添加请求ID
    config.headers['X-Request-ID'] = `${Date.now()}-${Math.random().toString(36).substr(2, 9)}`

    // 添加认证token
    const authStore = useAuthStore()
    if (authStore.token) {
      config.headers['Authorization'] = `Bearer ${authStore.token}`
    }

    return config
  },
  error => {
    return Promise.reject(error)
  }
)

// 响应拦截器
api.interceptors.response.use(
  response => {
    return response
  },
  async error => {
    const config = error.config
    const status = error.response?.status

    // 401/403 认证失败，尝试刷新token
    if (status === 401 || status === 403) {
      // 避免刷新token的请求本身再次触发刷新
      if (config.url?.includes('/auth/refresh') || config.url?.includes('/auth/login')) {
        return Promise.reject(error)
      }

      if (!isRefreshing) {
        isRefreshing = true
        const authStore = useAuthStore()

        try {
          const refreshed = await authStore.refreshAccessToken()
          if (refreshed) {
            // 刷新成功，重试所有排队的请求
            requests.forEach(cb => cb(authStore.token))
            requests = []
            // 重试当前请求
            config.headers['Authorization'] = `Bearer ${authStore.token}`
            return api.request(config)
          } else {
            // 刷新失败，清除状态并跳转登录
            requests = []
            authStore.logout()
            router.push({ path: '/login', query: { redirect: window.location.pathname } })
            return Promise.reject(error)
          }
        } catch (e) {
          requests = []
          authStore.logout()
          router.push({ path: '/login', query: { redirect: window.location.pathname } })
          return Promise.reject(error)
        } finally {
          isRefreshing = false
        }
      }

      // 正在刷新中，排队等待
      return new Promise(resolve => {
        requests.push(token => {
          config.headers['Authorization'] = `Bearer ${token}`
          resolve(api.request(config))
        })
      })
    }

    return Promise.reject(error)
  }
)

export default api
