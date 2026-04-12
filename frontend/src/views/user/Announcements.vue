<template>
  <div class="announcements-page">
    <div class="page-header">
      <div class="header-actions">
        <el-button :icon="ArrowLeft" @click="router.push('/')">返回首页</el-button>
        <el-button v-if="hasUnread" type="primary" plain size="small" @click="markAllRead">
          全部标记已读
        </el-button>
      </div>
      <h1>系统公告</h1>
      <p class="subtitle">查看最新的系统通知和公告信息</p>
    </div>

    <!-- 公告列表 -->
    <div v-loading="loading" class="announcements-list">
      <el-empty v-if="announcements.length === 0" description="暂无公告" />

      <div
        v-for="announcement in announcements"
        :key="announcement.id"
        class="announcement-card"
        :class="[`type-${announcement.type}`, { unread: !announcement.isRead }]"
      >
        <div class="card-header">
          <div class="header-left">
            <span v-if="!announcement.isRead" class="unread-dot" />
            <el-tag :type="getTypeTag(announcement.type)" size="large" effect="dark">
              {{ getTypeLabel(announcement.type) }}
            </el-tag>
            <h3 class="title">
              {{ announcement.title }}
            </h3>
          </div>
          <div class="header-right">
            <el-button
              v-if="!announcement.isRead"
              type="primary"
              link
              size="small"
              @click="markRead(announcement)"
            >标记已读</el-button>
            <span v-else class="read-tag">已读</span>
            <span class="date">{{ formatDate(announcement.createdAt) }}</span>
          </div>
        </div>

        <div class="card-content">
          <p class="content">
            {{ announcement.content }}
          </p>
        </div>

        <div v-if="announcement.startTime || announcement.endTime" class="card-footer">
          <el-icon><Clock /></el-icon>
          <span class="validity">
            有效期：{{ formatDate(announcement.startTime) }} 至
            {{ formatDate(announcement.endTime) }}
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
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Clock, ArrowLeft } from '@element-plus/icons-vue'
import api from '@/services/api'

const router = useRouter()

const loading = ref(false)
const announcements = ref([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

const hasUnread = computed(() => announcements.value.some(a => !a.isRead))

// 加载公告列表
const loadAnnouncements = async () => {
  loading.value = true
  try {
    const { data } = await api.get('/announcements', {
      params: { page: currentPage.value, size: pageSize.value }
    })

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

// 标记单条已读
const markRead = async (announcement) => {
  try {
    await api.post(`/announcements/${announcement.id}/read`)
    announcement.isRead = true
  } catch {}
}

// 全部标记已读
const markAllRead = async () => {
  try {
    await api.post('/announcements/read-all')
    announcements.value.forEach(a => { a.isRead = true })
    ElMessage.success('已全部标记为已读')
  } catch {}
}

// 获取类型标签
const getTypeTag = type => {
  const typeMap = {
    NOTICE: 'info',
    WARNING: 'warning',
    ERROR: 'danger'
  }
  return typeMap[type] || 'info'
}

// 获取类型标签文本
const getTypeLabel = type => {
  const labelMap = {
    NOTICE: '通知',
    WARNING: '警告',
    ERROR: '紧急'
  }
  return labelMap[type] || '通知'
}

// 格式化日期
const formatDate = dateString => {
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
  font-family: 'Inter', sans-serif;
}

.page-header {
  text-align: center;
  margin-bottom: 40px;
}

.header-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.page-header h1 {
  font-size: 32px;
  font-weight: 600;
  color: #0F172A;
  margin-bottom: 8px;
  font-family: 'Calistoga', cursive;
}

.subtitle {
  font-size: 16px;
  color: #0F172A;
  opacity: 0.5;
  font-family: 'Inter', sans-serif;
}

.announcements-list {
  min-height: 400px;
}

.announcement-card {
  background: #FFFFFF;
  border-radius: 16px;
  padding: 24px;
  margin-bottom: 20px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
  border: 1px solid #E2E8F0;
  transition: all 0.2s ease;
  position: relative;
}

.announcement-card::before {
  display: none;
}

.announcement-card:hover {
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1), 0 2px 4px -2px rgba(0, 0, 0, 0.1);
  transform: translateY(-2px);
}

.announcement-card.type-WARNING {
  background: #FFFFFF;
  border-left: 3px solid #f59e0b;
}

.announcement-card.type-ERROR {
  background: #FFFFFF;
  border-color: #E2E8F0;
  border-left: 3px solid #ef4444;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
}

.announcement-card.type-ERROR:hover {
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1), 0 2px 4px -2px rgba(0, 0, 0, 0.1);
  transform: translateY(-2px);
}

.announcement-card.type-ERROR::before {
  display: none;
}

.announcement-card.type-WARNING::before {
  display: none;
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
  color: #0F172A;
  margin: 0;
  font-family: 'Calistoga', cursive;
}

.header-right {
  display: flex;
  align-items: center;
}

.date {
  font-size: 14px;
  color: #0F172A;
  opacity: 0.4;
  font-family: 'Inter', sans-serif;
}

.card-content {
  margin-bottom: 16px;
}

.content {
  font-size: 15px;
  line-height: 1.8;
  color: #0F172A;
  white-space: pre-wrap;
  word-break: break-word;
  font-family: 'Inter', sans-serif;
}

.card-footer {
  display: flex;
  align-items: center;
  gap: 8px;
  padding-top: 16px;
  border-top: 1px solid #E2E8F0;
  color: #0F172A;
  font-size: 14px;
  opacity: 0.5;
  font-family: 'Inter', sans-serif;
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

.announcement-card.unread {
  border-left: 3px solid var(--accent, #10B981);
  background: #f0fdf4;
}

.unread-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--accent, #10B981);
  display: inline-block;
  flex-shrink: 0;
}

.read-tag {
  color: #94a3b8;
  font-size: 12px;
}

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
