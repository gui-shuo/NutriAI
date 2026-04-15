<template>
  <view class="shop-page">
    <!-- Search -->
    <view class="search-bar">
      <view class="search-input">
        <text class="search-icon">🔍</text>
        <input v-model="keyword" placeholder="搜索营养产品" confirm-type="search" @confirm="onSearch" />
      </view>
      <view class="cart-btn" @tap="goCart">
        <text class="cart-icon">🛒</text>
        <view v-if="cartCount > 0" class="cart-badge">{{ cartCount > 99 ? '99+' : cartCount }}</view>
      </view>
    </view>

    <!-- Categories -->
    <scroll-view scroll-x class="category-tabs">
      <view class="tabs-inner">
        <view
          v-for="cat in categories"
          :key="cat.key"
          :class="['tab-item', { active: currentCategory === cat.key }]"
          @tap="switchCategory(cat.key)"
        >
          <text>{{ cat.label }}</text>
        </view>
      </view>
    </scroll-view>

    <!-- Products Grid -->
    <scroll-view
      scroll-y
      class="product-list"
      @scrolltolower="loadMore"
      refresher-enabled
      :refresher-triggered="refreshing"
      @refresherrefresh="onRefresh"
    >
      <view class="grid-2 px-lg py-md">
        <view
          v-for="item in products"
          :key="item.id"
          class="product-card"
          @tap="goDetail(item.id)"
        >
          <view class="product-img-wrap">
            <image :src="item.imageUrl || '/static/images/product-default.png'" mode="aspectFill" class="product-img" />
            <view v-if="item.recommended" class="recommend-badge">推荐</view>
          </view>
          <view class="product-info">
            <text class="product-name text-line-2">{{ item.name }}</text>
            <view class="product-bottom">
              <text class="text-price">¥{{ item.price }}</text>
              <view class="add-cart-btn" @tap.stop="addToCart(item)">
                <text>+</text>
              </view>
            </view>
          </view>
        </view>
      </view>

      <view v-if="loading" class="loading-hint">
        <text class="text-muted">加载中...</text>
      </view>
      <view v-else-if="!products.length" class="empty-state">
        <text class="empty-icon">🛍️</text>
        <text class="empty-text">暂无商品</text>
      </view>
      <view v-else-if="noMore" class="loading-hint">
        <text class="text-muted text-sm">— 没有更多了 —</text>
      </view>
    </scroll-view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { productApi, PRODUCT_CATEGORIES } from '../../services/api'

const keyword = ref('')
const currentCategory = ref('')
const products = ref<any[]>([])
const loading = ref(false)
const refreshing = ref(false)
const noMore = ref(false)
const page = ref(0)
const pageSize = 10

const categories = PRODUCT_CATEGORIES

const cartCount = computed(() => {
  const cart = getCart()
  return cart.reduce((sum: number, item: any) => sum + item.quantity, 0)
})

onShow(() => {
  if (!products.value.length) loadProducts(true)
})

async function loadProducts(reset = false) {
  if (loading.value) return
  if (reset) { page.value = 0; noMore.value = false }
  loading.value = true
  try {
    let data: any
    if (keyword.value) {
      data = await productApi.search(keyword.value, page.value, pageSize)
    } else {
      data = await productApi.getList({ category: currentCategory.value || undefined, page: page.value, size: pageSize })
    }
    const list = data?.content || data?.records || data || []
    meals(reset, list)
    if (list.length < pageSize) noMore.value = true
    page.value++
  } catch {}
  loading.value = false
  refreshing.value = false
}

function meals(reset: boolean, list: any[]) {
  if (reset) {
    products.value = list
  } else {
    products.value = [...products.value, ...list]
  }
}

function switchCategory(key: string) {
  currentCategory.value = key
  loadProducts(true)
}

function onSearch() { loadProducts(true) }
function onRefresh() { refreshing.value = true; loadProducts(true) }
function loadMore() { if (!noMore.value) loadProducts() }

function goDetail(id: number) {
  uni.navigateTo({ url: `/pages/shop/detail?id=${id}` })
}

function goCart() {
  uni.navigateTo({ url: '/pages/cart/index' })
}

function addToCart(item: any) {
  const cart = getCart()
  const existing = cart.find((c: any) => c.productId === item.id)
  if (existing) {
    existing.quantity++
  } else {
    cart.push({
      productId: item.id,
      name: item.name,
      price: item.price,
      imageUrl: item.imageUrl,
      quantity: 1
    })
  }
  saveCart(cart)
  uni.showToast({ title: '已加入购物车', icon: 'success' })
}

const CART_KEY = 'nutriai_cart'
function getCart(): any[] {
  try { return JSON.parse(uni.getStorageSync(CART_KEY) || '[]') } catch { return [] }
}
function saveCart(cart: any[]) {
  uni.setStorageSync(CART_KEY, JSON.stringify(cart))
}
</script>

<style lang="scss" scoped>
.shop-page {
  min-height: 100vh;
  background: $bg-page;
  display: flex;
  flex-direction: column;
}

.search-bar {
  padding: $spacing-md $spacing-lg;
  background: $bg-card;
  display: flex;
  align-items: center;
  gap: $spacing-md;
}

.search-input {
  flex: 1;
  display: flex;
  align-items: center;
  background: $bg-muted;
  border-radius: $radius-full;
  padding: 0 $spacing-md;
  height: 72rpx;

  input { flex: 1; font-size: $font-base; margin-left: $spacing-sm; }
}

.search-icon { font-size: 28rpx; }

.cart-btn {
  position: relative;
  padding: $spacing-sm;
}

.cart-icon { font-size: 40rpx; }

.cart-badge {
  position: absolute;
  top: 0;
  right: 0;
  min-width: 32rpx;
  height: 32rpx;
  background: $error;
  color: #fff;
  font-size: 20rpx;
  border-radius: $radius-full;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 6rpx;
}

.category-tabs {
  background: $bg-card;
  white-space: nowrap;
  border-bottom: 1rpx solid $border-light;
}

.tabs-inner {
  display: inline-flex;
  padding: $spacing-sm $spacing-md;
  gap: $spacing-sm;
}

.tab-item {
  display: inline-flex;
  padding: 12rpx 28rpx;
  border-radius: $radius-full;
  font-size: $font-sm;
  color: $text-secondary;
  background: $bg-muted;
  flex-shrink: 0;

  &.active { background: $primary; color: #fff; }
}

.product-list {
  flex: 1;
  height: 0;
}

.product-card {
  background: $bg-card;
  border-radius: $radius-lg;
  overflow: hidden;
  box-shadow: $shadow-sm;
}

.product-img-wrap {
  position: relative;
}

.product-img {
  width: 100%;
  height: 280rpx;
  background: $bg-muted;
}

.recommend-badge {
  position: absolute;
  top: $spacing-sm;
  left: $spacing-sm;
  padding: 4rpx 16rpx;
  background: $gradient-primary;
  color: #fff;
  font-size: $font-xs;
  border-radius: $radius-full;
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

.product-bottom {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: $spacing-sm;
}

.add-cart-btn {
  width: 48rpx;
  height: 48rpx;
  border-radius: 50%;
  background: $primary;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: $font-lg;
  font-weight: 700;
}

.loading-hint {
  text-align: center;
  padding: $spacing-xl 0;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 120rpx 0;
}

.empty-icon { font-size: 80rpx; margin-bottom: $spacing-md; }
</style>
