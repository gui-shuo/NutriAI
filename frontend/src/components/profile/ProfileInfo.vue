<template>
  <div class="profile-info">
    <div class="info-header">
      <h2 class="title">
        个人资料
      </h2>
      <el-button
        type="primary"
        @click="emit('edit')"
      >
        <el-icon><Edit /></el-icon>
        编辑资料
      </el-button>
    </div>

    <el-skeleton
      :loading="loading"
      animated
      :rows="6"
    >
      <div class="info-content">
        <!-- 基本信息 -->
        <div class="info-section">
          <h3 class="section-title">
            基本信息
          </h3>
          <div class="info-grid">
            <div class="info-item">
              <div class="item-label">
                <el-icon><User /></el-icon>
                用户名
              </div>
              <div class="item-value">
                {{ userProfile?.username }}
              </div>
            </div>

            <div class="info-item">
              <div class="item-label">
                <el-icon><Postcard /></el-icon>
                昵称
              </div>
              <div class="item-value">
                {{ userProfile?.nickname || '未设置' }}
              </div>
            </div>

            <div class="info-item">
              <div class="item-label">
                <el-icon><Message /></el-icon>
                邮箱
              </div>
              <div class="item-value">
                {{ userProfile?.email || '未设置' }}
              </div>
            </div>

            <div class="info-item">
              <div class="item-label">
                <el-icon><Phone /></el-icon>
                手机号
              </div>
              <div class="item-value">
                {{ userProfile?.phone || '未绑定' }}
              </div>
            </div>
          </div>
        </div>

        <!-- 账号信息 -->
        <div class="info-section">
          <h3 class="section-title">
            账号信息
          </h3>
          <div class="info-grid">
            <div class="info-item">
              <div class="item-label">
                <el-icon><UserFilled /></el-icon>
                角色
              </div>
              <div class="item-value">
                <el-tag :type="getRoleType(userProfile?.role)">
                  {{ getRoleText(userProfile?.role) }}
                </el-tag>
              </div>
            </div>

            <div class="info-item">
              <div class="item-label">
                <el-icon><CircleCheck /></el-icon>
                状态
              </div>
              <div class="item-value">
                <el-tag :type="userProfile?.status === 'ACTIVE' ? 'success' : 'danger'">
                  {{ getStatusText(userProfile?.status) }}
                </el-tag>
              </div>
            </div>

            <div class="info-item">
              <div class="item-label">
                <el-icon><Calendar /></el-icon>
                注册时间
              </div>
              <div class="item-value">
                {{ formatDate(userProfile?.createdAt) }}
              </div>
            </div>

            <div class="info-item">
              <div class="item-label">
                <el-icon><Clock /></el-icon>
                最后登录
              </div>
              <div class="item-value">
                {{ formatDate(userProfile?.lastLoginTime) }}
              </div>
            </div>
          </div>
        </div>
      </div>
    </el-skeleton>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import {
  Edit,
  User,
  Postcard,
  Message,
  Phone,
  UserFilled,
  CircleCheck,
  Calendar,
  Clock
} from '@element-plus/icons-vue'
import api from '@/services/api'
import message from '@/utils/message'

const emit = defineEmits(['edit'])

const loading = ref(false)
const userProfile = ref(null)

// 获取用户资料
const fetchProfile = async () => {
  loading.value = true
  try {
    const response = await api.get('/user/profile')
    if (response.data.code === 200) {
      userProfile.value = response.data.data
    } else {
      message.error('获取用户资料失败')
    }
  } catch (error) {
    console.error('获取用户资料失败:', error)
    message.error('获取用户资料失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

// 获取角色文本
const getRoleText = role => {
  const roleMap = {
    SUPER_ADMIN: '超级管理员',
    ADMIN: '管理员',
    USER: '普通用户'
  }
  return roleMap[role] || '普通用户'
}

// 获取角色标签类型
const getRoleType = role => {
  const typeMap = {
    SUPER_ADMIN: 'danger',
    ADMIN: 'warning',
    USER: 'primary'
  }
  return typeMap[role] || 'primary'
}

// 获取状态文本
const getStatusText = status => {
  const statusMap = {
    ACTIVE: '正常',
    DISABLED: '已禁用',
    BANNED: '已封禁'
  }
  return statusMap[status] || '未知'
}

// 格式化日期
const formatDate = dateStr => {
  if (!dateStr) return '暂无'
  const date = new Date(dateStr)
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

onMounted(() => {
  fetchProfile()
})
</script>

<style scoped lang="scss">
.profile-info {
  .info-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 32px;
    padding-bottom: 20px;
    border-bottom: 2px solid #f0f0f0;

    .title {
      font-size: 24px;
      font-weight: 600;
      color: #333;
      margin: 0;
    }

    .el-button {
      .el-icon {
        margin-right: 6px;
      }
    }
  }

  .info-content {
    .info-section {
      margin-bottom: 40px;

      &:last-child {
        margin-bottom: 0;
      }

      .section-title {
        font-size: 18px;
        font-weight: 600;
        color: #333;
        margin: 0 0 20px 0;
        padding-bottom: 12px;
        border-bottom: 1px solid #f0f0f0;
      }

      .info-grid {
        display: grid;
        grid-template-columns: repeat(2, 1fr);
        gap: 24px;

        @media (max-width: 768px) {
          grid-template-columns: 1fr;
        }

        .info-item {
          .item-label {
            display: flex;
            align-items: center;
            gap: 8px;
            font-size: 14px;
            color: #909399;
            margin-bottom: 8px;

            .el-icon {
              font-size: 16px;
            }
          }

          .item-value {
            font-size: 15px;
            color: #333;
            font-weight: 500;
          }
        }
      }
    }
  }
}
</style>
