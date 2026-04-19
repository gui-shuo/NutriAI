<script setup>
/**
 * 我的 - 个人中心首页
 * Design: Digital Greenhouse with uView components
 */
import { ref, computed } from 'vue'
import { useUserStore } from '../../stores/user'
import NavBar from '../../components/NavBar.vue'
import { checkLogin } from '../../utils/common'

const userStore = useUserStore()

const isLoggedIn = computed(() => userStore.isLoggedIn)

// 菜单分组 - 统一入口，无重复
const menuGroups = ref([
  {
    title: '我的订单',
    items: [
      { icon: 'list', label: '全部订单', url: '/pages/profile/orders' },
      { icon: 'coupon', label: '我的优惠券', url: '/pages/profile/coupons' },
    ]
  },
  {
    title: '服务',
    items: [
      { icon: 'chat', label: '商家消息', url: '/pages/profile/merchant-chat' },
      { icon: 'map', label: '收货地址', url: '/pages/profile/address' },
      { icon: 'bell', label: '消息通知', url: '/pages/profile/notifications' },
      { icon: 'edit-pen', label: '意见反馈', url: '/pages/profile/feedback' },
      { icon: 'setting', label: '设置', url: '/pages/profile/settings' },
    ]
  }
])

function goToLogin() {
  uni.navigateTo({ url: '/pages/auth/login' })
}

function goToEdit() {
  if (!checkLogin()) return
  uni.navigateTo({ url: '/pages/profile/edit' })
}

function goToPage(url) {
  if (!checkLogin()) return
  uni.navigateTo({ url })
}
</script>

<template>
  <view class="page">
    <NavBar>
      <template #center>
        <text class="nav-title">我的</text>
      </template>
      <template #right>
        <view class="nav-settings" @tap="goToPage('/pages/profile/settings')">
          <u-icon name="setting" size="24" color="#1a1c1a" />
        </view>
      </template>
    </NavBar>

    <scroll-view scroll-y class="content" :enhanced="true" :show-scrollbar="false">
      <!-- 用户信息卡片 -->
      <view class="user-card">
        <view class="user-card__bg" />

        <view v-if="isLoggedIn" class="user-card__info" @tap="goToEdit">
          <u-avatar :src="userStore.userAvatar" size="60" shape="circle" />
          <view class="user-card__text">
            <text class="user-card__name">{{ userStore.userName }}</text>
            <view v-if="userStore.isVip" class="user-card__vip">
              <u-icon name="crown-fill" size="14" color="#8b6914" />
              <text class="user-card__vip-text">VIP会员</text>
            </view>
            <text class="user-card__edit">编辑资料 ›</text>
          </view>
        </view>

        <view v-else class="user-card__info" @tap="goToLogin">
          <u-avatar icon="account" size="60" shape="circle" bgColor="#f0f0f0" />
          <view class="user-card__text">
            <text class="user-card__name">点击登录</text>
            <text class="user-card__desc">登录享受更多服务</text>
          </view>
        </view>
      </view>

      <!-- 菜单分组 (已去重，不再有重复的快捷入口) -->
      <view
        v-for="(group, gIdx) in menuGroups"
        :key="gIdx"
        class="menu-group"
      >
        <text class="menu-group__title">{{ group.title }}</text>
        <view class="menu-group__card">
          <view
            v-for="(item, iIdx) in group.items"
            :key="iIdx"
            class="menu-item"
            @tap="goToPage(item.url)"
          >
            <u-icon :name="item.icon" size="22" color="#0a6e2c" />
            <text class="menu-item__label">{{ item.label }}</text>
            <u-icon name="arrow-right" size="16" color="#ccc" />
          </view>
        </view>
      </view>

      <view style="height: 40rpx;" />
    </scroll-view>
  </view>
</template>

<style lang="scss" scoped>
@import '../../styles/design-system.scss';

.page {
  min-height: 100vh;
  background: #ffffff;
  overflow-x: hidden;
  width: 100%;
}

.content {
  height: calc(100vh - 88px);
}

.nav-title {
  font-size: 36rpx;
  font-weight: 800;
  color: $on-surface;
}

.nav-settings {
  width: 64rpx;
  height: 64rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}

// 用户卡片
.user-card {
  position: relative;
  margin: 16rpx 24rpx;
  border-radius: 16px;
  overflow: hidden;
  background: #ffffff;
  box-shadow: $shadow-md;

  &__bg {
    position: absolute;
    top: -60rpx;
    right: -60rpx;
    width: 300rpx;
    height: 300rpx;
    border-radius: 50%;
    background: rgba($primary, 0.08);
  }

  &__info {
    display: flex;
    align-items: center;
    gap: 24rpx;
    padding: 40rpx;
    position: relative;
    z-index: 1;
  }

  &__text {
    flex: 1;
  }

  &__name {
    display: block;
    font-size: 36rpx;
    font-weight: 700;
    color: $on-surface;
    margin-bottom: 6rpx;
  }

  &__desc {
    display: block;
    font-size: 24rpx;
    color: $on-surface-variant;
  }

  &__vip {
    display: inline-flex;
    align-items: center;
    gap: 6rpx;
    background: linear-gradient(135deg, #f5e6cc, #e8d5b0);
    padding: 4rpx 14rpx;
    border-radius: 50px;
    margin-bottom: 6rpx;

    &-text {
      font-size: 22rpx;
      font-weight: 600;
      color: #8b6914;
    }
  }

  &__edit {
    font-size: 24rpx;
    color: $primary;
  }
}

// 菜单分组
.menu-group {
  margin: 0 24rpx 24rpx;

  &__title {
    display: block;
    font-size: 24rpx;
    font-weight: 600;
    color: $on-surface-variant;
    margin-bottom: 12rpx;
    padding-left: 8rpx;
  }

  &__card {
    background: #ffffff;
    border-radius: 16px;
    overflow: hidden;
    box-shadow: $shadow-sm;
  }
}

.menu-item {
  display: flex;
  align-items: center;
  padding: 28rpx 24rpx;
  gap: 20rpx;

  & + & {
    border-top: 1rpx solid rgba(0, 0, 0, 0.04);
  }

  &__label {
    flex: 1;
    font-size: 28rpx;
    color: $on-surface;
  }
}
</style>
