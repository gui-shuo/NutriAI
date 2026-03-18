<template>
  <div class="announcement-management">
    <h2 class="page-title">公告管理</h2>

    <!-- 公告列表 -->
    <el-card v-loading="loading">
      <template #header>
        <div class="card-header">
          <span>公告列表</span>
          <el-button type="primary" :icon="Plus" @click="handleCreate">
            创建公告
          </el-button>
        </div>
      </template>

      <el-table :data="announcementList" stripe>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="title" label="标题" width="200" show-overflow-tooltip />
        <el-table-column prop="content" label="内容" show-overflow-tooltip />
        <el-table-column label="类型" width="100">
          <template #default="{ row }">
            <el-tag
              :type="row.type === 'error' ? 'danger' : row.type === 'warning' ? 'warning' : 'info'"
            >
              {{ typeMap[row.type] }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="priority" label="优先级" width="100" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-switch
              v-model="row.isActive"
              @change="handleToggleStatus(row)"
            />
          </template>
        </el-table-column>
        <el-table-column prop="startTime" label="开始时间" width="180">
          <template #default="{ row }">
            {{ formatDate(row.startTime) }}
          </template>
        </el-table-column>
        <el-table-column prop="endTime" label="结束时间" width="180">
          <template #default="{ row }">
            {{ formatDate(row.endTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="handleEdit(row)">
              编辑
            </el-button>
            <el-button type="danger" link size="small" @click="handleDelete(row)">
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 编辑公告对话框 -->
    <el-dialog
      v-model="editDialogVisible"
      :title="isCreate ? '创建公告' : '编辑公告'"
      width="700px"
    >
      <el-form :model="editForm" label-width="100px" :rules="rules" ref="formRef">
        <el-form-item label="标题" prop="title">
          <el-input v-model="editForm.title" placeholder="请输入公告标题" />
        </el-form-item>
        <el-form-item label="内容" prop="content">
          <el-input
            v-model="editForm.content"
            type="textarea"
            :rows="4"
            placeholder="请输入公告内容"
          />
        </el-form-item>
        <el-form-item label="类型" prop="type">
          <el-radio-group v-model="editForm.type">
            <el-radio value="info">通知</el-radio>
            <el-radio value="warning">警告</el-radio>
            <el-radio value="error">错误</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="优先级" prop="priority">
          <el-input-number v-model="editForm.priority" :min="0" :max="10" />
          <span class="form-tip">数字越大优先级越高</span>
        </el-form-item>
        <el-form-item label="是否启用">
          <el-switch v-model="editForm.isActive" />
        </el-form-item>
        <el-form-item label="有效期">
          <el-date-picker
            v-model="editForm.timeRange"
            type="datetimerange"
            range-separator="至"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            value-format="YYYY-MM-DD HH:mm:ss"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'

const loading = ref(false)
const announcementList = ref([])
const editDialogVisible = ref(false)
const isCreate = ref(false)
const formRef = ref(null)

const typeMap = {
  info: '通知',
  warning: '警告',
  error: '错误'
}

const editForm = reactive({
  id: null,
  title: '',
  content: '',
  type: 'info',
  priority: 1,
  isActive: true,
  timeRange: null
})

const rules = {
  title: [{ required: true, message: '请输入标题', trigger: 'blur' }],
  content: [{ required: true, message: '请输入内容', trigger: 'blur' }],
  type: [{ required: true, message: '请选择类型', trigger: 'change' }]
}

// 加载公告列表
const loadAnnouncements = async () => {
  loading.value = true
  try {
    const token = localStorage.getItem('token')
    const response = await fetch('http://localhost:8080/api/admin/announcements', {
      headers: {
        'Authorization': `Bearer ${token}`
      }
    })
    
    const data = await response.json()
    if (data.code === 200) {
      announcementList.value = data.data
    }
  } catch (error) {
    console.error('加载公告失败:', error)
    ElMessage.error('加载公告失败')
  } finally {
    loading.value = false
  }
}

// 创建公告
const handleCreate = () => {
  isCreate.value = true
  Object.assign(editForm, {
    id: null,
    title: '',
    content: '',
    type: 'info',
    priority: 1,
    isActive: true,
    timeRange: null
  })
  editDialogVisible.value = true
}

// 编辑公告
const handleEdit = (row) => {
  isCreate.value = false
  Object.assign(editForm, {
    ...row,
    timeRange: row.startTime && row.endTime ? [row.startTime, row.endTime] : null
  })
  editDialogVisible.value = true
}

// 保存公告
const handleSave = async () => {
  try {
    await formRef.value.validate()
    
    const token = localStorage.getItem('token')
    const requestData = {
      title: editForm.title,
      content: editForm.content,
      type: editForm.type,
      priority: editForm.priority,
      isActive: editForm.isActive,
      startTime: editForm.timeRange ? editForm.timeRange[0] : null,
      endTime: editForm.timeRange ? editForm.timeRange[1] : null
    }
    
    let response
    if (isCreate.value) {
      // 创建
      response = await fetch('http://localhost:8080/api/admin/announcements', {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json'
        },
        body: JSON.stringify(requestData)
      })
    } else {
      // 更新
      response = await fetch(
        `http://localhost:8080/api/admin/announcements/${editForm.id}`,
        {
          method: 'PUT',
          headers: {
            'Authorization': `Bearer ${token}`,
            'Content-Type': 'application/json'
          },
          body: JSON.stringify(requestData)
        }
      )
    }
    
    const data = await response.json()
    if (data.code === 200) {
      ElMessage.success(isCreate.value ? '创建成功' : '更新成功')
      editDialogVisible.value = false
      loadAnnouncements()
    } else {
      ElMessage.error(data.message || '操作失败')
    }
  } catch (error) {
    console.error('保存失败:', error)
  }
}

// 切换状态
const handleToggleStatus = async (row) => {
  try {
    const token = localStorage.getItem('token')
    const response = await fetch(
      `http://localhost:8080/api/admin/announcements/${row.id}`,
      {
        method: 'PUT',
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({
          ...row,
          isActive: row.isActive
        })
      }
    )
    
    const data = await response.json()
    if (data.code === 200) {
      ElMessage.success('状态更新成功')
    } else {
      row.isActive = !row.isActive
      ElMessage.error('状态更新失败')
    }
  } catch (error) {
    row.isActive = !row.isActive
    console.error('状态更新失败:', error)
  }
}

// 删除公告
const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除公告 "${row.title}" 吗？`,
      '确认删除',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
        center: true,
        customClass: 'custom-message-box',
        showClose: true,
        closeOnClickModal: false
      }
    )
    
    const token = localStorage.getItem('token')
    const response = await fetch(
      `http://localhost:8080/api/admin/announcements/${row.id}`,
      {
        method: 'DELETE',
        headers: {
          'Authorization': `Bearer ${token}`
        }
      }
    )
    
    const data = await response.json()
    if (data.code === 200) {
      ElMessage.success('删除成功')
      loadAnnouncements()
    } else {
      ElMessage.error('删除失败')
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除失败:', error)
    }
  }
}

// 格式化日期
const formatDate = (dateString) => {
  if (!dateString) return '-'
  return new Date(dateString).toLocaleString('zh-CN')
}

onMounted(() => {
  loadAnnouncements()
})
</script>

<style scoped>
.announcement-management {
  padding: 0;
}

.page-title {
  margin: 0 0 24px 0;
  font-size: 24px;
  font-weight: 500;
  color: #262626;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.form-tip {
  margin-left: 8px;
  font-size: 12px;
  color: #909399;
}
</style>
