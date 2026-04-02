<template>
  <view class="page">
    <!-- Pull to refresh handled by onPullDownRefresh -->
    <view v-if="loading && !announcements.length" class="loading-state">
      <text class="text-secondary">加载中...</text>
    </view>

    <view v-else-if="announcements.length" class="announcement-list">
      <view
        v-for="item in announcements"
        :key="item.id"
        class="announcement-card"
        :class="{ important: item.priority === 'HIGH' || item.priority === 'IMPORTANT' }"
        @tap="toggleExpand(item.id)"
      >
        <view class="announcement-header">
          <view class="title-row">
            <view
              v-if="item.priority === 'HIGH' || item.priority === 'IMPORTANT'"
              class="priority-badge"
            >
              重要
            </view>
            <text class="announcement-title">{{ item.title }}</text>
          </view>
          <text class="announcement-date text-sm text-secondary">
            {{ formatTime(item.publishedAt || item.createdAt) }}
          </text>
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
import { ref, onMounted } from 'vue'
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

function truncateText(text: string, max: number) {
  if (!text || text.length <= max) return text
  return text.substring(0, max) + '...'
}

function toggleExpand(id: number) {
  expandedId.value = expandedId.value === id ? null : id
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
  background: #fdfbf7;
  padding: 20rpx 0 30rpx;
  font-family: 'Patrick Hand', cursive;
}

.loading-state {
  text-align: center;
  padding: 100rpx 0;
  color: #2d2d2d;
  opacity: 0.6;
}

.announcement-list {
  padding: 0 24rpx;
}

.announcement-card {
  background: #fdfbf7;
  border-radius: 255px 15px 225px 15px / 15px 225px 15px 255px;
  padding: 28rpx;
  margin-bottom: 24rpx;
  border: 2rpx solid #2d2d2d;
  box-shadow: 4px 4px 0px 0px #2d2d2d;
  position: relative;
  transition: all 0.2s;

  /* Tack decoration */
  &::before {
    content: '\1F4CC';
    position: absolute;
    top: -14rpx;
    right: 30rpx;
    font-size: 32rpx;
    z-index: 1;
  }

  &.important {
    border-left: 6rpx solid #ff4d4d;

    &::before {
      content: '\1F4CD';
    }
  }
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
  background: #ff4d4d;
  color: #fdfbf7;
  font-size: 20rpx;
  padding: 4rpx 12rpx;
  border-radius: 8px 18px 8px 18px / 18px 8px 18px 8px;
  white-space: nowrap;
  font-weight: 600;
  border: 1rpx solid #2d2d2d;
}

.announcement-title {
  font-size: 30rpx;
  font-weight: 700;
  color: #2d2d2d;
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-family: 'Kalam', cursive;
}

.announcement-date {
  display: block;
  color: #2d2d2d;
  opacity: 0.5;
  font-size: 24rpx;
}

.announcement-preview {
  font-size: 26rpx;
  color: #2d2d2d;
  opacity: 0.7;
  line-height: 1.7;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  font-family: 'Patrick Hand', cursive;
}

.announcement-content {
  font-size: 26rpx;
  color: #2d2d2d;
  line-height: 1.8;
  word-break: break-all;
  font-family: 'Patrick Hand', cursive;
}

.expand-hint {
  margin-top: 12rpx;
  text-align: right;
  color: #2d5da1;
  font-size: 24rpx;
  text-decoration: underline;
  text-decoration-style: wavy;
  font-family: 'Patrick Hand', cursive;
}

.load-more {
  text-align: center;
  padding: 30rpx 0;
  color: #2d2d2d;
  opacity: 0.5;
  font-size: 24rpx;
  font-family: 'Patrick Hand', cursive;
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
  color: #2d2d2d;
  opacity: 0.6;
  font-family: 'Patrick Hand', cursive;
}

.safe-bottom {
  height: env(safe-area-inset-bottom);
}
</style>