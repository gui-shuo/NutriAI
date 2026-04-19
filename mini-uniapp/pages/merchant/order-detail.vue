<script setup>
/**
 * 商家端 - 订单详情
 */
import { ref, onMounted } from 'vue'
import { merchantOrderApi } from '../../services/api'

const order = ref(null)
const loading = ref(true)
const orderNo = ref('')

const statusMap = {
  PENDING_PAYMENT: { text: '待付款', color: '#999' },
  PAID: { text: '待接单', color: '#ff6600' },
  PREPARING: { text: '出餐中', color: '#0a6e2c' },
  READY: { text: '待取餐', color: '#1890ff' },
  PICKED_UP: { text: '已取餐', color: '#52c41a' },
  COMPLETED: { text: '已完成', color: '#999' },
  CANCELLED: { text: '已取消', color: '#999' },
  REFUNDED: { text: '已退款', color: '#ff4d4f' },
}

onMounted(() => {
  const pages = getCurrentPages()
  const page = pages[pages.length - 1]
  orderNo.value = page.options?.orderNo || ''
  if (orderNo.value) fetchDetail()
})

async function fetchDetail() {
  loading.value = true
  try {
    const res = await merchantOrderApi.getDetail(orderNo.value)
    order.value = res
  } catch (e) {
    uni.showToast({ title: e.message || '获取失败', icon: 'none' })
  } finally {
    loading.value = false
  }
}

async function acceptOrder() {
  try {
    await merchantOrderApi.acceptOrder(order.value.orderNo)
    uni.showToast({ title: '已接单', icon: 'success' })
    fetchDetail()
  } catch (e) {
    uni.showToast({ title: e.message || '操作失败', icon: 'none' })
  }
}

async function markReady() {
  try {
    await merchantOrderApi.markReady(order.value.orderNo)
    uni.showToast({ title: '已出餐', icon: 'success' })
    fetchDetail()
  } catch (e) {
    uni.showToast({ title: e.message || '操作失败', icon: 'none' })
  }
}

function formatTime(dateStr) {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')} ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
}

function formatPrice(price) {
  return price ? (price / 1).toFixed(2) : '0.00'
}
</script>

<template>
  <view class="page">
    <view v-if="loading" class="loading-wrap">
      <u-loading-icon />
    </view>

    <scroll-view v-else-if="order" scroll-y class="content" :enhanced="true" :show-scrollbar="false">
      <!-- 状态头 -->
      <view class="status-header" :style="{ background: (statusMap[order.orderStatus] || {}).color }">
        <text class="status-header__text">{{ (statusMap[order.orderStatus] || {}).text }}</text>
        <text class="status-header__no">订单号：{{ order.orderNo }}</text>
      </view>

      <!-- 取餐码 -->
      <view v-if="order.pickupCode && ['PREPARING', 'READY'].includes(order.orderStatus)" class="pickup-card">
        <text class="pickup-card__label">取餐码</text>
        <text class="pickup-card__code">{{ order.pickupCode }}</text>
      </view>

      <!-- 商品列表 -->
      <view class="section">
        <text class="section__title">商品信息</text>
        <view class="items-card">
          <view
            v-for="(item, idx) in (order.items || [])"
            :key="idx"
            class="item-row"
          >
            <view class="item-row__info">
              <text class="item-row__name">{{ item.mealName || item.productName }}</text>
              <text class="item-row__spec" v-if="item.specification">{{ item.specification }}</text>
            </view>
            <text class="item-row__qty">x{{ item.quantity }}</text>
            <text class="item-row__price">¥{{ formatPrice(item.price) }}</text>
          </view>
        </view>
      </view>

      <!-- 金额 -->
      <view class="section">
        <view class="amount-card">
          <view class="amount-row">
            <text class="amount-row__label">商品合计</text>
            <text class="amount-row__value">¥{{ formatPrice(order.totalAmount) }}</text>
          </view>
          <view v-if="order.deliveryFee" class="amount-row">
            <text class="amount-row__label">配送费</text>
            <text class="amount-row__value">¥{{ formatPrice(order.deliveryFee) }}</text>
          </view>
          <view class="amount-row amount-row--total">
            <text class="amount-row__label">实付金额</text>
            <text class="amount-row__total">¥{{ formatPrice(order.totalAmount) }}</text>
          </view>
        </view>
      </view>

      <!-- 订单信息 -->
      <view class="section">
        <text class="section__title">订单信息</text>
        <view class="info-card">
          <view class="info-item">
            <text class="info-item__label">下单时间</text>
            <text class="info-item__value">{{ formatTime(order.createdAt) }}</text>
          </view>
          <view v-if="order.paidAt" class="info-item">
            <text class="info-item__label">付款时间</text>
            <text class="info-item__value">{{ formatTime(order.paidAt) }}</text>
          </view>
          <view v-if="order.completedAt" class="info-item">
            <text class="info-item__label">完成时间</text>
            <text class="info-item__value">{{ formatTime(order.completedAt) }}</text>
          </view>
          <view v-if="order.remarks" class="info-item info-item--col">
            <text class="info-item__label">备注</text>
            <text class="info-item__remark">{{ order.remarks }}</text>
          </view>
        </view>
      </view>

      <view style="height: 160rpx;" />
    </scroll-view>

    <!-- 底部操作 -->
    <view v-if="order" class="bottom-bar">
      <u-button
        v-if="order.orderStatus === 'PAID'"
        text="接单"
        type="primary"
        shape="circle"
        color="#0a6e2c"
        :customStyle="{ flex: 1 }"
        @click="acceptOrder"
      />
      <u-button
        v-if="order.orderStatus === 'PREPARING'"
        text="出餐完成"
        type="primary"
        shape="circle"
        color="#1890ff"
        :customStyle="{ flex: 1 }"
        @click="markReady"
      />
    </view>
  </view>
</template>

<style lang="scss" scoped>
@import '../../styles/design-system.scss';

.page {
  min-height: 100vh;
  background: $surface-dim;
  overflow-x: hidden;
  width: 100%;
}

.content {
  height: 100vh;
}

.loading-wrap {
  display: flex;
  justify-content: center;
  padding: 120rpx;
}

// 状态头
.status-header {
  padding: 40rpx 32rpx;
  color: #fff;

  &__text {
    display: block;
    font-size: 36rpx;
    font-weight: 700;
    margin-bottom: 8rpx;
  }

  &__no {
    font-size: 24rpx;
    opacity: 0.8;
  }
}

// 取餐码
.pickup-card {
  margin: 16rpx 24rpx;
  padding: 28rpx 32rpx;
  background: #fff;
  border-radius: $radius-xl;
  box-shadow: $shadow-md;
  display: flex;
  align-items: center;
  gap: 24rpx;

  &__label {
    font-size: 26rpx;
    color: $on-surface-variant;
  }

  &__code {
    font-size: 48rpx;
    font-weight: 800;
    color: $primary;
    letter-spacing: 8rpx;
  }
}

// 分区
.section {
  margin: 16rpx 24rpx;

  &__title {
    display: block;
    font-size: 24rpx;
    font-weight: 600;
    color: $on-surface-variant;
    margin-bottom: 12rpx;
    padding-left: 8rpx;
  }
}

// 商品卡片
.items-card {
  background: #fff;
  border-radius: $radius-xl;
  overflow: hidden;
  box-shadow: $shadow-sm;
}

.item-row {
  display: flex;
  align-items: center;
  padding: 20rpx 24rpx;
  gap: 16rpx;

  & + & {
    border-top: 1rpx solid rgba(0, 0, 0, 0.04);
  }

  &__info {
    flex: 1;
    min-width: 0;
  }

  &__name {
    display: block;
    font-size: 28rpx;
    color: $on-surface;
  }

  &__spec {
    font-size: 22rpx;
    color: $on-surface-variant;
  }

  &__qty {
    font-size: 26rpx;
    color: $on-surface-variant;
  }

  &__price {
    font-size: 28rpx;
    color: $on-surface;
    font-weight: 600;
  }
}

// 金额
.amount-card {
  background: #fff;
  border-radius: $radius-xl;
  padding: 24rpx;
  box-shadow: $shadow-sm;
}

.amount-row {
  display: flex;
  justify-content: space-between;
  padding: 8rpx 0;

  &--total {
    border-top: 1rpx solid rgba(0, 0, 0, 0.04);
    margin-top: 8rpx;
    padding-top: 16rpx;
  }

  &__label {
    font-size: 26rpx;
    color: $on-surface-variant;
  }

  &__value {
    font-size: 26rpx;
    color: $on-surface;
  }

  &__total {
    font-size: 32rpx;
    font-weight: 700;
    color: #ff4d4f;
  }
}

// 订单信息
.info-card {
  background: #fff;
  border-radius: $radius-xl;
  overflow: hidden;
  box-shadow: $shadow-sm;
}

.info-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20rpx 24rpx;

  & + & {
    border-top: 1rpx solid rgba(0, 0, 0, 0.04);
  }

  &--col {
    flex-direction: column;
    align-items: flex-start;
    gap: 8rpx;
  }

  &__label {
    font-size: 26rpx;
    color: $on-surface-variant;
  }

  &__value {
    font-size: 26rpx;
    color: $on-surface;
  }

  &__remark {
    font-size: 26rpx;
    color: $on-surface;
    line-height: 1.5;
  }
}

// 底部操作栏
.bottom-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  display: flex;
  gap: 16rpx;
  padding: 16rpx 24rpx;
  padding-bottom: calc(16rpx + env(safe-area-inset-bottom));
  background: #fff;
  border-top: 1rpx solid rgba(0, 0, 0, 0.06);
}
</style>
