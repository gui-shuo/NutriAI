<template>
  <div class="version-management">
    <div class="page-header">
      <h2>📦 APP版本管理</h2>
      <el-button type="primary" @click="showUploadDialog = true">
        <el-icon><Upload /></el-icon> 上传新版本
      </el-button>
    </div>

    <!-- Version List -->
    <el-table :data="versions" v-loading="loading" stripe>
      <el-table-column prop="versionName" label="版本号" width="120">
        <template #default="{ row }">
          v{{ row.versionName }}
          <el-tag v-if="row.isLatest" type="success" size="small" style="margin-left:4px">最新</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="versionCode" label="版本码" width="80" />
      <el-table-column prop="platform" label="平台" width="80" />
      <el-table-column prop="description" label="更新说明" show-overflow-tooltip />
      <el-table-column label="文件大小" width="100">
        <template #default="{ row }">{{ formatSize(row.fileSize) }}</template>
      </el-table-column>
      <el-table-column prop="downloadCount" label="下载" width="70" />
      <el-table-column label="上传时间" width="170">
        <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="220" fixed="right">
        <template #default="{ row }">
          <el-button v-if="!row.isLatest" type="success" size="small" @click="setLatest(row)">
            设为最新
          </el-button>
          <el-button type="warning" size="small" @click="editVersion(row)">编辑</el-button>
          <el-popconfirm title="确定删除此版本？" @confirm="deleteVersion(row)">
            <template #reference>
              <el-button type="danger" size="small">删除</el-button>
            </template>
          </el-popconfirm>
        </template>
      </el-table-column>
    </el-table>

    <!-- Upload Dialog -->
    <el-dialog v-model="showUploadDialog" title="上传新版本" width="500px" @close="resetForm">
      <el-form :model="form" label-width="90px">
        <el-form-item label="版本号" required>
          <el-input v-model="form.versionName" placeholder="例如: 1.0.0" />
        </el-form-item>
        <el-form-item label="版本码" required>
          <el-input-number v-model="form.versionCode" :min="1" />
        </el-form-item>
        <el-form-item label="平台">
          <el-select v-model="form.platform">
            <el-option label="Android" value="android" />
            <el-option label="iOS" value="ios" />
          </el-select>
        </el-form-item>
        <el-form-item label="更新说明">
          <el-input v-model="form.description" type="textarea" :rows="4" placeholder="描述此版本的更新内容" />
        </el-form-item>
        <el-form-item label="APK文件" required>
          <el-upload
            ref="uploadRef"
            :auto-upload="false"
            :limit="1"
            accept=".apk"
            :on-change="handleFileChange"
            :on-remove="() => form.file = null"
          >
            <el-button type="primary">选择APK文件</el-button>
            <template #tip>
              <div class="el-upload__tip">仅支持 .apk 文件，最大 200MB</div>
            </template>
          </el-upload>
        </el-form-item>
      </el-form>

      <div v-if="uploadProgress > 0" style="margin-top: 10px;">
        <el-progress :percentage="uploadProgress" :status="uploadProgress === 100 ? 'success' : ''" />
        <div v-if="uploadProgress === 100 && uploading" style="margin-top: 6px; color: #E6A23C; font-size: 13px;">
          ⏳ 文件已上传到服务器，正在同步到云存储，请耐心等待...
        </div>
      </div>

      <template #footer>
        <el-button @click="showUploadDialog = false">取消</el-button>
        <el-button type="primary" @click="submitUpload" :loading="uploading">
          {{ uploading ? '上传中...' : '上传' }}
        </el-button>
      </template>
    </el-dialog>

    <!-- Edit Dialog -->
    <el-dialog v-model="showEditDialog" title="编辑版本" width="500px">
      <el-form :model="editForm" label-width="80px">
        <el-form-item label="更新说明">
          <el-input v-model="editForm.description" type="textarea" :rows="4" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showEditDialog = false">取消</el-button>
        <el-button type="primary" @click="submitEdit" :loading="saving">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { Upload } from '@element-plus/icons-vue'
import { appVersionApi } from '@/services/appVersion'
import { ElMessage } from 'element-plus'

const versions = ref([])
const loading = ref(false)
const showUploadDialog = ref(false)
const showEditDialog = ref(false)
const uploading = ref(false)
const saving = ref(false)
const uploadProgress = ref(0)

const form = ref({
  versionName: '',
  versionCode: 1,
  platform: 'android',
  description: '',
  file: null
})

const editForm = ref({ id: null, description: '' })

const loadVersions = async () => {
  loading.value = true
  try {
    const res = await appVersionApi.getList('android', 0, 100)
    const page = res.data?.data
    versions.value = page?.content || page || []
  } catch (e) {
    ElMessage.error('加载版本列表失败')
  } finally {
    loading.value = false
  }
}

const handleFileChange = (file) => {
  form.value.file = file.raw
}

const submitUpload = async () => {
  const { versionName, versionCode, platform, description, file } = form.value
  if (!versionName || !versionCode || !file) {
    ElMessage.warning('请填写完整信息并选择APK文件')
    return
  }

  const fd = new FormData()
  fd.append('file', file)
  fd.append('versionName', versionName)
  fd.append('versionCode', versionCode)
  fd.append('platform', platform)
  if (description) fd.append('description', description)

  uploading.value = true
  uploadProgress.value = 0
  try {
    await appVersionApi.upload(fd, (e) => {
      uploadProgress.value = Math.round((e.loaded / e.total) * 100)
    })
    ElMessage.success('版本上传成功')
    showUploadDialog.value = false
    loadVersions()
  } catch (e) {
    ElMessage.error('上传失败: ' + (e.response?.data?.message || e.message))
  } finally {
    uploading.value = false
  }
}

const resetForm = () => {
  form.value = { versionName: '', versionCode: 1, platform: 'android', description: '', file: null }
  uploadProgress.value = 0
}

const setLatest = async (row) => {
  try {
    await appVersionApi.setLatest(row.id)
    ElMessage.success('已设为最新版本')
    loadVersions()
  } catch (e) {
    ElMessage.error('操作失败')
  }
}

const editVersion = (row) => {
  editForm.value = { id: row.id, description: row.description || '' }
  showEditDialog.value = true
}

const submitEdit = async () => {
  saving.value = true
  try {
    await appVersionApi.update(editForm.value.id, editForm.value.description)
    ElMessage.success('更新成功')
    showEditDialog.value = false
    loadVersions()
  } catch (e) {
    ElMessage.error('更新失败')
  } finally {
    saving.value = false
  }
}

const deleteVersion = async (row) => {
  try {
    await appVersionApi.delete(row.id)
    ElMessage.success('删除成功')
    loadVersions()
  } catch (e) {
    ElMessage.error('删除失败')
  }
}

const formatSize = (bytes) => {
  if (!bytes) return '-'
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / (1024 * 1024)).toFixed(1) + ' MB'
}

const formatDateTime = (dateStr) => {
  if (!dateStr) return ''
  return new Date(dateStr).toLocaleString('zh-CN')
}

onMounted(loadVersions)
</script>

<style scoped>
.version-management {
  padding: 0;
  font-family: 'Inter', sans-serif;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.page-header h2 {
  margin: 0;
  font-size: 20px;
  color: #0F172A;
  font-family: 'Calistoga', serif;
}

.version-management :deep(.el-table) {
  border: 1px solid #E2E8F0;
  border-radius: 12px;
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1), 0 2px 4px -2px rgba(0, 0, 0, 0.1);
  background: #FFFFFF;
  overflow: hidden;
}

.version-management :deep(.el-table th.el-table__cell) {
  background: #F1F5F9;
  color: #0F172A;
  font-family: 'Inter', sans-serif;
  font-weight: 600;
  border-bottom: 1px solid #E2E8F0;
}

.version-management :deep(.el-table td.el-table__cell) {
  border-bottom: 1px solid #E2E8F0;
}

.version-management :deep(.el-table--striped .el-table__body tr.el-table__row--striped td.el-table__cell) {
  background: #FAFAFA;
}
</style>
