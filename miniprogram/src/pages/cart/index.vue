<template>
  <view class="cart-page">
    <!-- Header -->
    <view class="cart-header">
      <text class="header-title">🛒 购物车</text>
      <text class="edit-btn" @tap="toggleEditMode">{{ editMode ? '完成' : '管理' }}</text>
    </view>

    <!-- Empty State -->
    <view class="empty-cart" v-if="cartItems.length === 0">
      <text class="empty-icon">🛒</text>
      <text class="empty-text">购物车空空如也</text>
      <text class="empty-sub">去挑选一些健康食品吧</text>
      <view class="btn-primary empty-btn" @tap="goShop">去逛逛</view>
    </view>

    <!-- Cart Items -->
    <scroll-view class="cart-list" scroll-y v-else>
      <view
        v-for="item in cartItems"
        :key="item.id"
        class="cart-item"
        :class="{ 'item-invalid': item.invalid }"
      >
        <!-- Checkbox -->
        <view class="item-check" @tap="toggleSelect(item)">
          <view class="checkbox" :class="{ checked: item.selected && !item.invalid }">
            <text v-if="item.selected && !item.invalid" class="check-icon">✓</text>
          </view>
        </view>

        <!-- Image -->
        <image
          class="item-image"
          :src="item.imageUrl || '/static/images/product-placeholder.png'"
          mode="aspectFill"
          @tap="goDetail(item)"
        />

        <!-- Info -->
        <view class="item-info" @tap="goDetail(item)">
          <text class="item-name">{{ item.name }}</text>
          <text class="item-type-tag">{{ item.itemType === 'MEAL' ? '营养餐' : '产品' }}</text>
          <text v-if="item.invalid" class="invalid-tag">已失效</text>
          <view class="item-bottom">
            <text class="item-price">¥{{ formatPrice(item.price) }}</text>
          </view>
        </view>

        <!-- Quantity / Delete -->
        <view class="item-right">
          <view v-if="!editMode && !item.invalid" class="qty-ctrl">
            <view class="qty-btn" @tap="changeQty(item, -1)">−</view>
            <text class="qty-num">{{ item.quantity }}</text>
            <view class="qty-btn" @tap="changeQty(item, 1)">+</view>
          </view>
          <view v-else class="del-btn" @tap="removeItem(item)">
            <text class="del-icon">🗑</text>
          </view>
        </view>
      </view>

      <!-- Invalid Items notice -->
      <view class="invalid-notice" v-if="hasInvalidItems">
        <text>部分商品已失效，结算时将自动忽略</text>
        <text class="clean-btn" @tap="cleanInvalid">清理失效</text>
      </view>
    </scroll-view>

    <!-- Bottom Bar -->
    <view class="cart-bottom" v-if="cartItems.length > 0">
      <view class="select-all" @tap="toggleSelectAll">
        <view class="checkbox" :class="{ checked: allSelected }">
          <text v-if="allSelected" class="check-icon">✓</text>
        </view>
        <text class="select-all-text">全选</text>
      </view>

      <view class="total-area">
        <view class="total-line">
          <text class="total-label">合计：</text>
          <text class="total-price">¥{{ totalPrice }}</text>
        </view>
        <text class="selected-count">已选 {{ selectedCount }} 件</text>
      </view>

      <view
        class="checkout-btn"
        :class="{ disabled: selectedCount === 0 }"
        @tap="checkout"
      >
        {{ editMode ? `删除(${selectedCount})` : `结算(${selectedCount})` }}
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { cartApi } from '@/services/api'

const cartItems = ref<any[]>([])
const editMode = ref(false)
const loading = ref(false)

const allSelected = computed(() =>
  cartItems.value.filter(i => !i.invalid).length > 0 &&
  cartItems.value.filter(i => !i.invalid).every(i => i.selected)
)

const selectedItems = computed(() => cartItems.value.filter(i => i.selected && !i.invalid))
const selectedCount = computed(() => selectedItems.value.reduce((s, i) => s + i.quantity, 0))
const hasInvalidItems = computed(() => cartItems.value.some(i => i.invalid))

const totalPrice = computed(() => {
  const total = selectedItems.value.reduce((s, i) => s + (i.price || 0) * i.quantity, 0)
  return total.toFixed(2)
})

async function loadCart() {
  loading.value = true
  try {
    const res: any = await cartApi.getCart()
    cartItems.value = res.data || res || []
  } catch (e) {
    console.error('Load cart error:', e)
  } finally {
    loading.value = false
  }
}

async function toggleSelect(item: any) {
  if (item.invalid) return
  try {
    await cartApi.updateSelected(item.id, !item.selected)
    item.selected = !item.selected
  } catch (e) {
    console.error(e)
  }
}

async function toggleSelectAll() {
  const target = !allSelected.value
  try {
    await cartApi.selectAll(target)
    cartItems.value.forEach(i => { if (!i.invalid) i.selected = target })
  } catch (e) {
    console.error(e)
  }
}

async function changeQty(item: any, delta: number) {
  const newQty = item.quantity + delta
  if (newQty <= 0) {
    await removeItem(item)
    return
  }
  if (newQty > 99) return
  try {
    await cartApi.updateQuantity(item.id, newQty)
    item.quantity = newQty
  } catch (e) {
    console.error(e)
  }
}

async function removeItem(item: any) {
  try {
    await cartApi.removeItem(item.id)
    cartItems.value = cartItems.value.filter(i => i.id !== item.id)
    uni.showToast({ title: '已删除', icon: 'success', duration: 1000 })
  } catch (e) {
    console.error(e)
  }
}

async function cleanInvalid() {
  const invalidIds = cartItems.value.filter(i => i.invalid).map(i => i.id)
  if (invalidIds.length === 0) return
  try {
    await cartApi.removeBatch(invalidIds)
    cartItems.value = cartItems.value.filter(i => !i.invalid)
    uni.showToast({ title: '已清理', icon: 'success', duration: 1000 })
  } catch (e) {
    console.error(e)
  }
}

function toggleEditMode() {
  editMode.value = !editMode.value
}

async function checkout() {
  if (selectedCount.value === 0) {
    uni.showToast({ title: '请选择商品', icon: 'none' })
    return
  }
  if (editMode.value) {
    // Delete selected items
    const selectedIds = selectedItems.value.map(i => i.id)
    try {
      await cartApi.removeBatch(selectedIds)
      cartItems.value = cartItems.value.filter(i => !selectedIds.includes(i.id))
      uni.showToast({ title: '已删除', icon: 'success', duration: 1000 })
    } catch (e) {
      console.error(e)
    }
    return
  }

  // Group items by type
  const mealItems = selectedItems.value.filter(i => i.itemType === 'MEAL')
  const productItems = selectedItems.value.filter(i => i.itemType === 'PRODUCT')

  // Navigate to checkout with cart items
  const checkoutData = encodeURIComponent(JSON.stringify({
    source: 'cart',
    mealItems: mealItems.map(i => ({ mealId: i.itemId, quantity: i.quantity, name: i.name, price: i.price, cartId: i.id })),
    productItems: productItems.map(i => ({ productId: i.itemId, quantity: i.quantity, name: i.name, price: i.price, cartId: i.id }))
  }))

  if (productItems.length > 0 && mealItems.length === 0) {
    uni.navigateTo({ url: `/pages/product-shop/checkout?cartData=${checkoutData}` })
  } else if (mealItems.length > 0 && productItems.length === 0) {
    uni.navigateTo({ url: `/pages/meal-order/checkout?cartData=${checkoutData}` })
  } else {
    // Mixed — go to product checkout which handles both
    uni.navigateTo({ url: `/pages/product-shop/checkout?cartData=${checkoutData}` })
  }
}

function goShop() {
  uni.switchTab({ url: '/pages/product-shop/index' })
}

function goDetail(item: any) {
  if (item.itemType === 'MEAL') {
    uni.navigateTo({ url: `/pages/meal-order/detail?id=${item.itemId}` })
  } else {
    uni.navigateTo({ url: `/pages/product-shop/detail?id=${item.itemId}` })
  }
}

function formatPrice(price: any) {
  return Number(price || 0).toFixed(2)
}

onMounted(loadCart)
</script>

<style lang="scss" scoped>
.cart-page {
  min-height: 100vh;
  background: $background;
  padding-bottom: 140rpx;
}

/* ── Header ── */
.cart-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 48rpx 32rpx 24rpx;
  background: $card;
  box-shadow: $shadow-sm;
}

.header-title {
  font-size: 36rpx;
  font-weight: 700;
  color: $foreground;
}

.edit-btn {
  font-size: 28rpx;
  color: $accent;
  font-weight: 600;
}

/* ── Empty State ── */
.empty-cart {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding-top: 200rpx;
  gap: 16rpx;
}

.empty-icon {
  font-size: 120rpx;
  margin-bottom: 16rpx;
}

.empty-text {
  font-size: 34rpx;
  font-weight: 700;
  color: $foreground;
}

.empty-sub {
  font-size: 26rpx;
  color: $muted-foreground;
}

.empty-btn {
  margin-top: 32rpx;
  padding: 22rpx 72rpx;
  border-radius: $radius-full;
  background: $gradient-accent;
  color: #fff;
  font-size: 28rpx;
  font-weight: 600;
  box-shadow: 0 4rpx 16rpx rgba(16, 185, 129, 0.3);
}

/* ── Cart List ── */
.cart-list {
  height: calc(100vh - 200rpx);
  padding: 8rpx 0;
}

/* ── Cart Item Card ── */
.cart-item {
  display: flex;
  align-items: center;
  background: $card;
  margin: 24rpx 32rpx;
  border-radius: $radius-xl;
  padding: 24rpx;
  gap: 20rpx;
  box-shadow: 0 2rpx 12rpx rgba(0, 0, 0, 0.04);
  transition: opacity 0.2s;
}

.item-invalid {
  opacity: 0.45;
  position: relative;

  &::after {
    content: '';
    position: absolute;
    inset: 0;
    background: rgba(255, 255, 255, 0.3);
    border-radius: $radius-xl;
    pointer-events: none;
  }
}

/* ── Checkbox ── */
.item-check {
  flex-shrink: 0;
}

.checkbox {
  width: 44rpx;
  height: 44rpx;
  border-radius: 50%;
  border: 3rpx solid $border;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #fff;
  transition: all 0.2s;
}

.checkbox.checked {
  background: $gradient-accent;
  border-color: transparent;
  box-shadow: 0 2rpx 8rpx rgba(16, 185, 129, 0.25);
}

.check-icon {
  color: #fff;
  font-size: 24rpx;
  font-weight: 700;
}

/* ── Item Image ── */
.item-image {
  width: 128rpx;
  height: 128rpx;
  border-radius: $radius-lg;
  flex-shrink: 0;
  background: $muted;
}

/* ── Item Info ── */
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

.item-type-tag {
  font-size: 22rpx;
  color: #059669;
  background: #ECFDF5;
  padding: 4rpx 14rpx;
  border-radius: $radius-full;
  display: inline-block;
  margin: 8rpx 4rpx 8rpx 0;
  font-weight: 500;
}

.invalid-tag {
  font-size: 22rpx;
  color: $uni-error;
  background: rgba(239, 68, 68, 0.08);
  padding: 4rpx 14rpx;
  border-radius: $radius-full;
  display: inline-block;
  margin: 8rpx 0;
  font-weight: 500;
}

.item-bottom {
  margin-top: 8rpx;
}

.item-price {
  font-size: 32rpx;
  font-weight: 700;
  color: $uni-error;
}

/* ── Quantity Controls ── */
.item-right {
  flex-shrink: 0;
}

.qty-ctrl {
  display: flex;
  align-items: center;
  gap: 8rpx;
}

.qty-btn {
  width: 52rpx;
  height: 52rpx;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 32rpx;
  color: $foreground;
  background: $muted;
  transition: background 0.15s;

  &:active {
    background: darken(#F0F4F8, 6%);
  }
}

.qty-num {
  font-size: 28rpx;
  font-weight: 600;
  color: $foreground;
  min-width: 48rpx;
  text-align: center;
}

.del-btn {
  padding: 12rpx 16rpx;
}

.del-icon {
  font-size: 36rpx;
}

/* ── Invalid Notice ── */
.invalid-notice {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: 0 32rpx 24rpx;
  padding: 20rpx 24rpx;
  background: rgba(239, 68, 68, 0.06);
  border-radius: $radius-lg;
  font-size: 26rpx;
  color: $uni-error;
}

.clean-btn {
  color: $accent;
  font-weight: 600;
}

/* ── Bottom Bar ── */
.cart-bottom {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  display: flex;
  align-items: center;
  padding: 20rpx 32rpx;
  background: $card;
  box-shadow: 0 -4rpx 20rpx rgba(0, 0, 0, 0.06);
  gap: 16rpx;
  padding-bottom: calc(20rpx + env(safe-area-inset-bottom));
}

.select-all {
  display: flex;
  align-items: center;
  gap: 12rpx;
}

.select-all-text {
  font-size: 26rpx;
  color: $foreground;
  font-weight: 500;
}

.total-area {
  flex: 1;
  text-align: right;
}

.total-line {
  display: flex;
  align-items: baseline;
  justify-content: flex-end;
}

.total-label {
  font-size: 26rpx;
  color: $muted-foreground;
}

.total-price {
  font-size: 38rpx;
  font-weight: 700;
  color: $uni-error;
}

.selected-count {
  font-size: 22rpx;
  color: $muted-foreground;
  margin-top: 2rpx;
}

.checkout-btn {
  background: $gradient-accent;
  color: #fff;
  font-size: 28rpx;
  font-weight: 600;
  padding: 22rpx 40rpx;
  border-radius: $radius-full;
  white-space: nowrap;
  box-shadow: 0 4rpx 16rpx rgba(16, 185, 129, 0.3);
}

.checkout-btn.disabled {
  background: #cbd5e1;
  box-shadow: none;
}
</style>
