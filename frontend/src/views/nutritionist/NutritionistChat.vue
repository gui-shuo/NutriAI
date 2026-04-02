<template>
  <div class="wechat-chat">
    <!-- Left: Conversation List -->
    <aside class="conv-sidebar">
      <div class="conv-search">
        <el-input v-model="searchKey" placeholder="搜索咨询" prefix-icon="Search" size="small" clearable />
      </div>
      <div class="conv-list" v-loading="listLoading">
        <div v-if="!filteredOrders.length" class="no-conv">暂无咨询会话</div>
        <div
          v-for="order in filteredOrders"
          :key="order.orderNo"
          class="conv-item"
          :class="{ active: selectedOrderNo === order.orderNo }"
          @click="selectConversation(order)"
        >
          <el-avatar :size="40" class="conv-avatar">
            {{ order.description?.charAt(0) || '用' }}
          </el-avatar>
          <div class="conv-info">
            <div class="conv-top">
              <span class="conv-name">{{ order.description || '用户咨询' }}</span>
              <span class="conv-time">{{ formatShortTime(order.updatedAt || order.createdAt) }}</span>
            </div>
            <div class="conv-bottom">
              <span class="conv-preview">{{ getLastMessage(order) }}</span>
              <el-tag v-if="order.status === 'IN_PROGRESS'" type="success" size="small" class="conv-tag">进行中</el-tag>
              <el-tag v-else-if="order.status === 'WAITING'" type="warning" size="small" class="conv-tag">等待</el-tag>
              <el-tag v-else-if="order.status === 'COMPLETED'" type="info" size="small" class="conv-tag">已完成</el-tag>
            </div>
          </div>
        </div>
      </div>
    </aside>

    <!-- Right: Chat Area -->
    <section class="chat-main">
      <template v-if="selectedOrder">
        <!-- Chat Header -->
        <header class="chat-header">
          <div class="header-info">
            <h3>{{ selectedOrder.description || '用户咨询' }}</h3>
            <el-tag :type="selectedOrder.status === 'IN_PROGRESS' ? 'success' : 'info'" size="small">
              {{ statusText(selectedOrder.status) }}
            </el-tag>
          </div>
          <div class="header-actions">
            <el-button
              v-if="selectedOrder.status === 'IN_PROGRESS'"
              type="warning"
              size="small"
              plain
              @click="handleComplete"
              :loading="completeLoading"
            >
              结束咨询
            </el-button>
          </div>
        </header>

        <!-- Messages -->
        <div class="chat-body" ref="chatBodyRef">
          <!-- Order description -->
          <div class="system-msg" v-if="selectedOrder.description">
            <div class="system-bubble">
              <span class="system-label">咨询描述</span>
              {{ selectedOrder.description }}
            </div>
          </div>
          <div class="system-msg">
            <div class="system-bubble">
              订单 {{ selectedOrder.orderNo }} · {{ formatDate(selectedOrder.createdAt) }}
              · 咨询费 ¥{{ selectedOrder.amount }}
            </div>
          </div>

          <!-- Messages -->
          <div
            v-for="(msg, idx) in selectedOrder.messages || []"
            :key="idx"
            class="message-row"
            :class="msg.role === 'nutritionist' ? 'msg-self' : 'msg-other'"
          >
            <el-avatar :size="36" class="msg-avatar" :class="msg.role === 'nutritionist' ? 'avatar-self' : 'avatar-other'">
              {{ msg.role === 'nutritionist' ? '营' : '用' }}
            </el-avatar>
            <div class="msg-bubble">
              <div class="msg-text">{{ msg.content }}</div>
              <div class="msg-time">{{ formatMsgTime(msg.timestamp) }}</div>
            </div>
          </div>

          <!-- Completed notice -->
          <div v-if="selectedOrder.status === 'COMPLETED'" class="system-msg">
            <div class="system-bubble completed">
              ✅ 咨询已结束
              <template v-if="selectedOrder.userRating">
                · 用户评分: {{ '⭐'.repeat(selectedOrder.userRating) }}
              </template>
            </div>
          </div>
        </div>

        <!-- Input Area -->
        <div class="chat-input" v-if="selectedOrder.status === 'IN_PROGRESS'">
          <div class="input-toolbar">
            <span class="input-tip">Ctrl+Enter 发送</span>
          </div>
          <el-input
            v-model="chatInput"
            type="textarea"
            :rows="4"
            placeholder="输入回复内容..."
            @keydown="handleKeydown"
            maxlength="1000"
            show-word-limit
            :disabled="sendLoading"
            resize="none"
          />
          <div class="input-actions">
            <el-button
              type="primary"
              :loading="sendLoading"
              :disabled="!chatInput.trim()"
              @click="sendReply"
            >
              发送
            </el-button>
          </div>
        </div>
      </template>

      <!-- No selection -->
      <div v-else class="no-selection">
        <div class="no-selection-content">
          <div class="no-icon">💬</div>
          <h3>消息中心</h3>
          <p>从左侧选择一个咨询会话开始回复</p>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, nextTick, watch } from 'vue'
import { useRoute } from 'vue-router'
import {
  getNutritionistConsultations,
  nutritionistReply,
  getConsultationImConfig
} from '@/services/consultation'
import api from '@/services/api'
import {
  initTim, loginTim, logoutTim, onMessageReceived, offMessageReceived,
  getOrderNoFromMessage, getTextFromMessage
} from '@/services/tim'
import message from '@/utils/message'

const route = useRoute()
const props = defineProps({ profile: Object })

const listLoading = ref(true)
const sendLoading = ref(false)
const completeLoading = ref(false)
const allOrders = ref([])
const selectedOrderNo = ref(null)
const chatInput = ref('')
const searchKey = ref('')
const chatBodyRef = ref(null)
const imReady = ref(false)
let pollTimer = null
let imPeerMap = {}

const selectedOrder = computed(() =>
  allOrders.value.find(o => o.orderNo === selectedOrderNo.value) || null
)

const filteredOrders = computed(() => {
  if (!searchKey.value) return allOrders.value
  const key = searchKey.value.toLowerCase()
  return allOrders.value.filter(o =>
    (o.description || '').toLowerCase().includes(key) ||
    (o.orderNo || '').toLowerCase().includes(key)
  )
})

onMounted(async () => {
  await fetchAllOrders()
  // Auto-select from query param
  if (route.query.order) {
    selectedOrderNo.value = route.query.order
  } else if (allOrders.value.length > 0) {
    // Select first active order
    const active = allOrders.value.find(o => o.status === 'IN_PROGRESS' || o.status === 'WAITING')
    if (active) selectedOrderNo.value = active.orderNo
  }
  await initImConnection()
  startPolling()
})

onUnmounted(() => {
  stopPolling()
  offMessageReceived()
  logoutTim().catch(() => {})
})

watch(selectedOrderNo, async () => {
  await nextTick()
  scrollToBottom()
})

async function fetchAllOrders() {
  listLoading.value = true
  try {
    const res = await getNutritionistConsultations(0, 100)
    if (res.data.code === 200) {
      const orders = res.data.data?.content || []
      // Sort: active first, then by date
      orders.sort((a, b) => {
        const statusOrder = { IN_PROGRESS: 0, WAITING: 1, COMPLETED: 2, CANCELLED: 3 }
        const sa = statusOrder[a.status] ?? 9
        const sb = statusOrder[b.status] ?? 9
        if (sa !== sb) return sa - sb
        return new Date(b.createdAt) - new Date(a.createdAt)
      })
      allOrders.value = orders
    }
  } catch (e) {
    console.error('获取咨询列表失败', e)
  } finally {
    listLoading.value = false
  }
}

async function initImConnection() {
  if (!allOrders.value.length) return
  // Use the first active order to get IM config
  const activeOrder = allOrders.value.find(o => o.status === 'IN_PROGRESS')
  if (!activeOrder) return

  try {
    const res = await getConsultationImConfig(activeOrder.orderNo)
    if (res.data.code !== 200 || !res.data.data) return

    const { sdkAppId, userId, userSig, peerUserId } = res.data.data
    imPeerMap[activeOrder.orderNo] = peerUserId

    initTim(sdkAppId)
    await loginTim(userId, userSig)

    onMessageReceived(msg => {
      const msgOrderNo = getOrderNoFromMessage(msg)
      if (msgOrderNo) {
        const text = getTextFromMessage(msg)
        if (text) {
          const order = allOrders.value.find(o => o.orderNo === msgOrderNo)
          if (order) {
            if (!order.messages) order.messages = []
            order.messages.push({
              role: 'user',
              content: text,
              timestamp: new Date().toISOString()
            })
            if (selectedOrderNo.value === msgOrderNo) {
              nextTick(() => scrollToBottom())
            }
          }
        }
      }
    })

    imReady.value = true
    console.log('[IM] 营养师消息中心连接就绪')
  } catch (e) {
    console.warn('[IM] 初始化失败，使用轮询:', e.message || e)
  }
}

function selectConversation(order) {
  selectedOrderNo.value = order.orderNo
  nextTick(() => scrollToBottom())
}

async function sendReply() {
  if (!chatInput.value.trim() || !selectedOrder.value) return
  const content = chatInput.value.trim()
  chatInput.value = ''
  sendLoading.value = true
  try {
    const res = await nutritionistReply(selectedOrderNo.value, content)
    if (res.data.code === 200) {
      // Update the order in our list
      const idx = allOrders.value.findIndex(o => o.orderNo === selectedOrderNo.value)
      if (idx >= 0) allOrders.value[idx] = res.data.data
      await nextTick()
      scrollToBottom()
    } else {
      message.error(res.data.message || '发送失败')
      chatInput.value = content
    }
  } catch (e) {
    message.error('发送失败')
    chatInput.value = content
  } finally {
    sendLoading.value = false
  }
}

async function handleComplete() {
  if (!selectedOrder.value) return
  completeLoading.value = true
  try {
    const res = await api.post(`/nutritionist/consultations/${selectedOrderNo.value}/complete`, {})
    if (res.data.code === 200) {
      const idx = allOrders.value.findIndex(o => o.orderNo === selectedOrderNo.value)
      if (idx >= 0) allOrders.value[idx] = res.data.data
      message.success('咨询已结束')
    } else {
      message.error(res.data.message || '操作失败')
    }
  } catch (e) {
    message.error('操作失败')
  } finally {
    completeLoading.value = false
  }
}

function handleKeydown(e) {
  if (e.ctrlKey && e.key === 'Enter') {
    e.preventDefault()
    sendReply()
  }
}

function startPolling() {
  if (pollTimer) return
  pollTimer = setInterval(async () => {
    try {
      const res = await getNutritionistConsultations(0, 100)
      if (res.data.code === 200) {
        const orders = res.data.data?.content || []
        orders.sort((a, b) => {
          const statusOrder = { IN_PROGRESS: 0, WAITING: 1, COMPLETED: 2, CANCELLED: 3 }
          const sa = statusOrder[a.status] ?? 9
          const sb = statusOrder[b.status] ?? 9
          if (sa !== sb) return sa - sb
          return new Date(b.createdAt) - new Date(a.createdAt)
        })
        // Update orders preserving selection
        const currentMsgLen = (selectedOrder.value?.messages || []).length
        allOrders.value = orders
        const updated = allOrders.value.find(o => o.orderNo === selectedOrderNo.value)
        if (updated && (updated.messages || []).length > currentMsgLen) {
          await nextTick()
          scrollToBottom()
        }
      }
    } catch { /* silent */ }
  }, 8000)
}

function stopPolling() {
  if (pollTimer) { clearInterval(pollTimer); pollTimer = null }
}

function scrollToBottom() {
  if (chatBodyRef.value) chatBodyRef.value.scrollTop = chatBodyRef.value.scrollHeight
}

function getLastMessage(order) {
  const msgs = order.messages || []
  if (!msgs.length) return order.description || '暂无消息'
  const last = msgs[msgs.length - 1]
  const prefix = last.role === 'nutritionist' ? '[我] ' : ''
  const text = last.content || ''
  return prefix + (text.length > 30 ? text.slice(0, 30) + '...' : text)
}

function statusText(s) {
  return { IN_PROGRESS: '咨询中', WAITING: '等待中', COMPLETED: '已完成', CANCELLED: '已取消' }[s] || s
}

function formatDate(dt) {
  if (!dt) return ''
  return new Date(dt).toLocaleString('zh-CN', { year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' })
}

function formatShortTime(dt) {
  if (!dt) return ''
  const d = new Date(dt)
  const now = new Date()
  const isToday = d.toDateString() === now.toDateString()
  if (isToday) return d.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
  const yesterday = new Date(now); yesterday.setDate(yesterday.getDate() - 1)
  if (d.toDateString() === yesterday.toDateString()) return '昨天'
  return d.toLocaleDateString('zh-CN', { month: '2-digit', day: '2-digit' })
}

function formatMsgTime(ts) {
  if (!ts) return ''
  return new Date(ts).toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
}
</script>

<style scoped lang="scss">
.wechat-chat {
  display: flex;
  height: calc(100vh - 48px);
  margin: -24px;
  background: #FAFAFA;
  font-family: 'Inter', sans-serif;
}

// Left sidebar
.conv-sidebar {
  width: 320px;
  background: #FFFFFF;
  border-right: 1px solid #E2E8F0;
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
}

.conv-search {
  padding: 12px;
  border-bottom: 1px solid #E2E8F0;
}

.conv-list {
  flex: 1;
  overflow-y: auto;
}

.no-conv {
  text-align: center;
  color: #0F172A;
  padding: 40px 20px;
  font-size: 14px;
  font-family: 'Inter', sans-serif;
  opacity: 0.5;
}

.conv-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 16px;
  cursor: pointer;
  border-bottom: 1px solid #E2E8F0;
  transition: background 0.2s;

  &:hover { background: #F1F5F9; }
  &.active { background: #F1F5F9; border-left: 3px solid #10B981; }

  .conv-avatar {
    background: linear-gradient(135deg, #10B981, #34D399);
    color: #FFFFFF;
    font-weight: 600;
    flex-shrink: 0;
    border: none;
  }

  .conv-info {
    flex: 1;
    min-width: 0;
  }

  .conv-top {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }

  .conv-name {
    font-size: 14px;
    font-weight: 500;
    color: #0F172A;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    max-width: 160px;
    font-family: 'Inter', sans-serif;
  }

  .conv-time {
    font-size: 11px;
    color: #0F172A;
    opacity: 0.4;
    flex-shrink: 0;
    font-family: 'Inter', sans-serif;
  }

  .conv-bottom {
    display: flex;
    align-items: center;
    gap: 6px;
    margin-top: 4px;
  }

  .conv-preview {
    font-size: 12px;
    color: #0F172A;
    opacity: 0.5;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    flex: 1;
    font-family: 'Inter', sans-serif;
  }

  .conv-tag { flex-shrink: 0; }
}

// Right chat area
.chat-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
  background: #FAFAFA;
}

.chat-header {
  padding: 14px 20px;
  background: #FFFFFF;
  border-bottom: 1px solid #E2E8F0;
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-shrink: 0;

  .header-info {
    display: flex;
    align-items: center;
    gap: 10px;
    h3 { margin: 0; font-size: 16px; color: #0F172A; font-family: 'Calistoga', cursive; }
  }
}

.chat-body {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
  background: #FAFAFA;

  .system-msg {
    text-align: center;
    margin: 16px 0;
  }

  .system-bubble {
    display: inline-block;
    background: #F1F5F9;
    color: #0F172A;
    padding: 6px 16px;
    border-radius: 12px;
    font-size: 12px;
    border: 1px solid #E2E8F0;
    font-family: 'Inter', sans-serif;

    .system-label {
      font-weight: 600;
      color: #10B981;
      margin-right: 6px;
      font-family: 'Inter', sans-serif;
    }

    &.completed {
      background: #F1F5F9;
      color: #0F172A;
      border-color: #E2E8F0;
    }
  }

  .message-row {
    display: flex;
    align-items: flex-start;
    gap: 10px;
    margin-bottom: 16px;

    &.msg-self {
      flex-direction: row-reverse;
      .msg-bubble {
        background: linear-gradient(135deg, #10B981, #34D399);
        color: #FFFFFF;
        border: none;
        border-radius: 16px 16px 4px 16px;
        box-shadow: 0 2px 8px rgba(16, 185, 129, 0.2);
        .msg-time { color: rgba(255, 255, 255, 0.7); }
      }
    }

    &.msg-other {
      .msg-bubble {
        background: #FFFFFF;
        color: #0F172A;
        border: 1px solid #E2E8F0;
        border-radius: 16px 16px 16px 4px;
        box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
        .msg-time { color: #0F172A; opacity: 0.4; }
      }
    }
  }

  .msg-avatar {
    flex-shrink: 0;
    font-weight: 600;
    font-size: 13px;
    border: none;
    &.avatar-self { background: linear-gradient(135deg, #10B981, #34D399); color: #FFFFFF; }
    &.avatar-other { background: #F1F5F9; color: #0F172A; }
  }

  .msg-bubble {
    max-width: 60%;
    padding: 12px 16px;
    .msg-text { font-size: 14px; line-height: 1.7; white-space: pre-wrap; word-break: break-word; font-family: 'Inter', sans-serif; }
    .msg-time { font-size: 11px; text-align: right; margin-top: 6px; font-family: 'Inter', sans-serif; }
  }
}

.chat-input {
  background: #FFFFFF;
  border-top: 1px solid #E2E8F0;
  padding: 12px 16px;
  flex-shrink: 0;

  .input-toolbar {
    display: flex;
    justify-content: flex-end;
    margin-bottom: 6px;
    .input-tip { font-size: 11px; color: #0F172A; opacity: 0.4; font-family: 'Inter', sans-serif; }
  }

  .input-actions {
    display: flex;
    justify-content: flex-end;
    margin-top: 8px;
  }

  :deep(.el-textarea__inner) {
    border-radius: 12px;
    border: 1px solid #E2E8F0;
    background: #FAFAFA;
    font-family: 'Inter', sans-serif;
    &:focus { box-shadow: 0 0 0 2px rgba(16, 185, 129, 0.15); border-color: #10B981; }
  }
}

.no-selection {
  flex: 1;
  display: flex;
  justify-content: center;
  align-items: center;
  background: #FAFAFA;

  .no-selection-content {
    text-align: center;
    color: #0F172A;
    .no-icon { font-size: 64px; margin-bottom: 16px; }
    h3 { color: #0F172A; margin: 0 0 8px; font-family: 'Calistoga', cursive; }
    p { margin: 0; font-size: 14px; font-family: 'Inter', sans-serif; opacity: 0.5; }
  }
}
</style>
