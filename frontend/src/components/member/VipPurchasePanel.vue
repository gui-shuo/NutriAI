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
          :class="{
            'plan-selected': selectedPlanId === plan.id,
            'plan-recommended': plan.badge === '推荐'
          }"
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
          <!-- 权益预览 -->
          <ul class="plan-features" v-if="plan.benefits?.features">
            <li v-for="(f, i) in plan.benefits.features.slice(0, 3)" :key="i">
              <el-icon color="#67c23a"><CircleCheck /></el-icon> {{ f }}
            </li>
            <li v-if="plan.benefits.features.length > 3" class="more-features"
                @click.stop="showBenefitsDetail(plan)">
              查看全部 {{ plan.benefits.features.length }} 项权益 →
            </li>
          </ul>
        </div>
      </div>
    </el-skeleton>

    <!-- 支付按钮 -->
    <div class="pay-action">
      <div class="pay-type-select">
        <span class="pay-type-label">支付方式：</span>
        <el-radio-group v-model="payType" size="small">
          <el-radio-button value="alipay">支付宝</el-radio-button>
          <el-radio-button value="wxpay">微信支付</el-radio-button>
        </el-radio-group>
      </div>
      <el-button
        type="primary"
        size="large"
        :loading="payLoading"
        :disabled="!selectedPlanId"
        class="pay-btn"
        @click="handlePay"
      >
        <el-icon><CreditCard /></el-icon>
        立即开通
      </el-button>
      <div class="pay-tip">
        <el-icon><Lock /></el-icon>
        安全加密 · 支持多种支付方式
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
        <el-table v-else :data="orders" style="width: 100%">
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
          <el-table-column label="操作" width="100" align="center">
            <template #default="{ row }">
              <el-button
                v-if="row.paymentStatus === 'PAID'"
                type="danger"
                size="small"
                text
                @click="handleRefund(row)"
              >
                模拟退款
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-skeleton>
      <template #footer>
        <el-button @click="orderHistoryVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- 模拟支付确认 Dialog -->
    <el-dialog
      v-model="payDialogVisible"
      title="模拟支付"
      width="500px"
      :close-on-click-modal="false"
      @closed="handlePayDialogClose"
    >
      <div v-if="pendingOrder" class="pay-dialog-content">
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="套餐">{{ pendingOrder.planName }}</el-descriptions-item>
          <el-descriptions-item label="金额">¥{{ pendingOrder.amount }}</el-descriptions-item>
          <el-descriptions-item label="订单号">{{ pendingOrder.orderNo }}</el-descriptions-item>
          <el-descriptions-item label="有效期至">{{
            formatDate(pendingOrder.expireTime)
          }}</el-descriptions-item>
        </el-descriptions>

        <div class="pay-countdown" v-if="countdown > 0">
          请在 <span class="countdown-num">{{ countdownStr }}</span> 内完成支付
        </div>
        <div class="pay-expired" v-else>订单已超时，请重新下单</div>

        <el-alert type="info" :closable="false" show-icon style="margin: 16px 0">
          <template #title>当前为模拟支付模式，点击下方按钮即可模拟支付成功</template>
        </el-alert>

        <div class="pay-buttons">
          <el-button
            type="primary"
            size="large"
            :loading="queryLoading"
            :disabled="countdown <= 0"
            @click="confirmSimulatePayment"
          >
            <el-icon><CreditCard /></el-icon>
            确认支付（模拟）
          </el-button>
          <el-button size="large" @click="payDialogVisible = false"> 取消 </el-button>
        </div>
        <p class="pay-hint">模拟支付模式：点击"确认支付"将自动完成支付流程，无需真实付款</p>
      </div>
    </el-dialog>
    <!-- 权益详情 Dialog -->
    <el-dialog
      v-model="benefitsDialogVisible"
      :title="benefitsDialogPlan?.planName + ' · 全部权益'"
      width="420px"
    >
      <ul v-if="benefitsDialogPlan?.benefits?.features" class="benefits-full-list">
        <li v-for="(f, i) in benefitsDialogPlan.benefits.features" :key="i">
          <el-icon color="#67c23a"><CircleCheck /></el-icon>
          <span>{{ f }}</span>
        </li>
      </ul>
      <div class="benefits-quota" v-if="benefitsDialogPlan?.benefits">
        <p v-if="benefitsDialogPlan.benefits.ai_daily_quota === -1">✨ AI咨询：无限次/天</p>
        <p v-else>🤖 AI咨询：{{ benefitsDialogPlan.benefits.ai_daily_quota }}次/天</p>
        <p v-if="benefitsDialogPlan.benefits.food_recognition === -1">📸 食物识别：不限次数</p>
        <p v-else>📸 食物识别：{{ benefitsDialogPlan.benefits.food_recognition }}次/天</p>
      </div>
      <template #footer>
        <el-button type="primary" @click="benefitsDialogVisible = false; selectedPlanId = benefitsDialogPlan?.id">
          选择此套餐
        </el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { CircleCheck, Present, CreditCard, Lock } from '@element-plus/icons-vue'
import {
  getVipPlans,
  getVipStatus,
  createVipOrder,
  simulateVipPayment,
  simulateVipRefund,
  getVipOrderHistory
} from '@/services/member'
import message from '@/utils/message'

const emit = defineEmits(['vip-activated'])

const plansLoading = ref(false)
const plans = ref([])

// 权益详情
const benefitsDialogVisible = ref(false)
const benefitsDialogPlan = ref(null)
const selectedPlanId = ref(null)
const vipStatus = ref(null)

const payLoading = ref(false)
const payDialogVisible = ref(false)
const pendingOrder = ref(null)
const payType = ref('alipay') // 支付方式

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
    const res = await createVipOrder(selectedPlanId.value, payType.value)
    if (res.data.code === 200) {
      pendingOrder.value = res.data.data
      payDialogVisible.value = true
      startCountdown()
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

async function confirmSimulatePayment() {
  if (!pendingOrder.value) return
  queryLoading.value = true
  try {
    const res = await simulateVipPayment(pendingOrder.value.orderNo)
    if (res.data.code === 200) {
      const order = res.data.data
      if (order.paymentStatus === 'PAID') {
        handlePaySuccess()
      } else {
        message.error('模拟支付失败，请重试')
      }
    } else {
      message.error(res.data.message || '支付失败')
    }
  } catch (e) {
    message.error('支付失败，请稍后重试')
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

function clearTimers() {
  clearInterval(countdownTimer)
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

async function handleRefund(row) {
  try {
    const res = await simulateVipRefund(row.orderNo)
    if (res.data.code === 200) {
      message.success('退款成功，VIP权益已回收')
      showOrderHistory()
      fetchVipStatus()
      emit('vip-activated') // 通知父组件刷新权限
    } else {
      message.error(res.data.message || '退款失败')
    }
  } catch (e) {
    message.error('退款操作失败')
  }
}

function showBenefitsDetail(plan) {
  benefitsDialogPlan.value = plan
  benefitsDialogVisible.value = true
}

// ------ 工具函数 ------

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

function badgeClass(badge) {
  return (
    {
      热门: 'badge-hot',
      推荐: 'badge-recommend',
      超值: 'badge-value'
    }[badge] || ''
  )
}

function statusTagType(status) {
  return (
    { PAID: 'success', PENDING: 'warning', EXPIRED: 'info', CANCELLED: 'info', FAILED: 'danger' }[
      status
    ] || 'info'
  )
}
</script>

<style scoped lang="scss">
.vip-purchase-panel {
  border-radius: 255px 15px 225px 15px / 15px 225px 15px 255px;
  border: 2px solid #2d2d2d;
  box-shadow: 4px 4px 0px 0px #2d2d2d;

  :deep(.el-card__header) {
    padding: 12px 16px;
  }

  :deep(.el-card__body) {
    padding: 14px 16px;
  }

  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;

    .title-group {
      display: flex;
      align-items: center;
      gap: 8px;

      .title {
        font-size: 15px;
        font-weight: 600;
        color: #2d2d2d;
      }
    }
  }
}

.plans-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(170px, 1fr));
  gap: 10px;
  margin-bottom: 16px;

  @media (max-width: 768px) {
    grid-template-columns: repeat(2, 1fr);
  }
  @media (max-width: 480px) {
    grid-template-columns: 1fr;
  }
}

.plan-card {
  position: relative;
  border: 2px solid #2d2d2d;
  border-radius: 185px 10px 155px 10px / 10px 155px 10px 185px;
  padding: 14px 12px;
  cursor: pointer;
  transition: all 0.25s;
  background: #fdfbf7;

  &:hover {
    border-color: #2d2d2d;
    box-shadow: 4px 4px 0px 0px #2d2d2d;
    transform: translate(-2px, -2px);
  }

  &.plan-selected {
    border-color: #2d5da1;
    background: #fff9c4;
    box-shadow: 4px 4px 0px 0px #2d2d2d;
  }

  &.plan-recommended {
    border-color: #e6a23c;

    &.plan-selected {
      border-color: #2d5da1;
    }
  }

  .plan-badge {
    position: absolute;
    top: -1px;
    right: 10px;
    padding: 1px 8px;
    border-radius: 0 0 6px 6px;
    font-size: 11px;
    font-weight: 600;
    color: #fff;

    &.badge-hot {
      background: #ff4d4d;
    }
    &.badge-recommend {
      background: #2d5da1;
    }
    &.badge-value {
      background: #67c23a;
    }
  }

  .plan-name {
    font-size: 14px;
    font-weight: 700;
    color: #2d2d2d;
    margin-bottom: 2px;
  }

  .plan-duration {
    font-size: 12px;
    color: #9ca3af;
    margin-bottom: 8px;
  }

  .plan-price {
    display: flex;
    align-items: baseline;
    gap: 4px;
    margin-bottom: 2px;

    .price-current {
      font-size: 22px;
      font-weight: 700;
      color: #2d5da1;
    }

    .price-discount {
      font-size: 11px;
      color: #f56c6c;
      background: #fef0f0;
      padding: 1px 4px;
      border-radius: 3px;
    }
  }

  .plan-original {
    font-size: 11px;
    color: #9ca3af;
    text-decoration: line-through;
    margin-bottom: 2px;
  }

  .plan-per-day {
    font-size: 11px;
    color: #6b7280;
    margin-bottom: 6px;
  }

  .plan-bonus {
    display: flex;
    align-items: center;
    gap: 3px;
    font-size: 11px;
    color: #e6a23c;
    margin-bottom: 8px;
  }

  .plan-features {
    list-style: none;
    padding: 0;
    margin: 0;

    li {
      display: flex;
      align-items: center;
      gap: 4px;
      font-size: 12px;
      color: #4b5563;
      padding: 2px 0;
    }

    .more-features {
      color: #2d5da1;
      font-size: 11px;
      cursor: pointer;
      &:hover { text-decoration: underline; }
    }
  }
}

// 权益详情弹窗
.benefits-full-list {
  list-style: none;
  padding: 0;
  margin: 0;
  li {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 8px 0;
    font-size: 14px;
    color: #374151;
    border-bottom: 1px dashed #2d2d2d;
    &:last-child { border-bottom: none; }
  }
}
.benefits-quota {
  margin-top: 12px;
  padding: 12px;
  background: #fdfbf7;
  border: 1px dashed #2d2d2d;
  border-radius: 8px;
  p { margin: 4px 0; font-size: 13px; color: #4b5563; }
}

.pay-action {
  text-align: center;

  .pay-type-select {
    margin-bottom: 10px;

    .pay-type-label {
      font-size: 13px;
      color: #6b7280;
    }
  }

  .pay-btn {
    min-width: 220px;
    height: 40px;
    font-size: 14px;
    border-radius: 255px 15px 225px 15px / 15px 225px 15px 255px;
    background: #2d5da1;
    border: 2px solid #2d2d2d;
    box-shadow: 4px 4px 0px 0px #2d2d2d;

    &:hover {
      transform: translate(-2px, -2px);
      box-shadow: 6px 6px 0px 0px #2d2d2d;
    }
  }

  .pay-tip {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 4px;
    margin-top: 8px;
    font-size: 11px;
    color: #2d2d2d;
  }
}

// 支付 Dialog
.pay-dialog-content {
  .pay-countdown {
    text-align: center;
    font-size: 14px;
    color: #6b7280;
    margin: 12px 0;

    .countdown-num {
      font-size: 18px;
      font-weight: 700;
      color: #e6a23c;
      font-family: monospace;
    }
  }

  .pay-expired {
    text-align: center;
    font-size: 14px;
    color: #f56c6c;
    margin: 12px 0;
  }

  .pay-buttons {
    display: flex;
    gap: 10px;
    justify-content: center;
    margin-top: 16px;
  }

  .pay-hint {
    text-align: center;
    font-size: 11px;
    color: #9ca3af;
    margin-top: 10px;
  }
}
</style>
