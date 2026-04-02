<template>
  <div class="nutritionist-management">
    <!-- Tabs: Pending / All -->
    <el-tabs v-model="activeTab" @tab-change="handleTabChange">
      <el-tab-pane label="待审核" name="pending">
        <template #label>
          待审核
          <el-badge v-if="pendingCount > 0" :value="pendingCount" class="tab-badge" />
        </template>
      </el-tab-pane>
      <el-tab-pane label="全部营养师" name="all" />
    </el-tabs>

    <!-- Pending Tab -->
    <template v-if="activeTab === 'pending'">
      <el-skeleton :loading="pendingLoading" animated :rows="4">
        <el-empty v-if="!pendingList.length" description="暂无待审核的营养师申请" />
        <div v-else class="pending-list">
          <el-card v-for="n in pendingList" :key="n.id" class="pending-card" shadow="hover">
            <div class="pending-header">
              <el-avatar :size="48" :src="n.avatar" class="pending-avatar">{{ n.name?.charAt(0) }}</el-avatar>
              <div class="pending-info">
                <h3>{{ n.name }} <el-tag size="small" type="warning">待审核</el-tag></h3>
                <p>{{ n.title }} · {{ n.experienceYears || 0 }}年经验 · 咨询费 ¥{{ n.consultationFee }}</p>
              </div>
            </div>
            <el-descriptions :column="2" border size="small" class="pending-details">
              <el-descriptions-item label="邮箱">
                <span v-if="n.userId">用户ID: {{ n.userId }}</span>
                <span v-else>-</span>
              </el-descriptions-item>
              <el-descriptions-item label="专业领域">{{ (n.specialties || []).join('、') || '-' }}</el-descriptions-item>
              <el-descriptions-item label="简介" :span="2">{{ n.introduction || '-' }}</el-descriptions-item>
              <el-descriptions-item label="工作时间">{{ n.workingHours || '-' }}</el-descriptions-item>
              <el-descriptions-item label="申请时间">{{ formatDate(n.createdAt) }}</el-descriptions-item>
            </el-descriptions>
            <!-- Credential images -->
            <div class="cert-section">
              <p class="cert-label">资质证书：</p>
              <div v-if="getCertUrls(n).length" class="cert-images">
                <el-image
                  v-for="(url, i) in getCertUrls(n)"
                  :key="i"
                  :src="url"
                  :preview-src-list="getCertUrls(n)"
                  :initial-index="i"
                  fit="cover"
                  class="cert-img"
                  :zoom-rate="1.2"
                  :max-scale="5"
                  :min-scale="0.5"
                >
                  <template #error>
                    <div class="cert-img-error">加载失败</div>
                  </template>
                </el-image>
              </div>
              <el-empty v-else description="未上传资质证书" :image-size="40" />
            </div>
            <div class="pending-actions">
              <el-button type="success" @click="handleApprove(n)" :loading="n._approving">
                <el-icon><Check /></el-icon> 通过审核
              </el-button>
              <el-button type="danger" plain @click="handleReject(n)" :loading="n._rejecting">
                <el-icon><Close /></el-icon> 拒绝
              </el-button>
            </div>
          </el-card>
        </div>
      </el-skeleton>
    </template>

    <!-- All Tab -->
    <template v-if="activeTab === 'all'">
      <el-card class="toolbar-card">
        <div class="toolbar">
          <el-input v-model="keyword" placeholder="搜索营养师姓名/职称" clearable style="width: 240px"
            @keyup.enter="fetchList" @clear="fetchList">
            <template #prefix><el-icon><Search /></el-icon></template>
          </el-input>
          <el-button type="primary" @click="openForm()">
            <el-icon><Plus /></el-icon> 添加营养师
          </el-button>
        </div>
      </el-card>

      <el-card class="list-card">
        <el-table :data="list" v-loading="loading" stripe>
          <el-table-column label="头像" width="70" align="center">
            <template #default="{ row }">
              <el-avatar :size="36" :src="row.avatar">{{ row.name?.charAt(0) }}</el-avatar>
            </template>
          </el-table-column>
          <el-table-column prop="name" label="姓名" width="100" />
          <el-table-column prop="title" label="职称" width="120" />
          <el-table-column label="专业领域" min-width="180">
            <template #default="{ row }">
              <el-tag v-for="s in (row.specialties || []).slice(0, 3)" :key="s" size="small" style="margin: 2px">{{ s }}</el-tag>
              <span v-if="(row.specialties || []).length > 3" style="color:#999;font-size:12px"> +{{ row.specialties.length - 3 }}</span>
            </template>
          </el-table-column>
          <el-table-column label="评分" width="70" align="center">
            <template #default="{ row }">{{ row.rating > 0 ? row.rating : '-' }}</template>
          </el-table-column>
          <el-table-column label="审核" width="90" align="center">
            <template #default="{ row }">
              <el-tag :type="approvalType(row.approvalStatus)" size="small">{{ approvalLabel(row.approvalStatus) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="80" align="center">
            <template #default="{ row }">
              <el-tag :type="statusType(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="启用" width="70" align="center">
            <template #default="{ row }">
              <el-switch v-model="row.isActive" @change="toggleActive(row)" size="small" />
            </template>
          </el-table-column>
          <el-table-column label="操作" width="200" align="center" fixed="right">
            <template #default="{ row }">
              <el-button type="primary" size="small" text @click="openForm(row)">编辑</el-button>
              <el-button v-if="row.approvalStatus === 'PENDING'" type="success" size="small" text @click="handleApprove(row)">通过</el-button>
              <el-button v-if="row.approvalStatus === 'PENDING'" type="warning" size="small" text @click="handleReject(row)">拒绝</el-button>
              <el-popconfirm title="确定删除？" @confirm="handleDelete(row.id)">
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
    </template>

    <!-- Edit Dialog -->
    <el-dialog v-model="formVisible" :title="isEdit ? '编辑营养师' : '添加营养师'" width="640px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="姓名" prop="name">
              <el-input v-model="form.name" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="职称" prop="title">
              <el-select v-model="form.title" style="width:100%">
                <el-option v-for="t in NutritionistTitles" :key="t" :label="t" :value="t" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="经验年数" prop="experienceYears">
              <el-input-number v-model="form.experienceYears" :min="0" :max="50" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="咨询费" prop="consultationFee">
              <el-input-number v-model="form.consultationFee" :min="0" :precision="2" style="width:100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="头像URL">
          <el-input v-model="form.avatar" placeholder="输入头像图片URL" />
        </el-form-item>
        <el-form-item label="专业领域">
          <el-select v-model="form.specialties" multiple filterable allow-create style="width:100%"
            placeholder="输入后回车添加">
          </el-select>
        </el-form-item>
        <el-form-item label="简介">
          <el-input v-model="form.introduction" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item label="工作时间">
          <el-input v-model="form.workingHours" placeholder="如: 周一至周五 9:00-18:00" />
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="状态">
              <el-select v-model="form.status" style="width:100%">
                <el-option label="在线" value="ONLINE" />
                <el-option label="离线" value="OFFLINE" />
                <el-option label="忙碌" value="BUSY" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="排序">
              <el-input-number v-model="form.sortOrder" :min="0" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="启用">
              <el-switch v-model="form.isActive" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { Search, Plus, Check, Close } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getAdminNutritionists, createNutritionist, updateNutritionist,
  deleteNutritionist, updateNutritionistStatus, NutritionistTitles, NutritionistStatuses
} from '@/services/adminShop'
import api from '@/services/api'

const activeTab = ref('pending')
const loading = ref(false)
const list = ref([])
const total = ref(0)
const page = ref(1)
const pageSize = 10
const keyword = ref('')

const pendingLoading = ref(false)
const pendingList = ref([])
const pendingCount = ref(0)

const formVisible = ref(false)
const isEdit = ref(false)
const saving = ref(false)
const formRef = ref(null)
const editId = ref(null)

const defaultForm = () => ({
  name: '', title: '初级营养师', avatar: '', specialties: [],
  introduction: '', experienceYears: 1, consultationFee: 99,
  status: 'OFFLINE', workingHours: '周一至周五 9:00-18:00',
  isActive: true, sortOrder: 0
})

const form = reactive(defaultForm())

const rules = {
  name: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  title: [{ required: true, message: '请选择职称', trigger: 'change' }],
  consultationFee: [{ required: true, message: '请输入咨询费', trigger: 'blur' }]
}

const fetchPending = async () => {
  pendingLoading.value = true
  try {
    const res = await api.get('/admin/nutritionists/pending')
    if (res.data.code === 200) {
      pendingList.value = (res.data.data || []).map(n => ({ ...n, _approving: false, _rejecting: false }))
      pendingCount.value = pendingList.value.length
    }
  } catch (e) { console.error(e) }
  finally { pendingLoading.value = false }
}

const fetchList = async () => {
  loading.value = true
  try {
    const res = await getAdminNutritionists({ keyword: keyword.value || undefined, page: page.value - 1, size: pageSize })
    if (res.data.code === 200) {
      list.value = res.data.data.content || []
      total.value = res.data.data.totalElements || 0
    }
  } catch (e) { console.error(e) }
  finally { loading.value = false }
}

const handleTabChange = (tab) => {
  if (tab === 'pending') fetchPending()
  else fetchList()
}

const handleApprove = async (n) => {
  try {
    await ElMessageBox.confirm(`确认通过 ${n.name} 的营养师入驻申请？`, '审核确认', { type: 'success' })
  } catch { return }
  n._approving = true
  try {
    const res = await api.put(`/admin/nutritionists/${n.id}/approve`)
    if (res.data.code === 200) {
      ElMessage.success(`${n.name} 审核已通过`)
      fetchPending()
      if (activeTab.value === 'all') fetchList()
    } else { ElMessage.error(res.data.message) }
  } catch { ElMessage.error('操作失败') }
  finally { n._approving = false }
}

const handleReject = async (n) => {
  let reason = ''
  try {
    const { value } = await ElMessageBox.prompt('请输入拒绝原因（可选）', '拒绝确认', {
      inputPlaceholder: '拒绝原因',
      type: 'warning',
      confirmButtonText: '确认拒绝',
      cancelButtonText: '取消'
    })
    reason = value
  } catch { return }
  n._rejecting = true
  try {
    const res = await api.put(`/admin/nutritionists/${n.id}/reject`, { reason })
    if (res.data.code === 200) {
      ElMessage.success(`已拒绝 ${n.name} 的申请`)
      fetchPending()
      if (activeTab.value === 'all') fetchList()
    } else { ElMessage.error(res.data.message) }
  } catch { ElMessage.error('操作失败') }
  finally { n._rejecting = false }
}

const openForm = (row) => {
  if (row) {
    isEdit.value = true
    editId.value = row.id
    Object.assign(form, {
      name: row.name, title: row.title, avatar: row.avatar || '',
      specialties: row.specialties || [], introduction: row.introduction || '',
      experienceYears: row.experienceYears, consultationFee: row.consultationFee,
      status: row.status, workingHours: row.workingHours || '',
      isActive: row.isActive, sortOrder: row.sortOrder || 0
    })
  } else {
    isEdit.value = false
    editId.value = null
    Object.assign(form, defaultForm())
  }
  formVisible.value = true
}

const handleSave = async () => {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    const res = isEdit.value
      ? await updateNutritionist(editId.value, form)
      : await createNutritionist(form)
    if (res.data.code === 200) {
      ElMessage.success(isEdit.value ? '更新成功' : '创建成功')
      formVisible.value = false
      fetchList()
    } else { ElMessage.error(res.data.message) }
  } catch (e) { ElMessage.error('操作失败') }
  finally { saving.value = false }
}

const handleDelete = async (id) => {
  try {
    const res = await deleteNutritionist(id)
    if (res.data.code === 200) { ElMessage.success('删除成功'); fetchList() }
  } catch (e) { ElMessage.error('删除失败') }
}

const changeStatus = async (id, status) => {
  try {
    const res = await updateNutritionistStatus(id, { status })
    if (res.data.code === 200) { ElMessage.success('状态已更新'); fetchList() }
  } catch (e) { ElMessage.error('更新失败') }
}

const toggleActive = async (row) => {
  try {
    const res = await updateNutritionistStatus(row.id, { isActive: row.isActive })
    if (res.data.code === 200) { ElMessage.success(row.isActive ? '已启用' : '已禁用') }
    else { row.isActive = !row.isActive }
  } catch (e) { row.isActive = !row.isActive; ElMessage.error('操作失败') }
}

const approvalLabel = (s) => ({ PENDING: '待审核', APPROVED: '已通过', REJECTED: '已拒绝' }[s] || s)
const approvalType = (s) => ({ PENDING: 'warning', APPROVED: 'success', REJECTED: 'danger' }[s] || 'info')
const statusLabel = (s) => NutritionistStatuses[s]?.label || s
const statusType = (s) => NutritionistStatuses[s]?.type || 'info'

const formatDate = (dt) => {
  if (!dt) return '-'
  return new Date(dt).toLocaleString('zh-CN', { year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' })
}

// 兼容不同证书格式：{urls:[...]}, ["url1"], null, {}
const getCertUrls = (n) => {
  if (!n || !n.certificates) return []
  const c = n.certificates
  if (Array.isArray(c)) return c
  if (c.urls && Array.isArray(c.urls)) return c.urls
  return []
}

onMounted(() => {
  fetchPending()
})
</script>

<style scoped lang="scss">
.nutritionist-management {
  font-family: 'Inter', sans-serif;
  .tab-badge { margin-left: 6px; }

  .toolbar-card {
    margin-bottom: 16px;
    border: 1px solid #E2E8F0;
    border-radius: 12px;
    box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
    background: #FFFFFF;
    .toolbar {
      display: flex;
      justify-content: space-between;
      align-items: center;
    }
  }
  .list-card {
    border: 1px solid #E2E8F0;
    border-radius: 12px;
    box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1), 0 2px 4px -2px rgba(0, 0, 0, 0.1);
    background: #FFFFFF;

    :deep(.el-table th.el-table__cell) {
      background: #F1F5F9;
      color: #0F172A;
      font-family: 'Inter', sans-serif;
      font-weight: 600;
      border-bottom: 1px solid #E2E8F0;
    }
    :deep(.el-table td.el-table__cell) {
      border-bottom: 1px solid #E2E8F0;
    }
    :deep(.el-table--striped .el-table__body tr.el-table__row--striped td.el-table__cell) {
      background: #FAFAFA;
    }
  }
  .pagination {
    display: flex;
    justify-content: center;
    margin-top: 16px;
  }
}

.pending-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.pending-card {
  border: 1px solid #E2E8F0;
  border-radius: 12px;
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1), 0 2px 4px -2px rgba(0, 0, 0, 0.1);
  background: #FFFFFF;

  .pending-header {
    display: flex;
    align-items: center;
    gap: 16px;
    margin-bottom: 16px;

    .pending-avatar {
      background: linear-gradient(135deg, #10B981, #34D399);
      color: #fff;
      font-weight: 600;
      border: 2px solid #E2E8F0;
    }

    .pending-info {
      h3 {
        margin: 0 0 4px;
        font-size: 16px;
        display: flex;
        align-items: center;
        gap: 8px;
        font-family: 'Inter', sans-serif;
        color: #0F172A;
      }
      p { margin: 0; color: #0F172A; opacity: 0.6; font-size: 13px; }
    }
  }

  .pending-details {
    margin-bottom: 16px;
    border: 1px solid #E2E8F0;
    border-radius: 8px;
  }

  .cert-section {
    margin-bottom: 16px;
    padding-top: 16px;
    border-top: 1px solid #E2E8F0;
    .cert-label { font-size: 13px; color: #0F172A; opacity: 0.6; margin: 0 0 8px; }
    .cert-images {
      display: flex;
      gap: 8px;
      flex-wrap: wrap;
    }
    .cert-img {
      width: 120px;
      height: 90px;
      border-radius: 8px;
      border: 1px solid #E2E8F0;
      cursor: pointer;
    }
    .cert-img-error {
      width: 120px;
      height: 90px;
      display: flex;
      align-items: center;
      justify-content: center;
      background: #FAFAFA;
      color: #0F172A;
      opacity: 0.5;
      font-size: 12px;
      border-radius: 8px;
      border: 1px solid #E2E8F0;
    }
  }

  .pending-actions {
    display: flex;
    gap: 12px;
    justify-content: flex-end;
    padding-top: 12px;
    border-top: 1px solid #E2E8F0;
  }
}
</style>
