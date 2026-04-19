<script setup>
/**
 * 角色选择页 - 用户/商家登录入口
 */

function selectRole(role) {
  uni.navigateTo({
    url: `/pages/auth/login?role=${role}`
  })
}
</script>

<template>
  <view class="page">
    <!-- 顶部装饰 -->
    <view class="header">
      <view class="header__decor-1" />
      <view class="header__decor-2" />
      <view class="header__content">
        <u-icon name="leaf-fill" size="48" color="#0a6e2c" />
        <text class="header__title">NutriAI</text>
        <text class="header__subtitle">请选择登录身份</text>
      </view>
    </view>

    <!-- 角色选择卡片 -->
    <view class="role-cards">
      <view class="role-card" @tap="selectRole('user')">
        <view class="role-card__icon-wrap role-card__icon-wrap--user">
          <u-icon name="account" size="48" color="#0a6e2c" />
        </view>
        <text class="role-card__title">用户登录</text>
        <text class="role-card__desc">浏览菜品、下单点餐、管理健康</text>
        <view class="role-card__arrow">
          <u-icon name="arrow-right" size="20" color="#0a6e2c" />
        </view>
      </view>

      <view class="role-card" @tap="selectRole('merchant')">
        <view class="role-card__icon-wrap role-card__icon-wrap--merchant">
          <u-icon name="bag" size="48" color="#0a6e2c" />
        </view>
        <text class="role-card__title">商家登录</text>
        <text class="role-card__desc">管理订单、店铺设置、处理消息</text>
        <view class="role-card__arrow">
          <u-icon name="arrow-right" size="20" color="#0a6e2c" />
        </view>
      </view>
    </view>

    <!-- 底部 -->
    <view class="footer" @tap="() => uni.navigateBack()">
      <text class="footer__text">暂不登录</text>
    </view>
  </view>
</template>

<style lang="scss" scoped>
@import '../../styles/design-system.scss';

.page {
  min-height: 100vh;
  background: #ffffff;
  display: flex;
  flex-direction: column;
  overflow-x: hidden;
  width: 100%;
}

.header {
  position: relative;
  padding: 160rpx 40rpx 80rpx;
  overflow: hidden;
  text-align: center;

  &__decor-1 {
    position: absolute;
    top: -100rpx;
    right: -80rpx;
    width: 400rpx;
    height: 400rpx;
    border-radius: $radius-full;
    background: rgba($primary, 0.08);
  }

  &__decor-2 {
    position: absolute;
    top: 40rpx;
    left: -120rpx;
    width: 300rpx;
    height: 300rpx;
    border-radius: $radius-full;
    background: rgba($secondary, 0.06);
  }

  &__content {
    position: relative;
    z-index: 1;
  }

  &__title {
    display: block;
    font-size: $font-4xl;
    font-weight: 800;
    color: $primary;
    letter-spacing: -0.02em;
    margin: 16rpx 0 8rpx;
  }

  &__subtitle {
    display: block;
    font-size: $font-xl;
    color: $on-surface-variant;
    font-weight: 400;
  }
}

.role-cards {
  flex: 1;
  padding: 0 40rpx;
  display: flex;
  flex-direction: column;
  gap: 32rpx;
}

.role-card {
  position: relative;
  background: $surface-container-lowest;
  border-radius: $radius-2xl;
  padding: 48rpx 40rpx;
  box-shadow: $shadow-md;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16rpx;

  &:active {
    transform: scale(0.98);
    box-shadow: $shadow-sm;
  }

  &__icon-wrap {
    width: 120rpx;
    height: 120rpx;
    border-radius: $radius-full;
    display: flex;
    align-items: center;
    justify-content: center;
    margin-bottom: 8rpx;

    &--user {
      background: rgba($primary, 0.1);
    }

    &--merchant {
      background: rgba($secondary, 0.15);
    }
  }

  &__title {
    font-size: $font-xl;
    font-weight: 700;
    color: $on-surface;
  }

  &__desc {
    font-size: $font-sm;
    color: $on-surface-variant;
  }

  &__arrow {
    position: absolute;
    right: 32rpx;
    top: 50%;
    transform: translateY(-50%);
  }
}

.footer {
  padding: 60rpx 0 80rpx;
  text-align: center;

  &__text {
    font-size: $font-sm;
    color: $on-surface-variant;
  }
}
</style>
