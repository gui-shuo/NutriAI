<template>
  <view class="settings-page">
    <view class="menu-section card">
      <view class="menu-item">
        <text class="menu-label">用户名</text>
        <text class="menu-value text-secondary">{{ userStore.userInfo?.username || '-' }}</text>
      </view>
      <view class="menu-item">
        <text class="menu-label">邮箱</text>
        <text class="menu-value text-secondary">{{ userStore.userInfo?.email || '-' }}</text>
      </view>
      <view class="menu-item">
        <text class="menu-label">手机号</text>
        <text class="menu-value text-secondary">{{ userStore.userInfo?.phone || '未绑定' }}</text>
      </view>
      <view class="menu-item" @tap="changeNickname">
        <text class="menu-label">昵称</text>
        <view class="flex items-center">
          <text class="menu-value text-secondary">{{ userStore.userInfo?.nickname || '未设置' }}</text>
          <text class="menu-arrow">›</text>
        </view>
      </view>
    </view>

    <view class="menu-section card">
      <view class="menu-item" @tap="changePassword">
        <text class="menu-label">修改密码</text>
        <text class="menu-arrow">›</text>
      </view>
    </view>

    <view class="menu-section card">
      <view class="menu-item">
        <text class="menu-label">当前版本</text>
        <text class="menu-value text-muted">v1.0.0</text>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { useUserStore } from '../../stores/user'
import { userApi } from '../../services/api'

const userStore = useUserStore()

function changeNickname() {
  uni.showModal({
    title: '修改昵称',
    editable: true,
    placeholderText: '请输入新昵称',
    confirmColor: '#10B981',
    success: async (res) => {
      if (res.confirm && res.content?.trim()) {
        try {
          await userApi.updateProfile({ nickname: res.content.trim() })
          await userStore.fetchUserInfo()
          uni.showToast({ title: '修改成功', icon: 'success' })
        } catch {
          uni.showToast({ title: '修改失败', icon: 'none' })
        }
      }
    }
  })
}

function changePassword() {
  // Navigate to a password change flow or show inline dialog
  uni.showToast({ title: '请通过邮箱重置密码', icon: 'none' })
}
</script>

<style lang="scss" scoped>
.settings-page {
  min-height: 100vh;
  background: $bg-page;
  padding: $spacing-md $spacing-lg;
}

.menu-section {
  margin-bottom: $spacing-md;
  padding: 0 $spacing-lg;
}

.menu-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: $spacing-lg 0;
  border-bottom: 1rpx solid $border-light;
  &:last-child { border-bottom: none; }
}

.menu-label {
  font-size: $font-base;
  color: $text-primary;
}

.menu-value {
  font-size: $font-sm;
}

.menu-arrow {
  font-size: $font-xl;
  color: $text-muted;
  margin-left: $spacing-sm;
}
</style>
