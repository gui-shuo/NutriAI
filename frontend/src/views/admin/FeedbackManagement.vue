<template>
  <div class="feedback-management">
    <!-- 统计卡片 -->
    <div class="stats-row">
      <div class="stat-card" v-for="(stat, key) in statCards" :key="key" :style="{ '--accent': stat.color }">
        <div class="stat-icon">{{ stat.icon }}</div>
        <div class="stat-info">
          <span class="stat-value">{{ stats[key] || 0 }}</span>
          <span class="stat-label">{{ stat.label }}</span>
        </div>
      </div>
    </div>

    <!-- 筛选栏 -->
    <el-card class="filter-card">
      <div class="filter-row">
        <el-select v-model="filterStatus" placeholder="状态筛选" clearable @change="fetchFeedbacks" style="width: 140px">
          <el-option v-for="(s, key) in FeedbackStatus" :key="key" :label="s.name" :value="key" />
        </el-select>
        <el-select v-model="filterType" placeholder="类型筛选" clearable @change="fetchFeedbacks" style="width: 140px">
          <el-option v-for="(t, key) in FeedbackTypes" :key="key" :label="`${t.icon} ${t.name}`" :value="key" />
        </el-select>
        <el-button @click="resetFilter">重置</el-button>
        <div class="filter-right">
          <el-tag>共 {{ total }} 条反馈</el-tag>
        </div>
      </div>
    </el-card>

    <!-- 反馈列表 -->
    <el-card class="list-card">
      <el-table :data="feedbacks" v-loading="loading" stripe style="width: 100%">
        <el-table-column label="类型" width="90" align="center">
          <template #default="{ row }">
            <span>{{ getTypeIcon(row.type) }} {{ getTypeName(row.type) }}</span>
          </template>
        </el-table-column>

        <el-table-column label="标题" min-width="180">
          <template #default="{ row }">
            <div class="cell-title" @click="showDetail(row)">{{ row.title }}</div>
          </template>
        </el-table-column>

        <el-table-column label="用户" width="100">
          <template #default="{ row }">
            <span class="cell-user">{{ row.username }}</span>
          </template>
        </el-table-column>

        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)" size="small">
              {{ getStatusName(row.status) }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="提交时间" width="160">
          <template #default="{ row }">
            {{ formatTime(row.createdAt) }}
          </template>
        </el-table-column>

        <el-table-column label="操作" width="180" align="center">
          <template #default="{ row }">
            <el-button type="primary" size="small" text @click="showDetail(row)">查看</el-button>
            <el-button type="success" size="small" text @click="openReply(row)">回复</el-button>
            <el-dropdown trigger="click" @command="(cmd) => handleStatusChange(row.id, cmd)">
              <el-button type="info" size="small" text>
                状态 <el-icon><ArrowDown /></el-icon>
              </el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="PENDING">待处理</el-dropdown-item>
                  <el-dropdown-item command="PROCESSING">处理中</el-dropdown-item>
                  <el-dropdown-item command="RESOLVED">已解决</el-dropdown-item>
                  <el-dropdown-item command="CLOSED">已关闭</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination">
        <el-pagination
          v-model:current-page="currentPage"
          :page-size="pageSize"
          :total="total"
          layout="total, prev, pager, next"
          @current-change="fetchFeedbacks"
        />
      </div>
    </el-card>

    <!-- 详情对话框 -->
    <el-dialog v-model="detailVisible" title="反馈详情" width="600px">
      <div v-if="detailItem" class="detail-content">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="类型">
            {{ getTypeIcon(detailItem.type) }} {{ getTypeName(detailItem.type) }}
          </el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="getStatusType(detailItem.status)" size="small">
              {{ getStatusName(detailItem.status) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="用户">{{ detailItem.username }}</el-descriptions-item>
          <el-descriptions-item label="联系方式">{{ detailItem.contactInfo || '未提供' }}</el-descriptions-item>
          <el-descriptions-item label="提交时间" :span="2">
            {{ formatTime(detailItem.createdAt) }}
          </el-descriptions-item>
          <el-descriptions-item label="标题" :span="2">
            <strong>{{ detailItem.title }}</strong>
          </el-descriptions-item>
        </el-descriptions>

        <div class="detail-section">
          <div class="detail-section-title">反馈内容</div>
          <div class="detail-text">{{ detailItem.content }}</div>
        </div>

        <div v-if="detailItem.adminReply" class="detail-section reply-section">
          <div class="detail-section-title">管理员回复</div>
          <div class="detail-text reply-text">{{ detailItem.adminReply }}</div>
          <div class="reply-time">回复于 {{ formatTime(detailItem.repliedAt) }}</div>
        </div>
      </div>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
        <el-button type="primary" @click="openReply(detailItem)">回复</el-button>
      </template>
    </el-dialog>

    <!-- 回复对话框 -->
    <el-dialog v-model="replyVisible" title="回复反馈" width="560px">
      <div v-if="replyTarget" class="reply-form">
        <div class="reply-feedback-info">
          <span class="reply-type">{{ getTypeIcon(replyTarget.type) }}</span>
          <span class="reply-title">{{ replyTarget.title }}</span>
        </div>

        <!-- 快捷模板按钮 -->
        <div class="template-bar">
          <span class="template-label">快捷模板：</span>
          <el-button size="small" type="warning" plain @click="applyTemplate('PROCESSING')">🔧 维护中</el-button>
          <el-button size="small" type="success" plain @click="applyTemplate('RESOLVED')">✅ 修复完毕</el-button>
        </div>

        <el-input
          v-model="replyContent"
          type="textarea"
          placeholder="请输入回复内容..."
          :rows="6"
          maxlength="1000"
          show-word-limit
        />
        <div class="reply-status">
          <span>回复后状态：</span>
          <el-radio-group v-model="replyStatus" size="small">
            <el-radio-button value="RESOLVED">已解决</el-radio-button>
            <el-radio-button value="PROCESSING">处理中</el-radio-button>
            <el-radio-button value="CLOSED">已关闭</el-radio-button>
          </el-radio-group>
        </div>
        <div class="email-hint">
          <el-icon><InfoFilled /></el-icon>
          <span>回复将同时通过邮件通知用户（需用户已绑定邮箱或在反馈中填写邮箱）</span>
        </div>
      </div>
      <template #footer>
        <el-button @click="replyVisible = false">取消</el-button>
        <el-button type="primary" :loading="replying" @click="handleReply">发送回复</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ArrowDown, InfoFilled } from '@element-plus/icons-vue'
import {
  getAllFeedbacks,
  replyFeedback,
  updateFeedbackStatus,
  getFeedbackStats,
  FeedbackTypes,
  FeedbackStatus
} from '@/services/feedback'
import { ElMessage, ElMessageBox } from 'element-plus'

const loading = ref(false)
const feedbacks = ref([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = 10

const filterStatus = ref('')
const filterType = ref('')

const stats = ref({})

const detailVisible = ref(false)
const detailItem = ref(null)

const replyVisible = ref(false)
const replyTarget = ref(null)
const replyContent = ref('')
const replyStatus = ref('RESOLVED')
const replying = ref(false)

const statCards = {
  total: { icon: '📊', label: '总反馈', color: '#409eff' },
  pending: { icon: '⏳', label: '待处理', color: '#e6a23c' },
  processing: { icon: '🔧', label: '处理中', color: '#409eff' },
  resolved: { icon: '✅', label: '已解决', color: '#67c23a' }
}

const fetchFeedbacks = async () => {
  loading.value = true
  try {
    const params = {
      page: currentPage.value - 1,
      size: pageSize
    }
    if (filterStatus.value) params.status = filterStatus.value
    if (filterType.value) params.type = filterType.value

    const res = await getAllFeedbacks(params)
    if (res.data.code === 200) {
      feedbacks.value = res.data.data.content || []
      total.value = res.data.data.totalElements || 0
    }
  } catch (err) {
    console.error('获取反馈列表失败:', err)
  } finally {
    loading.value = false
  }
}

const fetchStats = async () => {
  try {
    const res = await getFeedbackStats()
    if (res.data.code === 200) {
      stats.value = res.data.data
    }
  } catch (err) {
    console.error('获取统计失败:', err)
  }
}

const resetFilter = () => {
  filterStatus.value = ''
  filterType.value = ''
  currentPage.value = 1
  fetchFeedbacks()
}

const showDetail = (item) => {
  detailItem.value = item
  detailVisible.value = true
}

const openReply = (item) => {
  replyTarget.value = item
  replyContent.value = item.adminReply || ''
  replyStatus.value = 'RESOLVED'
  replyVisible.value = true
  detailVisible.value = false
}

const applyTemplate = (templateType) => {
  if (!replyTarget.value) return
  const username = replyTarget.value.username || '用户'
  const title = replyTarget.value.title || '您的反馈'

  if (templateType === 'PROCESSING') {
    replyContent.value = `尊敬的${username}，您好！\n\n感谢您提交的反馈。您反馈的问题「${title}」我们已收到并确认，目前该问题正在维护处理中，我们的技术团队正在积极排查和修复。\n\n我们会尽快完成处理，届时将再次通知您。感谢您的耐心等待与支持！`
    replyStatus.value = 'PROCESSING'
  } else if (templateType === 'RESOLVED') {
    replyContent.value = `尊敬的${username}，您好！\n\n感谢您提交的反馈。您反馈的问题「${title}」已经修复完毕，请您更新至最新版本体验。\n\n如问题仍然存在或有其他建议，欢迎继续反馈。感谢您对 NutriAI 的支持！`
    replyStatus.value = 'RESOLVED'
  }
}

const handleReply = async () => {
  if (!replyContent.value.trim()) {
    ElMessage.warning('请输入回复内容')
    return
  }
  replying.value = true
  try {
    const res = await replyFeedback(replyTarget.value.id, replyContent.value, replyStatus.value)
    if (res.data.code === 200) {
      ElMessage.success('回复成功')
      replyVisible.value = false
      fetchFeedbacks()
      fetchStats()
    } else {
      ElMessage.error(res.data.message || '回复失败')
    }
  } catch (err) {
    ElMessage.error('回复失败')
  } finally {
    replying.value = false
  }
}

const handleStatusChange = async (id, status) => {
  try {
    const res = await updateFeedbackStatus(id, status)
    if (res.data.code === 200) {
      ElMessage.success('状态已更新')
      fetchFeedbacks()
      fetchStats()
    }
  } catch (err) {
    ElMessage.error('更新失败')
  }
}

const getTypeIcon = (type) => FeedbackTypes[type]?.icon || '📋'
const getTypeName = (type) => FeedbackTypes[type]?.name || '其他'
const getStatusName = (status) => FeedbackStatus[status]?.name || status
const getStatusType = (status) => FeedbackStatus[status]?.type || 'info'

const formatTime = (time) => {
  if (!time) return ''
  return new Date(time).toLocaleString('zh-CN')
}

onMounted(() => {
  fetchFeedbacks()
  fetchStats()
})
</script>

<style scoped lang="scss">
.feedback-management {
  font-family: 'Inter', sans-serif;

  .stats-row {
    display: grid;
    grid-template-columns: repeat(4, 1fr);
    gap: 16px;
    margin-bottom: 16px;

    .stat-card {
      display: flex;
      align-items: center;
      gap: 14px;
      padding: 18px 20px;
      background: #FFFFFF;
      border: 1px solid #E2E8F0;
      border-radius: 12px;
      box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
      border-left: 4px solid var(--accent);

      .stat-icon {
        font-size: 28px;
      }

      .stat-info {
        display: flex;
        flex-direction: column;

        .stat-value {
          font-size: 24px;
          font-weight: 700;
          color: #0F172A;
          font-family: 'Inter', sans-serif;
        }

        .stat-label {
          font-size: 12px;
          color: #0F172A;
          opacity: 0.55;
        }
      }
    }
  }

  .filter-card {
    margin-bottom: 16px;
    border: 1px solid #E2E8F0;
    border-radius: 12px;
    box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
    background: #FFFFFF;

    .filter-row {
      display: flex;
      gap: 12px;
      align-items: center;

      .filter-right {
        margin-left: auto;
      }
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

    .cell-title {
      color: #0F172A;
      font-weight: 500;
      cursor: pointer;

      &:hover {
        color: #10B981;
      }
    }

    .cell-user {
      color: #0F172A;
      opacity: 0.6;
      font-size: 13px;
    }
  }

  .pagination {
    display: flex;
    justify-content: center;
    margin-top: 16px;
  }
}

// 详情对话框
.detail-content {
  .detail-section {
    margin-top: 16px;

    .detail-section-title {
      font-size: 14px;
      font-weight: 600;
      color: #0F172A;
      margin-bottom: 8px;
      padding-left: 10px;
      border-left: 3px solid #10B981;
      font-family: 'Inter', sans-serif;
    }

    .detail-text {
      background: #FAFAFA;
      padding: 14px;
      border: 1px solid #E2E8F0;
      border-radius: 8px;
      font-size: 14px;
      line-height: 1.8;
      color: #0F172A;
      white-space: pre-wrap;
    }

    &.reply-section {
      background: #FAFAFA;
      padding: 14px;
      border-radius: 10px;
      border: 1px solid #E2E8F0;

      .reply-text {
        background: #FFFFFF;
        border: 1px solid #E2E8F0;
      }

      .reply-time {
        margin-top: 8px;
        font-size: 12px;
        color: #0F172A;
        opacity: 0.45;
      }
    }
  }
}

// 回复对话框
.reply-form {
  .reply-feedback-info {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 10px 14px;
    background: #F1F5F9;
    border: 1px solid #E2E8F0;
    border-radius: 8px;
    margin-bottom: 14px;

    .reply-type {
      font-size: 18px;
    }

    .reply-title {
      font-size: 14px;
      font-weight: 500;
      color: #0F172A;
    }
  }

  .reply-status {
    display: flex;
    align-items: center;
    gap: 10px;
    margin-top: 14px;
    font-size: 14px;
    color: #0F172A;
    opacity: 0.7;
  }

  .template-bar {
    display: flex;
    align-items: center;
    gap: 8px;
    margin-bottom: 14px;

    .template-label {
      font-size: 13px;
      color: #0F172A;
      opacity: 0.6;
      white-space: nowrap;
    }
  }

  .email-hint {
    display: flex;
    align-items: center;
    gap: 6px;
    margin-top: 12px;
    font-size: 12px;
    color: #0F172A;
    opacity: 0.45;

    .el-icon {
      font-size: 14px;
      color: #E2E8F0;
    }
  }
}
</style>
