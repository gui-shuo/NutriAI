<template>
  <div class="dashboard">
    <h2>🩺 工作台</h2>

    <div class="stats-row">
      <el-card class="stat-card">
        <div class="stat-num">{{ activeCount }}</div>
        <div class="stat-label">进行中的咨询</div>
      </el-card>
      <el-card class="stat-card">
        <div class="stat-num">{{ todayCount }}</div>
        <div class="stat-label">今日咨询</div>
      </el-card>
    </div>

    <h3>📋 待处理咨询</h3>
    <el-skeleton :loading="loading" animated :rows="3">
      <el-empty v-if="!activeOrders.length" description="暂无待处理咨询" />
      <div v-else class="order-list">
        <el-card v-for="o in activeOrders" :key="o.id" class="order-card" shadow="hover">
          <div class="order-info">
            <div>
              <strong>订单 {{ o.orderNo }}</strong>
              <el-tag :type="o.status === 'IN_PROGRESS' ? 'success' : 'warning'" size="small" style="margin-left:8px">
                {{ o.status === 'IN_PROGRESS' ? '咨询中' : '等待中' }}
              </el-tag>
              <p class="order-desc">{{ o.description || '暂无描述' }}</p>
              <p class="order-time">{{ formatDate(o.createdAt) }}</p>
            </div>
            <el-button type="primary" @click="$router.push(`/nutritionist/chat/${o.orderNo}`)">
              进入聊天
            </el-button>
          </div>
        </el-card>
      </div>
    </el-skeleton>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { getNutritionistActiveConsultations, getNutritionistConsultations } from '@/services/consultation'

const loading = ref(true)
const activeOrders = ref([])
const totalOrders = ref([])

const activeCount = computed(() => activeOrders.value.length)
const todayCount = computed(() => {
  const today = new Date().toISOString().slice(0, 10)
  return totalOrders.value.filter(o => o.createdAt?.startsWith(today)).length
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
  max-width: 900px;

  h2 { margin: 0 0 24px; font-size: 22px; }
  h3 { margin: 24px 0 16px; font-size: 18px; }
}

.stats-row {
  display: flex;
  gap: 16px;
  margin-bottom: 8px;
}

.stat-card {
  flex: 1;
  text-align: center;

  .stat-num { font-size: 32px; font-weight: 700; color: #667eea; }
  .stat-label { font-size: 14px; color: #6b7280; margin-top: 4px; }
}

.order-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.order-card {
  .order-info {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }

  .order-desc {
    margin: 8px 0 4px;
    color: #6b7280;
    font-size: 13px;
  }

  .order-time {
    margin: 0;
    color: #9ca3af;
    font-size: 12px;
  }
}
</style>
