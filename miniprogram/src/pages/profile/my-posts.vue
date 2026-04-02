<template>
  <view class="page">
    <view v-if="loading && !posts.length" class="loading-state">
      <text>加载中...</text>
    </view>

    <view v-else-if="!posts.length" class="empty-state">
      <text class="empty-icon">📝</text>
      <text class="empty-text">暂无帖子</text>
      <button class="btn-primary btn-create" @tap="goCreate">去发布</button>
    </view>

    <view v-else class="post-list">
      <view
        v-for="post in posts"
        :key="post.id"
        class="post-card"
        @tap="goDetail(post.id)"
      >
        <view class="post-header flex-between">
          <text class="post-category">{{ post.category || '动态' }}</text>
          <text class="post-time text-sm text-secondary">{{ formatTime(post.createdAt) }}</text>
        </view>
        <text class="post-title" v-if="post.title">{{ post.title }}</text>
        <text class="post-content">{{ truncate(post.content, 100) }}</text>
        <view v-if="post.images?.length" class="post-images">
          <image
            v-for="(img, idx) in post.images.slice(0, 3)"
            :key="idx"
            :src="img"
            class="post-img"
            mode="aspectFill"
          />
          <view v-if="post.images.length > 3" class="more-images">
            <text>+{{ post.images.length - 3 }}</text>
          </view>
        </view>
        <view class="post-stats flex">
          <text class="stat-item">👍 {{ post.likeCount || 0 }}</text>
          <text class="stat-item">💬 {{ post.commentCount || 0 }}</text>
          <text class="stat-item">👁️ {{ post.viewCount || 0 }}</text>
        </view>
        <view class="post-actions">
          <text class="delete-btn" @tap.stop="deletePost(post.id)">删除</text>
        </view>
      </view>
    </view>

    <view v-if="!loading && noMore && posts.length" class="no-more">
      <text class="text-sm text-secondary">没有更多了</text>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { onShow, onReachBottom } from '@dcloudio/uni-app'
import { communityApi } from '@/services/api'
import { useUserStore } from '@/stores/user'
import { checkLogin, formatTime, truncate } from '@/utils/common'

const userStore = useUserStore()
const posts = ref<any[]>([])
const loading = ref(false)
const page = ref(1)
const noMore = ref(false)

async function loadPosts(reset = false) {
  if (!userStore.userInfo?.id) return
  if (loading.value) return
  if (reset) {
    page.value = 1
    noMore.value = false
  }
  loading.value = true
  try {
    const res = await communityApi.getUserPosts(userStore.userInfo.id, { page: page.value, size: 10 })
    if (res.code === 200) {
      const list = res.data?.content || res.data?.records || res.data?.list || res.data || []
      if (reset) {
        posts.value = list
      } else {
        posts.value.push(...list)
      }
      if (list.length < 10) noMore.value = true
      else page.value++
    }
  } catch {} finally {
    loading.value = false
  }
}

async function deletePost(id: number) {
  uni.showModal({
    title: '提示',
    content: '确定删除这篇帖子吗？',
    success: async (res) => {
      if (!res.confirm) return
      try {
        await communityApi.deletePost(id)
        uni.showToast({ title: '已删除', icon: 'success' })
        posts.value = posts.value.filter(p => p.id !== id)
      } catch {
        uni.showToast({ title: '删除失败', icon: 'none' })
      }
    }
  })
}

function goDetail(id: number) {
  uni.navigateTo({ url: `/pages/community/detail?id=${id}` })
}

function goCreate() {
  uni.navigateTo({ url: '/pages/community/create' })
}

onShow(() => {
  if (checkLogin()) loadPosts(true)
})

onReachBottom(() => {
  if (!noMore.value) loadPosts()
})
</script>

<style scoped lang="scss">
.page {
  min-height: 100vh;
  background: $background;
  padding: 20rpx 24rpx;
  font-family: 'Inter', sans-serif;
}

.loading-state, .empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 120rpx 0;
}

.empty-icon { font-size: 80rpx; margin-bottom: 20rpx; }
.empty-text { font-size: 28rpx; color: $muted-foreground; margin-bottom: 30rpx; }

.btn-create {
  width: 240rpx;
  height: 72rpx;
  font-size: 28rpx;
  border-radius: $radius-full;
  background: $accent;
  color: #fff;
  border: none;
  box-shadow: $shadow-accent;
  font-family: 'Inter', sans-serif;
}

.post-card {
  background: $card;
  border-radius: $radius-xl;
  padding: 28rpx;
  margin-bottom: 24rpx;
  border: 1rpx solid $border;
  box-shadow: $shadow-sm;
  position: relative;
}

.post-header { margin-bottom: 12rpx; }

.post-category {
  font-size: 22rpx;
  color: $accent;
  background: rgba(0, 82, 255, 0.06);
  padding: 4rpx 16rpx;
  border-radius: $radius-full;
  font-family: 'JetBrains Mono', monospace;
}

.post-time { font-size: 24rpx; color: $muted-foreground; }

.post-title {
  font-size: 32rpx;
  font-weight: 600;
  color: $foreground;
  display: block;
  margin-bottom: 10rpx;
  font-family: 'Calistoga', cursive;
}

.post-content {
  font-size: 28rpx;
  color: $muted-foreground;
  line-height: 1.6;
  display: block;
  margin-bottom: 16rpx;
}

.post-images {
  display: flex;
  gap: 12rpx;
  margin-bottom: 16rpx;
}

.post-img {
  width: 200rpx;
  height: 200rpx;
  border-radius: $radius-lg;
  background: $muted;
}

.more-images {
  width: 200rpx;
  height: 200rpx;
  border-radius: $radius-lg;
  background: $muted;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28rpx;
  color: $muted-foreground;
  font-family: 'Inter', sans-serif;
}

.post-stats {
  gap: 30rpx;
  margin-bottom: 12rpx;
}

.stat-item { font-size: 24rpx; color: $muted-foreground; }

.post-actions { text-align: right; }

.delete-btn {
  font-size: 24rpx;
  color: $uni-error;
  padding: 8rpx 16rpx;
}

.no-more {
  text-align: center;
  padding: 30rpx 0;
  color: $muted-foreground;
}
</style>
