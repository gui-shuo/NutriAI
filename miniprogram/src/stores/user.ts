import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { request, setToken, setRefreshToken, clearTokens, getToken } from '@/utils/request'

interface UserInfo {
  id: number
  username: string
  nickname: string
  email: string
  avatar: string
  role: string
  status: string
  phone?: string
}

export const useUserStore = defineStore('user', () => {
  const userInfo = ref<UserInfo | null>(null)
  const token = ref(getToken())
  const isLoggedIn = computed(() => !!token.value && !!userInfo.value)
  const isAdmin = computed(() => userInfo.value?.role === 'ADMIN')

  function _saveLogin(data: any) {
    setToken(data.accessToken)
    setRefreshToken(data.refreshToken)
    token.value = data.accessToken
    userInfo.value = data.userInfo
    uni.setStorageSync('userInfo', data.userInfo)
  }

  // 账号密码登录
  async function login(username: string, password: string, captchaKey?: string, captchaCode?: string) {
    const res = await request<any>({
      url: '/auth/login',
      method: 'POST',
      data: { username, password, captchaKey, captchaCode }
    })
    if (res.code === 200) _saveLogin(res.data)
    return res
  }

  // 注册
  async function register(data: any) {
    return request({ url: '/auth/register', method: 'POST', data })
  }

  // 登出
  function logout() {
    clearTokens()
    token.value = ''
    userInfo.value = null
    uni.removeStorageSync('userInfo')
    uni.reLaunch({ url: '/pages/auth/login' })
  }

  // 从缓存恢复状态
  function restore() {
    const t = getToken()
    const cached = uni.getStorageSync('userInfo')
    if (cached && t) {
      token.value = t
      userInfo.value = cached
    } else {
      token.value = ''
      userInfo.value = null
    }
  }

  // 刷新用户信息
  async function fetchUserInfo() {
    try {
      const res = await request<UserInfo>({ url: '/user/profile', showError: false })
      if (res.code === 200) {
        userInfo.value = res.data
        uni.setStorageSync('userInfo', res.data)
      }
    } catch {}
  }

  return { userInfo, isLoggedIn, isAdmin, login, register, logout, restore, fetchUserInfo }
})
