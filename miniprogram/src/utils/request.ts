// API 配置
const BASE_URL = 'https://nutriai.itshuo.me/api'

// Token 管理
const getToken = (): string => uni.getStorageSync('accessToken') || ''
const setToken = (token: string) => uni.setStorageSync('accessToken', token)
const getRefreshToken = (): string => uni.getStorageSync('refreshToken') || ''
const setRefreshToken = (token: string) => uni.setStorageSync('refreshToken', token)
const clearTokens = () => {
  uni.removeStorageSync('accessToken')
  uni.removeStorageSync('refreshToken')
}

let isRefreshing = false
let pendingRequests: Array<(token: string) => void> = []

function refreshToken(): Promise<string> {
  return new Promise((resolve, reject) => {
    if (isRefreshing) {
      pendingRequests.push(resolve)
      return
    }
    isRefreshing = true
    uni.request({
      url: `${BASE_URL}/auth/refresh`,
      method: 'POST',
      data: { refreshToken: getRefreshToken() },
      success: (res: any) => {
        if (res.statusCode === 200 && res.data?.code === 200) {
          const { accessToken, refreshToken: newRefresh } = res.data.data
          setToken(accessToken)
          if (newRefresh) setRefreshToken(newRefresh)
          pendingRequests.forEach(cb => cb(accessToken))
          pendingRequests = []
          resolve(accessToken)
        } else {
          clearTokens()
          reject(new Error('refresh failed'))
        }
      },
      fail: () => {
        clearTokens()
        reject(new Error('refresh failed'))
      },
      complete: () => { isRefreshing = false }
    })
  })
}

interface RequestOptions {
  url: string
  method?: 'GET' | 'POST' | 'PUT' | 'DELETE'
  data?: any
  header?: Record<string, string>
  showLoading?: boolean
  loadingText?: string
  showError?: boolean
}

interface ApiResponse<T = any> {
  code: number
  message: string
  data: T
  timestamp: number
}

function request<T = any>(options: RequestOptions): Promise<ApiResponse<T>> {
  const { url, method = 'GET', data, header = {}, showLoading = false, loadingText = '加载中...', showError = true } = options

  if (showLoading) uni.showLoading({ title: loadingText, mask: true })

  const token = getToken()
  if (token) header['Authorization'] = `Bearer ${token}`
  if (!header['Content-Type']) header['Content-Type'] = 'application/json'

  return new Promise((resolve, reject) => {
    uni.request({
      url: url.startsWith('http') ? url : `${BASE_URL}${url}`,
      method,
      data,
      header,
      timeout: 30000,
      success: async (res: any) => {
        if (showLoading) uni.hideLoading()
        // Skip token refresh for auth endpoints — they don't use tokens
        const isAuthUrl = url.includes('/auth/')
        if (!isAuthUrl && (res.statusCode === 401 || res.statusCode === 403)) {
          try {
            const newToken = await refreshToken()
            header['Authorization'] = `Bearer ${newToken}`
            // Retry original request
            const retryRes: any = await new Promise((r, j) => {
              uni.request({
                url: url.startsWith('http') ? url : `${BASE_URL}${url}`,
                method, data, header, timeout: 30000,
                success: r, fail: j
              })
            })
            resolve(retryRes.data)
          } catch {
            clearTokens()
            uni.reLaunch({ url: '/pages/auth/login' })
            reject(new Error('登录已过期'))
          }
          return
        }
        if (res.statusCode >= 200 && res.statusCode < 300) {
          resolve(res.data)
        } else {
          const msg = res.data?.message || `请求失败(${res.statusCode})`
          if (showError) uni.showToast({ title: msg, icon: 'none' })
          reject(new Error(msg))
        }
      },
      fail: (err) => {
        if (showLoading) uni.hideLoading()
        const msg = '网络连接失败，请检查网络'
        if (showError) uni.showToast({ title: msg, icon: 'none' })
        reject(new Error(msg))
      }
    })
  })
}

// Upload file helper
function uploadFile(options: { url: string, filePath: string, name?: string, formData?: Record<string, any> }): Promise<ApiResponse> {
  const token = getToken()
  return new Promise((resolve, reject) => {
    uni.uploadFile({
      url: options.url.startsWith('http') ? options.url : `${BASE_URL}${options.url}`,
      filePath: options.filePath,
      name: options.name || 'file',
      formData: options.formData || {},
      header: token ? { 'Authorization': `Bearer ${token}` } : {},
      success: (res) => {
        if (res.statusCode === 200) {
          try {
            resolve(JSON.parse(res.data))
          } catch {
            resolve(res.data as any)
          }
        } else {
          reject(new Error(`上传失败(${res.statusCode})`))
        }
      },
      fail: () => reject(new Error('上传失败'))
    })
  })
}

export { request, uploadFile, getToken, setToken, getRefreshToken, setRefreshToken, clearTokens, BASE_URL }
export type { ApiResponse, RequestOptions }
