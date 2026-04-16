<template>
  <view class="page">
    <!-- Custom Nav Bar -->
    <view class="nav-bar" :style="{ paddingTop: statusBarHeight + 'px' }">
      <view class="nav-bar-content">
        <text class="nav-title">营养餐</text>
        <view class="nav-search" @tap="toggleSearch">
          <text class="search-icon">🔍</text>
        </view>
      </view>
    </view>

    <!-- Search Overlay -->
    <view v-if="showSearch" class="search-overlay" :style="{ paddingTop: navBarTotalHeight + 'px' }">
      <view class="search-bar">
        <input
          class="search-input"
          v-model="searchKeyword"
          placeholder="搜索营养餐..."
          focus
          @confirm="doSearch"
        />
        <text class="search-cancel" @tap="toggleSearch">取消</text>
      </view>
    </view>

    <scroll-view class="content" scroll-y :style="{ paddingTop: navBarTotalHeight + 'px' }">
      <!-- Category Tabs -->
      <scroll-view class="category-scroll" scroll-x :show-scrollbar="false">
        <view class="category-list">
          <view
            v-for="cat in categories"
            :key="cat.value"
            class="category-tab"
            :class="{ active: activeCategory === cat.value }"
            @tap="selectCategory(cat.value)"
          >
            <text>{{ cat.label }}</text>
          </view>
        </view>
      </scroll-view>

      <!-- Loading -->
      <view v-if="loading" class="loading-box">
        <text class="loading-text">加载中...</text>
      </view>

      <!-- Meal List -->
      <view v-else class="meal-list">
        <view
          v-for="meal in filteredMeals"
          :key="meal.id"
          class="meal-card"
          @tap="goToDetail(meal.id)"
        >
          <image class="meal-img" :src="meal.imageUrl || '/static/images/meal-placeholder.png'" mode="aspectFill" />
          <view class="meal-info">
            <text class="meal-name">{{ meal.name }}</text>
            <text class="meal-desc">{{ meal.description || '新鲜现做，科学配比' }}</text>
            <view class="meal-tags">
              <text class="meal-tag" v-for="tag in (meal.tags || []).slice(0, 3)" :key="tag">{{ tag }}</text>
            </view>
            <view class="meal-row">
              <view class="meal-price-wrap">
                <text class="meal-price">¥{{ meal.price }}</text>
                <text class="meal-cal">{{ meal.calories }}kcal</text>
              </view>
              <view class="add-btn" @tap.stop="addToCart(meal)">
                <text class="add-icon">+</text>
              </view>
            </view>
          </view>
        </view>

        <view v-if="!filteredMeals.length" class="empty-box">
          <text class="empty-icon">🍱</text>
          <text class="empty-text">暂无营养餐</text>
        </view>
      </view>

      <!-- Bottom Spacer -->
      <view style="height: 160rpx;" />
    </scroll-view>

    <!-- Cart Bar -->
    <view v-if="cartTotal.count > 0" class="cart-bar">
      <view class="cart-left" @tap="goToCheckout">
        <view class="cart-badge-wrap">
          <text class="cart-icon-text">🛒</text>
          <view class="cart-badge">{{ cartTotal.count }}</view>
        </view>
        <text class="cart-total">¥{{ cartTotal.price }}</text>
      </view>
      <view class="cart-btn" @tap="goToCheckout">
        <text class="cart-btn-text">去结算</text>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { mealApi } from '@/services/api'

const statusBarHeight = ref(0)
const navBarTotalHeight = ref(0)
const loading = ref(false)
const showSearch = ref(false)
const searchKeyword = ref('')
const activeCategory = ref('all')
const meals = ref<any[]>([])

const CART_KEY = 'nutriai_meal_cart'

const categories = [
  { value: 'all', label: '全部' },
  { value: 'anti-inflammatory', label: '抗炎套餐' },
  { value: 'low-gi', label: '低GI餐' },
  { value: 'high-protein', label: '高蛋白餐' },
  { value: 'salad', label: '轻食沙拉' },
  { value: 'soup', label: '汤品' }
]

const mockMeals = [
  { id: 1, name: '地中海抗炎沙拉', price: '28.00', calories: 380, tags: ['抗炎', '低GI'], category: 'anti-inflammatory', description: '新鲜蔬菜搭配橄榄油，富含抗氧化成分', imageUrl: '' },
  { id: 2, name: '三文鱼藜麦碗', price: '42.00', calories: 520, tags: ['高蛋白', '抗炎'], category: 'high-protein', description: '挪威三文鱼搭配有机藜麦，Omega-3丰富', imageUrl: '' },
  { id: 3, name: '姜黄鸡胸套餐', price: '35.00', calories: 450, tags: ['抗炎', '增肌'], category: 'anti-inflammatory', description: '姜黄腌制鸡胸肉，搭配糙米和时蔬', imageUrl: '' },
  { id: 4, name: '牛油果虾仁沙拉', price: '38.00', calories: 420, tags: ['低GI', '高蛋白'], category: 'salad', description: '新鲜牛油果搭配大虾仁，健康轻食', imageUrl: '' },
  { id: 5, name: '番茄牛腩汤', price: '32.00', calories: 350, tags: ['抗炎', '暖胃'], category: 'soup', description: '慢炖番茄牛腩，营养滋补', imageUrl: '' },
  { id: 6, name: '低GI杂粮饭套餐', price: '26.00', calories: 400, tags: ['低GI', '高纤维'], category: 'low-gi', description: '五谷杂粮饭搭配清蒸时蔬', imageUrl: '' }
]

const filteredMeals = computed(() => {
  let list = meals.value
  if (activeCategory.value !== 'all') {
    list = list.filter(m => m.category === activeCategory.value || (m.tags || []).some((t: string) => {
      const catMap: Record<string, string[]> = {
        'anti-inflammatory': ['抗炎'],
        'low-gi': ['低GI'],
        'high-protein': ['高蛋白'],
        'salad': ['沙拉', '轻食'],
        'soup': ['汤', '汤品']
      }
      return (catMap[activeCategory.value] || []).some(k => t.includes(k))
    }))
  }
  if (searchKeyword.value.trim()) {
    const kw = searchKeyword.value.trim().toLowerCase()
    list = list.filter(m => m.name.toLowerCase().includes(kw))
  }
  return list
})

const cartTotal = ref({ count: 0, price: '0.00' })

function getCart(): any[] {
  return JSON.parse(uni.getStorageSync(CART_KEY) || '[]')
}

function saveCart(cart: any[]) {
  uni.setStorageSync(CART_KEY, JSON.stringify(cart))
}

function updateCartSummary() {
  const cart = getCart()
  const count = cart.reduce((s: number, c: any) => s + c.quantity, 0)
  const price = cart.reduce((s: number, c: any) => s + Number(c.price) * c.quantity, 0)
  cartTotal.value = { count, price: price.toFixed(2) }
}

function addToCart(meal: any) {
  const cart = getCart()
  const existing = cart.find((c: any) => c.id === meal.id)
  if (existing) {
    existing.quantity += 1
  } else {
    cart.push({ id: meal.id, name: meal.name, price: meal.price, imageUrl: meal.imageUrl, quantity: 1 })
  }
  saveCart(cart)
  updateCartSummary()
  uni.showToast({ title: '已加入购物车', icon: 'success' })
}

function selectCategory(val: string) {
  activeCategory.value = val
}

function toggleSearch() {
  showSearch.value = !showSearch.value
  if (!showSearch.value) searchKeyword.value = ''
}

function doSearch() {
  showSearch.value = false
}

function goToDetail(id: number | string) {
  uni.navigateTo({ url: `/pages/meal-order/detail?id=${id}` })
}

function goToCheckout() {
  uni.navigateTo({ url: '/pages/meal-order/checkout' })
}

async function fetchMeals() {
  loading.value = true
  try {
    const res = await mealApi.getList({ page: 0, size: 20 })
    const list = res.data?.content || res.data?.records || (Array.isArray(res.data) ? res.data : [])
    if (res.code === 200) {
      meals.value = list.map((m: any) => ({
        ...m,
        price: m.salePrice || m.sale_price || m.price,
        calories: m.nutritionInfo?.calories || m.nutrition_info?.calories || 0,
        description: m.brief || m.description,
        tags: m.tags || []
      }))
    }
  } catch {
    // silent fail — keep previous data
  } finally {
    loading.value = false
  }
}

onShow(() => {
  const windowInfo = uni.getWindowInfo()
  statusBarHeight.value = windowInfo.statusBarHeight || 0
  navBarTotalHeight.value = statusBarHeight.value + 44

  fetchMeals()
  updateCartSummary()
})
</script>

<style scoped lang="scss">
.page {
  min-height: 100vh;
  background: #F5F7FA;
}

/* Nav Bar */
.nav-bar {
  position: fixed;
  top: 0; left: 0; right: 0;
  z-index: 999;
  background: #fff;
  box-shadow: 0 2rpx 12rpx rgba(0,0,0,0.04);
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
  color: #10B981;
  letter-spacing: 2rpx;
}
.nav-search {
  width: 64rpx; height: 64rpx;
  display: flex; align-items: center; justify-content: center;
  background: #F0F4F8;
  border-radius: 50%;
}
.search-icon { font-size: 32rpx; }

/* Search Overlay */
.search-overlay {
  position: fixed;
  top: 0; left: 0; right: 0;
  z-index: 1000;
  background: #fff;
  padding-bottom: 16rpx;
  box-shadow: 0 4rpx 20rpx rgba(0,0,0,0.06);
}
.search-bar {
  display: flex;
  align-items: center;
  padding: 16rpx 32rpx;
  gap: 16rpx;
}
.search-input {
  flex: 1;
  height: 72rpx;
  background: #F0F4F8;
  border-radius: 48rpx;
  padding: 0 28rpx;
  font-size: 28rpx;
  color: #1A1A2E;
}
.search-cancel {
  font-size: 28rpx;
  color: #10B981;
  font-weight: 500;
}

.content {
  height: 100vh;
}

/* Category Tabs */
.category-scroll {
  white-space: nowrap;
  padding: 20rpx 0;
  background: #fff;
}
.category-list {
  display: inline-flex;
  padding: 0 32rpx;
  gap: 16rpx;
}
.category-tab {
  padding: 14rpx 32rpx;
  border-radius: 48rpx;
  font-size: 26rpx;
  color: #8896AB;
  background: #F0F4F8;
  font-weight: 500;
  flex-shrink: 0;

  &.active {
    background: linear-gradient(135deg, #10B981, #059669);
    color: #fff;
  }

  &:active { opacity: 0.7; }
}

/* Loading */
.loading-box {
  text-align: center;
  padding: 80rpx 0;
}
.loading-text {
  font-size: 28rpx;
  color: #8896AB;
}

/* Meal List */
.meal-list {
  padding: 24rpx 32rpx 0;
}
.meal-card {
  display: flex;
  background: #fff;
  border-radius: 24rpx;
  overflow: hidden;
  margin-bottom: 24rpx;
  box-shadow: 0 2rpx 12rpx rgba(0,0,0,0.04);

  &:active {
    transform: scale(0.99);
  }
}
.meal-img {
  width: 260rpx;
  height: 260rpx;
  flex-shrink: 0;
  background: #F0F4F8;
}
.meal-info {
  flex: 1;
  padding: 20rpx 24rpx;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  min-width: 0;
}
.meal-name {
  font-size: 30rpx;
  font-weight: 600;
  color: #1A1A2E;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.meal-desc {
  font-size: 24rpx;
  color: #8896AB;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  margin-top: 8rpx;
}
.meal-tags {
  display: flex;
  gap: 10rpx;
  margin-top: 12rpx;
  flex-wrap: wrap;
}
.meal-tag {
  font-size: 20rpx;
  color: #059669;
  background: #ECFDF5;
  padding: 4rpx 14rpx;
  border-radius: 48rpx;
  font-weight: 500;
}
.meal-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 12rpx;
}
.meal-price-wrap {
  display: flex;
  align-items: baseline;
  gap: 12rpx;
}
.meal-price {
  font-size: 32rpx;
  font-weight: 700;
  color: #EF4444;
}
.meal-cal {
  font-size: 22rpx;
  color: #8896AB;
}
.add-btn {
  width: 56rpx; height: 56rpx;
  background: linear-gradient(135deg, #10B981, #059669);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 4rpx 12rpx rgba(16,185,129,0.3);

  &:active { opacity: 0.8; }
}
.add-icon {
  font-size: 36rpx;
  font-weight: 700;
  color: #fff;
  line-height: 1;
}

/* Empty */
.empty-box {
  text-align: center;
  padding: 100rpx 0;
}
.empty-icon {
  display: block;
  font-size: 80rpx;
  margin-bottom: 16rpx;
}
.empty-text {
  font-size: 28rpx;
  color: #8896AB;
}

/* Cart Bar */
.cart-bar {
  position: fixed;
  bottom: 0; left: 0; right: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #fff;
  padding: 20rpx 32rpx;
  padding-bottom: calc(20rpx + env(safe-area-inset-bottom));
  box-shadow: 0 -4rpx 20rpx rgba(0,0,0,0.06);
  z-index: 999;
}
.cart-left {
  display: flex;
  align-items: center;
  gap: 20rpx;
}
.cart-badge-wrap {
  position: relative;
}
.cart-icon-text {
  font-size: 48rpx;
}
.cart-badge {
  position: absolute;
  top: -8rpx; right: -12rpx;
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
.cart-total {
  font-size: 36rpx;
  font-weight: 700;
  color: #1A1A2E;
}
.cart-btn {
  background: linear-gradient(135deg, #10B981, #059669);
  padding: 18rpx 48rpx;
  border-radius: 48rpx;
  box-shadow: 0 4rpx 16rpx rgba(16,185,129,0.3);

  &:active { opacity: 0.8; }
}
.cart-btn-text {
  font-size: 30rpx;
  font-weight: 600;
  color: #fff;
}
</style>
