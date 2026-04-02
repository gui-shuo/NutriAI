<template>
  <div class="nutrition-stats-wrap">
    <!-- 日期选择卡片 -->
    <el-card class="date-card" shadow="never">
      <div class="date-header">
        <h3 class="date-title">📅 营养统计</h3>
        <el-date-picker
          v-model="currentDate"
          type="date"
          placeholder="选择日期"
          format="MM月DD日"
          value-format="YYYY-MM-DD"
          clearable
          size="small"
          @change="handleDateChange"
        />
      </div>
    </el-card>

    <el-skeleton :loading="loading" animated :rows="4">
      <template v-if="stats">
        <!-- 卡路里总览 -->
        <el-card class="calorie-card" shadow="never">
          <div class="calorie-top">
            <div class="calorie-number">
              {{ stats.totalCalories || 0 }}
              <span class="calorie-unit">千卡</span>
            </div>
            <el-tag type="info" size="small">共 {{ stats.recordCount || 0 }} 条</el-tag>
          </div>
          <div class="meal-breakdown">
            <div v-for="meal in mealCalories" :key="meal.type" class="meal-row">
              <span class="meal-dot" :style="{ background: meal.color }" />
              <span class="meal-name">{{ meal.name }}</span>
              <span class="meal-val">{{ meal.value }} kcal</span>
            </div>
          </div>
        </el-card>

        <!-- 营养素横向条 -->
        <el-card class="nutrients-card" shadow="never">
          <div class="nutrient-strip">
            <div class="nutrient-item">
              <div class="nutrient-val protein-color">{{ stats.totalProtein || 0 }}g</div>
              <div class="nutrient-label">蛋白质</div>
            </div>
            <div class="nutrient-item">
              <div class="nutrient-val carbs-color">{{ stats.totalCarbohydrates || 0 }}g</div>
              <div class="nutrient-label">碳水</div>
            </div>
            <div class="nutrient-item">
              <div class="nutrient-val fat-color">{{ stats.totalFat || 0 }}g</div>
              <div class="nutrient-label">脂肪</div>
            </div>
            <div class="nutrient-item">
              <div class="nutrient-val fiber-color">{{ stats.totalFiber || 0 }}g</div>
              <div class="nutrient-label">纤维</div>
            </div>
          </div>
        </el-card>

        <!-- 图表（折叠） -->
        <el-card class="charts-card" shadow="never">
          <el-collapse>
            <el-collapse-item title="📊 营养成分占比" name="pie">
              <v-chart
                :key="`pie-${currentDate}`"
                class="sidebar-chart"
                :option="nutritionPieOption"
                :init-options="{ renderer: 'canvas' }"
                autoresize
              />
            </el-collapse-item>
            <el-collapse-item title="📈 餐次卡路里分布" name="bar">
              <v-chart
                :key="`bar-${currentDate}`"
                class="sidebar-chart"
                :option="mealBarOption"
                :init-options="{ renderer: 'canvas' }"
                autoresize
              />
            </el-collapse-item>
          </el-collapse>
        </el-card>
      </template>

      <el-empty v-else description="暂无数据" />
    </el-skeleton>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted, onBeforeUnmount } from 'vue'
import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { PieChart, BarChart } from 'echarts/charts'
import {
  TitleComponent,
  TooltipComponent,
  LegendComponent,
  GridComponent
} from 'echarts/components'
import { getNutritionStats, MealTypes } from '@/services/foodRecord'
import message from '@/utils/message'

// 注册ECharts组件
use([
  CanvasRenderer,
  PieChart,
  BarChart,
  TitleComponent,
  TooltipComponent,
  LegendComponent,
  GridComponent
])

const props = defineProps({
  date: {
    type: [Date, String],
    default: () => new Date()
  }
})

const emit = defineEmits(['date-change'])

// 格式化日期函数
const formatDate = date => {
  if (!date) return ''
  const d = new Date(date)
  const year = d.getFullYear()
  const month = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

const loading = ref(false)
const stats = ref(null)
const currentDate = ref(formatDate(new Date()))

// 餐次卡路里数据
const mealCalories = computed(() => {
  if (!stats.value) return []
  return [
    {
      type: 'BREAKFAST',
      name: '早餐',
      value: stats.value.breakfastCalories || 0,
      color: MealTypes.BREAKFAST.color
    },
    {
      type: 'LUNCH',
      name: '午餐',
      value: stats.value.lunchCalories || 0,
      color: MealTypes.LUNCH.color
    },
    {
      type: 'DINNER',
      name: '晚餐',
      value: stats.value.dinnerCalories || 0,
      color: MealTypes.DINNER.color
    },
    {
      type: 'SNACK',
      name: '加餐',
      value: stats.value.snackCalories || 0,
      color: MealTypes.SNACK.color
    }
  ]
})

// 营养成分饼图配置
const nutritionPieOption = computed(() => {
  if (!stats.value) return {}

  const data = [
    { value: stats.value.totalProtein || 0, name: '蛋白质', itemStyle: { color: '#FF6384' } },
    {
      value: stats.value.totalCarbohydrates || 0,
      name: '碳水化合物',
      itemStyle: { color: '#36A2EB' }
    },
    { value: stats.value.totalFat || 0, name: '脂肪', itemStyle: { color: '#FFCE56' } },
    { value: stats.value.totalFiber || 0, name: '膳食纤维', itemStyle: { color: '#4BC0C0' } }
  ]

  return {
    tooltip: {
      trigger: 'item',
      formatter: '{a} <br/>{b}: {c}g ({d}%)'
    },
    legend: {
      bottom: '5%',
      left: 'center'
    },
    series: [
      {
        name: '营养成分',
        type: 'pie',
        radius: ['40%', '70%'],
        avoidLabelOverlap: false,
        itemStyle: {
          borderRadius: 10,
          borderColor: '#fff',
          borderWidth: 2
        },
        label: {
          show: false
        },
        emphasis: {
          label: {
            show: true,
            fontSize: 16,
            fontWeight: 'bold'
          }
        },
        labelLine: {
          show: false
        },
        data
      }
    ]
  }
})

// 餐次卡路里柱状图配置
const mealBarOption = computed(() => {
  if (!stats.value) return {}

  return {
    tooltip: {
      trigger: 'axis',
      axisPointer: {
        type: 'shadow'
      },
      formatter: '{b}: {c}千卡'
    },
    xAxis: {
      type: 'category',
      data: ['早餐', '午餐', '晚餐', '加餐'],
      axisLabel: {
        fontSize: 12
      }
    },
    yAxis: {
      type: 'value',
      name: '千卡',
      axisLabel: {
        fontSize: 12
      }
    },
    series: [
      {
        type: 'bar',
        data: [
          {
            value: stats.value.breakfastCalories || 0,
            itemStyle: { color: MealTypes.BREAKFAST.color }
          },
          { value: stats.value.lunchCalories || 0, itemStyle: { color: MealTypes.LUNCH.color } },
          { value: stats.value.dinnerCalories || 0, itemStyle: { color: MealTypes.DINNER.color } },
          { value: stats.value.snackCalories || 0, itemStyle: { color: MealTypes.SNACK.color } }
        ],
        barWidth: '60%',
        itemStyle: {
          borderRadius: [8, 8, 0, 0]
        }
      }
    ]
  }
})

// 获取营养统计
const fetchStats = async date => {
  loading.value = true
  try {
    const dateStr = typeof date === 'string' ? date : formatDate(date)
    console.log('获取统计数据:', dateStr)
    const response = await getNutritionStats(dateStr)
    console.log('统计API响应:', response.data)
    if (response.data.code === 200) {
      stats.value = response.data.data
      console.log('统计数据:', stats.value)
    } else {
      console.error('API返回错误:', response.data)
      message.error(response.data.message || '获取统计失败')
    }
  } catch (error) {
    console.error('获取营养统计失败:', error)
    message.error('获取营养统计失败: ' + (error.message || '网络错误'))
  } finally {
    loading.value = false
  }
}

// 日期变化
const handleDateChange = date => {
  console.log('用户选择日期:', date)
  if (date) {
    currentDate.value = date
    emit('date-change', date)
    fetchStats(date)
  }
}

watch(
  () => props.date,
  (newDate, oldDate) => {
    console.log('date prop变化:', oldDate, '->', newDate)
    if (newDate) {
      const dateStr = typeof newDate === 'string' ? newDate : formatDate(newDate)
      currentDate.value = dateStr
      fetchStats(newDate)
    }
  }
)

onMounted(() => {
  console.log('NutritionStats组件挂载, props.date:', props.date)
  // 初始加载数据
  if (props.date) {
    fetchStats(props.date)
  } else {
    fetchStats(new Date())
  }
})

// 组件卸载前清理
onBeforeUnmount(() => {
  // 清空数据
  stats.value = null
})
</script>

<style scoped lang="scss">
.nutrition-stats-wrap {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.date-card,
.calorie-card,
.nutrients-card,
.charts-card {
  border-radius: 12px;
  border: 1px solid #E2E8F0;
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1), 0 2px 4px -2px rgba(0, 0, 0, 0.1);
  :deep(.el-card__body) {
    padding: 16px;
  }
}

/* 日期选择 */
.date-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.date-title {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: #0F172A;
  font-family: 'Calistoga', serif;
}

/* 卡路里卡片 */
.calorie-card {
  background: #10B981;
  color: white;
  :deep(.el-card__body) { padding: 20px; }
  :deep(.el-tag) { color: rgba(255,255,255,0.9); background: rgba(255,255,255,0.2); border: none; }
}

.calorie-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.calorie-number {
  font-size: 36px;
  font-weight: 700;
  line-height: 1;
}

.calorie-unit {
  font-size: 14px;
  font-weight: 400;
  opacity: 0.85;
  margin-left: 4px;
}

.meal-breakdown {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
}

.meal-row {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
}

.meal-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex-shrink: 0;
}

.meal-name {
  opacity: 0.85;
}

.meal-val {
  margin-left: auto;
  font-weight: 600;
  font-size: 12px;
}

/* 营养素条 */
.nutrient-strip {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 8px;
  text-align: center;
}

.nutrient-item {
  padding: 10px 4px;
  background: #FAFAFA;
  border-radius: 12px;
  border: 1px solid #E2E8F0;
}

.nutrient-val {
  font-size: 18px;
  font-weight: 700;
  margin-bottom: 2px;
}

.nutrient-label {
  font-size: 11px;
  color: #a8a29e;
}

.protein-color { color: #EF4444; }
.carbs-color { color: #10B981; }
.fat-color { color: #e6a23c; }
.fiber-color { color: #67c23a; }

/* 图表 */
.charts-card {
  :deep(.el-collapse) { border: none; }
  :deep(.el-collapse-item__header) {
    font-size: 14px;
    font-weight: 600;
    font-family: 'Calistoga', serif;
    height: 40px;
    line-height: 40px;
    border: none;
  }
  :deep(.el-collapse-item__wrap) { border: none; }
}

.sidebar-chart {
  width: 100%;
  height: 220px;
  border: 1px solid #E2E8F0;
  border-radius: 12px;
  padding: 8px;
}

@media (max-width: 1024px) {
  .nutrient-strip {
    grid-template-columns: repeat(4, 1fr);
  }
}

@media (max-width: 768px) {
  .nutrient-strip {
    grid-template-columns: repeat(2, 1fr);
  }

  .meal-breakdown {
    grid-template-columns: 1fr;
  }
}
</style>
