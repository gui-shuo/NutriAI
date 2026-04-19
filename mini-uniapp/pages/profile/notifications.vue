<script setup>
/**
 * 消息通知页
 */
import { ref, onMounted } from 'vue'
import NavBar from '../../components/NavBar.vue'
import { announcementApi } from '../../services/api'
import { formatTime } from '../../utils/common'

const notifications = ref([])
const loading = ref(false)

async function fetchNotifications() {
  loading.value = true
  try {
    const res = await announcementApi.getList({ page: 1, size: 20 })
    // Announcement API uses page starting from 1, returns Page object
    const list = res?.content || (Array.isArray(res) ? res : [])
    notifications.value = list.map(n => ({
      ...n,
      type: n.type === 'info' ? '系统通知' : n.type === 'warning' ? '重要提醒' : n.type === 'error' ? '紧急通知' : '系统通知',
    }))
  } catch (e) {
    notifications.value = []
  } finally {
    loading.value = false
  }
}

async function markRead(id) {
  try {
    await announcementApi.markRead(id)
  } catch (e) {
    // ignore
  }
}

onMounted(() => fetchNotifications())
</script>

<template>
  <view class="page">
    <NavBar showBack title="消息通知" />

    <scroll-view scroll-y class="content" :enhanced="true" :show-scrollbar="false">
      <view v-if="loading" class="state-tip">
        <u-loading-icon mode="circle" size="28" color="#0a6e2c" />
        <text style="margin-top: 16rpx;">加载中...</text>
      </view>
      <u-empty v-else-if="notifications.length === 0" text="暂无消息" icon="bell" mode="message" marginTop="80" />

      <view v-for="item in notifications" :key="item.id" class="notif-card">
        <view class="notif-card__header">
          <text class="notif-card__type">{{ item.type || '系统通知' }}</text>
          <text class="notif-card__time">{{ formatTime(item.createdAt) }}</text>
        </view>
        <text class="notif-card__title">{{ item.title }}</text>
        <text class="notif-card__content">{{ item.content }}</text>
      </view>

      <view style="height: 40rpx;" />
    </scroll-view>
  </view>
</template>

<style lang="scss" scoped>
@import '../../styles/design-system.scss';

.page { min-height: 100vh; background: #ffffff; overflow-x: hidden; width: 100%; }
.content { padding: 24rpx; height: calc(100vh - 100px); }

.state-tip {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12rpx;
  padding: 100rpx 0;
  color: $on-surface-variant;
  &__icon { font-size: 80rpx; }
}

.notif-card {
  background: $surface-container-lowest;
  border-radius: $radius-xl;
  padding: 24rpx;
  margin-bottom: 16rpx;
  box-shadow: $shadow-sm;

  &__header {
    display: flex;
    justify-content: space-between;
    margin-bottom: 12rpx;
  }

  &__type {
    font-size: $font-xs;
    color: $primary;
    font-weight: 600;
  }

  &__time {
    font-size: $font-xs;
    color: $on-surface-variant;
  }

  &__title {
    display: block;
    font-size: $font-base;
    font-weight: 600;
    color: $on-surface;
    margin-bottom: 8rpx;
  }

  &__content {
    display: block;
    font-size: $font-sm;
    color: $on-surface-variant;
    line-height: $leading-relaxed;
  }
}
</style>
