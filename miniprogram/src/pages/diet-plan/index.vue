<template>
  <view class="container">
    <!-- VIP Gate -->
    <view v-if="!isVip && !loading" class="vip-gate card">
      <view class="vip-icon">👑</view>
      <text class="vip-title">VIP 专属功能</text>
      <text class="vip-desc">AI 饮食计划为VIP会员专属功能，升级VIP即可解锁个性化饮食方案</text>
      <button class="btn-primary vip-btn" @tap="goVip">立即开通 VIP</button>
    </view>

    <!-- Main Content -->
    <view v-if="isVip">
      <view class="disclaimer-tip" v-if="showDisclaimer">
        <text>⚕️ AI饮食计划仅供参考，不能替代专业营养师或医生的建议。患有疾病者请遵医嘱。</text>
        <text class="dismiss" @tap="showDisclaimer = false">✕</text>
      </view>
      <!-- Toggle: Form / Result / History -->
      <view class="tab-bar flex">
        <view
          class="tab-item flex-1"
          :class="{ active: currentTab === 'form' }"
          @tap="currentTab = 'form'"
        >生成计划</view>
        <view
          class="tab-item flex-1"
          :class="{ active: currentTab === 'result' }"
          @tap="currentTab = 'result'"
          v-if="planResult"
        >当前计划</view>
        <view
          class="tab-item flex-1"
          :class="{ active: currentTab === 'history' }"
          @tap="currentTab = 'history'"
        >历史记录</view>
      </view>

      <!-- Form -->
      <view v-show="currentTab === 'form'" class="form-section">
        <view class="card">
          <view class="form-title">📋 填写基本信息</view>

          <view class="input-group">
            <text class="label">身高 (cm)</text>
            <input type="digit" v-model="form.height" placeholder="请输入身高" />
          </view>

          <view class="input-group">
            <text class="label">体重 (kg)</text>
            <input type="digit" v-model="form.weight" placeholder="请输入体重" />
          </view>

          <view class="input-group">
            <text class="label">年龄</text>
            <input type="number" v-model="form.age" placeholder="请输入年龄" />
          </view>

          <view class="input-group">
            <text class="label">性别</text>
            <view class="radio-group flex">
              <view
                class="radio-item flex-center"
                :class="{ selected: form.gender === '男' }"
                @tap="form.gender = '男'"
              >♂ 男</view>
              <view
                class="radio-item flex-center"
                :class="{ selected: form.gender === '女' }"
                @tap="form.gender = '女'"
              >♀ 女</view>
            </view>
          </view>

          <view class="input-group" @tap="showActivityPicker = true">
            <text class="label">活动水平</text>
            <view class="picker-display flex-between">
              <text :class="{ placeholder: !form.activityLevel }">
                {{ form.activityLevel || '请选择活动水平' }}
              </text>
              <text class="arrow">›</text>
            </view>
          </view>

          <view class="input-group" @tap="showGoalPicker = true">
            <text class="label">饮食目标</text>
            <view class="picker-display flex-between">
              <text :class="{ placeholder: !form.goal }">
                {{ form.goal || '请选择饮食目标' }}
              </text>
              <text class="arrow">›</text>
            </view>
          </view>

          <view class="input-group">
            <text class="label">特殊需求（选填）</text>
            <textarea
              v-model="form.specialRequirements"
              placeholder="如：过敏食物、素食、低碳水等"
              :maxlength="200"
              class="special-textarea"
            />
          </view>
        </view>

        <button class="btn-primary generate-btn" :disabled="generating" @tap="generatePlan">
          {{ generating ? '🤖 AI 生成中...' : '✨ 生成饮食计划' }}
        </button>
      </view>

      <!-- Result -->
      <view v-show="currentTab === 'result'" class="result-section" v-if="planResult">
        <view class="card calorie-card">
          <view class="calorie-header flex-between">
            <text class="calorie-label">每日目标热量</text>
            <text class="calorie-value">{{ planResult.dailyCalories }} kcal</text>
          </view>
          <view class="nutrition-row flex" v-if="planResult.nutritionBreakdown">
            <view class="nutrition-item flex-1">
              <text class="n-value">{{ planResult.nutritionBreakdown.protein }}g</text>
              <text class="n-label">蛋白质</text>
            </view>
            <view class="nutrition-item flex-1">
              <text class="n-value">{{ planResult.nutritionBreakdown.fat }}g</text>
              <text class="n-label">脂肪</text>
            </view>
            <view class="nutrition-item flex-1">
              <text class="n-value">{{ planResult.nutritionBreakdown.carbs }}g</text>
              <text class="n-label">碳水</text>
            </view>
          </view>
        </view>

        <view class="card meal-card" v-for="(meal, idx) in planResult.meals" :key="idx">
          <view class="meal-header flex">
            <text class="meal-icon">{{ getMealIcon(meal.type) }}</text>
            <text class="meal-name">{{ getMealLabel(meal.type) }}</text>
            <text class="meal-calories text-secondary">{{ meal.calories }} kcal</text>
          </view>
          <view class="food-list">
            <view class="food-item flex-between" v-for="(food, fi) in meal.foods" :key="fi">
              <view>
                <text class="food-name">{{ food.name }}</text>
                <text class="food-amount text-secondary"> {{ food.amount }}</text>
              </view>
              <text class="food-cal text-secondary">{{ food.calories }} kcal</text>
            </view>
          </view>
        </view>
      </view>

      <!-- History -->
      <view v-show="currentTab === 'history'" class="history-section">
        <view v-if="historyList.length === 0" class="empty-state">
          <text class="empty-icon">📝</text>
          <text class="empty-text">暂无历史记录</text>
        </view>
        <view
          class="card history-item"
          v-for="item in historyList"
          :key="item.id"
          @tap="viewHistoryPlan(item)"
        >
          <view class="flex-between">
            <text class="history-target">{{ item.dailyCalories }} kcal/天</text>
            <text class="history-goal">{{ item.goal }}</text>
          </view>
          <text class="history-date text-secondary">{{ item.createdAt }}</text>
        </view>
        <view v-if="historyHasMore" class="load-more" @tap="loadHistory">
          <text class="text-secondary">加载更多</text>
        </view>
      </view>
    </view>

    <!-- Loading Overlay -->
    <view v-if="generating" class="loading-overlay flex-center">
      <view class="loading-card card">
        <view class="loading-animation">
          <view class="loading-spinner" />
        </view>
        <text class="loading-text">🤖 AI 正在为您生成个性化饮食方案...</text>
        <text class="loading-hint text-secondary">预计需要10-30秒，请耐心等待</text>
      </view>
    </view>

    <!-- Picker Modals -->
    <view class="picker-mask" v-if="showActivityPicker" @tap="showActivityPicker = false">
      <view class="picker-content" @tap.stop>
        <view class="picker-header flex-between">
          <text @tap="showActivityPicker = false">取消</text>
          <text class="picker-title">选择活动水平</text>
          <text class="text-primary" @tap="showActivityPicker = false">确定</text>
        </view>
        <view
          class="picker-option"
          v-for="opt in activityLevels"
          :key="opt"
          :class="{ selected: form.activityLevel === opt }"
          @tap="form.activityLevel = opt; showActivityPicker = false"
        >{{ opt }}</view>
      </view>
    </view>

    <view class="picker-mask" v-if="showGoalPicker" @tap="showGoalPicker = false">
      <view class="picker-content" @tap.stop>
        <view class="picker-header flex-between">
          <text @tap="showGoalPicker = false">取消</text>
          <text class="picker-title">选择饮食目标</text>
          <text class="text-primary" @tap="showGoalPicker = false">确定</text>
        </view>
        <view
          class="picker-option"
          v-for="opt in goals"
          :key="opt"
          :class="{ selected: form.goal === opt }"
          @tap="form.goal = opt; showGoalPicker = false"
        >{{ opt }}</view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { onLoad, onPullDownRefresh } from '@dcloudio/uni-app'
import { dietPlanApi, vipApi, MealTypes } from '@/services/api'
import { checkLogin } from '@/utils/common'

interface PlanFood {
  name: string
  amount: string
  calories: number
}
interface PlanMeal {
  type: string
  calories: number
  foods: PlanFood[]
}
interface PlanResult {
  id?: number
  dailyCalories: number
  meals: PlanMeal[]
  nutritionBreakdown?: { protein: number; fat: number; carbs: number }
  goal?: string
  createdAt?: string
}

const isVip = ref(false)
const loading = ref(true)
const showDisclaimer = ref(true)
const generating = ref(false)
const currentTab = ref('form')
const showActivityPicker = ref(false)
const showGoalPicker = ref(false)

const activityLevels = ['久坐', '轻度活动', '中度活动', '重度活动']
const goals = ['减脂', '增肌', '维持体重', '营养饮食']

const form = ref({
  height: '',
  weight: '',
  age: '',
  gender: '男',
  activityLevel: '',
  goal: '',
  specialRequirements: ''
})

const planResult = ref<PlanResult | null>(null)
const historyList = ref<PlanResult[]>([])
const historyPage = ref(0)
const historyHasMore = ref(false)

onLoad(() => {
  if (!checkLogin()) return
  checkVipStatus()
})

onPullDownRefresh(async () => {
  await checkVipStatus()
  uni.stopPullDownRefresh()
})

async function checkVipStatus() {
  loading.value = true
  try {
    const res = await vipApi.getVipStatus()
    if (res.code === 200) {
      isVip.value = !!res.data?.isVip || !!res.data?.vip
      if (isVip.value) loadHistory()
    }
  } catch {
    uni.showToast({ title: '获取VIP状态失败', icon: 'none' })
  } finally {
    loading.value = false
  }
}

function validateForm(): boolean {
  if (!form.value.height || Number(form.value.height) <= 0) {
    uni.showToast({ title: '请输入正确的身高', icon: 'none' }); return false
  }
  if (!form.value.weight || Number(form.value.weight) <= 0) {
    uni.showToast({ title: '请输入正确的体重', icon: 'none' }); return false
  }
  if (!form.value.age || Number(form.value.age) <= 0) {
    uni.showToast({ title: '请输入正确的年龄', icon: 'none' }); return false
  }
  if (!form.value.activityLevel) {
    uni.showToast({ title: '请选择活动水平', icon: 'none' }); return false
  }
  if (!form.value.goal) {
    uni.showToast({ title: '请选择饮食目标', icon: 'none' }); return false
  }
  return true
}

async function generatePlan() {
  if (!validateForm()) return
  generating.value = true
  try {
    const res = await dietPlanApi.generate({
      height: Number(form.value.height),
      weight: Number(form.value.weight),
      age: Number(form.value.age),
      gender: form.value.gender,
      activityLevel: form.value.activityLevel,
      goal: form.value.goal,
      specialRequirements: form.value.specialRequirements || undefined
    })
    if (res.code === 200 && res.data) {
      planResult.value = res.data
      currentTab.value = 'result'
      uni.showToast({ title: '饮食计划已生成', icon: 'success' })
      loadHistory()
    } else {
      uni.showToast({ title: res.message || '生成失败', icon: 'none' })
    }
  } catch (e: any) {
    uni.showToast({ title: e.message || '生成失败，请重试', icon: 'none' })
  } finally {
    generating.value = false
  }
}

async function loadHistory() {
  try {
    const res = await dietPlanApi.getPlans({ page: historyPage.value, size: 10 })
    if (res.code === 200) {
      const list = res.data?.content || res.data?.records || res.data?.list || res.data || []
      if (historyPage.value === 0) {
        historyList.value = list
      } else {
        historyList.value.push(...list)
      }
      historyHasMore.value = list.length >= 10
    }
  } catch {}
}

function viewHistoryPlan(item: PlanResult) {
  planResult.value = item
  currentTab.value = 'result'
}

function getMealIcon(type: string): string {
  const found = MealTypes.find(m => m.value === type)
  return found?.icon || '🍽️'
}

function getMealLabel(type: string): string {
  const found = MealTypes.find(m => m.value === type)
  return found?.label || type
}

function goVip() {
  uni.switchTab({ url: '/pages/member/index' })
}
</script>

<style lang="scss" scoped>
.container {
  padding: 20rpx 30rpx;
  padding-bottom: 40rpx;
  min-height: 100vh;
  background: $background;
}

/* VIP Gate */
.vip-gate {
  margin-top: 100rpx;
  text-align: center;
  padding: 60rpx 40rpx;
}
.vip-icon {
  font-size: 100rpx;
  margin-bottom: 20rpx;
}
.vip-title {
  display: block;
  font-size: 36rpx;
  font-weight: 700;
  color: $foreground;
  margin-bottom: 16rpx;
  font-family: 'Calistoga', Georgia, 'PingFang SC', serif;
}
.vip-desc {
  display: block;
  font-size: 26rpx;
  color: $muted-foreground;
  margin-bottom: 40rpx;
  line-height: 1.6;
}
.vip-btn {
  width: 60%;
  margin: 0 auto;
}

/* Tabs */
.tab-bar {
  background: $card;
  border: 1rpx solid $border;
  border-radius: $radius-full;
  margin-bottom: 20rpx;
  overflow: hidden;
  box-shadow: $shadow-sm;
  padding: 6rpx;
}
.tab-item {
  text-align: center;
  padding: 24rpx 0;
  font-size: 28rpx;
  color: $muted-foreground;
  position: relative;
  transition: all 0.2s;
  font-family: 'Inter', 'PingFang SC', sans-serif;
  border-radius: $radius-full;
}
.tab-item.active {
  color: #fff;
  font-weight: 600;
  background: $gradient-accent;
  box-shadow: $shadow-accent;
}
.tab-item.active::after {
  display: none;
}

/* Form */
.form-title {
  font-size: 32rpx;
  font-weight: 700;
  margin-bottom: 24rpx;
  color: $foreground;
  font-family: 'Calistoga', Georgia, 'PingFang SC', serif;
}
.radio-group {
  gap: 20rpx;
  margin-top: 8rpx;
}
.radio-item {
  flex: 1;
  height: 72rpx;
  border: 1rpx solid $border;
  border-radius: $radius-lg;
  font-size: 28rpx;
  color: $muted-foreground;
  transition: all 0.2s;
  font-family: 'Inter', 'PingFang SC', sans-serif;
}
.radio-item.selected {
  border-color: $accent;
  background: rgba(16, 185, 129, 0.06);
  color: $accent;
  font-weight: 500;
  box-shadow: 0 0 0 3px rgba(16, 185, 129, 0.1);
}
.picker-display {
  margin-top: 8rpx;
  padding: 8rpx 0;
}
.placeholder {
  color: $uni-text-color-placeholder;
}
.arrow {
  font-size: 32rpx;
  color: $border;
}
.special-textarea {
  width: 100%;
  height: 160rpx;
  font-size: 28rpx;
  margin-top: 8rpx;
  line-height: 1.5;
  font-family: 'Inter', 'PingFang SC', sans-serif;
}
.generate-btn {
  margin-top: 30rpx;
}

/* Result — calorie card */
.calorie-card {
  background: $gradient-accent;
  border: none;
  box-shadow: $shadow-accent;
  color: #fff;
  border-radius: $radius-2xl;
}
.calorie-header {
  margin-bottom: 24rpx;
}
.calorie-label {
  font-size: 28rpx;
  color: rgba(255, 255, 255, 0.85);
  font-family: 'Calistoga', Georgia, 'PingFang SC', serif;
}
.calorie-value {
  font-size: 40rpx;
  font-weight: 700;
  color: #fff;
  font-family: 'Calistoga', Georgia, 'PingFang SC', serif;
}
.nutrition-row {
  gap: 16rpx;
}
.nutrition-item {
  text-align: center;
  background: rgba(255, 255, 255, 0.15);
  border: 1rpx solid rgba(255, 255, 255, 0.2);
  border-radius: $radius-lg;
  padding: 16rpx 0;
}
.n-value {
  display: block;
  font-size: 32rpx;
  font-weight: 600;
  color: #fff;
}
.n-label {
  display: block;
  font-size: 22rpx;
  color: rgba(255, 255, 255, 0.75);
  margin-top: 4rpx;
}

/* Meal cards */
.meal-card {
  padding: 24rpx;
  background: $card;
  border: 1rpx solid $border;
  box-shadow: $shadow-sm;
  border-radius: $radius-2xl;
}
.meal-header {
  align-items: center;
  gap: 12rpx;
  margin-bottom: 20rpx;
  padding-bottom: 16rpx;
  border-bottom: 1rpx solid $border;
}
.meal-icon {
  font-size: 36rpx;
}
.meal-name {
  font-size: 30rpx;
  font-weight: 700;
  flex: 1;
  font-family: 'Calistoga', Georgia, 'PingFang SC', serif;
  color: $foreground;
}
.meal-calories {
  font-size: 26rpx;
}
.food-item {
  padding: 12rpx 0;
  border-bottom: 1rpx solid $border;
}
.food-item:last-child {
  border-bottom: none;
}
.food-name {
  font-size: 28rpx;
  color: $foreground;
}
.food-amount {
  font-size: 24rpx;
}
.food-cal {
  font-size: 26rpx;
}

/* History */
.history-item {
  padding: 20rpx 24rpx;
}
.history-target {
  font-size: 30rpx;
  font-weight: 600;
  color: $accent;
  font-family: 'Calistoga', Georgia, 'PingFang SC', serif;
}
.history-goal {
  font-size: 24rpx;
  background: rgba(16, 185, 129, 0.08);
  color: $accent;
  padding: 4rpx 16rpx;
  border-radius: $radius-full;
  font-family: 'JetBrains Mono', monospace;
}
.history-date {
  display: block;
  font-size: 24rpx;
  margin-top: 8rpx;
}
.load-more {
  text-align: center;
  padding: 20rpx;
}

/* Loading Overlay */
.loading-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(15, 23, 42, 0.5);
  z-index: 999;
}
.loading-card {
  width: 80%;
  text-align: center;
  padding: 60rpx 40rpx;
}
.loading-spinner {
  width: 80rpx;
  height: 80rpx;
  border: 6rpx solid $border;
  border-top-color: $accent;
  border-radius: 50%;
  margin: 0 auto 30rpx;
  animation: spin 0.8s linear infinite;
}
@keyframes spin {
  to { transform: rotate(360deg); }
}
.loading-text {
  display: block;
  font-size: 30rpx;
  color: $foreground;
  margin-bottom: 12rpx;
  font-family: 'Inter', 'PingFang SC', sans-serif;
}
.loading-hint {
  display: block;
  font-size: 24rpx;
}

/* Picker Modal */
.picker-mask {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(15, 23, 42, 0.5);
  z-index: 1000;
  display: flex;
  align-items: flex-end;
}
.picker-content {
  width: 100%;
  background: $card;
  border-radius: $radius-2xl $radius-2xl 0 0;
  padding-bottom: env(safe-area-inset-bottom);
}
.picker-header {
  padding: 24rpx 30rpx;
  border-bottom: 1rpx solid $border;
  font-size: 28rpx;
}
.picker-title {
  font-weight: 700;
  font-family: 'Calistoga', Georgia, 'PingFang SC', serif;
  color: $foreground;
}
.picker-option {
  padding: 28rpx 30rpx;
  font-size: 30rpx;
  border-bottom: 1rpx solid $border;
  font-family: 'Inter', 'PingFang SC', sans-serif;
  color: $foreground;
}
.picker-option.selected {
  color: $accent;
  font-weight: 500;
  background: rgba(16, 185, 129, 0.04);
}
.disclaimer-tip {
  background: rgba(16, 185, 129, 0.06);
  color: $foreground;
  border: 1rpx solid rgba(16, 185, 129, 0.15);
  border-radius: $radius-lg;
  padding: 14rpx 48rpx 14rpx 20rpx;
  font-size: 22rpx;
  margin-bottom: 20rpx;
  position: relative;
}
.disclaimer-tip .dismiss {
  position: absolute;
  right: 16rpx;
  top: 50%;
  transform: translateY(-50%);
  font-size: 28rpx;
  color: $muted-foreground;
}
</style>
