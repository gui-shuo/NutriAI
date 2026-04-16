<template>
  <view class="page">
    <!-- Custom Navigation Bar -->
    <view class="nav-bar" :style="{ paddingTop: statusBarHeight + 'px' }">
      <view class="nav-bar-content">
        <view class="nav-left">
          <image class="nav-logo" src="/static/images/logo.png" mode="aspectFit" />
          <text class="nav-brand">NutriAI</text>
        </view>
        <view class="nav-right">
          <view class="nav-search" @tap="goTo('/pages/recipes/index')">
            <text class="search-icon">🔍</text>
          </view>
          <view class="nav-bell" @tap="goTo('/pages/announcements/index')">
            <text class="bell-icon">🔔</text>
            <view v-if="unreadCount > 0" class="unread-dot" />
          </view>
        </view>
      </view>
    </view>

    <scroll-view class="content" scroll-y :style="{ paddingTop: navBarTotalHeight + 'px' }">
      <!-- Hero Banner -->
      <view class="hero-section">
        <view class="hero-card" @tap="goToMeals">
          <view class="hero-text-area">
            <view class="hero-label">
              <text class="hero-label-text">🔥 限时推荐</text>
            </view>
            <text class="hero-title">科学抗炎 · 营养定制</text>
            <text class="hero-desc">专业营养师配比，每日新鲜现做</text>
            <view class="hero-btn">
              <text class="hero-btn-text">立即点餐</text>
              <text class="hero-btn-arrow">→</text>
            </view>
          </view>
          <view class="hero-visual">
            <text class="hero-emoji">🥗</text>
          </view>
        </view>
      </view>

      <!-- Quick Service Entries -->
      <view class="service-section">
        <view class="service-grid">
          <view class="service-item" v-for="f in features" :key="f.label" @tap="handleFeatureTap(f)">
            <view class="service-icon-wrap" :style="{ background: f.bg }">
              <text class="service-icon">{{ f.icon }}</text>
            </view>
            <text class="service-label">{{ f.label }}</text>
          </view>
        </view>
      </view>

      <!-- Member Benefits Bar -->
      <view class="member-bar" @tap="goTo('/pages/member/index')">
        <view class="member-left">
          <text class="member-icon">👑</text>
          <view class="member-info">
            <text class="member-title" v-if="userStore.isLoggedIn">{{ userStore.userInfo?.nickname || '会员用户' }}</text>
            <text class="member-title" v-else>开通会员享专属优惠</text>
            <text class="member-desc">AI配额 · 专属折扣 · 积分翻倍</text>
          </view>
        </view>
        <view class="member-btn">
          <text class="member-btn-text">{{ userStore.isLoggedIn ? '查看权益' : '立即开通' }}</text>
        </view>
      </view>

      <!-- Today's Recommended Meals -->
      <view class="section">
        <view class="section-hd">
          <view class="section-hd-left">
            <view class="section-bar" />
            <text class="section-name">今日推荐</text>
          </view>
          <view class="section-link" @tap="goToMeals">
            <text class="section-link-text">全部</text>
            <text class="section-link-arrow">›</text>
          </view>
        </view>
        <scroll-view class="meal-scroll" scroll-x :show-scrollbar="false">
          <view class="meal-card" v-for="meal in featuredMeals" :key="meal.id" @tap="goToMealDetail(meal.id)">
            <view class="meal-img-wrap">
              <image class="meal-img" :src="meal.imageUrl || '/static/images/meal-placeholder.png'" mode="aspectFill" />
              <view class="meal-cal-badge" v-if="meal.calories">
                <text class="meal-cal-text">{{ meal.calories }}kcal</text>
              </view>
            </view>
            <view class="meal-info">
              <text class="meal-name">{{ meal.name }}</text>
              <view class="meal-tag-row">
                <text class="meal-tag" v-for="tag in (meal.tags || []).slice(0, 2)" :key="tag">{{ tag }}</text>
              </view>
              <view class="meal-price-row">
                <text class="meal-price"><text class="yen">¥</text>{{ meal.price }}</text>
                <view class="meal-add-btn">
                  <text class="meal-add-icon">+</text>
                </view>
              </view>
            </view>
          </view>
        </scroll-view>
      </view>

      <!-- Featured Products -->
      <view class="section">
        <view class="section-hd">
          <view class="section-hd-left">
            <view class="section-bar" />
            <text class="section-name">营养好物</text>
          </view>
          <view class="section-link" @tap="goToShop">
            <text class="section-link-text">进入商城</text>
            <text class="section-link-arrow">›</text>
          </view>
        </view>
        <view class="product-grid">
          <view class="prod-card" v-for="prod in featuredProducts" :key="prod.id" @tap="goToProductDetail(prod.id)">
            <view class="prod-img-wrap">
              <image class="prod-img" :src="prod.imageUrl || prod.image || '/static/images/product-placeholder.png'" mode="aspectFill" />
            </view>
            <view class="prod-info">
              <text class="prod-name">{{ prod.name }}</text>
              <view class="prod-price-row">
                <text class="prod-price"><text class="yen">¥</text>{{ formatPrice(prod.salePrice || prod.price) }}</text>
                <view class="prod-buy-btn" @tap.stop="goToProductDetail(prod.id)">
                  <text class="prod-buy-text">购买</text>
                </view>
              </view>
            </view>
          </view>
        </view>
      </view>

      <!-- Footer -->
      <view class="app-footer">
        <view class="footer-line" />
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
const hasLoaded = ref(false)

const features = [
  { icon: '🤖', label: 'AI营养师', path: '/pages/ai-chat/index', isTab: true, bg: 'linear-gradient(135deg, #ECFDF5, #D1FAE5)' },
  { icon: '📸', label: '食物识别', path: '/pages/food-recognition/index', bg: 'linear-gradient(135deg, #EFF6FF, #DBEAFE)' },
  { icon: '📋', label: '饮食计划', path: '/pages/diet-plan/index', bg: 'linear-gradient(135deg, #F5F3FF, #EDE9FE)' },
  { icon: '📝', label: '饮食记录', path: '/pages/food-records/index', bg: 'linear-gradient(135deg, #FFFBEB, #FEF3C7)' },
  { icon: '👨‍⚕️', label: '营养咨询', path: '/pages/consultation/index', bg: 'linear-gradient(135deg, #FFF1F2, #FFE4E6)' },
  { icon: '🍳', label: '食谱库', path: '/pages/recipes/index', bg: 'linear-gradient(135deg, #FFF7ED, #FFEDD5)' },
  { icon: '💊', label: '营养商城', path: '/pages/product-shop/index', isTab: true, bg: 'linear-gradient(135deg, #F0FDF4, #DCFCE7)' },
  { icon: '👤', label: '个人中心', path: '/pages/profile/index', isTab: true, bg: 'linear-gradient(135deg, #F8FAFC, #F1F5F9)' }
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
  if (!hasLoaded.value) {
    hasLoaded.value = true
    // Defer heavy API calls to avoid blocking WeChat miniprogram JS thread on launch
    setTimeout(() => {
      fetchFeaturedMeals()
      fetchFeaturedProducts()
      fetchUnreadCount()
    }, 500)
  } else {
    // On subsequent visits only refresh lightweight data
    fetchUnreadCount()
  }
})
</script>

<style scoped lang="scss">
.page {
  min-height: 100vh;
  background: #F5F7FA;
}

/* ---- Navigation Bar ---- */
.nav-bar {
  position: fixed;
  top: 0; left: 0; right: 0;
  z-index: 999;
  background: #fff;
}
.nav-bar-content {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 88rpx;
  padding: 0 32rpx;
}
.nav-left {
  display: flex;
  align-items: center;
}
.nav-logo {
  width: 52rpx;
  height: 52rpx;
  margin-right: 12rpx;
  border-radius: 12rpx;
}
.nav-brand {
  font-size: 36rpx;
  font-weight: 800;
  color: #10B981;
  letter-spacing: 1rpx;
}
.nav-right {
  display: flex;
  align-items: center;
  gap: 20rpx;
}
.nav-search, .nav-bell {
  width: 64rpx;
  height: 64rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #F5F7FA;
  border-radius: 50%;
  position: relative;
}
.search-icon, .bell-icon { font-size: 32rpx; }
.unread-dot {
  position: absolute;
  top: 10rpx;
  right: 10rpx;
  width: 16rpx;
  height: 16rpx;
  background: #EF4444;
  border-radius: 50%;
  border: 2rpx solid #fff;
}

.content {
  height: 100vh;
}

/* ---- Hero Section ---- */
.hero-section {
  padding: 16rpx 32rpx 0;
}
.hero-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: linear-gradient(135deg, #10B981, #059669);
  border-radius: 28rpx;
  padding: 36rpx 32rpx;
  position: relative;
  overflow: hidden;
}
.hero-text-area {
  flex: 1;
  z-index: 1;
}
.hero-label {
  display: inline-flex;
  margin-bottom: 16rpx;
}
.hero-label-text {
  font-size: 22rpx;
  color: #fff;
  background: rgba(255, 255, 255, 0.2);
  padding: 6rpx 20rpx;
  border-radius: 100rpx;
  font-weight: 500;
}
.hero-title {
  display: block;
  font-size: 38rpx;
  font-weight: 800;
  color: #fff;
  letter-spacing: 2rpx;
  margin-bottom: 8rpx;
}
.hero-desc {
  display: block;
  font-size: 24rpx;
  color: rgba(255, 255, 255, 0.8);
  margin-bottom: 24rpx;
}
.hero-btn {
  display: inline-flex;
  align-items: center;
  gap: 8rpx;
  background: #fff;
  padding: 16rpx 36rpx;
  border-radius: 100rpx;
}
.hero-btn-text {
  font-size: 26rpx;
  font-weight: 600;
  color: #059669;
}
.hero-btn-arrow {
  font-size: 26rpx;
  color: #059669;
}
.hero-visual {
  z-index: 1;
}
.hero-emoji {
  font-size: 100rpx;
  opacity: 0.9;
}

/* ---- Service Grid ---- */
.service-section {
  padding: 24rpx 32rpx 0;
}
.service-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20rpx 0;
  background: #fff;
  border-radius: 24rpx;
  padding: 28rpx 16rpx;
  box-shadow: 0 2rpx 12rpx rgba(0, 0, 0, 0.04);
}
.service-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10rpx;
  &:active { opacity: 0.7; }
}
.service-icon-wrap {
  width: 88rpx;
  height: 88rpx;
  border-radius: 22rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}
.service-icon {
  font-size: 40rpx;
}
.service-label {
  font-size: 22rpx;
  font-weight: 500;
  color: #1A1A2E;
  text-align: center;
}

/* ---- Member Bar ---- */
.member-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: 24rpx 32rpx 0;
  padding: 24rpx 28rpx;
  background: linear-gradient(135deg, #FEF3C7, #FDE68A);
  border-radius: 20rpx;
}
.member-left {
  display: flex;
  align-items: center;
  gap: 16rpx;
  flex: 1;
  min-width: 0;
}
.member-icon {
  font-size: 40rpx;
}
.member-info {
  display: flex;
  flex-direction: column;
  flex: 1;
  min-width: 0;
}
.member-title {
  font-size: 26rpx;
  font-weight: 600;
  color: #92400E;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.member-desc {
  font-size: 20rpx;
  color: #B45309;
  margin-top: 4rpx;
}
.member-btn {
  background: #92400E;
  padding: 10rpx 24rpx;
  border-radius: 100rpx;
  flex-shrink: 0;
}
.member-btn-text {
  font-size: 22rpx;
  font-weight: 600;
  color: #FEF3C7;
}

/* ---- Section Header ---- */
.section {
  margin-top: 32rpx;
}
.section-hd {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 32rpx;
  margin-bottom: 20rpx;
}
.section-hd-left {
  display: flex;
  align-items: center;
}
.section-bar {
  width: 6rpx;
  height: 30rpx;
  background: linear-gradient(180deg, #10B981, #059669);
  border-radius: 3rpx;
  margin-right: 12rpx;
}
.section-name {
  font-size: 32rpx;
  font-weight: 700;
  color: #1A1A2E;
}
.section-link {
  display: flex;
  align-items: center;
  gap: 4rpx;
}
.section-link-text {
  font-size: 24rpx;
  color: #8896AB;
}
.section-link-arrow {
  font-size: 28rpx;
  color: #8896AB;
}

/* ---- Meal Cards ---- */
.meal-scroll {
  white-space: nowrap;
  padding-left: 32rpx;
}
.meal-card {
  display: inline-flex;
  flex-direction: column;
  width: 300rpx;
  margin-right: 20rpx;
  background: #fff;
  border-radius: 24rpx;
  overflow: hidden;
  box-shadow: 0 2rpx 12rpx rgba(0, 0, 0, 0.04);
  vertical-align: top;
  &:active { transform: scale(0.97); }
}
.meal-img-wrap {
  position: relative;
  width: 300rpx;
  height: 200rpx;
}
.meal-img {
  width: 300rpx;
  height: 200rpx;
  background: #F0F4F8;
}
.meal-cal-badge {
  position: absolute;
  bottom: 12rpx;
  right: 12rpx;
  background: rgba(0, 0, 0, 0.5);
  padding: 4rpx 14rpx;
  border-radius: 100rpx;
}
.meal-cal-text {
  font-size: 20rpx;
  color: #fff;
  font-weight: 500;
}
.meal-info {
  padding: 16rpx 20rpx 20rpx;
}
.meal-name {
  font-size: 28rpx;
  font-weight: 600;
  color: #1A1A2E;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  display: block;
  margin-bottom: 8rpx;
}
.meal-tag-row {
  display: flex;
  gap: 8rpx;
  margin-bottom: 12rpx;
}
.meal-tag {
  font-size: 20rpx;
  color: #059669;
  background: #ECFDF5;
  padding: 4rpx 12rpx;
  border-radius: 6rpx;
  font-weight: 500;
}
.meal-price-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.meal-price {
  font-size: 32rpx;
  font-weight: 700;
  color: #EF4444;
}
.yen {
  font-size: 22rpx;
}
.meal-add-btn {
  width: 48rpx;
  height: 48rpx;
  background: linear-gradient(135deg, #10B981, #059669);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 4rpx 12rpx rgba(16, 185, 129, 0.3);
}
.meal-add-icon {
  font-size: 32rpx;
  font-weight: 700;
  color: #fff;
  line-height: 1;
}

/* ---- Product Grid ---- */
.product-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16rpx;
  padding: 0 32rpx;
}
.prod-card {
  background: #fff;
  border-radius: 20rpx;
  overflow: hidden;
  box-shadow: 0 2rpx 12rpx rgba(0, 0, 0, 0.04);
  &:active { transform: scale(0.97); }
}
.prod-img-wrap {
  width: 100%;
  height: 200rpx;
}
.prod-img {
  width: 100%;
  height: 200rpx;
  background: #F0F4F8;
}
.prod-info {
  padding: 12rpx 16rpx 16rpx;
}
.prod-name {
  font-size: 24rpx;
  font-weight: 500;
  color: #1A1A2E;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  display: block;
  margin-bottom: 8rpx;
}
.prod-price-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.prod-price {
  font-size: 28rpx;
  font-weight: 700;
  color: #EF4444;
}
.prod-buy-btn {
  background: #ECFDF5;
  padding: 6rpx 16rpx;
  border-radius: 8rpx;
}
.prod-buy-text {
  font-size: 20rpx;
  color: #059669;
  font-weight: 600;
}

/* ---- Footer ---- */
.app-footer {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 48rpx 32rpx 140rpx;
}
.footer-line {
  width: 64rpx;
  height: 4rpx;
  background: #E5E7EB;
  border-radius: 2rpx;
  margin-bottom: 24rpx;
}
.footer-text {
  font-size: 22rpx;
  color: #8896AB;
  margin-bottom: 8rpx;
}
.footer-copy {
  font-size: 20rpx;
  color: #C0C8D4;
}
</style>
