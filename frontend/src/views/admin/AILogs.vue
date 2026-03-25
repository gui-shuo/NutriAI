<template>
  <div class="ai-logs">
    <h2 class="page-title">
      AI日志查询
    </h2>

    <!-- 搜索筛选 -->
    <el-card class="search-card">
      <el-form
        :inline="true"
        :model="searchForm"
      >
        <el-form-item label="用户ID">
          <el-input
            v-model="searchForm.userId"
            placeholder="用户ID"
            clearable
          />
        </el-form-item>
        <el-form-item label="状态">
          <el-select
            v-model="searchForm.status"
            placeholder="全部"
            clearable
            style="width: 120px"
          >
            <el-option
              label="成功"
              value="success"
            />
            <el-option
              label="失败"
              value="error"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="时间范围">
          <el-date-picker
            v-model="searchForm.dateRange"
            type="datetimerange"
            range-separator="至"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
          />
        </el-form-item>
        <el-form-item>
          <el-button
            type="primary"
            @click="handleSearch"
          >
            搜索
          </el-button>
          <el-button @click="handleReset">
            重置
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 日志列表 -->
    <el-card>
      <el-table
        v-loading="loading"
        :data="logList"
        stripe
      >
        <el-table-column
          prop="id"
          label="ID"
          width="80"
        />
        <el-table-column
          prop="userId"
          label="用户ID"
          width="100"
        />
        <el-table-column
          prop="username"
          label="用户名"
          width="120"
        />
        <el-table-column
          prop="userMessage"
          label="用户消息"
          width="200"
          show-overflow-tooltip
        />
        <el-table-column
          prop="aiResponse"
          label="AI回复"
          width="200"
          show-overflow-tooltip
        />
        <el-table-column
          prop="model"
          label="模型"
          width="120"
        />
        <el-table-column
          prop="tokensUsed"
          label="Token"
          width="80"
        />
        <el-table-column
          prop="responseTime"
          label="响应时间(ms)"
          width="120"
        />
        <el-table-column
          label="状态"
          width="100"
        >
          <template #default="{ row }">
            <el-tag :type="row.status === 'success' ? 'success' : 'danger'">
              {{ row.status === 'success' ? '成功' : '失败' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column
          prop="createdAt"
          label="时间"
          width="180"
        >
          <template #default="{ row }">
            {{ formatDate(row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column
          label="操作"
          width="100"
          fixed="right"
        >
          <template #default="{ row }">
            <el-button
              type="primary"
              link
              size="small"
              @click="handleView(row)"
            >
              详情
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="pagination.page"
        v-model:page-size="pagination.size"
        :total="pagination.total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        class="pagination"
        @size-change="handleSearch"
        @current-change="handleSearch"
      />
    </el-card>

    <!-- 详情对话框 -->
    <el-dialog
      v-model="detailVisible"
      title="日志详情"
      width="800px"
    >
      <el-descriptions
        v-if="currentLog"
        :column="2"
        border
      >
        <el-descriptions-item label="日志ID">
          {{ currentLog.id }}
        </el-descriptions-item>
        <el-descriptions-item label="用户">
          {{ currentLog.username }} (ID: {{ currentLog.userId }})
        </el-descriptions-item>
        <el-descriptions-item
          label="会话ID"
          :span="2"
        >
          {{ currentLog.sessionId }}
        </el-descriptions-item>
        <el-descriptions-item
          label="用户消息"
          :span="2"
        >
          <div class="message-content">
            {{ currentLog.userMessage }}
          </div>
        </el-descriptions-item>
        <el-descriptions-item
          label="AI回复"
          :span="2"
        >
          <div class="message-content">
            {{ currentLog.aiResponse }}
          </div>
        </el-descriptions-item>
        <el-descriptions-item label="模型">
          {{ currentLog.model }}
        </el-descriptions-item>
        <el-descriptions-item label="Token使用">
          {{ currentLog.tokensUsed }}
        </el-descriptions-item>
        <el-descriptions-item label="响应时间">
          {{ currentLog.responseTime }}ms
        </el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="currentLog.status === 'success' ? 'success' : 'danger'">
            {{ currentLog.status }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item
          v-if="currentLog.errorMessage"
          label="错误信息"
          :span="2"
        >
          <el-text type="danger">
            {{ currentLog.errorMessage }}
          </el-text>
        </el-descriptions-item>
        <el-descriptions-item
          label="创建时间"
          :span="2"
        >
          {{ formatDate(currentLog.createdAt) }}
        </el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'

const loading = ref(false)
const logList = ref([])
const detailVisible = ref(false)
const currentLog = ref(null)

const searchForm = reactive({
  userId: '',
  status: '',
  dateRange: null
})

const pagination = reactive({
  page: 1,
  size: 20,
  total: 0
})

const loadLogs = async () => {
  loading.value = true
  try {
    const token = localStorage.getItem('token')
    const params = new URLSearchParams({
      page: pagination.page,
      size: pagination.size
    })

    if (searchForm.userId) params.append('userId', searchForm.userId)
    if (searchForm.status) params.append('status', searchForm.status)
    if (searchForm.dateRange && searchForm.dateRange.length === 2) {
      params.append('startDate', searchForm.dateRange[0].toISOString())
      params.append('endDate', searchForm.dateRange[1].toISOString())
    }

    const response = await fetch(`http://localhost:8080/api/admin/ai-logs?${params}`, {
      headers: {
        Authorization: `Bearer ${token}`
      }
    })

    const data = await response.json()
    if (data.code === 200) {
      logList.value = data.data.content
      pagination.total = data.data.totalElements
    }
  } catch (error) {
    console.error('加载日志失败:', error)
    ElMessage.error('加载日志失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  pagination.page = 1
  loadLogs()
}

const handleReset = () => {
  searchForm.userId = ''
  searchForm.status = ''
  searchForm.dateRange = null
  handleSearch()
}

const handleView = row => {
  currentLog.value = row
  detailVisible.value = true
}

const formatDate = dateString => {
  if (!dateString) return '-'
  return new Date(dateString).toLocaleString('zh-CN')
}

onMounted(() => {
  loadLogs()
})
</script>

<style scoped>
.ai-logs {
  padding: 0;
}

.page-title {
  margin: 0 0 24px 0;
  font-size: 24px;
  font-weight: 500;
  color: #262626;
}

.search-card {
  margin-bottom: 16px;
}

.pagination {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}

.message-content {
  max-height: 200px;
  overflow-y: auto;
  padding: 8px;
  background: #f5f5f5;
  border-radius: 4px;
  white-space: pre-wrap;
  word-break: break-word;
}
</style>
