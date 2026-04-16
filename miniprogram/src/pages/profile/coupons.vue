<template>
  <view class="coupons-page">
    <!-- Status Tabs -->
    <view class="tabs">
      <view class="tab" :class="{ active: currentTab === 'UNUSED' }" @tap="switchTab('UNUSED')">未使用</view>
      <view class="tab" :class="{ active: currentTab === 'USED' }" @tap="switchTab('USED')">已使用</view>
      <view class="tab" :class="{ active: currentTab === 'EXPIRED' }" @tap="switchTab('EXPIRED')">已过期</view>
    </view>

    <scroll-view class="coupon-list" scroll-y>
      <view class="loading-state" v-if="loading && coupons.length === 0">加载中...</view>
      <view class="empty-state" v-if="!loading && coupons.length === 0">
        <text class="empty-icon">🏷</text>
        <text class="empty-text">暂无优惠券</text>
      </view>

      <view
        v-for="uc in coupons"
        :key="uc.id"
        class="coupon-card"
        :class="{ used: uc.status !== 'UNUSED', expired: uc.status === 'EXPIRED' }"
      >
        <view class="coupon-left">
          <text class="coupon-amount">
            <text v-if="uc.coupon?.couponType === 'REDUCE'">¥{{ uc.coupon.discountValue }}</text>
            <text v-else>{{ (uc.coupon?.discountValue * 10).toFixed(1) }}折</text>
          </text>
          <text class="coupon-cond">满{{ uc.coupon?.minOrderAmount }}元可用</text>
        </view>
        <view class="coupon-divider">
          <view class="divider-dot top"></view>
          <view class="divider-line"></view>
          <view class="divider-dot bottom"></view>
        </view>
        <view class="coupon-right">
          <text class="coupon-name">{{ uc.coupon?.name }}</text>
          <text class="coupon-type">{{ uc.coupon?.couponType === 'REDUCE' ? '满减券' : '折扣券' }}</text>
          <text class="coupon-exp">{{ formatDate(uc.expireTime) }} 到期</text>
          <view class="status-tag">{{ getStatusLabel(uc.status) }}</view>
        </view>
      </view>
    </scroll-view>
  </view>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { couponApi } from '@/services/api'

const currentTab = ref('UNUSED')
const coupons = ref<any[]>([])
const loading = ref(false)

async function loadCoupons() {
  loading.value = true
  try {
    const res: any = await couponApi.getMyCoupons(currentTab.value)
    coupons.value = res.data || res || []
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

function switchTab(status: string) {
  currentTab.value = status
  coupons.value = []
  loadCoupons()
}

function getStatusLabel(status: string) {
  return { UNUSED: '未使用', USED: '已使用', EXPIRED: '已过期' }[status] || status
}

function formatDate(dt: string) {
  if (!dt) return '-'
  return dt.substring(0, 10)
}

onMounted(loadCoupons)
</script>

<style lang="scss" scoped>
.coupons-page {
  min-height: 100vh;
  background: $background;
}

.tabs {
  display: flex;
  background: $card;
  border-bottom: 1rpx solid $border;
}

.tab {
  flex: 1;
  text-align: center;
  padding: 28rpx 0;
  font-size: 28rpx;
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

.coupon-list {
  height: calc(100vh - 90rpx);
  padding: 20rpx;
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

.coupon-card {
  display: flex;
  background: $card;
  border-radius: 16rpx;
  margin-bottom: 20rpx;
  overflow: hidden;
  border: 1rpx solid $border;
}

.coupon-card.used, .coupon-card.expired {
  opacity: 0.6;
}

.coupon-left {
  background: $accent;
  color: white;
  padding: 28rpx 24rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-width: 160rpx;
}

.coupon-amount {
  font-size: 40rpx;
  font-weight: 700;
}

.coupon-cond {
  font-size: 22rpx;
  margin-top: 8rpx;
  opacity: 0.9;
}

.coupon-divider {
  display: flex;
  flex-direction: column;
  align-items: center;
  position: relative;
  width: 24rpx;
  background: $background;
}

.divider-dot {
  width: 24rpx;
  height: 24rpx;
  border-radius: 50%;
  background: $background;
  position: absolute;
}

.divider-dot.top { top: -12rpx; }
.divider-dot.bottom { bottom: -12rpx; }

.divider-line {
  flex: 1;
  width: 2rpx;
  border-left: 2rpx dashed $border;
  margin: 16rpx 0;
}

.coupon-right {
  flex: 1;
  padding: 24rpx;
  display: flex;
  flex-direction: column;
  gap: 8rpx;
}

.coupon-name {
  font-size: 28rpx;
  font-weight: 600;
  color: $foreground;
}

.coupon-type {
  font-size: 22rpx;
  color: #94a3b8;
}

.coupon-exp {
  font-size: 22rpx;
  color: #94a3b8;
}

.status-tag {
  align-self: flex-start;
  font-size: 22rpx;
  padding: 4rpx 14rpx;
  border-radius: 20rpx;
  background: rgba(16,185,129,0.1);
  color: $accent;
}
</style>
