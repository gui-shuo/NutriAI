<template>
  <view class="page">
    <!-- User Info Header -->
    <view class="user-header">
      <view class="avatar-wrap" @tap="changeAvatar">
        <image class="avatar" :src="defaultAvatar(userStore.userInfo?.avatar)" mode="aspectFill" />
        <view class="avatar-edit">📷</view>
      </view>
      <view class="user-meta">
        <view class="nickname-row">
          <text class="nickname">{{ userStore.userInfo?.nickname || '未设置昵称' }}</text>
          <view v-if="isVip" class="vip-badge-tag">VIP</view>
        </view>
        <text class="username text-sm text-secondary">@{{ userStore.userInfo?.username }}</text>
      </view>
      <view class="edit-btn" @tap="showEditProfile = !showEditProfile">
        {{ showEditProfile ? '收起' : '编辑资料' }}
      </view>
    </view>

    <!-- Profile Edit Section -->
    <view v-if="showEditProfile" class="card edit-section">
      <view class="input-group">
        <text class="label">昵称</text>
        <input v-model="profileForm.nickname" placeholder="请输入昵称" maxlength="20" />
      </view>
      <view class="input-group">
        <text class="label">邮箱</text>
        <input v-model="profileForm.email" type="text" placeholder="请输入邮箱" />
      </view>
      <view class="input-group">
        <text class="label">性别</text>
        <picker :range="genderOptions" :value="genderIndex" @change="onGenderChange">
          <view class="picker-value">{{ genderOptions[genderIndex] || '请选择' }}</view>
        </picker>
      </view>
      <view class="input-group">
        <text class="label">年龄</text>
        <input v-model="profileForm.age" type="number" placeholder="请输入年龄" />
      </view>
      <view class="input-group">
        <text class="label">身高 (cm)</text>
        <input v-model="profileForm.height" type="digit" placeholder="请输入身高" />
      </view>
      <view class="input-group">
        <text class="label">体重 (kg)</text>
        <input v-model="profileForm.weight" type="digit" placeholder="请输入体重" />
      </view>
      <view class="input-group">
        <text class="label">手机号</text>
        <input v-model="profileForm.phone" type="number" placeholder="请输入手机号" maxlength="11" />
      </view>
      <button class="btn-primary save-btn" :loading="saving" @tap="saveProfile">保存修改</button>
    </view>

    <!-- Menu List -->
    <view class="card menu-card">
      <view class="menu-item" @tap="navigateTo('/pages/food-records/index')">
        <text class="menu-icon">🍽️</text>
        <text class="menu-text">我的饮食记录</text>
        <text class="menu-arrow">›</text>
      </view>
      <view class="menu-item" @tap="navigateTo('/pages/diet-plan/index')">
        <text class="menu-icon">📋</text>
        <text class="menu-text">我的饮食计划</text>
        <text class="menu-arrow">›</text>
      </view>
      <view class="menu-item" @tap="navigateTo('/pages/profile/my-posts')">
        <text class="menu-icon">📝</text>
        <text class="menu-text">我的帖子</text>
        <text class="menu-arrow">›</text>
      </view>
      <view class="menu-item" @tap="navigateTo('/pages/consultation/index?tab=orders')">
        <text class="menu-icon">👩‍⚕️</text>
        <text class="menu-text">我的咨询</text>
        <text class="menu-arrow">›</text>
      </view>
      <view class="menu-item" @tap="navigateTo('/pages/profile/my-orders')">
        <text class="menu-icon">📦</text>
        <text class="menu-text">我的订单</text>
        <text class="menu-arrow">›</text>
      </view>
      <view class="menu-item" @tap="navigateTo('/pages/address/index')">
        <text class="menu-icon">📍</text>
        <text class="menu-text">我的地址</text>
        <text class="menu-arrow">›</text>
      </view>
    </view>

    <view class="card menu-card">
      <view class="menu-item" @tap="navigateTo('/pages/profile/account-binding')">
        <text class="menu-icon">🔗</text>
        <text class="menu-text">账号绑定</text>
        <text class="menu-arrow">›</text>
      </view>
      <view class="menu-item" @tap="showPasswordDialog = true">
        <text class="menu-icon">🔒</text>
        <text class="menu-text">修改密码</text>
        <text class="menu-arrow">›</text>
      </view>
      <view class="menu-item" @tap="navigateTo('/pages/feedback/index')">
        <text class="menu-icon">💬</text>
        <text class="menu-text">意见反馈</text>
        <text class="menu-arrow">›</text>
      </view>
      <view class="menu-item" @tap="showAboutDialog = true">
        <text class="menu-icon">ℹ️</text>
        <text class="menu-text">关于我们</text>
        <text class="menu-arrow">›</text>
      </view>
    </view>

    <!-- Logout -->
    <view class="logout-wrap">
      <button class="btn-logout" @tap="handleLogout">退出登录</button>
    </view>

    <!-- Change Password Dialog -->
    <view v-if="showPasswordDialog" class="dialog-mask" @tap.self="showPasswordDialog = false">
      <view class="dialog">
        <view class="dialog-title">修改密码</view>
        <view class="input-group">
          <text class="label">旧密码</text>
          <input v-model="passwordForm.oldPassword" type="password" placeholder="请输入旧密码" />
        </view>
        <view class="input-group">
          <text class="label">新密码</text>
          <input v-model="passwordForm.newPassword" type="password" placeholder="请输入新密码" />
        </view>
        <view class="input-group">
          <text class="label">确认新密码</text>
          <input v-model="passwordForm.confirmPassword" type="password" placeholder="请再次输入新密码" />
        </view>
        <view class="dialog-actions">
          <button class="btn-dialog-cancel" @tap="showPasswordDialog = false">取消</button>
          <button class="btn-dialog-confirm" :loading="changingPwd" @tap="changePassword">确认</button>
        </view>
      </view>
    </view>

    <!-- About Dialog -->
    <view v-if="showAboutDialog" class="dialog-mask" @tap.self="showAboutDialog = false">
      <view class="dialog about-dialog">
        <view class="dialog-title">关于我们</view>
        <view class="about-content">
          <view class="about-logo">🥗</view>
          <text class="about-name">NutriAI 饮食规划助手</text>
          <text class="about-version text-sm text-secondary">Version 1.0.0</text>
          <text class="about-desc text-sm text-secondary mt-20">
            NutriAI 是一款基于人工智能的饮食管理应用，为您提供个性化的饮食计划、
            智能食物识别、营养师咨询等专业服务，助您实现生活目标。
          </text>
        </view>
        <button class="btn-dialog-confirm full-btn" @tap="showAboutDialog = false">知道了</button>
      </view>
    </view>

    <view class="safe-bottom"></view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed, reactive } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { useUserStore } from '@/stores/user'
import { userApi, memberApi } from '@/services/api'
import { checkLogin, defaultAvatar } from '@/utils/common'

const userStore = useUserStore()

const showEditProfile = ref(false)
const showPasswordDialog = ref(false)
const showAboutDialog = ref(false)
const saving = ref(false)
const changingPwd = ref(false)
const memberInfo = ref<any>({})

const genderOptions = ['保密', '男', '女']
const genderMap: Record<string, number> = { '': 0, 'SECRET': 0, 'MALE': 1, 'FEMALE': 2 }
const genderValueMap = ['SECRET', 'MALE', 'FEMALE']
const genderIndex = ref(0)

const profileForm = reactive({
  nickname: '',
  email: '',
  gender: 'SECRET',
  age: '',
  height: '',
  weight: '',
  phone: ''
})

const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const isVip = computed(() => {
  const info = memberInfo.value
  return info.vipLevel && info.vipLevel !== 'NORMAL' && info.vipExpiry && new Date(info.vipExpiry) > new Date()
})

function initProfileForm() {
  const u = userStore.userInfo as any
  if (!u) return
  profileForm.nickname = u.nickname || ''
  profileForm.email = u.email || ''
  profileForm.gender = u.gender || 'SECRET'
  profileForm.age = u.age ? String(u.age) : ''
  profileForm.height = u.height ? String(u.height) : ''
  profileForm.weight = u.weight ? String(u.weight) : ''
  profileForm.phone = u.phone || ''
  genderIndex.value = genderMap[u.gender] ?? 0
}

function onGenderChange(e: any) {
  genderIndex.value = e.detail.value
  profileForm.gender = genderValueMap[e.detail.value]
}

async function changeAvatar() {
  uni.chooseImage({
    count: 1,
    sizeType: ['compressed'],
    sourceType: ['album', 'camera'],
    success: async (res) => {
      if (!res.tempFilePaths?.length) return
      uni.showLoading({ title: '上传中...' })
      try {
        const uploadRes = await userApi.uploadAvatar(res.tempFilePaths[0])
        uni.hideLoading()
        if (uploadRes.code === 200) {
          uni.showToast({ title: '头像已更新', icon: 'success' })
          userStore.fetchUserInfo()
        } else {
          uni.showToast({ title: uploadRes.message || '上传失败', icon: 'none' })
        }
      } catch (e: any) {
        uni.hideLoading()
        uni.showToast({ title: e?.message || '上传失败', icon: 'none' })
      }
    },
    fail: () => {}
  })
}

async function saveProfile() {
  if (!profileForm.nickname.trim()) {
    uni.showToast({ title: '请输入昵称', icon: 'none' })
    return
  }
  saving.value = true
  try {
    const data: any = {
      nickname: profileForm.nickname.trim(),
      gender: profileForm.gender,
      email: profileForm.email?.trim() || undefined,
      phone: profileForm.phone || undefined
    }
    if (profileForm.age) data.age = Number(profileForm.age)
    if (profileForm.height) data.height = Number(profileForm.height)
    if (profileForm.weight) data.weight = Number(profileForm.weight)

    const res = await userApi.updateProfile(data)
    if (res.code === 200) {
      uni.showToast({ title: '保存成功', icon: 'success' })
      userStore.fetchUserInfo()
      showEditProfile.value = false
    } else {
      uni.showToast({ title: res.message || '保存失败', icon: 'none' })
    }
  } catch {
    uni.showToast({ title: '保存失败', icon: 'none' })
  } finally {
    saving.value = false
  }
}

async function changePassword() {
  const { oldPassword, newPassword, confirmPassword } = passwordForm
  if (!oldPassword || !newPassword || !confirmPassword) {
    uni.showToast({ title: '请填写完整信息', icon: 'none' })
    return
  }
  if (newPassword.length < 6) {
    uni.showToast({ title: '新密码至少6位', icon: 'none' })
    return
  }
  if (newPassword !== confirmPassword) {
    uni.showToast({ title: '两次密码不一致', icon: 'none' })
    return
  }
  changingPwd.value = true
  try {
    const res = await userApi.changePassword({ oldPassword, newPassword })
    if (res.code === 200) {
      uni.showToast({ title: '密码修改成功', icon: 'success' })
      showPasswordDialog.value = false
      passwordForm.oldPassword = ''
      passwordForm.newPassword = ''
      passwordForm.confirmPassword = ''
    } else {
      uni.showToast({ title: res.message || '修改失败', icon: 'none' })
    }
  } catch {
    uni.showToast({ title: '修改失败', icon: 'none' })
  } finally {
    changingPwd.value = false
  }
}

function navigateTo(url: string) {
  uni.navigateTo({ url })
}

function handleLogout() {
  uni.showModal({
    title: '提示',
    content: '确定要退出登录吗？',
    success: (res) => {
      if (res.confirm) userStore.logout()
    }
  })
}

async function loadMemberInfo() {
  try {
    const res = await memberApi.getInfo()
    if (res.code === 200) memberInfo.value = res.data || {}
  } catch {}
}

onShow(() => {
  if (!checkLogin()) return
  userStore.fetchUserInfo()
  loadMemberInfo()
  initProfileForm()
})
</script>

<style scoped lang="scss">
.page {
  min-height: 100vh;
  background: #f5f5f5;
  padding-bottom: 30rpx;
}

.user-header {
  background: linear-gradient(135deg, #07c160, #06ad56);
  padding: 50rpx 30rpx 40rpx;
  display: flex;
  align-items: center;
}

.avatar-wrap {
  position: relative;
  margin-right: 24rpx;
}

.avatar {
  width: 120rpx;
  height: 120rpx;
  border-radius: 50%;
  border: 4rpx solid rgba(255, 255, 255, 0.6);
}

.avatar-edit {
  position: absolute;
  bottom: 0;
  right: 0;
  background: rgba(0, 0, 0, 0.5);
  border-radius: 50%;
  width: 36rpx;
  height: 36rpx;
  font-size: 20rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}

.user-meta {
  flex: 1;
}

.nickname-row {
  display: flex;
  align-items: center;
  gap: 12rpx;
}

.nickname {
  font-size: 36rpx;
  font-weight: 700;
  color: #fff;
}

.vip-badge-tag {
  background: linear-gradient(135deg, #d4a248, #b8860b);
  color: #fff;
  font-size: 20rpx;
  padding: 4rpx 12rpx;
  border-radius: 6rpx;
  font-weight: 600;
}

.username {
  color: rgba(255, 255, 255, 0.8);
  margin-top: 4rpx;
  display: block;
}

.edit-btn {
  color: #fff;
  font-size: 26rpx;
  background: rgba(255, 255, 255, 0.2);
  padding: 10rpx 24rpx;
  border-radius: 30rpx;
}

.card {
  background: #fff;
  border-radius: 16rpx;
  margin: 20rpx 24rpx;
  padding: 10rpx 0;
  box-shadow: 0 2rpx 12rpx rgba(0, 0, 0, 0.04);
}

.edit-section {
  padding: 30rpx;
}

.edit-section .input-group {
  background: #f7f8fa;
  border-radius: 12rpx;
  padding: 16rpx 24rpx;
  margin-bottom: 16rpx;
}

.edit-section .input-group .label {
  font-size: 24rpx;
  color: #999;
  margin-bottom: 6rpx;
  display: block;
}

.edit-section .input-group input {
  height: 48rpx;
  font-size: 28rpx;
}

.picker-value {
  height: 48rpx;
  line-height: 48rpx;
  font-size: 28rpx;
  color: #333;
}

.save-btn {
  margin-top: 10rpx;
  height: 80rpx;
  line-height: 80rpx;
  border-radius: 40rpx;
  font-size: 30rpx;
}

.menu-card {
  padding: 0;
}

.menu-item {
  display: flex;
  align-items: center;
  padding: 30rpx;
  border-bottom: 1rpx solid #f5f5f5;
  position: relative;

  &:last-child {
    border-bottom: none;
  }

  &:active {
    background: #f9f9f9;
  }
}

.menu-icon {
  font-size: 36rpx;
  margin-right: 20rpx;
}

.menu-text {
  flex: 1;
  font-size: 28rpx;
  color: #333;
}

.menu-arrow {
  font-size: 32rpx;
  color: #ccc;
}

.logout-wrap {
  padding: 40rpx 24rpx 20rpx;
}

.btn-logout {
  background: #fff;
  color: #ee0a24;
  border: 2rpx solid #ee0a24;
  border-radius: 44rpx;
  height: 88rpx;
  line-height: 88rpx;
  font-size: 30rpx;
  text-align: center;
}

.btn-logout::after {
  border: none;
}

/* Dialog Styles */
.dialog-mask {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 999;
}

.dialog {
  background: #fff;
  border-radius: 20rpx;
  width: 600rpx;
  padding: 40rpx;
}

.dialog-title {
  font-size: 34rpx;
  font-weight: 700;
  text-align: center;
  margin-bottom: 30rpx;
  color: #333;
}

.dialog .input-group {
  background: #f7f8fa;
  border-radius: 12rpx;
  padding: 16rpx 24rpx;
  margin-bottom: 16rpx;
}

.dialog .input-group .label {
  font-size: 24rpx;
  color: #999;
  margin-bottom: 6rpx;
  display: block;
}

.dialog .input-group input {
  height: 48rpx;
  font-size: 28rpx;
}

.dialog-actions {
  display: flex;
  gap: 20rpx;
  margin-top: 20rpx;
}

.btn-dialog-cancel {
  flex: 1;
  background: #f5f5f5;
  color: #666;
  border: none;
  border-radius: 40rpx;
  height: 80rpx;
  line-height: 80rpx;
  font-size: 28rpx;
}

.btn-dialog-cancel::after {
  border: none;
}

.btn-dialog-confirm {
  flex: 1;
  background: linear-gradient(135deg, #07c160, #06ad56);
  color: #fff;
  border: none;
  border-radius: 40rpx;
  height: 80rpx;
  line-height: 80rpx;
  font-size: 28rpx;
}

.btn-dialog-confirm::after {
  border: none;
}

.full-btn {
  width: 100%;
  margin-top: 20rpx;
}

.about-dialog {
  text-align: center;
}

.about-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 20rpx 0;
}

.about-logo {
  font-size: 80rpx;
  margin-bottom: 16rpx;
}

.about-name {
  font-size: 32rpx;
  font-weight: 700;
  color: #333;
}

.about-version {
  margin-top: 8rpx;
}

.about-desc {
  line-height: 1.8;
  text-align: center;
}

.safe-bottom {
  height: env(safe-area-inset-bottom);
}
</style>
