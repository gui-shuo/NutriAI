<template>
  <view class="page">
    <!-- VIP Status Banner -->
    <view v-if="isVip" class="vip-banner vip-active">
      <view class="vip-badge">👑 {{ vipLevelText }}</view>
      <view class="vip-expiry">
        到期时间：{{ vipExpiry }}
        <text class="remaining">（剩余{{ remainingDays }}天）</text>
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
      <view v-if="growthRecords.length" class="growth-list">
        <view v-for="record in growthRecords" :key="record.id" class="growth-item flex-between">
          <view>
            <text class="growth-desc">{{ record.description }}</text>
            <text class="growth-time text-sm text-secondary">{{ formatTime(record.createdAt) }}</text>
          </view>
          <text class="growth-points" :class="record.points > 0 ? 'text-primary' : 'text-danger'">
            {{ record.points > 0 ? '+' : '' }}{{ record.points }}
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
import { ref, computed, onMounted } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { useUserStore } from '@/stores/user'
import { memberApi, vipApi } from '@/services/api'
import { checkLogin, formatTime, formatDate } from '@/utils/common'

const userStore = useUserStore()

const memberInfo = ref<any>({})
const signInInfo = ref<any>({})
const weekDays = ref<any[]>([])
const vipPlans = ref<any[]>([])
const selectedPlan = ref<any>(null)
const purchasing = ref(false)
const growthRecords = ref<any[]>([])
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
  const info = memberInfo.value
  return info.vipLevel && info.vipLevel !== 'NORMAL' && info.vipExpiry && new Date(info.vipExpiry) > new Date()
})

const vipLevelText = computed(() => vipLevelMap[memberInfo.value.vipLevel] || '普通用户')

const vipExpiry = computed(() => {
  if (!memberInfo.value.vipExpiry) return ''
  return formatDate(new Date(memberInfo.value.vipExpiry))
})

const remainingDays = computed(() => {
  if (!memberInfo.value.vipExpiry) return 0
  const diff = new Date(memberInfo.value.vipExpiry).getTime() - Date.now()
  return Math.max(0, Math.ceil(diff / 86400000))
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

async function loadMemberInfo() {
  try {
    const res = await memberApi.getInfo()
    if (res.code === 200) memberInfo.value = res.data || {}
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
    const res = await memberApi.getGrowthRecords({ page: 1, size: 10 })
    if (res.code === 200) growthRecords.value = res.data?.content || res.data?.records || res.data?.list || res.data || []
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
    const res = await memberApi.getInvitationRecords({ page: 1, size: 10 })
    if (res.code === 200) invitationRecords.value = res.data?.content || res.data?.records || res.data?.list || res.data || []
  } catch {}
}

onShow(() => {
  if (!checkLogin()) return
  loadMemberInfo()
  loadSignInCalendar()
  loadVipPlans()
  loadGrowthRecords()
  loadInvitationRecords()
})
</script>

<style scoped lang="scss">
.page {
  min-height: 100vh;
  background: #f5f5f5;
  padding-bottom: 30rpx;
}

.vip-banner {
  padding: 40rpx 30rpx;
  color: #fff;

  &.vip-active {
    background: linear-gradient(135deg, #d4a248, #b8860b, #c89632);
  }

  &.vip-inactive {
    background: linear-gradient(135deg, #07c160, #06ad56);
  }
}

.vip-badge {
  font-size: 36rpx;
  font-weight: 700;
  margin-bottom: 10rpx;
}

.vip-expiry {
  font-size: 26rpx;
  opacity: 0.9;
}

.remaining {
  font-size: 24rpx;
}

.vip-hint {
  font-size: 26rpx;
  opacity: 0.85;
  margin-top: 4rpx;
}

.card {
  background: #fff;
  border-radius: 16rpx;
  margin: 20rpx 24rpx;
  padding: 30rpx;
  box-shadow: 0 2rpx 12rpx rgba(0, 0, 0, 0.04);
}

.card-title {
  font-size: 32rpx;
  margin-bottom: 24rpx;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

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
    color: #999;
    margin-bottom: 10rpx;
  }

  .day-dot {
    width: 56rpx;
    height: 56rpx;
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    background: #f5f5f5;
    font-size: 28rpx;
    color: #ccc;
  }

  &.signed .day-dot {
    background: #07c160;
    color: #fff;
  }

  &.today .day-dot {
    border: 2rpx solid #07c160;
    color: #07c160;
  }
}

.sign-btn {
  height: 80rpx;
  line-height: 80rpx;
  font-size: 30rpx;
  border-radius: 40rpx;

  &[disabled] {
    background: #ccc !important;
    opacity: 0.7;
  }
}

.plan-list {
  display: flex;
  flex-wrap: wrap;
  gap: 16rpx;
}

.plan-card {
  flex: 1 1 calc(33.33% - 16rpx);
  min-width: 180rpx;
  max-width: calc(50% - 8rpx);
  border: 2rpx solid #eee;
  border-radius: 16rpx;
  padding: 24rpx 16rpx;
  text-align: center;
  position: relative;
  transition: all 0.2s;

  &.selected {
    border-color: #07c160;
    background: #f0faf4;
  }

  &.recommended {
    border-color: #ff976a;
  }
}

.plan-tag {
  position: absolute;
  top: -2rpx;
  right: -2rpx;
  background: #ff976a;
  color: #fff;
  font-size: 20rpx;
  padding: 4rpx 14rpx;
  border-radius: 0 16rpx 0 12rpx;
}

.plan-name {
  font-size: 26rpx;
  font-weight: 600;
  margin-bottom: 12rpx;
  color: #333;
}

.plan-price {
  color: #ee0a24;
  margin-bottom: 6rpx;
}

.price-symbol {
  font-size: 24rpx;
}

.price-value {
  font-size: 44rpx;
  font-weight: 700;
}

.plan-original {
  font-size: 22rpx;
  color: #999;
  text-decoration: line-through;
  margin-bottom: 6rpx;
}

.plan-daily {
  font-size: 22rpx;
  color: #07c160;
}

.plan-features {
  margin-top: 24rpx;
  padding-top: 20rpx;
  border-top: 1rpx solid #eee;
}

.feature-item {
  display: flex;
  align-items: center;
  gap: 10rpx;
  font-size: 26rpx;
  color: #666;
  padding: 8rpx 0;
}

.feature-check {
  color: #07c160;
  font-weight: 700;
}

.growth-list {
  max-height: 500rpx;
  overflow-y: auto;
}

.growth-item {
  padding: 16rpx 0;
  border-bottom: 1rpx solid #f5f5f5;

  &:last-child {
    border-bottom: none;
  }
}

.growth-desc {
  font-size: 28rpx;
  color: #333;
  display: block;
}

.growth-time {
  display: block;
  margin-top: 6rpx;
}

.growth-points {
  font-size: 32rpx;
  font-weight: 700;
}

.invite-code-box {
  background: #f7f8fa;
  border-radius: 12rpx;
  padding: 24rpx;
}

.invite-code {
  font-size: 36rpx;
  font-weight: 700;
  color: #07c160;
  margin-top: 8rpx;
  letter-spacing: 4rpx;
}

.btn-small {
  font-size: 24rpx;
  padding: 10rpx 24rpx;
  border-radius: 30rpx;
  line-height: 1.4;
  height: auto;
  min-height: 0;
}

.btn-outline-sm {
  background: #fff;
  color: #07c160;
  border: 2rpx solid #07c160;
}

.invite-record {
  padding: 14rpx 0;
  border-bottom: 1rpx solid #f5f5f5;
  font-size: 28rpx;

  &:last-child {
    border-bottom: none;
  }
}

.mb-10 {
  margin-bottom: 10rpx;
}

.benefits-list {
  margin-top: 10rpx;
}

.benefit-item {
  display: flex;
  align-items: center;
  gap: 20rpx;
  padding: 18rpx 0;
  border-bottom: 1rpx solid #f5f5f5;

  &:last-child {
    border-bottom: none;
  }
}

.benefit-icon {
  font-size: 44rpx;
}

.benefit-info {
  flex: 1;
}

.benefit-name {
  font-size: 28rpx;
  font-weight: 600;
  color: #333;
  display: block;
}

.benefit-desc {
  display: block;
  margin-top: 4rpx;
}

.safe-bottom {
  height: env(safe-area-inset-bottom);
}
</style>
