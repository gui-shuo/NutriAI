<template>
  <view class="page">
    <!-- Custom Navigation Bar -->
    <view class="nav-bar" :style="{ paddingTop: statusBarHeight + 'px' }">
      <view class="nav-bar-content">
        <text class="nav-title">NutriAI</text>
        <view class="nav-bell" @tap="goTo('/pages/announcements/index')">
          <text class="bell-icon">🔔</text>
        </view>
      </view>
    </view>

    <view class="content" :style="{ paddingTop: navBarTotalHeight + 'px' }">
      <!-- Hero Greeting with gradient background -->
      <view class="hero-section" @tap="handleGreetingTap">
        <view class="hero-bg" />
        <view class="hero-inner">
          <view class="hero-left">
            <image
              v-if="userStore.isLoggedIn"
              class="hero-avatar"
              :src="defaultAvatar(userStore.userInfo?.avatar)"
              mode="aspectFill"
            />
            <view v-else class="hero-avatar-placeholder">👤</view>
          </view>
          <view class="hero-right">
            <text class="hero-greeting" v-if="userStore.isLoggedIn">
              {{ greetingText }}, {{ userStore.userInfo?.nickname || userStore.userInfo?.username }}
            </text>
            <text class="hero-greeting" v-else>{{ greetingText }}, 请登录</text>
            <text class="hero-sub">{{ appStore.config.siteDescription || '智能营养，科学饮食' }}</text>
          </view>
        </view>
      </view>

      <!-- Quick Actions - horizontal scroll -->
      <view class="quick-section">
        <text class="section-label">快捷功能</text>
        <scroll-view class="quick-scroll" scroll-x :show-scrollbar="false">
          <view class="quick-list">
            <view
              v-for="item in quickActions"
              :key="item.label"
              class="quick-item"
              @tap="handleFeatureTap(item)"
            >
              <view class="quick-icon-wrap" :style="{ background: item.bg }">
                <text class="quick-icon">{{ item.icon }}</text>
              </view>
              <text class="quick-label">{{ item.label }}</text>
            </view>
          </view>
        </scroll-view>
      </view>

      <!-- Feature Grid - 2 columns -->
      <view class="section-header">
        <text class="section-label">全部功能</text>
      </view>
      <view class="feature-grid">
        <view
          v-for="item in features"
          :key="item.label"
          class="feature-card"
          @tap="handleFeatureTap(item)"
        >
          <view class="feature-icon-wrap" :style="{ background: item.bg }">
            <text class="feature-icon">{{ item.icon }}</text>
          </view>
          <view class="feature-info">
            <view class="feature-title-row">
              <text class="feature-title">{{ item.label }}</text>
              <text v-if="item.badge" class="vip-badge">VIP</text>
            </view>
            <text class="feature-desc">{{ item.desc }}</text>
          </view>
        </view>
      </view>

      <!-- App Footer -->
      <view class="app-footer">
        <text class="footer-text" v-if="appStore.config.siteDescription">{{ appStore.config.siteDescription }}</text>
        <text class="footer-copy" v-if="appStore.config.copyright">{{ appStore.config.copyright }}</text>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { useUserStore } from '@/stores/user'
import { useAppStore } from '@/stores/app'
import { defaultAvatar } from '@/utils/common'

const userStore = useUserStore()
const appStore = useAppStore()

const statusBarHeight = ref(0)
const navBarTotalHeight = ref(0)

const greetingText = computed(() => {
  const h = new Date().getHours()
  if (h < 6) return '夜深了'
  if (h < 11) return '早上好'
  if (h < 14) return '中午好'
  if (h < 18) return '下午好'
  return '晚上好'
})

const quickActions = [
  { icon: '🤖', label: 'AI对话', path: '/pages/ai-chat/index', isTab: true, bg: 'rgba(16,185,129,0.1)' },
  { icon: '📸', label: '识别食物', path: '/pages/food-recognition/index', bg: 'rgba(59,130,246,0.1)' },
  { icon: '📝', label: '饮食记录', path: '/pages/food-records/index', bg: 'rgba(245,158,11,0.1)' },
  { icon: '📋', label: '饮食计划', path: '/pages/diet-plan/index', badge: 'VIP', bg: 'rgba(139,92,246,0.1)' },
]

const features = [
  { icon: '🤖', label: 'AI营养师', desc: '智能对话，个性化建议', path: '/pages/ai-chat/index', isTab: true, bg: 'linear-gradient(135deg, #10B981, #34D399)' },
  { icon: '📋', label: 'AI饮食计划', desc: '定制专属饮食方案', path: '/pages/diet-plan/index', badge: 'VIP', bg: 'linear-gradient(135deg, #8B5CF6, #A78BFA)' },
  { icon: '📸', label: 'AI食物识别', desc: '拍照识别营养成分', path: '/pages/food-recognition/index', bg: 'linear-gradient(135deg, #3B82F6, #60A5FA)' },
  { icon: '📝', label: '饮食记录', desc: '记录每日饮食摄入', path: '/pages/food-records/index', bg: 'linear-gradient(135deg, #F59E0B, #FBBF24)' },
  { icon: '👨‍⚕️', label: '营养师咨询', desc: '专业营养师在线答疑', path: '/pages/consultation/index', bg: 'linear-gradient(135deg, #EC4899, #F472B6)' },
  { icon: '🛒', label: '营养商城', desc: '精选营养品推荐', path: '/pages/product-shop/index', bg: 'linear-gradient(135deg, #14B8A6, #5EEAD4)' },
  { icon: '🌐', label: '营养圈', desc: '社区交流与分享', path: '/pages/community/index', isTab: true, bg: 'linear-gradient(135deg, #6366F1, #818CF8)' },
  { icon: '💬', label: '意见反馈', desc: '帮助我们改进产品', path: '/pages/feedback/index', bg: 'linear-gradient(135deg, #64748B, #94A3B8)' },
  { icon: '📢', label: '系统公告', desc: '最新通知和活动', path: '/pages/announcements/index', bg: 'linear-gradient(135deg, #EF4444, #F87171)' }
]

onShow(() => {
  const sysInfo = uni.getSystemInfoSync()
  statusBarHeight.value = sysInfo.statusBarHeight || 0
  navBarTotalHeight.value = statusBarHeight.value + 44

  userStore.restore()
  appStore.fetchConfig()
})

function handleGreetingTap() {
  if (!userStore.isLoggedIn) {
    uni.navigateTo({ url: '/pages/auth/login' })
  }
}

function handleFeatureTap(item: { path: string; isTab?: boolean }) {
  if (item.isTab) {
    uni.switchTab({ url: item.path })
  } else {
    uni.navigateTo({ url: item.path })
  }
}

function goTo(url: string) {
  uni.navigateTo({ url })
}
</script>

<style scoped lang="scss">
.page {
  min-height: 100vh;
  background: $background;
}

/* Navigation Bar */
.nav-bar {
  position: fixed;
  top: 0; left: 0; right: 0;
  z-index: 999;
  background: $gradient-accent;
}
.nav-bar-content {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 88rpx;
  padding: 0 32rpx;
}
.nav-title {
  font-size: 36rpx;
  font-weight: 700;
  color: #fff;
  letter-spacing: 2rpx;
  font-family: 'Calistoga', Georgia, 'PingFang SC', serif;
}
.nav-bell {
  width: 64rpx; height: 64rpx;
  display: flex; align-items: center; justify-content: center;
  background: rgba(255,255,255,0.15);
  border-radius: 50%;
}
.bell-icon { font-size: 36rpx; }

.content {
  padding: 0 0 48rpx;
}

/* Hero Section */
.hero-section {
  position: relative;
  margin: 0 0 32rpx;
  overflow: hidden;
}
.hero-bg {
  position: absolute;
  top: 0; left: 0; right: 0; bottom: 0;
  background: $gradient-accent;
  border-radius: 0 0 48rpx 48rpx;
}
.hero-inner {
  position: relative;
  display: flex;
  align-items: center;
  padding: 40rpx 32rpx 48rpx;
}
.hero-avatar, .hero-avatar-placeholder {
  width: 108rpx; height: 108rpx;
  border-radius: 50%;
  margin-right: 28rpx;
  border: 4rpx solid rgba(255,255,255,0.6);
  flex-shrink: 0;
}
.hero-avatar-placeholder {
  background: rgba(255,255,255,0.2);
  display: flex; align-items: center; justify-content: center;
  font-size: 52rpx;
}
.hero-right {
  display: flex;
  flex-direction: column;
  flex: 1;
  min-width: 0;
}
.hero-greeting {
  font-size: 36rpx;
  font-weight: 700;
  color: #fff;
  margin-bottom: 8rpx;
  font-family: 'Calistoga', Georgia, 'PingFang SC', serif;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.hero-sub {
  font-size: 24rpx;
  color: rgba(255,255,255,0.8);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* Quick Actions */
.quick-section {
  padding: 0 32rpx;
  margin-bottom: 36rpx;
}
.section-label {
  display: block;
  font-size: 30rpx;
  font-weight: 600;
  color: $foreground;
  margin-bottom: 20rpx;
  font-family: 'Inter', 'PingFang SC', sans-serif;
}
.quick-scroll {
  white-space: nowrap;
}
.quick-list {
  display: inline-flex;
  gap: 24rpx;
  padding-bottom: 8rpx;
}
.quick-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  width: 136rpx;

  &:active { opacity: 0.7; }
}
.quick-icon-wrap {
  width: 100rpx; height: 100rpx;
  border-radius: $radius-xl;
  display: flex; align-items: center; justify-content: center;
  margin-bottom: 12rpx;
}
.quick-icon { font-size: 44rpx; }
.quick-label {
  font-size: 24rpx;
  color: $foreground;
  font-weight: 500;
  text-align: center;
}

/* Section Header */
.section-header {
  padding: 0 32rpx;
  margin-bottom: 4rpx;
}

/* Feature Grid */
.feature-grid {
  display: flex;
  flex-wrap: wrap;
  padding: 0 24rpx;
  gap: 20rpx;
}
.feature-card {
  width: calc(50% - 10rpx);
  background: $card;
  border-radius: $radius-xl;
  padding: 28rpx 24rpx;
  border: 1rpx solid $border;
  box-shadow: $shadow-sm;
  display: flex;
  flex-direction: column;
  gap: 16rpx;
  transition: transform 0.15s, box-shadow 0.15s;

  &:active {
    transform: translateY(2rpx);
    box-shadow: none;
  }
}
.feature-icon-wrap {
  width: 80rpx; height: 80rpx;
  border-radius: $radius-lg;
  display: flex; align-items: center; justify-content: center;
}
.feature-icon { font-size: 40rpx; }
.feature-info {
  display: flex;
  flex-direction: column;
  gap: 6rpx;
}
.feature-title-row {
  display: flex;
  align-items: center;
  gap: 10rpx;
}
.feature-title {
  font-size: 28rpx;
  font-weight: 600;
  color: $foreground;
  font-family: 'Inter', 'PingFang SC', sans-serif;
}
.vip-badge {
  font-size: 18rpx;
  color: #fff;
  background: $gradient-accent;
  padding: 2rpx 12rpx;
  border-radius: $radius-full;
  font-weight: 500;
  font-family: 'JetBrains Mono', monospace;
  letter-spacing: 0.05em;
}
.feature-desc {
  font-size: 22rpx;
  color: $muted-foreground;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* Footer */
.app-footer {
  text-align: center;
  padding: 48rpx 32rpx 24rpx;
  margin-top: 16rpx;
}
.footer-text {
  display: block;
  font-size: 22rpx;
  color: $muted-foreground;
  margin-bottom: 8rpx;
}
.footer-copy {
  display: block;
  font-size: 20rpx;
  color: $muted-foreground;
  opacity: 0.6;
}
</style>
