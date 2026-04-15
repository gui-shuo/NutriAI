<template>
  <view class="page">
    <view class="placeholder">
      <text class="icon">🛍</text>
      <text class="title">产品管理</text>
      <text class="desc">功能开发中，敬请期待</text>
    </view>
  </view>
</template>

<script setup lang="ts">
</script>

<style scoped lang="scss">
.page {
  min-height: 100vh;
  background: $background;
  display: flex;
  align-items: center;
  justify-content: center;
}
.placeholder {
  text-align: center;
}
.icon {
  display: block;
  font-size: 80rpx;
  margin-bottom: 24rpx;
}
.title {
  display: block;
  font-size: 36rpx;
  font-weight: 600;
  color: $foreground;
  margin-bottom: 12rpx;
}
.desc {
  display: block;
  font-size: 26rpx;
  color: $muted-foreground;
}
</style>
