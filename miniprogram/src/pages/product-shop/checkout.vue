<template>
  <view class="checkout-page">
    <!-- Address Section -->
    <view class="section">
      <view class="section-header">
        <text class="section-title">📍 收货地址</text>
        <text class="link-btn" @tap="goManageAddress">管理</text>
      </view>
      <view v-if="savedAddresses.length > 0">
        <view
          v-for="addr in savedAddresses"
          :key="addr.id"
          class="addr-card"
          :class="{ selected: selectedAddressId === addr.id }"
          @tap="selectedAddressId = addr.id; useManualAddress = false"
        >
          <view class="addr-radio">
            <view class="radio" :class="{ checked: selectedAddressId === addr.id }"></view>
          </view>
          <view class="addr-info">
            <view class="addr-name-row">
              <text class="addr-name">{{ addr.receiverName }}</text>
              <text class="addr-phone">{{ addr.receiverPhone }}</text>
              <view class="default-tag" v-if="addr.isDefault">默认</view>
            </view>
            <text class="addr-detail">{{ getFullAddress(addr) }}</text>
          </view>
        </view>
        <view class="manual-toggle" @tap="useManualAddress = !useManualAddress">
          <text>{{ useManualAddress ? '收起手动输入' : '+ 手动填写新地址' }}</text>
        </view>
      </view>

      <view v-if="savedAddresses.length === 0 || useManualAddress" class="manual-form">
        <view class="form-item">
          <text class="form-label">收货人</text>
          <input class="form-input" v-model="manualAddr.name" placeholder="收货人姓名" />
        </view>
        <view class="form-item">
          <text class="form-label">手机号</text>
          <input class="form-input" v-model="manualAddr.phone" type="number" placeholder="手机号码" maxlength="11" />
        </view>
        <view class="form-item">
          <text class="form-label">地址</text>
          <input class="form-input" v-model="manualAddr.detail" placeholder="详细地址" />
        </view>
        <view class="save-addr-row">
          <view class="checkbox-sm" :class="{ checked: saveAddr }" @tap="saveAddr = !saveAddr">
            <text v-if="saveAddr" class="check-icon-sm">✓</text>
          </view>
          <text class="save-addr-label">保存到地址簿</text>
        </view>
      </view>
    </view>

    <!-- Order Items -->
    <view class="section">
      <text class="section-title">🛍 订单商品</text>
      <view class="order-item" v-for="item in orderItems" :key="item.productId || item.id">
        <image class="item-image" :src="item.imageUrl || '/static/images/product-placeholder.png'" mode="aspectFill" />
        <view class="item-info">
          <text class="item-name">{{ item.name }}</text>
          <view class="item-row">
            <text class="item-price">¥{{ formatPrice(item.price) }}</text>
            <text class="item-qty">× {{ item.quantity }}</text>
          </view>
        </view>
        <text class="item-subtotal">¥{{ formatPrice(item.price * item.quantity) }}</text>
      </view>
    </view>

    <!-- Coupon Section -->
    <view class="section" @tap="showCouponPicker = true">
      <view class="coupon-row">
        <text class="section-title">🏷 优惠券</text>
        <view class="coupon-right">
          <text v-if="selectedCoupon" class="coupon-selected">-¥{{ discountAmount.toFixed(2) }}</text>
          <text v-else class="coupon-none">不使用</text>
          <text class="arrow">›</text>
        </view>
      </view>
    </view>

    <!-- Notes -->
    <view class="section">
      <text class="section-title">📝 备注</text>
      <textarea class="notes-input" v-model="notes" placeholder="如有特殊要求请在此备注" :maxlength="200" />
    </view>

    <!-- Price Summary -->
    <view class="section price-summary">
      <view class="price-row">
        <text class="price-label">商品合计</text>
        <text class="price-value">¥{{ subtotal.toFixed(2) }}</text>
      </view>
      <view class="price-row" v-if="selectedCoupon">
        <text class="price-label">优惠券抵扣</text>
        <text class="price-value discount">-¥{{ discountAmount.toFixed(2) }}</text>
      </view>
      <view class="divider"></view>
      <view class="price-row total-row">
        <text class="price-label total">应付金额</text>
        <text class="price-value total">¥{{ finalTotal.toFixed(2) }}</text>
      </view>
    </view>

    <view style="height: 160rpx;"></view>

    <!-- Bottom Bar -->
    <view class="bottom-bar">
      <view class="bottom-total">
        <text class="total-label">合计 </text>
        <text class="total-price">¥{{ finalTotal.toFixed(2) }}</text>
      </view>
      <view class="submit-btn" :class="{ disabled: submitting }" @tap="submitOrder">
        {{ submitting ? '提交中...' : '提交订单' }}
      </view>
    </view>

    <!-- Coupon Picker -->
    <view class="coupon-modal" v-if="showCouponPicker">
      <view class="modal-mask" @tap="showCouponPicker = false"></view>
      <view class="modal-body">
        <view class="modal-header">
          <text class="modal-title">选择优惠券</text>
          <text class="modal-close" @tap="showCouponPicker = false">✕</text>
        </view>
        <view class="coupon-no-use" @tap="selectCoupon(null)">
          <text>不使用优惠券</text>
        </view>
        <scroll-view class="coupon-list" scroll-y>
          <view
            v-for="uc in availableCoupons"
            :key="uc.id"
            class="coupon-item"
            :class="{ selected: selectedCoupon?.id === uc.id }"
            @tap="selectCoupon(uc)"
          >
            <view class="coupon-amount">
              <text v-if="uc.coupon?.couponType === 'REDUCE'">-¥{{ uc.coupon.discountValue }}</text>
              <text v-else>{{ (uc.coupon?.discountValue * 10).toFixed(1) }}折</text>
            </view>
            <view class="coupon-info">
              <text class="coupon-name">{{ uc.coupon?.name }}</text>
              <text class="coupon-cond">满{{ uc.coupon?.minOrderAmount }}元可用</text>
              <text class="coupon-exp">{{ formatDate(uc.expireTime) }} 到期</text>
            </view>
            <view class="check-mark" v-if="selectedCoupon?.id === uc.id">✓</view>
          </view>
          <view class="empty-coupons" v-if="availableCoupons.length === 0">
            <text>暂无可用优惠券</text>
          </view>
        </scroll-view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { productApi, couponApi, addressApi, cartApi } from '@/services/api'

const orderItems = ref<any[]>([])
const cartItemIds = ref<number[]>([])
const savedAddresses = ref<any[]>([])
const selectedAddressId = ref<number | null>(null)
const useManualAddress = ref(false)
const manualAddr = ref({ name: '', phone: '', detail: '' })
const saveAddr = ref(true)
const notes = ref('')
const submitting = ref(false)
const showCouponPicker = ref(false)
const selectedCoupon = ref<any>(null)
const availableCoupons = ref<any[]>([])

const subtotal = computed(() =>
  orderItems.value.reduce((s, i) => s + Number(i.price || 0) * (i.quantity || 1), 0)
)

const discountAmount = computed(() => {
  if (!selectedCoupon.value) return 0
  const coupon = selectedCoupon.value.coupon
  if (!coupon) return 0
  if (subtotal.value < Number(coupon.minOrderAmount || 0)) return 0
  if (coupon.couponType === 'REDUCE') return Number(coupon.discountValue)
  if (coupon.couponType === 'DISCOUNT') {
    const disc = subtotal.value * Number(coupon.discountValue)
    const max = Number(coupon.maxDiscountAmount || 999999)
    return Math.min(disc, max)
  }
  return 0
})

const finalTotal = computed(() => Math.max(0, subtotal.value - discountAmount.value))

async function loadAddresses() {
  try {
    const res: any = await addressApi.getList()
    savedAddresses.value = res.data || []
    const def = savedAddresses.value.find((a: any) => a.isDefault) || savedAddresses.value[0]
    if (def) {
      selectedAddressId.value = def.id
      useManualAddress.value = false
    } else {
      useManualAddress.value = true
    }
  } catch {
    useManualAddress.value = true
  }
}

async function loadCoupons() {
  try {
    const res: any = await couponApi.getMyCoupons('UNUSED')
    availableCoupons.value = (res.data || res || []).filter((uc: any) => {
      const coupon = uc.coupon
      if (!coupon) return false
      return subtotal.value >= Number(coupon.minOrderAmount || 0)
    })
  } catch {
    availableCoupons.value = []
  }
}

function selectCoupon(uc: any) {
  selectedCoupon.value = uc
  showCouponPicker.value = false
}

function getFullAddress(addr: any) {
  let s = ''
  if (addr.province) s += addr.province
  if (addr.city) s += addr.city
  if (addr.district) s += addr.district
  s += addr.detailAddress
  return s
}

function formatPrice(v: any) {
  return Number(v || 0).toFixed(2)
}

function formatDate(dt: string) {
  if (!dt) return ''
  return dt.substring(0, 10)
}

function goManageAddress() {
  uni.navigateTo({ url: '/pages/address/index' })
}

async function submitOrder() {
  if (submitting.value) return
  if (orderItems.value.length === 0) {
    uni.showToast({ title: '没有商品', icon: 'none' })
    return
  }

  let receiverName: string, receiverPhone: string, receiverAddress: string

  if (!useManualAddress.value && selectedAddressId.value) {
    const sel = savedAddresses.value.find((a: any) => a.id === selectedAddressId.value)
    if (!sel) return uni.showToast({ title: '请选择收货地址', icon: 'none' })
    receiverName = sel.receiverName
    receiverPhone = sel.receiverPhone
    receiverAddress = getFullAddress(sel)
  } else {
    if (!manualAddr.value.name.trim()) return uni.showToast({ title: '请填写收货人姓名', icon: 'none' })
    if (!manualAddr.value.phone.trim() || manualAddr.value.phone.length < 11) return uni.showToast({ title: '请填写正确手机号', icon: 'none' })
    if (!manualAddr.value.detail.trim()) return uni.showToast({ title: '请填写详细地址', icon: 'none' })
    receiverName = manualAddr.value.name
    receiverPhone = manualAddr.value.phone
    receiverAddress = manualAddr.value.detail
    if (saveAddr.value) {
      try {
        await addressApi.add({ receiverName, receiverPhone, detailAddress: receiverAddress, isDefault: savedAddresses.value.length === 0 })
      } catch { /* ignore */ }
    }
  }

  submitting.value = true
  uni.showLoading({ title: '提交订单...' })
  try {
    // Create orders for each product item
    const results = []
    for (const item of orderItems.value) {
      const res: any = await productApi.createOrder({
        productId: item.productId || item.id,
        quantity: item.quantity || 1,
        receiverName,
        receiverPhone,
        receiverAddress,
        notes: notes.value,
        couponId: selectedCoupon.value?.couponId || null,
        discountAmount: discountAmount.value / orderItems.value.length
      })
      results.push(res.data?.orderNo || res.data)
    }

    // Simulate payment for each
    for (const orderNo of results) {
      try {
        await productApi.simulatePay(orderNo)
      } catch { /* ignore */ }
    }

    // Clear from server cart
    if (cartItemIds.value.length > 0) {
      try { await cartApi.removeBatch(cartItemIds.value) } catch { /* ignore */ }
    }

    uni.hideLoading()
    uni.showToast({ title: '下单成功！', icon: 'success' })
    setTimeout(() => {
      uni.navigateTo({ url: '/pages/profile/my-product-orders' })
    }, 1200)
  } catch (e: any) {
    uni.hideLoading()
    uni.showToast({ title: e?.message || '下单失败，请重试', icon: 'none' })
  } finally {
    submitting.value = false
  }
}

onLoad((query: any) => {
  if (query?.cartData) {
    try {
      const data = JSON.parse(decodeURIComponent(query.cartData))
      if (data.productItems) {
        orderItems.value = data.productItems.map((i: any) => ({
          productId: i.productId,
          name: i.name,
          price: i.price,
          quantity: i.quantity,
          imageUrl: i.imageUrl,
          cartId: i.cartId
        }))
        cartItemIds.value = data.productItems.map((i: any) => i.cartId).filter(Boolean)
      }
    } catch { /* ignore */ }
  } else if (query?.productId) {
    // Direct buy
    orderItems.value = [{
      productId: Number(query.productId),
      name: query.name || '商品',
      price: Number(query.price || 0),
      quantity: Number(query.qty || 1),
      imageUrl: query.imageUrl || ''
    }]
  }
})

onMounted(() => {
  loadAddresses()
  loadCoupons()
})
</script>

<style lang="scss" scoped>
.checkout-page {
  min-height: 100vh;
  background: $background;
  padding: 24rpx 32rpx;
  padding-bottom: 160rpx;
}

/* ── Section Cards ── */
.section {
  background: $card;
  border-radius: $radius-xl;
  padding: 28rpx 28rpx;
  margin-bottom: 24rpx;
  box-shadow: 0 2rpx 12rpx rgba(0, 0, 0, 0.04);
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20rpx;
}

.section-title {
  font-size: 28rpx;
  font-weight: 700;
  color: $foreground;
  display: block;
  margin-bottom: 16rpx;
}

.link-btn {
  font-size: 26rpx;
  color: $accent;
  font-weight: 600;
}

/* ── Address Cards ── */
.addr-card {
  display: flex;
  align-items: flex-start;
  padding: 20rpx;
  border-radius: $radius-lg;
  background: $muted;
  margin-bottom: 16rpx;
  gap: 16rpx;
  border-left: 6rpx solid transparent;
  transition: all 0.2s;
}

.addr-card.selected {
  background: #ECFDF5;
  border-left-color: $accent;
}

.addr-radio {
  flex-shrink: 0;
  padding-top: 4rpx;
}

.radio {
  width: 40rpx;
  height: 40rpx;
  border-radius: 50%;
  border: 3rpx solid $border;
  background: #fff;
  transition: all 0.2s;
}

.radio.checked {
  background: $gradient-accent;
  border-color: transparent;
  position: relative;
  box-shadow: 0 2rpx 8rpx rgba(16, 185, 129, 0.25);
}

.radio.checked::after {
  content: '';
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 14rpx;
  height: 14rpx;
  border-radius: 50%;
  background: #fff;
}

.addr-info {
  flex: 1;
}

.addr-name-row {
  display: flex;
  align-items: center;
  gap: 12rpx;
  margin-bottom: 8rpx;
}

.addr-name {
  font-size: 28rpx;
  font-weight: 700;
  color: $foreground;
}

.addr-phone {
  font-size: 26rpx;
  color: $muted-foreground;
}

.default-tag {
  font-size: 20rpx;
  color: #059669;
  background: #ECFDF5;
  padding: 4rpx 14rpx;
  border-radius: $radius-full;
  font-weight: 500;
}

.addr-detail {
  font-size: 26rpx;
  color: $muted-foreground;
  line-height: 1.5;
}

.manual-toggle {
  margin-top: 16rpx;
  font-size: 26rpx;
  color: $accent;
  font-weight: 600;
}

/* ── Manual Form ── */
.manual-form {
  margin-top: 16rpx;
}

.form-item {
  display: flex;
  align-items: center;
  padding: 20rpx 0;

  & + .form-item {
    border-top: 1rpx solid $border;
  }
}

.form-label {
  font-size: 26rpx;
  color: $muted-foreground;
  width: 110rpx;
  flex-shrink: 0;
  font-weight: 500;
}

.form-input {
  flex: 1;
  font-size: 28rpx;
  color: $foreground;
  padding: 0 16rpx;
}

.save-addr-row {
  display: flex;
  align-items: center;
  gap: 12rpx;
  margin-top: 20rpx;
}

.checkbox-sm {
  width: 36rpx;
  height: 36rpx;
  border-radius: 8rpx;
  border: 3rpx solid $border;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #fff;
  transition: all 0.2s;
}

.checkbox-sm.checked {
  background: $accent;
  border-color: transparent;
}

.check-icon-sm {
  color: #fff;
  font-size: 22rpx;
  font-weight: 700;
}

.save-addr-label {
  font-size: 26rpx;
  color: $muted-foreground;
}

/* ── Order Items ── */
.order-item {
  display: flex;
  align-items: center;
  gap: 20rpx;
  padding: 16rpx 0;

  & + .order-item {
    border-top: 1rpx solid $border;
  }
}

.order-item:last-child {
  padding-bottom: 0;
}

.item-image {
  width: 112rpx;
  height: 112rpx;
  border-radius: $radius-lg;
  flex-shrink: 0;
  background: $muted;
}

.item-info {
  flex: 1;
  min-width: 0;
}

.item-name {
  font-size: 28rpx;
  font-weight: 600;
  color: $foreground;
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  line-height: 1.5;
}

.item-row {
  display: flex;
  align-items: center;
  gap: 12rpx;
  margin-top: 10rpx;
}

.item-price {
  font-size: 28rpx;
  font-weight: 700;
  color: $uni-error;
}

.item-qty {
  font-size: 24rpx;
  color: $muted-foreground;
  background: $muted;
  padding: 2rpx 12rpx;
  border-radius: $radius-full;
}

.item-subtotal {
  font-size: 30rpx;
  font-weight: 700;
  color: $uni-error;
  flex-shrink: 0;
}

/* ── Coupon ── */
.coupon-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.coupon-right {
  display: flex;
  align-items: center;
  gap: 8rpx;
}

.coupon-selected {
  font-size: 26rpx;
  color: $uni-error;
  font-weight: 700;
  background: rgba(239, 68, 68, 0.06);
  padding: 4rpx 16rpx;
  border-radius: $radius-full;
}

.coupon-none {
  font-size: 26rpx;
  color: $muted-foreground;
}

.arrow {
  font-size: 30rpx;
  color: $muted-foreground;
}

/* ── Notes ── */
.notes-input {
  width: 100%;
  min-height: 120rpx;
  font-size: 26rpx;
  color: $foreground;
  background: $muted;
  border-radius: $radius-lg;
  padding: 20rpx;
  box-sizing: border-box;
}

/* ── Price Summary ── */
.price-summary {
  .price-row {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 12rpx 0;
  }

  .price-label {
    font-size: 26rpx;
    color: $muted-foreground;
  }

  .price-value {
    font-size: 26rpx;
    color: $foreground;
    font-weight: 500;
  }

  .price-value.discount {
    color: $uni-error;
    font-weight: 600;
  }

  .divider {
    height: 1rpx;
    background: $border;
    margin: 16rpx 0;
  }

  .total-row {
    padding-top: 16rpx;

    .price-label.total {
      font-size: 32rpx;
      font-weight: 700;
      color: $foreground;
    }

    .price-value.total {
      font-size: 36rpx;
      font-weight: 700;
      color: $uni-error;
    }
  }
}

/* ── Bottom Bar ── */
.bottom-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20rpx 32rpx;
  padding-bottom: calc(20rpx + env(safe-area-inset-bottom));
  background: $card;
  box-shadow: 0 -4rpx 20rpx rgba(0, 0, 0, 0.06);
}

.bottom-total {
  display: flex;
  align-items: baseline;
}

.total-label {
  font-size: 26rpx;
  color: $muted-foreground;
}

.total-price {
  font-size: 40rpx;
  font-weight: 700;
  color: $uni-error;
}

.submit-btn {
  background: $gradient-accent;
  color: #fff;
  font-size: 30rpx;
  font-weight: 700;
  padding: 24rpx 56rpx;
  border-radius: $radius-full;
  box-shadow: 0 4rpx 16rpx rgba(16, 185, 129, 0.3);
  transition: opacity 0.15s;

  &:active {
    opacity: 0.85;
  }
}

.submit-btn.disabled {
  background: #cbd5e1;
  box-shadow: none;
}

/* ── Coupon Modal ── */
.coupon-modal {
  position: fixed;
  inset: 0;
  z-index: 1000;
}

.modal-mask {
  position: absolute;
  inset: 0;
  background: rgba(0, 0, 0, 0.45);
}

.modal-body {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  background: $card;
  border-radius: 32rpx 32rpx 0 0;
  max-height: 70vh;
  display: flex;
  flex-direction: column;
  box-sizing: border-box;
  overflow-x: hidden;
  padding-bottom: env(safe-area-inset-bottom);
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 36rpx 32rpx 24rpx;
}

.modal-title {
  font-size: 32rpx;
  font-weight: 700;
  color: $foreground;
}

.modal-close {
  width: 56rpx;
  height: 56rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28rpx;
  color: $muted-foreground;
  background: $muted;
  border-radius: 50%;
}

.coupon-no-use {
  padding: 24rpx 32rpx;
  font-size: 28rpx;
  color: $muted-foreground;
  background: $muted;
  margin: 0 24rpx 16rpx;
  border-radius: $radius-lg;
  text-align: center;
}

.coupon-list {
  flex: 1;
  padding: 8rpx 24rpx 24rpx;
}

.coupon-item {
  display: flex;
  align-items: center;
  padding: 24rpx;
  background: $card;
  border-radius: $radius-xl;
  margin-bottom: 16rpx;
  gap: 20rpx;
  box-shadow: 0 2rpx 12rpx rgba(0, 0, 0, 0.04);
  transition: all 0.2s;
}

.coupon-item.selected {
  background: #ECFDF5;
  box-shadow: 0 0 0 3rpx $accent;
}

.coupon-amount {
  background: $gradient-accent;
  color: #fff;
  padding: 20rpx 8rpx;
  border-radius: $radius-lg;
  font-size: 32rpx;
  font-weight: 700;
  text-align: center;
  min-width: 120rpx;
}

.coupon-info {
  flex: 1;
}

.coupon-name {
  font-size: 28rpx;
  font-weight: 700;
  color: $foreground;
  display: block;
  margin-bottom: 4rpx;
}

.coupon-cond {
  font-size: 24rpx;
  color: $muted-foreground;
  display: block;
  margin-bottom: 2rpx;
}

.coupon-exp {
  font-size: 22rpx;
  color: $muted-foreground;
  display: block;
}

.check-mark {
  color: $accent;
  font-size: 36rpx;
  font-weight: 700;
}

.empty-coupons {
  text-align: center;
  padding: 60rpx 40rpx;
  color: $muted-foreground;
  font-size: 26rpx;
}
</style>
