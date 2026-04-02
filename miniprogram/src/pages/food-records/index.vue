<template>
  <view class="container">
    <!-- Date Selector with working picker -->
    <picker mode="date" :value="currentDate" :end="todayStr" @change="onDatePick">
      <view class="date-selector card flex-between">
        <view class="date-arrow flex-center" @tap.stop="changeDate(-1)">
          <text class="arrow-icon">‹</text>
        </view>
        <view class="date-display flex-center">
          <text class="date-text">{{ displayDate }}</text>
          <text v-if="isToday" class="today-badge">今天</text>
          <text class="date-picker-hint">📅</text>
        </view>
        <view class="date-arrow flex-center" @tap.stop="changeDate(1)">
          <text class="arrow-icon">›</text>
        </view>
      </view>
    </picker>

    <!-- Daily Summary with macro rings -->
    <view class="card summary-card">
      <view class="summary-header flex-between">
        <text class="summary-title">📊 今日营养摄入</text>
        <text class="summary-cal">{{ stats.totalCalories || 0 }} / {{ dailyGoal.calories }} kcal</text>
      </view>

      <!-- Macro ring charts -->
      <view class="macro-rings flex-between">
        <view class="ring-item" v-for="macro in macroRings" :key="macro.key">
          <view class="ring-wrapper">
            <canvas
              :canvas-id="'ring-' + macro.key"
              :id="'ring-' + macro.key"
              class="ring-canvas"
              style="width: 100rpx; height: 100rpx;"
            />
            <text class="ring-percent">{{ macro.percent }}%</text>
          </view>
          <text class="ring-label">{{ macro.icon }} {{ macro.label }}</text>
          <text class="ring-value">{{ macro.current }}/{{ macro.goal }}{{ macro.unit }}</text>
        </view>
      </view>

      <!-- Progress bars -->
      <view class="progress-section">
        <view class="progress-item">
          <view class="flex-between">
            <text class="p-label">🔥 热量</text>
            <text class="p-value">{{ stats.totalCalories || 0 }} / {{ dailyGoal.calories }} kcal</text>
          </view>
          <view class="progress-bar">
            <view class="progress-fill calories-fill" :style="{ width: calcPercent(stats.totalCalories, dailyGoal.calories) + '%' }" />
          </view>
        </view>
        <view class="progress-item">
          <view class="flex-between">
            <text class="p-label">🥩 蛋白质</text>
            <text class="p-value">{{ stats.totalProtein || 0 }} / {{ dailyGoal.protein }}g</text>
          </view>
          <view class="progress-bar">
            <view class="progress-fill protein-fill" :style="{ width: calcPercent(stats.totalProtein, dailyGoal.protein) + '%' }" />
          </view>
        </view>
        <view class="progress-item">
          <view class="flex-between">
            <text class="p-label">🧈 脂肪</text>
            <text class="p-value">{{ stats.totalFat || 0 }} / {{ dailyGoal.fat }}g</text>
          </view>
          <view class="progress-bar">
            <view class="progress-fill fat-fill" :style="{ width: calcPercent(stats.totalFat, dailyGoal.fat) + '%' }" />
          </view>
        </view>
        <view class="progress-item">
          <view class="flex-between">
            <text class="p-label">🍚 碳水</text>
            <text class="p-value">{{ stats.totalCarbs || 0 }} / {{ dailyGoal.carbs }}g</text>
          </view>
          <view class="progress-bar">
            <view class="progress-fill carbs-fill" :style="{ width: calcPercent(stats.totalCarbs, dailyGoal.carbs) + '%' }" />
          </view>
        </view>
      </view>
    </view>

    <!-- Meal type filter tabs -->
    <scroll-view scroll-x class="filter-tabs-scroll">
      <view class="filter-tabs flex">
        <view
          class="filter-tab"
          :class="{ active: activeMealFilter === 'ALL' }"
          @tap="setMealFilter('ALL')"
        >
          <text>🍽️ 全部</text>
          <text class="tab-count">{{ records.length }}</text>
        </view>
        <view
          class="filter-tab"
          v-for="m in mealTypes"
          :key="m.value"
          :class="{ active: activeMealFilter === m.value }"
          @tap="setMealFilter(m.value)"
        >
          <text>{{ m.icon }} {{ m.label }}</text>
          <text class="tab-count">{{ getMealRecords(m.value).length }}</text>
        </view>
      </view>
    </scroll-view>

    <!-- Meal Sections -->
    <view
      class="meal-section card"
      v-for="meal in filteredMealTypes"
      :key="meal.value"
    >
      <view class="meal-header flex-between">
        <view class="flex" style="align-items: center; gap: 12rpx;">
          <text class="meal-icon">{{ meal.icon }}</text>
          <text class="meal-name">{{ meal.label }}</text>
          <text class="meal-count-badge">{{ getMealRecords(meal.value).length }}项</text>
          <text class="meal-cal text-secondary">{{ getMealCalories(meal.value) }} kcal</text>
        </view>
        <view class="add-btn flex-center" @tap="openAddDialog(meal.value)">
          <text class="add-icon">+</text>
        </view>
      </view>

      <view v-if="getMealRecords(meal.value).length === 0" class="meal-empty">
        <text class="text-secondary">暂无记录，点击 + 添加</text>
      </view>

      <view v-else class="food-list">
        <view
          class="food-item"
          v-for="record in getMealRecords(meal.value)"
          :key="record.id"
          @touchstart="onTouchStart($event, record)"
          @touchmove="onTouchMove($event, record)"
          @touchend="onTouchEnd(record)"
        >
          <view
            class="food-item-content flex-between"
            :style="{ transform: `translateX(${record._offsetX || 0}px)` }"
            @tap="openDetail(record)"
          >
            <view class="food-info">
              <text class="food-name">{{ record.foodName }}</text>
              <text class="food-amount text-secondary"> {{ record.amount || record.portion }}g</text>
            </view>
            <view class="food-meta">
              <text class="food-cal">{{ record.calories }} kcal</text>
              <text class="food-macros-hint">点击查看详情 ›</text>
            </view>
          </view>
          <view
            class="delete-action flex-center"
            v-if="record._offsetX && record._offsetX < -30"
            @tap.stop="deleteRecord(record)"
          >
            <text>删除</text>
          </view>
        </view>
      </view>
    </view>

    <!-- Empty state when filtering -->
    <view v-if="filteredMealTypes.length === 0 || (filteredMealTypes.length === 1 && getMealRecords(filteredMealTypes[0].value).length === 0)" class="empty-state card">
      <text class="empty-icon">🍽️</text>
      <text class="empty-text">暂无{{ activeMealFilter === 'ALL' ? '' : filterLabel }}记录</text>
      <view class="btn-primary empty-btn" @tap="openAddDialog(activeMealFilter === 'ALL' ? 'BREAKFAST' : activeMealFilter)">
        <text style="color: #fff;">+ 添加记录</text>
      </view>
    </view>

    <!-- Food Detail Popup -->
    <view class="dialog-mask" v-if="showDetail" @tap="showDetail = false">
      <view class="dialog-content detail-dialog" @tap.stop>
        <view class="dialog-header flex-between">
          <text class="dialog-title">🔍 食物详情</text>
          <text class="dialog-close" @tap="showDetail = false">✕</text>
        </view>

        <view class="detail-body" v-if="detailRecord">
          <!-- Food title -->
          <view class="detail-title-row flex-between">
            <view>
              <text class="detail-food-name">{{ detailRecord.foodName }}</text>
              <view class="detail-meal-badge">
                <text>{{ getMealLabel(detailRecord.mealType) }}</text>
              </view>
            </view>
            <text class="detail-cal">{{ detailRecord.calories }} kcal</text>
          </view>

          <!-- Basic info -->
          <view class="detail-section">
            <text class="detail-section-title">基本信息</text>
            <view class="detail-grid">
              <view class="detail-item">
                <text class="detail-item-label">份量</text>
                <text class="detail-item-value">{{ detailRecord.amount || detailRecord.portion || '-' }}g</text>
              </view>
              <view class="detail-item">
                <text class="detail-item-label">热量</text>
                <text class="detail-item-value highlight">{{ detailRecord.calories || 0 }} kcal</text>
              </view>
              <view class="detail-item">
                <text class="detail-item-label">记录时间</text>
                <text class="detail-item-value">{{ formatRecordTime(detailRecord.recordTime || detailRecord.createdAt) }}</text>
              </view>
            </view>
          </view>

          <!-- Macro nutrition detail -->
          <view class="detail-section">
            <text class="detail-section-title">营养成分</text>
            <view class="detail-grid">
              <view class="detail-item">
                <text class="detail-item-label">🥩 蛋白质</text>
                <text class="detail-item-value">{{ detailRecord.protein || 0 }}g</text>
              </view>
              <view class="detail-item">
                <text class="detail-item-label">🧈 脂肪</text>
                <text class="detail-item-value">{{ detailRecord.fat || 0 }}g</text>
              </view>
              <view class="detail-item">
                <text class="detail-item-label">🍚 碳水化合物</text>
                <text class="detail-item-value">{{ detailRecord.carbohydrates || detailRecord.carbs || 0 }}g</text>
              </view>
              <view class="detail-item">
                <text class="detail-item-label">🌾 膳食纤维</text>
                <text class="detail-item-value">{{ detailRecord.fiber || detailRecord.dietaryFiber || '-' }}g</text>
              </view>
              <view class="detail-item">
                <text class="detail-item-label">🧂 钠</text>
                <text class="detail-item-value">{{ detailRecord.sodium || '-' }}mg</text>
              </view>
              <view class="detail-item">
                <text class="detail-item-label">🍬 糖</text>
                <text class="detail-item-value">{{ detailRecord.sugar || '-' }}g</text>
              </view>
            </view>
          </view>

          <!-- Macro breakdown mini rings -->
          <view class="detail-section" v-if="detailMacroTotal > 0">
            <text class="detail-section-title">宏量营养素占比</text>
            <view class="detail-macro-bars">
              <view class="detail-bar-item" v-for="bar in detailMacroBars" :key="bar.label">
                <view class="flex-between" style="margin-bottom: 6rpx;">
                  <text class="detail-bar-label">{{ bar.label }}</text>
                  <text class="detail-bar-percent">{{ bar.percent }}%</text>
                </view>
                <view class="detail-bar-track">
                  <view class="detail-bar-fill" :style="{ width: bar.percent + '%', background: bar.color }" />
                </view>
              </view>
            </view>
          </view>

          <!-- Notes -->
          <view class="detail-section" v-if="detailRecord.notes || detailRecord.remark">
            <text class="detail-section-title">备注</text>
            <text class="detail-notes">{{ detailRecord.notes || detailRecord.remark }}</text>
          </view>
        </view>

        <view class="dialog-footer detail-footer flex" style="gap: 16rpx;">
          <button class="btn-outline flex-1" @tap="editFromDetail">✏️ 编辑</button>
          <button class="btn-primary flex-1" @tap="showDetail = false">关闭</button>
        </view>
      </view>
    </view>

    <!-- Add / Edit Food Dialog -->
    <view class="dialog-mask" v-if="showAddDialog" @tap="showAddDialog = false">
      <view class="dialog-content" @tap.stop>
        <view class="dialog-header flex-between">
          <text class="dialog-title">{{ isEditing ? '编辑食物记录' : '添加食物记录' }}</text>
          <text class="dialog-close" @tap="showAddDialog = false">✕</text>
        </view>

        <scroll-view scroll-y class="dialog-scroll">
          <view class="dialog-body">
            <view class="input-group">
              <text class="label">餐次</text>
              <view class="meal-type-selector flex">
                <view
                  class="meal-type-option flex-center"
                  v-for="m in mealTypes"
                  :key="m.value"
                  :class="{ selected: addForm.mealType === m.value }"
                  @tap="addForm.mealType = m.value"
                >{{ m.icon }} {{ m.label }}</view>
              </view>
            </view>

            <view class="input-group">
              <text class="label">食物名称</text>
              <input v-model="addForm.foodName" placeholder="请输入食物名称" />
            </view>

            <view class="input-group">
              <text class="label">数量（克）</text>
              <input type="digit" v-model="addForm.amount" placeholder="请输入数量" />
            </view>

            <view class="input-group">
              <text class="label">热量（千卡）</text>
              <input type="digit" v-model="addForm.calories" placeholder="请输入热量" />
            </view>

            <view class="flex" style="gap: 16rpx; margin-top: 8rpx;">
              <view class="nutrition-input flex-1">
                <text class="mini-label">蛋白质(g)</text>
                <input type="digit" v-model="addForm.protein" placeholder="0" />
              </view>
              <view class="nutrition-input flex-1">
                <text class="mini-label">脂肪(g)</text>
                <input type="digit" v-model="addForm.fat" placeholder="0" />
              </view>
              <view class="nutrition-input flex-1">
                <text class="mini-label">碳水(g)</text>
                <input type="digit" v-model="addForm.carbs" placeholder="0" />
              </view>
            </view>
          </view>
        </scroll-view>

        <view class="dialog-footer">
          <button class="btn-primary save-btn" :disabled="saving" @tap="saveRecord">
            {{ saving ? '保存中...' : (isEditing ? '更新记录' : '保存记录') }}
          </button>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed, reactive, watch, nextTick } from 'vue'
import { onLoad, onPullDownRefresh } from '@dcloudio/uni-app'
import { foodApi, MealTypes } from '@/services/api'
import { checkLogin, formatDate } from '@/utils/common'

interface FoodRecord {
  id: number
  foodName: string
  mealType: string
  amount: number
  portion?: number
  calories: number
  protein?: number
  fat?: number
  carbs?: number
  carbohydrates?: number
  fiber?: number
  dietaryFiber?: number
  sodium?: number
  sugar?: number
  notes?: string
  remark?: string
  recordTime?: string
  createdAt?: string
  _offsetX?: number
  _startX?: number
}

interface DailyStats {
  totalCalories: number
  totalProtein: number
  totalFat: number
  totalCarbs: number
}

const mealTypes = MealTypes
const todayStr = formatDate(new Date())

const currentDate = ref(formatDate(new Date()))
const records = ref<FoodRecord[]>([])
const stats = ref<DailyStats>({ totalCalories: 0, totalProtein: 0, totalFat: 0, totalCarbs: 0 })
const loading = ref(false)
const showAddDialog = ref(false)
const showDetail = ref(false)
const detailRecord = ref<FoodRecord | null>(null)
const saving = ref(false)
const isEditing = ref(false)
const editingId = ref<number | null>(null)
const activeMealFilter = ref('ALL')

const dailyGoal = reactive({
  calories: 2000,
  protein: 60,
  fat: 65,
  carbs: 300
})

const addForm = ref({
  mealType: 'BREAKFAST',
  foodName: '',
  amount: '',
  calories: '',
  protein: '',
  fat: '',
  carbs: ''
})

const displayDate = computed(() => {
  const parts = currentDate.value.split('-')
  return `${parts[1]}月${parts[2]}日`
})

const isToday = computed(() => {
  return currentDate.value === formatDate(new Date())
})

const filterLabel = computed(() => {
  const found = mealTypes.find(m => m.value === activeMealFilter.value)
  return found ? found.label : ''
})

const filteredMealTypes = computed(() => {
  if (activeMealFilter.value === 'ALL') return mealTypes
  return mealTypes.filter(m => m.value === activeMealFilter.value)
})

// Macro ring data for the summary card
const macroRings = computed(() => {
  const items = [
    { key: 'protein', label: '蛋白质', icon: '🥩', current: stats.value.totalProtein || 0, goal: dailyGoal.protein, unit: 'g', color: '#3B82F6' },
    { key: 'fat', label: '脂肪', icon: '🧈', current: stats.value.totalFat || 0, goal: dailyGoal.fat, unit: 'g', color: '#F59E0B' },
    { key: 'carbs', label: '碳水', icon: '🍚', current: stats.value.totalCarbs || 0, goal: dailyGoal.carbs, unit: 'g', color: '#10B981' },
    { key: 'calories', label: '热量', icon: '🔥', current: stats.value.totalCalories || 0, goal: dailyGoal.calories, unit: 'kcal', color: '#EF4444' }
  ]
  return items.map(i => ({ ...i, percent: calcPercent(i.current, i.goal) }))
})

// Detail macro breakdown
const detailMacroTotal = computed(() => {
  if (!detailRecord.value) return 0
  const p = detailRecord.value.protein || 0
  const f = detailRecord.value.fat || 0
  const c = detailRecord.value.carbohydrates || detailRecord.value.carbs || 0
  return p + f + c
})

const detailMacroBars = computed(() => {
  if (!detailRecord.value || detailMacroTotal.value === 0) return []
  const total = detailMacroTotal.value
  const p = detailRecord.value.protein || 0
  const f = detailRecord.value.fat || 0
  const c = detailRecord.value.carbohydrates || detailRecord.value.carbs || 0
  return [
    { label: '蛋白质', percent: Math.round((p / total) * 100), color: '#3B82F6' },
    { label: '脂肪', percent: Math.round((f / total) * 100), color: '#F59E0B' },
    { label: '碳水', percent: Math.round((c / total) * 100), color: '#10B981' }
  ]
})

onLoad((options) => {
  if (!checkLogin()) return
  if (options?.prefill) {
    try {
      const data = JSON.parse(decodeURIComponent(options.prefill))
      addForm.value.foodName = data.foodName || ''
      addForm.value.calories = String(data.calories || '')
      addForm.value.protein = String(data.protein || '')
      addForm.value.fat = String(data.fat || '')
      addForm.value.carbs = String(data.carbs || '')
      showAddDialog.value = true
    } catch {}
  }
  loadData()
})

onPullDownRefresh(async () => {
  await loadData()
  uni.stopPullDownRefresh()
})

async function loadData() {
  loading.value = true
  try {
    const [recordsRes, statsRes] = await Promise.all([
      foodApi.getRecords({ date: currentDate.value }),
      foodApi.getStats(currentDate.value)
    ])
    if (recordsRes.code === 200) {
      const list = recordsRes.data?.content || recordsRes.data?.records || recordsRes.data?.list || recordsRes.data || []
      records.value = list.map((r: any) => ({ ...r, _offsetX: 0, _startX: 0 }))
    }
    if (statsRes.code === 200 && statsRes.data) {
      stats.value = {
        totalCalories: statsRes.data.totalCalories || 0,
        totalProtein: statsRes.data.totalProtein || 0,
        totalFat: statsRes.data.totalFat || 0,
        totalCarbs: statsRes.data.totalCarbohydrates || statsRes.data.totalCarbs || 0
      }
      if (statsRes.data.dailyGoal) {
        Object.assign(dailyGoal, statsRes.data.dailyGoal)
      }
    }
    // Draw rings after data loads
    nextTick(() => drawAllRings())
  } catch (e: any) {
    uni.showToast({ title: '加载失败', icon: 'none' })
  } finally {
    loading.value = false
  }
}

function changeDate(offset: number) {
  const d = new Date(currentDate.value)
  d.setDate(d.getDate() + offset)
  if (d > new Date()) return
  currentDate.value = formatDate(d)
  loadData()
}

function onDatePick(e: any) {
  currentDate.value = e.detail.value
  loadData()
}

function setMealFilter(type: string) {
  activeMealFilter.value = type
}

function getMealRecords(mealType: string): FoodRecord[] {
  return records.value.filter(r => r.mealType === mealType)
}

function getMealCalories(mealType: string): number {
  return getMealRecords(mealType).reduce((sum, r) => sum + (r.calories || 0), 0)
}

function getMealLabel(mealType: string): string {
  const found = mealTypes.find(m => m.value === mealType)
  return found ? `${found.icon} ${found.label}` : mealType
}

function calcPercent(current: number, goal: number): number {
  if (!goal) return 0
  return Math.min(Math.round((current / goal) * 100), 100)
}

function formatRecordTime(timeStr?: string): string {
  if (!timeStr) return '-'
  try {
    const d = new Date(timeStr)
    const h = String(d.getHours()).padStart(2, '0')
    const m = String(d.getMinutes()).padStart(2, '0')
    return `${d.getMonth() + 1}月${d.getDate()}日 ${h}:${m}`
  } catch {
    return timeStr
  }
}

// Detail view
function openDetail(record: FoodRecord) {
  if (record._offsetX && record._offsetX < -20) return
  detailRecord.value = record
  showDetail.value = true
}

function editFromDetail() {
  if (!detailRecord.value) return
  const r = detailRecord.value
  isEditing.value = true
  editingId.value = r.id
  addForm.value = {
    mealType: r.mealType,
    foodName: r.foodName,
    amount: String(r.amount || r.portion || ''),
    calories: String(r.calories || ''),
    protein: String(r.protein || ''),
    fat: String(r.fat || ''),
    carbs: String(r.carbohydrates || r.carbs || '')
  }
  showDetail.value = false
  showAddDialog.value = true
}

function openAddDialog(mealType: string) {
  isEditing.value = false
  editingId.value = null
  addForm.value = {
    mealType,
    foodName: '',
    amount: '',
    calories: '',
    protein: '',
    fat: '',
    carbs: ''
  }
  showAddDialog.value = true
}

async function saveRecord() {
  if (!addForm.value.foodName.trim()) {
    uni.showToast({ title: '请输入食物名称', icon: 'none' }); return
  }
  if (!addForm.value.amount || Number(addForm.value.amount) <= 0) {
    uni.showToast({ title: '请输入正确的数量', icon: 'none' }); return
  }
  if (!addForm.value.calories || Number(addForm.value.calories) < 0) {
    uni.showToast({ title: '请输入热量', icon: 'none' }); return
  }

  saving.value = true
  try {
    const payload: any = {
      mealType: addForm.value.mealType,
      foodName: addForm.value.foodName.trim(),
      portion: Number(addForm.value.amount),
      calories: Number(addForm.value.calories),
      protein: Number(addForm.value.protein) || 0,
      fat: Number(addForm.value.fat) || 0,
      carbohydrates: Number(addForm.value.carbs) || 0,
      recordTime: currentDate.value + 'T' + new Date().toTimeString().slice(0, 8)
    }

    let res: any
    if (isEditing.value && editingId.value) {
      // Edit: delete old then recreate since API has no PUT
      await foodApi.deleteRecord(editingId.value)
      res = await foodApi.createRecord(payload)
    } else {
      res = await foodApi.createRecord(payload)
    }

    if (res.code === 200) {
      uni.showToast({ title: isEditing.value ? '更新成功' : '记录成功', icon: 'success' })
      showAddDialog.value = false
      isEditing.value = false
      editingId.value = null
      loadData()
    } else {
      uni.showToast({ title: res.message || '保存失败', icon: 'none' })
    }
  } catch (e: any) {
    uni.showToast({ title: e.message || '保存失败', icon: 'none' })
  } finally {
    saving.value = false
  }
}

// Swipe to delete
function onTouchStart(e: any, record: FoodRecord) {
  record._startX = e.touches[0].clientX
  record._offsetX = 0
}

function onTouchMove(e: any, record: FoodRecord) {
  const dx = e.touches[0].clientX - (record._startX || 0)
  if (dx < 0) {
    record._offsetX = Math.max(dx, -80)
  }
}

function onTouchEnd(record: FoodRecord) {
  if ((record._offsetX || 0) < -40) {
    record._offsetX = -80
  } else {
    record._offsetX = 0
  }
}

async function deleteRecord(record: FoodRecord) {
  uni.showModal({
    title: '确认删除',
    content: `确定要删除"${record.foodName}"吗？`,
    success: async (res) => {
      if (res.confirm) {
        try {
          const result = await foodApi.deleteRecord(record.id)
          if (result.code === 200) {
            uni.showToast({ title: '已删除', icon: 'success' })
            loadData()
          } else {
            uni.showToast({ title: '删除失败', icon: 'none' })
          }
        } catch {
          uni.showToast({ title: '删除失败', icon: 'none' })
        }
      }
    }
  })
}

// Draw macro ring charts using canvas
function drawAllRings() {
  macroRings.value.forEach(macro => {
    drawRing(`ring-${macro.key}`, macro.percent, macro.color)
  })
}

function drawRing(canvasId: string, percent: number, color: string) {
  const ctx = uni.createCanvasContext(canvasId)
  if (!ctx) return

  const size = 50
  const center = size
  const radius = size - 8
  const lineWidth = 6
  const startAngle = -Math.PI / 2
  const endAngle = startAngle + (Math.PI * 2 * Math.min(percent, 100)) / 100

  // Background track
  ctx.beginPath()
  ctx.arc(center, center, radius, 0, Math.PI * 2)
  ctx.setStrokeStyle('rgba(255, 255, 255, 0.2)')
  ctx.setLineWidth(lineWidth)
  ctx.setLineCap('round')
  ctx.stroke()

  // Progress arc
  if (percent > 0) {
    ctx.beginPath()
    ctx.arc(center, center, radius, startAngle, endAngle)
    ctx.setStrokeStyle('#FFFFFF')
    ctx.setLineWidth(lineWidth)
    ctx.setLineCap('round')
    ctx.stroke()
  }

  ctx.draw()
}

// Redraw rings when stats change
watch(() => [stats.value.totalCalories, stats.value.totalProtein, stats.value.totalFat, stats.value.totalCarbs], () => {
  nextTick(() => drawAllRings())
})
</script>

<style lang="scss" scoped>
.container {
  padding: 20rpx 30rpx;
  padding-bottom: 60rpx;
  min-height: 100vh;
  background: $background;
}

/* Date Selector */
.date-selector {
  padding: 16rpx 20rpx;
}
.date-arrow {
  width: 64rpx;
  height: 64rpx;
  border-radius: $radius-full;
  background: $muted;
  border: 1rpx solid $border;
  transition: all 0.2s;
}
.date-arrow:active {
  background: rgba(16, 185, 129, 0.08);
  border-color: $accent;
}
.arrow-icon {
  font-size: 40rpx;
  color: $foreground;
  font-weight: bold;
}
.date-display {
  gap: 12rpx;
}
.date-text {
  font-size: 32rpx;
  font-weight: 700;
  color: $foreground;
  font-family: 'Calistoga', Georgia, 'PingFang SC', serif;
}
.today-badge {
  font-size: 20rpx;
  color: $accent;
  background: rgba(16, 185, 129, 0.08);
  padding: 4rpx 12rpx;
  border-radius: $radius-full;
  font-family: 'JetBrains Mono', monospace;
}
.date-picker-hint {
  font-size: 28rpx;
  opacity: 0.5;
}

/* Summary Card */
.summary-card {
  padding: 24rpx;
  background: $gradient-accent;
  border: none;
  box-shadow: $shadow-accent;
  border-radius: $radius-2xl;
  color: #fff;
}
.summary-header {
  margin-bottom: 20rpx;
}
.summary-title {
  font-size: 30rpx;
  font-weight: 700;
  color: #fff;
  font-family: 'Calistoga', Georgia, 'PingFang SC', serif;
}
.summary-cal {
  font-size: 24rpx;
  color: rgba(255, 255, 255, 0.85);
  font-weight: 500;
}

/* Macro ring charts */
.macro-rings {
  margin-bottom: 24rpx;
  padding: 16rpx 0;
}
.ring-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  flex: 1;
}
.ring-wrapper {
  position: relative;
  width: 100rpx;
  height: 100rpx;
  margin-bottom: 8rpx;
}
.ring-canvas {
  width: 100rpx;
  height: 100rpx;
}
.ring-percent {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  font-size: 20rpx;
  font-weight: 700;
  color: #fff;
  font-family: 'JetBrains Mono', monospace;
}
.ring-label {
  font-size: 20rpx;
  color: rgba(255, 255, 255, 0.9);
  margin-bottom: 2rpx;
}
.ring-value {
  font-size: 18rpx;
  color: rgba(255, 255, 255, 0.65);
  font-family: 'JetBrains Mono', monospace;
}

/* Progress bars */
.progress-item {
  margin-bottom: 16rpx;
}
.progress-item:last-child {
  margin-bottom: 0;
}
.p-label {
  font-size: 24rpx;
  color: #fff;
}
.p-value {
  font-size: 22rpx;
  color: rgba(255, 255, 255, 0.75);
}
.progress-bar {
  height: 12rpx;
  background: rgba(255, 255, 255, 0.2);
  border: none;
  border-radius: $radius-full;
  margin-top: 8rpx;
  overflow: hidden;
}
.progress-fill {
  height: 100%;
  border-radius: $radius-full;
  transition: width 0.3s ease;
  min-width: 4rpx;
}
.calories-fill { background: #fff; }
.protein-fill { background: rgba(255, 255, 255, 0.85); }
.fat-fill { background: rgba(255, 255, 255, 0.7); }
.carbs-fill { background: rgba(255, 255, 255, 0.55); }

/* Filter Tabs */
.filter-tabs-scroll {
  white-space: nowrap;
  margin-bottom: 4rpx;
}
.filter-tabs {
  gap: 12rpx;
  padding: 12rpx 0;
}
.filter-tab {
  display: inline-flex;
  align-items: center;
  gap: 8rpx;
  padding: 12rpx 24rpx;
  border-radius: $radius-full;
  background: $card;
  border: 1rpx solid $border;
  font-size: 24rpx;
  color: $muted-foreground;
  white-space: nowrap;
  transition: all 0.2s;
  font-family: 'Inter', 'PingFang SC', sans-serif;
}
.filter-tab.active {
  background: $accent;
  color: #fff;
  border-color: $accent;
  box-shadow: $shadow-accent;
}
.tab-count {
  font-size: 20rpx;
  font-family: 'JetBrains Mono', monospace;
  background: rgba(0, 0, 0, 0.08);
  padding: 2rpx 10rpx;
  border-radius: $radius-full;
  min-width: 32rpx;
  text-align: center;
}
.filter-tab.active .tab-count {
  background: rgba(255, 255, 255, 0.25);
  color: #fff;
}

/* Meal Section */
.meal-section {
  padding: 20rpx 24rpx;
}
.meal-header {
  padding-bottom: 16rpx;
  border-bottom: 1rpx solid $border;
}
.meal-icon {
  font-size: 36rpx;
}
.meal-name {
  font-size: 30rpx;
  font-weight: 700;
  color: $foreground;
  font-family: 'Calistoga', Georgia, 'PingFang SC', serif;
}
.meal-count-badge {
  font-size: 20rpx;
  color: $accent;
  background: rgba(16, 185, 129, 0.08);
  padding: 2rpx 12rpx;
  border-radius: $radius-full;
  font-family: 'JetBrains Mono', monospace;
}
.meal-cal {
  font-size: 24rpx;
}
.add-btn {
  width: 56rpx;
  height: 56rpx;
  border-radius: $radius-full;
  background: rgba(16, 185, 129, 0.08);
  border: none;
}
.add-icon {
  font-size: 36rpx;
  color: $accent;
  font-weight: bold;
}

.meal-empty {
  padding: 24rpx 0;
  text-align: center;
  font-size: 24rpx;
}

/* Empty state */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 60rpx 30rpx;
}
.empty-icon {
  font-size: 72rpx;
  margin-bottom: 16rpx;
}
.empty-text {
  font-size: 28rpx;
  color: $muted-foreground;
  margin-bottom: 24rpx;
}
.empty-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 72rpx;
  padding: 0 40rpx;
  border-radius: $radius-full;
}

/* Food List */
.food-item {
  position: relative;
  overflow: hidden;
}
.food-item-content {
  padding: 20rpx 0;
  border-bottom: 1rpx solid $border;
  background: $card;
  position: relative;
  z-index: 1;
  transition: transform 0.15s ease;
}
.food-info {
  flex: 1;
  min-width: 0;
}
.food-name {
  font-size: 28rpx;
  color: $foreground;
}
.food-amount {
  font-size: 24rpx;
}
.food-meta {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  flex-shrink: 0;
}
.food-cal {
  font-size: 26rpx;
  color: $accent;
  font-weight: 600;
  font-family: 'JetBrains Mono', monospace;
}
.food-macros-hint {
  font-size: 20rpx;
  color: $muted-foreground;
  margin-top: 4rpx;
}

.delete-action {
  position: absolute;
  right: 0;
  top: 0;
  bottom: 0;
  width: 160rpx;
  background: #EF4444;
  color: #fff;
  font-size: 26rpx;
  z-index: 0;
  border-radius: 0 $radius-lg $radius-lg 0;
}

/* Dialog shared styles */
.dialog-mask {
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
.dialog-content {
  width: 100%;
  background: $card;
  border-radius: $radius-2xl $radius-2xl 0 0;
  max-height: 85vh;
  overflow-y: auto;
  padding-bottom: env(safe-area-inset-bottom);
}
.dialog-header {
  padding: 28rpx 30rpx;
  border-bottom: 1rpx solid $border;
  position: sticky;
  top: 0;
  background: $card;
  z-index: 10;
}
.dialog-title {
  font-size: 32rpx;
  font-weight: 700;
  font-family: 'Calistoga', Georgia, 'PingFang SC', serif;
  color: $foreground;
}
.dialog-close {
  font-size: 36rpx;
  color: $muted-foreground;
  padding: 0 10rpx;
}
.dialog-body {
  padding: 20rpx 30rpx;
}
.dialog-scroll {
  max-height: 60vh;
}
.dialog-footer {
  padding: 16rpx 30rpx 20rpx;
  border-top: 1rpx solid $border;
}
.save-btn {
  height: 84rpx;
  line-height: 84rpx;
}

/* Detail Dialog */
.detail-dialog {
  max-height: 90vh;
}
.detail-body {
  padding: 24rpx 30rpx;
}
.detail-title-row {
  margin-bottom: 24rpx;
  align-items: flex-start;
}
.detail-food-name {
  font-size: 36rpx;
  font-weight: 700;
  color: $foreground;
  font-family: 'Calistoga', Georgia, 'PingFang SC', serif;
  display: block;
  margin-bottom: 8rpx;
}
.detail-meal-badge {
  display: inline-block;
  padding: 4rpx 16rpx;
  background: rgba(16, 185, 129, 0.08);
  border: 1rpx solid rgba(16, 185, 129, 0.2);
  border-radius: $radius-full;
  font-size: 22rpx;
  color: $accent;
}
.detail-cal {
  font-size: 36rpx;
  font-weight: 700;
  color: $accent;
  font-family: 'JetBrains Mono', monospace;
  white-space: nowrap;
}
.detail-section {
  margin-bottom: 24rpx;
}
.detail-section-title {
  display: block;
  font-size: 26rpx;
  font-weight: 700;
  color: $foreground;
  margin-bottom: 16rpx;
  padding-bottom: 8rpx;
  border-bottom: 1rpx solid $border;
  font-family: 'Calistoga', Georgia, 'PingFang SC', serif;
}
.detail-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16rpx;
}
.detail-item {
  background: $muted;
  border-radius: $radius-lg;
  padding: 16rpx;
}
.detail-item-label {
  display: block;
  font-size: 22rpx;
  color: $muted-foreground;
  margin-bottom: 6rpx;
}
.detail-item-value {
  display: block;
  font-size: 28rpx;
  font-weight: 600;
  color: $foreground;
  font-family: 'JetBrains Mono', monospace;
}
.detail-item-value.highlight {
  color: $accent;
}
.detail-notes {
  font-size: 26rpx;
  color: $muted-foreground;
  line-height: 1.6;
  padding: 16rpx;
  background: $muted;
  border-radius: $radius-lg;
}

/* Detail macro bars */
.detail-macro-bars {
  padding: 8rpx 0;
}
.detail-bar-item {
  margin-bottom: 16rpx;
}
.detail-bar-item:last-child {
  margin-bottom: 0;
}
.detail-bar-label {
  font-size: 24rpx;
  color: $foreground;
}
.detail-bar-percent {
  font-size: 22rpx;
  color: $muted-foreground;
  font-family: 'JetBrains Mono', monospace;
}
.detail-bar-track {
  height: 14rpx;
  background: $muted;
  border-radius: $radius-full;
  overflow: hidden;
}
.detail-bar-fill {
  height: 100%;
  border-radius: $radius-full;
  transition: width 0.3s ease;
}
.detail-footer {
  padding: 16rpx 30rpx 20rpx;
}

/* Add Dialog form styles */
.meal-type-selector {
  flex-wrap: wrap;
  gap: 12rpx;
  margin-top: 8rpx;
}
.meal-type-option {
  padding: 12rpx 20rpx;
  border: 1rpx solid $border;
  border-radius: $radius-full;
  font-size: 26rpx;
  color: $muted-foreground;
  transition: all 0.2s;
  font-family: 'Inter', 'PingFang SC', sans-serif;
}
.meal-type-option.selected {
  border-color: $accent;
  color: $accent;
  background: rgba(16, 185, 129, 0.06);
  box-shadow: 0 0 0 3px rgba(16, 185, 129, 0.1);
}

.nutrition-input {
  background: $muted;
  border: 1rpx solid $border;
  border-radius: $radius-lg;
  padding: 12rpx 16rpx;
}
.mini-label {
  display: block;
  font-size: 20rpx;
  color: $muted-foreground;
  margin-bottom: 4rpx;
}
.nutrition-input input {
  height: 40rpx;
  font-size: 26rpx;
  font-family: 'Inter', 'PingFang SC', sans-serif;
}

.btn-outline {
  height: 80rpx;
  line-height: 80rpx;
  border: 1rpx solid $border;
  border-radius: $radius-lg;
  background: $card;
  color: $foreground;
  font-size: 28rpx;
  text-align: center;
}
.btn-outline:active {
  background: $muted;
}
</style>
