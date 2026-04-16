<template>
  <view class="page">
    <!-- Loading -->
    <view v-if="loading" class="loading-page">
      <text class="loading-text">加载中...</text>
    </view>

    <template v-else>
      <!-- Top Image -->
      <image class="hero-img" :src="meal.imageUrl || '/static/images/meal-placeholder.png'" mode="aspectFill" />

      <view class="info-section">
        <!-- Name & Price -->
        <view class="title-row">
          <text class="meal-name">{{ meal.name }}</text>
          <text class="meal-price">¥{{ meal.price }}</text>
        </view>

        <!-- Tags -->
        <view class="tags-row" v-if="meal.tags?.length">
          <text class="tag" v-for="tag in meal.tags" :key="tag">{{ tag }}</text>
        </view>

        <!-- Description -->
        <text class="meal-desc">{{ meal.description || '新鲜现做，科学配比，营养均衡' }}</text>

        <!-- Nutrition Info -->
        <view class="nutrition-card">
          <text class="card-title">营养信息</text>
          <view class="nutrition-grid">
            <view class="nutrition-item">
              <text class="nutrition-val">{{ meal.calories || '--' }}</text>
              <text class="nutrition-label">热量(kcal)</text>
            </view>
            <view class="nutrition-item">
              <text class="nutrition-val">{{ meal.protein || '--' }}</text>
              <text class="nutrition-label">蛋白质(g)</text>
            </view>
            <view class="nutrition-item">
              <text class="nutrition-val">{{ meal.fat || '--' }}</text>
              <text class="nutrition-label">脂肪(g)</text>
            </view>
            <view class="nutrition-item">
              <text class="nutrition-val">{{ meal.carbs || '--' }}</text>
              <text class="nutrition-label">碳水(g)</text>
            </view>
          </view>
        </view>

        <!-- Allergen Info -->
        <view class="info-card" v-if="meal.allergens">
          <text class="card-title">⚠️ 过敏原信息</text>
          <text class="card-text">{{ meal.allergens }}</text>
        </view>

        <!-- Ingredients -->
        <view class="info-card" v-if="meal.ingredients?.length">
          <text class="card-title">🥬 食材清单</text>
          <view class="ingredient-list">
            <text class="ingredient" v-for="(ing, idx) in meal.ingredients" :key="idx">{{ ing }}</text>
          </view>
        </view>
      </view>

      <!-- Bottom spacer for fixed bar -->
      <view style="height: 180rpx;" />

      <!-- Bottom Action Bar -->
      <view class="bottom-bar">
        <view class="qty-selector">
          <view class="qty-btn" @tap="changeQty(-1)">
            <text class="qty-btn-text">-</text>
          </view>
          <text class="qty-val">{{ quantity }}</text>
          <view class="qty-btn" @tap="changeQty(1)">
            <text class="qty-btn-text">+</text>
          </view>
        </view>
        <view class="add-cart-btn" @tap="handleAddToCart">
          <text class="add-cart-text">加入购物车 · ¥{{ subtotal }}</text>
        </view>
      </view>
    </template>
  </view>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { mealApi } from '@/services/api'

const loading = ref(true)
const quantity = ref(1)
const meal = ref<any>({})

const mockMeals: Record<string, any> = {
  '1': { id: 1, name: '地中海抗炎沙拉', price: '28.00', calories: 380, protein: 15, fat: 18, carbs: 30, tags: ['抗炎', '低GI'], description: '新鲜蔬菜搭配特级初榨橄榄油，富含多种抗氧化成分和膳食纤维', allergens: '可能含有坚果', ingredients: ['混合生菜', '樱桃番茄', '紫甘蓝', '鹰嘴豆', '牛油果', '特级初榨橄榄油', '柠檬汁'], imageUrl: '' },
  '2': { id: 2, name: '三文鱼藜麦碗', price: '42.00', calories: 520, protein: 35, fat: 22, carbs: 45, tags: ['高蛋白', '抗炎'], description: '挪威三文鱼搭配有机藜麦，富含Omega-3脂肪酸', allergens: '含有鱼类', ingredients: ['挪威三文鱼', '有机藜麦', '毛豆', '紫甘蓝', '牛油果', '芝麻酱'], imageUrl: '' },
  '3': { id: 3, name: '姜黄鸡胸套餐', price: '35.00', calories: 450, protein: 40, fat: 12, carbs: 42, tags: ['抗炎', '增肌'], description: '姜黄腌制鸡胸肉，搭配糙米和时蔬，高蛋白低脂肪', ingredients: ['姜黄鸡胸肉', '糙米饭', '西兰花', '胡萝卜', '菠菜'], imageUrl: '' },
  '4': { id: 4, name: '牛油果虾仁沙拉', price: '38.00', calories: 420, protein: 28, fat: 20, carbs: 35, tags: ['低GI', '高蛋白'], description: '新鲜牛油果搭配大虾仁，健康轻食之选', allergens: '含有甲壳类', ingredients: ['大虾仁', '牛油果', '混合沙拉菜', '樱桃番茄', '玉米粒', '橄榄油'], imageUrl: '' },
  '5': { id: 5, name: '番茄牛腩汤', price: '32.00', calories: 350, protein: 25, fat: 15, carbs: 28, tags: ['抗炎', '暖胃'], description: '慢炖番茄牛腩，营养滋补暖身', ingredients: ['牛腩', '番茄', '土豆', '胡萝卜', '洋葱', '姜'], imageUrl: '' },
  '6': { id: 6, name: '低GI杂粮饭套餐', price: '26.00', calories: 400, protein: 18, fat: 10, carbs: 58, tags: ['低GI', '高纤维'], description: '五谷杂粮饭搭配清蒸时蔬，低升糖健康选择', ingredients: ['糙米', '燕麦', '红豆', '西兰花', '南瓜', '秋葵'], imageUrl: '' }
}

const subtotal = computed(() => {
  const price = Number(meal.value.price) || 0
  return (price * quantity.value).toFixed(2)
})

function changeQty(delta: number) {
  const next = quantity.value + delta
  if (next >= 1 && next <= 99) {
    quantity.value = next
  }
}

function handleAddToCart() {
  const CART_KEY = 'nutriai_meal_cart'
  const cart = JSON.parse(uni.getStorageSync(CART_KEY) || '[]')
  const existing = cart.find((c: any) => c.id === meal.value.id)
  if (existing) {
    existing.quantity += quantity.value
  } else {
    cart.push({
      id: meal.value.id,
      name: meal.value.name,
      price: meal.value.price,
      imageUrl: meal.value.imageUrl,
      quantity: quantity.value
    })
  }
  uni.setStorageSync(CART_KEY, JSON.stringify(cart))
  uni.showToast({ title: '已加入购物车', icon: 'success' })
  setTimeout(() => uni.navigateBack(), 600)
}

onLoad((query: any) => {
  const id = query?.id
  if (!id) {
    uni.showToast({ title: '参数错误', icon: 'none' })
    setTimeout(() => uni.navigateBack(), 500)
    return
  }
  fetchDetail(id)
})

async function fetchDetail(id: string) {
  loading.value = true
  try {
    const res = await mealApi.getDetail(id)
    if (res.code === 200 && res.data) {
      meal.value = res.data
    } else {
      uni.showToast({ title: res.message || '餐品不存在', icon: 'none' })
      setTimeout(() => uni.navigateBack(), 1200)
    }
  } catch {
    uni.showToast({ title: '加载失败，请重试', icon: 'none' })
    setTimeout(() => uni.navigateBack(), 1200)
  } finally {
    loading.value = false
  }
}
</script>

<style scoped lang="scss">
.page {
  min-height: 100vh;
  background: #F5F7FA;
}

.loading-page {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 60vh;
}
.loading-text {
  font-size: 28rpx;
  color: #8896AB;
}

.hero-img {
  width: 100%;
  height: 480rpx;
  background: #F0F4F8;
}

.info-section {
  padding: 32rpx;
}

.title-row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: 16rpx;
}
.meal-name {
  font-size: 40rpx;
  font-weight: 700;
  color: #1A1A2E;
  flex: 1;
  margin-right: 16rpx;
}
.meal-price {
  font-size: 40rpx;
  font-weight: 700;
  color: #EF4444;
  flex-shrink: 0;
}

.tags-row {
  display: flex;
  gap: 12rpx;
  margin-bottom: 20rpx;
  flex-wrap: wrap;
}
.tag {
  font-size: 22rpx;
  color: #059669;
  background: #ECFDF5;
  padding: 6rpx 18rpx;
  border-radius: 48rpx;
  font-weight: 500;
}

.meal-desc {
  font-size: 28rpx;
  color: #8896AB;
  line-height: 1.7;
  margin-bottom: 32rpx;
}

/* Nutrition Card */
.nutrition-card {
  background: #fff;
  border-radius: 24rpx;
  padding: 28rpx;
  margin-bottom: 24rpx;
  box-shadow: 0 2rpx 12rpx rgba(0,0,0,0.04);
  border-top: 6rpx solid #10B981;
}
.card-title {
  display: block;
  font-size: 30rpx;
  font-weight: 600;
  color: #1A1A2E;
  margin-bottom: 20rpx;
}
.nutrition-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16rpx;
  text-align: center;
}
.nutrition-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6rpx;
}
.nutrition-val {
  font-size: 34rpx;
  font-weight: 700;
  color: #10B981;
}
.nutrition-label {
  font-size: 22rpx;
  color: #8896AB;
}

/* Info Card */
.info-card {
  background: #fff;
  border-radius: 24rpx;
  padding: 28rpx;
  margin-bottom: 24rpx;
  box-shadow: 0 2rpx 12rpx rgba(0,0,0,0.04);
}
.card-text {
  font-size: 26rpx;
  color: #8896AB;
  line-height: 1.6;
}

.ingredient-list {
  display: flex;
  flex-wrap: wrap;
  gap: 12rpx;
}
.ingredient {
  font-size: 24rpx;
  color: #1A1A2E;
  background: #F0F4F8;
  padding: 8rpx 20rpx;
  border-radius: 48rpx;
}

/* Bottom Bar */
.bottom-bar {
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

.qty-selector {
  display: flex;
  align-items: center;
  gap: 20rpx;
}
.qty-btn {
  width: 60rpx; height: 60rpx;
  border-radius: 50%;
  background: #F0F4F8;
  display: flex;
  align-items: center;
  justify-content: center;

  &:active { opacity: 0.7; }
}
.qty-btn-text {
  font-size: 36rpx;
  font-weight: 600;
  color: #1A1A2E;
  line-height: 1;
}
.qty-val {
  font-size: 34rpx;
  font-weight: 600;
  color: #1A1A2E;
  min-width: 48rpx;
  text-align: center;
}

.add-cart-btn {
  flex: 1;
  margin-left: 32rpx;
  background: linear-gradient(135deg, #10B981, #059669);
  padding: 22rpx 0;
  border-radius: 48rpx;
  text-align: center;
  box-shadow: 0 4rpx 16rpx rgba(16,185,129,0.3);

  &:active { opacity: 0.8; }
}
.add-cart-text {
  font-size: 30rpx;
  font-weight: 600;
  color: #fff;
}
</style>
