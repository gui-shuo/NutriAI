<template>
  <view class="page">
    <!-- ==================== VIP Status Card ==================== -->
    <view v-if="loading" class="vip-banner vip-loading">
      <view class="vip-top-row">
        <text class="vip-icon-big">⏳</text>
        <view class="vip-info">
          <text class="vip-plan-name skeleton-pulse">加载中...</text>
        </view>
      </view>
    </view>
    <view v-else-if="isVip" class="vip-banner vip-active">
      <view class="vip-top-row">
        <text class="vip-icon-big">💎</text>
        <view class="vip-info">
          <view class="vip-name-row">
            <text class="vip-plan-name">{{ vipStatus.planName || vipLevelText }}</text>
            <view class="vip-tag-badge">VIP</view>
          </view>
          <text class="vip-username">{{ memberInfo.username || '会员用户' }}</text>
        </view>
        <view class="quota-badge">
          <text class="quota-label">今日AI配额</text>
          <text class="quota-value">
            {{ permissions.aiQuotaTotal === -1 ? '∞' : (permissions.aiQuotaRemain ?? 0) + ' / ' + (permissions.aiQuotaTotal ?? 3) }}
          </text>
        </view>
      </view>
      <view class="vip-detail-row">
        <view class="vip-detail-item">
          <text class="vip-detail-label">到期时间</text>
          <text class="vip-detail-val">{{ vipExpiry }}</text>
        </view>
        <view class="vip-detail-item">
          <text class="vip-detail-label">剩余天数</text>
          <text class="vip-detail-val gold">{{ remainingDays }}天</text>
        </view>
        <view class="vip-detail-item">
          <text class="vip-detail-label">会员等级</text>
          <text class="vip-detail-val">{{ memberInfo.currentLevel?.levelName || '青铜会员' }}</text>
        </view>
      </view>
      <view v-if="memberInfo.nextLevel" class="level-progress-row">
        <text class="level-progress-text">距{{ memberInfo.nextLevel.levelName }}还需 {{ memberInfo.growthToNextLevel || 0 }} 成长值</text>
        <view class="progress-bar-bg">
          <view class="progress-bar-fill" :style="{ width: (memberInfo.upgradeProgress || 0) + '%' }"></view>
        </view>
      </view>
    </view>
    <view v-else class="vip-banner vip-inactive" @tap="scrollToPlan">
      <view class="vip-top-row">
        <text class="vip-icon-big">🌱</text>
        <view class="vip-info">
          <text class="vip-plan-name">免费用户</text>
          <text class="vip-username">开通营养卡，解锁全部AI营养功能</text>
        </view>
        <view class="quota-badge">
          <text class="quota-label">今日AI配额</text>
          <text class="quota-value">
            {{ (permissions.aiQuotaRemain ?? 0) + ' / ' + (permissions.aiQuotaTotal ?? 3) }}
          </text>
        </view>
      </view>
    </view>

    <!-- ==================== Two-Column: Sign-In + Invitation ==================== -->
    <view class="engagement-grid">
      <!-- Sign-In Calendar Card -->
      <view class="card sign-card">
        <view class="card-header-row">
          <text class="card-title-text">📅 每日签到</text>
          <text class="card-header-sub">{{ currentYear }}年{{ currentMonth }}月</text>
        </view>

        <!-- Sign-In Stats -->
        <view class="sign-stats">
          <view class="stat-item">
            <text class="stat-num">{{ signedDays.length }}</text>
            <text class="stat-label">本月已签</text>
          </view>
          <view class="stat-divider"></view>
          <view class="stat-item">
            <text class="stat-num">{{ continuousDays }}</text>
            <text class="stat-label">连续天数</text>
          </view>
          <view class="stat-divider"></view>
          <view class="stat-item">
            <text class="stat-num">{{ signedDays.length * 10 }}</text>
            <text class="stat-label">本月成长值</text>
          </view>
        </view>

        <!-- Monthly Calendar Grid (7 columns) -->
        <view class="calendar-grid">
          <view v-for="label in calendarWeekLabels" :key="label" class="cal-week-label">{{ label }}</view>
          <view v-for="n in firstDayOffset" :key="'e-' + n" class="cal-day-cell empty"></view>
          <view
            v-for="day in daysInMonth"
            :key="day"
            class="cal-day-cell"
            :class="{
              signed: signedDays.includes(day),
              today: day === todayDay && !signedDays.includes(day),
              future: day > todayDay
            }"
          >
            <text class="cal-day-num">{{ day }}</text>
            <text v-if="signedDays.includes(day)" class="cal-check">✓</text>
          </view>
        </view>

        <!-- Sign-In Button -->
        <button
          class="sign-btn"
          :class="{ disabled: todaySigned }"
          :disabled="todaySigned"
          @tap="handleSignIn"
        >
          {{ todaySigned ? '今日已签到 ✓' : '立即签到 +10成长值' }}
        </button>
        <view class="sign-tip">
          <text>ℹ️ 每日签到可获得 10 成长值，积累升级会员等级</text>
        </view>
      </view>

      <!-- Invitation Card -->
      <view class="card invite-card">
        <view class="card-header-row">
          <text class="card-title-text">🎁 邀请好友</text>
          <view class="invite-reward-tag">每人+50成长值</view>
        </view>

        <!-- Invitation Code -->
        <view class="invite-section">
          <text class="invite-label">我的邀请码</text>
          <view class="invite-code-display">
            <text class="invite-code-text">{{ invitationCode || '点击生成' }}</text>
            <button v-if="!invitationCode" class="btn-sm btn-outline" @tap="generateCode">生成</button>
            <button v-else class="btn-sm btn-accent" @tap="copyCode">复制</button>
          </view>
        </view>

        <!-- Invitation Link -->
        <view v-if="invitationCode" class="invite-section">
          <text class="invite-label">邀请链接</text>
          <view class="invite-link-display">
            <text class="invite-link-text">{{ invitationLink }}</text>
            <button class="btn-sm btn-outline" @tap="copyLink">复制链接</button>
          </view>
        </view>

        <!-- Invitation Stats -->
        <view class="invite-stats">
          <view class="invite-stat-box">
            <view class="invite-stat-icon bg-accent">👥</view>
            <view class="invite-stat-info">
              <text class="invite-stat-val">{{ memberInfo.invitationCount || 0 }}</text>
              <text class="invite-stat-lbl">已邀请</text>
            </view>
          </view>
          <view class="invite-stat-box">
            <view class="invite-stat-icon bg-gold">🎁</view>
            <view class="invite-stat-info">
              <text class="invite-stat-val">{{ (memberInfo.invitationCount || 0) * 50 }}</text>
              <text class="invite-stat-lbl">奖励成长值</text>
            </view>
          </view>
        </view>

        <!-- Recent Invitations -->
        <view class="invite-records-section">
          <view class="invite-records-header">
            <text class="invite-records-title">最近邀请</text>
          </view>
          <view v-if="invitationRecords.length" class="invite-records-list">
            <view v-for="rec in invitationRecords.slice(0, 3)" :key="rec.id" class="invite-record-item">
              <view class="invite-record-avatar">{{ (rec.inviteeName || '?').charAt(0) }}</view>
              <view class="invite-record-info">
                <text class="invite-record-name">{{ rec.inviteeName || '新用户' }}</text>
                <text class="invite-record-time">{{ formatTime(rec.acceptedAt || rec.createdAt) }}</text>
              </view>
              <view class="invite-record-badge" :class="rec.isRewarded ? 'rewarded' : 'pending'">
                {{ rec.isRewarded ? '+' + (rec.rewardGrowth || 50) : '待接受' }}
              </view>
            </view>
          </view>
          <view v-else class="invite-empty">
            <text>暂无邀请记录，快去邀请好友吧</text>
          </view>
        </view>
      </view>
    </view>

    <!-- ==================== VIP Plans ==================== -->
    <view id="vip-plans" class="card">
      <view class="card-header-row">
        <text class="card-title-text">💳 NutriAI 营养卡套餐</text>
        <text v-if="isVip" class="card-header-sub vip-expire-tag">VIP 到期：{{ vipExpiry }}</text>
      </view>

      <!-- Plans Grid -->
      <view v-if="plansLoading" class="plans-skeleton">
        <view class="skeleton-card" v-for="i in 3" :key="i">
          <view class="skeleton-line w60"></view>
          <view class="skeleton-line w40 mt8"></view>
          <view class="skeleton-line w80 mt16"></view>
        </view>
      </view>
      <view v-else class="plans-grid">
        <view
          v-for="plan in vipPlans"
          :key="plan.id"
          class="plan-card"
          :class="{
            selected: selectedPlan?.id === plan.id,
            recommended: plan.badge === '推荐'
          }"
          @tap="selectedPlan = plan"
        >
          <view v-if="plan.badge" class="plan-badge" :class="badgeClass(plan.badge)">{{ plan.badge }}</view>
          <text class="plan-name">{{ plan.planName }}</text>
          <text class="plan-duration">{{ plan.durationDays }}天</text>
          <view class="plan-price-row">
            <text class="plan-price-current">¥{{ plan.discountPrice }}</text>
            <view v-if="plan.discountLabel" class="plan-discount-label">{{ plan.discountLabel }}</view>
          </view>
          <text v-if="plan.originalPrice !== plan.discountPrice" class="plan-original">原价 ¥{{ plan.originalPrice }}</text>
          <text class="plan-per-day">约¥{{ plan.pricePerDay }}/天</text>
          <!-- Feature preview -->
          <view v-if="plan.benefits?.features" class="plan-features-preview">
            <view v-for="(f, i) in plan.benefits.features.slice(0, 3)" :key="i" class="plan-feature-item">
              <text class="feature-check">✓</text>
              <text>{{ f }}</text>
            </view>
            <view
              v-if="plan.benefits.features.length > 3"
              class="plan-feature-more"
              @tap.stop="showBenefitsDetail(plan)"
            >
              查看全部 {{ plan.benefits.features.length }} 项权益 →
            </view>
          </view>
        </view>
      </view>

      <!-- Pay Action -->
      <view class="pay-action">
        <view class="pay-type-row">
          <text class="pay-type-label">支付方式：</text>
          <view class="pay-type-options">
            <view
              class="pay-type-option"
              :class="{ active: payType === 'alipay' }"
              @tap="payType = 'alipay'"
            >支付宝</view>
            <view
              class="pay-type-option"
              :class="{ active: payType === 'wxpay' }"
              @tap="payType = 'wxpay'"
            >微信支付</view>
          </view>
        </view>
        <button
          class="pay-btn"
          :class="{ 'pay-btn-disabled': !selectedPlan }"
          :disabled="!selectedPlan"
          :loading="payLoading"
          @tap="handlePay"
        >
          💳 立即开通
        </button>
        <view class="pay-tip">🔒 安全加密 · 支持多种支付方式</view>
      </view>
    </view>

    <!-- ==================== Simulate Pay Modal ==================== -->
    <view v-if="payModalVisible" class="modal-overlay" @tap.self="payModalVisible = false">
      <view class="modal-content">
        <view class="modal-header">
          <text class="modal-title">模拟支付</text>
          <text class="modal-close" @tap="payModalVisible = false">✕</text>
        </view>
        <view v-if="pendingOrder" class="modal-body">
          <view class="order-info-grid">
            <view class="order-info-item">
              <text class="order-info-label">套餐</text>
              <text class="order-info-value">{{ pendingOrder.planName }}</text>
            </view>
            <view class="order-info-item">
              <text class="order-info-label">金额</text>
              <text class="order-info-value price">¥{{ pendingOrder.amount }}</text>
            </view>
            <view class="order-info-item">
              <text class="order-info-label">订单号</text>
              <text class="order-info-value mono">{{ pendingOrder.orderNo }}</text>
            </view>
            <view class="order-info-item">
              <text class="order-info-label">有效期至</text>
              <text class="order-info-value">{{ fmtDateTime(pendingOrder.expireTime) }}</text>
            </view>
          </view>
          <view v-if="countdown > 0" class="pay-countdown">
            请在 <text class="countdown-num">{{ countdownStr }}</text> 内完成支付
          </view>
          <view v-else class="pay-expired">订单已超时，请重新下单</view>
          <view class="pay-simulate-hint">
            <text>ℹ️ 当前为模拟支付模式，点击下方按钮即可模拟支付成功</text>
          </view>
          <view class="modal-btns">
            <button
              class="btn-confirm"
              :loading="simulateLoading"
              :disabled="countdown <= 0"
              @tap="confirmSimulatePay"
            >💳 确认支付（模拟）</button>
            <button class="btn-cancel" @tap="payModalVisible = false">取消</button>
          </view>
          <view class="pay-hint-small">模拟支付模式：点击"确认支付"将自动完成支付流程，无需真实付款</view>
        </view>
      </view>
    </view>

    <!-- ==================== Benefits Detail Modal ==================== -->
    <view v-if="benefitsModalVisible" class="modal-overlay" @tap.self="benefitsModalVisible = false">
      <view class="modal-content">
        <view class="modal-header">
          <text class="modal-title">{{ benefitsModalPlan?.planName }} · 全部权益</text>
          <text class="modal-close" @tap="benefitsModalVisible = false">✕</text>
        </view>
        <view class="modal-body">
          <view v-if="benefitsModalPlan?.benefits?.features" class="benefits-full-list">
            <view v-for="(f, i) in benefitsModalPlan.benefits.features" :key="i" class="benefits-full-item">
              <text class="feature-check">✓</text>
              <text>{{ f }}</text>
            </view>
          </view>
          <view v-if="benefitsModalPlan?.benefits" class="benefits-quota-box">
            <text v-if="benefitsModalPlan.benefits.ai_daily_quota === -1">✨ AI咨询：无限次/天</text>
            <text v-else>🤖 AI咨询：{{ benefitsModalPlan.benefits.ai_daily_quota }}次/天</text>
            <text v-if="benefitsModalPlan.benefits.food_recognition === -1">📸 食物识别：不限次数</text>
            <text v-else>📸 食物识别：{{ benefitsModalPlan.benefits.food_recognition }}次/天</text>
          </view>
          <button class="btn-confirm mt20" @tap="selectFromBenefits">选择此套餐</button>
        </view>
      </view>
    </view>

    <!-- ==================== Growth Records ==================== -->
    <view class="card">
      <view class="card-header-row">
        <text class="card-title-text">📊 成长记录</text>
        <text class="card-header-sub">总成长值：{{ memberInfo.totalGrowth || 0 }}</text>
      </view>

      <!-- Growth Chart -->
      <view v-if="growthChartData.length" class="growth-chart">
        <view class="chart-bars">
          <view v-for="(bar, i) in growthChartData" :key="i" class="chart-bar-col">
            <text class="chart-bar-val">{{ bar.value > 0 ? '+' + bar.value : bar.value }}</text>
            <view class="chart-bar" :class="{ negative: bar.value < 0 }" :style="{ height: bar.height + 'rpx' }"></view>
            <text class="chart-bar-label">{{ bar.label }}</text>
          </view>
        </view>
      </view>

      <view v-if="growthRecords.length" class="growth-list">
        <view v-for="record in growthRecords" :key="record.id" class="growth-item">
          <view class="growth-item-left">
            <text class="growth-desc">{{ record.description }}</text>
            <text class="growth-time">{{ formatTime(record.createdAt) }}</text>
          </view>
          <text class="growth-points" :class="record.growthValue > 0 ? 'positive' : 'negative'">
            {{ record.growthValue > 0 ? '+' : '' }}{{ record.growthValue }}
          </text>
        </view>
      </view>
      <view v-else class="empty-state">
        <text class="empty-icon">📊</text>
        <text class="empty-text">暂无成长记录</text>
      </view>

      <!-- Load More -->
      <view v-if="growthHasMore && growthRecords.length" class="load-more" @tap="loadMoreGrowth">
        <text>{{ growthLoadingMore ? '加载中...' : '加载更多' }}</text>
      </view>
    </view>

    <!-- ==================== VIP Benefits Comparison ==================== -->
    <view class="card">
      <view class="card-header-row" @tap="showBenefitsCompare = !showBenefitsCompare">
        <text class="card-title-text">🏆 VIP权益对比</text>
        <text class="toggle-text">{{ showBenefitsCompare ? '收起 ▲' : '展开 ▼' }}</text>
      </view>
      <view v-if="showBenefitsCompare" class="benefits-compare">
        <!-- Table Header -->
        <view class="compare-row compare-header">
          <text class="compare-cell name-cell">权益项目</text>
          <text class="compare-cell">免费版</text>
          <text class="compare-cell vip-cell">VIP版</text>
        </view>
        <view v-for="(item, i) in benefitsCompareData" :key="i" class="compare-row">
          <view class="compare-cell name-cell">
            <text class="compare-icon">{{ item.icon }}</text>
            <text>{{ item.name }}</text>
          </view>
          <text class="compare-cell free-val">{{ item.free }}</text>
          <text class="compare-cell vip-val">{{ item.vip }}</text>
        </view>
      </view>
    </view>

    <!-- ==================== Order History ==================== -->
    <view v-if="isVip" class="card">
      <view class="card-header-row" @tap="toggleOrderHistory">
        <text class="card-title-text">📋 我的订单</text>
        <text class="toggle-text">{{ showOrders ? '收起 ▲' : '查看 ▼' }}</text>
      </view>
      <view v-if="showOrders">
        <view v-if="ordersLoading" class="empty-state">
          <text class="empty-text skeleton-pulse">加载中...</text>
        </view>
        <view v-else-if="orders.length" class="order-list">
          <view v-for="order in orders" :key="order.orderNo" class="order-item">
            <view class="order-item-top">
              <text class="order-plan-name">{{ order.planName }}</text>
              <view class="order-status-tag" :class="orderStatusClass(order.paymentStatus)">
                {{ order.paymentStatusName }}
              </view>
            </view>
            <view class="order-item-details">
              <text class="order-detail-text">订单号: {{ order.orderNo }}</text>
              <text class="order-detail-text">金额: ¥{{ order.amount }}</text>
              <text v-if="order.vipExpireAt" class="order-detail-text">VIP到期: {{ fmtDateTime(order.vipExpireAt) }}</text>
              <text class="order-detail-text">下单: {{ fmtDateTime(order.createdAt) }}</text>
            </view>
          </view>
        </view>
        <view v-else class="empty-state">
          <text class="empty-text">暂无订单记录</text>
        </view>
      </view>
    </view>

    <view class="safe-bottom"></view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { useUserStore } from '@/stores/user'
import { memberApi, vipApi } from '@/services/api'
import { checkLogin, formatTime, formatDate } from '@/utils/common'

const userStore = useUserStore()

// ========== Reactive State ==========
const loading = ref(true)
const memberInfo = ref<any>({})
const vipStatus = ref<any>({})
const permissions = ref<any>({})
const signedDays = ref<number[]>([])
const signInInfo = ref<any>({})

// VIP Plans
const plansLoading = ref(false)
const vipPlans = ref<any[]>([])
const selectedPlan = ref<any>(null)
const payType = ref('alipay')
const payLoading = ref(false)

// Payment Modal
const payModalVisible = ref(false)
const pendingOrder = ref<any>(null)
const simulateLoading = ref(false)
const countdown = ref(0)
let countdownTimer: any = null

// Benefits Detail Modal
const benefitsModalVisible = ref(false)
const benefitsModalPlan = ref<any>(null)

// Growth
const growthRecords = ref<any[]>([])
const growthChartData = ref<any[]>([])
const growthPage = ref(0)
const growthHasMore = ref(true)
const growthLoadingMore = ref(false)

// Invitation
const invitationCode = ref('')
const invitationRecords = ref<any[]>([])

// UI toggles
const showBenefitsCompare = ref(false)
const showOrders = ref(false)
const orders = ref<any[]>([])
const ordersLoading = ref(false)

// ========== Calendar Computed ==========
const now = new Date()
const currentYear = now.getFullYear()
const currentMonth = now.getMonth() + 1
const todayDay = now.getDate()
const calendarWeekLabels = ['一', '二', '三', '四', '五', '六', '日']

const daysInMonth = computed(() => new Date(currentYear, currentMonth, 0).getDate())

const firstDayOffset = computed(() => {
  const day = new Date(currentYear, currentMonth - 1, 1).getDay()
  return day === 0 ? 6 : day - 1
})

const todaySigned = computed(() => signedDays.value.includes(todayDay))

const continuousDays = computed(() => {
  let count = 0
  for (let d = todayDay; d >= 1; d--) {
    if (signedDays.value.includes(d)) count++
    else break
  }
  return count
})

const countdownStr = computed(() => {
  const m = Math.floor(countdown.value / 60)
  const s = countdown.value % 60
  return `${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`
})

// ========== VIP Computed ==========
const vipLevelMap: Record<string, string> = {
  MONTHLY: '月度VIP', QUARTERLY: '季度VIP', HALF_YEARLY: '半年VIP',
  YEARLY: '年度VIP', BIANNUAL: '双年VIP', LIFETIME: '终身VIP'
}

const isVip = computed(() => vipStatus.value.isVip === true)
const vipLevelText = computed(() => vipLevelMap[memberInfo.value.vipLevel] || vipStatus.value.planName || '普通用户')

const vipExpiry = computed(() => {
  const expiry = vipStatus.value.vipExpireAt || memberInfo.value.vipExpiry
  if (!expiry) return ''
  return formatDate(new Date(expiry))
})

const remainingDays = computed(() => vipStatus.value.remainDays || 0)

const invitationLink = computed(() => {
  if (!invitationCode.value) return ''
  return `${window.location.origin}/register?code=${invitationCode.value}`
})

// ========== Benefits Comparison Data ==========
const benefitsCompareData = [
  { icon: '🤖', name: 'AI对话', free: '3次/天', vip: '无限次' },
  { icon: '📋', name: '饮食计划', free: '1次/天', vip: '无限次' },
  { icon: '📷', name: '食物识别', free: '5次/天', vip: '无限次' },
  { icon: '👩‍⚕️', name: '专属客服', free: '❌', vip: '✅ 优先' },
  { icon: '🛒', name: '商城折扣', free: '❌', vip: '✅ 专属折扣' },
  { icon: '📊', name: '深度报告', free: '❌', vip: '✅ 周/月报' },
  { icon: '🎯', name: '个性推荐', free: '基础', vip: '深度定制' },
  { icon: '🏅', name: '成长加速', free: '1x', vip: '2x 加速' }
]

// ========== Data Loading ==========
async function loadMemberInfo() {
  try {
    const res = await memberApi.getInfo()
    if (res.code === 200) {
      memberInfo.value = res.data || {}
      if (res.data?.invitationCode) invitationCode.value = res.data.invitationCode
    }
  } catch {}
}

async function loadVipStatus() {
  try {
    const res = await vipApi.getVipStatus()
    if (res.code === 200) vipStatus.value = res.data || {}
  } catch {}
}

async function loadPermissions() {
  try {
    const res = await memberApi.getPermissions()
    if (res.code === 200) permissions.value = res.data || {}
  } catch {}
}

async function loadSignInCalendar() {
  try {
    const res = await memberApi.getSignInCalendar({ year: currentYear, month: currentMonth })
    if (res.code === 200) {
      signInInfo.value = res.data || {}
      // Backend returns List<Integer> (day numbers) directly
      if (Array.isArray(res.data)) {
        signedDays.value = (res.data as number[]).filter((n: number) => !isNaN(n))
      } else if (res.data?.signedDates) {
        // Fallback: date strings like "2026-04-01"
        const dates: string[] = res.data.signedDates || []
        signedDays.value = dates.map((d: string) => {
          const parts = d.split('-')
          return parseInt(parts[parts.length - 1], 10)
        }).filter((n: number) => !isNaN(n))
      } else {
        signedDays.value = []
      }
    }
  } catch {}
}

async function loadVipPlans() {
  plansLoading.value = true
  try {
    const res = await vipApi.getPlans()
    if (res.code === 200 && res.data) {
      vipPlans.value = (res.data as any[]).map((p: any) => ({
        ...p,
        pricePerDay: p.pricePerDay || ((p.discountPrice || p.originalPrice) / (p.durationDays || 30)).toFixed(2)
      }))
      // Default select recommended or second plan
      if (vipPlans.value.length >= 2) {
        selectedPlan.value = vipPlans.value.find((p: any) => p.badge === '推荐') || vipPlans.value[1]
      } else if (vipPlans.value.length) {
        selectedPlan.value = vipPlans.value[0]
      }
    }
  } catch {} finally {
    plansLoading.value = false
  }
}

async function loadGrowthRecords(append = false) {
  if (!append) growthPage.value = 0
  try {
    const res = await memberApi.getGrowthRecords({ page: growthPage.value, size: 15 })
    if (res.code === 200) {
      const records = res.data?.content || res.data?.records || res.data?.list || res.data || []
      if (append) {
        growthRecords.value = [...growthRecords.value, ...records]
      } else {
        growthRecords.value = records
        buildGrowthChart(records)
      }
      growthHasMore.value = records.length >= 15
    }
  } catch {}
}

async function loadMoreGrowth() {
  if (growthLoadingMore.value) return
  growthLoadingMore.value = true
  growthPage.value++
  await loadGrowthRecords(true)
  growthLoadingMore.value = false
}

async function loadInvitationRecords() {
  try {
    const res = await memberApi.getInvitationRecords({ page: 0, size: 5 })
    if (res.code === 200) {
      invitationRecords.value = res.data?.content || res.data?.records || res.data?.list || res.data || []
    }
  } catch {}
}

// ========== Sign-In ==========
async function handleSignIn() {
  if (todaySigned.value) return
  try {
    const res = await memberApi.signIn()
    if (res.code === 200 && res.data > 0) {
      uni.showToast({ title: '签到成功！+10成长值', icon: 'success' })
      signedDays.value = [...signedDays.value, todayDay]
      loadSignInCalendar()
      loadGrowthRecords()
      loadMemberInfo()
    } else if (res.code === 200 && res.data === 0) {
      uni.showToast({ title: '今日已签到', icon: 'none' })
      if (!signedDays.value.includes(todayDay)) {
        signedDays.value = [...signedDays.value, todayDay]
      }
    }
  } catch (e: any) {
    uni.showToast({ title: e?.message || '签到失败，请稍后重试', icon: 'none' })
  }
}

// ========== VIP Purchase ==========
async function handlePay() {
  if (!selectedPlan.value || payLoading.value) return
  payLoading.value = true
  try {
    const res = await vipApi.createOrder({ planId: selectedPlan.value.id, payType: payType.value })
    if (res.code === 200) {
      pendingOrder.value = res.data
      payModalVisible.value = true
      startCountdown()
    } else {
      uni.showToast({ title: res.message || '创建订单失败', icon: 'none' })
    }
  } catch {
    uni.showToast({ title: '创建订单失败，请稍后重试', icon: 'none' })
  } finally {
    payLoading.value = false
  }
}

async function confirmSimulatePay() {
  if (!pendingOrder.value || simulateLoading.value) return
  simulateLoading.value = true
  try {
    const res = await vipApi.simulatePay(pendingOrder.value.orderNo)
    if (res.code === 200 && res.data?.paymentStatus === 'PAID') {
      clearCountdown()
      payModalVisible.value = false
      uni.showToast({ title: '🎉 开通成功！VIP已生效', icon: 'none' })
      refreshAll()
    } else {
      uni.showToast({ title: res.message || '模拟支付失败', icon: 'none' })
    }
  } catch {
    uni.showToast({ title: '支付失败，请稍后重试', icon: 'none' })
  } finally {
    simulateLoading.value = false
  }
}

function startCountdown() {
  if (!pendingOrder.value?.expireTime) return
  const expireMs = new Date(pendingOrder.value.expireTime).getTime()
  countdown.value = Math.max(0, Math.round((expireMs - Date.now()) / 1000))
  clearCountdown()
  countdownTimer = setInterval(() => {
    countdown.value = Math.max(0, countdown.value - 1)
    if (countdown.value === 0) clearInterval(countdownTimer)
  }, 1000)
}

function clearCountdown() {
  if (countdownTimer) { clearInterval(countdownTimer); countdownTimer = null }
}

// ========== Invitation ==========
async function generateCode() {
  try {
    const res = await memberApi.generateInvitation()
    if (res.code === 200) {
      invitationCode.value = res.data?.invitationCode || res.data?.code || ''
      uni.showToast({ title: '生成成功', icon: 'success' })
    }
  } catch {}
}

function copyCode() {
  if (!invitationCode.value) return
  uni.setClipboardData({
    data: invitationCode.value,
    success: () => uni.showToast({ title: '邀请码已复制', icon: 'success' })
  })
}

function copyLink() {
  if (!invitationLink.value) return
  uni.setClipboardData({
    data: invitationLink.value,
    success: () => uni.showToast({ title: '链接已复制', icon: 'success' })
  })
}

// ========== Benefits Detail ==========
function showBenefitsDetail(plan: any) {
  benefitsModalPlan.value = plan
  benefitsModalVisible.value = true
}

function selectFromBenefits() {
  benefitsModalVisible.value = false
  selectedPlan.value = benefitsModalPlan.value
  scrollToPlan()
}

// ========== Order History ==========
async function toggleOrderHistory() {
  showOrders.value = !showOrders.value
  if (showOrders.value && !orders.value.length) {
    ordersLoading.value = true
    try {
      const res = await vipApi.getOrders({ page: 0, size: 20 })
      if (res.code === 200) {
        orders.value = res.data?.content || res.data?.records || res.data || []
      }
    } catch {} finally {
      ordersLoading.value = false
    }
  }
}

// ========== Utility ==========
function scrollToPlan() {
  uni.pageScrollTo({ selector: '#vip-plans', duration: 300 })
}

function fmtDateTime(dt: string) {
  if (!dt) return '-'
  return new Date(dt).toLocaleString('zh-CN', {
    year: 'numeric', month: '2-digit', day: '2-digit',
    hour: '2-digit', minute: '2-digit'
  })
}

function badgeClass(badge: string) {
  return { '热门': 'badge-hot', '推荐': 'badge-recommend', '超值': 'badge-value' }[badge] || ''
}

function orderStatusClass(status: string) {
  return { PAID: 'status-paid', PENDING: 'status-pending', EXPIRED: 'status-expired', FAILED: 'status-failed' }[status] || 'status-pending'
}

function buildGrowthChart(records: any[]) {
  const dayMap: Record<string, number> = {}
  const today = new Date()
  for (let i = 6; i >= 0; i--) {
    const d = new Date(today)
    d.setDate(today.getDate() - i)
    dayMap[formatDate(d)] = 0
  }
  for (const r of records) {
    if (!r.createdAt) continue
    const dateStr = formatDate(new Date(r.createdAt))
    if (dateStr in dayMap) dayMap[dateStr] += (r.growthValue || 0)
  }
  const vals = Object.values(dayMap)
  const maxAbs = Math.max(1, ...vals.map(v => Math.abs(v)))
  growthChartData.value = Object.entries(dayMap).map(([date, value]) => ({
    label: date.slice(5),
    value,
    height: Math.max(8, Math.abs(value) / maxAbs * 120)
  }))
}

// ========== Refresh All ==========
async function refreshAll() {
  await Promise.all([
    loadMemberInfo(),
    loadVipStatus(),
    loadPermissions(),
    loadSignInCalendar(),
    loadVipPlans(),
    loadGrowthRecords(),
    loadInvitationRecords()
  ])
  loading.value = false
  userStore.fetchUserInfo()
}

// ========== Lifecycle ==========
onShow(() => {
  if (!checkLogin()) return
  loading.value = true
  refreshAll()
})
</script>

<style scoped lang="scss">
.page {
  min-height: 100vh;
  background: $background;
  padding-bottom: 30rpx;
  font-family: 'Inter', sans-serif;
}

/* ============ VIP Banner ============ */
.vip-banner {
  padding: 40rpx 30rpx;
  color: #fff;

  &.vip-active {
    background: linear-gradient(135deg, $accent, #0d9668);
    padding-bottom: 30rpx;
  }

  &.vip-inactive {
    background: $gradient-accent;
  }

  &.vip-loading {
    background: $muted;
    color: $foreground;
  }
}

.skeleton-pulse {
  animation: pulse 1.5s ease-in-out infinite;
  opacity: 0.5;
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.3; }
}

.vip-top-row {
  display: flex;
  align-items: center;
  gap: 20rpx;
  margin-bottom: 24rpx;
}

.vip-icon-big {
  font-size: 64rpx;
}

.vip-info {
  flex: 1;
}

.vip-name-row {
  display: flex;
  align-items: center;
  gap: 12rpx;
}

.vip-plan-name {
  font-size: 34rpx;
  font-weight: 700;
  font-family: 'Calistoga', cursive;
}

.vip-tag-badge {
  background: rgba(255, 255, 255, 0.2);
  color: #fff;
  font-size: 20rpx;
  font-weight: 700;
  padding: 4rpx 14rpx;
  border-radius: $radius-full;
  letter-spacing: 2rpx;
  font-family: 'JetBrains Mono', monospace;
}

.vip-username {
  font-size: 26rpx;
  opacity: 0.85;
  margin-top: 4rpx;
  display: block;
}

.quota-badge {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 4rpx;
  flex-shrink: 0;

  .quota-label {
    font-size: 20rpx;
    opacity: 0.7;
  }

  .quota-value {
    font-size: 36rpx;
    font-weight: 700;
    font-family: 'JetBrains Mono', monospace;
  }
}

.vip-detail-row {
  display: flex;
  justify-content: space-between;
  background: rgba(255, 255, 255, 0.12);
  border-radius: $radius-lg;
  padding: 20rpx 24rpx;
  margin-bottom: 20rpx;
  backdrop-filter: blur(4px);
}

.vip-detail-item {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.vip-detail-label {
  font-size: 22rpx;
  opacity: 0.75;
  margin-bottom: 6rpx;
}

.vip-detail-val {
  font-size: 28rpx;
  font-weight: 600;
  font-family: 'JetBrains Mono', monospace;

  &.gold {
    color: #FCD34D;
  }
}

.level-progress-row {
  margin-top: 4rpx;
}

.level-progress-text {
  font-size: 22rpx;
  opacity: 0.8;
  margin-bottom: 8rpx;
  display: block;
}

.progress-bar-bg {
  height: 12rpx;
  background: rgba(255, 255, 255, 0.2);
  border-radius: $radius-full;
  overflow: hidden;
}

.progress-bar-fill {
  height: 100%;
  background: #fff;
  border-radius: $radius-full;
  transition: width 0.5s;
}

/* ============ Card Layout ============ */
.card {
  background: $card;
  border-radius: $radius-xl;
  margin: 20rpx 24rpx;
  padding: 30rpx;
  border: 1rpx solid $border;
  box-shadow: $shadow-sm;
}

.card-header-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 24rpx;
}

.card-title-text {
  font-size: 30rpx;
  font-weight: 700;
  color: $foreground;
  font-family: 'Calistoga', cursive;
}

.card-header-sub {
  font-size: 22rpx;
  color: $muted-foreground;
}

.vip-expire-tag {
  color: $accent;
  font-weight: 600;
}

.toggle-text {
  font-size: 24rpx;
  color: $muted-foreground;
}

/* ============ Two-Column Grid ============ */
.engagement-grid {
  display: flex;
  gap: 0;

  .card {
    flex: 1;
    margin: 20rpx 12rpx;

    &:first-child { margin-left: 24rpx; }
    &:last-child { margin-right: 24rpx; }
  }
}

@media (max-width: 750px) {
  .engagement-grid {
    flex-direction: column;

    .card {
      margin: 20rpx 24rpx;

      &:first-child { margin-left: 24rpx; }
      &:last-child { margin-right: 24rpx; }
    }
  }
}

/* ============ Sign-In Calendar ============ */
.sign-stats {
  display: flex;
  align-items: center;
  justify-content: space-around;
  padding: 12rpx 0 16rpx;
  border-bottom: 1rpx solid $border;
  margin-bottom: 16rpx;
}

.stat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4rpx;
}

.stat-num {
  font-size: 32rpx;
  font-weight: 700;
  color: #EF4444;
  font-family: 'JetBrains Mono', monospace;
}

.stat-label {
  font-size: 20rpx;
  color: $muted-foreground;
}

.stat-divider {
  width: 1rpx;
  height: 40rpx;
  background: $border;
}

.calendar-grid {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 4rpx;
  margin-bottom: 16rpx;
}

.cal-week-label {
  text-align: center;
  font-size: 20rpx;
  color: $muted-foreground;
  padding: 4rpx 0;
  font-weight: 500;
}

.cal-day-cell {
  position: relative;
  aspect-ratio: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  border-radius: $radius-sm;
  background: $muted;
  border: 1rpx solid $border;
  transition: all 0.2s;

  &.empty {
    background: transparent;
    border-color: transparent;
  }

  &.signed {
    background: $accent;
    color: #fff;
    border-color: #0d9668;

    .cal-day-num { color: #fff; }
    .cal-check {
      font-size: 14rpx;
      color: rgba(255, 255, 255, 0.9);
    }
  }

  &.today {
    border: 2rpx solid #EF4444;
    color: #EF4444;
    background: $muted;

    .cal-day-num {
      color: #EF4444;
      font-weight: 700;
    }
  }

  &.future {
    color: #d1d5db;
    .cal-day-num { color: #d1d5db; }
  }
}

.cal-day-num {
  font-size: 20rpx;
  line-height: 1;
  color: $foreground;
}

.cal-check {
  font-size: 14rpx;
  line-height: 1;
}

.sign-btn {
  width: 100%;
  height: 72rpx;
  line-height: 72rpx;
  font-size: 26rpx;
  font-weight: 600;
  border-radius: $radius-xl;
  background: $accent;
  color: #fff;
  border: none;
  box-shadow: $shadow-accent;
  margin-bottom: 12rpx;

  &.disabled, &[disabled] {
    background: $muted !important;
    color: $muted-foreground !important;
    box-shadow: none;
  }
}

.sign-tip {
  text-align: center;
  font-size: 20rpx;
  color: $muted-foreground;
}

/* ============ Invitation Card ============ */
.invite-reward-tag {
  background: rgba(245, 158, 11, 0.12);
  color: #F59E0B;
  font-size: 20rpx;
  font-weight: 600;
  padding: 4rpx 14rpx;
  border-radius: $radius-full;
}

.invite-section {
  margin-bottom: 20rpx;
}

.invite-label {
  font-size: 22rpx;
  color: $muted-foreground;
  margin-bottom: 8rpx;
  display: block;
}

.invite-code-display {
  display: flex;
  align-items: center;
  gap: 12rpx;
  padding: 16rpx;
  background: $muted;
  border-radius: $radius-lg;
  border: 1rpx solid $accent;
}

.invite-code-text {
  flex: 1;
  font-size: 28rpx;
  font-weight: 700;
  color: $accent;
  letter-spacing: 3rpx;
  font-family: 'JetBrains Mono', monospace;
}

.invite-link-display {
  display: flex;
  align-items: center;
  gap: 12rpx;
  padding: 12rpx 16rpx;
  background: $muted;
  border-radius: $radius-lg;
  border: 1rpx solid $border;
}

.invite-link-text {
  flex: 1;
  font-size: 20rpx;
  color: $muted-foreground;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.btn-sm {
  font-size: 22rpx;
  padding: 8rpx 20rpx;
  border-radius: $radius-full;
  line-height: 1.4;
  height: auto;
  min-height: 0;
  border: none;
  flex-shrink: 0;
}

.btn-accent {
  background: $accent;
  color: #fff;
}

.btn-outline {
  background: $card;
  color: $accent;
  border: 1rpx solid $border;
}

.invite-stats {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12rpx;
  margin-bottom: 20rpx;
}

.invite-stat-box {
  display: flex;
  align-items: center;
  gap: 12rpx;
  padding: 16rpx;
  background: $muted;
  border: 1rpx solid $border;
  border-radius: $radius-lg;
}

.invite-stat-icon {
  width: 52rpx;
  height: 52rpx;
  border-radius: $radius-md;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24rpx;
  flex-shrink: 0;

  &.bg-accent { background: $accent; }
  &.bg-gold { background: #F59E0B; }
}

.invite-stat-info {
  display: flex;
  flex-direction: column;
  gap: 2rpx;
}

.invite-stat-val {
  font-size: 30rpx;
  font-weight: 700;
  color: $foreground;
  font-family: 'JetBrains Mono', monospace;
}

.invite-stat-lbl {
  font-size: 20rpx;
  color: $muted-foreground;
}

.invite-records-section {
  margin-top: 8rpx;
}

.invite-records-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12rpx;
}

.invite-records-title {
  font-size: 26rpx;
  font-weight: 600;
  color: $foreground;
}

.invite-records-list {
  .invite-record-item {
    display: flex;
    align-items: center;
    gap: 12rpx;
    padding: 12rpx 0;

    &:not(:last-child) {
      border-bottom: 1rpx solid $border;
    }
  }
}

.invite-record-avatar {
  width: 52rpx;
  height: 52rpx;
  border-radius: $radius-full;
  background: $accent;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24rpx;
  font-weight: 600;
  flex-shrink: 0;
}

.invite-record-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4rpx;
}

.invite-record-name {
  font-size: 24rpx;
  font-weight: 500;
  color: $foreground;
}

.invite-record-time {
  font-size: 20rpx;
  color: $muted-foreground;
}

.invite-record-badge {
  font-size: 20rpx;
  font-weight: 600;
  padding: 4rpx 12rpx;
  border-radius: $radius-full;
  flex-shrink: 0;

  &.rewarded {
    background: rgba(16, 185, 129, 0.1);
    color: $accent;
  }

  &.pending {
    background: $muted;
    color: $muted-foreground;
  }
}

.invite-empty {
  text-align: center;
  padding: 24rpx 0;
  font-size: 22rpx;
  color: $muted-foreground;
}

/* ============ VIP Plans ============ */
.plans-skeleton {
  display: flex;
  gap: 16rpx;
  margin-bottom: 24rpx;

  .skeleton-card {
    flex: 1;
    background: $muted;
    border-radius: $radius-xl;
    padding: 24rpx;
    animation: pulse 1.5s ease-in-out infinite;
  }

  .skeleton-line {
    height: 20rpx;
    background: $border;
    border-radius: 4rpx;

    &.w60 { width: 60%; }
    &.w40 { width: 40%; }
    &.w80 { width: 80%; }
    &.mt8 { margin-top: 16rpx; }
    &.mt16 { margin-top: 32rpx; }
  }
}

.plans-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16rpx;
  margin-bottom: 24rpx;
}

.plan-card {
  min-width: 0;
  border: 1rpx solid $border;
  border-radius: $radius-xl;
  padding: 24rpx 16rpx;
  text-align: center;
  position: relative;
  transition: all 0.2s;
  background: $card;

  &.selected {
    border-color: $accent;
    background: rgba(16, 185, 129, 0.04);
    box-shadow: $shadow-accent;
  }

  &.recommended {
    border-color: #F59E0B;
    border-width: 3rpx;
  }
}

.plan-badge {
  position: absolute;
  top: -2rpx;
  right: 16rpx;
  padding: 2rpx 12rpx;
  border-radius: 0 0 $radius-sm $radius-sm;
  font-size: 20rpx;
  font-weight: 600;
  color: #fff;

  &.badge-hot { background: #EF4444; }
  &.badge-recommend { background: $accent; }
  &.badge-value { background: $uni-success; }
}

.plan-name {
  font-size: 26rpx;
  font-weight: 700;
  color: $foreground;
  margin-bottom: 4rpx;
  font-family: 'Calistoga', cursive;
}

.plan-duration {
  font-size: 22rpx;
  color: $muted-foreground;
  margin-bottom: 12rpx;
}

.plan-price-row {
  display: flex;
  align-items: baseline;
  justify-content: center;
  gap: 8rpx;
  margin-bottom: 4rpx;
}

.plan-price-current {
  font-size: 44rpx;
  font-weight: 700;
  color: $accent;
  font-family: 'JetBrains Mono', monospace;
}

.plan-discount-label {
  font-size: 20rpx;
  color: #EF4444;
  background: rgba(239, 68, 68, 0.08);
  padding: 2rpx 8rpx;
  border-radius: 4rpx;
}

.plan-original {
  font-size: 22rpx;
  color: $muted-foreground;
  text-decoration: line-through;
  margin-bottom: 4rpx;
}

.plan-per-day {
  font-size: 22rpx;
  color: $accent-secondary;
  margin-bottom: 12rpx;
}

.plan-features-preview {
  margin-top: 12rpx;
  padding-top: 12rpx;
  border-top: 1rpx solid $border;
  text-align: left;
}

.plan-feature-item {
  display: flex;
  align-items: center;
  gap: 8rpx;
  font-size: 22rpx;
  color: $muted-foreground;
  padding: 4rpx 0;
}

.feature-check {
  color: $uni-success;
  font-weight: 700;
}

.plan-feature-more {
  font-size: 20rpx;
  color: $accent;
  padding: 4rpx 0;
}

/* ============ Pay Action ============ */
.pay-action {
  text-align: center;
}

.pay-type-row {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12rpx;
  margin-bottom: 20rpx;
}

.pay-type-label {
  font-size: 24rpx;
  color: $muted-foreground;
}

.pay-type-options {
  display: flex;
  gap: 0;
  border: 1rpx solid $border;
  border-radius: $radius-md;
  overflow: hidden;
}

.pay-type-option {
  font-size: 24rpx;
  padding: 10rpx 24rpx;
  background: $card;
  color: $muted-foreground;
  transition: all 0.2s;

  &.active {
    background: $accent;
    color: #fff;
  }
}

.pay-btn {
  width: 80%;
  height: 84rpx;
  line-height: 84rpx;
  font-size: 30rpx;
  font-weight: 600;
  border-radius: $radius-xl;
  background: $accent;
  color: #fff;
  border: none;
  box-shadow: $shadow-accent;
  margin-bottom: 12rpx;

  &.pay-btn-disabled, &[disabled] {
    background: $muted !important;
    color: $muted-foreground !important;
    box-shadow: none;
  }
}

.pay-tip {
  font-size: 22rpx;
  color: $muted-foreground;
}

/* ============ Modal Overlay ============ */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(15, 23, 42, 0.5);
  z-index: 999;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40rpx;
}

.modal-content {
  background: $card;
  border-radius: $radius-xl;
  width: 100%;
  max-width: 620rpx;
  max-height: 80vh;
  overflow-y: auto;
  box-shadow: $shadow-lg;
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 28rpx 30rpx;
  border-bottom: 1rpx solid $border;
}

.modal-title {
  font-size: 30rpx;
  font-weight: 700;
  color: $foreground;
  font-family: 'Calistoga', cursive;
}

.modal-close {
  font-size: 32rpx;
  color: $muted-foreground;
  padding: 8rpx;
}

.modal-body {
  padding: 30rpx;
}

/* Order Info Grid */
.order-info-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16rpx;
  margin-bottom: 24rpx;
}

.order-info-item {
  background: $muted;
  border-radius: $radius-md;
  padding: 16rpx;
}

.order-info-label {
  font-size: 20rpx;
  color: $muted-foreground;
  display: block;
  margin-bottom: 6rpx;
}

.order-info-value {
  font-size: 26rpx;
  font-weight: 600;
  color: $foreground;
  display: block;
  word-break: break-all;

  &.price { color: $accent; }
  &.mono { font-family: 'JetBrains Mono', monospace; font-size: 22rpx; }
}

.pay-countdown {
  text-align: center;
  font-size: 26rpx;
  color: $muted-foreground;
  margin: 16rpx 0;
}

.countdown-num {
  font-size: 36rpx;
  font-weight: 700;
  color: #F59E0B;
  font-family: 'JetBrains Mono', monospace;
}

.pay-expired {
  text-align: center;
  font-size: 26rpx;
  color: #EF4444;
  margin: 16rpx 0;
}

.pay-simulate-hint {
  background: rgba(16, 185, 129, 0.06);
  border: 1rpx solid rgba(16, 185, 129, 0.2);
  border-radius: $radius-md;
  padding: 16rpx;
  font-size: 22rpx;
  color: $muted-foreground;
  margin: 16rpx 0;
}

.modal-btns {
  display: flex;
  gap: 16rpx;
  margin-top: 24rpx;
}

.btn-confirm {
  flex: 1;
  height: 80rpx;
  line-height: 80rpx;
  background: $accent;
  color: #fff;
  border: none;
  border-radius: $radius-xl;
  font-size: 28rpx;
  font-weight: 600;
  box-shadow: $shadow-accent;

  &[disabled] {
    background: $muted !important;
    color: $muted-foreground !important;
    box-shadow: none;
  }
}

.btn-cancel {
  flex: 0 0 auto;
  height: 80rpx;
  line-height: 80rpx;
  background: $muted;
  color: $muted-foreground;
  border: 1rpx solid $border;
  border-radius: $radius-xl;
  font-size: 28rpx;
  padding: 0 40rpx;
}

.pay-hint-small {
  text-align: center;
  font-size: 20rpx;
  color: $muted-foreground;
  margin-top: 16rpx;
}

/* Benefits Full List */
.benefits-full-list {
  margin-bottom: 16rpx;
}

.benefits-full-item {
  display: flex;
  align-items: center;
  gap: 12rpx;
  padding: 16rpx 0;
  font-size: 26rpx;
  color: $foreground;
  border-bottom: 1rpx solid $border;

  &:last-child { border-bottom: none; }
}

.benefits-quota-box {
  background: $muted;
  border: 1rpx solid $border;
  border-radius: $radius-md;
  padding: 20rpx;
  display: flex;
  flex-direction: column;
  gap: 8rpx;
  font-size: 24rpx;
  color: $muted-foreground;
  margin-bottom: 16rpx;
}

.mt20 {
  margin-top: 24rpx;
}

/* ============ Growth Records ============ */
.growth-chart {
  margin-bottom: 24rpx;
  padding-bottom: 16rpx;
  border-bottom: 1rpx solid $border;
}

.chart-bars {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  height: 180rpx;
  padding: 0 8rpx;
}

.chart-bar-col {
  display: flex;
  flex-direction: column;
  align-items: center;
  flex: 1;
  gap: 6rpx;
}

.chart-bar-val {
  font-size: 20rpx;
  color: $accent;
  font-weight: 600;
  font-family: 'JetBrains Mono', monospace;
}

.chart-bar {
  width: 40rpx;
  min-height: 8rpx;
  background: $accent;
  border-radius: $radius-sm $radius-sm 0 0;
  transition: height 0.3s;

  &.negative {
    background: $accent-secondary;
  }
}

.chart-bar-label {
  font-size: 20rpx;
  color: $muted-foreground;
  white-space: nowrap;
}

.growth-list {
  max-height: 600rpx;
  overflow-y: auto;
}

.growth-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16rpx 0;
  border-bottom: 1rpx solid $border;

  &:last-child { border-bottom: none; }
}

.growth-item-left {
  flex: 1;
}

.growth-desc {
  font-size: 26rpx;
  color: $foreground;
  display: block;
}

.growth-time {
  font-size: 22rpx;
  color: $muted-foreground;
  display: block;
  margin-top: 6rpx;
}

.growth-points {
  font-size: 32rpx;
  font-weight: 700;
  font-family: 'JetBrains Mono', monospace;
  flex-shrink: 0;
  margin-left: 16rpx;

  &.positive { color: $accent; }
  &.negative { color: $uni-error; }
}

.load-more {
  text-align: center;
  padding: 20rpx 0;
  font-size: 24rpx;
  color: $accent;
}

/* ============ Benefits Comparison ============ */
.benefits-compare {
  margin-top: 8rpx;
}

.compare-row {
  display: flex;
  align-items: center;
  border-bottom: 1rpx solid $border;

  &.compare-header {
    background: $muted;
    border-radius: $radius-md $radius-md 0 0;
    font-weight: 600;
    font-size: 22rpx;
    color: $foreground;
  }

  &:last-child { border-bottom: none; }
}

.compare-cell {
  flex: 1;
  padding: 16rpx 12rpx;
  font-size: 22rpx;
  text-align: center;
  color: $muted-foreground;

  &.name-cell {
    flex: 1.5;
    text-align: left;
    display: flex;
    align-items: center;
    gap: 8rpx;
    color: $foreground;
    font-weight: 500;
  }

  &.vip-cell {
    color: #F59E0B;
    font-weight: 600;
  }

  &.vip-val {
    color: $accent;
    font-weight: 600;
  }

  &.free-val {
    color: $muted-foreground;
  }
}

.compare-icon {
  font-size: 28rpx;
}

/* ============ Order History ============ */
.order-list {
  margin-top: 8rpx;
}

.order-item {
  padding: 20rpx 0;
  border-bottom: 1rpx solid $border;

  &:last-child { border-bottom: none; }
}

.order-item-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10rpx;
}

.order-plan-name {
  font-size: 28rpx;
  font-weight: 600;
  color: $foreground;
}

.order-status-tag {
  font-size: 20rpx;
  font-weight: 600;
  padding: 4rpx 14rpx;
  border-radius: $radius-full;

  &.status-paid { background: rgba(16, 185, 129, 0.1); color: $accent; }
  &.status-pending { background: rgba(245, 158, 11, 0.1); color: #F59E0B; }
  &.status-expired { background: $muted; color: $muted-foreground; }
  &.status-failed { background: rgba(239, 68, 68, 0.1); color: #EF4444; }
}

.order-item-details {
  display: flex;
  flex-direction: column;
  gap: 4rpx;
}

.order-detail-text {
  font-size: 22rpx;
  color: $muted-foreground;
}

/* ============ Shared ============ */
.empty-state {
  text-align: center;
  padding: 40rpx 0;
}

.empty-icon {
  font-size: 60rpx;
  display: block;
  margin-bottom: 12rpx;
}

.empty-text {
  font-size: 26rpx;
  color: $muted-foreground;
}

.safe-bottom {
  height: env(safe-area-inset-bottom);
}
</style>
