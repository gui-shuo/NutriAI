<template>
  <el-card class="invitation-panel">
    <template #header>
      <div class="card-header">
        <span class="title">邀请好友</span>
        <el-tag type="success"> 每人+50成长值 </el-tag>
      </div>
    </template>

    <div class="invitation-content">
      <!-- 邀请码展示 -->
      <div class="invitation-code-section">
        <div class="code-label">我的邀请码</div>
        <div class="code-display">
          <span class="code">{{ invitationCode }}</span>
          <el-button type="primary" size="small" @click="copyInvitationCode">
            <el-icon><DocumentCopy /></el-icon>
            复制
          </el-button>
        </div>
      </div>

      <!-- 邀请链接 -->
      <div class="invitation-link-section">
        <div class="link-label">邀请链接</div>
        <el-input v-model="invitationLink" readonly size="default">
          <template #append>
            <el-button @click="copyInvitationLink">
              <el-icon><Link /></el-icon>
              复制链接
            </el-button>
          </template>
        </el-input>
      </div>

      <!-- 邀请统计 -->
      <div class="invitation-stats">
        <div class="stat-box">
          <div
            class="stat-icon"
            style="background: linear-gradient(135deg, #667eea 0%, #764ba2 100%)"
          >
            <el-icon><User /></el-icon>
          </div>
          <div class="stat-info">
            <span class="stat-value">{{ memberInfo?.invitationCount || 0 }}</span>
            <span class="stat-label">已邀请</span>
          </div>
        </div>
        <div class="stat-box">
          <div
            class="stat-icon"
            style="background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%)"
          >
            <el-icon><Present /></el-icon>
          </div>
          <div class="stat-info">
            <span class="stat-value">{{ totalReward }}</span>
            <span class="stat-label">奖励成长值</span>
          </div>
        </div>
      </div>

      <!-- 邀请记录 -->
      <div class="invitation-records">
        <div class="records-header">
          <span>最近邀请</span>
          <el-button text size="small" @click="viewAllRecords"> 查看全部 </el-button>
        </div>
        <el-skeleton :loading="loading" animated :rows="3">
          <div v-if="records.length > 0" class="records-list">
            <div v-for="record in records.slice(0, 3)" :key="record.id" class="record-item">
              <div class="record-avatar">
                <el-avatar :size="40">
                  {{ record.inviteeName?.charAt(0) || '?' }}
                </el-avatar>
              </div>
              <div class="record-info">
                <span class="record-name">{{ record.inviteeName || '新用户' }}</span>
                <span class="record-time">{{ formatTime(record.acceptedAt) }}</span>
              </div>
              <div class="record-reward">
                <el-tag v-if="record.isRewarded" type="success" size="small">
                  +{{ record.rewardGrowth }}
                </el-tag>
                <el-tag v-else type="info" size="small"> 待接受 </el-tag>
              </div>
            </div>
          </div>
          <el-empty v-else description="暂无邀请记录" :image-size="80" />
        </el-skeleton>
      </div>
    </div>

    <!-- 查看全部对话框 -->
    <el-dialog
      v-model="showAllDialog"
      title="邀请记录"
      width="600px"
      :close-on-click-modal="false"
      custom-class="member-dialog"
    >
      <el-skeleton :loading="allLoading" animated :rows="5">
        <div v-if="allRecords.length > 0" class="all-records-list">
          <div v-for="record in allRecords" :key="record.id" class="all-record-item">
            <div class="record-avatar">
              <el-avatar :size="48">
                {{ record.inviteeName?.charAt(0) || '?' }}
              </el-avatar>
            </div>
            <div class="record-details">
              <div class="record-header">
                <span class="record-name">{{ record.inviteeName || '新用户' }}</span>
                <el-tag v-if="record.isRewarded" type="success" size="small">
                  已奖励 +{{ record.rewardGrowth }}
                </el-tag>
                <el-tag v-else type="info" size="small"> 待激活 </el-tag>
              </div>
              <div class="record-meta">
                <span class="meta-item">
                  <el-icon><Calendar /></el-icon>
                  邀请时间: {{ formatFullTime(record.invitedAt) }}
                </span>
                <span v-if="record.acceptedAt" class="meta-item">
                  <el-icon><CircleCheck /></el-icon>
                  接受时间: {{ formatFullTime(record.acceptedAt) }}
                </span>
              </div>
              <div v-if="record.status" class="record-status">
                <span :class="['status-badge', record.status.toLowerCase()]">
                  {{ getStatusText(record.status) }}
                </span>
              </div>
            </div>
          </div>
        </div>
        <el-empty v-else description="暂无邀请记录" :image-size="100" />
      </el-skeleton>

      <template #footer>
        <el-button @click="showAllDialog = false"> 关闭 </el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { DocumentCopy, Link, User, Present, Calendar, CircleCheck } from '@element-plus/icons-vue'
import { useClipboard } from '@vueuse/core'
import { getInvitationRecords } from '@/services/member'
import message from '@/utils/message'

const props = defineProps({
  memberInfo: {
    type: Object,
    default: null
  }
})

const { copy } = useClipboard()
const loading = ref(false)
const invitationCode = computed(() => props.memberInfo?.invitationCode || '')
const invitationLink = computed(() => {
  const code = props.memberInfo?.invitationCode
  if (!code) return ''
  return `${window.location.origin}/register?code=${code}`
})
const records = ref([])
const showAllDialog = ref(false)
const allLoading = ref(false)
const allRecords = ref([])

// 总奖励成长值
const totalReward = computed(() => {
  return (props.memberInfo?.invitationCount || 0) * 50
})

// 复制邀请码
const copyInvitationCode = async () => {
  try {
    await copy(invitationCode.value)
    message.success('邀请码已复制到剪贴板')
  } catch (error) {
    message.error('复制失败')
  }
}

// 复制邀请链接
const copyInvitationLink = async () => {
  try {
    await copy(invitationLink.value)
    message.success('邀请链接已复制到剪贴板')
  } catch (error) {
    message.error('复制失败')
  }
}

// 获取邀请记录
const fetchInvitationRecords = async () => {
  loading.value = true
  try {
    const response = await getInvitationRecords(0, 5)
    if (response.data.code === 200) {
      records.value = response.data.data.content || []
    }
  } catch (error) {
    console.error('获取邀请记录失败:', error)
  } finally {
    loading.value = false
  }
}

// 查看全部记录
const viewAllRecords = async () => {
  showAllDialog.value = true
  allLoading.value = true
  try {
    const response = await getInvitationRecords(0, 100) // 获取最多100条记录
    if (response.data.code === 200) {
      allRecords.value = response.data.data.content || []
    } else {
      message.error('获取邀请记录失败')
    }
  } catch (error) {
    console.error('获取邀请记录失败:', error)
    message.error('获取邀请记录失败')
  } finally {
    allLoading.value = false
  }
}

// 格式化时间（简短版）
const formatTime = time => {
  if (!time) return ''
  const date = new Date(time)
  const now = new Date()
  const diff = now - date
  const days = Math.floor(diff / (1000 * 60 * 60 * 24))

  if (days === 0) return '今天'
  if (days === 1) return '昨天'
  if (days < 7) return `${days}天前`
  return date.toLocaleDateString('zh-CN')
}

// 格式化时间（完整版）
const formatFullTime = time => {
  if (!time) return ''
  const date = new Date(time)
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

// 获取状态文本
const getStatusText = status => {
  const statusMap = {
    PENDING: '待接受',
    ACCEPTED: '已接受',
    EXPIRED: '已过期'
  }
  return statusMap[status] || status
}

onMounted(() => {
  fetchInvitationRecords()
})
</script>

<style scoped lang="scss">
.invitation-panel {
  border-radius: 12px;
  border: 1px solid #E2E8F0;
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1), 0 2px 4px -2px rgba(0, 0, 0, 0.1);

  :deep(.el-card__header) {
    padding: 10px 14px;
  }

  :deep(.el-card__body) {
    padding: 10px 14px;
  }

  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;

    .title {
      font-size: 14px;
      font-weight: 600;
      color: #0F172A;
      font-family: 'Calistoga', serif;
    }
  }

  .invitation-content {
    .invitation-code-section {
      margin-bottom: 10px;

      .code-label {
        font-size: 11px;
        color: #64748B;
        margin-bottom: 4px;
      }

      .code-display {
        display: flex;
        align-items: center;
        gap: 6px;
        padding: 8px;
        background: #FAFAFA;
        border-radius: 12px;
        border: 1px solid #0052FF;

        .code {
          flex: 1;
          font-size: 14px;
          font-weight: 600;
          color: #0052FF;
          letter-spacing: 1.5px;
          font-family: 'Inter', sans-serif;
        }
      }
    }

    .invitation-link-section {
      margin-bottom: 10px;

      .link-label {
        font-size: 11px;
        color: #64748B;
        margin-bottom: 4px;
      }
    }

    .invitation-stats {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 8px;
      margin-bottom: 10px;

      .stat-box {
        display: flex;
        align-items: center;
        gap: 8px;
        padding: 8px;
        background: #FAFAFA;
        border: 1px solid #E2E8F0;
        border-radius: 12px;

        .stat-icon {
          width: 30px;
          height: 30px;
          border-radius: 8px;
          display: flex;
          align-items: center;
          justify-content: center;
          color: white;
          font-size: 14px;
          flex-shrink: 0;
        }

        &:nth-child(1) .stat-icon {
          background: #0052FF;
        }
        &:nth-child(2) .stat-icon {
          background: #EF4444;
        }

        .stat-info {
          display: flex;
          flex-direction: column;
          gap: 1px;

          .stat-value {
            font-size: 16px;
            font-weight: 600;
            color: #0F172A;
          }

          .stat-label {
            font-size: 10px;
            color: #64748B;
          }
        }
      }
    }

    .invitation-records {
      .records-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-bottom: 6px;
        font-size: 13px;
        font-weight: 600;
        color: #0F172A;
      }

      .records-list {
        .record-item {
          display: flex;
          align-items: center;
          gap: 8px;
          padding: 6px;
          border-radius: 6px;
          transition: all 0.3s;

          &:hover {
            background: #F1F5F9;
          }

          &:not(:last-child) {
            border-bottom: 1px solid #E2E8F0;
          }

          .record-avatar {
            flex-shrink: 0;

            :deep(.el-avatar) {
              width: 32px !important;
              height: 32px !important;
              font-size: 13px !important;
            }
          }

          .record-info {
            flex: 1;
            display: flex;
            flex-direction: column;
            gap: 1px;

            .record-name {
              font-size: 12px;
              font-weight: 500;
              color: #0F172A;
              font-family: 'Calistoga', serif;
            }

            .record-time {
              font-size: 10px;
              color: #9ca3af;
            }
          }

          .record-reward {
            flex-shrink: 0;
          }
        }
      }
    }
  }
}

// 查看全部对话框样式
.all-records-list {
  max-height: 500px;
  overflow-y: auto;

  .all-record-item {
    display: flex;
    gap: 12px;
    padding: 12px;
    border-radius: 6px;
    transition: all 0.3s;

    &:hover {
      background: #F1F5F9;
    }

    &:not(:last-child) {
      border-bottom: 1px solid #E2E8F0;
    }

    .record-avatar {
      flex-shrink: 0;
    }

    .record-details {
      flex: 1;
      display: flex;
      flex-direction: column;
      gap: 6px;

      .record-header {
        display: flex;
        align-items: center;
        gap: 10px;

        .record-name {
          font-size: 14px;
          font-weight: 600;
          color: #0F172A;
          font-family: 'Calistoga', serif;
        }
      }

      .record-meta {
        display: flex;
        flex-direction: column;
        gap: 4px;

        .meta-item {
          display: flex;
          align-items: center;
          gap: 4px;
          font-size: 12px;
          color: #64748B;

          .el-icon {
            font-size: 13px;
          }
        }
      }

      .record-status {
        .status-badge {
          display: inline-block;
          padding: 2px 6px;
          border-radius: 4px;
          font-size: 11px;
          font-weight: 500;
          border: 1px solid;

          &.pending {
            background: #fef3c7;
            color: #92400e;
            border-color: #92400e;
          }

          &.accepted {
            background: #d1fae5;
            color: #065f46;
            border-color: #065f46;
          }

          &.expired {
            background: #fee2e2;
            color: #991b1b;
            border-color: #991b1b;
          }
        }
      }
    }
  }
}
</style>
