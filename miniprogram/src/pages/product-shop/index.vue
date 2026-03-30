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
          placeholder="搜索健康食品..."
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
  HEALTH_FOOD: '健康食品',
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
  background: #f5f5f5;
}

.search-bar {
  background: #fff;
  padding: 16rpx 24rpx;
  position: sticky;
  top: 0;
  z-index: 10;
}

.search-inner {
  display: flex;
  align-items: center;
  background: #f5f5f5;
  border-radius: 36rpx;
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
}

.clear-btn {
  color: #999;
  font-size: 28rpx;
  padding: 8rpx;
}

.category-tabs {
  white-space: nowrap;
  background: #fff;
  padding: 16rpx 16rpx;
  border-top: 1rpx solid #f0f0f0;
}

.cat-tab {
  display: inline-block;
  padding: 12rpx 28rpx;
  margin: 0 8rpx;
  border-radius: 30rpx;
  font-size: 26rpx;
  color: #666;
  background: #f0f0f0;
}

.cat-tab.active {
  background: #07c160;
  color: #fff;
}

/* Recommended */
.recommend-section {
  background: #fff;
  margin-top: 16rpx;
  padding: 24rpx 0;
}

.section-header {
  padding: 0 24rpx;
  margin-bottom: 16rpx;
}

.section-title {
  font-size: 30rpx;
  font-weight: 600;
  color: #333;
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
  border-radius: 12rpx;
}

.recommend-name {
  display: block;
  font-size: 24rpx;
  color: #333;
  margin-top: 8rpx;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.recommend-price {
  font-size: 28rpx;
  color: #ee0a24;
  font-weight: 600;
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
  background: #fff;
  border-radius: 16rpx;
  overflow: hidden;
  box-shadow: 0 2rpx 8rpx rgba(0, 0, 0, 0.04);
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
  color: #333;
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  min-height: 72rpx;
}

.price-row {
  display: flex;
  align-items: baseline;
  gap: 10rpx;
  margin-top: 12rpx;
}

.current-price {
  font-size: 32rpx;
  color: #ee0a24;
  font-weight: 700;
}

.original-price {
  font-size: 22rpx;
  color: #999;
  text-decoration: line-through;
}

.sales-count {
  font-size: 22rpx;
  color: #999;
  margin-top: 6rpx;
  display: block;
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
  color: #999;
}

.loading-more,
.no-more {
  text-align: center;
  padding: 30rpx;
  font-size: 24rpx;
  color: #999;
}
.disclaimer-tip {
  background: #fff3cd;
  color: #856404;
  border-radius: 12rpx;
  padding: 14rpx 48rpx 14rpx 20rpx;
  font-size: 22rpx;
  margin: 0 20rpx 20rpx;
  position: relative;
}
.disclaimer-tip .dismiss {
  position: absolute;
  right: 16rpx;
  top: 50%;
  transform: translateY(-50%);
  font-size: 28rpx;
  color: #999;
}
</style>
