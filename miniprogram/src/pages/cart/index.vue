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
          :src="item.imageUrl || '/static/placeholder.png'"
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
  padding-bottom: 120rpx;
}

.cart-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 48rpx 32rpx 24rpx;
  background: $card;
  border-bottom: 1rpx solid $border;
}

.header-title {
  font-size: 36rpx;
  font-weight: 700;
  color: $foreground;
}

.edit-btn {
  font-size: 28rpx;
  color: $accent;
}

.empty-cart {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding-top: 160rpx;
  gap: 20rpx;
}

.empty-icon {
  font-size: 96rpx;
}

.empty-text {
  font-size: 34rpx;
  font-weight: 600;
  color: $foreground;
}

.empty-sub {
  font-size: 26rpx;
  color: #94a3b8;
}

.empty-btn {
  margin-top: 24rpx;
  padding: 20rpx 60rpx;
  border-radius: 40rpx;
}

.cart-list {
  height: calc(100vh - 200rpx);
}

.cart-item {
  display: flex;
  align-items: center;
  background: $card;
  margin: 16rpx 24rpx;
  border-radius: 16rpx;
  padding: 20rpx;
  gap: 16rpx;
  box-shadow: 0 2rpx 8rpx rgba(0,0,0,0.05);
}

.item-invalid {
  opacity: 0.5;
}

.item-check {
  flex-shrink: 0;
}

.checkbox {
  width: 40rpx;
  height: 40rpx;
  border-radius: 50%;
  border: 2rpx solid $border;
  display: flex;
  align-items: center;
  justify-content: center;
  background: white;
}

.checkbox.checked {
  background: $accent;
  border-color: $accent;
}

.check-icon {
  color: white;
  font-size: 24rpx;
  font-weight: 700;
}

.item-image {
  width: 120rpx;
  height: 120rpx;
  border-radius: 12rpx;
  flex-shrink: 0;
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
}

.item-type-tag {
  font-size: 22rpx;
  color: $accent;
  background: rgba(16, 185, 129, 0.1);
  padding: 2rpx 10rpx;
  border-radius: 20rpx;
  display: inline-block;
  margin: 6rpx 0;
}

.invalid-tag {
  font-size: 22rpx;
  color: #ef4444;
  background: rgba(239, 68, 68, 0.1);
  padding: 2rpx 10rpx;
  border-radius: 20rpx;
  display: inline-block;
  margin: 6rpx 0;
}

.item-bottom {
  margin-top: 8rpx;
}

.item-price {
  font-size: 30rpx;
  font-weight: 700;
  color: #ef4444;
}

.item-right {
  flex-shrink: 0;
}

.qty-ctrl {
  display: flex;
  align-items: center;
  gap: 12rpx;
}

.qty-btn {
  width: 48rpx;
  height: 48rpx;
  border-radius: 50%;
  border: 2rpx solid $border;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 32rpx;
  color: $foreground;
  background: white;
}

.qty-num {
  font-size: 28rpx;
  font-weight: 600;
  color: $foreground;
  min-width: 40rpx;
  text-align: center;
}

.del-btn {
  padding: 8rpx 16rpx;
}

.del-icon {
  font-size: 36rpx;
}

.invalid-notice {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16rpx 32rpx;
  background: rgba(239, 68, 68, 0.05);
  font-size: 26rpx;
  color: #ef4444;
}

.clean-btn {
  color: $accent;
  font-weight: 600;
}

.cart-bottom {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  display: flex;
  align-items: center;
  padding: 20rpx 24rpx;
  background: $card;
  border-top: 1rpx solid $border;
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
  color: #64748b;
}

.total-price {
  font-size: 36rpx;
  font-weight: 700;
  color: #ef4444;
}

.selected-count {
  font-size: 22rpx;
  color: #94a3b8;
}

.checkout-btn {
  background: $accent;
  color: white;
  font-size: 28rpx;
  font-weight: 600;
  padding: 20rpx 36rpx;
  border-radius: 40rpx;
  white-space: nowrap;
}

.checkout-btn.disabled {
  background: #cbd5e1;
}
</style>
