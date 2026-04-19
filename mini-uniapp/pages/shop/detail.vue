<script setup>
/**
 * 商品详情页
 */
import { ref, onMounted } from 'vue'
import NavBar from '../../components/NavBar.vue'
import { productApi } from '../../services/api'
import { useCartStore } from '../../stores/cart'
import { formatPrice, cosUrl, checkLogin } from '../../utils/common'

const cartStore = useCartStore()
const productId = ref('')
const product = ref(null)
const loading = ref(true)
const currentImageIdx = ref(0)

onMounted(() => {
  const pages = getCurrentPages()
  const page = pages[pages.length - 1]
  productId.value = page.options?.id || ''
  fetchDetail()
})

async function fetchDetail() {
  loading.value = true
  try {
    const res = await productApi.getDetail(productId.value)
    product.value = {
      ...res,
      image: res.imageUrl || res.image || '',
      images: res.imageUrls || res.images || [],
      price: res.salePrice ?? res.price,
      tag: (res.tags || [])[0] || res.tag || '',
      sales: res.salesCount ?? res.sales ?? 0,
    }
  } catch (e) {
    console.error('获取商品详情失败', e)
    uni.showToast({ title: '加载失败', icon: 'none' })
  } finally {
    loading.value = false
  }
}

async function addToCart() {
  if (!checkLogin()) return
  try {
    await cartStore.addItem({ itemType: 'PRODUCT', itemId: Number(productId.value), quantity: 1 })
  } catch (e) {
    uni.showToast({ title: '添加失败', icon: 'none' })
  }
}

function buyNow() {
  if (!checkLogin()) return
  uni.showToast({ title: '立即购买功能开发中', icon: 'none' })
}

function onSwiperChange(e) {
  currentImageIdx.value = e.detail.current
}
</script>

<template>
  <view class="page">
    <NavBar showBack transparent />

    <view v-if="loading" class="loading">
      <u-loading-icon mode="circle" size="28" color="#0a6e2c" />
      <text style="margin-top: 16rpx; color: #666;">加载中...</text>
    </view>

    <view v-else-if="product" class="detail">
      <!-- 商品图片轮播 -->
      <view class="gallery">
        <swiper
          class="gallery__swiper"
          :indicator-dots="false"
          :autoplay="true"
          :interval="4000"
          :circular="true"
          @change="onSwiperChange"
        >
          <swiper-item v-if="!product.images || product.images.length === 0">
            <u-image
              src="/static/images/product-placeholder.png"
              width="100%"
              height="680rpx"
              mode="aspectFill"
            />
          </swiper-item>
          <swiper-item v-for="(img, idx) in product.images" :key="idx">
            <u-image
              :src="cosUrl(img)"
              width="100%"
              height="680rpx"
              mode="aspectFill"
              :lazy-load="true"
            />
          </swiper-item>
        </swiper>
        <view v-if="product.images && product.images.length > 1" class="gallery__dots">
          <view
            v-for="(_, idx) in product.images"
            :key="idx"
            class="gallery__dot"
            :class="{ 'gallery__dot--active': currentImageIdx === idx }"
          />
        </view>
      </view>

      <view class="info-area">
        <!-- 价格 & 标签 -->
        <view class="price-section">
          <view class="price-row">
            <text class="price-current">¥{{ formatPrice(product.price) }}</text>
            <text v-if="product.originalPrice" class="price-original">
              ¥{{ formatPrice(product.originalPrice) }}
            </text>
          </view>
          <view v-if="product.tag" class="product-tag">
            <u-tag :text="product.tag" size="mini" color="#0a6e2c" bgColor="#e8f5e9" borderColor="#e8f5e9" />
          </view>
        </view>

        <!-- 名称 & 销量 -->
        <text class="product-name">{{ product.name }}</text>
        <text v-if="product.sales" class="product-sales">
          累计售出 {{ product.sales }} 件
        </text>

        <!-- 描述 -->
        <view class="desc-section">
          <text class="desc-title">商品介绍</text>
          <text class="desc-text">{{ product.description }}</text>
        </view>
      </view>

      <!-- 底部操作栏 -->
      <view class="action-bar safe-bottom">
        <view class="action-bar__left">
          <view class="action-bar__icon-btn" @tap="() => uni.navigateTo({ url: '/pages/shop/cart' })">
            <u-icon name="shopping-cart" size="24" color="#333" />
            <text class="action-bar__icon-label">购物车</text>
          </view>
        </view>
        <view class="action-bar__buttons">
          <u-button text="加入购物车" shape="circle" size="normal" color="#e8f5e9" :customStyle="{color: '#0a6e2c', fontWeight: '600'}" @click="addToCart" />
          <u-button text="立即购买" type="primary" shape="circle" size="normal" color="#0a6e2c" @click="buyNow" />
        </view>
      </view>
    </view>
  </view>
</template>

<style lang="scss" scoped>
@import '../../styles/design-system.scss';

.page {
  min-height: 100vh;
  background: #ffffff;
  padding-bottom: 160rpx;
  overflow-x: hidden;
  width: 100%;
}

.loading {
  padding: 200rpx 0;
  text-align: center;
  color: $on-surface-variant;
  display: flex;
  flex-direction: column;
  align-items: center;
}

// 图片轮播
.gallery {
  position: relative;

  &__swiper {
    height: 680rpx;
  }

  &__image {
    width: 100%;
    height: 100%;
  }

  &__dots {
    position: absolute;
    bottom: 24rpx;
    left: 50%;
    transform: translateX(-50%);
    display: flex;
    gap: 12rpx;
  }

  &__dot {
    width: 12rpx;
    height: 12rpx;
    border-radius: $radius-full;
    background: rgba(255, 255, 255, 0.4);
    transition: all $transition-normal;

    &--active {
      width: 32rpx;
      background: #fff;
    }
  }
}

.info-area {
  padding: 32rpx 24rpx;
}

// 价格
.price-section {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16rpx;
}

.price-row {
  display: flex;
  align-items: baseline;
  gap: 12rpx;
}

.price-current {
  font-size: $font-3xl;
  font-weight: 800;
  color: $primary;
}

.price-original {
  font-size: $font-base;
  color: $on-surface-variant;
  text-decoration: line-through;
}

.product-tag {
  background: $primary-container;
  padding: 6rpx 16rpx;
  border-radius: $radius-sm;

  &__text {
    font-size: $font-xs;
    color: $primary;
    font-weight: 600;
  }
}

.product-name {
  display: block;
  font-size: $font-xl;
  font-weight: 700;
  color: $on-surface;
  margin-bottom: 8rpx;
}

.product-sales {
  display: block;
  font-size: $font-sm;
  color: $on-surface-variant;
  margin-bottom: 28rpx;
}

// 描述
.desc-section {
  background: $surface-container-low;
  border-radius: $radius-xl;
  padding: 24rpx;
}

.desc-title {
  display: block;
  font-size: $font-base;
  font-weight: 600;
  color: $on-surface;
  margin-bottom: 12rpx;
}

.desc-text {
  display: block;
  font-size: $font-sm;
  color: $on-surface-variant;
  line-height: $leading-relaxed;
}

// 底部操作栏
.action-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  display: flex;
  align-items: center;
  padding: 16rpx 24rpx;
  background: rgba(255, 255, 255, 0.95);
  background: rgba(255, 255, 255, 0.95);
  gap: 20rpx;
  z-index: 50;

  &__left {
    flex-shrink: 0;
  }

  &__icon-btn {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 4rpx;
  }

  &__icon-label {
    font-size: 20rpx;
    color: $on-surface-variant;
  }

  &__buttons {
    flex: 1;
    display: flex;
    gap: 16rpx;
  }

  &__btn {
    flex: 1;
    padding: 20rpx 0;
    text-align: center;
    border-radius: $radius-lg;
    font-size: $font-base;
    font-weight: 600;

    &--cart {
      background: $secondary-container;
      color: $on-surface;
    }

    &--buy {
      background: $primary;
      color: $on-primary;
    }

    &:active {
      transform: scale(0.96);
    }
  }
}
</style>
