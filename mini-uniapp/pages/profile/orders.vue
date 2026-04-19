<script setup>
/**
 * 我的订单页 - 合并显示营养餐订单和商城订单
 */
import { ref, onMounted } from 'vue'
import NavBar from '../../components/NavBar.vue'
import { productApi, mealApi } from '../../services/api'
import { formatPrice, formatTime } from '../../utils/common'

const tabs = ['全部', '待付款', '待自提', '已完成']
const activeTab = ref(0)
const orders = ref([])
const loading = ref(false)

const statusNames = {
  PENDING_PAYMENT: '待付款',
  PAID: '已付款',
  PREPARING: '备餐中',
  READY: '待自提',
  PICKED_UP: '已取餐',
  SHIPPED: '已发货',
  DELIVERED: '已送达',
  COMPLETED: '已完成',
  CANCELLED: '已取消',
  REFUNDED: '已退款',
}

function switchTab(idx) {
  activeTab.value = idx
  fetchOrders()
}

async function fetchOrders() {
  loading.value = true
  try {
    // 同时拉取商城订单和营养餐订单
    const statusMap = ['', 'PENDING_PAYMENT', 'READY', 'COMPLETED']
    const params = { page: 0, size: 20 }
    if (statusMap[activeTab.value]) params.status = statusMap[activeTab.value]

    const [prodRes, mealRes] = await Promise.allSettled([
      productApi.getOrders(params),
      mealApi.getOrders(params),
    ])

    const prodOrders = (prodRes.status === 'fulfilled' ? (prodRes.value?.content || []) : []).map(o => ({
      ...o,
      orderType: '商城',
      statusText: statusNames[o.orderStatus] || o.orderStatus,
      itemName: o.items?.[0]?.productName || '商品',
    }))

    const mealOrders = (mealRes.status === 'fulfilled' ? (mealRes.value?.content || []) : []).map(o => ({
      ...o,
      orderType: '营养餐',
      statusText: statusNames[o.orderStatus] || o.orderStatus,
      itemName: o.items?.[0]?.mealName || '餐品',
    }))

    // 合并排序
    orders.value = [...prodOrders, ...mealOrders].sort((a, b) =>
      new Date(b.createdAt || 0) - new Date(a.createdAt || 0)
    )
  } catch (e) {
    orders.value = []
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  fetchOrders()
})
</script>

<template>
  <view class="page">
    <NavBar showBack title="我的订单" />

    <!-- Tab栏 -->
    <u-tabs
      :list="tabs.map(t => ({ name: t }))"
      :current="activeTab"
      lineColor="#0a6e2c"
      :activeStyle="{color: '#0a6e2c', fontWeight: '600'}"
      :inactiveStyle="{color: '#666'}"
      @click="(item) => switchTab(tabs.indexOf(item.name))"
    />

    <scroll-view scroll-y class="content" :enhanced="true" :show-scrollbar="false">
      <view v-if="loading" class="state-tip">
        <u-loading-icon mode="circle" size="28" color="#0a6e2c" />
        <text style="margin-top: 16rpx;">加载中...</text>
      </view>
      <u-empty v-else-if="orders.length === 0" text="暂无订单" icon="list" mode="list" marginTop="80" />

      <view v-for="order in orders" :key="order.id" class="order-card">
        <view class="order-card__header">
          <text class="order-card__no">订单号: {{ order.orderNo || order.id }}</text>
          <text class="order-card__status">{{ order.statusText }}</text>
        </view>
        <view class="order-card__body">
          <text class="order-card__type">{{ order.orderType }}</text>
          <text class="order-card__name">{{ order.itemName }}</text>
          <text class="order-card__amount">¥{{ formatPrice(order.totalAmount) }}</text>
        </view>
        <view class="order-card__footer">
          <text class="order-card__time">{{ formatTime(order.createdAt) }}</text>
        </view>
      </view>

      <view style="height: 40rpx;" />
    </scroll-view>
  </view>
</template>

<style lang="scss" scoped>
@import '../../styles/design-system.scss';

.page { min-height: 100vh; background: #ffffff; overflow-x: hidden; width: 100%; }

.tabs {
  display: flex;
  background: $surface-container-lowest;
  padding: 0 8rpx;
}

.tab {
  flex: 1;
  padding: 24rpx 0;
  text-align: center;
  position: relative;

  &--active {
    &::after {
      content: '';
      position: absolute;
      bottom: 0;
      left: 30%;
      right: 30%;
      height: 4rpx;
      background: $primary;
      border-radius: $radius-full;
    }
  }

  &__text {
    font-size: $font-sm;
    color: $on-surface;
    font-weight: 500;
  }

  &--active &__text {
    color: $primary;
    font-weight: 600;
  }
}

.content {
  padding: 24rpx;
  height: calc(100vh - 200px);
}

.state-tip {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12rpx;
  padding: 100rpx 0;
  color: $on-surface-variant;

  &__icon { font-size: 80rpx; }
}

.order-card {
  background: $surface-container-lowest;
  border-radius: $radius-xl;
  padding: 24rpx;
  margin-bottom: 16rpx;
  box-shadow: $shadow-sm;

  &__header {
    display: flex;
    justify-content: space-between;
    margin-bottom: 16rpx;
  }

  &__no {
    font-size: $font-xs;
    color: $on-surface-variant;
  }

  &__status {
    font-size: $font-xs;
    color: $primary;
    font-weight: 600;
  }

  &__body {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 12rpx;
  }

  &__name {
    font-size: $font-base;
    font-weight: 600;
    color: $on-surface;
    flex: 1;
  }

  &__type {
    font-size: $font-xs;
    color: $primary;
    background: rgba($primary, 0.1);
    padding: 4rpx 12rpx;
    border-radius: $radius-sm;
    margin-right: 12rpx;
  }

  &__amount {
    font-size: $font-lg;
    font-weight: 700;
    color: $primary;
  }

  &__footer {
    display: flex;
    justify-content: flex-end;
  }

  &__time {
    font-size: $font-xs;
    color: $on-surface-variant;
  }
}
</style>
