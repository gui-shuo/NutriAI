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
            <view class="result-name-wrap">
              <text class="result-name">{{ item.name }}</text>
              <view v-if="item.category" class="category-badge" :class="item.category === '果蔬' ? 'category-fruit' : 'category-dish'">
                {{ item.category }}
              </view>
            </view>
            <view class="confidence-badge" :class="confidenceClass(item.confidence)">
              {{ formatConfidence(item.confidence) }}
            </view>
          </view>
          <text class="result-portion text-secondary">每100g</text>
        </view>

        <view class="divider" />

        <!-- 基础营养 -->
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
            <text class="n-icon">💧</text>
            <text class="n-value">{{ nf(item.cholesterol) }}mg</text>
            <text class="n-label">胆固醇</text>
          </view>
        </view>

        <!-- 矿物质 -->
        <view v-if="hasMinerals(item)" class="nutrition-sub-section">
          <text class="sub-section-title">💎 矿物质</text>
          <view class="nutrition-grid-sm">
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

        <!-- 维生素 -->
        <view v-if="hasVitamins(item)" class="nutrition-sub-section">
          <text class="sub-section-title">🧪 维生素</text>
          <view class="nutrition-grid-sm">
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

        <!-- 数据来源 -->
        <view v-if="item.source" class="data-source-row">
          <text class="data-source-text">📋 数据来源: {{ sourceText(item.source) }}</text>
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
  category?: string
  calories?: number
  protein?: number
  fat?: number
  carbs?: number
  fiber?: number
  // 矿物质
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
  // 维生素
  vitaminA?: number
  vitaminC?: number
  vitaminE?: number
  carotene?: number
  thiamine?: number
  riboflavin?: number
  niacin?: number
  retinolEquivalent?: number
  // 其他
  cholesterol?: number
  source?: string
}

const mode = ref<'photo' | 'text'>('photo')
const showDisclaimer = ref(true)
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
      compressAndRecognize(res.tempFilePaths[0])
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
      compressAndRecognize(res.tempFilePaths[0])
    },
    fail: () => {}
  })
}

function compressAndRecognize(filePath: string) {
  // 先压缩图片再上传，减少超时风险
  uni.compressImage({
    src: filePath,
    quality: 60,
    success: (compressed) => {
      photoPath.value = compressed.tempFilePath
      recognizePhoto(compressed.tempFilePath)
    },
    fail: () => {
      // 压缩失败则用原图
      photoPath.value = filePath
      recognizePhoto(filePath)
    }
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
    database: '数据库',
    estimated: 'AI估算',
    default: '默认值'
  }
  return map[source] || source
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
.result-name-wrap {
  display: flex;
  align-items: center;
  gap: 12rpx;
}
.category-badge {
  font-size: 20rpx;
  padding: 2rpx 12rpx;
  border-radius: 8rpx;
  font-weight: 500;
}
.category-dish {
  background: #fff3e0;
  color: #e65100;
}
.category-fruit {
  background: #e8f5e9;
  color: #2e7d32;
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

/* 营养子板块 */
.nutrition-sub-section {
  margin-top: 16rpx;
}
.sub-section-title {
  display: block;
  font-size: 24rpx;
  font-weight: 600;
  color: #555;
  margin-bottom: 10rpx;
}
.nutrition-grid-sm {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 10rpx;
}
.n-item-sm {
  background: #f8f9fa;
  border-radius: 10rpx;
  padding: 12rpx 6rpx;
  text-align: center;
}
.n-sm-label {
  display: block;
  font-size: 20rpx;
  color: #999;
  margin-bottom: 4rpx;
}
.n-sm-value {
  display: block;
  font-size: 24rpx;
  font-weight: 600;
  color: #333;
}
.n-sm-unit {
  font-size: 18rpx;
  font-weight: normal;
  color: #999;
}
.data-source-row {
  margin-top: 14rpx;
  padding-top: 10rpx;
  border-top: 1rpx solid #eee;
}
.data-source-text {
  font-size: 22rpx;
  color: #999;
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
.disclaimer-tip {
  background: #fff3cd;
  color: #856404;
  border-radius: 12rpx;
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
  color: #999;
}
</style>
