<template>
  <div class="nutrition-stats-card">
    <div class="stats-header">
      <div class="header-left">
        <span class="header-icon">📊</span>
        <h2 class="title">今日营养摄入</h2>
      </div>
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

    <el-skeleton :loading="loading" animated :rows="3">
      <div v-if="stats" class="stats-content">
        <!-- 卡路里总览卡片 -->
        <div class="calorie-card">
          <div class="calorie-glow"></div>
          <div class="calorie-main">
            <div class="calorie-label">总卡路里摄入</div>
            <div class="calorie-value">
              <span class="number">{{ stats.totalCalories || 0 }}</span>
              <span class="unit">千卡</span>
            </div>
            <div class="calorie-ring">
              <svg viewBox="0 0 120 120" class="ring-svg">
                <circle cx="60" cy="60" r="52" class="ring-bg" />
                <circle cx="60" cy="60" r="52" class="ring-fill" />
              </svg>
              <span class="ring-label">🔥</span>
            </div>
          </div>
          <div class="calorie-breakdown">
            <div v-for="meal in mealCalories" :key="meal.type" class="meal-item">
              <span class="meal-dot" :style="{ background: meal.color }" />
              <span class="meal-name">{{ meal.name }}</span>
              <span class="meal-value">{{ meal.value }} <small>kcal</small></span>
            </div>
          </div>
        </div>

        <!-- 营养成分卡片 -->
        <div class="nutrition-cards">
          <div class="nutrition-item protein">
            <div class="nut-icon-wrap">
              <el-icon class="icon"><User /></el-icon>
            </div>
            <div class="nut-info">
              <div class="value">{{ stats.totalProtein || 0 }}<small>g</small></div>
              <div class="label">蛋白质</div>
            </div>
          </div>
          <div class="nutrition-item carbs">
            <div class="nut-icon-wrap">
              <el-icon class="icon"><Food /></el-icon>
            </div>
            <div class="nut-info">
              <div class="value">{{ stats.totalCarbohydrates || 0 }}<small>g</small></div>
              <div class="label">碳水化合物</div>
            </div>
          </div>
          <div class="nutrition-item fat">
            <div class="nut-icon-wrap">
              <el-icon class="icon"><Apple /></el-icon>
            </div>
            <div class="nut-info">
              <div class="value">{{ stats.totalFat || 0 }}<small>g</small></div>
              <div class="label">脂肪</div>
            </div>
          </div>
          <div class="nutrition-item fiber">
            <div class="nut-icon-wrap">
              <el-icon class="icon"><Grape /></el-icon>
            </div>
            <div class="nut-info">
              <div class="value">{{ stats.totalFiber || 0 }}<small>g</small></div>
              <div class="label">膳食纤维</div>
            </div>
          </div>
        </div>

        <!-- ECharts图表 -->
        <div class="charts-section">
          <div class="chart-container">
            <h3 class="chart-title">
              <span class="chart-dot pie"></span>
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
              <span class="chart-dot bar"></span>
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
          <el-tag type="info"> 今日共记录 {{ stats.recordCount || 0 }} 条 </el-tag>
        </div>
      </div>

      <el-empty v-else description="暂无数据" />
    </el-skeleton>
  </div>
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
.nutrition-stats-card {
  border-radius: 20px;
  border: 1px solid rgba(255, 255, 255, 0.08);
  background: rgba(30, 41, 59, 0.7);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  box-shadow:
    0 8px 32px rgba(0, 0, 0, 0.3),
    inset 0 1px 0 rgba(255, 255, 255, 0.05);
  padding: 28px;
}

.stats-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 28px;

  .header-left {
    display: flex;
    align-items: center;
    gap: 10px;

    .header-icon {
      font-size: 24px;
    }
  }

  .title {
    font-size: 22px;
    font-weight: 700;
    color: rgba(255, 255, 255, 0.95);
    margin: 0;
    letter-spacing: 0.3px;
  }

  :deep(.el-input__wrapper) {
    border-radius: 12px;
    background: rgba(255, 255, 255, 0.06);
    border: 1px solid rgba(255, 255, 255, 0.1);
    box-shadow: none;

    &:hover,
    &.is-focus {
      border-color: rgba(99, 102, 241, 0.5);
      background: rgba(255, 255, 255, 0.08);
    }

    .el-input__inner {
      color: rgba(255, 255, 255, 0.85);

      &::placeholder {
        color: rgba(255, 255, 255, 0.35);
      }
    }

    .el-input__icon {
      color: rgba(255, 255, 255, 0.5);
    }
  }
}

.stats-content {
  .calorie-card {
    position: relative;
    background: linear-gradient(135deg, #312e81, #4c1d95, #581c87);
    border-radius: 20px;
    padding: 32px;
    color: white;
    margin-bottom: 24px;
    display: flex;
    justify-content: space-between;
    align-items: center;
    overflow: hidden;

    .calorie-glow {
      position: absolute;
      top: -40px;
      right: -40px;
      width: 200px;
      height: 200px;
      background: radial-gradient(circle, rgba(167, 139, 250, 0.3) 0%, transparent 70%);
      border-radius: 50%;
      pointer-events: none;
    }

    .calorie-main {
      position: relative;
      z-index: 1;
      display: flex;
      flex-direction: column;
      gap: 6px;

      .calorie-label {
        font-size: 14px;
        opacity: 0.7;
        text-transform: uppercase;
        letter-spacing: 1px;
        font-weight: 500;
      }

      .calorie-value {
        display: flex;
        align-items: baseline;
        gap: 8px;

        .number {
          font-size: 52px;
          font-weight: 800;
          line-height: 1;
          letter-spacing: -1px;
          background: linear-gradient(180deg, #fff 0%, rgba(255, 255, 255, 0.8) 100%);
          -webkit-background-clip: text;
          -webkit-text-fill-color: transparent;
          background-clip: text;
        }

        .unit {
          font-size: 18px;
          font-weight: 500;
          opacity: 0.7;
        }
      }

      .calorie-ring {
        position: absolute;
        right: -140px;
        top: 50%;
        transform: translateY(-50%);
        width: 100px;
        height: 100px;

        .ring-svg {
          width: 100%;
          height: 100%;
          transform: rotate(-90deg);

          .ring-bg {
            fill: none;
            stroke: rgba(255, 255, 255, 0.1);
            stroke-width: 8;
          }

          .ring-fill {
            fill: none;
            stroke: rgba(167, 139, 250, 0.6);
            stroke-width: 8;
            stroke-linecap: round;
            stroke-dasharray: 327;
            stroke-dashoffset: 80;
            animation: ring-anim 1.5s ease-out forwards;
          }
        }

        .ring-label {
          position: absolute;
          top: 50%;
          left: 50%;
          transform: translate(-50%, -50%);
          font-size: 28px;
        }
      }
    }

    .calorie-breakdown {
      position: relative;
      z-index: 1;
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 14px 24px;

      .meal-item {
        display: flex;
        align-items: center;
        gap: 10px;
        padding: 8px 14px;
        background: rgba(255, 255, 255, 0.08);
        border-radius: 10px;
        backdrop-filter: blur(4px);

        .meal-dot {
          width: 10px;
          height: 10px;
          border-radius: 50%;
          flex-shrink: 0;
          box-shadow: 0 0 8px currentColor;
        }

        .meal-name {
          font-size: 13px;
          opacity: 0.85;
          white-space: nowrap;
        }

        .meal-value {
          font-weight: 700;
          font-size: 15px;
          margin-left: auto;

          small {
            font-size: 11px;
            font-weight: 400;
            opacity: 0.6;
            margin-left: 2px;
          }
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
      border-radius: 16px;
      padding: 22px 18px;
      display: flex;
      align-items: center;
      gap: 14px;
      transition: all 0.35s cubic-bezier(0.4, 0, 0.2, 1);
      border: 1px solid transparent;
      position: relative;
      overflow: hidden;

      &::after {
        content: '';
        position: absolute;
        top: -20px;
        right: -20px;
        width: 80px;
        height: 80px;
        border-radius: 50%;
        background: rgba(255, 255, 255, 0.06);
        pointer-events: none;
      }

      &:hover {
        transform: translateY(-4px);
        box-shadow: 0 12px 28px rgba(0, 0, 0, 0.25);
      }

      .nut-icon-wrap {
        width: 48px;
        height: 48px;
        border-radius: 14px;
        display: flex;
        align-items: center;
        justify-content: center;
        flex-shrink: 0;

        .icon {
          font-size: 24px;
          color: #fff;
        }
      }

      .nut-info {
        .value {
          font-size: 26px;
          font-weight: 800;
          color: #fff;
          line-height: 1.2;

          small {
            font-size: 14px;
            font-weight: 500;
            opacity: 0.7;
          }
        }

        .label {
          font-size: 13px;
          color: rgba(255, 255, 255, 0.7);
          margin-top: 2px;
        }
      }

      &.protein {
        background: linear-gradient(135deg, #1e40af, #3b82f6);
        .nut-icon-wrap {
          background: rgba(255, 255, 255, 0.15);
        }
      }

      &.carbs {
        background: linear-gradient(135deg, #065f46, #10b981);
        .nut-icon-wrap {
          background: rgba(255, 255, 255, 0.15);
        }
      }

      &.fat {
        background: linear-gradient(135deg, #92400e, #f59e0b);
        .nut-icon-wrap {
          background: rgba(255, 255, 255, 0.15);
        }
      }

      &.fiber {
        background: linear-gradient(135deg, #581c87, #a855f7);
        .nut-icon-wrap {
          background: rgba(255, 255, 255, 0.15);
        }
      }
    }
  }

  .charts-section {
    display: grid;
    grid-template-columns: repeat(2, 1fr);
    gap: 20px;
    margin-bottom: 24px;

    .chart-container {
      background: rgba(255, 255, 255, 0.04);
      border-radius: 16px;
      padding: 22px;
      border: 1px solid rgba(255, 255, 255, 0.06);
      transition: border-color 0.3s ease;

      &:hover {
        border-color: rgba(255, 255, 255, 0.12);
      }

      .chart-title {
        font-size: 15px;
        font-weight: 600;
        color: rgba(255, 255, 255, 0.9);
        margin: 0 0 16px 0;
        display: flex;
        align-items: center;
        gap: 8px;

        .chart-dot {
          width: 8px;
          height: 8px;
          border-radius: 50%;

          &.pie {
            background: #818cf8;
          }

          &.bar {
            background: #34d399;
          }
        }
      }

      .chart {
        width: 100%;
        height: 300px;
      }
    }
  }

  .stats-info {
    text-align: center;

    :deep(.el-tag) {
      border-radius: 20px;
      padding: 6px 20px;
      background: rgba(99, 102, 241, 0.15);
      border-color: rgba(99, 102, 241, 0.25);
      color: #a5b4fc;
      font-weight: 500;
    }
  }
}

@keyframes ring-anim {
  from {
    stroke-dashoffset: 327;
  }
  to {
    stroke-dashoffset: 80;
  }
}

:deep(.el-skeleton) {
  --el-skeleton-color: rgba(255, 255, 255, 0.06);
  --el-skeleton-to-color: rgba(255, 255, 255, 0.12);
}

:deep(.el-empty__description p) {
  color: rgba(255, 255, 255, 0.5);
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
  .nutrition-stats-card {
    padding: 20px;
  }

  .stats-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }

  .nutrition-cards {
    grid-template-columns: 1fr !important;
  }

  .calorie-card {
    flex-direction: column !important;
    gap: 24px;
  }
}
</style>
