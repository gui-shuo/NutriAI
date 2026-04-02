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
        <li>个人资料、头像、饮食档案</li>
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
        <p class="confirm-tip" v-if="needPassword">请输入登录密码以确认注销：</p>
        <p class="confirm-tip" v-else>请确认注销操作：</p>
        <el-form @submit.prevent="handleDelete">
          <el-form-item v-if="needPassword">
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
              :disabled="!confirmed || (needPassword && !password)"
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
import { ref, computed } from 'vue'
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

// OAuth-only用户（无邮箱）无需密码验证
const needPassword = computed(() => !!authStore.user?.email)

const handleDelete = async () => {
  if (!confirmed.value || (needPassword.value && !password.value)) return

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
      color: #ff4d4d;
      font-family: 'Kalam', 'ZCOOL KuaiLe', cursive;
    }
  }
}

.data-list {
  margin: 20px 0;
  padding-left: 0;
  list-style: none;
  color: #2d2d2d;
  line-height: 2;
  font-family: 'Patrick Hand', cursive;

  li {
    font-size: 14px;
    padding-left: 16px;
    border-left: 2px dashed #ff4d4d;
    margin-bottom: 4px;
  }
}

.confirm-section {
  .confirm-tip {
    font-size: 15px;
    color: #2d2d2d;
    font-weight: 600;
    margin-bottom: 16px;
    font-family: 'Kalam', 'ZCOOL KuaiLe', cursive;
  }
}

:deep(.el-input) {
  max-width: 400px;
}

:deep(.el-checkbox__label) {
  color: #e5e0d8;
  font-size: 13px;
}
</style>
