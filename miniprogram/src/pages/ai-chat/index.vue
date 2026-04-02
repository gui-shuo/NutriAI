<template>
  <view class="chat-page">
    <!-- Custom Navigation Bar -->
    <view class="nav-bar" :style="{ paddingTop: statusBarHeight + 'px' }">
      <view class="nav-bar-inner flex-between">
        <view class="nav-back" @tap="goBack">
          <text class="icon-back">‹</text>
        </view>
        <text class="nav-title">AI 营养师</text>
        <view class="nav-placeholder" />
      </view>
    </view>

    <!-- Disclaimer -->
    <view class="disclaimer-bar" v-if="showDisclaimer" :style="{ top: navHeight + 'px' }">
      <text>⚕️ AI建议仅供参考，不构成医疗建议。如有身体不适请咨询专业医生。</text>
      <text class="dismiss" @tap="showDisclaimer = false">✕</text>
    </view>

    <!-- Connection Status -->
    <view class="conn-bar" v-if="!connected && !isConnecting" :style="{ top: (navHeight + (showDisclaimer ? 32 : 0)) + 'px' }">
      <text class="conn-text">⚠️ 连接已断开</text>
      <text class="conn-retry" @tap="retryConnect">点击重连</text>
    </view>
    <view class="conn-bar connecting" v-else-if="isConnecting" :style="{ top: (navHeight + (showDisclaimer ? 32 : 0)) + 'px' }">
      <text class="conn-text">🔄 正在连接...</text>
    </view>
    <view class="conn-bar connected" v-else-if="showConnected" :style="{ top: (navHeight + (showDisclaimer ? 32 : 0)) + 'px' }">
      <text class="conn-text">✅ 已连接</text>
    </view>

    <!-- Message List -->
    <scroll-view
      class="message-list"
      scroll-y
      :scroll-top="scrollTop"
      :scroll-into-view="scrollIntoView"
      :style="{ top: (navHeight + (showDisclaimer ? 32 : 0) + (!connected || showConnected ? 32 : 0)) + 'px', bottom: inputAreaHeight + 'px' }"
    >
      <view class="message-wrapper" v-for="(msg, idx) in messages" :key="idx" :id="'msg-' + idx">
        <!-- AI Message -->
        <view v-if="msg.role === 'assistant'" class="message-row assistant">
          <view class="avatar avatar-ai">🤖</view>
          <view class="bubble bubble-ai">
            <text class="bubble-text">{{ msg.content }}</text>
          </view>
        </view>
        <!-- User Message -->
        <view v-else class="message-row user">
          <view class="bubble bubble-user">
            <text class="bubble-text">{{ msg.content }}</text>
          </view>
          <image class="avatar avatar-user" :src="userAvatar" mode="aspectFill" />
        </view>
      </view>

      <!-- Typing Indicator -->
      <view v-if="isTyping" class="message-row assistant" id="typing-indicator">
        <view class="avatar avatar-ai">🤖</view>
        <view class="bubble bubble-ai typing-bubble">
          <view class="typing-dots">
            <view class="dot" />
            <view class="dot" />
            <view class="dot" />
          </view>
          <text v-if="waitingSeconds >= 5" class="waiting-hint">
            {{ waitingSeconds >= 15 ? 'AI正在深度思考，请耐心等待...' : '网络响应中，请稍候...' }}
          </text>
        </view>
      </view>

      <view class="scroll-bottom-anchor" :id="'msg-' + messages.length" />
    </scroll-view>

    <!-- Bottom Input Area -->
    <view class="input-area" :id="'input-area'">
      <view class="input-row flex">
        <input
          class="chat-input flex-1"
          v-model="inputText"
          placeholder="输入你的营养饮食问题..."
          :disabled="isSending"
          confirm-type="send"
          @confirm="sendMessage"
          :adjust-position="true"
        />
        <view
          class="send-btn"
          :class="{ 'send-btn-active': inputText.trim() && !isSending }"
          @tap="sendMessage"
        >
          <text>发送</text>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, nextTick, onMounted, onUnmounted, computed } from 'vue'
import { onLoad, onShow, onHide } from '@dcloudio/uni-app'
import { checkLogin, defaultAvatar } from '@/utils/common'
import { getToken } from '@/utils/request'
import { useUserStore } from '@/stores/user'

interface ChatMessage {
  role: 'user' | 'assistant'
  content: string
}

// #ifdef H5
const WS_URL = `${window.location.protocol === 'https:' ? 'wss:' : 'ws:'}//${window.location.host}/api/ws/ai/chat`
// #endif
// #ifndef H5
const WS_URL = 'wss://nutriai.itshuo.me/api/ws/ai/chat'
// #endif

const userStore = useUserStore()
const userAvatar = computed(() => defaultAvatar(userStore.userInfo?.avatar))

const messages = ref<ChatMessage[]>([])
const inputText = ref('')
const isTyping = ref(false)
const isSending = ref(false)
const scrollTop = ref(0)
const scrollIntoView = ref('')
const statusBarHeight = ref(0)
const navHeight = ref(0)
const inputAreaHeight = ref(100)
const showDisclaimer = ref(true)
const showConnected = ref(false)
let connectedTimer: ReturnType<typeof setTimeout> | null = null

const waitingSeconds = ref(0)
let waitingTimer: ReturnType<typeof setInterval> | null = null

let socketTask: UniApp.SocketTask | null = null
const connected = ref(false)
let reconnectTimer: ReturnType<typeof setTimeout> | null = null
let reconnectAttempts = 0
const MAX_RECONNECT = 5
const isConnecting = ref(false)
let pageVisible = true
let heartbeatTimer: ReturnType<typeof setInterval> | null = null

function startWaitingTimer() {
  stopWaitingTimer()
  waitingSeconds.value = 0
  waitingTimer = setInterval(() => { waitingSeconds.value++ }, 1000)
}

function stopWaitingTimer() {
  if (waitingTimer) { clearInterval(waitingTimer); waitingTimer = null }
  waitingSeconds.value = 0
}

onLoad(() => {
  if (!checkLogin()) return
  const sysInfo = uni.getSystemInfoSync()
  // #ifdef APP-PLUS
  statusBarHeight.value = sysInfo.statusBarHeight || 44
  // #endif
  // #ifdef H5
  statusBarHeight.value = 0
  // #endif
  // #ifdef MP
  statusBarHeight.value = sysInfo.statusBarHeight || 20
  // #endif
  navHeight.value = statusBarHeight.value + 44
  // Account for tabBar height (windowBottom) + safe area
  const tabBarHeight = sysInfo.windowBottom || 50
  inputAreaHeight.value = 100 + tabBarHeight + (sysInfo.safeAreaInsets?.bottom || 0)

  messages.value.push({
    role: 'assistant',
    content: '你好！我是AI营养师，有什么营养饮食问题可以问我哦～'
  })

  connectWebSocket()
})

onShow(() => {
  pageVisible = true
  if (!connected.value && !isConnecting.value && getToken()) {
    connectWebSocket()
  }
})

onHide(() => {
  pageVisible = false
  clearReconnectTimer()
})

onUnmounted(() => {
  pageVisible = false
  closeWebSocket()
  clearReconnectTimer()
  stopWaitingTimer()
})

function retryConnect() {
  reconnectAttempts = 0
  connectWebSocket()
}

function connectWebSocket() {
  const token = getToken()
  if (!token || isConnecting.value) return

  // Close existing connection without triggering reconnect
  if (socketTask) {
    const oldTask = socketTask
    socketTask = null
    connected.value = false
    try { oldTask.close({}) } catch {}
  }

  isConnecting.value = true
  const wsUrl = `${WS_URL}?token=${encodeURIComponent(token)}`
  socketTask = uni.connectSocket({
    url: wsUrl,
    success: () => {},
    fail: (err) => {
      console.error('WebSocket connect fail:', err)
      isConnecting.value = false
      scheduleReconnect()
    }
  })

  const currentTask = socketTask

  socketTask.onOpen(() => {
    if (currentTask !== socketTask) return
    connected.value = true
    isConnecting.value = false
    reconnectAttempts = 0
    startHeartbeat()
    // Show "已连接" indicator for 2 seconds
    showConnected.value = true
    if (connectedTimer) clearTimeout(connectedTimer)
    connectedTimer = setTimeout(() => { showConnected.value = false }, 2000)
    if (pendingMessage) {
      const msg = pendingMessage
      pendingMessage = ''
      doSend(msg)
    }
  })

  socketTask.onMessage((res) => {
    if (currentTask !== socketTask) return
    try {
      const data = JSON.parse(res.data as string)
      handleMessage(data)
    } catch (e) {
      console.error('Parse message error:', e)
    }
  })

  socketTask.onError(() => {
    if (currentTask !== socketTask) return
    connected.value = false
    isConnecting.value = false
    scheduleReconnect()
  })

  socketTask.onClose(() => {
    if (currentTask !== socketTask) return
    connected.value = false
    isConnecting.value = false
    if (pageVisible && !isSending.value) {
      scheduleReconnect()
    }
  })
}

function handleMessage(data: { type: string; content?: string; message?: string; status?: string }) {
  if (data.type === 'connection') {
    return
  } else if (data.type === 'start') {
    if (!isTyping.value) isTyping.value = true
  } else if (data.type === 'token' || data.type === 'chunk') {
    stopWaitingTimer()
    const lastMsg = messages.value[messages.value.length - 1]
    if (lastMsg && lastMsg.role === 'assistant' && isTyping.value) {
      lastMsg.content += data.content || ''
    } else {
      messages.value.push({ role: 'assistant', content: data.content || '' })
    }
    scrollToBottom()
  } else if (data.type === 'done' || data.type === 'complete') {
    stopWaitingTimer()
    isTyping.value = false
    isSending.value = false
    scrollToBottom()
  } else if (data.type === 'error') {
    stopWaitingTimer()
    isTyping.value = false
    isSending.value = false
    uni.showToast({ title: data.message || 'AI响应出错', icon: 'none' })
  } else if (data.type === 'pong') {
    // Heartbeat response
  }
}

let pendingMessage = ''

function sendMessage() {
  const text = inputText.value.trim()
  if (!text || isSending.value) return

  messages.value.push({ role: 'user', content: text })
  inputText.value = ''
  isSending.value = true
  isTyping.value = true
  startWaitingTimer()
  scrollToBottom()

  if (!connected.value) {
    pendingMessage = text
    connectWebSocket()
    return
  }

  doSend(text)
}

function doSend(text: string) {
  socketTask?.send({
    data: JSON.stringify({ type: 'chat', message: text }),
    success: () => {},
    fail: () => {
      stopWaitingTimer()
      isSending.value = false
      isTyping.value = false
      uni.showToast({ title: '发送失败，请重试', icon: 'none' })
    }
  })
}

function scrollToBottom() {
  nextTick(() => {
    scrollIntoView.value = ''
    setTimeout(() => {
      scrollIntoView.value = 'msg-' + messages.value.length
    }, 50)
  })
}

function scheduleReconnect() {
  clearReconnectTimer()
  if (reconnectAttempts >= MAX_RECONNECT) {
    uni.showToast({ title: '连接失败，请重新进入', icon: 'none' })
    return
  }
  const delay = Math.min(1000 * Math.pow(2, reconnectAttempts), 10000)
  reconnectAttempts++
  reconnectTimer = setTimeout(() => {
    if (!connected.value) connectWebSocket()
  }, delay)
}

function clearReconnectTimer() {
  if (reconnectTimer) {
    clearTimeout(reconnectTimer)
    reconnectTimer = null
  }
}

function closeWebSocket() {
  stopHeartbeat()
  const task = socketTask
  socketTask = null
  connected.value = false
  isConnecting.value = false
  if (task) {
    try { task.close({}) } catch {}
  }
}

function startHeartbeat() {
  stopHeartbeat()
  heartbeatTimer = setInterval(() => {
    if (connected.value && socketTask) {
      socketTask.send({
        data: JSON.stringify({ type: 'ping' }),
        fail: () => {}
      })
    }
  }, 25000)
}

function stopHeartbeat() {
  if (heartbeatTimer) {
    clearInterval(heartbeatTimer)
    heartbeatTimer = null
  }
}

function goBack() {
  uni.navigateBack({ fail: () => uni.switchTab({ url: '/pages/index/index' }) })
}
</script>

<style lang="scss" scoped>
.chat-page {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: $background;
}

/* Navigation Bar */
.nav-bar {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 100;
  background: $gradient-accent;
  box-shadow: $shadow-md;
}
.nav-bar-inner {
  height: 88rpx;
  padding: 0 20rpx;
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.nav-back {
  width: 80rpx;
  height: 80rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}
.icon-back {
  font-size: 48rpx;
  color: #fff;
  font-weight: bold;
}
.nav-title {
  font-size: 34rpx;
  font-weight: 700;
  color: #fff;
  font-family: 'Calistoga', Georgia, 'PingFang SC', serif;
}
.nav-placeholder {
  width: 80rpx;
}

/* Message List */
.message-list {
  position: fixed;
  left: 0;
  right: 0;
  overflow-y: auto;
  padding: 20rpx 24rpx;
  background: $background;
}

.message-wrapper {
  margin-bottom: 24rpx;
}

.message-row {
  display: flex;
  align-items: flex-start;
  padding: 0 8rpx;
}
.message-row.user {
  justify-content: flex-end;
}
.message-row.assistant {
  justify-content: flex-start;
}

.avatar {
  width: 72rpx;
  height: 72rpx;
  min-width: 72rpx;
  border-radius: $radius-full;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 36rpx;
  flex-shrink: 0;
  overflow: hidden;
  border: 1rpx solid $border;
}
.avatar-ai {
  background: $card;
  margin-right: 16rpx;
  box-shadow: $shadow-sm;
}
.avatar-user {
  margin-left: 16rpx;
  background: $muted;
}

/* Chat bubbles */
.bubble {
  max-width: 65%;
  padding: 20rpx 28rpx;
  border: 1rpx solid $border;
  border-radius: $radius-xl;
  word-break: break-all;
}
.bubble-ai {
  background: $card;
  border-top-left-radius: $radius-sm;
  box-shadow: $shadow-sm;
}
.bubble-user {
  background: $accent;
  border-color: $accent;
  border-top-right-radius: $radius-sm;
  box-shadow: $shadow-accent;
}
.bubble-user .bubble-text {
  color: #fff;
}
.bubble-text {
  font-size: 30rpx;
  line-height: 1.6;
  color: $foreground;
  font-family: 'Inter', 'PingFang SC', sans-serif;
}

/* Typing Indicator */
.typing-bubble {
  padding: 24rpx 32rpx;
}
.waiting-hint {
  display: block;
  font-size: 22rpx;
  color: $muted-foreground;
  margin-top: 12rpx;
  animation: fadeIn 0.3s;
}
@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}
.typing-dots {
  display: flex;
  align-items: center;
  gap: 8rpx;
}
.dot {
  width: 14rpx;
  height: 14rpx;
  background: $accent;
  border-radius: 50%;
  animation: bounce 1.4s infinite ease-in-out;
}
.dot:nth-child(1) { animation-delay: 0s; }
.dot:nth-child(2) { animation-delay: 0.2s; }
.dot:nth-child(3) { animation-delay: 0.4s; }

@keyframes bounce {
  0%, 80%, 100% { transform: scale(0.6); opacity: 0.4; }
  40% { transform: scale(1); opacity: 1; }
}

.scroll-bottom-anchor {
  height: 2rpx;
}

/* Input Area */
.input-area {
  position: fixed;
  bottom: var(--window-bottom, 0);
  left: 0;
  right: 0;
  background: $card;
  padding: 16rpx 24rpx;
  padding-bottom: calc(16rpx + env(safe-area-inset-bottom));
  border-top: 1rpx solid $border;
  z-index: 100;
  box-shadow: 0 -2px 8px rgba(0, 0, 0, 0.04);
}
.input-row {
  display: flex;
  align-items: center;
  gap: 16rpx;
}
.chat-input {
  flex: 1;
  height: 72rpx;
  background: $muted;
  border: 1rpx solid $border;
  border-radius: $radius-full;
  padding: 0 28rpx;
  font-size: 28rpx;
  color: $foreground;
  font-family: 'Inter', 'PingFang SC', sans-serif;
  transition: border-color 0.2s;
}
.send-btn {
  width: 120rpx;
  height: 72rpx;
  border-radius: $radius-full;
  background: $muted;
  border: none;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
}
.send-btn text {
  font-size: 28rpx;
  color: $muted-foreground;
  font-weight: 500;
  font-family: 'Inter', 'PingFang SC', sans-serif;
}
.send-btn-active {
  background: $gradient-accent;
  box-shadow: $shadow-accent;
}
.send-btn-active text {
  color: #fff;
}
.send-btn-active:active {
  opacity: 0.9;
  transform: translateY(1px);
}
.disclaimer-bar {
  position: fixed;
  left: 0; right: 0;
  z-index: 90;
  background: rgba(16, 185, 129, 0.06);
  color: $foreground;
  font-size: 22rpx;
  text-align: center;
  padding: 8rpx 48rpx 8rpx 16rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  border-bottom: 1rpx solid $border;
}
.disclaimer-bar .dismiss {
  position: absolute;
  right: 16rpx;
  font-size: 28rpx;
  color: $muted-foreground;
}

/* Connection Status Bar */
.conn-bar {
  position: fixed;
  left: 0; right: 0;
  z-index: 89;
  background: rgba(239, 68, 68, 0.06);
  color: #EF4444;
  font-size: 22rpx;
  padding: 6rpx 24rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16rpx;
  border-bottom: 1rpx solid $border;
}
.conn-bar.connecting {
  background: rgba(245, 158, 11, 0.06);
  color: #F59E0B;
}
.conn-bar.connected {
  background: rgba(16, 185, 129, 0.06);
  color: #10B981;
}
.conn-text {
  font-size: 22rpx;
}
.conn-retry {
  font-size: 22rpx;
  color: $accent;
  font-weight: 600;
  text-decoration: underline;
}
</style>
