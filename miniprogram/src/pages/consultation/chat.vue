<template>
  <view class="chat-page">
    <!-- Header -->
    <view class="chat-header">
      <view class="header-back" @tap="goBack">← 返回</view>
      <view class="header-title">
        <text class="title-text">{{ order?.nutritionistName || '营养师咨询' }}</text>
        <text class="status-tag" :class="statusClass">{{ statusText }}</text>
      </view>
      <view class="header-action" v-if="order?.status === 'IN_PROGRESS'" @tap="handleComplete">
        结束
      </view>
    </view>

    <!-- Messages -->
    <scroll-view
      class="chat-body"
      scroll-y
      :scroll-into-view="scrollAnchor"
      :scroll-with-animation="true"
    >
      <!-- Consultation Description -->
      <view class="desc-card" v-if="order?.description">
        <text class="desc-label">咨询描述</text>
        <text class="desc-content">{{ order.description }}</text>
      </view>

      <!-- Message List -->
      <view
        v-for="(msg, idx) in messages"
        :key="idx"
        :id="'msg-' + idx"
        class="message-item"
        :class="msg.role === 'user' ? 'msg-right' : 'msg-left'"
      >
        <view class="msg-bubble">
          <text class="msg-text">{{ msg.content }}</text>
          <text class="msg-time">{{ formatTime(msg.timestamp) }}</text>
        </view>
      </view>

      <!-- IM Status -->
      <view class="im-status" v-if="imReady">
        <text class="im-dot">●</text>
        <text class="im-text">实时连接</text>
      </view>

      <view id="scroll-bottom" style="height: 2rpx;"></view>
    </scroll-view>

    <!-- Input Area -->
    <view class="chat-input-area" v-if="order?.status === 'IN_PROGRESS'">
      <input
        class="chat-input"
        v-model="inputText"
        placeholder="输入咨询内容..."
        confirm-type="send"
        @confirm="sendMessage"
      />
      <view class="send-btn" :class="{ disabled: !inputText.trim() }" @tap="sendMessage">
        发送
      </view>
    </view>

    <!-- Completed Footer -->
    <view class="completed-footer" v-else-if="order?.status === 'COMPLETED'">
      <text>咨询已结束</text>
    </view>

    <!-- Rating Modal -->
    <view class="modal-mask" v-if="showRating" @tap="showRating = false">
      <view class="modal-content" @tap.stop>
        <view class="modal-title">评价咨询服务</view>
        <view class="rating-row">
          <text
            v-for="i in 5"
            :key="i"
            class="star"
            :class="{ active: i <= ratingScore }"
            @tap="ratingScore = i"
          >★</text>
        </view>
        <input class="review-input" v-model="reviewText" placeholder="请输入评价内容..." />
        <view class="modal-actions">
          <view class="modal-btn cancel" @tap="showRating = false">取消</view>
          <view class="modal-btn confirm" @tap="submitRating">提交</view>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { consultationApi } from '@/services/api'
import {
  initTim, loginTim, logoutTim, onMessageReceived, offMessageReceived,
  getOrderNoFromMessage, getTextFromMessage
} from '@/services/tim'
import { checkLogin, formatTime } from '@/utils/common'

const orderNo = ref('')
const order = ref<any>(null)
const messages = computed(() => order.value?.messages || [])
const inputText = ref('')
const scrollAnchor = ref('')
const imReady = ref(false)
let pollTimer: any = null

const showRating = ref(false)
const ratingScore = ref(5)
const reviewText = ref('')

const statusText = computed(() => {
  const map: Record<string, string> = {
    IN_PROGRESS: '咨询中', WAITING: '等待中', COMPLETED: '已完成', CANCELLED: '已取消'
  }
  return map[order.value?.status] || ''
})

const statusClass = computed(() => {
  const map: Record<string, string> = {
    IN_PROGRESS: 'active', WAITING: 'pending', COMPLETED: 'done', CANCELLED: 'cancelled'
  }
  return map[order.value?.status] || ''
})

onLoad((options: any) => {
  orderNo.value = options?.orderNo || ''
})

onMounted(async () => {
  if (!orderNo.value) {
    uni.showToast({ title: '订单号缺失', icon: 'none' })
    return
  }
  await fetchOrder()
  await initImConnection()
  if (!imReady.value) {
    startPolling()
  }
})

onUnmounted(() => {
  stopPolling()
  offMessageReceived()
  logoutTim().catch(() => {})
})

async function fetchOrder() {
  try {
    const res = await consultationApi.getOrderDetail(orderNo.value)
    if (res.data) {
      order.value = res.data
      await nextTick()
      scrollToBottom()
    }
  } catch (e) {
    uni.showToast({ title: '加载咨询失败', icon: 'none' })
  }
}

async function initImConnection() {
  try {
    const res = await consultationApi.getImConfig(orderNo.value)
    if (!res.data) {
      console.warn('[IM] 获取IM配置失败')
      return
    }

    const { sdkAppId, userId, userSig, peerUserId } = res.data

    await initTim(sdkAppId)
    await loginTim(userId, userSig)

    onMessageReceived((msg: any) => {
      const msgOrderNo = getOrderNoFromMessage(msg)
      if (msgOrderNo === orderNo.value && msg.from === peerUserId) {
        const text = getTextFromMessage(msg)
        if (text && order.value) {
          if (!order.value.messages) order.value.messages = []
          order.value.messages.push({
            role: 'nutritionist',
            content: text,
            timestamp: new Date().toISOString()
          })
          nextTick(() => scrollToBottom())
        }
      }
    })

    imReady.value = true
    console.log('[IM] 实时消息连接就绪')
  } catch (e: any) {
    console.warn('[IM] 初始化失败，降级轮询:', e.message || e)
    startPolling()
  }
}

function startPolling() {
  if (pollTimer) return
  pollTimer = setInterval(async () => {
    if (!order.value || order.value.status === 'COMPLETED' || order.value.status === 'CANCELLED') {
      stopPolling()
      return
    }
    try {
      const res = await consultationApi.getOrderDetail(orderNo.value)
      if (res.data) {
        const oldLen = (order.value.messages || []).length
        const newLen = (res.data.messages || []).length
        order.value = res.data
        if (newLen > oldLen) {
          await nextTick()
          scrollToBottom()
        }
      }
    } catch { /* silent */ }
  }, 5000)
}

function stopPolling() {
  if (pollTimer) { clearInterval(pollTimer); pollTimer = null }
}

async function sendMessage() {
  const text = inputText.value.trim()
  if (!text) return

  inputText.value = ''
  try {
    const res = await consultationApi.sendMessage(orderNo.value, { content: text, role: 'user' })
    if (res.data) {
      order.value = res.data
      await nextTick()
      scrollToBottom()
    }
  } catch {
    uni.showToast({ title: '发送失败', icon: 'none' })
    inputText.value = text
  }
}

function handleComplete() {
  showRating.value = true
  ratingScore.value = 5
  reviewText.value = ''
}

async function submitRating() {
  try {
    const res = await consultationApi.completeOrder(orderNo.value, {
      rating: ratingScore.value,
      review: reviewText.value
    })
    if (res.data) {
      order.value = res.data
      showRating.value = false
      stopPolling()
      uni.showToast({ title: '感谢您的评价！', icon: 'success' })
    }
  } catch {
    uni.showToast({ title: '操作失败', icon: 'none' })
  }
}

function scrollToBottom() {
  scrollAnchor.value = ''
  nextTick(() => { scrollAnchor.value = 'scroll-bottom' })
}

function goBack() {
  uni.navigateBack({ fail: () => uni.switchTab({ url: '/pages/profile/index' }) })
}
</script>

<style lang="scss" scoped>
.chat-page {
  display: flex;
  flex-direction: column;
  height: 100vh;
  background: $background;
}

.chat-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24rpx;
  height: 88rpx;
  background: $card;
  border-bottom: 1rpx solid $border;
  flex-shrink: 0;
}

.header-back {
  font-size: 28rpx;
  color: $accent;
  padding: 10rpx;
  font-family: 'Inter', 'PingFang SC', sans-serif;
}

.header-title {
  display: flex;
  align-items: center;
  gap: 12rpx;
}

.title-text {
  font-size: 30rpx;
  font-weight: 600;
  color: $foreground;
  font-family: 'Calistoga', 'PingFang SC', sans-serif;
}

.status-tag {
  font-size: 20rpx;
  padding: 4rpx 12rpx;
  border-radius: $radius-full;
  border: none;
  font-family: 'JetBrains Mono', 'PingFang SC', monospace;
}
.status-tag.active { background: rgba(16, 185, 129, 0.1); color: $uni-success; }
.status-tag.pending { background: rgba(245, 158, 11, 0.1); color: $uni-warning; }
.status-tag.done { background: $muted; color: $muted-foreground; }
.status-tag.cancelled { background: rgba(239, 68, 68, 0.1); color: $uni-error; }

.header-action {
  font-size: 26rpx;
  color: $uni-error;
  padding: 10rpx;
  font-family: 'Inter', 'PingFang SC', sans-serif;
}

.chat-body {
  flex: 1;
  padding: 20rpx;
}

.desc-card {
  background: $muted;
  border: 1rpx solid $border;
  border-radius: $radius-lg;
  padding: 20rpx;
  margin-bottom: 20rpx;
}

.desc-label {
  font-size: 22rpx;
  color: $muted-foreground;
  display: block;
  margin-bottom: 8rpx;
  font-family: 'Calistoga', 'PingFang SC', sans-serif;
  font-weight: 600;
}

.desc-content {
  font-size: 26rpx;
  color: $foreground;
  line-height: 1.6;
  font-family: 'Inter', 'PingFang SC', sans-serif;
}

.message-item {
  display: flex;
  margin-bottom: 20rpx;
}

.message-item.msg-right {
  justify-content: flex-end;
}

.message-item.msg-left {
  justify-content: flex-start;
}

.msg-bubble {
  max-width: 70%;
  padding: 16rpx 24rpx;
  border: none;
}

.msg-right .msg-bubble {
  background: $accent;
  color: $accent-foreground;
  border-radius: $radius-xl $radius-xl 4rpx $radius-xl;
  box-shadow: $shadow-accent;
}

.msg-left .msg-bubble {
  background: $card;
  color: $foreground;
  border-radius: $radius-xl $radius-xl $radius-xl 4rpx;
  box-shadow: $shadow-sm;
  border: 1rpx solid $border;
}

.msg-text {
  font-size: 28rpx;
  line-height: 1.6;
  word-break: break-word;
  font-family: 'Inter', 'PingFang SC', sans-serif;
}

.msg-time {
  display: block;
  font-size: 20rpx;
  margin-top: 6rpx;
  text-align: right;
  font-family: 'Inter', 'PingFang SC', sans-serif;
}

.msg-right .msg-time { color: rgba(255, 255, 255, 0.7); }
.msg-left .msg-time { color: $muted-foreground; }

.im-status {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8rpx;
  padding: 12rpx;
}

.im-dot {
  font-size: 16rpx;
  color: $uni-success;
}

.im-text {
  font-size: 20rpx;
  color: $muted-foreground;
  font-family: 'Inter', 'PingFang SC', sans-serif;
}

.chat-input-area {
  display: flex;
  align-items: center;
  gap: 12rpx;
  padding: 16rpx 20rpx;
  padding-bottom: calc(16rpx + env(safe-area-inset-bottom));
  background: $card;
  border-top: 1rpx solid $border;
  flex-shrink: 0;
}

.chat-input {
  flex: 1;
  height: 72rpx;
  background: $muted;
  border: 1rpx solid $border;
  border-radius: $radius-lg;
  padding: 0 24rpx;
  font-size: 28rpx;
  font-family: 'Inter', 'PingFang SC', sans-serif;
  color: $foreground;
}

.send-btn {
  padding: 0 32rpx;
  height: 72rpx;
  line-height: 72rpx;
  border-radius: $radius-lg;
  font-size: 28rpx;
  background: $accent;
  color: $accent-foreground;
  border: none;
  box-shadow: $shadow-accent;
  flex-shrink: 0;
  font-family: 'Inter', 'PingFang SC', sans-serif;
  transition: transform 0.15s ease;
}
.send-btn:active {
  transform: scale(0.97);
}

.send-btn.disabled {
  opacity: 0.4;
  box-shadow: none;
}

.completed-footer {
  text-align: center;
  padding: 24rpx;
  padding-bottom: calc(24rpx + env(safe-area-inset-bottom));
  background: $card;
  font-size: 28rpx;
  color: $muted-foreground;
  border-top: 1rpx solid $border;
  font-family: 'Inter', 'PingFang SC', sans-serif;
}

/* Rating Modal */
.modal-mask {
  position: fixed;
  top: 0; left: 0; right: 0; bottom: 0;
  background: rgba(15, 23, 42, 0.5);
  z-index: 1000;
  display: flex;
  align-items: center;
  justify-content: center;
}

.modal-content {
  width: 80%;
  max-width: 600rpx;
  background: $card;
  border: none;
  border-radius: $radius-2xl;
  padding: 32rpx;
  box-shadow: $shadow-lg;
  box-sizing: border-box;
  overflow-x: hidden;
}

.modal-title {
  font-size: 32rpx;
  font-weight: 600;
  text-align: center;
  margin-bottom: 24rpx;
  color: $foreground;
  font-family: 'Calistoga', 'PingFang SC', sans-serif;
}

.rating-row {
  display: flex;
  justify-content: center;
  gap: 16rpx;
  margin-bottom: 24rpx;
}

.star {
  font-size: 48rpx;
  color: $border;
}

.star.active {
  color: $uni-warning;
}

.review-input {
  width: 100%;
  height: 80rpx;
  background: $muted;
  border: 1rpx solid $border;
  border-radius: $radius-lg;
  padding: 0 20rpx;
  font-size: 26rpx;
  margin-bottom: 24rpx;
  font-family: 'Inter', 'PingFang SC', sans-serif;
  color: $foreground;
}

.modal-actions {
  display: flex;
  gap: 16rpx;
}

.modal-btn {
  flex: 1;
  text-align: center;
  padding: 20rpx;
  border-radius: $radius-lg;
  font-size: 28rpx;
  border: none;
  font-family: 'Inter', 'PingFang SC', sans-serif;
  transition: transform 0.15s ease;
}

.modal-btn.cancel {
  background: $muted;
  color: $foreground;
}

.modal-btn.confirm {
  background: $accent;
  color: $accent-foreground;
  box-shadow: $shadow-accent;
}
.modal-btn.confirm:active {
  transform: scale(0.97);
}
</style>
