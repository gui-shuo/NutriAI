<template>
  <div class="consultation-view">
    <!-- 顶部导航 -->
    <nav class="top-nav">
      <div class="nav-inner">
        <div class="nav-left">
          <el-button text @click="$router.push('/')">
            <el-icon><ArrowLeft /></el-icon>
            返回首页
          </el-button>
          <h2 class="page-title">🩺 营养师在线咨询</h2>
        </div>
        <el-button type="primary" text @click="showMyOrders">
          <el-icon><Document /></el-icon>
          我的咨询
        </el-button>
      </div>
    </nav>

    <main class="main-area">
      <el-alert type="info" :closable="true" show-icon style="margin-bottom: 16px">
        <template #title>
          本平台咨询服务仅供参考，不构成医疗诊断或治疗方案。如有疾病请及时就医。
          <router-link to="/legal/disclaimer" style="color:#409eff">详细声明</router-link>
        </template>
      </el-alert>
      <!-- 当前进行中的咨询 -->
      <section v-if="activeConsultation" class="active-consultation-section">
        <el-card class="active-card">
          <div class="active-info">
            <el-avatar :size="48" :src="activeNutritionist?.avatar" class="n-avatar">
              {{ activeConsultation.nutritionistName?.charAt(0) }}
            </el-avatar>
            <div class="active-detail">
              <h3>{{ activeConsultation.nutritionistName }}</h3>
              <el-tag type="success" size="small">咨询进行中</el-tag>
              <p class="active-desc">{{ activeConsultation.description || '营养咨询' }}</p>
            </div>
            <el-button type="primary" size="large" @click="enterChatRoom(activeConsultation.orderNo)">
              进入聊天室
            </el-button>
          </div>
        </el-card>
      </section>

      <!-- 营养师列表 -->
      <section v-else class="nutritionist-section">
        <div class="section-header">
          <h2>专业营养师团队</h2>
          <p class="section-desc">选择一位营养师开始在线咨询，获取个性化的营养指导方案</p>
        </div>

        <el-skeleton :loading="loading" animated :rows="6">
          <el-empty v-if="!nutritionists.length" description="暂无可用营养师" />
          <div v-else class="nutritionist-grid">
            <el-card
              v-for="n in nutritionists"
              :key="n.id"
              class="nutritionist-card"
              shadow="hover"
            >
              <div class="n-header">
                <el-avatar :size="64" :src="n.avatar" class="n-avatar">
                  {{ n.name?.charAt(0) }}
                </el-avatar>
                <el-tag
                  :type="
                    n.status === 'ONLINE' ? 'success' : n.status === 'BUSY' ? 'warning' : 'info'
                  "
                  size="small"
                  class="status-tag"
                >
                  {{ statusText(n.status) }}
                </el-tag>
              </div>
              <h3 class="n-name">{{ n.name }}</h3>
              <p class="n-title">{{ n.title }}</p>
              <div class="n-specialties">
                <el-tag
                  v-for="(s, i) in (n.specialties || []).slice(0, 3)"
                  :key="i"
                  size="small"
                  type="info"
                  effect="plain"
                >
                  {{ s }}
                </el-tag>
              </div>
              <p class="n-intro">{{ n.introduction }}</p>
              <div class="n-stats">
                <div class="stat">
                  <span class="stat-value">{{ n.experienceYears }}年</span>
                  <span class="stat-label">从业经验</span>
                </div>
                <div class="stat">
                  <span class="stat-value">
                    <el-icon color="#e6a23c"><Star /></el-icon>
                    {{ n.rating }}
                  </span>
                  <span class="stat-label">用户评分</span>
                </div>
                <div class="stat">
                  <span class="stat-value">{{ n.consultationCount }}</span>
                  <span class="stat-label">咨询次数</span>
                </div>
              </div>
              <div class="n-footer">
                <div class="n-fee">
                  <span class="fee-label">咨询费</span>
                  <span class="fee-value">¥{{ n.consultationFee }}/次</span>
                </div>
                <el-button
                  type="primary"
                  :disabled="n.status === 'OFFLINE'"
                  @click="openConsultDialog(n)"
                >
                  立即咨询
                </el-button>
              </div>
            </el-card>
          </div>
        </el-skeleton>
      </section>
    </main>

    <!-- 创建咨询 Dialog -->
    <el-dialog v-model="consultDialogVisible" title="预约咨询" width="520px">
      <div v-if="selectedNutritionist" class="consult-form">
        <div class="consult-info">
          <el-avatar :size="48" :src="selectedNutritionist.avatar" class="n-avatar">
            {{ selectedNutritionist.name?.charAt(0) }}
          </el-avatar>
          <div>
            <h4>{{ selectedNutritionist.name }}</h4>
            <p>{{ selectedNutritionist.title }}</p>
          </div>
          <span class="consult-fee">¥{{ selectedNutritionist.consultationFee }}</span>
        </div>
        <el-form label-position="top">
          <el-form-item label="咨询描述">
            <el-input
              v-model="consultForm.description"
              type="textarea"
              :rows="4"
              placeholder="请简要描述您的营养需求或饮食问题..."
              maxlength="500"
              show-word-limit
            />
          </el-form-item>
        </el-form>
        <el-alert type="info" :closable="false" show-icon style="margin-top: 8px">
          <template #title>模拟支付模式：点击确认后将自动完成支付</template>
        </el-alert>
      </div>
      <template #footer>
        <el-button @click="consultDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="createLoading" @click="handleCreateConsultation">
          确认预约并支付（模拟）
        </el-button>
      </template>
    </el-dialog>

    <!-- 我的咨询订单 Dialog -->
    <el-dialog v-model="ordersDialogVisible" title="我的咨询记录" width="780px">
      <el-skeleton :loading="ordersLoading" animated :rows="4">
        <el-empty v-if="!orders.length" description="暂无咨询记录" />
        <el-table v-else :data="orders" style="width: 100%">
          <el-table-column label="订单号" prop="orderNo" min-width="150" />
          <el-table-column label="营养师" prop="nutritionistName" width="100" />
          <el-table-column label="金额" width="80" align="center">
            <template #default="{ row }">¥{{ row.amount }}</template>
          </el-table-column>
          <el-table-column label="支付状态" width="90" align="center">
            <template #default="{ row }">
              <el-tag :type="payStatusType(row.paymentStatus)" size="small">
                {{ payStatusText(row.paymentStatus) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="咨询状态" width="90" align="center">
            <template #default="{ row }">
              <el-tag :type="consultStatusType(row.status)" size="small">
                {{ consultStatusText(row.status) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="时间" min-width="140">
            <template #default="{ row }">{{ formatDate(row.createdAt) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="140" align="center">
            <template #default="{ row }">
              <el-button
                v-if="row.status === 'IN_PROGRESS'"
                type="primary"
                size="small"
                text
                @click="enterChatRoom(row.orderNo)"
              >
                聊天室
              </el-button>
              <el-button
                v-if="row.paymentStatus === 'PAID' && row.status !== 'COMPLETED'"
                type="danger"
                size="small"
                text
                @click="handleRefund(row)"
              >
                退款
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-skeleton>
      <template #footer>
        <el-button @click="ordersDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ArrowLeft, Document, Star } from '@element-plus/icons-vue'
import {
  getNutritionists,
  createConsultation,
  simulatePayConsultation,
  simulateRefundConsultation,
  getConsultationHistory,
  getActiveConsultations
} from '@/services/consultation'
import message from '@/utils/message'

const router = useRouter()

// --- 状态 ---
const loading = ref(false)
const nutritionists = ref([])
const activeConsultation = ref(null)
const activeNutritionist = ref(null)

const consultDialogVisible = ref(false)
const selectedNutritionist = ref(null)
const consultForm = ref({ description: '' })
const createLoading = ref(false)

const ordersDialogVisible = ref(false)
const ordersLoading = ref(false)
const orders = ref([])

// --- 初始化 ---
onMounted(async () => {
  await Promise.all([fetchNutritionists(), fetchActiveConsultation()])
})

async function fetchNutritionists() {
  loading.value = true
  try {
    const res = await getNutritionists()
    if (res.data.code === 200) {
      nutritionists.value = res.data.data || []
    }
  } catch (e) {
    console.error('获取营养师列表失败', e)
  } finally {
    loading.value = false
  }
}

async function fetchActiveConsultation() {
  try {
    const res = await getActiveConsultations()
    if (res.data.code === 200) {
      const actives = res.data.data || []
      if (actives.length > 0) {
        activeConsultation.value = actives[0]
        activeNutritionist.value = nutritionists.value.find(n => n.id === actives[0].nutritionistId)
      }
    }
  } catch (e) {
    console.error('获取活跃咨询失败', e)
  }
}

function openConsultDialog(n) {
  selectedNutritionist.value = n
  consultForm.value = { description: '' }
  consultDialogVisible.value = true
}

async function handleCreateConsultation() {
  if (!selectedNutritionist.value) return
  createLoading.value = true
  try {
    // 1. 创建订单
    const res = await createConsultation(
      selectedNutritionist.value.id,
      consultForm.value.description
    )
    if (res.data.code !== 200) {
      message.error(res.data.message || '创建失败')
      return
    }
    const order = res.data.data

    // 2. 模拟支付
    const payRes = await simulatePayConsultation(order.orderNo)
    if (payRes.data.code === 200) {
      message.success('🎉 预约成功！咨询已开始')
      activeConsultation.value = payRes.data.data
      activeNutritionist.value = selectedNutritionist.value
      consultDialogVisible.value = false
    } else {
      message.error(payRes.data.message || '支付失败')
    }
  } catch (e) {
    console.error('创建咨询失败', e)
    message.error('操作失败，请重试')
  } finally {
    createLoading.value = false
  }
}

function enterChatRoom(orderNo) {
  router.push(`/consultation/chat/${orderNo}`)
}

async function showMyOrders() {
  ordersDialogVisible.value = true
  ordersLoading.value = true
  try {
    const res = await getConsultationHistory(0, 20)
    if (res.data.code === 200) {
      orders.value = res.data.data.content || []
    }
  } catch (e) {
    message.error('获取记录失败')
  } finally {
    ordersLoading.value = false
  }
}

async function handleRefund(row) {
  try {
    const res = await simulateRefundConsultation(row.orderNo)
    if (res.data.code === 200) {
      message.success('退款成功')
      showMyOrders()
    } else {
      message.error(res.data.message || '退款失败')
    }
  } catch (e) {
    message.error('退款操作失败')
  }
}

// --- 工具函数 ---

function statusText(s) {
  return { ONLINE: '在线', BUSY: '忙碌', OFFLINE: '离线' }[s] || s
}

function payStatusType(s) {
  return (
    { PAID: 'success', PENDING: 'warning', REFUNDED: 'danger', EXPIRED: 'info', CANCELLED: 'info' }[
      s
    ] || 'info'
  )
}

function payStatusText(s) {
  return (
    {
      PAID: '已支付',
      PENDING: '待支付',
      REFUNDED: '已退款',
      EXPIRED: '已超时',
      CANCELLED: '已取消'
    }[s] || s
  )
}

function consultStatusType(s) {
  return (
    { IN_PROGRESS: 'success', WAITING: 'warning', COMPLETED: '', CANCELLED: 'info' }[s] || 'info'
  )
}

function consultStatusText(s) {
  return (
    { IN_PROGRESS: '进行中', WAITING: '等待中', COMPLETED: '已完成', CANCELLED: '已取消' }[s] || s
  )
}

function formatDate(dt) {
  if (!dt) return '-'
  return new Date(dt).toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}
</script>

<style scoped lang="scss">
.consultation-view {
  min-height: 100vh;
  background: #f5f7fa;
}

.top-nav {
  background: #fff;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  position: sticky;
  top: 0;
  z-index: 100;

  .nav-inner {
    max-width: 1200px;
    margin: 0 auto;
    padding: 0 20px;
    display: flex;
    justify-content: space-between;
    align-items: center;
    height: 56px;
  }

  .nav-left {
    display: flex;
    align-items: center;
    gap: 12px;
  }

  .page-title {
    font-size: 18px;
    font-weight: 600;
    color: #1f2937;
    margin: 0;
  }
}

.main-area {
  max-width: 1200px;
  margin: 0 auto;
  padding: 24px 20px;
}

// === 营养师列表 ===
.section-header {
  text-align: center;
  margin-bottom: 32px;

  h2 {
    font-size: 28px;
    font-weight: 700;
    color: #1f2937;
    margin-bottom: 8px;
  }

  .section-desc {
    color: #6b7280;
    font-size: 15px;
  }
}

.nutritionist-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;

  @media (max-width: 960px) {
    grid-template-columns: repeat(2, 1fr);
  }
  @media (max-width: 640px) {
    grid-template-columns: 1fr;
  }
}

.nutritionist-card {
  border-radius: 12px;
  transition: transform 0.2s;

  &:hover {
    transform: translateY(-4px);
  }

  .n-header {
    display: flex;
    justify-content: space-between;
    align-items: flex-start;
    margin-bottom: 12px;
  }

  .n-avatar {
    background: linear-gradient(135deg, #667eea, #764ba2);
    color: #fff;
    font-weight: 600;
  }

  .n-name {
    font-size: 18px;
    font-weight: 600;
    margin: 0 0 4px;
  }

  .n-title {
    color: #6b7280;
    font-size: 13px;
    margin: 0 0 8px;
  }

  .n-specialties {
    display: flex;
    flex-wrap: wrap;
    gap: 4px;
    margin-bottom: 10px;
  }

  .n-intro {
    color: #4b5563;
    font-size: 13px;
    line-height: 1.5;
    margin: 0 0 12px;
    display: -webkit-box;
    -webkit-line-clamp: 3;
    -webkit-box-orient: vertical;
    overflow: hidden;
  }

  .n-stats {
    display: flex;
    justify-content: space-between;
    margin-bottom: 16px;
    padding: 10px 0;
    border-top: 1px solid #f3f4f6;
    border-bottom: 1px solid #f3f4f6;

    .stat {
      text-align: center;
      .stat-value {
        font-size: 15px;
        font-weight: 600;
        display: flex;
        align-items: center;
        justify-content: center;
        gap: 2px;
      }
      .stat-label {
        font-size: 12px;
        color: #9ca3af;
        display: block;
        margin-top: 2px;
      }
    }
  }

  .n-footer {
    display: flex;
    justify-content: space-between;
    align-items: center;

    .fee-label {
      font-size: 12px;
      color: #9ca3af;
    }
    .fee-value {
      font-size: 20px;
      font-weight: 700;
      color: #f56c6c;
    }
  }
}

// === 活跃咨询卡片 ===
.active-consultation-section {
  margin-bottom: 24px;
}

.active-card {
  border-radius: 12px;

  .active-info {
    display: flex;
    align-items: center;
    gap: 16px;

    .n-avatar {
      background: linear-gradient(135deg, #667eea, #764ba2);
      color: #fff;
      font-weight: 600;
    }

    .active-detail {
      flex: 1;

      h3 {
        margin: 0 0 4px;
        font-size: 18px;
        font-weight: 600;
      }

      .active-desc {
        margin: 8px 0 0;
        color: #6b7280;
        font-size: 13px;
      }
    }
  }
}

// === 预约表单 ===
.consult-form {
  .consult-info {
    display: flex;
    align-items: center;
    gap: 12px;
    padding: 16px;
    background: #f9fafb;
    border-radius: 8px;
    margin-bottom: 20px;

    h4 {
      margin: 0;
      font-size: 16px;
    }
    p {
      margin: 0;
      color: #6b7280;
      font-size: 13px;
    }
    .consult-fee {
      margin-left: auto;
      font-size: 22px;
      font-weight: 700;
      color: #f56c6c;
    }
  }
}
</style>
