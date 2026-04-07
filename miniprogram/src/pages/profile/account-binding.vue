<template>
  <view class="page">
    <!-- Email Account -->
    <view class="card">
      <text class="section-title">📧 邮箱账号</text>
      <view class="bind-item">
        <view class="bind-info">
          <view class="bind-icon email-icon">
            <text>邮箱</text>
          </view>
          <view class="bind-detail">
            <text class="bind-name">邮箱登录</text>
            <text class="bind-status" :class="emailBound ? 'text-primary' : 'text-secondary'">
              {{ emailBound ? maskedEmail : '未绑定' }}
            </text>
          </view>
        </view>
        <view class="bind-tag" :class="emailBound ? 'tag-bound' : 'tag-unbound'">
          {{ emailBound ? '已绑定' : '未绑定' }}
        </view>
      </view>
    </view>

    <!-- Social Accounts -->
    <view class="card">
      <text class="section-title">🔗 第三方账号</text>
      <text class="section-desc">绑定后可使用第三方账号快捷登录</text>

      <!-- WeChat -->
      <view class="bind-item">
        <view class="bind-info">
          <view class="bind-icon wechat-icon">
            <text>微信</text>
          </view>
          <view class="bind-detail">
            <text class="bind-name">微信账号</text>
            <text class="bind-status" :class="bindInfo.wechatBound ? 'text-primary' : 'text-secondary'">
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

      <!-- QQ -->
      <view class="bind-item">
        <view class="bind-info">
          <view class="bind-icon qq-icon">
            <text>QQ</text>
          </view>
          <view class="bind-detail">
            <text class="bind-name">QQ账号</text>
            <text class="bind-status" :class="bindInfo.qqBound ? 'text-primary' : 'text-secondary'">
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

    <!-- Binding Summary -->
    <view class="card summary-card">
      <text class="summary-title">绑定状态概览</text>
      <view class="summary-row">
        <view class="summary-item">
          <text class="summary-dot" :class="emailBound ? 'dot-active' : 'dot-inactive'" />
          <text class="summary-label">邮箱</text>
        </view>
        <view class="summary-item">
          <text class="summary-dot" :class="bindInfo.wechatBound ? 'dot-active' : 'dot-inactive'" />
          <text class="summary-label">微信</text>
        </view>
        <view class="summary-item">
          <text class="summary-dot" :class="bindInfo.qqBound ? 'dot-active' : 'dot-inactive'" />
          <text class="summary-label">QQ</text>
        </view>
      </view>
    </view>

    <!-- Tips -->
    <view class="card tips-card">
      <text class="tips-title">💡 注意事项</text>
      <text class="tips-item">• 绑定第三方账号后可使用该方式快捷登录</text>
      <text class="tips-item">• 三种登录方式（邮箱、微信、QQ）指向同一个账号</text>
      <text class="tips-item">• 解绑前请确保至少保留一种登录方式</text>
      <text class="tips-item">• 每个第三方账号只能绑定一个系统账户</text>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, reactive, computed } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { socialAuthApi } from '@/services/api'
import { useUserStore } from '@/stores/user'
import { checkLogin } from '@/utils/common'

const userStore = useUserStore()

const bindInfo = reactive({
  wechatBound: false,
  qqBound: false
})

const bindingWechat = ref(false)
const bindingQq = ref(false)
const unbindingWechat = ref(false)
const unbindingQq = ref(false)

const emailBound = computed(() => !!(userStore.userInfo as any)?.email)
const maskedEmail = computed(() => {
  const email = (userStore.userInfo as any)?.email
  if (!email) return ''
  const [local, domain] = email.split('@')
  if (!domain) return email
  const masked = local.length > 2 ? local[0] + '***' + local[local.length - 1] : local
  return masked + '@' + domain
})

// Count total bound methods for unbind check
const boundCount = computed(() => {
  let count = 0
  if (emailBound.value) count++
  if (bindInfo.wechatBound) count++
  if (bindInfo.qqBound) count++
  return count
})

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

  if (provider === 'qq') {
    // #ifdef APP-PLUS
    // APP端使用内嵌WebView进行QQ OAuth绑定，使用和网页端相同的QQ_WEB_ID
    try {
      const urlRes = await socialAuthApi.getQqAuthUrl('bind_qq') as any
      if (urlRes.code !== 200 || !urlRes.data) {
        uni.showToast({ title: urlRes.message || '获取授权地址失败', icon: 'none' })
        return
      }
      const authUrl = urlRes.data
      const redirectHost = 'nutriai.itshuo.me'
      const wv = plus.webview.create(authUrl, 'qq-auth-bind', {
        titleNView: {
          titleText: 'QQ绑定',
          backgroundColor: '#10B981',
          titleColor: '#ffffff',
          autoBackButton: true
        },
        backButtonAutoControl: 'close'
      } as any)
      let handled = false
      const checkUrl = () => {
        if (handled) return
        try {
          const url = wv.getURL()
          if (url && url.includes(redirectHost) && url.includes('/auth/callback/') && url.includes('code=')) {
            handled = true
            const match = url.match(/[?&]code=([^&]+)/)
            const stateMatch = url.match(/[?&]state=([^&]+)/)
            const code = match ? decodeURIComponent(match[1]) : ''
            const state = stateMatch ? decodeURIComponent(stateMatch[1]) : 'bind_qq'
            wv.close('auto')
            if (code) {
              handleQqCodeBind(code, state)
            }
          }
        } catch {}
      }
      const pollTimer = setInterval(checkUrl, 300)
      wv.addEventListener('close', () => {
        clearInterval(pollTimer)
        loading.value = false
      })
      wv.show('slide-in-right')
    } catch (e: any) {
      uni.showToast({ title: 'QQ绑定失败，请稍后重试', icon: 'none' })
      loading.value = false
    }
    return
    // #endif
  }

  try {
    let state = `h5_bind_${provider}`
    const res = provider === 'wechat'
      ? await socialAuthApi.getWechatAuthUrl(state) as any
      : await socialAuthApi.getQqAuthUrl(state) as any

    if (res.code === 200 && res.data) {
      // #ifdef H5
      window.location.href = res.data
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

// APP端QQ授权码绑定
async function handleQqCodeBind(code: string, state: string) {
  try {
    uni.showLoading({ title: '正在绑定...', mask: true })
    const res = await socialAuthApi.bindQq(code, state) as any
    uni.hideLoading()
    if (res.code === 200) {
      uni.showToast({ title: 'QQ绑定成功', icon: 'success' })
      await loadBindInfo()
    } else {
      uni.showToast({ title: res.message || 'QQ绑定失败', icon: 'none' })
    }
  } catch (e: any) {
    uni.hideLoading()
    uni.showToast({ title: e?.message || 'QQ绑定失败', icon: 'none' })
  }
}

async function handleUnbind(provider: 'wechat' | 'qq') {
  // Prevent unbinding if it's the only login method
  if (boundCount.value <= 1) {
    uni.showToast({ title: '至少保留一种登录方式', icon: 'none' })
    return
  }

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
          uni.showToast({ title: (res as any).message || '解绑失败', icon: 'none' })
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
  padding-bottom: calc(30rpx + env(safe-area-inset-bottom));
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
  margin-bottom: 20rpx;
  color: $muted-foreground;
  font-size: 24rpx;
}

.bind-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 24rpx 0;
  border-bottom: 1rpx solid $border;
}

.bind-item:last-child { border-bottom: none; }

.bind-info {
  display: flex;
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

.email-icon { background: #6366F1; }
.wechat-icon { background: #07C160; }
.qq-icon { background: $accent; }

.bind-detail {
  display: flex;
  flex-direction: column;
}

.bind-name {
  font-size: 28rpx;
  color: $foreground;
  font-weight: 500;
}

.bind-status {
  font-size: 24rpx;
  margin-top: 4rpx;
}

.text-primary { color: $accent; }
.text-secondary { color: $muted-foreground; }

.bind-tag {
  font-size: 22rpx;
  font-weight: 600;
  padding: 6rpx 16rpx;
  border-radius: $radius-full;
}

.tag-bound {
  background: rgba(16, 185, 129, 0.1);
  color: $accent;
}

.tag-unbound {
  background: $muted;
  color: $muted-foreground;
}

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

/* Summary card */
.summary-card {
  background: $card;
}

.summary-title {
  font-size: 26rpx;
  font-weight: 600;
  color: $foreground;
  display: block;
  margin-bottom: 20rpx;
}

.summary-row {
  display: flex;
  justify-content: space-around;
}

.summary-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8rpx;
}

.summary-dot {
  width: 24rpx;
  height: 24rpx;
  border-radius: $radius-full;
  display: inline-block;
}

.dot-active { background: $accent; }
.dot-inactive { background: $muted; border: 2rpx solid $border; }

.summary-label {
  font-size: 24rpx;
  color: $muted-foreground;
}

/* Tips */
.tips-card {
  background: #ECFDF5;
  border: 1rpx solid rgba(16, 185, 129, 0.1);
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
