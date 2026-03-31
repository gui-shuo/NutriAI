import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import api from '@/services/api'
import { getMemberPermissions } from '@/services/member'

export const useAuthStore = defineStore('auth', () => {
  // 状态
  const token = ref(localStorage.getItem('token') || '')
  const refreshToken = ref(localStorage.getItem('refreshToken') || '')
  const user = ref(null)
  const permissions = ref(null) // 会员权限信息

  // Getters
  const isTokenExpired = computed(() => {
    if (!token.value) return true
    try {
      const payload = JSON.parse(atob(token.value.split('.')[1]))
      // 提前60秒认为过期，避免边界情况
      return payload.exp * 1000 < Date.now() + 60000
    } catch {
      return true
    }
  })

  const isLoggedIn = computed(() => !!token.value && !!user.value && !isTokenExpired.value)
  const userRole = computed(() => user.value?.role || '')
  const isAdmin = computed(() => ['ADMIN', 'SUPER_ADMIN'].includes(userRole.value))
  const isNutritionist = computed(() => userRole.value === 'NUTRITIONIST')
  const userName = computed(() => user.value?.nickname || user.value?.username || '')
  const isVip = computed(() => permissions.value?.isVip || false)
  const memberTier = computed(() => permissions.value?.tier || 'FREE')
  const aiQuotaRemain = computed(() => permissions.value?.aiQuotaRemain ?? 0)

  // Actions
  const setToken = newToken => {
    token.value = newToken
    localStorage.setItem('token', newToken)
  }

  const setRefreshToken = newRefreshToken => {
    refreshToken.value = newRefreshToken
    localStorage.setItem('refreshToken', newRefreshToken)
  }

  const setUser = userData => {
    user.value = userData
    localStorage.setItem('user', JSON.stringify(userData))
  }

  const login = async loginData => {
    try {
      const response = await api.post('/auth/login', loginData)
      if (response.data.code === 200) {
        const { accessToken, refreshToken: newRefreshToken, userInfo } = response.data.data

        setToken(accessToken)
        setRefreshToken(newRefreshToken)
        setUser(userInfo)

        // 登录后获取会员权限
        fetchPermissions()

        return { success: true, data: response.data.data }
      }
      return { success: false, message: response.data.message }
    } catch (error) {
      console.error('Login failed:', error)
      return {
        success: false,
        message: error.response?.data?.message || '登录失败'
      }
    }
  }

  const register = async registerData => {
    try {
      const response = await api.post('/auth/register', registerData)
      return {
        success: response.data.code === 200,
        message: response.data.message
      }
    } catch (error) {
      console.error('Register failed:', error)
      return {
        success: false,
        message: error.response?.data?.message || '注册失败'
      }
    }
  }

  const logout = async () => {
    try {
      // 调用后端退出接口
      if (token.value) {
        await api.post('/auth/logout')
      }
    } catch (error) {
      console.error('Logout API failed:', error)
    } finally {
      // 清除本地数据
      token.value = ''
      refreshToken.value = ''
      user.value = null
      permissions.value = null

      localStorage.removeItem('token')
      localStorage.removeItem('refreshToken')
      localStorage.removeItem('user')
    }
  }

  const refreshAccessToken = async () => {
    try {
      if (!refreshToken.value) {
        return false
      }

      const response = await api.post('/auth/refresh', {
        refreshToken: refreshToken.value
      })

      if (response.data.code === 200) {
        const { accessToken } = response.data.data
        setToken(accessToken)
        return true
      }

      return false
    } catch (error) {
      console.error('Token refresh failed:', error)
      logout()
      return false
    }
  }

  const updateUserInfo = newUserInfo => {
    user.value = { ...user.value, ...newUserInfo }
    localStorage.setItem('user', JSON.stringify(user.value))
  }

  const fetchPermissions = async () => {
    try {
      const res = await getMemberPermissions()
      if (res.data.code === 200) {
        permissions.value = res.data.data
      }
    } catch (e) {
      console.error('Failed to fetch permissions:', e)
    }
  }

  const checkAuth = () => {
    return isLoggedIn.value
  }

  // 初始化时从localStorage恢复用户信息
  const storedUser = localStorage.getItem('user')
  if (storedUser) {
    try {
      user.value = JSON.parse(storedUser)
    } catch (e) {
      console.error('Failed to parse user data:', e)
      localStorage.removeItem('user')
    }
  }

  return {
    // 状态
    token,
    refreshToken,
    user,
    permissions,
    // Getters
    isLoggedIn,
    isTokenExpired,
    userRole,
    isAdmin,
    isNutritionist,
    userName,
    isVip,
    memberTier,
    aiQuotaRemain,
    // Actions
    setToken,
    setRefreshToken,
    setUser,
    login,
    register,
    logout,
    refreshAccessToken,
    updateUserInfo,
    fetchPermissions,
    checkAuth
  }
})
