<template>
  <view class="page-container" v-if="recipe">
    <!-- Hero cover -->
    <view class="hero-wrap">
      <image class="hero-image" :src="recipe.coverImage" mode="aspectFill" />
    </view>

    <!-- Title section -->
    <view class="section title-section">
      <text class="recipe-title">{{ recipe.title }}</text>
      <text class="recipe-desc" v-if="recipe.description">{{ recipe.description }}</text>
      <view class="badge-row">
        <view class="info-badge" v-if="recipe.difficulty">
          <text :class="['badge-dot', difficultyClass(recipe.difficulty)]" />
          <text class="badge-text">{{ recipe.difficulty }}</text>
        </view>
        <view class="info-badge" v-if="recipe.cuisine">
          <text class="badge-text">{{ recipe.cuisine }}</text>
        </view>
        <view class="info-badge" v-if="recipe.dietType">
          <text class="badge-text">{{ recipe.dietType }}</text>
        </view>
      </view>
    </view>

    <!-- Stats row -->
    <view class="section stats-row">
      <view class="stat-item">
        <text class="stat-icon">⏱</text>
        <text class="stat-value">{{ (recipe.prepTime || 0) + (recipe.cookingTime || 0) }}</text>
        <text class="stat-label">分钟</text>
      </view>
      <view class="stat-divider" />
      <view class="stat-item">
        <text class="stat-icon">🍽</text>
        <text class="stat-value">{{ recipe.servings || 1 }}</text>
        <text class="stat-label">份</text>
      </view>
      <view class="stat-divider" />
      <view class="stat-item">
        <text class="stat-icon">🔥</text>
        <text class="stat-value">{{ recipe.calories || 0 }}</text>
        <text class="stat-label">kcal</text>
      </view>
      <view class="stat-divider" />
      <view class="stat-item">
        <text class="stat-icon">⭐</text>
        <text class="stat-value">{{ (recipe.ratingAvg || 0).toFixed(1) }}</text>
        <text class="stat-label">评分</text>
      </view>
    </view>

    <!-- Nutrition card -->
    <view class="section">
      <text class="section-title">营养成分</text>
      <view class="nutrition-grid">
        <view class="nutrition-item">
          <text class="nut-icon nut-cal">🔥</text>
          <text class="nut-value">{{ recipe.calories || 0 }}</text>
          <text class="nut-label">热量(kcal)</text>
        </view>
        <view class="nutrition-item">
          <text class="nut-icon nut-protein">🥩</text>
          <text class="nut-value">{{ recipe.protein || 0 }}g</text>
          <text class="nut-label">蛋白质</text>
        </view>
        <view class="nutrition-item">
          <text class="nut-icon nut-fat">🫒</text>
          <text class="nut-value">{{ recipe.fat || 0 }}g</text>
          <text class="nut-label">脂肪</text>
        </view>
        <view class="nutrition-item">
          <text class="nut-icon nut-carbs">🌾</text>
          <text class="nut-value">{{ recipe.carbs || 0 }}g</text>
          <text class="nut-label">碳水</text>
        </view>
        <view class="nutrition-item">
          <text class="nut-icon nut-fiber">🥦</text>
          <text class="nut-value">{{ recipe.fiber || 0 }}g</text>
          <text class="nut-label">膳食纤维</text>
        </view>
        <view class="nutrition-item">
          <text class="nut-icon nut-sodium">🧂</text>
          <text class="nut-value">{{ recipe.sodium || 0 }}mg</text>
          <text class="nut-label">钠</text>
        </view>
      </view>
    </view>

    <!-- Ingredients -->
    <view class="section">
      <text class="section-title">食材清单</text>
      <!-- Main ingredients -->
      <view class="ingredient-group" v-if="mainIngredients.length">
        <text class="group-label">主料</text>
        <view
          v-for="(ing, idx) in mainIngredients"
          :key="'main-' + idx"
          class="ingredient-row"
        >
          <text class="ing-name">{{ ing.name }}</text>
          <text class="ing-amount">{{ ing.amount }}{{ ing.unit }}</text>
        </view>
      </view>
      <!-- Auxiliary ingredients -->
      <view class="ingredient-group" v-if="auxIngredients.length">
        <text class="group-label">辅料</text>
        <view
          v-for="(ing, idx) in auxIngredients"
          :key="'aux-' + idx"
          class="ingredient-row"
        >
          <text class="ing-name">{{ ing.name }}</text>
          <text class="ing-amount">{{ ing.amount }}{{ ing.unit }}</text>
        </view>
      </view>
    </view>

    <!-- Steps -->
    <view class="section">
      <text class="section-title">烹饪步骤</text>
      <view
        v-for="(step, idx) in recipe.steps || []"
        :key="'step-' + idx"
        class="step-item"
      >
        <view class="step-header">
          <view class="step-number">{{ idx + 1 }}</view>
          <text class="step-desc">{{ step.description }}</text>
        </view>
        <image
          v-if="step.image"
          class="step-image"
          :src="step.image"
          mode="aspectFill"
          @tap="previewImage(step.image)"
        />
        <view class="step-tip" v-if="step.tips">
          <text class="tip-icon">💡</text>
          <text class="tip-text">{{ step.tips }}</text>
        </view>
      </view>
    </view>

    <!-- Tags -->
    <view class="section" v-if="recipe.tags && recipe.tags.length">
      <text class="section-title">标签</text>
      <view class="tag-row">
        <view class="tag-chip" v-for="(tag, idx) in recipe.tags" :key="'tag-' + idx">
          <text class="tag-text"># {{ tag }}</text>
        </view>
      </view>
    </view>

    <!-- Bottom spacer for action bar -->
    <view class="bottom-spacer" />

    <!-- Fixed action bar -->
    <view class="action-bar">
      <view
        class="action-btn fav-btn"
        :class="{ favorited: isFavorited }"
        @tap="toggleFavorite"
      >
        <text class="action-icon">{{ isFavorited ? '❤️' : '🤍' }}</text>
        <text class="action-label">{{ recipe.favoriteCount || 0 }}</text>
      </view>
      <view class="action-btn rate-btn" @tap="showRatingPopup = true">
        <text class="action-icon">⭐</text>
        <text class="action-label">评分</text>
      </view>
      <button class="action-btn share-btn" open-type="share">
        <text class="action-icon">📤</text>
        <text class="action-label">分享</text>
      </button>
    </view>

    <!-- Rating popup -->
    <view class="popup-mask" v-if="showRatingPopup" @tap="showRatingPopup = false">
      <view class="popup-content" @tap.stop>
        <view class="popup-header">
          <text class="popup-title">为这道食谱打分</text>
          <text class="popup-close" @tap="showRatingPopup = false">✕</text>
        </view>
        <view class="rating-stars">
          <text
            v-for="s in 5"
            :key="s"
            class="star-icon"
            :class="{ filled: s <= ratingScore }"
            @tap="ratingScore = s"
          >
            {{ s <= ratingScore ? '★' : '☆' }}
          </text>
        </view>
        <textarea
          class="rating-comment"
          v-model="ratingComment"
          placeholder="写下你的评价（选填）"
          maxlength="200"
          :auto-height="false"
        />
        <view
          class="popup-submit"
          :class="{ disabled: ratingScore === 0 }"
          @tap="submitRating"
        >
          <text class="submit-text">提交评分</text>
        </view>
      </view>
    </view>
  </view>

  <!-- Loading state -->
  <view class="loading-page" v-else>
    <text class="loading-text">加载中...</text>
  </view>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { request } from '@/utils/request'

const props = defineProps({ id: String })

const recipe = ref<any>(null)
const isFavorited = ref(false)
const showRatingPopup = ref(false)
const ratingScore = ref(0)
const ratingComment = ref('')

const mainIngredients = computed(() =>
  (recipe.value?.ingredients || []).filter((i: any) => i.isMain)
)

const auxIngredients = computed(() =>
  (recipe.value?.ingredients || []).filter((i: any) => !i.isMain)
)

function difficultyClass(difficulty: string) {
  const map: Record<string, string> = {
    '简单': 'dot-easy',
    '中等': 'dot-medium',
    '困难': 'dot-hard'
  }
  return map[difficulty] || 'dot-easy'
}

function previewImage(url: string) {
  const allImages = (recipe.value?.steps || [])
    .filter((s: any) => s.image)
    .map((s: any) => s.image)
  uni.previewImage({
    urls: allImages.length ? allImages : [url],
    current: url
  })
}

async function fetchRecipe() {
  try {
    const res = await request({ url: `/recipes/${props.id}` })
    recipe.value = res.data
    isFavorited.value = res.data?.isFavorited || false
  } catch (e: any) {
    uni.showToast({ title: '加载失败', icon: 'none' })
  }
}

async function toggleFavorite() {
  try {
    const res = await request({
      url: `/recipes/${props.id}/favorite`,
      method: 'POST'
    })
    isFavorited.value = !isFavorited.value
    if (recipe.value) {
      recipe.value.favoriteCount += isFavorited.value ? 1 : -1
    }
    uni.showToast({
      title: isFavorited.value ? '已收藏' : '已取消收藏',
      icon: 'none'
    })
  } catch (e: any) {
    uni.showToast({ title: '操作失败', icon: 'none' })
  }
}

async function submitRating() {
  if (ratingScore.value === 0) return
  try {
    await request({
      url: `/recipes/${props.id}/rate`,
      method: 'POST',
      data: {
        score: ratingScore.value,
        comment: ratingComment.value
      }
    })
    uni.showToast({ title: '评分成功', icon: 'success' })
    showRatingPopup.value = false
    ratingScore.value = 0
    ratingComment.value = ''
    fetchRecipe()
  } catch (e: any) {
    uni.showToast({ title: '评分失败', icon: 'none' })
  }
}

onLoad(() => {
  if (props.id) {
    fetchRecipe()
  }
})
</script>

<style lang="scss" scoped>
.page-container {
  min-height: 100vh;
  background: $background;
}

.loading-page {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  background: $background;
}

.loading-text {
  font-size: 28rpx;
  color: $muted-foreground;
}

/* ===== Hero ===== */
.hero-wrap {
  width: 100%;
  height: 400rpx;
}

.hero-image {
  width: 100%;
  height: 100%;
}

/* ===== Section ===== */
.section {
  background: $card;
  margin: 20rpx 24rpx;
  padding: 28rpx;
  border-radius: $radius-xl;
  box-shadow: $shadow-sm;
}

.section-title {
  font-size: 30rpx;
  font-weight: 700;
  color: $foreground;
  margin-bottom: 20rpx;
  display: block;
}

/* ===== Title Section ===== */
.title-section {
  margin-top: -40rpx;
  position: relative;
  z-index: 2;
}

.recipe-title {
  font-size: 36rpx;
  font-weight: 700;
  color: $foreground;
  display: block;
  line-height: 1.4;
}

.recipe-desc {
  font-size: 26rpx;
  color: $muted-foreground;
  margin-top: 12rpx;
  display: block;
  line-height: 1.6;
}

.badge-row {
  display: flex;
  flex-wrap: wrap;
  gap: 12rpx;
  margin-top: 16rpx;
}

.info-badge {
  display: flex;
  align-items: center;
  gap: 8rpx;
  padding: 6rpx 16rpx;
  border-radius: $radius-full;
  background: $muted;
}

.badge-dot {
  width: 12rpx;
  height: 12rpx;
  border-radius: 50%;
  display: inline-block;
}

.dot-easy { background: #22C55E; }
.dot-medium { background: #F59E0B; }
.dot-hard { background: #EF4444; }

.badge-text {
  font-size: 22rpx;
  color: $foreground;
}

/* ===== Stats Row ===== */
.stats-row {
  display: flex;
  align-items: center;
  justify-content: space-around;
}

.stat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4rpx;
}

.stat-icon {
  font-size: 36rpx;
}

.stat-value {
  font-size: 30rpx;
  font-weight: 700;
  color: $foreground;
}

.stat-label {
  font-size: 22rpx;
  color: $muted-foreground;
}

.stat-divider {
  width: 1rpx;
  height: 60rpx;
  background: $border;
}

/* ===== Nutrition Grid ===== */
.nutrition-grid {
  display: grid;
  grid-template-columns: 1fr 1fr 1fr;
  gap: 20rpx;
}

.nutrition-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6rpx;
  padding: 16rpx 0;
  background: $muted;
  border-radius: $radius-md;
}

.nut-icon {
  font-size: 32rpx;
}

.nut-value {
  font-size: 28rpx;
  font-weight: 600;
  color: $foreground;
}

.nut-label {
  font-size: 20rpx;
  color: $muted-foreground;
}

/* ===== Ingredients ===== */
.ingredient-group {
  margin-bottom: 20rpx;
}

.ingredient-group:last-child {
  margin-bottom: 0;
}

.group-label {
  font-size: 26rpx;
  font-weight: 600;
  color: $accent;
  display: block;
  margin-bottom: 12rpx;
  padding-left: 12rpx;
  border-left: 6rpx solid $accent;
}

.ingredient-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 14rpx 0;
  border-bottom: 1rpx solid $border;
}

.ingredient-row:last-child {
  border-bottom: none;
}

.ing-name {
  font-size: 26rpx;
  color: $foreground;
}

.ing-amount {
  font-size: 26rpx;
  color: $muted-foreground;
}

/* ===== Steps ===== */
.step-item {
  margin-bottom: 28rpx;
}

.step-item:last-child {
  margin-bottom: 0;
}

.step-header {
  display: flex;
  gap: 16rpx;
}

.step-number {
  flex-shrink: 0;
  width: 48rpx;
  height: 48rpx;
  border-radius: 50%;
  background: $gradient-accent;
  color: #fff;
  font-size: 24rpx;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
}

.step-desc {
  flex: 1;
  font-size: 26rpx;
  color: $foreground;
  line-height: 1.7;
  padding-top: 6rpx;
}

.step-image {
  width: 100%;
  height: 300rpx;
  border-radius: $radius-md;
  margin-top: 16rpx;
}

.step-tip {
  display: flex;
  align-items: flex-start;
  gap: 8rpx;
  margin-top: 12rpx;
  padding: 14rpx 16rpx;
  background: #FFFBEB;
  border-radius: $radius-sm;
}

.tip-icon {
  font-size: 24rpx;
  flex-shrink: 0;
}

.tip-text {
  font-size: 24rpx;
  color: #92400E;
  line-height: 1.5;
}

/* ===== Tags ===== */
.tag-row {
  display: flex;
  flex-wrap: wrap;
  gap: 12rpx;
}

.tag-chip {
  padding: 8rpx 20rpx;
  border-radius: $radius-full;
  background: rgba(16, 185, 129, 0.08);
}

.tag-text {
  font-size: 22rpx;
  color: $accent;
}

/* ===== Bottom Spacer & Action Bar ===== */
.bottom-spacer {
  height: 140rpx;
}

.action-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  display: flex;
  align-items: center;
  justify-content: space-around;
  height: 110rpx;
  padding-bottom: env(safe-area-inset-bottom);
  background: $card;
  border-top: 1rpx solid $border;
  box-shadow: 0 -2rpx 10rpx rgba(0, 0, 0, 0.05);
  z-index: 100;
}

.action-btn {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4rpx;
  padding: 0;
  margin: 0;
  background: none;
  border: none;
  line-height: 1.4;
}

.action-btn::after {
  display: none;
}

.action-icon {
  font-size: 36rpx;
}

.action-label {
  font-size: 22rpx;
  color: $muted-foreground;
}

.fav-btn.favorited .action-label {
  color: #EF4444;
}

/* ===== Rating Popup ===== */
.popup-mask {
  position: fixed;
  top: 0; left: 0; right: 0; bottom: 0;
  background: rgba(15, 23, 42, 0.5);
  z-index: 200;
  display: flex;
  align-items: flex-end;
}

.popup-content {
  width: 100%;
  background: $card;
  border-radius: $radius-xl $radius-xl 0 0;
  padding: 36rpx 32rpx;
  padding-bottom: calc(36rpx + env(safe-area-inset-bottom));
}

.popup-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 32rpx;
}

.popup-title {
  font-size: 32rpx;
  font-weight: 700;
  color: $foreground;
}

.popup-close {
  font-size: 32rpx;
  color: $muted-foreground;
  padding: 8rpx;
}

.rating-stars {
  display: flex;
  justify-content: center;
  gap: 24rpx;
  margin-bottom: 28rpx;
}

.star-icon {
  font-size: 56rpx;
  color: #CBD5E1;
  transition: color 0.15s;
}

.star-icon.filled {
  color: #F59E0B;
}

.rating-comment {
  width: 100%;
  height: 160rpx;
  padding: 20rpx;
  font-size: 26rpx;
  color: $foreground;
  background: $muted;
  border-radius: $radius-md;
  border: 1rpx solid $border;
  box-sizing: border-box;
  margin-bottom: 28rpx;
}

.popup-submit {
  width: 100%;
  height: 88rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: $radius-lg;
  background: $gradient-accent;
  transition: opacity 0.2s;
}

.popup-submit.disabled {
  opacity: 0.4;
}

.submit-text {
  font-size: 30rpx;
  font-weight: 600;
  color: #fff;
}
</style>
