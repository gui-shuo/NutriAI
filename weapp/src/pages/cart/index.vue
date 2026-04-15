<template>
  <view class="cart-page">
    <view v-if="cartItems.length" class="cart-content">
      <!-- Cart Items -->
      <view class="cart-list">
        <view v-for="(item, index) in cartItems" :key="item.productId" class="cart-item card">
          <view class="item-check" @tap="toggleCheck(index)">
            <text :class="['check-icon', { checked: item.checked }]">{{ item.checked ? '✅' : '⬜' }}</text>
          </view>
          <image :src="item.imageUrl || '/static/images/product-default.png'" mode="aspectFill" class="item-img" />
          <view class="item-info">
            <text class="item-name text-ellipsis">{{ item.name }}</text>
            <view class="item-bottom">
              <text class="text-price">¥{{ item.price }}</text>
              <view class="quantity-ctrl">
                <view class="qty-btn" @tap="changeQty(index, -1)"><text>−</text></view>
                <text class="qty-num">{{ item.quantity }}</text>
                <view class="qty-btn" @tap="changeQty(index, 1)"><text>+</text></view>
              </view>
            </view>
          </view>
          <view class="item-delete" @tap="removeItem(index)">
            <text>🗑️</text>
          </view>
        </view>
      </view>
    </view>

    <!-- Empty -->
    <view v-else class="empty-state">
      <text class="empty-icon">🛒</text>
      <text class="empty-text">购物车是空的</text>
      <button class="btn-primary btn-sm mt-lg" @tap="goShop">去逛逛</button>
    </view>

    <!-- Bottom Settlement Bar -->
    <view v-if="cartItems.length" class="settle-bar safe-bottom">
      <view class="settle-left" @tap="toggleAll">
        <text :class="['check-icon', { checked: allChecked }]">{{ allChecked ? '✅' : '⬜' }}</text>
        <text class="text-sm ml-sm">全选</text>
      </view>
      <view class="settle-right">
        <view class="total-info">
          <text class="text-sm">合计：</text>
          <text class="text-price text-lg">¥{{ totalPrice.toFixed(2) }}</text>
        </view>
        <button class="settle-btn" :disabled="!checkedCount" @tap="goSettle">
          结算({{ checkedCount }})
        </button>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { useUserStore } from '../../stores/user'

const userStore = useUserStore()

interface CartItem {
  productId: number
  name: string
  price: number
  imageUrl: string
  quantity: number
  checked: boolean
}

const cartItems = ref<CartItem[]>([])

const allChecked = computed(() => cartItems.value.length > 0 && cartItems.value.every(i => i.checked))
const checkedCount = computed(() => cartItems.value.filter(i => i.checked).reduce((s, i) => s + i.quantity, 0))
const totalPrice = computed(() => cartItems.value.filter(i => i.checked).reduce((s, i) => s + i.price * i.quantity, 0))

onShow(() => {
  loadCart()
})

function loadCart() {
  const raw = getCart()
  cartItems.value = raw.map((item: any) => ({ ...item, checked: true }))
}

function toggleCheck(index: number) {
  cartItems.value[index].checked = !cartItems.value[index].checked
}

function toggleAll() {
  const newVal = !allChecked.value
  cartItems.value.forEach(i => i.checked = newVal)
}

function changeQty(index: number, delta: number) {
  const item = cartItems.value[index]
  item.quantity = Math.max(1, item.quantity + delta)
  persistCart()
}

function removeItem(index: number) {
  cartItems.value.splice(index, 1)
  persistCart()
}

function persistCart() {
  const data = cartItems.value.map(({ checked, ...rest }) => rest)
  saveCart(data)
}

function goShop() {
  uni.switchTab({ url: '/pages/shop/index' })
}

function goSettle() {
  if (!userStore.isLoggedIn) {
    uni.navigateTo({ url: '/pages/auth/login' })
    return
  }
  const checked = cartItems.value.filter(i => i.checked)
  if (!checked.length) return

  // Store checked items for order confirm page
  uni.setStorageSync('nutriai_checkout_items', JSON.stringify(checked.map(({ checked: _, ...rest }) => rest)))
  uni.navigateTo({ url: '/pages/order/confirm?type=cart' })
}

const CART_KEY = 'nutriai_cart'
function getCart(): any[] { try { return JSON.parse(uni.getStorageSync(CART_KEY) || '[]') } catch { return [] } }
function saveCart(cart: any[]) { uni.setStorageSync(CART_KEY, JSON.stringify(cart)) }
</script>

<style lang="scss" scoped>
.cart-page {
  min-height: 100vh;
  background: $bg-page;
  padding-bottom: 140rpx;
}

.cart-list {
  padding: $spacing-md $spacing-lg;
}

.cart-item {
  display: flex;
  align-items: center;
  gap: $spacing-md;
  padding: $spacing-md;
  margin-bottom: $spacing-md;
}

.item-check { padding: $spacing-xs; }

.check-icon {
  font-size: 36rpx;
  &.checked { opacity: 1; }
}

.item-img {
  width: 160rpx;
  height: 160rpx;
  border-radius: $radius-md;
  background: $bg-muted;
  flex-shrink: 0;
}

.item-info {
  flex: 1;
  min-width: 0;
}

.item-name {
  font-size: $font-base;
  color: $text-primary;
  font-weight: 500;
}

.item-bottom {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: $spacing-md;
}

.quantity-ctrl {
  display: flex;
  align-items: center;
  background: $bg-muted;
  border-radius: $radius-md;
}

.qty-btn {
  width: 56rpx;
  height: 48rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: $font-lg;
  color: $text-secondary;
}

.qty-num {
  width: 60rpx;
  text-align: center;
  font-size: $font-base;
  font-weight: 500;
}

.item-delete {
  padding: $spacing-sm;
  font-size: 28rpx;
}

// Settlement Bar
.settle-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  background: $bg-card;
  box-shadow: 0 -2rpx 10rpx rgba(0, 0, 0, 0.05);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: $spacing-sm $spacing-lg;
  z-index: 100;
}

.settle-left {
  display: flex;
  align-items: center;
}

.settle-right {
  display: flex;
  align-items: center;
  gap: $spacing-md;
}

.settle-btn {
  padding: 0 40rpx;
  height: 76rpx;
  line-height: 76rpx;
  background: $gradient-primary;
  color: #fff;
  border-radius: $radius-xl;
  font-size: $font-base;
  font-weight: 500;
  border: none;

  &::after { border: none; }
  &[disabled] { opacity: 0.5; }
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 200rpx 0;
}

.empty-icon { font-size: 100rpx; margin-bottom: $spacing-md; }
</style>
