<template>
  <view class="refunds-page">
    <scroll-view class="list" scroll-y>
      <view class="loading-state" v-if="loading && refunds.length === 0">加载中...</view>
      <view class="empty-state" v-if="!loading && refunds.length === 0">
        <text class="empty-icon">↩️</text>
        <text class="empty-text">暂无退款记录</text>
      </view>

      <view class="refund-card" v-for="rf in refunds" :key="rf.id">
        <view class="card-header">
          <text class="order-no">{{ rf.orderNo }}</text>
          <view class="status-badge" :class="getStatusClass(rf.status)">{{ getStatusLabel(rf.status) }}</view>
        </view>
        <text class="refund-amount">退款金额: ¥{{ formatPrice(rf.refundAmount) }}</text>
        <text class="refund-reason">原因: {{ rf.reason }}</text>
        <text class="refund-time">{{ formatDate(rf.createdAt) }}</text>
        <text class="admin-remark" v-if="rf.adminRemark">审核备注: {{ rf.adminRemark }}</text>
      </view>
    </scroll-view>
  </view>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { refundApi } from '@/services/api'

const refunds = ref<any[]>([])
const loading = ref(false)

async function loadRefunds() {
  loading.value = true
  try {
    const res: any = await refundApi.getMyRefunds()
    refunds.value = res.data?.content || res.data || []
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

function getStatusLabel(status: string) {
  const map: Record<string, string> = { PENDING: '审核中', APPROVED: '已批准', REJECTED: '已拒绝', COMPLETED: '已退款' }
  return map[status] || status
}

function getStatusClass(status: string) {
  if (status === 'PENDING') return 'pending'
  if (status === 'APPROVED') return 'approved'
  if (status === 'REJECTED') return 'rejected'
  return 'completed'
}

function formatPrice(v: any) { return Number(v || 0).toFixed(2) }
function formatDate(dt: string) {
  if (!dt) return '-'
  return dt.substring(0, 16).replace('T', ' ')
}

onMounted(loadRefunds)
</script>

<style lang="scss" scoped>
.refunds-page {
  min-height: 100vh;
  background: $background;
  padding: 20rpx;
}

.list {
  height: 100vh;
}

.loading-state, .empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding-top: 120rpx;
  gap: 20rpx;
  color: #94a3b8;
}

.empty-icon { font-size: 80rpx; }
.empty-text { font-size: 28rpx; }

.refund-card {
  background: $card;
  border-radius: 16rpx;
  padding: 24rpx;
  margin-bottom: 20rpx;
  border: 1rpx solid $border;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12rpx;
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

.pending { background: rgba(245,158,11,0.1); color: #f59e0b; }
.approved { background: rgba(16,185,129,0.1); color: $accent; }
.rejected { background: rgba(239,68,68,0.1); color: #ef4444; }
.completed { background: rgba(148,163,184,0.1); color: #94a3b8; }

.refund-amount, .refund-reason, .refund-time, .admin-remark {
  font-size: 26rpx;
  color: $foreground;
  display: block;
  margin-bottom: 6rpx;
}

.refund-time {
  color: #94a3b8;
  font-size: 24rpx;
}

.admin-remark {
  color: #64748b;
  font-size: 24rpx;
  background: $background;
  padding: 10rpx 14rpx;
  border-radius: 8rpx;
  margin-top: 6rpx;
}
</style>
