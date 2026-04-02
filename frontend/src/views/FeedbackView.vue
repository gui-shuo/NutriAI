<template>
  <div class="feedback-view">
    <!-- 顶部导航 -->
    <nav class="top-nav">
      <div class="nav-inner">
        <div class="nav-left">
          <el-button text @click="router.push('/')">
            <el-icon><ArrowLeft /></el-icon>
            返回首页
          </el-button>
          <h2 class="page-title">📮 意见反馈</h2>
        </div>
      </div>
    </nav>

    <main class="main-area">
      <div class="content-grid">
        <!-- 左侧：提交反馈 -->
        <div class="submit-section">
          <el-card class="form-card">
            <template #header>
              <div class="card-header">
                <span class="section-title">提交反馈</span>
                <span class="section-hint">我们重视您的每一条建议</span>
              </div>
            </template>

            <!-- 反馈类型选择 -->
            <div class="type-selector">
              <div class="type-label">反馈类型</div>
              <div class="type-grid">
                <div
                  v-for="(item, key) in FeedbackTypes"
                  :key="key"
                  class="type-card"
                  :class="{ active: form.type === key }"
                  :style="{ '--accent': item.color }"
                  @click="form.type = key"
                >
                  <span class="type-icon">{{ item.icon }}</span>
                  <span class="type-name">{{ item.name }}</span>
                </div>
              </div>
            </div>

            <el-form ref="formRef" :model="form" :rules="rules" label-position="top" class="feedback-form">
              <el-form-item label="标题" prop="title">
                <el-input
                  v-model="form.title"
                  placeholder="简要描述您的问题或建议"
                  maxlength="100"
                  show-word-limit
                />
              </el-form-item>

              <el-form-item label="详细描述" prop="content">
                <el-input
                  v-model="form.content"
                  type="textarea"
                  placeholder="请详细描述您遇到的问题、复现步骤或建议内容..."
                  :rows="6"
                  maxlength="2000"
                  show-word-limit
                />
              </el-form-item>

              <el-form-item label="联系方式（选填）">
                <el-input
                  v-model="form.contactInfo"
                  placeholder="邮箱或手机号，方便我们联系您"
                />
              </el-form-item>

              <el-form-item>
                <el-button
                  type="primary"
                  :loading="submitting"
                  class="submit-btn"
                  @click="handleSubmit"
                >
                  <el-icon><Promotion /></el-icon>
                  提交反馈
                </el-button>
              </el-form-item>
            </el-form>
          </el-card>
        </div>

        <!-- 右侧：我的反馈历史 -->
        <div class="history-section">
          <el-card class="history-card">
            <template #header>
              <div class="card-header">
                <span class="section-title">我的反馈</span>
                <el-tag>{{ total }} 条</el-tag>
              </div>
            </template>

            <el-skeleton :loading="loading" animated :rows="6">
              <div v-if="feedbacks.length > 0" class="feedback-list">
                <div
                  v-for="item in feedbacks"
                  :key="item.id"
                  class="feedback-item"
                  @click="showDetail(item)"
                >
                  <div class="item-header">
                    <span class="item-type-icon">{{ getTypeIcon(item.type) }}</span>
                    <span class="item-title">{{ item.title }}</span>
                    <el-tag :type="getStatusType(item.status)" size="small">
                      {{ getStatusName(item.status) }}
                    </el-tag>
                  </div>
                  <div class="item-content">{{ item.content }}</div>
                  <div class="item-footer">
                    <span class="item-time">{{ formatTime(item.createdAt) }}</span>
                    <span v-if="item.adminReply" class="item-replied">
                      <el-icon><ChatDotSquare /></el-icon>
                      已回复
                    </span>
                  </div>
                </div>
              </div>
              <el-empty v-else description="暂无反馈记录" :image-size="80" />
            </el-skeleton>

            <!-- 分页 -->
            <div v-if="total > pageSize" class="pagination">
              <el-pagination
                v-model:current-page="currentPage"
                :page-size="pageSize"
                :total="total"
                layout="prev, pager, next"
                small
                @current-change="fetchFeedbacks"
              />
            </div>
          </el-card>
        </div>
      </div>
    </main>

    <!-- 详情对话框 -->
    <el-dialog v-model="detailVisible" title="反馈详情" width="560px">
      <div v-if="detailItem" class="detail-content">
        <div class="detail-row">
          <span class="detail-label">类型</span>
          <span>{{ getTypeIcon(detailItem.type) }} {{ getTypeName(detailItem.type) }}</span>
        </div>
        <div class="detail-row">
          <span class="detail-label">状态</span>
          <el-tag :type="getStatusType(detailItem.status)" size="small">
            {{ getStatusName(detailItem.status) }}
          </el-tag>
        </div>
        <div class="detail-row">
          <span class="detail-label">标题</span>
          <span class="detail-title">{{ detailItem.title }}</span>
        </div>
        <div class="detail-row vertical">
          <span class="detail-label">描述</span>
          <div class="detail-text">{{ detailItem.content }}</div>
        </div>
        <div class="detail-row">
          <span class="detail-label">提交时间</span>
          <span>{{ formatFullTime(detailItem.createdAt) }}</span>
        </div>

        <!-- 管理员回复 -->
        <div v-if="detailItem.adminReply" class="admin-reply-section">
          <div class="reply-header">
            <el-icon><ChatDotSquare /></el-icon>
            管理员回复
          </div>
          <div class="reply-content">{{ detailItem.adminReply }}</div>
          <div class="reply-time">{{ formatFullTime(detailItem.repliedAt) }}</div>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ArrowLeft, Promotion, ChatDotSquare } from '@element-plus/icons-vue'
import { submitFeedback, getMyFeedbacks, FeedbackTypes, FeedbackStatus } from '@/services/feedback'
import message from '@/utils/message'

const router = useRouter()
const formRef = ref(null)
const submitting = ref(false)
const loading = ref(false)

const form = reactive({
  type: 'BUG',
  title: '',
  content: '',
  contactInfo: ''
})

const rules = {
  title: [{ required: true, message: '请输入标题', trigger: 'blur' }],
  content: [{ required: true, message: '请输入详细描述', trigger: 'blur' }]
}

const feedbacks = ref([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = 10

const detailVisible = ref(false)
const detailItem = ref(null)

const handleSubmit = async () => {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    const res = await submitFeedback(form)
    if (res.data.code === 200) {
      message.success('反馈提交成功，感谢您的意见！')
      form.title = ''
      form.content = ''
      form.contactInfo = ''
      currentPage.value = 1
      fetchFeedbacks()
    } else {
      message.error(res.data.message || '提交失败')
    }
  } catch (err) {
    message.error('提交失败，请稍后重试')
  } finally {
    submitting.value = false
  }
}

const fetchFeedbacks = async () => {
  loading.value = true
  try {
    const res = await getMyFeedbacks(currentPage.value - 1, pageSize)
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

const showDetail = (item) => {
  detailItem.value = item
  detailVisible.value = true
}

const getTypeIcon = (type) => FeedbackTypes[type]?.icon || '📋'
const getTypeName = (type) => FeedbackTypes[type]?.name || '其他'
const getStatusName = (status) => FeedbackStatus[status]?.name || status
const getStatusType = (status) => FeedbackStatus[status]?.type || 'info'

const formatTime = (time) => {
  if (!time) return ''
  const d = new Date(time)
  const now = new Date()
  const diff = now - d
  const days = Math.floor(diff / (1000 * 60 * 60 * 24))
  if (days === 0) return '今天'
  if (days === 1) return '昨天'
  if (days < 7) return `${days}天前`
  return d.toLocaleDateString('zh-CN')
}

const formatFullTime = (time) => {
  if (!time) return ''
  return new Date(time).toLocaleString('zh-CN')
}

onMounted(() => {
  fetchFeedbacks()
})
</script>

<style scoped lang="scss">
.feedback-view {
  min-height: 100vh;
  background: #FAFAFA;
  font-family: 'Inter', sans-serif;
}

.top-nav {
  position: sticky;
  top: 0;
  z-index: 100;
  background: #FFFFFF;
  border-bottom: 1px solid #E2E8F0;
  box-shadow: 0 1px 3px 0 rgba(0, 0, 0, 0.05);
}

.nav-inner {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 24px;
  height: 56px;
  display: flex;
  align-items: center;
}

.nav-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.page-title {
  font-size: 18px;
  font-family: 'Calistoga', 'Inter', sans-serif;
  font-weight: 600;
  color: #0F172A;
  margin: 0;
}

.main-area {
  max-width: 1200px;
  margin: 0 auto;
  padding: 24px;
}

.content-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.section-title {
  font-size: 16px;
  font-family: 'Calistoga', 'Inter', sans-serif;
  font-weight: 600;
  color: #0F172A;
  border-left: 3px solid #10B981;
  padding-left: 10px;
}

.section-hint {
  font-size: 12px;
  color: #0F172A;
  opacity: 0.5;
}

// 反馈类型选择
.type-selector {
  margin-bottom: 20px;

  .type-label {
    font-size: 14px;
    font-weight: 500;
    color: #0F172A;
    margin-bottom: 10px;
  }

  .type-grid {
    display: grid;
    grid-template-columns: repeat(4, 1fr);
    gap: 10px;
  }

  .type-card {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 6px;
    padding: 14px 8px;
    border-radius: 12px;
    border: 1px solid #E2E8F0;
    background: #FFFFFF;
    cursor: pointer;
    transition: all 0.25s;

    &:hover {
      border-color: #34D399;
      transform: translateY(-2px);
      box-shadow: 0 4px 6px -1px rgba(16, 185, 129, 0.1);
    }

    &.active {
      border-color: #10B981;
      background: linear-gradient(135deg, rgba(16, 185, 129, 0.04), rgba(77, 124, 255, 0.08));
      box-shadow: 0 0 0 1px #10B981;
    }

    .type-icon {
      font-size: 24px;
    }

    .type-name {
      font-size: 12px;
      font-weight: 500;
      color: #0F172A;
    }
  }
}

.feedback-form {
  :deep(.el-form-item__label) {
    font-weight: 500;
    font-family: 'Inter', sans-serif;
    color: #0F172A;
  }

  :deep(.el-input__wrapper:focus-within),
  :deep(.el-textarea__inner:focus) {
    box-shadow: 0 0 0 2px rgba(16, 185, 129, 0.2) !important;
    border-color: #10B981 !important;
  }
}

.submit-btn {
  width: 100%;
  height: 42px;
  font-size: 15px;
  font-weight: 600;
  font-family: 'Inter', sans-serif;
  border-radius: 12px;
  border: none;
  background: linear-gradient(135deg, #10B981, #34D399) !important;
  box-shadow: 0 4px 6px -1px rgba(16, 185, 129, 0.25);
  transition: box-shadow 0.2s, transform 0.2s;

  &:hover {
    transform: translateY(-1px);
    box-shadow: 0 10px 15px -3px rgba(16, 185, 129, 0.3);
  }
}

// 反馈历史
.form-card,
.history-card {
  border-radius: 16px;
  border: 1px solid #E2E8F0;
  box-shadow: 0 1px 3px 0 rgba(0, 0, 0, 0.1), 0 1px 2px -1px rgba(0, 0, 0, 0.1);
  background: #FFFFFF;
}

.feedback-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.feedback-item {
  padding: 14px;
  border-radius: 12px;
  background: #FAFAFA;
  border: 1px solid #E2E8F0;
  cursor: pointer;
  transition: all 0.2s;

  &:hover {
    background: #F1F5F9;
    border-color: #34D399;
    transform: translateX(2px);
    box-shadow: 0 2px 4px -1px rgba(16, 185, 129, 0.1);
  }

  .item-header {
    display: flex;
    align-items: center;
    gap: 8px;
    margin-bottom: 6px;

    .item-type-icon {
      font-size: 16px;
    }

    .item-title {
      flex: 1;
      font-size: 14px;
      font-weight: 600;
      color: #0F172A;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }
  }

  .item-content {
    font-size: 13px;
    color: #0F172A;
    opacity: 0.6;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    margin-bottom: 6px;
  }

  .item-footer {
    display: flex;
    justify-content: space-between;
    align-items: center;
    font-size: 12px;
    color: #0F172A;
    opacity: 0.45;

    .item-replied {
      display: flex;
      align-items: center;
      gap: 4px;
      color: #10B981;
      opacity: 1;
    }
  }
}

.pagination {
  display: flex;
  justify-content: center;
  margin-top: 16px;
}

// 详情对话框
.detail-content {
  .detail-row {
    display: flex;
    align-items: center;
    gap: 12px;
    padding: 10px 0;
    border-bottom: 1px solid #E2E8F0;
    font-size: 14px;

    &.vertical {
      flex-direction: column;
      align-items: flex-start;
    }

    .detail-label {
      font-weight: 500;
      font-family: 'Inter', sans-serif;
      color: #0F172A;
      opacity: 0.6;
      min-width: 60px;
    }

    .detail-title {
      font-weight: 600;
      color: #0F172A;
    }

    .detail-text {
      background: #FAFAFA;
      padding: 12px;
      border-radius: 12px;
      border: 1px solid #E2E8F0;
      width: 100%;
      line-height: 1.8;
      color: #0F172A;
      white-space: pre-wrap;
    }
  }
}

.admin-reply-section {
  margin-top: 16px;
  padding: 16px;
  background: linear-gradient(135deg, rgba(16, 185, 129, 0.04), rgba(77, 124, 255, 0.08));
  border-radius: 12px;
  border: 1px solid #34D399;
  box-shadow: 0 1px 3px 0 rgba(16, 185, 129, 0.1);
  position: relative;

  .reply-header {
    display: flex;
    align-items: center;
    gap: 6px;
    font-size: 14px;
    font-family: 'Calistoga', 'Inter', sans-serif;
    font-weight: 600;
    color: #10B981;
    margin-bottom: 8px;
  }

  .reply-content {
    font-size: 14px;
    line-height: 1.8;
    color: #0F172A;
    white-space: pre-wrap;
  }

  .reply-time {
    margin-top: 8px;
    font-size: 12px;
    color: #0F172A;
    opacity: 0.45;
  }
}

@media (max-width: 900px) {
  .content-grid {
    grid-template-columns: 1fr;
  }
  .type-grid {
    grid-template-columns: repeat(2, 1fr) !important;
  }
}

@media (max-width: 768px) {
  .main-area {
    padding: 16px;
  }
}
</style>
