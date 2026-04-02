<template>
  <div class="profile-sidebar">
    <!-- 用户头像和基本信息 -->
    <div class="user-card">
      <div class="avatar-wrapper">
        <el-avatar :size="80" class="avatar">
          <img
            v-if="userInfo?.avatar"
            :src="userInfo.avatar"
            referrerpolicy="no-referrer"
            style="width: 100%; height: 100%; object-fit: cover"
          />
          <img v-else src="https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png" />
        </el-avatar>
        <el-button class="avatar-btn" size="small" circle @click="emit('change', 'edit')">
          <el-icon><Edit /></el-icon>
        </el-button>
      </div>
      <h3 class="username">
        {{ userInfo?.nickname || userInfo?.username }}
      </h3>
      <p class="role">
        {{ getRoleText(userInfo?.role) }}
      </p>
    </div>

    <!-- 导航菜单 -->
    <el-menu :default-active="activeMenu" class="sidebar-menu" @select="handleSelect">
      <el-menu-item index="info">
        <el-icon><User /></el-icon>
        <span>个人资料</span>
      </el-menu-item>

      <el-menu-item index="edit">
        <el-icon><Edit /></el-icon>
        <span>编辑资料</span>
      </el-menu-item>

      <el-menu-item index="password">
        <el-icon><Lock /></el-icon>
        <span>修改密码</span>
      </el-menu-item>

      <el-menu-item index="health">
        <el-icon><TrendCharts /></el-icon>
        <span>体质档案</span>
      </el-menu-item>

      <el-menu-item index="address">
        <el-icon><Location /></el-icon>
        <span>收货地址</span>
      </el-menu-item>

      <el-menu-item index="bindAccount">
        <el-icon><Link /></el-icon>
        <span>账号绑定</span>
      </el-menu-item>

      <el-menu-item index="deleteAccount" class="danger-menu-item">
        <el-icon><Delete /></el-icon>
        <span>注销账号</span>
      </el-menu-item>
    </el-menu>

    <!-- 返回首页按钮 -->
    <div class="sidebar-footer">
      <el-button type="primary" class="back-btn" @click="router.push('/')">
        <el-icon><HomeFilled /></el-icon>
        返回首页
      </el-button>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { Edit, User, Lock, TrendCharts, HomeFilled, Location, Link, Delete } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'

defineProps({
  activeMenu: {
    type: String,
    default: 'info'
  }
})

const emit = defineEmits(['change'])

const router = useRouter()
const authStore = useAuthStore()

const userInfo = computed(() => authStore.user)

// 获取角色文本
const getRoleText = role => {
  const roleMap = {
    SUPER_ADMIN: '超级管理员',
    ADMIN: '管理员',
    USER: '普通用户'
  }
  return roleMap[role] || '普通用户'
}

// 菜单选择
const handleSelect = index => {
  emit('change', index)
}
</script>

<style scoped lang="scss">
.profile-sidebar {
  background: #fdfbf7;
  border-radius: 255px 15px 225px 15px / 15px 225px 15px 255px;
  border: 2px solid #2d2d2d;
  box-shadow: 4px 4px 0px 0px #2d2d2d;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  height: fit-content;
  position: sticky;
  top: 20px;
}

.user-card {
  padding: 32px 24px;
  text-align: center;
  background: #2d5da1;
  color: white;

  .avatar-wrapper {
    position: relative;
    display: inline-block;
    margin-bottom: 16px;

    .avatar {
      border: 3px dashed rgba(255, 255, 255, 0.8);
      box-shadow: 4px 4px 0px 0px rgba(0, 0, 0, 0.15);
    }

    .avatar-btn {
      position: absolute;
      bottom: 0;
      right: 0;
      background: white;
      color: #2d5da1;
      border: none;
      box-shadow: 3px 3px 0px 0px rgba(45,45,45,0.1);

      &:hover {
        background: #e5e0d8;
      }
    }
  }

  .username {
    font-size: 20px;
    font-weight: 600;
    margin: 0 0 8px 0;
    font-family: 'Kalam', 'ZCOOL KuaiLe', cursive;
  }

  .role {
    font-size: 14px;
    opacity: 0.9;
    margin: 0;
  }
}

.sidebar-menu {
  border: none;
  flex: 1;

  :deep(.el-menu-item) {
    height: 56px;
    line-height: 56px;
    font-size: 15px;
    padding: 0 24px;
    margin: 4px 12px;
    border-radius: 185px 10px 155px 10px / 10px 155px 10px 185px;
    transition: all 0.3s;

    &:hover {
      background: #fdfbf7;
    }

    &.is-active {
      background: #2d5da1;
      color: white;
      box-shadow: 4px 4px 0px 0px #2d2d2d;

      .el-icon {
        color: white;
      }
    }

    &.danger-menu-item {
      color: #ff4d4d;

      .el-icon {
        color: #ff4d4d;
      }

      &:hover {
        background: #fef0f0;
      }

      &.is-active {
        background: #ff4d4d;
        color: white;

        .el-icon {
          color: white;
        }
      }
    }

    .el-icon {
      margin-right: 12px;
      font-size: 18px;
    }
  }
}

.sidebar-footer {
  padding: 20px;
  border-top: 1px dashed #2d2d2d;

  .back-btn {
    width: 100%;
    height: 44px;
    font-size: 15px;
    background: #2d5da1;
    border: none;
    border-radius: 255px 15px 225px 15px / 15px 225px 15px 255px;
    box-shadow: 4px 4px 0px 0px #2d2d2d;

    &:hover {
      opacity: 0.9;
    }

    .el-icon {
      margin-right: 8px;
    }
  }
}

@media (max-width: 768px) {
  .profile-sidebar {
    position: static;
  }
}
</style>
