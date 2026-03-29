<template>
  <view class="container">
    <view class="page-header">
      <text class="page-title">🔍 食物识别</text>
      <text class="page-desc text-secondary">拍照或输入食物名称，获取营养信息</text>
    </view>

    <!-- Mode Tabs -->
    <view class="mode-tabs flex">
      <view
        class="mode-tab flex-1 flex-center"
        :class="{ active: mode === 'photo' }"
        @tap="mode = 'photo'"
      >📷 拍照识别</view>
      <view
        class="mode-tab flex-1 flex-center"
        :class="{ active: mode === 'text' }"
        @tap="mode = 'text'"
      >✏️ 文字识别</view>
    </view>

    <!-- Photo Mode -->
    <view v-show="mode === 'photo'" class="photo-section">
      <view class="card photo-area flex-center" @tap="takePhoto">
        <image v-if="photoPath" :src="photoPath" class="preview-image" mode="aspectFit" />
        <view v-else class="photo-placeholder flex-center">
          <text class="photo-icon">📷</text>
          <text class="photo-hint">点击拍照识别食物</text>
        </view>
      </view>
      <view class="photo-actions flex">
        <button class="btn-primary flex-1" @tap="takePhoto">
          {{ photoPath ? '重新拍照' : '拍照识别' }}
        </button>
        <button class="btn-outline flex-1" @tap="chooseFromAlbum" style="margin-left: 20rpx;">
          从相册选择
        </button>
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
              placeholder="请输入食物名称，如：鸡胸肉"
              confirm-type="search"
              @confirm="searchByName"
            />
          </view>
        </view>
      </view>
      <button class="btn-primary search-btn" :disabled="!foodName.trim() || recognizing" @tap="searchByName">
        {{ recognizing ? '识别中...' : '🔍 识别食物' }}
      </button>
    </view>

    <!-- Loading State -->
    <view v-if="recognizing" class="loading-section card flex-center">
      <view class="loading-content">
        <view class="loading-spinner" />
        <text class="loading-text">正在识别中，请稍候...</text>
      </view>
    </view>

    <!-- Recognition Results (multiple) -->
    <view v-if="results.length > 0 && !recognizing" class="result-section">
      <view class="result-count">
        <text class="result-count-text">共识别出 {{ results.length }} 种食物</text>
      </view>

      <view
        v-for="(item, index) in results"
        :key="index"
        class="card result-card"
        :class="{ 'result-card-selected': selectedIndex === index }"
        @tap="selectedIndex = index"
      >
        <view class="result-header">
          <view class="result-title-row">
            <text class="result-name">{{ item.name }}</text>
            <view class="confidence-badge" :class="confidenceClass(item.confidence)">
              {{ formatConfidence(item.confidence) }}
            </view>
          </view>
          <text class="result-portion text-secondary">每100g</text>
        </view>

        <view class="divider" />

        <view class="nutrition-grid">
          <view class="nutrition-card calories-card">
            <text class="n-icon">🔥</text>
            <text class="n-value">{{ item.calories || 0 }}</text>
            <text class="n-unit">千卡</text>
            <text class="n-label">热量</text>
          </view>
          <view class="nutrition-card">
            <text class="n-icon">🥩</text>
            <text class="n-value">{{ item.protein || 0 }}g</text>
            <text class="n-label">蛋白质</text>
          </view>
          <view class="nutrition-card">
            <text class="n-icon">🧈</text>
            <text class="n-value">{{ item.fat || 0 }}g</text>
            <text class="n-label">脂肪</text>
          </view>
          <view class="nutrition-card">
            <text class="n-icon">🍚</text>
            <text class="n-value">{{ item.carbs || 0 }}g</text>
            <text class="n-label">碳水</text>
          </view>
          <view class="nutrition-card" v-if="item.fiber">
            <text class="n-icon">🌾</text>
            <text class="n-value">{{ item.fiber }}g</text>
            <text class="n-label">膳食纤维</text>
          </view>
        </view>

        <button
          class="btn-primary record-btn-inline"
          @tap.stop="goToRecord(item)"
        >📝 记录到饮食</button>
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
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { foodApi } from '@/services/api'
import { checkLogin } from '@/utils/common'

interface RecognitionResult {
  name: string
  confidence: number
  calories?: number
  protein?: number
  fat?: number
  carbs?: number
  fiber?: number
}

const mode = ref<'photo' | 'text'>('photo')
const photoPath = ref('')
const foodName = ref('')
const recognizing = ref(false)
const results = ref<RecognitionResult[]>([])
const selectedIndex = ref(0)
const errorMsg = ref('')

onLoad((options) => {
  if (!checkLogin()) return
  if (options?.mode === 'text') {
    mode.value = 'text'
  }
})

function takePhoto() {
  uni.chooseImage({
    count: 1,
    sourceType: ['camera'],
    sizeType: ['compressed'],
    success: (res) => {
      photoPath.value = res.tempFilePaths[0]
      recognizePhoto(res.tempFilePaths[0])
    },
    fail: () => {}
  })
}

function chooseFromAlbum() {
  uni.chooseImage({
    count: 1,
    sourceType: ['album'],
    sizeType: ['compressed'],
    success: (res) => {
      photoPath.value = res.tempFilePaths[0]
      recognizePhoto(res.tempFilePaths[0])
    },
    fail: () => {}
  })
}

async function recognizePhoto(filePath: string) {
  recognizing.value = true
  results.value = []
  selectedIndex.value = 0
  errorMsg.value = ''
  try {
    const res = await foodApi.photoRecognize(filePath)
    if (res.code === 200 && res.data) {
      results.value = parseFoodResults(res.data)
      if (results.value.length === 0) {
        errorMsg.value = '无法识别该食物，请重试'
      }
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
  recognizing.value = true
  results.value = []
  selectedIndex.value = 0
  errorMsg.value = ''
  try {
    const res = await foodApi.recognizeByName(name)
    if (res.code === 200 && res.data) {
      results.value = parseFoodResults(res.data)
      if (results.value.length === 0) {
        errorMsg.value = `未找到"${name}"的营养信息`
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

function parseFoodResults(data: any): RecognitionResult[] {
  // Backend: { foods: [{ name, confidence, nutrition: { energy, protein, carbohydrate, fat, fiber } }] }
  if (data.foods && Array.isArray(data.foods)) {
    return data.foods.map((item: any) => {
      const n = item.nutrition || {}
      return {
        name: item.name || '未知食物',
        confidence: item.confidence || 0,
        calories: n.energy || n.calories || 0,
        protein: n.protein || 0,
        fat: n.fat || 0,
        carbs: n.carbohydrate || n.carbs || 0,
        fiber: n.fiber || 0
      }
    })
  }
  // Fallback: single flat result
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

function formatConfidence(val: number): string {
  if (!val) return '—'
  return (val * 100).toFixed(1) + '%'
}

function confidenceClass(val: number): string {
  if (val >= 0.8) return 'confidence-high'
  if (val >= 0.5) return 'confidence-mid'
  return 'confidence-low'
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
</script>

<style scoped>
.container {
  padding: 20rpx 30rpx;
  padding-bottom: 60rpx;
  min-height: 100vh;
  background: #f5f5f5;
}

.page-header {
  padding: 20rpx 0 10rpx;
}
.page-title {
  display: block;
  font-size: 40rpx;
  font-weight: 700;
  color: #333;
}
.page-desc {
  display: block;
  font-size: 26rpx;
  margin-top: 8rpx;
}

/* Mode Tabs */
.mode-tabs {
  background: #fff;
  border-radius: 16rpx;
  margin: 20rpx 0;
  overflow: hidden;
  padding: 6rpx;
}
.mode-tab {
  height: 80rpx;
  font-size: 28rpx;
  color: #666;
  border-radius: 12rpx;
  transition: all 0.2s;
}
.mode-tab.active {
  background: #07c160;
  color: #fff;
  font-weight: 500;
}

/* Photo Section */
.photo-area {
  height: 400rpx;
  overflow: hidden;
  margin-bottom: 20rpx;
  cursor: pointer;
}
.preview-image {
  width: 100%;
  height: 100%;
}
.photo-placeholder {
  flex-direction: column;
  gap: 16rpx;
}
.photo-icon {
  font-size: 80rpx;
}
.photo-hint {
  font-size: 28rpx;
  color: #999;
}
.photo-actions {
  gap: 20rpx;
}
.photo-actions .btn-primary,
.photo-actions .btn-outline {
  height: 80rpx;
  line-height: 80rpx;
  font-size: 28rpx;
}

/* Text Section */
.search-btn {
  margin-top: 20rpx;
}

/* Loading */
.loading-section {
  padding: 60rpx 0;
}
.loading-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 20rpx;
}
.loading-spinner {
  width: 60rpx;
  height: 60rpx;
  border: 6rpx solid #eee;
  border-top-color: #07c160;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}
@keyframes spin {
  to { transform: rotate(360deg); }
}
.loading-text {
  font-size: 28rpx;
  color: #666;
}

/* Result Count */
.result-count {
  padding: 16rpx 0;
}
.result-count-text {
  font-size: 26rpx;
  color: #666;
  font-weight: 500;
}

/* Result Card */
.result-card {
  padding: 30rpx;
  margin-bottom: 20rpx;
  border: 4rpx solid transparent;
  transition: border-color 0.2s;
}
.result-card-selected {
  border-color: #07c160;
}

.result-header {
  margin-bottom: 10rpx;
}
.result-title-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.result-name {
  font-size: 36rpx;
  font-weight: 700;
  color: #333;
}
.result-portion {
  display: block;
  font-size: 24rpx;
  margin-top: 6rpx;
}

/* Confidence Badge */
.confidence-badge {
  font-size: 22rpx;
  font-weight: 600;
  padding: 6rpx 16rpx;
  border-radius: 20rpx;
  flex-shrink: 0;
}
.confidence-high {
  background: #e8f5e9;
  color: #2e7d32;
}
.confidence-mid {
  background: #fff3e0;
  color: #e65100;
}
.confidence-low {
  background: #fce4ec;
  color: #c62828;
}

.nutrition-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16rpx;
  margin-top: 10rpx;
}
.nutrition-card {
  background: #f8f9fa;
  border-radius: 16rpx;
  padding: 20rpx 12rpx;
  text-align: center;
}
.calories-card {
  grid-column: span 3;
  background: linear-gradient(135deg, #fff3e0, #ffe0b2);
  display: flex;
  flex-direction: row;
  align-items: center;
  justify-content: center;
  gap: 12rpx;
  padding: 28rpx;
}
.calories-card .n-value {
  font-size: 48rpx;
  color: #e65100;
}
.calories-card .n-label {
  color: #e65100;
}
.calories-card .n-unit {
  font-size: 24rpx;
  color: #e65100;
  opacity: 0.8;
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
  color: #333;
}
.n-label {
  display: block;
  font-size: 22rpx;
  color: #999;
  margin-top: 4rpx;
}

.record-btn-inline {
  margin-top: 20rpx;
  height: 72rpx;
  line-height: 72rpx;
  font-size: 26rpx;
  background: #07c160;
  color: #fff;
  border: none;
  border-radius: 12rpx;
}

/* Error */
.error-icon {
  font-size: 80rpx;
  margin-bottom: 16rpx;
}
.error-text {
  display: block;
  font-size: 30rpx;
  color: #333;
  margin-bottom: 8rpx;
}
.error-hint {
  display: block;
  font-size: 24rpx;
}
</style>
