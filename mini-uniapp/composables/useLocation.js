/**
 * 位置定位 Composable
 * 封装微信小程序 wx.getLocation 获取用户经纬度
 */
import { ref } from 'vue'

// 缓存位置信息（同一会话内避免重复授权弹窗）
let cachedLocation = null
const CACHE_DURATION = 5 * 60 * 1000 // 5分钟缓存
let cacheTimestamp = 0

export function useLocation() {
  const latitude = ref(null)
  const longitude = ref(null)
  const loading = ref(false)
  const error = ref(null)
  const authorized = ref(false)

  /**
   * 获取用户当前位置（经纬度）
   * @returns {Promise<{latitude: number, longitude: number}>}
   */
  async function getLocation() {
    // 检查缓存
    if (cachedLocation && Date.now() - cacheTimestamp < CACHE_DURATION) {
      latitude.value = cachedLocation.latitude
      longitude.value = cachedLocation.longitude
      authorized.value = true
      return cachedLocation
    }

    loading.value = true
    error.value = null

    try {
      const res = await new Promise((resolve, reject) => {
        uni.getLocation({
          type: 'gcj02', // 国测局坐标系（微信/高德/腾讯地图通用）
          success: resolve,
          fail: reject,
        })
      })

      latitude.value = res.latitude
      longitude.value = res.longitude
      authorized.value = true

      // 更新缓存
      cachedLocation = { latitude: res.latitude, longitude: res.longitude }
      cacheTimestamp = Date.now()

      return cachedLocation
    } catch (e) {
      error.value = e
      authorized.value = false

      // 用户拒绝授权时引导打开设置
      if (e.errMsg && e.errMsg.includes('deny')) {
        handleDenied()
      }

      return null
    } finally {
      loading.value = false
    }
  }

  /**
   * 用户拒绝定位后引导打开设置
   */
  function handleDenied() {
    uni.showModal({
      title: '需要位置授权',
      content: '请在设置中开启位置权限，以便为您推荐附近门店',
      confirmText: '去设置',
      success: (res) => {
        if (res.confirm) {
          uni.openSetting()
        }
      },
    })
  }

  /**
   * 清除缓存的位置信息
   */
  function clearCache() {
    cachedLocation = null
    cacheTimestamp = 0
    latitude.value = null
    longitude.value = null
  }

  return {
    latitude,
    longitude,
    loading,
    error,
    authorized,
    getLocation,
    clearCache,
  }
}
