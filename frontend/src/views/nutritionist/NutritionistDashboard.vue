<template>
  <div class="dashboard">
    <!-- Profile Summary -->
    <div class="profile-summary">
      <div class="profile-left">
        <h2>👋 {{ greeting }}，{{ profile?.name || '营养师' }}</h2>
        <p class="profile-subtitle">{{ profile?.title }} · {{ profile?.specialties?.join('、') || '暂无专业领域' }}</p>
      </div>
      <div class="profile-stats">
        <div class="mini-stat">
          <span class="label">评分</span>
          <span class="value">{{ profile?.rating > 0 ? profile.rating : '暂无' }}</span>
        </div>
        <div class="mini-stat">
          <span class="label">总咨询</span>
          <span class="value">{{ profile?.consultationCount || 0 }}</span>
        </div>
      </div>
    </div>

    <!-- Stats Cards -->
    <div class="stats-row">
      <el-card class="stat-card active-card">
        <div class="stat-icon">💬</div>
        <div class="stat-num">{{ activeCount }}</div>
        <div class="stat-label">进行中咨询</div>
      </el-card>
      <el-card class="stat-card today-card">
        <div class="stat-icon">📋</div>
        <div class="stat-num">{{ todayCount }}</div>
        <div class="stat-label">今日咨询</div>
      </el-card>
      <el-card class="stat-card completed-card">
        <div class="stat-icon">✅</div>
        <div class="stat-num">{{ completedCount }}</div>
        <div class="stat-label">已完成</div>
      </el-card>
      <el-card class="stat-card fee-card">
        <div class="stat-icon">💰</div>
        <div class="stat-num">¥{{ profile?.consultationFee || 0 }}</div>
        <div class="stat-label">咨询费/次</div>
      </el-card>
    </div>

    <!-- Active Consultations -->
    <div class="section">
      <div class="section-header">
        <h3>📋 待处理咨询</h3>
        <el-button text type="primary" @click="$router.push('/nutritionist/chat')" v-if="activeOrders.length">
          前往消息中心 →
        </el-button>
      </div>
      <el-skeleton :loading="loading" animated :rows="3">
        <el-empty v-if="!activeOrders.length" description="暂无待处理咨询，休息一下吧 ☕" :image-size="80" />
        <div v-else class="order-list">
          <el-card v-for="o in activeOrders" :key="o.id" class="order-card" shadow="hover" @click="$router.push(`/nutritionist/chat?order=${o.orderNo}`)">
            <div class="order-info">
              <div class="order-left">
                <div class="order-top">
                  <strong>{{ o.description || '用户咨询' }}</strong>
                  <el-tag :type="o.status === 'IN_PROGRESS' ? 'success' : 'warning'" size="small">
                    {{ o.status === 'IN_PROGRESS' ? '咨询中' : '等待中' }}
                  </el-tag>
                </div>
                <p class="order-meta">
                  订单 {{ o.orderNo?.slice(-8) }} · {{ formatDate(o.createdAt) }}
                  · {{ (o.messages || []).length }} 条消息
                </p>
              </div>
              <el-button type="primary" size="small" round>
                回复
              </el-button>
            </div>
          </el-card>
        </div>
      </el-skeleton>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { getNutritionistActiveConsultations, getNutritionistConsultations } from '@/services/consultation'

const props = defineProps({ profile: Object })

const loading = ref(true)
const activeOrders = ref([])
const totalOrders = ref([])

const activeCount = computed(() => activeOrders.value.length)
const completedCount = computed(() => totalOrders.value.filter(o => o.status === 'COMPLETED').length)
const todayCount = computed(() => {
  const today = new Date().toISOString().slice(0, 10)
  return totalOrders.value.filter(o => o.createdAt?.startsWith(today)).length
})

const greeting = computed(() => {
  const h = new Date().getHours()
  if (h < 6) return '夜深了'
  if (h < 12) return '早上好'
  if (h < 14) return '中午好'
  if (h < 18) return '下午好'
  return '晚上好'
})

onMounted(async () => {
  try {
    const [activeRes, allRes] = await Promise.all([
      getNutritionistActiveConsultations(),
      getNutritionistConsultations(0, 100)
    ])
    if (activeRes.data.code === 200) activeOrders.value = activeRes.data.data || []
    if (allRes.data.code === 200) totalOrders.value = allRes.data.data?.content || []
  } catch (e) {
    console.error('加载失败', e)
  } finally {
    loading.value = false
  }
})

function formatDate(dt) {
  if (!dt) return '-'
  return new Date(dt).toLocaleString('zh-CN', { month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' })
}
</script>

<style scoped lang="scss">
.dashboard {
  max-width: 960px;
  font-family: 'Inter', sans-serif;
}

.profile-summary {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  padding: 20px 24px;
  background: linear-gradient(135deg, #10B981, #34D399);
  border-radius: 16px;
  border: none;
  box-shadow: 0 4px 14px 0 rgba(16, 185, 129, 0.25);
  color: #FFFFFF;

  h2 { margin: 0 0 4px; font-size: 20px; font-family: 'Calistoga', cursive; }
  .profile-subtitle { margin: 0; font-size: 13px; opacity: 0.85; font-family: 'Inter', sans-serif; }

  .profile-stats {
    display: flex;
    gap: 24px;
  }

  .mini-stat {
    text-align: center;
    .label { display: block; font-size: 12px; opacity: 0.8; font-family: 'Inter', sans-serif; }
    .value { display: block; font-size: 20px; font-weight: 700; margin-top: 2px; font-family: 'Calistoga', cursive; }
  }
}

.stats-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 24px;
}

.stat-card {
  text-align: center;
  border: 1px solid #E2E8F0;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
  transition: all 0.2s ease;
  background: #FFFFFF;

  &:hover {
    box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1), 0 2px 4px -2px rgba(0, 0, 0, 0.1);
    transform: translateY(-2px);
  }

  :deep(.el-card__body) {
    border-radius: 0;
  }

  .stat-icon { font-size: 28px; margin-bottom: 8px; }
  .stat-num { font-size: 28px; font-weight: 700; color: #10B981; font-family: 'Calistoga', cursive; }
  .stat-label { font-size: 13px; color: #0F172A; margin-top: 4px; font-family: 'Inter', sans-serif; }
}

:deep(.el-card) {
  border-radius: 16px;
  overflow: hidden;
}

.section {
  .section-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 16px;
    h3 { margin: 0; font-size: 18px; color: #0F172A; font-family: 'Calistoga', cursive; }
  }
}

.order-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.order-card {
  cursor: pointer;
  transition: all 0.2s ease;
  border: 1px solid #E2E8F0;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
  background: #FFFFFF;

  &:hover {
    box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1), 0 2px 4px -2px rgba(0, 0, 0, 0.1);
    transform: translateY(-2px);
  }

  .order-info {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }

  .order-top {
    display: flex;
    align-items: center;
    gap: 8px;
    strong { font-size: 15px; font-family: 'Inter', sans-serif; color: #0F172A; }
  }

  .order-meta {
    margin: 6px 0 0;
    color: #0F172A;
    font-size: 12px;
    font-family: 'Inter', sans-serif;
    opacity: 0.6;
  }
}
</style>
