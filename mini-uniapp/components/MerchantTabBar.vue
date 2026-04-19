<script setup>
/**
 * 商家端自定义底部导航栏
 * 订单 / 消息 / 店铺 / 我的
 */
import { computed } from 'vue'

const props = defineProps({
  current: { type: Number, default: 0 },
})

const tabs = [
  { icon: 'order', text: '订单', url: '/pages/merchant/orders' },
  { icon: 'chat', text: '消息', url: '/pages/merchant/messages' },
  { icon: 'bag', text: '店铺', url: '/pages/merchant/store' },
  { icon: 'account', text: '我的', url: '/pages/merchant/profile' },
]

function switchTab(index) {
  if (index === props.current) return
  uni.redirectTo({ url: tabs[index].url })
}
</script>

<template>
  <view class="tabbar">
    <view class="tabbar__safe-area" />
    <view class="tabbar__content">
      <view
        v-for="(tab, index) in tabs"
        :key="index"
        class="tabbar__item"
        :class="{ 'tabbar__item--active': index === current }"
        @tap="switchTab(index)"
      >
        <u-icon
          :name="tab.icon"
          :size="24"
          :color="index === current ? '#0a6e2c' : '#707a6c'"
        />
        <text class="tabbar__text">{{ tab.text }}</text>
      </view>
    </view>
  </view>
</template>

<style lang="scss" scoped>
@import '../styles/design-system.scss';

.tabbar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  z-index: 99;
  background: #ffffff;
  border-top: 1rpx solid rgba(0, 0, 0, 0.06);

  &__safe-area {
    padding-bottom: env(safe-area-inset-bottom);
  }

  &__content {
    display: flex;
    align-items: center;
    height: 100rpx;
  }

  &__item {
    flex: 1;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    gap: 4rpx;

    &--active .tabbar__text {
      color: $primary;
      font-weight: 600;
    }
  }

  &__text {
    font-size: 20rpx;
    color: $outline;
  }
}
</style>
