<template>
  <view class="page">
    <!-- Header -->
    <view class="header">
      <view class="header-bg" />
      <view class="header-inner">
        <text class="header-title">管理后台</text>
        <text class="header-sub">NutriAI 运营管理</text>
      </view>
    </view>

    <!-- Stats Grid -->
    <view class="stats-grid">
      <view class="stat-card">
        <text class="stat-num">{{ stats.todayOrders || 0 }}</text>
        <text class="stat-label">今日订单</text>
      </view>
      <view class="stat-card revenue">
        <text class="stat-num">¥{{ stats.todayRevenue || '0.00' }}</text>
        <text class="stat-label">今日营收</text>
      </view>
      <view class="stat-card">
        <text class="stat-num">{{ stats.totalMeals || 0 }}</text>
        <text class="stat-label">营养餐</text>
      </view>
      <view class="stat-card">
        <text class="stat-num">{{ stats.totalProducts || 0 }}</text>
        <text class="stat-label">商品</text>
      </view>
    </view>

    <!-- Quick Actions -->
    <view class="section-label">快捷操作</view>
    <view class="action-list">
      <view class="action-item" @tap="goTo('/pages/admin/meals')">
        <view class="action-icon-wrap" style="background: rgba(16,185,129,0.1)">
          <text class="action-icon">🍱</text>
        </view>
        <view class="action-info">
          <text class="action-title">营养餐管理</text>
          <text class="action-desc">发布、编辑、上下架营养餐</text>
        </view>
        <text class="action-arrow">›</text>
      </view>
      <view class="action-item" @tap="goTo('/pages/admin/products')">
        <view class="action-icon-wrap" style="background: rgba(59,130,246,0.1)">
          <text class="action-icon">🛍</text>
        </view>
        <view class="action-info">
          <text class="action-title">产品管理</text>
          <text class="action-desc">管理营养产品上下架与库存</text>
        </view>
        <text class="action-arrow">›</text>
      </view>
      <view class="action-item" @tap="goTo('/pages/admin/orders')">
        <view class="action-icon-wrap" style="background: rgba(245,158,11,0.1)">
          <text class="action-icon">📦</text>
        </view>
        <view class="action-info">
          <text class="action-title">营养餐订单</text>
          <text class="action-desc">查看处理营养餐订单</text>
        </view>
        <text class="action-arrow">›</text>
      </view>
      <view class="action-item" @tap="goTo('/pages/admin/product-orders')">
        <view class="action-icon-wrap" style="background: rgba(139,92,246,0.1)">
          <text class="action-icon">🛒</text>
        </view>
        <view class="action-info">
          <text class="action-title">产品订单</text>
          <text class="action-desc">发货、退款、产品订单管理</text>
        </view>
        <text class="action-arrow">›</text>
      </view>
      <view class="action-item" @tap="goTo('/pages/admin/coupons')">
        <view class="action-icon-wrap" style="background: rgba(239,68,68,0.1)">
          <text class="action-icon">🏷</text>
        </view>
        <view class="action-info">
          <text class="action-title">优惠券管理</text>
          <text class="action-desc">创建和管理优惠券活动</text>
        </view>
        <text class="action-arrow">›</text>
      </view>
      <view class="action-item" @tap="goTo('/pages/admin/refunds')">
        <view class="action-icon-wrap" style="background: rgba(245,158,11,0.1)">
          <text class="action-icon">↩️</text>
        </view>
        <view class="action-info">
          <text class="action-title">退款管理</text>
          <text class="action-desc">审核和处理退款申请</text>
        </view>
        <text class="action-arrow">›</text>
      </view>
    </view>

    <view class="safe-bottom" />
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()

import { adminApi } from '@/services/api'

const stats = ref<Record<string, any>>({})

function checkPermission() {
  if (!userStore.isAdmin) {
    uni.showToast({ title: '无管理员权限', icon: 'none' })
    setTimeout(() => uni.navigateBack(), 300)
    return false
  }
  return true
}

async function loadDashboard() {
  try {
    const res = await adminApi.getDashboard()
    if (res.code === 200) stats.value = res.data || {}
  } catch {}
}

function goTo(url: string) {
  uni.navigateTo({ url })
}

onShow(() => {
  if (checkPermission()) loadDashboard()
})
</script>

<style scoped lang="scss">
.page {
  min-height: 100vh;
  background: $background;
}

.header {
  position: relative;
  padding: 0 0 32rpx;
  overflow: hidden;
}

.header-bg {
  position: absolute;
  top: 0; left: 0; right: 0;
  height: 320rpx;
  background: $gradient-accent;
  border-radius: 0 0 $radius-2xl $radius-2xl;
}

.header-inner {
  position: relative;
  padding: 100rpx 40rpx 0;
}

.header-title {
  display: block;
  font-family: 'Calistoga', serif;
  font-size: 48rpx;
  font-weight: 700;
  color: #fff;
}

.header-sub {
  display: block;
  font-size: 26rpx;
  color: rgba(255, 255, 255, 0.8);
  margin-top: 8rpx;
}

.stats-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20rpx;
  padding: 0 32rpx;
  margin-top: -20rpx;
  position: relative;
  z-index: 2;
}

.stat-card {
  background: $card;
  border-radius: $radius-lg;
  padding: 32rpx 28rpx;
  box-shadow: $shadow-sm;
  display: flex;
  flex-direction: column;
  gap: 8rpx;
}

.stat-num {
  font-family: 'Calistoga', serif;
  font-size: 40rpx;
  font-weight: 700;
  color: $foreground;
}

.stat-card.revenue .stat-num {
  color: $accent;
}

.stat-label {
  font-size: 24rpx;
  color: $muted-foreground;
}

.section-label {
  padding: 40rpx 32rpx 16rpx;
  font-size: 28rpx;
  font-weight: 600;
  color: $foreground;
}

.action-list {
  margin: 0 32rpx;
  background: $card;
  border-radius: $radius-xl;
  box-shadow: $shadow-sm;
  overflow: hidden;
}

.action-item {
  display: flex;
  align-items: center;
  padding: 32rpx 28rpx;
  gap: 24rpx;

  &:not(:last-child) {
    border-bottom: 1rpx solid $border;
  }

  &:active {
    background: rgba(16, 185, 129, 0.04);
  }
}

.action-icon-wrap {
  width: 80rpx;
  height: 80rpx;
  border-radius: $radius-lg;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.action-icon {
  font-size: 40rpx;
}

.action-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 6rpx;
}

.action-title {
  font-size: 30rpx;
  font-weight: 600;
  color: $foreground;
}

.action-desc {
  font-size: 24rpx;
  color: $muted-foreground;
}

.action-arrow {
  font-size: 36rpx;
  color: $muted-foreground;
  flex-shrink: 0;
}

.safe-bottom {
  height: env(safe-area-inset-bottom);
  padding-bottom: 40rpx;
}
</style>
