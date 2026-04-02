<template>
  <view class="community-page">
    <!-- Disclaimer -->
    <view class="disclaimer-tip" v-if="showDisclaimer">
      <text>💬 社区内容为用户个人观点，不代表平台立场，请自行甄别信息的准确性。</text>
      <text class="dismiss" @tap="showDisclaimer = false">✕</text>
    </view>

    <!-- Category Filter Bar -->
    <scroll-view class="category-bar" scroll-x :show-scrollbar="false">
      <view
        v-for="cat in categoryList"
        :key="cat.value"
        class="category-item"
        :class="{ active: currentCategory === cat.value }"
        @tap="switchCategory(cat.value)"
      >
        {{ cat.label }}
      </view>
    </scroll-view>

    <!-- Post Feed -->
    <view class="feed-list" v-if="posts.length > 0">
      <view class="post-card" v-for="post in posts" :key="post.id" @tap="goDetail(post.id)">
        <!-- Author Info -->
        <view class="post-header">
          <image class="avatar" :src="defaultAvatar(post.avatarUrl)" mode="aspectFill" />
          <view class="author-info">
            <text class="nickname">{{ post.username }}</text>
            <text class="time">{{ formatTime(post.createdAt) }}</text>
          </view>
          <view class="category-tag" v-if="post.category">{{ post.category }}</view>
        </view>

        <!-- Content -->
        <view class="post-content">
          <text class="content-text">{{ truncate(post.content, 120) }}</text>
        </view>

        <!-- Image Grid -->
        <view class="image-grid" v-if="getPostImages(post).length > 0">
          <view
            class="grid-item"
            v-for="(img, idx) in getPostImages(post).slice(0, getMaxImages(getPostImages(post).length))"
            :key="idx"
          >
            <image class="grid-img" :src="img" mode="aspectFill" />
            <view
              class="more-badge"
              v-if="idx === getMaxImages(getPostImages(post).length) - 1 && getPostImages(post).length > getMaxImages(getPostImages(post).length)"
            >
              +{{ getPostImages(post).length - getMaxImages(getPostImages(post).length) }}
            </view>
          </view>
        </view>

        <!-- Video Thumbnail -->
        <view class="video-thumb" v-if="post.videoUrl && getPostImages(post).length === 0">
          <image class="video-cover" :src="post.videoUrl + '?x-oss-process=video/snapshot,t_1000'" mode="aspectFill" />
          <view class="play-icon">▶</view>
        </view>

        <!-- Bottom Bar -->
        <view class="post-footer">
          <view class="footer-item">
            <text class="icon">❤️</text>
            <text class="count">{{ post.likesCount || 0 }}</text>
          </view>
          <view class="footer-item">
            <text class="icon">💬</text>
            <text class="count">{{ post.commentsCount || 0 }}</text>
          </view>
        </view>
      </view>
    </view>

    <!-- Empty State -->
    <view class="empty-state" v-else-if="!loading">
      <text class="empty-icon">📭</text>
      <text class="empty-text">暂无帖子，快来发布第一条吧</text>
    </view>

    <!-- Loading -->
    <view class="loading-more" v-if="loading">
      <text>加载中...</text>
    </view>
    <view class="no-more" v-if="!loading && noMore && posts.length > 0">
      <text>— 没有更多了 —</text>
    </view>

    <!-- Floating Create Button -->
    <view class="fab-btn" @tap="goCreate">
      <text class="fab-icon">+</text>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { onPullDownRefresh, onReachBottom, onShow } from '@dcloudio/uni-app'
import { communityApi, PostCategories } from '@/services/api'
import { checkLogin, formatTime, defaultAvatar, truncate } from '@/utils/common'

const posts = ref<any[]>([])
const loading = ref(false)
const noMore = ref(false)
const page = ref(0)
const pageSize = 10
const currentCategory = ref('')
const showDisclaimer = ref(true)

const categoryList = computed(() => [
  { value: '', label: '全部' },
  ...PostCategories
])

function getMaxImages(total: number): number {
  return total === 1 ? 1 : total <= 4 ? total : 3
}

function getPostImages(post: any): string[] {
  if (Array.isArray(post.images)) return post.images
  if (typeof post.images === 'string' && post.images) {
    try { return JSON.parse(post.images) } catch { return [] }
  }
  if (Array.isArray(post.imageUrls)) return post.imageUrls
  return []
}

function switchCategory(value: string) {
  if (currentCategory.value === value) return
  currentCategory.value = value
  refreshData()
}

async function loadPosts(isRefresh = false) {
  if (loading.value) return
  loading.value = true
  try {
    const params: any = { page: page.value, size: pageSize }
    if (currentCategory.value) params.category = currentCategory.value
    const res = await communityApi.getFeed(params)
    const list = res.data?.content || res.data?.records || res.data?.list || res.data || []
    if (isRefresh) {
      posts.value = list
    } else {
      posts.value = [...posts.value, ...list]
    }
    noMore.value = list.length < pageSize
  } catch (e: any) {
    uni.showToast({ title: '加载失败', icon: 'none' })
  } finally {
    loading.value = false
  }
}

function refreshData() {
  page.value = 0
  noMore.value = false
  loadPosts(true)
}

function goDetail(id: number) {
  uni.navigateTo({ url: `/pages/community/detail?postId=${id}` })
}

function goCreate() {
  if (!checkLogin()) return
  uni.navigateTo({ url: '/pages/community/create' })
}

onShow(() => {
  refreshData()
})

onPullDownRefresh(() => {
  page.value = 0
  noMore.value = false
  loadPosts(true).then(() => uni.stopPullDownRefresh())
})

onReachBottom(() => {
  if (noMore.value || loading.value) return
  page.value++
  loadPosts()
})
</script>

<style lang="scss" scoped>
.community-page {
  min-height: 100vh;
  background: $background;
  padding-bottom: 120rpx;
}

.category-bar {
  white-space: nowrap;
  background: $card;
  padding: 20rpx 16rpx;
  position: sticky;
  top: 0;
  z-index: 10;
  border-bottom: 1rpx solid $border;
}

.category-item {
  display: inline-block;
  padding: 12rpx 28rpx;
  margin: 0 10rpx;
  border-radius: $radius-full;
  font-size: 26rpx;
  color: $foreground;
  background: $card;
  border: 1rpx solid $border;
  font-family: 'Inter', 'PingFang SC', sans-serif;
  transition: all 0.2s;
}

.category-item.active {
  background: $accent;
  color: $accent-foreground;
  border-color: $accent;
  box-shadow: $shadow-accent;
}

.feed-list {
  padding: 16rpx;
}

.post-card {
  background: $card;
  border: 1rpx solid $border;
  border-radius: $radius-xl;
  padding: 28rpx;
  margin-bottom: 20rpx;
  box-shadow: $shadow-sm;
  position: relative;
}

.post-header {
  display: flex;
  align-items: center;
  margin-bottom: 20rpx;
}

.avatar {
  width: 72rpx;
  height: 72rpx;
  border-radius: $radius-full;
  border: none;
  margin-right: 16rpx;
  flex-shrink: 0;
}

.author-info {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.nickname {
  font-size: 28rpx;
  font-weight: 600;
  color: $foreground;
  font-family: 'Inter', 'PingFang SC', sans-serif;
}

.time {
  font-size: 22rpx;
  color: $muted-foreground;
  margin-top: 4rpx;
  font-family: 'Inter', 'PingFang SC', sans-serif;
}

.category-tag {
  font-size: 22rpx;
  color: $accent;
  background: rgba(16, 185, 129, 0.06);
  padding: 6rpx 16rpx;
  border-radius: $radius-full;
  border: none;
  flex-shrink: 0;
  font-family: 'JetBrains Mono', 'PingFang SC', monospace;
}

.post-content {
  margin-bottom: 20rpx;
}

.content-text {
  font-size: 28rpx;
  color: $foreground;
  line-height: 1.6;
  font-family: 'Inter', 'PingFang SC', sans-serif;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.image-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 8rpx;
  margin-bottom: 20rpx;
}

.grid-item {
  position: relative;
  width: calc(33.33% - 6rpx);
  aspect-ratio: 1;
  border-radius: $radius-lg;
  overflow: hidden;
  border: none;
}

.grid-img {
  width: 100%;
  height: 100%;
}

.more-badge {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(15, 23, 42, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 36rpx;
  font-weight: 600;
  font-family: 'Inter', 'PingFang SC', sans-serif;
}

.video-thumb {
  position: relative;
  width: 100%;
  height: 360rpx;
  border-radius: $radius-lg;
  overflow: hidden;
  margin-bottom: 20rpx;
  border: none;
}

.video-cover {
  width: 100%;
  height: 100%;
}

.play-icon {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 80rpx;
  height: 80rpx;
  border-radius: $radius-full;
  background: rgba(15, 23, 42, 0.6);
  backdrop-filter: blur(4px);
  color: #fff;
  font-size: 36rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}

.post-footer {
  display: flex;
  gap: 40rpx;
  padding-top: 16rpx;
  border-top: 1rpx solid $border;
}

.footer-item {
  display: flex;
  align-items: center;
  gap: 8rpx;
}

.footer-item .icon {
  font-size: 28rpx;
}

.footer-item .count {
  font-size: 24rpx;
  color: $muted-foreground;
  font-family: 'Inter', 'PingFang SC', sans-serif;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 200rpx 0;
}

.empty-icon {
  font-size: 80rpx;
  margin-bottom: 20rpx;
}

.empty-text {
  font-size: 28rpx;
  color: $muted-foreground;
  font-family: 'Inter', 'PingFang SC', sans-serif;
}

.loading-more,
.no-more {
  text-align: center;
  padding: 30rpx;
  font-size: 24rpx;
  color: $muted-foreground;
  font-family: 'Inter', 'PingFang SC', sans-serif;
}

.fab-btn {
  position: fixed;
  right: 40rpx;
  bottom: 160rpx;
  width: 100rpx;
  height: 100rpx;
  border-radius: $radius-full;
  background: $gradient-accent;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: $shadow-accent;
  z-index: 100;
  transition: transform 0.15s ease;
}
.fab-btn:active {
  transform: scale(0.95);
}

.fab-icon {
  font-size: 52rpx;
  color: #fff;
  line-height: 1;
  font-family: 'Inter', 'PingFang SC', sans-serif;
}

.disclaimer-tip {
  background: $muted;
  color: $foreground;
  border: 1rpx solid $border;
  border-radius: $radius-lg;
  padding: 14rpx 48rpx 14rpx 20rpx;
  font-size: 22rpx;
  margin: 16rpx 20rpx;
  position: relative;
  font-family: 'Inter', 'PingFang SC', sans-serif;
}

.disclaimer-tip .dismiss {
  position: absolute;
  right: 16rpx;
  top: 50%;
  transform: translateY(-50%);
  font-size: 28rpx;
  color: $muted-foreground;
}
</style>
