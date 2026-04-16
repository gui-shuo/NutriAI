<template>
  <view class="orders-page">
    <!-- Status Tabs -->
    <scroll-view class="status-tabs" scroll-x :show-scrollbar="false">
      <view
        v-for="tab in tabs"
        :key="tab.key"
        class="tab"
        :class="{ active: currentTab === tab.key }"
        @tap="switchTab(tab.key)"
      >{{ tab.label }}</view>
    </scroll-view>

    <!-- Order List -->
    <scroll-view class="order-list" scroll-y @scrolltolower="loadMore">
      <view class="loading-hint" v-if="loading && orders.length === 0">
        <text>加载中...</text>
      </view>

      <view class="empty-state" v-if="!loading && orders.length === 0">
        <text class="empty-icon">📦</text>
        <text class="empty-text">暂无订单</text>
      </view>

      <view class="order-card" v-for="order in filteredOrders()" :key="order.id">
        <view class="card-header">
          <text class="order-no">{{ order.orderNo }}</text>
          <text class="order-status" :class="getStatusClass(order.orderStatus, order.orderNo)">
            {{ getStatusLabel(order.orderStatus, order.orderNo) }}
          </text>
        </view>

        <view class="card-product">
          <image class="product-img" :src="order.imageUrl || '/static/images/product-placeholder.png'" mode="aspectFill" />
          <view class="product-info">
            <text class="product-name">{{ order.productName || '营养产品' }}</text>
            <text class="product-qty">× {{ order.quantity }}</text>
          </view>
          <text class="product-price">¥{{ formatPrice(order.totalAmount) }}</text>
        </view>

        <view class="card-footer">
          <text class="order-time">{{ formatDate(order.createdAt) }}</text>
          <view class="action-buttons">
            <view class="action-btn outline" v-if="order.orderStatus === 'PENDING_PAYMENT'" @tap="cancelOrder(order)">
              取消订单
            </view>
            <view class="action-btn outline" v-if="order.orderStatus === 'DELIVERED'" @tap="confirmReceipt(order)">
              确认收货
            </view>
            <view class="action-btn outline" v-if="canRefund(order)" @tap="applyRefund(order)">
              申请退款
            </view>
            <view class="action-btn primary" v-if="order.orderStatus === 'SHIPPED'" @tap="viewLogistics(order)">
              查看物流
            </view>
            <view class="action-btn outline" v-if="['COMPLETED'].includes(order.orderStatus)" @tap="buyAgain(order)">
              再次购买
            </view>
          </view>
        </view>

        <!-- Shipping Info -->
        <view class="shipping-info" v-if="order.trackingNo && order.shippingCompany">
          <text class="shipping-text">{{ order.shippingCompany }}: {{ order.trackingNo }}</text>
        </view>
      </view>

      <view class="load-more-hint" v-if="!loading && hasMore">
        <text>上拉加载更多</text>
      </view>
      <view class="no-more-hint" v-if="!loading && !hasMore && orders.length > 0">
        <text>没有更多了</text>
      </view>
    </scroll-view>

    <!-- Refund Modal -->
    <view class="refund-modal" v-if="showRefundModal">
      <view class="modal-mask" @tap="showRefundModal = false"></view>
      <view class="modal-body">
        <view class="modal-header">
          <text class="modal-title">申请退款</text>
          <text class="modal-close" @tap="showRefundModal = false">✕</text>
        </view>
        <view class="modal-content">
          <view class="form-item">
            <text class="form-label">退款原因</text>
            <textarea class="form-textarea" v-model="refundReason" placeholder="请描述退款原因" :maxlength="200" />
          </view>
        </view>
        <view class="modal-footer">
          <view class="modal-btn cancel" @tap="showRefundModal = false">取消</view>
          <view class="modal-btn confirm" @tap="submitRefund">提交申请</view>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { productApi, refundApi } from '@/services/api'

const tabs = [
  { key: '', label: '全部' },
  { key: 'PENDING_PAYMENT', label: '待付款' },
  { key: 'PAID', label: '待发货' },
  { key: 'SHIPPED', label: '已发货' },
  { key: 'DELIVERED', label: '待收货' },
  { key: 'COMPLETED', label: '已完成' },
  { key: 'refund', label: '退款/售后' }
]

const currentTab = ref('')
const orders = ref<any[]>([])
const refundMap = ref<Record<string, any>>({})
const loading = ref(false)
const page = ref(0)
const hasMore = ref(true)

const showRefundModal = ref(false)
const refundReason = ref('')
const refundTargetOrder = ref<any>(null)

function switchTab(key: string) {
  currentTab.value = key
  if (key === 'refund') return
  orders.value = []
  page.value = 0
  hasMore.value = true
  loadOrders()
}

function filteredOrders() {
  if (currentTab.value === 'refund') {
    return orders.value.filter(o => {
      const r = refundMap.value[o.orderNo]
      return r && ['PENDING', 'APPROVED', 'PROCESSING'].includes(r.status)
    })
  }
  return orders.value
}

async function loadOrders() {
  if (loading.value || !hasMore.value) return
  loading.value = true
  try {
    const isReset = page.value === 0
    const [res, refundRes] = await Promise.all([
      productApi.getMyOrders({ status: currentTab.value === 'refund' ? '' : currentTab.value, page: page.value, size: 10 }),
      isReset ? refundApi.getMyRefunds({ page: 0, size: 100 }) : Promise.resolve(null)
    ])
    const data = res.data || {}
    const list = data.content || data.list || data || []
    if (isReset) {
      orders.value = list
    } else {
      orders.value.push(...list)
    }
    hasMore.value = list.length >= 10
    page.value++

    if (refundRes && refundRes.code === 200) {
      const refunds = refundRes.data?.content || refundRes.data || []
      const map: Record<string, any> = {}
      refunds.forEach((r: any) => { if (r.orderType === 'PRODUCT') map[r.orderNo] = r })
      refundMap.value = map
    }
  } catch (e) {
    console.error('Load orders error:', e)
  } finally {
    loading.value = false
  }
}

function loadMore() {
  loadOrders()
}

async function cancelOrder(order: any) {
  uni.showModal({
    title: '确认取消',
    content: '确定取消此订单吗？',
    success: async (res) => {
      if (!res.confirm) return
      try {
        await productApi.cancelOrder(order.orderNo)
        order.orderStatus = 'CANCELLED'
        uni.showToast({ title: '已取消', icon: 'success' })
      } catch (e: any) {
        uni.showToast({ title: e?.message || '取消失败', icon: 'none' })
      }
    }
  })
}

async function confirmReceipt(order: any) {
  uni.showModal({
    title: '确认收货',
    content: '确认已收到商品？',
    success: async (res) => {
      if (!res.confirm) return
      try {
        await productApi.confirmReceipt(order.orderNo)
        order.orderStatus = 'COMPLETED'
        uni.showToast({ title: '已确认收货', icon: 'success' })
      } catch (e: any) {
        uni.showToast({ title: e?.message || '操作失败', icon: 'none' })
      }
    }
  })
}

function applyRefund(order: any) {
  refundTargetOrder.value = order
  refundReason.value = ''
  showRefundModal.value = true
}

async function submitRefund() {
  if (!refundReason.value.trim()) {
    uni.showToast({ title: '请填写退款原因', icon: 'none' })
    return
  }
  try {
    await refundApi.apply({
      orderNo: refundTargetOrder.value.orderNo,
      orderType: 'PRODUCT',
      refundAmount: refundTargetOrder.value.totalAmount,
      reason: refundReason.value
    })
    showRefundModal.value = false
    uni.showToast({ title: '退款申请已提交', icon: 'success' })
    orders.value = []
    page.value = 0
    hasMore.value = true
    loadOrders()
  } catch (e: any) {
    uni.showToast({ title: e?.message || '申请失败', icon: 'none' })
  }
}

function viewLogistics(order: any) {
  if (order.trackingNo) {
    uni.showModal({
      title: '物流信息',
      content: `快递公司：${order.shippingCompany || '未知'}\n快递单号：${order.trackingNo}`,
      showCancel: false
    })
  } else {
    uni.showToast({ title: '暂无物流信息', icon: 'none' })
  }
}

function buyAgain(order: any) {
  if (order.productId) {
    uni.navigateTo({ url: `/pages/product-shop/detail?id=${order.productId}` })
  } else {
    uni.switchTab({ url: '/pages/product-shop/index' })
  }
}

function getStatusLabel(status: string, orderNo?: string) {
  if (orderNo) {
    const r = refundMap.value[orderNo]
    if (r) {
      const rMap: Record<string, string> = { PENDING: '退款申请中', APPROVED: '退款审批通过', PROCESSING: '退款处理中', COMPLETED: '已退款', REJECTED: '退款被拒绝' }
      if (['PENDING', 'APPROVED', 'PROCESSING'].includes(r.status)) return rMap[r.status] || '退款中'
      if (r.status === 'COMPLETED') return '已退款'
    }
  }
  const map: Record<string, string> = {
    PENDING_PAYMENT: '待付款',
    PAID: '待发货',
    SHIPPED: '已发货',
    DELIVERED: '待收货',
    COMPLETED: '已完成',
    CANCELLED: '已取消',
    REFUNDING: '退款中'
  }
  return map[status] || status
}

function getStatusClass(status: string, orderNo?: string) {
  if (orderNo) {
    const r = refundMap.value[orderNo]
    if (r && ['PENDING', 'APPROVED', 'PROCESSING'].includes(r.status)) return 'status-refunding'
    if (r && r.status === 'COMPLETED') return 'status-cancelled'
  }
  if (status === 'COMPLETED') return 'status-done'
  if (status === 'CANCELLED') return 'status-cancelled'
  if (status === 'PAID' || status === 'SHIPPED' || status === 'DELIVERED') return 'status-active'
  if (status === 'PENDING_PAYMENT') return 'status-pending'
  return ''
}

function canRefund(order: any): boolean {
  if (!['PAID', 'SHIPPED', 'DELIVERED'].includes(order.orderStatus)) return false
  const r = refundMap.value[order.orderNo]
  if (r && ['PENDING', 'APPROVED', 'PROCESSING'].includes(r.status)) return false
  return true
}

function formatPrice(v: any) {
  return Number(v || 0).toFixed(2)
}

function formatDate(dt: string) {
  if (!dt) return ''
  return dt.substring(0, 16).replace('T', ' ')
}

onShow(() => {
  orders.value = []
  page.value = 0
  hasMore.value = true
  loadOrders()
})
</script>

<style lang="scss" scoped>
.orders-page {
  min-height: 100vh;
  background: $background;
  display: flex;
  flex-direction: column;
  width: 100%;
  box-sizing: border-box;
  overflow-x: hidden;
}

.status-tabs {
  background: $card;
  border-bottom: 1rpx solid $border;
  white-space: nowrap;
  padding: 0 16rpx;
}

.tab {
  display: inline-block;
  padding: 24rpx 24rpx;
  font-size: 26rpx;
  color: #64748b;
  position: relative;
}

.tab.active {
  color: $accent;
  font-weight: 600;
}

.tab.active::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 50%;
  transform: translateX(-50%);
  width: 40rpx;
  height: 4rpx;
  background: $accent;
  border-radius: 2rpx;
}

.order-list {
  flex: 1;
  padding: 16rpx 20rpx;
  height: calc(100vh - 90rpx);
  box-sizing: border-box;
  width: 100%;
  overflow-x: hidden;
}

.loading-hint, .empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding-top: 120rpx;
  gap: 20rpx;
  color: #94a3b8;
}

.empty-icon { font-size: 80rpx; }
.empty-text { font-size: 30rpx; }

.order-card {
  background: $card;
  border-radius: 16rpx;
  padding: 24rpx;
  margin-bottom: 20rpx;
  border: 1rpx solid $border;
  width: 100%;
  box-sizing: border-box;
  overflow: hidden;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16rpx;
}

.order-no {
  font-size: 24rpx;
  color: #94a3b8;
}

.order-status {
  font-size: 26rpx;
  font-weight: 600;
}

.status-pending { color: #f59e0b; }
.status-active { color: $accent; }
.status-done { color: #94a3b8; }
.status-cancelled { color: #ef4444; }
.status-refunding { color: #e67e22; }

.card-product {
  display: flex;
  align-items: center;
  gap: 16rpx;
  padding: 16rpx 0;
  border-top: 1rpx solid $border;
  border-bottom: 1rpx solid $border;
}

.product-img {
  width: 100rpx;
  height: 100rpx;
  border-radius: 10rpx;
  flex-shrink: 0;
}

.product-info {
  flex: 1;
}

.product-name {
  font-size: 28rpx;
  color: $foreground;
  display: block;
}

.product-qty {
  font-size: 24rpx;
  color: #94a3b8;
  display: block;
  margin-top: 6rpx;
}

.product-price {
  font-size: 30rpx;
  font-weight: 700;
  color: #ef4444;
}

.card-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 16rpx;
}

.order-time {
  font-size: 24rpx;
  color: #94a3b8;
}

.action-buttons {
  display: flex;
  gap: 12rpx;
}

.action-btn {
  font-size: 24rpx;
  padding: 12rpx 24rpx;
  border-radius: 30rpx;
}

.action-btn.outline {
  border: 1rpx solid $border;
  color: #64748b;
}

.action-btn.primary {
  background: $accent;
  color: white;
}

.shipping-info {
  margin-top: 12rpx;
  padding-top: 12rpx;
  border-top: 1rpx solid $border;
}

.shipping-text {
  font-size: 24rpx;
  color: #64748b;
}

.load-more-hint, .no-more-hint {
  text-align: center;
  padding: 30rpx;
  font-size: 24rpx;
  color: #94a3b8;
}

.refund-modal {
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
  padding: 32rpx 32rpx 20rpx;
  border-bottom: 1rpx solid $border;
}

.modal-title {
  font-size: 32rpx;
  font-weight: 600;
  color: $foreground;
}

.modal-close {
  font-size: 30rpx;
  color: #94a3b8;
}

.modal-content {
  padding: 24rpx 32rpx;
}

.form-item {
  display: flex;
  flex-direction: column;
  gap: 12rpx;
}

.form-label {
  font-size: 26rpx;
  color: #64748b;
}

.form-textarea {
  border: 1rpx solid $border;
  border-radius: 10rpx;
  padding: 16rpx;
  font-size: 26rpx;
  color: $foreground;
  min-height: 150rpx;
}

.modal-footer {
  display: flex;
  gap: 20rpx;
  padding: 20rpx 32rpx;
  padding-bottom: calc(20rpx + env(safe-area-inset-bottom));
  border-top: 1rpx solid $border;
}

.modal-btn {
  flex: 1;
  text-align: center;
  padding: 24rpx;
  border-radius: 40rpx;
  font-size: 28rpx;
}

.modal-btn.cancel {
  background: $background;
  color: #64748b;
  border: 1rpx solid $border;
}

.modal-btn.confirm {
  background: $accent;
  color: white;
  font-weight: 600;
}
</style>
