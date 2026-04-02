<template>
  <div class="home-view">
    <!-- 导航栏 -->
    <nav class="navbar">
      <div class="container">
        <div class="nav-content">
          <div class="logo">
            <img src="/logo.svg" alt="NutriAI" class="logo-img" />
            <span class="logo-text font-heading">{{ siteName }}</span>
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
        <h1 class="hero-title font-heading">
          ✏️ {{ siteName }}
        </h1>
        <p class="hero-subtitle font-hand">
          {{ getConfig('system.site_description', '智能营养分析 · 个性化饮食方案 · 饮食管理') }}
        </p>
        <div class="hero-buttons">
          <el-button type="primary" size="large" @click="getStarted"> 立即开始 </el-button>
          <el-button size="large" @click="learnMore"> 了解更多 </el-button>
        </div>
        <div class="hero-mobile-hint">
          📱 手机用户？访问
          <a href="/h5/" target="_blank" class="h5-link">H5移动版</a>
          或前往
          <router-link to="/download" class="h5-link">下载中心</router-link>
          获取APP
        </div>
      </div>

      <!-- 功能特色 -->
      <div class="features-section">
        <div class="container">
          <h2 class="section-title font-heading"><span class="wavy-underline">核心功能</span></h2>
          <div class="features-grid">
            <div class="feature-card" @click="goToFeature('profile')">
              <div class="sketch-icon"><el-icon :size="32" color="#2d5da1">
                <user />
              </el-icon></div>
              <h3 class="font-heading">个人中心</h3>
              <p>管理个人资料，记录身体数据</p>
            </div>
            <div class="feature-card" @click="goToFeature('ai-chat')">
              <div class="sketch-icon"><el-icon :size="32" color="#2d5da1">
                <chatDotRound />
              </el-icon></div>
              <h3 class="font-heading">AI营养师</h3>
              <p>智能对话，获取专业营养建议</p>
            </div>
            <div class="feature-card" @click="goToFeature('diet-plan')">
              <div class="sketch-icon"><el-icon :size="32" color="#2d5da1">
                <calendar />
              </el-icon></div>
              <h3 class="font-heading">AI饮食计划</h3>
              <p>智能生成个性化饮食计划</p>
            </div>
            <div class="feature-card" @click="goToFeature('food-recognition')">
              <div class="sketch-icon"><el-icon :size="32" color="#2d5da1">
                <camera />
              </el-icon></div>
              <h3 class="font-heading">AI食物识别</h3>
              <p>拍照识别食物，智能分析营养</p>
            </div>
            <div class="feature-card" @click="goToFeature('food-records')">
              <div class="sketch-icon"><el-icon :size="32" color="#2d5da1">
                <document />
              </el-icon></div>
              <h3 class="font-heading">饮食记录</h3>
              <p>记录每日饮食，分析营养摄入</p>
            </div>
            <div class="feature-card" @click="goToFeature('consultation')">
              <div class="sketch-icon"><el-icon :size="32" color="#2d5da1">
                <service />
              </el-icon></div>
              <h3 class="font-heading">营养师咨询</h3>
              <p>专业营养师在线咨询，获取个性化指导</p>
            </div>
            <div class="feature-card" @click="goToFeature('product-shop')">
              <div class="sketch-icon"><el-icon :size="32" color="#2d5da1">
                <goods />
              </el-icon></div>
              <h3 class="font-heading">营养产品商城</h3>
              <p>优质营养产品，品质生活从此开始</p>
            </div>
            <div class="feature-card" @click="goToFeature('membership')">
              <div class="sketch-icon"><el-icon :size="32" color="#2d5da1">
                <trophy />
              </el-icon></div>
              <h3 class="font-heading">会员服务</h3>
              <p>专属功能，更多权益</p>
            </div>
            <div class="feature-card" @click="goToFeature('community')">
              <div class="sketch-icon"><el-icon :size="32" color="#2d5da1">
                <chatDotRound />
              </el-icon></div>
              <h3 class="font-heading">营养圈</h3>
              <p>分享饮食心得，交流营养知识</p>
            </div>
          </div>
        </div>
      </div>
    </main>

    <!-- 页脚 -->
    <footer class="footer font-hand">
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
  font-family: 'Patrick Hand', 'PingFang SC', 'Microsoft YaHei', cursive;
  color: #2d2d2d;
}

/* 导航栏 */
.navbar {
  background: #fdfbf7;
  border-bottom: 3px solid #2d2d2d;
  box-shadow: 0 4px 0px 0px rgba(45, 45, 45, 0.08);
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
  font-size: 22px;
  font-weight: 700;
  color: #ff4d4d;
  font-family: 'Kalam', 'ZCOOL KuaiLe', cursive;
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
  background-color: #fdfbf7;
  background-image: radial-gradient(#e5e0d8 1px, transparent 1px);
  background-size: 24px 24px;
  padding: 100px 20px;
  text-align: center;
  position: relative;
}

.hero-title {
  font-size: 48px;
  font-weight: 700;
  color: #2d2d2d;
  margin-bottom: 16px;
  font-family: 'Kalam', 'ZCOOL KuaiLe', cursive;
}

.hero-subtitle {
  font-size: 20px;
  color: #5a5a5a;
  margin-bottom: 32px;
  font-family: 'Patrick Hand', cursive;
}

.hero-buttons {
  display: flex;
  gap: 16px;
  justify-content: center;
}

.hero-mobile-hint {
  margin-top: 20px;
  font-size: 14px;
  color: #5a5a5a;
}
.h5-link {
  color: #2d5da1;
  font-weight: 600;
  text-decoration: none;
  &:hover { color: #ff4d4d; text-decoration: underline; }
}

/* 功能特色 */
.features-section {
  padding: 80px 20px;
  background: #fdfbf7;
  border-top: 2px dashed #e5e0d8;
}

.section-title {
  font-size: 36px;
  font-weight: 600;
  text-align: center;
  color: #2d2d2d;
  margin-bottom: 48px;
  font-family: 'Kalam', 'ZCOOL KuaiLe', cursive;
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
  padding: 32px 24px;
  border-radius: 255px 15px 225px 15px / 15px 225px 15px 255px;
  background: #fff;
  border: 2px solid #2d2d2d;
  box-shadow: 4px 4px 0px 0px #2d2d2d;
  transition: all 0.3s ease;
  cursor: pointer;
}

.feature-card:nth-child(odd) {
  transform: rotate(-1deg);
}
.feature-card:nth-child(even) {
  transform: rotate(1deg);
}

.feature-card:hover {
  transform: translateY(-4px) rotate(0deg);
  box-shadow: 6px 6px 0px 0px #2d2d2d;
  background: #fff9c4;
}

.feature-card .sketch-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 56px;
  height: 56px;
  border: 2px solid #2d2d2d;
  border-radius: 255px 15px 225px 15px / 15px 225px 15px 255px;
  box-shadow: 2px 2px 0px 0px rgba(45, 45, 45, 0.1);
  background: #fdfbf7;
  margin-bottom: 8px;
}

.feature-card h3 {
  font-size: 20px;
  font-weight: 600;
  margin: 12px 0 8px;
  color: #2d2d2d;
  font-family: 'Kalam', 'ZCOOL KuaiLe', cursive;
}

.feature-card p {
  color: #5a5a5a;
  line-height: 1.6;
  font-family: 'Patrick Hand', cursive;
}

/* 页脚 */
.footer {
  background: #fdfbf7;
  padding: 32px 20px;
  color: #5a5a5a;
  border-top: 3px dashed #e5e0d8;
  font-family: 'Patrick Hand', cursive;
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
  color: #5a5a5a;
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
  color: #2d5da1;
  transition: color .2s;
}
.feedback-link:hover { color: #ff4d4d; text-decoration: underline; }

.footer-legal {
  width: 100%;
  text-align: center;
  margin-top: 16px;
  padding-top: 12px;
  border-top: 2px dashed #e5e0d8;
  font-size: 13px;
  a { color: #5a5a5a; text-decoration: none; &:hover { color: #2d5da1; } }
  .sep { margin: 0 8px; color: #e5e0d8; }
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

  .feature-card:nth-child(odd),
  .feature-card:nth-child(even) {
    transform: rotate(0deg);
  }
}
</style>
