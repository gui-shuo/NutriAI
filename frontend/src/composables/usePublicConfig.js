import { ref } from 'vue'

/**
 * 公开配置组合式函数
 * 用于在用户前端获取和使用系统配置
 */
export function usePublicConfig() {
  const config = ref({})
  const loading = ref(false)
  const error = ref(null)

  /**
   * 加载所有公开配置
   */
  const loadConfig = async () => {
    loading.value = true
    error.value = null

    try {
      const response = await fetch('http://localhost:8080/api/public/config')
      const data = await response.json()

      if (data.code === 200) {
        config.value = data.data
        console.log('公开配置加载成功:', config.value)
        return config.value
      } else {
        error.value = data.message
        console.error('加载公开配置失败:', data.message)
      }
    } catch (err) {
      error.value = err.message
      console.error('加载公开配置失败:', err)
    } finally {
      loading.value = false
    }
  }

  /**
   * 获取特定配置值
   */
  const getConfig = (key, defaultValue = '') => {
    return config.value[key] || defaultValue
  }

  /**
   * 应用网站配置到页面
   */
  const applyConfig = () => {
    // 应用网站名称
    if (config.value['system.site_name']) {
      document.title = config.value['system.site_name']
    }

    // 应用网站图标
    if (config.value['system.site_icon']) {
      const link = document.querySelector("link[rel*='icon']") || document.createElement('link')
      link.type = 'image/x-icon'
      link.rel = 'shortcut icon'
      link.href = config.value['system.site_icon']
      document.getElementsByTagName('head')[0].appendChild(link)
    }

    // 检查维护模式
    if (config.value['system.maintenance_mode'] === 'true') {
      console.warn('系统正在维护中')
      // 可以在这里显示维护页面
    }
  }

  /**
   * 定期刷新配置（可选）
   */
  const startAutoRefresh = (intervalMs = 60000) => {
    const intervalId = setInterval(() => {
      loadConfig().then(() => {
        applyConfig()
      })
    }, intervalMs)

    // 返回清理函数
    return () => clearInterval(intervalId)
  }

  return {
    config,
    loading,
    error,
    loadConfig,
    getConfig,
    applyConfig,
    startAutoRefresh
  }
}
