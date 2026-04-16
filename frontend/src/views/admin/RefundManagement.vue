<template>
  <div class="refund-management">
    <!-- 工具栏 -->
    <el-card class="toolbar-card">
      <div class="toolbar">
        <div class="toolbar-left">
          <el-input v-model="keyword" placeholder="搜索订单号/用户ID" clearable style="width:220px"
            @keyup.enter="fetchList" @clear="fetchList">
            <template #prefix><el-icon><Search /></el-icon></template>
          </el-input>
          <el-select v-model="filterStatus" placeholder="状态筛选" clearable style="width:140px" @change="fetchList">
            <el-option label="待审核" value="PENDING" />
            <el-option label="已批准" value="APPROVED" />
            <el-option label="已拒绝" value="REJECTED" />
            <el-option label="已完成" value="COMPLETED" />
          </el-select>
        </div>
        <el-button @click="fetchList"><el-icon><Refresh /></el-icon> 刷新</el-button>
      </div>
    </el-card>

    <!-- 退款列表 -->
    <el-card class="list-card">
      <el-table :data="list" v-loading="loading" stripe>
        <el-table-column prop="orderNo" label="订单号" min-width="160" show-overflow-tooltip />
        <el-table-column label="退款类型" width="90" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="row.orderType === 'PRODUCT' ? 'primary' : 'success'">
              {{ row.orderType === 'PRODUCT' ? '产品订单' : '营养餐' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="userId" label="用户ID" width="80" align="center" />
        <el-table-column prop="refundAmount" label="退款金额" width="100" align="center">
          <template #default="{ row }">
            <span style="color:#f56c6c;font-weight:600">¥{{ row.refundAmount }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="reason" label="退款原因" min-width="150" show-overflow-tooltip />
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)" size="small">{{ getStatusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="adminRemark" label="审核备注" min-width="120" show-overflow-tooltip>
          <template #default="{ row }">{{ row.adminRemark || '-' }}</template>
        </el-table-column>
        <el-table-column label="申请时间" width="150">
          <template #default="{ row }">{{ formatDate(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right" align="center">
          <template #default="{ row }">
            <template v-if="row.status === 'PENDING'">
              <el-button type="success" size="small" text @click="handleApprove(row)">批准</el-button>
              <el-button type="danger" size="small" text @click="openRejectDialog(row)">拒绝</el-button>
            </template>
            <el-button v-if="row.status === 'APPROVED'" type="primary" size="small" text @click="handleComplete(row)">完成退款</el-button>
            <el-button type="info" size="small" text @click="showDetail(row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination">
        <el-pagination v-model:current-page="page" :page-size="pageSize" :total="total"
          layout="total, prev, pager, next" @current-change="fetchList" />
      </div>
    </el-card>

    <!-- 拒绝备注弹窗 -->
    <el-dialog v-model="rejectVisible" title="填写拒绝原因" width="420px" destroy-on-close>
      <el-form label-width="80px">
        <el-form-item label="拒绝原因">
          <el-input v-model="rejectRemark" type="textarea" rows="3" placeholder="请填写拒绝原因" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="rejectVisible = false">取消</el-button>
        <el-button type="danger" :loading="processing" @click="submitReject">确认拒绝</el-button>
      </template>
    </el-dialog>

    <!-- 详情弹窗 -->
    <el-dialog v-model="detailVisible" title="退款详情" width="540px">
      <el-descriptions :column="2" border v-if="current">
        <el-descriptions-item label="退款ID">{{ current.id }}</el-descriptions-item>
        <el-descriptions-item label="订单号">{{ current.orderNo }}</el-descriptions-item>
        <el-descriptions-item label="用户ID">{{ current.userId }}</el-descriptions-item>
        <el-descriptions-item label="退款金额">¥{{ current.refundAmount }}</el-descriptions-item>
        <el-descriptions-item label="退款类型" :span="2">{{ current.orderType === 'PRODUCT' ? '产品订单' : '营养餐订单' }}</el-descriptions-item>
        <el-descriptions-item label="退款原因" :span="2">{{ current.reason }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ getStatusLabel(current.status) }}</el-descriptions-item>
        <el-descriptions-item label="审核备注">{{ current.adminRemark || '-' }}</el-descriptions-item>
        <el-descriptions-item label="申请时间" :span="2">{{ formatDate(current.createdAt) }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Search, Refresh } from '@element-plus/icons-vue'
import api from '@/services/api'

const list = ref([])
const loading = ref(false)
const page = ref(1)
const pageSize = 10
const total = ref(0)
const keyword = ref('')
const filterStatus = ref('')
const rejectVisible = ref(false)
const rejectRemark = ref('')
const processing = ref(false)
const current = ref(null)
const detailVisible = ref(false)

function getStatusLabel(s) {
  const m = { PENDING: '待审核', APPROVED: '已批准', REJECTED: '已拒绝', COMPLETED: '已退款' }
  return m[s] || s
}
function getStatusType(s) {
  const m = { PENDING: 'warning', APPROVED: 'success', REJECTED: 'danger', COMPLETED: 'info' }
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
    const res = await api.get('/admin/refunds', { params })
    const data = res.data?.data
    list.value = data?.content || []
    total.value = data?.totalElements || 0
  } catch (e) { console.error(e) }
  finally { loading.value = false }
}

async function handleApprove(row) {
  try {
    await api.post(`/admin/refunds/${row.id}/approve`, { remark: '审核通过' })
    ElMessage.success('已批准退款')
    fetchList()
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '操作失败')
  }
}

function openRejectDialog(row) {
  current.value = row
  rejectRemark.value = ''
  rejectVisible.value = true
}

async function submitReject() {
  if (!rejectRemark.value.trim()) return ElMessage.warning('请填写拒绝原因')
  processing.value = true
  try {
    await api.post(`/admin/refunds/${current.value.id}/reject`, { remark: rejectRemark.value })
    ElMessage.success('已拒绝退款申请')
    rejectVisible.value = false
    fetchList()
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '操作失败')
  } finally { processing.value = false }
}

async function handleComplete(row) {
  try {
    await api.post(`/admin/refunds/${row.id}/complete`)
    ElMessage.success('退款已完成')
    fetchList()
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '操作失败')
  }
}

function showDetail(row) {
  current.value = row
  detailVisible.value = true
}

onMounted(fetchList)
</script>

<style scoped>
.refund-management { padding: 0; }
.toolbar-card { margin-bottom: 16px; }
.toolbar { display: flex; justify-content: space-between; align-items: center; gap: 12px; }
.toolbar-left { display: flex; gap: 12px; align-items: center; }
.pagination { margin-top: 16px; display: flex; justify-content: flex-end; }
</style>
