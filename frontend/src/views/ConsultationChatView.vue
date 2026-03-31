<template>
  <div class="chat-view">
    <!-- Header -->
    <header class="chat-header">
      <div class="header-left">
        <el-button text @click="goBack">
          <el-icon><ArrowLeft /></el-icon>
          返回
        </el-button>
        <div class="header-info" v-if="order">
          <el-avatar :size="36" :src="nutritionistAvatar" class="n-avatar">
            {{ order.nutritionistName?.charAt(0) }}
          </el-avatar>
          <div>
            <h3>{{ order.nutritionistName }}</h3>
            <el-tag :type="order.status === 'IN_PROGRESS' ? 'success' : 'info'" size="small">
              {{ statusText(order.status) }}
            </el-tag>
          </div>
        </div>
      </div>
      <div class="header-right" v-if="order?.status === 'IN_PROGRESS'">
        <el-button type="danger" size="small" @click="handleComplete">结束咨询</el-button>
      </div>
    </header>

    <!-- Messages -->
    <div class="chat-body" ref="chatBodyRef">
      <div v-if="loading" class="loading-area">
        <el-skeleton :rows="5" animated />
      </div>
      <template v-else-if="order">
        <div class="system-msg">
          <el-tag type="info" size="small">咨询已开始，请描述您的营养需求</el-tag>
        </div>
        <div class="desc-msg" v-if="order.description">
          <div class="desc-label">咨询描述</div>
          <div class="desc-content">{{ order.description }}</div>
        </div>
        <div
          v-for="(msg, idx) in order.messages || []"
          :key="idx"
          class="message-item"
          :class="msg.role === 'user' ? 'msg-user' : 'msg-nutritionist'"
        >
          <div class="msg-bubble">
            <div class="msg-content">{{ msg.content }}</div>
            <div class="msg-time">{{ formatMsgTime(msg.timestamp) }}</div>
          </div>
        </div>
      </template>
      <el-empty v-else description="咨询不存在" />
    </div>

    <!-- Input -->
    <div class="chat-footer" v-if="order?.status === 'IN_PROGRESS'">
      <el-input
        v-model="chatInput"
        type="textarea"
        :rows="2"
        placeholder="请输入您的营养问题..."
        @keydown.enter.ctrl="sendMessage"
        maxlength="500"
        show-word-limit
        :disabled="sendLoading"
      />
      <div class="footer-actions">
        <span class="input-tip">Ctrl+Enter 发送</span>
        <el-button
          type="primary"
          :loading="sendLoading"
          :disabled="!chatInput.trim()"
          @click="sendMessage"
        >
          发送
        </el-button>
      </div>
    </div>
    <div class="chat-footer-closed" v-else-if="order?.status === 'COMPLETED'">
      <el-result icon="success" title="咨询已结束" sub-title="感谢您的使用">
        <template #extra>
          <el-button type="primary" @click="goBack">返回咨询列表</el-button>
        </template>
      </el-result>
    </div>

    <!-- Complete Dialog -->
    <el-dialog v-model="completeDialogVisible" title="结束咨询 & 评价" width="480px">
      <el-form label-position="top">
        <el-form-item label="服务评分">
          <el-rate
            v-model="completeForm.rating"
            :colors="['#99A9BF', '#F7BA2A', '#FF9900']"
            show-text
            :texts="['很差', '较差', '一般', '满意', '非常满意']"
          />
        </el-form-item>
        <el-form-item label="评价内容">
          <el-input
            v-model="completeForm.review"
            type="textarea"
            :rows="3"
            placeholder="请对本次咨询进行评价..."
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="completeDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="completeLoading" @click="submitComplete">提交评价</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft } from '@element-plus/icons-vue'
import {
  getConsultationDetail,
  sendConsultationMessage,
  simulateNutritionistReply,
  completeConsultation,
  getConsultationImConfig
} from '@/services/consultation'
import {
  initTim, loginTim, logoutTim, onMessageReceived, offMessageReceived,
  getOrderNoFromMessage, getTextFromMessage
} from '@/services/tim'
import message from '@/utils/message'

const route = useRoute()
const router = useRouter()
const orderNo = route.params.orderNo

const loading = ref(true)
const order = ref(null)
const nutritionistAvatar = ref('')
const chatInput = ref('')
const sendLoading = ref(false)
const chatBodyRef = ref(null)
const imReady = ref(false)
let pollTimer = null

const completeDialogVisible = ref(false)
const completeForm = ref({ rating: 5, review: '' })
const completeLoading = ref(false)

onMounted(async () => {
  await fetchOrder()
  await initImConnection()
  // 降级：IM连接失败时使用轮询
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
    const res = await getConsultationDetail(orderNo)
    if (res.data.code === 200) {
      order.value = res.data.data
      await nextTick()
      scrollToBottom()
    } else {
      message.error(res.data.message || '获取咨询详情失败')
    }
  } catch (e) {
    console.error('获取咨询详情失败', e)
    message.error('获取咨询详情失败')
  } finally {
    loading.value = false
  }
}

/**
 * 初始化IM实时消息连接
 */
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

    // 监听来自对方的实时消息
    onMessageReceived(msg => {
      const msgOrderNo = getOrderNoFromMessage(msg)
      // 只处理当前订单的消息
      if (msgOrderNo === orderNo && msg.from === peerUserId) {
        const text = getTextFromMessage(msg)
        if (text && order.value) {
          // 添加到消息列表
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
  } catch (e) {
    console.warn('[IM] 初始化失败，降级为轮询模式:', e.message || e)
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
      const res = await getConsultationDetail(orderNo)
      if (res.data.code === 200) {
        const newOrder = res.data.data
        const oldLen = (order.value.messages || []).length
        const newLen = (newOrder.messages || []).length
        order.value = newOrder
        if (newLen > oldLen) {
          await nextTick()
          scrollToBottom()
        }
      }
    } catch (e) {
      // silent
    }
  }, 5000)
}

function stopPolling() {
  if (pollTimer) {
    clearInterval(pollTimer)
    pollTimer = null
  }
}

async function sendMessage() {
  if (!chatInput.value.trim() || !order.value) return
  const content = chatInput.value.trim()
  chatInput.value = ''
  sendLoading.value = true

  try {
    const res = await sendConsultationMessage(order.value.orderNo, content)
    if (res.data.code === 200) {
      order.value = res.data.data
      await nextTick()
      scrollToBottom()

      // IM未就绪时才触发模拟回复（有IM时等待营养师真实回复）
      if (!imReady.value) {
        try {
          const replyRes = await simulateNutritionistReply(order.value.orderNo)
          if (replyRes.data.code === 200) {
            order.value = replyRes.data.data
            await nextTick()
            scrollToBottom()
          }
        } catch (e) {
          // Nutritionist may reply manually
        }
      }
    }
  } catch (e) {
    message.error('发送失败')
    chatInput.value = content
  } finally {
    sendLoading.value = false
  }
}

function handleComplete() {
  completeForm.value = { rating: 5, review: '' }
  completeDialogVisible.value = true
}

async function submitComplete() {
  if (!order.value) return
  completeLoading.value = true
  try {
    const res = await completeConsultation(
      order.value.orderNo,
      completeForm.value.rating,
      completeForm.value.review
    )
    if (res.data.code === 200) {
      message.success('咨询已结束，感谢您的评价！')
      order.value = res.data.data
      completeDialogVisible.value = false
      stopPolling()
    }
  } catch (e) {
    message.error('操作失败')
  } finally {
    completeLoading.value = false
  }
}

function goBack() {
  router.push('/consultation')
}

function scrollToBottom() {
  if (chatBodyRef.value) {
    chatBodyRef.value.scrollTop = chatBodyRef.value.scrollHeight
  }
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
.chat-view {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: #f5f7fa;
}

.chat-header {
  background: #fff;
  padding: 0 20px;
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  box-shadow: 0 2px 8px rgba(0,0,0,0.06);
  flex-shrink: 0;
  z-index: 10;

  .header-left {
    display: flex;
    align-items: center;
    gap: 12px;
  }

  .header-info {
    display: flex;
    align-items: center;
    gap: 10px;

    h3 {
      margin: 0;
      font-size: 16px;
      font-weight: 600;
    }
  }

  .n-avatar {
    background: linear-gradient(135deg, #667eea, #764ba2);
    color: #fff;
    font-weight: 600;
  }
}

.chat-body {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
  max-width: 900px;
  width: 100%;
  margin: 0 auto;

  .loading-area {
    padding: 20px;
  }

  .system-msg {
    text-align: center;
    margin-bottom: 20px;
  }

  .desc-msg {
    background: #fff;
    border-radius: 12px;
    padding: 14px 18px;
    margin-bottom: 20px;
    border: 1px solid #e5e7eb;

    .desc-label {
      font-size: 12px;
      color: #9ca3af;
      margin-bottom: 6px;
    }
    .desc-content {
      font-size: 14px;
      color: #374151;
      line-height: 1.6;
    }
  }

  .message-item {
    margin-bottom: 16px;
    display: flex;

    &.msg-user {
      justify-content: flex-end;
      .msg-bubble {
        background: #667eea;
        color: #fff;
        border-radius: 18px 18px 4px 18px;
        .msg-time { color: rgba(255,255,255,0.7); }
      }
    }

    &.msg-nutritionist {
      justify-content: flex-start;
      .msg-bubble {
        background: #fff;
        color: #1f2937;
        border-radius: 18px 18px 18px 4px;
        border: 1px solid #e5e7eb;
        .msg-time { color: #9ca3af; }
      }
    }

    .msg-bubble {
      max-width: 70%;
      padding: 12px 18px;

      .msg-content {
        font-size: 14px;
        line-height: 1.7;
        white-space: pre-wrap;
        word-break: break-word;
      }
      .msg-time {
        font-size: 11px;
        text-align: right;
        margin-top: 6px;
      }
    }
  }
}

.chat-footer {
  background: #fff;
  padding: 16px 20px;
  box-shadow: 0 -2px 8px rgba(0,0,0,0.06);
  flex-shrink: 0;
  max-width: 900px;
  width: 100%;
  margin: 0 auto;

  .footer-actions {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-top: 10px;

    .input-tip {
      font-size: 12px;
      color: #9ca3af;
    }
  }
}

.chat-footer-closed {
  background: #fff;
  padding: 20px;
  flex-shrink: 0;
}
</style>
