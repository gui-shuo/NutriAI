<script setup>
/**
 * 商城首页 - 直营销售模式
 * 搜索栏 + 分类 + 推荐商品 + 商品列表
 */
import { ref, onMounted } from 'vue'
import NavBar from '../../components/NavBar.vue'
import { productApi } from '../../services/api'
import { useCartStore } from '../../stores/cart'
import { formatPrice, cosUrl, checkLogin } from '../../utils/common'

const cartStore = useCartStore()

const defaultCategories = [
  { id: 'all', name: '全部', icon: 'star-fill' },
  { id: 'supplement', name: '营养补剂', icon: 'coupon' },
  { id: 'organic', name: '有机食材', icon: 'leaf-fill' },
  { id: 'protein', name: '蛋白粉', icon: 'integral' },
  { id: 'snack', name: '健康零食', icon: 'gift' },
  { id: 'drink', name: '健康饮品', icon: 'red-packet' },
]
const categories = ref(defaultCategories)

const activeCategory = ref('all')
const products = ref([])
const recommended = ref([])
const loading = ref(false)

// 标准化商品数据
function normalizeProduct(p) {
  return {
    ...p,
    image: p.imageUrl || p.image || '',
    images: p.imageUrls || p.images || [],
    price: p.salePrice ?? p.price,
    tag: (p.tags || [])[0] || p.tag || '',
    sales: p.salesCount ?? p.sales ?? 0,
  }
}

function goToSearch() {
  uni.navigateTo({ url: '/pages/shop/search' })
}

function goToCart() {
  uni.navigateTo({ url: '/pages/shop/cart' })
}

function goToDetail(id) {
  uni.navigateTo({ url: `/pages/shop/detail?id=${id}` })
}

function selectCategory(id) {
  activeCategory.value = id
  fetchProducts()
}

// 后端分类枚举名 → 中文名映射
const categoryNameMap = {
  SUPPLEMENT: '营养补剂',
  ORGANIC: '有机食材',
  PROTEIN: '蛋白粉',
  SNACK: '健康零食',
  DRINK: '健康饮品',
  HEALTH_FOOD: '健康食品',
  EQUIPMENT: '健身器材',
  VITAMIN: '维生素',
  MINERAL: '矿物质',
  HERBAL: '草本精华',
  SUPERFOOD: '超级食物',
  FIBER: '膳食纤维',
}

const categoryIconMap = {
  SUPPLEMENT: 'coupon',
  ORGANIC: 'leaf-fill',
  PROTEIN: 'integral',
  SNACK: 'gift',
  DRINK: 'red-packet',
  HEALTH_FOOD: 'star-fill',
  EQUIPMENT: 'grid-fill',
  VITAMIN: 'heart-fill',
  MINERAL: 'checkbox-mark',
  HERBAL: 'leaf-fill',
  SUPERFOOD: 'star-fill',
  FIBER: 'reload',
}

async function fetchCategories() {
  try {
    const res = await productApi.getCategories()
    if (Array.isArray(res) && res.length > 0) {
      categories.value = [
        { id: 'all', name: '全部', icon: 'star-fill' },
        ...res.map(c => ({
          id: c,
          name: categoryNameMap[c] || c,
          icon: categoryIconMap[c] || 'grid-fill',
        }))
      ]
    }
  } catch (e) {
    // 使用默认分类
  }
}

async function fetchProducts() {
  loading.value = true
  try {
    const params = { page: 0, size: 20 }
    if (activeCategory.value !== 'all') params.category = activeCategory.value
    const res = await productApi.getList(params)
    const list = res?.content || (Array.isArray(res) ? res : [])
    products.value = list.map(normalizeProduct)
  } catch (e) {
    console.error('获取商品列表失败', e)
    products.value = []
  } finally {
    loading.value = false
  }
}

async function fetchRecommended() {
  try {
    const res = await productApi.getRecommended()
    const list = Array.isArray(res) ? res : []
    recommended.value = list.map(normalizeProduct)
  } catch (e) {
    recommended.value = []
  }
}

async function addToCart(product) {
  if (!checkLogin()) return
  await cartStore.addItem({ itemType: 'PRODUCT', itemId: product.id, quantity: 1 })
}

onMounted(() => {
  fetchCategories()
  fetchProducts()
  fetchRecommended()
})
</script>

<template>
  <view class="page">
    <NavBar>
      <template #center>
        <text class="nav-title">有机商城</text>
      </template>
      <template #right>
        <view class="nav-cart" @tap="goToCart">
          <u-icon name="shopping-cart" size="24" color="#1a1c1a" />
        </view>
      </template>
    </NavBar>

    <scroll-view scroll-y class="content" :enhanced="true" :show-scrollbar="false">
      <!-- 搜索入口 -->
      <view class="search-entry" @tap="goToSearch">
        <u-icon name="search" size="18" color="#999" />
        <text class="search-entry__placeholder">搜索有机食材、营养补剂...</text>
      </view>

      <!-- 分类横向滚动 -->
      <scroll-view scroll-x class="categories" :enhanced="true" :show-scrollbar="false">
        <view
          v-for="cat in categories"
          :key="cat.id"
          class="cat-chip"
          :class="{ 'cat-chip--active': activeCategory === cat.id }"
          @tap="selectCategory(cat.id)"
        >
          <u-icon :name="cat.icon" size="16" :color="activeCategory === cat.id ? '#0a6e2c' : '#666'" />
          <text class="cat-chip__name">{{ cat.name }}</text>
        </view>
      </scroll-view>

      <!-- 商品网格 -->
      <view class="section-header">
        <text class="section-header__title">精选好物</text>
      </view>

      <view v-if="loading" class="state-tip">
        <u-loading-icon mode="circle" size="28" color="#0a6e2c" />
        <text style="margin-top: 16rpx;">加载中...</text>
      </view>

      <view class="product-grid">
        <view
          v-for="product in products"
          :key="product.id"
          class="product-card"
          @tap="goToDetail(product.id)"
        >
          <view class="product-card__image-wrap">
            <u-image
              :src="cosUrl(product.image) || '/static/images/product-placeholder.png'"
              width="100%"
              height="340rpx"
              mode="aspectFill"
              :lazy-load="true"
            />
            <view v-if="product.tag" class="product-card__tag">
              <text class="product-card__tag-text">{{ product.tag }}</text>
            </view>
          </view>
          <view class="product-card__info">
            <text class="product-card__name">{{ product.name }}</text>
            <view class="product-card__bottom">
              <view class="product-card__price-wrap">
                <text class="product-card__price">¥{{ formatPrice(product.price) }}</text>
                <text v-if="product.originalPrice" class="product-card__original-price">
                  ¥{{ formatPrice(product.originalPrice) }}
                </text>
              </view>
              <view class="product-card__add" @tap.stop="addToCart(product)">
                <u-icon name="plus" size="18" color="#ffffff" />
              </view>
            </view>
            <text v-if="product.sales" class="product-card__sales">
              已售{{ product.sales > 999 ? (product.sales / 1000).toFixed(1) + 'k' : product.sales }}
            </text>
          </view>
        </view>
      </view>

      <view style="height: 40rpx;" />
    </scroll-view>
  </view>
</template>

<style lang="scss" scoped>
@import '../../styles/design-system.scss';

.page {
  min-height: 100vh;
  background: #ffffff;
  overflow-x: hidden;
  width: 100%;
}

.content {
  height: calc(100vh - 88px);
}

.nav-title {
  font-size: $font-xl;
  font-weight: 800;
  color: $primary;
}

.nav-cart {
  position: relative;
  width: 64rpx;
  height: 64rpx;
  display: flex;
  align-items: center;
  justify-content: center;

  &__icon {
    font-size: 44rpx;
  }
}

// 搜索入口
.search-entry {
  display: flex;
  align-items: center;
  gap: 12rpx;
  margin: 16rpx 24rpx;
  padding: 20rpx 28rpx;
  background: $surface-container-low;
  border-radius: $radius-full;

  &__icon {
    font-size: 32rpx;
    opacity: 0.5;
  }

  &__placeholder {
    font-size: $font-base;
    color: $outline;
  }
}

// 分类
.categories {
  white-space: nowrap;
  padding: 0 24rpx;
  margin-bottom: 24rpx;
}

.cat-chip {
  display: inline-flex;
  align-items: center;
  gap: 8rpx;
  padding: 12rpx 24rpx;
  background: $surface-container-lowest;
  border-radius: $radius-full;
  margin-right: 12rpx;
  transition: all $transition-normal;

  &--active {
    background: $primary-container;
  }

  &__icon {
    font-size: 28rpx;
  }

  &__name {
    font-size: $font-sm;
    font-weight: 500;
    color: $on-surface;
  }
}

.section-header {
  padding: 0 24rpx 12rpx;

  &__title {
    font-size: $font-lg;
    font-weight: 700;
    color: $on-surface;
  }
}

.state-tip {
  padding: 80rpx 0;
  text-align: center;
  color: $on-surface-variant;
}

// 商品网格 - 两列瀑布流
.product-grid {
  display: flex;
  flex-wrap: wrap;
  padding: 0 16rpx;
  gap: 16rpx;
}

.product-card {
  width: calc(50% - 24rpx);
  margin: 0 4rpx;
  background: $surface-container-lowest;
  border-radius: $radius-xl;
  overflow: hidden;
  box-shadow: $shadow-sm;

  &__image-wrap {
    position: relative;
    width: 100%;
    height: 340rpx;
    overflow: hidden;
  }

  &__image {
    width: 100%;
    height: 100%;
  }

  &__tag {
    position: absolute;
    top: 12rpx;
    left: 12rpx;
    background: $primary;
    padding: 4rpx 14rpx;
    border-radius: $radius-sm;

    &-text {
      font-size: 20rpx;
      color: $on-primary;
      font-weight: 600;
    }
  }

  &__info {
    padding: 16rpx;
  }

  &__name {
    display: block;
    font-size: $font-base;
    font-weight: 600;
    color: $on-surface;
    margin-bottom: 8rpx;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  &__bottom {
    display: flex;
    align-items: center;
    justify-content: space-between;
  }

  &__price-wrap {
    display: flex;
    align-items: baseline;
    gap: 6rpx;
  }

  &__price {
    font-size: $font-lg;
    font-weight: 800;
    color: $primary;
  }

  &__original-price {
    font-size: $font-xs;
    color: $on-surface-variant;
    text-decoration: line-through;
  }

  &__add {
    width: 48rpx;
    height: 48rpx;
    background: $primary-container;
    border-radius: $radius-full;
    display: flex;
    align-items: center;
    justify-content: center;

    &-icon {
      font-size: 32rpx;
      color: $on-primary;
      font-weight: 300;
      line-height: 1;
    }
  }

  &__sales {
    display: block;
    font-size: 20rpx;
    color: $on-surface-variant;
    margin-top: 4rpx;
  }
}
</style>
