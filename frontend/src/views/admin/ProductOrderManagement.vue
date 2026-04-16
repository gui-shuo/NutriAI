<template>
  <div class="product-order-management">
    <!-- 工具栏 -->
    <el-card class="toolbar-card">
      <div class="toolbar">
        <div class="toolbar-left">
          <el-input v-model="keyword" placeholder="搜索订单号/收货人/手机" clearable style="width:240px"
            @keyup.enter="fetchList" @clear="fetchList">
            <template #prefix><el-icon><Search /></el-icon></template>
          </el-input>
          <el-select v-model="filterStatus" placeholder="状态筛选" clearable style="width:160px" @change="fetchList">
            <el-option v-for="s in statusOptions" :key="s.value" :label="s.label" :value="s.value" />
          </el-select>
        </div>
        <el-button @click="fetchList"><el-icon><Refresh /></el-icon> 刷新</el-button>
      </div>
    </el-card>

    <!-- 订单列表 -->
    <el-card class="list-card">
      <el-table :data="list" v-loading="loading" stripe>
        <el-table-column prop="orderNo" label="订单号" min-width="160" show-overflow-tooltip />
        <el-table-column prop="userId" label="用户ID" width="80" align="center" />
        <el-table-column label="收货人" width="110">
          <template #default="{ row }">
            <div>{{ row.receiverName }}</div>
            <div style="color:#999;font-size:12px">{{ row.receiverPhone }}</div>
          </template>
        </el-table-column>
        <el-table-column prop="totalAmount" label="金额" width="90" align="center">
          <template #default="{ row }">
            <span style="color:#f56c6c;font-weight:600">¥{{ row.totalAmount }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="totalQuantity" label="数量" width="70" align="center" />
        <el-table-column label="配送方式" width="90" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="row.fulfillmentType === 'SELF_PICKUP' ? 'success' : 'primary'">
              {{ row.fulfillmentType === 'SELF_PICKUP' ? '自提' : '快递' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="订单状态" width="110" align="center">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.orderStatus)" size="small">{{ getStatusLabel(row.orderStatus) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="物流" min-width="120" show-overflow-tooltip>
          <template #default="{ row }">
            <span v-if="row.trackingCompany">{{ row.trackingCompany }} {{ row.trackingNo }}</span>
            <span v-else style="color:#ccc">-</span>
          </template>
        </el-table-column>
        <el-table-column label="下单时间" width="150">
          <template #default="{ row }">{{ formatDate(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right" align="center">
          <template #default="{ row }">
            <el-button type="primary" size="small" text @click="showDetail(row)">详情</el-button>
            <el-button v-if="row.orderStatus === 'PAID'" type="success" size="small" text @click="openShipDialog(row)">发货</el-button>
            <el-dropdown trigger="click" @command="(cmd) => handleAction(row, cmd)">
              <el-button type="warning" size="small" text>更多<el-icon><ArrowDown /></el-icon></el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item v-if="row.orderStatus === 'SHIPPED'" command="COMPLETED">确认收货</el-dropdown-item>
                  <el-dropdown-item v-if="['PENDING_PAYMENT','PAID'].includes(row.orderStatus)" command="CANCELLED">取消订单</el-dropdown-item>
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
    <el-dialog v-model="detailVisible" title="订单详情" width="700px" destroy-on-close>
      <el-descriptions :column="2" border v-if="current">
        <el-descriptions-item label="订单号" :span="2">{{ current.orderNo }}</el-descriptions-item>
        <el-descriptions-item label="用户ID">{{ current.userId }}</el-descriptions-item>
        <el-descriptions-item label="总金额">¥{{ current.totalAmount }}</el-descriptions-item>
        <el-descriptions-item label="收货人">{{ current.receiverName }}</el-descriptions-item>
        <el-descriptions-item label="手机">{{ current.receiverPhone }}</el-descriptions-item>
        <el-descriptions-item label="地址" :span="2">{{ current.receiverAddress }}</el-descriptions-item>
        <el-descriptions-item label="物流公司">{{ current.trackingCompany || '-' }}</el-descriptions-item>
        <el-descriptions-item label="快递单号">{{ current.trackingNo || '-' }}</el-descriptions-item>
        <el-descriptions-item label="订单状态">{{ getStatusLabel(current.orderStatus) }}</el-descriptions-item>
        <el-descriptions-item label="支付状态">{{ current.paymentStatus }}</el-descriptions-item>
        <el-descriptions-item label="配送方式">{{ current.fulfillmentType === 'SELF_PICKUP' ? '自提' : '快递' }}</el-descriptions-item>
        <el-descriptions-item label="下单时间">{{ formatDate(current.createdAt) }}</el-descriptions-item>
        <el-descriptions-item label="备注" :span="2">{{ current.remark || '-' }}</el-descriptions-item>
      </el-descriptions>

      <div v-if="current?.items?.length" style="margin-top:16px">
        <div style="font-weight:600;margin-bottom:8px">商品明细</div>
        <el-table :data="current.items" size="small" stripe>
          <el-table-column prop="productName" label="商品" min-width="140" />
          <el-table-column prop="price" label="单价" width="80" align="center">
            <template #default="{ row }">¥{{ row.price }}</template>
          </el-table-column>
          <el-table-column prop="quantity" label="数量" width="70" align="center" />
          <el-table-column prop="subtotal" label="小计" width="90" align="center">
            <template #default="{ row }">¥{{ row.subtotal }}</template>
          </el-table-column>
        </el-table>
      </div>
    </el-dialog>

    <!-- 发货弹窗 -->
    <el-dialog v-model="shipVisible" title="填写发货信息" width="420px" destroy-on-close>
      <el-form :model="shipForm" label-width="80px">
        <el-form-item label="物流公司">
          <el-select v-model="shipForm.trackingCompany" style="width:100%">
            <el-option v-for="c in carriers" :key="c" :label="c" :value="c" />
          </el-select>
        </el-form-item>
        <el-form-item label="快递单号">
          <el-input v-model="shipForm.trackingNo" placeholder="请输入快递单号" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="shipVisible = false">取消</el-button>
        <el-button type="primary" :loading="shipping" @click="submitShip">确认发货</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
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
const shipVisible = ref(false)
const shipping = ref(false)
const shipForm = ref({ trackingCompany: '', trackingNo: '' })

const carriers = ['顺丰速运', '中通快递', '圆通快递', '韵达快递', '申通快递', '邮政EMS', '京东快递', '德邦物流']

const statusOptions = [
  { value: 'PENDING_PAYMENT', label: '待支付' },
  { value: 'PAID', label: '待发货' },
  { value: 'SHIPPED', label: '已发货' },
  { value: 'COMPLETED', label: '已完成' },
  { value: 'CANCELLED', label: '已取消' },
]

function getStatusLabel(s) {
  const m = { PENDING_PAYMENT: '待支付', PAID: '待发货', SHIPPED: '已发货', COMPLETED: '已完成', CANCELLED: '已取消' }
  return m[s] || s
}
function getStatusType(s) {
  const m = { PENDING_PAYMENT: 'warning', PAID: 'primary', SHIPPED: 'success', COMPLETED: '', CANCELLED: 'danger' }
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
    const res = await axios.get('/api/admin/product-orders', { params })
    const data = res.data?.data
    list.value = data?.content || []
    total.value = data?.totalElements || 0
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

function openShipDialog(row) {
  current.value = row
  shipForm.value = { trackingCompany: '', trackingNo: '' }
  shipVisible.value = true
}

async function submitShip() {
  if (!shipForm.value.trackingNo.trim()) return ElMessage.warning('请填写快递单号')
  shipping.value = true
  try {
    await axios.put(`/api/admin/product-orders/${current.value.orderNo}/status`, {
      status: 'SHIPPED',
      trackingCompany: shipForm.value.trackingCompany,
      trackingNo: shipForm.value.trackingNo
    })
    ElMessage.success('发货成功')
    shipVisible.value = false
    fetchList()
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '发货失败')
  } finally {
    shipping.value = false
  }
}

async function handleAction(row, cmd) {
  try {
    await axios.put(`/api/admin/product-orders/${row.orderNo}/status`, { status: cmd })
    ElMessage.success('操作成功')
    fetchList()
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '操作失败')
  }
}

onMounted(fetchList)
</script>

<style scoped>
.product-order-management { padding: 0; }
.toolbar-card { margin-bottom: 16px; }
.toolbar { display: flex; justify-content: space-between; align-items: center; gap: 12px; }
.toolbar-left { display: flex; gap: 12px; align-items: center; }
.list-card { }
.pagination { margin-top: 16px; display: flex; justify-content: flex-end; }
</style>
