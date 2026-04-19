<script setup>
/**
 * 营养餐下单/结算页
 */
import { ref, computed, onMounted, onUnmounted } from 'vue'
import NavBar from '../../components/NavBar.vue'
import { mealApi } from '../../services/api'
import { useUserStore } from '../../stores/user'
import { formatPrice, cosUrl, checkLogin } from '../../utils/common'

const userStore = useUserStore()

const mealId = ref('')
const meal = ref(null)
const quantity = ref(1)
const pickupTime = ref('')
const remark = ref('')
const loading = ref(false)
const submitting = ref(false)

// 门店选择
const selectedStore = ref(null)

const totalPrice = computed(() => {
  if (!meal.value) return 0
  return meal.value.price * quantity.value
})

// 取餐时间选项
const timeSlots = ref([
  '11:30 - 12:00',
  '12:00 - 12:30',
  '12:30 - 13:00',
  '17:30 - 18:00',
  '18:00 - 18:30',
])

onMounted(() => {
  const pages = getCurrentPages()
  const page = pages[pages.length - 1]
  mealId.value = page.options?.id || ''
  fetchDetail()

  // 监听门店选择事件
  uni.$on('store-selected', onStoreSelected)
})

onUnmounted(() => {
  uni.$off('store-selected', onStoreSelected)
})

function onStoreSelected(store) {
  selectedStore.value = store
}

async function fetchDetail() {
  loading.value = true
  try {
    const res = await mealApi.getDetail(mealId.value)
    const ni = res.nutritionInfo || {}
    meal.value = {
      ...res,
      image: res.imageUrl || res.image || '',
      price: res.salePrice ?? res.price,
      calories: ni.calories ?? res.calories ?? 0,
    }
  } catch (e) {
    console.error('获取餐品详情失败', e)
    uni.showToast({ title: '加载失败', icon: 'none' })
  } finally {
    loading.value = false
  }
}

function selectTime(t) {
  pickupTime.value = t
}

function goSelectStore() {
  uni.navigateTo({ url: '/pages/meal/store-select' })
}

async function submitOrder() {
  if (!checkLogin()) return
  if (!selectedStore.value) {
    uni.showToast({ title: '请选择取餐门店', icon: 'none' })
    return
  }
  if (!pickupTime.value) {
    uni.showToast({ title: '请选择取餐时间', icon: 'none' })
    return
  }
  submitting.value = true
  try {
    await mealApi.createOrder({
      items: [{ mealItemId: Number(mealId.value), quantity: quantity.value }],
      fulfillmentType: 'PICKUP',
      pickupTime: pickupTime.value,
      pickupLocation: selectedStore.value.address || selectedStore.value.name,
      merchantId: selectedStore.value.id,
      merchantName: selectedStore.value.name,
      remark: remark.value,
    })
    uni.showToast({ title: '下单成功', icon: 'success' })
    setTimeout(() => {
      uni.navigateBack()
    }, 1500)
  } catch (e) {
    uni.showToast({ title: e.message || '下单失败，请重试', icon: 'none' })
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <view class="page">
    <NavBar showBack title="确认订单" />

    <scroll-view scroll-y class="content" :enhanced="true" :show-scrollbar="false">
      <!-- 餐品信息 -->
      <view v-if="meal" class="meal-info">
        <u-image
          :src="cosUrl(meal.image) || '/static/images/meal-placeholder.png'"
          width="180rpx"
          height="180rpx"
          mode="aspectFill"
          radius="12"
          :lazy-load="true"
        />
        <view class="meal-info__text">
          <text class="meal-info__name">{{ meal.name }}</text>
          <text class="meal-info__calories">{{ meal.calories }} kcal</text>
          <text class="meal-info__price">¥{{ formatPrice(meal.price) }}</text>
        </view>
      </view>

      <!-- 门店选择 -->
      <view class="section store-section" @tap="goSelectStore">
        <view class="store-section__left">
          <u-icon name="home" size="20" color="#0a6e2c" />
          <view class="store-section__info">
            <text class="store-section__label">取餐门店</text>
            <text v-if="selectedStore" class="store-section__name">{{ selectedStore.name }}</text>
            <text v-if="selectedStore" class="store-section__addr">{{ selectedStore.address }}</text>
            <text v-else class="store-section__placeholder">点击选择取餐门店</text>
          </view>
        </view>
        <view class="store-section__right">
          <text v-if="selectedStore && selectedStore.distanceText" class="store-section__dist">{{ selectedStore.distanceText }}</text>
          <u-icon name="arrow-right" size="16" color="#ccc" />
        </view>
      </view>

      <!-- 数量 -->
      <view class="section">
        <text class="section__title">数量</text>
        <u-number-box v-model="quantity" :min="1" :max="99" buttonSize="30" bgColor="#f5f5f5" />
      </view>

      <!-- 取餐时间 -->
      <view class="section">
        <text class="section__title">取餐时间</text>
        <view class="time-slots">
          <u-tag
            v-for="(t, idx) in timeSlots"
            :key="idx"
            :text="t"
            :plain="pickupTime !== t"
            :color="pickupTime === t ? '#ffffff' : '#333'"
            :bgColor="pickupTime === t ? '#0a6e2c' : '#f5f5f5'"
            :borderColor="pickupTime === t ? '#0a6e2c' : '#e0e0e0'"
            size="medium"
            @click="selectTime(t)"
          />
        </view>
      </view>

      <!-- 备注 -->
      <view class="section">
        <text class="section__title">备注</text>
        <textarea
          class="remark-input"
          v-model="remark"
          placeholder="有什么特殊要求？（可选）"
          maxlength="200"
          :auto-height="true"
        />
      </view>

      <view style="height: 160rpx;" />
    </scroll-view>

    <!-- 底部下单栏 -->
    <view class="order-bar safe-bottom">
      <view class="order-bar__total">
        <text class="order-bar__label">合计</text>
        <text class="order-bar__price">¥{{ formatPrice(totalPrice) }}</text>
      </view>
      <u-button
        :text="submitting ? '提交中...' : '立即下单'"
        type="primary"
        shape="circle"
        color="#0a6e2c"
        :loading="submitting"
        :disabled="submitting"
        @click="submitOrder"
        :customStyle="{padding: '20rpx 48rpx'}"
      />
    </view>
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
  padding: 24rpx;
  height: calc(100vh - 100px);
}

// 餐品信息
.meal-info {
  display: flex;
  gap: 20rpx;
  padding: 24rpx;
  background: $surface-container-lowest;
  border-radius: $radius-xl;
  margin-bottom: 24rpx;

  &__image {
    width: 180rpx;
    height: 180rpx;
    border-radius: $radius-lg;
    flex-shrink: 0;
  }

  &__text {
    flex: 1;
    display: flex;
    flex-direction: column;
    justify-content: center;
  }

  &__name {
    font-size: $font-lg;
    font-weight: 700;
    color: $on-surface;
    margin-bottom: 8rpx;
  }

  &__calories {
    font-size: $font-sm;
    color: $on-surface-variant;
    margin-bottom: 12rpx;
  }

  &__price {
    font-size: $font-xl;
    font-weight: 800;
    color: $primary;
  }
}

// 通用段落
.section {
  background: $surface-container-lowest;
  border-radius: $radius-xl;
  padding: 24rpx;
  margin-bottom: 20rpx;

  &__title {
    display: block;
    font-size: $font-base;
    font-weight: 600;
    color: $on-surface;
    margin-bottom: 16rpx;
  }
}

// 门店选择
.store-section {
  display: flex;
  align-items: center;
  justify-content: space-between;

  &__left {
    display: flex;
    align-items: flex-start;
    gap: 16rpx;
    flex: 1;
    min-width: 0;
  }

  &__info {
    flex: 1;
    min-width: 0;
  }

  &__label {
    display: block;
    font-size: 24rpx;
    color: #999;
    margin-bottom: 6rpx;
  }

  &__name {
    display: block;
    font-size: 28rpx;
    font-weight: 600;
    color: $on-surface;
    margin-bottom: 4rpx;
  }

  &__addr {
    display: block;
    font-size: 24rpx;
    color: #888;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  &__placeholder {
    font-size: 28rpx;
    color: #bbb;
  }

  &__right {
    display: flex;
    align-items: center;
    gap: 8rpx;
    flex-shrink: 0;
  }

  &__dist {
    font-size: 24rpx;
    color: $primary;
    font-weight: 600;
  }
}

// 数量控制
.qty-control {
  display: flex;
  align-items: center;
  gap: 24rpx;
}

.qty-btn {
  width: 64rpx;
  height: 64rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  background: $surface-container-low;
  border-radius: $radius-full;
  font-size: 36rpx;
  color: $on-surface;

  &:active {
    background: $surface-container;
  }
}

.qty-value {
  font-size: $font-xl;
  font-weight: 700;
  color: $on-surface;
  min-width: 60rpx;
  text-align: center;
}

// 取餐时间
.time-slots {
  display: flex;
  flex-wrap: wrap;
  gap: 12rpx;
}

.time-slot {
  padding: 14rpx 24rpx;
  background: $surface-container-low;
  border-radius: $radius-lg;

  &--active {
    background: $primary-container;
  }

  &__text {
    font-size: $font-sm;
    color: $on-surface;
  }
}

// 备注
.remark-input {
  width: 100%;
  font-size: $font-base;
  color: $on-surface;
  background: $surface-container-low;
  border-radius: $radius-lg;
  padding: 16rpx 20rpx;
  box-sizing: border-box;
}

// 底部下单栏
.order-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16rpx 24rpx;
  background: rgba(255, 255, 255, 0.95);
  background: rgba(255, 255, 255, 0.95);
  z-index: 50;

  &__total {
    display: flex;
    align-items: baseline;
    gap: 8rpx;
  }

  &__label {
    font-size: $font-base;
    color: $on-surface-variant;
  }

  &__price {
    font-size: $font-2xl;
    font-weight: 800;
    color: $primary;
  }

  &__btn {
    padding: 20rpx 48rpx;
    background: $primary;
    color: $on-primary;
    border-radius: $radius-lg;
    font-size: $font-base;
    font-weight: 600;

    &--disabled {
      opacity: 0.5;
      pointer-events: none;
    }

    &:active {
      transform: scale(0.96);
    }
  }
}
</style>
