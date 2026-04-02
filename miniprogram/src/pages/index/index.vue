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
      <!-- User Greeting -->
      <view class="greeting-section" @tap="handleGreetingTap">
        <image
          v-if="userStore.isLoggedIn"
          class="avatar"
          :src="defaultAvatar(userStore.userInfo?.avatar)"
          mode="aspectFill"
        />
        <view v-else class="avatar-placeholder">👤</view>
        <view class="greeting-text">
          <text class="greeting-hello" v-if="userStore.isLoggedIn">
            你好, {{ userStore.userInfo?.nickname || userStore.userInfo?.username }}
          </text>
          <text class="greeting-hello" v-else>你好, 请登录</text>
          <text class="greeting-sub">{{ appStore.config.siteDescription }}</text>
        </view>
      </view>

      <!-- Feature Grid -->
      <view class="grid">
        <view
          v-for="item in features"
          :key="item.label"
          class="grid-item card"
          @tap="handleFeatureTap(item)"
        >
          <view class="grid-item-left">
            <text class="grid-icon">{{ item.icon }}</text>
            <view class="grid-label-wrap">
              <text class="grid-label">{{ item.label }}</text>
              <text v-if="item.badge" class="vip-badge">VIP</text>
            </view>
          </view>
          <text class="grid-arrow">›</text>
        </view>
      </view>

      <!-- App Description -->
      <view class="app-desc" v-if="appStore.config.siteDescription">
        <text class="desc-text">{{ appStore.config.siteDescription }}</text>
        <text class="copy-text" v-if="appStore.config.copyright">{{ appStore.config.copyright }}</text>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { useUserStore } from '@/stores/user'
import { useAppStore } from '@/stores/app'
import { defaultAvatar } from '@/utils/common'

const userStore = useUserStore()
const appStore = useAppStore()

const statusBarHeight = ref(0)
const navBarTotalHeight = ref(0)

const features = [
  { icon: '🤖', label: 'AI营养师', path: '/pages/ai-chat/index', isTab: true },
  { icon: '📋', label: 'AI饮食计划', path: '/pages/diet-plan/index', badge: 'VIP' },
  { icon: '📸', label: 'AI食物识别', path: '/pages/food-recognition/index' },
  { icon: '📝', label: '饮食记录', path: '/pages/food-records/index' },
  { icon: '👨‍⚕️', label: '营养师咨询', path: '/pages/consultation/index' },
  { icon: '🛒', label: '营养商城', path: '/pages/product-shop/index' },
  { icon: '🌐', label: '营养圈', path: '/pages/community/index', isTab: true },
  { icon: '💬', label: '意见反馈', path: '/pages/feedback/index' },
  { icon: '📢', label: '系统公告', path: '/pages/announcements/index' }
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

.nav-bar {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 999;
  background: $gradient-accent;
  border-bottom: 1rpx solid rgba(16, 185, 129, 0.15);
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
  width: 64rpx;
  height: 64rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}

.bell-icon {
  font-size: 40rpx;
}

.content {
  padding: 24rpx 24rpx 48rpx;
}

.greeting-section {
  display: flex;
  align-items: center;
  padding: 32rpx;
  margin-bottom: 24rpx;
  background: $card;
  border-radius: $radius-2xl;
  border: 1rpx solid $border;
  box-shadow: $shadow-sm;
}

.avatar {
  width: 96rpx;
  height: 96rpx;
  border-radius: 50%;
  margin-right: 24rpx;
  border: 2rpx solid $border;
}

.avatar-placeholder {
  width: 96rpx;
  height: 96rpx;
  border-radius: 50%;
  margin-right: 24rpx;
  background: $muted;
  border: 2rpx solid $border;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 48rpx;
}

.greeting-text {
  display: flex;
  flex-direction: column;
}

.greeting-hello {
  font-size: 32rpx;
  font-weight: 600;
  color: $foreground;
  margin-bottom: 8rpx;
  font-family: 'Calistoga', Georgia, 'PingFang SC', serif;
}

.greeting-sub {
  font-size: 24rpx;
  color: $muted-foreground;
}

.grid {
  display: flex;
  flex-direction: column;
  gap: 20rpx;
  margin-bottom: 32rpx;
}

.grid-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 28rpx 32rpx;
  background: $card;
  border-radius: $radius-xl;
  border: 1rpx solid $border;
  box-shadow: $shadow-sm;
  transition: transform 0.15s, box-shadow 0.15s;

  &:active {
    transform: translateY(1px);
    box-shadow: none;
  }
}

.grid-item-left {
  display: flex;
  align-items: center;
}

.grid-icon {
  font-size: 44rpx;
  margin-right: 24rpx;
}

.grid-label-wrap {
  display: flex;
  align-items: center;
  gap: 12rpx;
}

.grid-label {
  font-size: 30rpx;
  font-weight: 500;
  color: $foreground;
  font-family: 'Inter', 'PingFang SC', 'Microsoft YaHei', sans-serif;
}

.vip-badge {
  font-size: 20rpx;
  color: #fff;
  background: $gradient-accent;
  padding: 2rpx 16rpx;
  border-radius: $radius-full;
  border: none;
  font-weight: 500;
  font-family: 'JetBrains Mono', monospace;
  letter-spacing: 0.05em;
}

.grid-arrow {
  font-size: 36rpx;
  color: $muted-foreground;
}

.app-desc {
  text-align: center;
  padding: 32rpx 0 24rpx;
  border-top: 1rpx solid $border;
}

.desc-text {
  display: block;
  font-size: 24rpx;
  color: $muted-foreground;
  margin-bottom: 8rpx;
}

.copy-text {
  display: block;
  font-size: 22rpx;
  color: $muted-foreground;
  opacity: 0.7;
}
</style>
