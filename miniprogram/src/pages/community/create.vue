<template>
  <view class="create-page">
    <!-- Header -->
    <view class="page-header">
      <view class="cancel-btn" @tap="goBack">取消</view>
      <text class="page-title">发布动态</text>
      <view class="publish-btn" :class="{ disabled: !canPublish }" @tap="handlePublish">
        发布
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
      <view class="section-label">图片（最多9张）</view>
      <view class="image-upload-grid">
        <view class="upload-item" v-for="(img, idx) in imageList" :key="idx">
          <image class="upload-img" :src="img.localPath" mode="aspectFill" />
          <view class="upload-status" v-if="img.uploading">
            <text class="uploading-text">上传中</text>
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
  background: $paper;
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #fff;
  padding: 24rpx 28rpx;
  position: sticky;
  top: 0;
  z-index: 10;
  border-bottom: 2rpx solid $pencil;
}

.cancel-btn {
  font-size: 28rpx;
  color: $pencil;
  padding: 8rpx 16rpx;
  font-family: 'Patrick Hand', 'PingFang SC', cursive;
}

.page-title {
  font-size: 32rpx;
  font-weight: 700;
  color: $pencil;
  font-family: 'Kalam', 'ZCOOL KuaiLe', 'PingFang SC', cursive;
}

.publish-btn {
  font-size: 28rpx;
  color: #fff;
  background: $accent;
  padding: 12rpx 32rpx;
  border-radius: $wobbly-sm;
  border: 2rpx solid $pencil;
  box-shadow: $shadow-hard-hover;
  font-family: 'Patrick Hand', 'PingFang SC', cursive;
}
.publish-btn:active {
  box-shadow: none;
  transform: translate(2rpx, 2rpx);
}

.publish-btn.disabled {
  background: $muted;
  color: rgba(45, 45, 45, 0.4);
  border-color: $muted;
  box-shadow: none;
}

.section {
  background: #fff;
  margin-top: 16rpx;
  padding: 28rpx;
  border-top: 2rpx dashed $muted;
}

.section-label {
  font-size: 28rpx;
  font-weight: 700;
  color: $pencil;
  margin-bottom: 20rpx;
  font-family: 'Kalam', 'ZCOOL KuaiLe', 'PingFang SC', cursive;
}

.category-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 16rpx;
}

.category-chip {
  padding: 14rpx 24rpx;
  border-radius: $wobbly-sm;
  font-size: 26rpx;
  color: $pencil;
  background: #fff;
  border: 2rpx solid $muted;
  font-family: 'Patrick Hand', 'PingFang SC', cursive;
  transition: all 0.2s;
}

.category-chip.active {
  background: $accent;
  color: #fff;
  border-color: $pencil;
  box-shadow: $shadow-hard-hover;
}

.textarea-wrapper {
  position: relative;
  border: 2rpx solid $pencil;
  border-radius: $wobbly-sm;
  padding: 20rpx;
  background: $paper;
}

.content-textarea {
  width: 100%;
  font-size: 30rpx;
  color: $pencil;
  line-height: 1.6;
  font-family: 'Patrick Hand', 'PingFang SC', cursive;
}

.char-count {
  text-align: right;
  font-size: 22rpx;
  color: rgba(45, 45, 45, 0.4);
  margin-top: 12rpx;
  font-family: 'Patrick Hand', 'PingFang SC', cursive;
}

.char-count.warn {
  color: $accent;
}

.media-toggle {
  display: flex;
  gap: 20rpx;
}

.toggle-btn {
  flex: 1;
  text-align: center;
  padding: 20rpx;
  border-radius: $wobbly-sm;
  font-size: 28rpx;
  color: $pencil;
  background: $paper;
  border: 2rpx solid $muted;
  font-family: 'Patrick Hand', 'PingFang SC', cursive;
}

.toggle-btn.active {
  background: rgba(255, 77, 77, 0.08);
  color: $accent;
  border: 2rpx solid $accent;
  box-shadow: $shadow-hard-hover;
}

.image-upload-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 12rpx;
}

.upload-item {
  position: relative;
  width: calc(33.33% - 8rpx);
  aspect-ratio: 1;
  border-radius: $wobbly-sm;
  overflow: hidden;
  border: 2rpx solid $pencil;
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
  background: rgba(45, 45, 45, 0.55);
  display: flex;
  align-items: center;
  justify-content: center;
}

.uploading-text {
  color: #fff;
  font-size: 24rpx;
  font-family: 'Patrick Hand', 'PingFang SC', cursive;
}

.remove-btn {
  position: absolute;
  top: 6rpx;
  right: 6rpx;
  width: 40rpx;
  height: 40rpx;
  border-radius: 50%;
  background: $accent;
  border: 2rpx solid $pencil;
  color: #fff;
  font-size: 24rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}

.add-btn {
  width: calc(33.33% - 8rpx);
  aspect-ratio: 1;
  border: 3rpx dashed $pencil;
  border-radius: $wobbly-sm;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8rpx;
  background: $paper;
}

.add-icon {
  font-size: 48rpx;
  color: $pencil;
}

.add-text {
  font-size: 22rpx;
  color: rgba(45, 45, 45, 0.5);
  font-family: 'Patrick Hand', 'PingFang SC', cursive;
}

.video-upload-area {
  display: flex;
}

.add-video-btn {
  width: 200rpx;
  height: 200rpx;
  border: 3rpx dashed $pencil;
  border-radius: $wobbly-sm;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12rpx;
  background: $paper;
}

.video-preview {
  position: relative;
}

.preview-video {
  width: 100%;
  height: 400rpx;
  border-radius: $wobbly-sm;
  border: 2rpx solid $pencil;
}

.remove-video {
  text-align: center;
  margin-top: 16rpx;
  font-size: 26rpx;
  color: $accent;
  font-family: 'Patrick Hand', 'PingFang SC', cursive;
}
</style>
