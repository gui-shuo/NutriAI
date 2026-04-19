<script setup>
/**
 * 编辑资料页
 */
import { ref, onMounted } from 'vue'
import NavBar from '../../components/NavBar.vue'
import { useUserStore } from '../../stores/user'
import { userApi } from '../../services/api'

const userStore = useUserStore()
const form = ref({
  nickname: '',
  bio: '',
  gender: '',
  birthday: '',
})
const submitting = ref(false)

onMounted(() => {
  const info = userStore.userInfo
  if (info) {
    form.value.nickname = info.nickname || ''
    form.value.bio = info.bio || ''
    form.value.gender = info.gender || ''
    form.value.birthday = info.birthday || ''
  }
})

function chooseAvatar() {
  uni.chooseImage({
    count: 1,
    sizeType: ['compressed'],
    sourceType: ['album', 'camera'],
    success: async (res) => {
      try {
        await userApi.uploadAvatar(res.tempFilePaths[0])
        await userStore.fetchProfile()
        uni.showToast({ title: '头像已更新', icon: 'success' })
      } catch (e) {
        uni.showToast({ title: '上传失败', icon: 'none' })
      }
    }
  })
}

function onGenderChange(e) {
  const genders = ['男', '女', '保密']
  form.value.gender = genders[e.detail.value]
}

function onDateChange(e) {
  form.value.birthday = e.detail.value
}

async function save() {
  if (!form.value.nickname.trim()) {
    uni.showToast({ title: '请输入昵称', icon: 'none' })
    return
  }
  submitting.value = true
  try {
    await userApi.updateProfile(form.value)
    await userStore.fetchProfile()
    uni.showToast({ title: '保存成功', icon: 'success' })
    setTimeout(() => uni.navigateBack(), 1000)
  } catch (e) {
    uni.showToast({ title: '保存失败', icon: 'none' })
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <view class="page">
    <NavBar showBack title="编辑资料" />

    <scroll-view scroll-y class="content" :enhanced="true" :show-scrollbar="false">
      <!-- 头像 -->
      <view class="avatar-section" @tap="chooseAvatar">
        <u-avatar :src="userStore.userAvatar" size="80" shape="circle" />
        <text class="avatar-hint">点击更换头像</text>
      </view>

      <!-- 表单 -->
      <view class="form-card">
        <view class="form-item">
          <text class="form-item__label">昵称</text>
          <input class="form-item__input" v-model="form.nickname" placeholder="请输入昵称" maxlength="20" />
        </view>

        <view class="form-item">
          <text class="form-item__label">性别</text>
          <picker :range="['男','女','保密']" @change="onGenderChange">
            <text class="form-item__value">{{ form.gender || '请选择' }}</text>
          </picker>
        </view>

        <view class="form-item">
          <text class="form-item__label">生日</text>
          <picker mode="date" :value="form.birthday" @change="onDateChange">
            <text class="form-item__value">{{ form.birthday || '请选择' }}</text>
          </picker>
        </view>

        <view class="form-item form-item--col">
          <text class="form-item__label">个人简介</text>
          <textarea
            class="form-item__textarea"
            v-model="form.bio"
            placeholder="介绍一下自己..."
            maxlength="200"
            :auto-height="true"
          />
        </view>
      </view>

      <!-- 保存按钮 -->
      <u-button
        :text="submitting ? '保存中...' : '保存修改'"
        type="primary"
        shape="circle"
        color="#0a6e2c"
        :loading="submitting"
        :disabled="submitting"
        @click="save"
      />

      <view style="height: 60rpx;" />
    </scroll-view>
  </view>
</template>

<style lang="scss" scoped>
@import '../../styles/design-system.scss';

.page { min-height: 100vh; background: #ffffff; overflow-x: hidden; width: 100%; }
.content { padding: 24rpx; height: calc(100vh - 100px); }

.avatar-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 40rpx 0;
}

.avatar-img {
  width: 160rpx;
  height: 160rpx;
  border-radius: $radius-full;
  margin-bottom: 12rpx;
}

.avatar-hint {
  font-size: $font-sm;
  color: $primary;
}

.form-card {
  background: $surface-container-lowest;
  border-radius: $radius-xl;
  padding: 8rpx 24rpx;
  margin-bottom: 32rpx;
}

.form-item {
  display: flex;
  align-items: center;
  padding: 28rpx 0;

  & + & { border-top: 1rpx solid $surface-container; }

  &--col {
    flex-direction: column;
    align-items: flex-start;
    gap: 12rpx;
  }

  &__label {
    font-size: $font-base;
    color: $on-surface;
    font-weight: 500;
    width: 160rpx;
    flex-shrink: 0;
  }

  &__input {
    flex: 1;
    font-size: $font-base;
    color: $on-surface;
    text-align: right;
  }

  &__value {
    flex: 1;
    font-size: $font-base;
    color: $on-surface-variant;
    text-align: right;
  }

  &__textarea {
    width: 100%;
    font-size: $font-base;
    color: $on-surface;
    background: $surface-container-low;
    border-radius: $radius-lg;
    padding: 16rpx;
    box-sizing: border-box;
    min-height: 120rpx;
  }
}

.save-btn {
  padding: 24rpx;
  background: $primary;
  color: $on-primary;
  text-align: center;
  border-radius: $radius-xl;
  font-size: $font-base;
  font-weight: 600;

  &--disabled { opacity: 0.5; pointer-events: none; }
  &:active { transform: scale(0.98); }
}
</style>
