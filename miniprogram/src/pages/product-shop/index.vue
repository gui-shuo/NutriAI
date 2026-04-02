<template>
  <view class="shop-page">
    <!-- Disclaimer -->
    <view class="disclaimer-tip" v-if="showDisclaimer">
      <text>📋 商品信息来源于第三方，仅供参考。营养补充剂不能替代正常饮食和药物治疗。</text>
      <text class="dismiss" @tap="showDisclaimer = false">✕</text>
    </view>

    <!-- Search Bar -->
    <view class="search-bar">
      <view class="search-inner">
        <text class="search-icon">🔍</text>
        <input
          class="search-input"
          v-model="keyword"
          placeholder="搜索营养食品..."
          confirm-type="search"
          @confirm="handleSearch"
        />
        <text class="clear-btn" v-if="keyword" @tap="clearSearch">✕</text>
      </view>
    </view>

    <!-- Category Tabs -->
    <scroll-view class="category-tabs" scroll-x :show-scrollbar="false">
      <view
        class="cat-tab"
        :class="{ active: currentCategory === '' }"
        @tap="switchCategory('')"
      >全部</view>
      <view
        v-for="cat in categories"
        :key="cat.id || cat.name"
        class="cat-tab"
        :class="{ active: currentCategory === (cat.id || cat.name) }"
        @tap="switchCategory(cat.id || cat.name)"
      >
        {{ cat.name }}
      </view>
    </scroll-view>

    <!-- Recommended Section -->
    <view class="recommend-section" v-if="!keyword && !currentCategory && recommended.length > 0">
      <view class="section-header">
        <text class="section-title">🌟 为你推荐</text>
      </view>
      <scroll-view class="recommend-scroll" scroll-x :show-scrollbar="false">
        <view
          class="recommend-card"
          v-for="item in recommended"
          :key="item.id"
          @tap="goDetail(item.id)"
        >
          <image class="recommend-img" :src="item.imageUrl || item.image" mode="aspectFill" />
          <text class="recommend-name">{{ item.name }}</text>
          <text class="recommend-price">¥{{ formatPrice(item.salePrice || item.price) }}</text>
        </view>
      </scroll-view>
    </view>

    <!-- Product Grid -->
    <view class="product-grid" v-if="products.length > 0">
      <view
        class="product-card"
        v-for="item in products"
        :key="item.id"
        @tap="goDetail(item.id)"
      >
        <image class="product-img" :src="item.imageUrl || item.image" mode="aspectFill" />
        <view class="product-info">
          <text class="product-name">{{ item.name }}</text>
          <view class="price-row">
            <text class="current-price">¥{{ formatPrice(item.salePrice || item.price) }}</text>
            <text class="original-price" v-if="item.originalPrice && item.originalPrice > (item.salePrice || item.price)">
              ¥{{ formatPrice(item.originalPrice) }}
            </text>
          </view>
          <text class="sales-count">已售 {{ item.salesCount || 0 }}件</text>
        </view>
      </view>
    </view>

    <!-- Empty State -->
    <view class="empty-state" v-else-if="!loading">
      <text class="empty-icon">🛒</text>
      <text class="empty-text">{{ keyword ? '未找到相关商品' : '暂无商品' }}</text>
    </view>

    <!-- Loading & No More -->
    <view class="loading-more" v-if="loading">
      <text>加载中...</text>
    </view>
    <view class="no-more" v-if="!loading && noMore && products.length > 0">
      <text>— 没有更多了 —</text>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { onPullDownRefresh, onReachBottom, onShow } from '@dcloudio/uni-app'
import { productApi } from '@/services/api'

const keyword = ref('')
const showDisclaimer = ref(true)
const currentCategory = ref('')
const categories = ref<any[]>([])
const products = ref<any[]>([])
const recommended = ref<any[]>([])
const loading = ref(false)
const noMore = ref(false)
const page = ref(0)
const pageSize = 10

const categoryNameMap: Record<string, string> = {
  EQUIPMENT: '健身器材',
  HEALTH_FOOD: '营养食品',
  ORGANIC: '有机食品',
  PROTEIN: '蛋白质',
  SUPPLEMENT: '营养补剂',
  VITAMIN: '维生素'
}

function formatPrice(val: number | undefined): string {
  return (val || 0).toFixed(2)
}

async function loadCategories() {
  try {
    const res = await productApi.getCategories()
    const raw = res.data || []
    categories.value = raw.map((c: any) => {
      if (typeof c === 'string') {
        return { id: c, name: categoryNameMap[c] || c }
      }
      return c
    })
  } catch {}
}

async function loadRecommended() {
  try {
    const res = await productApi.getRecommended()
    recommended.value = res.data?.content || res.data?.records || res.data?.list || res.data || []
  } catch {}
}

async function loadProducts(isRefresh = false) {
  if (loading.value) return
  loading.value = true
  try {
    const params: any = { page: page.value, size: pageSize }
    if (currentCategory.value) params.category = currentCategory.value
    if (keyword.value.trim()) params.keyword = keyword.value.trim()

    const res = keyword.value.trim()
      ? await productApi.search(keyword.value.trim())
      : await productApi.getProducts(params)

    const list = res.data?.content || res.data?.content || res.data?.records || res.data?.list || res.data || []
    if (isRefresh) {
      products.value = list
    } else {
      products.value = [...products.value, ...list]
    }
    noMore.value = list.length < pageSize
  } catch {
    uni.showToast({ title: '加载失败', icon: 'none' })
  } finally {
    loading.value = false
  }
}

function switchCategory(cat: string) {
  if (currentCategory.value === cat) return
  currentCategory.value = cat
  keyword.value = ''
  refreshData()
}

function handleSearch() {
  currentCategory.value = ''
  refreshData()
}

function clearSearch() {
  keyword.value = ''
  refreshData()
}

function refreshData() {
  page.value = 0
  noMore.value = false
  loadProducts(true)
}

function goDetail(id: number) {
  uni.navigateTo({ url: `/pages/product-shop/detail?id=${id}` })
}

onShow(() => {
  loadCategories()
  loadRecommended()
  refreshData()
})

onPullDownRefresh(() => {
  page.value = 0
  noMore.value = false
  Promise.all([loadProducts(true), loadRecommended()])
    .then(() => uni.stopPullDownRefresh())
})

onReachBottom(() => {
  if (noMore.value || loading.value) return
  page.value++
  loadProducts()
})
</script>

<style scoped>
.shop-page {
  min-height: 100vh;
  background: #fdfbf7;
  font-family: 'Patrick Hand', cursive;
}

.search-bar {
  background: #fdfbf7;
  padding: 16rpx 24rpx;
  position: sticky;
  top: 0;
  z-index: 10;
}

.search-inner {
  display: flex;
  align-items: center;
  background: #fdfbf7;
  border: 2rpx dashed #2d2d2d;
  border-radius: 255px 15px 225px 15px / 15px 225px 15px 255px;
  padding: 0 24rpx;
  height: 72rpx;
}

.search-icon {
  font-size: 28rpx;
  margin-right: 12rpx;
}

.search-input {
  flex: 1;
  font-size: 28rpx;
  height: 72rpx;
  font-family: 'Patrick Hand', cursive;
}

.clear-btn {
  color: #2d2d2d;
  font-size: 28rpx;
  padding: 8rpx;
}

.category-tabs {
  white-space: nowrap;
  background: #fdfbf7;
  padding: 16rpx 16rpx;
  border-top: 2rpx dashed #e5e0d8;
}

.cat-tab {
  display: inline-block;
  padding: 12rpx 28rpx;
  margin: 0 8rpx;
  border-radius: 255px 15px 225px 15px / 15px 225px 15px 255px;
  font-size: 26rpx;
  color: #2d2d2d;
  background: #e5e0d8;
  border: 2rpx solid #2d2d2d;
  font-family: 'Patrick Hand', cursive;
}

.cat-tab.active {
  background: #ff4d4d;
  color: #fdfbf7;
  border-color: #2d2d2d;
  box-shadow: 3px 3px 0px 0px #2d2d2d;
}

/* Recommended */
.recommend-section {
  background: #fdfbf7;
  margin-top: 16rpx;
  padding: 24rpx 0;
  border-top: 2rpx dashed #e5e0d8;
}

.section-header {
  padding: 0 24rpx;
  margin-bottom: 16rpx;
}

.section-title {
  font-size: 30rpx;
  font-weight: 600;
  color: #2d2d2d;
  font-family: 'Kalam', cursive;
}

.recommend-scroll {
  white-space: nowrap;
  padding: 0 16rpx;
}

.recommend-card {
  display: inline-block;
  width: 240rpx;
  margin: 0 8rpx;
  vertical-align: top;
}

.recommend-img {
  width: 240rpx;
  height: 240rpx;
  border-radius: 15px 225px 15px 255px / 255px 15px 225px 15px;
  border: 2rpx solid #2d2d2d;
}

.recommend-name {
  display: block;
  font-size: 24rpx;
  color: #2d2d2d;
  margin-top: 8rpx;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-family: 'Patrick Hand', cursive;
}

.recommend-price {
  font-size: 28rpx;
  color: #ff4d4d;
  font-weight: 600;
  background: #fff9c4;
  padding: 2rpx 12rpx;
  border-radius: 8px 18px 8px 18px / 18px 8px 18px 8px;
  display: inline-block;
  transform: rotate(-1deg);
  font-family: 'Kalam', cursive;
}

/* Product Grid */
.product-grid {
  display: flex;
  flex-wrap: wrap;
  padding: 12rpx;
  gap: 12rpx;
  margin-top: 8rpx;
}

.product-card {
  width: calc(50% - 6rpx);
  background: #fdfbf7;
  border-radius: 255px 15px 225px 15px / 15px 225px 15px 255px;
  overflow: hidden;
  border: 2rpx solid #2d2d2d;
  box-shadow: 4px 4px 0px 0px #2d2d2d;
}

.product-img {
  width: 100%;
  height: 340rpx;
}

.product-info {
  padding: 16rpx 20rpx 20rpx;
}

.product-name {
  font-size: 26rpx;
  color: #2d2d2d;
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  min-height: 72rpx;
  font-family: 'Patrick Hand', cursive;
}

.price-row {
  display: flex;
  align-items: baseline;
  gap: 10rpx;
  margin-top: 12rpx;
}

.current-price {
  font-size: 32rpx;
  color: #ff4d4d;
  font-weight: 700;
  font-family: 'Kalam', cursive;
  background: #fff9c4;
  padding: 0 8rpx;
  transform: rotate(-1deg);
  display: inline-block;
}

.original-price {
  font-size: 22rpx;
  color: #2d2d2d;
  text-decoration: line-through;
  opacity: 0.5;
}

.sales-count {
  font-size: 22rpx;
  color: #2d2d2d;
  opacity: 0.5;
  margin-top: 6rpx;
  display: block;
  font-family: 'Patrick Hand', cursive;
}

/* Common */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 200rpx 0;
}

.empty-icon {
  font-size: 80rpx;
  margin-bottom: 20rpx;
}

.empty-text {
  font-size: 28rpx;
  color: #2d2d2d;
  opacity: 0.6;
  font-family: 'Patrick Hand', cursive;
}

.loading-more,
.no-more {
  text-align: center;
  padding: 30rpx;
  font-size: 24rpx;
  color: #2d2d2d;
  opacity: 0.5;
  font-family: 'Patrick Hand', cursive;
}

.disclaimer-tip {
  background: #fff9c4;
  color: #2d2d2d;
  border-radius: 255px 15px 225px 15px / 15px 225px 15px 255px;
  padding: 14rpx 48rpx 14rpx 20rpx;
  font-size: 22rpx;
  margin: 0 20rpx 20rpx;
  position: relative;
  border: 2rpx solid #2d2d2d;
  box-shadow: 3px 3px 0px 0px rgba(45,45,45,0.1);
  font-family: 'Patrick Hand', cursive;
}

.disclaimer-tip .dismiss {
  position: absolute;
  right: 16rpx;
  top: 50%;
  transform: translateY(-50%);
  font-size: 28rpx;
  color: #2d2d2d;
}
</style>
