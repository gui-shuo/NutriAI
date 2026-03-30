<template>
  <div class="profile-container">
    <div class="profile-layout">
      <!-- 侧边导航 -->
      <ProfileSidebar :active-menu="activeMenu" @change="handleMenuChange" />

      <!-- 主内容区 -->
      <div class="profile-content">
        <!-- 用户资料展示 -->
        <ProfileInfo v-if="activeMenu === 'info'" @edit="handleMenuChange('edit')" />

        <!-- 资料编辑表单 -->
        <ProfileEdit
          v-if="activeMenu === 'edit'"
          @saved="handleMenuChange('info')"
          @cancel="handleMenuChange('info')"
        />

        <!-- 修改密码 -->
        <PasswordChange v-if="activeMenu === 'password'" />

        <!-- 健康档案 -->
        <HealthRecord v-if="activeMenu === 'health'" />

        <!-- 收货地址 -->
        <AddressManager v-if="activeMenu === 'address'" />

        <!-- 账号绑定 -->
        <AccountBinding v-if="activeMenu === 'bindAccount'" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import ProfileSidebar from '@/components/profile/ProfileSidebar.vue'
import ProfileInfo from '@/components/profile/ProfileInfo.vue'
import ProfileEdit from '@/components/profile/ProfileEdit.vue'
import PasswordChange from '@/components/profile/PasswordChange.vue'
import HealthRecord from '@/components/profile/HealthRecord.vue'
import AddressManager from '@/components/profile/AddressManager.vue'
import AccountBinding from '@/components/profile/AccountBinding.vue'

// 当前激活的菜单
const activeMenu = ref('info')

// 切换菜单
const handleMenuChange = menu => {
  activeMenu.value = menu
}
</script>

<style scoped lang="scss">
.profile-container {
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 40px 20px;
}

.profile-layout {
  max-width: 1200px;
  margin: 0 auto;
  display: grid;
  grid-template-columns: 280px 1fr;
  gap: 24px;

  @media (max-width: 768px) {
    grid-template-columns: 1fr;
  }
}

.profile-content {
  background: white;
  border-radius: 16px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);
  padding: 32px;
  min-height: 600px;

  @media (max-width: 768px) {
    padding: 20px;
  }
}
</style>
