<template>
  <view class="admin-page">
    <!-- Filters -->
    <view class="filter-bar">
      <scroll-view scroll-x :show-scrollbar="false">
        <view
          v-for="s in statusOptions"
          :key="s.key"
          class="filter-chip"
          :class="{ active: filterStatus === s.key }"
          @tap="setFilter(s.key)"
        >{{ s.label }}</view>
      </scroll-view>
    </view>

    <!-- Order List -->
    <scroll-view class="order-list" scroll-y @scrolltolower="loadMore">
      <view class="loading-state" v-if="loading && orders.length === 0">加载中...</view>
      <view class="empty-state" v-if="!loading && orders.length === 0">暂无订单</view>

      <view class="order-card" v-for="order in orders" :key="order.id">
        <view class="card-top">
          <text class="order-no">{{ order.orderNo }}</text>
          <view class="status-badge" :class="getStatusClass(order.orderStatus)">
            {{ getStatusLabel(order.orderStatus) }}
          </view>
        </view>

        <view class="order-info">
          <text class="info-row">买家ID: {{ order.userId }}</text>
          <text class="info-row">商品: {{ order.productName || '-' }}</text>
          <text class="info-row">数量: {{ order.quantity }} | 金额: ¥{{ formatPrice(order.totalAmount) }}</text>
          <text class="info-row">收货人: {{ order.receiverName }} {{ order.receiverPhone }}</text>
          <text class="info-row" v-if="order.receiverAddress">地址: {{ order.receiverAddress }}</text>
          <text class="info-row" v-if="order.trackingNo">快递: {{ order.shippingCompany }} {{ order.trackingNo }}</text>
          <text class="info-row time">{{ formatDate(order.createdAt) }}</text>
        </view>

        <view class="card-actions">
          <view class="action-btn" v-if="order.orderStatus === 'PAID'" @tap="openShip(order)">发货</view>
          <view class="action-btn danger" v-if="['PAID','SHIPPED'].includes(order.orderStatus)" @tap="refundOrder(order)">退款</view>
          <view class="action-btn" v-if="order.orderStatus === 'SHIPPED'" @tap="completeOrder(order)">完成</view>
        </view>
      </view>

      <view class="load-more" v-if="!loading && hasMore">上拉加载更多</view>
      <view class="no-more" v-if="!loading && !hasMore && orders.length > 0">没有更多了</view>
    </scroll-view>

    <!-- Ship Modal -->
    <view class="ship-modal" v-if="showShipModal">
      <view class="modal-mask" @tap="showShipModal = false"></view>
      <view class="modal-body">
        <view class="modal-header">
          <text class="modal-title">填写发货信息</text>
          <text class="modal-close" @tap="showShipModal = false">✕</text>
        </view>
        <view class="modal-content">
          <view class="form-item">
            <text class="form-label">快递公司</text>
            <input class="form-input" v-model="shipForm.company" placeholder="如：顺丰速运" />
          </view>
          <view class="form-item">
            <text class="form-label">快递单号</text>
            <input class="form-input" v-model="shipForm.trackingNo" placeholder="请输入快递单号" />
          </view>
        </view>
        <view class="modal-footer">
          <view class="btn-cancel" @tap="showShipModal = false">取消</view>
          <view class="btn-confirm" @tap="confirmShip">确认发货</view>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { adminApi } from '@/services/api'

const statusOptions = [
  { key: '', label: '全部' },
  { key: 'PENDING_PAYMENT', label: '待付款' },
  { key: 'PAID', label: '待发货' },
  { key: 'SHIPPED', label: '已发货' },
  { key: 'COMPLETED', label: '已完成' },
  { key: 'CANCELLED', label: '已取消' }
]

const orders = ref<any[]>([])
const loading = ref(false)
const page = ref(0)
const hasMore = ref(true)
const filterStatus = ref('')
const showShipModal = ref(false)
const shipTargetOrder = ref<any>(null)
const shipForm = ref({ company: '', trackingNo: '' })

function setFilter(status: string) {
  filterStatus.value = status
  orders.value = []
  page.value = 0
  hasMore.value = true
  loadOrders()
}

async function loadOrders() {
  if (loading.value || !hasMore.value) return
  loading.value = true
  try {
    const res: any = await adminApi.getProductOrders({ status: filterStatus.value, page: page.value, size: 20 })
    const data = res.data || {}
    const list = data.content || data.list || data || []
    if (page.value === 0) {
      orders.value = list
    } else {
      orders.value.push(...list)
    }
    hasMore.value = list.length >= 20
    page.value++
  } catch (e) {
    console.error('Load product orders error:', e)
  } finally {
    loading.value = false
  }
}

function loadMore() { loadOrders() }

function openShip(order: any) {
  shipTargetOrder.value = order
  shipForm.value = { company: '', trackingNo: '' }
  showShipModal.value = true
}

async function confirmShip() {
  if (!shipForm.value.company.trim()) return uni.showToast({ title: '请输入快递公司', icon: 'none' })
  if (!shipForm.value.trackingNo.trim()) return uni.showToast({ title: '请输入快递单号', icon: 'none' })
  try {
    await adminApi.shipProductOrder(shipTargetOrder.value.orderNo, {
      shippingCompany: shipForm.value.company,
      trackingNo: shipForm.value.trackingNo
    })
    shipTargetOrder.value.orderStatus = 'SHIPPED'
    shipTargetOrder.value.shippingCompany = shipForm.value.company
    shipTargetOrder.value.trackingNo = shipForm.value.trackingNo
    showShipModal.value = false
    uni.showToast({ title: '发货成功', icon: 'success' })
  } catch (e: any) {
    uni.showToast({ title: e?.message || '操作失败', icon: 'none' })
  }
}

async function refundOrder(order: any) {
  uni.showModal({
    title: '确认退款',
    content: `确认对订单 ${order.orderNo} 进行退款？`,
    success: async (res) => {
      if (!res.confirm) return
      try {
        await adminApi.updateProductOrderStatus(order.orderNo, { status: 'REFUNDED' })
        order.orderStatus = 'REFUNDED'
        uni.showToast({ title: '退款成功', icon: 'success' })
      } catch (e: any) {
        uni.showToast({ title: e?.message || '操作失败', icon: 'none' })
      }
    }
  })
}

async function completeOrder(order: any) {
  uni.showModal({
    title: '确认完成',
    content: '确认将此订单标记为已完成？',
    success: async (res) => {
      if (!res.confirm) return
      try {
        await adminApi.updateProductOrderStatus(order.orderNo, { status: 'COMPLETED' })
        order.orderStatus = 'COMPLETED'
        uni.showToast({ title: '已完成', icon: 'success' })
      } catch (e: any) {
        uni.showToast({ title: e?.message || '操作失败', icon: 'none' })
      }
    }
  })
}

function getStatusLabel(status: string) {
  const map: Record<string, string> = {
    PENDING_PAYMENT: '待付款', PAID: '待发货', SHIPPED: '已发货',
    DELIVERED: '待收货', COMPLETED: '已完成', CANCELLED: '已取消', REFUNDED: '已退款'
  }
  return map[status] || status
}

function getStatusClass(status: string) {
  if (status === 'COMPLETED') return 'status-done'
  if (status === 'CANCELLED' || status === 'REFUNDED') return 'status-cancelled'
  if (status === 'PAID' || status === 'SHIPPED') return 'status-active'
  if (status === 'PENDING_PAYMENT') return 'status-pending'
  return ''
}

function formatPrice(v: any) { return Number(v || 0).toFixed(2) }
function formatDate(dt: string) {
  if (!dt) return ''
  return dt.substring(0, 16).replace('T', ' ')
}

onShow(loadOrders)
</script>

<style lang="scss" scoped>
.admin-page {
  min-height: 100vh;
  background: $background;
  width: 100%;
  box-sizing: border-box;
  overflow-x: hidden;
}

.filter-bar {
  background: $card;
  border-bottom: 1rpx solid $border;
  padding: 16rpx 20rpx;
  white-space: nowrap;
}

.filter-chip {
  display: inline-block;
  padding: 12rpx 28rpx;
  border-radius: 30rpx;
  font-size: 26rpx;
  color: #64748b;
  background: $background;
  margin-right: 12rpx;
  border: 1rpx solid $border;
}

.filter-chip.active {
  background: $accent;
  color: white;
  border-color: $accent;
}

.order-list {
  height: calc(100vh - 120rpx);
  padding: 16rpx 20rpx;
}

.loading-state, .empty-state {
  text-align: center;
  padding: 80rpx;
  color: #94a3b8;
  font-size: 28rpx;
}

.order-card {
  background: $card;
  border-radius: 16rpx;
  padding: 24rpx;
  margin-bottom: 20rpx;
  border: 1rpx solid $border;
}

.card-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16rpx;
}

.order-no {
  font-size: 24rpx;
  color: #94a3b8;
  word-break: break-all;
  flex: 1;
  margin-right: 12rpx;
}

.status-badge {
  font-size: 22rpx;
  padding: 6rpx 16rpx;
  border-radius: 20rpx;
  font-weight: 600;
}

.status-pending { background: rgba(245,158,11,0.1); color: #f59e0b; }
.status-active { background: rgba(16,185,129,0.1); color: $accent; }
.status-done { background: rgba(148,163,184,0.1); color: #94a3b8; }
.status-cancelled { background: rgba(239,68,68,0.1); color: #ef4444; }

.order-info {
  display: flex;
  flex-direction: column;
  gap: 8rpx;
  padding: 16rpx 0;
  border-top: 1rpx solid $border;
  border-bottom: 1rpx solid $border;
  margin-bottom: 16rpx;
}

.info-row {
  font-size: 26rpx;
  color: $foreground;
  display: block;
  word-break: break-all;
}

.info-row.time {
  color: #94a3b8;
  font-size: 24rpx;
}

.card-actions {
  display: flex;
  gap: 16rpx;
  justify-content: flex-end;
}

.action-btn {
  font-size: 26rpx;
  padding: 14rpx 32rpx;
  border-radius: 30rpx;
  background: $accent;
  color: white;
}

.action-btn.danger {
  background: #ef4444;
}

.load-more, .no-more {
  text-align: center;
  padding: 30rpx;
  color: #94a3b8;
  font-size: 24rpx;
}

.ship-modal {
  position: fixed;
  inset: 0;
  z-index: 1000;
}

.modal-mask {
  position: absolute;
  inset: 0;
  background: rgba(0,0,0,0.5);
}

.modal-body {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  background: white;
  border-radius: 24rpx 24rpx 0 0;
  box-sizing: border-box;
  overflow-x: hidden;
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 28rpx 28rpx 16rpx;
  border-bottom: 1rpx solid $border;
}

.modal-title {
  font-size: 30rpx;
  font-weight: 600;
  color: $foreground;
}

.modal-close {
  font-size: 28rpx;
  color: #94a3b8;
}

.modal-content {
  padding: 20rpx 28rpx;
}

.form-item {
  display: flex;
  align-items: center;
  padding: 16rpx 0;
  border-bottom: 1rpx solid $border;
}

.form-label {
  width: 120rpx;
  font-size: 26rpx;
  color: #64748b;
  flex-shrink: 0;
}

.form-input {
  flex: 1;
  font-size: 28rpx;
  color: $foreground;
  padding: 0 16rpx;
}

.modal-footer {
  display: flex;
  gap: 20rpx;
  padding: 20rpx 28rpx;
  padding-bottom: calc(20rpx + env(safe-area-inset-bottom));
}

.btn-cancel, .btn-confirm {
  flex: 1;
  text-align: center;
  padding: 22rpx;
  border-radius: 40rpx;
  font-size: 28rpx;
}

.btn-cancel {
  background: $background;
  color: #64748b;
  border: 1rpx solid $border;
}

.btn-confirm {
  background: $accent;
  color: white;
  font-weight: 600;
}
</style>
