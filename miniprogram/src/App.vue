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
/* Google Fonts for hand-drawn style */
@import url('https://fonts.googleapis.com/css2?family=Kalam:wght@400;700&family=Patrick+Hand&family=ZCOOL+KuaiLe&display=swap');

page {
  background-color: #fdfbf7;
  background-image: radial-gradient(#e5e0d8 1px, transparent 1px);
  background-size: 24px 24px;
  font-family: 'Patrick Hand', 'PingFang SC', 'Microsoft YaHei', -apple-system, BlinkMacSystemFont, cursive;
  font-size: 28rpx;
  color: #2d2d2d;
  line-height: 1.6;
}

.container {
  padding: 20rpx 30rpx;
}

/* 主题色 */
.text-primary { color: #ff4d4d; }
.text-secondary { color: #5a5a5a; }
.text-danger { color: #ff4d4d; }
.text-warning { color: #ff9800; }
.text-ink { color: #2d5da1; }
.bg-primary { background-color: #ff4d4d; }
.bg-white { background-color: #fff; }
.bg-paper { background-color: #fdfbf7; }
.bg-sticky { background-color: #fff9c4; }
.bg-muted { background-color: #e5e0d8; }

/* 通用卡片 — 手绘风格 */
.card {
  background: #fff;
  border: 2rpx solid #2d2d2d;
  border-radius: 15px 225px 15px 255px / 255px 15px 225px 15px;
  padding: 24rpx;
  margin-bottom: 20rpx;
  box-shadow: 3px 3px 0px 0px rgba(45, 45, 45, 0.1);
  position: relative;
}

/* 按钮 — 手绘风格 */
.btn-primary {
  background: #ff4d4d;
  color: #fff;
  border: 3rpx solid #2d2d2d;
  border-radius: 255px 15px 225px 15px / 15px 225px 15px 255px;
  height: 88rpx;
  line-height: 88rpx;
  font-size: 32rpx;
  font-weight: 500;
  text-align: center;
  font-family: 'Patrick Hand', 'PingFang SC', cursive;
  box-shadow: 4px 4px 0px 0px #2d2d2d;
}
.btn-primary::after { border: none; }
.btn-primary:active {
  box-shadow: none;
  transform: translate(4rpx, 4rpx);
}

.btn-outline {
  background: #fff;
  color: #2d2d2d;
  border: 3rpx solid #2d2d2d;
  border-radius: 255px 15px 225px 15px / 15px 225px 15px 255px;
  height: 88rpx;
  line-height: 88rpx;
  font-size: 32rpx;
  text-align: center;
  font-family: 'Patrick Hand', 'PingFang SC', cursive;
  box-shadow: 4px 4px 0px 0px #2d2d2d;
}
.btn-outline:active {
  background: #ff4d4d;
  color: #fff;
  box-shadow: none;
  transform: translate(4rpx, 4rpx);
}

.btn-ink {
  background: #2d5da1;
  color: #fff;
  border: 3rpx solid #2d2d2d;
  border-radius: 255px 15px 225px 15px / 15px 225px 15px 255px;
  height: 88rpx;
  line-height: 88rpx;
  font-size: 32rpx;
  text-align: center;
  font-family: 'Patrick Hand', 'PingFang SC', cursive;
  box-shadow: 4px 4px 0px 0px #2d2d2d;
}
.btn-ink:active {
  box-shadow: none;
  transform: translate(4rpx, 4rpx);
}

/* 输入框 — 手绘风格 */
.input-group {
  background: #fff;
  border: 2rpx solid #2d2d2d;
  border-radius: 185px 10px 155px 10px / 10px 155px 10px 185px;
  padding: 20rpx 24rpx;
  margin-bottom: 20rpx;
}
.input-group input {
  height: 48rpx;
  font-size: 28rpx;
  font-family: 'Patrick Hand', 'PingFang SC', cursive;
  color: #2d2d2d;
}
.input-group .label {
  font-size: 24rpx;
  color: rgba(45, 45, 45, 0.6);
  margin-bottom: 8rpx;
  font-family: 'Kalam', 'ZCOOL KuaiLe', 'PingFang SC', cursive;
  font-weight: 700;
}

/* 分割线 — 虚线风格 */
.divider {
  height: 0;
  border: none;
  border-top: 2rpx dashed #e5e0d8;
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

/* Heading font utility */
.font-heading {
  font-family: 'Kalam', 'ZCOOL KuaiLe', 'PingFang SC', cursive;
  font-weight: 700;
}

/* Hand-drawn decorative utilities */
.wobbly {
  border-radius: 255px 15px 225px 15px / 15px 225px 15px 255px;
}
.wobbly-md {
  border-radius: 15px 225px 15px 255px / 255px 15px 225px 15px;
}
.wobbly-sm {
  border-radius: 185px 10px 155px 10px / 10px 155px 10px 185px;
}

.hard-shadow {
  box-shadow: 4px 4px 0px 0px #2d2d2d;
}
.hard-shadow-sm {
  box-shadow: 3px 3px 0px 0px rgba(45, 45, 45, 0.1);
}

.dashed-border {
  border-style: dashed;
}

.sticky-note {
  background-color: #fff9c4;
  border: 2rpx solid #2d2d2d;
  box-shadow: 4px 4px 0px 0px #2d2d2d;
  padding: 20rpx;
  transform: rotate(-1deg);
}

/* Tape decoration */
.decoration-tape::before {
  content: '';
  position: absolute;
  top: -8px;
  left: 50%;
  transform: translateX(-50%) rotate(-2deg);
  width: 80px;
  height: 24px;
  background: rgba(229, 224, 216, 0.7);
  border: 1px solid rgba(45, 45, 45, 0.15);
  z-index: 1;
}

/* Tack decoration */
.decoration-tack::before {
  content: '';
  position: absolute;
  top: -8px;
  left: 50%;
  transform: translateX(-50%);
  width: 16px;
  height: 16px;
  background: #ff4d4d;
  border: 2px solid #2d2d2d;
  border-radius: 50%;
  box-shadow: 1px 1px 0px 0px rgba(45, 45, 45, 0.3);
  z-index: 1;
}

/* Sketch icon wrapper */
.sketch-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 72rpx;
  height: 72rpx;
  border: 2rpx solid #2d2d2d;
  border-radius: 255px 15px 225px 15px / 15px 225px 15px 255px;
  background: #fff;
  box-shadow: 2px 2px 0px 0px rgba(45, 45, 45, 0.1);
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
  color: rgba(45, 45, 45, 0.6);
  font-size: 28rpx;
  font-family: 'Patrick Hand', 'PingFang SC', cursive;
}

/* Section title style */
.section-title {
  font-family: 'Kalam', 'ZCOOL KuaiLe', 'PingFang SC', cursive;
  font-weight: 700;
  font-size: 36rpx;
  color: #2d2d2d;
  position: relative;
  display: inline-block;
}
.section-title::after {
  content: '';
  position: absolute;
  bottom: -4rpx;
  left: 0;
  width: 100%;
  height: 3rpx;
  background: #ff4d4d;
  border-radius: 2rpx;
  transform: rotate(-0.5deg);
}
</style>

