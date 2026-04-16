<template>
  <div class="meals-view">
    <!-- Hero Banner -->
    <div class="hero-banner">
      <div class="hero-content">
        <div class="hero-badge">🌿 科学抗炎 · 专业营养</div>
        <h1 class="hero-title">抗炎营养餐</h1>
        <p class="hero-subtitle">精选天然食材，科学搭配营养素，每一餐都为你的健康加分</p>
        <div class="hero-stats">
          <div class="stat-item"><span class="stat-num">{{ mealsCount }}</span><span class="stat-label">道餐品</span></div>
          <div class="stat-divider">|</div>
          <div class="stat-item"><span class="stat-num">100%</span><span class="stat-label">天然食材</span></div>
          <div class="stat-divider">|</div>
          <div class="stat-item"><span class="stat-num">专业</span><span class="stat-label">营养师配方</span></div>
        </div>
      </div>
    </div>

    <div class="page-body">
      <!-- Search & Filter Bar -->
      <div class="filter-bar">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索餐品名称..."
          clearable
          :prefix-icon="SearchIcon"
          class="search-input"
          @keyup.enter="handleSearch"
          @clear="handleSearch"
        />
        <div class="category-tabs">
          <span
            v-for="cat in categories"
            :key="cat.value"
            class="cat-tab"
            :class="{ active: selectedCategory === cat.value }"
            @click="selectCategory(cat.value)"
          >{{ cat.label }}</span>
        </div>
      </div>

      <!-- Meals Grid -->
      <div v-if="loading" class="loading-state">
        <el-skeleton :rows="3" animated />
      </div>

      <div v-else-if="meals.length === 0" class="empty-state">
        <img src="/icons/empty-meal.svg" alt="暂无餐品" class="empty-img" onerror="this.style.display='none'" />
        <p class="empty-text">暂无可订购的餐品，请稍后再来</p>
      </div>

      <div v-else class="meals-grid">
        <div
          v-for="meal in meals"
          :key="meal.id"
          class="meal-card"
          @click="openDetail(meal)"
        >
          <div class="meal-image-wrap">
            <img
              :src="meal.imageUrl || defaultMealImg"
              :alt="meal.name"
              class="meal-image"
              @error="(e) => (e.target as HTMLImageElement).src = defaultMealImg"
            />
            <div v-if="meal.isRecommended || meal.recommended" class="recommend-badge">推荐</div>
            <div v-if="meal.mealType" class="type-badge">{{ mealTypeLabel(meal.mealType) }}</div>
          </div>
          <div class="meal-info">
            <h3 class="meal-name">{{ meal.name }}</h3>
            <p class="meal-desc">{{ meal.description || meal.brief }}</p>
            <div class="meal-tags" v-if="meal.tags?.length">
              <span v-for="tag in (meal.tags || []).slice(0, 3)" :key="tag" class="meal-tag">{{ tag }}</span>
            </div>
            <div class="meal-nutrition" v-if="meal.nutritionInfo || meal.nutrition_info">
              <span class="nut-item">🔥 {{ (meal.nutritionInfo || meal.nutrition_info)?.calories || 0 }}kcal</span>
              <span class="nut-item">💪 蛋白质 {{ (meal.nutritionInfo || meal.nutrition_info)?.protein || 0 }}g</span>
            </div>
            <div class="meal-footer">
              <div class="price-wrap">
                <span class="price-symbol">¥</span>
                <span class="price-main">{{ formatPrice(meal.salePrice || meal.sale_price || meal.price) }}</span>
              </div>
              <el-button type="primary" size="small" class="order-btn" @click.stop="startOrder(meal)">
                立即订购
              </el-button>
            </div>
          </div>
        </div>
      </div>

      <!-- Pagination -->
      <div v-if="total > pageSize" class="pagination-wrap">
        <el-pagination
          v-model:current-page="currentPage"
          :page-size="pageSize"
          :total="total"
          layout="prev, pager, next"
          @current-change="loadMeals"
        />
      </div>
    </div>

    <!-- Meal Detail Dialog -->
    <el-dialog
      v-model="detailVisible"
      :title="currentMeal?.name || '餐品详情'"
      width="560px"
      class="detail-dialog"
    >
      <div v-if="currentMeal" class="detail-content">
        <img
          :src="currentMeal.imageUrl || defaultMealImg"
          :alt="currentMeal.name"
          class="detail-image"
          @error="(e) => (e.target as HTMLImageElement).src = defaultMealImg"
        />
        <div class="detail-info">
          <div class="detail-tags" v-if="currentMeal.tags?.length">
            <el-tag v-for="tag in currentMeal.tags" :key="tag" size="small" class="mr-1">{{ tag }}</el-tag>
          </div>
          <p class="detail-desc">{{ currentMeal.description || currentMeal.brief || '专业营养师精心配方，科学抗炎食材搭配。' }}</p>
          <div class="detail-nutrition" v-if="currentMeal.nutritionInfo || currentMeal.nutrition_info">
            <h4>营养成分</h4>
            <div class="nut-grid">
              <div class="nut-cell"><span class="nut-val">{{ (currentMeal.nutritionInfo || currentMeal.nutrition_info)?.calories || 0 }}</span><span class="nut-lbl">卡路里</span></div>
              <div class="nut-cell"><span class="nut-val">{{ (currentMeal.nutritionInfo || currentMeal.nutrition_info)?.protein || 0 }}g</span><span class="nut-lbl">蛋白质</span></div>
              <div class="nut-cell"><span class="nut-val">{{ (currentMeal.nutritionInfo || currentMeal.nutrition_info)?.carbs || 0 }}g</span><span class="nut-lbl">碳水</span></div>
              <div class="nut-cell"><span class="nut-val">{{ (currentMeal.nutritionInfo || currentMeal.nutrition_info)?.fat || 0 }}g</span><span class="nut-lbl">脂肪</span></div>
            </div>
          </div>
          <div class="detail-price-row">
            <span class="detail-price">¥{{ formatPrice(currentMeal.salePrice || currentMeal.sale_price || currentMeal.price) }}</span>
            <el-button type="primary" @click="startOrder(currentMeal)">立即订购</el-button>
          </div>
        </div>
      </div>
    </el-dialog>

    <!-- Order Dialog -->
    <el-dialog
      v-model="orderVisible"
      title="确认订单"
      width="480px"
      class="order-dialog"
    >
      <div v-if="orderMeal" class="order-form">
        <div class="order-meal-info">
          <img :src="orderMeal.imageUrl || defaultMealImg" class="order-meal-img" />
          <div>
            <div class="order-meal-name">{{ orderMeal.name }}</div>
            <div class="order-meal-price">¥{{ formatPrice(orderMeal.salePrice || orderMeal.sale_price || orderMeal.price) }}</div>
          </div>
        </div>
        <el-divider />
        <el-form :model="orderForm" label-width="80px">
          <el-form-item label="数量">
            <el-input-number v-model="orderForm.quantity" :min="1" :max="10" />
          </el-form-item>
          <el-form-item label="取餐方式">
            <el-radio-group v-model="orderForm.fulfillmentType">
              <el-radio value="PICKUP">🏪 到店自取</el-radio>
              <el-radio value="DELIVERY">🚴 配送上门</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item v-if="orderForm.fulfillmentType === 'DELIVERY'" label="收货地址">
            <el-input v-model="orderForm.deliveryAddress" placeholder="请输入收货地址" type="textarea" :rows="2" />
          </el-form-item>
          <el-form-item label="备注">
            <el-input v-model="orderForm.remark" placeholder="口味偏好、过敏提示等（选填）" type="textarea" :rows="2" />
          </el-form-item>
        </el-form>
        <div class="order-total">
          <span>合计：</span>
          <span class="total-price">¥{{ formatPrice((orderMeal.salePrice || orderMeal.sale_price || orderMeal.price) * orderForm.quantity) }}</span>
        </div>
      </div>
      <template #footer>
        <el-button @click="orderVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitOrder">确认下单</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search as SearchIcon } from '@element-plus/icons-vue'
import api from '@/services/api'
import { useRouter } from 'vue-router'

const router = useRouter()

const loading = ref(false)
const meals = ref<any[]>([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = 12
const searchKeyword = ref('')
const selectedCategory = ref('')

const detailVisible = ref(false)
const currentMeal = ref<any>(null)

const orderVisible = ref(false)
const orderMeal = ref<any>(null)
const submitting = ref(false)
const orderForm = ref({
  quantity: 1,
  fulfillmentType: 'PICKUP',
  deliveryAddress: '',
  remark: ''
})

const defaultMealImg = 'https://images.unsplash.com/photo-1512621776951-a57141f2eefd?w=400&q=80'

const categories = [
  { value: '', label: '全部' },
  { value: '抗炎套餐', label: '🌿 抗炎套餐' },
  { value: '高蛋白', label: '💪 高蛋白' },
  { value: '低脂', label: '🥗 低脂餐' },
  { value: '素食', label: '🌱 素食' },
  { value: '早餐', label: '🌅 早餐' },
]

const mealsCount = computed(() => total.value || meals.value.length)

function mealTypeLabel(type: string) {
  const map: Record<string, string> = {
    BREAKFAST: '早餐', LUNCH: '午餐', DINNER: '晚餐', SNACK: '加餐'
  }
  return map[type] || type
}

function formatPrice(price: any): string {
  const n = parseFloat(price) || 0
  return n.toFixed(2)
}

async function loadMeals(page = 1) {
  loading.value = true
  currentPage.value = page
  try {
    const params: any = { page: page - 1, size: pageSize }
    if (selectedCategory.value) params.category = selectedCategory.value
    if (searchKeyword.value) params.keyword = searchKeyword.value
    const res = await api.get('/meals', { params })
    const body = res.data
    if (body?.code === 200) {
      const pageData = body.data || {}
      meals.value = pageData.content || pageData.records || (Array.isArray(body.data) ? body.data : [])
      total.value = pageData.totalElements || pageData.total || meals.value.length
    }
  } catch (e: any) {
    ElMessage.error('加载餐品失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  currentPage.value = 1
  loadMeals(1)
}

function selectCategory(cat: string) {
  selectedCategory.value = cat
  loadMeals(1)
}

function openDetail(meal: any) {
  currentMeal.value = meal
  detailVisible.value = true
}

function startOrder(meal: any) {
  const token = localStorage.getItem('token')
  if (!token) {
    ElMessageBox.confirm('请先登录后再下单', '提示', {
      confirmButtonText: '去登录',
      cancelButtonText: '取消',
      type: 'warning'
    }).then(() => router.push('/login'))
    return
  }
  orderMeal.value = meal
  orderForm.value = { quantity: 1, fulfillmentType: 'PICKUP', deliveryAddress: '', remark: '' }
  orderVisible.value = true
  detailVisible.value = false
}

async function submitOrder() {
  if (orderForm.value.fulfillmentType === 'DELIVERY' && !orderForm.value.deliveryAddress.trim()) {
    ElMessage.warning('请填写收货地址')
    return
  }
  submitting.value = true
  try {
    const body = {
      items: [{ mealItemId: orderMeal.value.id, quantity: orderForm.value.quantity }],
      fulfillmentType: orderForm.value.fulfillmentType,
      deliveryAddress: orderForm.value.deliveryAddress || undefined,
      remark: orderForm.value.remark || undefined
    }
    const res = await api.post('/meals/orders', body)
    if (res.data?.code === 200) {
      ElMessage.success('下单成功！')
      orderVisible.value = false
      router.push('/profile?tab=meal-orders')
    } else {
      ElMessage.error(res.data?.message || '下单失败')
    }
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '下单失败，请稍后重试')
  } finally {
    submitting.value = false
  }
}

onMounted(() => loadMeals(1))
</script>

<style scoped>
.meals-view {
  min-height: 100vh;
  background: #f8faf8;
}

/* Hero */
.hero-banner {
  background: linear-gradient(135deg, #10b981 0%, #059669 60%, #047857 100%);
  padding: 48px 24px 40px;
  color: #fff;
  text-align: center;
}
.hero-badge {
  display: inline-block;
  background: rgba(255,255,255,0.2);
  border: 1px solid rgba(255,255,255,0.4);
  border-radius: 20px;
  padding: 4px 16px;
  font-size: 13px;
  margin-bottom: 12px;
}
.hero-title {
  font-size: 32px;
  font-weight: 700;
  margin: 0 0 8px;
  letter-spacing: 2px;
}
.hero-subtitle {
  font-size: 15px;
  opacity: 0.9;
  margin: 0 0 24px;
}
.hero-stats {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 20px;
}
.stat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
}
.stat-num {
  font-size: 22px;
  font-weight: 700;
}
.stat-label {
  font-size: 12px;
  opacity: 0.8;
}
.stat-divider {
  opacity: 0.4;
  font-size: 18px;
}

/* Body */
.page-body {
  max-width: 1200px;
  margin: 0 auto;
  padding: 24px 20px 40px;
}

/* Filter Bar */
.filter-bar {
  background: #fff;
  border-radius: 12px;
  padding: 16px 20px;
  margin-bottom: 24px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.06);
}
.search-input {
  margin-bottom: 12px;
}
.category-tabs {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}
.cat-tab {
  padding: 5px 14px;
  border-radius: 20px;
  font-size: 13px;
  cursor: pointer;
  border: 1px solid #e2e8f0;
  color: #64748b;
  transition: all 0.2s;
}
.cat-tab:hover { border-color: #10b981; color: #10b981; }
.cat-tab.active { background: #10b981; color: #fff; border-color: #10b981; }

/* Loading/Empty */
.loading-state { padding: 40px 0; }
.empty-state {
  text-align: center;
  padding: 80px 0;
  color: #94a3b8;
}
.empty-img { width: 120px; margin-bottom: 16px; }
.empty-text { font-size: 15px; }

/* Meals Grid */
.meals-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: 20px;
}

.meal-card {
  background: #fff;
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 2px 12px rgba(0,0,0,0.07);
  cursor: pointer;
  transition: transform 0.2s, box-shadow 0.2s;
}
.meal-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 24px rgba(16,185,129,0.15);
}
.meal-image-wrap {
  position: relative;
  height: 180px;
  overflow: hidden;
}
.meal-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.3s;
}
.meal-card:hover .meal-image { transform: scale(1.05); }
.recommend-badge {
  position: absolute;
  top: 10px;
  left: 10px;
  background: #f59e0b;
  color: #fff;
  padding: 2px 10px;
  border-radius: 12px;
  font-size: 11px;
  font-weight: 600;
}
.type-badge {
  position: absolute;
  top: 10px;
  right: 10px;
  background: rgba(0,0,0,0.5);
  color: #fff;
  padding: 2px 8px;
  border-radius: 8px;
  font-size: 11px;
}
.meal-info {
  padding: 14px 16px 16px;
}
.meal-name {
  font-size: 15px;
  font-weight: 600;
  color: #0f172a;
  margin: 0 0 6px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.meal-desc {
  font-size: 12px;
  color: #64748b;
  margin: 0 0 8px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  line-height: 1.5;
  min-height: 36px;
}
.meal-tags {
  display: flex;
  gap: 4px;
  flex-wrap: wrap;
  margin-bottom: 8px;
}
.meal-tag {
  background: #f0fdf4;
  color: #16a34a;
  border: 1px solid #bbf7d0;
  padding: 1px 8px;
  border-radius: 10px;
  font-size: 11px;
}
.meal-nutrition {
  display: flex;
  gap: 10px;
  margin-bottom: 10px;
}
.nut-item {
  font-size: 11px;
  color: #94a3b8;
}
.meal-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.price-wrap {
  display: flex;
  align-items: baseline;
  gap: 1px;
  color: #ef4444;
}
.price-symbol { font-size: 13px; font-weight: 500; }
.price-main { font-size: 22px; font-weight: 700; }
.order-btn {
  background: #10b981 !important;
  border-color: #10b981 !important;
  border-radius: 20px !important;
  padding: 6px 18px !important;
}

/* Pagination */
.pagination-wrap {
  display: flex;
  justify-content: center;
  margin-top: 32px;
}

/* Detail Dialog */
.detail-image {
  width: 100%;
  max-height: 240px;
  object-fit: cover;
  border-radius: 12px;
  margin-bottom: 16px;
}
.detail-info {}
.detail-tags { margin-bottom: 10px; }
.detail-desc { color: #475569; font-size: 14px; line-height: 1.7; margin-bottom: 16px; }
.detail-nutrition h4 { font-size: 14px; color: #374151; margin-bottom: 10px; }
.nut-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 8px;
  margin-bottom: 20px;
}
.nut-cell {
  background: #f0fdf4;
  border-radius: 10px;
  padding: 10px 6px;
  text-align: center;
}
.nut-val { display: block; font-size: 18px; font-weight: 700; color: #10b981; }
.nut-lbl { font-size: 11px; color: #64748b; }
.detail-price-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.detail-price { font-size: 26px; font-weight: 700; color: #ef4444; }

/* Order Dialog */
.order-form {}
.order-meal-info {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-bottom: 4px;
}
.order-meal-img {
  width: 64px;
  height: 64px;
  object-fit: cover;
  border-radius: 10px;
}
.order-meal-name { font-weight: 600; font-size: 15px; color: #0f172a; margin-bottom: 4px; }
.order-meal-price { font-size: 16px; color: #ef4444; font-weight: 600; }
.order-total {
  text-align: right;
  font-size: 15px;
  color: #374151;
  padding-top: 8px;
}
.total-price { font-size: 22px; font-weight: 700; color: #ef4444; }

.mr-1 { margin-right: 4px; }
</style>
