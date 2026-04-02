<template>
  <view class="page">
    <!-- Loading -->
    <view v-if="pageLoading" class="loading-state">
      <text>加载中...</text>
    </view>

    <!-- ========== Dashboard Mode ========== -->
    <view v-else-if="mode === 'dashboard' && savedProfile" class="dashboard">
      <!-- BMI Card -->
      <view v-if="dashBmi" class="bmi-card" :class="'bmi-' + dashBmi.level">
        <view class="bmi-gauge">
          <text class="bmi-value">{{ dashBmi.value }}</text>
          <text class="bmi-label">BMI</text>
        </view>
        <view class="bmi-detail">
          <view class="bmi-tag" :class="'tag-' + dashBmi.level">{{ dashBmi.status }}</view>
          <text class="bmi-advice">{{ dashBmi.advice }}</text>
        </view>
      </view>

      <!-- Stat Cards -->
      <view class="stat-cards">
        <view class="stat-card" v-if="savedProfile.height">
          <text class="stat-icon">📏</text>
          <text class="stat-val">{{ savedProfile.height }}</text>
          <text class="stat-lbl">身高 (cm)</text>
        </view>
        <view class="stat-card" v-if="savedProfile.weight">
          <text class="stat-icon">⚖️</text>
          <text class="stat-val">{{ savedProfile.weight }}</text>
          <text class="stat-lbl">体重 (kg)</text>
        </view>
        <view class="stat-card" v-if="savedProfile.age">
          <text class="stat-icon">🎂</text>
          <text class="stat-val">{{ savedProfile.age }}</text>
          <text class="stat-lbl">年龄 (岁)</text>
        </view>
        <view class="stat-card" v-if="savedProfile.dailyCalorieTarget">
          <text class="stat-icon">🔥</text>
          <text class="stat-val">{{ savedProfile.dailyCalorieTarget }}</text>
          <text class="stat-lbl">每日推荐 (kcal)</text>
        </view>
        <view class="stat-card" v-if="savedProfile.idealWeightMin">
          <text class="stat-icon">🎯</text>
          <text class="stat-val">{{ savedProfile.idealWeightMin }}~{{ savedProfile.idealWeightMax }}</text>
          <text class="stat-lbl">理想体重 (kg)</text>
        </view>
        <view class="stat-card" v-if="savedProfile.targetWeight">
          <text class="stat-icon">🏁</text>
          <text class="stat-val">{{ savedProfile.targetWeight }}</text>
          <text class="stat-lbl">目标体重 (kg)</text>
        </view>
      </view>

      <!-- Tags -->
      <view class="tag-section" v-if="hasAnyTags">
        <view class="tag-group" v-if="savedProfile.healthGoals?.length">
          <text class="tag-group-title">🎯 饮食目标</text>
          <view class="tag-list">
            <text v-for="g in savedProfile.healthGoals" :key="g" class="tag success">{{ g }}</text>
          </view>
        </view>
        <view class="tag-group" v-if="savedProfile.dietaryRestrictions?.length">
          <text class="tag-group-title">🚫 饮食限制</text>
          <view class="tag-list">
            <text v-for="r in savedProfile.dietaryRestrictions" :key="r" class="tag warning">{{ r }}</text>
          </view>
        </view>
        <view class="tag-group" v-if="savedProfile.allergies?.length">
          <text class="tag-group-title">⚠️ 过敏源</text>
          <view class="tag-list">
            <text v-for="a in savedProfile.allergies" :key="a" class="tag danger">{{ a }}</text>
          </view>
        </view>
        <view class="tag-group" v-if="savedProfile.medicalConditions?.length">
          <text class="tag-group-title">🏥 身体状况</text>
          <view class="tag-list">
            <text v-for="m in savedProfile.medicalConditions" :key="m" class="tag info">{{ m }}</text>
          </view>
        </view>
      </view>

      <!-- Activity Level -->
      <view class="card" v-if="savedProfile.activityLevel">
        <text class="card-title">💪 活动量</text>
        <view class="activity-bar">
          <view
            v-for="(lvl, i) in activityLevels"
            :key="lvl.value"
            class="activity-seg"
            :class="{ active: i <= activityIndex }"
          >
            <text class="seg-label">{{ lvl.label }}</text>
          </view>
        </view>
      </view>

      <!-- Notes -->
      <view class="card" v-if="savedProfile.notes">
        <text class="card-title">📝 备注</text>
        <text class="card-desc">{{ savedProfile.notes }}</text>
      </view>

      <button class="btn-primary edit-btn" @tap="startEdit">✏️ 编辑档案</button>
    </view>

    <!-- ========== Wizard Mode ========== -->
    <view v-else-if="mode === 'wizard'" class="wizard">
      <!-- Step Bar -->
      <view class="step-bar">
        <view
          v-for="(s, i) in steps"
          :key="i"
          class="step-dot"
          :class="{ active: i === currentStep, done: i < currentStep }"
          @tap="i < currentStep && (currentStep = i)"
        >
          <text class="dot">{{ i < currentStep ? '✓' : i + 1 }}</text>
          <text class="step-name">{{ s }}</text>
        </view>
      </view>

      <text class="step-desc">{{ stepDescriptions[currentStep] }}</text>

      <!-- Step 0: Basic body data -->
      <view v-show="currentStep === 0" class="step-content">
        <text class="chip-title">性别</text>
        <view class="gender-select">
          <view
            class="gender-card"
            :class="{ selected: form.gender === 'MALE' }"
            @tap="form.gender = 'MALE'"
          >
            <text class="gender-icon">👨</text>
            <text>男</text>
          </view>
          <view
            class="gender-card"
            :class="{ selected: form.gender === 'FEMALE' }"
            @tap="form.gender = 'FEMALE'"
          >
            <text class="gender-icon">👩</text>
            <text>女</text>
          </view>
        </view>

        <view class="form-group">
          <text class="form-label">出生日期</text>
          <picker mode="date" :value="form.birthDate || ''" :end="todayStr" @change="onBirthDateChange">
            <view class="form-input picker-display">{{ form.birthDate || '请选择日期' }}</view>
          </picker>
        </view>

        <view class="form-row">
          <view class="form-group flex-1">
            <text class="form-label">身高 (cm)</text>
            <input class="form-input" v-model="form.height" type="digit" placeholder="如 170" />
          </view>
          <view class="form-group flex-1">
            <text class="form-label">体重 (kg)</text>
            <input class="form-input" v-model="form.weight" type="digit" placeholder="如 65" />
          </view>
        </view>

        <!-- Live BMI Preview -->
        <view v-if="liveBmi" class="bmi-preview" :class="'bmi-' + liveBmi.level">
          <text class="preview-bmi">BMI: {{ liveBmi.value }}</text>
          <view class="bmi-tag" :class="'tag-' + liveBmi.level">{{ liveBmi.status }}</view>
        </view>
      </view>

      <!-- Step 1: Lifestyle -->
      <view v-show="currentStep === 1" class="step-content">
        <text class="chip-title">日常活动量</text>
        <view class="activity-cards">
          <view
            v-for="lvl in activityLevels"
            :key="lvl.value"
            class="activity-card"
            :class="{ selected: form.activityLevel === lvl.value }"
            @tap="form.activityLevel = lvl.value"
          >
            <text class="activity-emoji">{{ lvl.emoji }}</text>
            <text class="activity-name">{{ lvl.label }}</text>
            <text class="activity-desc">{{ lvl.desc }}</text>
          </view>
        </view>

        <text class="chip-title">饮食目标 <text class="chip-hint">可多选</text></text>
        <view class="chip-group">
          <view
            v-for="g in goalOptions"
            :key="g"
            class="chip"
            :class="{ selected: form.healthGoals.includes(g) }"
            @tap="toggleChip(form.healthGoals, g)"
          >{{ g }}</view>
        </view>
      </view>

      <!-- Step 2: Dietary preferences -->
      <view v-show="currentStep === 2" class="step-content">
        <text class="chip-title">饮食限制 <text class="chip-hint">可多选，无则跳过</text></text>
        <view class="chip-group">
          <view
            v-for="r in restrictionOptions"
            :key="r"
            class="chip"
            :class="{ selected: form.dietaryRestrictions.includes(r) }"
            @tap="toggleChip(form.dietaryRestrictions, r)"
          >{{ r }}</view>
        </view>

        <text class="chip-title">食物过敏 <text class="chip-hint">可多选，无则跳过</text></text>
        <view class="chip-group">
          <view
            v-for="a in allergyOptions"
            :key="a"
            class="chip chip-warning"
            :class="{ selected: form.allergies.includes(a) }"
            @tap="toggleChip(form.allergies, a)"
          >{{ a }}</view>
        </view>

        <text class="chip-title">身体状况 <text class="chip-hint">可多选，无则跳过</text></text>
        <view class="chip-group">
          <view
            v-for="m in conditionOptions"
            :key="m"
            class="chip chip-info"
            :class="{ selected: form.medicalConditions.includes(m) }"
            @tap="toggleChip(form.medicalConditions, m)"
          >{{ m }}</view>
        </view>
      </view>

      <!-- Step 3: Optional extras -->
      <view v-show="currentStep === 3" class="step-content">
        <view class="info-tip">
          <text class="info-tip-text">💡 以下信息均为选填，留空系统会自动计算推荐值</text>
        </view>

        <view class="form-row">
          <view class="form-group flex-1">
            <text class="form-label">目标体重 (kg)</text>
            <input class="form-input" v-model="form.targetWeight" type="digit" placeholder="选填" />
          </view>
          <view class="form-group flex-1">
            <text class="form-label">每日卡路里 (kcal)</text>
            <input class="form-input" v-model="form.dailyCalorieTarget" type="number" placeholder="留空自动计算" />
          </view>
        </view>

        <view class="form-row">
          <view class="form-group flex-1">
            <text class="form-label">腰围 (cm) <text class="optional">选填</text></text>
            <input class="form-input" v-model="form.waistCircumference" type="digit" placeholder="选填" />
          </view>
          <view class="form-group flex-1">
            <text class="form-label">体脂率 (%) <text class="optional">选填</text></text>
            <input class="form-input" v-model="form.bodyFatPercentage" type="digit" placeholder="选填" />
          </view>
        </view>

        <view class="form-group">
          <text class="form-label">备注 <text class="optional">选填</text></text>
          <textarea class="form-textarea" v-model="form.notes" placeholder="其他需要记录的信息" maxlength="1000" />
        </view>
      </view>

      <!-- Wizard footer -->
      <view class="wizard-footer">
        <button v-if="currentStep > 0" class="btn-secondary" @tap="currentStep--">上一步</button>
        <button v-if="isEditing" class="btn-secondary" @tap="cancelEdit">取消</button>
        <button v-if="currentStep < steps.length - 1" class="btn-primary" @tap="nextStep">下一步</button>
        <button v-if="currentStep === steps.length - 1" class="btn-primary" :loading="saving" @tap="handleSave">保存档案</button>
      </view>
    </view>

    <!-- ========== Empty State ========== -->
    <view v-else class="empty-state">
      <text class="empty-icon">📋</text>
      <text class="empty-title">还没有体质档案</text>
      <text class="empty-desc">只需 1 分钟，快速创建您的体质档案，获取个性化的饮食建议</text>
      <button class="btn-primary create-btn" @tap="startCreate">开始创建</button>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, reactive, computed } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { request } from '@/utils/request'
import { checkLogin } from '@/utils/common'

// --- Constants ---
const steps = ['基本信息', '生活习惯', '饮食偏好', '补充信息']
const stepDescriptions = [
  '填写性别、年龄和身体基本数据',
  '选择您的日常活动量和饮食目标',
  '告诉我们您的饮食限制和过敏信息',
  '可选的补充信息，留空也没关系'
]

const activityLevels = [
  { value: 'SEDENTARY', label: '久坐', emoji: '🪑', desc: '办公室工作，很少运动' },
  { value: 'LIGHT', label: '轻度', emoji: '🚶', desc: '偶尔散步或轻度运动' },
  { value: 'MODERATE', label: '中度', emoji: '🏃', desc: '每周运动3-5次' },
  { value: 'ACTIVE', label: '活跃', emoji: '💪', desc: '每天运动或体力劳动' },
  { value: 'VERY_ACTIVE', label: '专业', emoji: '🏋️', desc: '高强度训练或重体力工作' }
]

const goalOptions = ['减重', '增肌', '保持体重', '改善饮食', '控制血糖', '降低血压', '改善睡眠']
const restrictionOptions = ['素食', '纯素', '无麸质', '低碳水', '低脂', '高蛋白', '清真']
const allergyOptions = ['花生', '牛奶', '鸡蛋', '海鲜', '大豆', '坚果', '小麦']
const conditionOptions = ['糖尿病', '高血压', '高血脂', '痛风', '胃炎', '贫血']

// --- State ---
const pageLoading = ref(false)
const saving = ref(false)
const mode = ref<'dashboard' | 'wizard' | 'empty' | 'loading'>('loading')
const isEditing = ref(false)
const currentStep = ref(0)
const savedProfile = ref<any>(null)

const todayStr = (() => {
  const d = new Date()
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
})()

const form = reactive({
  gender: null as string | null,
  birthDate: null as string | null,
  height: '' as string,
  weight: '' as string,
  targetWeight: '' as string,
  activityLevel: null as string | null,
  healthGoals: [] as string[],
  dietaryRestrictions: [] as string[],
  allergies: [] as string[],
  medicalConditions: [] as string[],
  dailyCalorieTarget: '' as string,
  waistCircumference: '' as string,
  bodyFatPercentage: '' as string,
  notes: ''
})

// --- Computed ---
const liveBmi = computed(() => calcBmi(Number(form.height), Number(form.weight)))
const dashBmi = computed(() => savedProfile.value ? calcBmi(Number(savedProfile.value.height), Number(savedProfile.value.weight)) : null)

const activityIndex = computed(() => {
  const idx = activityLevels.findIndex(l => l.value === savedProfile.value?.activityLevel)
  return idx >= 0 ? idx : -1
})

const hasAnyTags = computed(() => {
  const p = savedProfile.value
  return p && (p.healthGoals?.length || p.dietaryRestrictions?.length || p.allergies?.length || p.medicalConditions?.length)
})

// --- BMI calculation ---
function calcBmi(height: number, weight: number) {
  if (!height || !weight) return null
  const h = height / 100
  const val = (weight / (h * h)).toFixed(1)
  const v = Number(val)
  if (v < 18.5) return { value: val, status: '偏瘦', level: 'underweight', advice: '建议增加优质蛋白质和脂肪摄入，配合适当力量训练。' }
  if (v < 24) return { value: val, status: '正常', level: 'normal', advice: '体重正常，请继续保持均衡饮食和适量运动。' }
  if (v < 28) return { value: val, status: '超重', level: 'overweight', advice: '建议控制饮食摄入，增加有氧运动。' }
  return { value: val, status: '肥胖', level: 'obese', advice: '建议咨询营养师制定减重计划。' }
}

// --- Chip toggle ---
function toggleChip(arr: string[], val: string) {
  const i = arr.indexOf(val)
  i >= 0 ? arr.splice(i, 1) : arr.push(val)
}

// --- Date picker ---
function onBirthDateChange(e: any) {
  form.birthDate = e.detail.value
}

// --- Mode switching ---
function startCreate() {
  Object.assign(form, {
    gender: null, birthDate: null, height: '', weight: '', targetWeight: '',
    activityLevel: null, healthGoals: [], dietaryRestrictions: [], allergies: [],
    medicalConditions: [], dailyCalorieTarget: '', waistCircumference: '',
    bodyFatPercentage: '', notes: ''
  })
  isEditing.value = false
  currentStep.value = 0
  mode.value = 'wizard'
}

function startEdit() {
  if (savedProfile.value) {
    const p = savedProfile.value
    Object.assign(form, {
      gender: p.gender || null,
      birthDate: p.birthDate || null,
      height: p.height ? String(p.height) : '',
      weight: p.weight ? String(p.weight) : '',
      targetWeight: p.targetWeight ? String(p.targetWeight) : '',
      activityLevel: p.activityLevel || null,
      healthGoals: [...(p.healthGoals || [])],
      dietaryRestrictions: [...(p.dietaryRestrictions || [])],
      allergies: [...(p.allergies || [])],
      medicalConditions: [...(p.medicalConditions || [])],
      dailyCalorieTarget: p.dailyCalorieTarget ? String(p.dailyCalorieTarget) : '',
      waistCircumference: p.waistCircumference ? String(p.waistCircumference) : '',
      bodyFatPercentage: p.bodyFatPercentage ? String(p.bodyFatPercentage) : '',
      notes: p.notes || ''
    })
  }
  isEditing.value = true
  currentStep.value = 0
  mode.value = 'wizard'
}

function cancelEdit() {
  mode.value = 'dashboard'
}

// --- Wizard navigation ---
function nextStep() {
  if (currentStep.value === 0) {
    if (!form.height || !form.weight) {
      uni.showToast({ title: '请至少填写身高和体重', icon: 'none' })
      return
    }
  }
  currentStep.value++
}

// --- Load & Save ---
async function fetchProfile() {
  pageLoading.value = true
  try {
    const res = await request<any>({ url: '/user/health-profile', showError: false })
    if (res.code === 200 && res.data) {
      savedProfile.value = res.data
      mode.value = 'dashboard'
    } else {
      savedProfile.value = null
      mode.value = 'empty'
    }
  } catch {
    mode.value = 'empty'
  } finally {
    pageLoading.value = false
  }
}

async function handleSave() {
  saving.value = true
  try {
    const payload: any = {
      gender: form.gender,
      birthDate: form.birthDate,
      height: form.height ? Number(form.height) : null,
      weight: form.weight ? Number(form.weight) : null,
      targetWeight: form.targetWeight ? Number(form.targetWeight) : null,
      activityLevel: form.activityLevel,
      healthGoals: form.healthGoals,
      dietaryRestrictions: form.dietaryRestrictions,
      allergies: form.allergies,
      medicalConditions: form.medicalConditions,
      dailyCalorieTarget: form.dailyCalorieTarget ? Number(form.dailyCalorieTarget) : null,
      waistCircumference: form.waistCircumference ? Number(form.waistCircumference) : null,
      bodyFatPercentage: form.bodyFatPercentage ? Number(form.bodyFatPercentage) : null,
      notes: form.notes
    }
    const res = await request<any>({ url: '/user/health-profile', method: 'PUT', data: payload })
    if (res.code === 200) {
      uni.showToast({ title: '体质档案保存成功！', icon: 'success' })
      savedProfile.value = res.data
      mode.value = 'dashboard'
    } else {
      uni.showToast({ title: (res as any).message || '保存失败', icon: 'none' })
    }
  } catch {
    uni.showToast({ title: '保存失败', icon: 'none' })
  } finally {
    saving.value = false
  }
}

onShow(() => {
  if (checkLogin()) fetchProfile()
})
</script>

<style scoped lang="scss">
.page {
  min-height: 100vh;
  background: $background;
  padding: 20rpx 24rpx;
  padding-bottom: calc(30rpx + env(safe-area-inset-bottom));
  font-family: 'Inter', sans-serif;
}

.loading-state {
  display: flex;
  justify-content: center;
  padding: 120rpx 0;
  color: $muted-foreground;
  font-size: 28rpx;
}

/* ============ Dashboard ============ */
.bmi-card {
  background: $card;
  border-radius: $radius-xl;
  padding: 32rpx;
  margin-bottom: 24rpx;
  display: flex;
  align-items: center;
  gap: 28rpx;
  border: 1rpx solid $border;
  box-shadow: $shadow-sm;
}

.bmi-gauge {
  width: 140rpx; height: 140rpx;
  border-radius: $radius-full;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.bmi-underweight .bmi-gauge { background: rgba(96, 165, 250, 0.15); border: 4rpx solid #60A5FA; }
.bmi-normal .bmi-gauge { background: rgba(16, 185, 129, 0.15); border: 4rpx solid $accent; }
.bmi-overweight .bmi-gauge { background: rgba(245, 158, 11, 0.15); border: 4rpx solid $uni-warning; }
.bmi-obese .bmi-gauge { background: rgba(239, 68, 68, 0.15); border: 4rpx solid $uni-error; }

.bmi-value {
  font-size: 40rpx;
  font-weight: 700;
  font-family: 'JetBrains Mono', monospace;
  color: $foreground;
}

.bmi-label {
  font-size: 22rpx;
  color: $muted-foreground;
  margin-top: 2rpx;
}

.bmi-detail { flex: 1; }

.bmi-tag {
  display: inline-block;
  font-size: 24rpx;
  font-weight: 600;
  padding: 6rpx 20rpx;
  border-radius: $radius-full;
  margin-bottom: 8rpx;
}

.tag-underweight { background: rgba(96, 165, 250, 0.15); color: #3B82F6; }
.tag-normal { background: rgba(16, 185, 129, 0.15); color: $accent; }
.tag-overweight { background: rgba(245, 158, 11, 0.15); color: $uni-warning; }
.tag-obese { background: rgba(239, 68, 68, 0.15); color: $uni-error; }

.bmi-advice {
  font-size: 24rpx;
  color: $muted-foreground;
  line-height: 1.6;
  display: block;
}

/* Stat Cards */
.stat-cards {
  display: flex;
  flex-wrap: wrap;
  gap: 16rpx;
  margin-bottom: 24rpx;
}

.stat-card {
  background: $card;
  border-radius: $radius-xl;
  padding: 24rpx;
  flex: 1;
  min-width: 200rpx;
  text-align: center;
  border: 1rpx solid $border;
  box-shadow: $shadow-sm;
}

.stat-icon { font-size: 36rpx; display: block; margin-bottom: 8rpx; }
.stat-val {
  font-size: 32rpx;
  font-weight: 700;
  color: $foreground;
  font-family: 'JetBrains Mono', monospace;
  display: block;
}
.stat-lbl {
  font-size: 22rpx;
  color: $muted-foreground;
  margin-top: 4rpx;
  display: block;
}

/* Tags */
.tag-section {
  background: $card;
  border-radius: $radius-xl;
  padding: 28rpx;
  margin-bottom: 24rpx;
  border: 1rpx solid $border;
  box-shadow: $shadow-sm;
}

.tag-group { margin-bottom: 20rpx; }
.tag-group:last-child { margin-bottom: 0; }

.tag-group-title {
  font-size: 26rpx;
  font-weight: 600;
  color: $foreground;
  display: block;
  margin-bottom: 12rpx;
}

.tag-list {
  display: flex;
  flex-wrap: wrap;
  gap: 12rpx;
}

.tag {
  font-size: 24rpx;
  padding: 8rpx 20rpx;
  border-radius: $radius-full;
  font-weight: 500;
}

.tag.success { background: rgba(16, 185, 129, 0.1); color: $accent; }
.tag.warning { background: rgba(245, 158, 11, 0.1); color: $uni-warning; }
.tag.danger { background: rgba(239, 68, 68, 0.1); color: $uni-error; }
.tag.info { background: rgba(96, 165, 250, 0.1); color: #3B82F6; }

/* Activity Bar */
.card {
  background: $card;
  border-radius: $radius-xl;
  padding: 28rpx;
  margin-bottom: 24rpx;
  border: 1rpx solid $border;
  box-shadow: $shadow-sm;
}

.card-title {
  font-size: 26rpx;
  font-weight: 600;
  color: $foreground;
  display: block;
  margin-bottom: 16rpx;
}

.card-desc {
  font-size: 26rpx;
  color: $muted-foreground;
  line-height: 1.6;
}

.activity-bar {
  display: flex;
  gap: 8rpx;
}

.activity-seg {
  flex: 1;
  height: 40rpx;
  border-radius: $radius-sm;
  background: $muted;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background 0.3s;
}

.activity-seg.active {
  background: $accent;
}

.seg-label {
  font-size: 20rpx;
  color: $muted-foreground;
}

.activity-seg.active .seg-label {
  color: #fff;
  font-weight: 600;
}

.edit-btn {
  margin-top: 8rpx;
}

/* ============ Wizard ============ */
.step-bar {
  display: flex;
  justify-content: space-between;
  margin-bottom: 16rpx;
  padding: 0 8rpx;
  position: relative;
}

.step-dot {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8rpx;
  z-index: 1;
}

.dot {
  width: 52rpx; height: 52rpx;
  border-radius: $radius-full;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24rpx;
  font-weight: 600;
  color: $muted-foreground;
  background: $muted;
  border: 2rpx solid $border;
  transition: all 0.3s;
}

.step-dot.active .dot {
  background: $accent;
  color: #fff;
  border-color: $accent;
}

.step-dot.done .dot {
  background: rgba(16, 185, 129, 0.15);
  color: $accent;
  border-color: $accent;
}

.step-name {
  font-size: 20rpx;
  color: $muted-foreground;
}

.step-dot.active .step-name { color: $accent; font-weight: 600; }

.step-desc {
  font-size: 26rpx;
  color: $muted-foreground;
  text-align: center;
  margin-bottom: 28rpx;
  display: block;
}

.step-content {
  background: $card;
  border-radius: $radius-xl;
  padding: 28rpx;
  border: 1rpx solid $border;
  box-shadow: $shadow-sm;
  margin-bottom: 24rpx;
}

/* Gender select */
.gender-select {
  display: flex;
  gap: 20rpx;
  margin-bottom: 24rpx;
}

.gender-card {
  flex: 1;
  background: $muted;
  border: 2rpx solid $border;
  border-radius: $radius-xl;
  padding: 28rpx;
  text-align: center;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8rpx;
  font-size: 28rpx;
  color: $foreground;
  transition: all 0.2s;
}

.gender-card.selected {
  background: rgba(16, 185, 129, 0.1);
  border-color: $accent;
  color: $accent;
}

.gender-icon { font-size: 48rpx; }

/* Form elements */
.form-group { margin-bottom: 24rpx; }
.form-label {
  display: block;
  font-size: 24rpx;
  color: $muted-foreground;
  margin-bottom: 10rpx;
  font-weight: 500;
}

.form-input {
  background: $muted;
  border: 1rpx solid $border;
  border-radius: $radius-lg;
  padding: 20rpx 24rpx;
  font-size: 28rpx;
  color: $foreground;
  width: 100%;
  max-width: 100%;
  box-sizing: border-box;
}

.picker-display {
  color: $foreground;
}

.form-textarea {
  background: $muted;
  border: 1rpx solid $border;
  border-radius: $radius-lg;
  padding: 20rpx 24rpx;
  font-size: 28rpx;
  color: $foreground;
  width: 100%;
  max-width: 100%;
  min-height: 120rpx;
  box-sizing: border-box;
}

.form-row {
  display: flex;
  gap: 20rpx;
}

.flex-1 { flex: 1; }

.optional {
  font-size: 20rpx;
  color: $muted-foreground;
  font-weight: 400;
}

/* Chips */
.chip-title {
  font-size: 28rpx;
  font-weight: 600;
  color: $foreground;
  display: block;
  margin-bottom: 16rpx;
  margin-top: 8rpx;
}

.chip-hint {
  font-size: 22rpx;
  color: $muted-foreground;
  font-weight: 400;
}

.chip-group {
  display: flex;
  flex-wrap: wrap;
  gap: 16rpx;
  margin-bottom: 24rpx;
}

.chip {
  background: $muted;
  border: 2rpx solid $border;
  border-radius: $radius-full;
  padding: 12rpx 28rpx;
  font-size: 26rpx;
  color: $foreground;
  transition: all 0.2s;
}

.chip.selected {
  background: rgba(16, 185, 129, 0.15);
  border-color: $accent;
  color: $accent;
  font-weight: 600;
}

.chip-warning.selected {
  background: rgba(245, 158, 11, 0.15);
  border-color: $uni-warning;
  color: $uni-warning;
}

.chip-info.selected {
  background: rgba(96, 165, 250, 0.15);
  border-color: #3B82F6;
  color: #3B82F6;
}

/* Activity cards */
.activity-cards {
  display: flex;
  flex-wrap: wrap;
  gap: 16rpx;
  margin-bottom: 28rpx;
}

.activity-card {
  background: $muted;
  border: 2rpx solid $border;
  border-radius: $radius-xl;
  padding: 20rpx 24rpx;
  min-width: 200rpx;
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4rpx;
  text-align: center;
  transition: all 0.2s;
}

.activity-card.selected {
  background: rgba(16, 185, 129, 0.1);
  border-color: $accent;
}

.activity-emoji { font-size: 36rpx; }
.activity-name { font-size: 26rpx; font-weight: 600; color: $foreground; }
.activity-desc { font-size: 20rpx; color: $muted-foreground; }

/* BMI Preview */
.bmi-preview {
  display: flex;
  align-items: center;
  gap: 16rpx;
  padding: 16rpx 20rpx;
  border-radius: $radius-lg;
  margin-top: 16rpx;
}

.bmi-underweight { background: rgba(96, 165, 250, 0.08); }
.bmi-normal { background: rgba(16, 185, 129, 0.08); }
.bmi-overweight { background: rgba(245, 158, 11, 0.08); }
.bmi-obese { background: rgba(239, 68, 68, 0.08); }

.preview-bmi {
  font-size: 28rpx;
  font-weight: 600;
  color: $foreground;
  font-family: 'JetBrains Mono', monospace;
}

/* Info Tip */
.info-tip {
  background: #ECFDF5;
  border-radius: $radius-lg;
  padding: 20rpx 24rpx;
  margin-bottom: 24rpx;
  border: 1rpx solid rgba(16, 185, 129, 0.15);
}

.info-tip-text {
  font-size: 24rpx;
  color: $accent;
}

/* Wizard footer */
.wizard-footer {
  display: flex;
  gap: 16rpx;
  padding: 8rpx 0;
}

.wizard-footer button {
  flex: 1;
  height: 88rpx;
  line-height: 88rpx;
  border-radius: $radius-xl;
  font-size: 30rpx;
  border: none;
  font-family: 'Inter', sans-serif;
}

.btn-primary {
  background: $accent;
  color: #fff;
  box-shadow: $shadow-accent;
}

.btn-primary::after { border: none; }

.btn-secondary {
  background: $muted;
  color: $foreground;
  border: 1rpx solid $border;
}

.btn-secondary::after { border: none; }

/* ============ Empty State ============ */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 120rpx 40rpx;
  text-align: center;
}

.empty-icon { font-size: 100rpx; margin-bottom: 24rpx; }
.empty-title {
  font-size: 36rpx;
  font-weight: 700;
  color: $foreground;
  font-family: 'Calistoga', cursive;
  margin-bottom: 12rpx;
}
.empty-desc {
  font-size: 26rpx;
  color: $muted-foreground;
  line-height: 1.6;
  margin-bottom: 40rpx;
}

.create-btn {
  width: 400rpx;
  height: 88rpx;
  line-height: 88rpx;
  border-radius: $radius-full;
  font-size: 32rpx;
}
</style>
