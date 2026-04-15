<template>
  <view class="profile-page">
    <!-- Header -->
    <view class="profile-header" :style="{ paddingTop: statusBarHeight + 'px' }">
      <view class="header-bg"></view>
      <view class="header-content">
        <view class="user-card" v-if="userStore.isLoggedIn">
          <view class="avatar-wrap" @tap="changeAvatar">
            <image
              v-if="userStore.avatarUrl"
              :src="userStore.avatarUrl"
              mode="aspectFill"
              class="user-avatar"
            />
            <view v-else class="avatar-placeholder">
              <text>{{ userStore.displayName.charAt(0) }}</text>
            </view>
          </view>
          <view class="user-info">
            <text class="user-name">{{ userStore.displayName }}</text>
            <text class="user-bio text-sm">{{ userStore.userInfo?.bio || '用营养守护健康' }}</text>
            <view class="user-tags">
              <view v-if="userStore.isVip" class="vip-badge">👑 VIP</view>
              <text class="text-xs text-muted">ID: {{ userStore.userInfo?.id }}</text>
            </view>
          </view>
        </view>
        <view v-else class="login-prompt" @tap="goLogin">
          <view class="avatar-placeholder">
            <text>?</text>
          </view>
          <view class="user-info">
            <text class="user-name">点击登录</text>
            <text class="text-sm text-muted">登录后享受更多服务</text>
          </view>
        </view>
      </view>
    </view>

    <!-- Menu Sections -->
    <scroll-view scroll-y :style="{ height: scrollHeight + 'px' }">
      <!-- Order Section -->
      <view class="menu-section card">
        <view class="section-header">
          <text class="section-title">我的订单</text>
          <text class="section-more" @tap="goOrders('')">全部订单 ›</text>
        </view>
        <view class="order-shortcuts">
          <view class="shortcut-item" @tap="goOrders('PENDING')">
            <text class="shortcut-icon">💰</text>
            <text class="shortcut-text">待付款</text>
          </view>
          <view class="shortcut-item" @tap="goOrders('PAID')">
            <text class="shortcut-icon">📦</text>
            <text class="shortcut-text">待发货</text>
          </view>
          <view class="shortcut-item" @tap="goOrders('SHIPPED')">
            <text class="shortcut-icon">🚚</text>
            <text class="shortcut-text">待收货</text>
          </view>
          <view class="shortcut-item" @tap="goOrders('RECEIVED')">
            <text class="shortcut-icon">✅</text>
            <text class="shortcut-text">已完成</text>
          </view>
        </view>
      </view>

      <!-- Services -->
      <view class="menu-section card">
        <text class="section-title">我的服务</text>
        <view class="menu-list">
          <view class="menu-item" @tap="goPage('/pages/address/index')">
            <text class="menu-icon">📍</text>
            <text class="menu-label">收货地址</text>
            <text class="menu-arrow">›</text>
          </view>
          <view class="menu-item" @tap="goPage('/pages/cart/index')">
            <text class="menu-icon">🛒</text>
            <text class="menu-label">购物车</text>
            <text class="menu-arrow">›</text>
          </view>
        </view>
      </view>

      <!-- Settings -->
      <view class="menu-section card">
        <text class="section-title">账号设置</text>
        <view class="menu-list">
          <view class="menu-item" @tap="goPage('/pages/profile/settings')">
            <text class="menu-icon">⚙️</text>
            <text class="menu-label">账号与安全</text>
            <text class="menu-arrow">›</text>
          </view>
          <view v-if="userStore.isLoggedIn" class="menu-item" @tap="handleLogout">
            <text class="menu-icon">🚪</text>
            <text class="menu-label text-error">退出登录</text>
            <text class="menu-arrow">›</text>
          </view>
        </view>
      </view>

      <!-- App Info -->
      <view class="app-info">
        <text class="text-xs text-muted">NutriAI 营养助手 v1.0.0</text>
      </view>
    </scroll-view>
  </view>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { useUserStore } from '../../stores/user'
import { showConfirm } from '../../utils'

const userStore = useUserStore()
const statusBarHeight = ref(0)
const scrollHeight = ref(600)

onMounted(() => {
  const sysInfo = uni.getSystemInfoSync()
  statusBarHeight.value = sysInfo.statusBarHeight || 20
  // Calculate scroll area height
  const headerH = (sysInfo.statusBarHeight || 20) + 200
  scrollHeight.value = sysInfo.windowHeight - headerH
})

onShow(() => {
  if (userStore.isLoggedIn) {
    userStore.fetchUserInfo()
  }
})

function goLogin() {
  uni.navigateTo({ url: '/pages/auth/login' })
}

function goOrders(status: string) {
  if (!userStore.isLoggedIn) { goLogin(); return }
  uni.navigateTo({ url: `/pages/profile/orders${status ? '?status=' + status : ''}` })
}

function goPage(url: string) {
  if (!userStore.isLoggedIn) { goLogin(); return }
  uni.navigateTo({ url })
}

function changeAvatar() {
  uni.chooseImage({
    count: 1,
    sizeType: ['compressed'],
    sourceType: ['album', 'camera'],
    success: async (res) => {
      try {
        const { userApi } = await import('../../services/api')
        await userApi.uploadAvatar(res.tempFilePaths[0])
        userStore.fetchUserInfo()
        uni.showToast({ title: '头像已更新', icon: 'success' })
      } catch {
        uni.showToast({ title: '上传失败', icon: 'none' })
      }
    }
  })
}

async function handleLogout() {
  const ok = await showConfirm('确定要退出登录吗？')
  if (ok) {
    await userStore.logout()
    uni.showToast({ title: '已退出登录', icon: 'success' })
  }
}
</script>

<style lang="scss" scoped>
.profile-page {
  min-height: 100vh;
  background: $bg-page;
}

.profile-header {
  position: relative;
  padding-bottom: $spacing-lg;
}

.header-bg {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 300rpx;
  background: $gradient-hero;
  border-radius: 0 0 40rpx 40rpx;
}

.header-content {
  position: relative;
  z-index: 1;
  padding: $spacing-xl $spacing-lg 0;
}

.user-card, .login-prompt {
  display: flex;
  align-items: center;
  gap: $spacing-lg;
}

.avatar-wrap {
  position: relative;
}

.user-avatar {
  width: 128rpx;
  height: 128rpx;
  border-radius: 50%;
  border: 4rpx solid rgba(255, 255, 255, 0.5);
}

.avatar-placeholder {
  width: 128rpx;
  height: 128rpx;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.2);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: $font-2xl;
  color: #fff;
  font-weight: 700;
  border: 4rpx solid rgba(255, 255, 255, 0.3);
}

.user-info {
  flex: 1;
}

.user-name {
  display: block;
  font-size: $font-xl;
  font-weight: 700;
  color: #fff;
}

.user-bio {
  display: block;
  color: rgba(255, 255, 255, 0.8);
  margin-top: 4rpx;
}

.user-tags {
  display: flex;
  align-items: center;
  gap: $spacing-sm;
  margin-top: $spacing-sm;
}

.vip-badge {
  padding: 4rpx 16rpx;
  background: rgba(255, 255, 255, 0.2);
  border-radius: $radius-full;
  font-size: $font-xs;
  color: #FCD34D;
}

// Menu sections
.menu-section {
  margin: $spacing-md $spacing-lg;
  padding: $spacing-lg;
}

.order-shortcuts {
  display: flex;
  justify-content: space-around;
  margin-top: $spacing-md;
}

.shortcut-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: $spacing-xs;
}

.shortcut-icon { font-size: 40rpx; }
.shortcut-text { font-size: $font-xs; color: $text-secondary; }

.menu-list {}
.menu-item {
  display: flex;
  align-items: center;
  padding: $spacing-md 0;
  border-bottom: 1rpx solid $border-light;

  &:last-child { border-bottom: none; }
}

.menu-icon { font-size: 32rpx; margin-right: $spacing-md; }
.menu-label { flex: 1; font-size: $font-base; color: $text-primary; }
.menu-arrow { font-size: $font-xl; color: $text-muted; }

.app-info {
  text-align: center;
  padding: $spacing-xl 0;
}
</style>
