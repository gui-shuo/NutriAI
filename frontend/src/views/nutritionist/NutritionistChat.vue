<template>
  <div class="n-chat-view">
    <header class="chat-header">
      <div class="header-left">
        <el-button text @click="$router.push('/nutritionist/consultations')">
          <el-icon><ArrowLeft /></el-icon>
          返回列表
        </el-button>
        <div class="header-info" v-if="order">
          <h3>咨询 #{{ order.orderNo?.slice(-8) }}</h3>
          <el-tag :type="order.status === 'IN_PROGRESS' ? 'success' : 'info'" size="small">
            {{ statusText(order.status) }}
          </el-tag>
        </div>
      </div>
    </header>

    <div class="chat-body" ref="chatBodyRef">
      <div v-if="loading" class="loading-area">
        <el-skeleton :rows="5" animated />
      </div>
      <template v-else-if="order">
        <div class="desc-msg" v-if="order.description">
          <div class="desc-label">用户咨询描述</div>
          <div class="desc-content">{{ order.description }}</div>
        </div>
        <div
          v-for="(msg, idx) in order.messages || []"
          :key="idx"
          class="message-item"
          :class="msg.role === 'nutritionist' ? 'msg-self' : 'msg-other'"
        >
          <div class="msg-bubble">
            <div class="msg-sender">{{ msg.role === 'nutritionist' ? '我' : '用户' }}</div>
            <div class="msg-content">{{ msg.content }}</div>
            <div class="msg-time">{{ formatMsgTime(msg.timestamp) }}</div>
          </div>
        </div>
      </template>
    </div>

    <div class="chat-footer" v-if="order?.status === 'IN_PROGRESS'">
      <el-input
        v-model="chatInput"
        type="textarea"
        :rows="3"
        placeholder="输入回复内容..."
        @keydown.enter.ctrl="sendReply"
        maxlength="1000"
        show-word-limit
        :disabled="sendLoading"
      />
      <div class="footer-actions">
        <span class="input-tip">Ctrl+Enter 发送</span>
        <el-button type="primary" :loading="sendLoading" :disabled="!chatInput.trim()" @click="sendReply">
          发送回复
        </el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, nextTick } from 'vue'
import { useRoute } from 'vue-router'
import { ArrowLeft } from '@element-plus/icons-vue'
import { getNutritionistConsultations, nutritionistReply, getConsultationImConfig } from '@/services/consultation'
import {
  initTim, loginTim, logoutTim, onMessageReceived, offMessageReceived,
  getOrderNoFromMessage, getTextFromMessage
} from '@/services/tim'
import message from '@/utils/message'

const route = useRoute()
const orderNo = route.params.orderNo

const loading = ref(true)
const order = ref(null)
const chatInput = ref('')
const sendLoading = ref(false)
const chatBodyRef = ref(null)
const imReady = ref(false)
let pollTimer = null

onMounted(async () => {
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
  loading.value = true
  try {
    const res = await getNutritionistConsultations(0, 100)
    if (res.data.code === 200) {
      const orders = res.data.data?.content || []
      order.value = orders.find(o => o.orderNo === orderNo) || null
      await nextTick()
      scrollToBottom()
    }
  } catch (e) {
    console.error('获取咨询失败', e)
  } finally {
    loading.value = false
  }
}

async function initImConnection() {
  try {
    const res = await getConsultationImConfig(orderNo)
    if (res.data.code !== 200 || !res.data.data) {
      console.warn('[IM] 获取IM配置失败，使用轮询降级')
      return
    }

    const { sdkAppId, userId, userSig, peerUserId } = res.data.data

    initTim(sdkAppId)
    await loginTim(userId, userSig)

    onMessageReceived(msg => {
      const msgOrderNo = getOrderNoFromMessage(msg)
      if (msgOrderNo === orderNo && msg.from === peerUserId) {
        const text = getTextFromMessage(msg)
        if (text && order.value) {
          if (!order.value.messages) order.value.messages = []
          order.value.messages.push({
            role: 'user',
            content: text,
            timestamp: new Date().toISOString()
          })
          nextTick(() => scrollToBottom())
        }
      }
    })

    imReady.value = true
    console.log('[IM] 营养师端实时消息连接就绪')
  } catch (e) {
    console.warn('[IM] 初始化失败，降级为轮询模式:', e.message || e)
    startPolling()
  }
}

function startPolling() {
  if (pollTimer) return
  pollTimer = setInterval(async () => {
    if (!order.value || order.value.status === 'COMPLETED') {
      stopPolling()
      return
    }
    try {
      const res = await getNutritionistConsultations(0, 100)
      if (res.data.code === 200) {
        const orders = res.data.data?.content || []
        const updated = orders.find(o => o.orderNo === orderNo)
        if (updated) {
          const oldLen = (order.value.messages || []).length
          const newLen = (updated.messages || []).length
          order.value = updated
          if (newLen > oldLen) {
            await nextTick()
            scrollToBottom()
          }
        }
      }
    } catch (e) { /* silent */ }
  }, 5000)
}

function stopPolling() {
  if (pollTimer) { clearInterval(pollTimer); pollTimer = null }
}

async function sendReply() {
  if (!chatInput.value.trim() || !order.value) return
  const content = chatInput.value.trim()
  chatInput.value = ''
  sendLoading.value = true
  try {
    const res = await nutritionistReply(orderNo, content)
    if (res.data.code === 200) {
      order.value = res.data.data
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

function scrollToBottom() {
  if (chatBodyRef.value) chatBodyRef.value.scrollTop = chatBodyRef.value.scrollHeight
}

function statusText(s) {
  return { IN_PROGRESS: '咨询中', WAITING: '等待中', COMPLETED: '已完成', CANCELLED: '已取消' }[s] || s
}

function formatMsgTime(ts) {
  if (!ts) return ''
  return new Date(ts).toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
}
</script>

<style scoped lang="scss">
.n-chat-view {
  height: calc(100vh - 48px);
  display: flex;
  flex-direction: column;
  background: #f5f7fa;
  margin: -24px;
}

.chat-header {
  background: #fff;
  padding: 0 20px;
  height: 56px;
  display: flex;
  align-items: center;
  box-shadow: 0 2px 8px rgba(0,0,0,0.06);
  flex-shrink: 0;

  .header-left {
    display: flex;
    align-items: center;
    gap: 12px;
  }

  .header-info {
    display: flex;
    align-items: center;
    gap: 8px;
    h3 { margin: 0; font-size: 16px; }
  }
}

.chat-body {
  flex: 1;
  overflow-y: auto;
  padding: 20px;

  .loading-area { padding: 20px; }

  .desc-msg {
    background: #fff;
    border-radius: 12px;
    padding: 14px 18px;
    margin-bottom: 20px;
    border: 1px solid #e5e7eb;
    .desc-label { font-size: 12px; color: #9ca3af; margin-bottom: 6px; }
    .desc-content { font-size: 14px; color: #374151; line-height: 1.6; }
  }

  .message-item {
    margin-bottom: 16px;
    display: flex;

    &.msg-self {
      justify-content: flex-end;
      .msg-bubble {
        background: #667eea;
        color: #fff;
        border-radius: 18px 18px 4px 18px;
        .msg-sender { color: rgba(255,255,255,0.8); }
        .msg-time { color: rgba(255,255,255,0.7); }
      }
    }

    &.msg-other {
      justify-content: flex-start;
      .msg-bubble {
        background: #fff;
        color: #1f2937;
        border-radius: 18px 18px 18px 4px;
        border: 1px solid #e5e7eb;
        .msg-sender { color: #667eea; }
        .msg-time { color: #9ca3af; }
      }
    }

    .msg-bubble {
      max-width: 70%;
      padding: 12px 18px;

      .msg-sender { font-size: 12px; font-weight: 600; margin-bottom: 4px; }
      .msg-content { font-size: 14px; line-height: 1.7; white-space: pre-wrap; word-break: break-word; }
      .msg-time { font-size: 11px; text-align: right; margin-top: 6px; }
    }
  }
}

.chat-footer {
  background: #fff;
  padding: 16px 20px;
  box-shadow: 0 -2px 8px rgba(0,0,0,0.06);
  flex-shrink: 0;

  .footer-actions {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-top: 10px;
    .input-tip { font-size: 12px; color: #9ca3af; }
  }
}
</style>
