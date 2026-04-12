<template>
  <div class="home-view">
    <!-- 导航栏 -->
    <nav class="navbar">
      <div class="container">
        <div class="nav-content">
          <div class="logo">
            <img src="/logo.svg" alt="NutriAI" class="logo-img" />
            <span class="logo-text font-display gradient-text">{{ siteName }}</span>
          </div>
          <div class="nav-buttons">
            <el-button link @click="goToAnnouncements">
              <el-icon><Bell /></el-icon>
              公告
            </el-button>
            <el-button v-if="!isLoggedIn" type="primary" @click="goToLogin"> 登录 </el-button>
            <el-button v-if="!isLoggedIn" @click="goToRegister"> 注册 </el-button>
            <el-dropdown v-if="isLoggedIn" @command="handleCommand">
              <el-button type="primary">
                {{ userName }}
                <el-icon class="el-icon--right">
                  <arrow-down />
                </el-icon>
              </el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="profile"> 个人中心 </el-dropdown-item>
                  <el-dropdown-item command="ai-chat"> AI营养师 </el-dropdown-item>
                  <el-dropdown-item command="diet-plan"> AI饮食计划 </el-dropdown-item>
                  <el-dropdown-item command="food-recognition"> AI食物识别 </el-dropdown-item>
                  <el-dropdown-item command="food-records"> 饮食记录 </el-dropdown-item>
                  <el-dropdown-item command="consultation"> 营养师咨询 </el-dropdown-item>
                  <el-dropdown-item command="product-shop"> 营养产品商城 </el-dropdown-item>
                  <el-dropdown-item command="membership"> 会员中心 </el-dropdown-item>
                  <el-dropdown-item command="community"> 营养圈 </el-dropdown-item>
                  <el-dropdown-item command="feedback"> 意见反馈 </el-dropdown-item>
                  <el-dropdown-item v-if="isAdmin" command="admin"> 管理后台 </el-dropdown-item>
                  <el-dropdown-item divided command="logout"> 退出登录 </el-dropdown-item>
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
        <h1 class="hero-title font-display animate-fade-in-up">
          <span class="gradient-text">{{ siteName }}</span>
        </h1>
        <p class="hero-subtitle font-sans animate-fade-in-up" style="animation-delay: 0.1s">
          {{ getConfig('system.site_description', '智能营养分析 · 个性化饮食方案 · 饮食管理') }}
        </p>
        <div class="hero-buttons animate-fade-in-up" style="animation-delay: 0.2s">
          <el-button type="primary" size="large" @click="getStarted"> 立即开始 </el-button>
          <el-button size="large" @click="learnMore"> 了解更多 </el-button>
        </div>
        <div class="hero-mobile-hint animate-fade-in-up" style="animation-delay: 0.3s">
          📱 手机用户？访问
          <a href="/h5/" target="_blank" class="h5-link">H5移动版</a>
          或前往
          <router-link to="/download" class="h5-link">下载中心</router-link>
          获取APP
        </div>
      </div>

      <!-- 营养师工作台入口 -->
      <div v-if="isNutritionist" class="nutritionist-banner">
        <div class="container">
          <div class="nutritionist-banner-inner">
            <div class="nutritionist-banner-left">
              <span class="nutritionist-banner-icon">🩺</span>
              <div class="nutritionist-banner-text">
                <h3>营养师工作台</h3>
                <p>查看咨询消息，管理您的客户</p>
              </div>
            </div>
            <el-button type="primary" size="large" @click="router.push('/nutritionist/chat')">
              进入聊天室
            </el-button>
          </div>
        </div>
      </div>

      <!-- 功能特色 -->
      <div class="features-section">
        <div class="container">
          <h2 class="section-title font-display"><span class="gradient-text">核心功能</span></h2>
          <div class="features-grid">
            <div class="feature-card" @click="goToFeature('profile')">
              <div class="feature-icon"><el-icon :size="32" color="white">
                <user />
              </el-icon></div>
              <h3 class="font-display">个人中心</h3>
              <p>管理个人资料，记录身体数据</p>
            </div>
            <div class="feature-card" @click="goToFeature('ai-chat')">
              <div class="feature-icon"><el-icon :size="32" color="white">
                <chatDotRound />
              </el-icon></div>
              <h3 class="font-display">AI营养师</h3>
              <p>智能对话，获取专业营养建议</p>
            </div>
            <div class="feature-card" @click="goToFeature('diet-plan')">
              <div class="feature-icon"><el-icon :size="32" color="white">
                <calendar />
              </el-icon></div>
              <h3 class="font-display">AI饮食计划</h3>
              <p>智能生成个性化饮食计划</p>
            </div>
            <div class="feature-card" @click="goToFeature('food-recognition')">
              <div class="feature-icon"><el-icon :size="32" color="white">
                <camera />
              </el-icon></div>
              <h3 class="font-display">AI食物识别</h3>
              <p>拍照识别食物，智能分析营养</p>
            </div>
            <div class="feature-card" @click="goToFeature('food-records')">
              <div class="feature-icon"><el-icon :size="32" color="white">
                <document />
              </el-icon></div>
              <h3 class="font-display">饮食记录</h3>
              <p>记录每日饮食，分析营养摄入</p>
            </div>
            <div class="feature-card" @click="goToFeature('consultation')">
              <div class="feature-icon"><el-icon :size="32" color="white">
                <service />
              </el-icon></div>
              <h3 class="font-display">营养师咨询</h3>
              <p>专业营养师在线咨询，获取个性化指导</p>
            </div>
            <div class="feature-card" @click="goToFeature('product-shop')">
              <div class="feature-icon"><el-icon :size="32" color="white">
                <goods />
              </el-icon></div>
              <h3 class="font-display">营养产品商城</h3>
              <p>优质营养产品，品质生活从此开始</p>
            </div>
            <div class="feature-card" @click="goToFeature('membership')">
              <div class="feature-icon"><el-icon :size="32" color="white">
                <trophy />
              </el-icon></div>
              <h3 class="font-display">会员服务</h3>
              <p>专属功能，更多权益</p>
            </div>
            <div class="feature-card" @click="goToFeature('community')">
              <div class="feature-icon"><el-icon :size="32" color="white">
                <chatDotRound />
              </el-icon></div>
              <h3 class="font-display">营养圈</h3>
              <p>分享饮食心得，交流营养知识</p>
            </div>
          </div>
        </div>
      </div>
    </main>

    <!-- 页脚 -->
    <footer class="footer font-sans">
      <div class="container">
        <div class="footer-content">
          <div class="footer-info">
            <p>
              {{ getConfig('system.copyright_text', `© 2026 ${siteName}. All rights reserved.`) }}
            </p>
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
            <p class="feedback-link" @click="goToFeature('feedback')">
              <el-icon><ChatLineSquare /></el-icon>
              意见反馈
            </p>
          </div>
          <div class="footer-legal">
            <router-link to="/legal/terms">用户协议</router-link>
            <span class="sep">|</span>
            <router-link to="/legal/privacy">隐私政策</router-link>
            <span class="sep">|</span>
            <router-link to="/legal/disclaimer">免责声明</router-link>
            <span class="sep">|</span>
            <a href="/h5/" target="_blank">H5移动版</a>
            <span class="sep">|</span>
            <router-link to="/download">APP下载</router-link>
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
import {
  Bell,
  ArrowDown,
  User,
  ChatDotRound,
  Document,
  Trophy,
  Calendar,
  Camera,
  Message,
  Phone,
  Service,
  Goods,
  ChatLineSquare
} from '@element-plus/icons-vue'
import { usePublicConfig } from '@/composables/usePublicConfig'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

// 使用公开配置
const { loadConfig, getConfig, applyConfig } = usePublicConfig()

// 网站名称（从配置获取，带默认值）
const siteName = computed(() => getConfig('system.site_name', 'NutriAI饮食规划助手'))

const isLoggedIn = computed(() => authStore.isLoggedIn)
const userName = computed(() => authStore.user?.username || '用户')
const isAdmin = computed(() => authStore.isAdmin)
const isNutritionist = computed(() => authStore.isNutritionist)

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

const handleCommand = command => {
  if (command === 'logout') {
    handleLogout()
  } else if (command === 'profile') {
    if (route.path !== '/profile') router.push('/profile')
  } else if (command === 'ai-chat') {
    if (route.path !== '/ai-chat') router.push('/ai-chat')
  } else if (command === 'diet-plan') {
    if (route.path !== '/diet-plan') router.push('/diet-plan')
  } else if (command === 'food-recognition') {
    if (route.path !== '/food-recognition') router.push('/food-recognition')
  } else if (command === 'food-records') {
    if (route.path !== '/food-records') router.push('/food-records')
  } else if (command === 'consultation') {
    if (route.path !== '/consultation') router.push('/consultation')
  } else if (command === 'product-shop') {
    if (route.path !== '/product-shop') router.push('/product-shop')
  } else if (command === 'membership') {
    if (route.path !== '/membership') router.push('/membership')
  } else if (command === 'community') {
    if (route.path !== '/community') router.push('/community')
  } else if (command === 'feedback') {
    if (route.path !== '/feedback') router.push('/feedback')
  } else if (command === 'admin') {
    router.push('/admin/dashboard')
  }
}

const goToFeature = feature => {
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
  font-family: 'Inter', 'PingFang SC', 'Microsoft YaHei', system-ui, sans-serif;
  color: #0F172A;
  background: #FAFAFA;
}

/* 导航栏 */
.navbar {
  border-bottom: 1px solid #E2E8F0;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
  position: sticky;
  top: 0;
  z-index: 100;
  backdrop-filter: blur(12px);
  background: rgba(255, 255, 255, 0.88);
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
  font-size: 22px;
  font-weight: 700;
}

.nav-buttons {
  display: flex;
  gap: 12px;
}

.nav-buttons :deep(.el-button--primary) {
  background: linear-gradient(135deg, #10B981, #34D399);
  border: none;
  border-radius: 10px;
  font-weight: 600;
}

/* 主要内容 */
.main-content {
  flex: 1;
}

/* Hero区域 */
.hero-section {
  background: #FAFAFA;
  padding: 120px 20px 100px;
  text-align: center;
  position: relative;
  overflow: hidden;
}

.hero-section::before {
  content: '';
  position: absolute;
  top: -50%;
  left: -50%;
  width: 200%;
  height: 200%;
  background: radial-gradient(circle at 30% 40%, rgba(16, 185, 129, 0.05) 0%, transparent 50%),
              radial-gradient(circle at 70% 60%, rgba(77, 124, 255, 0.04) 0%, transparent 50%);
  animation: floatBg 8s ease-in-out infinite;
  pointer-events: none;
}

.hero-title {
  font-size: 52px;
  font-weight: 700;
  color: #0F172A;
  margin-bottom: 20px;
  position: relative;
  z-index: 1;
  letter-spacing: -0.02em;
}

.hero-subtitle {
  font-size: 20px;
  color: #64748B;
  margin-bottom: 40px;
  margin-left: auto;
  margin-right: auto;
  position: relative;
  z-index: 1;
  max-width: 600px;
  line-height: 1.7;
}

.hero-buttons {
  display: flex;
  gap: 16px;
  justify-content: center;
  position: relative;
  z-index: 1;
}

.hero-buttons :deep(.el-button--primary) {
  background: linear-gradient(135deg, #10B981, #34D399);
  border: none;
  box-shadow: 0 4px 14px rgba(16, 185, 129, 0.25);
  border-radius: 12px;
  font-weight: 600;
  transition: all 0.3s ease;
}

.hero-buttons :deep(.el-button--primary:hover) {
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(16, 185, 129, 0.35);
}

.hero-buttons :deep(.el-button--default) {
  border: 1px solid #E2E8F0;
  border-radius: 12px;
  font-weight: 600;
  background: #FFFFFF;
  color: #0F172A;
  transition: all 0.3s ease;
}

.hero-buttons :deep(.el-button--default:hover) {
  border-color: #10B981;
  color: #10B981;
  transform: translateY(-2px);
  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.07);
}

.hero-mobile-hint {
  margin-top: 24px;
  font-size: 14px;
  color: #64748B;
  position: relative;
  z-index: 1;
}

.h5-link {
  color: #10B981;
  font-weight: 600;
  text-decoration: none;
  transition: color 0.2s;
}

.h5-link:hover {
  color: #34D399;
  text-decoration: underline;
}

/* 营养师工作台入口 */
.nutritionist-banner {
  background: linear-gradient(135deg, #ECFDF5, #D1FAE5);
  border-top: 1px solid #A7F3D0;
  border-bottom: 1px solid #A7F3D0;
  padding: 24px 20px;
}

.nutritionist-banner-inner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  max-width: 1000px;
  margin: 0 auto;
}

.nutritionist-banner-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.nutritionist-banner-icon {
  font-size: 36px;
}

.nutritionist-banner-text h3 {
  font-size: 18px;
  font-weight: 600;
  color: #065F46;
  margin: 0 0 4px;
  font-family: 'Calistoga', serif;
}

.nutritionist-banner-text p {
  font-size: 14px;
  color: #047857;
  margin: 0;
}

.nutritionist-banner :deep(.el-button--primary) {
  background: linear-gradient(135deg, #10B981, #34D399);
  border: none;
  border-radius: 12px;
  font-weight: 600;
  box-shadow: 0 4px 14px rgba(16, 185, 129, 0.25);
}

/* 功能特色 */
.features-section {
  padding: 80px 20px;
  background: #FFFFFF;
  border-top: 1px solid #E2E8F0;
}

.section-title {
  font-size: 36px;
  font-weight: 700;
  text-align: center;
  color: #0F172A;
  margin-bottom: 48px;
  letter-spacing: -0.02em;
}

.features-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(260px, 1fr));
  gap: 24px;
  max-width: 1000px;
  margin: 0 auto;
}

.feature-card {
  text-align: center;
  padding: 32px 24px;
  border-radius: 16px;
  background: #FFFFFF;
  border: 1px solid #E2E8F0;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
  transition: all 0.3s ease;
  cursor: pointer;
}

.feature-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 10px 15px rgba(0, 0, 0, 0.08);
  border-color: rgba(16, 185, 129, 0.2);
}

.feature-card .feature-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 56px;
  height: 56px;
  border-radius: 16px;
  background: linear-gradient(135deg, #10B981, #34D399);
  box-shadow: 0 4px 14px rgba(16, 185, 129, 0.25);
  margin-bottom: 12px;
}

.feature-card h3 {
  font-size: 18px;
  font-weight: 600;
  margin: 12px 0 8px;
  color: #0F172A;
}

.feature-card p {
  color: #64748B;
  line-height: 1.6;
  font-size: 14px;
}

/* 页脚 */
.footer {
  background: #FFFFFF;
  padding: 32px 20px;
  color: #64748B;
  border-top: 1px solid #E2E8F0;
  font-size: 14px;
}

.footer-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
  max-width: 1200px;
  margin: 0 auto;
  flex-wrap: wrap;
}

.footer-info {
  text-align: left;
}

.footer-info p {
  margin: 4px 0;
}

.icp-number {
  font-size: 12px;
  color: #64748B;
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

.feedback-link {
  cursor: pointer;
  color: #10B981;
  transition: color 0.2s;
}

.feedback-link:hover {
  color: #34D399;
  text-decoration: underline;
}

.footer-legal {
  width: 100%;
  text-align: center;
  margin-top: 16px;
  padding-top: 12px;
  border-top: 1px solid #E2E8F0;
  font-size: 13px;
}

.footer-legal a {
  color: #64748B;
  text-decoration: none;
  transition: color 0.2s;
}

.footer-legal a:hover {
  color: #10B981;
}

.footer-legal .sep {
  margin: 0 8px;
  color: #E2E8F0;
}

/* Animations */
@keyframes floatBg {
  0%, 100% { transform: translateY(0) rotate(0deg); }
  50% { transform: translateY(-20px) rotate(1deg); }
}

/* 响应式 */
@media (max-width: 768px) {
  .hero-section {
    padding: 80px 20px 60px;
  }

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
