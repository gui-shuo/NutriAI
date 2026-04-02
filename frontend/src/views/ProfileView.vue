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

        <!-- 体质档案 -->
        <HealthRecord v-if="activeMenu === 'health'" />

        <!-- 收货地址 -->
        <AddressManager v-if="activeMenu === 'address'" />

        <!-- 账号绑定 -->
        <AccountBinding v-if="activeMenu === 'bindAccount'" />

        <!-- 注销账号 -->
        <DeleteAccount v-if="activeMenu === 'deleteAccount'" />
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
import DeleteAccount from '@/components/profile/DeleteAccount.vue'

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
  background: #fdfbf7;
  padding: 40px 20px;
  font-family: 'Patrick Hand', 'ZCOOL KuaiLe', cursive, sans-serif;
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
  background: #fff;
  border-radius: 255px 15px 225px 15px / 15px 225px 15px 255px;
  border: 2.5px solid #2d2d2d;
  box-shadow: 4px 4px 0px 0px #2d2d2d;
  padding: 32px;
  min-height: 600px;
  position: relative;
  transition: box-shadow 0.2s, transform 0.2s;

  // notebook ruled-line effect
  background-image: repeating-linear-gradient(
    transparent,
    transparent 31px,
    #e5e0d8 31px,
    #e5e0d8 32px
  );
  background-position: 0 32px;

  &:hover {
    box-shadow: 2px 2px 0px 0px #2d2d2d;
    transform: translate(2px, 2px);
  }

  @media (max-width: 768px) {
    padding: 20px;
  }
}
</style>
