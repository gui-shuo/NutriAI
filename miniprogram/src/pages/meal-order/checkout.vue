<template>
  <view class="page">
    <!-- Order Items -->
    <view class="section">
      <text class="section-title">订单明细</text>
      <view class="order-items">
        <view class="order-item" v-for="item in cartItems" :key="item.id">
          <image class="item-img" :src="item.imageUrl || '/static/images/meal-placeholder.png'" mode="aspectFill" />
          <view class="item-info">
            <text class="item-name">{{ item.name }}</text>
            <view class="item-row">
              <text class="item-price">¥{{ item.price }}</text>
              <view class="qty-control">
                <view class="qty-btn" @tap="changeQty(item, -1)">
                  <text class="qty-btn-text">-</text>
                </view>
                <text class="qty-val">{{ item.quantity }}</text>
                <view class="qty-btn" @tap="changeQty(item, 1)">
                  <text class="qty-btn-text">+</text>
                </view>
              </view>
            </view>
          </view>
        </view>
      </view>
      <view v-if="!cartItems.length" class="empty-cart">
        <text class="empty-text">购物车为空</text>
      </view>
    </view>

    <!-- Delivery Method -->
    <view class="section">
      <text class="section-title">取餐方式</text>
      <view class="method-options">
        <view
          class="method-item"
          :class="{ active: deliveryMethod === 'pickup' }"
          @tap="deliveryMethod = 'pickup'"
        >
          <text class="method-icon">🏪</text>
          <text class="method-label">到店自取</text>
        </view>
        <view
          class="method-item disabled"
          :class="{ active: deliveryMethod === 'delivery' }"
        >
          <text class="method-icon">🚗</text>
          <view class="method-info">
            <text class="method-label">外卖配送</text>
            <text class="method-tip">暂未开放</text>
          </view>
        </view>
      </view>
    </view>

    <!-- Pickup Time -->
    <view class="section" v-if="deliveryMethod === 'pickup'">
      <text class="section-title">取餐时间</text>
      <scroll-view class="time-scroll" scroll-x :show-scrollbar="false">
        <view class="time-list">
          <view
            v-for="slot in timeSlots"
            :key="slot"
            class="time-slot"
            :class="{ active: selectedTime === slot }"
            @tap="selectedTime = slot"
          >
            <text>{{ slot }}</text>
          </view>
        </view>
      </scroll-view>
    </view>

    <!-- Notes -->
    <view class="section">
      <text class="section-title">备注</text>
      <textarea
        class="notes-input"
        v-model="orderNotes"
        placeholder="如有特殊要求请备注（如过敏原、忌口等）"
        :maxlength="200"
      />
    </view>

    <!-- Bottom spacer -->
    <view style="height: 200rpx;" />

    <!-- Bottom Bar -->
    <view class="bottom-bar">
      <view class="total-section">
        <text class="total-label">合计</text>
        <text class="total-price">¥{{ totalPrice }}</text>
        <text class="total-count">共{{ totalCount }}件</text>
      </view>
      <view class="submit-btn" :class="{ disabled: submitting || !cartItems.length }" @tap="submitOrder">
        <text class="submit-text">{{ submitting ? '提交中...' : '确认下单' }}</text>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { mealApi } from '@/services/api'

const CART_KEY = 'nutriai_meal_cart'

const cartItems = ref<any[]>([])
const deliveryMethod = ref('pickup')
const selectedTime = ref('')
const orderNotes = ref('')
const submitting = ref(false)

const timeSlots = (() => {
  const slots: string[] = []
  const now = new Date()
  let h = now.getHours()
  let m = now.getMinutes()
  // Start from next half hour
  m = m < 30 ? 30 : 0
  if (m === 0) h += 1
  for (let i = 0; i < 8; i++) {
    if (h > 20) break
    const hh = String(h).padStart(2, '0')
    const mm = String(m).padStart(2, '0')
    slots.push(`${hh}:${mm}`)
    m += 30
    if (m >= 60) { m = 0; h += 1 }
  }
  if (!slots.length) {
    slots.push('11:00', '11:30', '12:00', '12:30', '17:00', '17:30', '18:00', '18:30')
  }
  return slots
})()

const totalPrice = computed(() => {
  return cartItems.value.reduce((s, c) => s + Number(c.price) * c.quantity, 0).toFixed(2)
})

const totalCount = computed(() => {
  return cartItems.value.reduce((s, c) => s + c.quantity, 0)
})

function loadCart() {
  cartItems.value = JSON.parse(uni.getStorageSync(CART_KEY) || '[]')
}

function saveCart() {
  uni.setStorageSync(CART_KEY, JSON.stringify(cartItems.value))
}

function changeQty(item: any, delta: number) {
  const next = item.quantity + delta
  if (next <= 0) {
    cartItems.value = cartItems.value.filter(c => c.id !== item.id)
  } else {
    item.quantity = next
  }
  saveCart()
}

async function submitOrder() {
  if (submitting.value || !cartItems.value.length) return

  if (deliveryMethod.value === 'pickup' && !selectedTime.value) {
    uni.showToast({ title: '请选择取餐时间', icon: 'none' })
    return
  }

  submitting.value = true
  try {
    const orderData = {
      items: cartItems.value.map(c => ({ mealId: c.id, quantity: c.quantity })),
      deliveryMethod: deliveryMethod.value,
      pickupTime: selectedTime.value,
      notes: orderNotes.value
    }

    const res = await mealApi.createOrder(orderData)
    if (res.code === 200 && res.data) {
      const orderNo = res.data.orderNo || res.data.id
      // Clear cart
      uni.setStorageSync(CART_KEY, '[]')
      cartItems.value = []

      // Simulate payment
      try {
        await mealApi.simulatePay(orderNo)
      } catch {
        // payment simulation is optional
      }

      uni.showToast({ title: '下单成功', icon: 'success' })
      setTimeout(() => {
        uni.navigateBack()
      }, 1000)
    } else {
      uni.showToast({ title: res.message || '下单失败', icon: 'none' })
    }
  } catch (e: any) {
    console.error('Submit order error:', e)
    uni.showToast({ title: '下单失败，请重试', icon: 'none' })
  } finally {
    submitting.value = false
  }
}

onShow(() => {
  loadCart()
  if (timeSlots.length && !selectedTime.value) {
    selectedTime.value = timeSlots[0]
  }
})
</script>

<style scoped lang="scss">
.page {
  min-height: 100vh;
  background: #F5F7FA;
  padding: 32rpx;
}

.section {
  background: #fff;
  border-radius: 24rpx;
  padding: 28rpx;
  margin-bottom: 24rpx;
  box-shadow: 0 2rpx 12rpx rgba(0,0,0,0.04);
}

.section-title {
  display: block;
  font-size: 30rpx;
  font-weight: 600;
  color: #1A1A2E;
  margin-bottom: 24rpx;
}

/* Order Items */
.order-items {
  display: flex;
  flex-direction: column;
  gap: 20rpx;
}
.order-item {
  display: flex;
  gap: 20rpx;
  padding-bottom: 20rpx;
  border-bottom: 1rpx solid #EAEFF5;

  &:last-child {
    border-bottom: none;
    padding-bottom: 0;
  }
}
.item-img {
  width: 140rpx; height: 140rpx;
  border-radius: 16rpx;
  background: #F0F4F8;
  flex-shrink: 0;
}
.item-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}
.item-name {
  font-size: 28rpx;
  font-weight: 600;
  color: #1A1A2E;
}
.item-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.item-price {
  font-size: 30rpx;
  font-weight: 700;
  color: #EF4444;
}
.qty-control {
  display: flex;
  align-items: center;
  gap: 16rpx;
}
.qty-btn {
  width: 48rpx; height: 48rpx;
  border-radius: 50%;
  background: #F0F4F8;
  display: flex;
  align-items: center;
  justify-content: center;

  &:active { opacity: 0.7; }
}
.qty-btn-text {
  font-size: 28rpx;
  font-weight: 600;
  color: #1A1A2E;
  line-height: 1;
}
.qty-val {
  font-size: 28rpx;
  font-weight: 600;
  color: #1A1A2E;
  min-width: 40rpx;
  text-align: center;
}

.empty-cart {
  text-align: center;
  padding: 40rpx 0;
}
.empty-text {
  font-size: 28rpx;
  color: #8896AB;
}

/* Delivery Method */
.method-options {
  display: flex;
  gap: 20rpx;
}
.method-item {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 16rpx;
  padding: 24rpx;
  border-radius: 24rpx;
  background: #F0F4F8;
  border: 2rpx solid transparent;

  &.active {
    border-color: #10B981;
    background: #ECFDF5;
  }

  &.disabled {
    opacity: 0.5;
  }
}
.method-icon {
  font-size: 40rpx;
}
.method-info {
  display: flex;
  flex-direction: column;
}
.method-label {
  font-size: 28rpx;
  font-weight: 500;
  color: #1A1A2E;
}
.method-tip {
  font-size: 22rpx;
  color: #8896AB;
  margin-top: 4rpx;
}

/* Time Slots */
.time-scroll {
  white-space: nowrap;
}
.time-list {
  display: inline-flex;
  gap: 16rpx;
}
.time-slot {
  padding: 16rpx 32rpx;
  border-radius: 48rpx;
  background: #F0F4F8;
  font-size: 26rpx;
  color: #8896AB;
  font-weight: 500;

  &.active {
    background: linear-gradient(135deg, #10B981, #059669);
    color: #fff;
  }

  &:active { opacity: 0.7; }
}

/* Notes */
.notes-input {
  width: 100%;
  height: 160rpx;
  background: #F0F4F8;
  border-radius: 16rpx;
  padding: 20rpx;
  font-size: 26rpx;
  color: #1A1A2E;
  box-sizing: border-box;
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
.total-section {
  display: flex;
  align-items: baseline;
  gap: 8rpx;
}
.total-label {
  font-size: 26rpx;
  color: #8896AB;
}
.total-price {
  font-size: 40rpx;
  font-weight: 700;
  color: #EF4444;
}
.total-count {
  font-size: 22rpx;
  color: #8896AB;
}
.submit-btn {
  background: linear-gradient(135deg, #10B981, #059669);
  padding: 22rpx 48rpx;
  border-radius: 48rpx;
  box-shadow: 0 4rpx 16rpx rgba(16,185,129,0.3);

  &:active { opacity: 0.8; }

  &.disabled {
    opacity: 0.5;
  }
}
.submit-text {
  font-size: 30rpx;
  font-weight: 600;
  color: #fff;
}
</style>
