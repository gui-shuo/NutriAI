<template>
  <view class="page">
    <!-- Submit Feedback Form -->
    <view class="card">
      <view class="card-title font-bold">提交反馈</view>

      <!-- Type Selector -->
      <view class="type-selector">
        <view
          v-for="t in feedbackTypes"
          :key="t.value"
          class="type-tag"
          :class="{ active: form.type === t.value }"
          @tap="form.type = t.value"
        >
          {{ t.label }}
        </view>
      </view>

      <!-- Title -->
      <view class="input-group">
        <text class="label">标题 <text class="required">*</text></text>
        <input v-model="form.title" placeholder="请输入反馈标题" maxlength="50" />
      </view>

      <!-- Content -->
      <view class="input-group textarea-group">
        <text class="label">详细内容 <text class="required">*</text></text>
        <textarea
          v-model="form.content"
          placeholder="请详细描述您的反馈内容"
          maxlength="500"
          :show-count="true"
        />
        <text class="char-count text-sm text-secondary">{{ form.content.length }}/500</text>
      </view>

      <!-- Contact -->
      <view class="input-group">
        <text class="label">联系方式（选填）</text>
        <input v-model="form.contact" placeholder="邮箱或手机号，方便我们回复" />
      </view>

      <button class="btn-primary submit-btn" :loading="submitting" @tap="submitFeedback">
        提交反馈
      </button>
    </view>

    <!-- My Feedbacks -->
    <view class="card">
      <view class="card-title font-bold">我的反馈</view>

      <view v-if="loading" class="loading-state">
        <text class="text-secondary">加载中...</text>
      </view>

      <view v-else-if="feedbacks.length">
        <view
          v-for="item in feedbacks"
          :key="item.id"
          class="feedback-item"
          @tap="toggleExpand(item.id)"
        >
          <view class="feedback-header">
            <view class="feedback-title-row">
              <text class="feedback-title">{{ item.title }}</text>
              <view class="flex" style="gap: 10rpx;">
                <text class="badge" :class="'badge-' + item.type">{{ typeLabel(item.type) }}</text>
                <text class="badge" :class="'status-' + item.status">{{ statusLabel(item.status) }}</text>
              </view>
            </view>
            <text class="feedback-time text-sm text-secondary">{{ formatTime(item.createdAt) }}</text>
          </view>

          <!-- Expanded Content -->
          <view v-if="expandedId === item.id" class="feedback-detail">
            <view class="detail-content">{{ item.content }}</view>
            <view v-if="item.reply" class="reply-box">
              <text class="reply-label">管理员回复：</text>
              <text class="reply-content">{{ item.reply }}</text>
            </view>
          </view>
        </view>
      </view>

      <view v-else class="empty-state">
        <text class="empty-icon">📭</text>
        <text class="empty-text">暂无反馈记录</text>
      </view>
    </view>

    <view class="safe-bottom"></view>
  </view>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { feedbackApi } from '@/services/api'
import { checkLogin, formatTime } from '@/utils/common'

const feedbackTypes = [
  { value: 'SUGGESTION', label: '功能建议' },
  { value: 'BUG', label: 'Bug反馈' },
  { value: 'OTHER', label: '其他' }
]

const form = reactive({
  type: 'SUGGESTION',
  title: '',
  content: '',
  contact: ''
})

const submitting = ref(false)
const loading = ref(false)
const feedbacks = ref<any[]>([])
const expandedId = ref<number | null>(null)

const typeMap: Record<string, string> = { SUGGESTION: '功能建议', BUG: 'Bug反馈', OTHER: '其他' }
const statusMap: Record<string, string> = { PENDING: '待处理', REPLIED: '已回复', CLOSED: '已关闭' }

function typeLabel(type: string) {
  return typeMap[type] || type
}

function statusLabel(status: string) {
  return statusMap[status] || status
}

function toggleExpand(id: number) {
  expandedId.value = expandedId.value === id ? null : id
}

async function submitFeedback() {
  if (!form.title.trim()) {
    uni.showToast({ title: '请输入标题', icon: 'none' })
    return
  }
  if (!form.content.trim()) {
    uni.showToast({ title: '请输入反馈内容', icon: 'none' })
    return
  }
  submitting.value = true
  try {
    const data: any = {
      type: form.type,
      title: form.title.trim(),
      content: form.content.trim()
    }
    if (form.contact.trim()) data.contact = form.contact.trim()

    const res = await feedbackApi.submit(data)
    if (res.code === 200) {
      uni.showToast({ title: '提交成功', icon: 'success' })
      form.title = ''
      form.content = ''
      form.contact = ''
      loadFeedbacks()
    } else {
      uni.showToast({ title: res.message || '提交失败', icon: 'none' })
    }
  } catch {
    uni.showToast({ title: '提交失败', icon: 'none' })
  } finally {
    submitting.value = false
  }
}

async function loadFeedbacks() {
  loading.value = true
  try {
    const res = await feedbackApi.getMyFeedbacks({ page: 0, size: 20 })
    if (res.code === 200) {
      feedbacks.value = res.data?.content || res.data?.records || res.data?.list || res.data || []
    }
  } catch {} finally {
    loading.value = false
  }
}

onMounted(() => {
  if (!checkLogin()) return
  loadFeedbacks()
})
</script>

<style scoped lang="scss">
.page {
  min-height: 100vh;
  background: $background;
  padding: 20rpx 0 30rpx;
  font-family: 'Inter', sans-serif;
}

.card {
  background: $card;
  border-radius: $radius-xl;
  margin: 0 24rpx 20rpx;
  padding: 30rpx;
  border: 1rpx solid $border;
  box-shadow: $shadow-sm;
}

.card-title {
  font-size: 32rpx;
  margin-bottom: 24rpx;
  color: $foreground;
  font-family: 'Calistoga', cursive;
}

.type-selector {
  display: flex;
  gap: 16rpx;
  margin-bottom: 24rpx;
}

.type-tag {
  flex: 1;
  text-align: center;
  padding: 16rpx 0;
  border: 1rpx solid $border;
  border-radius: $radius-lg;
  font-size: 26rpx;
  color: $muted-foreground;
  font-family: 'Inter', sans-serif;
  transition: all 0.2s ease;

  &.active {
    border-color: $accent;
    color: $accent;
    background: rgba(16, 185, 129, 0.06);
    box-shadow: $shadow-sm;
  }
}

.input-group {
  background: $background;
  border: 1rpx solid $border;
  border-radius: $radius-lg;
  padding: 16rpx 24rpx;
  margin-bottom: 16rpx;
}

.input-group .label {
  font-size: 24rpx;
  color: $muted-foreground;
  margin-bottom: 6rpx;
  display: block;
  font-family: 'Inter', sans-serif;
}

.required {
  color: $uni-error;
}

.input-group input {
  height: 48rpx;
  font-size: 28rpx;
  font-family: 'Inter', sans-serif;
}

.textarea-group {
  position: relative;
}

.textarea-group textarea {
  width: 100%;
  height: 200rpx;
  font-size: 28rpx;
  line-height: 1.6;
  font-family: 'Inter', sans-serif;
}

.char-count {
  text-align: right;
  display: block;
  margin-top: 6rpx;
  color: $muted-foreground;
  font-size: 24rpx;
}

.submit-btn {
  margin-top: 10rpx;
  height: 84rpx;
  line-height: 84rpx;
  border-radius: $radius-xl;
  font-size: 30rpx;
  background: $gradient-accent !important;
  color: $accent-foreground !important;
  border: none;
  box-shadow: $shadow-accent;
  font-family: 'Calistoga', cursive;
}

.submit-btn::after {
  border: none;
}

.loading-state {
  text-align: center;
  padding: 40rpx 0;
  color: $muted-foreground;
}

.feedback-item {
  padding: 20rpx 0;
  border-bottom: 1rpx solid $border;

  &:last-child {
    border-bottom: none;
  }
}

.feedback-header {
  display: flex;
  flex-direction: column;
  gap: 8rpx;
}

.feedback-title-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.feedback-title {
  font-size: 28rpx;
  font-weight: 600;
  color: $foreground;
  flex: 1;
  margin-right: 16rpx;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-family: 'Calistoga', cursive;
}

.feedback-time {
  display: block;
  color: $muted-foreground;
  font-size: 24rpx;
}

.badge {
  font-size: 20rpx;
  padding: 4rpx 12rpx;
  border-radius: $radius-full;
  white-space: nowrap;
  border: none;
  font-family: 'JetBrains Mono', monospace;
}

.badge-SUGGESTION {
  background: rgba(16, 185, 129, 0.1);
  color: $accent;
}

.badge-BUG {
  background: rgba(245, 158, 11, 0.1);
  color: $uni-warning;
}

.badge-OTHER {
  background: $muted;
  color: $muted-foreground;
}

.status-PENDING {
  background: rgba(245, 158, 11, 0.1);
  color: $uni-warning;
}

.status-REPLIED {
  background: rgba(16, 185, 129, 0.1);
  color: $uni-success;
}

.status-CLOSED {
  background: $muted;
  color: $muted-foreground;
}

.feedback-detail {
  margin-top: 16rpx;
  padding: 16rpx;
  background: $muted;
  border-radius: $radius-lg;
  border: 1rpx solid $border;
}

.detail-content {
  font-size: 26rpx;
  color: $foreground;
  line-height: 1.7;
  word-break: break-all;
  font-family: 'Inter', sans-serif;
}

.reply-box {
  margin-top: 16rpx;
  padding-top: 16rpx;
  border-top: 1rpx solid $border;
}

.reply-label {
  font-size: 24rpx;
  color: $accent;
  font-weight: 600;
  display: block;
  margin-bottom: 6rpx;
  font-family: 'Inter', sans-serif;
}

.reply-content {
  font-size: 26rpx;
  color: $foreground;
  line-height: 1.7;
  font-family: 'Inter', sans-serif;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 60rpx 0;
}

.empty-icon {
  font-size: 60rpx;
  margin-bottom: 16rpx;
}

.empty-text {
  font-size: 26rpx;
  color: $muted-foreground;
  font-family: 'Inter', sans-serif;
}

.safe-bottom {
  height: env(safe-area-inset-bottom);
}
</style>