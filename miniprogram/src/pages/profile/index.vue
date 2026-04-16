<template>
  <view class="page">
    <!-- User Info Header -->
    <view class="user-header">
      <view class="header-bg" />
      <view class="header-inner">
        <view class="avatar-wrap" @tap="changeAvatar">
          <image class="avatar" :src="defaultAvatar(userStore.userInfo?.avatar)" mode="aspectFill" />
          <view class="avatar-edit">📷</view>
        </view>
        <view class="user-meta">
          <view class="nickname-row">
            <text class="nickname">{{ userStore.userInfo?.nickname || '未设置昵称' }}</text>
            <view v-if="isVip" class="vip-badge-tag">VIP</view>
          </view>
          <text class="username">@{{ userStore.userInfo?.username }}</text>
          <text class="user-bio" v-if="(userStore.userInfo as any)?.bio">{{ (userStore.userInfo as any).bio }}</text>
        </view>
        <view class="edit-btn" @tap="openEditSheet">编辑资料</view>
      </view>

      <!-- Quick Stats Row -->
      <view class="stats-row">
        <view class="stat-item">
          <text class="stat-value">{{ (userStore.userInfo as any)?.email ? '✓' : '-' }}</text>
          <text class="stat-label">邮箱</text>
        </view>
        <view class="stat-divider" />
        <view class="stat-item">
          <text class="stat-value">{{ maskedPhone || '-' }}</text>
          <text class="stat-label">手机</text>
        </view>
        <view class="stat-divider" />
        <view class="stat-item">
          <text class="stat-value">{{ isVip ? 'VIP' : '普通' }}</text>
          <text class="stat-label">等级</text>
        </view>
        <view class="stat-divider" />
        <view class="stat-item">
          <text class="stat-value">{{ joinDays }}</text>
          <text class="stat-label">加入天数</text>
        </view>
      </view>
    </view>

    <!-- Menu List - Services -->
    <view class="section-label">我的服务</view>
    <view class="card menu-card">
      <view class="menu-item" @tap="navigateTo('/pages/food-records/index')">
        <view class="menu-icon-wrap" style="background: rgba(245,158,11,0.1)">
          <text class="menu-icon">🍽️</text>
        </view>
        <text class="menu-text">我的饮食记录</text>
        <text class="menu-arrow">›</text>
      </view>
      <view class="menu-item" @tap="navigateTo('/pages/diet-plan/index')">
        <view class="menu-icon-wrap" style="background: rgba(139,92,246,0.1)">
          <text class="menu-icon">📋</text>
        </view>
        <text class="menu-text">我的饮食计划</text>
        <text class="menu-arrow">›</text>
      </view>
      <view class="menu-item" @tap="navigateTo('/pages/profile/health-record')">
        <view class="menu-icon-wrap" style="background: rgba(16,185,129,0.1)">
          <text class="menu-icon">💪</text>
        </view>
        <text class="menu-text">体质档案</text>
        <text class="menu-arrow">›</text>
      </view>
      <view class="menu-item" @tap="navigateTo('/pages/profile/my-posts')">
        <view class="menu-icon-wrap" style="background: rgba(99,102,241,0.1)">
          <text class="menu-icon">📝</text>
        </view>
        <text class="menu-text">我的帖子</text>
        <text class="menu-arrow">›</text>
      </view>
      <view class="menu-item" @tap="navigateTo('/pages/consultation/index?tab=orders')">
        <view class="menu-icon-wrap" style="background: rgba(236,72,153,0.1)">
          <text class="menu-icon">👩‍⚕️</text>
        </view>
        <text class="menu-text">我的咨询</text>
        <text class="menu-arrow">›</text>
      </view>
      <view class="menu-item" @tap="navigateTo('/pages/profile/my-orders?tab=meals')">
        <view class="menu-icon-wrap" style="background: rgba(16,185,129,0.1)">
          <text class="menu-icon">🍱</text>
        </view>
        <text class="menu-text">我的营养餐</text>
        <text class="menu-arrow">›</text>
      </view>
      <view class="menu-item" @tap="navigateTo('/pages/profile/my-product-orders')">
        <view class="menu-icon-wrap" style="background: rgba(59,130,246,0.1)">
          <text class="menu-icon">🛍</text>
        </view>
        <text class="menu-text">产品订单</text>
        <text class="menu-arrow">›</text>
      </view>
      <view class="menu-item" @tap="navigateTo('/pages/profile/my-orders')">
        <view class="menu-icon-wrap" style="background: rgba(20,184,166,0.1)">
          <text class="menu-icon">📦</text>
        </view>
        <text class="menu-text">我的订单</text>
        <text class="menu-arrow">›</text>
      </view>
      <view class="menu-item" @tap="navigateTo('/pages/profile/coupons')">
        <view class="menu-icon-wrap" style="background: rgba(239,68,68,0.1)">
          <text class="menu-icon">🏷</text>
        </view>
        <text class="menu-text">我的优惠券</text>
        <text class="menu-arrow">›</text>
      </view>
      <view class="menu-item" @tap="navigateTo('/pages/address/index')">
        <view class="menu-icon-wrap" style="background: rgba(59,130,246,0.1)">
          <text class="menu-icon">📍</text>
        </view>
        <text class="menu-text">收货地址</text>
        <text class="menu-arrow">›</text>
      </view>
    </view>

    <!-- Admin Entry - only show for admin users -->
    <view v-if="userStore.isAdmin" class="section-label">管理功能</view>
    <view v-if="userStore.isAdmin" class="card menu-card">
      <view class="menu-item" @tap="navigateTo('/pages/admin/index')">
        <view class="menu-icon-wrap" style="background: rgba(16,185,129,0.1)">
          <text class="menu-icon">⚙️</text>
        </view>
        <text class="menu-text">管理后台</text>
        <text class="menu-arrow">›</text>
      </view>
      <view class="menu-item" @tap="navigateTo('/pages/admin/meals')">
        <view class="menu-icon-wrap" style="background: rgba(16,185,129,0.1)">
          <text class="menu-icon">🍱</text>
        </view>
        <text class="menu-text">营养餐管理</text>
        <text class="menu-arrow">›</text>
      </view>
      <view class="menu-item" @tap="navigateTo('/pages/admin/orders')">
        <view class="menu-icon-wrap" style="background: rgba(245,158,11,0.1)">
          <text class="menu-icon">📦</text>
        </view>
        <text class="menu-text">订单管理</text>
        <text class="menu-arrow">›</text>
      </view>
    </view>

    <!-- Menu List - Settings -->
    <view class="section-label">设置</view>
    <view class="card menu-card">
      <view class="menu-item" @tap="navigateTo('/pages/profile/account-binding')">
        <view class="menu-icon-wrap" style="background: rgba(16,185,129,0.1)">
          <text class="menu-icon">🔗</text>
        </view>
        <text class="menu-text">账号绑定</text>
        <view class="menu-badge-row">
          <text class="menu-badge" v-if="bindSummary">{{ bindSummary }}</text>
          <text class="menu-arrow">›</text>
        </view>
      </view>
      <view class="menu-item" @tap="showPasswordSheet = true">
        <view class="menu-icon-wrap" style="background: rgba(239,68,68,0.1)">
          <text class="menu-icon">🔒</text>
        </view>
        <text class="menu-text">修改密码</text>
        <text class="menu-arrow">›</text>
      </view>
      <view class="menu-item" @tap="navigateTo('/pages/feedback/index')">
        <view class="menu-icon-wrap" style="background: rgba(100,116,139,0.1)">
          <text class="menu-icon">💬</text>
        </view>
        <text class="menu-text">意见反馈</text>
        <text class="menu-arrow">›</text>
      </view>
      <view class="menu-item" @tap="showAboutSheet = true">
        <view class="menu-icon-wrap" style="background: rgba(16,185,129,0.1)">
          <text class="menu-icon">ℹ️</text>
        </view>
        <text class="menu-text">关于我们</text>
        <text class="menu-arrow">›</text>
      </view>
      <view class="menu-item" @tap="showDeleteSheet = true">
        <view class="menu-icon-wrap" style="background: rgba(239,68,68,0.08)">
          <text class="menu-icon">⚠️</text>
        </view>
        <text class="menu-text danger-text">注销账号</text>
        <text class="menu-arrow">›</text>
      </view>
    </view>

    <!-- Logout -->
    <view class="logout-wrap">
      <button class="btn-logout" @tap="handleLogout">退出登录</button>
    </view>

    <!-- ========== Bottom Sheet: Edit Profile ========== -->
    <view v-if="showEditSheet" class="sheet-mask" @tap.self="showEditSheet = false">
      <view class="sheet">
        <view class="sheet-header">
          <text class="sheet-title">编辑资料</text>
          <text class="sheet-close" @tap="showEditSheet = false">✕</text>
        </view>
        <scroll-view scroll-y class="sheet-body" :scroll-into-view="''" :enable-flex="true">
          <view class="form-group">
            <text class="form-label">昵称</text>
            <input class="form-input" v-model="profileForm.nickname" placeholder="请输入昵称" maxlength="20" :adjust-position="true" />
          </view>
          <view class="form-group">
            <text class="form-label">个人简介</text>
            <textarea class="form-textarea" v-model="profileForm.bio" placeholder="介绍一下自己" maxlength="200" :adjust-position="true" auto-height />
          </view>
          <view class="form-group">
            <text class="form-label">性别</text>
            <view class="gender-row">
              <view
                v-for="(opt, idx) in genderOptions"
                :key="idx"
                class="gender-chip"
                :class="{ active: profileForm.gender === genderValueMap[idx] }"
                @tap="profileForm.gender = genderValueMap[idx]"
              >{{ opt }}</view>
            </view>
          </view>
          <view class="form-group">
            <text class="form-label">邮箱</text>
            <input class="form-input" v-model="profileForm.email" type="text" placeholder="请输入邮箱" :adjust-position="true" />
          </view>
          <view class="form-group">
            <text class="form-label">手机号</text>
            <view class="phone-row">
              <input class="form-input flex-1" v-model="profileForm.phone" type="number" placeholder="请输入手机号" maxlength="11" :adjust-position="true" />
              <view v-if="(userStore.userInfo as any)?.email" class="send-code-btn" @tap="handleSendEmailCode">
                {{ smsCooldown > 0 ? `${smsCooldown}s` : '发送验证码' }}
              </view>
            </view>
          </view>
          <view v-if="showPhoneCode" class="form-group">
            <text class="form-label">邮箱验证码</text>
            <input class="form-input" v-model="profileForm.emailCode" type="number" placeholder="请输入邮箱验证码" maxlength="6" :adjust-position="true" />
          </view>
          <button class="btn-primary sheet-save-btn" :loading="saving" @tap="saveProfile">保存修改</button>
        </scroll-view>
      </view>
    </view>

    <!-- ========== Bottom Sheet: Change Password ========== -->
    <view v-if="showPasswordSheet" class="sheet-mask" @tap.self="showPasswordSheet = false">
      <view class="sheet">
        <view class="sheet-header">
          <text class="sheet-title">修改密码</text>
          <text class="sheet-close" @tap="showPasswordSheet = false">✕</text>
        </view>
        <scroll-view scroll-y class="sheet-body">
          <view class="form-group">
            <text class="form-label">原密码</text>
            <input class="form-input" v-model="passwordForm.oldPassword" type="password" placeholder="请输入原密码" />
          </view>
          <view class="form-group">
            <text class="form-label">新密码</text>
            <input class="form-input" v-model="passwordForm.newPassword" type="password" placeholder="8-20位，含大小写字母和数字" />
            <view v-if="passwordForm.newPassword" class="pwd-strength">
              <view class="pwd-bar">
                <view class="pwd-bar-fill" :class="pwdStrengthClass" :style="{ width: pwdStrengthPercent + '%' }" />
              </view>
              <text class="pwd-strength-text" :class="pwdStrengthClass">{{ pwdStrengthLabel }}</text>
            </view>
            <text class="form-hint">密码需包含大小写字母、数字，长度8-20位</text>
          </view>
          <view class="form-group">
            <text class="form-label">确认新密码</text>
            <input class="form-input" v-model="passwordForm.confirmPassword" type="password" placeholder="请再次输入新密码" />
          </view>
          <button class="btn-primary sheet-save-btn" :loading="changingPwd" @tap="changePassword">确认修改</button>
        </scroll-view>
      </view>
    </view>

    <!-- ========== Bottom Sheet: About ========== -->
    <view v-if="showAboutSheet" class="sheet-mask" @tap.self="showAboutSheet = false">
      <view class="sheet sheet-sm">
        <view class="sheet-header">
          <text class="sheet-title">关于我们</text>
          <text class="sheet-close" @tap="showAboutSheet = false">✕</text>
        </view>
        <view class="sheet-body about-body">
          <view class="about-logo">🥗</view>
          <text class="about-name">NutriAI 饮食规划助手</text>
          <text class="about-version">Version 1.0.0</text>
          <text class="about-desc">
            NutriAI 是一款基于人工智能的饮食管理应用，为您提供个性化的饮食计划、
            智能食物识别、营养师咨询等专业服务，助您实现健康生活目标。
          </text>
          <button class="btn-primary sheet-save-btn" @tap="showAboutSheet = false">知道了</button>
        </view>
      </view>
    </view>

    <!-- ========== Bottom Sheet: Delete Account ========== -->
    <view v-if="showDeleteSheet" class="sheet-mask" @tap.self="showDeleteSheet = false">
      <view class="sheet">
        <view class="sheet-header">
          <text class="sheet-title danger-color">⚠️ 注销账号</text>
          <text class="sheet-close" @tap="showDeleteSheet = false">✕</text>
        </view>
        <scroll-view scroll-y class="sheet-body">
          <view class="delete-warning">
            <text class="warning-text">注销后以下数据将被永久删除且无法恢复：</text>
            <text class="warning-item">• 个人资料、头像、饮食档案</text>
            <text class="warning-item">• AI对话记录、收藏内容</text>
            <text class="warning-item">• 饮食计划、饮食记录</text>
            <text class="warning-item">• 食物识别历史</text>
            <text class="warning-item">• 社区帖子、评论、点赞</text>
            <text class="warning-item">• 订单记录、会员信息</text>
            <text class="warning-item">• QQ/微信绑定关系</text>
            <text class="warning-item">• 所有其他关联数据</text>
          </view>
          <view v-if="needPassword" class="form-group">
            <text class="form-label">输入登录密码确认</text>
            <input class="form-input" v-model="deletePassword" type="password" placeholder="请输入当前登录密码" />
          </view>
          <view class="delete-checkbox" @tap="deleteConfirmed = !deleteConfirmed">
            <text :class="['checkbox-icon', { checked: deleteConfirmed }]">{{ deleteConfirmed ? '☑' : '☐' }}</text>
            <text class="checkbox-text">我已了解注销后所有数据将被永久删除且无法恢复</text>
          </view>
          <button
            class="btn-danger sheet-save-btn"
            :disabled="!deleteConfirmed || (needPassword && !deletePassword)"
            :loading="deleting"
            @tap="confirmDeleteAccount"
          >确认注销账号</button>
        </scroll-view>
      </view>
    </view>

    <view class="safe-bottom" />
  </view>
</template>

<script setup lang="ts">
import { ref, computed, reactive, onUnmounted } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { useUserStore } from '@/stores/user'
import { userApi, memberApi, socialAuthApi } from '@/services/api'
import { checkLogin, defaultAvatar } from '@/utils/common'

const userStore = useUserStore()

// --- Sheet visibility ---
const showEditSheet = ref(false)
const showPasswordSheet = ref(false)
const showAboutSheet = ref(false)
const showDeleteSheet = ref(false)

// --- Loading states ---
const saving = ref(false)
const changingPwd = ref(false)
const deleting = ref(false)
const memberInfo = ref<any>({})

// --- Binding summary ---
const bindSummary = ref('')

// --- Profile edit ---
const genderOptions = ['保密', '男', '女']
const genderValueMap = ['SECRET', 'MALE', 'FEMALE']

const profileForm = reactive({
  nickname: '',
  email: '',
  bio: '',
  gender: 'SECRET',
  phone: '',
  emailCode: ''
})

const showPhoneCode = ref(false)
const smsCooldown = ref(0)
const smsSending = ref(false)
let cooldownTimer: ReturnType<typeof setInterval> | null = null

// --- Password change ---
const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

// --- Delete account ---
const deletePassword = ref('')
const deleteConfirmed = ref(false)

// --- Computed ---
const isVip = computed(() => {
  const info = memberInfo.value
  return info.vipLevel && info.vipLevel !== 'NORMAL' && info.vipExpiry && new Date(info.vipExpiry) > new Date()
})

const maskedPhone = computed(() => {
  const p = (userStore.userInfo as any)?.phone
  if (!p || p.length < 7) return p || ''
  return p.substring(0, 3) + '****' + p.substring(7)
})

const joinDays = computed(() => {
  const created = (userStore.userInfo as any)?.createdAt
  if (!created) return '-'
  const diff = Date.now() - new Date(created).getTime()
  return Math.max(1, Math.floor(diff / 86400000))
})

const needPassword = computed(() => !!(userStore.userInfo as any)?.email)

// Password strength
const pwdStrengthLevel = computed(() => {
  const p = passwordForm.newPassword
  if (!p) return 0
  let score = 0
  if (p.length >= 8) score++
  if (/[a-z]/.test(p)) score++
  if (/[A-Z]/.test(p)) score++
  if (/\d/.test(p)) score++
  if (/[@$!%*?&]/.test(p)) score++
  return score
})

const pwdStrengthPercent = computed(() => (pwdStrengthLevel.value / 5) * 100)
const pwdStrengthClass = computed(() => {
  const l = pwdStrengthLevel.value
  if (l <= 1) return 'weak'
  if (l <= 3) return 'medium'
  return 'strong'
})
const pwdStrengthLabel = computed(() => {
  const l = pwdStrengthLevel.value
  if (l <= 1) return '弱'
  if (l <= 3) return '中'
  return '强'
})

// --- Init form ---
function initProfileForm() {
  const u = userStore.userInfo as any
  if (!u) return
  profileForm.nickname = u.nickname || ''
  profileForm.email = u.email || ''
  profileForm.bio = u.bio || ''
  profileForm.gender = u.gender || 'SECRET'
  profileForm.phone = u.phone || ''
  profileForm.emailCode = ''
  showPhoneCode.value = false
}

function openEditSheet() {
  initProfileForm()
  showEditSheet.value = true
}

// --- Avatar upload ---
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

// --- Send email verification code for phone change ---
async function handleSendEmailCode() {
  if (smsCooldown.value > 0 || smsSending.value) return
  smsSending.value = true
  try {
    const res = await userApi.sendEmailCode()
    if (res.code === 200) {
      uni.showToast({ title: '验证码已发送到邮箱', icon: 'success' })
      showPhoneCode.value = true
      smsCooldown.value = 60
      cooldownTimer = setInterval(() => {
        smsCooldown.value--
        if (smsCooldown.value <= 0 && cooldownTimer) {
          clearInterval(cooldownTimer)
          cooldownTimer = null
        }
      }, 1000)
    } else {
      uni.showToast({ title: (res as any).message || '发送失败', icon: 'none' })
    }
  } catch (e: any) {
    uni.showToast({ title: e?.message || '发送失败', icon: 'none' })
  } finally {
    smsSending.value = false
  }
}

// --- Save profile ---
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
      bio: profileForm.bio?.trim() || undefined,
      email: profileForm.email?.trim() || undefined
    }
    const res = await userApi.updateProfile(data)
    if (res.code !== 200) {
      uni.showToast({ title: (res as any).message || '保存失败', icon: 'none' })
      saving.value = false
      return
    }

    // If phone changed and we have a verification code, update phone separately
    const currentPhone = (userStore.userInfo as any)?.phone || ''
    if (profileForm.phone && profileForm.phone !== currentPhone && profileForm.emailCode) {
      try {
        const phoneRes = await userApi.changePhone({
          newPhone: profileForm.phone,
          emailCode: profileForm.emailCode
        })
        if (phoneRes.code !== 200) {
          uni.showToast({ title: (phoneRes as any).message || '手机号修改失败', icon: 'none' })
        }
      } catch {}
    }

    uni.showToast({ title: '保存成功', icon: 'success' })
    userStore.fetchUserInfo()
    showEditSheet.value = false
  } catch {
    uni.showToast({ title: '保存失败', icon: 'none' })
  } finally {
    saving.value = false
  }
}

// --- Change password with validation matching web ---
async function changePassword() {
  const { oldPassword, newPassword, confirmPassword } = passwordForm
  if (!oldPassword || !newPassword || !confirmPassword) {
    uni.showToast({ title: '请填写完整信息', icon: 'none' })
    return
  }
  if (!/^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)[A-Za-z\d@$!%*?&]{8,20}$/.test(newPassword)) {
    uni.showToast({ title: '密码需8-20位含大小写字母和数字', icon: 'none' })
    return
  }
  if (newPassword === oldPassword) {
    uni.showToast({ title: '新密码不能与原密码相同', icon: 'none' })
    return
  }
  if (newPassword !== confirmPassword) {
    uni.showToast({ title: '两次密码不一致', icon: 'none' })
    return
  }
  changingPwd.value = true
  try {
    const res = await userApi.changePassword({ oldPassword, newPassword, confirmPassword })
    if (res.code === 200) {
      uni.showToast({ title: '密码修改成功，请重新登录', icon: 'success' })
      showPasswordSheet.value = false
      passwordForm.oldPassword = ''
      passwordForm.newPassword = ''
      passwordForm.confirmPassword = ''
      setTimeout(() => userStore.logout(), 1500)
    } else {
      uni.showToast({ title: (res as any).message || '修改失败', icon: 'none' })
    }
  } catch {
    uni.showToast({ title: '修改失败', icon: 'none' })
  } finally {
    changingPwd.value = false
  }
}

// --- Navigation ---
function navigateTo(url: string) {
  uni.navigateTo({ url })
}

// --- Logout ---
function handleLogout() {
  uni.showModal({
    title: '提示',
    content: '确定要退出登录吗？',
    success: (res) => {
      if (res.confirm) userStore.logout()
    }
  })
}

// --- Delete account ---
async function confirmDeleteAccount() {
  if (!deleteConfirmed.value) return
  if (needPassword.value && !deletePassword.value) return

  uni.showModal({
    title: '最终确认',
    content: '此操作不可撤销！确定要永久注销您的账号吗？',
    confirmText: '确认注销',
    confirmColor: '#ee0a24',
    success: async (res) => {
      if (!res.confirm) return
      deleting.value = true
      try {
        const result = await userApi.deleteAccount(deletePassword.value || '')
        if (result.code === 200) {
          uni.showToast({ title: '账号已注销', icon: 'success' })
          showDeleteSheet.value = false
          setTimeout(() => userStore.logout(), 1500)
        } else {
          uni.showToast({ title: (result as any).message || '注销失败', icon: 'none' })
        }
      } catch (e: any) {
        uni.showToast({ title: e?.message || '注销失败', icon: 'none' })
      } finally {
        deleting.value = false
      }
    }
  })
}

// --- Load binding summary ---
async function loadBindSummary() {
  try {
    const res = await socialAuthApi.getBindInfo()
    if (res.code === 200 && res.data) {
      const parts: string[] = []
      if (res.data.wechatBound) parts.push('微信')
      if (res.data.qqBound) parts.push('QQ')
      bindSummary.value = parts.length ? parts.join(' · ') + ' 已绑定' : '未绑定'
    }
  } catch {
    bindSummary.value = ''
  }
}

// --- Load member info ---
async function loadMemberInfo() {
  try {
    const res = await memberApi.getInfo()
    if (res.code === 200) memberInfo.value = res.data || {}
  } catch {}
}

// --- Lifecycle ---
onShow(() => {
  if (!checkLogin()) return
  userStore.fetchUserInfo()
  loadMemberInfo()
  loadBindSummary()
  initProfileForm()
  deletePassword.value = ''
  deleteConfirmed.value = false
})

onUnmounted(() => {
  if (cooldownTimer) clearInterval(cooldownTimer)
})
</script>

<style scoped lang="scss">
.page {
  min-height: 100vh;
  background: $background;
  padding-bottom: 30rpx;
  font-family: 'Inter', sans-serif;
}

/* ============ Header ============ */
.user-header {
  position: relative;
  overflow: hidden;
}

.header-bg {
  position: absolute;
  top: 0; left: 0; right: 0;
  height: 100%;
  background: $gradient-accent;
  border-radius: 0 0 40rpx 40rpx;
}

.header-inner {
  position: relative;
  padding: 48rpx 32rpx 24rpx;
  display: flex;
  align-items: center;
}

.avatar-wrap {
  position: relative;
  margin-right: 24rpx;
  flex-shrink: 0;
}

.avatar {
  width: 120rpx;
  height: 120rpx;
  border-radius: $radius-full;
  border: 4rpx solid rgba(255, 255, 255, 0.3);
}

.avatar-edit {
  position: absolute;
  bottom: 0; right: 0;
  background: rgba(0, 0, 0, 0.4);
  border-radius: $radius-full;
  width: 36rpx; height: 36rpx;
  font-size: 20rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}

.user-meta {
  flex: 1;
  min-width: 0;
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
  font-family: 'Calistoga', cursive;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.vip-badge-tag {
  background: rgba(255, 255, 255, 0.2);
  color: #fff;
  font-size: 20rpx;
  padding: 4rpx 14rpx;
  border-radius: $radius-full;
  font-weight: 700;
  letter-spacing: 2rpx;
  font-family: 'JetBrains Mono', monospace;
  flex-shrink: 0;
}

.username {
  color: rgba(255, 255, 255, 0.8);
  margin-top: 4rpx;
  display: block;
  font-size: 24rpx;
}

.user-bio {
  color: rgba(255, 255, 255, 0.7);
  font-size: 22rpx;
  display: block;
  margin-top: 6rpx;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.edit-btn {
  color: #fff;
  font-size: 24rpx;
  background: rgba(255, 255, 255, 0.15);
  padding: 8rpx 20rpx;
  border-radius: $radius-full;
  border: 2rpx solid rgba(255, 255, 255, 0.3);
  font-family: 'Inter', sans-serif;
  flex-shrink: 0;
  white-space: nowrap;
}

/* ============ Stats Row ============ */
.stats-row {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: space-around;
  background: rgba(255, 255, 255, 0.15);
  margin: 0 24rpx 24rpx;
  padding: 24rpx 16rpx;
  border-radius: $radius-xl;
  background-color: rgba(0, 0, 0, 0.5);
}

.stat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  flex: 1;
}

.stat-value {
  font-size: 26rpx;
  font-weight: 700;
  color: #fff;
  font-family: 'JetBrains Mono', monospace;
}

.stat-label {
  font-size: 22rpx;
  color: rgba(255, 255, 255, 0.75);
  margin-top: 4rpx;
}

.stat-divider {
  width: 1rpx;
  height: 40rpx;
  background: rgba(255, 255, 255, 0.25);
}

/* ============ Sections ============ */
.section-label {
  font-size: 26rpx;
  font-weight: 600;
  color: $muted-foreground;
  padding: 24rpx 32rpx 8rpx;
  font-family: 'Inter', sans-serif;
}

.card {
  background: $card;
  border-radius: $radius-xl;
  margin: 8rpx 24rpx 0;
  border: 1rpx solid $border;
  box-shadow: $shadow-sm;
}

.menu-card {
  padding: 0;
}

.menu-item {
  display: flex;
  align-items: center;
  padding: 26rpx 28rpx;
  border-bottom: 1rpx solid rgba(226, 232, 240, 0.6);
  position: relative;

  &:last-child { border-bottom: none; }
  &:active { background: $muted; }
}

.menu-icon-wrap {
  width: 64rpx; height: 64rpx;
  border-radius: $radius-lg;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 20rpx;
  flex-shrink: 0;
}

.menu-icon { font-size: 32rpx; }

.menu-text {
  flex: 1;
  font-size: 28rpx;
  color: $foreground;
  font-family: 'Inter', sans-serif;
}

.danger-text { color: $uni-error; }

.menu-badge-row {
  display: flex;
  align-items: center;
  gap: 8rpx;
}

.menu-badge {
  font-size: 22rpx;
  color: $accent;
  background: rgba(16, 185, 129, 0.08);
  padding: 4rpx 12rpx;
  border-radius: $radius-full;
}

.menu-arrow {
  font-size: 32rpx;
  color: $border;
}

/* ============ Logout ============ */
.logout-wrap {
  padding: 40rpx 24rpx 20rpx;
}

.btn-logout {
  background: $card;
  color: $uni-error;
  border: 1rpx solid $border;
  border-radius: $radius-xl;
  height: 88rpx;
  line-height: 88rpx;
  font-size: 30rpx;
  text-align: center;
  box-shadow: $shadow-sm;
  font-family: 'Inter', sans-serif;
}

.btn-logout::after { border: none; }

/* ============ Bottom Sheet ============ */
.sheet-mask {
  position: fixed;
  top: 0; left: 0; right: 0; bottom: 0;
  background: rgba(15, 23, 42, 0.5);
  z-index: 999;
  display: flex;
  align-items: flex-end;
  justify-content: center;
}

.sheet {
  background: $card;
  border-radius: $radius-2xl $radius-2xl 0 0;
  width: 100%;
  max-width: 750rpx;
  max-height: 85vh;
  display: flex;
  flex-direction: column;
  animation: sheetUp 0.3s ease;
  overflow: hidden;
  box-sizing: border-box;
}

.sheet-sm {
  max-height: 60vh;
}

@keyframes sheetUp {
  from { transform: translateY(100%); }
  to { transform: translateY(0); }
}

.sheet-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 32rpx 32rpx 20rpx;
  border-bottom: 1rpx solid $border;
  flex-shrink: 0;
}

.sheet-title {
  font-size: 34rpx;
  font-weight: 700;
  color: $foreground;
  font-family: 'Calistoga', cursive;
}

.sheet-close {
  font-size: 36rpx;
  color: $muted-foreground;
  padding: 8rpx;
}

.sheet-body {
  padding: 24rpx 32rpx;
  padding-bottom: calc(24rpx + env(safe-area-inset-bottom));
  overflow-y: auto;
  overflow-x: hidden;
  flex: 1;
  width: 100%;
  box-sizing: border-box;
}

/* ============ Form controls ============ */
.form-group {
  margin-bottom: 24rpx;
  width: 100%;
}

.form-label {
  display: block;
  font-size: 24rpx;
  color: $muted-foreground;
  margin-bottom: 10rpx;
  font-weight: 500;
}

.form-input {
  background: $muted;
  border: 1rpx solid $border;
  border-radius: $radius-lg;
  padding: 20rpx 24rpx;
  font-size: 28rpx;
  color: $foreground;
  width: 100%;
  max-width: 100%;
  min-height: 80rpx;
  box-sizing: border-box;
  font-family: 'Inter', sans-serif;
  display: block;
  position: relative;
  z-index: 1;
}

.form-textarea {
  background: $muted;
  border: 1rpx solid $border;
  border-radius: $radius-lg;
  padding: 20rpx 24rpx;
  font-size: 28rpx;
  color: $foreground;
  width: 100%;
  max-width: 100%;
  min-height: 120rpx;
  box-sizing: border-box;
  font-family: 'Inter', sans-serif;
  display: block;
  position: relative;
  z-index: 1;
}

.form-hint {
  display: block;
  font-size: 22rpx;
  color: $muted-foreground;
  margin-top: 8rpx;
}

.gender-row {
  display: flex;
  gap: 16rpx;
}

.gender-chip {
  flex: 1;
  text-align: center;
  padding: 16rpx 0;
  border-radius: $radius-lg;
  background: $muted;
  border: 2rpx solid $border;
  font-size: 28rpx;
  color: $foreground;
  transition: all 0.2s;

  &.active {
    background: rgba(16, 185, 129, 0.1);
    border-color: $accent;
    color: $accent;
    font-weight: 600;
  }
}

.phone-row {
  display: flex;
  gap: 12rpx;
  align-items: center;
  width: 100%;
  overflow: hidden;
}

.phone-row .form-input {
  flex: 1;
  min-width: 0;
  width: auto;
}

.flex-1 { flex: 1; }

.send-code-btn {
  background: $accent;
  color: #fff;
  font-size: 24rpx;
  padding: 16rpx 24rpx;
  border-radius: $radius-lg;
  white-space: nowrap;
  flex-shrink: 0;
}

/* Password strength */
.pwd-strength {
  display: flex;
  align-items: center;
  gap: 16rpx;
  margin-top: 12rpx;
}

.pwd-bar {
  flex: 1;
  height: 8rpx;
  background: $muted;
  border-radius: 4rpx;
  overflow: hidden;
}

.pwd-bar-fill {
  height: 100%;
  border-radius: 4rpx;
  transition: width 0.3s, background 0.3s;
  &.weak { background: $uni-error; }
  &.medium { background: $uni-warning; }
  &.strong { background: $uni-success; }
}

.pwd-strength-text {
  font-size: 22rpx;
  font-weight: 600;
  &.weak { color: $uni-error; }
  &.medium { color: $uni-warning; }
  &.strong { color: $uni-success; }
}

/* Save button */
.sheet-save-btn {
  margin-top: 16rpx;
  height: 88rpx;
  line-height: 88rpx;
  border-radius: $radius-xl;
  font-size: 30rpx;
  border: none;
  font-family: 'Inter', sans-serif;
  width: 100%;
}

.btn-primary {
  background: $accent;
  color: #fff;
  box-shadow: $shadow-accent;
}

.btn-primary::after { border: none; }

.btn-danger {
  background: $uni-error;
  color: #fff;
  &[disabled] { opacity: 0.4; }
}

.btn-danger::after { border: none; }

/* ============ Delete account ============ */
.delete-warning {
  background: #FEF2F2;
  border-radius: $radius-lg;
  padding: 24rpx;
  margin-bottom: 24rpx;
  border: 1rpx solid rgba(239, 68, 68, 0.2);
}

.warning-text {
  display: block;
  font-size: 26rpx;
  color: $uni-error;
  font-weight: 600;
  margin-bottom: 12rpx;
}

.warning-item {
  display: block;
  font-size: 24rpx;
  color: $foreground;
  opacity: 0.7;
  line-height: 1.8;
}

.delete-checkbox {
  display: flex;
  align-items: flex-start;
  gap: 12rpx;
  margin: 20rpx 0;
}

.checkbox-icon {
  font-size: 36rpx;
  color: $border;
  &.checked { color: $uni-error; }
}

.checkbox-text {
  font-size: 24rpx;
  color: $foreground;
  opacity: 0.7;
  flex: 1;
  line-height: 1.6;
}

.danger-color { color: $uni-error; }

/* ============ About ============ */
.about-body {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
}

.about-logo {
  font-size: 80rpx;
  margin-bottom: 16rpx;
}

.about-name {
  font-size: 32rpx;
  font-weight: 700;
  color: $foreground;
  font-family: 'Calistoga', cursive;
}

.about-version {
  margin-top: 8rpx;
  font-size: 24rpx;
  color: $muted-foreground;
}

.about-desc {
  margin-top: 20rpx;
  font-size: 26rpx;
  line-height: 1.8;
  color: $muted-foreground;
}

/* ============ Safe area ============ */
.safe-bottom {
  height: env(safe-area-inset-bottom);
}
</style>
