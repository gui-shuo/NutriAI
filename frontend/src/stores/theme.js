import { defineStore } from 'pinia'
import { ref, watch } from 'vue'

export const useThemeStore = defineStore('theme', () => {
  // State
  const isDark = ref(localStorage.getItem('nutri_theme') === 'dark')

  // Watch for theme changes
  watch(
    isDark,
    newValue => {
      if (newValue) {
        document.documentElement.classList.add('dark')
        localStorage.setItem('nutri_theme', 'dark')
      } else {
        document.documentElement.classList.remove('dark')
        localStorage.setItem('nutri_theme', 'light')
      }
    },
    { immediate: true }
  )

  // Actions
  function toggleTheme() {
    isDark.value = !isDark.value
  }

  function setTheme(theme) {
    isDark.value = theme === 'dark'
  }

  return {
    isDark,
    toggleTheme,
    setTheme
  }
})
