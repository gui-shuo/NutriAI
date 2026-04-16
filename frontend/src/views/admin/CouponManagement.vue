<template>
  <div class="coupon-management">
    <!-- 工具栏 -->
    <el-card class="toolbar-card">
      <div class="toolbar">
        <div class="toolbar-left">
          <el-input v-model="keyword" placeholder="搜索优惠券名称" clearable style="width:220px"
            @keyup.enter="fetchList" @clear="fetchList">
            <template #prefix><el-icon><Search /></el-icon></template>
          </el-input>
          <el-select v-model="filterType" placeholder="类型筛选" clearable style="width:140px" @change="fetchList">
            <el-option label="满减券" value="REDUCE" />
            <el-option label="折扣券" value="DISCOUNT" />
          </el-select>
        </div>
        <el-button type="primary" @click="openForm()">
          <el-icon><Plus /></el-icon> 新建优惠券
        </el-button>
      </div>
    </el-card>

    <!-- 优惠券列表 -->
    <el-card class="list-card">
      <el-table :data="list" v-loading="loading" stripe>
        <el-table-column prop="name" label="名称" min-width="150" show-overflow-tooltip />
        <el-table-column label="类型" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.couponType === 'REDUCE' ? 'danger' : 'warning'" size="small">
              {{ row.couponType === 'REDUCE' ? '满减' : '折扣' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="优惠" width="120" align="center">
          <template #default="{ row }">
            <span v-if="row.couponType === 'REDUCE'" style="color:#f56c6c;font-weight:600">
              减¥{{ row.discountValue }}
            </span>
            <span v-else style="color:#e6a23c;font-weight:600">
              {{ (row.discountValue * 10).toFixed(1) }}折
            </span>
          </template>
        </el-table-column>
        <el-table-column label="最低消费" width="100" align="center">
          <template #default="{ row }">¥{{ row.minOrderAmount }}</template>
        </el-table-column>
        <el-table-column label="最大优惠" width="100" align="center">
          <template #default="{ row }">
            {{ row.maxDiscountAmount ? '¥' + row.maxDiscountAmount : '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="totalQuantity" label="总量" width="70" align="center" />
        <el-table-column prop="usedQuantity" label="已用" width="70" align="center" />
        <el-table-column label="有效期" min-width="200">
          <template #default="{ row }">
            {{ formatDate(row.startDate) }} ~ {{ formatDate(row.endDate) }}
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-switch v-model="row.isActive" size="small" @change="toggleActive(row)" />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="140" fixed="right" align="center">
          <template #default="{ row }">
            <el-button type="primary" size="small" text @click="openForm(row)">编辑</el-button>
            <el-popconfirm title="确定删除此优惠券？" @confirm="handleDelete(row.id)">
              <template #reference>
                <el-button type="danger" size="small" text>删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination">
        <el-pagination v-model:current-page="page" :page-size="pageSize" :total="total"
          layout="total, prev, pager, next" @current-change="fetchList" />
      </div>
    </el-card>

    <!-- 新建/编辑弹窗 -->
    <el-dialog v-model="formVisible" :title="isEdit ? '编辑优惠券' : '新建优惠券'" width="580px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="名称" prop="name">
          <el-input v-model="form.name" placeholder="如：新用户满50减10" />
        </el-form-item>
        <el-form-item label="类型" prop="couponType">
          <el-radio-group v-model="form.couponType">
            <el-radio value="REDUCE">满减券</el-radio>
            <el-radio value="DISCOUNT">折扣券</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="优惠值" prop="discountValue">
          <el-input-number v-model="form.discountValue" :min="0.01" :precision="2" style="width:150px" />
          <span style="margin-left:8px;color:#999;font-size:13px">
            {{ form.couponType === 'REDUCE' ? '元（直减金额）' : '（0.8=8折，0.5=5折）' }}
          </span>
        </el-form-item>
        <el-form-item label="最低消费" prop="minOrderAmount">
          <el-input-number v-model="form.minOrderAmount" :min="0" :precision="2" style="width:150px" />
          <span style="margin-left:8px;color:#999;font-size:13px">元（0=无门槛）</span>
        </el-form-item>
        <el-form-item label="最大优惠" v-if="form.couponType === 'DISCOUNT'">
          <el-input-number v-model="form.maxDiscountAmount" :min="0" :precision="2" style="width:150px" />
          <span style="margin-left:8px;color:#999;font-size:13px">元（0=不限）</span>
        </el-form-item>
        <el-form-item label="发行数量" prop="totalQuantity">
          <el-input-number v-model="form.totalQuantity" :min="1" style="width:150px" />
          <span style="margin-left:8px;color:#999;font-size:13px">张（-1=不限量）</span>
        </el-form-item>
        <el-form-item label="每人限领">
          <el-input-number v-model="form.perUserLimit" :min="1" style="width:150px" />
          <span style="margin-left:8px;color:#999;font-size:13px">张</span>
        </el-form-item>
        <el-form-item label="有效期" prop="startDate">
          <el-date-picker v-model="dateRange" type="daterange" start-placeholder="开始日期" end-placeholder="结束日期"
            value-format="YYYY-MM-DDTHH:mm:ss" style="width:100%" />
        </el-form-item>
        <el-form-item label="说明">
          <el-input v-model="form.description" type="textarea" rows="2" />
        </el-form-item>
        <el-form-item label="启用">
          <el-switch v-model="form.isActive" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitForm">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { Search, Plus } from '@element-plus/icons-vue'
import axios from 'axios'

const list = ref([])
const loading = ref(false)
const page = ref(1)
const pageSize = 10
const total = ref(0)
const keyword = ref('')
const filterType = ref('')
const formVisible = ref(false)
const isEdit = ref(false)
const saving = ref(false)
const formRef = ref(null)
const dateRange = ref([])
const form = ref(defaultForm())

function defaultForm() {
  return {
    name: '', couponType: 'REDUCE', discountValue: 10, minOrderAmount: 0,
    maxDiscountAmount: 0, totalQuantity: 100, perUserLimit: 1,
    startDate: '', endDate: '', description: '', isActive: true
  }
}

const rules = {
  name: [{ required: true, message: '请输入名称', trigger: 'blur' }],
  couponType: [{ required: true, message: '请选择类型' }],
  discountValue: [{ required: true, message: '请填写优惠值', type: 'number', trigger: 'blur' }],
  totalQuantity: [{ required: true, message: '请填写发行数量', type: 'number' }],
}

watch(dateRange, (val) => {
  form.value.startDate = val?.[0] || ''
  form.value.endDate = val?.[1] || ''
})

function formatDate(dt) {
  if (!dt) return '-'
  return dt.substring(0, 10)
}

async function fetchList() {
  loading.value = true
  try {
    const params = { page: page.value - 1, size: pageSize }
    if (keyword.value) params.keyword = keyword.value
    if (filterType.value) params.type = filterType.value
    const res = await axios.get('/api/admin/coupons', { params })
    const data = res.data?.data
    list.value = data?.content || []
    total.value = data?.totalElements || 0
  } catch (e) { console.error(e) }
  finally { loading.value = false }
}

function openForm(row = null) {
  isEdit.value = !!row
  if (row) {
    form.value = { ...row }
    dateRange.value = [row.startDate, row.endDate]
  } else {
    form.value = defaultForm()
    dateRange.value = []
  }
  formVisible.value = true
}

async function submitForm() {
  await formRef.value?.validate()
  saving.value = true
  try {
    if (isEdit.value) {
      await axios.put(`/api/admin/coupons/${form.value.id}`, form.value)
    } else {
      await axios.post('/api/admin/coupons', form.value)
    }
    ElMessage.success(isEdit.value ? '更新成功' : '创建成功')
    formVisible.value = false
    fetchList()
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '操作失败')
  } finally { saving.value = false }
}

async function toggleActive(row) {
  try {
    await axios.put(`/api/admin/coupons/${row.id}/toggle`)
  } catch (e) {
    row.isActive = !row.isActive
    ElMessage.error('操作失败')
  }
}

async function handleDelete(id) {
  try {
    await axios.delete(`/api/admin/coupons/${id}`)
    ElMessage.success('已删除')
    fetchList()
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '删除失败')
  }
}

onMounted(fetchList)
</script>

<style scoped>
.coupon-management { padding: 0; }
.toolbar-card { margin-bottom: 16px; }
.toolbar { display: flex; justify-content: space-between; align-items: center; gap: 12px; }
.toolbar-left { display: flex; gap: 12px; align-items: center; }
.pagination { margin-top: 16px; display: flex; justify-content: flex-end; }
</style>
