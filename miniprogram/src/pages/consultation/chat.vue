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

    initTim(sdkAppId)
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

<style scoped>
.chat-page {
  display: flex;
  flex-direction: column;
  height: 100vh;
  background: #f5f5f5;
}

.chat-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24rpx;
  height: 88rpx;
  background: #fff;
  border-bottom: 1rpx solid #eee;
  flex-shrink: 0;
}

.header-back {
  font-size: 28rpx;
  color: #07c160;
  padding: 10rpx;
}

.header-title {
  display: flex;
  align-items: center;
  gap: 12rpx;
}

.title-text {
  font-size: 30rpx;
  font-weight: 600;
  color: #333;
}

.status-tag {
  font-size: 20rpx;
  padding: 4rpx 12rpx;
  border-radius: 16rpx;
}
.status-tag.active { background: #e8f5e9; color: #07c160; }
.status-tag.pending { background: #fff3e0; color: #ff976a; }
.status-tag.done { background: #f0f0f0; color: #666; }
.status-tag.cancelled { background: #fce4ec; color: #ee0a24; }

.header-action {
  font-size: 26rpx;
  color: #ee0a24;
  padding: 10rpx;
}

.chat-body {
  flex: 1;
  padding: 20rpx;
}

.desc-card {
  background: #fff;
  border-radius: 16rpx;
  padding: 20rpx;
  margin-bottom: 20rpx;
  border: 1rpx solid #eee;
}

.desc-label {
  font-size: 22rpx;
  color: #999;
  display: block;
  margin-bottom: 8rpx;
}

.desc-content {
  font-size: 26rpx;
  color: #333;
  line-height: 1.6;
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
  border-radius: 20rpx;
}

.msg-right .msg-bubble {
  background: #07c160;
  color: #fff;
  border-radius: 20rpx 20rpx 6rpx 20rpx;
}

.msg-left .msg-bubble {
  background: #fff;
  color: #333;
  border: 1rpx solid #eee;
  border-radius: 20rpx 20rpx 20rpx 6rpx;
}

.msg-text {
  font-size: 28rpx;
  line-height: 1.6;
  word-break: break-word;
}

.msg-time {
  display: block;
  font-size: 20rpx;
  margin-top: 6rpx;
  text-align: right;
}

.msg-right .msg-time { color: rgba(255,255,255,0.7); }
.msg-left .msg-time { color: #999; }

.im-status {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8rpx;
  padding: 12rpx;
}

.im-dot {
  font-size: 16rpx;
  color: #07c160;
}

.im-text {
  font-size: 20rpx;
  color: #999;
}

.chat-input-area {
  display: flex;
  align-items: center;
  gap: 12rpx;
  padding: 16rpx 20rpx;
  padding-bottom: calc(16rpx + env(safe-area-inset-bottom));
  background: #fff;
  border-top: 1rpx solid #eee;
  flex-shrink: 0;
}

.chat-input {
  flex: 1;
  height: 72rpx;
  background: #f5f5f5;
  border-radius: 36rpx;
  padding: 0 24rpx;
  font-size: 28rpx;
}

.send-btn {
  padding: 0 32rpx;
  height: 72rpx;
  line-height: 72rpx;
  border-radius: 36rpx;
  font-size: 28rpx;
  background: #07c160;
  color: #fff;
  flex-shrink: 0;
}

.send-btn.disabled {
  opacity: 0.5;
}

.completed-footer {
  text-align: center;
  padding: 24rpx;
  padding-bottom: calc(24rpx + env(safe-area-inset-bottom));
  background: #fff;
  font-size: 28rpx;
  color: #999;
}

/* Rating Modal */
.modal-mask {
  position: fixed;
  top: 0; left: 0; right: 0; bottom: 0;
  background: rgba(0,0,0,0.5);
  z-index: 1000;
  display: flex;
  align-items: center;
  justify-content: center;
}

.modal-content {
  width: 80%;
  background: #fff;
  border-radius: 20rpx;
  padding: 32rpx;
}

.modal-title {
  font-size: 32rpx;
  font-weight: 600;
  text-align: center;
  margin-bottom: 24rpx;
}

.rating-row {
  display: flex;
  justify-content: center;
  gap: 16rpx;
  margin-bottom: 24rpx;
}

.star {
  font-size: 48rpx;
  color: #ddd;
}

.star.active {
  color: #ff976a;
}

.review-input {
  width: 100%;
  height: 80rpx;
  background: #f5f5f5;
  border-radius: 12rpx;
  padding: 0 20rpx;
  font-size: 26rpx;
  margin-bottom: 24rpx;
}

.modal-actions {
  display: flex;
  gap: 16rpx;
}

.modal-btn {
  flex: 1;
  text-align: center;
  padding: 20rpx;
  border-radius: 12rpx;
  font-size: 28rpx;
}

.modal-btn.cancel {
  background: #f5f5f5;
  color: #666;
}

.modal-btn.confirm {
  background: #07c160;
  color: #fff;
}
</style>
