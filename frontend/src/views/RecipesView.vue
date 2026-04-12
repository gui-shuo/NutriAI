<template>
  <div class="recipes-page">
    <!-- Top Nav -->
    <header class="top-nav">
      <div class="nav-left">
        <el-button text round @click="router.push('/')">
          <el-icon><ArrowLeft /></el-icon>
          返回首页
        </el-button>
      </div>
      <h1 class="page-title">🍳 食谱库</h1>
      <div class="nav-right">
        <el-button round type="primary" @click="openFavorites">
          <el-icon><StarFilled /></el-icon>
          我的收藏
        </el-button>
      </div>
    </header>

    <!-- Main Tabs: 精选食谱 / 食谱大全 -->
    <div class="main-tabs">
      <span
        class="main-tab"
        :class="{ active: activeMainTab === 'curated' }"
        @click="switchMainTab('curated')"
      >
        ⭐ 精选食谱
      </span>
      <span
        class="main-tab"
        :class="{ active: activeMainTab === 'corpus' }"
        @click="switchMainTab('corpus')"
      >
        📚 食谱大全
        <span v-if="corpusRealTotal > 0" class="corpus-count">{{ formatCount(corpusRealTotal) }}</span>
      </span>
    </div>

    <!-- ══════════ CURATED TAB ══════════ -->
    <template v-if="activeMainTab === 'curated'">

    <!-- Search Bar -->
    <div class="search-bar">
      <el-input
        v-model="keyword"
        placeholder="搜索食谱名称、食材、标签..."
        size="large"
        clearable
        @keyup.enter="doSearch"
        @clear="doSearch"
      >
        <template #prefix>
          <el-icon><Search /></el-icon>
        </template>
        <template #append>
          <el-button @click="doSearch">搜索</el-button>
        </template>
      </el-input>
    </div>

    <!-- Filter Bar -->
    <div class="filter-bar">
      <div class="category-tabs">
        <span
          v-for="cat in categoryOptions"
          :key="cat.value"
          class="category-tab"
          :class="{ active: filters.category === cat.value }"
          @click="setCategory(cat.value)"
        >
          {{ cat.label }}
        </span>
      </div>
      <div class="filter-dropdowns">
        <el-select
          v-model="filters.dietType"
          placeholder="饮食类型"
          clearable
          size="default"
          @change="resetAndLoad"
        >
          <el-option
            v-for="(label, key) in dietTypeMap"
            :key="key"
            :label="label"
            :value="key"
          />
        </el-select>
        <el-select
          v-model="filters.cuisineType"
          placeholder="菜系"
          clearable
          size="default"
          @change="resetAndLoad"
        >
          <el-option
            v-for="(label, key) in cuisineTypeMap"
            :key="key"
            :label="label"
            :value="key"
          />
        </el-select>
        <el-select
          v-model="filters.difficulty"
          placeholder="难度"
          clearable
          size="default"
          @change="resetAndLoad"
        >
          <el-option
            v-for="(label, key) in difficultyMap"
            :key="key"
            :label="label"
            :value="key"
          />
        </el-select>
        <el-select
          v-model="filters.sort"
          placeholder="排序"
          size="default"
          @change="resetAndLoad"
        >
          <el-option
            v-for="opt in sortOptions"
            :key="opt.value"
            :label="opt.label"
            :value="opt.value"
          />
        </el-select>
      </div>
    </div>

    <!-- Tags Bar -->
    <div v-if="tags.length" class="tags-bar">
      <span
        v-for="tag in tags"
        :key="tag"
        class="tag-chip"
        :class="{ active: filters.tag === tag }"
        @click="toggleTag(tag)"
      >
        {{ tag }}
      </span>
    </div>

    <!-- Featured Section -->
    <section v-if="featuredRecipes.length && !hasActiveFilters" class="featured-section">
      <h2 class="section-title">⭐ 精选推荐</h2>
      <div class="featured-grid">
        <div
          v-for="recipe in featuredRecipes"
          :key="'feat-' + recipe.id"
          class="featured-card"
          @click="openDetail(recipe.id)"
        >
          <div class="featured-image">
            <img
              v-if="recipe.coverImage"
              :src="recipe.coverImage"
              :alt="recipe.title"
              loading="lazy"
            />
            <div v-else class="image-placeholder">🍽️</div>
            <div class="featured-badge">精选</div>
          </div>
          <div class="featured-info">
            <h3>{{ recipe.title }}</h3>
            <p class="featured-desc">{{ recipe.description }}</p>
          </div>
        </div>
      </div>
    </section>

    <!-- Recipe Grid -->
    <section class="recipes-section">
      <div v-if="loading" class="loading-container">
        <el-skeleton :rows="3" animated />
        <el-skeleton :rows="3" animated />
      </div>
      <div v-else-if="recipes.length === 0" class="empty-container">
        <el-empty description="没有找到相关食谱，试试其他筛选条件吧" />
      </div>
      <div v-else class="recipe-grid">
        <div
          v-for="recipe in recipes"
          :key="recipe.id"
          class="recipe-card"
          @click="openDetail(recipe.id)"
        >
          <div class="card-image">
            <img
              v-if="recipe.coverImage"
              :src="recipe.coverImage"
              :alt="recipe.title"
              loading="lazy"
            />
            <div v-else class="image-placeholder">🍽️</div>
            <span class="badge-category">{{ categoryMap[recipe.category] || recipe.category }}</span>
            <span
              class="badge-difficulty"
              :class="difficultyClass(recipe.difficulty)"
            >
              {{ difficultyMap[recipe.difficulty] || recipe.difficulty }}
            </span>
          </div>
          <div class="card-body">
            <h3 class="card-title">{{ recipe.title }}</h3>
            <div class="card-meta">
              <span>⏱ {{ (recipe.prepTime || 0) + (recipe.cookingTime || 0) }} 分钟</span>
              <span>🔥 {{ recipe.caloriesPerServing || '-' }} kcal</span>
              <span>⭐ {{ recipe.ratingAvg ? recipe.ratingAvg.toFixed(1) : '-' }}</span>
            </div>
            <div v-if="recipe.tags && recipe.tags.length" class="card-tags">
              <span
                v-for="t in recipe.tags.slice(0, 3)"
                :key="t.tagName || t"
                class="mini-tag"
              >
                {{ t.tagName || t }}
              </span>
            </div>
            <div class="card-footer">
              <span class="stat">
                <el-icon><Star /></el-icon>
                {{ recipe.favoriteCount || 0 }}
              </span>
              <span class="stat">
                <el-icon><View /></el-icon>
                {{ recipe.viewCount || 0 }}
              </span>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- Pagination -->
    <div v-if="totalRecipes > 0" class="pagination-wrap">
      <el-pagination
        v-model:current-page="currentPage"
        :page-size="pageSize"
        :total="totalRecipes"
        layout="prev, pager, next, jumper"
        background
        @current-change="loadRecipes"
      />
    </div>

    </template>

    <!-- ══════════ CORPUS TAB (食谱大全) ══════════ -->
    <template v-if="activeMainTab === 'corpus'">
      <!-- Corpus Search -->
      <div class="search-bar">
        <el-input
          v-model="corpusKeyword"
          placeholder="搜索155万+食谱（输入菜名、食材...）"
          size="large"
          clearable
          @keyup.enter="doCorpusSearch"
          @clear="doCorpusSearch"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
          <template #append>
            <el-button @click="doCorpusSearch">搜索</el-button>
          </template>
        </el-input>
      </div>

      <!-- Corpus Categories -->
      <div class="filter-bar">
        <div class="category-tabs">
          <span
            v-for="cat in corpusCategoryOptions"
            :key="cat.value"
            class="category-tab"
            :class="{ active: corpusCategory === cat.value }"
            @click="setCorpusCategory(cat.value)"
          >
            {{ cat.label }}
          </span>
        </div>
      </div>

      <!-- Corpus Grid -->
      <section class="recipes-section">
        <div v-if="corpusLoading" class="loading-container">
          <el-skeleton :rows="3" animated />
          <el-skeleton :rows="3" animated />
        </div>
        <div v-else-if="corpusRecipes.length === 0" class="empty-container">
          <el-empty description="没有找到相关食谱，试试其他关键词" />
        </div>
        <div v-else class="recipe-grid">
          <div
            v-for="recipe in corpusRecipes"
            :key="'corpus-' + recipe.id"
            class="recipe-card corpus-card"
            @click="openCorpusDetail(recipe)"
          >
            <div class="card-image">
              <div class="image-placeholder corpus-placeholder">
                <span class="corpus-emoji">{{ getCategoryEmoji(recipe.category) }}</span>
              </div>
              <span class="badge-category">{{ corpusCategoryMap[recipe.category] || '其他' }}</span>
            </div>
            <div class="card-body">
              <h3 class="card-title">{{ recipe.name }}</h3>
              <p class="card-desc">{{ truncate(recipe.description, 60) }}</p>
              <div class="card-meta">
                <span v-if="recipe.author" class="meta-item">👨‍🍳 {{ recipe.author }}</span>
              </div>
            </div>
          </div>
        </div>
      </section>

      <!-- Corpus Pagination -->
      <div v-if="corpusTotal > 0" class="pagination-wrap">
        <el-pagination
          v-model:current-page="corpusPage"
          :page-size="corpusPageSize"
          :total="Math.min(corpusTotal, 10000)"
          layout="prev, pager, next, jumper"
          background
          @current-change="loadCorpus"
        />
        <p class="corpus-total-info">共 {{ formatCount(corpusRealTotal) }} 道食谱</p>
      </div>
    </template>

    <!-- Corpus Detail Dialog -->
    <el-dialog
      v-model="corpusDetailVisible"
      :title="corpusDetailData?.name || '食谱详情'"
      width="600px"
      top="5vh"
      class="corpus-detail-dialog"
    >
      <div v-if="corpusDetailData" class="corpus-detail-content">
        <div v-if="corpusDetailData.description" class="corpus-desc">
          <p>{{ corpusDetailData.description }}</p>
        </div>

        <div class="corpus-section">
          <h4>🥗 食材</h4>
          <div class="ingredient-tags">
            <el-tag
              v-for="(ing, idx) in parseCorpusJson(corpusDetailData.ingredientsJson)"
              :key="idx"
              size="default"
              type="success"
              class="ingredient-tag"
            >
              {{ ing }}
            </el-tag>
          </div>
        </div>

        <div class="corpus-section">
          <h4>📝 步骤</h4>
          <ol class="corpus-steps">
            <li
              v-for="(step, idx) in parseCorpusJson(corpusDetailData.stepsJson)"
              :key="idx"
            >
              {{ step }}
            </li>
          </ol>
        </div>

        <div v-if="corpusDetailData.keywordsJson" class="corpus-section">
          <h4>🏷️ 标签</h4>
          <div class="keyword-tags">
            <el-tag
              v-for="(kw, idx) in parseCorpusJson(corpusDetailData.keywordsJson)"
              :key="idx"
              size="small"
              type="info"
            >
              {{ kw }}
            </el-tag>
          </div>
        </div>
      </div>
    </el-dialog>

    <!-- Recipe Detail Drawer -->
    <el-drawer
      v-model="detailVisible"
      direction="rtl"
      size="560px"
      :show-close="true"
      :destroy-on-close="true"
      class="detail-drawer"
    >
      <template #header>
        <span class="drawer-title">食谱详情</span>
      </template>
      <div v-if="detailLoading" class="drawer-loading">
        <el-skeleton :rows="8" animated />
      </div>
      <div v-else-if="detailRecipe" class="detail-content">
        <!-- Cover -->
        <div class="detail-cover">
          <img
            v-if="detailRecipe.coverImage"
            :src="detailRecipe.coverImage"
            :alt="detailRecipe.title"
          />
          <div v-else class="image-placeholder large">🍽️</div>
        </div>

        <!-- Title & Description -->
        <h2 class="detail-title">{{ detailRecipe.title }}</h2>
        <p v-if="detailRecipe.description" class="detail-desc">{{ detailRecipe.description }}</p>

        <!-- Info Badges -->
        <div class="info-badges">
          <span class="info-badge" :class="difficultyClass(detailRecipe.difficulty)">
            {{ difficultyMap[detailRecipe.difficulty] || detailRecipe.difficulty }}
          </span>
          <span class="info-badge neutral">⏱ 准备 {{ detailRecipe.prepTime || 0 }} 分钟</span>
          <span class="info-badge neutral">🍳 烹饪 {{ detailRecipe.cookingTime || 0 }} 分钟</span>
          <span class="info-badge neutral">🍽 {{ detailRecipe.servings || 1 }} 人份</span>
          <span class="info-badge neutral">
            {{ cuisineTypeMap[detailRecipe.cuisineType] || detailRecipe.cuisineType }}
          </span>
        </div>

        <!-- Nutrition Grid -->
        <div class="nutrition-section">
          <h3 class="section-label">营养信息（每份）</h3>
          <div class="nutrition-grid">
            <div class="nutrition-item">
              <span class="nutrition-value">{{ detailRecipe.caloriesPerServing || '-' }}</span>
              <span class="nutrition-label">卡路里 (kcal)</span>
            </div>
            <div class="nutrition-item">
              <span class="nutrition-value">{{ detailRecipe.proteinPerServing || '-' }}</span>
              <span class="nutrition-label">蛋白质 (g)</span>
            </div>
            <div class="nutrition-item">
              <span class="nutrition-value">{{ detailRecipe.fatPerServing || '-' }}</span>
              <span class="nutrition-label">脂肪 (g)</span>
            </div>
            <div class="nutrition-item">
              <span class="nutrition-value">{{ detailRecipe.carbsPerServing || '-' }}</span>
              <span class="nutrition-label">碳水 (g)</span>
            </div>
            <div class="nutrition-item">
              <span class="nutrition-value">{{ detailRecipe.fiberPerServing || '-' }}</span>
              <span class="nutrition-label">膳食纤维 (g)</span>
            </div>
          </div>
        </div>

        <!-- Ingredients -->
        <div v-if="detailRecipe.ingredients && detailRecipe.ingredients.length" class="ingredients-section">
          <h3 class="section-label">食材清单</h3>
          <div v-if="mainIngredients.length" class="ingredient-group">
            <h4 class="group-label">主料</h4>
            <div
              v-for="ing in mainIngredients"
              :key="ing.name + ing.sortOrder"
              class="ingredient-row"
            >
              <span class="ing-name">{{ ing.name }}</span>
              <span class="ing-amount">{{ ing.amount }}{{ ing.unit }}</span>
            </div>
          </div>
          <div v-if="subIngredients.length" class="ingredient-group">
            <h4 class="group-label">辅料</h4>
            <div
              v-for="ing in subIngredients"
              :key="ing.name + ing.sortOrder"
              class="ingredient-row"
            >
              <span class="ing-name">{{ ing.name }}</span>
              <span class="ing-amount">{{ ing.amount }}{{ ing.unit }}</span>
            </div>
          </div>
        </div>

        <!-- Steps -->
        <div v-if="detailRecipe.steps && detailRecipe.steps.length" class="steps-section">
          <h3 class="section-label">烹饪步骤</h3>
          <div
            v-for="step in sortedSteps"
            :key="step.stepNumber"
            class="step-item"
          >
            <div class="step-number">{{ step.stepNumber }}</div>
            <div class="step-body">
              <p class="step-text">{{ step.description }}</p>
              <img
                v-if="step.imageUrl"
                :src="step.imageUrl"
                class="step-image"
                loading="lazy"
              />
              <p v-if="step.tips" class="step-tips">💡 {{ step.tips }}</p>
            </div>
          </div>
        </div>

        <!-- Rating Section -->
        <div class="rating-section">
          <h3 class="section-label">评分与评价</h3>
          <div class="rating-overview">
            <div class="rating-avg">
              <span class="avg-number">{{ detailRecipe.ratingAvg ? detailRecipe.ratingAvg.toFixed(1) : '-' }}</span>
              <el-rate
                :model-value="detailRecipe.ratingAvg || 0"
                disabled
                show-score
                score-template=""
              />
              <span class="rating-count">{{ detailRecipe.ratingCount || 0 }} 人评价</span>
            </div>
          </div>
          <div v-if="authStore.isLoggedIn" class="user-rating">
            <p class="rating-prompt">你的评分：</p>
            <el-rate
              v-model="userRating.score"
              :colors="['#F59E0B', '#F59E0B', '#10B981']"
              allow-half
              show-text
              :texts="['很差', '较差', '一般', '不错', '很棒']"
            />
            <el-input
              v-model="userRating.comment"
              type="textarea"
              :rows="2"
              placeholder="分享你的烹饪体验..."
              maxlength="200"
              show-word-limit
              class="rating-comment"
            />
            <el-button
              type="primary"
              round
              :loading="ratingSubmitting"
              :disabled="!userRating.score"
              @click="submitRating"
            >
              提交评价
            </el-button>
          </div>
        </div>

        <!-- Favorite Button -->
        <div class="detail-actions">
          <el-button
            :type="isFavorited ? 'warning' : 'default'"
            size="large"
            round
            :loading="favoriting"
            @click="toggleFavorite"
          >
            <el-icon>
              <StarFilled v-if="isFavorited" />
              <Star v-else />
            </el-icon>
            {{ isFavorited ? '已收藏' : '收藏食谱' }}
          </el-button>
        </div>
      </div>
    </el-drawer>

    <!-- My Favorites Drawer -->
    <el-drawer
      v-model="favoritesVisible"
      direction="rtl"
      size="480px"
      :show-close="true"
      :destroy-on-close="true"
    >
      <template #header>
        <span class="drawer-title">我的收藏</span>
      </template>
      <div v-if="favLoading" class="drawer-loading">
        <el-skeleton :rows="6" animated />
      </div>
      <div v-else-if="favoriteRecipes.length === 0" class="drawer-empty">
        <el-empty description="还没有收藏任何食谱" />
      </div>
      <div v-else class="favorites-list">
        <div
          v-for="recipe in favoriteRecipes"
          :key="'fav-' + recipe.id"
          class="fav-item"
          @click="openDetail(recipe.id); favoritesVisible = false"
        >
          <div class="fav-image">
            <img
              v-if="recipe.coverImage"
              :src="recipe.coverImage"
              :alt="recipe.title"
              loading="lazy"
            />
            <div v-else class="image-placeholder small">🍽️</div>
          </div>
          <div class="fav-info">
            <h4>{{ recipe.title }}</h4>
            <p>
              {{ categoryMap[recipe.category] || '' }}
              · {{ difficultyMap[recipe.difficulty] || '' }}
              · {{ (recipe.prepTime || 0) + (recipe.cookingTime || 0) }} 分钟
            </p>
            <span class="fav-cal">🔥 {{ recipe.caloriesPerServing || '-' }} kcal</span>
          </div>
        </div>
        <div v-if="favTotal > favPageSize" class="fav-pagination">
          <el-pagination
            v-model:current-page="favPage"
            :page-size="favPageSize"
            :total="favTotal"
            layout="prev, pager, next"
            small
            background
            @current-change="loadFavorites"
          />
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import message from '@/utils/message'
import api from '@/services/api'
import {
  ArrowLeft,
  Search,
  Star,
  StarFilled,
  View
} from '@element-plus/icons-vue'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

// ─── Maps & Constants ────────────────────────────────────────
const categoryMap = {
  BREAKFAST: '早餐',
  LUNCH: '午餐',
  DINNER: '晚餐',
  SNACK: '小食',
  DESSERT: '甜品',
  SOUP: '汤品'
}

const cuisineTypeMap = {
  CHINESE: '中式',
  WESTERN: '西式',
  JAPANESE: '日式',
  KOREAN: '韩式',
  SOUTHEAST_ASIAN: '东南亚',
  OTHER: '其他'
}

const dietTypeMap = {
  NORMAL: '普通',
  LOW_FAT: '低脂',
  HIGH_PROTEIN: '高蛋白',
  VEGETARIAN: '素食',
  VEGAN: '纯素',
  KETO: '生酮',
  MEDITERRANEAN: '地中海'
}

const difficultyMap = {
  EASY: '简单',
  MEDIUM: '中等',
  HARD: '困难'
}

const categoryOptions = [
  { label: '全部', value: '' },
  ...Object.entries(categoryMap).map(([value, label]) => ({ label, value }))
]

const sortOptions = [
  { label: '最新', value: 'latest' },
  { label: '最热', value: 'popular' },
  { label: '好评', value: 'rating' },
  { label: '低卡', value: 'calories_asc' },
  { label: '快手', value: 'time_asc' }
]

// ─── Reactive State ──────────────────────────────────────────
const keyword = ref('')
const filters = reactive({
  category: '',
  dietType: '',
  cuisineType: '',
  difficulty: '',
  sort: 'latest',
  tag: ''
})

const recipes = ref([])
const loading = ref(false)
const currentPage = ref(1)
const pageSize = 12
const totalRecipes = ref(0)

const tags = ref([])
const featuredRecipes = ref([])

// Detail drawer
const detailVisible = ref(false)
const detailLoading = ref(false)
const detailRecipe = ref(null)
const isFavorited = ref(false)
const favoriting = ref(false)

// Rating
const userRating = reactive({ score: 0, comment: '' })
const ratingSubmitting = ref(false)

// Favorites drawer
const favoritesVisible = ref(false)
const favoriteRecipes = ref([])
const favLoading = ref(false)
const favPage = ref(1)
const favPageSize = 12
const favTotal = ref(0)

// ─── Main Tab ────────────────────────────────────────────────
const activeMainTab = ref('curated')

// ─── Corpus State ────────────────────────────────────────────
const corpusCategoryMap = {
  BREAKFAST: '早餐',
  LUNCH: '午餐',
  DINNER: '晚餐',
  SOUP: '汤品',
  DESSERT: '甜品',
  SNACK: '小食',
  STAPLE: '主食',
  OTHER: '其他'
}
const corpusCategoryOptions = [
  { label: '全部', value: '' },
  ...Object.entries(corpusCategoryMap).map(([value, label]) => ({ label, value }))
]
const corpusKeyword = ref('')
const corpusCategory = ref('')
const corpusRecipes = ref([])
const corpusLoading = ref(false)
const corpusPage = ref(1)
const corpusPageSize = 20
const corpusTotal = ref(0)
const corpusRealTotal = ref(0)
const corpusDetailVisible = ref(false)
const corpusDetailData = ref(null)

// ─── Computed ────────────────────────────────────────────────
const hasActiveFilters = computed(() => {
  return !!(
    keyword.value ||
    filters.category ||
    filters.dietType ||
    filters.cuisineType ||
    filters.difficulty ||
    filters.tag
  )
})

const mainIngredients = computed(() => {
  if (!detailRecipe.value?.ingredients) return []
  return detailRecipe.value.ingredients
    .filter(i => i.isMain)
    .sort((a, b) => (a.sortOrder || 0) - (b.sortOrder || 0))
})

const subIngredients = computed(() => {
  if (!detailRecipe.value?.ingredients) return []
  return detailRecipe.value.ingredients
    .filter(i => !i.isMain)
    .sort((a, b) => (a.sortOrder || 0) - (b.sortOrder || 0))
})

const sortedSteps = computed(() => {
  if (!detailRecipe.value?.steps) return []
  return [...detailRecipe.value.steps].sort(
    (a, b) => (a.stepNumber || 0) - (b.stepNumber || 0)
  )
})

// ─── Methods ─────────────────────────────────────────────────
function difficultyClass(difficulty) {
  return {
    EASY: 'diff-easy',
    MEDIUM: 'diff-medium',
    HARD: 'diff-hard'
  }[difficulty] || ''
}

function setCategory(val) {
  filters.category = val
  resetAndLoad()
}

function toggleTag(tag) {
  filters.tag = filters.tag === tag ? '' : tag
  resetAndLoad()
}

function doSearch() {
  currentPage.value = 1
  loadRecipes()
}

function resetAndLoad() {
  currentPage.value = 1
  loadRecipes()
}

async function loadRecipes() {
  loading.value = true
  try {
    const params = {
      page: currentPage.value,
      size: pageSize,
      sort: filters.sort
    }
    if (keyword.value) params.keyword = keyword.value
    if (filters.category) params.category = filters.category
    if (filters.dietType) params.dietType = filters.dietType
    if (filters.cuisineType) params.cuisineType = filters.cuisineType
    if (filters.difficulty) params.difficulty = filters.difficulty
    if (filters.tag) params.tag = filters.tag

    const res = await api.get('/recipes', { params })
    if (res.data.code === 200) {
      const page = res.data.data
      recipes.value = page.content || []
      totalRecipes.value = page.totalElements || 0
    } else {
      recipes.value = []
      totalRecipes.value = 0
    }
  } catch (e) {
    console.error('加载食谱失败', e)
    message.error('加载食谱失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

async function loadFeatured() {
  try {
    const res = await api.get('/recipes/featured')
    if (res.data.code === 200) {
      featuredRecipes.value = res.data.data || []
    }
  } catch {
    // silent
  }
}

async function loadTags() {
  try {
    const res = await api.get('/recipes/tags')
    if (res.data.code === 200) {
      tags.value = res.data.data || []
    }
  } catch {
    // silent
  }
}

async function openDetail(id) {
  detailVisible.value = true
  detailLoading.value = true
  detailRecipe.value = null
  isFavorited.value = false
  userRating.score = 0
  userRating.comment = ''

  try {
    const res = await api.get(`/recipes/${id}`)
    if (res.data.code === 200) {
      const data = res.data.data
      detailRecipe.value = data.recipe || data
      isFavorited.value = data.isFavorited || false
    } else {
      message.error('食谱加载失败')
      detailVisible.value = false
    }
  } catch (e) {
    console.error('加载食谱详情失败', e)
    message.error('加载食谱详情失败')
    detailVisible.value = false
  } finally {
    detailLoading.value = false
  }
}

async function toggleFavorite() {
  if (!authStore.isLoggedIn) {
    message.warning('请先登录')
    return
  }
  if (!detailRecipe.value) return
  favoriting.value = true
  try {
    const res = await api.post(`/recipes/${detailRecipe.value.id}/favorite`)
    if (res.data.code === 200) {
      const data = res.data.data
      isFavorited.value = typeof data === 'object' ? data.favorited : !!data
      message.success(isFavorited.value ? '已收藏' : '已取消收藏')
      // Update counts in list
      const found = recipes.value.find(r => r.id === detailRecipe.value.id)
      if (found) {
        found.favoriteCount = (found.favoriteCount || 0) + (isFavorited.value ? 1 : -1)
      }
    }
  } catch (e) {
    console.error('收藏操作失败', e)
    message.error('操作失败，请稍后重试')
  } finally {
    favoriting.value = false
  }
}

async function submitRating() {
  if (!authStore.isLoggedIn) {
    message.warning('请先登录')
    return
  }
  if (!detailRecipe.value || !userRating.score) return
  ratingSubmitting.value = true
  try {
    const res = await api.post(`/recipes/${detailRecipe.value.id}/rate`, {
      score: userRating.score,
      comment: userRating.comment
    })
    if (res.data.code === 200) {
      message.success('评价提交成功')
      // Refresh detail to get updated rating
      const refreshRes = await api.get(`/recipes/${detailRecipe.value.id}`)
      if (refreshRes.data.code === 200) {
        const data = refreshRes.data.data
        detailRecipe.value = data.recipe || data
      }
    } else {
      message.error(res.data.message || '评价失败')
    }
  } catch (e) {
    console.error('提交评价失败', e)
    message.error('提交评价失败，请稍后重试')
  } finally {
    ratingSubmitting.value = false
  }
}

function openFavorites() {
  if (!authStore.isLoggedIn) {
    message.warning('请先登录')
    return
  }
  favoritesVisible.value = true
  favPage.value = 1
  loadFavorites()
}

async function loadFavorites() {
  favLoading.value = true
  try {
    const res = await api.get('/recipes/my-favorites', {
      params: { page: favPage.value, size: favPageSize }
    })
    if (res.data.code === 200) {
      const page = res.data.data
      favoriteRecipes.value = page.content || page || []
      favTotal.value = page.totalElements || 0
    }
  } catch (e) {
    console.error('加载收藏失败', e)
    message.error('加载收藏列表失败')
  } finally {
    favLoading.value = false
  }
}

// ─── Corpus Methods ──────────────────────────────────────────
function switchMainTab(tab) {
  activeMainTab.value = tab
  if (tab === 'corpus' && corpusRecipes.value.length === 0) {
    loadCorpus()
    loadCorpusCategories()
  }
}

function formatCount(n) {
  if (n >= 10000) return (n / 10000).toFixed(1) + '万'
  if (n >= 1000) return (n / 1000).toFixed(1) + 'k'
  return String(n)
}

function getCategoryEmoji(cat) {
  const map = {
    BREAKFAST: '🌅', LUNCH: '☀️', DINNER: '🌙',
    SOUP: '🍲', DESSERT: '🍰', SNACK: '🍡',
    STAPLE: '🍚', OTHER: '🍽️'
  }
  return map[cat] || '🍽️'
}

function truncate(str, len) {
  if (!str) return ''
  return str.length > len ? str.substring(0, len) + '...' : str
}

function parseCorpusJson(jsonStr) {
  if (!jsonStr) return []
  try { return JSON.parse(jsonStr) } catch { return [jsonStr] }
}

function doCorpusSearch() {
  corpusPage.value = 1
  loadCorpus()
}

function setCorpusCategory(val) {
  corpusCategory.value = val
  corpusPage.value = 1
  loadCorpus()
}

async function loadCorpus() {
  corpusLoading.value = true
  try {
    const params = { page: corpusPage.value, size: corpusPageSize }
    if (corpusKeyword.value) params.keyword = corpusKeyword.value
    if (corpusCategory.value) params.category = corpusCategory.value
    const res = await api.get('/recipes/corpus', { params })
    if (res.data.code === 200) {
      const page = res.data.data
      corpusRecipes.value = page.content || []
      corpusTotal.value = page.totalElements || 0
    }
  } catch (e) {
    console.error('加载食谱大全失败', e)
    message.error('加载食谱大全失败')
  } finally {
    corpusLoading.value = false
  }
}

async function loadCorpusCategories() {
  try {
    const res = await api.get('/recipes/corpus/categories')
    if (res.data.code === 200) {
      corpusRealTotal.value = res.data.data.total || corpusRealTotal.value
    }
  } catch { /* silent */ }
}

function openCorpusDetail(recipe) {
  corpusDetailData.value = recipe
  corpusDetailVisible.value = true
}

// ─── Lifecycle ───────────────────────────────────────────────
onMounted(() => {
  loadRecipes()
  loadFeatured()
  loadTags()

  // 支持从营养餐跳转直接打开食谱详情
  const recipeId = route.query.recipeId
  if (recipeId) {
    openDetail(Number(recipeId))
  }
})
</script>

<style lang="scss" scoped>
@use '@/styles/variables.scss' as *;

.recipes-page {
  max-width: 1200px;
  margin: 0 auto;
  padding: $spacing-lg $spacing-md;
  background: $background;
  min-height: 100vh;
  font-family: $font-body;
  color: $foreground;
}

/* ─── Top Nav ───────────────────────────────────── */
.top-nav {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: $spacing-lg;

  .page-title {
    font-family: $font-display;
    font-size: 24px;
    margin: 0;
    background: $gradient-accent;
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
    background-clip: text;
  }
}

/* ─── Search Bar ────────────────────────────────── */
.search-bar {
  margin-bottom: $spacing-md;

  :deep(.el-input__wrapper) {
    border-radius: $radius-lg;
    box-shadow: $shadow-sm;
  }

  :deep(.el-input-group__append) {
    background: $accent;
    color: #fff;
    border: none;
    border-radius: 0 $radius-lg $radius-lg 0;
    cursor: pointer;

    &:hover {
      background: $accent-secondary;
    }
  }
}

/* ─── Filter Bar ────────────────────────────────── */
.filter-bar {
  margin-bottom: $spacing-md;

  .category-tabs {
    display: flex;
    flex-wrap: wrap;
    gap: $spacing-sm;
    margin-bottom: $spacing-sm;
  }

  .category-tab {
    padding: 6px 16px;
    border-radius: $radius-full;
    font-size: 14px;
    cursor: pointer;
    transition: all 0.2s;
    border: 1px solid $border;
    background: $card;
    color: $text-secondary;

    &:hover {
      border-color: $accent;
      color: $accent;
    }

    &.active {
      background: $gradient-accent;
      color: #fff;
      border-color: transparent;
    }
  }

  .filter-dropdowns {
    display: flex;
    flex-wrap: wrap;
    gap: $spacing-sm;

    :deep(.el-select) {
      width: 120px;
    }

    :deep(.el-input__wrapper) {
      border-radius: $radius-lg;
    }
  }
}

/* ─── Tags Bar ──────────────────────────────────── */
.tags-bar {
  display: flex;
  flex-wrap: wrap;
  gap: $spacing-xs;
  margin-bottom: $spacing-lg;

  .tag-chip {
    padding: 4px 12px;
    border-radius: $radius-full;
    font-size: 12px;
    cursor: pointer;
    transition: all 0.2s;
    background: $muted;
    color: $text-secondary;

    &:hover {
      background: rgba(16, 185, 129, 0.1);
      color: $accent;
    }

    &.active {
      background: $accent;
      color: #fff;
    }
  }
}

/* ─── Featured Section ──────────────────────────── */
.featured-section {
  margin-bottom: $spacing-xl;

  .section-title {
    font-family: $font-display;
    font-size: 20px;
    margin: 0 0 $spacing-md 0;
    color: $foreground;
  }
}

.featured-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: $spacing-md;

  @media (max-width: $breakpoint-tablet) {
    grid-template-columns: repeat(2, 1fr);
  }

  @media (max-width: $breakpoint-mobile) {
    grid-template-columns: 1fr;
  }
}

.featured-card {
  border-radius: $radius-xl;
  overflow: hidden;
  background: $card;
  box-shadow: $shadow-sm;
  cursor: pointer;
  transition: all 0.3s;

  &:hover {
    transform: translateY(-4px);
    box-shadow: $shadow-xl;
  }

  .featured-image {
    position: relative;
    aspect-ratio: 16 / 9;
    overflow: hidden;

    img {
      width: 100%;
      height: 100%;
      object-fit: cover;
    }

    .featured-badge {
      position: absolute;
      top: 12px;
      left: 12px;
      padding: 4px 12px;
      background: $gradient-accent;
      color: #fff;
      font-size: 12px;
      font-weight: 600;
      border-radius: $radius-full;
    }
  }

  .featured-info {
    padding: $spacing-md;

    h3 {
      margin: 0 0 $spacing-xs 0;
      font-size: 16px;
      font-weight: 600;
      color: $foreground;
    }

    .featured-desc {
      margin: 0;
      font-size: 13px;
      color: $text-secondary;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }
  }
}

/* ─── Recipe Grid ───────────────────────────────── */
.loading-container,
.empty-container {
  padding: 60px 0;
  text-align: center;
}

.recipe-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;

  @media (max-width: 1100px) {
    grid-template-columns: repeat(3, 1fr);
  }

  @media (max-width: $breakpoint-mobile) {
    grid-template-columns: repeat(2, 1fr);
  }

  @media (max-width: 480px) {
    grid-template-columns: 1fr;
  }
}

.recipe-card {
  border-radius: $radius-xl;
  overflow: hidden;
  background: $card;
  box-shadow: $shadow-sm;
  cursor: pointer;
  transition: all 0.3s;

  &:hover {
    transform: translateY(-4px);
    box-shadow: 0 10px 15px rgba(0, 0, 0, 0.1);
  }

  .card-image {
    position: relative;
    aspect-ratio: 4 / 3;
    overflow: hidden;

    img {
      width: 100%;
      height: 100%;
      object-fit: cover;
      transition: transform 0.3s;
    }

    &:hover img {
      transform: scale(1.05);
    }

    .badge-category {
      position: absolute;
      top: 10px;
      left: 10px;
      padding: 3px 10px;
      background: rgba(0, 0, 0, 0.55);
      color: #fff;
      font-size: 11px;
      border-radius: $radius-full;
      backdrop-filter: blur(4px);
    }

    .badge-difficulty {
      position: absolute;
      top: 10px;
      right: 10px;
      padding: 3px 10px;
      font-size: 11px;
      font-weight: 600;
      border-radius: $radius-full;
      color: #fff;

      &.diff-easy {
        background: #10B981;
      }

      &.diff-medium {
        background: #F59E0B;
      }

      &.diff-hard {
        background: #EF4444;
      }
    }
  }

  .card-body {
    padding: 12px $spacing-md $spacing-md;
  }

  .card-title {
    margin: 0 0 $spacing-xs 0;
    font-size: 15px;
    font-weight: 600;
    color: $foreground;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .card-meta {
    display: flex;
    flex-wrap: wrap;
    gap: $spacing-sm;
    font-size: 12px;
    color: $text-secondary;
    margin-bottom: $spacing-xs;
  }

  .card-tags {
    display: flex;
    flex-wrap: wrap;
    gap: 4px;
    margin-bottom: $spacing-sm;

    .mini-tag {
      padding: 2px 8px;
      background: $muted;
      color: $text-secondary;
      font-size: 11px;
      border-radius: $radius-full;
    }
  }

  .card-footer {
    display: flex;
    gap: $spacing-md;
    font-size: 12px;
    color: $text-secondary;
    border-top: 1px solid $border;
    padding-top: $spacing-sm;

    .stat {
      display: flex;
      align-items: center;
      gap: 4px;
    }
  }
}

.image-placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
  background: $muted;
  font-size: 40px;

  &.large {
    height: 240px;
    font-size: 64px;
  }

  &.small {
    font-size: 24px;
  }
}

/* ─── Pagination ────────────────────────────────── */
.pagination-wrap {
  display: flex;
  justify-content: center;
  padding: $spacing-xl 0;
}

/* ─── Detail Drawer ─────────────────────────────── */
.drawer-title {
  font-family: $font-display;
  font-size: 18px;
  color: $foreground;
}

.drawer-loading {
  padding: $spacing-lg;
}

.detail-content {
  padding: 0 4px;
}

.detail-cover {
  border-radius: $radius-xl;
  overflow: hidden;
  margin-bottom: $spacing-md;

  img {
    width: 100%;
    display: block;
    max-height: 300px;
    object-fit: cover;
  }
}

.detail-title {
  font-family: $font-display;
  font-size: 22px;
  margin: 0 0 $spacing-sm 0;
  color: $foreground;
}

.detail-desc {
  font-size: 14px;
  color: $text-secondary;
  line-height: 1.6;
  margin: 0 0 $spacing-md 0;
}

.info-badges {
  display: flex;
  flex-wrap: wrap;
  gap: $spacing-sm;
  margin-bottom: $spacing-lg;

  .info-badge {
    padding: 4px 12px;
    border-radius: $radius-full;
    font-size: 13px;
    font-weight: 500;

    &.neutral {
      background: $muted;
      color: $text-secondary;
    }

    &.diff-easy {
      background: rgba(16, 185, 129, 0.12);
      color: #059669;
    }

    &.diff-medium {
      background: rgba(245, 158, 11, 0.12);
      color: #D97706;
    }

    &.diff-hard {
      background: rgba(239, 68, 68, 0.12);
      color: #DC2626;
    }
  }
}

/* ─── Nutrition ─────────────────────────────────── */
.section-label {
  font-size: 16px;
  font-weight: 600;
  color: $foreground;
  margin: 0 0 $spacing-md 0;
  padding-bottom: $spacing-sm;
  border-bottom: 2px solid $accent;
  display: inline-block;
}

.nutrition-section {
  margin-bottom: $spacing-lg;
}

.nutrition-grid {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: $spacing-sm;

  @media (max-width: 560px) {
    grid-template-columns: repeat(3, 1fr);
  }
}

.nutrition-item {
  text-align: center;
  padding: $spacing-md $spacing-sm;
  background: $muted;
  border-radius: $radius-lg;

  .nutrition-value {
    display: block;
    font-size: 20px;
    font-weight: 700;
    color: $accent;
    margin-bottom: 4px;
  }

  .nutrition-label {
    font-size: 11px;
    color: $text-secondary;
  }
}

/* ─── Ingredients ───────────────────────────────── */
.ingredients-section {
  margin-bottom: $spacing-lg;

  .ingredient-group {
    margin-bottom: $spacing-md;
  }

  .group-label {
    font-size: 14px;
    font-weight: 600;
    color: $accent;
    margin: 0 0 $spacing-sm 0;
  }

  .ingredient-row {
    display: flex;
    justify-content: space-between;
    padding: $spacing-sm $spacing-md;
    border-bottom: 1px dashed $border;

    &:last-child {
      border-bottom: none;
    }

    .ing-name {
      color: $foreground;
      font-size: 14px;
    }

    .ing-amount {
      color: $text-secondary;
      font-size: 14px;
    }
  }
}

/* ─── Steps ─────────────────────────────────────── */
.steps-section {
  margin-bottom: $spacing-lg;
}

.step-item {
  display: flex;
  gap: $spacing-md;
  margin-bottom: $spacing-md;

  .step-number {
    flex-shrink: 0;
    width: 32px;
    height: 32px;
    border-radius: 50%;
    background: $gradient-accent;
    color: #fff;
    display: flex;
    align-items: center;
    justify-content: center;
    font-weight: 700;
    font-size: 14px;
  }

  .step-body {
    flex: 1;

    .step-text {
      margin: 0 0 $spacing-sm 0;
      font-size: 14px;
      line-height: 1.6;
      color: $foreground;
    }

    .step-image {
      width: 100%;
      max-height: 200px;
      object-fit: cover;
      border-radius: $radius-lg;
      margin-bottom: $spacing-sm;
    }

    .step-tips {
      margin: 0;
      font-size: 13px;
      color: $warning-color;
      background: rgba(245, 158, 11, 0.08);
      padding: $spacing-sm $spacing-md;
      border-radius: $radius-md;
    }
  }
}

/* ─── Rating ────────────────────────────────────── */
.rating-section {
  margin-bottom: $spacing-lg;
}

.rating-overview {
  margin-bottom: $spacing-md;

  .rating-avg {
    display: flex;
    align-items: center;
    gap: $spacing-sm;

    .avg-number {
      font-size: 28px;
      font-weight: 700;
      color: $accent;
      font-family: $font-display;
    }

    .rating-count {
      font-size: 13px;
      color: $text-secondary;
    }
  }
}

.user-rating {
  .rating-prompt {
    font-size: 14px;
    color: $text-secondary;
    margin: 0 0 $spacing-sm 0;
  }

  .rating-comment {
    margin: $spacing-md 0;

    :deep(.el-textarea__inner) {
      border-radius: $radius-lg;
    }
  }
}

/* ─── Detail Actions ────────────────────────────── */
.detail-actions {
  position: sticky;
  bottom: 0;
  background: $card;
  padding: $spacing-md 0;
  border-top: 1px solid $border;
  text-align: center;
}

/* ─── Favorites Drawer ──────────────────────────── */
.drawer-empty {
  padding: 60px 0;
}

.favorites-list {
  .fav-item {
    display: flex;
    gap: $spacing-md;
    padding: $spacing-md;
    border-radius: $radius-xl;
    cursor: pointer;
    transition: all 0.2s;

    &:hover {
      background: $muted;
    }

    .fav-image {
      flex-shrink: 0;
      width: 80px;
      height: 60px;
      border-radius: $radius-md;
      overflow: hidden;

      img {
        width: 100%;
        height: 100%;
        object-fit: cover;
      }
    }

    .fav-info {
      flex: 1;
      min-width: 0;

      h4 {
        margin: 0 0 4px 0;
        font-size: 14px;
        font-weight: 600;
        color: $foreground;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
      }

      p {
        margin: 0 0 4px 0;
        font-size: 12px;
        color: $text-secondary;
      }

      .fav-cal {
        font-size: 12px;
        color: $accent;
        font-weight: 500;
      }
    }
  }

  .fav-pagination {
    padding: $spacing-md 0;
    display: flex;
    justify-content: center;
  }
}

/* ─── Global Drawer Overrides ───────────────────── */
:deep(.el-drawer__header) {
  margin-bottom: 0;
  padding: $spacing-md $spacing-lg;
  border-bottom: 1px solid $border;
}

:deep(.el-drawer__body) {
  padding: $spacing-lg;
}

:deep(.el-rate) {
  --el-rate-fill-color: #F59E0B;
}

/* ─── Main Tabs ─────────────────────────────────── */
.main-tabs {
  display: flex;
  gap: $spacing-sm;
  margin-bottom: $spacing-lg;
  border-bottom: 2px solid $border;
  padding-bottom: 0;
}

.main-tab {
  padding: $spacing-sm $spacing-lg;
  cursor: pointer;
  font-size: 16px;
  font-weight: 600;
  color: rgba($foreground, 0.6);
  border-bottom: 3px solid transparent;
  margin-bottom: -2px;
  transition: all 0.2s;
  display: flex;
  align-items: center;
  gap: 6px;

  &:hover { color: $foreground; }
  &.active {
    color: $accent;
    border-bottom-color: $accent;
  }

  .corpus-count {
    background: rgba($accent, 0.1);
    color: $accent;
    font-size: 11px;
    padding: 2px 6px;
    border-radius: 10px;
    font-weight: 500;
  }
}

/* ─── Corpus Styles ─────────────────────────────── */
.corpus-card {
  .corpus-placeholder {
    display: flex;
    align-items: center;
    justify-content: center;
    background: linear-gradient(135deg, rgba($accent, 0.08), rgba($accent, 0.03));
  }
  .corpus-emoji {
    font-size: 42px;
  }
}

.corpus-total-info {
  text-align: center;
  color: rgba($foreground, 0.5);
  font-size: 13px;
  margin-top: $spacing-sm;
}

.corpus-detail-content {
  .corpus-desc {
    color: rgba($foreground, 0.7);
    line-height: 1.8;
    margin-bottom: $spacing-lg;
    font-size: 14px;
  }

  .corpus-section {
    margin-bottom: $spacing-lg;

    h4 {
      font-size: 15px;
      font-weight: 600;
      margin-bottom: $spacing-sm;
      color: $foreground;
    }
  }

  .ingredient-tags {
    display: flex;
    flex-wrap: wrap;
    gap: 6px;
  }

  .ingredient-tag {
    font-size: 13px;
  }

  .corpus-steps {
    padding-left: 20px;
    li {
      line-height: 1.8;
      margin-bottom: 8px;
      color: rgba($foreground, 0.8);
      font-size: 14px;
    }
  }

  .keyword-tags {
    display: flex;
    flex-wrap: wrap;
    gap: 6px;
  }
}
</style>
