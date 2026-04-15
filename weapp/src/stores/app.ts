import { defineStore } from 'pinia'
import { ref } from 'vue'
import { configApi } from '../services/api'

export interface AppConfig {
  siteName: string
  siteDescription: string
  contactEmail: string
  contactPhone: string
  copyright: string
}

export const useAppStore = defineStore('app', () => {
  const config = ref<AppConfig>({
    siteName: 'NutriAI营养助手',
    siteDescription: '智能营养健康管理平台',
    contactEmail: '',
    contactPhone: '',
    copyright: '© 2026 NutriAI'
  })

  async function fetchConfig() {
    try {
      const res: any = await configApi.getPublicConfig()
      if (res) {
        config.value = { ...config.value, ...res }
      }
    } catch {}
  }

  return { config, fetchConfig }
})
