<template>
  <div class="delete-account">
    <div class="danger-zone">
      <div class="danger-header">
        <el-icon :size="28" color="#f56c6c"><WarningFilled /></el-icon>
        <h2>注销账号</h2>
      </div>

      <el-alert
        title="注销账号后，以下数据将被永久删除且无法恢复："
        type="error"
        :closable="false"
        show-icon
      />

      <ul class="data-list">
        <li>个人资料、头像、健康档案</li>
        <li>AI对话记录、收藏内容</li>
        <li>饮食计划、饮食记录</li>
        <li>食物识别历史</li>
        <li>社区帖子、评论、点赞</li>
        <li>订单记录、会员信息</li>
        <li>QQ/微信绑定关系</li>
        <li>所有其他关联数据</li>
      </ul>

      <el-divider />

      <div class="confirm-section">
        <p class="confirm-tip">请输入登录密码以确认注销：</p>
        <el-form @submit.prevent="handleDelete">
          <el-form-item>
            <el-input
              v-model="password"
              type="password"
              placeholder="请输入当前登录密码"
              show-password
              size="large"
              :prefix-icon="Lock"
            />
          </el-form-item>
          <el-form-item>
            <el-checkbox v-model="confirmed">
              我已了解注销后所有数据将被永久删除且无法恢复
            </el-checkbox>
          </el-form-item>
          <el-form-item>
            <el-button
              type="danger"
              size="large"
              :loading="loading"
              :disabled="!confirmed || !password"
              @click="handleDelete"
            >
              确认注销账号
            </el-button>
          </el-form-item>
        </el-form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { WarningFilled, Lock } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'
import api from '@/services/api'

const router = useRouter()
const authStore = useAuthStore()
const password = ref('')
const confirmed = ref(false)
const loading = ref(false)

const handleDelete = async () => {
  if (!confirmed.value || !password.value) return

  try {
    await ElMessageBox.confirm(
      '此操作不可撤销！确定要永久注销您的账号吗？',
      '最终确认',
      {
        confirmButtonText: '确认注销',
        cancelButtonText: '取消',
        type: 'error',
        center: true,
        customClass: 'custom-message-box',
        confirmButtonClass: 'el-button--danger'
      }
    )
  } catch {
    return
  }

  loading.value = true
  try {
    const res = await api.post('/user/delete-account', { password: password.value })
    if (res.data.code === 200) {
      ElMessage.success('账号已注销')
      authStore.logout()
      router.push('/login')
    } else {
      ElMessage.error(res.data.message || '注销失败')
    }
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '注销失败，请稍后重试')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped lang="scss">
.delete-account {
  max-width: 560px;
}

.danger-zone {
  .danger-header {
    display: flex;
    align-items: center;
    gap: 12px;
    margin-bottom: 24px;

    h2 {
      margin: 0;
      font-size: 22px;
      color: #f56c6c;
    }
  }
}

.data-list {
  margin: 20px 0;
  padding-left: 20px;
  color: #606266;
  line-height: 2;

  li {
    font-size: 14px;
  }
}

.confirm-section {
  .confirm-tip {
    font-size: 15px;
    color: #303133;
    font-weight: 600;
    margin-bottom: 16px;
  }
}

:deep(.el-input) {
  max-width: 400px;
}

:deep(.el-checkbox__label) {
  color: #909399;
  font-size: 13px;
}
</style>
