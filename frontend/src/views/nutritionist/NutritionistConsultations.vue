<template>
  <div class="consultations-page">
    <h2>💬 我的咨询</h2>
    <el-skeleton :loading="loading" animated :rows="5">
      <el-empty v-if="!orders.length" description="暂无咨询记录" />
      <el-table v-else :data="orders" style="width: 100%">
        <el-table-column label="订单号" prop="orderNo" min-width="160" />
        <el-table-column label="金额" width="80" align="center">
          <template #default="{ row }">¥{{ row.amount }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)" size="small">{{ statusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="描述" min-width="200" show-overflow-tooltip>
          <template #default="{ row }">{{ row.description || '-' }}</template>
        </el-table-column>
        <el-table-column label="时间" width="150">
          <template #default="{ row }">{{ formatDate(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="100" align="center">
          <template #default="{ row }">
            <el-button
              v-if="row.status === 'IN_PROGRESS' || row.status === 'WAITING'"
              type="primary"
              size="small"
              text
              @click="$router.push(`/nutritionist/chat?order=${row.orderNo}`)"
            >
              聊天
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination
        v-if="total > pageSize"
        :current-page="currentPage"
        :page-size="pageSize"
        :total="total"
        layout="prev, pager, next"
        @current-change="handlePageChange"
        style="margin-top:16px; justify-content: center"
      />
    </el-skeleton>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getNutritionistConsultations } from '@/services/consultation'

const loading = ref(true)
const orders = ref([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = 20

onMounted(() => fetchOrders())

async function fetchOrders(page = 0) {
  loading.value = true
  try {
    const res = await getNutritionistConsultations(page, pageSize)
    if (res.data.code === 200) {
      orders.value = res.data.data?.content || []
      total.value = res.data.data?.totalElements || 0
    }
  } catch (e) {
    console.error('获取咨询列表失败', e)
  } finally {
    loading.value = false
  }
}

function handlePageChange(page) {
  currentPage.value = page
  fetchOrders(page - 1)
}

function statusType(s) {
  return { IN_PROGRESS: 'success', WAITING: 'warning', COMPLETED: '', CANCELLED: 'info' }[s] || 'info'
}

function statusText(s) {
  return { IN_PROGRESS: '进行中', WAITING: '等待中', COMPLETED: '已完成', CANCELLED: '已取消' }[s] || s
}

function formatDate(dt) {
  if (!dt) return '-'
  return new Date(dt).toLocaleString('zh-CN', { year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' })
}
</script>

<style scoped>
.consultations-page h2 {
  margin: 0 0 20px;
  font-size: 22px;
}
</style>
