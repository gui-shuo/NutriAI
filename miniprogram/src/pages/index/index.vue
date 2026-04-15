<template>
  <view class="page">
    <!-- Custom Navigation Bar -->
    <view class="nav-bar" :style="{ paddingTop: statusBarHeight + 'px' }">
      <view class="nav-bar-content">
        <text class="nav-title">NutriAI</text>
        <view class="nav-bell" @tap="goTo('/pages/announcements/index')">
          <text class="bell-icon">🔔</text>
          <view v-if="unreadCount > 0" class="unread-badge">{{ unreadCount > 99 ? '99+' : unreadCount }}</view>
        </view>
      </view>
    </view>

    <scroll-view class="content" scroll-y :style="{ paddingTop: navBarTotalHeight + 'px' }">
      <!-- Hero Banner -->
      <view class="hero-banner">
        <view class="hero-bg" />
        <view class="hero-inner">
          <text class="hero-title">科学抗炎 · 营养定制</text>
          <text class="hero-desc">专注抗炎营养餐研发，科学配比，新鲜现做</text>
          <view class="hero-btn" @tap="goToMeals">
            <text class="hero-btn-text">立即点餐</text>
          </view>
        </view>
      </view>

      <!-- Today's Recommended Meals -->
      <view class="section">
        <view class="section-header">
          <text class="section-title">🍱 今日推荐</text>
          <text class="section-more" @tap="goToMeals">更多 ›</text>
        </view>
        <scroll-view class="meal-scroll" scroll-x :show-scrollbar="false">
          <view class="meal-card" v-for="meal in featuredMeals" :key="meal.id" @tap="goToMealDetail(meal.id)">
            <image class="meal-img" :src="meal.imageUrl || '/static/images/meal-placeholder.png'" mode="aspectFill" />
            <view class="meal-tags">
              <text class="meal-tag" v-for="tag in (meal.tags || []).slice(0, 2)" :key="tag">{{ tag }}</text>
            </view>
            <text class="meal-name">{{ meal.name }}</text>
            <view class="meal-bottom">
              <text class="meal-price">¥{{ meal.price }}</text>
              <text class="meal-cal">{{ meal.calories }}kcal</text>
            </view>
          </view>
        </scroll-view>
      </view>

      <!-- Featured Products -->
      <view class="section">
        <view class="section-header">
          <text class="section-title">🛍 营养好物</text>
          <text class="section-more" @tap="goToShop">更多 ›</text>
        </view>
        <scroll-view class="product-scroll" scroll-x :show-scrollbar="false">
          <view class="prod-card" v-for="prod in featuredProducts" :key="prod.id" @tap="goToProductDetail(prod.id)">
            <image class="prod-img" :src="prod.imageUrl || prod.image || '/static/images/product-placeholder.png'" mode="aspectFill" />
            <text class="prod-name">{{ prod.name }}</text>
            <text class="prod-price">¥{{ formatPrice(prod.salePrice || prod.price) }}</text>
          </view>
        </scroll-view>
      </view>

      <!-- Quick Features Grid -->
      <view class="section">
        <view class="section-header">
          <text class="section-title">✨ 更多服务</text>
        </view>
        <view class="feature-grid">
          <view class="feature-item" v-for="f in features" :key="f.label" @tap="handleFeatureTap(f)">
            <view class="feature-icon-box" :style="{ background: f.bg }">
              <text class="feature-icon">{{ f.icon }}</text>
            </view>
            <text class="feature-label">{{ f.label }}</text>
          </view>
        </view>
      </view>

      <!-- Footer -->
      <view class="app-footer">
        <text class="footer-text">{{ appStore.config.siteDescription || '智能营养，科学饮食' }}</text>
        <text class="footer-copy">{{ appStore.config.copyright || '© NutriAI' }}</text>
      </view>
    </scroll-view>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { useUserStore } from '@/stores/user'
import { useAppStore } from '@/stores/app'
import { mealApi, productApi, announcementApi } from '@/services/api'

const userStore = useUserStore()
const appStore = useAppStore()

const statusBarHeight = ref(0)
const navBarTotalHeight = ref(0)
const unreadCount = ref(0)

const mockMeals = [
  { id: 1, name: '地中海抗炎沙拉', price: '28.00', calories: 380, tags: ['抗炎', '低GI'], imageUrl: '' },
  { id: 2, name: '三文鱼藜麦碗', price: '42.00', calories: 520, tags: ['高蛋白', '抗炎'], imageUrl: '' },
  { id: 3, name: '姜黄鸡胸套餐', price: '35.00', calories: 450, tags: ['抗炎', '增肌'], imageUrl: '' }
]

const mockProducts = [
  { id: 1, name: '深海鱼油 Omega-3', price: '128.00', imageUrl: '' },
  { id: 2, name: '有机姜黄粉', price: '68.00', imageUrl: '' },
  { id: 3, name: '益生菌冻干粉', price: '98.00', imageUrl: '' }
]

const featuredMeals = ref<any[]>(mockMeals)
const featuredProducts = ref<any[]>(mockProducts)

const features = [
  { icon: '🤖', label: 'AI营养师', path: '/pages/ai-chat/index', isTab: true, bg: 'rgba(16,185,129,0.1)' },
  { icon: '📸', label: '食物识别', path: '/pages/food-recognition/index', bg: 'rgba(59,130,246,0.1)' },
  { icon: '📋', label: '饮食计划', path: '/pages/diet-plan/index', bg: 'rgba(139,92,246,0.1)' },
  { icon: '📝', label: '饮食记录', path: '/pages/food-records/index', bg: 'rgba(245,158,11,0.1)' },
  { icon: '👨‍⚕️', label: '营养咨询', path: '/pages/consultation/index', bg: 'rgba(236,72,153,0.1)' },
  { icon: '🍳', label: '食谱库', path: '/pages/recipes/index', bg: 'rgba(244,63,94,0.1)' }
]

async function fetchFeaturedMeals() {
  try {
    const res = await mealApi.getRecommended()
    if (res.code === 200 && res.data?.length) {
      featuredMeals.value = res.data.map((m: any) => ({
        ...m,
        price: m.salePrice || m.price || m.sale_price,
        calories: m.nutritionInfo?.calories || m.nutrition_info?.calories || 0,
        tags: m.tags || []
      }))
    }
  } catch {
    // keep mock data
  }
}

async function fetchFeaturedProducts() {
  try {
    const res = await productApi.getRecommended()
    if (res.code === 200 && res.data?.length) {
      featuredProducts.value = res.data
    }
  } catch {
    // keep mock data
  }
}

async function fetchUnreadCount() {
  if (!userStore.isLoggedIn) return
  try {
    const res = await announcementApi.getUnreadCount()
    if (res.code === 200) {
      unreadCount.value = res.data?.count || 0
    }
  } catch {}
}

function formatPrice(val: any) {
  const n = Number(val)
  return isNaN(n) ? val : n.toFixed(2)
}

function goToMeals() {
  uni.switchTab({ url: '/pages/meal-order/index' })
}

function goToMealDetail(id: number | string) {
  uni.navigateTo({ url: `/pages/meal-order/detail?id=${id}` })
}

function goToShop() {
  uni.switchTab({ url: '/pages/product-shop/index' })
}

function goToProductDetail(id: number | string) {
  uni.navigateTo({ url: `/pages/product-shop/detail?id=${id}` })
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

onShow(() => {
  const windowInfo = uni.getWindowInfo()
  statusBarHeight.value = windowInfo.statusBarHeight || 0
  navBarTotalHeight.value = statusBarHeight.value + 44

  userStore.restore()
  appStore.fetchConfig()
  fetchUnreadCount()
  fetchFeaturedMeals()
  fetchFeaturedProducts()
})
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
  position: relative;
}
.bell-icon { font-size: 36rpx; }
.unread-badge {
  position: absolute;
  top: -6rpx; right: -10rpx;
  min-width: 32rpx; height: 32rpx;
  line-height: 32rpx;
  padding: 0 8rpx;
  background: #EF4444;
  color: #fff;
  font-size: 20rpx;
  border-radius: 16rpx;
  text-align: center;
  font-weight: bold;
}

.content {
  height: 100vh;
  padding-bottom: 48rpx;
}

/* Hero Banner */
.hero-banner {
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
  flex-direction: column;
  align-items: center;
  padding: 56rpx 40rpx 64rpx;
  text-align: center;
}
.hero-title {
  font-size: 44rpx;
  font-weight: 700;
  color: #fff;
  letter-spacing: 4rpx;
  margin-bottom: 16rpx;
  font-family: 'Calistoga', Georgia, 'PingFang SC', serif;
}
.hero-desc {
  font-size: 26rpx;
  color: rgba(255,255,255,0.85);
  margin-bottom: 36rpx;
  line-height: 1.6;
}
.hero-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: #fff;
  padding: 18rpx 56rpx;
  border-radius: $radius-full;
  box-shadow: $shadow-md;
}
.hero-btn-text {
  font-size: 30rpx;
  font-weight: 600;
  color: $accent;
}

/* Section */
.section {
  margin-bottom: 36rpx;
}
.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 32rpx;
  margin-bottom: 20rpx;
}
.section-title {
  font-size: 32rpx;
  font-weight: 600;
  color: $foreground;
}
.section-more {
  font-size: 26rpx;
  color: $accent;
  font-weight: 500;
}

/* Meal Scroll */
.meal-scroll {
  white-space: nowrap;
  padding-left: 32rpx;
}
.meal-card {
  display: inline-flex;
  flex-direction: column;
  width: 320rpx;
  margin-right: 24rpx;
  background: $card;
  border-radius: $radius-xl;
  overflow: hidden;
  border: 1rpx solid $border;
  box-shadow: $shadow-sm;
  vertical-align: top;

  &:active {
    transform: scale(0.98);
  }
}
.meal-img {
  width: 320rpx;
  height: 200rpx;
  background: $muted;
}
.meal-tags {
  display: flex;
  gap: 10rpx;
  padding: 16rpx 20rpx 0;
}
.meal-tag {
  font-size: 20rpx;
  color: $accent;
  background: rgba(16,185,129,0.1);
  padding: 4rpx 14rpx;
  border-radius: $radius-full;
  font-weight: 500;
}
.meal-name {
  font-size: 28rpx;
  font-weight: 600;
  color: $foreground;
  padding: 10rpx 20rpx 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.meal-bottom {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10rpx 20rpx 20rpx;
}
.meal-price {
  font-size: 30rpx;
  font-weight: 700;
  color: #EF4444;
}
.meal-cal {
  font-size: 22rpx;
  color: $muted-foreground;
}

/* Product Scroll */
.product-scroll {
  white-space: nowrap;
  padding-left: 32rpx;
}
.prod-card {
  display: inline-flex;
  flex-direction: column;
  width: 260rpx;
  margin-right: 24rpx;
  background: $card;
  border-radius: $radius-xl;
  overflow: hidden;
  border: 1rpx solid $border;
  box-shadow: $shadow-sm;
  vertical-align: top;

  &:active {
    transform: scale(0.98);
  }
}
.prod-img {
  width: 260rpx;
  height: 200rpx;
  background: $muted;
}
.prod-name {
  font-size: 26rpx;
  font-weight: 500;
  color: $foreground;
  padding: 16rpx 16rpx 6rpx;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.prod-price {
  font-size: 28rpx;
  font-weight: 700;
  color: #EF4444;
  padding: 0 16rpx 20rpx;
}

/* Feature Grid */
.feature-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 24rpx;
  padding: 0 32rpx;
}
.feature-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12rpx;

  &:active { opacity: 0.7; }
}
.feature-icon-box {
  width: 96rpx; height: 96rpx;
  border-radius: $radius-xl;
  display: flex; align-items: center; justify-content: center;
}
.feature-icon { font-size: 44rpx; }
.feature-label {
  font-size: 24rpx;
  font-weight: 500;
  color: $foreground;
  text-align: center;
}

/* Footer */
.app-footer {
  text-align: center;
  padding: 48rpx 32rpx 120rpx;
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
