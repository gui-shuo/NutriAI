<script setup>
/**
 * 购物车页
 */
import { ref, onMounted } from 'vue'
import NavBar from '../../components/NavBar.vue'
import { useCartStore } from '../../stores/cart'
import { formatPrice, cosUrl, checkLogin } from '../../utils/common'

const cartStore = useCartStore()

onMounted(() => {
  const token = uni.getStorageSync('accessToken')
  if (token) {
    cartStore.fetchCart()
  }
})

function goToDetail(item) {
  // 根据类型跳转到不同详情页
  if (item.itemType === 'MEAL') {
    uni.navigateTo({ url: `/pages/meal/detail?id=${item.itemId}` })
  } else {
    uni.navigateTo({ url: `/pages/shop/detail?id=${item.itemId}` })
  }
}

function changeQty(item, delta) {
  const next = item.quantity + delta
  if (next <= 0) {
    uni.showModal({
      title: '提示',
      content: '确定移除该商品吗？',
      success: (res) => {
        if (res.confirm) cartStore.removeItem(item.id)
      }
    })
    return
  }
  cartStore.updateQuantity(item.id, next)
}

function checkout() {
  if (!checkLogin()) return
  if (cartStore.selectedItems.length === 0) {
    uni.showToast({ title: '请选择商品', icon: 'none' })
    return
  }
  uni.showToast({ title: '结算功能开发中', icon: 'none' })
}
</script>

<template>
  <view class="page">
    <NavBar showBack title="购物车" />

    <scroll-view scroll-y class="content" :enhanced="true" :show-scrollbar="false">
      <!-- 空购物车 -->
      <view v-if="cartStore.items.length === 0" class="empty">
        <u-empty text="购物车空空如也" icon="cart" mode="car" marginTop="80" />
        <u-button
          text="去逛逛"
          type="primary"
          shape="circle"
          color="#0a6e2c"
          size="normal"
          :customStyle="{marginTop: '32rpx', width: '240rpx'}"
          @click="() => uni.switchTab({ url: '/pages/shop/index' })"
        />
      </view>

      <!-- 购物车列表 -->
      <view v-else>
        <!-- 全选栏 -->
        <view class="select-all" @tap="cartStore.toggleSelectAll()">
          <view class="checkbox" :class="{ 'checkbox--checked': cartStore.isAllSelected }" />
          <text class="select-all__text">全选</text>
        </view>

        <view
          v-for="item in cartStore.items"
          :key="item.id"
          class="cart-item"
        >
          <!-- 选中状态 -->
          <view
            class="checkbox"
            :class="{ 'checkbox--checked': item.selected }"
            @tap="cartStore.toggleSelect(item.id)"
          >
            <u-icon v-if="item.selected" name="checkmark" size="16" color="#ffffff" />
          </view>

          <!-- 商品图片 -->
          <u-image
            :src="cosUrl(item.image) || '/static/images/product-placeholder.png'"
            width="160rpx"
            height="160rpx"
            mode="aspectFill"
            radius="12"
            :lazy-load="true"
            @click="goToDetail(item)"
          />

          <!-- 商品信息 -->
          <view class="cart-item__info">
            <text class="cart-item__name" @tap="goToDetail(item)">{{ item.name }}</text>
            <view class="cart-item__bottom">
              <text class="cart-item__price">¥{{ formatPrice(item.price) }}</text>
              <u-number-box v-model="item.quantity" :min="0" :max="99" buttonSize="26" bgColor="#f5f5f5" @change="(val) => { if (val.value <= 0) { uni.showModal({ title: '提示', content: '确定移除该商品吗？', success: (res) => { if (res.confirm) cartStore.removeItem(item.id) } }) } else { cartStore.updateQuantity(item.id, val.value) } }" />
            </view>
          </view>
        </view>
      </view>

      <view style="height: 160rpx;" />
    </scroll-view>

    <!-- 底部结算栏 -->
    <view v-if="cartStore.items.length > 0" class="checkout-bar safe-bottom">
      <view class="checkout-bar__info">
        <text class="checkout-bar__label">合计：</text>
        <text class="checkout-bar__price">¥{{ formatPrice(cartStore.totalPrice) }}</text>
      </view>
      <u-button
        :text="`结算 (${cartStore.selectedItems.length})`"
        type="primary"
        shape="circle"
        color="#0a6e2c"
        size="normal"
        :customStyle="{padding: '20rpx 48rpx'}"
        @click="checkout"
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

// 空状态
.empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 120rpx 0;

  &__icon {
    font-size: 100rpx;
    margin-bottom: 24rpx;
  }

  &__title {
    font-size: $font-xl;
    font-weight: 700;
    color: $on-surface;
    margin-bottom: 8rpx;
  }

  &__desc {
    font-size: $font-sm;
    color: $on-surface-variant;
    margin-bottom: 32rpx;
  }

  &__btn {
    padding: 16rpx 48rpx;
    background: $primary;
    color: $on-primary;
    border-radius: $radius-lg;
    font-size: $font-base;
    font-weight: 600;
  }
}

// 全选
.select-all {
  display: flex;
  align-items: center;
  gap: 16rpx;
  padding: 16rpx 0;
  margin-bottom: 12rpx;

  &__text {
    font-size: $font-base;
    color: $on-surface;
  }
}

// 复选框
.checkbox {
  width: 44rpx;
  height: 44rpx;
  border-radius: $radius-md;
  background: $surface-container-low;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;

  &--checked {
    background: $primary;

    &::after {
      content: '✓';
      color: $on-primary;
      font-size: 28rpx;
      font-weight: 700;
    }
  }
}

// 购物车项
.cart-item {
  display: flex;
  align-items: center;
  gap: 16rpx;
  padding: 20rpx;
  background: $surface-container-lowest;
  border-radius: $radius-xl;
  margin-bottom: 16rpx;

  &__image {
    width: 160rpx;
    height: 160rpx;
    border-radius: $radius-lg;
    flex-shrink: 0;
  }

  &__info {
    flex: 1;
    min-width: 0;
    display: flex;
    flex-direction: column;
    justify-content: space-between;
    height: 160rpx;
  }

  &__name {
    font-size: $font-base;
    font-weight: 600;
    color: $on-surface;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  &__bottom {
    display: flex;
    align-items: center;
    justify-content: space-between;
  }

  &__price {
    font-size: $font-lg;
    font-weight: 800;
    color: $primary;
  }
}

// 数量控制
.qty-control {
  display: flex;
  align-items: center;
  gap: 16rpx;
}

.qty-btn {
  width: 52rpx;
  height: 52rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  background: $surface-container-low;
  border-radius: $radius-full;
  font-size: 32rpx;
  color: $on-surface;
}

.qty-value {
  font-size: $font-base;
  font-weight: 600;
  color: $on-surface;
  min-width: 40rpx;
  text-align: center;
}

// 结算栏
.checkout-bar {
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

  &__info {
    display: flex;
    align-items: baseline;
    gap: 4rpx;
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

    &:active {
      transform: scale(0.96);
    }
  }
}
</style>
