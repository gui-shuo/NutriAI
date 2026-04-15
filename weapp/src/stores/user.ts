import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { authApi, userApi, memberApi } from '../services/api'
import { getToken, setToken, getRefreshToken, setRefreshToken, clearTokens, getStoredUser, setStoredUser } from '../utils/request'

export interface UserInfo {
  id: number
  username: string
  nickname: string
  email: string
  avatar: string
  role: string
  status: string
  phone?: string
  bio?: string
}

export interface Permissions {
  isVip: boolean
  tier: string
  aiQuotaRemain: number
  aiQuotaTotal: number
  planName: string
  vipExpireAt?: string
}

export const useUserStore = defineStore('user', () => {
  const userInfo = ref<UserInfo | null>(null)
  const token = ref('')
  const permissions = ref<Permissions | null>(null)

  // Computed
  const isLoggedIn = computed(() => !!token.value && !!userInfo.value)
  const userRoles = computed(() => userInfo.value?.role?.split(',') || [])
  const isAdmin = computed(() => userRoles.value.includes('ADMIN'))
  const isNutritionist = computed(() => userRoles.value.includes('NUTRITIONIST'))
  const isVip = computed(() => permissions.value?.isVip || false)
  const displayName = computed(() => userInfo.value?.nickname || userInfo.value?.username || '用户')
  const avatarUrl = computed(() => userInfo.value?.avatar || '')

  // Restore from storage
  function restore() {
    const savedToken = getToken()
    const savedUser = getStoredUser()
    if (savedToken && savedUser) {
      token.value = savedToken
      userInfo.value = savedUser
      fetchPermissions()
    }
  }

  // Login
  async function login(username: string, password: string, captchaKey?: string, captchaCode?: string) {
    try {
      const data: any = { username, password }
      if (captchaKey) data.captchaKey = captchaKey
      if (captchaCode) data.captchaCode = captchaCode

      const res: any = await authApi.login(data)
      _saveLogin(res)
      return { success: true, data: res }
    } catch (e: any) {
      return { success: false, message: e?.message || '登录失败', data: e }
    }
  }

  // WeChat Login
  async function wxLogin() {
    try {
      const loginRes: any = await new Promise((resolve, reject) => {
        uni.login({
          provider: 'weixin',
          success: resolve,
          fail: reject
        })
      })
      const res: any = await authApi.wxLogin(loginRes.code)
      _saveLogin(res)
      return { success: true, data: res }
    } catch (e: any) {
      return { success: false, message: e?.message || '微信登录失败' }
    }
  }

  // Register
  async function register(data: { username: string; email: string; password: string; verificationCode: string }) {
    try {
      await authApi.register(data)
      return { success: true }
    } catch (e: any) {
      return { success: false, message: e?.message || '注册失败' }
    }
  }

  // Logout
  async function logout() {
    try { await authApi.logout() } catch {}
    token.value = ''
    userInfo.value = null
    permissions.value = null
    clearTokens()
  }

  // Fetch profile
  async function fetchUserInfo() {
    try {
      const res: any = await userApi.getProfile()
      userInfo.value = res
      setStoredUser(res)
    } catch {}
  }

  // Fetch permissions
  async function fetchPermissions() {
    try {
      const res: any = await memberApi.getPermissions()
      permissions.value = res
    } catch {}
  }

  // Internal: save login result
  function _saveLogin(res: any) {
    const accessToken = res.accessToken || res.token
    const refreshTk = res.refreshToken
    const user = res.userInfo || res.user

    if (accessToken) {
      token.value = accessToken
      setToken(accessToken)
    }
    if (refreshTk) {
      setRefreshToken(refreshTk)
    }
    if (user) {
      userInfo.value = user
      setStoredUser(user)
    }
    fetchPermissions()
  }

  return {
    userInfo,
    token,
    permissions,
    isLoggedIn,
    userRoles,
    isAdmin,
    isNutritionist,
    isVip,
    displayName,
    avatarUrl,
    restore,
    login,
    wxLogin,
    register,
    logout,
    fetchUserInfo,
    fetchPermissions
  }
})
