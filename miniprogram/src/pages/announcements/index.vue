<template>
  <view class="page">
    <!-- Mark all read button -->
    <view v-if="hasUnread" class="mark-all-bar" @tap="markAllRead">
      <text class="mark-all-text">全部标记已读</text>
    </view>

    <view v-if="loading && !announcements.length" class="loading-state">
      <text class="text-secondary">加载中...</text>
    </view>

    <view v-else-if="announcements.length" class="announcement-list">
      <view
        v-for="item in announcements"
        :key="item.id"
        class="announcement-card"
        :class="{ important: item.priority === 'HIGH' || item.priority === 'IMPORTANT', unread: !item.isRead }"
        @tap="toggleExpand(item)"
      >
        <view class="announcement-header">
          <view class="title-row">
            <view v-if="!item.isRead" class="unread-dot" />
            <view
              v-if="item.priority === 'HIGH' || item.priority === 'IMPORTANT'"
              class="priority-badge"
            >
              重要
            </view>
            <text class="announcement-title">{{ item.title }}</text>
          </view>
          <view class="date-row">
            <text class="announcement-date text-sm text-secondary">
              {{ formatTime(item.publishedAt || item.createdAt) }}
            </text>
            <text v-if="item.isRead" class="read-tag">已读</text>
          </view>
        </view>

        <!-- Preview (collapsed) -->
        <view v-if="expandedId !== item.id" class="announcement-preview">
          {{ truncateText(item.content, 80) }}
        </view>

        <!-- Full content (expanded) -->
        <view v-else class="announcement-content">
          {{ item.content }}
        </view>

        <view class="expand-hint text-sm text-secondary">
          {{ expandedId === item.id ? '收起' : '展开查看' }}
        </view>
      </view>

      <!-- Load More -->
      <view v-if="hasMore" class="load-more" @tap="loadMore">
        <text class="text-secondary text-sm">{{ loadingMore ? '加载中...' : '点击加载更多' }}</text>
      </view>

      <view v-else class="load-more">
        <text class="text-secondary text-sm">没有更多了</text>
      </view>
    </view>

    <view v-else class="empty-state">
      <text class="empty-icon">📢</text>
      <text class="empty-text">暂无公告</text>
    </view>

    <view class="safe-bottom"></view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { onPullDownRefresh, onReachBottom } from '@dcloudio/uni-app'
import { announcementApi } from '@/services/api'
import { formatTime } from '@/utils/common'

const announcements = ref<any[]>([])
const loading = ref(false)
const loadingMore = ref(false)
const page = ref(1)
const pageSize = 10
const hasMore = ref(true)
const expandedId = ref<number | null>(null)

const hasUnread = computed(() => announcements.value.some(a => !a.isRead))

function truncateText(text: string, max: number) {
  if (!text || text.length <= max) return text
  return text.substring(0, max) + '...'
}

function toggleExpand(item: any) {
  if (expandedId.value === item.id) {
    expandedId.value = null
  } else {
    expandedId.value = item.id
    // 展开时自动标记已读
    if (!item.isRead) {
      markRead(item)
    }
  }
}

async function markRead(item: any) {
  try {
    await announcementApi.markRead(item.id)
    item.isRead = true
  } catch {}
}

async function markAllRead() {
  try {
    await announcementApi.markAllRead()
    announcements.value.forEach(a => { a.isRead = true })
    uni.showToast({ title: '已全部标记为已读', icon: 'success' })
  } catch {}
}

async function loadAnnouncements(isRefresh = false) {
  if (isRefresh) {
    page.value = 1
    hasMore.value = true
  }
  if (!hasMore.value && !isRefresh) return

  loading.value = page.value === 1
  loadingMore.value = page.value > 1

  try {
    const res = await announcementApi.getList({ page: page.value, size: pageSize })
    if (res.code === 200) {
      const list = res.data?.content || res.data?.records || res.data?.list || res.data || []
      if (isRefresh) {
        announcements.value = list
      } else {
        announcements.value = [...announcements.value, ...list]
      }
      hasMore.value = list.length >= pageSize
    }
  } catch {} finally {
    loading.value = false
    loadingMore.value = false
  }
}

function loadMore() {
  if (loadingMore.value || !hasMore.value) return
  page.value++
  loadAnnouncements()
}

onPullDownRefresh(async () => {
  await loadAnnouncements(true)
  uni.stopPullDownRefresh()
})

onReachBottom(() => {
  loadMore()
})

onMounted(() => {
  loadAnnouncements()
})
</script>

<style scoped lang="scss">
.page {
  min-height: 100vh;
  background: $background;
  padding: 20rpx 0 30rpx;
  font-family: 'Inter', sans-serif;
}

.loading-state {
  text-align: center;
  padding: 100rpx 0;
  color: $muted-foreground;
}

.announcement-list {
  padding: 0 24rpx;
}

.announcement-card {
  background: $card;
  border-radius: $radius-xl;
  padding: 28rpx;
  margin-bottom: 24rpx;
  border: 1rpx solid $border;
  box-shadow: $shadow-sm;
  position: relative;
  transition: all 0.2s ease;

  &.important {
    border-left: 6rpx solid $accent;
  }

  &.unread {
    border-left: 6rpx solid $accent;
    background: rgba(16, 185, 129, 0.04);
  }
}

.unread-dot {
  width: 14rpx;
  height: 14rpx;
  border-radius: 50%;
  background: $accent;
  flex-shrink: 0;
}

.date-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.read-tag {
  font-size: 22rpx;
  color: $muted-foreground;
}

.mark-all-bar {
  text-align: center;
  padding: 16rpx 24rpx;
  margin: 0 24rpx 16rpx;
  background: $card;
  border-radius: $radius-lg;
  border: 1rpx solid $border;
}

.mark-all-text {
  color: $accent;
  font-size: 26rpx;
  font-weight: 600;
}

.announcement-header {
  margin-bottom: 16rpx;
}

.title-row {
  display: flex;
  align-items: center;
  gap: 12rpx;
  margin-bottom: 8rpx;
}

.priority-badge {
  background: $accent;
  color: $accent-foreground;
  font-size: 20rpx;
  padding: 4rpx 12rpx;
  border-radius: $radius-full;
  white-space: nowrap;
  font-weight: 600;
  font-family: 'JetBrains Mono', monospace;
}

.announcement-title {
  font-size: 30rpx;
  font-weight: 700;
  color: $foreground;
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-family: 'Calistoga', cursive;
}

.announcement-date {
  display: block;
  color: $muted-foreground;
  font-size: 24rpx;
}

.announcement-preview {
  font-size: 26rpx;
  color: $muted-foreground;
  line-height: 1.7;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  font-family: 'Inter', sans-serif;
}

.announcement-content {
  font-size: 26rpx;
  color: $foreground;
  line-height: 1.8;
  word-break: break-all;
  font-family: 'Inter', sans-serif;
}

.expand-hint {
  margin-top: 12rpx;
  text-align: right;
  color: $accent;
  font-size: 24rpx;
  font-family: 'Inter', sans-serif;
}

.load-more {
  text-align: center;
  padding: 30rpx 0;
  color: $muted-foreground;
  font-size: 24rpx;
  font-family: 'Inter', sans-serif;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding-top: 200rpx;
}

.empty-icon {
  font-size: 80rpx;
  margin-bottom: 20rpx;
}

.empty-text {
  font-size: 28rpx;
  color: $muted-foreground;
  font-family: 'Inter', sans-serif;
}

.safe-bottom {
  height: env(safe-area-inset-bottom);
}
</style>