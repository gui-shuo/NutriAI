<template>
  <view class="result-page">
    <view class="result-content">
      <view class="result-icon">
        <text>{{ isSuccess ? '✅' : '❌' }}</text>
      </view>
      <text class="result-title">{{ isSuccess ? '下单成功' : '下单失败' }}</text>
      <text class="result-desc text-secondary mt-sm">
        {{ isSuccess ? '您的订单已创建，请等待处理' : '订单创建失败，请重试' }}
      </text>

      <view v-if="orderNo" class="order-no mt-lg">
        <text class="text-sm text-muted">订单编号：{{ orderNo }}</text>
      </view>

      <view class="result-actions mt-xl">
        <button class="btn-primary btn-block" @tap="goOrders">查看订单</button>
        <button class="btn-outline btn-block mt-md" @tap="goHome">返回首页</button>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { onLoad } from '@dcloudio/uni-app'

const orderNo = ref('')
const status = ref('success')

const isSuccess = computed(() => status.value === 'success')

onLoad((query: any) => {
  orderNo.value = query?.orderNo || ''
  status.value = query?.status || 'success'
})

function goOrders() {
  uni.navigateTo({ url: '/pages/profile/orders' })
}

function goHome() {
  uni.switchTab({ url: '/pages/index/index' })
}
</script>

<style lang="scss" scoped>
.result-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: $bg-page;
  padding: 0 60rpx;
}

.result-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  width: 100%;
}

.result-icon {
  font-size: 100rpx;
}

.result-title {
  font-size: $font-2xl;
  font-weight: 700;
  color: $text-primary;
  margin-top: $spacing-lg;
}

.result-desc {
  font-size: $font-base;
}

.order-no {
  padding: $spacing-md $spacing-lg;
  background: $bg-muted;
  border-radius: $radius-md;
}

.result-actions {
  width: 100%;
}
</style>
