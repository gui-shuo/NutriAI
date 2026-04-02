<template>
  <view class="container">
    <!-- Date Selector -->
    <view class="date-selector card flex-between">
      <view class="date-arrow flex-center" @tap="changeDate(-1)">
        <text class="arrow-icon">‹</text>
      </view>
      <view class="date-display flex-center" @tap="openDatePicker">
        <text class="date-text">{{ displayDate }}</text>
        <text v-if="isToday" class="today-badge">今天</text>
      </view>
      <view class="date-arrow flex-center" @tap="changeDate(1)">
        <text class="arrow-icon">›</text>
      </view>
    </view>

    <!-- Daily Summary -->
    <view class="card summary-card">
      <view class="summary-header flex-between">
        <text class="summary-title">📊 今日营养摄入</text>
        <text class="summary-cal">{{ stats.totalCalories || 0 }} / {{ dailyGoal.calories }} kcal</text>
      </view>
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

    <!-- Meal Sections -->
    <view class="meal-section card" v-for="meal in mealTypes" :key="meal.value">
      <view class="meal-header flex-between">
        <view class="flex" style="align-items: center; gap: 12rpx;">
          <text class="meal-icon">{{ meal.icon }}</text>
          <text class="meal-name">{{ meal.label }}</text>
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
          <view class="food-item-content flex-between" :style="{ transform: `translateX(${record._offsetX || 0}px)` }">
            <view>
              <text class="food-name">{{ record.foodName }}</text>
              <text class="food-amount text-secondary"> {{ record.amount }}g</text>
            </view>
            <text class="food-cal">{{ record.calories }} kcal</text>
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

    <!-- Add Food Dialog -->
    <view class="dialog-mask" v-if="showAddDialog" @tap="showAddDialog = false">
      <view class="dialog-content" @tap.stop>
        <view class="dialog-header flex-between">
          <text class="dialog-title">添加食物记录</text>
          <text class="dialog-close" @tap="showAddDialog = false">✕</text>
        </view>

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

        <view class="dialog-footer">
          <button class="btn-primary save-btn" :disabled="saving" @tap="saveRecord">
            {{ saving ? '保存中...' : '保存记录' }}
          </button>
        </view>
      </view>
    </view>

    <!-- Date Picker -->
    <picker
      mode="date"
      :value="currentDate"
      @change="onDatePick"
      ref="datePickerRef"
      style="display: none;"
    />
  </view>
</template>

<script setup lang="ts">
import { ref, computed, reactive } from 'vue'
import { onLoad, onPullDownRefresh } from '@dcloudio/uni-app'
import { foodApi, MealTypes } from '@/services/api'
import { checkLogin, formatDate } from '@/utils/common'

interface FoodRecord {
  id: number
  foodName: string
  mealType: string
  amount: number
  calories: number
  protein?: number
  fat?: number
  carbs?: number
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

const currentDate = ref(formatDate(new Date()))
const records = ref<FoodRecord[]>([])
const stats = ref<DailyStats>({ totalCalories: 0, totalProtein: 0, totalFat: 0, totalCarbs: 0 })
const loading = ref(false)
const showAddDialog = ref(false)
const saving = ref(false)

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

onLoad((options) => {
  if (!checkLogin()) return
  // Handle prefill data from food recognition
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
      // Update daily goals if server provides them
      if (statsRes.data.dailyGoal) {
        Object.assign(dailyGoal, statsRes.data.dailyGoal)
      }
    }
  } catch (e: any) {
    uni.showToast({ title: '加载失败', icon: 'none' })
  } finally {
    loading.value = false
  }
}

function changeDate(offset: number) {
  const d = new Date(currentDate.value)
  d.setDate(d.getDate() + offset)
  // Don't allow future dates
  if (d > new Date()) return
  currentDate.value = formatDate(d)
  loadData()
}

function openDatePicker() {
  // Trigger the hidden picker
  uni.showToast({ title: '请使用左右箭头切换日期', icon: 'none', duration: 1500 })
}

function onDatePick(e: any) {
  currentDate.value = e.detail.value
  loadData()
}

function getMealRecords(mealType: string): FoodRecord[] {
  return records.value.filter(r => r.mealType === mealType)
}

function getMealCalories(mealType: string): number {
  return getMealRecords(mealType).reduce((sum, r) => sum + (r.calories || 0), 0)
}

function calcPercent(current: number, goal: number): number {
  if (!goal) return 0
  return Math.min(Math.round((current / goal) * 100), 100)
}

function openAddDialog(mealType: string) {
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
    const res = await foodApi.createRecord({
      mealType: addForm.value.mealType,
      foodName: addForm.value.foodName.trim(),
      portion: Number(addForm.value.amount),
      calories: Number(addForm.value.calories),
      protein: Number(addForm.value.protein) || 0,
      fat: Number(addForm.value.fat) || 0,
      carbohydrates: Number(addForm.value.carbs) || 0,
      recordTime: currentDate.value + 'T' + new Date().toTimeString().slice(0, 8)
    })
    if (res.code === 200) {
      uni.showToast({ title: '记录成功', icon: 'success' })
      showAddDialog.value = false
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
</script>

<style scoped>
.container {
  padding: 20rpx 30rpx;
  padding-bottom: 60rpx;
  min-height: 100vh;
  background: #fdfbf7;
}

/* Date Selector */
.date-selector {
  padding: 16rpx 20rpx;
}
.date-arrow {
  width: 64rpx;
  height: 64rpx;
  border-radius: 255px 15px 225px 15px / 15px 225px 15px 255px;
  background: #fdfbf7;
  border: 2rpx solid #2d2d2d;
  box-shadow: 2px 2px 0px 0px rgba(45, 45, 45, 0.1);
}
.date-arrow:active {
  box-shadow: none;
  transform: translate(2rpx, 2rpx);
}
.arrow-icon {
  font-size: 40rpx;
  color: #2d2d2d;
  font-weight: bold;
}
.date-display {
  gap: 12rpx;
}
.date-text {
  font-size: 32rpx;
  font-weight: 700;
  color: #2d2d2d;
  font-family: 'Kalam', 'ZCOOL KuaiLe', 'PingFang SC', cursive;
}
.today-badge {
  font-size: 20rpx;
  color: #ff4d4d;
  background: rgba(255, 77, 77, 0.1);
  padding: 4rpx 12rpx;
  border-radius: 185px 10px 155px 10px / 10px 155px 10px 185px;
  border: 1rpx solid #ff4d4d;
}

/* Summary Card — sticky note */
.summary-card {
  padding: 24rpx;
  background: #fff9c4;
  border: 2rpx solid #2d2d2d;
  box-shadow: 4px 4px 0px 0px #2d2d2d;
}
.summary-header {
  margin-bottom: 20rpx;
}
.summary-title {
  font-size: 30rpx;
  font-weight: 700;
  font-family: 'Kalam', 'ZCOOL KuaiLe', 'PingFang SC', cursive;
}
.summary-cal {
  font-size: 24rpx;
  color: #ff4d4d;
  font-weight: 500;
}

.progress-item {
  margin-bottom: 16rpx;
}
.progress-item:last-child {
  margin-bottom: 0;
}
.p-label {
  font-size: 24rpx;
  color: #2d2d2d;
}
.p-value {
  font-size: 22rpx;
  color: #5a5a5a;
}
.progress-bar {
  height: 12rpx;
  background: #e5e0d8;
  border: 1rpx solid #2d2d2d;
  border-radius: 185px 10px 155px 10px / 10px 155px 10px 185px;
  margin-top: 8rpx;
  overflow: hidden;
}
.progress-fill {
  height: 100%;
  border-radius: 185px 10px 155px 10px / 10px 155px 10px 185px;
  transition: width 0.3s ease;
  min-width: 4rpx;
}
.calories-fill {
  background: #ff4d4d;
}
.protein-fill {
  background: #2d5da1;
}
.fat-fill {
  background: #ff9800;
}
.carbs-fill {
  background: #4caf50;
}

/* Meal Section */
.meal-section {
  padding: 20rpx 24rpx;
}
.meal-header {
  padding-bottom: 16rpx;
  border-bottom: 2rpx dashed #e5e0d8;
}
.meal-icon {
  font-size: 36rpx;
}
.meal-name {
  font-size: 30rpx;
  font-weight: 700;
  color: #2d2d2d;
  font-family: 'Kalam', 'ZCOOL KuaiLe', 'PingFang SC', cursive;
}
.meal-cal {
  font-size: 24rpx;
}
.add-btn {
  width: 56rpx;
  height: 56rpx;
  border-radius: 255px 15px 225px 15px / 15px 225px 15px 255px;
  background: rgba(255, 77, 77, 0.1);
  border: 2rpx solid #ff4d4d;
}
.add-icon {
  font-size: 36rpx;
  color: #ff4d4d;
  font-weight: bold;
}

.meal-empty {
  padding: 24rpx 0;
  text-align: center;
  font-size: 24rpx;
}

/* Food List */
.food-item {
  position: relative;
  overflow: hidden;
}
.food-item-content {
  padding: 18rpx 0;
  border-bottom: 2rpx dashed #e5e0d8;
  background: #fff;
  position: relative;
  z-index: 1;
  transition: transform 0.15s ease;
}
.food-name {
  font-size: 28rpx;
  color: #2d2d2d;
}
.food-amount {
  font-size: 24rpx;
}
.food-cal {
  font-size: 26rpx;
  color: #ff4d4d;
  font-weight: 500;
}

.delete-action {
  position: absolute;
  right: 0;
  top: 0;
  bottom: 0;
  width: 160rpx;
  background: #ff4d4d;
  color: #fff;
  font-size: 26rpx;
  z-index: 0;
  border: 2rpx solid #2d2d2d;
}

/* Add Dialog */
.dialog-mask {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(45, 45, 45, 0.4);
  z-index: 1000;
  display: flex;
  align-items: flex-end;
}
.dialog-content {
  width: 100%;
  background: #fdfbf7;
  border-radius: 255px 15px 0 0 / 15px 225px 0 0;
  border-top: 3rpx solid #2d2d2d;
  max-height: 85vh;
  overflow-y: auto;
  padding-bottom: env(safe-area-inset-bottom);
}
.dialog-header {
  padding: 28rpx 30rpx;
  border-bottom: 2rpx dashed #e5e0d8;
}
.dialog-title {
  font-size: 32rpx;
  font-weight: 700;
  font-family: 'Kalam', 'ZCOOL KuaiLe', 'PingFang SC', cursive;
}
.dialog-close {
  font-size: 36rpx;
  color: #5a5a5a;
  padding: 0 10rpx;
}
.dialog-body {
  padding: 20rpx 30rpx;
}
.dialog-footer {
  padding: 16rpx 30rpx 20rpx;
}
.save-btn {
  height: 84rpx;
  line-height: 84rpx;
}

.meal-type-selector {
  flex-wrap: wrap;
  gap: 12rpx;
  margin-top: 8rpx;
}
.meal-type-option {
  padding: 12rpx 20rpx;
  border: 2rpx solid #2d2d2d;
  border-radius: 185px 10px 155px 10px / 10px 155px 10px 185px;
  font-size: 26rpx;
  color: #5a5a5a;
  transition: all 0.2s;
  font-family: 'Patrick Hand', 'PingFang SC', cursive;
}
.meal-type-option.selected {
  border-color: #ff4d4d;
  color: #ff4d4d;
  background: rgba(255, 77, 77, 0.06);
  box-shadow: 2px 2px 0px 0px rgba(45, 45, 45, 0.1);
}

.nutrition-input {
  background: #fdfbf7;
  border: 2rpx solid #e5e0d8;
  border-radius: 185px 10px 155px 10px / 10px 155px 10px 185px;
  padding: 12rpx 16rpx;
}
.mini-label {
  display: block;
  font-size: 20rpx;
  color: #5a5a5a;
  margin-bottom: 4rpx;
}
.nutrition-input input {
  height: 40rpx;
  font-size: 26rpx;
  font-family: 'Patrick Hand', 'PingFang SC', cursive;
}
</style>
