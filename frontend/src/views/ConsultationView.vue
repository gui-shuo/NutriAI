<template>
  <div class="consultation-view">
    <!-- 顶部导航 -->
    <nav class="top-nav">
      <div class="nav-inner">
        <div class="nav-left">
          <el-button text @click="$router.push('/')">
            <el-icon><ArrowLeft /></el-icon>
            返回首页
          </el-button>
          <h2 class="page-title">🩺 营养师在线咨询</h2>
        </div>
        <el-button type="primary" text @click="showMyOrders">
          <el-icon><Document /></el-icon>
          我的咨询
        </el-button>
      </div>
    </nav>

    <main class="main-area">
      <el-alert type="info" :closable="true" show-icon style="margin-bottom: 16px">
        <template #title>
          本平台咨询服务仅供健康参考，不构成医疗诊断或治疗方案。如有疾病请及时就医。
          <router-link to="/legal/disclaimer" style="color:#409eff">详细声明</router-link>
        </template>
      </el-alert>
      <!-- 当前进行中的咨询 -->
      <section v-if="activeConsultation" class="active-consultation-section">
        <el-card class="chat-room-card">
          <template #header>
            <div class="chat-header">
              <div class="chat-info">
                <el-avatar :size="40" :src="activeNutritionist?.avatar" class="n-avatar">
                  {{ activeConsultation.nutritionistName?.charAt(0) }}
                </el-avatar>
                <div>
                  <h3>{{ activeConsultation.nutritionistName }}</h3>
                  <el-tag type="success" size="small">咨询中</el-tag>
                </div>
              </div>
              <div class="chat-actions">
                <el-button type="danger" size="small" @click="handleComplete">结束咨询</el-button>
              </div>
            </div>
          </template>

          <!-- 聊天消息区 -->
          <div class="chat-messages" ref="chatMessagesRef">
            <div class="system-msg">
              <el-tag type="info" size="small">咨询已开始，请描述您的营养需求</el-tag>
            </div>
            <div
              v-for="(msg, idx) in activeConsultation.messages || []"
              :key="idx"
              class="message-item"
              :class="msg.role === 'user' ? 'msg-user' : 'msg-nutritionist'"
            >
              <div class="msg-bubble">
                <div class="msg-content">{{ msg.content }}</div>
                <div class="msg-time">{{ formatMsgTime(msg.timestamp) }}</div>
              </div>
            </div>
            <div v-if="replyLoading" class="message-item msg-nutritionist">
              <div class="msg-bubble">
                <div class="msg-content typing-indicator">
                  <span></span><span></span><span></span>
                </div>
              </div>
            </div>
          </div>

          <!-- 输入区 -->
          <div class="chat-input-area">
            <el-input
              v-model="chatInput"
              type="textarea"
              :rows="2"
              placeholder="请输入您的营养问题..."
              @keydown.enter.ctrl="sendChatMessage"
              maxlength="500"
              show-word-limit
            />
            <div class="input-actions">
              <span class="input-tip">Ctrl+Enter 发送</span>
              <el-button
                type="primary"
                :loading="sendLoading"
                :disabled="!chatInput.trim()"
                @click="sendChatMessage"
              >
                发送
              </el-button>
            </div>
          </div>
        </el-card>
      </section>

      <!-- 营养师列表 -->
      <section v-else class="nutritionist-section">
        <div class="section-header">
          <h2>专业营养师团队</h2>
          <p class="section-desc">选择一位营养师开始在线咨询，获取个性化的营养指导方案</p>
        </div>

        <el-skeleton :loading="loading" animated :rows="6">
          <el-empty v-if="!nutritionists.length" description="暂无可用营养师" />
          <div v-else class="nutritionist-grid">
            <el-card
              v-for="n in nutritionists"
              :key="n.id"
              class="nutritionist-card"
              shadow="hover"
            >
              <div class="n-header">
                <el-avatar :size="64" :src="n.avatar" class="n-avatar">
                  {{ n.name?.charAt(0) }}
                </el-avatar>
                <el-tag
                  :type="
                    n.status === 'ONLINE' ? 'success' : n.status === 'BUSY' ? 'warning' : 'info'
                  "
                  size="small"
                  class="status-tag"
                >
                  {{ statusText(n.status) }}
                </el-tag>
              </div>
              <h3 class="n-name">{{ n.name }}</h3>
              <p class="n-title">{{ n.title }}</p>
              <div class="n-specialties">
                <el-tag
                  v-for="(s, i) in (n.specialties || []).slice(0, 3)"
                  :key="i"
                  size="small"
                  type="info"
                  effect="plain"
                >
                  {{ s }}
                </el-tag>
              </div>
              <p class="n-intro">{{ n.introduction }}</p>
              <div class="n-stats">
                <div class="stat">
                  <span class="stat-value">{{ n.experienceYears }}年</span>
                  <span class="stat-label">从业经验</span>
                </div>
                <div class="stat">
                  <span class="stat-value">
                    <el-icon color="#e6a23c"><Star /></el-icon>
                    {{ n.rating }}
                  </span>
                  <span class="stat-label">用户评分</span>
                </div>
                <div class="stat">
                  <span class="stat-value">{{ n.consultationCount }}</span>
                  <span class="stat-label">咨询次数</span>
                </div>
              </div>
              <div class="n-footer">
                <div class="n-fee">
                  <span class="fee-label">咨询费</span>
                  <span class="fee-value">¥{{ n.consultationFee }}/次</span>
                </div>
                <el-button
                  type="primary"
                  :disabled="n.status === 'OFFLINE'"
                  @click="openConsultDialog(n)"
                >
                  立即咨询
                </el-button>
              </div>
            </el-card>
          </div>
        </el-skeleton>
      </section>
    </main>

    <!-- 创建咨询 Dialog -->
    <el-dialog v-model="consultDialogVisible" title="预约咨询" width="520px">
      <div v-if="selectedNutritionist" class="consult-form">
        <div class="consult-info">
          <el-avatar :size="48" :src="selectedNutritionist.avatar" class="n-avatar">
            {{ selectedNutritionist.name?.charAt(0) }}
          </el-avatar>
          <div>
            <h4>{{ selectedNutritionist.name }}</h4>
            <p>{{ selectedNutritionist.title }}</p>
          </div>
          <span class="consult-fee">¥{{ selectedNutritionist.consultationFee }}</span>
        </div>
        <el-form label-position="top">
          <el-form-item label="咨询描述">
            <el-input
              v-model="consultForm.description"
              type="textarea"
              :rows="4"
              placeholder="请简要描述您的营养需求或健康问题..."
              maxlength="500"
              show-word-limit
            />
          </el-form-item>
        </el-form>
        <el-alert type="info" :closable="false" show-icon style="margin-top: 8px">
          <template #title>模拟支付模式：点击确认后将自动完成支付</template>
        </el-alert>
      </div>
      <template #footer>
        <el-button @click="consultDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="createLoading" @click="handleCreateConsultation">
          确认预约并支付（模拟）
        </el-button>
      </template>
    </el-dialog>

    <!-- 完成咨询 Dialog -->
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
        <el-button type="primary" :loading="completeLoading" @click="submitComplete"
          >提交评价</el-button
        >
      </template>
    </el-dialog>

    <!-- 我的咨询订单 Dialog -->
    <el-dialog v-model="ordersDialogVisible" title="我的咨询记录" width="780px">
      <el-skeleton :loading="ordersLoading" animated :rows="4">
        <el-empty v-if="!orders.length" description="暂无咨询记录" />
        <el-table v-else :data="orders" style="width: 100%">
          <el-table-column label="订单号" prop="orderNo" min-width="150" />
          <el-table-column label="营养师" prop="nutritionistName" width="100" />
          <el-table-column label="金额" width="80" align="center">
            <template #default="{ row }">¥{{ row.amount }}</template>
          </el-table-column>
          <el-table-column label="支付状态" width="90" align="center">
            <template #default="{ row }">
              <el-tag :type="payStatusType(row.paymentStatus)" size="small">
                {{ payStatusText(row.paymentStatus) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="咨询状态" width="90" align="center">
            <template #default="{ row }">
              <el-tag :type="consultStatusType(row.status)" size="small">
                {{ consultStatusText(row.status) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="时间" min-width="140">
            <template #default="{ row }">{{ formatDate(row.createdAt) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="100" align="center">
            <template #default="{ row }">
              <el-button
                v-if="row.paymentStatus === 'PAID' && row.status !== 'COMPLETED'"
                type="danger"
                size="small"
                text
                @click="handleRefund(row)"
              >
                退款
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-skeleton>
      <template #footer>
        <el-button @click="ordersDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick } from 'vue'
import { ArrowLeft, Document, Star } from '@element-plus/icons-vue'
import {
  getNutritionists,
  createConsultation,
  simulatePayConsultation,
  sendConsultationMessage,
  simulateNutritionistReply,
  completeConsultation,
  simulateRefundConsultation,
  getConsultationHistory,
  getActiveConsultations
} from '@/services/consultation'
import message from '@/utils/message'

// --- 状态 ---
const loading = ref(false)
const nutritionists = ref([])
const activeConsultation = ref(null)
const activeNutritionist = ref(null)

const consultDialogVisible = ref(false)
const selectedNutritionist = ref(null)
const consultForm = ref({ description: '' })
const createLoading = ref(false)

const chatInput = ref('')
const sendLoading = ref(false)
const replyLoading = ref(false)
const chatMessagesRef = ref(null)

const completeDialogVisible = ref(false)
const completeForm = ref({ rating: 5, review: '' })
const completeLoading = ref(false)

const ordersDialogVisible = ref(false)
const ordersLoading = ref(false)
const orders = ref([])

// --- 初始化 ---
onMounted(async () => {
  await Promise.all([fetchNutritionists(), fetchActiveConsultation()])
})

async function fetchNutritionists() {
  loading.value = true
  try {
    const res = await getNutritionists()
    if (res.data.code === 200) {
      nutritionists.value = res.data.data || []
    }
  } catch (e) {
    console.error('获取营养师列表失败', e)
  } finally {
    loading.value = false
  }
}

async function fetchActiveConsultation() {
  try {
    const res = await getActiveConsultations()
    if (res.data.code === 200) {
      const actives = res.data.data || []
      if (actives.length > 0) {
        activeConsultation.value = actives[0]
        activeNutritionist.value = nutritionists.value.find(n => n.id === actives[0].nutritionistId)
        await nextTick()
        scrollToBottom()
      }
    }
  } catch (e) {
    console.error('获取活跃咨询失败', e)
  }
}

function openConsultDialog(n) {
  selectedNutritionist.value = n
  consultForm.value = { description: '' }
  consultDialogVisible.value = true
}

async function handleCreateConsultation() {
  if (!selectedNutritionist.value) return
  createLoading.value = true
  try {
    // 1. 创建订单
    const res = await createConsultation(
      selectedNutritionist.value.id,
      consultForm.value.description
    )
    if (res.data.code !== 200) {
      message.error(res.data.message || '创建失败')
      return
    }
    const order = res.data.data

    // 2. 模拟支付
    const payRes = await simulatePayConsultation(order.orderNo)
    if (payRes.data.code === 200) {
      message.success('🎉 预约成功！咨询已开始')
      activeConsultation.value = payRes.data.data
      activeNutritionist.value = selectedNutritionist.value
      consultDialogVisible.value = false
    } else {
      message.error(payRes.data.message || '支付失败')
    }
  } catch (e) {
    console.error('创建咨询失败', e)
    message.error('操作失败，请重试')
  } finally {
    createLoading.value = false
  }
}

async function sendChatMessage() {
  if (!chatInput.value.trim() || !activeConsultation.value) return
  const content = chatInput.value.trim()
  chatInput.value = ''
  sendLoading.value = true

  try {
    const res = await sendConsultationMessage(activeConsultation.value.orderNo, content)
    if (res.data.code === 200) {
      activeConsultation.value = res.data.data
      await nextTick()
      scrollToBottom()

      // 模拟营养师回复（延迟1-2秒）
      replyLoading.value = true
      await nextTick()
      scrollToBottom()

      setTimeout(async () => {
        try {
          const replyRes = await simulateNutritionistReply(activeConsultation.value.orderNo)
          if (replyRes.data.code === 200) {
            activeConsultation.value = replyRes.data.data
            await nextTick()
            scrollToBottom()
          }
        } catch (e) {
          console.error('营养师回复失败', e)
        } finally {
          replyLoading.value = false
        }
      }, 1500)
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
  if (!activeConsultation.value) return
  completeLoading.value = true
  try {
    const res = await completeConsultation(
      activeConsultation.value.orderNo,
      completeForm.value.rating,
      completeForm.value.review
    )
    if (res.data.code === 200) {
      message.success('咨询已结束，感谢您的评价！')
      activeConsultation.value = null
      activeNutritionist.value = null
      completeDialogVisible.value = false
      fetchNutritionists()
    }
  } catch (e) {
    message.error('操作失败')
  } finally {
    completeLoading.value = false
  }
}

async function showMyOrders() {
  ordersDialogVisible.value = true
  ordersLoading.value = true
  try {
    const res = await getConsultationHistory(0, 20)
    if (res.data.code === 200) {
      orders.value = res.data.data.content || []
    }
  } catch (e) {
    message.error('获取记录失败')
  } finally {
    ordersLoading.value = false
  }
}

async function handleRefund(row) {
  try {
    const res = await simulateRefundConsultation(row.orderNo)
    if (res.data.code === 200) {
      message.success('退款成功')
      showMyOrders()
    } else {
      message.error(res.data.message || '退款失败')
    }
  } catch (e) {
    message.error('退款操作失败')
  }
}

function scrollToBottom() {
  if (chatMessagesRef.value) {
    chatMessagesRef.value.scrollTop = chatMessagesRef.value.scrollHeight
  }
}

// --- 工具函数 ---

function statusText(s) {
  return { ONLINE: '在线', BUSY: '忙碌', OFFLINE: '离线' }[s] || s
}

function payStatusType(s) {
  return (
    { PAID: 'success', PENDING: 'warning', REFUNDED: 'danger', EXPIRED: 'info', CANCELLED: 'info' }[
      s
    ] || 'info'
  )
}

function payStatusText(s) {
  return (
    {
      PAID: '已支付',
      PENDING: '待支付',
      REFUNDED: '已退款',
      EXPIRED: '已超时',
      CANCELLED: '已取消'
    }[s] || s
  )
}

function consultStatusType(s) {
  return (
    { IN_PROGRESS: 'success', WAITING: 'warning', COMPLETED: '', CANCELLED: 'info' }[s] || 'info'
  )
}

function consultStatusText(s) {
  return (
    { IN_PROGRESS: '进行中', WAITING: '等待中', COMPLETED: '已完成', CANCELLED: '已取消' }[s] || s
  )
}

function formatDate(dt) {
  if (!dt) return '-'
  return new Date(dt).toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

function formatMsgTime(ts) {
  if (!ts) return ''
  return new Date(ts).toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
}
</script>

<style scoped lang="scss">
.consultation-view {
  min-height: 100vh;
  background: #f5f7fa;
}

.top-nav {
  background: #fff;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  position: sticky;
  top: 0;
  z-index: 100;

  .nav-inner {
    max-width: 1200px;
    margin: 0 auto;
    padding: 0 20px;
    display: flex;
    justify-content: space-between;
    align-items: center;
    height: 56px;
  }

  .nav-left {
    display: flex;
    align-items: center;
    gap: 12px;
  }

  .page-title {
    font-size: 18px;
    font-weight: 600;
    color: #1f2937;
    margin: 0;
  }
}

.main-area {
  max-width: 1200px;
  margin: 0 auto;
  padding: 24px 20px;
}

// === 营养师列表 ===
.section-header {
  text-align: center;
  margin-bottom: 32px;

  h2 {
    font-size: 28px;
    font-weight: 700;
    color: #1f2937;
    margin-bottom: 8px;
  }

  .section-desc {
    color: #6b7280;
    font-size: 15px;
  }
}

.nutritionist-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;

  @media (max-width: 960px) {
    grid-template-columns: repeat(2, 1fr);
  }
  @media (max-width: 640px) {
    grid-template-columns: 1fr;
  }
}

.nutritionist-card {
  border-radius: 12px;
  transition: transform 0.2s;

  &:hover {
    transform: translateY(-4px);
  }

  .n-header {
    display: flex;
    justify-content: space-between;
    align-items: flex-start;
    margin-bottom: 12px;
  }

  .n-avatar {
    background: linear-gradient(135deg, #667eea, #764ba2);
    color: #fff;
    font-weight: 600;
  }

  .n-name {
    font-size: 18px;
    font-weight: 600;
    margin: 0 0 4px;
  }

  .n-title {
    color: #6b7280;
    font-size: 13px;
    margin: 0 0 8px;
  }

  .n-specialties {
    display: flex;
    flex-wrap: wrap;
    gap: 4px;
    margin-bottom: 10px;
  }

  .n-intro {
    color: #4b5563;
    font-size: 13px;
    line-height: 1.5;
    margin: 0 0 12px;
    display: -webkit-box;
    -webkit-line-clamp: 3;
    -webkit-box-orient: vertical;
    overflow: hidden;
  }

  .n-stats {
    display: flex;
    justify-content: space-between;
    margin-bottom: 16px;
    padding: 10px 0;
    border-top: 1px solid #f3f4f6;
    border-bottom: 1px solid #f3f4f6;

    .stat {
      text-align: center;
      .stat-value {
        font-size: 15px;
        font-weight: 600;
        display: flex;
        align-items: center;
        justify-content: center;
        gap: 2px;
      }
      .stat-label {
        font-size: 12px;
        color: #9ca3af;
        display: block;
        margin-top: 2px;
      }
    }
  }

  .n-footer {
    display: flex;
    justify-content: space-between;
    align-items: center;

    .fee-label {
      font-size: 12px;
      color: #9ca3af;
    }
    .fee-value {
      font-size: 20px;
      font-weight: 700;
      color: #f56c6c;
    }
  }
}

// === 咨询室 ===
.chat-room-card {
  border-radius: 12px;

  .chat-header {
    display: flex;
    justify-content: space-between;
    align-items: center;

    .chat-info {
      display: flex;
      align-items: center;
      gap: 12px;

      h3 {
        margin: 0;
        font-size: 16px;
      }
    }
  }
}

.chat-messages {
  height: 420px;
  overflow-y: auto;
  padding: 16px;
  background: #f9fafb;
  border-radius: 8px;
  margin-bottom: 16px;

  .system-msg {
    text-align: center;
    margin-bottom: 16px;
  }

  .message-item {
    margin-bottom: 14px;
    display: flex;

    &.msg-user {
      justify-content: flex-end;
      .msg-bubble {
        background: #667eea;
        color: #fff;
        border-radius: 16px 16px 4px 16px;
        .msg-time {
          color: rgba(255, 255, 255, 0.7);
        }
      }
    }

    &.msg-nutritionist {
      justify-content: flex-start;
      .msg-bubble {
        background: #fff;
        color: #1f2937;
        border-radius: 16px 16px 16px 4px;
        border: 1px solid #e5e7eb;
        .msg-time {
          color: #9ca3af;
        }
      }
    }

    .msg-bubble {
      max-width: 70%;
      padding: 12px 16px;

      .msg-content {
        font-size: 14px;
        line-height: 1.6;
        white-space: pre-wrap;
        word-break: break-word;
      }
      .msg-time {
        font-size: 11px;
        text-align: right;
        margin-top: 4px;
      }
    }
  }
}

.typing-indicator {
  display: flex;
  gap: 4px;
  padding: 4px 0;

  span {
    width: 8px;
    height: 8px;
    border-radius: 50%;
    background: #9ca3af;
    animation: typing 1.2s infinite;

    &:nth-child(2) {
      animation-delay: 0.2s;
    }
    &:nth-child(3) {
      animation-delay: 0.4s;
    }
  }
}

@keyframes typing {
  0%,
  60%,
  100% {
    opacity: 0.3;
    transform: translateY(0);
  }
  30% {
    opacity: 1;
    transform: translateY(-4px);
  }
}

.chat-input-area {
  .input-actions {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-top: 8px;

    .input-tip {
      font-size: 12px;
      color: #9ca3af;
    }
  }
}

// === 预约表单 ===
.consult-form {
  .consult-info {
    display: flex;
    align-items: center;
    gap: 12px;
    padding: 16px;
    background: #f9fafb;
    border-radius: 8px;
    margin-bottom: 20px;

    h4 {
      margin: 0;
      font-size: 16px;
    }
    p {
      margin: 0;
      color: #6b7280;
      font-size: 13px;
    }
    .consult-fee {
      margin-left: auto;
      font-size: 22px;
      font-weight: 700;
      color: #f56c6c;
    }
  }
}
</style>
