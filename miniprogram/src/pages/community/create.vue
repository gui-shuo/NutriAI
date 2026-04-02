<template>
  <view class="create-page">
    <!-- Header -->
    <view class="page-header">
      <view class="cancel-btn" @tap="goBack">取消</view>
      <text class="page-title">发布动态</text>
      <view
        class="publish-btn"
        :class="{ disabled: !canPublish, loading: publishing }"
        @tap="handlePublish"
      >
        {{ publishing ? '发布中...' : '发布' }}
      </view>
    </view>

    <!-- Category Picker -->
    <view class="section">
      <view class="section-label">选择分类</view>
      <view class="category-grid">
        <view
          v-for="cat in PostCategories"
          :key="cat.value"
          class="category-chip"
          :class="{ active: selectedCategory === cat.value }"
          @tap="selectedCategory = cat.value"
        >
          {{ cat.label }}
        </view>
      </view>
    </view>

    <!-- Content Textarea -->
    <view class="section">
      <view class="section-label">内容</view>
      <view class="textarea-wrapper">
        <textarea
          class="content-textarea"
          v-model="content"
          placeholder="分享你的营养饮食心得..."
          :maxlength="2000"
          auto-height
          :style="{ minHeight: '300rpx' }"
        />
        <view class="char-count" :class="{ warn: content.length > 1800 }">
          {{ content.length }}/2000
        </view>
      </view>
    </view>

    <!-- Media Type Toggle -->
    <view class="section" v-if="!videoUrl && imageList.length === 0">
      <view class="section-label">添加媒体（可选）</view>
      <view class="media-toggle">
        <view class="toggle-btn" :class="{ active: mediaType === 'image' }" @tap="mediaType = 'image'">
          📷 图片
        </view>
        <view class="toggle-btn" :class="{ active: mediaType === 'video' }" @tap="mediaType = 'video'">
          🎬 视频
        </view>
      </view>
    </view>

    <!-- Image Upload -->
    <view class="section" v-if="mediaType === 'image' && !videoUrl">
      <view class="section-label">图片（最多9张）<text class="label-hint">{{ imageList.length }}/9</text></view>
      <view class="image-upload-grid">
        <view class="upload-item" v-for="(img, idx) in imageList" :key="idx">
          <image class="upload-img" :src="img.localPath" mode="aspectFill" />
          <view class="upload-status" v-if="img.uploading">
            <text class="uploading-text">上传中...</text>
          </view>
          <view class="upload-done" v-else-if="img.url">
            <text class="done-icon">✓</text>
          </view>
          <view class="remove-btn" @tap="removeImage(idx)">✕</view>
        </view>
        <view class="add-btn" v-if="imageList.length < 9" @tap="chooseImages">
          <text class="add-icon">+</text>
          <text class="add-text">添加图片</text>
        </view>
      </view>
    </view>

    <!-- Video Upload -->
    <view class="section" v-if="mediaType === 'video' && imageList.length === 0">
      <view class="section-label">视频（最长30秒）</view>
      <view class="video-upload-area" v-if="!videoUrl">
        <view class="add-video-btn" @tap="chooseVideo">
          <text class="add-icon">🎬</text>
          <text class="add-text">选择视频</text>
        </view>
      </view>
      <view class="video-preview" v-else>
        <video :src="videoUrl" class="preview-video" controls :poster="videoCover" />
        <view class="remove-video" @tap="removeVideo">
          <text>✕ 移除视频</text>
        </view>
      </view>
    </view>

    <!-- Preview Section -->
    <view class="section preview-section" v-if="canPreview">
      <view class="section-label">预览</view>
      <view class="preview-card">
        <view class="preview-meta">
          <view class="preview-category" v-if="selectedCategory">{{ getCategoryLabel(selectedCategory) }}</view>
        </view>
        <text class="preview-content">{{ content }}</text>
        <view
          class="preview-images"
          :class="previewGridClass"
          v-if="imageList.length > 0"
        >
          <image
            class="preview-img"
            v-for="(img, idx) in imageList"
            :key="idx"
            :src="img.localPath"
            mode="aspectFill"
          />
        </view>
        <view class="preview-video-box" v-if="videoUrl">
          <text class="preview-video-tag">🎬 已添加视频</text>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { communityApi, PostCategories } from '@/services/api'
import { checkLogin } from '@/utils/common'

interface ImageItem {
  localPath: string
  url: string
  uploading: boolean
}

const content = ref('')
const selectedCategory = ref('')
const mediaType = ref<'image' | 'video'>('image')

const imageList = ref<ImageItem[]>([])
const videoUrl = ref('')
const videoCover = ref('')
const publishing = ref(false)

const canPublish = computed(() => {
  return (
    content.value.trim().length > 0 &&
    selectedCategory.value &&
    !publishing.value &&
    !imageList.value.some(img => img.uploading)
  )
})

const canPreview = computed(() => {
  return content.value.trim().length > 0 || imageList.value.length > 0 || videoUrl.value
})

const previewGridClass = computed(() => {
  const count = imageList.value.length
  if (count === 1) return 'cols-1'
  if (count === 2) return 'cols-2'
  return 'cols-3'
})

function getCategoryLabel(value: string): string {
  const found = PostCategories.find((c: any) => c.value === value)
  return found ? found.label : value
}

function goBack() {
  if (content.value.trim() || imageList.value.length || videoUrl.value) {
    uni.showModal({
      title: '提示',
      content: '确定放弃编辑吗？',
      success: (res) => {
        if (res.confirm) uni.navigateBack()
      }
    })
  } else {
    uni.navigateBack()
  }
}

async function chooseImages() {
  const remaining = 9 - imageList.value.length
  if (remaining <= 0) return

  uni.chooseImage({
    count: remaining,
    sizeType: ['compressed'],
    sourceType: ['album', 'camera'],
    success: (res) => {
      res.tempFilePaths.forEach((path) => {
        const item: ImageItem = { localPath: path, url: '', uploading: true }
        imageList.value.push(item)
        const idx = imageList.value.length - 1
        uploadImage(idx, path)
      })
      mediaType.value = 'image'
    }
  })
}

async function uploadImage(index: number, filePath: string) {
  try {
    const res = await communityApi.uploadMedia(filePath)
    if (imageList.value[index]) {
      imageList.value[index].url = res.data?.url || res.data || ''
      imageList.value[index].uploading = false
    }
  } catch {
    uni.showToast({ title: '图片上传失败', icon: 'none' })
    imageList.value.splice(index, 1)
  }
}

function removeImage(index: number) {
  imageList.value.splice(index, 1)
}

function chooseVideo() {
  uni.chooseVideo({
    maxDuration: 30,
    sourceType: ['album', 'camera'],
    compressed: true,
    success: async (res) => {
      uni.showLoading({ title: '上传视频中...' })
      try {
        const uploadRes = await communityApi.uploadMedia(res.tempFilePath)
        videoUrl.value = uploadRes.data?.url || uploadRes.data || ''
        videoCover.value = res.thumbTempFilePath || ''
        mediaType.value = 'video'
      } catch {
        uni.showToast({ title: '视频上传失败', icon: 'none' })
      } finally {
        uni.hideLoading()
      }
    }
  })
}

function removeVideo() {
  videoUrl.value = ''
  videoCover.value = ''
}

async function handlePublish() {
  if (!canPublish.value) return
  if (!checkLogin()) return

  if (!content.value.trim()) {
    return uni.showToast({ title: '请输入内容', icon: 'none' })
  }
  if (!selectedCategory.value) {
    return uni.showToast({ title: '请选择分类', icon: 'none' })
  }

  publishing.value = true
  uni.showLoading({ title: '发布中...' })
  try {
    const postData: any = {
      content: content.value.trim(),
      category: selectedCategory.value
    }

    const uploadedUrls = imageList.value.filter(img => img.url).map(img => img.url)
    if (uploadedUrls.length > 0) {
      postData.imageUrls = uploadedUrls
    }
    if (videoUrl.value) {
      postData.videoUrl = videoUrl.value
    }

    await communityApi.createPost(postData)
    uni.showToast({ title: '发布成功', icon: 'success' })
    setTimeout(() => uni.navigateBack(), 500)
  } catch {
    uni.showToast({ title: '发布失败', icon: 'none' })
  } finally {
    publishing.value = false
    uni.hideLoading()
  }
}
</script>

<style lang="scss" scoped>
.create-page {
  min-height: 100vh;
  background: $background;
  padding-bottom: 40rpx;
}

/* ===== Header ===== */
.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: $card;
  padding: 24rpx 28rpx;
  position: sticky;
  top: 0;
  z-index: 10;
  border-bottom: 1rpx solid $border;
}
.cancel-btn {
  font-size: 28rpx;
  color: $muted-foreground;
  padding: 8rpx 16rpx;
  font-family: 'Inter', 'PingFang SC', sans-serif;
}
.page-title {
  font-size: 32rpx;
  font-weight: 600;
  color: $foreground;
  font-family: 'Inter', 'PingFang SC', sans-serif;
}
.publish-btn {
  font-size: 28rpx;
  color: #fff;
  background: $gradient-accent;
  padding: 12rpx 32rpx;
  border-radius: $radius-full;
  border: none;
  box-shadow: $shadow-accent;
  font-family: 'Inter', 'PingFang SC', sans-serif;
  transition: transform 0.15s ease, opacity 0.2s;
}
.publish-btn:active {
  transform: scale(0.97);
}
.publish-btn.disabled {
  background: $muted;
  color: $muted-foreground;
  box-shadow: none;
}
.publish-btn.loading {
  opacity: 0.7;
}

/* ===== Sections ===== */
.section {
  background: $card;
  margin-top: 16rpx;
  padding: 28rpx;
  border-top: 1rpx solid $border;
}
.section-label {
  font-size: 28rpx;
  font-weight: 600;
  color: $foreground;
  margin-bottom: 20rpx;
  font-family: 'Inter', 'PingFang SC', sans-serif;
  display: flex;
  align-items: center;
  gap: 12rpx;
}
.label-hint {
  font-size: 22rpx;
  color: $muted-foreground;
  font-weight: 400;
}

/* ===== Category Grid ===== */
.category-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 16rpx;
}
.category-chip {
  padding: 14rpx 24rpx;
  border-radius: $radius-full;
  font-size: 26rpx;
  color: $foreground;
  background: $card;
  border: 1rpx solid $border;
  font-family: 'Inter', 'PingFang SC', sans-serif;
  transition: all 0.2s;
}
.category-chip.active {
  background: $gradient-accent;
  color: #fff;
  border-color: transparent;
  box-shadow: $shadow-accent;
}

/* ===== Textarea ===== */
.textarea-wrapper {
  position: relative;
  border: 1rpx solid $border;
  border-radius: $radius-lg;
  padding: 20rpx;
  background: $muted;
  box-sizing: border-box;
  width: 100%;
}
.content-textarea {
  width: 100%;
  font-size: 30rpx;
  color: $foreground;
  line-height: 1.6;
  font-family: 'Inter', 'PingFang SC', sans-serif;
  box-sizing: border-box;
}
.char-count {
  text-align: right;
  font-size: 22rpx;
  color: $muted-foreground;
  margin-top: 12rpx;
  font-family: 'Inter', 'PingFang SC', sans-serif;
}
.char-count.warn {
  color: $uni-error;
}

/* ===== Media Toggle ===== */
.media-toggle {
  display: flex;
  gap: 20rpx;
}
.toggle-btn {
  flex: 1;
  text-align: center;
  padding: 20rpx;
  border-radius: $radius-lg;
  font-size: 28rpx;
  color: $foreground;
  background: $muted;
  border: 1rpx solid $border;
  font-family: 'Inter', 'PingFang SC', sans-serif;
  transition: all 0.2s;
}
.toggle-btn.active {
  background: rgba(16, 185, 129, 0.06);
  color: $accent;
  border: 1rpx solid $accent;
}

/* ===== Image Upload Grid ===== */
.image-upload-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12rpx;
}
.upload-item {
  position: relative;
  aspect-ratio: 1;
  border-radius: $radius-lg;
  overflow: hidden;
  border: none;
  min-width: 0;
}
.upload-img {
  width: 100%;
  height: 100%;
}
.upload-status {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(15, 23, 42, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
}
.uploading-text {
  color: #fff;
  font-size: 24rpx;
  font-family: 'Inter', 'PingFang SC', sans-serif;
}
.upload-done {
  position: absolute;
  bottom: 6rpx;
  right: 6rpx;
  width: 36rpx;
  height: 36rpx;
  border-radius: $radius-full;
  background: $accent;
  display: flex;
  align-items: center;
  justify-content: center;
}
.done-icon {
  color: #fff;
  font-size: 20rpx;
  font-weight: 700;
}
.remove-btn {
  position: absolute;
  top: 6rpx;
  right: 6rpx;
  width: 40rpx;
  height: 40rpx;
  border-radius: $radius-full;
  background: $uni-error;
  border: none;
  color: #fff;
  font-size: 24rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}
.add-btn {
  aspect-ratio: 1;
  border: 2rpx dashed $border;
  border-radius: $radius-lg;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8rpx;
  background: $muted;
  transition: border-color 0.2s;
  min-width: 0;
}
.add-icon {
  font-size: 48rpx;
  color: $muted-foreground;
}
.add-text {
  font-size: 22rpx;
  color: $muted-foreground;
  font-family: 'Inter', 'PingFang SC', sans-serif;
}

/* ===== Video Upload ===== */
.video-upload-area {
  display: flex;
}
.add-video-btn {
  width: 200rpx;
  height: 200rpx;
  border: 2rpx dashed $border;
  border-radius: $radius-lg;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12rpx;
  background: $muted;
  transition: border-color 0.2s;
}
.video-preview {
  position: relative;
}
.preview-video {
  width: 100%;
  height: 400rpx;
  border-radius: $radius-lg;
  border: none;
}
.remove-video {
  text-align: center;
  margin-top: 16rpx;
  font-size: 26rpx;
  color: $uni-error;
  font-family: 'Inter', 'PingFang SC', sans-serif;
}

/* ===== Preview Section ===== */
.preview-section {
  border-top: 1rpx solid $border;
}
.preview-card {
  background: $muted;
  border: 1rpx solid $border;
  border-radius: $radius-lg;
  padding: 24rpx;
}
.preview-meta {
  display: flex;
  gap: 12rpx;
  margin-bottom: 16rpx;
}
.preview-category {
  font-size: 22rpx;
  color: $accent;
  background: rgba(16, 185, 129, 0.08);
  padding: 6rpx 16rpx;
  border-radius: $radius-full;
  font-family: 'Inter', 'PingFang SC', sans-serif;
}
.preview-content {
  font-size: 28rpx;
  color: $foreground;
  line-height: 1.7;
  font-family: 'Inter', 'PingFang SC', sans-serif;
  white-space: pre-wrap;
  word-break: break-word;
  display: -webkit-box;
  -webkit-line-clamp: 5;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.preview-images {
  display: grid;
  gap: 8rpx;
  margin-top: 16rpx;
  border-radius: $radius-lg;
  overflow: hidden;
}
.preview-images.cols-1 {
  grid-template-columns: 1fr;
  max-width: 400rpx;
}
.preview-images.cols-2 {
  grid-template-columns: 1fr 1fr;
  max-width: 480rpx;
}
.preview-images.cols-3 {
  grid-template-columns: 1fr 1fr 1fr;
}
.preview-img {
  width: 100%;
  aspect-ratio: 1;
  border-radius: $radius-md;
}
.preview-video-box {
  margin-top: 16rpx;
}
.preview-video-tag {
  font-size: 24rpx;
  color: $accent;
  background: rgba(16, 185, 129, 0.06);
  padding: 8rpx 20rpx;
  border-radius: $radius-full;
  font-family: 'Inter', 'PingFang SC', sans-serif;
}
</style>
