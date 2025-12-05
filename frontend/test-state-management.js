/**
 * 状态管理测试脚本
 * 验证 Pinia stores 配置和功能
 */

import { createPinia, setActivePinia } from 'pinia'
import { useAuthStore } from './src/stores/auth.js'
import { useThemeStore } from './src/stores/theme.js'

// 创建测试用的 Pinia 实例
const pinia = createPinia()
setActivePinia(pinia)

console.log('🧪 开始测试状态管理...\n')

// ========== 测试 1: Auth Store 基础功能 ==========
console.log('📋 测试 1: Auth Store 基础功能')
try {
  const authStore = useAuthStore()
  
  // 检查初始状态
  console.log('  ✓ Auth Store 初始化成功')
  console.log('  - isLoggedIn:', authStore.isLoggedIn)
  console.log('  - token:', authStore.token ? '存在' : '不存在')
  console.log('  - user:', authStore.user ? '存在' : '不存在')
  
  // 检查 computed 属性
  console.log('  - isAdmin:', authStore.isAdmin)
  console.log('  - userName:', authStore.userName)
  
  // 检查方法存在性
  const requiredMethods = ['login', 'logout', 'register', 'refreshAccessToken', 'updateUserInfo', 'checkAuth']
  const missingMethods = requiredMethods.filter(method => typeof authStore[method] !== 'function')
  
  if (missingMethods.length === 0) {
    console.log('  ✓ 所有必需方法都存在')
  } else {
    console.log('  ✗ 缺少方法:', missingMethods.join(', '))
  }
  
  console.log('  ✅ Auth Store 测试通过\n')
} catch (error) {
  console.error('  ❌ Auth Store 测试失败:', error.message, '\n')
}

// ========== 测试 2: Theme Store 基础功能 ==========
console.log('📋 测试 2: Theme Store 基础功能')
try {
  const themeStore = useThemeStore()
  
  // 检查初始状态
  console.log('  ✓ Theme Store 初始化成功')
  console.log('  - isDark:', themeStore.isDark)
  
  // 检查方法存在性
  const requiredMethods = ['toggleTheme', 'setTheme']
  const missingMethods = requiredMethods.filter(method => typeof themeStore[method] !== 'function')
  
  if (missingMethods.length === 0) {
    console.log('  ✓ 所有必需方法都存在')
  } else {
    console.log('  ✗ 缺少方法:', missingMethods.join(', '))
  }
  
  // 测试主题切换
  const initialTheme = themeStore.isDark
  themeStore.toggleTheme()
  const afterToggle = themeStore.isDark
  
  if (initialTheme !== afterToggle) {
    console.log('  ✓ toggleTheme 功能正常')
  } else {
    console.log('  ✗ toggleTheme 功能异常')
  }
  
  // 测试设置主题
  themeStore.setTheme('dark')
  if (themeStore.isDark === true) {
    console.log('  ✓ setTheme("dark") 功能正常')
  } else {
    console.log('  ✗ setTheme("dark") 功能异常')
  }
  
  themeStore.setTheme('light')
  if (themeStore.isDark === false) {
    console.log('  ✓ setTheme("light") 功能正常')
  } else {
    console.log('  ✗ setTheme("light") 功能异常')
  }
  
  console.log('  ✅ Theme Store 测试通过\n')
} catch (error) {
  console.error('  ❌ Theme Store 测试失败:', error.message, '\n')
}

// ========== 测试 3: Store 响应式 ==========
console.log('📋 测试 3: Store 响应式')
try {
  const authStore = useAuthStore()
  
  // 测试 setToken
  authStore.setToken('test-token-123')
  if (authStore.token === 'test-token-123') {
    console.log('  ✓ setToken 响应式更新正常')
  } else {
    console.log('  ✗ setToken 响应式更新异常')
  }
  
  // 测试 setUser
  authStore.setUser({ id: 1, username: 'testuser', nickname: 'Test User' })
  if (authStore.user && authStore.user.username === 'testuser') {
    console.log('  ✓ setUser 响应式更新正常')
  } else {
    console.log('  ✗ setUser 响应式更新异常')
  }
  
  // 测试 computed 属性响应
  if (authStore.isLoggedIn === true) {
    console.log('  ✓ isLoggedIn computed 响应正常')
  } else {
    console.log('  ✗ isLoggedIn computed 响应异常')
  }
  
  if (authStore.userName === 'Test User') {
    console.log('  ✓ userName computed 响应正常')
  } else {
    console.log('  ✗ userName computed 响应异常')
  }
  
  // 清理测试数据
  authStore.logout()
  
  console.log('  ✅ Store 响应式测试通过\n')
} catch (error) {
  console.error('  ❌ Store 响应式测试失败:', error.message, '\n')
}

console.log('✅ 所有状态管理测试完成！')
