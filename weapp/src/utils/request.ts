// ========================================
// NutriAI Request Utility
// ========================================

const TOKEN_KEY = 'nutriai_token'
const REFRESH_TOKEN_KEY = 'nutriai_refresh_token'
const USER_KEY = 'nutriai_user'

// #ifdef H5
const BASE_URL = '/api'
// #endif
// #ifndef H5
const BASE_URL = 'https://nutriai.itshuo.me/api'
// #endif

export function getToken(): string {
  return uni.getStorageSync(TOKEN_KEY) || ''
}

export function setToken(token: string) {
  uni.setStorageSync(TOKEN_KEY, token)
}

export function getRefreshToken(): string {
  return uni.getStorageSync(REFRESH_TOKEN_KEY) || ''
}

export function setRefreshToken(token: string) {
  uni.setStorageSync(REFRESH_TOKEN_KEY, token)
}

export function clearTokens() {
  uni.removeStorageSync(TOKEN_KEY)
  uni.removeStorageSync(REFRESH_TOKEN_KEY)
  uni.removeStorageSync(USER_KEY)
}

export function getStoredUser() {
  const raw = uni.getStorageSync(USER_KEY)
  if (raw) {
    try { return JSON.parse(raw) } catch { return null }
  }
  return null
}

export function setStoredUser(user: any) {
  uni.setStorageSync(USER_KEY, JSON.stringify(user))
}

// JWT decode (simple base64)
export function parseJwt(token: string): any {
  try {
    const base64 = token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/')
    const json = decodeURIComponent(
      atob(base64).split('').map(c =>
        '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2)
      ).join('')
    )
    return JSON.parse(json)
  } catch {
    return null
  }
}

export function isTokenExpired(token: string): boolean {
  const payload = parseJwt(token)
  if (!payload?.exp) return true
  return payload.exp * 1000 < Date.now() + 60000
}

// ========================================
// Request interceptor with token refresh
// ========================================
let isRefreshing = false
let pendingRequests: Array<() => void> = []

async function refreshAccessToken(): Promise<boolean> {
  const refreshToken = getRefreshToken()
  if (!refreshToken) return false
  try {
    const res: any = await new Promise((resolve, reject) => {
      uni.request({
        url: `${BASE_URL}/auth/refresh`,
        method: 'POST',
        data: { refreshToken },
        success: (r) => resolve(r),
        fail: (e) => reject(e)
      })
    })
    if (res.statusCode === 200 && res.data?.data?.accessToken) {
      setToken(res.data.data.accessToken)
      if (res.data.data.refreshToken) {
        setRefreshToken(res.data.data.refreshToken)
      }
      return true
    }
    return false
  } catch {
    return false
  }
}

interface RequestOptions {
  url: string
  method?: 'GET' | 'POST' | 'PUT' | 'DELETE'
  data?: any
  header?: Record<string, string>
  showError?: boolean
  timeout?: number
}

export function request<T = any>(options: RequestOptions): Promise<T> {
  return new Promise((resolve, reject) => {
    const token = getToken()
    const header: Record<string, string> = {
      'Content-Type': 'application/json',
      ...options.header
    }
    if (token) {
      header['Authorization'] = `Bearer ${token}`
    }

    uni.request({
      url: `${BASE_URL}${options.url}`,
      method: options.method || 'GET',
      data: options.data,
      header,
      timeout: options.timeout || 15000,
      success: async (res: any) => {
        if (res.statusCode === 200) {
          const body = res.data
          if (body.code === 200 || body.code === 0 || body.success !== false) {
            resolve(body.data !== undefined ? body.data : body)
          } else {
            if (options.showError !== false) {
              uni.showToast({ title: body.message || '请求失败', icon: 'none' })
            }
            reject(body)
          }
        } else if (res.statusCode === 401 || res.statusCode === 403) {
          // Token expired - try refresh
          if (!isRefreshing) {
            isRefreshing = true
            const ok = await refreshAccessToken()
            isRefreshing = false

            if (ok) {
              // Retry pending requests
              pendingRequests.forEach(cb => cb())
              pendingRequests = []
              // Retry this request
              try {
                const result = await request<T>(options)
                resolve(result)
              } catch (e) {
                reject(e)
              }
            } else {
              clearTokens()
              pendingRequests = []
              uni.showToast({ title: '登录已过期，请重新登录', icon: 'none' })
              setTimeout(() => {
                uni.reLaunch({ url: '/pages/auth/login' })
              }, 1000)
              reject(res.data)
            }
          } else {
            // Queue this request
            return new Promise<void>((resolveQueue) => {
              pendingRequests.push(() => resolveQueue())
            }).then(async () => {
              try {
                const result = await request<T>(options)
                resolve(result)
              } catch (e) {
                reject(e)
              }
            })
          }
        } else {
          if (options.showError !== false) {
            uni.showToast({ title: res.data?.message || `请求错误(${res.statusCode})`, icon: 'none' })
          }
          reject(res.data)
        }
      },
      fail: (err) => {
        if (options.showError !== false) {
          uni.showToast({ title: '网络连接失败', icon: 'none' })
        }
        reject(err)
      }
    })
  })
}

// Upload file
export function uploadFile(options: {
  url: string
  filePath: string
  name?: string
  formData?: Record<string, any>
}): Promise<any> {
  return new Promise((resolve, reject) => {
    const token = getToken()
    const header: Record<string, string> = {}
    if (token) {
      header['Authorization'] = `Bearer ${token}`
    }

    uni.uploadFile({
      url: `${BASE_URL}${options.url}`,
      filePath: options.filePath,
      name: options.name || 'file',
      formData: options.formData,
      header,
      success: (res) => {
        if (res.statusCode === 200) {
          try {
            const data = JSON.parse(res.data)
            resolve(data.data !== undefined ? data.data : data)
          } catch {
            resolve(res.data)
          }
        } else {
          reject(res)
        }
      },
      fail: reject
    })
  })
}

export { BASE_URL }
