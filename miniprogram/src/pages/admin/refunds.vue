<template>
  <view class="admin-page">
    <!-- Status Filter -->
    <scroll-view class="filter-bar" scroll-x :show-scrollbar="false">
      <view
        v-for="s in statusOptions"
        :key="s.key"
        class="filter-chip"
        :class="{ active: filterStatus === s.key }"
        @tap="setFilter(s.key)"
      >{{ s.label }}</view>
    </scroll-view>

    <!-- Refund List -->
    <scroll-view class="refund-list" scroll-y @scrolltolower="loadMore">
      <view class="loading-state" v-if="loading && refunds.length === 0">加载中...</view>
      <view class="empty-state" v-if="!loading && refunds.length === 0">暂无退款申请</view>

      <view class="refund-card" v-for="rf in refunds" :key="rf.id">
        <view class="card-top">
          <text class="order-no">订单: {{ rf.orderNo }}</text>
          <view class="status-badge" :class="getStatusClass(rf.status)">{{ getStatusLabel(rf.status) }}</view>
        </view>
        <view class="refund-info">
          <text class="info-row">类型: {{ rf.orderType === 'PRODUCT' ? '产品订单' : '营养餐订单' }}</text>
          <text class="info-row">退款金额: ¥{{ formatPrice(rf.refundAmount) }}</text>
          <text class="info-row">原因: {{ rf.reason }}</text>
          <text class="info-row time">申请时间: {{ formatDate(rf.createdAt) }}</text>
          <text class="info-row" v-if="rf.adminRemark">备注: {{ rf.adminRemark }}</text>
        </view>
        <view class="card-actions" v-if="rf.status === 'PENDING'">
          <view class="action-btn" @tap="openRemark(rf, 'approve')">批准</view>
          <view class="action-btn danger" @tap="openRemark(rf, 'reject')">拒绝</view>
        </view>
        <view class="card-actions" v-if="rf.status === 'APPROVED'">
          <view class="action-btn" @tap="completeRefund(rf)">完成退款</view>
        </view>
      </view>

      <view class="load-more" v-if="!loading && hasMore">上拉加载更多</view>
      <view class="no-more" v-if="!loading && !hasMore && refunds.length > 0">没有更多了</view>
    </scroll-view>

    <!-- Remark Modal -->
    <view class="remark-modal" v-if="showRemarkModal">
      <view class="modal-mask" @tap="showRemarkModal = false"></view>
      <view class="modal-body">
        <view class="modal-header">
          <text class="modal-title">{{ remarkAction === 'approve' ? '批准退款' : '拒绝退款' }}</text>
          <text class="modal-close" @tap="showRemarkModal = false">✕</text>
        </view>
        <view class="modal-content">
          <textarea class="remark-input" v-model="adminRemark" placeholder="填写备注（可选）" :maxlength="200" />
        </view>
        <view class="modal-footer">
          <view class="btn-cancel" @tap="showRemarkModal = false">取消</view>
          <view class="btn-confirm" :class="{ danger: remarkAction === 'reject' }" @tap="confirmAction">
            {{ remarkAction === 'approve' ? '确认批准' : '确认拒绝' }}
          </view>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { adminApi } from '@/services/api'

const statusOptions = [
  { key: '', label: '全部' },
  { key: 'PENDING', label: '待处理' },
  { key: 'APPROVED', label: '已批准' },
  { key: 'REJECTED', label: '已拒绝' },
  { key: 'COMPLETED', label: '已完成' }
]

const refunds = ref<any[]>([])
const loading = ref(false)
const page = ref(0)
const hasMore = ref(true)
const filterStatus = ref('')
const showRemarkModal = ref(false)
const remarkAction = ref<'approve' | 'reject'>('approve')
const remarkTarget = ref<any>(null)
const adminRemark = ref('')

function setFilter(status: string) {
  filterStatus.value = status
  refunds.value = []
  page.value = 0
  hasMore.value = true
  loadRefunds()
}

async function loadRefunds() {
  if (loading.value || !hasMore.value) return
  loading.value = true
  try {
    const res: any = await adminApi.getRefunds({ status: filterStatus.value, page: page.value, size: 20 })
    const data = res.data || {}
    const list = data.content || data.list || data || []
    if (page.value === 0) {
      refunds.value = list
    } else {
      refunds.value.push(...list)
    }
    hasMore.value = list.length >= 20
    page.value++
  } catch (e) {
    console.error('Load refunds error:', e)
  } finally {
    loading.value = false
  }
}

function loadMore() { loadRefunds() }

function openRemark(rf: any, action: 'approve' | 'reject') {
  remarkTarget.value = rf
  remarkAction.value = action
  adminRemark.value = ''
  showRemarkModal.value = true
}

async function confirmAction() {
  try {
    if (remarkAction.value === 'approve') {
      await adminApi.approveRefund(remarkTarget.value.id, { adminRemark: adminRemark.value })
      remarkTarget.value.status = 'APPROVED'
    } else {
      await adminApi.rejectRefund(remarkTarget.value.id, { adminRemark: adminRemark.value })
      remarkTarget.value.status = 'REJECTED'
    }
    remarkTarget.value.adminRemark = adminRemark.value
    showRemarkModal.value = false
    uni.showToast({ title: '操作成功', icon: 'success' })
  } catch (e: any) {
    uni.showToast({ title: e?.message || '操作失败', icon: 'none' })
  }
}

async function completeRefund(rf: any) {
  uni.showModal({
    title: '确认完成退款',
    content: '确认已完成退款操作？',
    success: async (res) => {
      if (!res.confirm) return
      try {
        await adminApi.completeRefund(rf.id)
        rf.status = 'COMPLETED'
        uni.showToast({ title: '退款完成', icon: 'success' })
      } catch (e: any) {
        uni.showToast({ title: e?.message || '操作失败', icon: 'none' })
      }
    }
  })
}

function getStatusLabel(status: string) {
  const map: Record<string, string> = {
    PENDING: '待处理', APPROVED: '已批准', REJECTED: '已拒绝', COMPLETED: '已完成'
  }
  return map[status] || status
}

function getStatusClass(status: string) {
  if (status === 'PENDING') return 'status-pending'
  if (status === 'APPROVED') return 'status-active'
  if (status === 'COMPLETED') return 'status-done'
  if (status === 'REJECTED') return 'status-cancelled'
  return ''
}

function formatPrice(v: any) { return Number(v || 0).toFixed(2) }
function formatDate(dt: string) {
  if (!dt) return '-'
  return dt.substring(0, 16).replace('T', ' ')
}

onMounted(loadRefunds)
</script>

<style lang="scss" scoped>
.admin-page {
  min-height: 100vh;
  background: $background;
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

.refund-list {
  height: calc(100vh - 110rpx);
  padding: 16rpx 20rpx;
}

.loading-state, .empty-state {
  text-align: center;
  padding: 80rpx;
  color: #94a3b8;
  font-size: 28rpx;
}

.refund-card {
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

.refund-info {
  display: flex;
  flex-direction: column;
  gap: 8rpx;
  padding: 12rpx 0 16rpx;
  border-top: 1rpx solid $border;
  border-bottom: 1rpx solid $border;
  margin-bottom: 16rpx;
}

.info-row {
  font-size: 26rpx;
  color: $foreground;
  display: block;
}

.info-row.time {
  font-size: 24rpx;
  color: #94a3b8;
}

.card-actions {
  display: flex;
  gap: 12rpx;
  justify-content: flex-end;
}

.action-btn {
  font-size: 24rpx;
  padding: 12rpx 28rpx;
  border-radius: 28rpx;
  background: $accent;
  color: white;
}

.action-btn.danger { background: #ef4444; }

.load-more, .no-more {
  text-align: center;
  padding: 30rpx;
  color: #94a3b8;
  font-size: 24rpx;
}

.remark-modal {
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

.modal-close { font-size: 28rpx; color: #94a3b8; }

.modal-content {
  padding: 24rpx 28rpx;
}

.remark-input {
  width: 100%;
  min-height: 140rpx;
  border: 1rpx solid $border;
  border-radius: 12rpx;
  padding: 16rpx;
  font-size: 26rpx;
  color: $foreground;
  box-sizing: border-box;
}

.modal-footer {
  display: flex;
  gap: 20rpx;
  padding: 20rpx 28rpx;
  padding-bottom: calc(20rpx + env(safe-area-inset-bottom));
  border-top: 1rpx solid $border;
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

.btn-confirm.danger { background: #ef4444; }
</style>
