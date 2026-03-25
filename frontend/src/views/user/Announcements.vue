<template>
  <div class="announcements-page">
    <div class="page-header">
      <h1>系统公告</h1>
      <p class="subtitle">
        查看最新的系统通知和公告信息
      </p>
    </div>

    <!-- 公告列表 -->
    <div
      v-loading="loading"
      class="announcements-list"
    >
      <el-empty
        v-if="announcements.length === 0"
        description="暂无公告"
      />
      
      <div
        v-for="announcement in announcements"
        :key="announcement.id"
        class="announcement-card"
        :class="`type-${announcement.type}`"
      >
        <div class="card-header">
          <div class="header-left">
            <el-tag
              :type="getTypeTag(announcement.type)"
              size="large"
              effect="dark"
            >
              {{ getTypeLabel(announcement.type) }}
            </el-tag>
            <h3 class="title">
              {{ announcement.title }}
            </h3>
          </div>
          <div class="header-right">
            <span class="date">{{ formatDate(announcement.createdAt) }}</span>
          </div>
        </div>
        
        <div class="card-content">
          <p class="content">
            {{ announcement.content }}
          </p>
        </div>
        
        <div
          v-if="announcement.startTime || announcement.endTime"
          class="card-footer"
        >
          <el-icon><Clock /></el-icon>
          <span class="validity">
            有效期：{{ formatDate(announcement.startTime) }} 至 {{ formatDate(announcement.endTime) }}
          </span>
        </div>
      </div>
    </div>

    <!-- 分页 -->
    <el-pagination
      v-if="total > 0"
      v-model:current-page="currentPage"
      v-model:page-size="pageSize"
      :total="total"
      :page-sizes="[10, 20, 30, 50]"
      layout="total, sizes, prev, pager, next, jumper"
      class="pagination"
      @size-change="loadAnnouncements"
      @current-change="loadAnnouncements"
    />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Clock } from '@element-plus/icons-vue'

const loading = ref(false)
const announcements = ref([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

// 加载公告列表
const loadAnnouncements = async () => {
  loading.value = true
  try {
    const response = await fetch(
      `http://localhost:8080/api/announcements?page=${currentPage.value}&size=${pageSize.value}`
    )
    
    const data = await response.json()
    if (data.code === 200) {
      announcements.value = data.data.content || []
      total.value = data.data.totalElements || 0
    } else {
      ElMessage.error('加载公告失败')
    }
  } catch (error) {
    console.error('加载公告失败:', error)
    ElMessage.error('加载公告失败')
  } finally {
    loading.value = false
  }
}

// 获取类型标签
const getTypeTag = (type) => {
  const typeMap = {
    'NOTICE': 'info',
    'WARNING': 'warning',
    'ERROR': 'danger'
  }
  return typeMap[type] || 'info'
}

// 获取类型标签文本
const getTypeLabel = (type) => {
  const labelMap = {
    'NOTICE': '通知',
    'WARNING': '警告',
    'ERROR': '紧急'
  }
  return labelMap[type] || '通知'
}

// 格式化日期
const formatDate = (dateString) => {
  if (!dateString) return '-'
  const date = new Date(dateString)
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

onMounted(() => {
  loadAnnouncements()
})
</script>

<style scoped>
.announcements-page {
  max-width: 1200px;
  margin: 0 auto;
  padding: 24px;
}

.page-header {
  text-align: center;
  margin-bottom: 40px;
}

.page-header h1 {
  font-size: 32px;
  font-weight: 600;
  color: #262626;
  margin-bottom: 8px;
}

.subtitle {
  font-size: 16px;
  color: #8c8c8c;
}

.announcements-list {
  min-height: 400px;
}

.announcement-card {
  background: #ffffff;
  border-radius: 12px;
  padding: 24px;
  margin-bottom: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  transition: all 0.3s ease;
  border-left: 4px solid #1890ff;
}

.announcement-card:hover {
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.12);
  transform: translateY(-2px);
}

.announcement-card.type-WARNING {
  border-left-color: #faad14;
}

.announcement-card.type-ERROR {
  border-left-color: #ff4d4f;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 16px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
  flex: 1;
}

.title {
  font-size: 20px;
  font-weight: 600;
  color: #262626;
  margin: 0;
}

.header-right {
  display: flex;
  align-items: center;
}

.date {
  font-size: 14px;
  color: #8c8c8c;
}

.card-content {
  margin-bottom: 16px;
}

.content {
  font-size: 15px;
  line-height: 1.8;
  color: #595959;
  white-space: pre-wrap;
  word-break: break-word;
}

.card-footer {
  display: flex;
  align-items: center;
  gap: 8px;
  padding-top: 16px;
  border-top: 1px solid #f0f0f0;
  color: #8c8c8c;
  font-size: 14px;
}

.validity {
  display: flex;
  align-items: center;
}

.pagination {
  margin-top: 32px;
  display: flex;
  justify-content: center;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .announcements-page {
    padding: 16px;
  }

  .page-header h1 {
    font-size: 24px;
  }

  .announcement-card {
    padding: 16px;
  }

  .card-header {
    flex-direction: column;
    gap: 12px;
  }

  .header-left {
    flex-direction: column;
    align-items: flex-start;
  }

  .title {
    font-size: 18px;
  }
}
</style>
