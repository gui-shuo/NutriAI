<template>
  <view class="detail-page">
    <scroll-view scroll-y class="page-scroll">
      <!-- Product Image -->
      <swiper v-if="images.length" class="product-swiper" indicator-dots circular>
        <swiper-item v-for="(img, i) in images" :key="i">
          <image :src="img" mode="aspectFill" class="swiper-img" @tap="previewImage(i)" />
        </swiper-item>
      </swiper>
      <view v-else class="product-swiper placeholder-img flex-center">
        <text style="font-size: 80rpx;">🍱</text>
      </view>

      <!-- Info Card -->
      <view class="info-card card">
        <view class="price-row">
          <text class="text-price text-2xl">¥{{ product.price || '0.00' }}</text>
          <text v-if="product.originalPrice && product.originalPrice > product.price" class="original-price text-md ml-sm">
            ¥{{ product.originalPrice }}
          </text>
          <view v-if="discount" class="discount-badge">{{ discount }}折</view>
        </view>
        <text class="product-title">{{ product.name }}</text>
        <text class="text-sm text-secondary mt-sm">{{ product.description }}</text>

        <!-- Tags -->
        <view v-if="product.tags" class="tag-row mt-md">
          <text v-for="tag in product.tags.split(',')" :key="tag" class="badge-primary">{{ tag }}</text>
        </view>

        <!-- Stats -->
        <view class="stats-row mt-md">
          <view class="stat-item">
            <text class="stat-value">{{ product.salesCount || 0 }}</text>
            <text class="stat-label">已售</text>
          </view>
          <view class="stat-item">
            <text class="stat-value">{{ product.rating || '5.0' }}</text>
            <text class="stat-label">评分</text>
          </view>
          <view class="stat-item">
            <text class="stat-value">{{ product.stock || 0 }}</text>
            <text class="stat-label">库存</text>
          </view>
        </view>
      </view>

      <!-- Nutrition Info -->
      <view v-if="product.nutritionInfo" class="card mt-md mx-lg">
        <text class="section-title">营养信息</text>
        <view class="nutrition-grid mt-md">
          <view class="nutrition-item">
            <text class="nut-value">{{ product.calories || '-' }}</text>
            <text class="nut-label">热量(kcal)</text>
          </view>
          <view class="nutrition-item">
            <text class="nut-value">{{ product.protein || '-' }}</text>
            <text class="nut-label">蛋白质(g)</text>
          </view>
          <view class="nutrition-item">
            <text class="nut-value">{{ product.fat || '-' }}</text>
            <text class="nut-label">脂肪(g)</text>
          </view>
          <view class="nutrition-item">
            <text class="nut-value">{{ product.carbs || '-' }}</text>
            <text class="nut-label">碳水(g)</text>
          </view>
        </view>
      </view>

      <!-- Detail Content -->
      <view class="card mt-md mx-lg">
        <text class="section-title">套餐详情</text>
        <view class="detail-content mt-md">
          <rich-text :nodes="product.detailContent || product.description || '暂无详情'"></rich-text>
        </view>
      </view>

      <!-- Pickup Info -->
      <view class="card mt-md mx-lg">
        <text class="section-title">取餐/配送说明</text>
        <view class="pickup-info mt-md">
          <view class="pickup-item">
            <text class="pickup-icon">🏫</text>
            <view>
              <text class="text-bold">线下取餐</text>
              <text class="text-sm text-secondary">下单后可至工作室取餐，具体地址订单中显示</text>
            </view>
          </view>
          <view class="pickup-item mt-md">
            <text class="pickup-icon">🚗</text>
            <view>
              <text class="text-bold">校园配送</text>
              <text class="text-sm text-secondary">后续将对接学校外卖平台配送</text>
            </view>
          </view>
        </view>
      </view>

      <view style="height: 160rpx;"></view>
    </scroll-view>

    <!-- Bottom Bar -->
    <view class="bottom-bar safe-bottom">
      <view class="bar-left">
        <view class="bar-action" @tap="goCart">
          <text class="bar-icon">🛒</text>
          <text class="bar-label">购物车</text>
        </view>
      </view>
      <view class="bar-right">
        <button class="btn-add-cart" @tap="addToCart">加入购物车</button>
        <button class="btn-buy-now" @tap="buyNow">立即订购</button>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { productApi } from '../../services/api'
import { useUserStore } from '../../stores/user'
import { formatPrice } from '../../utils'

const userStore = useUserStore()
const product = ref<any>({})
const productId = ref(0)

const images = computed(() => {
  if (product.value.imageUrl) {
    return [product.value.imageUrl]
  }
  return []
})

const discount = computed(() => {
  if (product.value.originalPrice && product.value.price) {
    const d = (product.value.price / product.value.originalPrice * 10).toFixed(1)
    return d !== '10.0' ? d : ''
  }
  return ''
})

onLoad((query: any) => {
  if (query?.id) {
    productId.value = Number(query.id)
    loadDetail()
  }
})

async function loadDetail() {
  uni.showLoading({ title: '加载中' })
  try {
    const data: any = await productApi.getDetail(productId.value)
    product.value = data || {}
  } catch {}
  uni.hideLoading()
}

function previewImage(index: number) {
  uni.previewImage({
    urls: images.value,
    current: index
  })
}

function addToCart() {
  if (!checkLogin()) return
  const cart = getCart()
  const existing = cart.find((c: any) => c.productId === product.value.id)
  if (existing) {
    existing.quantity++
  } else {
    cart.push({
      productId: product.value.id,
      name: product.value.name,
      price: product.value.price,
      imageUrl: product.value.imageUrl,
      quantity: 1
    })
  }
  saveCart(cart)
  uni.showToast({ title: '已加入购物车', icon: 'success' })
}

function buyNow() {
  if (!checkLogin()) return
  uni.navigateTo({
    url: `/pages/order/confirm?type=meal&productId=${product.value.id}&name=${encodeURIComponent(product.value.name)}&price=${product.value.price}&imageUrl=${encodeURIComponent(product.value.imageUrl || '')}`
  })
}

function goCart() {
  uni.navigateTo({ url: '/pages/cart/index' })
}

function checkLogin(): boolean {
  if (!userStore.isLoggedIn) {
    uni.navigateTo({ url: '/pages/auth/login' })
    return false
  }
  return true
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
.detail-page {
  min-height: 100vh;
  background: $bg-page;
}

.page-scroll {
  height: 100vh;
}

.product-swiper {
  width: 100%;
  height: 560rpx;
}

.swiper-img {
  width: 100%;
  height: 100%;
}

.placeholder-img {
  background: $bg-muted;
}

.info-card {
  margin: -40rpx $spacing-lg 0;
  position: relative;
  z-index: 1;
}

.price-row {
  display: flex;
  align-items: baseline;
}

.original-price {
  text-decoration: line-through;
  color: $text-muted;
}

.discount-badge {
  margin-left: $spacing-sm;
  padding: 4rpx 12rpx;
  background: $error;
  color: #fff;
  font-size: $font-xs;
  border-radius: $radius-full;
}

.product-title {
  display: block;
  font-size: $font-xl;
  font-weight: 700;
  color: $text-primary;
  margin-top: $spacing-sm;
}

.tag-row {
  display: flex;
  flex-wrap: wrap;
  gap: $spacing-sm;
}

.stats-row {
  display: flex;
  background: $bg-muted;
  border-radius: $radius-md;
  padding: $spacing-md 0;
}

.stat-item {
  flex: 1;
  text-align: center;
}

.stat-value {
  display: block;
  font-size: $font-lg;
  font-weight: 700;
  color: $text-primary;
}

.stat-label {
  display: block;
  font-size: $font-xs;
  color: $text-muted;
  margin-top: 4rpx;
}

.mx-lg {
  margin-left: $spacing-lg;
  margin-right: $spacing-lg;
}

.nutrition-grid {
  display: flex;
}

.nutrition-item {
  flex: 1;
  text-align: center;
}

.nut-value {
  display: block;
  font-size: $font-lg;
  font-weight: 600;
  color: $primary;
}

.nut-label {
  display: block;
  font-size: $font-xs;
  color: $text-muted;
}

.detail-content {
  font-size: $font-base;
  color: $text-secondary;
  line-height: 1.8;
}

.pickup-item {
  display: flex;
  gap: $spacing-md;
  align-items: flex-start;
}

.pickup-icon {
  font-size: 36rpx;
  flex-shrink: 0;
}

// Bottom bar
.bottom-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  background: $bg-card;
  box-shadow: 0 -2rpx 10rpx rgba(0, 0, 0, 0.05);
  display: flex;
  align-items: center;
  padding: $spacing-sm $spacing-lg;
  z-index: 100;
}

.bar-left {
  display: flex;
  gap: $spacing-lg;
  margin-right: $spacing-lg;
}

.bar-action {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.bar-icon {
  font-size: 36rpx;
}

.bar-label {
  font-size: $font-xs;
  color: $text-muted;
}

.bar-right {
  flex: 1;
  display: flex;
  gap: $spacing-sm;
}

.btn-add-cart {
  flex: 1;
  height: 80rpx;
  line-height: 80rpx;
  background: #FFA940;
  color: #fff;
  border-radius: $radius-xl 0 0 $radius-xl;
  font-size: $font-base;
  font-weight: 500;
  border: none;
  &::after { border: none; }
}

.btn-buy-now {
  flex: 1;
  height: 80rpx;
  line-height: 80rpx;
  background: $gradient-primary;
  color: #fff;
  border-radius: 0 $radius-xl $radius-xl 0;
  font-size: $font-base;
  font-weight: 500;
  border: none;
  &::after { border: none; }
}
</style>
