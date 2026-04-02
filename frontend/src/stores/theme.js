import { defineStore } from 'pinia'
import { ref, watch, computed } from 'vue'

// Theme definitions — each theme provides CSS custom property overrides
const THEMES = {
  'health-green': {
    label: '自然绿',
    accent: '#10B981',
    accentSecondary: '#34D399',
    accentRgb: '16, 185, 129',
  },
  'ocean-blue': {
    label: '海洋蓝',
    accent: '#3B82F6',
    accentSecondary: '#60A5FA',
    accentRgb: '59, 130, 246',
  },
  'sunset-orange': {
    label: '暖阳橙',
    accent: '#F59E0B',
    accentSecondary: '#FBBF24',
    accentRgb: '245, 158, 11',
  },
  'berry-purple': {
    label: '浆果紫',
    accent: '#8B5CF6',
    accentSecondary: '#A78BFA',
    accentRgb: '139, 92, 246',
  },
}

const STORAGE_KEY = 'nutri_theme'
const DARK_STORAGE_KEY = 'nutri_dark'

export const useThemeStore = defineStore('theme', () => {
  const currentTheme = ref(localStorage.getItem(STORAGE_KEY) || 'health-green')
  const isDark = ref(localStorage.getItem(DARK_STORAGE_KEY) === 'true')

  const themes = computed(() =>
    Object.entries(THEMES).map(([key, val]) => ({ key, ...val }))
  )

  const themeConfig = computed(() => THEMES[currentTheme.value] || THEMES['health-green'])

  function applyTheme() {
    const root = document.documentElement
    const t = themeConfig.value

    // Apply accent CSS custom properties
    root.style.setProperty('--accent', t.accent)
    root.style.setProperty('--accent-secondary', t.accentSecondary)
    root.style.setProperty('--accent-rgb', t.accentRgb)

    // Element Plus primary color overrides
    root.style.setProperty('--el-color-primary', t.accent)

    // Dark mode class
    if (isDark.value) {
      root.classList.add('dark')
    } else {
      root.classList.remove('dark')
    }
  }

  watch([currentTheme, isDark], () => {
    localStorage.setItem(STORAGE_KEY, currentTheme.value)
    localStorage.setItem(DARK_STORAGE_KEY, String(isDark.value))
    applyTheme()
  }, { immediate: true })

  function setTheme(themeKey) {
    if (THEMES[themeKey]) {
      currentTheme.value = themeKey
    }
  }

  function toggleDark() {
    isDark.value = !isDark.value
  }

  return {
    currentTheme,
    isDark,
    themes,
    themeConfig,
    setTheme,
    toggleDark,
    // Legacy compat
    toggleTheme: toggleDark
  }
})
