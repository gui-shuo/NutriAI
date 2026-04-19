<script setup>
/**
 * 营养餐列表页 - 左侧分类导航 + 右侧商品列表
 * 支持门店切换、自提模式，分类从后端动态加载
 */
import { ref, computed, onMounted } from 'vue'
import NavBar from '../../components/NavBar.vue'
import { mealApi } from '../../services/api'
import { useCartStore } from '../../stores/cart'
import { formatPrice, cosUrl, checkLogin } from '../../utils/common'

const cartStore = useCartStore()

// 分类中文名映射
const categoryNameMap = {
  ANTI_INFLAMMATORY: '抗炎餐',
  LOW_FAT: '减脂轻食',
  HIGH_PROTEIN: '高蛋白餐',
  VEGETARIAN: '素食',
  KETO: '生酮餐',
  VEGAN: '纯素',
  GLUTEN_FREE: '无麸质',
  LOW_CARB: '低碳水',
  BALANCED: '均衡营养',
  DETOX: '轻体排毒',
}
const categoryIconMap = {
  ANTI_INFLAMMATORY: 'fire-fill',
  LOW_FAT: 'star-fill',
  HIGH_PROTEIN: 'integral',
  VEGETARIAN: 'leaf-fill',
  KETO: 'grid-fill',
  VEGAN: 'heart-fill',
  GLUTEN_FREE: 'checkbox-mark',
  LOW_CARB: 'minus-circle-fill',
  BALANCED: 'star-fill',
  DETOX: 'reload',
}

const categories = ref([{ id: '', name: '全部', icon: 'star-fill' }])
const activeCategory = ref('')
const meals = ref([])
const loading = ref(false)
const pickupMode = ref('self')

const currentStore = ref({ name: 'NutriAI 旗舰店', distance: '1.2km' })

function normalizeMeal(m) {
  const ni = m.nutritionInfo || {}
  return {
    ...m,
    image: m.imageUrl || m.image || '',
    price: m.salePrice ?? m.price,
    tag: (m.tags || [])[0] || m.tag || '',
    calories: ni.calories ?? m.calories ?? 0,
    protein: ni.protein ?? m.protein ?? 0,
    fat: ni.fat ?? m.fat ?? 0,
    carbs: ni.carbs ?? m.carbs ?? 0,
  }
}

function selectCategory(id) {
  activeCategory.value = id
  fetchMeals()
}

async function fetchMeals() {
  loading.value = true
  try {
    const params = { page: 0, size: 20 }
    if (activeCategory.value) params.category = activeCategory.value
    const res = await mealApi.getList(params)
    const list = res?.content || (Array.isArray(res) ? res : [])
    meals.value = list.map(normalizeMeal)
  } catch (e) {
    console.error('获取营养餐失败', e)
    meals.value = []
  } finally {
    loading.value = false
  }
}

async function fetchCategories() {
  try {
    const res = await mealApi.getCategories()
    if (Array.isArray(res) && res.length > 0) {
      categories.value = [
        { id: '', name: '全部', icon: 'star-fill' },
        ...res.map(c => ({
          id: c,
          name: categoryNameMap[c] || c,
          icon: categoryIconMap[c] || 'grid-fill'
        }))
      ]
    }
  } catch (e) {
    // 使用默认
  }
}

function goToDetail(id) {
  uni.navigateTo({ url: `/pages/meal/detail?id=${id}` })
}

function goToSearch() {
  uni.navigateTo({ url: '/pages/meal/search' })
}

function switchStore() {
  uni.showToast({ title: '门店选择功能开发中', icon: 'none' })
}

async function addToCart(meal) {
  if (!checkLogin()) return
  await cartStore.addItem({ itemType: 'MEAL', itemId: meal.id, quantity: 1 })
}

onMounted(() => {
  fetchCategories()
  fetchMeals()
})
</script>

<template>
  <view class="page">
    <NavBar title="NutriAI">
      <template #right>
        <view class="nav-search" @tap="goToSearch">
          <u-icon name="search" size="22" color="#1a1c1a" />
        </view>
      </template>
    </NavBar>

    <!-- 取餐方式 & 门店信息 -->
    <view class="store-bar">
      <view class="pickup-toggle">
        <view
          class="pickup-toggle__item"
          :class="{ 'pickup-toggle__item--active': pickupMode === 'self' }"
          @tap="pickupMode = 'self'"
        >
          <u-icon name="home" size="16" :color="pickupMode === 'self' ? '#0a6e2c' : '#666'" />
          <text class="pickup-toggle__label" :style="{ color: pickupMode === 'self' ? '#0a6e2c' : '#666' }">自提</text>
        </view>
        <view
          class="pickup-toggle__item"
          :class="{ 'pickup-toggle__item--active': pickupMode === 'delivery' }"
          @tap="pickupMode = 'delivery'"
        >
          <u-icon name="car" size="16" :color="pickupMode === 'delivery' ? '#0a6e2c' : '#666'" />
          <text class="pickup-toggle__label" :style="{ color: pickupMode === 'delivery' ? '#0a6e2c' : '#666' }">外送</text>
        </view>
      </view>
      <view class="store-info" @tap="switchStore">
        <text class="store-info__name">{{ currentStore.name }}</text>
        <text class="store-info__distance">{{ currentStore.distance }}</text>
        <text class="store-info__switch">切换门店 ›</text>
      </view>
    </view>

    <!-- 主体内容 -->
    <view class="main-content">
      <!-- 左侧分类导航 -->
      <scroll-view scroll-y class="category-nav" :enhanced="true" :show-scrollbar="false">
        <view
          v-for="cat in categories"
          :key="cat.id"
          class="category-item"
          :class="{ 'category-item--active': activeCategory === cat.id }"
          @tap="selectCategory(cat.id)"
        >
          <u-icon :name="cat.icon" size="20" :color="activeCategory === cat.id ? '#0a6e2c' : '#666'" />
          <text class="category-item__name" :style="{ color: activeCategory === cat.id ? '#0a6e2c' : '#333' }">{{ cat.name }}</text>
        </view>
      </scroll-view>

      <!-- 右侧商品列表 -->
      <scroll-view scroll-y class="meal-list" :enhanced="true" :show-scrollbar="false">
        <view class="section-header">
          <text class="section-header__title">
            {{ categories.find(c => c.id === activeCategory)?.name }}推荐
          </text>
          <text class="section-header__subtitle">为您精选健康美味</text>
        </view>

        <view v-if="loading" class="loading-state">
          <u-loading-icon mode="circle" size="28" color="#0a6e2c" />
          <text class="loading-state__text">加载中...</text>
        </view>

        <view
          v-for="meal in meals"
          :key="meal.id"
          class="meal-card"
          @tap="goToDetail(meal.id)"
        >
          <view class="meal-card__image-wrap">
            <u-image
              :src="cosUrl(meal.image) || '/static/images/meal-placeholder.png'"
              width="100px"
              height="100px"
              mode="aspectFill"
              radius="8"
              :lazy-load="true"
            />
            <view v-if="meal.tag" class="meal-card__badge">
              <text class="meal-card__badge-text">{{ meal.tag }}</text>
            </view>
          </view>
          <view class="meal-card__info">
            <text class="meal-card__name">{{ meal.name }}</text>
            <text class="meal-card__macros">
              蛋白质{{ meal.protein }}g | 脂肪{{ meal.fat }}g | 碳水{{ meal.carbs }}g
            </text>
            <view class="meal-card__footer">
              <view class="meal-card__price">
                <text class="meal-card__price-current">¥{{ formatPrice(meal.price) }}</text>
                <text v-if="meal.originalPrice" class="meal-card__price-original">
                  ¥{{ formatPrice(meal.originalPrice) }}
                </text>
              </view>
              <view class="meal-card__add" @tap.stop="addToCart(meal)">
                <u-icon name="plus" size="18" color="#ffffff" />
              </view>
            </view>
          </view>
        </view>

        <u-empty
          v-if="!loading && meals.length === 0"
          text="暂无餐品"
          icon="list"
          mode="list"
          marginTop="80"
        />

        <view style="height: 40rpx;" />
      </scroll-view>
    </view>
  </view>
</template>

<style lang="scss" scoped>
@import '../../styles/design-system.scss';

.page {
  min-height: 100vh;
  background: #ffffff;
  display: flex;
  flex-direction: column;
  overflow-x: hidden;
  width: 100%;
}

.nav-search {
  width: 64rpx;
  height: 64rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}

.store-bar {
  padding: 16rpx 24rpx;
  background: #ffffff;
}

.pickup-toggle {
  display: flex;
  background: $surface-container-low;
  border-radius: 50px;
  padding: 4rpx;
  margin-bottom: 16rpx;
  width: 240rpx;

  &__item {
    flex: 1;
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 8rpx;
    padding: 12rpx 0;
    border-radius: 50px;
    transition: all 0.3s;

    &--active {
      background: #fff;
      box-shadow: $shadow-sm;
    }
  }

  &__label {
    font-size: 24rpx;
    font-weight: 500;
  }
}

.store-info {
  display: flex;
  align-items: center;
  gap: 12rpx;

  &__name {
    font-size: 28rpx;
    font-weight: 600;
    color: $on-surface;
  }

  &__distance {
    font-size: 20rpx;
    color: $on-surface-variant;
  }

  &__switch {
    font-size: 20rpx;
    color: $primary;
    margin-left: auto;
  }
}

.main-content {
  flex: 1;
  display: flex;
  overflow: hidden;
}

.category-nav {
  width: 168rpx;
  background: $surface-container-low;
  height: calc(100vh - 280px);
  flex-shrink: 0;
}

.category-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 24rpx 8rpx;
  gap: 8rpx;
  position: relative;
  transition: all 0.3s;

  &--active {
    background: #ffffff;

    &::before {
      content: '';
      position: absolute;
      left: 0;
      top: 16rpx;
      bottom: 16rpx;
      width: 6rpx;
      background: $primary;
      border-radius: 0 4px 4px 0;
    }
  }

  &__name {
    font-size: 20rpx;
    text-align: center;
    line-height: 1.3;
  }
}

.meal-list {
  flex: 1;
  height: calc(100vh - 280px);
  padding: 0 20rpx;
}

.section-header {
  padding: 24rpx 0 16rpx;

  &__title {
    display: block;
    font-size: 32rpx;
    font-weight: 700;
    color: $on-surface;
    margin-bottom: 4rpx;
  }

  &__subtitle {
    display: block;
    font-size: 20rpx;
    color: $on-surface-variant;
  }
}

.loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16rpx;
  padding: 80rpx 0;

  &__text {
    font-size: 28rpx;
    color: $on-surface-variant;
  }
}

.meal-card {
  display: flex;
  gap: 20rpx;
  padding: 20rpx;
  background: #ffffff;
  border-radius: 12px;
  margin-bottom: 16rpx;
  box-shadow: $shadow-sm;

  &__image-wrap {
    position: relative;
    flex-shrink: 0;
  }

  &__badge {
    position: absolute;
    top: 8rpx;
    left: 8rpx;
    background: $primary;
    padding: 4rpx 12rpx;
    border-radius: 4px;

    &-text {
      font-size: 20rpx;
      color: #ffffff;
      font-weight: 500;
    }
  }

  &__info {
    flex: 1;
    display: flex;
    flex-direction: column;
    justify-content: space-between;
    min-width: 0;
  }

  &__name {
    font-size: 30rpx;
    font-weight: 600;
    color: $on-surface;
    margin-bottom: 8rpx;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  &__macros {
    font-size: 20rpx;
    color: $on-surface-variant;
    margin-bottom: 12rpx;
  }

  &__footer {
    display: flex;
    align-items: center;
    justify-content: space-between;
  }

  &__price {
    display: flex;
    align-items: baseline;
    gap: 8rpx;

    &-current {
      font-size: 32rpx;
      font-weight: 700;
      color: $primary;
    }

    &-original {
      font-size: 20rpx;
      color: $on-surface-variant;
      text-decoration: line-through;
    }
  }

  &__add {
    width: 56rpx;
    height: 56rpx;
    background: $primary;
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;

    &:active {
      opacity: 0.8;
    }
  }
}
</style>
