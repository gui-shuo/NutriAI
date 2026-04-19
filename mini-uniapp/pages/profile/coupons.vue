<script setup>
/**
 * 优惠券页
 */
import { ref, onMounted } from 'vue'
import NavBar from '../../components/NavBar.vue'
import { couponApi } from '../../services/api'

const tabs = ['可使用', '已使用', '已过期']
const activeTab = ref(0)
const coupons = ref([])
const loading = ref(false)

function switchTab(idx) {
  activeTab.value = idx
  fetchCoupons()
}

async function fetchCoupons() {
  loading.value = true
  try {
    const statusMap = ['UNUSED', 'USED', 'EXPIRED']
    const res = await couponApi.getMyCoupons(statusMap[activeTab.value])
    // Coupon API returns direct response (no ApiResponse wrapper)
    const list = Array.isArray(res) ? res : []
    coupons.value = list.map(c => {
      const couponInfo = c.coupon || c
      return {
        ...c,
        name: couponInfo.name || '优惠券',
        amount: couponInfo.discountValue || c.amount || 0,
        minAmount: couponInfo.minOrderAmount || c.minAmount || 0,
        expireDate: c.expireAt ? new Date(c.expireAt).toLocaleDateString() : '无限期',
        couponType: couponInfo.type,
      }
    })
  } catch (e) {
    coupons.value = []
  } finally {
    loading.value = false
  }
}

onMounted(() => fetchCoupons())
</script>

<template>
  <view class="page">
    <NavBar showBack title="我的优惠券" />

    <u-tabs
      :list="tabs.map(t => ({ name: t }))"
      :current="activeTab"
      lineColor="#0a6e2c"
      :activeStyle="{color: '#0a6e2c', fontWeight: '600'}"
      :inactiveStyle="{color: '#666'}"
      @click="(item) => switchTab(tabs.indexOf(item.name))"
    />

    <scroll-view scroll-y class="content" :enhanced="true" :show-scrollbar="false">
      <view v-if="loading" class="state-tip">
        <u-loading-icon mode="circle" size="28" color="#0a6e2c" />
        <text style="margin-top: 16rpx;">加载中...</text>
      </view>
      <u-empty v-else-if="coupons.length === 0" text="暂无优惠券" icon="coupon" mode="coupon" marginTop="80" />

      <view v-for="coupon in coupons" :key="coupon.id" class="coupon-card">
        <view class="coupon-card__left">
          <text class="coupon-card__amount">
            <text class="coupon-card__symbol">¥</text>{{ coupon.amount || coupon.discount }}
          </text>
          <text class="coupon-card__threshold">满{{ coupon.minAmount || 0 }}可用</text>
        </view>
        <view class="coupon-card__right">
          <text class="coupon-card__name">{{ coupon.name || '优惠券' }}</text>
          <text class="coupon-card__expire">{{ coupon.expireDate || '无限期' }}</text>
        </view>
      </view>

      <view style="height: 40rpx;" />
    </scroll-view>
  </view>
</template>

<style lang="scss" scoped>
@import '../../styles/design-system.scss';

.page { min-height: 100vh; background: #ffffff; overflow-x: hidden; width: 100%; }

.tabs {
  display: flex;
  background: $surface-container-lowest;
  padding: 0 8rpx;
}

.tab {
  flex: 1;
  padding: 24rpx 0;
  text-align: center;
  font-size: $font-sm;
  color: $on-surface;
  position: relative;

  &--active {
    color: $primary;
    font-weight: 600;

    &::after {
      content: '';
      position: absolute;
      bottom: 0;
      left: 30%;
      right: 30%;
      height: 4rpx;
      background: $primary;
      border-radius: $radius-full;
    }
  }
}

.content { padding: 24rpx; height: calc(100vh - 200px); }

.state-tip {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12rpx;
  padding: 100rpx 0;
  color: $on-surface-variant;
  &__icon { font-size: 80rpx; }
}

.coupon-card {
  display: flex;
  background: $surface-container-lowest;
  border-radius: $radius-xl;
  overflow: hidden;
  margin-bottom: 16rpx;
  box-shadow: $shadow-sm;

  &__left {
    width: 200rpx;
    background: $primary-container;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    padding: 24rpx;
  }

  &__amount {
    font-size: $font-3xl;
    font-weight: 800;
    color: $primary;
  }

  &__symbol {
    font-size: $font-base;
  }

  &__threshold {
    font-size: 22rpx;
    color: $primary;
    opacity: 0.7;
  }

  &__right {
    flex: 1;
    padding: 24rpx;
    display: flex;
    flex-direction: column;
    justify-content: center;
  }

  &__name {
    font-size: $font-base;
    font-weight: 600;
    color: $on-surface;
    margin-bottom: 8rpx;
  }

  &__expire {
    font-size: $font-xs;
    color: $on-surface-variant;
  }
}
</style>
