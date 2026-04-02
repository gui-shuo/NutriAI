<script setup lang="ts">
import { onLaunch, onShow } from "@dcloudio/uni-app";
import { useUserStore } from "@/stores/user";
import { useAppStore } from "@/stores/app";

onLaunch(() => {
  const userStore = useUserStore();
  const appStore = useAppStore();
  userStore.restore();
  appStore.fetchConfig();
});

onShow(() => {});
</script>

<style>
/* Minimalist Modern Fonts */
@import url('https://fonts.googleapis.com/css2?family=Calistoga&family=Inter:wght@400;500;600;700&family=JetBrains+Mono:wght@400;500&display=swap');

page {
  background-color: #FAFAFA;
  font-family: 'Inter', 'PingFang SC', 'Microsoft YaHei', -apple-system, BlinkMacSystemFont, system-ui, sans-serif;
  font-size: 28rpx;
  color: #0F172A;
  line-height: 1.6;
}

.container {
  padding: 20rpx 30rpx;
}

/* Theme colors */
.text-primary { color: #10B981; }
.text-secondary { color: #64748B; }
.text-danger { color: #EF4444; }
.text-warning { color: #F59E0B; }
.text-success { color: #10B981; }
.bg-primary { background: linear-gradient(135deg, #10B981, #34D399); }
.bg-white { background-color: #fff; }
.bg-page { background-color: #FAFAFA; }
.bg-muted { background-color: #F1F5F9; }

/* Card — clean modern */
.card {
  background: #fff;
  border: 1rpx solid #E2E8F0;
  border-radius: 24rpx;
  padding: 24rpx;
  margin-bottom: 20rpx;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
  position: relative;
}

/* Primary Button — gradient */
.btn-primary {
  background: linear-gradient(135deg, #10B981, #34D399);
  color: #fff;
  border: none;
  border-radius: 16rpx;
  height: 88rpx;
  line-height: 88rpx;
  font-size: 32rpx;
  font-weight: 600;
  text-align: center;
  font-family: 'Inter', 'PingFang SC', sans-serif;
  box-shadow: 0 4px 14px rgba(16, 185, 129, 0.25);
}
.btn-primary::after { border: none; }
.btn-primary:active {
  box-shadow: 0 2px 6px rgba(16, 185, 129, 0.15);
  transform: translateY(1px);
  opacity: 0.95;
}

/* Outline Button */
.btn-outline {
  background: #fff;
  color: #0F172A;
  border: 2rpx solid #E2E8F0;
  border-radius: 16rpx;
  height: 88rpx;
  line-height: 88rpx;
  font-size: 32rpx;
  text-align: center;
  font-family: 'Inter', 'PingFang SC', sans-serif;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
}
.btn-outline:active {
  background: rgba(16, 185, 129, 0.04);
  border-color: #10B981;
  color: #10B981;
}

/* Accent Button */
.btn-accent {
  background: linear-gradient(135deg, #10B981, #34D399);
  color: #fff;
  border: none;
  border-radius: 16rpx;
  height: 88rpx;
  line-height: 88rpx;
  font-size: 32rpx;
  text-align: center;
  font-family: 'Inter', 'PingFang SC', sans-serif;
  box-shadow: 0 4px 14px rgba(16, 185, 129, 0.25);
}
.btn-accent:active {
  box-shadow: 0 2px 6px rgba(16, 185, 129, 0.15);
  transform: translateY(1px);
}

/* Input group — modern clean */
.input-group {
  background: #fff;
  border: 2rpx solid #E2E8F0;
  border-radius: 16rpx;
  padding: 20rpx 24rpx;
  margin-bottom: 20rpx;
  transition: border-color 0.2s ease;
}
.input-group:focus-within {
  border-color: #10B981;
  box-shadow: 0 0 0 3px rgba(16, 185, 129, 0.1);
}
.input-group input {
  height: 48rpx;
  font-size: 28rpx;
  font-family: 'Inter', 'PingFang SC', sans-serif;
  color: #0F172A;
}
.input-group .label {
  font-size: 24rpx;
  color: #64748B;
  margin-bottom: 8rpx;
  font-weight: 500;
}

/* Divider — clean line */
.divider {
  height: 0;
  border: none;
  border-top: 1rpx solid #E2E8F0;
  margin: 20rpx 0;
}

/* Flex helpers */
.flex { display: flex; }
.flex-col { display: flex; flex-direction: column; }
.flex-center { display: flex; align-items: center; justify-content: center; }
.flex-between { display: flex; align-items: center; justify-content: space-between; }
.flex-wrap { flex-wrap: wrap; }
.flex-1 { flex: 1; }
.items-center { align-items: center; }

/* Spacing */
.mt-10 { margin-top: 10rpx; }
.mt-20 { margin-top: 20rpx; }
.mt-30 { margin-top: 30rpx; }
.mb-20 { margin-bottom: 20rpx; }
.ml-10 { margin-left: 10rpx; }
.mr-10 { margin-right: 10rpx; }
.p-20 { padding: 20rpx; }
.p-30 { padding: 30rpx; }

/* Text */
.text-center { text-align: center; }
.text-sm { font-size: 24rpx; }
.text-base { font-size: 28rpx; }
.text-lg { font-size: 32rpx; }
.text-xl { font-size: 36rpx; }
.font-bold { font-weight: 600; }

/* Heading utility */
.font-heading {
  font-family: 'Calistoga', Georgia, 'PingFang SC', serif;
}

/* Gradient text */
.gradient-text {
  background: linear-gradient(135deg, #10B981, #34D399);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}

/* Section badge */
.section-badge {
  display: inline-flex;
  align-items: center;
  gap: 8rpx;
  padding: 4rpx 20rpx;
  font-family: 'JetBrains Mono', monospace;
  font-size: 22rpx;
  font-weight: 500;
  letter-spacing: 0.05em;
  text-transform: uppercase;
  color: #10B981;
  background: rgba(16, 185, 129, 0.08);
  border-radius: 100rpx;
}

/* Modern shadow utility */
.shadow-sm {
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
}
.shadow-md {
  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.07);
}
.shadow-accent {
  box-shadow: 0 4px 14px rgba(16, 185, 129, 0.25);
}

/* Empty state */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 100rpx 0;
}
.empty-state .empty-icon {
  font-size: 80rpx;
  margin-bottom: 20rpx;
}
.empty-state .empty-text {
  color: #64748B;
  font-size: 28rpx;
}

/* Section title */
.section-title {
  font-family: 'Calistoga', Georgia, 'PingFang SC', serif;
  font-size: 36rpx;
  color: #0F172A;
  position: relative;
  display: inline-block;
}
.section-title::after {
  content: '';
  position: absolute;
  bottom: -6rpx;
  left: 0;
  width: 40rpx;
  height: 4rpx;
  background: linear-gradient(135deg, #10B981, #34D399);
  border-radius: 4rpx;
}

/* Inverted section */
.section-inverted {
  background: #0F172A;
  color: #fff;
  position: relative;
  overflow: hidden;
}
.section-inverted::before {
  content: '';
  position: absolute;
  inset: 0;
  background-image: radial-gradient(rgba(255,255,255,0.04) 1px, transparent 1px);
  background-size: 20px 20px;
  pointer-events: none;
}
</style>

