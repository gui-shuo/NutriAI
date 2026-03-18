<template>
  <div class="dashboard">
    <h2 class="page-title">数据看板</h2>

    <!-- 统计卡片 -->
    <el-row :gutter="16" class="stats-row">
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-content">
            <div class="stat-icon user-icon">
              <el-icon><User /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-label">总用户数</div>
              <div class="stat-value">{{ stats.userStats.totalUsers }}</div>
              <div class="stat-sub">今日新增: {{ stats.userStats.todayNewUsers }}</div>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-content">
            <div class="stat-icon chat-icon">
              <el-icon><ChatDotRound /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-label">总对话数</div>
              <div class="stat-value">{{ stats.chatStats.totalChats }}</div>
              <div class="stat-sub">今日对话: {{ stats.chatStats.todayChats }}</div>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-content">
            <div class="stat-icon ai-icon">
              <el-icon><Cpu /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-label">AI调用次数</div>
              <div class="stat-value">{{ stats.aiStats.totalCalls }}</div>
              <div class="stat-sub">成功率: {{ stats.aiStats.successRate.toFixed(1) }}%</div>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-content">
            <div class="stat-icon active-icon">
              <el-icon><TrendCharts /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-label">活跃用户</div>
              <div class="stat-value">{{ stats.userStats.activeUsers }}</div>
              <div class="stat-sub">最近7天</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 图表区域 -->
    <el-row :gutter="16" class="charts-row">
      <el-col :span="12">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>用户增长趋势</span>
              <el-radio-group v-model="userGrowthDays" size="small" @change="loadUserGrowth">
                <el-radio-button :value="7">7天</el-radio-button>
                <el-radio-button :value="30">30天</el-radio-button>
              </el-radio-group>
            </div>
          </template>
          <div ref="userGrowthChart" class="chart"></div>
        </el-card>
      </el-col>

      <el-col :span="12">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>AI使用趋势</span>
              <el-radio-group v-model="aiUsageDays" size="small" @change="loadAIUsage">
                <el-radio-button :value="7">7天</el-radio-button>
                <el-radio-button :value="30">30天</el-radio-button>
              </el-radio-group>
            </div>
          </template>
          <div ref="aiUsageChart" class="chart"></div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 会员分布 -->
    <el-row :gutter="16">
      <el-col :span="12">
        <el-card>
          <template #header>
            <span>会员等级分布</span>
          </template>
          <div ref="memberChart" class="chart"></div>
        </el-card>
      </el-col>

      <el-col :span="12">
        <el-card>
          <template #header>
            <span>系统状态</span>
          </template>
          <el-descriptions :column="2" border>
            <el-descriptions-item label="平均响应时间">
              {{ stats.chatStats.avgResponseTime.toFixed(0) }}ms
            </el-descriptions-item>
            <el-descriptions-item label="平均Token使用">
              {{ stats.aiStats.avgTokens.toFixed(0) }}
            </el-descriptions-item>
            <el-descriptions-item label="昨日新增用户">
              {{ stats.userStats.yesterdayNewUsers }}
            </el-descriptions-item>
            <el-descriptions-item label="昨日对话数">
              {{ stats.chatStats.yesterdayChats }}
            </el-descriptions-item>
          </el-descriptions>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'
import { User, ChatDotRound, Cpu, TrendCharts } from '@element-plus/icons-vue'
// 按需导入ECharts组件
import * as echarts from 'echarts/core'
import { LineChart, PieChart } from 'echarts/charts'
import {
  TitleComponent,
  TooltipComponent,
  GridComponent,
  LegendComponent
} from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'

// 注册必需的组件
echarts.use([
  TitleComponent,
  TooltipComponent,
  GridComponent,
  LegendComponent,
  LineChart,
  PieChart,
  CanvasRenderer
])

const stats = ref({
  userStats: {
    totalUsers: 0,
    todayNewUsers: 0,
    yesterdayNewUsers: 0,
    activeUsers: 0
  },
  chatStats: {
    totalChats: 0,
    todayChats: 0,
    yesterdayChats: 0,
    avgResponseTime: 0
  },
  aiStats: {
    totalCalls: 0,
    todayCalls: 0,
    successRate: 0,
    avgTokens: 0
  },
  memberStats: {
    free: 0,
    bronze: 0,
    silver: 0,
    gold: 0
  }
})

const userGrowthDays = ref(7)
const aiUsageDays = ref(7)

const userGrowthChart = ref(null)
const aiUsageChart = ref(null)
const memberChart = ref(null)

let userGrowthChartInstance = null
let aiUsageChartInstance = null
let memberChartInstance = null

// 加载统计数据
const loadStats = async () => {
  try {
    const token = localStorage.getItem('token')
    const response = await fetch('http://localhost:8080/api/admin/dashboard/stats', {
      headers: {
        'Authorization': `Bearer ${token}`
      }
    })
    
    const data = await response.json()
    if (data.code === 200) {
      stats.value = data.data
      initMemberChart()
    }
  } catch (error) {
    console.error('加载统计数据失败:', error)
    ElMessage.error('加载统计数据失败')
  }
}

// 加载用户增长趋势
const loadUserGrowth = async () => {
  try {
    const token = localStorage.getItem('token')
    const response = await fetch(
      `http://localhost:8080/api/admin/dashboard/user-growth?days=${userGrowthDays.value}`,
      {
        headers: {
          'Authorization': `Bearer ${token}`
        }
      }
    )
    
    const data = await response.json()
    if (data.code === 200) {
      initUserGrowthChart(data.data)
    }
  } catch (error) {
    console.error('加载用户增长趋势失败:', error)
  }
}

// 加载AI使用趋势
const loadAIUsage = async () => {
  try {
    const token = localStorage.getItem('token')
    const response = await fetch(
      `http://localhost:8080/api/admin/dashboard/ai-usage-trend?days=${aiUsageDays.value}`,
      {
        headers: {
          'Authorization': `Bearer ${token}`
        }
      }
    )
    
    const data = await response.json()
    if (data.code === 200) {
      initAIUsageChart(data.data)
    } else {
      console.error('加载AI使用趋势失败:', data.message)
    }
  } catch (error) {
    console.error('加载AI使用趋势失败:', error)
  }
}

// 初始化用户增长图表
const initUserGrowthChart = (data) => {
  if (!userGrowthChart.value) return
  
  if (!userGrowthChartInstance) {
    userGrowthChartInstance = echarts.init(userGrowthChart.value)
  }
  
  const option = {
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(255, 255, 255, 0.9)',
      borderColor: '#ddd',
      borderWidth: 1,
      textStyle: {
        color: '#333'
      },
      formatter: (params) => {
        const param = params[0]
        return `${param.axisValue}<br/>新增用户: <strong>${param.value}</strong>`
      }
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      data: data.map(item => item.date.slice(5)),
      boundaryGap: false,
      axisLine: {
        lineStyle: {
          color: '#ddd'
        }
      },
      axisLabel: {
        color: '#666'
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
        color: '#666'
      },
      splitLine: {
        lineStyle: {
          color: '#f0f0f0'
        }
      }
    },
    series: [{
      name: '新增用户',
      type: 'line',
      data: data.map(item => item.count),
      smooth: true,
      symbol: 'circle',
      symbolSize: 6,
      areaStyle: {
        color: {
          type: 'linear',
          x: 0,
          y: 0,
          x2: 0,
          y2: 1,
          colorStops: [{
            offset: 0, color: 'rgba(24, 144, 255, 0.3)'
          }, {
            offset: 1, color: 'rgba(24, 144, 255, 0.05)'
          }]
        }
      },
      itemStyle: {
        color: '#1890ff',
        borderWidth: 2,
        borderColor: '#fff'
      },
      lineStyle: {
        width: 3
      }
    }]
  }
  
  userGrowthChartInstance.setOption(option)
}

// 初始化AI使用图表
const initAIUsageChart = (data) => {
  if (!aiUsageChart.value) return
  
  if (!aiUsageChartInstance) {
    aiUsageChartInstance = echarts.init(aiUsageChart.value)
  }
  
  const option = {
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(255, 255, 255, 0.9)',
      borderColor: '#ddd',
      borderWidth: 1,
      textStyle: {
        color: '#333'
      },
      axisPointer: {
        type: 'shadow'
      }
    },
    legend: {
      data: ['总调用', '成功调用'],
      bottom: 0
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '10%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      data: data.map(item => item.date.slice(5)),
      axisLine: {
        lineStyle: {
          color: '#ddd'
        }
      },
      axisLabel: {
        color: '#666'
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
        color: '#666'
      },
      splitLine: {
        lineStyle: {
          color: '#f0f0f0'
        }
      }
    },
    series: [
      {
        name: '总调用',
        type: 'bar',
        data: data.map(item => item.count),
        barWidth: '40%',
        itemStyle: {
          color: {
            type: 'linear',
            x: 0,
            y: 0,
            x2: 0,
            y2: 1,
            colorStops: [{
              offset: 0, color: '#4facfe'
            }, {
              offset: 1, color: '#00f2fe'
            }]
          },
          borderRadius: [4, 4, 0, 0]
        }
      },
      {
        name: '成功调用',
        type: 'bar',
        data: data.map(item => item.success),
        barWidth: '40%',
        itemStyle: {
          color: {
            type: 'linear',
            x: 0,
            y: 0,
            x2: 0,
            y2: 1,
            colorStops: [{
              offset: 0, color: '#43e97b'
            }, {
              offset: 1, color: '#38f9d7'
            }]
          },
          borderRadius: [4, 4, 0, 0]
        }
      }
    ]
  }
  
  aiUsageChartInstance.setOption(option)
}

// 初始化会员分布图表
const initMemberChart = () => {
  if (!memberChart.value) return
  
  if (!memberChartInstance) {
    memberChartInstance = echarts.init(memberChart.value)
  }
  
  const option = {
    tooltip: {
      trigger: 'item',
      backgroundColor: 'rgba(255, 255, 255, 0.9)',
      borderColor: '#ddd',
      borderWidth: 1,
      textStyle: {
        color: '#333'
      },
      formatter: '{b}: <strong>{c}</strong> ({d}%)'
    },
    legend: {
      orient: 'vertical',
      left: 'left',
      top: 'center',
      textStyle: {
        color: '#666'
      }
    },
    series: [
      {
        name: '会员等级',
        type: 'pie',
        radius: ['40%', '70%'],
        center: ['60%', '50%'],
        avoidLabelOverlap: false,
        itemStyle: {
          borderRadius: 10,
          borderColor: '#fff',
          borderWidth: 2
        },
        label: {
          show: false,
          position: 'center'
        },
        emphasis: {
          label: {
            show: true,
            fontSize: 20,
            fontWeight: 'bold'
          },
          itemStyle: {
            shadowBlur: 10,
            shadowOffsetX: 0,
            shadowColor: 'rgba(0, 0, 0, 0.5)'
          }
        },
        labelLine: {
          show: false
        },
        data: [
          { 
            value: stats.value.memberStats.free, 
            name: '免费用户',
            itemStyle: { color: '#91cc75' }
          },
          { 
            value: stats.value.memberStats.bronze, 
            name: '青铜会员',
            itemStyle: { color: '#fac858' }
          },
          { 
            value: stats.value.memberStats.silver, 
            name: '白银会员',
            itemStyle: { color: '#73c0de' }
          },
          { 
            value: stats.value.memberStats.gold, 
            name: '黄金会员',
            itemStyle: { color: '#ee6666' }
          }
        ]
      }
    ]
  }
  
  memberChartInstance.setOption(option)
}

// 窗口大小改变时重新渲染图表
const handleResize = () => {
  userGrowthChartInstance?.resize()
  aiUsageChartInstance?.resize()
  memberChartInstance?.resize()
}

onMounted(() => {
  loadStats()
  loadUserGrowth()
  loadAIUsage()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  userGrowthChartInstance?.dispose()
  aiUsageChartInstance?.dispose()
  memberChartInstance?.dispose()
})
</script>

<style scoped>
.dashboard {
  padding: 0;
}

.page-title {
  margin: 0 0 24px 0;
  font-size: 24px;
  font-weight: 500;
  color: #262626;
}

.stats-row {
  margin-bottom: 16px;
}

.stat-card {
  cursor: pointer;
  transition: all 0.3s;
}

.stat-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.stat-content {
  display: flex;
  align-items: center;
  gap: 16px;
}

.stat-icon {
  width: 56px;
  height: 56px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28px;
}

.user-icon {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
}

.chat-icon {
  background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
  color: #fff;
}

.ai-icon {
  background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%);
  color: #fff;
}

.active-icon {
  background: linear-gradient(135deg, #43e97b 0%, #38f9d7 100%);
  color: #fff;
}

.stat-info {
  flex: 1;
}

.stat-label {
  font-size: 14px;
  color: #8c8c8c;
  margin-bottom: 8px;
}

.stat-value {
  font-size: 24px;
  font-weight: 600;
  color: #262626;
  margin-bottom: 4px;
}

.stat-sub {
  font-size: 12px;
  color: #8c8c8c;
}

.charts-row {
  margin-bottom: 16px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.chart {
  height: 300px;
}
</style>
