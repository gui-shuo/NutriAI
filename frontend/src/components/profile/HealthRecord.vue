<template>
  <div class="health-record">
    <el-skeleton :loading="loading" animated :rows="10">
      <!-- ========== 仪表盘模式 ========== -->
      <div v-if="mode === 'dashboard' && savedProfile" class="dashboard">
        <div class="dash-header">
          <h2>我的体质档案</h2>
          <el-button type="primary" round @click="startEdit">
            <el-icon><Edit /></el-icon> 编辑档案
          </el-button>
        </div>

        <!-- BMI 仪表盘卡片 -->
        <div class="bmi-dashboard" v-if="dashBmi">
          <div class="bmi-gauge" :class="dashBmi.level">
            <div class="gauge-circle">
              <span class="gauge-value">{{ dashBmi.value }}</span>
              <span class="gauge-label">BMI</span>
            </div>
          </div>
          <div class="bmi-detail">
            <el-tag :type="dashBmi.tagType" size="large" effect="dark" round>{{ dashBmi.status }}</el-tag>
            <p class="bmi-advice">{{ dashBmi.advice }}</p>
          </div>
        </div>

        <!-- 数据卡片区 -->
        <div class="stat-cards">
          <div class="stat-card" v-if="savedProfile.height">
            <div class="stat-icon">📏</div>
            <div class="stat-value">{{ savedProfile.height }}</div>
            <div class="stat-label">身高 (cm)</div>
          </div>
          <div class="stat-card" v-if="savedProfile.weight">
            <div class="stat-icon">⚖️</div>
            <div class="stat-value">{{ savedProfile.weight }}</div>
            <div class="stat-label">体重 (kg)</div>
          </div>
          <div class="stat-card" v-if="savedProfile.age">
            <div class="stat-icon">🎂</div>
            <div class="stat-value">{{ savedProfile.age }}</div>
            <div class="stat-label">年龄 (岁)</div>
          </div>
          <div class="stat-card" v-if="savedProfile.dailyCalorieTarget">
            <div class="stat-icon">🔥</div>
            <div class="stat-value">{{ savedProfile.dailyCalorieTarget }}</div>
            <div class="stat-label">每日推荐 (kcal)</div>
          </div>
          <div class="stat-card" v-if="savedProfile.idealWeightMin">
            <div class="stat-icon">🎯</div>
            <div class="stat-value">{{ savedProfile.idealWeightMin }}~{{ savedProfile.idealWeightMax }}</div>
            <div class="stat-label">理想体重 (kg)</div>
          </div>
          <div class="stat-card" v-if="savedProfile.targetWeight">
            <div class="stat-icon">🏁</div>
            <div class="stat-value">{{ savedProfile.targetWeight }}</div>
            <div class="stat-label">目标体重 (kg)</div>
          </div>
        </div>

        <!-- 标签区 -->
        <div class="tag-section" v-if="hasAnyTags">
          <div class="tag-group" v-if="savedProfile.healthGoals?.length">
            <h4>🎯 饮食目标</h4>
            <div class="tags">
              <el-tag v-for="g in savedProfile.healthGoals" :key="g" type="success" round>{{ g }}</el-tag>
            </div>
          </div>
          <div class="tag-group" v-if="savedProfile.dietaryRestrictions?.length">
            <h4>🚫 饮食限制</h4>
            <div class="tags">
              <el-tag v-for="r in savedProfile.dietaryRestrictions" :key="r" type="warning" round>{{ r }}</el-tag>
            </div>
          </div>
          <div class="tag-group" v-if="savedProfile.allergies?.length">
            <h4>⚠️ 过敏源</h4>
            <div class="tags">
              <el-tag v-for="a in savedProfile.allergies" :key="a" type="danger" round>{{ a }}</el-tag>
            </div>
          </div>
          <div class="tag-group" v-if="savedProfile.medicalConditions?.length">
            <h4>🏥 身体状况</h4>
            <div class="tags">
              <el-tag v-for="m in savedProfile.medicalConditions" :key="m" type="info" round>{{ m }}</el-tag>
            </div>
          </div>
        </div>

        <!-- 活动量 -->
        <div class="activity-display" v-if="savedProfile.activityLevel">
          <h4>💪 活动量</h4>
          <div class="activity-bar">
            <div
              v-for="(lvl, i) in activityLevels"
              :key="lvl.value"
              class="activity-segment"
              :class="{ active: i <= activityIndex }"
            >
              <span class="segment-label">{{ lvl.label }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- ========== 向导模式 ========== -->
      <div v-else-if="mode === 'wizard'" class="wizard">
        <div class="wizard-header">
          <h2>{{ isEditing ? '编辑体质档案' : '创建体质档案' }}</h2>
          <p class="wizard-subtitle">{{ stepDescriptions[currentStep] }}</p>
        </div>

        <!-- 步骤条 -->
        <div class="step-bar">
          <div
            v-for="(s, i) in steps"
            :key="i"
            class="step-dot"
            :class="{ active: i === currentStep, done: i < currentStep }"
            @click="i < currentStep && (currentStep = i)"
          >
            <span class="dot">{{ i < currentStep ? '✓' : i + 1 }}</span>
            <span class="step-name">{{ s }}</span>
          </div>
          <div class="step-line">
            <div class="step-line-fill" :style="{ width: (currentStep / (steps.length - 1)) * 100 + '%' }"></div>
          </div>
        </div>

        <!-- Step 0: 基本身体数据 -->
        <div v-show="currentStep === 0" class="step-content">
          <div class="gender-select">
            <div
              class="gender-card"
              :class="{ selected: form.gender === 'MALE' }"
              @click="form.gender = 'MALE'"
            >
              <span class="gender-icon">👨</span>
              <span>男</span>
            </div>
            <div
              class="gender-card"
              :class="{ selected: form.gender === 'FEMALE' }"
              @click="form.gender = 'FEMALE'"
            >
              <span class="gender-icon">👩</span>
              <span>女</span>
            </div>
          </div>

          <div class="input-row">
            <div class="input-card">
              <label>出生日期</label>
              <el-date-picker
                v-model="form.birthDate"
                type="date"
                placeholder="选择日期"
                format="YYYY-MM-DD"
                value-format="YYYY-MM-DD"
                :disabled-date="d => d > new Date()"
                style="width: 100%"
              />
            </div>
          </div>

          <div class="input-row two-col">
            <div class="input-card">
              <label>身高 (cm)</label>
              <el-input-number v-model="form.height" :min="50" :max="300" :step="1" :precision="1" controls-position="right" style="width: 100%" />
            </div>
            <div class="input-card">
              <label>体重 (kg)</label>
              <el-input-number v-model="form.weight" :min="20" :max="500" :step="0.5" :precision="1" controls-position="right" style="width: 100%" />
            </div>
          </div>

          <!-- 实时 BMI 预览 -->
          <div v-if="liveBmi" class="bmi-preview" :class="liveBmi.level">
            <span class="preview-bmi">BMI: {{ liveBmi.value }}</span>
            <el-tag :type="liveBmi.tagType" size="small" round>{{ liveBmi.status }}</el-tag>
          </div>
        </div>

        <!-- Step 1: 生活习惯 -->
        <div v-show="currentStep === 1" class="step-content">
          <h3 class="chip-title">日常活动量</h3>
          <div class="activity-cards">
            <div
              v-for="lvl in activityLevels"
              :key="lvl.value"
              class="activity-card"
              :class="{ selected: form.activityLevel === lvl.value }"
              @click="form.activityLevel = lvl.value"
            >
              <span class="activity-emoji">{{ lvl.emoji }}</span>
              <span class="activity-name">{{ lvl.label }}</span>
              <span class="activity-desc">{{ lvl.desc }}</span>
            </div>
          </div>

          <h3 class="chip-title">饮食目标 <span class="chip-hint">可多选</span></h3>
          <div class="chip-group">
            <div
              v-for="g in goalOptions"
              :key="g"
              class="chip"
              :class="{ selected: form.healthGoals.includes(g) }"
              @click="toggleChip(form.healthGoals, g)"
            >{{ g }}</div>
          </div>
        </div>

        <!-- Step 2: 饮食偏好 -->
        <div v-show="currentStep === 2" class="step-content">
          <h3 class="chip-title">饮食限制 <span class="chip-hint">可多选，无则跳过</span></h3>
          <div class="chip-group">
            <div
              v-for="r in restrictionOptions"
              :key="r"
              class="chip"
              :class="{ selected: form.dietaryRestrictions.includes(r) }"
              @click="toggleChip(form.dietaryRestrictions, r)"
            >{{ r }}</div>
          </div>

          <h3 class="chip-title">食物过敏 <span class="chip-hint">可多选，无则跳过</span></h3>
          <div class="chip-group">
            <div
              v-for="a in allergyOptions"
              :key="a"
              class="chip warning"
              :class="{ selected: form.allergies.includes(a) }"
              @click="toggleChip(form.allergies, a)"
            >{{ a }}</div>
          </div>

          <h3 class="chip-title">身体状况 <span class="chip-hint">可多选，无则跳过</span></h3>
          <div class="chip-group">
            <div
              v-for="m in conditionOptions"
              :key="m"
              class="chip info"
              :class="{ selected: form.medicalConditions.includes(m) }"
              @click="toggleChip(form.medicalConditions, m)"
            >{{ m }}</div>
          </div>
        </div>

        <!-- Step 3: 补充信息（可选） -->
        <div v-show="currentStep === 3" class="step-content">
          <el-alert type="info" :closable="false" show-icon style="margin-bottom: 20px">
            以下信息均为选填，留空系统会自动计算推荐值
          </el-alert>

          <div class="input-row two-col">
            <div class="input-card">
              <label>目标体重 (kg)</label>
              <el-input-number v-model="form.targetWeight" :min="20" :max="500" :step="0.5" :precision="1" controls-position="right" style="width: 100%" placeholder="选填" />
            </div>
            <div class="input-card">
              <label>每日卡路里目标 (kcal)</label>
              <el-input-number v-model="form.dailyCalorieTarget" :min="500" :max="10000" :step="50" controls-position="right" style="width: 100%" placeholder="选填" />
              <span class="input-hint">留空自动计算</span>
            </div>
          </div>

          <div class="input-row two-col">
            <div class="input-card">
              <label>腰围 (cm) <span class="optional">选填</span></label>
              <el-input-number v-model="form.waistCircumference" :min="30" :max="300" :step="0.5" :precision="1" controls-position="right" style="width: 100%" />
            </div>
            <div class="input-card">
              <label>体脂率 (%) <span class="optional">选填</span></label>
              <el-input-number v-model="form.bodyFatPercentage" :min="1" :max="80" :step="0.1" :precision="1" controls-position="right" style="width: 100%" />
            </div>
          </div>

          <div class="input-row">
            <div class="input-card">
              <label>备注 <span class="optional">选填</span></label>
              <el-input v-model="form.notes" type="textarea" :rows="3" maxlength="1000" show-word-limit placeholder="其他需要记录的信息" />
            </div>
          </div>
        </div>

        <!-- 底部按钮 -->
        <div class="wizard-footer">
          <el-button v-if="currentStep > 0" round @click="currentStep--">
            上一步
          </el-button>
          <el-button v-if="isEditing" round @click="cancelEdit">
            取消
          </el-button>
          <el-button
            v-if="currentStep < steps.length - 1"
            type="primary"
            round
            @click="nextStep"
          >
            下一步
          </el-button>
          <el-button
            v-if="currentStep === steps.length - 1"
            type="primary"
            round
            :loading="saving"
            @click="handleSave"
          >
            {{ currentStep === 3 ? '保存档案' : '完成' }}
          </el-button>
        </div>
      </div>

      <!-- ========== 空状态 ========== -->
      <div v-else class="empty-state">
        <div class="empty-illustration">📋</div>
        <h2>还没有体质档案</h2>
        <p>只需 1 分钟，快速创建您的体质档案，<br/>获取个性化的饮食建议</p>
        <el-button type="primary" size="large" round @click="startCreate">
          开始创建
        </el-button>
      </div>
    </el-skeleton>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { Edit } from '@element-plus/icons-vue'
import api from '@/services/api'
import message from '@/utils/message'

// ---- 常量 ----
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

// ---- 状态 ----
const loading = ref(false)
const saving = ref(false)
const mode = ref('loading') // 'dashboard' | 'wizard' | 'empty' | 'loading'
const isEditing = ref(false)
const currentStep = ref(0)
const savedProfile = ref(null)

const form = reactive({
  gender: null,
  birthDate: null,
  height: null,
  weight: null,
  targetWeight: null,
  activityLevel: null,
  healthGoals: [],
  dietaryRestrictions: [],
  allergies: [],
  medicalConditions: [],
  dailyCalorieTarget: null,
  waistCircumference: null,
  bodyFatPercentage: null,
  notes: ''
})

// ---- 计算属性 ----
const liveBmi = computed(() => calcBmi(form.height, form.weight))
const dashBmi = computed(() => savedProfile.value ? calcBmi(Number(savedProfile.value.height), Number(savedProfile.value.weight)) : null)

const activityIndex = computed(() => {
  const idx = activityLevels.findIndex(l => l.value === savedProfile.value?.activityLevel)
  return idx >= 0 ? idx : -1
})

const hasAnyTags = computed(() => {
  const p = savedProfile.value
  return p && (p.healthGoals?.length || p.dietaryRestrictions?.length || p.allergies?.length || p.medicalConditions?.length)
})

// ---- BMI 计算 ----
function calcBmi(height, weight) {
  if (!height || !weight) return null
  const h = height / 100
  const val = (weight / (h * h)).toFixed(1)
  if (val < 18.5) return { value: val, status: '偏瘦', level: 'underweight', tagType: 'info', advice: '建议增加优质蛋白质和脂肪摄入，配合适当力量训练。' }
  if (val < 24) return { value: val, status: '正常', level: 'normal', tagType: 'success', advice: '体重正常，请继续保持均衡饮食和适量运动。' }
  if (val < 28) return { value: val, status: '超重', level: 'overweight', tagType: 'warning', advice: '建议控制饮食摄入，增加有氧运动。' }
  return { value: val, status: '肥胖', level: 'obese', tagType: 'danger', advice: '建议咨询营养师制定减重计划。' }
}

// ---- Chip 切换 ----
function toggleChip(arr, val) {
  const i = arr.indexOf(val)
  i >= 0 ? arr.splice(i, 1) : arr.push(val)
}

// ---- 模式切换 ----
function startCreate() {
  Object.assign(form, { gender: null, birthDate: null, height: null, weight: null, targetWeight: null, activityLevel: null, healthGoals: [], dietaryRestrictions: [], allergies: [], medicalConditions: [], dailyCalorieTarget: null, waistCircumference: null, bodyFatPercentage: null, notes: '' })
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
      height: p.height ? Number(p.height) : null,
      weight: p.weight ? Number(p.weight) : null,
      targetWeight: p.targetWeight ? Number(p.targetWeight) : null,
      activityLevel: p.activityLevel || null,
      healthGoals: [...(p.healthGoals || [])],
      dietaryRestrictions: [...(p.dietaryRestrictions || [])],
      allergies: [...(p.allergies || [])],
      medicalConditions: [...(p.medicalConditions || [])],
      dailyCalorieTarget: p.dailyCalorieTarget || null,
      waistCircumference: p.waistCircumference ? Number(p.waistCircumference) : null,
      bodyFatPercentage: p.bodyFatPercentage ? Number(p.bodyFatPercentage) : null,
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

// ---- 向导步骤 ----
function nextStep() {
  if (currentStep.value === 0) {
    if (!form.height || !form.weight) {
      message.warning('请至少填写身高和体重')
      return
    }
  }
  currentStep.value++
}

// ---- 加载 & 保存 ----
async function fetchProfile() {
  loading.value = true
  try {
    const res = await api.get('/user/health-profile')
    if (res.data.code === 200 && res.data.data) {
      savedProfile.value = res.data.data
      mode.value = 'dashboard'
    } else {
      savedProfile.value = null
      mode.value = 'empty'
    }
  } catch {
    message.error('获取体质档案失败')
    mode.value = 'empty'
  } finally {
    loading.value = false
  }
}

async function handleSave() {
  saving.value = true
  try {
    const res = await api.put('/user/health-profile', {
      gender: form.gender,
      birthDate: form.birthDate,
      height: form.height,
      weight: form.weight,
      targetWeight: form.targetWeight,
      activityLevel: form.activityLevel,
      healthGoals: form.healthGoals,
      dietaryRestrictions: form.dietaryRestrictions,
      allergies: form.allergies,
      medicalConditions: form.medicalConditions,
      dailyCalorieTarget: form.dailyCalorieTarget,
      waistCircumference: form.waistCircumference,
      bodyFatPercentage: form.bodyFatPercentage,
      notes: form.notes
    })
    if (res.data.code === 200) {
      message.success('体质档案保存成功！')
      savedProfile.value = res.data.data
      mode.value = 'dashboard'
    } else {
      message.error(res.data.message || '保存失败')
    }
  } catch (e) {
    message.error(e.response?.data?.message || '保存失败，请稍后重试')
  } finally {
    saving.value = false
  }
}

onMounted(fetchProfile)
</script>

<style scoped lang="scss">
.health-record { max-width: 720px; }

/* ===== 空状态 ===== */
.empty-state {
  text-align: center;
  padding: 60px 20px;
  .empty-illustration { font-size: 80px; margin-bottom: 16px; }
  h2 { font-size: 22px; color: #0F172A; margin: 0 0 12px; font-family: 'Calistoga', serif; }
  p { color: #64748B; line-height: 1.8; margin-bottom: 28px; }
}

/* ===== 仪表盘 ===== */
.dash-header {
  display: flex; justify-content: space-between; align-items: center; margin-bottom: 24px;
  h2 { margin: 0; font-size: 22px; color: #0F172A; font-family: 'Calistoga', serif; }
}

.bmi-dashboard {
  display: flex; align-items: center; gap: 28px;
  background: #FAFAFA;
  border: 1px solid #E2E8F0;
  border-radius: 12px;
  padding: 28px; margin-bottom: 24px;
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1), 0 2px 4px -2px rgba(0, 0, 0, 0.1);
}

.bmi-gauge {
  .gauge-circle {
    width: 110px; height: 110px; border-radius: 50%; display: flex; flex-direction: column;
    align-items: center; justify-content: center; background: #FAFAFA;
    box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1), 0 2px 4px -2px rgba(0, 0, 0, 0.1); border: 4px solid #E2E8F0;
    .gauge-value { font-size: 32px; font-weight: 700; line-height: 1.1; }
    .gauge-label { font-size: 13px; color: #64748B; }
  }
  &.underweight .gauge-circle { border-color: #64748B; .gauge-value { color: #64748B; } }
  &.normal .gauge-circle { border-color: #67c23a; .gauge-value { color: #67c23a; } }
  &.overweight .gauge-circle { border-color: #e6a23c; .gauge-value { color: #e6a23c; } }
  &.obese .gauge-circle { border-color: #f56c6c; .gauge-value { color: #f56c6c; } }
}

.bmi-detail {
  flex: 1;
  .bmi-advice { margin-top: 10px; color: #0F172A; font-size: 14px; line-height: 1.6; }
}

.stat-cards {
  display: grid; grid-template-columns: repeat(3, 1fr); gap: 14px; margin-bottom: 24px;
}

.stat-card {
  background: #FAFAFA; border-radius: 12px; padding: 18px 14px; text-align: center;
  border: 1px solid #E2E8F0; box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1), 0 2px 4px -2px rgba(0, 0, 0, 0.1); transition: transform 0.2s;
  &:hover { transform: translateY(-2px); }
  .stat-icon { font-size: 28px; margin-bottom: 6px; }
  .stat-value { font-size: 22px; font-weight: 700; color: #0F172A; }
  .stat-label { font-size: 12px; color: #64748B; margin-top: 4px; }
}

.tag-section {
  background: #FAFAFA; border-radius: 12px; padding: 20px; margin-bottom: 20px;
  border: 1px solid #E2E8F0;
}

.tag-group {
  margin-bottom: 16px;
  &:last-child { margin-bottom: 0; }
  h4 { font-size: 14px; color: #0F172A; margin: 0 0 10px; font-family: 'Calistoga', serif; }
  .tags { display: flex; flex-wrap: wrap; gap: 8px; }
}

.activity-display {
  background: #FAFAFA; border-radius: 12px; padding: 20px;
  border: 1px solid #E2E8F0;
  h4 { font-size: 14px; color: #0F172A; margin: 0 0 14px; font-family: 'Calistoga', serif; }
}

.activity-bar {
  display: flex; gap: 6px;
  .activity-segment {
    flex: 1; height: 32px; border-radius: 8px; background: #F1F5F9;
    display: flex; align-items: center; justify-content: center; transition: all 0.3s;
    .segment-label { font-size: 12px; color: #c0c4cc; }
    &.active {
      background: #0052FF;
      .segment-label { color: white; font-weight: 600; }
    }
  }
}

/* ===== 向导 ===== */
.wizard-header {
  text-align: center; margin-bottom: 8px;
  h2 { margin: 0 0 8px; font-size: 22px; color: #0F172A; font-family: 'Calistoga', serif; }
  .wizard-subtitle { color: #64748B; font-size: 14px; margin: 0; }
}

.step-bar {
  display: flex; justify-content: space-between; position: relative; padding: 20px 0 30px; margin: 0 20px;
  .step-line {
    position: absolute; top: 34px; left: 30px; right: 30px; height: 3px; background: #F1F5F9; border-radius: 2px; z-index: 0;
    .step-line-fill { height: 100%; background: #0052FF; border-radius: 2px; transition: width 0.4s ease; }
  }
  .step-dot {
    display: flex; flex-direction: column; align-items: center; z-index: 1; cursor: default;
    .dot {
      width: 32px; height: 32px; border-radius: 50%; background: #F1F5F9; color: #64748B;
      display: flex; align-items: center; justify-content: center; font-size: 14px; font-weight: 600;
      transition: all 0.3s;
    }
    .step-name { font-size: 12px; color: #64748B; margin-top: 6px; }
    &.active .dot { background: #0052FF; color: white; box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1), 0 2px 4px -2px rgba(0, 0, 0, 0.1); }
    &.done { cursor: pointer;
      .dot { background: #67c23a; color: white; }
      .step-name { color: #67c23a; }
    }
  }
}

.step-content { min-height: 300px; padding: 10px 0; }

/* Gender select */
.gender-select {
  display: flex; gap: 16px; justify-content: center; margin-bottom: 24px;
  .gender-card {
    width: 140px; padding: 24px 16px; border-radius: 12px; text-align: center; cursor: pointer;
    border: 1px solid #E2E8F0; background: #FAFAFA; transition: all 0.3s; display: flex; flex-direction: column; align-items: center; gap: 8px;
    .gender-icon { font-size: 40px; }
    span:last-child { font-size: 15px; color: #0F172A; font-weight: 500; }
    &:hover { border-color: #0F172A; background: #FAFAFA; }
    &.selected { border-color: #0052FF; background: #F1F5F9; box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1), 0 2px 4px -2px rgba(0, 0, 0, 0.1); }
  }
}

/* Input cards */
.input-row {
  display: flex; gap: 16px; margin-bottom: 16px;
  &.two-col .input-card { flex: 1; }
  .input-card {
    flex: 1; background: #FAFAFA; border-radius: 12px; padding: 14px 16px; border: 1px solid #E2E8F0;
    label { display: block; font-size: 13px; color: #0F172A; font-weight: 500; margin-bottom: 8px;
      .optional { color: #64748B; font-weight: 400; }
    }
    .input-hint { font-size: 12px; color: #64748B; margin-top: 4px; }
  }
}

/* BMI preview */
.bmi-preview {
  display: flex; align-items: center; justify-content: center; gap: 12px;
  padding: 12px; border-radius: 10px; margin-top: 8px; background: #FAFAFA;
  &.normal { background: rgba(103,194,58,0.08); }
  &.underweight { background: rgba(144,147,153,0.08); }
  &.overweight { background: rgba(230,162,60,0.08); }
  &.obese { background: rgba(245,108,108,0.08); }
  .preview-bmi { font-size: 16px; font-weight: 600; color: #0F172A; }
}

/* Activity cards */
.activity-cards {
  display: flex; gap: 10px; flex-wrap: wrap; margin-bottom: 24px;
  .activity-card {
    flex: 1; min-width: 120px; padding: 16px 10px; border-radius: 14px; text-align: center; cursor: pointer;
    border: 1px solid #E2E8F0; transition: all 0.3s; display: flex; flex-direction: column; align-items: center; gap: 4px;
    .activity-emoji { font-size: 28px; }
    .activity-name { font-size: 14px; font-weight: 600; color: #0F172A; }
    .activity-desc { font-size: 11px; color: #64748B; line-height: 1.3; }
    &:hover { border-color: #0F172A; }
    &.selected { border-color: #0052FF; background: rgba(0, 82, 255,0.06); box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1), 0 2px 4px -2px rgba(0, 0, 0, 0.1); }
  }
}

/* Chip groups */
.chip-title {
  font-size: 15px; color: #0F172A; margin: 0 0 12px; font-weight: 600; font-family: 'Calistoga', serif;
  .chip-hint { font-size: 12px; color: #64748B; font-weight: 400; margin-left: 8px; }
}

.chip-group {
  display: flex; flex-wrap: wrap; gap: 10px; margin-bottom: 24px;
  .chip {
    padding: 8px 18px; border-radius: 12px; font-size: 14px; cursor: pointer;
    border: 1px solid #E2E8F0; color: #0F172A; background: #FAFAFA; transition: all 0.25s; user-select: none;
    &:hover { border-color: #0F172A; background: #FAFAFA; }
    &.selected { border-color: #0052FF; color: #0052FF; background: #FAFAFA; font-weight: 500; }
    &.warning.selected { border-color: #EF4444; color: #EF4444; background: #FAFAFA; }
    &.info.selected { border-color: #64748B; color: #0F172A; background: rgba(144,147,153,0.08); }
  }
}

/* Wizard footer */
.wizard-footer {
  display: flex; justify-content: center; gap: 12px; padding: 20px 0 10px;
  border-top: 1px solid #E2E8F0; margin-top: 10px;
}

/* ===== 响应式 ===== */
@media (max-width: 640px) {
  .stat-cards { grid-template-columns: repeat(2, 1fr); }
  .bmi-dashboard { flex-direction: column; text-align: center; }
  .activity-cards { .activity-card { min-width: 100px; } }
  .input-row.two-col { flex-direction: column; }
}
</style>
