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
      <!-- Disclaimer -->
      <view class="disclaimer-tip" v-if="showDisclaimer">
        <text>⚕️ AI饮食计划仅供参考，不能替代专业营养师或医生的建议。患有疾病者请遵医嘱。</text>
        <text class="dismiss" @tap="showDisclaimer = false">✕</text>
      </view>

      <!-- Tabs -->
      <view class="tab-bar flex">
        <view
          class="tab-item flex-1"
          :class="{ active: currentTab === 'form' }"
          @tap="currentTab = 'form'"
        >生成计划</view>
        <view
          class="tab-item flex-1"
          :class="{ active: currentTab === 'result' }"
          @tap="handleResultTab"
        >当前计划</view>
        <view
          class="tab-item flex-1"
          :class="{ active: currentTab === 'history' }"
          @tap="switchToHistory"
        >历史记录</view>
      </view>

      <!-- ===== FORM TAB ===== -->
      <view v-show="currentTab === 'form'" class="form-section">
        <!-- Profile loaded hint -->
        <view v-if="profileLoaded" class="profile-loaded-tip">
          <text>✅ 已从健康档案自动填充个人数据</text>
        </view>

        <view class="card">
          <view class="form-title">📋 基本信息</view>

          <view class="input-group">
            <text class="label">身高 (cm)</text>
            <input type="digit" v-model="form.height" placeholder="请输入身高" />
          </view>

          <view class="input-group">
            <text class="label">体重 (kg)</text>
            <input type="digit" v-model="form.weight" placeholder="请输入体重" />
          </view>

          <view class="input-row">
            <view class="input-group input-half">
              <text class="label">年龄</text>
              <input type="number" v-model="form.age" placeholder="年龄" />
            </view>
            <view class="input-group input-half">
              <text class="label">性别</text>
              <view class="radio-group flex">
                <view
                  class="radio-item flex-center"
                  :class="{ selected: form.gender === 'male' }"
                  @tap="form.gender = 'male'"
                >♂ 男</view>
                <view
                  class="radio-item flex-center"
                  :class="{ selected: form.gender === 'female' }"
                  @tap="form.gender = 'female'"
                >♀ 女</view>
              </view>
            </view>
          </view>

          <view class="input-group" @tap="showActivityPicker = true">
            <text class="label">活动水平</text>
            <view class="picker-display flex-between">
              <text :class="{ placeholder: !form.exerciseLevel }">
                {{ activityLabelMap[form.exerciseLevel] || '请选择活动水平' }}
              </text>
              <text class="arrow">›</text>
            </view>
          </view>

          <view class="input-group" @tap="showGoalPicker = true">
            <text class="label">饮食目标</text>
            <view class="picker-display flex-between">
              <text :class="{ placeholder: !form.goal }">
                {{ goalLabelMap[form.goal] || '请选择饮食目标' }}
              </text>
              <text class="arrow">›</text>
            </view>
          </view>
        </view>

        <!-- Plan days -->
        <view class="card">
          <view class="form-title">📅 计划天数</view>
          <view class="days-chips flex">
            <view
              v-for="d in dayOptions"
              :key="d"
              class="day-chip flex-center"
              :class="{ selected: form.days === d }"
              @tap="form.days = d"
            >{{ d }}天</view>
          </view>
        </view>

        <!-- Preferences -->
        <view class="card">
          <view class="form-title">🍽️ 饮食偏好</view>

          <view class="input-group">
            <text class="label">饮食偏好（选填）</text>
            <textarea
              v-model="form.preferences"
              placeholder="如：喜欢清淡口味、素食主义、低碳水等"
              :maxlength="200"
              class="special-textarea"
            />
          </view>

          <view class="input-group">
            <text class="label">过敏/忌口（选填）</text>
            <textarea
              v-model="form.allergies"
              placeholder="如：海鲜过敏、不吃香菜、对牛奶过敏等"
              :maxlength="200"
              class="special-textarea"
            />
          </view>
        </view>

        <button class="btn-primary generate-btn" :disabled="isGenerating" @tap="handleGenerate">
          {{ isGenerating ? '🤖 AI 生成中...' : '✨ 生成饮食计划' }}
        </button>
      </view>

      <!-- ===== GENERATING OVERLAY ===== -->
      <view v-if="isGenerating" class="loading-overlay flex-center">
        <view class="loading-card card">
          <view class="loading-spinner" />
          <text class="loading-title">🤖 AI 正在生成饮食计划</text>
          <text class="loading-status">{{ progressText }}</text>

          <!-- Progress bar -->
          <view class="progress-bar-wrap">
            <view class="progress-bar-bg">
              <view class="progress-bar-fill" :style="{ width: progress + '%' }" />
            </view>
            <text class="progress-pct">{{ Math.floor(progress) }}%</text>
          </view>

          <text class="loading-hint text-secondary">{{ estimatedTimeText }}</text>

          <view v-if="currentTaskId" class="cancel-btn-wrap">
            <button class="btn-cancel" @tap="handleCancelGenerate">取消生成</button>
          </view>
        </view>
      </view>

      <!-- ===== RESULT TAB ===== -->
      <view v-show="currentTab === 'result'" class="result-section">
        <view v-if="!generatedPlan" class="empty-state">
          <text class="empty-icon">📋</text>
          <text class="empty-text">暂无计划，请先生成</text>
        </view>

        <template v-if="generatedPlan">
          <!-- Plan header -->
          <view class="card result-header-card">
            <view class="result-title-row flex-between">
              <text class="result-plan-title">{{ generatedPlan.title || 'AI 饮食计划' }}</text>
              <view class="result-badge">{{ generatedPlan.days || form.days }}天</view>
            </view>
            <text class="result-goal text-secondary">{{ getGoalText(generatedPlan.goal) }}</text>

            <!-- Action buttons -->
            <view class="result-actions flex">
              <view class="action-btn" @tap="openModifySheet">
                <text class="action-icon">✏️</text>
                <text class="action-label">修改</text>
              </view>
              <view class="action-btn" @tap="handleExportPlan">
                <text class="action-icon">📄</text>
                <text class="action-label">导出</text>
              </view>
              <view class="action-btn" @tap="handleClosePlan">
                <text class="action-icon">🗑️</text>
                <text class="action-label">关闭</text>
              </view>
            </view>
          </view>

          <!-- Parsed daily plans -->
          <view v-for="(day, dayIdx) in parsedDays" :key="dayIdx" class="day-block">
            <view class="day-title-bar flex">
              <text class="day-title">📅 第{{ dayIdx + 1 }}天</text>
              <text v-if="day.totalCalories" class="day-cal text-secondary">≈{{ day.totalCalories }} kcal</text>
            </view>

            <!-- Calorie & nutrition overview -->
            <view v-if="day.nutrition" class="card calorie-card">
              <view class="nutrition-row flex">
                <view class="nutrition-item flex-1">
                  <text class="n-value">{{ day.nutrition.protein || '-' }}</text>
                  <text class="n-label">蛋白质</text>
                </view>
                <view class="nutrition-item flex-1">
                  <text class="n-value">{{ day.nutrition.fat || '-' }}</text>
                  <text class="n-label">脂肪</text>
                </view>
                <view class="nutrition-item flex-1">
                  <text class="n-value">{{ day.nutrition.carbs || '-' }}</text>
                  <text class="n-label">碳水</text>
                </view>
              </view>
            </view>

            <!-- Meals -->
            <view class="card meal-card" v-for="(meal, mIdx) in day.meals" :key="mIdx">
              <view class="meal-header flex">
                <text class="meal-icon">{{ meal.icon }}</text>
                <text class="meal-name">{{ meal.name }}</text>
                <text v-if="meal.calories" class="meal-calories text-secondary">{{ meal.calories }}</text>
              </view>
              <view class="food-list">
                <view class="food-item" v-for="(food, fi) in meal.foods" :key="fi">
                  <text class="food-text">{{ food }}</text>
                </view>
              </view>
            </view>
          </view>

          <!-- Raw markdown fallback -->
          <view v-if="parsedDays.length === 0 && generatedPlan.markdownContent" class="card raw-content-card">
            <text class="raw-content">{{ generatedPlan.markdownContent }}</text>
          </view>
        </template>
      </view>

      <!-- ===== HISTORY TAB ===== -->
      <view v-show="currentTab === 'history'" class="history-section">
        <view v-if="historyLoading" class="loading-inline flex-center">
          <view class="loading-spinner-sm" />
          <text class="text-secondary">加载中...</text>
        </view>

        <view v-if="!historyLoading && historyList.length === 0" class="empty-state">
          <text class="empty-icon">📝</text>
          <text class="empty-text">暂无历史记录</text>
        </view>

        <view
          class="card history-item"
          v-for="item in historyList"
          :key="item.planId || item.id"
        >
          <view class="history-main" @tap="loadHistoryDetail(item.planId || item.id)">
            <view class="flex-between">
              <text class="history-title">{{ item.title || 'AI饮食计划' }}</text>
              <view class="history-badges flex">
                <text class="history-days-badge">{{ item.days || '-' }}天</text>
                <text class="history-goal-badge">{{ getGoalText(item.goal) }}</text>
              </view>
            </view>
            <text class="history-date text-secondary">{{ formatDate(item.createdAt) }}</text>
          </view>
          <view class="history-delete" @tap.stop="confirmDeleteHistory(item.planId || item.id)">
            <text class="delete-icon">🗑️</text>
          </view>
        </view>

        <view v-if="historyHasMore && !historyLoading" class="load-more" @tap="loadMoreHistory">
          <text class="text-secondary">加载更多</text>
        </view>
      </view>
    </view>

    <!-- ===== PICKER MODALS ===== -->
    <view class="picker-mask" v-if="showActivityPicker" @tap="showActivityPicker = false">
      <view class="picker-content" @tap.stop>
        <view class="picker-header flex-between">
          <text @tap="showActivityPicker = false">取消</text>
          <text class="picker-title">选择活动水平</text>
          <text class="text-primary" @tap="showActivityPicker = false">确定</text>
        </view>
        <view
          class="picker-option"
          v-for="opt in activityOptions"
          :key="opt.value"
          :class="{ selected: form.exerciseLevel === opt.value }"
          @tap="form.exerciseLevel = opt.value; showActivityPicker = false"
        >
          <text>{{ opt.label }}</text>
          <text v-if="form.exerciseLevel === opt.value" class="check-mark">✓</text>
        </view>
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
          v-for="opt in goalOptions"
          :key="opt.value"
          :class="{ selected: form.goal === opt.value }"
          @tap="form.goal = opt.value; showGoalPicker = false"
        >
          <text>{{ opt.label }}</text>
          <text v-if="form.goal === opt.value" class="check-mark">✓</text>
        </view>
      </view>
    </view>

    <!-- Modify suggestion sheet -->
    <view class="picker-mask" v-if="showModifySheet" @tap="showModifySheet = false">
      <view class="picker-content modify-sheet" @tap.stop>
        <view class="picker-header flex-between">
          <text @tap="showModifySheet = false">取消</text>
          <text class="picker-title">修改建议</text>
          <text class="text-primary" @tap="submitModify">{{ isModifying ? '修改中...' : '提交' }}</text>
        </view>
        <view class="modify-body">
          <textarea
            v-model="modifySuggestion"
            class="modify-textarea"
            placeholder="请描述修改意见，如：换成素食、减少主食、去掉海鲜、早餐增加水果等"
            :maxlength="500"
          />
        </view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed, onBeforeUnmount } from 'vue'
import { onLoad, onPullDownRefresh } from '@dcloudio/uni-app'
import { dietPlanApi, vipApi } from '@/services/api'
import { request } from '@/utils/request'
import { checkLogin } from '@/utils/common'
import { useUserStore } from '@/stores/user'

// ---- Types ----
interface DayMeal {
  icon: string
  name: string
  calories: string
  foods: string[]
}
interface DayNutrition {
  protein: string
  fat: string
  carbs: string
}
interface ParsedDay {
  totalCalories: string
  nutrition: DayNutrition | null
  meals: DayMeal[]
}

// ---- State ----
const userStore = useUserStore()
const isVip = ref(false)
const loading = ref(true)
const showDisclaimer = ref(true)
const currentTab = ref('form')
const profileLoaded = ref(false)

// Form
const showActivityPicker = ref(false)
const showGoalPicker = ref(false)

const dayOptions = [1, 3, 5, 7]

const activityOptions = [
  { value: 'low', label: '轻度运动（久坐/每周1-3天）' },
  { value: 'medium', label: '中度运动（每周3-5天）' },
  { value: 'high', label: '高强度运动（每周6-7天）' }
]
const activityLabelMap: Record<string, string> = {
  low: '轻度运动',
  medium: '中度运动',
  high: '高强度运动'
}

const goalOptions = [
  { value: 'lose_weight', label: '🔥 减脂塑形' },
  { value: 'gain_muscle', label: '💪 增肌强体' },
  { value: 'maintain', label: '❤️ 均衡饮食' }
]
const goalLabelMap: Record<string, string> = {
  lose_weight: '🔥 减脂塑形',
  gain_muscle: '💪 增肌强体',
  maintain: '❤️ 均衡饮食'
}

const form = ref({
  days: 7,
  goal: 'maintain',
  exerciseLevel: 'medium',
  gender: '' as string,
  age: '' as string | number,
  height: '' as string | number,
  weight: '' as string | number,
  preferences: '',
  allergies: ''
})

// Generation
const isGenerating = ref(false)
const progress = ref(0)
const progressText = ref('正在创建任务...')
const currentTaskId = ref<string | null>(null)
let pollTimer: ReturnType<typeof setInterval> | null = null

// Result
const generatedPlan = ref<any>(null)

// History
const historyList = ref<any[]>([])
const historyPage = ref(1)
const historyHasMore = ref(false)
const historyLoading = ref(false)

// Modify
const showModifySheet = ref(false)
const modifySuggestion = ref('')
const isModifying = ref(false)

// ---- Computed ----
const estimatedTimeText = computed(() => {
  const days = form.value.days || 7
  const seconds = days <= 3 ? 30 : days * 20
  const minutes = Math.ceil(seconds / 60)
  if (minutes <= 1) return `预计约30秒（${days}天计划）`
  return `预计约${minutes}分钟（${days}天计划）`
})

const parsedDays = computed<ParsedDay[]>(() => {
  if (!generatedPlan.value?.markdownContent) return []
  return parseMarkdownToDays(generatedPlan.value.markdownContent)
})

// ---- Lifecycle ----
onLoad(() => {
  if (!checkLogin()) return
  init()
})

onPullDownRefresh(async () => {
  await checkVipStatus()
  uni.stopPullDownRefresh()
})

onBeforeUnmount(() => {
  clearTimers()
})

async function init() {
  await checkVipStatus()
  if (isVip.value) {
    loadHealthProfile()
    loadHistory()
    restoreTask()
  }
}

// ---- VIP ----
async function checkVipStatus() {
  loading.value = true
  try {
    const res = await vipApi.getVipStatus()
    if (res.code === 200) {
      isVip.value = !!res.data?.isVip || !!res.data?.vip
    }
  } catch {
    uni.showToast({ title: '获取VIP状态失败', icon: 'none' })
  } finally {
    loading.value = false
  }
}

function goVip() {
  uni.switchTab({ url: '/pages/member/index' })
}

// ---- Health profile auto-load ----
async function loadHealthProfile() {
  try {
    const res = await request({ url: '/user/health-profile', showError: false })
    const profile = res?.data
    if (!profile) return

    if (profile.gender) {
      form.value.gender = profile.gender === 'MALE' ? 'male' : profile.gender === 'FEMALE' ? 'female' : ''
    }
    if (profile.age != null) form.value.age = profile.age
    if (profile.height != null) form.value.height = profile.height
    if (profile.weight != null) form.value.weight = profile.weight

    if (profile.activityLevel) {
      const map: Record<string, string> = { SEDENTARY: 'low', LIGHT: 'low', MODERATE: 'medium', ACTIVE: 'high', VERY_ACTIVE: 'high' }
      form.value.exerciseLevel = map[profile.activityLevel] || 'medium'
    }

    if (Array.isArray(profile.healthGoals) && profile.healthGoals.length > 0) {
      const goals = profile.healthGoals.join(',')
      if (goals.includes('减脂')) form.value.goal = 'lose_weight'
      else if (goals.includes('增肌')) form.value.goal = 'gain_muscle'
      else form.value.goal = 'maintain'
    }

    const allergyItems = [
      ...(Array.isArray(profile.allergies) ? profile.allergies : []),
      ...(Array.isArray(profile.dietaryRestrictions) ? profile.dietaryRestrictions : [])
    ]
    if (allergyItems.length > 0) form.value.allergies = allergyItems.join('，')

    if (Array.isArray(profile.dietaryRestrictions) && profile.dietaryRestrictions.length > 0) {
      form.value.preferences = profile.dietaryRestrictions.join('，')
    }

    profileLoaded.value = true
  } catch {
    // Health profile unavailable
  }
}

// ---- Form validation ----
function validateForm(): boolean {
  if (!form.value.goal) {
    uni.showToast({ title: '请选择饮食目标', icon: 'none' }); return false
  }
  return true
}

// ---- Generate plan ----
async function handleGenerate() {
  if (!validateForm()) return
  isGenerating.value = true
  progress.value = 0
  progressText.value = '正在创建任务...'

  try {
    const payload: any = {
      days: form.value.days,
      goal: form.value.goal,
      exerciseLevel: form.value.exerciseLevel
    }
    if (form.value.gender) payload.gender = form.value.gender
    if (form.value.age) payload.age = Number(form.value.age)
    if (form.value.height) payload.height = Number(form.value.height)
    if (form.value.weight) payload.weight = Number(form.value.weight)
    if (form.value.preferences) payload.preferences = form.value.preferences
    if (form.value.allergies) payload.allergies = form.value.allergies

    const res = await dietPlanApi.generate(payload)
    if (res.code === 200 && res.data?.taskId) {
      currentTaskId.value = res.data.taskId
      uni.setStorageSync('currentTaskId', currentTaskId.value)
      uni.showToast({ title: '任务已创建', icon: 'none' })
      startPolling()
    } else {
      throw new Error(res.message || '创建任务失败')
    }
  } catch (e: any) {
    uni.showToast({ title: e.message || '创建任务失败', icon: 'none' })
    isGenerating.value = false
    progress.value = 0
  }
}

// ---- Polling ----
function clearTimers() {
  if (pollTimer) {
    clearInterval(pollTimer)
    pollTimer = null
  }
}

function startPolling() {
  clearTimers()
  let errorCount = 0

  pollTimer = setInterval(async () => {
    if (!currentTaskId.value) { clearTimers(); return }
    try {
      const res = await request({ url: `/diet-plan/task/${currentTaskId.value}/status`, showError: false })
      errorCount = 0

      if (res.code === 200 && res.data) {
        const status = res.data
        if (status.progress != null) progress.value = status.progress

        const totalDays = status.totalDays || form.value.days
        if (status.status === 'running' && status.currentDay > 0) {
          progressText.value = `正在生成第 ${status.currentDay}/${totalDays} 天...`
        } else if (status.status === 'pending') {
          progressText.value = '任务排队中...'
        } else {
          progressText.value = '正在生成饮食计划...'
        }

        if (status.status === 'completed') {
          clearTimers()
          progress.value = 100
          progressText.value = '生成完成！'
          await loadPlanDetail(status.planId)
          isGenerating.value = false
          currentTaskId.value = null
          uni.removeStorageSync('currentTaskId')
          uni.showToast({ title: '饮食计划已生成', icon: 'success' })
          currentTab.value = 'result'
          loadHistory()
        } else if (status.status === 'failed') {
          clearTimers()
          isGenerating.value = false
          currentTaskId.value = null
          uni.removeStorageSync('currentTaskId')
          uni.showToast({ title: status.errorMessage || '生成失败', icon: 'none' })
        } else if (status.status === 'cancelled') {
          clearTimers()
          isGenerating.value = false
          currentTaskId.value = null
          uni.removeStorageSync('currentTaskId')
          uni.showToast({ title: '任务已取消', icon: 'none' })
        }
      }
    } catch {
      errorCount++
      if (errorCount >= 10) {
        clearTimers()
        isGenerating.value = false
        currentTaskId.value = null
        uni.removeStorageSync('currentTaskId')
        uni.showToast({ title: '网络异常，请稍后查看历史记录', icon: 'none' })
      }
    }
  }, 2500)
}

function handleCancelGenerate() {
  if (!currentTaskId.value) return
  uni.showModal({
    title: '确认取消',
    content: '确定要取消当前生成吗？',
    success: async (res) => {
      if (!res.confirm) return
      try {
        const r = await request({ url: `/diet-plan/task/${currentTaskId.value}/cancel`, method: 'POST' })
        if (r.code === 200) {
          clearTimers()
          isGenerating.value = false
          progress.value = 0
          currentTaskId.value = null
          uni.removeStorageSync('currentTaskId')
          uni.showToast({ title: '已取消生成', icon: 'none' })
        }
      } catch {
        uni.showToast({ title: '取消失败', icon: 'none' })
      }
    }
  })
}

// Restore in-progress task on page load
async function restoreTask() {
  const savedId = uni.getStorageSync('currentTaskId')
  if (!savedId) return

  currentTaskId.value = savedId
  isGenerating.value = true
  try {
    const res = await request({ url: `/diet-plan/task/${savedId}/status`, showError: false })
    if (res.code === 200 && res.data) {
      const s = res.data
      if (s.progress) progress.value = s.progress
      if (s.status === 'completed') {
        await loadPlanDetail(s.planId)
        isGenerating.value = false
        currentTaskId.value = null
        uni.removeStorageSync('currentTaskId')
        currentTab.value = 'result'
        return
      }
      if (s.status === 'failed' || s.status === 'cancelled') {
        isGenerating.value = false
        currentTaskId.value = null
        uni.removeStorageSync('currentTaskId')
        return
      }
    }
  } catch {
    // silent
  }
  startPolling()
}

// ---- Plan detail ----
async function loadPlanDetail(planId: number | string) {
  try {
    const res = await request({ url: `/diet-plan/history/${planId}` })
    if (res.code === 200 && res.data) {
      generatedPlan.value = res.data
    }
  } catch {
    uni.showToast({ title: '加载计划详情失败', icon: 'none' })
  }
}

function handleResultTab() {
  if (!generatedPlan.value) {
    uni.showToast({ title: '暂无计划，请先生成', icon: 'none' })
    return
  }
  currentTab.value = 'result'
}

function handleClosePlan() {
  generatedPlan.value = null
  currentTab.value = 'form'
}

// ---- History ----
async function loadHistory() {
  historyLoading.value = true
  historyPage.value = 1
  try {
    const res = await request({ url: '/diet-plan/history', data: { page: 1, size: 10 }, showError: false })
    if (res.code === 200) {
      historyList.value = res.data?.content || res.data?.records || res.data?.list || res.data || []
      historyHasMore.value = historyList.value.length >= 10
    }
  } catch {
    // silent
  } finally {
    historyLoading.value = false
  }
}

async function loadMoreHistory() {
  historyPage.value++
  try {
    const res = await request({ url: '/diet-plan/history', data: { page: historyPage.value, size: 10 }, showError: false })
    if (res.code === 200) {
      const list = res.data?.content || res.data?.records || res.data?.list || res.data || []
      historyList.value.push(...list)
      historyHasMore.value = list.length >= 10
    }
  } catch {
    historyPage.value--
  }
}

function switchToHistory() {
  currentTab.value = 'history'
  loadHistory()
}

async function loadHistoryDetail(planId: number | string) {
  uni.showLoading({ title: '加载中...' })
  try {
    await loadPlanDetail(planId)
    currentTab.value = 'result'
  } catch {
    uni.showToast({ title: '加载失败', icon: 'none' })
  } finally {
    uni.hideLoading()
  }
}

function confirmDeleteHistory(planId: number | string) {
  uni.showModal({
    title: '删除确认',
    content: '确定要删除这条记录吗？',
    success: async (res) => {
      if (!res.confirm) return
      try {
        const r = await request({ url: `/diet-plan/${planId}`, method: 'DELETE' })
        if (r.code === 200) {
          historyList.value = historyList.value.filter(i => (i.planId || i.id) !== planId)
          uni.showToast({ title: '删除成功', icon: 'success' })
        }
      } catch {
        uni.showToast({ title: '删除失败', icon: 'none' })
      }
    }
  })
}

// ---- Modify ----
function openModifySheet() {
  modifySuggestion.value = ''
  showModifySheet.value = true
}

async function submitModify() {
  if (!modifySuggestion.value.trim()) {
    uni.showToast({ title: '请输入修改建议', icon: 'none' }); return
  }
  if (!generatedPlan.value) return
  isModifying.value = true
  try {
    const planId = generatedPlan.value.planId || generatedPlan.value.id
    const r = await request({
      url: `/diet-plan/${planId}/modify`,
      method: 'POST',
      data: { suggestion: modifySuggestion.value.trim() }
    })
    if (r.code === 200 && r.data?.taskId) {
      showModifySheet.value = false
      currentTaskId.value = r.data.taskId
      uni.setStorageSync('currentTaskId', currentTaskId.value)
      isGenerating.value = true
      progress.value = 0
      generatedPlan.value = null
      currentTab.value = 'form'
      uni.showToast({ title: '修改任务已创建', icon: 'none' })
      startPolling()
    } else {
      throw new Error(r.message || '修改失败')
    }
  } catch (e: any) {
    uni.showToast({ title: e.message || '修改失败', icon: 'none' })
  } finally {
    isModifying.value = false
  }
}

// ---- Export ----
function handleExportPlan() {
  if (!generatedPlan.value?.markdownContent) {
    uni.showToast({ title: '无内容可导出', icon: 'none' }); return
  }
  // H5: copy to clipboard
  if (navigator?.clipboard) {
    navigator.clipboard.writeText(generatedPlan.value.markdownContent).then(() => {
      uni.showToast({ title: '计划已复制到剪贴板', icon: 'success' })
    }).catch(() => {
      uni.showToast({ title: '复制失败', icon: 'none' })
    })
  } else {
    uni.setClipboardData({
      data: generatedPlan.value.markdownContent,
      success: () => uni.showToast({ title: '计划已复制到剪贴板', icon: 'success' })
    })
  }
}

// ---- Helpers ----
function getGoalText(goal: string): string {
  const map: Record<string, string> = { lose_weight: '减脂塑形', gain_muscle: '增肌强体', maintain: '均衡饮食' }
  return map[goal] || goal || ''
}

function formatDate(dateStr: string): string {
  if (!dateStr) return ''
  try {
    return new Date(dateStr).toLocaleString('zh-CN', { year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' })
  } catch { return dateStr }
}

// ---- Markdown parser: structured day/meal extraction ----
function parseMarkdownToDays(md: string): ParsedDay[] {
  const days: ParsedDay[] = []
  // Split by day headings: "## 第X天" or "## Day X" or "# 第X天"
  const dayPattern = /^#{1,3}\s*(?:第\s*\d+\s*天|Day\s*\d+)/im
  const sections = md.split(/(?=^#{1,3}\s*(?:第\s*\d+\s*天|Day\s*\d+))/im).filter(s => dayPattern.test(s))

  if (sections.length === 0) {
    // Fallback: treat entire content as one day
    const singleDay = parseSingleDay(md)
    if (singleDay.meals.length > 0) days.push(singleDay)
    return days
  }

  for (const section of sections) {
    days.push(parseSingleDay(section))
  }
  return days
}

function parseSingleDay(section: string): ParsedDay {
  const meals: DayMeal[] = []
  let totalCalories = ''
  let nutrition: DayNutrition | null = null

  // Extract total calories from text like "总热量: 1800kcal" or "约1800千卡"
  const calMatch = section.match(/(?:总热量|总计|合计|Total)[：:\s]*[约]?(\d[\d,.]*)\s*(?:kcal|千卡|大卡|cal)/i)
  if (calMatch) totalCalories = calMatch[1]

  // Extract nutrition: "蛋白质: Xg" patterns
  const proteinMatch = section.match(/蛋白质[：:\s]*(\d[\d.]*\s*g)/i)
  const fatMatch = section.match(/脂肪[：:\s]*(\d[\d.]*\s*g)/i)
  const carbsMatch = section.match(/碳水[化合物]*[：:\s]*(\d[\d.]*\s*g)/i)
  if (proteinMatch || fatMatch || carbsMatch) {
    nutrition = {
      protein: proteinMatch?.[1] || '-',
      fat: fatMatch?.[1] || '-',
      carbs: carbsMatch?.[1] || '-'
    }
  }

  // Parse meals by headings or markers
  const mealDefs = [
    { pattern: /(?:早餐|breakfast)/i, icon: '🌅', name: '早餐' },
    { pattern: /(?:午餐|lunch)/i, icon: '☀️', name: '午餐' },
    { pattern: /(?:晚餐|dinner)/i, icon: '🌙', name: '晚餐' },
    { pattern: /(?:加餐|零食|snack)/i, icon: '🍪', name: '加餐' }
  ]

  // Find meal sections by heading patterns like "### 早餐" or "**早餐**"
  const mealBlocks = splitMealBlocks(section)

  for (const block of mealBlocks) {
    const firstLine = block.split('\n')[0]
    let matched = false
    for (const def of mealDefs) {
      if (def.pattern.test(firstLine)) {
        const mealCal = firstLine.match(/(\d[\d,.]*)\s*(?:kcal|千卡|大卡|cal)/i)
        const foods = extractFoods(block)
        meals.push({
          icon: def.icon,
          name: def.name,
          calories: mealCal ? mealCal[1] + ' kcal' : '',
          foods
        })
        matched = true
        break
      }
    }
    if (!matched && block.trim()) {
      // Check if any meal keyword appears anywhere in block
      for (const def of mealDefs) {
        if (def.pattern.test(block)) {
          const foods = extractFoods(block)
          if (foods.length > 0) {
            const mealCal = block.match(/(\d[\d,.]*)\s*(?:kcal|千卡|大卡|cal)/i)
            meals.push({ icon: def.icon, name: def.name, calories: mealCal ? mealCal[1] + ' kcal' : '', foods })
          }
          break
        }
      }
    }
  }

  return { totalCalories, nutrition, meals }
}

function splitMealBlocks(section: string): string[] {
  // Split on meal-related headings
  const parts = section.split(/(?=(?:^|\n)(?:#{2,4}\s*|[*_]{2})?\s*(?:早餐|午餐|晚餐|加餐|零食|breakfast|lunch|dinner|snack))/i)
  return parts.filter(p => p.trim())
}

function extractFoods(block: string): string[] {
  const lines = block.split('\n')
  const foods: string[] = []
  for (let i = 1; i < lines.length; i++) {
    let line = lines[i].trim()
    if (!line) continue
    // Skip headings and total/nutrition summary lines
    if (/^#{1,4}\s/.test(line)) continue
    if (/(?:总热量|总计|合计|蛋白质|脂肪|碳水|营养|小计)/i.test(line) && !/[:：]/.test(line.slice(0, 4))) continue
    // Clean up list markers
    line = line.replace(/^[-*•·+]\s*/, '').replace(/^\d+[.)]\s*/, '')
    if (line.length > 1 && line.length < 100) {
      foods.push(line)
    }
  }
  return foods
}
</script>

<style lang="scss" scoped>
.container {
  padding: 20rpx 30rpx;
  padding-bottom: 60rpx;
  min-height: 100vh;
  background: $background;
}

/* ===== VIP Gate ===== */
.vip-gate {
  margin-top: 100rpx;
  text-align: center;
  padding: 60rpx 40rpx;
}
.vip-icon { font-size: 100rpx; margin-bottom: 20rpx; }
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
.vip-btn { width: 60%; margin: 0 auto; }

/* ===== Disclaimer ===== */
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

/* ===== Tabs ===== */
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

/* ===== Profile loaded tip ===== */
.profile-loaded-tip {
  background: rgba(16, 185, 129, 0.08);
  border: 1rpx solid rgba(16, 185, 129, 0.2);
  border-radius: $radius-lg;
  padding: 16rpx 20rpx;
  margin-bottom: 16rpx;
  font-size: 24rpx;
  color: $accent;
}

/* ===== Form ===== */
.form-title {
  font-size: 32rpx;
  font-weight: 700;
  margin-bottom: 24rpx;
  color: $foreground;
  font-family: 'Calistoga', Georgia, 'PingFang SC', serif;
}

.input-row {
  display: flex;
  gap: 20rpx;
}
.input-half { flex: 1; min-width: 0; }

.radio-group { gap: 16rpx; margin-top: 8rpx; }
.radio-item {
  flex: 1;
  height: 72rpx;
  border: 1rpx solid $border;
  border-radius: $radius-lg;
  font-size: 26rpx;
  color: $muted-foreground;
  transition: all 0.2s;
}
.radio-item.selected {
  border-color: $accent;
  background: rgba(16, 185, 129, 0.06);
  color: $accent;
  font-weight: 500;
  box-shadow: 0 0 0 3px rgba(16, 185, 129, 0.1);
}

.picker-display { margin-top: 8rpx; padding: 8rpx 0; }
.placeholder { color: $uni-text-color-placeholder; }
.arrow { font-size: 32rpx; color: $border; }

/* Days chips */
.days-chips { gap: 16rpx; flex-wrap: wrap; }
.day-chip {
  min-width: 120rpx;
  height: 72rpx;
  border: 2rpx solid $border;
  border-radius: $radius-lg;
  font-size: 28rpx;
  color: $muted-foreground;
  background: $card;
  transition: all 0.2s;
  padding: 0 24rpx;
}
.day-chip.selected {
  border-color: $accent;
  background: rgba(16, 185, 129, 0.08);
  color: $accent;
  font-weight: 600;
  box-shadow: 0 0 0 3px rgba(16, 185, 129, 0.12);
}

.special-textarea {
  width: 100%;
  height: 140rpx;
  font-size: 28rpx;
  margin-top: 8rpx;
  line-height: 1.5;
}
.generate-btn { margin-top: 30rpx; }

/* ===== Loading / Progress Overlay ===== */
.loading-overlay {
  position: fixed;
  top: 0; left: 0; right: 0; bottom: 0;
  background: rgba(15, 23, 42, 0.55);
  z-index: 999;
}
.loading-card {
  width: 85%;
  max-width: 600rpx;
  text-align: center;
  padding: 50rpx 36rpx;
}
.loading-spinner {
  width: 72rpx;
  height: 72rpx;
  border: 6rpx solid $border;
  border-top-color: $accent;
  border-radius: 50%;
  margin: 0 auto 24rpx;
  animation: spin 0.8s linear infinite;
}
@keyframes spin { to { transform: rotate(360deg); } }

.loading-title {
  display: block;
  font-size: 30rpx;
  font-weight: 600;
  color: $foreground;
  margin-bottom: 12rpx;
}
.loading-status {
  display: block;
  font-size: 26rpx;
  color: $accent;
  margin-bottom: 24rpx;
}

/* Progress bar */
.progress-bar-wrap {
  display: flex;
  align-items: center;
  gap: 16rpx;
  margin-bottom: 16rpx;
}
.progress-bar-bg {
  flex: 1;
  height: 16rpx;
  background: $muted;
  border-radius: $radius-full;
  overflow: hidden;
}
.progress-bar-fill {
  height: 100%;
  background: $gradient-accent;
  border-radius: $radius-full;
  transition: width 0.5s ease;
  min-width: 0;
}
.progress-pct {
  font-size: 24rpx;
  color: $accent;
  font-weight: 600;
  min-width: 72rpx;
  text-align: right;
}

.loading-hint {
  display: block;
  font-size: 22rpx;
  margin-bottom: 16rpx;
}
.cancel-btn-wrap { margin-top: 8rpx; }
.btn-cancel {
  background: transparent;
  color: #EF4444;
  border: 2rpx solid #EF4444;
  font-size: 26rpx;
  height: 64rpx;
  line-height: 64rpx;
  border-radius: $radius-lg;
  padding: 0 40rpx;
}

/* ===== Result ===== */
.result-header-card {
  background: $card;
  border: 1rpx solid $border;
  box-shadow: $shadow-sm;
}
.result-title-row { margin-bottom: 8rpx; }
.result-plan-title {
  font-size: 32rpx;
  font-weight: 700;
  color: $foreground;
  font-family: 'Calistoga', Georgia, 'PingFang SC', serif;
}
.result-badge {
  background: rgba(16, 185, 129, 0.1);
  color: $accent;
  font-size: 22rpx;
  padding: 4rpx 16rpx;
  border-radius: $radius-full;
  font-weight: 600;
}
.result-goal {
  display: block;
  font-size: 24rpx;
  margin-bottom: 20rpx;
}

.result-actions {
  gap: 24rpx;
  border-top: 1rpx solid $border;
  padding-top: 20rpx;
}
.action-btn {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4rpx;
}
.action-icon { font-size: 36rpx; }
.action-label { font-size: 22rpx; color: $muted-foreground; }

/* Day blocks */
.day-block { margin-top: 24rpx; }
.day-title-bar {
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12rpx;
  padding: 0 8rpx;
}
.day-title {
  font-size: 30rpx;
  font-weight: 700;
  color: $foreground;
  font-family: 'Calistoga', Georgia, 'PingFang SC', serif;
}
.day-cal { font-size: 24rpx; }

/* Calorie / nutrition card */
.calorie-card {
  background: $gradient-accent;
  border: none;
  box-shadow: $shadow-accent;
  color: #fff;
  border-radius: $radius-2xl;
  margin-bottom: 12rpx;
}
.nutrition-row { gap: 16rpx; }
.nutrition-item {
  text-align: center;
  background: rgba(255, 255, 255, 0.15);
  border: 1rpx solid rgba(255, 255, 255, 0.2);
  border-radius: $radius-lg;
  padding: 16rpx 0;
}
.n-value {
  display: block;
  font-size: 30rpx;
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
  margin-bottom: 12rpx;
}
.meal-header {
  align-items: center;
  gap: 12rpx;
  margin-bottom: 16rpx;
  padding-bottom: 12rpx;
  border-bottom: 1rpx solid $border;
}
.meal-icon { font-size: 34rpx; }
.meal-name {
  font-size: 28rpx;
  font-weight: 700;
  flex: 1;
  font-family: 'Calistoga', Georgia, 'PingFang SC', serif;
  color: $foreground;
}
.meal-calories { font-size: 24rpx; }

.food-item {
  padding: 8rpx 0;
  border-bottom: 1rpx solid rgba(226, 232, 240, 0.5);
}
.food-item:last-child { border-bottom: none; }
.food-text {
  font-size: 26rpx;
  color: $foreground;
  line-height: 1.6;
}

/* Raw content fallback */
.raw-content-card { padding: 24rpx; }
.raw-content {
  font-size: 26rpx;
  color: $foreground;
  line-height: 1.8;
  white-space: pre-wrap;
  word-break: break-word;
}

/* ===== History ===== */
.history-item {
  padding: 0;
  display: flex;
  align-items: stretch;
  overflow: hidden;
}
.history-main {
  flex: 1;
  padding: 20rpx 24rpx;
  min-width: 0;
}
.history-title {
  font-size: 28rpx;
  font-weight: 600;
  color: $foreground;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.history-badges {
  gap: 8rpx;
  flex-shrink: 0;
}
.history-days-badge {
  font-size: 22rpx;
  background: rgba(16, 185, 129, 0.1);
  color: $accent;
  padding: 2rpx 12rpx;
  border-radius: $radius-full;
  font-weight: 600;
}
.history-goal-badge {
  font-size: 22rpx;
  background: $muted;
  color: $muted-foreground;
  padding: 2rpx 12rpx;
  border-radius: $radius-full;
}
.history-date {
  display: block;
  font-size: 22rpx;
  margin-top: 6rpx;
}
.history-delete {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 80rpx;
  border-left: 1rpx solid $border;
  flex-shrink: 0;
}
.delete-icon { font-size: 32rpx; }

.load-more {
  text-align: center;
  padding: 20rpx;
}

/* Loading inline */
.loading-inline {
  padding: 60rpx 0;
  gap: 12rpx;
}
.loading-spinner-sm {
  width: 40rpx;
  height: 40rpx;
  border: 4rpx solid $border;
  border-top-color: $accent;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

/* ===== Empty state ===== */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 80rpx 0;
}
.empty-icon { font-size: 80rpx; margin-bottom: 16rpx; }
.empty-text {
  font-size: 28rpx;
  color: $muted-foreground;
}

/* ===== Picker Modal ===== */
.picker-mask {
  position: fixed;
  top: 0; left: 0; right: 0; bottom: 0;
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
  max-height: 70vh;
  overflow-y: auto;
}
.picker-header {
  padding: 24rpx 30rpx;
  border-bottom: 1rpx solid $border;
  font-size: 28rpx;
  position: sticky;
  top: 0;
  background: $card;
  z-index: 1;
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
  color: $foreground;
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.picker-option.selected {
  color: $accent;
  font-weight: 500;
  background: rgba(16, 185, 129, 0.04);
}
.check-mark {
  color: $accent;
  font-weight: 700;
  font-size: 32rpx;
}

/* ===== Modify Sheet ===== */
.modify-sheet {
  max-height: 60vh;
}
.modify-body {
  padding: 24rpx 30rpx;
}
.modify-textarea {
  width: 100%;
  height: 260rpx;
  font-size: 28rpx;
  line-height: 1.6;
  border: 1rpx solid $border;
  border-radius: $radius-lg;
  padding: 16rpx;
  box-sizing: border-box;
}
</style>
