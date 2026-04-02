<template>
  <view class="container">
    <view class="page-header">
      <text class="page-title">🔍 食物识别</text>
      <text class="page-desc text-secondary">拍照或输入食物名称，获取营养信息</text>
    </view>

    <view class="disclaimer-tip" v-if="showDisclaimer">
      <text>📋 上传食物图片即可识别营养成分，支持识别各类常见食物。营养数据由AI生成，仅供参考。</text>
      <text class="dismiss" @tap="showDisclaimer = false">✕</text>
    </view>

    <!-- Mode Tabs -->
    <view class="mode-tabs flex">
      <view
        class="mode-tab flex-1 flex-center"
        :class="{ active: mode === 'photo' }"
        @tap="switchMode('photo')"
      >📷 拍照识别</view>
      <view
        class="mode-tab flex-1 flex-center"
        :class="{ active: mode === 'text' }"
        @tap="switchMode('text')"
      >✏️ 文字搜索</view>
    </view>

    <!-- Photo Mode -->
    <view v-show="mode === 'photo'" class="photo-section">
      <view class="card photo-area" @tap="!photoPath && chooseFromAlbum()">
        <image v-if="photoPath" :src="photoPath" class="preview-image" mode="aspectFit" />
        <view v-else class="photo-placeholder flex-center">
          <text class="photo-icon">📷</text>
          <text class="photo-hint">点击上传或拍照识别食物</text>
          <text class="photo-sub-hint">支持 JPG / PNG / WebP，大图自动压缩</text>
        </view>
        <view v-if="photoPath" class="remove-image-btn" @tap.stop="clearImage">✕</view>
      </view>

      <view class="photo-actions flex">
        <button class="btn-primary flex-1" @tap="takePhoto">📷 拍照</button>
        <button class="btn-outline flex-1" @tap="chooseFromAlbum">🖼️ 相册</button>
      </view>

      <button
        v-if="photoPath && !recognizing"
        class="btn-primary recognize-btn"
        @tap="recognizeCurrentPhoto"
      >🔍 开始识别</button>

      <view class="photo-tip">
        💡 识别结果请以置信度为准，置信度越高结果越可靠。大图将自动压缩以加速识别。
      </view>
    </view>

    <!-- Text Mode -->
    <view v-show="mode === 'text'" class="text-section">
      <view class="card">
        <view class="input-group" style="margin-bottom: 0;">
          <text class="label">食物名称</text>
          <view class="flex" style="gap: 16rpx;">
            <input
              class="flex-1"
              v-model="foodName"
              placeholder="请输入食物名称，如：鸡胸肉、燕麦"
              confirm-type="search"
              @confirm="searchByName"
            />
          </view>
        </view>
      </view>
      <button
        class="btn-primary search-btn"
        :disabled="!foodName.trim() || recognizing"
        @tap="searchByName"
      >{{ recognizing ? '识别中...' : '🔍 识别食物' }}</button>
    </view>

    <!-- Quick Food Chips -->
    <view class="card quick-section">
      <text class="quick-title">⚡ 快捷识别</text>
      <view class="quick-chips">
        <view
          v-for="food in quickFoods"
          :key="food"
          class="quick-chip"
          :class="{ 'quick-chip-disabled': recognizing }"
          @tap="quickRecognize(food)"
        >{{ food }}</view>
      </view>
    </view>

    <!-- Loading State -->
    <view v-if="recognizing" class="loading-section card flex-center">
      <view class="loading-content">
        <view class="loading-spinner" />
        <text class="loading-title">AI正在识别食物...</text>
        <text class="loading-text">正在分析食物营养成分，通常需要几秒钟</text>
      </view>
    </view>

    <!-- Empty State -->
    <view
      v-if="!recognizing && results.length === 0 && !errorMsg"
      class="empty-section card flex-center"
    >
      <view class="empty-content">
        <text class="empty-icon">🍽️</text>
        <text class="empty-title">开始识别食物</text>
        <text class="empty-desc text-secondary">在上方输入食物名称或上传图片，AI将分析完整营养成分</text>
      </view>
    </view>

    <!-- Recognition Results -->
    <view v-if="results.length > 0 && !recognizing" class="result-section">
      <view class="result-bar flex-between">
        <text class="result-bar-title">🔬 识别结果</text>
        <view class="result-bar-count">共 {{ results.length }} 种食物</view>
      </view>

      <view class="result-grid" :class="{ 'result-grid-multi': results.length > 1 }">
        <view
          v-for="(item, index) in results"
          :key="index"
          class="card result-card"
        >
          <!-- Header -->
          <view class="result-header">
            <view class="result-title-row">
              <view class="result-name-wrap">
                <text class="result-name">{{ item.name }}</text>
                <view
                  v-if="item.category"
                  class="category-badge"
                  :class="item.category === '果蔬' ? 'category-fruit' : 'category-dish'"
                >{{ item.category }}</view>
              </view>
              <view class="confidence-badge" :class="confidenceClass(item.confidence)">
                置信度 {{ formatConfidence(item.confidence) }}
              </view>
            </view>
            <text class="result-portion text-secondary">每100g</text>
          </view>

          <view class="divider" />

          <!-- Core Nutrition -->
          <view class="nutrition-grid">
            <view class="nutrition-card calories-card">
              <text class="n-icon">🔥</text>
              <text class="n-value">{{ item.calories || 0 }}</text>
              <text class="n-unit">千卡</text>
              <text class="n-label">热量</text>
            </view>
            <view class="nutrition-card">
              <text class="n-icon">🥩</text>
              <text class="n-value">{{ nf(item.protein) }}g</text>
              <text class="n-label">蛋白质</text>
            </view>
            <view class="nutrition-card">
              <text class="n-icon">🧈</text>
              <text class="n-value">{{ nf(item.fat) }}g</text>
              <text class="n-label">脂肪</text>
            </view>
            <view class="nutrition-card">
              <text class="n-icon">🍚</text>
              <text class="n-value">{{ nf(item.carbs) }}g</text>
              <text class="n-label">碳水</text>
            </view>
            <view class="nutrition-card" v-if="item.fiber">
              <text class="n-icon">🌾</text>
              <text class="n-value">{{ nf(item.fiber) }}g</text>
              <text class="n-label">膳食纤维</text>
            </view>
            <view class="nutrition-card" v-if="item.cholesterol">
              <text class="n-icon">��</text>
              <text class="n-value">{{ nf(item.cholesterol) }}mg</text>
              <text class="n-label">胆固醇</text>
            </view>
          </view>

          <!-- Minerals -->
          <view v-if="hasMinerals(item)" class="nutrition-sub-section">
            <view class="sub-section-header" @tap="toggleSection(index, 'mineral')">
              <text class="sub-section-title">💎 矿物质</text>
              <text class="sub-section-arrow">{{ isSectionOpen(index, 'mineral') ? '▲' : '▼' }}</text>
            </view>
            <view v-show="isSectionOpen(index, 'mineral')" class="nutrition-grid-sm">
              <view v-if="item.calcium" class="n-item-sm">
                <text class="n-sm-label">钙</text>
                <text class="n-sm-value">{{ nf(item.calcium) }}<text class="n-sm-unit">mg</text></text>
              </view>
              <view v-if="item.iron" class="n-item-sm">
                <text class="n-sm-label">铁</text>
                <text class="n-sm-value">{{ nf(item.iron) }}<text class="n-sm-unit">mg</text></text>
              </view>
              <view v-if="item.zinc" class="n-item-sm">
                <text class="n-sm-label">锌</text>
                <text class="n-sm-value">{{ nf(item.zinc) }}<text class="n-sm-unit">mg</text></text>
              </view>
              <view v-if="item.sodium" class="n-item-sm">
                <text class="n-sm-label">钠</text>
                <text class="n-sm-value">{{ nf(item.sodium) }}<text class="n-sm-unit">mg</text></text>
              </view>
              <view v-if="item.potassium" class="n-item-sm">
                <text class="n-sm-label">钾</text>
                <text class="n-sm-value">{{ nf(item.potassium) }}<text class="n-sm-unit">mg</text></text>
              </view>
              <view v-if="item.magnesium" class="n-item-sm">
                <text class="n-sm-label">镁</text>
                <text class="n-sm-value">{{ nf(item.magnesium) }}<text class="n-sm-unit">mg</text></text>
              </view>
              <view v-if="item.phosphorus" class="n-item-sm">
                <text class="n-sm-label">磷</text>
                <text class="n-sm-value">{{ nf(item.phosphorus) }}<text class="n-sm-unit">mg</text></text>
              </view>
              <view v-if="item.selenium" class="n-item-sm">
                <text class="n-sm-label">硒</text>
                <text class="n-sm-value">{{ nf(item.selenium) }}<text class="n-sm-unit">μg</text></text>
              </view>
              <view v-if="item.copper" class="n-item-sm">
                <text class="n-sm-label">铜</text>
                <text class="n-sm-value">{{ nf(item.copper, 2) }}<text class="n-sm-unit">mg</text></text>
              </view>
              <view v-if="item.manganese" class="n-item-sm">
                <text class="n-sm-label">锰</text>
                <text class="n-sm-value">{{ nf(item.manganese, 2) }}<text class="n-sm-unit">mg</text></text>
              </view>
            </view>
          </view>

          <!-- Vitamins -->
          <view v-if="hasVitamins(item)" class="nutrition-sub-section">
            <view class="sub-section-header" @tap="toggleSection(index, 'vitamin')">
              <text class="sub-section-title">🧪 维生素</text>
              <text class="sub-section-arrow">{{ isSectionOpen(index, 'vitamin') ? '▲' : '▼' }}</text>
            </view>
            <view v-show="isSectionOpen(index, 'vitamin')" class="nutrition-grid-sm">
              <view v-if="item.vitaminA" class="n-item-sm">
                <text class="n-sm-label">维A</text>
                <text class="n-sm-value">{{ nf(item.vitaminA) }}<text class="n-sm-unit">μg</text></text>
              </view>
              <view v-if="item.vitaminC" class="n-item-sm">
                <text class="n-sm-label">维C</text>
                <text class="n-sm-value">{{ nf(item.vitaminC) }}<text class="n-sm-unit">mg</text></text>
              </view>
              <view v-if="item.vitaminE" class="n-item-sm">
                <text class="n-sm-label">维E</text>
                <text class="n-sm-value">{{ nf(item.vitaminE, 2) }}<text class="n-sm-unit">mg</text></text>
              </view>
              <view v-if="item.carotene" class="n-item-sm">
                <text class="n-sm-label">胡萝卜素</text>
                <text class="n-sm-value">{{ nf(item.carotene) }}<text class="n-sm-unit">μg</text></text>
              </view>
              <view v-if="item.thiamine" class="n-item-sm">
                <text class="n-sm-label">B1</text>
                <text class="n-sm-value">{{ nf(item.thiamine, 2) }}<text class="n-sm-unit">mg</text></text>
              </view>
              <view v-if="item.riboflavin" class="n-item-sm">
                <text class="n-sm-label">B2</text>
                <text class="n-sm-value">{{ nf(item.riboflavin, 2) }}<text class="n-sm-unit">mg</text></text>
              </view>
              <view v-if="item.niacin" class="n-item-sm">
                <text class="n-sm-label">B3</text>
                <text class="n-sm-value">{{ nf(item.niacin) }}<text class="n-sm-unit">mg</text></text>
              </view>
              <view v-if="item.retinolEquivalent" class="n-item-sm">
                <text class="n-sm-label">视黄醇当量</text>
                <text class="n-sm-value">{{ nf(item.retinolEquivalent) }}<text class="n-sm-unit">μg</text></text>
              </view>
            </view>
          </view>

          <!-- Data Source -->
          <view v-if="item.source" class="data-source-row">
            <text class="data-source-text">📋 数据来源: {{ sourceText(item.source) }}</text>
          </view>

          <!-- Save to Record -->
          <view class="result-actions flex">
            <button class="btn-primary flex-1 record-btn" @tap.stop="openMealPicker(index)">
              📝 记录到饮食
            </button>
            <button class="btn-outline flex-1 record-btn" @tap.stop="goToRecord(item)">
              📋 详细记录
            </button>
          </view>
        </view>
      </view>
    </view>

    <!-- Error State -->
    <view v-if="errorMsg && !recognizing" class="error-section card">
      <view class="flex-center" style="flex-direction: column; padding: 40rpx 0;">
        <text class="error-icon">😅</text>
        <text class="error-text">{{ errorMsg }}</text>
        <text class="error-hint text-secondary">请尝试重新拍照或换个角度</text>
      </view>
    </view>

    <!-- Recognition History -->
    <view v-if="history.length > 0" class="history-section">
      <view class="history-bar flex-between">
        <text class="history-bar-title">📜 识别历史</text>
        <view class="history-bar-right flex">
          <text class="history-count">{{ history.length }} 条记录</text>
          <text class="history-clear" @tap="clearHistory">清空</text>
        </view>
      </view>

      <view
        v-for="(item, hIdx) in history"
        :key="item.id"
        class="card history-card"
      >
        <view class="history-summary" @tap="toggleHistory(item.id)">
          <view class="history-left flex">
            <view
              class="history-type-badge"
              :class="item.type === 'text' ? 'badge-text' : 'badge-photo'"
            >{{ item.type === 'text' ? '📝' : '📷' }}</view>
            <view class="history-info">
              <text class="history-query">{{ item.query }}</text>
              <text class="history-time text-secondary">{{ formatTime(item.timestamp) }}</text>
            </view>
          </view>
          <view class="history-right flex">
            <text class="history-food-count">{{ item.foodNames.length }}种</text>
            <text class="history-arrow">{{ expandedHistory[item.id] ? '▲' : '▼' }}</text>
          </view>
        </view>

        <view v-show="expandedHistory[item.id]" class="history-detail">
          <view
            v-for="(food, fIdx) in item.foodNames"
            :key="fIdx"
            class="history-food-row"
          >
            <text class="history-food-name">{{ food }}</text>
            <text class="history-food-cal text-secondary">
              {{ item.foodCalories[fIdx] || 0 }}kcal
            </text>
          </view>
        </view>
      </view>
    </view>

    <!-- Meal Type Picker Modal -->
    <view v-if="showMealPicker" class="modal-overlay" @tap="showMealPicker = false">
      <view class="modal-content" @tap.stop>
        <text class="modal-title">选择餐次</text>
        <view class="meal-options">
          <view
            v-for="meal in mealTypes"
            :key="meal.value"
            class="meal-option"
            @tap="saveToRecord(meal.value)"
          >
            <text class="meal-icon">{{ meal.icon }}</text>
            <text class="meal-label">{{ meal.label }}</text>
          </view>
        </view>
        <view class="modal-cancel" @tap="showMealPicker = false">
          <text class="modal-cancel-text">取消</text>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { onLoad, onPullDownRefresh } from '@dcloudio/uni-app'
import { foodApi } from '@/services/api'
import { checkLogin } from '@/utils/common'

// ─── Types ───────────────────────────────────────────────────────
interface RecognitionResult {
  name: string
  confidence: number
  category?: string
  calories?: number
  protein?: number
  fat?: number
  carbs?: number
  fiber?: number
  sodium?: number
  calcium?: number
  potassium?: number
  magnesium?: number
  iron?: number
  zinc?: number
  phosphorus?: number
  selenium?: number
  copper?: number
  manganese?: number
  vitaminA?: number
  vitaminC?: number
  vitaminE?: number
  carotene?: number
  thiamine?: number
  riboflavin?: number
  niacin?: number
  retinolEquivalent?: number
  cholesterol?: number
  source?: string
}

interface HistoryItem {
  id: string
  type: 'photo' | 'text'
  query: string
  foodNames: string[]
  foodCalories: number[]
  timestamp: number
}

// ─── Constants ───────────────────────────────────────────────────
const HISTORY_KEY = 'food_recognition_history'
const MAX_HISTORY = 20

const quickFoods = ['苹果', '香蕉', '鸡胸肉', '鸡蛋', '牛奶', '燕麦', '西兰花', '三文鱼', '米饭', '红薯']

const mealTypes = [
  { value: 'BREAKFAST', label: '早餐', icon: '🌅' },
  { value: 'LUNCH', label: '午餐', icon: '☀️' },
  { value: 'DINNER', label: '晚餐', icon: '🌙' },
  { value: 'SNACK', label: '加餐', icon: '🍪' }
]

// ─── State ───────────────────────────────────────────────────────
const mode = ref<'photo' | 'text'>('photo')
const showDisclaimer = ref(true)
const photoPath = ref('')
const compressedPath = ref('')
const foodName = ref('')
const recognizing = ref(false)
const results = ref<RecognitionResult[]>([])
const errorMsg = ref('')
const history = ref<HistoryItem[]>([])
const expandedHistory = reactive<Record<string, boolean>>({})
const expandedSections = reactive<Record<string, boolean>>({})
const showMealPicker = ref(false)
const savingIndex = ref(-1)

// ─── Lifecycle ───────────────────────────────────────────────────
onLoad((options) => {
  if (!checkLogin()) return
  if (options?.mode === 'text') mode.value = 'text'
  loadHistory()
})

onPullDownRefresh(() => {
  results.value = []
  errorMsg.value = ''
  loadHistory()
  uni.stopPullDownRefresh()
})

// ─── Mode ────────────────────────────────────────────────────────
function switchMode(m: 'photo' | 'text') {
  mode.value = m
}

// ─── Image Handling ──────────────────────────────────────────────
function takePhoto() {
  uni.chooseImage({
    count: 1,
    sourceType: ['camera'],
    sizeType: ['compressed'],
    success: (res) => handleImageChosen(res.tempFilePaths[0]),
    fail: () => {}
  })
}

function chooseFromAlbum() {
  uni.chooseImage({
    count: 1,
    sourceType: ['album'],
    sizeType: ['compressed'],
    success: (res) => handleImageChosen(res.tempFilePaths[0]),
    fail: () => {}
  })
}

function handleImageChosen(filePath: string) {
  photoPath.value = filePath
  compressedPath.value = ''
  results.value = []
  errorMsg.value = ''
  compressImage(filePath)
}

function compressImage(filePath: string) {
  // #ifdef H5
  compressImageCanvas(filePath)
  // #endif
  // #ifndef H5
  uni.compressImage({
    src: filePath,
    quality: 60,
    success: (res) => { compressedPath.value = res.tempFilePath },
    fail: () => { compressedPath.value = filePath }
  })
  // #endif
}

function compressImageCanvas(filePath: string) {
  try {
    const img = new Image()
    img.onload = () => {
      let { width, height } = img
      const maxWidth = 1280
      if (width > maxWidth) {
        height = Math.round(height * maxWidth / width)
        width = maxWidth
      }
      const canvas = document.createElement('canvas')
      canvas.width = width
      canvas.height = height
      const ctx = canvas.getContext('2d')
      if (!ctx) { compressedPath.value = filePath; return }
      ctx.drawImage(img, 0, 0, width, height)
      canvas.toBlob(
        (blob) => {
          if (blob) {
            compressedPath.value = URL.createObjectURL(blob)
          } else {
            compressedPath.value = filePath
          }
        },
        'image/jpeg',
        0.7
      )
    }
    img.onerror = () => { compressedPath.value = filePath }
    img.src = filePath
  } catch {
    compressedPath.value = filePath
  }
}

function clearImage() {
  photoPath.value = ''
  compressedPath.value = ''
  results.value = []
  errorMsg.value = ''
}

// ─── Recognition ─────────────────────────────────────────────────
function recognizeCurrentPhoto() {
  const path = compressedPath.value || photoPath.value
  if (!path) return
  recognizePhoto(path)
}

async function recognizePhoto(filePath: string) {
  recognizing.value = true
  results.value = []
  errorMsg.value = ''
  try {
    const res = await foodApi.photoRecognize(filePath)
    if (res.code === 200 && res.data) {
      results.value = parseFoodResults(res.data)
      if (results.value.length === 0) {
        errorMsg.value = '无法识别该食物，请重试'
      } else {
        addHistory('photo', '图片识别', results.value)
      }
    } else if (res.code === 4001) {
      errorMsg.value = res.message || '未能从图片中识别到食物，请换一张更清晰的照片'
    } else {
      errorMsg.value = res.message || '无法识别该食物，请重试'
    }
  } catch (e: any) {
    errorMsg.value = e.message || '识别失败，请检查网络后重试'
  } finally {
    recognizing.value = false
  }
}

async function searchByName() {
  const name = foodName.value.trim()
  if (!name) {
    uni.showToast({ title: '请输入食物名称', icon: 'none' })
    return
  }
  if (recognizing.value) return
  recognizing.value = true
  results.value = []
  errorMsg.value = ''
  try {
    const res = await foodApi.recognizeByName(name)
    if (res.code === 200 && res.data) {
      results.value = parseFoodResults(res.data)
      if (results.value.length === 0) {
        errorMsg.value = `未找到"${name}"的营养信息`
      } else {
        addHistory('text', name, results.value)
      }
    } else {
      errorMsg.value = res.message || `未找到"${name}"的营养信息`
    }
  } catch (e: any) {
    errorMsg.value = e.message || '识别失败，请检查网络后重试'
  } finally {
    recognizing.value = false
  }
}

function quickRecognize(food: string) {
  if (recognizing.value) return
  mode.value = 'text'
  foodName.value = food
  searchByName()
}

// ─── Result Parsing ──────────────────────────────────────────────
function parseFoodResults(data: any): RecognitionResult[] {
  if (data.foods && Array.isArray(data.foods)) {
    return data.foods.map((item: any) => {
      const n = item.nutrition || {}
      return {
        name: item.name || '未知食物',
        confidence: item.confidence || 0,
        category: item.category || '',
        calories: n.energy || n.calories || 0,
        protein: n.protein || 0,
        fat: n.fat || 0,
        carbs: n.carbohydrate || n.carbs || 0,
        fiber: n.fiber || 0,
        sodium: n.sodium, calcium: n.calcium, potassium: n.potassium,
        magnesium: n.magnesium, iron: n.iron, zinc: n.zinc,
        phosphorus: n.phosphorus, selenium: n.selenium,
        copper: n.copper, manganese: n.manganese,
        vitaminA: n.vitaminA, vitaminC: n.vitaminC, vitaminE: n.vitaminE,
        carotene: n.carotene, thiamine: n.thiamine, riboflavin: n.riboflavin,
        niacin: n.niacin, retinolEquivalent: n.retinolEquivalent,
        cholesterol: n.cholesterol,
        source: n.source || ''
      }
    })
  }
  if (data.name) {
    return [{
      name: data.name,
      confidence: data.confidence || 1,
      calories: data.calories || data.energy || 0,
      protein: data.protein || 0,
      fat: data.fat || 0,
      carbs: data.carbs || data.carbohydrate || 0,
      fiber: data.fiber || 0
    }]
  }
  return []
}

// ─── Collapsible Sections ────────────────────────────────────────
function toggleSection(index: number, type: string) {
  const key = `${index}_${type}`
  expandedSections[key] = !expandedSections[key]
}

function isSectionOpen(index: number, type: string): boolean {
  return !!expandedSections[`${index}_${type}`]
}

// ─── Save to Record ──────────────────────────────────────────────
function openMealPicker(index: number) {
  savingIndex.value = index
  showMealPicker.value = true
}

async function saveToRecord(mealType: string) {
  showMealPicker.value = false
  const item = results.value[savingIndex.value]
  if (!item) return

  try {
    const now = new Date()
    const pad = (n: number) => String(n).padStart(2, '0')
    const recordTime = `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())}T${pad(now.getHours())}:${pad(now.getMinutes())}:${pad(now.getSeconds())}`

    const res = await foodApi.createRecord({
      mealType,
      foodName: item.name,
      portion: 100,
      calories: item.calories || 0,
      protein: item.protein || 0,
      fat: item.fat || 0,
      carbohydrates: item.carbs || 0,
      recordTime
    })

    if (res.code === 200) {
      uni.showToast({ title: `已记录「${item.name}」`, icon: 'success' })
    } else {
      uni.showToast({ title: res.message || '记录失败', icon: 'none' })
    }
  } catch (e: any) {
    uni.showToast({ title: '记录失败，请重试', icon: 'none' })
  }
}

function goToRecord(item: RecognitionResult) {
  const data = encodeURIComponent(JSON.stringify({
    foodName: item.name,
    calories: item.calories || 0,
    protein: item.protein || 0,
    fat: item.fat || 0,
    carbs: item.carbs || 0,
    portion: '每100g'
  }))
  uni.navigateTo({ url: `/pages/food-records/index?prefill=${data}` })
}

// ─── History ─────────────────────────────────────────────────────
function loadHistory() {
  try {
    const raw = uni.getStorageSync(HISTORY_KEY)
    if (raw) history.value = JSON.parse(raw)
  } catch { history.value = [] }
}

function addHistory(type: 'photo' | 'text', query: string, items: RecognitionResult[]) {
  const entry: HistoryItem = {
    id: Date.now().toString(),
    type,
    query,
    foodNames: items.map(i => i.name),
    foodCalories: items.map(i => i.calories || 0),
    timestamp: Date.now()
  }
  history.value.unshift(entry)
  if (history.value.length > MAX_HISTORY) {
    history.value = history.value.slice(0, MAX_HISTORY)
  }
  saveHistory()
}

function saveHistory() {
  try {
    uni.setStorageSync(HISTORY_KEY, JSON.stringify(history.value))
  } catch {}
}

function toggleHistory(id: string) {
  expandedHistory[id] = !expandedHistory[id]
}

function clearHistory() {
  uni.showModal({
    title: '清空历史',
    content: '确定清空所有识别历史记录？',
    success: (res) => {
      if (res.confirm) {
        history.value = []
        Object.keys(expandedHistory).forEach(k => delete expandedHistory[k])
        saveHistory()
        uni.showToast({ title: '已清空', icon: 'success' })
      }
    }
  })
}

// ─── Utilities ───────────────────────────────────────────────────
function formatConfidence(val: number): string {
  if (!val) return '—'
  return (val * 100).toFixed(0) + '%'
}

function confidenceClass(val: number): string {
  if (val >= 0.8) return 'confidence-high'
  if (val >= 0.5) return 'confidence-mid'
  return 'confidence-low'
}

function nf(val: number | undefined | null, digits = 1): string {
  if (val == null || isNaN(val)) return '0'
  return Number(val).toFixed(digits)
}

function hasMinerals(item: RecognitionResult): boolean {
  return !!(item.calcium || item.iron || item.zinc || item.sodium || item.potassium ||
    item.magnesium || item.phosphorus || item.selenium || item.copper || item.manganese)
}

function hasVitamins(item: RecognitionResult): boolean {
  return !!(item.vitaminA || item.vitaminC || item.vitaminE || item.carotene ||
    item.thiamine || item.riboflavin || item.niacin || item.retinolEquivalent)
}

function sourceText(source: string): string {
  const map: Record<string, string> = {
    tianapi: '天聚数行API',
    database: '数据库（准确）',
    estimated: 'AI估算',
    default: '默认值',
    'baidu-calorie-only': '识别（仅卡路里）'
  }
  return map[source] || source
}

function formatTime(ts: number): string {
  try {
    return new Date(ts).toLocaleString('zh-CN', {
      month: '2-digit', day: '2-digit',
      hour: '2-digit', minute: '2-digit'
    })
  } catch { return '' }
}
</script>

<style lang="scss" scoped>
.container {
  padding: 20rpx 30rpx;
  padding-bottom: 80rpx;
  min-height: 100vh;
  background: $background;
}

/* ─── Header ──────────────────────────────────── */
.page-header {
  padding: 20rpx 0 10rpx;
}
.page-title {
  display: block;
  font-size: 40rpx;
  font-weight: 700;
  color: $foreground;
  font-family: 'Calistoga', Georgia, 'PingFang SC', serif;
}
.page-desc {
  display: block;
  font-size: 26rpx;
  margin-top: 8rpx;
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
  cursor: pointer;
}

/* ─── Mode Tabs ───────────────────────────────── */
.mode-tabs {
  background: $card;
  border: 1rpx solid $border;
  border-radius: $radius-full;
  margin: 20rpx 0;
  overflow: hidden;
  padding: 6rpx;
  box-shadow: $shadow-sm;
}
.mode-tab {
  height: 80rpx;
  font-size: 28rpx;
  color: $muted-foreground;
  border-radius: $radius-full;
  transition: all 0.2s;
  font-family: 'Inter', 'PingFang SC', sans-serif;
  cursor: pointer;
}
.mode-tab.active {
  background: $gradient-accent;
  color: #fff;
  font-weight: 500;
  box-shadow: $shadow-accent;
}

/* ─── Photo Section ───────────────────────────── */
.photo-area {
  height: 400rpx;
  overflow: hidden;
  margin-bottom: 20rpx;
  cursor: pointer;
  border: 2rpx dashed $border;
  border-radius: $radius-2xl;
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
}
.photo-area:active {
  border-color: $accent;
}
.preview-image {
  width: 100%;
  height: 100%;
}
.photo-placeholder {
  flex-direction: column;
  gap: 12rpx;
}
.photo-icon {
  font-size: 80rpx;
}
.photo-hint {
  font-size: 28rpx;
  color: $muted-foreground;
  font-family: 'Inter', 'PingFang SC', sans-serif;
}
.photo-sub-hint {
  font-size: 22rpx;
  color: $muted-foreground;
  opacity: 0.7;
}

.remove-image-btn {
  position: absolute;
  top: 16rpx;
  right: 16rpx;
  width: 52rpx;
  height: 52rpx;
  background: rgba(239, 68, 68, 0.9);
  color: #fff;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24rpx;
  font-weight: 700;
  box-shadow: 0 2rpx 8rpx rgba(0, 0, 0, 0.2);
  z-index: 5;
  cursor: pointer;
}

.photo-actions {
  gap: 20rpx;
  margin-bottom: 16rpx;
}
.photo-actions .btn-primary,
.photo-actions .btn-outline {
  height: 80rpx;
  line-height: 80rpx;
  font-size: 28rpx;
}

.recognize-btn {
  margin-bottom: 16rpx;
  height: 84rpx;
  line-height: 84rpx;
  font-size: 30rpx;
}

.photo-tip {
  padding: 12rpx 20rpx;
  font-size: 22rpx;
  color: #e6a23c;
}

/* ─── Text Section ────────────────────────────── */
.search-btn {
  margin-top: 20rpx;
}

/* ─── Quick Food Chips ────────────────────────── */
.quick-section {
  margin-top: 20rpx;
  padding: 24rpx;
}
.quick-title {
  display: block;
  font-size: 26rpx;
  font-weight: 700;
  color: $foreground;
  margin-bottom: 16rpx;
  font-family: 'Calistoga', Georgia, 'PingFang SC', serif;
}
.quick-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 16rpx;
}
.quick-chip {
  padding: 10rpx 28rpx;
  background: rgba(16, 185, 129, 0.08);
  color: $accent;
  border: 1rpx solid rgba(16, 185, 129, 0.2);
  border-radius: $radius-full;
  font-size: 24rpx;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.15s;
  font-family: 'Inter', 'PingFang SC', sans-serif;
}
.quick-chip:active {
  background: $accent;
  color: #fff;
  transform: scale(0.95);
}
.quick-chip-disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

/* ─── Loading State ───────────────────────────── */
.loading-section {
  padding: 60rpx 0;
  margin-top: 20rpx;
}
.loading-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16rpx;
}
.loading-spinner {
  width: 64rpx;
  height: 64rpx;
  border: 6rpx solid $border;
  border-top-color: $accent;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}
@keyframes spin {
  to { transform: rotate(360deg); }
}
.loading-title {
  font-size: 32rpx;
  font-weight: 700;
  color: $foreground;
  font-family: 'Calistoga', Georgia, 'PingFang SC', serif;
}
.loading-text {
  font-size: 26rpx;
  color: $muted-foreground;
  font-family: 'Inter', 'PingFang SC', sans-serif;
}

/* ─── Empty State ─────────────────────────────── */
.empty-section {
  margin-top: 20rpx;
  padding: 60rpx 0;
}
.empty-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12rpx;
}
.empty-icon {
  font-size: 80rpx;
}
.empty-title {
  font-size: 32rpx;
  font-weight: 600;
  color: $muted-foreground;
  font-family: 'Calistoga', Georgia, 'PingFang SC', serif;
}
.empty-desc {
  font-size: 24rpx;
  text-align: center;
  padding: 0 40rpx;
}

/* ─── Result Bar ──────────────────────────────── */
.result-section {
  margin-top: 24rpx;
}
.result-bar {
  margin-bottom: 16rpx;
  align-items: center;
}
.result-bar-title {
  font-size: 32rpx;
  font-weight: 700;
  color: $foreground;
  font-family: 'Calistoga', Georgia, 'PingFang SC', serif;
}
.result-bar-count {
  font-size: 22rpx;
  color: $accent;
  background: rgba(16, 185, 129, 0.1);
  padding: 4rpx 16rpx;
  border-radius: $radius-full;
  font-weight: 500;
  font-family: 'JetBrains Mono', monospace;
}

/* ─── Result Grid ─────────────────────────────── */
.result-grid {
  display: flex;
  flex-direction: column;
  gap: 20rpx;
}
/* Two-column layout on wider screens */
@media (min-width: 640px) {
  .result-grid-multi {
    display: grid;
    grid-template-columns: repeat(2, 1fr);
    gap: 20rpx;
  }
}

/* ─── Result Card ─────────────────────────────── */
.result-card {
  padding: 28rpx;
  border: 2rpx solid transparent;
  transition: all 0.15s;
}
.result-card:active {
  border-color: $accent;
  box-shadow: $shadow-accent;
}

.result-header {
  margin-bottom: 10rpx;
}
.result-title-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12rpx;
}
.result-name {
  font-size: 34rpx;
  font-weight: 700;
  color: $foreground;
  font-family: 'Calistoga', Georgia, 'PingFang SC', serif;
}
.result-name-wrap {
  display: flex;
  align-items: center;
  gap: 12rpx;
  flex: 1;
  min-width: 0;
}
.category-badge {
  font-size: 20rpx;
  padding: 2rpx 12rpx;
  border-radius: $radius-full;
  font-weight: 500;
  font-family: 'JetBrains Mono', monospace;
  flex-shrink: 0;
}
.category-dish {
  background: rgba(245, 158, 11, 0.1);
  color: #F59E0B;
}
.category-fruit {
  background: rgba(16, 185, 129, 0.1);
  color: #10B981;
}
.result-portion {
  display: block;
  font-size: 24rpx;
  margin-top: 6rpx;
}

/* ─── Confidence Badge ────────────────────────── */
.confidence-badge {
  font-size: 20rpx;
  font-weight: 600;
  padding: 6rpx 16rpx;
  border-radius: $radius-full;
  flex-shrink: 0;
  white-space: nowrap;
  font-family: 'JetBrains Mono', monospace;
}
.confidence-high {
  background: rgba(16, 185, 129, 0.12);
  color: #10B981;
}
.confidence-mid {
  background: rgba(245, 158, 11, 0.12);
  color: #F59E0B;
}
.confidence-low {
  background: rgba(239, 68, 68, 0.12);
  color: #EF4444;
}

/* ─── Nutrition Grid ──────────────────────────── */
.nutrition-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16rpx;
  margin-top: 10rpx;
}
.nutrition-card {
  background: $muted;
  border: 1rpx solid $border;
  border-radius: $radius-lg;
  padding: 20rpx 12rpx;
  text-align: center;
}
.calories-card {
  grid-column: span 3;
  background: $gradient-accent;
  border: none;
  border-radius: $radius-xl;
  display: flex;
  flex-direction: row;
  align-items: center;
  justify-content: center;
  gap: 12rpx;
  padding: 28rpx;
  box-shadow: $shadow-accent;
}
.calories-card .n-value {
  font-size: 48rpx;
  color: #fff;
}
.calories-card .n-label {
  color: rgba(255, 255, 255, 0.85);
}
.calories-card .n-unit {
  font-size: 24rpx;
  color: rgba(255, 255, 255, 0.75);
}
.calories-card .n-icon {
  font-size: 36rpx;
}
.n-icon {
  display: block;
  font-size: 36rpx;
  margin-bottom: 8rpx;
}
.n-value {
  display: block;
  font-size: 32rpx;
  font-weight: 700;
  color: $foreground;
  font-family: 'Calistoga', Georgia, 'PingFang SC', serif;
}
.n-unit {
  font-size: 22rpx;
  color: $muted-foreground;
}
.n-label {
  display: block;
  font-size: 22rpx;
  color: $muted-foreground;
  margin-top: 4rpx;
}

/* ─── Nutrition Sub-sections ──────────────────── */
.nutrition-sub-section {
  margin-top: 16rpx;
}
.sub-section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8rpx 0;
  cursor: pointer;
}
.sub-section-title {
  font-size: 24rpx;
  font-weight: 700;
  color: $foreground;
  font-family: 'Inter', 'PingFang SC', sans-serif;
}
.sub-section-arrow {
  font-size: 20rpx;
  color: $muted-foreground;
}
.nutrition-grid-sm {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 10rpx;
  margin-top: 8rpx;
}
.n-item-sm {
  background: $muted;
  border: 1rpx solid $border;
  border-radius: $radius-md;
  padding: 12rpx 6rpx;
  text-align: center;
}
.n-sm-label {
  display: block;
  font-size: 20rpx;
  color: $muted-foreground;
  margin-bottom: 4rpx;
}
.n-sm-value {
  display: block;
  font-size: 24rpx;
  font-weight: 600;
  color: $foreground;
}
.n-sm-unit {
  font-size: 18rpx;
  font-weight: normal;
  color: $muted-foreground;
}

.data-source-row {
  margin-top: 14rpx;
  padding-top: 10rpx;
  border-top: 1rpx solid $border;
}
.data-source-text {
  font-size: 22rpx;
  color: $muted-foreground;
}

/* ─── Result Actions ──────────────────────────── */
.result-actions {
  margin-top: 20rpx;
  gap: 16rpx;
}
.record-btn {
  height: 72rpx;
  line-height: 72rpx;
  font-size: 24rpx;
}

/* ─── Error ───────────────────────────────────── */
.error-section {
  margin-top: 20rpx;
}
.error-icon {
  font-size: 80rpx;
  margin-bottom: 16rpx;
}
.error-text {
  display: block;
  font-size: 30rpx;
  color: $foreground;
  margin-bottom: 8rpx;
}
.error-hint {
  display: block;
  font-size: 24rpx;
}

/* ─── History Section ─────────────────────────── */
.history-section {
  margin-top: 32rpx;
}
.history-bar {
  margin-bottom: 16rpx;
  align-items: center;
}
.history-bar-title {
  font-size: 32rpx;
  font-weight: 700;
  color: $foreground;
  font-family: 'Calistoga', Georgia, 'PingFang SC', serif;
}
.history-bar-right {
  gap: 16rpx;
  align-items: center;
}
.history-count {
  font-size: 22rpx;
  color: $muted-foreground;
  background: $muted;
  padding: 4rpx 14rpx;
  border-radius: $radius-full;
  font-family: 'JetBrains Mono', monospace;
}
.history-clear {
  font-size: 22rpx;
  color: #EF4444;
  cursor: pointer;
}

.history-card {
  padding: 20rpx 24rpx;
  margin-bottom: 12rpx;
}
.history-summary {
  display: flex;
  justify-content: space-between;
  align-items: center;
  cursor: pointer;
}
.history-left {
  gap: 16rpx;
  align-items: center;
  flex: 1;
  min-width: 0;
}
.history-type-badge {
  width: 48rpx;
  height: 48rpx;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24rpx;
  flex-shrink: 0;
}
.badge-text {
  background: rgba(16, 185, 129, 0.1);
}
.badge-photo {
  background: rgba(59, 130, 246, 0.1);
}
.history-info {
  flex: 1;
  min-width: 0;
}
.history-query {
  display: block;
  font-size: 26rpx;
  font-weight: 500;
  color: $foreground;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.history-time {
  display: block;
  font-size: 20rpx;
  margin-top: 4rpx;
}
.history-right {
  gap: 12rpx;
  align-items: center;
  flex-shrink: 0;
}
.history-food-count {
  font-size: 22rpx;
  color: $accent;
  font-weight: 500;
}
.history-arrow {
  font-size: 20rpx;
  color: $muted-foreground;
}

.history-detail {
  margin-top: 16rpx;
  padding: 16rpx;
  background: $muted;
  border-radius: $radius-md;
  border-left: 4rpx solid $accent;
}
.history-food-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8rpx 0;
}
.history-food-row + .history-food-row {
  border-top: 1rpx solid $border;
}
.history-food-name {
  font-size: 24rpx;
  font-weight: 500;
  color: $foreground;
}
.history-food-cal {
  font-size: 22rpx;
  font-family: 'JetBrains Mono', monospace;
}

/* ─── Meal Type Picker Modal ──────────────────── */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(15, 23, 42, 0.5);
  z-index: 1000;
  display: flex;
  align-items: flex-end;
  justify-content: center;
}
.modal-content {
  width: 100%;
  max-width: 750rpx;
  background: $card;
  border-radius: $radius-2xl $radius-2xl 0 0;
  padding: 40rpx 30rpx;
  padding-bottom: 60rpx;
}
.modal-title {
  display: block;
  text-align: center;
  font-size: 30rpx;
  font-weight: 700;
  color: $foreground;
  margin-bottom: 32rpx;
  font-family: 'Calistoga', Georgia, 'PingFang SC', serif;
}
.meal-options {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20rpx;
}
.meal-option {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10rpx;
  padding: 28rpx 12rpx;
  background: $muted;
  border: 1rpx solid $border;
  border-radius: $radius-xl;
  cursor: pointer;
  transition: all 0.15s;
}
.meal-option:active {
  background: rgba(16, 185, 129, 0.1);
  border-color: $accent;
  transform: scale(0.95);
}
.meal-icon {
  font-size: 40rpx;
}
.meal-label {
  font-size: 24rpx;
  font-weight: 500;
  color: $foreground;
}
.modal-cancel {
  margin-top: 24rpx;
  text-align: center;
  padding: 20rpx;
  cursor: pointer;
}
.modal-cancel-text {
  font-size: 28rpx;
  color: $muted-foreground;
}
</style>
