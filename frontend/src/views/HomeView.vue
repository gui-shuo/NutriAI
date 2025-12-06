<template>
  <div class="home-view">
    <!-- 导航栏 -->
    <nav class="navbar">
      <div class="container">
        <div class="nav-content">
          <div class="logo">
            <img src="/logo.svg" alt="NutriAI" class="logo-img" />
            <span class="logo-text">{{ siteName }}</span>
          </div>
          <div class="nav-buttons">
            <el-button link @click="goToAnnouncements">
              <el-icon><Bell /></el-icon>
              公告
            </el-button>
            <el-button v-if="!isLoggedIn" @click="goToLogin" type="primary">登录</el-button>
            <el-button v-if="!isLoggedIn" @click="goToRegister">注册</el-button>
            <el-dropdown v-if="isLoggedIn" @command="handleCommand">
              <el-button type="primary">
                {{ userName }} <el-icon class="el-icon--right"><arrow-down /></el-icon>
              </el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="profile">个人中心</el-dropdown-item>
                  <el-dropdown-item command="ai-chat">AI营养师</el-dropdown-item>
                  <el-dropdown-item command="diet-plan">AI饮食计划</el-dropdown-item>
                  <el-dropdown-item command="food-recognition">AI食物识别</el-dropdown-item>
                  <el-dropdown-item command="food-records">饮食记录</el-dropdown-item>
                  <el-dropdown-item command="membership">会员中心</el-dropdown-item>
                  <el-dropdown-item divided command="logout">退出登录</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
        </div>
      </div>
    </nav>

    <!-- 主要内容区 -->
    <main class="main-content">
      <div class="hero-section">
        <h1 class="hero-title">{{ siteName }}</h1>
        <p class="hero-subtitle">{{ getConfig('system.site_description', '智能营养分析 · 个性化饮食方案 · 健康管理') }}</p>
        <div class="hero-buttons">
          <el-button type="primary" size="large" @click="getStarted">立即开始</el-button>
          <el-button size="large" @click="learnMore">了解更多</el-button>
        </div>
      </div>

      <!-- 功能特色 -->
      <div class="features-section">
        <div class="container">
          <h2 class="section-title">核心功能</h2>
          <div class="features-grid">
            <div class="feature-card" @click="goToFeature('profile')">
              <el-icon :size="48" color="#22c55e"><user /></el-icon>
              <h3>个人中心</h3>
              <p>管理个人资料，记录健康数据</p>
            </div>
            <div class="feature-card" @click="goToFeature('ai-chat')">
              <el-icon :size="48" color="#22c55e"><chatDotRound /></el-icon>
              <h3>AI营养师</h3>
              <p>智能对话，获取专业营养建议</p>
            </div>
            <div class="feature-card" @click="goToFeature('diet-plan')">
              <el-icon :size="48" color="#22c55e"><calendar /></el-icon>
              <h3>AI饮食计划</h3>
              <p>智能生成个性化饮食计划</p>
            </div>
            <div class="feature-card" @click="goToFeature('food-recognition')">
              <el-icon :size="48" color="#22c55e"><camera /></el-icon>
              <h3>AI食物识别</h3>
              <p>拍照识别食物，智能分析营养</p>
            </div>
            <div class="feature-card" @click="goToFeature('food-records')">
              <el-icon :size="48" color="#22c55e"><document /></el-icon>
              <h3>饮食记录</h3>
              <p>记录每日饮食，分析营养摄入</p>
            </div>
            <div class="feature-card" @click="goToFeature('membership')">
              <el-icon :size="48" color="#22c55e"><trophy /></el-icon>
              <h3>会员服务</h3>
              <p>专属功能，更多权益</p>
            </div>
          </div>
        </div>
      </div>
    </main>

    <!-- 页脚 -->
    <footer class="footer">
      <div class="container">
        <div class="footer-content">
          <div class="footer-info">
            <p>{{ getConfig('system.copyright_text', `© 2025 ${siteName}. All rights reserved.`) }}</p>
            <p v-if="getConfig('system.icp_number')" class="icp-number">
              {{ getConfig('system.icp_number') }}
            </p>
          </div>
          <div class="footer-contact">
            <p v-if="getConfig('system.contact_email')">
              <el-icon><Message /></el-icon>
              {{ getConfig('system.contact_email') }}
            </p>
            <p v-if="getConfig('system.support_phone')">
              <el-icon><Phone /></el-icon>
              {{ getConfig('system.support_phone') }}
            </p>
          </div>
        </div>
      </div>
    </footer>
  </div>
</template>

<script setup>
import { computed, onMounted, onUnmounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { ElMessage as message } from 'element-plus'
import { Bell, ArrowDown, User, ChatDotRound, Document, Trophy, Calendar, Camera, Message, Phone } from '@element-plus/icons-vue'
import { usePublicConfig } from '@/composables/usePublicConfig'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

// 使用公开配置
const { config, loadConfig, getConfig, applyConfig, startAutoRefresh } = usePublicConfig()

// 网站名称（从配置获取，带默认值）
const siteName = computed(() => getConfig('system.site_name', 'AI健康饮食规划助手'))

const isLoggedIn = computed(() => authStore.isLoggedIn)
const userName = computed(() => authStore.user?.username || '用户')

// 页面加载时获取配置
let stopAutoRefresh = null
onMounted(async () => {
  await loadConfig()
  applyConfig()
  
  // 可选：每分钟自动刷新配置
  // stopAutoRefresh = startAutoRefresh(60000)
})

onUnmounted(() => {
  if (stopAutoRefresh) {
    stopAutoRefresh()
  }
})

const goToLogin = () => {
  router.push('/login')
}

const goToRegister = () => {
  router.push('/register')
}

const goToAnnouncements = () => {
  router.push('/announcements')
}

const getStarted = () => {
  if (isLoggedIn.value) {
    router.push('/profile')
  } else {
    router.push('/register')
  }
}

const learnMore = () => {
  message.info('更多功能即将推出，敬请期待！')
}

const handleLogout = () => {
  authStore.logout()
  message.success('退出登录成功')
  router.push('/')
}

const handleCommand = (command) => {
  if (command === 'logout') {
    handleLogout()
  } else if (command === 'ai-chat') {
    if (route.path !== '/ai-chat') router.push('/ai-chat')
  } else if (command === 'diet-plan') {
    if (route.path !== '/diet-plan') router.push('/diet-plan')
  } else if (command === 'food-recognition') {
    if (route.path !== '/food-recognition') router.push('/food-recognition')
  } else if (command === 'food-records') {
    if (route.path !== '/food-records') router.push('/food-records')
  } else if (command === 'membership') {
    if (route.path !== '/membership') router.push('/membership')
  }
}

const goToFeature = (feature) => {
  if (!isLoggedIn.value) {
    message.warning('请先登录后使用该功能')
    router.push('/login')
    return
  }
  
  // 检查是否已经在目标路由，避免重复导航
  const targetPath = `/${feature}`
  if (route.path !== targetPath) {
    router.push(targetPath)
  }
}
</script>

<style scoped>
.home-view {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

/* 导航栏 */
.navbar {
  background: white;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  position: sticky;
  top: 0;
  z-index: 100;
}

.container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 20px;
}

.nav-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
  height: 64px;
}

.logo {
  display: flex;
  align-items: center;
  gap: 12px;
}

.logo-img {
  width: 40px;
  height: 40px;
}

.logo-text {
  font-size: 18px;
  font-weight: 600;
  color: #22c55e;
}

.nav-buttons {
  display: flex;
  gap: 12px;
}

/* 主要内容 */
.main-content {
  flex: 1;
}

/* Hero区域 */
.hero-section {
  background: linear-gradient(135deg, #e0f2fe 0%, #dbeafe 100%);
  padding: 100px 20px;
  text-align: center;
}

.hero-title {
  font-size: 48px;
  font-weight: 700;
  color: #1e293b;
  margin-bottom: 16px;
}

.hero-subtitle {
  font-size: 20px;
  color: #64748b;
  margin-bottom: 32px;
}

.hero-buttons {
  display: flex;
  gap: 16px;
  justify-content: center;
}

/* 功能特色 */
.features-section {
  padding: 80px 20px;
  background: white;
}

.section-title {
  font-size: 36px;
  font-weight: 600;
  text-align: center;
  color: #1e293b;
  margin-bottom: 48px;
}

.features-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  gap: 32px;
  max-width: 1000px;
  margin: 0 auto;
}

.feature-card {
  text-align: center;
  padding: 32px;
  border-radius: 12px;
  background: #f8fafc;
  transition: all 0.3s ease;
  cursor: pointer;
}

.feature-card:hover {
  transform: translateY(-8px);
  box-shadow: 0 12px 24px rgba(0, 0, 0, 0.1);
  background: #e0f2fe;
}

.feature-card h3 {
  font-size: 20px;
  font-weight: 600;
  margin: 16px 0 8px;
  color: #1e293b;
}

.feature-card p {
  color: #64748b;
  line-height: 1.6;
}

/* 页脚 */
.footer {
  background: #f8fafc;
  padding: 32px 20px;
  color: #64748b;
  border-top: 1px solid #e2e8f0;
}

.footer-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
  max-width: 1200px;
  margin: 0 auto;
}

.footer-info {
  text-align: left;
}

.footer-info p {
  margin: 4px 0;
}

.icp-number {
  font-size: 12px;
  color: #94a3b8;
}

.footer-contact {
  text-align: right;
}

.footer-contact p {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 4px 0;
  justify-content: flex-end;
}

/* 响应式 */
@media (max-width: 768px) {
  .hero-title {
    font-size: 32px;
  }

  .hero-subtitle {
    font-size: 16px;
  }
  
  .footer-content {
    flex-direction: column;
    gap: 16px;
    text-align: center;
  }
  
  .footer-info,
  .footer-contact {
    text-align: center;
  }
  
  .footer-contact p {
    justify-content: center;
  }

  .features-grid {
    grid-template-columns: 1fr;
  }
}
</style>
