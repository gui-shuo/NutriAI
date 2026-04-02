<template>
  <view class="detail-page" v-if="post">
    <!-- Author Info Bar -->
    <view class="author-bar">
      <image class="avatar" :src="defaultAvatar(post.avatarUrl)" mode="aspectFill" />
      <view class="author-info">
        <text class="nickname">{{ post.username }}</text>
        <text class="time">{{ formatTime(post.createdAt) }}</text>
      </view>
      <view
        class="follow-btn"
        :class="{ followed: isFollowed }"
        v-if="!isAuthor"
        @tap="handleFollow"
      >
        {{ isFollowed ? '已关注' : '+ 关注' }}
      </view>
      <view class="delete-btn" v-if="isAuthor" @tap="handleDelete">
        <text>删除</text>
      </view>
    </view>

    <!-- Post Content -->
    <view class="post-body">
      <view class="category-tag" v-if="post.category">{{ post.category }}</view>
      <text class="content-text">{{ post.content }}</text>
    </view>

    <!-- Image Gallery -->
    <view class="image-gallery" v-if="postImages.length > 0">
      <image
        v-for="(img, idx) in postImages"
        :key="idx"
        class="gallery-img"
        :class="{ 'single-img': postImages.length === 1 }"
        :src="img"
        mode="aspectFill"
        @tap="previewImage(idx)"
      />
    </view>

    <!-- Video Player -->
    <view class="video-wrapper" v-if="post.videoUrl">
      <video
        :src="post.videoUrl"
        class="post-video"
        controls
        object-fit="contain"
      />
    </view>

    <!-- Stats Bar -->
    <view class="stats-bar">
      <view class="stat-item" :class="{ liked: isLiked }" @tap="handleLike">
        <text class="stat-icon">{{ isLiked ? '❤️' : '🤍' }}</text>
        <text class="stat-count">{{ post.likesCount || 0 }}</text>
      </view>
      <view class="stat-item">
        <text class="stat-icon">💬</text>
        <text class="stat-count">{{ post.commentsCount || 0 }}</text>
      </view>
    </view>

    <!-- Comments Section -->
    <view class="comments-section">
      <view class="section-title">评论 ({{ post.commentsCount || 0 }})</view>

      <view class="comment-list" v-if="comments.length > 0">
        <view class="comment-item" v-for="comment in comments" :key="comment.id">
          <image class="comment-avatar" :src="defaultAvatar(comment.avatarUrl)" mode="aspectFill" />
          <view class="comment-body">
            <view class="comment-header">
              <text class="comment-nick">{{ comment.username }}</text>
              <text class="comment-time">{{ formatTime(comment.createdAt) }}</text>
            </view>
            <view class="reply-tag" v-if="comment.replyToUsername">
              回复 <text class="reply-nick">@{{ comment.replyToUsername }}</text>
            </view>
            <text class="comment-content">{{ comment.content }}</text>
            <view class="comment-actions">
              <text class="action-btn" @tap="setReply(comment)">回复</text>
              <text
                class="action-btn delete"
                v-if="comment.userId === userStore.userInfo?.id"
                @tap="deleteComment(comment.id)"
              >删除</text>
            </view>
          </view>
        </view>
      </view>

      <view class="empty-comments" v-else-if="!commentLoading">
        <text>暂无评论，快来说两句</text>
      </view>

      <view class="loading-more" v-if="commentLoading">
        <text>加载中...</text>
      </view>
      <view
        class="load-more-btn"
        v-if="!commentLoading && !commentNoMore && comments.length > 0"
        @tap="loadComments"
      >
        <text>加载更多评论</text>
      </view>
    </view>

    <!-- Spacer for bottom bar -->
    <view style="height: 120rpx"></view>

    <!-- Bottom Input Bar -->
    <view class="bottom-bar">
      <view class="reply-hint" v-if="replyTo" @tap="cancelReply">
        <text>回复 @{{ replyTo.username }}</text>
        <text class="cancel-reply">✕</text>
      </view>
      <view class="input-row">
        <input
          class="comment-input"
          v-model="commentText"
          :placeholder="replyTo ? `回复 @${replyTo.username}` : '写评论...'"
          :adjust-position="true"
          confirm-type="send"
          @confirm="submitComment"
        />
        <view class="send-btn" :class="{ active: commentText.trim() }" @tap="submitComment">
          发送
        </view>
      </view>
    </view>
  </view>

  <!-- Loading State -->
  <view class="loading-page" v-else-if="pageLoading">
    <text>加载中...</text>
  </view>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { communityApi } from '@/services/api'
import { checkLogin, formatTime, defaultAvatar } from '@/utils/common'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()

const post = ref<any>(null)
const postId = ref(0)
const pageLoading = ref(true)
const isLiked = ref(false)
const isFollowed = ref(false)

const comments = ref<any[]>([])
const commentPage = ref(0)
const commentLoading = ref(false)
const commentNoMore = ref(false)
const commentText = ref('')
const replyTo = ref<any>(null)

const isAuthor = computed(() => post.value?.userId === userStore.userInfo?.id)

// Backend stores images as JSON string, parse to array
const postImages = computed(() => {
  if (!post.value) return []
  if (Array.isArray(post.value.images)) return post.value.images
  if (typeof post.value.images === 'string' && post.value.images) {
    try { return JSON.parse(post.value.images) } catch { return [] }
  }
  if (Array.isArray(post.value.imageUrls)) return post.value.imageUrls
  return []
})

async function loadPost() {
  pageLoading.value = true
  try {
    const res = await communityApi.getPost(postId.value)
    post.value = res.data
    isLiked.value = res.data?.liked || false
    if (post.value?.userId && !isAuthor.value) {
      try {
        const followRes = await communityApi.getFollowStatus(post.value.userId)
        isFollowed.value = followRes.data?.isFollowing || followRes.data?.followed || false
      } catch {}
    }
  } catch {
    uni.showToast({ title: '帖子加载失败', icon: 'none' })
  } finally {
    pageLoading.value = false
  }
}

async function loadComments() {
  if (commentLoading.value) return
  commentLoading.value = true
  try {
    const res = await communityApi.getComments(postId.value, {
      page: commentPage.value,
      size: 15
    })
    const page = res.data
    const list = page?.content || page?.records || page?.list || (Array.isArray(page) ? page : [])
    comments.value = [...comments.value, ...list]
    commentNoMore.value = list.length < 15
    commentPage.value++
  } catch {
    uni.showToast({ title: '评论加载失败', icon: 'none' })
  } finally {
    commentLoading.value = false
  }
}

function previewImage(index: number) {
  uni.previewImage({
    urls: postImages.value,
    current: index
  })
}

async function handleLike() {
  if (!checkLogin()) return
  try {
    const res = await communityApi.toggleLike({ targetType: 'POST', targetId: postId.value })
    isLiked.value = res.data?.liked ?? !isLiked.value
    post.value.likesCount = (post.value.likesCount || 0) + (isLiked.value ? 1 : -1)
  } catch {
    uni.showToast({ title: '操作失败', icon: 'none' })
  }
}

async function handleFollow() {
  if (!checkLogin()) return
  try {
    const res = await communityApi.toggleFollow(post.value.userId)
    isFollowed.value = res.data?.followed ?? !isFollowed.value
    uni.showToast({ title: isFollowed.value ? '已关注' : '已取消关注', icon: 'none' })
  } catch {
    uni.showToast({ title: '操作失败', icon: 'none' })
  }
}

function setReply(comment: any) {
  replyTo.value = comment
}

function cancelReply() {
  replyTo.value = null
}

async function submitComment() {
  const text = commentText.value.trim()
  if (!text) return
  if (!checkLogin()) return
  try {
    const data: any = { content: text }
    if (replyTo.value) {
      data.parentId = replyTo.value.id
      data.replyToUserId = replyTo.value.userId
    }
    await communityApi.addComment(postId.value, data)
    commentText.value = ''
    replyTo.value = null
    uni.showToast({ title: '评论成功', icon: 'success' })
    comments.value = []
    commentPage.value = 0
    commentNoMore.value = false
    loadComments()
    if (post.value) post.value.commentsCount = (post.value.commentsCount || 0) + 1
  } catch {
    uni.showToast({ title: '评论失败', icon: 'none' })
  }
}

async function deleteComment(id: number) {
  uni.showModal({
    title: '提示',
    content: '确定删除这条评论吗？',
    success: async (res) => {
      if (res.confirm) {
        try {
          await communityApi.deleteComment(id)
          comments.value = comments.value.filter(c => c.id !== id)
          if (post.value) post.value.commentsCount = Math.max(0, (post.value.commentsCount || 0) - 1)
          uni.showToast({ title: '已删除', icon: 'success' })
        } catch {
          uni.showToast({ title: '删除失败', icon: 'none' })
        }
      }
    }
  })
}

async function handleDelete() {
  uni.showModal({
    title: '提示',
    content: '确定删除这篇帖子吗？删除后不可恢复',
    success: async (res) => {
      if (res.confirm) {
        try {
          await communityApi.deletePost(postId.value)
          uni.showToast({ title: '已删除', icon: 'success' })
          setTimeout(() => uni.navigateBack(), 500)
        } catch {
          uni.showToast({ title: '删除失败', icon: 'none' })
        }
      }
    }
  })
}

onLoad((query) => {
  postId.value = Number(query?.postId || 0)
  if (postId.value) {
    loadPost()
    loadComments()
  }
})
</script>

<style lang="scss" scoped>
.detail-page {
  min-height: 100vh;
  background: $background;
}

.loading-page {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  font-size: 28rpx;
  color: $muted-foreground;
  font-family: 'Inter', 'PingFang SC', sans-serif;
}

.author-bar {
  display: flex;
  align-items: center;
  background: $card;
  padding: 28rpx;
  border-bottom: 1rpx solid $border;
}

.avatar {
  width: 80rpx;
  height: 80rpx;
  border-radius: $radius-full;
  border: none;
  margin-right: 20rpx;
  flex-shrink: 0;
}

.author-info {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.nickname {
  font-size: 30rpx;
  font-weight: 600;
  color: $foreground;
  font-family: 'Inter', 'PingFang SC', sans-serif;
}

.time {
  font-size: 24rpx;
  color: $muted-foreground;
  margin-top: 4rpx;
  font-family: 'Inter', 'PingFang SC', sans-serif;
}

.follow-btn {
  padding: 10rpx 28rpx;
  border-radius: $radius-full;
  font-size: 24rpx;
  background: $accent;
  color: $accent-foreground;
  border: none;
  box-shadow: $shadow-accent;
  font-family: 'Inter', 'PingFang SC', sans-serif;
  transition: transform 0.15s ease;
}
.follow-btn:active {
  transform: scale(0.97);
}

.follow-btn.followed {
  background: $muted;
  color: $muted-foreground;
  border: 1rpx solid $border;
  box-shadow: none;
}

.delete-btn {
  padding: 10rpx 28rpx;
  border-radius: $radius-full;
  font-size: 24rpx;
  background: $uni-error;
  color: #fff;
  border: none;
  font-family: 'Inter', 'PingFang SC', sans-serif;
  transition: transform 0.15s ease;
}
.delete-btn:active {
  transform: scale(0.97);
}

.post-body {
  background: $card;
  padding: 0 28rpx 28rpx;
}

.category-tag {
  display: inline-block;
  font-size: 22rpx;
  color: $accent;
  background: rgba(0, 82, 255, 0.06);
  padding: 6rpx 16rpx;
  border-radius: $radius-full;
  border: none;
  margin-bottom: 16rpx;
  font-family: 'JetBrains Mono', 'PingFang SC', monospace;
}

.content-text {
  font-size: 30rpx;
  color: $foreground;
  line-height: 1.8;
  word-break: break-all;
  font-family: 'Inter', 'PingFang SC', sans-serif;
}

.image-gallery {
  background: $card;
  padding: 0 28rpx 28rpx;
  display: flex;
  flex-wrap: wrap;
  gap: 8rpx;
}

.gallery-img {
  width: calc(33.33% - 6rpx);
  aspect-ratio: 1;
  border-radius: $radius-lg;
  border: none;
}

.gallery-img.single-img {
  width: 100%;
  max-height: 600rpx;
  aspect-ratio: auto;
}

.video-wrapper {
  background: $card;
  padding: 0 28rpx 28rpx;
}

.post-video {
  width: 100%;
  height: 400rpx;
  border-radius: $radius-lg;
  border: none;
}

.stats-bar {
  background: $card;
  display: flex;
  padding: 24rpx 28rpx;
  gap: 60rpx;
  border-top: 1rpx solid $border;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 8rpx;
  padding: 8rpx 16rpx;
  border-radius: $radius-full;
  transition: all 0.2s;
}

.stat-item.liked {
  background: rgba(239, 68, 68, 0.06);
}

.stat-icon {
  font-size: 32rpx;
}

.stat-count {
  font-size: 26rpx;
  color: $muted-foreground;
  font-family: 'Inter', 'PingFang SC', sans-serif;
}

.comments-section {
  background: $card;
  margin-top: 16rpx;
  padding: 28rpx;
  border-top: 1rpx solid $border;
}

.section-title {
  font-size: 30rpx;
  font-weight: 600;
  color: $foreground;
  margin-bottom: 24rpx;
  font-family: 'Calistoga', 'PingFang SC', sans-serif;
  position: relative;
  display: inline-block;
}
.section-title::after {
  content: '';
  position: absolute;
  bottom: -4rpx;
  left: 0;
  width: 100%;
  height: 3rpx;
  background: $accent;
  border-radius: 2rpx;
}

.comment-item {
  display: flex;
  padding: 20rpx 0;
  border-bottom: 1rpx solid $border;
}

.comment-avatar {
  width: 64rpx;
  height: 64rpx;
  border-radius: $radius-full;
  border: none;
  margin-right: 16rpx;
  flex-shrink: 0;
}

.comment-body {
  flex: 1;
  min-width: 0;
  background: $muted;
  border: none;
  border-radius: $radius-lg;
  padding: 16rpx;
  position: relative;
}

.comment-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8rpx;
}

.comment-nick {
  font-size: 26rpx;
  font-weight: 600;
  color: $foreground;
  font-family: 'Inter', 'PingFang SC', sans-serif;
}

.comment-time {
  font-size: 22rpx;
  color: $muted-foreground;
  font-family: 'Inter', 'PingFang SC', sans-serif;
}

.reply-tag {
  font-size: 22rpx;
  color: $muted-foreground;
  margin-bottom: 8rpx;
}

.reply-nick {
  color: $accent;
  font-weight: 600;
}

.comment-content {
  font-size: 28rpx;
  color: $foreground;
  line-height: 1.6;
  font-family: 'Inter', 'PingFang SC', sans-serif;
}

.comment-actions {
  display: flex;
  gap: 24rpx;
  margin-top: 12rpx;
}

.action-btn {
  font-size: 22rpx;
  color: $accent;
  font-family: 'Inter', 'PingFang SC', sans-serif;
}

.action-btn.delete {
  color: $uni-error;
}

.empty-comments {
  text-align: center;
  padding: 60rpx 0;
  font-size: 26rpx;
  color: $muted-foreground;
  font-family: 'Inter', 'PingFang SC', sans-serif;
}

.loading-more {
  text-align: center;
  padding: 20rpx;
  font-size: 24rpx;
  color: $muted-foreground;
  font-family: 'Inter', 'PingFang SC', sans-serif;
}

.load-more-btn {
  text-align: center;
  padding: 20rpx;
  font-size: 26rpx;
  color: $accent;
  font-family: 'Inter', 'PingFang SC', sans-serif;
  border: 1rpx solid $border;
  border-radius: $radius-lg;
  margin-top: 16rpx;
  transition: background 0.2s;
}

.bottom-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  background: $card;
  border-top: 1rpx solid $border;
  padding: 16rpx 28rpx;
  padding-bottom: calc(16rpx + env(safe-area-inset-bottom));
  z-index: 100;
}

.reply-hint {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8rpx 16rpx;
  margin-bottom: 8rpx;
  background: $muted;
  border-radius: $radius-lg;
  border: none;
  font-size: 22rpx;
  color: $foreground;
  font-family: 'Inter', 'PingFang SC', sans-serif;
}

.cancel-reply {
  color: $muted-foreground;
  padding: 4rpx 8rpx;
}

.input-row {
  display: flex;
  align-items: center;
  gap: 16rpx;
}

.comment-input {
  flex: 1;
  height: 72rpx;
  background: $muted;
  border: 1rpx solid $border;
  border-radius: $radius-lg;
  padding: 0 28rpx;
  font-size: 28rpx;
  font-family: 'Inter', 'PingFang SC', sans-serif;
  color: $foreground;
}

.send-btn {
  padding: 16rpx 32rpx;
  border-radius: $radius-lg;
  font-size: 28rpx;
  background: $muted;
  color: $muted-foreground;
  border: none;
  flex-shrink: 0;
  font-family: 'Inter', 'PingFang SC', sans-serif;
  transition: all 0.2s;
}

.send-btn.active {
  background: $accent;
  color: $accent-foreground;
  box-shadow: $shadow-accent;
}
.send-btn.active:active {
  transform: scale(0.97);
}
</style>
