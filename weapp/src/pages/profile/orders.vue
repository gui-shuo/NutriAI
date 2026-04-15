<template>
  <view class="orders-page">
    <!-- Status Tabs -->
    <view class="status-tabs">
      <view
        v-for="tab in tabs"
        :key="tab.key"
        :class="['tab-item', { active: currentStatus === tab.key }]"
        @tap="switchTab(tab.key)"
      >
        <text>{{ tab.label }}</text>
      </view>
    </view>

    <!-- Order List -->
    <scroll-view
      scroll-y
      class="order-list"
      @scrolltolower="loadMore"
      refresher-enabled
      :refresher-triggered="refreshing"
      @refresherrefresh="onRefresh"
    >
      <view class="list-content">
        <view v-for="order in orders" :key="order.orderNo" class="order-card card">
          <view class="order-header">
            <text class="text-sm text-muted">{{ order.orderNo }}</text>
            <text
              class="status-text"
              :style="{ color: getStatusColor(order.orderStatus || order.status) }"
            >
              {{ getStatusText(order.orderStatus || order.status) }}
            </text>
          </view>

          <!-- Products in order -->
          <view class="order-items">
            <view v-for="item in (order.items || order.orderItems || [])" :key="item.productId" class="order-item">
              <image :src="item.imageUrl || item.productImageUrl || '/static/images/product-default.png'" mode="aspectFill" class="item-img" />
              <view class="item-info">
                <text class="item-name text-ellipsis">{{ item.productName || item.name }}</text>
                <view class="flex-between">
                  <text class="text-price text-sm">¥{{ item.price }}</text>
                  <text class="text-muted text-sm">x{{ item.quantity }}</text>
                </view>
              </view>
            </view>
          </view>

          <view class="order-footer">
            <text class="text-sm text-secondary">共{{ getItemCount(order) }}件商品</text>
            <view class="flex items-center gap-sm">
              <text class="text-sm">合计：</text>
              <text class="text-price text-md">¥{{ order.totalAmount || '0.00' }}</text>
            </view>
          </view>

          <!-- Actions -->
          <view class="order-actions" v-if="hasActions(order)">
            <button
              v-if="isPending(order)"
              class="btn-primary btn-sm"
              @tap="payOrder(order.orderNo)"
            >
              去付款
            </button>
            <button
              v-if="isShipped(order)"
              class="btn-outline btn-sm"
              @tap="confirmReceive(order.orderNo)"
            >
              确认收货
            </button>
          </view>
        </view>

        <view v-if="loading" class="loading-hint">
          <text class="text-muted">加载中...</text>
        </view>
        <view v-else-if="!orders.length" class="empty-state">
          <text class="empty-icon">📋</text>
          <text class="empty-text">暂无订单</text>
        </view>
        <view v-else-if="noMore" class="loading-hint">
          <text class="text-muted text-sm">— 没有更多了 —</text>
        </view>
      </view>
    </scroll-view>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { onLoad, onShow } from '@dcloudio/uni-app'
import { productApi } from '../../services/api'
import { ORDER_STATUS_MAP, showConfirm } from '../../utils'

const tabs = [
  { key: '', label: '全部' },
  { key: 'PENDING', label: '待付款' },
  { key: 'PAID', label: '待发货' },
  { key: 'SHIPPED', label: '待收货' },
  { key: 'RECEIVED', label: '已完成' }
]

const currentStatus = ref('')
const orders = ref<any[]>([])
const loading = ref(false)
const refreshing = ref(false)
const noMore = ref(false)
const page = ref(0)

onLoad((query: any) => {
  if (query?.status) currentStatus.value = query.status
})

onShow(() => {
  loadOrders(true)
})

async function loadOrders(reset = false) {
  if (loading.value) return
  if (reset) { page.value = 0; noMore.value = false }
  loading.value = true
  try {
    const data: any = await productApi.getOrders({
      page: page.value,
      size: 10,
      status: currentStatus.value || undefined
    })
    const list = data?.content || data?.records || data || []
    if (reset) { orders.value = list } else { orders.value = [...orders.value, ...list] }
    if (list.length < 10) noMore.value = true
    page.value++
  } catch {}
  loading.value = false
  refreshing.value = false
}

function switchTab(key: string) {
  currentStatus.value = key
  loadOrders(true)
}

function onRefresh() { refreshing.value = true; loadOrders(true) }
function loadMore() { if (!noMore.value) loadOrders() }

function getStatusText(status: string) { return ORDER_STATUS_MAP[status]?.text || status }
function getStatusColor(status: string) { return ORDER_STATUS_MAP[status]?.color || '#64748B' }
function getItemCount(order: any) { return (order.items || order.orderItems || []).reduce((s: number, i: any) => s + (i.quantity || 1), 0) }

function isPending(order: any) { return (order.orderStatus || order.status) === 'PENDING' }
function isShipped(order: any) { return (order.orderStatus || order.status) === 'SHIPPED' }
function hasActions(order: any) { return isPending(order) || isShipped(order) }

async function payOrder(orderNo: string) {
  // TODO: Integrate WeChat Pay when approved
  try {
    await productApi.payOrder(orderNo)
    uni.showToast({ title: '支付成功', icon: 'success' })
    loadOrders(true)
  } catch (e: any) {
    uni.showToast({ title: e?.message || '支付失败', icon: 'none' })
  }
}

async function confirmReceive(orderNo: string) {
  const ok = await showConfirm('确认已收到商品？')
  if (!ok) return
  try {
    await productApi.confirmReceive(orderNo)
    uni.showToast({ title: '已确认收货', icon: 'success' })
    loadOrders(true)
  } catch (e: any) {
    uni.showToast({ title: e?.message || '操作失败', icon: 'none' })
  }
}
</script>

<style lang="scss" scoped>
.orders-page {
  min-height: 100vh;
  background: $bg-page;
  display: flex;
  flex-direction: column;
}

.status-tabs {
  display: flex;
  background: $bg-card;
  border-bottom: 1rpx solid $border-light;
  flex-shrink: 0;
}

.tab-item {
  flex: 1;
  text-align: center;
  padding: $spacing-md 0;
  font-size: $font-sm;
  color: $text-secondary;
  position: relative;

  &.active {
    color: $primary;
    font-weight: 600;
    &::after {
      content: '';
      position: absolute;
      bottom: 0;
      left: 50%;
      transform: translateX(-50%);
      width: 40rpx;
      height: 4rpx;
      background: $primary;
      border-radius: 2rpx;
    }
  }
}

.order-list { flex: 1; height: 0; }
.list-content { padding: $spacing-md $spacing-lg; }

.order-card { margin-bottom: $spacing-md; padding: $spacing-lg; }
.order-header {
  display: flex; align-items: center; justify-content: space-between;
  padding-bottom: $spacing-md; border-bottom: 1rpx solid $border-light;
}
.status-text { font-size: $font-sm; font-weight: 500; }

.order-items { padding: $spacing-md 0; }
.order-item {
  display: flex; gap: $spacing-md; padding: $spacing-sm 0;
}
.item-img { width: 120rpx; height: 120rpx; border-radius: $radius-md; background: $bg-muted; flex-shrink: 0; }
.item-info { flex: 1; min-width: 0; display: flex; flex-direction: column; justify-content: space-between; }
.item-name { font-size: $font-sm; color: $text-primary; }

.order-footer {
  display: flex; align-items: center; justify-content: space-between;
  padding-top: $spacing-md; border-top: 1rpx solid $border-light;
}

.order-actions {
  display: flex; justify-content: flex-end; gap: $spacing-sm;
  margin-top: $spacing-md;
}

.loading-hint { text-align: center; padding: $spacing-xl 0; }
.empty-state { display: flex; flex-direction: column; align-items: center; padding: 120rpx 0; }
.empty-icon { font-size: 80rpx; margin-bottom: $spacing-md; }
</style>
