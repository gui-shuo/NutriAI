<template>
  <view class="page">
    <view class="header">
      <text class="title">管理后台</text>
      <text class="desc">营养餐 & 商品管理</text>
    </view>
    <view class="menu-list">
      <view class="menu-item" @tap="goTo('/pages/admin/meals')">
        <text class="menu-icon">🍱</text>
        <text class="menu-label">营养餐管理</text>
        <text class="menu-arrow">›</text>
      </view>
      <view class="menu-item" @tap="goTo('/pages/admin/products')">
        <text class="menu-icon">🛍</text>
        <text class="menu-label">产品管理</text>
        <text class="menu-arrow">›</text>
      </view>
      <view class="menu-item" @tap="goTo('/pages/admin/orders')">
        <text class="menu-icon">📋</text>
        <text class="menu-label">订单管理</text>
        <text class="menu-arrow">›</text>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
function goTo(url: string) {
  uni.navigateTo({ url })
}
</script>

<style scoped lang="scss">
.page {
  min-height: 100vh;
  background: $background;
  padding: 32rpx;
}
.header {
  text-align: center;
  padding: 48rpx 0;
}
.title {
  display: block;
  font-size: 40rpx;
  font-weight: 700;
  color: $foreground;
  margin-bottom: 12rpx;
}
.desc {
  display: block;
  font-size: 26rpx;
  color: $muted-foreground;
}
.menu-list {
  margin-top: 24rpx;
}
.menu-item {
  display: flex;
  align-items: center;
  gap: 20rpx;
  background: $card;
  padding: 32rpx 28rpx;
  border-radius: $radius-xl;
  margin-bottom: 20rpx;
  border: 1rpx solid $border;
  box-shadow: $shadow-sm;

  &:active { opacity: 0.8; }
}
.menu-icon { font-size: 40rpx; }
.menu-label {
  flex: 1;
  font-size: 30rpx;
  font-weight: 500;
  color: $foreground;
}
.menu-arrow {
  font-size: 36rpx;
  color: $muted-foreground;
}
</style>
