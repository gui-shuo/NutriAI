<script setup lang="ts">
import { onLaunch, onShow } from "@dcloudio/uni-app";
import { useUserStore } from "@/stores/user";
import { useAppStore } from "@/stores/app";
import { setToken, setRefreshToken } from "@/utils/request";

onLaunch((options: any) => {
  const userStore = useUserStore();
  const appStore = useAppStore();
  userStore.restore()
  setTimeout(() => appStore.fetchConfig(), 0)

  // #ifdef APP-PLUS
  plus.globalEvent.addEventListener('newintent', () => {
    setTimeout(() => {
      const args = plus.runtime.arguments
      console.log('[App] newintent args:', args)
      if (args && args.startsWith('nutriai://')) {
        processSchemeUrl(args)
      }
    }, 500)
  })
  const launchArgs = plus.runtime.arguments
  console.log('[App] launch args:', launchArgs)
  if (launchArgs && launchArgs.startsWith('nutriai://')) {
    setTimeout(() => processSchemeUrl(launchArgs), 800)
  }
  // #endif
});

// #ifdef APP-PLUS
let lastProcessedSchemeUrl = ''

function processSchemeUrl(url: string) {
  if (!url || url === lastProcessedSchemeUrl) return
  lastProcessedSchemeUrl = url
  handleSchemeUrl(url)
}

function handleSchemeUrl(url: string) {
  try {
    console.log('[App] handleSchemeUrl:', url)
    if (!url || !url.startsWith('nutriai://')) return
    const urlObj = new URL(url.replace('nutriai://', 'https://nutriai.app/'))
    const token = urlObj.searchParams.get('token')
    const refreshToken = urlObj.searchParams.get('refreshToken')
    const action = urlObj.searchParams.get('action')

    if (action === 'bind') {
      const bindCode = urlObj.searchParams.get('code')
      const provider = urlObj.searchParams.get('provider')
      if (bindCode && provider) {
        uni.navigateTo({
          url: `/pages/auth/social-callback?action=bind&code=${bindCode}&provider=${provider}`
        })
      }
      return
    }

    if (token) {
      console.log('[App] 深度链接登录: token长度=', token.length)
      setToken(token)
      if (refreshToken) setRefreshToken(refreshToken)
      const userStore = useUserStore()
      userStore.restore()
      userStore.fetchUserInfo()
      uni.showToast({ title: '登录成功', icon: 'success' })
      setTimeout(() => uni.reLaunch({ url: '/pages/index/index' }), 500)
    } else {
      console.warn('[App] 深度链接缺少token:', url)
    }
  } catch (e) {
    console.error('[App] handleSchemeUrl error:', e)
  }
}
// #endif

onShow(() => {
  // #ifdef APP-PLUS
  // 从浏览器返回APP时再次检查深度链接（newintent可能丢失）
  setTimeout(() => {
    const args = plus.runtime.arguments
    if (args && args.startsWith('nutriai://')) {
      processSchemeUrl(args)
    }
  }, 600)
  // #endif
});
</script>

<style>
/* ========== NutriAI Design System v2 ========== */

page {
  background-color: #F5F7FA;
  font-family: -apple-system, BlinkMacSystemFont, 'PingFang SC', 'Helvetica Neue', 'Microsoft YaHei', sans-serif;
  font-size: 28rpx;
  color: #1A1A2E;
  line-height: 1.5;
  width: 100%;
  min-width: 0;
  -webkit-font-smoothing: antialiased;
}

/* #ifdef H5 */
*, *::before, *::after { box-sizing: border-box; }
uni-input, uni-textarea, input, textarea { max-width: 100%; box-sizing: border-box; }
uni-page-body { width: 100%; max-width: 960px; margin: 0 auto; overflow-x: hidden; }
/* #endif */

/* ---- Layout ---- */
.container {
  padding: 24rpx 32rpx;
  width: 100%;
  box-sizing: border-box;
}

.page-section {
  padding: 0 32rpx;
  margin-bottom: 24rpx;
}

/* ---- Grid ---- */
.grid-2 { display: grid !important; grid-template-columns: repeat(2, 1fr) !important; gap: 20rpx; }
.grid-3 { display: grid !important; grid-template-columns: repeat(3, 1fr) !important; gap: 20rpx; }

/* ---- Theme Colors ---- */
.text-primary { color: #10B981; }
.text-secondary { color: #8896AB; }
.text-danger { color: #EF4444; }
.text-warning { color: #F59E0B; }
.text-success { color: #10B981; }
.bg-primary { background: linear-gradient(135deg, #10B981, #059669); }
.bg-white { background-color: #fff; }
.bg-page { background-color: #F5F7FA; }
.bg-muted { background-color: #F0F4F8; }

/* ---- Card System ---- */
.card {
  background: #fff;
  border-radius: 24rpx;
  padding: 28rpx;
  margin-bottom: 20rpx;
  box-shadow: 0 2rpx 12rpx rgba(0, 0, 0, 0.04);
  position: relative;
}
.card-flat {
  background: #fff;
  border-radius: 20rpx;
  padding: 24rpx;
  margin-bottom: 16rpx;
}
.card-elevated {
  background: #fff;
  border-radius: 28rpx;
  padding: 32rpx;
  margin-bottom: 24rpx;
  box-shadow: 0 8rpx 40rpx rgba(0, 0, 0, 0.06);
}

/* ---- Buttons ---- */
.btn-primary {
  background: linear-gradient(135deg, #10B981, #059669);
  color: #fff;
  border: none;
  border-radius: 48rpx;
  height: 88rpx;
  line-height: 88rpx;
  font-size: 30rpx;
  font-weight: 600;
  text-align: center;
  letter-spacing: 1rpx;
  box-shadow: 0 8rpx 24rpx rgba(16, 185, 129, 0.3);
}
.btn-primary:active {
  opacity: 0.9;
  transform: scale(0.98);
}
.btn-outline {
  background: #fff;
  color: #10B981;
  border: 2rpx solid #10B981;
  border-radius: 48rpx;
  height: 88rpx;
  line-height: 84rpx;
  font-size: 30rpx;
  font-weight: 500;
  text-align: center;
}
.btn-outline:active {
  background: rgba(16, 185, 129, 0.06);
}
.btn-accent {
  background: linear-gradient(135deg, #10B981, #059669);
  color: #fff;
  border: none;
  border-radius: 48rpx;
  height: 88rpx;
  line-height: 88rpx;
  font-size: 30rpx;
  font-weight: 600;
  text-align: center;
  box-shadow: 0 8rpx 24rpx rgba(16, 185, 129, 0.3);
}
.btn-accent:active {
  opacity: 0.9;
  transform: scale(0.98);
}
.btn-sm {
  height: 64rpx;
  line-height: 64rpx;
  font-size: 26rpx;
  padding: 0 32rpx;
  border-radius: 32rpx;
}
.btn-pill {
  border-radius: 999rpx;
  padding: 0 40rpx;
}

/* ---- Tags & Chips ---- */
.tag {
  display: inline-flex;
  align-items: center;
  padding: 6rpx 16rpx;
  border-radius: 8rpx;
  font-size: 22rpx;
  font-weight: 500;
  background: #ECFDF5;
  color: #059669;
}
.tag-outline {
  background: transparent;
  border: 1rpx solid #D1FAE5;
  color: #059669;
}
.tag-danger {
  background: #FEF2F2;
  color: #EF4444;
}
.tag-warning {
  background: #FFFBEB;
  color: #D97706;
}
.tag-info {
  background: #EFF6FF;
  color: #3B82F6;
}
.tag-gold {
  background: linear-gradient(135deg, #FEF3C7, #FDE68A);
  color: #92400E;
}

/* ---- Bottom Fixed Bar ---- */
.bottom-bar {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  background: #fff;
  padding: 20rpx 32rpx;
  padding-bottom: calc(20rpx + env(safe-area-inset-bottom));
  box-shadow: 0 -4rpx 20rpx rgba(0, 0, 0, 0.06);
  z-index: 100;
  display: flex;
  align-items: center;
}

/* ---- Input Group ---- */
.input-group {
  background: #F5F7FA;
  border: 2rpx solid transparent;
  border-radius: 16rpx;
  padding: 20rpx 24rpx;
  margin-bottom: 20rpx;
  width: 100%;
  box-sizing: border-box;
  overflow: hidden;
}
.input-group:focus-within {
  border-color: #10B981;
  background: #fff;
}
.input-group input {
  height: 48rpx;
  font-size: 28rpx;
  color: #1A1A2E;
  width: 100%;
  box-sizing: border-box;
}
.input-group .label {
  font-size: 24rpx;
  color: #8896AB;
  margin-bottom: 8rpx;
  font-weight: 500;
}

/* ---- Divider ---- */
.divider {
  height: 0;
  border: none;
  border-top: 1rpx solid #EEF2F6;
  margin: 20rpx 0;
}

/* ---- Section Title ---- */
.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20rpx;
}
.section-title {
  font-size: 34rpx;
  font-weight: 700;
  color: #1A1A2E;
  display: flex;
  align-items: center;
}
.section-title-bar {
  width: 6rpx;
  height: 32rpx;
  background: linear-gradient(180deg, #10B981, #059669);
  border-radius: 3rpx;
  margin-right: 12rpx;
}
.section-more {
  font-size: 24rpx;
  color: #8896AB;
  display: flex;
  align-items: center;
}
.section-badge {
  display: inline-flex;
  align-items: center;
  gap: 8rpx;
  padding: 6rpx 20rpx;
  font-size: 22rpx;
  font-weight: 500;
  color: #10B981;
  background: #ECFDF5;
  border-radius: 100rpx;
}

/* ---- Price Display ---- */
.price {
  color: #EF4444;
  font-weight: 700;
}
.price-symbol {
  font-size: 24rpx;
}
.price-integer {
  font-size: 36rpx;
}
.price-decimal {
  font-size: 24rpx;
}
.price-original {
  color: #C0C8D4;
  font-size: 24rpx;
  text-decoration: line-through;
  margin-left: 8rpx;
}

/* ---- Flex Helpers ---- */
.flex { display: flex; }
.flex-col { display: flex; flex-direction: column; }
.flex-center { display: flex; align-items: center; justify-content: center; }
.flex-between { display: flex; align-items: center; justify-content: space-between; }
.flex-wrap { flex-wrap: wrap; }
.flex-1 { flex: 1; min-width: 0; }
.items-center { align-items: center; }
.gap-sm { gap: 12rpx; }
.gap-md { gap: 20rpx; }
.gap-lg { gap: 32rpx; }

/* ---- Spacing ---- */
.mt-10 { margin-top: 10rpx; }
.mt-20 { margin-top: 20rpx; }
.mt-30 { margin-top: 30rpx; }
.mb-20 { margin-bottom: 20rpx; }
.ml-10 { margin-left: 10rpx; }
.mr-10 { margin-right: 10rpx; }
.p-20 { padding: 20rpx; }
.p-30 { padding: 30rpx; }

/* ---- Typography ---- */
.text-center { text-align: center; }
.text-xs { font-size: 20rpx; }
.text-sm { font-size: 24rpx; }
.text-base { font-size: 28rpx; }
.text-lg { font-size: 32rpx; }
.text-xl { font-size: 36rpx; }
.text-2xl { font-size: 44rpx; }
.font-medium { font-weight: 500; }
.font-bold { font-weight: 700; }
.text-ellipsis {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.text-clamp-2 {
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}
.font-heading {
  font-weight: 700;
  letter-spacing: 0.5rpx;
}

/* #ifdef H5 */
.gradient-text {
  background: linear-gradient(135deg, #10B981, #059669);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}
/* #endif */
/* #ifndef H5 */
.gradient-text { color: #10B981; }
/* #endif */

/* ---- Shadows ---- */
.shadow-sm { box-shadow: 0 2rpx 8rpx rgba(0, 0, 0, 0.04); }
.shadow-md { box-shadow: 0 4rpx 16rpx rgba(0, 0, 0, 0.06); }
.shadow-lg { box-shadow: 0 8rpx 40rpx rgba(0, 0, 0, 0.08); }
.shadow-accent { box-shadow: 0 8rpx 24rpx rgba(16, 185, 129, 0.25); }

/* ---- Empty State ---- */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 120rpx 60rpx;
}
.empty-state .empty-icon {
  font-size: 100rpx;
  margin-bottom: 24rpx;
  opacity: 0.6;
}
.empty-state .empty-text {
  color: #8896AB;
  font-size: 28rpx;
  text-align: center;
}

/* ---- Loading Skeleton ---- */
.skeleton {
  background: linear-gradient(90deg, #F0F4F8 25%, #E8ECF1 50%, #F0F4F8 75%);
  background-size: 200% 100%;
  border-radius: 12rpx;
}

/* ---- Inverted Section ---- */
.section-inverted {
  background: #0F172A;
  color: #fff;
  position: relative;
  overflow: hidden;
}

/* ---- Badge ---- */
.badge-dot {
  width: 16rpx;
  height: 16rpx;
  background: #EF4444;
  border-radius: 50%;
  position: absolute;
  top: -4rpx;
  right: -4rpx;
}
.badge-count {
  min-width: 32rpx;
  height: 32rpx;
  line-height: 32rpx;
  text-align: center;
  font-size: 20rpx;
  font-weight: 600;
  color: #fff;
  background: #EF4444;
  border-radius: 16rpx;
  padding: 0 8rpx;
  position: absolute;
  top: -8rpx;
  right: -8rpx;
}

/* ---- Safe Area Bottom Spacing ---- */
.safe-bottom {
  padding-bottom: env(safe-area-inset-bottom);
}
.bottom-placeholder {
  height: calc(120rpx + env(safe-area-inset-bottom));
}
</style>

