<template>
  <el-card class="growth-chart-card">
    <template #header>
      <div class="card-header">
        <span class="title">成长值趋势</span>
        <el-select
          v-model="timeRange"
          size="small"
          style="width: 120px"
          @change="handleTimeRangeChange"
        >
          <el-option
            label="最近7天"
            value="7"
          />
          <el-option
            label="最近30天"
            value="30"
          />
          <el-option
            label="最近90天"
            value="90"
          />
        </el-select>
      </div>
    </template>

    <el-skeleton
      :loading="loading"
      animated
      :rows="8"
    >
      <div
        v-if="!loading && chartData.dates.length > 0"
        class="chart-container"
      >
        <v-chart
          :key="`chart-${timeRange}`"
          :option="chartOption"
          :autoresize="true"
          :init-options="{ renderer: 'canvas' }"
          style="height: 300px"
        />
      </div>
      <el-empty
        v-else-if="!loading"
        description="暂无数据"
        :image-size="100"
      />

      <!-- 成长值统计 -->
      <div class="growth-stats">
        <div class="stat-item">
          <span class="stat-label">总获得</span>
          <span
            class="stat-value"
            style="color: #67c23a"
          >+{{ totalGained }}</span>
        </div>
        <div class="stat-item">
          <span class="stat-label">平均/天</span>
          <span
            class="stat-value"
            style="color: #409eff"
          >{{ avgPerDay }}</span>
        </div>
        <div class="stat-item">
          <span class="stat-label">最高/天</span>
          <span
            class="stat-value"
            style="color: #e6a23c"
          >{{ maxPerDay }}</span>
        </div>
      </div>
    </el-skeleton>
  </el-card>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { LineChart } from 'echarts/charts'
import {
  TitleComponent,
  TooltipComponent,
  LegendComponent,
  GridComponent
} from 'echarts/components'
import { getGrowthRecords } from '@/services/member'
import message from '@/utils/message'

// 注册ECharts组件
use([CanvasRenderer, LineChart, TitleComponent, TooltipComponent, LegendComponent, GridComponent])

defineProps({
  userId: {
    type: Number,
    required: true
  }
})

const loading = ref(false)
const timeRange = ref('30')
const growthData = ref([])

// 图表数据处理
const chartData = computed(() => {
  if (!growthData.value.length) return { dates: [], values: [] }

  // 计算时间范围的起始日期
  const now = new Date()
  const daysAgo = parseInt(timeRange.value)
  const startDate = new Date(now.getTime() - daysAgo * 24 * 60 * 60 * 1000)

  // 过滤时间范围内的数据
  const filteredData = growthData.value.filter(record => {
    const recordDate = new Date(record.createdAt)
    return recordDate >= startDate
  })

  // 按日期分组
  const dailyGrowthMap = {}
  filteredData.forEach(record => {
    const date = new Date(record.createdAt).toLocaleDateString('zh-CN', {
      month: 'short',
      day: 'numeric'
    })
    dailyGrowthMap[date] = (dailyGrowthMap[date] || 0) + record.growthValue
  })

  // 生成日期序列和累计值
  const dates = []
  const values = []
  let cumulativeGrowth = 0

  // 按时间顺序排序
  const sortedEntries = Object.entries(dailyGrowthMap).sort((a, b) => {
    const dateA = new Date(a[0])
    const dateB = new Date(b[0])
    return dateA - dateB
  })

  sortedEntries.forEach(([date, dailyGrowth]) => {
    cumulativeGrowth += dailyGrowth
    dates.push(date)
    values.push(cumulativeGrowth)
  })

  return { dates, values }
})

// 图表配置
const chartOption = computed(() => ({
  tooltip: {
    trigger: 'axis',
    backgroundColor: 'rgba(255, 255, 255, 0.95)',
    borderColor: '#e5e7eb',
    borderWidth: 1,
    textStyle: {
      color: '#374151'
    },
    formatter: params => {
      const { name, value } = params[0]
      return `<div style="padding: 8px">
        <div style="margin-bottom: 4px; font-weight: 600">${name}</div>
        <div>成长值: <span style="color: #667eea; font-weight: 600">${value}</span></div>
      </div>`
    }
  },
  grid: {
    left: '50',
    right: '20',
    bottom: '30',
    top: '30'
  },
  xAxis: {
    type: 'category',
    data: chartData.value.dates,
    boundaryGap: false,
    axisLine: {
      lineStyle: {
        color: '#e5e7eb'
      }
    },
    axisLabel: {
      color: '#6b7280',
      fontSize: 12
    }
  },
  yAxis: {
    type: 'value',
    axisLine: {
      show: false
    },
    axisTick: {
      show: false
    },
    axisLabel: {
      color: '#6b7280',
      fontSize: 12
    },
    splitLine: {
      lineStyle: {
        color: '#f3f4f6',
        type: 'dashed'
      }
    }
  },
  series: [
    {
      name: '成长值',
      type: 'line',
      smooth: true,
      symbol: 'circle',
      symbolSize: 6,
      data: chartData.value.values,
      lineStyle: {
        width: 3,
        color: {
          type: 'linear',
          x: 0,
          y: 0,
          x2: 1,
          y2: 0,
          colorStops: [
            { offset: 0, color: '#667eea' },
            { offset: 1, color: '#764ba2' }
          ]
        }
      },
      itemStyle: {
        color: '#667eea',
        borderWidth: 2,
        borderColor: '#fff'
      },
      areaStyle: {
        color: {
          type: 'linear',
          x: 0,
          y: 0,
          x2: 0,
          y2: 1,
          colorStops: [
            { offset: 0, color: 'rgba(102, 126, 234, 0.3)' },
            { offset: 1, color: 'rgba(118, 75, 162, 0.05)' }
          ]
        }
      }
    }
  ]
}))

// 获取过滤后的数据（用于统计）
const filteredGrowthData = computed(() => {
  if (!growthData.value.length) return []

  const now = new Date()
  const daysAgo = parseInt(timeRange.value)
  const startDate = new Date(now.getTime() - daysAgo * 24 * 60 * 60 * 1000)

  return growthData.value.filter(record => {
    const recordDate = new Date(record.createdAt)
    return recordDate >= startDate
  })
})

// 统计数据
const totalGained = computed(() => {
  return filteredGrowthData.value.reduce((sum, record) => sum + record.growthValue, 0)
})

const avgPerDay = computed(() => {
  if (!filteredGrowthData.value.length) return 0
  return Math.round(totalGained.value / parseInt(timeRange.value))
})

const maxPerDay = computed(() => {
  if (!filteredGrowthData.value.length) return 0
  // 按日期分组，找出最大值
  const dailyGrowth = {}
  filteredGrowthData.value.forEach(record => {
    const date = new Date(record.createdAt).toLocaleDateString()
    dailyGrowth[date] = (dailyGrowth[date] || 0) + record.growthValue
  })
  return Math.max(...Object.values(dailyGrowth))
})

// 获取成长值记录
const fetchGrowthRecords = async () => {
  loading.value = true
  try {
    const response = await getGrowthRecords(0, parseInt(timeRange.value) * 10) // 获取足够的数据
    if (response.data.code === 200) {
      growthData.value = response.data.data.content || []
      console.log('成长值记录:', growthData.value)
    } else {
      message.error(response.data.message || '获取成长值记录失败')
    }
  } catch (error) {
    console.error('获取成长值记录失败:', error)
    message.error('获取成长值记录失败')
  } finally {
    loading.value = false
  }
}

const handleTimeRangeChange = () => {
  fetchGrowthRecords()
}

onMounted(() => {
  fetchGrowthRecords()
})

// 组件卸载前清理
onBeforeUnmount(() => {
  // vue-echarts 会自动清理实例，但我们确保数据也被清空
  growthData.value = []
})
</script>

<style scoped lang="scss">
.growth-chart-card {
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);

  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;

    .title {
      font-size: 18px;
      font-weight: 600;
      color: #1f2937;
    }
  }

  .chart-container {
    margin-bottom: 24px;
  }

  .growth-stats {
    display: grid;
    grid-template-columns: repeat(3, 1fr);
    gap: 16px;
    padding: 20px;
    background: #f9fafb;
    border-radius: 8px;

    .stat-item {
      text-align: center;

      .stat-label {
        display: block;
        font-size: 13px;
        color: #6b7280;
        margin-bottom: 8px;
      }

      .stat-value {
        display: block;
        font-size: 24px;
        font-weight: 600;
      }
    }
  }
}
</style>
