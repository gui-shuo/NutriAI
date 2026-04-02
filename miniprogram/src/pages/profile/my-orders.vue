<template>
  <view class="page">
    <!-- Order Tabs -->
    <view class="tab-bar">
      <view
        class="tab-item"
        :class="{ active: activeTab === 'all' }"
        @tap="switchTab('all')"
      >全部</view>
      <view
        class="tab-item"
        :class="{ active: activeTab === 'pending' }"
        @tap="switchTab('pending')"
      >待支付</view>
      <view
        class="tab-item"
        :class="{ active: activeTab === 'paid' }"
        @tap="switchTab('paid')"
      >待收货</view>
      <view
        class="tab-item"
        :class="{ active: activeTab === 'completed' }"
        @tap="switchTab('completed')"
      >已完成</view>
    </view>

    <view v-if="loading && !orders.length" class="loading-state">
      <text>加载中...</text>
    </view>

    <view v-else-if="!orders.length" class="empty-state">
      <text class="empty-icon">📦</text>
      <text class="empty-text">暂无订单</text>
      <button class="btn-primary btn-shop" @tap="goShop">去逛逛</button>
    </view>

    <view v-else class="order-list">
      <view v-for="order in filteredOrders" :key="order.id || order.orderNo" class="order-card">
        <view class="order-header flex-between">
          <text class="order-no text-sm text-secondary">订单号：{{ order.orderNo }}</text>
          <text class="order-status" :class="getStatusClass(order.status)">{{ getStatusText(order.status) }}</text>
        </view>

        <view class="order-items">
          <view class="order-item flex" v-for="(item, idx) in (order.items || [order])" :key="idx">
            <image v-if="item.productImage || item.imageUrl" class="item-img" :src="item.productImage || item.imageUrl" mode="aspectFill" />
            <view class="item-info flex-1">
              <text class="item-name">{{ item.productName || item.name || '商品' }}</text>
              <text class="item-spec text-sm text-secondary" v-if="item.specification">{{ item.specification }}</text>
              <view class="flex-between">
                <text class="item-price">¥{{ item.price || item.unitPrice }}</text>
                <text class="item-qty text-sm text-secondary">x{{ item.quantity || 1 }}</text>
              </view>
            </view>
          </view>
        </view>

        <view class="order-footer flex-between">
          <text class="order-time text-sm text-secondary">{{ formatTime(order.createdAt) }}</text>
          <view class="order-total">
            <text class="text-sm">合计：</text>
            <text class="total-price">¥{{ order.totalAmount || order.totalPrice || 0 }}</text>
          </view>
        </view>

        <view class="order-actions flex" v-if="order.status === 'PENDING'">
          <button class="btn-small btn-pay" @tap="payOrder(order.orderNo)">立即支付</button>
        </view>
        <view class="order-actions flex" v-else-if="order.status === 'PAID' || order.status === 'SHIPPED'">
          <button class="btn-small btn-confirm" @tap="confirmReceive(order.orderNo)">确认收货</button>
        </view>
      </view>
    </view>

    <view v-if="!loading && noMore && filteredOrders.length" class="no-more">
      <text class="text-sm text-secondary">没有更多了</text>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { onShow, onReachBottom } from '@dcloudio/uni-app'
import { productApi } from '@/services/api'
import { checkLogin, formatTime } from '@/utils/common'

const activeTab = ref('all')
const orders = ref<any[]>([])
const loading = ref(false)
const page = ref(1)
const noMore = ref(false)

const filteredOrders = computed(() => {
  if (activeTab.value === 'all') return orders.value
  const statusMap: Record<string, string[]> = {
    pending: ['PENDING'],
    paid: ['PAID', 'SHIPPED'],
    completed: ['COMPLETED', 'RECEIVED']
  }
  const statuses = statusMap[activeTab.value] || []
  return orders.value.filter(o => statuses.includes(o.status))
})

function getStatusText(status: string) {
  const map: Record<string, string> = {
    PENDING: '待支付', PAID: '已支付', SHIPPED: '已发货',
    RECEIVED: '已收货', COMPLETED: '已完成', CANCELLED: '已取消', REFUNDED: '已退款'
  }
  return map[status] || status
}

function getStatusClass(status: string) {
  if (['PENDING'].includes(status)) return 'status-pending'
  if (['PAID', 'SHIPPED'].includes(status)) return 'status-active'
  if (['COMPLETED', 'RECEIVED'].includes(status)) return 'status-done'
  return 'status-cancelled'
}

function switchTab(tab: string) {
  activeTab.value = tab
}

async function loadOrders(reset = false) {
  if (loading.value) return
  if (reset) { page.value = 1; noMore.value = false }
  loading.value = true
  try {
    const res = await productApi.getOrders({ page: page.value, size: 20 })
    if (res.code === 200) {
      const list = res.data?.content || res.data?.records || res.data?.list || res.data || []
      orders.value = reset ? list : [...orders.value, ...list]
      if (list.length < 20) noMore.value = true
      else page.value++
    }
  } catch {} finally {
    loading.value = false
  }
}

async function payOrder(orderNo: string) {
  try {
    uni.showLoading({ title: '支付中...' })
    const res = await productApi.simulatePay(orderNo)
    uni.hideLoading()
    if (res.code === 200) {
      uni.showToast({ title: '支付成功', icon: 'success' })
      loadOrders(true)
    } else {
      uni.showToast({ title: res.message || '支付失败', icon: 'none' })
    }
  } catch { uni.hideLoading(); uni.showToast({ title: '支付失败', icon: 'none' }) }
}

async function confirmReceive(orderNo: string) {
  uni.showModal({
    title: '提示', content: '确认已收到商品？',
    success: async (r) => {
      if (!r.confirm) return
      try {
        await productApi.confirmReceive(orderNo)
        uni.showToast({ title: '已确认收货', icon: 'success' })
        loadOrders(true)
      } catch { uni.showToast({ title: '操作失败', icon: 'none' }) }
    }
  })
}

function goShop() {
  uni.navigateTo({ url: '/pages/product-shop/index' })
}

onShow(() => {
  if (checkLogin()) loadOrders(true)
})

onReachBottom(() => {
  if (!noMore.value) loadOrders()
})
</script>

<style scoped lang="scss">
.page {
  min-height: 100vh;
  background: $background;
  font-family: 'Inter', sans-serif;
}

.tab-bar {
  display: flex;
  background: $card;
  position: sticky;
  top: 0;
  z-index: 10;
  border-bottom: 1rpx solid $border;
  box-shadow: $shadow-sm;
}

.tab-item {
  flex: 1;
  text-align: center;
  padding: 24rpx 0;
  font-size: 28rpx;
  color: $muted-foreground;
  position: relative;
  font-family: 'Inter', sans-serif;
  transition: color 0.2s;
}

.tab-item.active {
  color: $accent;
  font-weight: 600;
}

.tab-item.active::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 50%;
  transform: translateX(-50%);
  width: 60rpx;
  height: 4rpx;
  background: $accent;
  border-radius: 2rpx;
}

.loading-state, .empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 120rpx 0;
}

.empty-icon { font-size: 80rpx; margin-bottom: 20rpx; }
.empty-text { font-size: 28rpx; color: $muted-foreground; margin-bottom: 30rpx; }

.btn-shop {
  width: 240rpx;
  height: 72rpx;
  font-size: 28rpx;
  border-radius: $radius-full;
  background: $accent;
  color: #fff;
  border: none;
  box-shadow: $shadow-accent;
  font-family: 'Inter', sans-serif;
}

.order-list { padding: 20rpx 24rpx; }

.order-card {
  background: $card;
  border-radius: $radius-xl;
  padding: 24rpx;
  margin-bottom: 20rpx;
  border: 1rpx solid $border;
  box-shadow: $shadow-sm;
}

.order-header { margin-bottom: 20rpx; }
.order-no { font-size: 24rpx; color: $muted-foreground; }
.order-status { font-size: 24rpx; font-weight: 600; font-family: 'JetBrains Mono', monospace; }
.status-pending { color: $uni-warning; }
.status-active { color: $accent; }
.status-done { color: $uni-success; }
.status-cancelled { color: $muted-foreground; }

.order-item {
  padding: 12rpx 0;
  border-bottom: 1rpx solid $border;
  gap: 20rpx;
}

.order-item:last-child { border-bottom: none; }

.item-img {
  width: 140rpx;
  height: 140rpx;
  border-radius: $radius-lg;
  background: $muted;
  flex-shrink: 0;
}

.item-info { min-width: 0; }

.item-name {
  font-size: 28rpx;
  color: $foreground;
  display: block;
  margin-bottom: 8rpx;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-family: 'Inter', sans-serif;
}

.item-spec { display: block; margin-bottom: 8rpx; color: $muted-foreground; }
.item-price { font-size: 28rpx; color: $accent; font-weight: 600; font-family: 'JetBrains Mono', monospace; }
.item-qty { font-size: 24rpx; color: $muted-foreground; }

.order-footer {
  margin-top: 16rpx;
  padding-top: 16rpx;
  border-top: 1rpx solid $border;
}

.total-price { font-size: 32rpx; color: $accent; font-weight: 700; font-family: 'JetBrains Mono', monospace; }

.order-actions {
  margin-top: 16rpx;
  justify-content: flex-end;
  gap: 16rpx;
}

.btn-small {
  font-size: 24rpx;
  padding: 12rpx 32rpx;
  border-radius: $radius-full;
  line-height: 1.4;
  height: auto;
  min-height: 0;
  border: none;
  font-family: 'Inter', sans-serif;
}

.btn-pay {
  background: $accent;
  color: #fff;
  box-shadow: $shadow-accent;
}

.btn-confirm {
  background: $muted;
  color: $accent;
  border: 1rpx solid $border;
}

.no-more { text-align: center; padding: 30rpx 0; color: $muted-foreground; }
</style>
