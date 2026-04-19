<script setup>
/**
 * 商家端 - 我的
 * 与用户端"我的"功能类似：头像昵称、设置、退出等
 */
import { ref, computed } from 'vue'
import { useUserStore } from '../../stores/user'
import NavBar from '../../components/NavBar.vue'
import MerchantTabBar from '../../components/MerchantTabBar.vue'

const userStore = useUserStore()

const isLoggedIn = computed(() => userStore.isLoggedIn)
const merchantName = computed(() => userStore.merchantInfo?.name || '我的店铺')

const menuGroups = ref([
  {
    title: '店铺管理',
    items: [
      { icon: 'order', label: '订单管理', url: '/pages/merchant/orders' },
      { icon: 'bag', label: '店铺设置', url: '/pages/merchant/store' },
    ]
  },
  {
    title: '服务',
    items: [
      { icon: 'bell', label: '消息通知', url: '/pages/merchant/messages' },
      { icon: 'chat', label: '意见反馈', action: 'feedback' },
      { icon: 'setting', label: '设置', action: 'settings' },
    ]
  }
])

function goToPage(item) {
  if (item.url) {
    uni.redirectTo({ url: item.url })
  } else if (item.action === 'feedback') {
    uni.navigateTo({ url: '/pages/profile/feedback' })
  } else if (item.action === 'settings') {
    uni.navigateTo({ url: '/pages/profile/settings' })
  }
}

function handleLogout() {
  uni.showModal({
    title: '提示',
    content: '确定退出商家端登录吗？',
    success: (res) => {
      if (res.confirm) {
        userStore.logout()
        uni.redirectTo({ url: '/pages/auth/role-select' })
      }
    }
  })
}

function switchToUser() {
  uni.switchTab({ url: '/pages/index/index' })
}
</script>

<template>
  <view class="page">
    <NavBar>
      <template #center>
        <text class="nav-title">我的</text>
      </template>
    </NavBar>

    <scroll-view scroll-y class="content" :enhanced="true" :show-scrollbar="false">
      <!-- 商家信息卡片 -->
      <view class="user-card">
        <view class="user-card__bg" />
        <view class="user-card__info">
          <u-avatar :src="userStore.userAvatar" size="60" shape="circle" />
          <view class="user-card__text">
            <text class="user-card__name">{{ userStore.userName }}</text>
            <view class="user-card__merchant-tag">
              <u-icon name="bag" size="14" color="#0a6e2c" />
              <text class="user-card__merchant-name">{{ merchantName }}</text>
            </view>
          </view>
        </view>
      </view>

      <!-- 切换用户端 -->
      <view class="switch-card" @tap="switchToUser">
        <u-icon name="account" size="22" color="#0a6e2c" />
        <text class="switch-card__text">切换到用户端</text>
        <u-icon name="arrow-right" size="16" color="#ccc" />
      </view>

      <!-- 菜单分组 -->
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
            @tap="goToPage(item)"
          >
            <u-icon :name="item.icon" size="22" color="#0a6e2c" />
            <text class="menu-item__label">{{ item.label }}</text>
            <u-icon name="arrow-right" size="16" color="#ccc" />
          </view>
        </view>
      </view>

      <!-- 退出登录 -->
      <view class="logout-wrap">
        <u-button
          text="退出登录"
          shape="circle"
          color="#ff4d4f"
          :plain="true"
          @click="handleLogout"
        />
      </view>

      <view style="height: 160rpx;" />
    </scroll-view>

    <MerchantTabBar :current="3" />
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
    margin-bottom: 8rpx;
  }

  &__merchant-tag {
    display: inline-flex;
    align-items: center;
    gap: 6rpx;
    background: rgba($primary, 0.08);
    padding: 4rpx 14rpx;
    border-radius: 50px;

    &-name {
      font-size: 22rpx;
      color: $primary;
      font-weight: 600;
    }
  }
}

// 切换用户端
.switch-card {
  display: flex;
  align-items: center;
  gap: 16rpx;
  margin: 0 24rpx 24rpx;
  padding: 24rpx 28rpx;
  background: rgba($primary, 0.04);
  border-radius: $radius-xl;

  &__text {
    flex: 1;
    font-size: 28rpx;
    color: $primary;
    font-weight: 600;
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

// 退出登录
.logout-wrap {
  padding: 32rpx 24rpx;
}
</style>
