<template>
  <view class="page">
    <!-- VIP Status Banner -->
    <view v-if="isVip" class="vip-banner vip-active">
      <view class="vip-top-row">
        <text class="vip-icon-big">👑</text>
        <view class="vip-info">
          <view class="vip-name-row">
            <text class="vip-plan-name">{{ vipStatus.planName || vipLevelText }}</text>
            <view class="vip-tag-badge">VIP</view>
          </view>
          <text class="vip-username">{{ memberInfo.username || '会员用户' }}</text>
        </view>
      </view>
      <view class="vip-detail-row">
        <view class="vip-detail-item">
          <text class="vip-detail-label">到期时间</text>
          <text class="vip-detail-val">{{ vipExpiry }}</text>
        </view>
        <view class="vip-detail-item">
          <text class="vip-detail-label">剩余天数</text>
          <text class="vip-detail-val accent">{{ remainingDays }}天</text>
        </view>
        <view class="vip-detail-item">
          <text class="vip-detail-label">会员等级</text>
          <text class="vip-detail-val">{{ memberInfo.currentLevel?.levelName || '青铜会员' }}</text>
        </view>
      </view>
      <!-- Level Progress -->
      <view v-if="memberInfo.nextLevel" class="level-progress-row">
        <text class="level-progress-text">距{{ memberInfo.nextLevel.levelName }}还需 {{ memberInfo.growthToNextLevel || 0 }} 成长值</text>
        <view class="progress-bar-bg">
          <view class="progress-bar-fill" :style="{ width: (memberInfo.upgradeProgress || 0) + '%' }"></view>
        </view>
      </view>
    </view>
    <view v-else class="vip-banner vip-inactive" @tap="scrollToPlan">
      <view class="vip-badge">💎 开通VIP享更多权益</view>
      <view class="vip-hint">解锁AI饮食计划、无限对话等专属功能</view>
    </view>

    <!-- Sign-In Card -->
    <view class="card sign-card">
      <view class="card-title flex-between">
        <text class="font-bold">每日签到</text>
        <text class="text-sm text-secondary">已连续签到 {{ signInInfo.consecutiveDays || 0 }} 天</text>
      </view>
      <view class="calendar-week">
        <view
          v-for="(day, idx) in weekDays"
          :key="idx"
          class="calendar-day"
          :class="{ signed: day.signed, today: day.isToday }"
        >
          <text class="day-label">{{ day.label }}</text>
          <view class="day-dot">{{ day.signed ? '✓' : day.isToday ? '●' : '' }}</view>
        </view>
      </view>
      <button
        class="btn-primary sign-btn"
        :disabled="signInInfo.signedToday"
        @tap="handleSignIn"
      >
        {{ signInInfo.signedToday ? '今日已签到' : '签到 +' + (signInInfo.growthPoints || 5) + '成长值' }}
      </button>
    </view>

    <!-- VIP Plans -->
    <view id="vip-plans" class="card">
      <view class="card-title font-bold">开通VIP会员</view>
      <view class="plan-list">
        <view
          v-for="plan in vipPlans"
          :key="plan.id"
          class="plan-card"
          :class="{ selected: selectedPlan?.id === plan.id, recommended: plan.recommended }"
          @tap="selectedPlan = plan"
        >
          <view v-if="plan.recommended" class="plan-tag">推荐</view>
          <view class="plan-name">{{ plan.name }}</view>
          <view class="plan-price">
            <text class="price-symbol">¥</text>
            <text class="price-value">{{ plan.price }}</text>
          </view>
          <view class="plan-original" v-if="plan.originalPrice">
            原价 ¥{{ plan.originalPrice }}
          </view>
          <view class="plan-daily">约 ¥{{ plan.dailyCost }}/天</view>
        </view>
      </view>

      <!-- Plan Features -->
      <view v-if="selectedPlan" class="plan-features">
        <view class="feature-item" v-for="(feat, i) in selectedPlan.features" :key="i">
          <text class="feature-check">✓</text>
          <text>{{ feat }}</text>
        </view>
      </view>

      <button class="btn-primary mt-20" :loading="purchasing" @tap="handlePurchase">
        {{ selectedPlan ? '立即开通 ¥' + selectedPlan.price : '请选择套餐' }}
      </button>
    </view>

    <!-- Growth Center -->
    <view class="card">
      <view class="card-title flex-between">
        <text class="font-bold">成长中心</text>
        <text class="text-primary text-sm">总成长值：{{ memberInfo.totalGrowth || 0 }}</text>
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
        <view v-for="record in growthRecords" :key="record.id" class="growth-item flex-between">
          <view>
            <text class="growth-desc">{{ record.description }}</text>
            <text class="growth-time text-sm text-secondary">{{ formatTime(record.createdAt) }}</text>
          </view>
          <text class="growth-points" :class="record.growthValue > 0 ? 'text-primary' : 'text-danger'">
            {{ record.growthValue > 0 ? '+' : '' }}{{ record.growthValue }}
          </text>
        </view>
      </view>
      <view v-else class="empty-state">
        <text class="empty-icon">📊</text>
        <text class="empty-text">暂无成长记录</text>
      </view>
    </view>

    <!-- Invitation -->
    <view class="card">
      <view class="card-title font-bold">邀请有礼</view>
      <view class="invite-code-box flex-between">
        <view>
          <text class="text-sm text-secondary">我的邀请码</text>
          <view class="invite-code">{{ invitationCode || '点击生成' }}</view>
        </view>
        <view class="flex">
          <button v-if="!invitationCode" class="btn-small btn-outline-sm" @tap="generateCode">生成</button>
          <button v-else class="btn-small btn-outline-sm" @tap="copyCode">复制</button>
        </view>
      </view>
      <view v-if="invitationRecords.length" class="mt-20">
        <view class="text-sm text-secondary mb-10">邀请记录</view>
        <view v-for="rec in invitationRecords" :key="rec.id" class="invite-record flex-between">
          <text>{{ rec.inviteeName || '用户' + rec.inviteeId }}</text>
          <text class="text-sm text-secondary">{{ formatTime(rec.createdAt) }}</text>
        </view>
      </view>
    </view>

    <!-- VIP Benefits -->
    <view class="card">
      <view class="card-title flex-between" @tap="showBenefits = !showBenefits">
        <text class="font-bold">VIP权益详情</text>
        <text class="text-secondary">{{ showBenefits ? '收起' : '展开' }}</text>
      </view>
      <view v-if="showBenefits" class="benefits-list">
        <view v-for="(b, i) in vipBenefits" :key="i" class="benefit-item">
          <text class="benefit-icon">{{ b.icon }}</text>
          <view class="benefit-info">
            <text class="benefit-name">{{ b.name }}</text>
            <text class="benefit-desc text-sm text-secondary">{{ b.desc }}</text>
          </view>
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

const memberInfo = ref<any>({})
const vipStatus = ref<any>({})
const signInInfo = ref<any>({})
const weekDays = ref<any[]>([])
const vipPlans = ref<any[]>([])
const selectedPlan = ref<any>(null)
const purchasing = ref(false)
const growthRecords = ref<any[]>([])
const growthChartData = ref<any[]>([])
const invitationCode = ref('')
const invitationRecords = ref<any[]>([])
const showBenefits = ref(false)

const vipLevelMap: Record<string, string> = {
  MONTHLY: '月度VIP',
  QUARTERLY: '季度VIP',
  HALF_YEARLY: '半年VIP',
  YEARLY: '年度VIP',
  BIANNUAL: '双年VIP',
  LIFETIME: '终身VIP'
}

const isVip = computed(() => {
  return vipStatus.value.isVip === true
})

const vipLevelText = computed(() => vipLevelMap[memberInfo.value.vipLevel] || vipStatus.value.planName || '普通用户')

const vipExpiry = computed(() => {
  const expiry = vipStatus.value.vipExpireAt || memberInfo.value.vipExpiry
  if (!expiry) return ''
  return formatDate(new Date(expiry))
})

const remainingDays = computed(() => {
  return vipStatus.value.remainDays || 0
})

const vipBenefits = [
  { icon: '🤖', name: 'AI对话次数', desc: 'VIP每日享更多AI营养师对话次数' },
  { icon: '📋', name: 'AI饮食计划', desc: '无限生成个性化AI饮食计划' },
  { icon: '📷', name: '食物识别', desc: '无限次AI食物识别与营养分析' },
  { icon: '👩‍⚕️', name: '专属客服', desc: '优先营养师咨询与专属服务' },
  { icon: '🛒', name: '商城折扣', desc: '营养商城商品享专属VIP折扣' },
  { icon: '📊', name: '深度报告', desc: '每周/月度营养摄入深度分析报告' }
]

function scrollToPlan() {
  uni.pageScrollTo({ selector: '#vip-plans', duration: 300 })
}

function buildWeekDays(signedDates: string[] = []) {
  const today = new Date()
  const dayOfWeek = today.getDay() || 7
  const labels = ['一', '二', '三', '四', '五', '六', '日']
  const days: any[] = []
  for (let i = 1; i <= 7; i++) {
    const d = new Date(today)
    d.setDate(today.getDate() - dayOfWeek + i)
    const dateStr = formatDate(d)
    days.push({
      label: labels[i - 1],
      date: dateStr,
      signed: signedDates.includes(dateStr),
      isToday: dateStr === formatDate(today)
    })
  }
  weekDays.value = days
}

function buildGrowthChart(records: any[]) {
  const dayMap: Record<string, number> = {}
  const today = new Date()
  // Init last 7 days
  for (let i = 6; i >= 0; i--) {
    const d = new Date(today)
    d.setDate(today.getDate() - i)
    dayMap[formatDate(d)] = 0
  }
  // Accumulate from records
  for (const r of records) {
    if (!r.createdAt) continue
    const dateStr = formatDate(new Date(r.createdAt))
    if (dateStr in dayMap) {
      dayMap[dateStr] += (r.growthValue || 0)
    }
  }
  const vals = Object.values(dayMap)
  const maxAbs = Math.max(1, ...vals.map(v => Math.abs(v)))
  const chartData = Object.entries(dayMap).map(([date, value]) => ({
    label: date.slice(5), // MM-DD
    value,
    height: Math.max(8, Math.abs(value) / maxAbs * 120)
  }))
  growthChartData.value = chartData
}

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

async function loadSignInCalendar() {
  try {
    const now = new Date()
    const res = await memberApi.getSignInCalendar({ year: now.getFullYear(), month: now.getMonth() + 1 })
    if (res.code === 200) {
      signInInfo.value = res.data || {}
      buildWeekDays(res.data?.signedDates || [])
    } else {
      buildWeekDays()
    }
  } catch {
    buildWeekDays()
  }
}

async function loadVipPlans() {
  try {
    const res = await vipApi.getPlans()
    if (res.code === 200 && res.data) {
      const plans = (res.data as any[]).map((p: any) => ({
        ...p,
        name: p.planName,
        price: p.discountPrice || p.originalPrice,
        recommended: p.planCode === 'QUARTERLY',
        dailyCost: p.pricePerDay || ((p.discountPrice || p.originalPrice) / (p.durationDays || 30)).toFixed(2),
        features: (p.benefits && p.benefits.features) || p.features || []
      }))
      vipPlans.value = plans
      selectedPlan.value = plans.find((p: any) => p.recommended) || plans[0]
    }
  } catch {}
}

async function loadGrowthRecords() {
  try {
    // Spring Pageable is 0-indexed: page 0 = first page
    const res = await memberApi.getGrowthRecords({ page: 0, size: 20 })
    if (res.code === 200) {
      const records = res.data?.content || res.data?.records || res.data?.list || res.data || []
      growthRecords.value = records
      buildGrowthChart(records)
    }
  } catch {}
}

async function handleSignIn() {
  if (signInInfo.value.signedToday) return
  try {
    const res = await memberApi.signIn()
    if (res.code === 200) {
      uni.showToast({ title: '签到成功！', icon: 'success' })
      signInInfo.value.signedToday = true
      loadSignInCalendar()
      loadGrowthRecords()
      loadMemberInfo()
    }
  } catch {}
}

async function handlePurchase() {
  if (!selectedPlan.value || purchasing.value) return
  purchasing.value = true
  try {
    const orderRes = await vipApi.createOrder({ planId: selectedPlan.value.id })
    if (orderRes.code === 200) {
      const orderNo = orderRes.data?.orderNo
      const payRes = await vipApi.simulatePay(orderNo)
      if (payRes.code === 200) {
        uni.showToast({ title: '开通成功！', icon: 'success' })
        loadMemberInfo()
        loadVipStatus()
        userStore.fetchUserInfo()
      } else {
        uni.showToast({ title: payRes.message || '支付失败', icon: 'none' })
      }
    } else {
      uni.showToast({ title: orderRes.message || '创建订单失败', icon: 'none' })
    }
  } catch {
    uni.showToast({ title: '操作失败，请重试', icon: 'none' })
  } finally {
    purchasing.value = false
  }
}

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
    success: () => uni.showToast({ title: '已复制', icon: 'success' })
  })
}

async function loadInvitationRecords() {
  try {
    const res = await memberApi.getInvitationRecords({ page: 0, size: 10 })
    if (res.code === 200) invitationRecords.value = res.data?.content || res.data?.records || res.data?.list || res.data || []
  } catch {}
}

onShow(() => {
  if (!checkLogin()) return
  loadMemberInfo()
  loadVipStatus()
  loadSignInCalendar()
  loadVipPlans()
  loadGrowthRecords()
  loadInvitationRecords()
})
</script>

<style scoped lang="scss">
.page {
  min-height: 100vh;
  background: $background;
  padding-bottom: 30rpx;
  font-family: 'Inter', sans-serif;
}

/* VIP Banner */
.vip-banner {
  padding: 40rpx 30rpx;
  color: #fff;

  &.vip-active {
    background: $gradient-accent;
    padding-bottom: 30rpx;
  }

  &.vip-inactive {
    background: $gradient-accent;
  }
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

  &.accent {
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

.vip-badge {
  font-size: 36rpx;
  font-weight: 700;
  margin-bottom: 10rpx;
  font-family: 'Calistoga', cursive;
}

.vip-hint {
  font-size: 26rpx;
  opacity: 0.85;
  margin-top: 4rpx;
}

/* Card */
.card {
  background: $card;
  border-radius: $radius-xl;
  margin: 20rpx 24rpx;
  padding: 30rpx;
  border: 1rpx solid $border;
  box-shadow: $shadow-sm;
}

.card-title {
  font-size: 32rpx;
  margin-bottom: 24rpx;
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-family: 'Calistoga', cursive;
}

/* Sign-In */
.sign-card .calendar-week {
  display: flex;
  justify-content: space-between;
  margin-bottom: 24rpx;
}

.calendar-day {
  display: flex;
  flex-direction: column;
  align-items: center;
  width: 72rpx;

  .day-label {
    font-size: 24rpx;
    color: $muted-foreground;
    margin-bottom: 10rpx;
  }

  .day-dot {
    width: 56rpx;
    height: 56rpx;
    border-radius: $radius-full;
    display: flex;
    align-items: center;
    justify-content: center;
    background: $muted;
    font-size: 28rpx;
    color: $border;
  }

  &.signed .day-dot {
    background: $accent;
    color: #fff;
  }

  &.today .day-dot {
    border: 2rpx solid $accent;
    color: $accent;
    background: rgba(0, 82, 255, 0.06);
  }
}

.sign-btn {
  height: 80rpx;
  line-height: 80rpx;
  font-size: 30rpx;
  border-radius: $radius-xl;
  background: $accent;
  color: #fff;
  border: none;
  box-shadow: $shadow-accent;
  font-family: 'Inter', sans-serif;

  &[disabled] {
    background: $muted !important;
    color: $muted-foreground !important;
    box-shadow: none;
  }
}

/* Plans */
.plan-list {
  display: flex;
  flex-wrap: wrap;
  gap: 16rpx;
}

.plan-card {
  flex: 1 1 calc(33.33% - 16rpx);
  min-width: 180rpx;
  max-width: calc(50% - 8rpx);
  border: 1rpx solid $border;
  border-radius: $radius-xl;
  padding: 24rpx 16rpx;
  text-align: center;
  position: relative;
  transition: all 0.2s;
  background: $card;

  &.selected {
    border-color: $accent;
    background: rgba(0, 82, 255, 0.04);
    box-shadow: $shadow-accent;
  }

  &.recommended {
    border-color: $accent;
    border-width: 3rpx;
  }
}

.plan-tag {
  position: absolute;
  top: -2rpx;
  right: -2rpx;
  background: $accent;
  color: #fff;
  font-size: 20rpx;
  padding: 4rpx 14rpx;
  border-radius: 0 $radius-xl 0 $radius-lg;
  font-family: 'JetBrains Mono', monospace;
}

.plan-name {
  font-size: 26rpx;
  font-weight: 600;
  margin-bottom: 12rpx;
  color: $foreground;
  font-family: 'Calistoga', cursive;
}

.plan-price {
  color: $accent;
  margin-bottom: 6rpx;
}

.price-symbol {
  font-size: 24rpx;
}

.price-value {
  font-size: 44rpx;
  font-weight: 700;
  font-family: 'JetBrains Mono', monospace;
}

.plan-original {
  font-size: 22rpx;
  color: $muted-foreground;
  text-decoration: line-through;
  margin-bottom: 6rpx;
}

.plan-daily {
  font-size: 22rpx;
  color: $accent-secondary;
  font-family: 'Inter', sans-serif;
}

.plan-features {
  margin-top: 24rpx;
  padding-top: 20rpx;
  border-top: 1rpx solid $border;
}

.feature-item {
  display: flex;
  align-items: center;
  gap: 10rpx;
  font-size: 26rpx;
  color: $muted-foreground;
  padding: 8rpx 0;
  font-family: 'Inter', sans-serif;
}

.feature-check {
  color: $uni-success;
  font-weight: 700;
}

/* Growth Chart */
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

/* Growth List */
.growth-list {
  max-height: 500rpx;
  overflow-y: auto;
}

.growth-item {
  padding: 16rpx 0;
  border-bottom: 1rpx solid $border;

  &:last-child {
    border-bottom: none;
  }
}

.growth-desc {
  font-size: 28rpx;
  color: $foreground;
  display: block;
  font-family: 'Inter', sans-serif;
}

.growth-time {
  display: block;
  margin-top: 6rpx;
  color: $muted-foreground;
}

.growth-points {
  font-size: 32rpx;
  font-weight: 700;
  font-family: 'JetBrains Mono', monospace;
}

/* Invitation */
.invite-code-box {
  background: $muted;
  border-radius: $radius-lg;
  padding: 24rpx;
}

.invite-code {
  font-size: 36rpx;
  font-weight: 700;
  color: $accent;
  margin-top: 8rpx;
  letter-spacing: 4rpx;
  font-family: 'JetBrains Mono', monospace;
}

.btn-small {
  font-size: 24rpx;
  padding: 10rpx 24rpx;
  border-radius: $radius-full;
  line-height: 1.4;
  height: auto;
  min-height: 0;
  border: none;
  font-family: 'Inter', sans-serif;
}

.btn-outline-sm {
  background: $card;
  color: $accent;
  border: 1rpx solid $border;
  box-shadow: $shadow-sm;
}

.invite-record {
  padding: 14rpx 0;
  border-bottom: 1rpx solid $border;
  font-size: 28rpx;
  color: $foreground;

  &:last-child {
    border-bottom: none;
  }
}

.mb-10 {
  margin-bottom: 10rpx;
}

/* Benefits */
.benefits-list {
  margin-top: 10rpx;
}

.benefit-item {
  display: flex;
  align-items: center;
  gap: 20rpx;
  padding: 18rpx 0;
  border-bottom: 1rpx solid $border;

  &:last-child {
    border-bottom: none;
  }
}

.benefit-icon {
  font-size: 44rpx;
  width: 72rpx;
  height: 72rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  background: $muted;
  border-radius: $radius-lg;
}

.benefit-info {
  flex: 1;
}

.benefit-name {
  font-size: 28rpx;
  font-weight: 600;
  color: $foreground;
  display: block;
  font-family: 'Calistoga', cursive;
}

.benefit-desc {
  display: block;
  margin-top: 4rpx;
  color: $muted-foreground;
}

/* Override global button style for this page */
.btn-primary {
  background: $accent;
  color: #fff;
  border: none;
  border-radius: $radius-xl;
  box-shadow: $shadow-accent;
  font-family: 'Inter', sans-serif;
}

.text-primary { color: $accent !important; }
.text-danger { color: $uni-error !important; }

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
  font-size: 28rpx;
  color: $muted-foreground;
}

.safe-bottom {
  height: env(safe-area-inset-bottom);
}
</style>
