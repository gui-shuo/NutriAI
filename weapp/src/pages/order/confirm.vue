<template>
  <view class="confirm-page">
    <scroll-view scroll-y class="page-scroll">
      <!-- Address Section -->
      <view class="address-section" @tap="chooseAddress">
        <view v-if="selectedAddress" class="address-card card">
          <view class="addr-top">
            <text class="text-bold">{{ selectedAddress.receiverName }}</text>
            <text class="text-secondary ml-sm">{{ selectedAddress.receiverPhone }}</text>
          </view>
          <text class="addr-detail text-sm text-secondary mt-sm">
            {{ selectedAddress.province }}{{ selectedAddress.city }}{{ selectedAddress.district }}{{ selectedAddress.detailAddress }}
          </text>
        </view>
        <view v-else class="address-empty card flex-center">
          <text class="text-muted">📍 请选择收货地址</text>
          <text class="text-primary text-sm ml-sm">去添加 ›</text>
        </view>
      </view>

      <!-- Order Items -->
      <view class="items-section card">
        <text class="section-title">订单商品</text>
        <view v-for="item in orderItems" :key="item.productId" class="order-item">
          <image :src="item.imageUrl || '/static/images/product-default.png'" mode="aspectFill" class="item-img" />
          <view class="item-info">
            <text class="item-name text-ellipsis">{{ item.name }}</text>
            <view class="flex-between mt-sm">
              <text class="text-price">¥{{ item.price }}</text>
              <text class="text-muted text-sm">x{{ item.quantity }}</text>
            </view>
          </view>
        </view>
      </view>

      <!-- Remark -->
      <view class="card mt-md mx-lg">
        <view class="flex-between">
          <text class="text-bold">订单备注</text>
          <input v-model="remark" placeholder="选填，可备注取餐时间等" class="remark-input" />
        </view>
      </view>

      <!-- Payment Method -->
      <view class="card mt-md mx-lg">
        <text class="section-title">支付方式</text>
        <view class="pay-methods mt-md">
          <view
            v-for="m in payMethods"
            :key="m.key"
            :class="['pay-item', { active: payMethod === m.key }]"
            @tap="payMethod = m.key"
          >
            <text class="pay-icon">{{ m.icon }}</text>
            <text class="pay-label">{{ m.label }}</text>
            <text class="pay-check">{{ payMethod === m.key ? '✅' : '⬜' }}</text>
          </view>
        </view>
      </view>

      <view style="height: 160rpx;"></view>
    </scroll-view>

    <!-- Bottom Bar -->
    <view class="bottom-bar safe-bottom">
      <view class="total-info">
        <text class="text-sm text-secondary">共{{ totalCount }}件</text>
        <view class="flex items-end">
          <text class="text-sm">合计：</text>
          <text class="text-price text-xl">¥{{ totalPrice.toFixed(2) }}</text>
        </view>
      </view>
      <button class="submit-btn" :disabled="submitting" @tap="submitOrder">
        {{ submitting ? '提交中...' : '提交订单' }}
      </button>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { productApi, addressApi } from '../../services/api'

interface OrderItem {
  productId: number
  name: string
  price: number
  imageUrl: string
  quantity: number
}

const orderItems = ref<OrderItem[]>([])
const selectedAddress = ref<any>(null)
const remark = ref('')
const payMethod = ref('wxpay')
const submitting = ref(false)
const orderType = ref('')

const payMethods = [
  { key: 'wxpay', icon: '💬', label: '微信支付' },
  { key: 'simulate', icon: '🧪', label: '模拟支付（测试）' }
]

const totalCount = computed(() => orderItems.value.reduce((s, i) => s + i.quantity, 0))
const totalPrice = computed(() => orderItems.value.reduce((s, i) => s + i.price * i.quantity, 0))

onLoad((query: any) => {
  orderType.value = query?.type || ''

  if (query?.type === 'cart') {
    const items = uni.getStorageSync('nutriai_checkout_items')
    if (items) orderItems.value = JSON.parse(items)
  } else if (query?.productId) {
    orderItems.value = [{
      productId: Number(query.productId),
      name: decodeURIComponent(query.name || ''),
      price: Number(query.price || 0),
      imageUrl: decodeURIComponent(query.imageUrl || ''),
      quantity: 1
    }]
  }
})

onMounted(() => {
  loadDefaultAddress()
})

async function loadDefaultAddress() {
  try {
    const list: any = await addressApi.getList()
    const addresses = list?.content || list || []
    selectedAddress.value = addresses.find((a: any) => a.isDefault) || addresses[0] || null
  } catch {}
}

function chooseAddress() {
  uni.navigateTo({
    url: '/pages/address/index?select=1',
    events: {
      selectAddress: (addr: any) => {
        selectedAddress.value = addr
      }
    }
  })
}

async function submitOrder() {
  if (!selectedAddress.value) {
    return uni.showToast({ title: '请选择收货地址', icon: 'none' })
  }
  if (!orderItems.value.length) {
    return uni.showToast({ title: '订单商品为空', icon: 'none' })
  }

  submitting.value = true
  try {
    const addr = selectedAddress.value
    const orderData = {
      items: orderItems.value.map(i => ({
        productId: i.productId,
        quantity: i.quantity,
        price: i.price
      })),
      receiverName: addr.receiverName,
      receiverPhone: addr.receiverPhone,
      receiverAddress: `${addr.province}${addr.city}${addr.district}${addr.detailAddress}`,
      remark: remark.value
    }

    const res: any = await productApi.createOrder(orderData)
    const orderNo = res?.orderNo || res?.data?.orderNo || ''

    // If simulate payment selected, auto-pay
    if (payMethod.value === 'simulate' && orderNo) {
      try {
        await productApi.payOrder(orderNo)
      } catch {}
    }

    // WeChat Pay placeholder
    if (payMethod.value === 'wxpay' && orderNo) {
      // TODO: Integrate real WeChat Payment when approved
      // wx.requestPayment({ ... })
      // For now, use simulate
      try {
        await productApi.payOrder(orderNo)
      } catch {}
    }

    // Clear cart items if from cart
    if (orderType.value === 'cart') {
      uni.removeStorageSync('nutriai_checkout_items')
      // Remove purchased items from cart
      const cart = JSON.parse(uni.getStorageSync('nutriai_cart') || '[]')
      const purchasedIds = new Set(orderItems.value.map(i => i.productId))
      const remaining = cart.filter((c: any) => !purchasedIds.has(c.productId))
      uni.setStorageSync('nutriai_cart', JSON.stringify(remaining))
    }

    uni.redirectTo({ url: `/pages/order/result?orderNo=${orderNo}&status=success` })
  } catch (e: any) {
    uni.showToast({ title: e?.message || '下单失败', icon: 'none' })
  }
  submitting.value = false
}
</script>

<style lang="scss" scoped>
.confirm-page { min-height: 100vh; background: $bg-page; }
.page-scroll { height: 100vh; }

.address-section { padding: $spacing-md $spacing-lg; }
.address-card { padding: $spacing-lg; }
.addr-top { display: flex; align-items: center; }
.addr-detail { display: block; }
.address-empty { padding: $spacing-xl; }

.items-section { margin: 0 $spacing-lg; }
.order-item {
  display: flex; gap: $spacing-md; padding: $spacing-md 0;
  border-bottom: 1rpx solid $border-light;
  &:last-child { border-bottom: none; }
}
.item-img { width: 140rpx; height: 140rpx; border-radius: $radius-md; background: $bg-muted; flex-shrink: 0; }
.item-info { flex: 1; min-width: 0; }
.item-name { font-size: $font-base; color: $text-primary; }

.mx-lg { margin-left: $spacing-lg; margin-right: $spacing-lg; }

.remark-input { flex: 1; text-align: right; font-size: $font-sm; color: $text-secondary; }

.pay-methods {}
.pay-item {
  display: flex; align-items: center; padding: $spacing-md 0;
  border-bottom: 1rpx solid $border-light;
  &:last-child { border-bottom: none; }
  &.active { .pay-label { color: $primary; font-weight: 500; } }
}
.pay-icon { font-size: 36rpx; margin-right: $spacing-md; }
.pay-label { flex: 1; font-size: $font-base; }
.pay-check { font-size: 32rpx; }

.bottom-bar {
  position: fixed; bottom: 0; left: 0; right: 0;
  background: $bg-card; box-shadow: 0 -2rpx 10rpx rgba(0, 0, 0, 0.05);
  display: flex; align-items: center; justify-content: space-between;
  padding: $spacing-sm $spacing-lg; z-index: 100;
}

.total-info { display: flex; flex-direction: column; }

.submit-btn {
  padding: 0 48rpx; height: 80rpx; line-height: 80rpx;
  background: $gradient-primary; color: #fff; border-radius: $radius-xl;
  font-size: $font-md; font-weight: 600; border: none;
  &::after { border: none; }
  &[disabled] { opacity: 0.5; }
}
</style>
