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
        <view v-if="!emailBound" class="bind-tag tag-unbound" @tap="showEmailBindDialog = true">
          绑定邮箱
        </view>
        <view v-else class="bind-tag tag-bound">已绑定</view>
      </view>
    </view>

    <!-- #ifdef APP-PLUS -->
    <!-- QQ APP Verify Banner -->
    <view v-if="bindInfo.qqNeedAppVerify" class="card verify-banner">
      <view class="verify-content">
        <text class="verify-icon">💡</text>
        <view class="verify-text">
          <text class="verify-title">APP端QQ登录验证</text>
          <text class="verify-desc">{{ bindInfo.qqBound
            ? '检测到您的QQ账号尚未在APP端验证绑定。如需在APP端使用QQ登录，请点击下方按钮完成身份验证，验证一次后即可长期使用。'
            : '绑定QQ后即可使用QQ快速登录APP端。请点击下方按钮，通过QQ授权完成绑定。' }}</text>
        </view>
      </view>
      <button class="btn-verify" @tap="handleQqAppVerify" :loading="verifyingQq">
        {{ bindInfo.qqBound ? 'QQ验证' : '绑定QQ' }}
      </button>
    </view>
    <!-- #endif -->

    <!-- #ifdef H5 -->
    <!-- QQ Web Verify Banner (H5) -->
    <view v-if="bindInfo.qqNeedWebVerify" class="card verify-banner">
      <view class="verify-content">
        <text class="verify-icon">💡</text>
        <view class="verify-text">
          <text class="verify-title">Web端QQ登录验证</text>
          <text class="verify-desc">{{ bindInfo.qqBound
            ? '检测到您的QQ账号尚未在网页端验证绑定。如需在网页端使用QQ登录，请点击下方按钮完成身份验证，验证一次后即可长期使用。'
            : '绑定QQ后即可使用QQ快速登录网页端。请点击下方按钮，通过QQ授权完成绑定。' }}</text>
        </view>
      </view>
      <button class="btn-verify" @tap="handleBind('qq')">
        {{ bindInfo.qqBound ? 'QQ验证' : '绑定QQ' }}
      </button>
    </view>
    <!-- #endif -->

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
          v-if="bindInfo.qqBound && !bindInfo.qqNeedAppVerify && !bindInfo.qqNeedWebVerify"
          class="btn-small btn-unbind"
          @tap="handleUnbind('qq')"
          :loading="unbindingQq"
        >解绑</button>
        <button
          v-else-if="!bindInfo.qqBound"
          class="btn-small btn-bind"
          @tap="handleBind('qq')"
          :loading="bindingQq"
        >绑定</button>
        <view v-else-if="bindInfo.qqNeedAppVerify || bindInfo.qqNeedWebVerify" class="bind-tag tag-warn">需验证</view>
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
          <text class="summary-dot" :class="bindInfo.qqBound && !bindInfo.qqNeedAppVerify && !bindInfo.qqNeedWebVerify ? 'dot-active' : (bindInfo.qqNeedAppVerify || bindInfo.qqNeedWebVerify) ? 'dot-warn' : 'dot-inactive'" />
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

    <!-- Email Bind Dialog -->
    <view v-if="showEmailBindDialog" class="dialog-mask" @tap.self="showEmailBindDialog = false">
      <view class="dialog-box">
        <text class="dialog-title">绑定邮箱</text>
        <input class="dialog-input" v-model="emailForm.email" placeholder="请输入邮箱地址" type="text" />
        <view class="code-row">
          <input class="dialog-input code-input" v-model="emailForm.code" placeholder="验证码" type="number" maxlength="6" />
          <button class="btn-code" @tap="sendEmailCode" :disabled="emailCodeCountdown > 0">
            {{ emailCodeCountdown > 0 ? `${emailCodeCountdown}s` : '发送验证码' }}
          </button>
        </view>
        <view class="dialog-actions">
          <button class="btn-cancel" @tap="showEmailBindDialog = false">取消</button>
          <button class="btn-confirm" @tap="handleBindEmail" :loading="bindingEmail">确认绑定</button>
        </view>
      </view>
    </view>

    <!-- Merge Confirm Dialog -->
    <view v-if="showMergeDialog" class="dialog-mask" @tap.self="showMergeDialog = false">
      <view class="dialog-box">
        <text class="dialog-title">⚠️ 账号合并确认</text>
        <text class="dialog-desc">该邮箱已注册账号。确认将当前QQ账号的资源合并到邮箱账号吗？合并后当前账号将被删除，QQ将绑定到邮箱账号。</text>
        <view class="code-row">
          <input class="dialog-input code-input" v-model="mergeForm.code" placeholder="邮箱验证码" type="number" maxlength="6" />
          <button class="btn-code" @tap="sendMergeVerifyCode" :disabled="mergeCodeCountdown > 0">
            {{ mergeCodeCountdown > 0 ? `${mergeCodeCountdown}s` : '发送验证码' }}
          </button>
        </view>
        <view class="dialog-actions">
          <button class="btn-cancel" @tap="showMergeDialog = false">取消</button>
          <button class="btn-confirm" @tap="handleMerge" :loading="merging">确认合并</button>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, reactive, computed } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { authApi, socialAuthApi } from '@/services/api'
import { useUserStore } from '@/stores/user'
import { checkLogin } from '@/utils/common'

const userStore = useUserStore()

const bindInfo = reactive({
  wechatBound: false,
  qqBound: false,
  qqNeedAppVerify: false,
  qqNeedWebVerify: false
})

const bindingWechat = ref(false)
const bindingQq = ref(false)
const unbindingWechat = ref(false)
const unbindingQq = ref(false)
const verifyingQq = ref(false)

// Email bind
const showEmailBindDialog = ref(false)
const bindingEmail = ref(false)
const emailForm = reactive({ email: '', code: '' })
const emailCodeCountdown = ref(0)

// Merge
const showMergeDialog = ref(false)
const merging = ref(false)
const mergeForm = reactive({ email: '', code: '' })
const mergeCodeCountdown = ref(0)

const emailBound = computed(() => !!(userStore.userInfo as any)?.email)
const maskedEmail = computed(() => {
  const email = (userStore.userInfo as any)?.email
  if (!email) return ''
  const [local, domain] = email.split('@')
  if (!domain) return email
  const masked = local.length > 2 ? local[0] + '***' + local[local.length - 1] : local
  return masked + '@' + domain
})

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
      bindInfo.qqNeedAppVerify = !!res.data.qqNeedAppVerify
      bindInfo.qqNeedWebVerify = !!res.data.qqNeedWebVerify
    }
  } catch {}
}

// ====== Email bind ======
function startCountdown(ref: any, seconds = 60) {
  ref.value = seconds
  const timer = setInterval(() => {
    ref.value--
    if (ref.value <= 0) clearInterval(timer)
  }, 1000)
}

async function sendEmailCode() {
  const email = emailForm.email.trim()
  if (!email) {
    uni.showToast({ title: '请输入邮箱', icon: 'none' })
    return
  }
  try {
    // First check if email is available
    const checkRes = await authApi.checkEmail(email) as any
    if (checkRes.code === 200 && !checkRes.data) {
      // Email occupied -> prompt merge
      mergeForm.email = email
      showEmailBindDialog.value = false
      showMergeDialog.value = true
      uni.showModal({
        title: '邮箱已注册',
        content: '该邮箱已被其他账号使用，是否确认是您的邮箱？确认后将发送验证码，用于将当前QQ账号资源合并到该邮箱账号。',
        confirmText: '确认合并',
        cancelText: '取消',
        success: (r) => {
          if (!r.confirm) {
            showMergeDialog.value = false
          }
        }
      })
      return
    }
    // Email available, send registration code for binding
    const res = await authApi.sendEmailCode(email) as any
    if (res.code === 200) {
      uni.showToast({ title: '验证码已发送', icon: 'success' })
      startCountdown(emailCodeCountdown)
    } else {
      uni.showToast({ title: res.message || '发送失败', icon: 'none' })
    }
  } catch (e: any) {
    uni.showToast({ title: e?.message || '发送失败', icon: 'none' })
  }
}

async function handleBindEmail() {
  const email = emailForm.email.trim()
  const code = emailForm.code.trim()
  if (!email || !code) {
    uni.showToast({ title: '请填写完整信息', icon: 'none' })
    return
  }
  bindingEmail.value = true
  try {
    const res = await socialAuthApi.bindEmail(email, code) as any
    if (res.code === 200) {
      uni.showToast({ title: '邮箱绑定成功', icon: 'success' })
      showEmailBindDialog.value = false
      emailForm.email = ''
      emailForm.code = ''
      // Refresh user info
      try {
        const profileRes = await (await import('@/services/api')).userApi.getProfile() as any
        if (profileRes.code === 200) userStore.setUserInfo(profileRes.data)
      } catch {}
    } else if (res.code === 409) {
      // Email occupied -> show merge dialog
      mergeForm.email = email
      showEmailBindDialog.value = false
      showMergeDialog.value = true
    } else {
      uni.showToast({ title: res.message || '绑定失败', icon: 'none' })
    }
  } catch (e: any) {
    uni.showToast({ title: e?.message || '绑定失败', icon: 'none' })
  } finally {
    bindingEmail.value = false
  }
}

// ====== Merge ======
async function sendMergeVerifyCode() {
  const email = mergeForm.email.trim()
  if (!email) return
  try {
    const res = await socialAuthApi.sendMergeCode(email) as any
    if (res.code === 200) {
      uni.showToast({ title: '验证码已发送到邮箱', icon: 'success' })
      startCountdown(mergeCodeCountdown)
    } else {
      uni.showToast({ title: res.message || '发送失败', icon: 'none' })
    }
  } catch (e: any) {
    uni.showToast({ title: e?.message || '发送失败', icon: 'none' })
  }
}

async function handleMerge() {
  const email = mergeForm.email.trim()
  const code = mergeForm.code.trim()
  if (!email || !code) {
    uni.showToast({ title: '请输入验证码', icon: 'none' })
    return
  }
  merging.value = true
  try {
    const res = await socialAuthApi.mergeToEmail(email, code) as any
    if (res.code === 200 && res.data) {
      uni.showToast({ title: '账号合并成功', icon: 'success' })
      showMergeDialog.value = false
      mergeForm.code = ''
      // Login as merged account
      userStore._saveLogin(res.data)
      setTimeout(() => uni.reLaunch({ url: '/pages/index/index' }), 1000)
    } else {
      uni.showToast({ title: res.message || '合并失败', icon: 'none' })
    }
  } catch (e: any) {
    uni.showToast({ title: e?.message || '合并失败', icon: 'none' })
  } finally {
    merging.value = false
  }
}

// ====== QQ APP Verify ======
async function handleQqAppVerify() {
  // #ifdef APP-PLUS
  verifyingQq.value = true
  try {
    const services = await new Promise<any[]>((resolve, reject) => {
      plus.oauth.getServices((s: any[]) => resolve(s), (e: any) => reject(e))
    })
    const qqService = services.find((s: any) => s.id === 'qq')
    if (!qqService) {
      uni.showToast({ title: '当前设备不支持QQ', icon: 'none' })
      return
    }
    await new Promise<void>((resolve, reject) => {
      qqService.login((result: any) => resolve(), (e: any) => reject(e))
    })
    const authResult = qqService.authResult
    if (!authResult || !authResult.access_token) {
      uni.showToast({ title: 'QQ授权失败', icon: 'none' })
      return
    }
    const res = await socialAuthApi.bindQqToken(authResult.access_token) as any
    if (res.code === 200) {
      uni.showToast({ title: 'QQ验证成功', icon: 'success' })
      bindInfo.qqNeedAppVerify = false
    } else {
      uni.showToast({ title: res.message || '验证失败', icon: 'none' })
    }
  } catch (e: any) {
    console.error('QQ APP verify error:', e)
    uni.showToast({ title: 'QQ验证失败', icon: 'none' })
  } finally {
    verifyingQq.value = false
  }
  // #endif
}

async function handleBind(provider: 'wechat' | 'qq') {
  const loading = provider === 'wechat' ? bindingWechat : bindingQq
  loading.value = true

  if (provider === 'qq') {
    // #ifdef APP-PLUS
    try {
      const services = await new Promise<any[]>((resolve, reject) => {
        plus.oauth.getServices((s: any[]) => resolve(s), (e: any) => reject(e))
      })
      const qqService = services.find((s: any) => s.id === 'qq')
      if (!qqService) {
        uni.showToast({ title: '当前设备不支持QQ登录', icon: 'none' })
        loading.value = false
        return
      }

      await new Promise<void>((resolve, reject) => {
        qqService.login((result: any) => resolve(), (e: any) => reject(e))
      })

      const authResult = qqService.authResult
      if (!authResult || !authResult.access_token) {
        uni.showToast({ title: 'QQ授权失败', icon: 'none' })
        loading.value = false
        return
      }

      const res = await socialAuthApi.bindQqToken(authResult.access_token) as any
      if (res.code === 200) {
        uni.showToast({ title: '绑定成功', icon: 'success' })
        bindInfo.qqBound = true
      } else {
        uni.showToast({ title: res.message || '绑定失败', icon: 'none' })
      }
    } catch (e: any) {
      console.error('QQ APP bind error:', e)
      uni.showToast({ title: 'QQ绑定失败，请稍后重试', icon: 'none' })
    }
    loading.value = false
    return
    // #endif
  }

  // #ifdef MP-WEIXIN
  if (provider === 'wechat') {
    try {
      const loginRes: any = await new Promise((resolve, reject) => {
        uni.login({ provider: 'weixin', success: resolve, fail: reject })
      })
      if (!loginRes.code) {
        uni.showToast({ title: '微信授权失败', icon: 'none' })
        loading.value = false
        return
      }
      const res = await socialAuthApi.bindWechat(loginRes.code) as any
      if (res.code === 200) {
        uni.showToast({ title: '微信绑定成功', icon: 'success' })
        bindInfo.wechatBound = true
      } else {
        uni.showToast({ title: res.message || '绑定失败', icon: 'none' })
      }
    } catch (e: any) {
      uni.showToast({ title: e?.message || '微信绑定失败', icon: 'none' })
    } finally {
      loading.value = false
    }
    return
  }
  // #endif

  // #ifndef MP-WEIXIN
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
  // #endif
}

async function handleUnbind(provider: 'wechat' | 'qq') {
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
          else { bindInfo.qqBound = false; bindInfo.qqNeedAppVerify = false }
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

/* Verify Banner */
.verify-banner {
  background: #FFF7ED;
  border: 1rpx solid #FDBA74;
}

.verify-content {
  display: flex;
  align-items: flex-start;
  gap: 16rpx;
  margin-bottom: 20rpx;
}

.verify-icon { font-size: 36rpx; }

.verify-text {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4rpx;
}

.verify-title {
  font-size: 28rpx;
  font-weight: 600;
  color: #C2410C;
}

.verify-desc {
  font-size: 24rpx;
  color: #9A3412;
  line-height: 1.5;
}

.btn-verify {
  background: #EA580C;
  color: #fff;
  border-radius: $radius-full;
  font-size: 26rpx;
  height: 72rpx;
  line-height: 72rpx;
  border: none;
}

.tag-warn {
  background: rgba(234, 88, 12, 0.1);
  color: #EA580C;
}

.dot-warn { background: #F59E0B; }

/* Dialog */
.dialog-mask {
  position: fixed;
  top: 0; left: 0; right: 0; bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 999;
}

.dialog-box {
  background: $card;
  border-radius: $radius-xl;
  padding: 40rpx;
  width: 85%;
  max-width: 600rpx;
  box-sizing: border-box;
}

.dialog-title {
  font-size: 32rpx;
  font-weight: 700;
  color: $foreground;
  display: block;
  margin-bottom: 24rpx;
  text-align: center;
  font-family: 'Calistoga', cursive;
}

.dialog-desc {
  font-size: 26rpx;
  color: $muted-foreground;
  line-height: 1.6;
  display: block;
  margin-bottom: 24rpx;
}

.dialog-input {
  border: 1rpx solid $border;
  border-radius: $radius-lg;
  padding: 20rpx 24rpx;
  font-size: 28rpx;
  margin-bottom: 20rpx;
  background: $background;
}

.code-row {
  display: flex;
  gap: 16rpx;
  align-items: flex-start;
}

.code-input {
  flex: 1;
}

.btn-code {
  white-space: nowrap;
  font-size: 24rpx;
  padding: 0 20rpx;
  height: 76rpx;
  line-height: 76rpx;
  border-radius: $radius-lg;
  background: $accent;
  color: #fff;
  border: none;
  min-width: 180rpx;
}

.btn-code[disabled] {
  background: $muted;
  color: $muted-foreground;
}

.dialog-actions {
  display: flex;
  gap: 20rpx;
  margin-top: 24rpx;
}

.btn-cancel {
  flex: 1;
  background: $muted;
  color: $foreground;
  border-radius: $radius-lg;
  font-size: 28rpx;
  height: 80rpx;
  line-height: 80rpx;
  border: 1rpx solid $border;
}

.btn-confirm {
  flex: 1;
  background: $accent;
  color: #fff;
  border-radius: $radius-lg;
  font-size: 28rpx;
  height: 80rpx;
  line-height: 80rpx;
  border: none;
  box-shadow: $shadow-accent;
}
</style>
