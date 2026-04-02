<template>
  <view class="page">
    <view class="card">
      <view class="section-title">第三方账号绑定</view>
      <text class="section-desc text-sm text-secondary">绑定后可使用第三方账号快捷登录</text>

      <!-- WeChat Binding -->
      <view class="bind-item flex-between">
        <view class="bind-info flex">
          <view class="bind-icon wechat-icon">
            <text>微信</text>
          </view>
          <view>
            <text class="bind-name">微信账号</text>
            <text class="bind-status text-sm" :class="bindInfo.wechatBound ? 'text-primary' : 'text-secondary'">
              {{ bindInfo.wechatBound ? '已绑定' : '未绑定' }}
            </text>
          </view>
        </view>
        <button
          v-if="bindInfo.wechatBound"
          class="btn-small btn-unbind"
          @tap="handleUnbind('wechat')"
          :loading="unbindingWechat"
        >解绑</button>
        <button
          v-else
          class="btn-small btn-bind"
          @tap="handleBind('wechat')"
          :loading="bindingWechat"
        >绑定</button>
      </view>

      <!-- QQ Binding -->
      <view class="bind-item flex-between">
        <view class="bind-info flex">
          <view class="bind-icon qq-icon">
            <text>QQ</text>
          </view>
          <view>
            <text class="bind-name">QQ账号</text>
            <text class="bind-status text-sm" :class="bindInfo.qqBound ? 'text-primary' : 'text-secondary'">
              {{ bindInfo.qqBound ? '已绑定' : '未绑定' }}
            </text>
          </view>
        </view>
        <button
          v-if="bindInfo.qqBound"
          class="btn-small btn-unbind"
          @tap="handleUnbind('qq')"
          :loading="unbindingQq"
        >解绑</button>
        <button
          v-else
          class="btn-small btn-bind"
          @tap="handleBind('qq')"
          :loading="bindingQq"
        >绑定</button>
      </view>
    </view>

    <view class="card tips-card">
      <text class="tips-title">💡 注意事项</text>
      <text class="tips-item">• 绑定后可使用对应第三方账号登录</text>
      <text class="tips-item">• 解绑后将无法使用该方式登录</text>
      <text class="tips-item">• 每个第三方账号只能绑定一个系统账户</text>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { socialAuthApi } from '@/services/api'
import { checkLogin } from '@/utils/common'

const bindInfo = reactive({
  wechatBound: false,
  qqBound: false
})

const bindingWechat = ref(false)
const bindingQq = ref(false)
const unbindingWechat = ref(false)
const unbindingQq = ref(false)

async function loadBindInfo() {
  try {
    const res = await socialAuthApi.getBindInfo()
    if (res.code === 200 && res.data) {
      bindInfo.wechatBound = !!res.data.wechatBound
      bindInfo.qqBound = !!res.data.qqBound
    }
  } catch {}
}

async function handleBind(provider: 'wechat' | 'qq') {
  const loading = provider === 'wechat' ? bindingWechat : bindingQq
  loading.value = true
  try {
    const state = `h5_bind_${provider}`
    const res = provider === 'wechat'
      ? await socialAuthApi.getWechatAuthUrl(state) as any
      : await socialAuthApi.getQqAuthUrl(state) as any

    if (res.code === 200 && res.data) {
      // #ifdef H5
      window.location.href = res.data
      // #endif
      // #ifdef APP-PLUS
      plus.runtime.openURL(res.data)
      uni.showToast({ title: '请在浏览器中完成绑定', icon: 'none', duration: 3000 })
      // #endif
    } else {
      uni.showToast({ title: res.message || '获取授权地址失败', icon: 'none' })
    }
  } catch (e: any) {
    uni.showToast({ title: e?.message || '操作失败', icon: 'none' })
  } finally {
    loading.value = false
  }
}

async function handleUnbind(provider: 'wechat' | 'qq') {
  uni.showModal({
    title: '提示',
    content: `确定解绑${provider === 'wechat' ? '微信' : 'QQ'}账号？解绑后将无法使用该方式登录。`,
    success: async (r) => {
      if (!r.confirm) return
      const loading = provider === 'wechat' ? unbindingWechat : unbindingQq
      loading.value = true
      try {
        const res = provider === 'wechat'
          ? await socialAuthApi.unbindWechat()
          : await socialAuthApi.unbindQq()
        if (res.code === 200) {
          uni.showToast({ title: '解绑成功', icon: 'success' })
          if (provider === 'wechat') bindInfo.wechatBound = false
          else bindInfo.qqBound = false
        } else {
          uni.showToast({ title: res.message || '解绑失败', icon: 'none' })
        }
      } catch (e: any) {
        uni.showToast({ title: e?.message || '解绑失败', icon: 'none' })
      } finally {
        loading.value = false
      }
    }
  })
}

onShow(() => {
  if (checkLogin()) loadBindInfo()
})
</script>

<style scoped lang="scss">
.page {
  min-height: 100vh;
  background: $background;
  padding: 20rpx 24rpx;
  font-family: 'Inter', sans-serif;
}

.card {
  background: $card;
  border-radius: $radius-xl;
  padding: 30rpx;
  margin-bottom: 20rpx;
  border: 1rpx solid $border;
  box-shadow: $shadow-sm;
}

.section-title {
  font-size: 32rpx;
  font-weight: 700;
  color: $foreground;
  display: block;
  margin-bottom: 8rpx;
  font-family: 'Calistoga', cursive;
}

.section-desc {
  display: block;
  margin-bottom: 30rpx;
  color: $muted-foreground;
}

.bind-item {
  padding: 28rpx 0;
  border-bottom: 1rpx solid $border;
}

.bind-item:last-child { border-bottom: none; }

.bind-info {
  align-items: center;
  gap: 20rpx;
}

.bind-icon {
  width: 72rpx;
  height: 72rpx;
  border-radius: $radius-lg;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 22rpx;
  font-weight: 600;
  color: #fff;
}

.wechat-icon { background: #07C160; }
.qq-icon { background: $accent; }

.bind-name {
  font-size: 28rpx;
  color: $foreground;
  display: block;
}

.bind-status { display: block; margin-top: 4rpx; }

.btn-small {
  font-size: 24rpx;
  padding: 10rpx 28rpx;
  border-radius: $radius-full;
  line-height: 1.4;
  height: auto;
  min-height: 0;
  border: none;
  font-family: 'Inter', sans-serif;
}

.btn-bind {
  background: $accent;
  color: #fff;
  box-shadow: $shadow-accent;
}

.btn-unbind {
  background: $muted;
  color: $foreground;
  border: 1rpx solid $border;
}

.tips-card {
  background: #EFF6FF;
  border: 1rpx solid rgba(0, 82, 255, 0.1);
}

.tips-title {
  font-size: 28rpx;
  font-weight: 600;
  color: $foreground;
  display: block;
  margin-bottom: 16rpx;
  font-family: 'Calistoga', cursive;
}

.tips-item {
  font-size: 24rpx;
  color: $muted-foreground;
  display: block;
  line-height: 2;
}
</style>
