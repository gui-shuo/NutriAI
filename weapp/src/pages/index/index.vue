<template>
  <view class="home-page">
    <!-- Custom Nav Bar -->
    <view class="nav-bar" :style="{ paddingTop: statusBarHeight + 'px' }">
      <view class="nav-content">
        <view class="nav-left">
          <text class="nav-logo">🥗</text>
          <text class="nav-title">NutriAI</text>
        </view>
        <view class="nav-right" @tap="goAnnouncements">
          <text class="nav-bell">🔔</text>
          <view v-if="unreadCount > 0" class="badge-dot"></view>
        </view>
      </view>
    </view>

    <scroll-view scroll-y class="page-content" :style="{ paddingTop: (statusBarHeight + 44) + 'px' }">
      <!-- Hero Banner -->
      <view class="hero-banner">
        <view class="hero-content">
          <text class="hero-greeting">{{ greeting }}，{{ userStore.displayName }}</text>
          <text class="hero-subtitle">开始您的健康营养之旅</text>
        </view>
        <view class="hero-avatar" @tap="goProfile">
          <image v-if="userStore.avatarUrl" :src="userStore.avatarUrl" mode="aspectFill" class="avatar-img" />
          <text v-else class="avatar-placeholder">{{ userStore.displayName.charAt(0) }}</text>
        </view>
      </view>

      <!-- Quick Actions -->
      <view class="section">
        <view class="quick-actions">
          <view class="action-item" @tap="goPage('/pages/meals/index')">
            <view class="action-icon" style="background: #ECFDF5;">🍱</view>
            <text class="action-text">抗炎营养餐</text>
          </view>
          <view class="action-item" @tap="goPage('/pages/shop/index')">
            <view class="action-icon" style="background: #FEF3C7;">🛒</view>
            <text class="action-text">营养商城</text>
          </view>
          <view class="action-item" @tap="goPage('/pages/circle/index')">
            <view class="action-icon" style="background: #EDE9FE;">💬</view>
            <text class="action-text">营养圈</text>
          </view>
          <view class="action-item" @tap="goPage('/pages/cart/index')">
            <view class="action-icon" style="background: #FCE7F3;">🛍️</view>
            <text class="action-text">购物车</text>
          </view>
        </view>
      </view>

      <!-- Featured Meals -->
      <view class="section">
        <view class="section-header">
          <text class="section-title">本周推荐套餐</text>
          <text class="section-more" @tap="goPage('/pages/meals/index')">查看全部 ›</text>
        </view>
        <scroll-view scroll-x class="meal-scroll">
          <view class="meal-scroll-inner">
            <view
              v-for="meal in featuredMeals"
              :key="meal.id"
              class="meal-card"
              @tap="goPage(`/pages/meals/detail?id=${meal.id}`)"
            >
              <image :src="meal.imageUrl || '/static/images/meal-default.png'" mode="aspectFill" class="meal-img" />
              <view class="meal-info">
                <text class="meal-name text-ellipsis">{{ meal.name }}</text>
                <text class="meal-desc text-ellipsis">{{ meal.description }}</text>
                <view class="meal-bottom">
                  <text class="text-price">¥{{ meal.price }}</text>
                  <text class="meal-sales text-xs text-muted">已售 {{ meal.salesCount || 0 }}</text>
                </view>
              </view>
            </view>

            <view v-if="!featuredMeals.length" class="meal-card placeholder-card">
              <view class="flex-center" style="height: 200rpx;">
                <text class="text-muted">暂无推荐套餐</text>
              </view>
            </view>
          </view>
        </scroll-view>
      </view>

      <!-- Recommended Products -->
      <view class="section">
        <view class="section-header">
          <text class="section-title">热门营养品</text>
          <text class="section-more" @tap="goPage('/pages/shop/index')">查看全部 ›</text>
        </view>
        <view class="product-grid">
          <view
            v-for="product in recommendedProducts"
            :key="product.id"
            class="product-card"
            @tap="goPage(`/pages/shop/detail?id=${product.id}`)"
          >
            <image :src="product.imageUrl || '/static/images/product-default.png'" mode="aspectFill" class="product-img" />
            <view class="product-info">
              <text class="product-name text-line-2">{{ product.name }}</text>
              <view class="flex-between items-end">
                <text class="text-price text-md">¥{{ product.price }}</text>
                <text v-if="product.originalPrice" class="original-price">¥{{ product.originalPrice }}</text>
              </view>
            </view>
          </view>

          <view v-if="!recommendedProducts.length" class="empty-hint">
            <text class="text-muted">暂无推荐商品</text>
          </view>
        </view>
      </view>

      <!-- Health Tips -->
      <view class="section">
        <view class="section-header">
          <text class="section-title">健康小贴士</text>
        </view>
        <view class="tips-card">
          <text class="tip-icon">💡</text>
          <view class="tip-content">
            <text class="tip-title">抗炎饮食的关键</text>
            <text class="tip-text">多摄入深色蔬果、全谷物、omega-3脂肪酸丰富的食物，减少精加工食品和添加糖的摄入，有助于降低身体炎症水平。</text>
          </view>
        </view>
      </view>

      <view style="height: 40rpx;"></view>
    </scroll-view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { useUserStore } from '../../stores/user'
import { productApi, announcementApi } from '../../services/api'

const userStore = useUserStore()
const statusBarHeight = ref(0)
const featuredMeals = ref<any[]>([])
const recommendedProducts = ref<any[]>([])
const unreadCount = ref(0)

const greeting = computed(() => {
  const hour = new Date().getHours()
  if (hour < 6) return '夜深了'
  if (hour < 11) return '早上好'
  if (hour < 14) return '中午好'
  if (hour < 18) return '下午好'
  return '晚上好'
})

onMounted(() => {
  const sysInfo = uni.getSystemInfoSync()
  statusBarHeight.value = sysInfo.statusBarHeight || 20
})

onShow(() => {
  loadData()
})

async function loadData() {
  try {
    // Load recommended products
    const products: any = await productApi.getRecommended()
    recommendedProducts.value = (products || []).slice(0, 4)
  } catch {}

  try {
    // Load featured meals (using product API with category filter)
    const meals: any = await productApi.getList({ category: '抗炎套餐', size: 6 })
    featuredMeals.value = meals?.content || meals?.records || meals || []
  } catch {}

  if (userStore.isLoggedIn) {
    try {
      const count: any = await announcementApi.getUnreadCount()
      unreadCount.value = typeof count === 'number' ? count : (count?.count || 0)
    } catch {}
  }
}

function goPage(url: string) {
  if (!userStore.isLoggedIn && url !== '/pages/auth/login') {
    uni.navigateTo({ url: '/pages/auth/login' })
    return
  }
  if (url.startsWith('/pages/meals/index') || url.startsWith('/pages/shop/index') || url.startsWith('/pages/circle/index')) {
    uni.switchTab({ url: url.split('?')[0] })
  } else {
    uni.navigateTo({ url })
  }
}

function goProfile() {
  if (!userStore.isLoggedIn) {
    uni.navigateTo({ url: '/pages/auth/login' })
    return
  }
  uni.switchTab({ url: '/pages/profile/index' })
}

function goAnnouncements() {
  // Navigate to announcements if logged in
}
</script>

<style lang="scss" scoped>
.home-page {
  min-height: 100vh;
  background: $bg-page;
}

.nav-bar {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 100;
  background: $bg-card;
  box-shadow: $shadow-sm;
}

.nav-content {
  height: 88rpx;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 $spacing-lg;
}

.nav-left {
  display: flex;
  align-items: center;
  gap: $spacing-sm;
}

.nav-logo {
  font-size: 40rpx;
}

.nav-title {
  font-size: $font-lg;
  font-weight: 700;
  color: $primary;
}

.nav-right {
  position: relative;
  padding: $spacing-sm;
}

.nav-bell {
  font-size: 36rpx;
}

.badge-dot {
  position: absolute;
  top: 8rpx;
  right: 8rpx;
  width: 16rpx;
  height: 16rpx;
  border-radius: 50%;
  background: $error;
}

.page-content {
  height: 100vh;
}

// Hero Banner
.hero-banner {
  margin: $spacing-md $spacing-lg;
  padding: $spacing-xl;
  background: $gradient-hero;
  border-radius: $radius-xl;
  display: flex;
  align-items: center;
  justify-content: space-between;
  box-shadow: $shadow-primary;
}

.hero-content {
  flex: 1;
}

.hero-greeting {
  display: block;
  font-size: $font-xl;
  font-weight: 600;
  color: #fff;
}

.hero-subtitle {
  display: block;
  font-size: $font-sm;
  color: rgba(255, 255, 255, 0.8);
  margin-top: 8rpx;
}

.hero-avatar {
  width: 96rpx;
  height: 96rpx;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.2);
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  margin-left: $spacing-md;
  border: 3rpx solid rgba(255, 255, 255, 0.4);
}

.avatar-img {
  width: 100%;
  height: 100%;
}

.avatar-placeholder {
  font-size: $font-xl;
  color: #fff;
  font-weight: 600;
}

// Quick Actions
.quick-actions {
  display: flex;
  justify-content: space-around;
  padding: $spacing-md $spacing-lg;
}

.action-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: $spacing-sm;
}

.action-icon {
  width: 96rpx;
  height: 96rpx;
  border-radius: $radius-lg;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 40rpx;
}

.action-text {
  font-size: $font-xs;
  color: $text-secondary;
}

// Meal scroll
.meal-scroll {
  white-space: nowrap;
  padding-left: $spacing-lg;
}

.meal-scroll-inner {
  display: inline-flex;
  gap: $spacing-md;
  padding-right: $spacing-lg;
}

.meal-card {
  width: 300rpx;
  background: $bg-card;
  border-radius: $radius-lg;
  overflow: hidden;
  box-shadow: $shadow-sm;
  display: inline-block;
}

.placeholder-card {
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.meal-img {
  width: 300rpx;
  height: 200rpx;
  background: $bg-muted;
}

.meal-info {
  padding: $spacing-sm $spacing-md;
}

.meal-name {
  font-size: $font-base;
  font-weight: 500;
  color: $text-primary;
  display: block;
}

.meal-desc {
  font-size: $font-xs;
  color: $text-muted;
  margin-top: 4rpx;
  display: block;
}

.meal-bottom {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: $spacing-sm;
}

.meal-sales {
  flex-shrink: 0;
}

// Product grid
.product-grid {
  display: flex;
  flex-wrap: wrap;
  padding: 0 $spacing-lg;
  gap: $spacing-md;
}

.product-card {
  width: calc(50% - 12rpx);
  background: $bg-card;
  border-radius: $radius-lg;
  overflow: hidden;
  box-shadow: $shadow-sm;
}

.product-img {
  width: 100%;
  height: 280rpx;
  background: $bg-muted;
}

.product-info {
  padding: $spacing-sm $spacing-md $spacing-md;
}

.product-name {
  font-size: $font-sm;
  color: $text-primary;
  line-height: 1.4;
  min-height: 60rpx;
}

.original-price {
  font-size: $font-xs;
  color: $text-muted;
  text-decoration: line-through;
}

.empty-hint {
  width: 100%;
  text-align: center;
  padding: $spacing-xl 0;
}

// Tips
.tips-card {
  margin: 0 $spacing-lg;
  padding: $spacing-lg;
  background: $primary-50;
  border-radius: $radius-lg;
  display: flex;
  gap: $spacing-md;
  border: 1rpx solid $primary-100;
}

.tip-icon {
  font-size: 40rpx;
  flex-shrink: 0;
}

.tip-content {
  flex: 1;
}

.tip-title {
  display: block;
  font-size: $font-base;
  font-weight: 600;
  color: $primary-dark;
}

.tip-text {
  display: block;
  font-size: $font-sm;
  color: $text-secondary;
  margin-top: 8rpx;
  line-height: 1.6;
}
</style>
