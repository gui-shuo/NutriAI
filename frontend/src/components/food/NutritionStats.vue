<template>
  <el-card class="nutrition-stats">
    <div class="stats-header">
      <h2 class="title">
        今日营养摄入
      </h2>
      <el-date-picker
        v-model="currentDate"
        type="date"
        placeholder="选择日期"
        format="YYYY年MM月DD日"
        value-format="YYYY-MM-DD"
        clearable
        @change="handleDateChange"
      />
    </div>

    <el-skeleton
      :loading="loading"
      animated
      :rows="3"
    >
      <div
        v-if="stats"
        class="stats-content"
      >
        <!-- 卡路里总览卡片 -->
        <div class="calorie-card">
          <div class="calorie-main">
            <div class="calorie-value">
              {{ stats.totalCalories || 0 }}
              <span class="unit">千卡</span>
            </div>
            <div class="calorie-label">
              总卡路里
            </div>
          </div>
          <div class="calorie-breakdown">
            <div
              v-for="meal in mealCalories"
              :key="meal.type"
              class="meal-item"
            >
              <span
                class="meal-dot"
                :style="{ background: meal.color }"
              />
              <span class="meal-name">{{ meal.name }}</span>
              <span class="meal-value">{{ meal.value }}</span>
            </div>
          </div>
        </div>

        <!-- 营养成分卡片 -->
        <div class="nutrition-cards">
          <div class="nutrition-item protein">
            <el-icon class="icon">
              <User />
            </el-icon>
            <div class="value">
              {{ stats.totalProtein || 0 }}g
            </div>
            <div class="label">
              蛋白质
            </div>
          </div>
          <div class="nutrition-item carbs">
            <el-icon class="icon">
              <Food />
            </el-icon>
            <div class="value">
              {{ stats.totalCarbohydrates || 0 }}g
            </div>
            <div class="label">
              碳水化合物
            </div>
          </div>
          <div class="nutrition-item fat">
            <el-icon class="icon">
              <Apple />
            </el-icon>
            <div class="value">
              {{ stats.totalFat || 0 }}g
            </div>
            <div class="label">
              脂肪
            </div>
          </div>
          <div class="nutrition-item fiber">
            <el-icon class="icon">
              <Grape />
            </el-icon>
            <div class="value">
              {{ stats.totalFiber || 0 }}g
            </div>
            <div class="label">
              膳食纤维
            </div>
          </div>
        </div>

        <!-- ECharts图表 -->
        <div class="charts-section">
          <div class="chart-container">
            <h3 class="chart-title">
              营养成分占比
            </h3>
            <v-chart
              :key="`pie-${currentDate}`"
              class="chart"
              :option="nutritionPieOption"
              :init-options="{ renderer: 'canvas' }"
              autoresize
            />
          </div>
          <div class="chart-container">
            <h3 class="chart-title">
              餐次卡路里分布
            </h3>
            <v-chart
              :key="`bar-${currentDate}`"
              class="chart"
              :option="mealBarOption"
              :init-options="{ renderer: 'canvas' }"
              autoresize
            />
          </div>
        </div>

        <!-- 统计信息 -->
        <div class="stats-info">
          <el-tag type="info">
            今日共记录 {{ stats.recordCount || 0 }} 条
          </el-tag>
        </div>
      </div>

      <el-empty
        v-else
        description="暂无数据"
      />
    </el-skeleton>
  </el-card>
</template>

<script setup>
import { ref, computed, watch, onMounted, onBeforeUnmount } from 'vue'
import { User, Food, Apple, Grape } from '@element-plus/icons-vue'
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
const formatDate = (date) => {
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
    { value: stats.value.totalCarbohydrates || 0, name: '碳水化合物', itemStyle: { color: '#36A2EB' } },
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
          { value: stats.value.breakfastCalories || 0, itemStyle: { color: MealTypes.BREAKFAST.color } },
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
const fetchStats = async (date) => {
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
const handleDateChange = (date) => {
  console.log('用户选择日期:', date)
  if (date) {
    currentDate.value = date
    emit('date-change', date)
    fetchStats(date)
  }
}

watch(() => props.date, (newDate, oldDate) => {
  console.log('date prop变化:', oldDate, '->', newDate)
  if (newDate) {
    const dateStr = typeof newDate === 'string' ? newDate : formatDate(newDate)
    currentDate.value = dateStr
    fetchStats(newDate)
  }
})

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
.nutrition-stats {
  border-radius: 16px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);

  :deep(.el-card__body) {
    padding: 24px;
  }
}

.stats-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;

  .title {
    font-size: 24px;
    font-weight: 600;
    color: #303133;
    margin: 0;
  }
}

.stats-content {
  .calorie-card {
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    border-radius: 16px;
    padding: 32px;
    color: white;
    margin-bottom: 24px;
    display: flex;
    justify-content: space-between;
    align-items: center;

    .calorie-main {
      .calorie-value {
        font-size: 48px;
        font-weight: 700;

        .unit {
          font-size: 20px;
          font-weight: 400;
          margin-left: 8px;
        }
      }

      .calorie-label {
        font-size: 16px;
        opacity: 0.9;
        margin-top: 8px;
      }
    }

    .calorie-breakdown {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 16px;

      .meal-item {
        display: flex;
        align-items: center;
        gap: 8px;

        .meal-dot {
          width: 12px;
          height: 12px;
          border-radius: 50%;
        }

        .meal-name {
          font-size: 14px;
          opacity: 0.9;
        }

        .meal-value {
          font-weight: 600;
          margin-left: auto;
        }
      }
    }
  }

  .nutrition-cards {
    display: grid;
    grid-template-columns: repeat(4, 1fr);
    gap: 16px;
    margin-bottom: 24px;

    .nutrition-item {
      background: white;
      border-radius: 12px;
      padding: 24px;
      text-align: center;
      border: 2px solid #f0f0f0;
      transition: all 0.3s;

      &:hover {
        transform: translateY(-4px);
        box-shadow: 0 8px 16px rgba(0, 0, 0, 0.1);
      }

      .icon {
        font-size: 32px;
        margin-bottom: 12px;
      }

      .value {
        font-size: 28px;
        font-weight: 700;
        margin-bottom: 8px;
      }

      .label {
        font-size: 14px;
        color: #909399;
      }

      &.protein {
        .icon { color: #FF6384; }
        .value { color: #FF6384; }
      }

      &.carbs {
        .icon { color: #36A2EB; }
        .value { color: #36A2EB; }
      }

      &.fat {
        .icon { color: #FFCE56; }
        .value { color: #FFCE56; }
      }

      &.fiber {
        .icon { color: #4BC0C0; }
        .value { color: #4BC0C0; }
      }
    }
  }

  .charts-section {
    display: grid;
    grid-template-columns: repeat(2, 1fr);
    gap: 24px;
    margin-bottom: 24px;

    .chart-container {
      background: white;
      border-radius: 12px;
      padding: 20px;
      border: 2px solid #f0f0f0;

      .chart-title {
        font-size: 16px;
        font-weight: 600;
        color: #303133;
        margin: 0 0 16px 0;
      }

      .chart {
        width: 100%;
        height: 300px;
      }
    }
  }

  .stats-info {
    text-align: center;
  }
}

@media (max-width: 1200px) {
  .nutrition-cards {
    grid-template-columns: repeat(2, 1fr) !important;
  }

  .charts-section {
    grid-template-columns: 1fr !important;
  }
}

@media (max-width: 768px) {
  .nutrition-cards {
    grid-template-columns: 1fr !important;
  }

  .calorie-card {
    flex-direction: column !important;
    gap: 24px;
  }
}
</style>
