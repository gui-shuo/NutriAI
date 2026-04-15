<template>
  <view class="circle-page">
    <!-- Category Tabs -->
    <scroll-view scroll-x class="category-tabs">
      <view class="tabs-inner">
        <view
          v-for="cat in categories"
          :key="cat.key"
          :class="['tab-item', { active: currentCategory === cat.key }]"
          @tap="switchCategory(cat.key)"
        >
          <text>{{ cat.icon }} {{ cat.label }}</text>
        </view>
      </view>
    </scroll-view>

    <!-- Feed -->
    <scroll-view
      scroll-y
      class="feed-list"
      @scrolltolower="loadMore"
      refresher-enabled
      :refresher-triggered="refreshing"
      @refresherrefresh="onRefresh"
    >
      <view class="feed-content">
        <view v-for="post in posts" :key="post.id" class="post-card card" @tap="goDetail(post.id)">
          <!-- Author Info -->
          <view class="post-header">
            <view class="author-info">
              <image
                :src="post.authorAvatar || '/static/images/avatar-default.png'"
                class="avatar-sm"
                mode="aspectFill"
              />
              <view class="author-detail">
                <text class="author-name">{{ post.authorNickname || post.authorUsername }}</text>
                <text class="post-time text-xs text-muted">{{ formatTime(post.createdAt) }}</text>
              </view>
            </view>
            <view v-if="post.category" class="badge-primary">{{ post.category }}</view>
          </view>

          <!-- Content -->
          <text class="post-content">{{ post.content }}</text>

          <!-- Images -->
          <view v-if="post.images && post.images.length" class="post-images" :class="'grid-' + Math.min(post.images.length, 3)">
            <image
              v-for="(img, i) in post.images.slice(0, 9)"
              :key="i"
              :src="img"
              mode="aspectFill"
              class="post-img"
              @tap.stop="previewImages(post.images, i)"
            />
          </view>

          <!-- Actions -->
          <view class="post-actions">
            <view class="action-item" @tap.stop="toggleLike(post)">
              <text>{{ post.liked ? '❤️' : '🤍' }}</text>
              <text class="action-count">{{ post.likeCount || 0 }}</text>
            </view>
            <view class="action-item">
              <text>💬</text>
              <text class="action-count">{{ post.commentCount || 0 }}</text>
            </view>
            <view class="action-item" @tap.stop="sharePost(post)">
              <text>📤</text>
            </view>
          </view>
        </view>

        <view v-if="loading" class="loading-hint">
          <text class="text-muted">加载中...</text>
        </view>
        <view v-else-if="!posts.length" class="empty-state">
          <text class="empty-icon">💬</text>
          <text class="empty-text">营养圈暂无动态</text>
          <text class="text-sm text-muted mt-sm">快来发布第一条动态吧</text>
        </view>
        <view v-else-if="noMore" class="loading-hint">
          <text class="text-muted text-sm">— 没有更多了 —</text>
        </view>
      </view>
    </scroll-view>

    <!-- FAB: Create Post -->
    <view class="fab-btn" @tap="goCreate">
      <text class="fab-icon">✏️</text>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { communityApi, POST_CATEGORIES } from '../../services/api'
import { useUserStore } from '../../stores/user'
import { timeAgo } from '../../utils'

const userStore = useUserStore()
const currentCategory = ref('')
const posts = ref<any[]>([])
const loading = ref(false)
const refreshing = ref(false)
const noMore = ref(false)
const page = ref(0)

const categories = POST_CATEGORIES

onShow(() => {
  if (!posts.value.length) loadPosts(true)
})

async function loadPosts(reset = false) {
  if (loading.value) return
  if (reset) { page.value = 0; noMore.value = false }
  loading.value = true
  try {
    const data: any = await communityApi.getFeed({
      category: currentCategory.value || undefined,
      page: page.value,
      size: 10
    })
    const list = data?.content || data?.records || data || []
    // Parse images from JSON string if needed
    list.forEach((p: any) => {
      if (typeof p.images === 'string') {
        try { p.images = JSON.parse(p.images) } catch { p.images = [] }
      }
    })
    if (reset) { posts.value = list } else { posts.value = [...posts.value, ...list] }
    if (list.length < 10) noMore.value = true
    page.value++
  } catch {}
  loading.value = false
  refreshing.value = false
}

function switchCategory(key: string) {
  currentCategory.value = key
  loadPosts(true)
}

function onRefresh() { refreshing.value = true; loadPosts(true) }
function loadMore() { if (!noMore.value) loadPosts() }

function formatTime(dateStr: string) {
  return timeAgo(dateStr)
}

function goDetail(id: number) {
  uni.navigateTo({ url: `/pages/circle/detail?id=${id}` })
}

function goCreate() {
  if (!userStore.isLoggedIn) {
    uni.navigateTo({ url: '/pages/auth/login' })
    return
  }
  uni.navigateTo({ url: '/pages/circle/create' })
}

async function toggleLike(post: any) {
  if (!userStore.isLoggedIn) {
    uni.navigateTo({ url: '/pages/auth/login' })
    return
  }
  try {
    await communityApi.toggleLike({ targetId: post.id, targetType: 'POST' })
    post.liked = !post.liked
    post.likeCount = post.liked ? (post.likeCount || 0) + 1 : Math.max(0, (post.likeCount || 1) - 1)
  } catch {}
}

function previewImages(images: string[], index: number) {
  uni.previewImage({ urls: images, current: index })
}

function sharePost(post: any) {
  // WeChat share is handled by onShareAppMessage in page options
}
</script>

<style lang="scss" scoped>
.circle-page {
  min-height: 100vh;
  background: $bg-page;
  display: flex;
  flex-direction: column;
}

.category-tabs {
  background: $bg-card;
  white-space: nowrap;
  border-bottom: 1rpx solid $border-light;
  flex-shrink: 0;
}

.tabs-inner {
  display: inline-flex;
  padding: $spacing-sm $spacing-md;
  gap: $spacing-sm;
}

.tab-item {
  display: inline-flex;
  padding: 12rpx 24rpx;
  border-radius: $radius-full;
  font-size: $font-sm;
  color: $text-secondary;
  background: $bg-muted;
  flex-shrink: 0;
  white-space: nowrap;

  &.active { background: $primary; color: #fff; }
}

.feed-list {
  flex: 1;
  height: 0;
}

.feed-content {
  padding: $spacing-md $spacing-lg;
}

.post-card {
  padding: $spacing-lg;
  margin-bottom: $spacing-md;
}

.post-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.author-info {
  display: flex;
  align-items: center;
  gap: $spacing-sm;
}

.author-detail {
  display: flex;
  flex-direction: column;
}

.author-name {
  font-size: $font-sm;
  font-weight: 500;
  color: $text-primary;
}

.post-time {
  margin-top: 2rpx;
}

.post-content {
  display: block;
  margin-top: $spacing-md;
  font-size: $font-base;
  color: $text-primary;
  line-height: 1.7;
  word-break: break-all;
}

.post-images {
  display: grid;
  gap: 8rpx;
  margin-top: $spacing-md;
  border-radius: $radius-md;
  overflow: hidden;

  &.grid-1 { grid-template-columns: 1fr; }
  &.grid-2 { grid-template-columns: repeat(2, 1fr); }
  &.grid-3 { grid-template-columns: repeat(3, 1fr); }
}

.post-img {
  width: 100%;
  height: 0;
  padding-bottom: 100%;
  background: $bg-muted;
}

.post-actions {
  display: flex;
  gap: $spacing-xl;
  margin-top: $spacing-md;
  padding-top: $spacing-md;
  border-top: 1rpx solid $border-light;
}

.action-item {
  display: flex;
  align-items: center;
  gap: 8rpx;
}

.action-count {
  font-size: $font-sm;
  color: $text-muted;
}

// FAB
.fab-btn {
  position: fixed;
  right: 40rpx;
  bottom: 160rpx;
  width: 100rpx;
  height: 100rpx;
  border-radius: 50%;
  background: $gradient-primary;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: $shadow-primary;
  z-index: 100;
}

.fab-icon {
  font-size: 40rpx;
}

.loading-hint { text-align: center; padding: $spacing-xl 0; }
.empty-state {
  display: flex; flex-direction: column; align-items: center; padding: 120rpx 0;
}
.empty-icon { font-size: 80rpx; margin-bottom: $spacing-md; }
</style>
