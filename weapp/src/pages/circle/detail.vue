<template>
  <view class="detail-page">
    <scroll-view scroll-y class="page-scroll">
      <!-- Post Content -->
      <view class="post-section card">
        <view class="post-header">
          <image :src="post.authorAvatar || '/static/images/avatar-default.png'" class="avatar" mode="aspectFill" />
          <view class="author-detail">
            <text class="author-name text-bold">{{ post.authorNickname || post.authorUsername }}</text>
            <text class="text-xs text-muted">{{ formatTime(post.createdAt) }}</text>
          </view>
          <view v-if="post.category" class="badge-primary">{{ post.category }}</view>
        </view>

        <text class="post-content">{{ post.content }}</text>

        <view v-if="postImages.length" class="post-images" :class="'grid-' + Math.min(postImages.length, 3)">
          <image
            v-for="(img, i) in postImages"
            :key="i"
            :src="img"
            mode="aspectFill"
            class="post-img"
            @tap="previewImages(i)"
          />
        </view>

        <view class="post-stats">
          <view class="stat-item" @tap="toggleLike">
            <text>{{ post.liked ? '❤️' : '🤍' }}</text>
            <text class="text-sm text-muted">{{ post.likeCount || 0 }}</text>
          </view>
          <text class="text-sm text-muted">{{ post.commentCount || 0 }} 条评论</text>
        </view>
      </view>

      <!-- Comments -->
      <view class="comments-section">
        <text class="section-title px-lg">评论</text>

        <view v-for="comment in comments" :key="comment.id" class="comment-item">
          <image :src="comment.authorAvatar || '/static/images/avatar-default.png'" class="avatar-sm" mode="aspectFill" />
          <view class="comment-body">
            <view class="comment-top">
              <text class="text-sm text-bold">{{ comment.authorNickname || comment.authorUsername }}</text>
              <text class="text-xs text-muted">{{ formatTime(comment.createdAt) }}</text>
            </view>
            <text class="comment-text">{{ comment.content }}</text>
          </view>
        </view>

        <view v-if="!comments.length" class="empty-hint">
          <text class="text-muted text-sm">暂无评论，快来说点什么吧</text>
        </view>
      </view>

      <view style="height: 120rpx;"></view>
    </scroll-view>

    <!-- Comment Input -->
    <view class="comment-bar safe-bottom">
      <input
        v-model="commentText"
        placeholder="写评论..."
        class="comment-input"
        confirm-type="send"
        @confirm="sendComment"
      />
      <button class="send-btn" :disabled="!commentText.trim()" @tap="sendComment">发送</button>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { communityApi } from '../../services/api'
import { useUserStore } from '../../stores/user'
import { timeAgo } from '../../utils'

const userStore = useUserStore()
const post = ref<any>({})
const comments = ref<any[]>([])
const commentText = ref('')
const postId = ref(0)

const postImages = computed(() => {
  let imgs = post.value.images || []
  if (typeof imgs === 'string') {
    try { imgs = JSON.parse(imgs) } catch { imgs = [] }
  }
  return imgs
})

onLoad((query: any) => {
  if (query?.id) {
    postId.value = Number(query.id)
    loadData()
  }
})

async function loadData() {
  uni.showLoading({ title: '加载中' })
  try {
    post.value = await communityApi.getPost(postId.value) || {}
  } catch {}
  try {
    const data: any = await communityApi.getComments(postId.value, { size: 50 })
    comments.value = data?.content || data?.records || data || []
  } catch {}
  uni.hideLoading()
}

function formatTime(dateStr: string) { return timeAgo(dateStr) }

function previewImages(index: number) {
  uni.previewImage({ urls: postImages.value, current: index })
}

async function toggleLike() {
  if (!userStore.isLoggedIn) { uni.navigateTo({ url: '/pages/auth/login' }); return }
  try {
    await communityApi.toggleLike({ targetId: post.value.id, targetType: 'POST' })
    post.value.liked = !post.value.liked
    post.value.likeCount = post.value.liked ? (post.value.likeCount || 0) + 1 : Math.max(0, (post.value.likeCount || 1) - 1)
  } catch {}
}

async function sendComment() {
  if (!userStore.isLoggedIn) { uni.navigateTo({ url: '/pages/auth/login' }); return }
  const content = commentText.value.trim()
  if (!content) return

  try {
    await communityApi.addComment(postId.value, { content })
    commentText.value = ''
    uni.showToast({ title: '评论成功', icon: 'success' })
    loadData()
  } catch (e: any) {
    uni.showToast({ title: e?.message || '评论失败', icon: 'none' })
  }
}
</script>

<style lang="scss" scoped>
.detail-page { min-height: 100vh; background: $bg-page; }
.page-scroll { height: 100vh; }

.post-section { margin: $spacing-md $spacing-lg; padding: $spacing-lg; }
.post-header { display: flex; align-items: center; gap: $spacing-md; margin-bottom: $spacing-md; }
.author-detail { flex: 1; }
.author-name { font-size: $font-base; }
.post-content { display: block; font-size: $font-md; color: $text-primary; line-height: 1.8; }

.post-images {
  display: grid; gap: 8rpx; margin-top: $spacing-md; border-radius: $radius-md; overflow: hidden;
  &.grid-1 { grid-template-columns: 1fr; }
  &.grid-2 { grid-template-columns: repeat(2, 1fr); }
  &.grid-3 { grid-template-columns: repeat(3, 1fr); }
}
.post-img { width: 100%; height: 0; padding-bottom: 100%; background: $bg-muted; }

.post-stats {
  display: flex; align-items: center; justify-content: space-between;
  margin-top: $spacing-md; padding-top: $spacing-md; border-top: 1rpx solid $border-light;
}

.stat-item { display: flex; align-items: center; gap: 8rpx; }

.comments-section { padding: $spacing-md 0; }
.px-lg { padding-left: $spacing-lg; padding-right: $spacing-lg; }

.comment-item {
  display: flex; gap: $spacing-md; padding: $spacing-md $spacing-lg;
  border-bottom: 1rpx solid $border-light;
}
.comment-body { flex: 1; }
.comment-top { display: flex; align-items: center; justify-content: space-between; }
.comment-text { display: block; margin-top: 8rpx; font-size: $font-base; color: $text-primary; line-height: 1.6; }

.empty-hint { text-align: center; padding: $spacing-xl; }

.comment-bar {
  position: fixed; bottom: 0; left: 0; right: 0;
  background: $bg-card; box-shadow: 0 -2rpx 10rpx rgba(0, 0, 0, 0.05);
  display: flex; align-items: center; padding: $spacing-sm $spacing-lg; gap: $spacing-md; z-index: 100;
}

.comment-input {
  flex: 1; height: 72rpx; background: $bg-muted; border-radius: $radius-full;
  padding: 0 $spacing-md; font-size: $font-base;
}

.send-btn {
  height: 72rpx; padding: 0 32rpx; background: $gradient-primary; color: #fff;
  border-radius: $radius-full; font-size: $font-sm; border: none; line-height: 72rpx;
  &::after { border: none; }
  &[disabled] { opacity: 0.4; }
}
</style>
