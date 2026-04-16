<template>
  <div class="meal-order-management">
    <!-- 工具栏 -->
    <el-card class="toolbar-card">
      <div class="toolbar">
        <div class="toolbar-left">
          <el-input v-model="keyword" placeholder="搜索订单号/用户" clearable style="width:220px"
            @keyup.enter="fetchList" @clear="fetchList">
            <template #prefix><el-icon><Search /></el-icon></template>
          </el-input>
          <el-select v-model="filterStatus" placeholder="状态筛选" clearable style="width:160px" @change="fetchList">
            <el-option v-for="s in statusOptions" :key="s.value" :label="s.label" :value="s.value" />
          </el-select>
        </div>
        <div class="toolbar-right">
          <el-button @click="fetchList"><el-icon><Refresh /></el-icon> 刷新</el-button>
        </div>
      </div>
    </el-card>

    <!-- 统计卡片 -->
    <div class="stats-row">
      <div v-for="s in stats" :key="s.label" class="stat-card">
        <div class="stat-num" :style="{ color: s.color }">{{ s.value }}</div>
        <div class="stat-label">{{ s.label }}</div>
      </div>
    </div>

    <!-- 订单列表 -->
    <el-card class="list-card">
      <el-table :data="list" v-loading="loading" stripe>
        <el-table-column prop="orderNo" label="订单号" min-width="160" show-overflow-tooltip />
        <el-table-column prop="userId" label="用户ID" width="80" align="center" />
        <el-table-column label="餐品" min-width="140" show-overflow-tooltip>
          <template #default="{ row }">
            <div>{{ row.mealName || row.meal?.name || '-' }}</div>
            <div style="color:#999;font-size:12px">x{{ row.quantity }}</div>
          </template>
        </el-table-column>
        <el-table-column prop="totalAmount" label="金额" width="90" align="center">
          <template #default="{ row }">¥{{ row.totalAmount }}</template>
        </el-table-column>
        <el-table-column label="取餐码" width="90" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.pickupCode" type="success" size="small">{{ row.pickupCode }}</el-tag>
            <span v-else style="color:#ccc">-</span>
          </template>
        </el-table-column>
        <el-table-column label="订单状态" width="110" align="center">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.orderStatus)" size="small">{{ getStatusLabel(row.orderStatus) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="支付状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.paymentStatus === 'PAID' ? 'success' : 'warning'" size="small">
              {{ row.paymentStatus === 'PAID' ? '已支付' : '待支付' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="取餐时间" width="110" show-overflow-tooltip>
          <template #default="{ row }">{{ row.pickupTime || '-' }}</template>
        </el-table-column>
        <el-table-column label="下单时间" width="150">
          <template #default="{ row }">{{ formatDate(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right" align="center">
          <template #default="{ row }">
            <el-button type="primary" size="small" text @click="showDetail(row)">详情</el-button>
            <el-button v-if="row.orderStatus === 'PAID'" type="success" size="small" text
              @click="verifyPickup(row)">核销</el-button>
            <el-dropdown v-if="['PENDING_PAYMENT','PAID'].includes(row.orderStatus)"
              trigger="click" @command="(cmd) => updateStatus(row, cmd)">
              <el-button type="warning" size="small" text>更多<el-icon><ArrowDown /></el-icon></el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="COMPLETED">完成订单</el-dropdown-item>
                  <el-dropdown-item command="CANCELLED">取消订单</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination">
        <el-pagination v-model:current-page="page" :page-size="pageSize" :total="total"
          layout="total, prev, pager, next" @current-change="fetchList" />
      </div>
    </el-card>

    <!-- 详情弹窗 -->
    <el-dialog v-model="detailVisible" title="订单详情" width="600px">
      <el-descriptions :column="2" border v-if="current">
        <el-descriptions-item label="订单号">{{ current.orderNo }}</el-descriptions-item>
        <el-descriptions-item label="用户ID">{{ current.userId }}</el-descriptions-item>
        <el-descriptions-item label="餐品名称">{{ current.mealName }}</el-descriptions-item>
        <el-descriptions-item label="数量">{{ current.quantity }}</el-descriptions-item>
        <el-descriptions-item label="总金额">¥{{ current.totalAmount }}</el-descriptions-item>
        <el-descriptions-item label="取餐码">{{ current.pickupCode || '-' }}</el-descriptions-item>
        <el-descriptions-item label="取餐地点">{{ current.pickupLocation || '-' }}</el-descriptions-item>
        <el-descriptions-item label="取餐时间">{{ current.pickupTime || '-' }}</el-descriptions-item>
        <el-descriptions-item label="订单状态">{{ getStatusLabel(current.orderStatus) }}</el-descriptions-item>
        <el-descriptions-item label="支付状态">{{ current.paymentStatus === 'PAID' ? '已支付' : '待支付' }}</el-descriptions-item>
        <el-descriptions-item label="备注" :span="2">{{ current.remark || '-' }}</el-descriptions-item>
        <el-descriptions-item label="下单时间" :span="2">{{ formatDate(current.createdAt) }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>

    <!-- 核销弹窗 -->
    <el-dialog v-model="verifyVisible" title="核验取餐码" width="400px">
      <el-form>
        <el-form-item label="输入取餐码">
          <el-input v-model="inputCode" placeholder="请输入6位取餐码" maxlength="6" />
        </el-form-item>
        <div v-if="verifyResult" :class="verifyResult.success ? 'verify-ok' : 'verify-fail'">
          {{ verifyResult.message }}
        </div>
      </el-form>
      <template #footer>
        <el-button @click="verifyVisible = false">取消</el-button>
        <el-button type="primary" :loading="verifying" @click="submitVerify">核销</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { Search, Refresh, ArrowDown } from '@element-plus/icons-vue'
import axios from 'axios'

const list = ref([])
const loading = ref(false)
const page = ref(1)
const pageSize = 10
const total = ref(0)
const keyword = ref('')
const filterStatus = ref('')
const detailVisible = ref(false)
const current = ref(null)
const verifyVisible = ref(false)
const verifyResult = ref(null)
const inputCode = ref('')
const verifying = ref(false)
const statsData = ref({ pending: 0, paid: 0, completed: 0, cancelled: 0 })

const statusOptions = [
  { value: 'PENDING_PAYMENT', label: '待支付' },
  { value: 'PAID', label: '已支付' },
  { value: 'COMPLETED', label: '已完成' },
  { value: 'CANCELLED', label: '已取消' },
]

const stats = computed(() => [
  { label: '待支付', value: statsData.value.pending, color: '#f59e0b' },
  { label: '已支付待取餐', value: statsData.value.paid, color: '#3b82f6' },
  { label: '已完成', value: statsData.value.completed, color: '#10b981' },
  { label: '已取消', value: statsData.value.cancelled, color: '#ef4444' },
])

function getStatusLabel(s) {
  const m = { PENDING_PAYMENT: '待支付', PAID: '已支付', COMPLETED: '已完成', CANCELLED: '已取消' }
  return m[s] || s
}
function getStatusType(s) {
  const m = { PENDING_PAYMENT: 'warning', PAID: 'success', COMPLETED: '', CANCELLED: 'danger' }
  return m[s] || 'info'
}
function formatDate(dt) {
  if (!dt) return '-'
  return dt.substring(0, 16).replace('T', ' ')
}

async function fetchList() {
  loading.value = true
  try {
    const params = { page: page.value - 1, size: pageSize }
    if (filterStatus.value) params.status = filterStatus.value
    if (keyword.value) params.keyword = keyword.value
    const res = await axios.get('/api/admin/meal-orders', { params })
    const data = res.data?.data
    list.value = data?.content || []
    total.value = data?.totalElements || 0

    // fetch stats
    const sr = await axios.get('/api/admin/meal-orders/stats')
    if (sr.data?.data) {
      statsData.value = sr.data.data
    }
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

function showDetail(row) {
  current.value = row
  detailVisible.value = true
}

function verifyPickup(row) {
  current.value = row
  inputCode.value = ''
  verifyResult.value = null
  verifyVisible.value = true
}

async function submitVerify() {
  if (!inputCode.value) return ElMessage.warning('请输入取餐码')
  verifying.value = true
  try {
    const res = await axios.post(`/api/admin/meal-orders/verify-pickup`, {
      pickupCode: inputCode.value
    })
    if (res.data?.code === 200) {
      verifyResult.value = { success: true, message: '核销成功！' }
      ElMessage.success('核销成功')
      fetchList()
    } else {
      verifyResult.value = { success: false, message: res.data?.message || '核销失败' }
    }
  } catch (e) {
    verifyResult.value = { success: false, message: e.response?.data?.message || '取餐码错误' }
  } finally {
    verifying.value = false
  }
}

async function updateStatus(row, newStatus) {
  try {
    await axios.put(`/api/admin/meal-orders/${row.orderNo}/status`, { status: newStatus })
    ElMessage.success('状态更新成功')
    fetchList()
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '操作失败')
  }
}

onMounted(fetchList)
</script>

<style scoped>
.meal-order-management { padding: 0; }
.toolbar-card { margin-bottom: 16px; }
.toolbar { display: flex; justify-content: space-between; align-items: center; gap: 12px; }
.toolbar-left { display: flex; gap: 12px; align-items: center; }
.stats-row { display: flex; gap: 12px; margin-bottom: 16px; }
.stat-card { flex: 1; background: #fff; border-radius: 8px; padding: 16px; text-align: center; box-shadow: 0 1px 4px rgba(0,0,0,.08); }
.stat-num { font-size: 28px; font-weight: 700; }
.stat-label { font-size: 13px; color: #666; margin-top: 4px; }
.list-card { }
.pagination { margin-top: 16px; display: flex; justify-content: flex-end; }
.verify-ok { background: #f0fdf4; color: #16a34a; padding: 10px; border-radius: 6px; font-weight: 600; text-align: center; }
.verify-fail { background: #fef2f2; color: #dc2626; padding: 10px; border-radius: 6px; font-weight: 600; text-align: center; }
</style>
