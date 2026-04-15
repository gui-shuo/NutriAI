<script setup lang="ts">
import { onLaunch, onShow } from '@dcloudio/uni-app'
import { useUserStore } from './stores/user'

onLaunch(() => {
  const userStore = useUserStore()
  userStore.restore()
})

onShow(() => {})
</script>

<style lang="scss">
/* Reset & Global */
page {
  font-family: -apple-system, BlinkMacSystemFont, 'PingFang SC', 'Helvetica Neue', 'Microsoft YaHei', sans-serif;
  font-size: 28rpx;
  color: $text-primary;
  background-color: $bg-page;
  line-height: 1.6;
  -webkit-font-smoothing: antialiased;
}

/* ========== Layout Utilities ========== */
.flex { display: flex; }
.flex-col { flex-direction: column; }
.flex-row { flex-direction: row; }
.flex-wrap { flex-wrap: wrap; }
.flex-1 { flex: 1; }
.flex-center { display: flex; align-items: center; justify-content: center; }
.flex-between { display: flex; align-items: center; justify-content: space-between; }
.flex-start { display: flex; align-items: flex-start; }
.items-center { align-items: center; }
.items-end { align-items: flex-end; }
.justify-center { justify-content: center; }
.justify-between { justify-content: space-between; }
.justify-end { justify-content: flex-end; }
.gap-sm { gap: $spacing-sm; }
.gap-md { gap: $spacing-md; }

/* ========== Spacing ========== */
.p-sm { padding: $spacing-sm; }
.p-md { padding: $spacing-md; }
.p-lg { padding: $spacing-lg; }
.px-md { padding-left: $spacing-md; padding-right: $spacing-md; }
.px-lg { padding-left: $spacing-lg; padding-right: $spacing-lg; }
.py-sm { padding-top: $spacing-sm; padding-bottom: $spacing-sm; }
.py-md { padding-top: $spacing-md; padding-bottom: $spacing-md; }
.mt-sm { margin-top: $spacing-sm; }
.mt-md { margin-top: $spacing-md; }
.mt-lg { margin-top: $spacing-lg; }
.mb-sm { margin-bottom: $spacing-sm; }
.mb-md { margin-bottom: $spacing-md; }
.mb-lg { margin-bottom: $spacing-lg; }
.ml-sm { margin-left: $spacing-sm; }
.mr-sm { margin-right: $spacing-sm; }

/* ========== Typography ========== */
.text-xs { font-size: $font-xs; }
.text-sm { font-size: $font-sm; }
.text-base { font-size: $font-base; }
.text-md { font-size: $font-md; }
.text-lg { font-size: $font-lg; }
.text-xl { font-size: $font-xl; }
.text-2xl { font-size: $font-2xl; }
.text-3xl { font-size: $font-3xl; }
.text-bold { font-weight: 600; }
.text-bolder { font-weight: 700; }
.text-center { text-align: center; }
.text-right { text-align: right; }
.text-primary { color: $primary; }
.text-secondary { color: $text-secondary; }
.text-muted { color: $text-muted; }
.text-white { color: $text-white; }
.text-success { color: $success; }
.text-warning { color: $warning; }
.text-error { color: $error; }
.text-price { color: $error; font-weight: 700; font-family: 'DIN Alternate', 'Helvetica Neue', sans-serif; }
.text-ellipsis { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.text-line-2 {
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

/* ========== Card ========== */
.card {
  background: $bg-card;
  border-radius: $radius-lg;
  padding: $spacing-lg;
  box-shadow: $shadow-sm;
}

.card-flat {
  background: $bg-card;
  border-radius: $radius-lg;
  padding: $spacing-lg;
}

/* ========== Buttons ========== */
.btn {
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: $radius-xl;
  font-size: $font-base;
  font-weight: 500;
  padding: 0 $spacing-lg;
  height: 80rpx;
  line-height: 80rpx;
  border: none;
  transition: opacity 0.2s;

  &::after { border: none; }
  &:active { opacity: 0.85; }
}

.btn-primary {
  @extend .btn;
  background: $gradient-primary;
  color: $text-white;
  box-shadow: $shadow-primary;
}

.btn-outline {
  @extend .btn;
  background: transparent;
  color: $primary;
  border: 2rpx solid $primary;
}

.btn-ghost {
  @extend .btn;
  background: $primary-50;
  color: $primary;
}

.btn-plain {
  @extend .btn;
  background: $bg-muted;
  color: $text-secondary;
}

.btn-block {
  width: 100%;
}

.btn-sm {
  height: 56rpx;
  line-height: 56rpx;
  font-size: $font-sm;
  padding: 0 $spacing-md;
  border-radius: $radius-md;
}

.btn-lg {
  height: 96rpx;
  line-height: 96rpx;
  font-size: $font-lg;
  border-radius: $radius-2xl;
}

.btn-disabled {
  opacity: 0.5;
  pointer-events: none;
}

/* ========== Form ========== */
.input-group {
  background: $bg-input;
  border-radius: $radius-lg;
  padding: 0 $spacing-md;
  height: 88rpx;
  display: flex;
  align-items: center;
  border: 2rpx solid transparent;
  transition: border-color 0.2s;

  &:focus-within {
    border-color: $primary;
    background: $bg-card;
  }

  .input-icon {
    width: 40rpx;
    height: 40rpx;
    margin-right: $spacing-sm;
    color: $text-muted;
  }

  input {
    flex: 1;
    height: 100%;
    font-size: $font-base;
    color: $text-primary;
  }
}

/* ========== Badge ========== */
.badge {
  display: inline-flex;
  align-items: center;
  padding: 4rpx 16rpx;
  border-radius: $radius-full;
  font-size: $font-xs;
  font-weight: 500;
}

.badge-primary {
  @extend .badge;
  background: $primary-50;
  color: $primary;
}

.badge-warning {
  @extend .badge;
  background: #FEF3C7;
  color: #D97706;
}

.badge-error {
  @extend .badge;
  background: #FEE2E2;
  color: $error;
}

.badge-success {
  @extend .badge;
  background: #DCFCE7;
  color: #16A34A;
}

/* ========== Tag ========== */
.tag {
  display: inline-flex;
  align-items: center;
  padding: 6rpx 20rpx;
  border-radius: $radius-full;
  font-size: $font-sm;
  background: $bg-muted;
  color: $text-secondary;
  margin-right: $spacing-sm;
  margin-bottom: $spacing-sm;

  &.active {
    background: $primary-50;
    color: $primary;
  }
}

/* ========== Divider ========== */
.divider {
  height: 1rpx;
  background: $border-color;
  margin: $spacing-md 0;
}

/* ========== Empty State ========== */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 120rpx $spacing-lg;

  .empty-icon {
    width: 200rpx;
    height: 200rpx;
    margin-bottom: $spacing-lg;
    opacity: 0.4;
  }

  .empty-text {
    font-size: $font-base;
    color: $text-muted;
  }

  .empty-action {
    margin-top: $spacing-lg;
  }
}

/* ========== Safe Area ========== */
.safe-bottom {
  padding-bottom: constant(safe-area-inset-bottom);
  padding-bottom: env(safe-area-inset-bottom);
}

/* ========== Section ========== */
.section {
  margin-bottom: $spacing-md;
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: $spacing-md $spacing-lg;
}

.section-title {
  font-size: $font-lg;
  font-weight: 600;
  color: $text-primary;
  position: relative;
  padding-left: 20rpx;

  &::before {
    content: '';
    position: absolute;
    left: 0;
    top: 50%;
    transform: translateY(-50%);
    width: 6rpx;
    height: 28rpx;
    background: $gradient-primary;
    border-radius: 3rpx;
  }
}

.section-more {
  font-size: $font-sm;
  color: $text-muted;
}

/* ========== Grid ========== */
.grid-2 {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: $spacing-md;
}

/* ========== Avatar ========== */
.avatar {
  width: 80rpx;
  height: 80rpx;
  border-radius: $radius-full;
  overflow: hidden;
  background: $bg-muted;
}

.avatar-sm {
  width: 56rpx;
  height: 56rpx;
  border-radius: $radius-full;
  overflow: hidden;
  background: $bg-muted;
}

.avatar-lg {
  width: 120rpx;
  height: 120rpx;
  border-radius: $radius-full;
  overflow: hidden;
  background: $bg-muted;
}

/* ========== Loading ========== */
.loading-mask {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(255, 255, 255, 0.7);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 999;
}

/* ========== Status Bar Placeholder ========== */
.status-bar {
  width: 100%;
}
</style>
