<template>
  <view class="chat-page">
    <!-- Custom Navigation Bar -->
    <view class="nav-bar" :style="{ paddingTop: statusBarHeight + 'px' }">
      <view class="nav-bar-inner">
        <view class="nav-left">
          <view class="nav-back" @tap="goBack">
            <text class="icon-back">‹</text>
          </view>
          <text class="nav-title">AI 营养师</text>
          <view class="conn-dot" :class="connStatusClass" />
        </view>
        <view class="nav-right">
          <view class="nav-icon-btn" @tap="showHistoryPopup = true">
            <text>📂</text>
          </view>
          <view class="nav-icon-btn" @tap="showFavoritesPopup = true">
            <text>⭐</text>
          </view>
          <view class="nav-icon-btn" @tap="handleClearConversation">
            <text>🗑️</text>
          </view>
          <view class="nav-icon-btn" @tap="showSettingsPopup = true">
            <text>⚙️</text>
          </view>
        </view>
      </view>
    </view>

    <!-- Disclaimer -->
    <view class="disclaimer-bar" v-if="showDisclaimer" :style="{ top: navHeight + 'px' }">
      <text>⚕️ AI建议仅供参考，不构成医疗建议。如有身体不适请咨询专业医生。</text>
      <text class="dismiss" @tap="showDisclaimer = false">✕</text>
    </view>

    <!-- Connection Status -->
    <view class="conn-bar" v-if="!connected && !isConnecting" :style="{ top: connBarTop + 'px' }">
      <text class="conn-text">⚠️ 连接已断开</text>
      <text class="conn-retry" @tap="retryConnect">点击重连</text>
    </view>
    <view class="conn-bar connecting" v-else-if="isConnecting" :style="{ top: connBarTop + 'px' }">
      <text class="conn-text">🔄 正在连接...</text>
    </view>
    <view class="conn-bar conn-ok" v-else-if="showConnected" :style="{ top: connBarTop + 'px' }">
      <text class="conn-text">✅ 已连接</text>
    </view>

    <!-- Message List -->
    <scroll-view
      class="message-list"
      scroll-y
      :scroll-into-view="scrollIntoView"
      :style="{ top: messageListTop + 'px', bottom: bottomAreaHeight + 'px' }"
    >
      <!-- Welcome message when empty -->
      <view v-if="messages.length === 0" class="welcome-block">
        <text class="welcome-icon">🤖</text>
        <text class="welcome-title">你好！我是AI营养师</text>
        <text class="welcome-desc">我可以帮你分析食物营养、制定饮食计划、提供健康建议</text>
      </view>

      <view class="message-wrapper" v-for="(msg, idx) in messages" :key="msg.id || idx" :id="'msg-' + idx">
        <!-- AI Message -->
        <view v-if="msg.role === 'assistant'" class="message-row assistant">
          <view class="avatar avatar-ai">🤖</view>
          <view class="bubble-wrap">
            <view class="bubble bubble-ai">
              <!-- Streaming: show with cursor -->
              <view v-if="msg.streaming" class="md-content">
                <rich-text :nodes="renderMarkdown(msg.content)" />
                <text class="streaming-cursor">|</text>
              </view>
              <!-- Complete: render markdown -->
              <view v-else class="md-content">
                <rich-text :nodes="renderMarkdown(msg.content)" />
              </view>
            </view>
            <!-- Message actions -->
            <view v-if="!msg.streaming && msg.content" class="msg-actions">
              <text class="msg-time">{{ formatMsgTime(msg.timestamp) }}</text>
              <view class="msg-action-btns">
                <text class="action-btn" @tap="copyMessage(msg.content)">📋</text>
                <text
                  class="action-btn"
                  :class="{ 'favorited': msg.favorite }"
                  @tap="toggleFavorite(msg, idx)"
                >{{ msg.favorite ? '💛' : '🤍' }}</text>
              </view>
            </view>
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
      <view v-if="isTyping && !streamingIdx" class="message-row assistant" id="typing-indicator">
        <view class="avatar avatar-ai">🤖</view>
        <view class="bubble bubble-ai typing-bubble">
          <view class="typing-dots">
            <view class="dot" />
            <view class="dot" />
            <view class="dot" />
          </view>
          <text v-if="waitingSeconds >= 5" class="waiting-hint">
            {{ waitingHintText }}
          </text>
        </view>
      </view>

      <view class="scroll-bottom-anchor" :id="'msg-' + messages.length" />
    </scroll-view>

    <!-- Bottom Input Area -->
    <view class="input-area">
      <view class="input-row">
        <input
          class="chat-input"
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

    <!-- Settings Popup -->
    <view class="popup-mask" v-if="showSettingsPopup" @tap="showSettingsPopup = false">
      <view class="popup-panel" @tap.stop>
        <view class="popup-header">
          <text class="popup-title">⚙️ 聊天设置</text>
          <text class="popup-close" @tap="showSettingsPopup = false">✕</text>
        </view>
        <view class="popup-body">
          <view class="setting-item">
            <text class="setting-label">温度参数</text>
            <text class="setting-value">{{ settings.temperature.toFixed(1) }}</text>
          </view>
          <slider
            class="setting-slider"
            :value="settings.temperature * 10"
            :min="0"
            :max="10"
            :step="1"
            activeColor="#10B981"
            backgroundColor="#E2E8F0"
            @change="(e: any) => settings.temperature = e.detail.value / 10"
          />
          <text class="setting-tip">控制回答的随机性，值越大回答越发散</text>

          <view class="setting-item">
            <text class="setting-label">最大字数</text>
            <text class="setting-value">{{ settings.maxTokens }}</text>
          </view>
          <slider
            class="setting-slider"
            :value="settings.maxTokens"
            :min="500"
            :max="4000"
            :step="100"
            activeColor="#10B981"
            backgroundColor="#E2E8F0"
            @change="(e: any) => settings.maxTokens = e.detail.value"
          />
          <text class="setting-tip">单次回复的最大字数</text>

          <view class="setting-item">
            <text class="setting-label">上下文记忆</text>
            <switch
              :checked="settings.keepContext"
              color="#10B981"
              @change="(e: any) => settings.keepContext = e.detail.value"
            />
          </view>
          <text class="setting-tip">开启后AI会记住之前的对话内容</text>
        </view>
        <view class="popup-footer">
          <view class="popup-btn popup-btn-primary" @tap="saveSettings">
            <text>保存设置</text>
          </view>
        </view>
      </view>
    </view>

    <!-- History Popup -->
    <view class="popup-mask" v-if="showHistoryPopup" @tap="showHistoryPopup = false">
      <view class="popup-panel popup-panel-tall" @tap.stop>
        <view class="popup-header">
          <text class="popup-title">📂 历史记录</text>
          <text class="popup-close" @tap="showHistoryPopup = false">✕</text>
        </view>
        <view class="popup-body">
          <view class="popup-action-row">
            <view class="popup-btn popup-btn-sm popup-btn-primary" @tap="saveCurrentConversation">
              <text>💾 保存当前对话</text>
            </view>
          </view>
          <view v-if="historyList.length === 0" class="empty-hint">
            <text>暂无历史记录</text>
          </view>
          <scroll-view v-else scroll-y class="history-scroll">
            <view
              class="history-item"
              v-for="item in historyList"
              :key="item.id"
              @tap="loadHistoryConversation(item)"
            >
              <view class="history-top">
                <text class="history-title">{{ item.title }}</text>
                <text class="history-time">{{ formatMsgTime(item.timestamp) }}</text>
              </view>
              <text class="history-preview">{{ item.preview }}</text>
              <view class="history-del" @tap.stop="deleteHistory(item.id)">
                <text>删除</text>
              </view>
            </view>
          </scroll-view>
        </view>
      </view>
    </view>

    <!-- Favorites Popup -->
    <view class="popup-mask" v-if="showFavoritesPopup" @tap="showFavoritesPopup = false">
      <view class="popup-panel popup-panel-tall" @tap.stop>
        <view class="popup-header">
          <text class="popup-title">⭐ 我的收藏</text>
          <text class="popup-close" @tap="showFavoritesPopup = false">✕</text>
        </view>
        <view class="popup-body">
          <view v-if="favoritesList.length === 0" class="empty-hint">
            <text>暂无收藏</text>
          </view>
          <scroll-view v-else scroll-y class="history-scroll">
            <view class="favorite-item" v-for="fav in favoritesList" :key="fav.id">
              <view class="fav-content">
                <rich-text :nodes="renderMarkdown(fav.content)" />
              </view>
              <view class="fav-footer">
                <text class="fav-time">{{ formatMsgTime(fav.timestamp) }}</text>
                <view class="fav-actions">
                  <text class="action-btn" @tap="copyMessage(fav.content)">📋</text>
                  <text class="action-btn" @tap="removeFavorite(fav.id)">🗑️</text>
                </view>
              </view>
            </view>
          </scroll-view>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, reactive, nextTick, onUnmounted, computed } from 'vue'
import { onLoad, onShow, onHide } from '@dcloudio/uni-app'
import { checkLogin, defaultAvatar } from '@/utils/common'
import { getToken, request } from '@/utils/request'
import { useUserStore } from '@/stores/user'

// ─── Types ───
interface ChatMessage {
  id: string
  role: 'user' | 'assistant'
  content: string
  timestamp: number
  streaming?: boolean
  favorite?: boolean
}

interface HistoryRecord {
  id: string
  title: string
  preview: string
  timestamp: number
  messages: ChatMessage[]
}

interface FavoriteRecord {
  id: string
  content: string
  timestamp: number
}

// ─── WebSocket URL (conditional compilation) ───
// #ifdef H5
const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
const WS_URL = `${protocol}//${window.location.host}/api/ws/ai/chat`
// #endif
// #ifndef H5
const WS_URL = 'wss://nutriai.itshuo.me/api/ws/ai/chat'
// #endif

// ─── Stores & computed ───
const userStore = useUserStore()
const userAvatar = computed(() => defaultAvatar(userStore.userInfo?.avatar))

// ─── State ───
const messages = ref<ChatMessage[]>([])
const inputText = ref('')
const isTyping = ref(false)
const isSending = ref(false)
const scrollIntoView = ref('')
const statusBarHeight = ref(0)
const navHeight = ref(0)
const inputBaseHeight = ref(100)
const showDisclaimer = ref(true)
const showConnected = ref(false)
let connectedTimer: ReturnType<typeof setTimeout> | null = null

// Streaming tracking
const streamingIdx = ref<number | null>(null)

// Waiting timer
const waitingSeconds = ref(0)
let waitingTimer: ReturnType<typeof setInterval> | null = null

// WebSocket
let socketTask: UniApp.SocketTask | null = null
const connected = ref(false)
let reconnectTimer: ReturnType<typeof setTimeout> | null = null
let reconnectAttempts = 0
const MAX_RECONNECT = 5
const isConnecting = ref(false)
let pageVisible = true
let heartbeatTimer: ReturnType<typeof setInterval> | null = null
let pendingMessage = ''

// Popups
const showSettingsPopup = ref(false)
const showHistoryPopup = ref(false)
const showFavoritesPopup = ref(false)

// Settings (persisted in localStorage)
const settings = reactive({
  temperature: 0.7,
  maxTokens: 2000,
  keepContext: true
})

// History & Favorites (localStorage)
const historyList = ref<HistoryRecord[]>([])
const favoritesList = ref<FavoriteRecord[]>([])

// ─── Computed ───
const connStatusClass = computed(() => {
  if (connected.value) return 'dot-ok'
  if (isConnecting.value) return 'dot-connecting'
  return 'dot-off'
})

const connBarTop = computed(() => navHeight.value + (showDisclaimer.value ? 32 : 0))

const messageListTop = computed(() => {
  let top = navHeight.value
  if (showDisclaimer.value) top += 32
  if (!connected.value || showConnected.value || isConnecting.value) top += 32
  return top
})

const bottomAreaHeight = computed(() => {
  return inputBaseHeight.value
})

const waitingHintText = computed(() => {
  if (waitingSeconds.value >= 60) return '🔄 AI仍在处理中，请继续等待...'
  if (waitingSeconds.value >= 30) return '⏳ AI正在生成回复，请耐心等待...'
  if (waitingSeconds.value >= 15) return '🧠 AI正在深度思考，请耐心等待...'
  return '⏳ 网络响应中，请稍候...'
})

// ─── ID generation ───
function genId(): string {
  return `msg_${Date.now()}_${Math.random().toString(36).slice(2, 9)}`
}

// ─── Markdown Renderer (regex-based, returns HTML string) ───
function renderMarkdown(text: string): string {
  if (!text) return ''
  let html = text
    // Escape HTML entities
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')

  // Code blocks (```...```)
  html = html.replace(/```[\s\S]*?```/g, (match) => {
    const code = match.slice(3, -3).replace(/^\w*\n/, '')
    return `<div style="background:#F1F5F9;padding:12px;border-radius:8px;margin:8px 0;font-family:monospace;font-size:13px;overflow-x:auto;white-space:pre-wrap;border:1px solid #E2E8F0;">${code}</div>`
  })

  // Inline code
  html = html.replace(/`([^`]+)`/g, '<span style="background:#F1F5F9;padding:2px 6px;border-radius:4px;font-family:monospace;font-size:0.9em;border:1px solid #E2E8F0;">$1</span>')

  // Bold
  html = html.replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')

  // Italic
  html = html.replace(/\*(.+?)\*/g, '<em>$1</em>')

  // Headers (### > ## > #)
  html = html.replace(/^### (.+)$/gm, '<div style="font-size:15px;font-weight:700;margin:12px 0 6px;color:#0F172A;">$1</div>')
  html = html.replace(/^## (.+)$/gm, '<div style="font-size:16px;font-weight:700;margin:14px 0 6px;color:#0F172A;">$1</div>')
  html = html.replace(/^# (.+)$/gm, '<div style="font-size:17px;font-weight:700;margin:16px 0 8px;color:#0F172A;">$1</div>')

  // Blockquote
  html = html.replace(/^&gt; (.+)$/gm, '<div style="border-left:3px solid #10B981;padding-left:10px;margin:6px 0;color:#64748B;">$1</div>')

  // Unordered list items
  html = html.replace(/^[\-\*] (.+)$/gm, '<div style="padding-left:16px;margin:3px 0;">• $1</div>')

  // Ordered list items
  html = html.replace(/^(\d+)\. (.+)$/gm, '<div style="padding-left:16px;margin:3px 0;">$1. $2</div>')

  // Line breaks
  html = html.replace(/\n/g, '<br/>')

  return html
}

// ─── Time formatting ───
function formatMsgTime(ts: number): string {
  if (!ts) return ''
  const diff = Date.now() - ts
  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return `${Math.floor(diff / 60000)}分钟前`
  if (diff < 86400000) return `${Math.floor(diff / 3600000)}小时前`
  const d = new Date(ts)
  return `${(d.getMonth() + 1).toString().padStart(2, '0')}-${d.getDate().toString().padStart(2, '0')} ${d.getHours().toString().padStart(2, '0')}:${d.getMinutes().toString().padStart(2, '0')}`
}

// ─── Clipboard ───
function copyMessage(content: string) {
  uni.setClipboardData({
    data: content,
    success: () => uni.showToast({ title: '已复制', icon: 'success' }),
    fail: () => uni.showToast({ title: '复制失败', icon: 'none' })
  })
}

// ─── Waiting Timer ───
function startWaitingTimer() {
  stopWaitingTimer()
  waitingSeconds.value = 0
  waitingTimer = setInterval(() => { waitingSeconds.value++ }, 1000)
}

function stopWaitingTimer() {
  if (waitingTimer) { clearInterval(waitingTimer); waitingTimer = null }
  waitingSeconds.value = 0
}

// ─── Settings persistence ───
function loadSettings() {
  try {
    const saved = uni.getStorageSync('aiChatSettings')
    if (saved) {
      const parsed = typeof saved === 'string' ? JSON.parse(saved) : saved
      if (typeof parsed.temperature === 'number') settings.temperature = parsed.temperature
      if (typeof parsed.maxTokens === 'number') settings.maxTokens = parsed.maxTokens
      if (typeof parsed.keepContext === 'boolean') settings.keepContext = parsed.keepContext
    }
  } catch { /* ignore */ }
}

function saveSettings() {
  uni.setStorageSync('aiChatSettings', JSON.stringify(settings))
  showSettingsPopup.value = false
  uni.showToast({ title: '设置已保存', icon: 'success' })
}

// ─── History persistence (localStorage) ───
function loadHistoryList() {
  try {
    const saved = uni.getStorageSync('aiChatHistory')
    if (saved) {
      historyList.value = typeof saved === 'string' ? JSON.parse(saved) : saved
    }
  } catch { historyList.value = [] }
}

function persistHistory() {
  uni.setStorageSync('aiChatHistory', JSON.stringify(historyList.value))
}

function saveCurrentConversation() {
  if (messages.value.length === 0) {
    uni.showToast({ title: '当前没有对话', icon: 'none' })
    return
  }
  const firstUser = messages.value.find(m => m.role === 'user')
  const title = firstUser
    ? firstUser.content.substring(0, 20) + (firstUser.content.length > 20 ? '...' : '')
    : '新对话'
  const lastMsg = messages.value[messages.value.length - 1]
  const preview = lastMsg.content.substring(0, 60) + (lastMsg.content.length > 60 ? '...' : '')

  const record: HistoryRecord = {
    id: genId(),
    title,
    preview,
    timestamp: Date.now(),
    messages: JSON.parse(JSON.stringify(messages.value.map(m => ({ ...m, streaming: false }))))
  }
  historyList.value.unshift(record)
  // Keep max 20 records
  if (historyList.value.length > 20) historyList.value = historyList.value.slice(0, 20)
  persistHistory()
  uni.showToast({ title: '已保存', icon: 'success' })
}

function loadHistoryConversation(item: HistoryRecord) {
  messages.value = item.messages.map(m => ({ ...m, streaming: false }))
  showHistoryPopup.value = false
  scrollToBottom()
  uni.showToast({ title: '已加载', icon: 'success' })
}

function deleteHistory(id: string) {
  historyList.value = historyList.value.filter(h => h.id !== id)
  persistHistory()
  uni.showToast({ title: '已删除', icon: 'success' })
}

// ─── Favorites persistence (localStorage) ───
function loadFavorites() {
  try {
    const saved = uni.getStorageSync('aiChatFavorites')
    if (saved) {
      favoritesList.value = typeof saved === 'string' ? JSON.parse(saved) : saved
    }
  } catch { favoritesList.value = [] }
}

function persistFavorites() {
  uni.setStorageSync('aiChatFavorites', JSON.stringify(favoritesList.value))
}

function toggleFavorite(msg: ChatMessage, idx: number) {
  if (msg.favorite) {
    // Unfavorite
    favoritesList.value = favoritesList.value.filter(f => f.id !== msg.id)
    messages.value[idx] = { ...msg, favorite: false }
  } else {
    // Favorite
    favoritesList.value.unshift({ id: msg.id, content: msg.content, timestamp: msg.timestamp })
    messages.value[idx] = { ...msg, favorite: true }
  }
  persistFavorites()
}

function removeFavorite(id: string) {
  favoritesList.value = favoritesList.value.filter(f => f.id !== id)
  // Also update message list
  const msgIdx = messages.value.findIndex(m => m.id === id)
  if (msgIdx > -1) messages.value[msgIdx] = { ...messages.value[msgIdx], favorite: false }
  persistFavorites()
}

// ─── Clear Conversation ───
function handleClearConversation() {
  if (messages.value.length === 0) {
    uni.showToast({ title: '当前没有对话', icon: 'none' })
    return
  }
  uni.showModal({
    title: '开启新对话',
    content: '确定要清空当前对话吗？',
    confirmColor: '#10B981',
    success: (res) => {
      if (res.confirm) {
        messages.value = []
        streamingIdx.value = null
        isTyping.value = false
        isSending.value = false
        stopWaitingTimer()
        uni.showToast({ title: '已清空', icon: 'success' })
      }
    }
  })
}

// ─── WebSocket ───
function retryConnect() {
  reconnectAttempts = 0
  connectWebSocket()
}

function connectWebSocket() {
  const token = getToken()
  if (!token || isConnecting.value) return

  if (socketTask) {
    const oldTask = socketTask
    socketTask = null
    connected.value = false
    try { oldTask.close({}) } catch { /* */ }
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
    showConnected.value = true
    if (connectedTimer) clearTimeout(connectedTimer)
    connectedTimer = setTimeout(() => { showConnected.value = false }, 2000)
    if (pendingMessage) {
      const msg = pendingMessage
      pendingMessage = ''
      doSendWs(msg)
    }
  })

  socketTask.onMessage((res) => {
    if (currentTask !== socketTask) return
    try {
      const data = JSON.parse(res.data as string)
      handleWsMessage(data)
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
    stopHeartbeat()
    if (pageVisible && !isSending.value) {
      scheduleReconnect()
    }
  })
}

function handleWsMessage(data: { type: string; content?: string; message?: string }) {
  if (data.type === 'connection') return

  if (data.type === 'start') {
    // Create a new streaming AI message
    const aiMsg: ChatMessage = {
      id: genId(),
      role: 'assistant',
      content: '',
      timestamp: Date.now(),
      streaming: true
    }
    messages.value.push(aiMsg)
    streamingIdx.value = messages.value.length - 1
    isTyping.value = false
    scrollToBottom()
  } else if (data.type === 'token' || data.type === 'chunk') {
    stopWaitingTimer()
    const idx = streamingIdx.value
    if (idx !== null && messages.value[idx]) {
      messages.value[idx].content += data.content || ''
    } else {
      // No streaming message yet – append
      const aiMsg: ChatMessage = {
        id: genId(),
        role: 'assistant',
        content: data.content || '',
        timestamp: Date.now(),
        streaming: true
      }
      messages.value.push(aiMsg)
      streamingIdx.value = messages.value.length - 1
      isTyping.value = false
    }
    scrollToBottom()
  } else if (data.type === 'done' || data.type === 'complete') {
    stopWaitingTimer()
    const idx = streamingIdx.value
    if (idx !== null && messages.value[idx]) {
      messages.value[idx].streaming = false
      messages.value[idx].timestamp = Date.now()
      // Check if content in favorites
      const isFav = favoritesList.value.some(f => f.id === messages.value[idx].id)
      if (isFav) messages.value[idx].favorite = true
    }
    streamingIdx.value = null
    isTyping.value = false
    isSending.value = false
    scrollToBottom()
  } else if (data.type === 'error') {
    stopWaitingTimer()
    streamingIdx.value = null
    isTyping.value = false
    isSending.value = false
    uni.showToast({ title: data.message || 'AI响应出错', icon: 'none' })
  } else if (data.type === 'pong') {
    // Heartbeat response
  }
}

// ─── Send message ───
function sendMessage() {
  const text = inputText.value.trim()
  if (!text || isSending.value) return

  messages.value.push({
    id: genId(),
    role: 'user',
    content: text,
    timestamp: Date.now()
  })
  inputText.value = ''
  isSending.value = true
  isTyping.value = true
  streamingIdx.value = null
  startWaitingTimer()
  scrollToBottom()

  if (!connected.value) {
    // Try to connect, if fails use HTTP fallback
    pendingMessage = text
    connectWebSocket()
    // Set a timeout for HTTP fallback
    setTimeout(() => {
      if (pendingMessage === text && !connected.value) {
        pendingMessage = ''
        sendViaHttp(text)
      }
    }, 5000)
    return
  }

  doSendWs(text)
}

function doSendWs(text: string) {
  socketTask?.send({
    data: JSON.stringify({
      type: 'chat',
      message: text,
      keepContext: settings.keepContext
    }),
    success: () => {},
    fail: () => {
      // WebSocket send failed, try HTTP fallback
      sendViaHttp(text)
    }
  })
}

// ─── HTTP Fallback ───
async function sendViaHttp(text: string) {
  try {
    const res = await request<{ message?: string }>({
      url: '/ai/chat',
      method: 'POST',
      data: {
        message: text,
        temperature: settings.temperature,
        maxTokens: settings.maxTokens,
        keepContext: settings.keepContext
      },
      showLoading: false,
      showError: false
    })

    stopWaitingTimer()
    isTyping.value = false
    isSending.value = false

    const reply = (res as any)?.data?.message || (res as any)?.data || (res as any)?.message || ''
    if (reply) {
      messages.value.push({
        id: genId(),
        role: 'assistant',
        content: typeof reply === 'string' ? reply : JSON.stringify(reply),
        timestamp: Date.now()
      })
    } else {
      uni.showToast({ title: 'AI回复为空，请重试', icon: 'none' })
    }
    scrollToBottom()
  } catch (err: any) {
    stopWaitingTimer()
    isTyping.value = false
    isSending.value = false
    const msg = err?.message || err?.errMsg || '发送失败，请重试'
    uni.showToast({ title: msg, icon: 'none' })
  }
}

// ─── Scroll ───
function scrollToBottom() {
  nextTick(() => {
    scrollIntoView.value = ''
    setTimeout(() => {
      scrollIntoView.value = 'msg-' + messages.value.length
    }, 50)
  })
}

// ─── Reconnect ───
function scheduleReconnect() {
  clearReconnectTimer()
  if (reconnectAttempts >= MAX_RECONNECT) return
  const delay = Math.min(1000 * Math.pow(2, reconnectAttempts), 10000)
  reconnectAttempts++
  reconnectTimer = setTimeout(() => {
    if (!connected.value) connectWebSocket()
  }, delay)
}

function clearReconnectTimer() {
  if (reconnectTimer) { clearTimeout(reconnectTimer); reconnectTimer = null }
}

function closeWebSocket() {
  stopHeartbeat()
  const task = socketTask
  socketTask = null
  connected.value = false
  isConnecting.value = false
  if (task) { try { task.close({}) } catch { /* */ } }
}

function startHeartbeat() {
  stopHeartbeat()
  heartbeatTimer = setInterval(() => {
    if (connected.value && socketTask) {
      socketTask.send({ data: JSON.stringify({ type: 'ping' }), fail: () => {} })
    }
  }, 25000)
}

function stopHeartbeat() {
  if (heartbeatTimer) { clearInterval(heartbeatTimer); heartbeatTimer = null }
}

function goBack() {
  uni.navigateBack({ fail: () => uni.switchTab({ url: '/pages/index/index' }) })
}

// ─── Lifecycle ───
onLoad(() => {
  if (!checkLogin()) return
  const windowInfo = uni.getWindowInfo()
  // #ifdef APP-PLUS
  statusBarHeight.value = windowInfo.statusBarHeight || 44
  // #endif
  // #ifdef H5
  statusBarHeight.value = 0
  // #endif
  // #ifdef MP
  statusBarHeight.value = windowInfo.statusBarHeight || 20
  // #endif
  navHeight.value = statusBarHeight.value + 44
  const tabBarHeight = windowInfo.windowBottom || 50
  const safeBottom = windowInfo.safeArea ? (windowInfo.screenHeight - windowInfo.safeArea.bottom) : 0
  inputBaseHeight.value = 110 + tabBarHeight + safeBottom

  loadSettings()
  loadHistoryList()
  loadFavorites()

  connectWebSocket()
})

onShow(() => {
  pageVisible = true
  if (!connected.value && !isConnecting.value && getToken()) {
    reconnectAttempts = 0
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
  if (connectedTimer) clearTimeout(connectedTimer)
})
</script>

<style lang="scss" scoped>
.chat-page {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: $background;
}

/* ─── Navigation Bar ─── */
.nav-bar {
  position: fixed;
  top: 0; left: 0; right: 0;
  z-index: 100;
  background: $gradient-accent;
  box-shadow: $shadow-md;
}
.nav-bar-inner {
  height: 88rpx;
  padding: 0 16rpx;
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.nav-left {
  display: flex;
  align-items: center;
  gap: 4rpx;
}
.nav-right {
  display: flex;
  align-items: center;
  gap: 4rpx;
}
.nav-back {
  width: 64rpx;
  height: 64rpx;
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
  font-size: 32rpx;
  font-weight: 700;
  color: #fff;
  font-family: 'Calistoga', Georgia, 'PingFang SC', serif;
}
.nav-icon-btn {
  width: 56rpx;
  height: 56rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: $radius-full;
  &:active { opacity: 0.7; }
  text { font-size: 28rpx; }
}
.conn-dot {
  width: 14rpx;
  height: 14rpx;
  border-radius: 50%;
  margin-left: 10rpx;
  &.dot-ok { background: #fff; box-shadow: 0 0 6px rgba(255,255,255,0.8); }
  &.dot-connecting { background: #FCD34D; animation: pulse-dot 1.5s infinite; }
  &.dot-off { background: #FCA5A5; }
}
@keyframes pulse-dot {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.3; }
}

/* ─── Disclaimer ─── */
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

/* ─── Connection Status Bar ─── */
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
.conn-bar.conn-ok {
  background: rgba(16, 185, 129, 0.06);
  color: #10B981;
}
.conn-text { font-size: 22rpx; }
.conn-retry {
  font-size: 22rpx;
  color: $accent;
  font-weight: 600;
  text-decoration: underline;
}

/* ─── Welcome ─── */
.welcome-block {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 80rpx 40rpx;
  animation: fadeInUp 0.4s ease-out;
}
.welcome-icon { font-size: 80rpx; margin-bottom: 20rpx; }
.welcome-title {
  font-size: 36rpx;
  font-weight: 700;
  color: $foreground;
  margin-bottom: 12rpx;
  font-family: 'Calistoga', Georgia, 'PingFang SC', serif;
}
.welcome-desc {
  font-size: 26rpx;
  color: $muted-foreground;
  text-align: center;
  line-height: 1.6;
}
@keyframes fadeInUp {
  from { opacity: 0; transform: translateY(40rpx); }
  to { opacity: 1; transform: translateY(0); }
}

/* ─── Message List ─── */
.message-list {
  position: fixed;
  left: 0; right: 0;
  overflow-y: auto;
  padding: 20rpx 24rpx;
  background: $background;
}
.message-wrapper { margin-bottom: 24rpx; }

.message-row {
  display: flex;
  align-items: flex-start;
  padding: 0 8rpx;
}
.message-row.user { justify-content: flex-end; }
.message-row.assistant { justify-content: flex-start; }

.avatar {
  width: 68rpx;
  height: 68rpx;
  min-width: 68rpx;
  border-radius: $radius-full;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 34rpx;
  flex-shrink: 0;
  overflow: hidden;
  border: 1rpx solid $border;
}
.avatar-ai {
  background: $card;
  margin-right: 14rpx;
  box-shadow: $shadow-sm;
}
.avatar-user {
  margin-left: 14rpx;
  background: $muted;
}

.bubble-wrap {
  max-width: 72%;
  display: flex;
  flex-direction: column;
}

/* ─── Chat bubbles ─── */
.bubble {
  padding: 20rpx 28rpx;
  border: 1rpx solid $border;
  border-radius: $radius-xl;
  word-break: break-word;
}
.bubble-ai {
  background: $card;
  border-top-left-radius: $radius-sm;
  box-shadow: $shadow-sm;
}
.bubble-user {
  max-width: 72%;
  background: $accent;
  border-color: $accent;
  border-top-right-radius: $radius-sm;
  box-shadow: $shadow-accent;
}
.bubble-user .bubble-text { color: #fff; }
.bubble-text {
  font-size: 30rpx;
  line-height: 1.6;
  color: $foreground;
  font-family: 'Inter', 'PingFang SC', sans-serif;
}

/* ─── Markdown content ─── */
.md-content {
  font-size: 30rpx;
  line-height: 1.6;
  color: $foreground;
}

/* ─── Streaming cursor ─── */
.streaming-cursor {
  display: inline;
  color: $accent;
  font-weight: bold;
  animation: blink-cursor 1s step-end infinite;
}
@keyframes blink-cursor {
  0%, 100% { opacity: 1; }
  50% { opacity: 0; }
}

/* ─── Message actions ─── */
.msg-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 6rpx 4rpx 0;
  margin-top: 4rpx;
}
.msg-time {
  font-size: 20rpx;
  color: $muted-foreground;
}
.msg-action-btns {
  display: flex;
  gap: 12rpx;
}
.action-btn {
  font-size: 26rpx;
  &:active { opacity: 0.6; }
}
.favorited { transform: scale(1.1); }

/* ─── Typing Indicator ─── */
.typing-bubble { padding: 24rpx 32rpx; }
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
.scroll-bottom-anchor { height: 2rpx; }

/* ─── Input Area ─── */
.input-area {
  position: fixed;
  bottom: var(--window-bottom, 0);
  left: 0; right: 0;
  background: $card;
  padding: 12rpx 24rpx;
  padding-bottom: calc(12rpx + env(safe-area-inset-bottom));
  border-top: 1rpx solid $border;
  z-index: 100;
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
  text {
    font-size: 28rpx;
    color: $muted-foreground;
    font-weight: 500;
  }
}
.send-btn-active {
  background: $gradient-accent;
  box-shadow: $shadow-accent;
  text { color: #fff; }
  &:active { opacity: 0.9; transform: translateY(1px); }
}

/* ─── Popup (shared) ─── */
.popup-mask {
  position: fixed;
  top: 0; left: 0; right: 0; bottom: 0;
  z-index: 200;
  background: rgba(0, 0, 0, 0.45);
  display: flex;
  align-items: center;
  justify-content: center;
  animation: fadeIn 0.2s;
}
.popup-panel {
  width: 88%;
  max-width: 680rpx;
  max-height: 70vh;
  background: $card;
  border-radius: $radius-xl;
  box-shadow: $shadow-lg;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}
.popup-panel-tall { max-height: 80vh; }
.popup-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 24rpx 28rpx;
  border-bottom: 1rpx solid $border;
}
.popup-title {
  font-size: 30rpx;
  font-weight: 700;
  color: $foreground;
}
.popup-close {
  font-size: 32rpx;
  color: $muted-foreground;
  padding: 8rpx;
}
.popup-body {
  flex: 1;
  overflow-y: auto;
  padding: 20rpx 28rpx;
}
.popup-footer {
  padding: 16rpx 28rpx;
  border-top: 1rpx solid $border;
}
.popup-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 72rpx;
  border-radius: $radius-full;
  text { font-size: 28rpx; font-weight: 500; }
}
.popup-btn-primary {
  background: $gradient-accent;
  box-shadow: $shadow-accent;
  text { color: #fff; }
  &:active { opacity: 0.9; }
}
.popup-btn-sm {
  height: 60rpx;
  padding: 0 24rpx;
  text { font-size: 24rpx; }
}
.popup-action-row {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 16rpx;
}

/* ─── Settings ─── */
.setting-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 20rpx;
  margin-bottom: 8rpx;
}
.setting-label {
  font-size: 28rpx;
  font-weight: 600;
  color: $foreground;
}
.setting-value {
  font-size: 28rpx;
  color: $accent;
  font-weight: 600;
}
.setting-slider {
  margin: 8rpx 0;
}
.setting-tip {
  font-size: 22rpx;
  color: $muted-foreground;
  display: block;
  margin-bottom: 8rpx;
}

/* ─── History list ─── */
.history-scroll {
  max-height: 60vh;
}
.history-item {
  padding: 16rpx 0;
  border-bottom: 1rpx solid $border;
  position: relative;
  &:active { background: rgba(16, 185, 129, 0.04); }
}
.history-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 6rpx;
}
.history-title {
  font-size: 28rpx;
  font-weight: 600;
  color: $foreground;
  flex: 1;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}
.history-time {
  font-size: 22rpx;
  color: $muted-foreground;
  flex-shrink: 0;
  margin-left: 16rpx;
}
.history-preview {
  font-size: 24rpx;
  color: $muted-foreground;
  line-height: 1.4;
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
  overflow: hidden;
}
.history-del {
  position: absolute;
  right: 0;
  top: 16rpx;
  text {
    font-size: 22rpx;
    color: #EF4444;
    font-weight: 500;
  }
  &:active { opacity: 0.6; }
}

/* ─── Favorites list ─── */
.favorite-item {
  padding: 16rpx 0;
  border-bottom: 1rpx solid $border;
}
.fav-content {
  font-size: 28rpx;
  color: $foreground;
  line-height: 1.6;
  margin-bottom: 8rpx;
}
.fav-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.fav-time {
  font-size: 22rpx;
  color: $muted-foreground;
}
.fav-actions {
  display: flex;
  gap: 16rpx;
}

.empty-hint {
  display: flex;
  justify-content: center;
  padding: 60rpx 0;
  text { font-size: 26rpx; color: $muted-foreground; }
}
</style>
