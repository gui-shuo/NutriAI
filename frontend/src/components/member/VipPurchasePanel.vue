<template>
  <el-card class="vip-purchase-panel">
    <template #header>
      <div class="card-header">
        <div class="title-group">
          <span class="title">NutriAI 营养卡套餐</span>
          <el-tag v-if="vipStatus?.isVip" type="success" effect="dark">
            VIP 到期：{{ formatDate(vipStatus.vipExpireAt) }}
          </el-tag>
        </div>
        <el-button
          v-if="vipStatus?.isVip"
          type="warning"
          size="small"
          text
          @click="showOrderHistory"
        >
          查看订单
        </el-button>
      </div>
    </template>

    <!-- 套餐列表 -->
    <el-skeleton :loading="plansLoading" animated :rows="4">
      <div class="plans-grid">
        <div
          v-for="plan in plans"
          :key="plan.id"
          class="plan-card"
          :class="{ 'plan-selected': selectedPlanId === plan.id, 'plan-recommended': plan.badge === '推荐' }"
          @click="selectedPlanId = plan.id"
        >
          <div v-if="plan.badge" class="plan-badge" :class="badgeClass(plan.badge)">
            {{ plan.badge }}
          </div>
          <div class="plan-name">{{ plan.planName }}</div>
          <div class="plan-duration">{{ plan.durationDays }}天</div>
          <div class="plan-price">
            <span class="price-current">¥{{ plan.discountPrice }}</span>
            <span v-if="plan.discountLabel" class="price-discount">{{ plan.discountLabel }}</span>
          </div>
          <div class="plan-original" v-if="plan.originalPrice !== plan.discountPrice">
            原价 ¥{{ plan.originalPrice }}
          </div>
          <div class="plan-per-day">约¥{{ plan.pricePerDay }}/天</div>
          <div v-if="plan.bonusGrowth" class="plan-bonus">
            <el-icon><Present /></el-icon>
            赠 {{ plan.bonusGrowth }} 成长值
          </div>
          <!-- 权益预览 -->
          <ul class="plan-features" v-if="plan.benefits?.features">
            <li v-for="(f, i) in plan.benefits.features.slice(0, 3)" :key="i">
              <el-icon color="#67c23a"><CircleCheck /></el-icon> {{ f }}
            </li>
            <li v-if="plan.benefits.features.length > 3" class="more-features">
              +{{ plan.benefits.features.length - 3 }} 项更多权益
            </li>
          </ul>
        </div>
      </div>
    </el-skeleton>

    <!-- 支付按钮 -->
    <div class="pay-action">
      <el-button
        type="primary"
        size="large"
        :loading="payLoading"
        :disabled="!selectedPlanId"
        class="pay-btn"
        @click="handlePay"
      >
        <el-icon><CreditCard /></el-icon>
        立即开通 · 支付宝支付
      </el-button>
      <div class="pay-tip">
        <el-icon><Lock /></el-icon>
        安全加密 · 支持退款 · 自动续期到期提醒
      </div>
    </div>

    <!-- 订单历史 Dialog -->
    <el-dialog
      v-model="orderHistoryVisible"
      title="我的订单"
      width="680px"
      :close-on-click-modal="false"
    >
      <el-skeleton :loading="orderLoading" animated :rows="4">
        <el-empty v-if="!orders.length" description="暂无订单记录" />
        <el-table v-else :data="orders" style="width:100%">
          <el-table-column label="订单号" prop="orderNo" min-width="160" />
          <el-table-column label="套餐" prop="planName" width="120" />
          <el-table-column label="金额" width="90" align="center">
            <template #default="{ row }">¥{{ row.amount }}</template>
          </el-table-column>
          <el-table-column label="状态" width="90" align="center">
            <template #default="{ row }">
              <el-tag :type="statusTagType(row.paymentStatus)" size="small">
                {{ row.paymentStatusName }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="VIP到期" min-width="140">
            <template #default="{ row }">
              <span v-if="row.vipExpireAt">{{ formatDate(row.vipExpireAt) }}</span>
              <span v-else>-</span>
            </template>
          </el-table-column>
          <el-table-column label="下单时间" min-width="140">
            <template #default="{ row }">{{ formatDate(row.createdAt) }}</template>
          </el-table-column>
        </el-table>
      </el-skeleton>
      <template #footer>
        <el-button @click="orderHistoryVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- 支付确认 Dialog（包含支付宝跳转 HTML） -->
    <el-dialog
      v-model="payDialogVisible"
      title="支付宝支付"
      width="500px"
      :close-on-click-modal="false"
      @closed="handlePayDialogClose"
    >
      <div v-if="pendingOrder" class="pay-dialog-content">
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="套餐">{{ pendingOrder.planName }}</el-descriptions-item>
          <el-descriptions-item label="金额">¥{{ pendingOrder.amount }}</el-descriptions-item>
          <el-descriptions-item label="订单号">{{ pendingOrder.orderNo }}</el-descriptions-item>
          <el-descriptions-item label="有效期至">{{ formatDate(pendingOrder.expireTime) }}</el-descriptions-item>
        </el-descriptions>

        <div class="pay-countdown" v-if="countdown > 0">
          请在 <span class="countdown-num">{{ countdownStr }}</span> 内完成支付
        </div>
        <div class="pay-expired" v-else>订单已超时，请重新下单</div>

        <div class="pay-buttons">
          <el-button
            type="primary"
            size="large"
            :disabled="countdown <= 0"
            @click="openAlipayPage"
          >
            <el-icon><CreditCard /></el-icon>
            跳转支付宝完成支付
          </el-button>
          <el-button
            type="success"
            size="large"
            :loading="queryLoading"
            @click="manualQueryStatus"
          >
            <el-icon><Refresh /></el-icon>
            我已完成支付
          </el-button>
        </div>
        <p class="pay-hint">点击"跳转支付宝"后，在新标签完成支付，返回此处点击"我已完成支付"</p>
      </div>
    </el-dialog>
  </el-card>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import {
  CircleCheck, Present, CreditCard, Lock, Refresh
} from '@element-plus/icons-vue'
import {
  getVipPlans, getVipStatus, createVipOrder,
  queryVipOrderStatus, getVipOrderHistory
} from '@/services/member'
import message from '@/utils/message'

const emit = defineEmits(['vip-activated'])

const plansLoading = ref(false)
const plans = ref([])
const selectedPlanId = ref(null)
const vipStatus = ref(null)

const payLoading = ref(false)
const payDialogVisible = ref(false)
const pendingOrder = ref(null)
const pendingPayForm = ref('')   // 支付宝 HTML form

// 倒计时
const countdown = ref(0)
let countdownTimer = null
const countdownStr = computed(() => {
  const m = Math.floor(countdown.value / 60)
  const s = countdown.value % 60
  return `${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`
})

// 订单查询
const queryLoading = ref(false)
let pollTimer = null

// 订单历史
const orderHistoryVisible = ref(false)
const orderLoading = ref(false)
const orders = ref([])

// ------ 初始化 ------

onMounted(async () => {
  await Promise.all([fetchPlans(), fetchVipStatus()])
})

onBeforeUnmount(() => {
  clearTimers()
})

// ------ 方法 ------

async function fetchPlans() {
  plansLoading.value = true
  try {
    const res = await getVipPlans()
    if (res.data.code === 200) {
      plans.value = res.data.data || []
      // 默认选中第二个（推荐）
      if (plans.value.length >= 2) selectedPlanId.value = plans.value[1].id
    }
  } catch (e) {
    console.error('获取VIP套餐失败', e)
  } finally {
    plansLoading.value = false
  }
}

async function fetchVipStatus() {
  try {
    const res = await getVipStatus()
    if (res.data.code === 200) vipStatus.value = res.data.data
  } catch (e) {
    console.error('获取VIP状态失败', e)
  }
}

async function handlePay() {
  if (!selectedPlanId.value) return
  payLoading.value = true
  try {
    const res = await createVipOrder(selectedPlanId.value)
    if (res.data.code === 200) {
      pendingOrder.value = res.data.data
      pendingPayForm.value = res.data.data.payUrl
      payDialogVisible.value = true
      startCountdown()
      startPolling()
    } else {
      message.error(res.data.message || '创建订单失败')
    }
  } catch (e) {
    console.error('创建VIP订单失败', e)
    message.error('创建订单失败，请稍后重试')
  } finally {
    payLoading.value = false
  }
}

function openAlipayPage() {
  if (!pendingPayForm.value) return
  // 使用 Blob URL 安全打开支付宝表单，避免 document.write XSS 风险
  const blob = new Blob([pendingPayForm.value], { type: 'text/html; charset=utf-8' })
  const url = URL.createObjectURL(blob)
  window.open(url, '_blank')
  // 延迟释放 URL 确保页面加载完成
  setTimeout(() => URL.revokeObjectURL(url), 5000)
}

async function manualQueryStatus() {
  if (!pendingOrder.value) return
  queryLoading.value = true
  try {
    const res = await queryVipOrderStatus(pendingOrder.value.orderNo)
    if (res.data.code === 200) {
      const order = res.data.data
      if (order.paymentStatus === 'PAID') {
        handlePaySuccess()
      } else if (order.paymentStatus === 'EXPIRED' || order.paymentStatus === 'CANCELLED') {
        message.warning('订单已超时或已取消，请重新下单')
        payDialogVisible.value = false
      } else {
        message.info('支付尚未到账，请稍后再试')
      }
    }
  } catch (e) {
    message.error('查询失败，请稍后重试')
  } finally {
    queryLoading.value = false
  }
}

function handlePaySuccess() {
  clearTimers()
  payDialogVisible.value = false
  message.success('🎉 开通成功！VIP权益已即时生效')
  fetchVipStatus()
  emit('vip-activated')
}

function handlePayDialogClose() {
  clearTimers()
  pendingOrder.value = null
  pendingPayForm.value = ''
}

function startCountdown() {
  if (!pendingOrder.value?.expireTime) return
  const expireMs = new Date(pendingOrder.value.expireTime).getTime()
  countdown.value = Math.max(0, Math.round((expireMs - Date.now()) / 1000))
  clearInterval(countdownTimer)
  countdownTimer = setInterval(() => {
    countdown.value = Math.max(0, countdown.value - 1)
    if (countdown.value === 0) clearInterval(countdownTimer)
  }, 1000)
}

function startPolling() {
  clearInterval(pollTimer)
  pollTimer = setInterval(async () => {
    if (!pendingOrder.value || !payDialogVisible.value) {
      clearInterval(pollTimer)
      return
    }
    try {
      const res = await queryVipOrderStatus(pendingOrder.value.orderNo)
      if (res.data.code === 200 && res.data.data.paymentStatus === 'PAID') {
        handlePaySuccess()
      }
    } catch (_) { /* ignore */ }
  }, 5000) // 每5秒轮询
}

function clearTimers() {
  clearInterval(countdownTimer)
  clearInterval(pollTimer)
}

async function showOrderHistory() {
  orderHistoryVisible.value = true
  orderLoading.value = true
  try {
    const res = await getVipOrderHistory(0, 20)
    if (res.data.code === 200) orders.value = res.data.data.content || []
  } catch (e) {
    message.error('获取订单失败')
  } finally {
    orderLoading.value = false
  }
}

// ------ 工具函数 ------

function formatDate(dt) {
  if (!dt) return '-'
  return new Date(dt).toLocaleString('zh-CN', {
    year: 'numeric', month: '2-digit', day: '2-digit',
    hour: '2-digit', minute: '2-digit'
  })
}

function badgeClass(badge) {
  return {
    '热门': 'badge-hot',
    '推荐': 'badge-recommend',
    '超值': 'badge-value'
  }[badge] || ''
}

function statusTagType(status) {
  return { PAID: 'success', PENDING: 'warning', EXPIRED: 'info', CANCELLED: 'info', FAILED: 'danger' }[status] || 'info'
}
</script>

<style scoped lang="scss">
.vip-purchase-panel {
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);

  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;

    .title-group {
      display: flex;
      align-items: center;
      gap: 10px;

      .title {
        font-size: 18px;
        font-weight: 600;
        color: #1f2937;
      }
    }
  }
}

.plans-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
  margin-bottom: 24px;

  @media (max-width: 768px) {
    grid-template-columns: 1fr;
  }
}

.plan-card {
  position: relative;
  border: 2px solid #e5e7eb;
  border-radius: 12px;
  padding: 20px 16px;
  cursor: pointer;
  transition: all 0.25s;
  background: #fff;

  &:hover {
    border-color: #667eea;
    box-shadow: 0 4px 16px rgba(102, 126, 234, 0.15);
    transform: translateY(-2px);
  }

  &.plan-selected {
    border-color: #667eea;
    background: linear-gradient(135deg, #f0f4ff 0%, #f8f0ff 100%);
    box-shadow: 0 4px 20px rgba(102, 126, 234, 0.2);
  }

  &.plan-recommended {
    border-color: #e6a23c;

    &.plan-selected {
      border-color: #667eea;
    }
  }

  .plan-badge {
    position: absolute;
    top: -1px;
    right: 12px;
    padding: 2px 10px;
    border-radius: 0 0 8px 8px;
    font-size: 12px;
    font-weight: 600;
    color: #fff;

    &.badge-hot      { background: linear-gradient(135deg, #f56c6c, #e91e63); }
    &.badge-recommend { background: linear-gradient(135deg, #e6a23c, #f5a623); }
    &.badge-value    { background: linear-gradient(135deg, #52c41a, #389e0d); }
  }

  .plan-name {
    font-size: 17px;
    font-weight: 700;
    color: #1f2937;
    margin-bottom: 4px;
  }

  .plan-duration {
    font-size: 13px;
    color: #9ca3af;
    margin-bottom: 12px;
  }

  .plan-price {
    display: flex;
    align-items: baseline;
    gap: 6px;
    margin-bottom: 2px;

    .price-current {
      font-size: 28px;
      font-weight: 700;
      color: #667eea;
    }

    .price-discount {
      font-size: 13px;
      color: #f56c6c;
      background: #fef0f0;
      padding: 2px 6px;
      border-radius: 4px;
    }
  }

  .plan-original {
    font-size: 12px;
    color: #9ca3af;
    text-decoration: line-through;
    margin-bottom: 4px;
  }

  .plan-per-day {
    font-size: 12px;
    color: #6b7280;
    margin-bottom: 10px;
  }

  .plan-bonus {
    display: flex;
    align-items: center;
    gap: 4px;
    font-size: 12px;
    color: #e6a23c;
    margin-bottom: 12px;
  }

  .plan-features {
    list-style: none;
    padding: 0;
    margin: 0;

    li {
      display: flex;
      align-items: center;
      gap: 6px;
      font-size: 13px;
      color: #4b5563;
      padding: 3px 0;
    }

    .more-features {
      color: #9ca3af;
      font-size: 12px;
    }
  }
}

.pay-action {
  text-align: center;

  .pay-btn {
    min-width: 260px;
    height: 48px;
    font-size: 16px;
    border-radius: 24px;
    background: linear-gradient(135deg, #1677ff 0%, #0958d9 100%);
    border: none;
    box-shadow: 0 4px 16px rgba(22, 119, 255, 0.3);

    &:hover {
      transform: translateY(-1px);
      box-shadow: 0 6px 20px rgba(22, 119, 255, 0.4);
    }
  }

  .pay-tip {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 4px;
    margin-top: 10px;
    font-size: 12px;
    color: #9ca3af;
  }
}

// 支付 Dialog
.pay-dialog-content {
  .pay-countdown {
    text-align: center;
    font-size: 15px;
    color: #6b7280;
    margin: 16px 0;

    .countdown-num {
      font-size: 20px;
      font-weight: 700;
      color: #e6a23c;
      font-family: monospace;
    }
  }

  .pay-expired {
    text-align: center;
    font-size: 15px;
    color: #f56c6c;
    margin: 16px 0;
  }

  .pay-buttons {
    display: flex;
    gap: 12px;
    justify-content: center;
    margin-top: 20px;
  }

  .pay-hint {
    text-align: center;
    font-size: 12px;
    color: #9ca3af;
    margin-top: 12px;
  }
}
</style>
