<template>
  <div class="dashboard">
    <!-- 顶部欢迎区域 -->
    <div class="welcome-section">
      <div class="welcome-text">
        <h2>数据看板</h2>
        <p>{{ greeting }}，管理员 · {{ todayStr }}</p>
      </div>
    </div>

    <!-- 核心指标卡片 -->
    <div class="stat-grid">
      <div class="stat-card stat-users" @click="$router.push('/admin/users')">
        <div class="stat-card-bg"><el-icon><User /></el-icon></div>
        <div class="stat-main">
          <span class="stat-value">{{ animatedStats.totalUsers }}</span>
          <span class="stat-label">总用户</span>
        </div>
        <div class="stat-footer">
          <span class="stat-badge up" v-if="stats.userStats.todayNewUsers > 0">
            +{{ stats.userStats.todayNewUsers }} 今日
          </span>
          <span class="stat-badge neutral" v-else>今日无新增</span>
          <span class="stat-extra">昨日 +{{ stats.userStats.yesterdayNewUsers }}</span>
        </div>
      </div>

      <div class="stat-card stat-chats">
        <div class="stat-card-bg"><el-icon><ChatDotRound /></el-icon></div>
        <div class="stat-main">
          <span class="stat-value">{{ animatedStats.totalChats }}</span>
          <span class="stat-label">会话总数</span>
        </div>
        <div class="stat-footer">
          <span class="stat-badge" :class="stats.chatStats.todayChats > 0 ? 'up' : 'neutral'">
            {{ stats.chatStats.todayChats > 0 ? '+' : '' }}{{ stats.chatStats.todayChats }} 今日
          </span>
          <span class="stat-extra">昨日 {{ stats.chatStats.yesterdayChats }}</span>
        </div>
      </div>

      <div class="stat-card stat-ai" @click="$router.push('/admin/ai-logs')">
        <div class="stat-card-bg"><el-icon><Cpu /></el-icon></div>
        <div class="stat-main">
          <span class="stat-value">{{ animatedStats.totalCalls }}</span>
          <span class="stat-label">AI调用</span>
        </div>
        <div class="stat-footer">
          <span class="stat-badge" :class="stats.aiStats.successRate >= 95 ? 'up' : 'warn'">
            {{ stats.aiStats.successRate.toFixed(1) }}% 成功
          </span>
          <span class="stat-extra">今日 {{ stats.aiStats.todayCalls }}</span>
        </div>
      </div>

      <div class="stat-card stat-active">
        <div class="stat-card-bg"><el-icon><TrendCharts /></el-icon></div>
        <div class="stat-main">
          <span class="stat-value">{{ animatedStats.activeUsers }}</span>
          <span class="stat-label">7日活跃</span>
        </div>
        <div class="stat-footer">
          <span class="stat-badge neutral">
            {{ stats.userStats.totalUsers > 0 ? ((stats.userStats.activeUsers / stats.userStats.totalUsers) * 100).toFixed(0) : 0 }}% 活跃率
          </span>
        </div>
      </div>
    </div>

    <!-- 图表行 1 -->
    <el-row :gutter="16" class="chart-row">
      <el-col :span="14">
        <el-card class="chart-card" shadow="hover">
          <template #header>
            <div class="card-header">
              <div class="card-title">
                <span class="title-dot" style="background: #667eea" />
                <span>用户增长趋势</span>
              </div>
              <el-radio-group v-model="userGrowthDays" size="small" @change="loadUserGrowth">
                <el-radio-button :value="7">7天</el-radio-button>
                <el-radio-button :value="30">30天</el-radio-button>
              </el-radio-group>
            </div>
          </template>
          <div ref="userGrowthChart" class="chart" />
        </el-card>
      </el-col>

      <el-col :span="10">
        <el-card class="chart-card" shadow="hover">
          <template #header>
            <div class="card-header">
              <div class="card-title">
                <span class="title-dot" style="background: #91cc75" />
                <span>用户构成</span>
              </div>
            </div>
          </template>
          <div ref="memberChart" class="chart" />
        </el-card>
      </el-col>
    </el-row>

    <!-- 图表行 2 -->
    <el-row :gutter="16" class="chart-row">
      <el-col :span="14">
        <el-card class="chart-card" shadow="hover">
          <template #header>
            <div class="card-header">
              <div class="card-title">
                <span class="title-dot" style="background: #4facfe" />
                <span>AI调用趋势</span>
              </div>
              <el-radio-group v-model="aiUsageDays" size="small" @change="loadAIUsage">
                <el-radio-button :value="7">7天</el-radio-button>
                <el-radio-button :value="30">30天</el-radio-button>
              </el-radio-group>
            </div>
          </template>
          <div ref="aiUsageChart" class="chart" />
        </el-card>
      </el-col>

      <el-col :span="10">
        <el-card class="chart-card info-card" shadow="hover">
          <template #header>
            <div class="card-header">
              <div class="card-title">
                <span class="title-dot" style="background: #f5576c" />
                <span>运营概览</span>
              </div>
            </div>
          </template>
          <div class="info-grid">
            <div class="info-item">
              <div class="info-value">{{ formatResponseTime(stats.chatStats.avgResponseTime) }}</div>
              <div class="info-label">平均响应</div>
            </div>
            <div class="info-item">
              <div class="info-value">{{ stats.aiStats.avgTokens.toFixed(0) }}</div>
              <div class="info-label">平均Token</div>
            </div>
            <div class="info-item">
              <div class="info-value">{{ stats.memberStats.free }}</div>
              <div class="info-label">免费用户</div>
            </div>
            <div class="info-item">
              <div class="info-value">{{ stats.memberStats.vip }}</div>
              <div class="info-label">付费用户</div>
            </div>
            <div class="info-item">
              <div class="info-value">{{ stats.chatStats.yesterdayChats }}</div>
              <div class="info-label">昨日会话</div>
            </div>
            <div class="info-item">
              <div class="info-value">{{ stats.userStats.yesterdayNewUsers }}</div>
              <div class="info-label">昨日注册</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onUnmounted, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { User, ChatDotRound, Cpu, TrendCharts } from '@element-plus/icons-vue'
import { getDashboardStats, getUserGrowthTrend, getAIUsageTrend } from '@/services/admin'
import * as echarts from 'echarts/core'
import { LineChart, PieChart, BarChart } from 'echarts/charts'
import {
  TitleComponent,
  TooltipComponent,
  GridComponent,
  LegendComponent,
  GraphicComponent
} from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'

echarts.use([
  TitleComponent, TooltipComponent, GridComponent, LegendComponent, GraphicComponent,
  LineChart, BarChart, PieChart, CanvasRenderer
])

const stats = ref({
  userStats: { totalUsers: 0, todayNewUsers: 0, yesterdayNewUsers: 0, activeUsers: 0 },
  chatStats: { totalChats: 0, todayChats: 0, yesterdayChats: 0, avgResponseTime: 0 },
  aiStats: { totalCalls: 0, todayCalls: 0, successRate: 0, avgTokens: 0 },
  memberStats: { free: 0, vip: 0 }
})

const animatedStats = reactive({ totalUsers: 0, totalChats: 0, totalCalls: 0, activeUsers: 0 })

const userGrowthDays = ref(7)
const aiUsageDays = ref(7)
const userGrowthChart = ref(null)
const aiUsageChart = ref(null)
const memberChart = ref(null)
let userGrowthChartInstance = null
let aiUsageChartInstance = null
let memberChartInstance = null

const now = new Date()
const greeting = computed(() => {
  const h = now.getHours()
  if (h < 6) return '夜深了'
  if (h < 12) return '上午好'
  if (h < 14) return '中午好'
  if (h < 18) return '下午好'
  return '晚上好'
})
const todayStr = new Date().toLocaleDateString('zh-CN', {
  year: 'numeric', month: 'long', day: 'numeric', weekday: 'long'
})

const formatResponseTime = (ms) => {
  if (!ms || ms === 0) return '0ms'
  if (ms < 1000) return `${ms.toFixed(0)}ms`
  return `${(ms / 1000).toFixed(1)}s`
}

// 数字递增动画
const animateNumber = (key, target) => {
  const duration = 800
  const startTime = Date.now()
  const startVal = animatedStats[key]
  const step = () => {
    const elapsed = Date.now() - startTime
    const progress = Math.min(elapsed / duration, 1)
    const eased = 1 - Math.pow(1 - progress, 3)
    animatedStats[key] = Math.round(startVal + (target - startVal) * eased)
    if (progress < 1) requestAnimationFrame(step)
  }
  requestAnimationFrame(step)
}

watch(stats, (s) => {
  animateNumber('totalUsers', s.userStats.totalUsers)
  animateNumber('totalChats', s.chatStats.totalChats)
  animateNumber('totalCalls', s.aiStats.totalCalls)
  animateNumber('activeUsers', s.userStats.activeUsers)
}, { deep: true })

const loadStats = async () => {
  try {
    const { data } = await getDashboardStats()
    if (data.code === 200) {
      stats.value = data.data
      initMemberChart()
    }
  } catch (error) {
    console.error('加载统计数据失败:', error)
    ElMessage.error('加载统计数据失败')
  }
}

const loadUserGrowth = async () => {
  try {
    const { data } = await getUserGrowthTrend(userGrowthDays.value)
    if (data.code === 200) initUserGrowthChart(data.data)
  } catch (error) {
    console.error('加载用户增长趋势失败:', error)
  }
}

const loadAIUsage = async () => {
  try {
    const { data } = await getAIUsageTrend(aiUsageDays.value)
    if (data.code === 200) initAIUsageChart(data.data)
  } catch (error) {
    console.error('加载AI使用趋势失败:', error)
  }
}

const initUserGrowthChart = data => {
  if (!userGrowthChart.value) return
  if (!userGrowthChartInstance) userGrowthChartInstance = echarts.init(userGrowthChart.value)

  userGrowthChartInstance.setOption({
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(255,255,255,0.95)',
      borderColor: '#e5e7eb',
      borderWidth: 1,
      textStyle: { color: '#333' },
      formatter: p => `${p[0].axisValue}<br/>新增用户: <strong>${p[0].value}</strong>`
    },
    grid: { left: '3%', right: '4%', top: '8%', bottom: '3%', containLabel: true },
    xAxis: {
      type: 'category',
      data: data.map(d => d.date.slice(5)),
      boundaryGap: false,
      axisLine: { lineStyle: { color: '#e5e7eb' } },
      axisLabel: { color: '#8c8c8c', fontSize: 11 }
    },
    yAxis: {
      type: 'value', minInterval: 1,
      axisLine: { show: false }, axisTick: { show: false },
      axisLabel: { color: '#8c8c8c' },
      splitLine: { lineStyle: { color: '#f5f5f5', type: 'dashed' } }
    },
    series: [{
      type: 'line', data: data.map(d => d.count),
      smooth: true, symbol: 'circle', symbolSize: 7, showSymbol: data.length <= 15,
      areaStyle: {
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: 'rgba(102,126,234,0.35)' },
          { offset: 1, color: 'rgba(102,126,234,0.02)' }
        ])
      },
      itemStyle: { color: '#667eea', borderWidth: 2, borderColor: '#fff' },
      lineStyle: { width: 2.5, color: '#667eea' }
    }]
  })
}

const initAIUsageChart = data => {
  if (!aiUsageChart.value) return
  if (!aiUsageChartInstance) aiUsageChartInstance = echarts.init(aiUsageChart.value)

  aiUsageChartInstance.setOption({
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(255,255,255,0.95)',
      borderColor: '#e5e7eb', borderWidth: 1,
      textStyle: { color: '#333' },
      axisPointer: { type: 'shadow' }
    },
    legend: { data: ['总调用', '成功'], bottom: 0, textStyle: { color: '#8c8c8c', fontSize: 11 } },
    grid: { left: '3%', right: '4%', top: '8%', bottom: '14%', containLabel: true },
    xAxis: {
      type: 'category',
      data: data.map(d => d.date.slice(5)),
      axisLine: { lineStyle: { color: '#e5e7eb' } },
      axisLabel: { color: '#8c8c8c', fontSize: 11 }
    },
    yAxis: {
      type: 'value', minInterval: 1,
      axisLine: { show: false }, axisTick: { show: false },
      axisLabel: { color: '#8c8c8c' },
      splitLine: { lineStyle: { color: '#f5f5f5', type: 'dashed' } }
    },
    series: [
      {
        name: '总调用', type: 'bar', data: data.map(d => d.count),
        barMaxWidth: 24,
        itemStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: '#4facfe' }, { offset: 1, color: '#00f2fe' }
          ]),
          borderRadius: [4, 4, 0, 0]
        }
      },
      {
        name: '成功', type: 'bar', data: data.map(d => d.success),
        barMaxWidth: 24,
        itemStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: '#43e97b' }, { offset: 1, color: '#38f9d7' }
          ]),
          borderRadius: [4, 4, 0, 0]
        }
      }
    ]
  })
}

const initMemberChart = () => {
  if (!memberChart.value) return
  if (!memberChartInstance) memberChartInstance = echarts.init(memberChart.value)

  const total = stats.value.memberStats.free + stats.value.memberStats.vip
  memberChartInstance.setOption({
    tooltip: {
      trigger: 'item',
      backgroundColor: 'rgba(255,255,255,0.95)',
      borderColor: '#e5e7eb', borderWidth: 1,
      textStyle: { color: '#333' },
      formatter: '{b}: <strong>{c}</strong> ({d}%)'
    },
    legend: {
      orient: 'vertical', right: 20, top: 'center',
      textStyle: { color: '#666', fontSize: 13 },
      formatter: name => {
        const val = name === '免费用户' ? stats.value.memberStats.free : stats.value.memberStats.vip
        return `${name}  ${val}`
      }
    },
    graphic: [{
      type: 'text',
      left: '28%', top: '42%',
      style: {
        text: `${total}`,
        textAlign: 'center',
        fill: '#333', fontSize: 28, fontWeight: 700
      }
    }, {
      type: 'text',
      left: '28%', top: '55%',
      style: {
        text: '总用户',
        textAlign: 'center',
        fill: '#8c8c8c', fontSize: 13
      }
    }],
    series: [{
      type: 'pie', radius: ['55%', '78%'], center: ['30%', '50%'],
      avoidLabelOverlap: false,
      itemStyle: { borderRadius: 8, borderColor: '#fff', borderWidth: 3 },
      label: { show: false },
      emphasis: {
        label: { show: false },
        itemStyle: { shadowBlur: 10, shadowColor: 'rgba(0,0,0,0.15)' }
      },
      data: [
        { value: stats.value.memberStats.free, name: '免费用户', itemStyle: { color: '#91cc75' } },
        { value: stats.value.memberStats.vip, name: '付费用户', itemStyle: { color: '#667eea' } }
      ]
    }]
  })
}

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

<style scoped lang="scss">
.dashboard {
  padding: 0;
  font-family: 'Inter', sans-serif;
}

/* 欢迎区域 */
.welcome-section {
  margin-bottom: 20px;
  .welcome-text {
    h2 {
      margin: 0 0 4px;
      font-size: 22px;
      font-weight: 600;
      color: #0F172A;
      font-family: 'Calistoga', serif;
    }
    p {
      margin: 0;
      font-size: 13px;
      color: #0F172A;
      opacity: 0.6;
    }
  }
}

/* 统计卡片网格 */
.stat-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 20px;
}

.stat-card {
  position: relative;
  overflow: hidden;
  border: none;
  border-radius: 16px;
  padding: 20px;
  color: #fff;
  cursor: pointer;
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1), 0 2px 4px -2px rgba(0, 0, 0, 0.1);
  transition: transform 0.25s, box-shadow 0.25s;

  &:hover {
    transform: translateY(-3px);
    box-shadow: 0 10px 15px -3px rgba(0, 0, 0, 0.1), 0 4px 6px -4px rgba(0, 0, 0, 0.1);
  }

  .stat-card-bg {
    position: absolute;
    right: -8px;
    top: -8px;
    font-size: 72px;
    opacity: 0.12;
    transform: rotate(-12deg);
  }

  .stat-main {
    position: relative;
    z-index: 1;
    margin-bottom: 14px;
    .stat-value {
      display: block;
      font-size: 32px;
      font-weight: 700;
      line-height: 1.2;
      letter-spacing: -0.5px;
      font-family: 'Inter', sans-serif;
    }
    .stat-label {
      display: block;
      font-size: 13px;
      opacity: 0.85;
      margin-top: 2px;
      font-family: 'Inter', sans-serif;
    }
  }

  .stat-footer {
    position: relative;
    z-index: 1;
    display: flex;
    align-items: center;
    gap: 10px;
    font-size: 12px;

    .stat-badge {
      background: rgba(255, 255, 255, 0.2);
      padding: 2px 8px;
      border-radius: 10px;
      font-weight: 500;
      border: 1px solid rgba(255,255,255,0.3);
      &.up { background: rgba(255, 255, 255, 0.25); }
      &.warn { background: rgba(255, 200, 50, 0.3); }
      &.neutral { background: rgba(255, 255, 255, 0.15); }
    }
    .stat-extra {
      opacity: 0.7;
    }
  }
}

.stat-users { background: linear-gradient(135deg, #10B981 0%, #34D399 100%); }
.stat-chats { background: linear-gradient(135deg, #7c3aed 0%, #a78bfa 100%); }
.stat-ai { background: linear-gradient(135deg, #0891b2 0%, #22d3ee 100%); }
.stat-active { background: linear-gradient(135deg, #059669 0%, #34d399 100%); }

/* 图表区域 */
.chart-row {
  margin-bottom: 16px;
}

.chart-card {
  border: 1px solid #E2E8F0;
  border-radius: 12px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
  background: #FFFFFF;

  :deep(.el-card__header) {
    padding: 14px 20px;
    border-bottom: 1px solid #E2E8F0;
  }
  :deep(.el-card__body) {
    padding: 12px 16px;
  }
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;

  .card-title {
    display: flex;
    align-items: center;
    gap: 8px;
    font-size: 15px;
    font-weight: 600;
    color: #0F172A;
    font-family: 'Inter', sans-serif;

    .title-dot {
      width: 8px;
      height: 8px;
      border-radius: 50%;
      display: inline-block;
    }
  }
}

.chart {
  height: 280px;
}

/* 运营概览 */
.info-card {
  .info-grid {
    display: grid;
    grid-template-columns: repeat(2, 1fr);
    gap: 0;
    height: 280px;

    .info-item {
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      border-bottom: 1px solid #E2E8F0;
      border-right: 1px solid #E2E8F0;
      padding: 12px;

      &:nth-child(even) { border-right: none; }
      &:nth-last-child(-n+2) { border-bottom: none; }

      .info-value {
        font-size: 22px;
        font-weight: 700;
        color: #0F172A;
        line-height: 1.3;
        font-family: 'Inter', sans-serif;
      }
      .info-label {
        font-size: 12px;
        color: #0F172A;
        opacity: 0.55;
        margin-top: 4px;
        font-family: 'Inter', sans-serif;
      }
    }
  }
}

/* 响应式 */
@media (max-width: 1200px) {
  .stat-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
