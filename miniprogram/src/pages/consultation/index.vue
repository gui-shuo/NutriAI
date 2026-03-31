<template>
  <view class="consultation-page">
    <!-- Disclaimer -->
    <view class="disclaimer-tip" v-if="showDisclaimer">
      <text>🩺 咨询服务仅供参考，不构成医疗诊断或治疗方案。如有疾病请及时就医。</text>
      <text class="dismiss" @tap="showDisclaimer = false">✕</text>
    </view>

    <!-- Tab Bar -->
    <view class="tab-bar">
      <view
        class="tab-item"
        :class="{ active: activeTab === 'list' }"
        @tap="activeTab = 'list'"
      >
        营养师列表
      </view>
      <view
        class="tab-item"
        :class="{ active: activeTab === 'orders' }"
        @tap="switchToOrders"
      >
        我的咨询
      </view>
    </view>

    <!-- Nutritionist List Tab -->
    <view v-show="activeTab === 'list'">
      <!-- Filter -->
      <view class="filter-bar">
        <view
          class="filter-item"
          :class="{ active: onlineFilter === 'all' }"
          @tap="onlineFilter = 'all'; loadNutritionists()"
        >全部</view>
        <view
          class="filter-item"
          :class="{ active: onlineFilter === 'online' }"
          @tap="onlineFilter = 'online'; loadNutritionists()"
        >在线</view>
      </view>

      <!-- Nutritionist Cards -->
      <view class="nutritionist-list">
        <view
          class="nutritionist-card"
          v-for="item in nutritionists"
          :key="item.id"
          @tap="selectNutritionist(item)"
        >
          <view class="card-main">
            <view class="card-avatar-wrap">
              <image class="card-avatar" :src="defaultAvatar(item.avatar)" mode="aspectFill" />
              <view class="online-dot" v-if="item.online"></view>
            </view>
            <view class="card-info">
              <view class="card-name-row">
                <text class="card-name">{{ item.name }}</text>
                <text class="card-title">{{ item.title }}</text>
              </view>
              <text class="card-specialty">{{ item.specialty }}</text>
              <view class="card-stats">
                <view class="rating">
                  <text class="stars">{{ getStars(item.rating) }}</text>
                  <text class="rating-num">{{ item.rating?.toFixed(1) || '5.0' }}</text>
                </view>
                <text class="consult-count">{{ item.consultCount || 0 }}次咨询</text>
              </view>
            </view>
          </view>
          <view class="card-bottom">
            <text class="price">¥{{ item.price?.toFixed(2) || '0.00' }}/次</text>
            <view class="book-btn" @tap.stop="openBooking(item)">预约咨询</view>
          </view>
        </view>
      </view>

      <view class="empty-state" v-if="!listLoading && nutritionists.length === 0">
        <text class="empty-icon">🧑‍⚕️</text>
        <text class="empty-text">暂无营养师</text>
      </view>
      <view class="loading-state" v-if="listLoading">
        <text>加载中...</text>
      </view>
    </view>

    <!-- My Orders Tab -->
    <view v-show="activeTab === 'orders'">
      <view class="order-list" v-if="orders.length > 0">
        <view class="order-card" v-for="order in orders" :key="order.orderNo">
          <view class="order-header">
            <text class="order-no">{{ order.orderNo }}</text>
            <view class="status-badge" :class="getStatusClass(order.status)">
              {{ getStatusText(order.status) }}
            </view>
          </view>
          <view class="order-body">
            <view class="order-info-row">
              <text class="label">营养师：</text>
              <text>{{ order.nutritionistName }}</text>
            </view>
            <view class="order-info-row">
              <text class="label">金额：</text>
              <text class="price-text">¥{{ order.amount?.toFixed(2) || '0.00' }}</text>
            </view>
            <view class="order-info-row">
              <text class="label">时间：</text>
              <text>{{ formatTime(order.createdAt) }}</text>
            </view>
          </view>

          <!-- Active Order: Chat -->
          <view class="order-actions" v-if="order.status === 'PAID' || order.status === 'IN_PROGRESS'">
            <view class="chat-btn" @tap="openChat(order)">💬 进入聊天室</view>
          </view>

          <!-- Chat Area -->
          <view class="chat-area" v-if="activeChatOrder === order.orderNo">
            <view class="messages-list">
              <view class="message-item" v-for="(msg, idx) in chatMessages" :key="idx" :class="msg.fromUser ? 'user-msg' : 'expert-msg'">
                <text class="msg-content">{{ msg.content }}</text>
                <text class="msg-time">{{ formatTime(msg.createdAt) }}</text>
              </view>
              <view class="empty-msg" v-if="chatMessages.length === 0">
                <text>暂无消息，开始咨询吧</text>
              </view>
            </view>
            <view class="chat-input-row">
              <input class="chat-input" v-model="chatText" placeholder="输入咨询内容..." />
              <view class="chat-send-btn" @tap="sendMessage(order.orderNo)">发送</view>
            </view>
          </view>

          <!-- Completed: Summary -->
          <view class="order-summary" v-if="order.status === 'COMPLETED' && order.summary">
            <text class="summary-label">咨询总结：</text>
            <text class="summary-text">{{ order.summary }}</text>
          </view>
        </view>
      </view>

      <view class="empty-state" v-if="!ordersLoading && orders.length === 0">
        <text class="empty-icon">📋</text>
        <text class="empty-text">暂无咨询订单</text>
      </view>
      <view class="loading-state" v-if="ordersLoading">
        <text>加载中...</text>
      </view>
    </view>

    <!-- Booking Modal -->
    <view class="modal-mask" v-if="showBooking" @tap="showBooking = false">
      <view class="modal-content" @tap.stop>
        <view class="modal-header">
          <text class="modal-title">营养师详情</text>
          <text class="modal-close" @tap="showBooking = false">✕</text>
        </view>

        <scroll-view class="modal-body" scroll-y>
          <view class="detail-top">
            <image class="detail-avatar" :src="defaultAvatar(selectedExpert?.avatar)" mode="aspectFill" />
            <view class="detail-info">
              <text class="detail-name">{{ selectedExpert?.name }}</text>
              <text class="detail-title">{{ selectedExpert?.title }}</text>
              <view class="detail-rating">
                <text>{{ getStars(selectedExpert?.rating) }} {{ selectedExpert?.rating?.toFixed(1) || '5.0' }}</text>
              </view>
            </view>
          </view>

          <view class="detail-section" v-if="selectedExpert?.bio">
            <text class="detail-label">简介</text>
            <text class="detail-text">{{ selectedExpert.bio }}</text>
          </view>

          <view class="detail-section" v-if="selectedExpert?.certifications">
            <text class="detail-label">资质证书</text>
            <text class="detail-text">{{ selectedExpert.certifications }}</text>
          </view>

          <view class="detail-section" v-if="selectedExpert?.availableSlots && selectedExpert.availableSlots.length > 0">
            <text class="detail-label">可预约时间</text>
            <view class="slot-grid">
              <view
                v-for="(slot, idx) in selectedExpert.availableSlots"
                :key="idx"
                class="time-slot"
                :class="{ active: selectedSlot === slot }"
                @tap="selectedSlot = slot"
              >
                {{ slot }}
              </view>
            </view>
          </view>

          <view class="detail-price">
            <text>咨询费用：</text>
            <text class="price-value">¥{{ selectedExpert?.price?.toFixed(2) || '0.00' }}</text>
          </view>
        </scroll-view>

        <view class="modal-footer">
          <view class="confirm-btn" @tap="handleCreateOrder">立即咨询</view>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { onLoad, onShow } from '@dcloudio/uni-app'
import { consultationApi } from '@/services/api'
import { checkLogin, formatTime, defaultAvatar } from '@/utils/common'

const activeTab = ref('list')
const showDisclaimer = ref(true)
const onlineFilter = ref('all')
const nutritionists = ref<any[]>([])
const listLoading = ref(false)

const orders = ref<any[]>([])
const ordersLoading = ref(false)

const showBooking = ref(false)
const selectedExpert = ref<any>(null)
const selectedSlot = ref('')

const activeChatOrder = ref('')
const chatMessages = ref<any[]>([])
const chatText = ref('')

function getStars(rating: number | undefined): string {
  const r = Math.round(rating || 5)
  return '⭐'.repeat(Math.min(r, 5))
}

function getStatusClass(status: string): string {
  const map: Record<string, string> = {
    PENDING: 'pending',
    PAID: 'active',
    IN_PROGRESS: 'active',
    COMPLETED: 'completed',
    CANCELLED: 'cancelled',
    REFUNDED: 'cancelled'
  }
  return map[status] || 'pending'
}

function getStatusText(status: string): string {
  const map: Record<string, string> = {
    PENDING: '待支付',
    PAID: '已支付',
    IN_PROGRESS: '咨询中',
    COMPLETED: '已完成',
    CANCELLED: '已取消',
    REFUNDED: '已退款'
  }
  return map[status] || status
}

async function loadNutritionists() {
  listLoading.value = true
  try {
    const res = onlineFilter.value === 'online'
      ? await consultationApi.getOnlineNutritionists()
      : await consultationApi.getNutritionists()
    nutritionists.value = res.data?.content || res.data?.records || res.data?.list || res.data || []
  } catch {
    uni.showToast({ title: '加载失败', icon: 'none' })
  } finally {
    listLoading.value = false
  }
}

async function loadOrders() {
  ordersLoading.value = true
  try {
    const res = await consultationApi.getOrders()
    orders.value = res.data?.content || res.data?.records || res.data?.list || res.data || []
  } catch {
    uni.showToast({ title: '加载失败', icon: 'none' })
  } finally {
    ordersLoading.value = false
  }
}

function switchToOrders() {
  if (!checkLogin()) return
  activeTab.value = 'orders'
  loadOrders()
}

function selectNutritionist(item: any) {
  selectedExpert.value = item
  selectedSlot.value = ''
  showBooking.value = true
}

function openBooking(item: any) {
  if (!checkLogin()) return
  selectNutritionist(item)
}

async function handleCreateOrder() {
  if (!checkLogin()) return
  if (!selectedExpert.value) return

  uni.showLoading({ title: '创建订单...' })
  try {
    const data: any = { nutritionistId: selectedExpert.value.id }
    if (selectedSlot.value) data.timeSlot = selectedSlot.value

    const res = await consultationApi.createOrder(data)
    const orderNo = res.data?.orderNo || res.data

    // Simulate payment
    uni.showLoading({ title: '支付中...' })
    await consultationApi.simulatePay(orderNo)

    showBooking.value = false
    uni.showToast({ title: '咨询预约成功', icon: 'success' })
    activeTab.value = 'orders'
    loadOrders()
  } catch (e: any) {
    uni.showToast({ title: e?.message || '预约失败', icon: 'none' })
  } finally {
    uni.hideLoading()
  }
}

async function openChat(order: any) {
  uni.navigateTo({ url: `/pages/consultation/chat?orderNo=${order.orderNo}` })
}

async function sendMessage(orderNo: string) {
  const text = chatText.value.trim()
  if (!text) return

  try {
    await consultationApi.sendMessage(orderNo, { content: text, type: 'TEXT' })
    chatMessages.value.push({
      content: text,
      fromUser: true,
      createdAt: new Date().toISOString()
    })
    chatText.value = ''
  } catch {
    uni.showToast({ title: '发送失败', icon: 'none' })
  }
}

onLoad((options: any) => {
  if (options?.tab === 'orders') {
    activeTab.value = 'orders'
    loadOrders()
  }
})

onShow(() => {
  loadNutritionists()
  if (activeTab.value === 'orders') loadOrders()
})
</script>

<style scoped>
.consultation-page {
  min-height: 100vh;
  background: #f5f5f5;
}

.tab-bar {
  display: flex;
  background: #fff;
  position: sticky;
  top: 0;
  z-index: 10;
}

.tab-item {
  flex: 1;
  text-align: center;
  padding: 28rpx 0;
  font-size: 28rpx;
  color: #666;
  position: relative;
}

.tab-item.active {
  color: #07c160;
  font-weight: 600;
}

.tab-item.active::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 50%;
  transform: translateX(-50%);
  width: 60rpx;
  height: 4rpx;
  background: #07c160;
  border-radius: 2rpx;
}

.filter-bar {
  display: flex;
  gap: 16rpx;
  padding: 20rpx 28rpx;
  background: #fff;
  border-top: 1rpx solid #f0f0f0;
}

.filter-item {
  padding: 10rpx 28rpx;
  border-radius: 30rpx;
  font-size: 26rpx;
  color: #666;
  background: #f0f0f0;
}

.filter-item.active {
  background: #07c160;
  color: #fff;
}

.nutritionist-list {
  padding: 16rpx;
}

.nutritionist-card {
  background: #fff;
  border-radius: 16rpx;
  padding: 28rpx;
  margin-bottom: 16rpx;
  box-shadow: 0 2rpx 12rpx rgba(0, 0, 0, 0.04);
}

.card-main {
  display: flex;
  margin-bottom: 20rpx;
}

.card-avatar-wrap {
  position: relative;
  margin-right: 20rpx;
  flex-shrink: 0;
}

.card-avatar {
  width: 100rpx;
  height: 100rpx;
  border-radius: 50%;
}

.online-dot {
  position: absolute;
  bottom: 4rpx;
  right: 4rpx;
  width: 20rpx;
  height: 20rpx;
  border-radius: 50%;
  background: #07c160;
  border: 3rpx solid #fff;
}

.card-info {
  flex: 1;
  min-width: 0;
}

.card-name-row {
  display: flex;
  align-items: center;
  gap: 12rpx;
  margin-bottom: 8rpx;
}

.card-name {
  font-size: 30rpx;
  font-weight: 600;
  color: #333;
}

.card-title {
  font-size: 22rpx;
  color: #07c160;
  background: rgba(7, 193, 96, 0.1);
  padding: 4rpx 12rpx;
  border-radius: 16rpx;
}

.card-specialty {
  font-size: 24rpx;
  color: #666;
  margin-bottom: 12rpx;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.card-stats {
  display: flex;
  align-items: center;
  gap: 20rpx;
}

.rating {
  display: flex;
  align-items: center;
  gap: 6rpx;
}

.stars {
  font-size: 20rpx;
}

.rating-num {
  font-size: 24rpx;
  color: #ff976a;
  font-weight: 600;
}

.consult-count {
  font-size: 22rpx;
  color: #999;
}

.card-bottom {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-top: 20rpx;
  border-top: 1rpx solid #f0f0f0;
}

.price {
  font-size: 30rpx;
  color: #ee0a24;
  font-weight: 600;
}

.book-btn {
  padding: 14rpx 36rpx;
  border-radius: 30rpx;
  font-size: 26rpx;
  background: #07c160;
  color: #fff;
}

/* Orders */
.order-list {
  padding: 16rpx;
}

.order-card {
  background: #fff;
  border-radius: 16rpx;
  padding: 28rpx;
  margin-bottom: 16rpx;
}

.order-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16rpx;
}

.order-no {
  font-size: 24rpx;
  color: #999;
}

.status-badge {
  font-size: 22rpx;
  padding: 6rpx 16rpx;
  border-radius: 16rpx;
}

.status-badge.pending { background: #fff3e0; color: #ff976a; }
.status-badge.active { background: #e8f5e9; color: #07c160; }
.status-badge.completed { background: #f0f0f0; color: #666; }
.status-badge.cancelled { background: #fce4ec; color: #ee0a24; }

.order-body {
  margin-bottom: 16rpx;
}

.order-info-row {
  display: flex;
  font-size: 26rpx;
  color: #333;
  margin-bottom: 8rpx;
}

.order-info-row .label {
  color: #999;
  flex-shrink: 0;
}

.price-text {
  color: #ee0a24;
  font-weight: 600;
}

.order-actions {
  padding-top: 16rpx;
  border-top: 1rpx solid #f0f0f0;
}

.chat-btn {
  text-align: center;
  padding: 16rpx;
  font-size: 28rpx;
  color: #07c160;
  background: rgba(7, 193, 96, 0.1);
  border-radius: 12rpx;
}

/* Chat Area */
.chat-area {
  margin-top: 16rpx;
  border: 1rpx solid #eee;
  border-radius: 12rpx;
  overflow: hidden;
}

.messages-list {
  max-height: 500rpx;
  overflow-y: auto;
  padding: 16rpx;
  background: #fafafa;
}

.message-item {
  margin-bottom: 16rpx;
  max-width: 80%;
}

.message-item.user-msg {
  margin-left: auto;
  text-align: right;
}

.msg-content {
  display: inline-block;
  padding: 16rpx 24rpx;
  border-radius: 16rpx;
  font-size: 26rpx;
  background: #fff;
  color: #333;
}

.user-msg .msg-content {
  background: #07c160;
  color: #fff;
}

.msg-time {
  display: block;
  font-size: 20rpx;
  color: #999;
  margin-top: 4rpx;
}

.empty-msg {
  text-align: center;
  padding: 40rpx;
  font-size: 24rpx;
  color: #999;
}

.chat-input-row {
  display: flex;
  padding: 12rpx;
  gap: 12rpx;
  background: #fff;
  border-top: 1rpx solid #eee;
}

.chat-input {
  flex: 1;
  height: 64rpx;
  background: #f5f5f5;
  border-radius: 32rpx;
  padding: 0 24rpx;
  font-size: 26rpx;
}

.chat-send-btn {
  padding: 0 24rpx;
  height: 64rpx;
  line-height: 64rpx;
  border-radius: 32rpx;
  font-size: 26rpx;
  background: #07c160;
  color: #fff;
  flex-shrink: 0;
}

.order-summary {
  margin-top: 16rpx;
  padding: 16rpx;
  background: #f9f9f9;
  border-radius: 12rpx;
}

.summary-label {
  font-size: 24rpx;
  color: #999;
  margin-bottom: 8rpx;
  display: block;
}

.summary-text {
  font-size: 26rpx;
  color: #333;
  line-height: 1.6;
}

/* Modal */
.modal-mask {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  z-index: 1000;
  display: flex;
  align-items: flex-end;
}

.modal-content {
  width: 100%;
  max-height: 80vh;
  background: #fff;
  border-radius: 24rpx 24rpx 0 0;
  display: flex;
  flex-direction: column;
}

.modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 28rpx;
  border-bottom: 1rpx solid #eee;
}

.modal-title {
  font-size: 32rpx;
  font-weight: 600;
  color: #333;
}

.modal-close {
  font-size: 36rpx;
  color: #999;
  padding: 8rpx;
}

.modal-body {
  flex: 1;
  padding: 28rpx;
  max-height: 60vh;
}

.detail-top {
  display: flex;
  align-items: center;
  margin-bottom: 28rpx;
}

.detail-avatar {
  width: 120rpx;
  height: 120rpx;
  border-radius: 50%;
  margin-right: 24rpx;
}

.detail-info {
  flex: 1;
}

.detail-name {
  font-size: 32rpx;
  font-weight: 600;
  color: #333;
  display: block;
}

.detail-title {
  font-size: 24rpx;
  color: #07c160;
  display: block;
  margin-top: 4rpx;
}

.detail-rating {
  font-size: 24rpx;
  color: #ff976a;
  margin-top: 8rpx;
}

.detail-section {
  margin-bottom: 24rpx;
}

.detail-label {
  font-size: 26rpx;
  font-weight: 600;
  color: #333;
  margin-bottom: 12rpx;
  display: block;
}

.detail-text {
  font-size: 26rpx;
  color: #666;
  line-height: 1.6;
}

.slot-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 12rpx;
}

.time-slot {
  padding: 12rpx 24rpx;
  border: 2rpx solid #eee;
  border-radius: 12rpx;
  font-size: 24rpx;
  color: #666;
}

.time-slot.active {
  border-color: #07c160;
  color: #07c160;
  background: rgba(7, 193, 96, 0.05);
}

.detail-price {
  font-size: 28rpx;
  color: #333;
  margin-top: 24rpx;
}

.price-value {
  color: #ee0a24;
  font-size: 36rpx;
  font-weight: 600;
}

.modal-footer {
  padding: 20rpx 28rpx;
  padding-bottom: calc(20rpx + env(safe-area-inset-bottom));
  border-top: 1rpx solid #eee;
}

.confirm-btn {
  text-align: center;
  padding: 24rpx;
  background: linear-gradient(135deg, #07c160, #06ad56);
  color: #fff;
  font-size: 30rpx;
  font-weight: 600;
  border-radius: 44rpx;
}

/* Common */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 200rpx 0;
}

.empty-icon {
  font-size: 80rpx;
  margin-bottom: 20rpx;
}

.empty-text {
  font-size: 28rpx;
  color: #999;
}

.loading-state {
  text-align: center;
  padding: 60rpx;
  font-size: 28rpx;
  color: #999;
}
.disclaimer-tip {
  background: #fff3cd;
  color: #856404;
  border-radius: 12rpx;
  padding: 14rpx 48rpx 14rpx 20rpx;
  font-size: 22rpx;
  margin: 0 20rpx 20rpx;
  position: relative;
}
.disclaimer-tip .dismiss {
  position: absolute;
  right: 16rpx;
  top: 50%;
  transform: translateY(-50%);
  font-size: 28rpx;
  color: #999;
}
</style>
