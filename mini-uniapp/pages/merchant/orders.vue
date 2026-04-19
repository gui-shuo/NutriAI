<script setup>
/**
 * 商家端 - 订单管理
 * 参考美团商家端：订单列表 + 售后
 * Tab: 全部 / 待接单 / 待出餐 / 待取餐
 */
import { ref, computed, onMounted } from 'vue'
import NavBar from '../../components/NavBar.vue'
import MerchantTabBar from '../../components/MerchantTabBar.vue'
import { merchantOrderApi } from '../../services/api'
import { useUserStore } from '../../stores/user'

const userStore = useUserStore()

// 顶部主Tab：订单 / 售后
const mainTab = ref(0)
const mainTabs = ['订单', '售后']

// 订单子Tab
const orderTabs = ref([
  { name: '全部', status: 'ALL', badge: 0 },
  { name: '待接单', status: 'PAID', badge: 0 },
  { name: '待出餐', status: 'PREPARING', badge: 0 },
  { name: '待取餐', status: 'READY', badge: 0 },
])
const activeOrderTab = ref(0)

const orders = ref([])
const loading = ref(false)
const page = ref(0)
const hasMore = ref(true)
const refreshing = ref(false)

// 订单状态文字
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

function switchMainTab(idx) {
  mainTab.value = idx
}

function switchOrderTab(idx) {
  activeOrderTab.value = idx
  page.value = 0
  orders.value = []
  hasMore.value = true
  fetchOrders()
}

async function fetchOrders() {
  if (loading.value) return
  loading.value = true
  try {
    const params = {
      page: page.value,
      size: 20,
      status: orderTabs.value[activeOrderTab.value].status,
    }
    if (params.status === 'ALL') delete params.status
    const res = await merchantOrderApi.getOrders(params)
    if (res) {
      const list = res.content || []
      if (page.value === 0) {
        orders.value = list
      } else {
        orders.value.push(...list)
      }
      hasMore.value = list.length >= 20
    }
  } catch (e) {
    console.error('获取订单失败', e)
  } finally {
    loading.value = false
    refreshing.value = false
  }
}

async function fetchStats() {
  try {
    const stats = await merchantOrderApi.getStats()
    if (stats) {
      orderTabs.value[1].badge = stats.paid || 0
      orderTabs.value[2].badge = stats.preparing || 0
      orderTabs.value[3].badge = stats.ready || 0
    }
  } catch (e) {
    console.error('获取统计失败', e)
  }
}

async function acceptOrder(orderNo) {
  try {
    await merchantOrderApi.acceptOrder(orderNo)
    uni.showToast({ title: '已接单', icon: 'success' })
    fetchOrders()
    fetchStats()
  } catch (e) {
    uni.showToast({ title: e.message || '操作失败', icon: 'none' })
  }
}

async function markReady(orderNo) {
  try {
    await merchantOrderApi.markReady(orderNo)
    uni.showToast({ title: '已出餐', icon: 'success' })
    fetchOrders()
    fetchStats()
  } catch (e) {
    uni.showToast({ title: e.message || '操作失败', icon: 'none' })
  }
}

// 扫码核销
function scanPickupCode() {
  // #ifdef MP-WEIXIN
  uni.scanCode({
    scanType: ['qrCode', 'barCode'],
    success: async (res) => {
      try {
        const result = await merchantOrderApi.verifyPickup(res.result)
        uni.showToast({ title: '核销成功', icon: 'success' })
        fetchOrders()
        fetchStats()
      } catch (e) {
        uni.showToast({ title: e.message || '核销失败', icon: 'none' })
      }
    },
  })
  // #endif
  // #ifndef MP-WEIXIN
  uni.showToast({ title: '请在微信小程序中使用扫码功能', icon: 'none' })
  // #endif
}

function onRefresh() {
  refreshing.value = true
  page.value = 0
  hasMore.value = true
  fetchOrders()
  fetchStats()
}

function loadMore() {
  if (!hasMore.value || loading.value) return
  page.value++
  fetchOrders()
}

function formatTime(dateStr) {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  const h = String(d.getHours()).padStart(2, '0')
  const min = String(d.getMinutes()).padStart(2, '0')
  return `${m}-${day} ${h}:${min}`
}

function formatPrice(price) {
  return price ? (price / 1).toFixed(2) : '0.00'
}

function goDetail(orderNo) {
  uni.navigateTo({ url: `/pages/merchant/order-detail?orderNo=${orderNo}` })
}

onMounted(() => {
  fetchOrders()
  fetchStats()
})
</script>

<template>
  <view class="page">
    <NavBar>
      <template #center>
        <view class="nav-tabs">
          <text
            v-for="(tab, idx) in mainTabs"
            :key="idx"
            class="nav-tab"
            :class="{ 'nav-tab--active': mainTab === idx }"
            @tap="switchMainTab(idx)"
          >{{ tab }}</text>
        </view>
      </template>
      <template #right>
        <view class="nav-scan" @tap="scanPickupCode">
          <u-icon name="scan" size="24" color="#1a1c1a" />
        </view>
      </template>
    </NavBar>

    <!-- 订单Tab -->
    <view v-if="mainTab === 0" class="order-content">
      <!-- 子Tab栏 -->
      <view class="sub-tabs">
        <view
          v-for="(tab, idx) in orderTabs"
          :key="idx"
          class="sub-tab"
          :class="{ 'sub-tab--active': activeOrderTab === idx }"
          @tap="switchOrderTab(idx)"
        >
          <text class="sub-tab__text">{{ tab.name }}</text>
          <view v-if="tab.badge > 0" class="sub-tab__badge">
            <text class="sub-tab__badge-text">{{ tab.badge }}</text>
          </view>
        </view>
      </view>

      <!-- 订单列表 -->
      <scroll-view
        scroll-y
        class="order-list"
        :enhanced="true"
        :show-scrollbar="false"
        :refresher-enabled="true"
        :refresher-triggered="refreshing"
        @refresherrefresh="onRefresh"
        @scrolltolower="loadMore"
      >
        <!-- 空状态 -->
        <view v-if="!loading && orders.length === 0" class="empty">
          <u-empty text="暂无订单" mode="order" />
        </view>

        <!-- 订单卡片 -->
        <view
          v-for="order in orders"
          :key="order.orderNo"
          class="order-card"
          @tap="goDetail(order.orderNo)"
        >
          <!-- 订单头 -->
          <view class="order-card__header">
            <view class="order-card__no">
              <text class="order-card__no-label">#</text>
              <text class="order-card__no-text">{{ order.orderNo?.slice(-6) }}</text>
            </view>
            <text
              class="order-card__status"
              :style="{ color: (statusMap[order.orderStatus] || {}).color }"
            >{{ (statusMap[order.orderStatus] || {}).text || order.orderStatus }}</text>
          </view>

          <!-- 时间 -->
          <view class="order-card__time-row">
            <u-icon name="clock" size="14" color="#999" />
            <text class="order-card__time">{{ formatTime(order.createdAt) }}</text>
            <text v-if="order.merchantName" class="order-card__store">{{ order.merchantName }}</text>
          </view>

          <!-- 商品摘要 -->
          <view class="order-card__items">
            <view
              v-for="(item, iIdx) in (order.items || []).slice(0, 3)"
              :key="iIdx"
              class="order-card__item"
            >
              <text class="order-card__item-name">{{ item.mealName || item.productName }}</text>
              <text class="order-card__item-qty">x{{ item.quantity }}</text>
            </view>
            <text v-if="(order.items || []).length > 3" class="order-card__more">
              等{{ order.items.length }}件商品
            </text>
          </view>

          <!-- 金额 + 操作按钮 -->
          <view class="order-card__footer">
            <view class="order-card__total">
              <text class="order-card__total-label">合计</text>
              <text class="order-card__total-price">¥{{ formatPrice(order.totalAmount) }}</text>
            </view>
            <view class="order-card__actions">
              <u-button
                v-if="order.orderStatus === 'PAID'"
                text="接单"
                type="primary"
                size="mini"
                color="#0a6e2c"
                shape="circle"
                @click.stop="acceptOrder(order.orderNo)"
              />
              <u-button
                v-if="order.orderStatus === 'PREPARING'"
                text="出餐完成"
                type="primary"
                size="mini"
                color="#1890ff"
                shape="circle"
                @click.stop="markReady(order.orderNo)"
              />
              <u-button
                v-if="order.orderStatus === 'READY'"
                text="核销取餐"
                size="mini"
                shape="circle"
                @click.stop="scanPickupCode"
              />
            </view>
          </view>

          <!-- 取餐码 -->
          <view v-if="order.pickupCode && (order.orderStatus === 'READY' || order.orderStatus === 'PREPARING')" class="order-card__pickup">
            <text class="order-card__pickup-label">取餐码</text>
            <text class="order-card__pickup-code">{{ order.pickupCode }}</text>
          </view>
        </view>

        <!-- 加载状态 -->
        <view v-if="loading" class="loading-more">
          <u-loading-icon />
        </view>
        <view v-if="!hasMore && orders.length > 0" class="no-more">
          <text class="no-more__text">没有更多了</text>
        </view>

        <view style="height: 140rpx;" />
      </scroll-view>
    </view>

    <!-- 售后Tab (占位) -->
    <view v-if="mainTab === 1" class="after-sale">
      <view class="empty">
        <u-empty text="暂无售后订单" mode="order" />
      </view>
    </view>

    <MerchantTabBar :current="0" />
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

// 导航栏Tab
.nav-tabs {
  display: flex;
  gap: 32rpx;
}

.nav-tab {
  font-size: 32rpx;
  font-weight: 600;
  color: $on-surface-variant;
  position: relative;
  padding-bottom: 4rpx;

  &--active {
    color: $on-surface;
    font-weight: 800;

    &::after {
      content: '';
      position: absolute;
      bottom: -4rpx;
      left: 20%;
      right: 20%;
      height: 4rpx;
      border-radius: 2rpx;
      background: $primary;
    }
  }
}

.nav-scan {
  width: 64rpx;
  height: 64rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}

// 子Tab栏
.sub-tabs {
  display: flex;
  background: #ffffff;
  padding: 16rpx 24rpx;
  gap: 8rpx;
  border-bottom: 1rpx solid rgba(0, 0, 0, 0.04);
}

.sub-tab {
  flex: 1;
  text-align: center;
  padding: 12rpx 0;
  position: relative;
  border-radius: $radius-lg;

  &--active {
    background: rgba($primary, 0.08);

    .sub-tab__text {
      color: $primary;
      font-weight: 700;
    }
  }

  &__text {
    font-size: 26rpx;
    color: $on-surface-variant;
  }

  &__badge {
    position: absolute;
    top: 2rpx;
    right: 12rpx;
    min-width: 32rpx;
    height: 32rpx;
    background: #ff4d4f;
    border-radius: $radius-full;
    display: flex;
    align-items: center;
    justify-content: center;
    padding: 0 8rpx;

    &-text {
      font-size: 20rpx;
      color: #fff;
      font-weight: 600;
    }
  }
}

// 订单内容
.order-content {
  display: flex;
  flex-direction: column;
  height: calc(100vh - 88px);
}

.order-list {
  flex: 1;
  padding: 16rpx 24rpx;
}

// 订单卡片
.order-card {
  background: #ffffff;
  border-radius: $radius-xl;
  padding: 24rpx;
  margin-bottom: 16rpx;
  box-shadow: $shadow-sm;

  &__header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 12rpx;
  }

  &__no {
    display: flex;
    align-items: center;
    gap: 4rpx;

    &-label {
      font-size: 24rpx;
      color: $on-surface-variant;
      font-weight: 600;
    }

    &-text {
      font-size: 28rpx;
      font-weight: 700;
      color: $on-surface;
    }
  }

  &__status {
    font-size: 24rpx;
    font-weight: 700;
  }

  &__time-row {
    display: flex;
    align-items: center;
    gap: 8rpx;
    margin-bottom: 16rpx;
  }

  &__time {
    font-size: 24rpx;
    color: $on-surface-variant;
  }

  &__store {
    font-size: 22rpx;
    color: $on-surface-variant;
    margin-left: auto;
  }

  // 商品摘要
  &__items {
    padding: 16rpx 0;
    border-top: 1rpx solid rgba(0, 0, 0, 0.04);
    border-bottom: 1rpx solid rgba(0, 0, 0, 0.04);
  }

  &__item {
    display: flex;
    justify-content: space-between;
    padding: 4rpx 0;

    &-name {
      font-size: 26rpx;
      color: $on-surface;
      flex: 1;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }

    &-qty {
      font-size: 24rpx;
      color: $on-surface-variant;
      margin-left: 16rpx;
    }
  }

  &__more {
    font-size: 24rpx;
    color: $on-surface-variant;
    margin-top: 4rpx;
  }

  // 底部
  &__footer {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-top: 16rpx;
  }

  &__total {
    display: flex;
    align-items: baseline;
    gap: 8rpx;

    &-label {
      font-size: 24rpx;
      color: $on-surface-variant;
    }

    &-price {
      font-size: 32rpx;
      font-weight: 700;
      color: $on-surface;
    }
  }

  &__actions {
    display: flex;
    gap: 12rpx;
  }

  // 取餐码
  &__pickup {
    display: flex;
    align-items: center;
    gap: 12rpx;
    margin-top: 16rpx;
    padding: 16rpx;
    background: rgba($primary, 0.06);
    border-radius: $radius-lg;

    &-label {
      font-size: 24rpx;
      color: $on-surface-variant;
    }

    &-code {
      font-size: 36rpx;
      font-weight: 800;
      color: $primary;
      letter-spacing: 4rpx;
    }
  }
}

// 售后
.after-sale {
  display: flex;
  align-items: center;
  justify-content: center;
  height: calc(100vh - 88px - 100rpx);
}

// 空状态
.empty {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 120rpx 0;
}

.loading-more {
  display: flex;
  justify-content: center;
  padding: 24rpx;
}

.no-more {
  text-align: center;
  padding: 24rpx;

  &__text {
    font-size: 24rpx;
    color: $on-surface-variant;
  }
}
</style>
