<script setup>
/**
 * 营养餐详情页
 * 营养成分Bento展示 + 食材清单
 */
import { ref, onMounted } from 'vue'
import NavBar from '../../components/NavBar.vue'
import { mealApi } from '../../services/api'
import { useCartStore } from '../../stores/cart'
import { formatPrice, cosUrl, checkLogin } from '../../utils/common'

const cartStore = useCartStore()
const mealId = ref('')
const meal = ref(null)
const loading = ref(true)

// 标准化餐品数据
function normalizeMeal(m) {
  const ni = m.nutritionInfo || {}
  // ingredients 可能是字符串或数组
  let ingredientList = m.ingredients || []
  if (typeof ingredientList === 'string') {
    ingredientList = ingredientList.split(/[,，、]/).map(name => ({ name: name.trim(), icon: '🥬' }))
  } else if (Array.isArray(ingredientList) && ingredientList.length && typeof ingredientList[0] === 'string') {
    ingredientList = ingredientList.map(name => ({ name, icon: '🥬' }))
  }
  return {
    ...m,
    image: m.imageUrl || m.image || '',
    price: m.salePrice ?? m.price,
    tag: (m.tags || [])[0] || m.tag || '',
    calories: ni.calories ?? m.calories ?? 0,
    protein: ni.protein ?? m.protein ?? 0,
    fat: ni.fat ?? m.fat ?? 0,
    carbs: ni.carbs ?? m.carbs ?? 0,
    fiber: ni.fiber ?? m.fiber ?? 0,
    dailyCalorieGoal: 2000,
    ingredients: ingredientList,
  }
}

onMounted(() => {
  const pages = getCurrentPages()
  const page = pages[pages.length - 1]
  mealId.value = page.options?.id || ''
  fetchDetail()
})

async function fetchDetail() {
  loading.value = true
  try {
    const res = await mealApi.getDetail(mealId.value)
    meal.value = normalizeMeal(res)
  } catch (e) {
    console.error('获取餐品详情失败', e)
    uni.showToast({ title: '加载失败', icon: 'none' })
  } finally {
    loading.value = false
  }
}

async function addToCart() {
  if (!checkLogin()) return
  await cartStore.addItem({ itemType: 'MEAL', itemId: mealId.value, quantity: 1 })
}

function buyNow() {
  if (!checkLogin()) return
  uni.navigateTo({ url: `/pages/meal/checkout?id=${mealId.value}` })
}
</script>

<template>
  <view class="page">
    <NavBar showBack transparent />

    <view v-if="loading" class="loading">
      <u-loading-icon mode="circle" size="28" color="#0a6e2c" />
      <text style="margin-top: 16rpx; color: #666;">加载中...</text>
    </view>

    <view v-else-if="meal" class="detail">
      <!-- 英雄图片 -->
      <view class="hero">
        <u-image
          :src="cosUrl(meal.image) || '/static/images/meal-placeholder.png'"
          width="100%"
          height="560rpx"
          mode="aspectFill"
          :lazy-load="true"
        />
        <view class="hero__overlay" />
        <view class="hero__content">
          <u-tag v-if="meal.tag" :text="meal.tag" size="mini" color="#ffffff" bgColor="#0a6e2c" borderColor="#0a6e2c" />
          <text class="hero__title">{{ meal.name }}</text>
        </view>
      </view>

      <view class="info-section">
        <!-- 营养成分 Bento Grid -->
        <view class="nutrition-grid">
          <!-- 热量卡片 (大) -->
          <view class="nutrition-card nutrition-card--energy">
            <text class="nutrition-card__label">热量</text>
            <view class="nutrition-card__value-row">
              <text class="nutrition-card__value">{{ meal.calories }}</text>
              <text class="nutrition-card__unit">kcal</text>
            </view>
            <view class="nutrition-card__bar">
              <view
                class="nutrition-card__bar-fill"
                :style="{ width: Math.min((meal.calories / (meal.dailyCalorieGoal || 2000)) * 100, 100) + '%' }"
              />
            </view>
            <text class="nutrition-card__goal">
              每日目标 {{ meal.dailyCalorieGoal || 2000 }} kcal
            </text>
          </view>

          <!-- 宏量素卡片 -->
          <view class="nutrition-card nutrition-card--macros">
            <text class="nutrition-card__label">宏量营养素</text>
            <view class="macros-bars">
              <view class="macros-bar">
                <view class="macros-bar__track">
                  <view class="macros-bar__fill macros-bar__fill--protein" style="height: 80%" />
                </view>
                <text class="macros-bar__label">蛋白质</text>
                <text class="macros-bar__value">{{ meal.protein }}g</text>
              </view>
              <view class="macros-bar">
                <view class="macros-bar__track">
                  <view class="macros-bar__fill macros-bar__fill--fat" style="height: 40%" />
                </view>
                <text class="macros-bar__label">脂肪</text>
                <text class="macros-bar__value">{{ meal.fat }}g</text>
              </view>
              <view class="macros-bar">
                <view class="macros-bar__track">
                  <view class="macros-bar__fill macros-bar__fill--carbs" style="height: 65%" />
                </view>
                <text class="macros-bar__label">碳水</text>
                <text class="macros-bar__value">{{ meal.carbs }}g</text>
              </view>
            </view>
          </view>
        </view>

        <!-- 描述 -->
        <view class="description">
          <text class="description__text">{{ meal.description }}</text>
        </view>

        <!-- 食材清单 -->
        <view class="ingredients">
          <text class="ingredients__title">食材清单</text>
          <scroll-view scroll-x class="ingredients__list" :enhanced="true" :show-scrollbar="false">
            <view
              v-for="(item, idx) in meal.ingredients"
              :key="idx"
              class="ingredient-chip"
            >
              <u-icon name="checkbox-mark" size="24" color="#0a6e2c" />
              <text class="ingredient-chip__name">{{ item.name }}</text>
            </view>
          </scroll-view>
        </view>
      </view>

      <!-- 底部操作栏 -->
      <view class="action-bar safe-bottom">
        <view class="action-bar__price">
          <text class="action-bar__price-current">¥{{ formatPrice(meal.price) }}</text>
          <text v-if="meal.originalPrice" class="action-bar__price-original">
            ¥{{ formatPrice(meal.originalPrice) }}
          </text>
        </view>
        <view class="action-bar__buttons">
          <u-button text="加入购物车" shape="circle" size="normal" color="#e8f5e9" :customStyle="{color: '#0a6e2c', fontWeight: '600'}" @click="addToCart" />
          <u-button text="立即下单" type="primary" shape="circle" size="normal" color="#0a6e2c" @click="buyNow" />
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
  padding-bottom: 140rpx;
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

// 英雄区域
.hero {
  position: relative;
  width: 100%;
  overflow: hidden;

  &__overlay {
    position: absolute;
    bottom: 0;
    left: 0;
    right: 0;
    height: 50%;
    background: linear-gradient(to top, rgba(0, 0, 0, 0.5), transparent);
  }

  &__content {
    position: absolute;
    bottom: 32rpx;
    left: 32rpx;
    right: 32rpx;
  }

  &__badge {
    display: inline-block;
    background: $primary;
    padding: 6rpx 16rpx;
    border-radius: $radius-sm;
    margin-bottom: 12rpx;

    &-text {
      font-size: $font-xs;
      color: $on-primary;
      font-weight: 600;
    }
  }

  &__title {
    display: block;
    font-size: $font-3xl;
    font-weight: 800;
    color: #fff;
  }
}

.info-section {
  padding: 32rpx 24rpx;
}

// 营养成分Bento Grid
.nutrition-grid {
  display: flex;
  gap: 16rpx;
  margin-bottom: 32rpx;
}

.nutrition-card {
  background: $surface-container-low;
  border-radius: $radius-lg;
  padding: 24rpx;

  &--energy {
    flex: 4;
  }

  &--macros {
    flex: 5;
  }

  &__label {
    display: block;
    font-size: $font-xs;
    color: $on-surface-variant;
    font-weight: 600;
    text-transform: uppercase;
    letter-spacing: 0.05em;
    margin-bottom: 12rpx;
  }

  &__value-row {
    display: flex;
    align-items: baseline;
    gap: 4rpx;
    margin-bottom: 16rpx;
  }

  &__value {
    font-size: $font-4xl;
    font-weight: 800;
    color: $primary;
    line-height: 1;
  }

  &__unit {
    font-size: $font-sm;
    color: $on-surface-variant;
    font-weight: 500;
  }

  &__bar {
    height: 8rpx;
    background: $surface-container;
    border-radius: $radius-full;
    overflow: hidden;
    margin-bottom: 8rpx;

    &-fill {
      height: 100%;
      background: $primary;
      border-radius: $radius-full;
      transition: width $transition-slow;
    }
  }

  &__goal {
    font-size: 20rpx;
    color: $on-surface-variant;
  }
}

// 宏量素柱状图
.macros-bars {
  display: flex;
  justify-content: space-around;
  align-items: flex-end;
  height: 200rpx;
  gap: 16rpx;
}

.macros-bar {
  display: flex;
  flex-direction: column;
  align-items: center;
  flex: 1;

  &__track {
    width: 100%;
    height: 140rpx;
    background: $surface-container;
    border-radius: $radius-md;
    overflow: hidden;
    display: flex;
    align-items: flex-end;
    margin-bottom: 8rpx;
  }

  &__fill {
    width: 100%;
    border-radius: $radius-md;
    transition: height $transition-slow;

    &--protein { background: $primary; }
    &--fat { background: $tertiary; }
    &--carbs { background: $secondary; }
  }

  &__label {
    font-size: 20rpx;
    color: $on-surface-variant;
    margin-bottom: 4rpx;
  }

  &__value {
    font-size: $font-sm;
    font-weight: 700;
    color: $on-surface;
  }
}

// 描述
.description {
  margin-bottom: 32rpx;

  &__text {
    font-size: $font-base;
    color: $on-surface-variant;
    line-height: $leading-relaxed;
  }
}

// 食材清单
.ingredients {
  margin-bottom: 32rpx;

  &__title {
    display: block;
    font-size: $font-lg;
    font-weight: 700;
    color: $on-surface;
    margin-bottom: 20rpx;
  }

  &__list {
    white-space: nowrap;
  }
}

.ingredient-chip {
  display: inline-flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  width: 160rpx;
  height: 120rpx;
  background: $surface-container-low;
  border-radius: $radius-lg;
  margin-right: 16rpx;

  &__icon {
    font-size: 48rpx;
    margin-bottom: 8rpx;
  }

  &__name {
    font-size: $font-xs;
    color: $on-surface;
  }
}

// 底部操作栏
.action-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16rpx 24rpx;
  background: rgba(255, 255, 255, 0.9);
  background: rgba(255, 255, 255, 0.95);
  z-index: 50;

  &__price {
    display: flex;
    align-items: baseline;
    gap: 8rpx;

    &-current {
      font-size: $font-2xl;
      font-weight: 800;
      color: $primary;
    }

    &-original {
      font-size: $font-sm;
      color: $on-surface-variant;
      text-decoration: line-through;
    }
  }

  &__buttons {
    display: flex;
    gap: 16rpx;
  }

  &__btn {
    padding: 16rpx 32rpx;
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
      transform: scale(0.95);
    }
  }
}
</style>
