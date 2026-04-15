<template>
  <view class="create-page">
    <!-- Category Select -->
    <view class="category-section">
      <text class="label">分类</text>
      <scroll-view scroll-x class="cat-scroll">
        <view class="cat-inner">
          <view
            v-for="cat in categories"
            :key="cat.key"
            :class="['cat-tag', { active: form.category === cat.key }]"
            @tap="form.category = cat.key"
          >
            <text>{{ cat.icon }} {{ cat.label }}</text>
          </view>
        </view>
      </scroll-view>
    </view>

    <!-- Content -->
    <view class="content-section">
      <textarea
        v-model="form.content"
        placeholder="分享你的营养心得、饮食打卡、健身日记..."
        :maxlength="1000"
        class="content-textarea"
      />
      <text class="char-count text-xs text-muted">{{ form.content.length }}/1000</text>
    </view>

    <!-- Images -->
    <view class="images-section">
      <view class="image-grid">
        <view v-for="(img, i) in images" :key="i" class="image-item">
          <image :src="img.localPath" mode="aspectFill" class="preview-img" />
          <view class="remove-btn" @tap="removeImage(i)">
            <text>✕</text>
          </view>
        </view>
        <view v-if="images.length < 9" class="add-image" @tap="chooseImage">
          <text class="add-icon">📷</text>
          <text class="text-xs text-muted">{{ images.length }}/9</text>
        </view>
      </view>
    </view>

    <!-- Submit -->
    <view class="submit-section safe-bottom">
      <button class="btn-primary btn-block btn-lg" :disabled="submitting || !canSubmit" @tap="submitPost">
        {{ submitting ? '发布中...' : '发布动态' }}
      </button>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, reactive, computed } from 'vue'
import { communityApi, POST_CATEGORIES } from '../../services/api'

const categories = POST_CATEGORIES.filter(c => c.key)
const form = reactive({ content: '', category: '饮食打卡' })
const images = ref<Array<{ localPath: string; url?: string }>>([])
const submitting = ref(false)

const canSubmit = computed(() => form.content.trim().length > 0 && form.category)

function chooseImage() {
  const remaining = 9 - images.value.length
  if (remaining <= 0) return
  uni.chooseImage({
    count: remaining,
    sizeType: ['compressed'],
    sourceType: ['album', 'camera'],
    success: (res) => {
      res.tempFilePaths.forEach(path => {
        images.value.push({ localPath: path })
      })
    }
  })
}

function removeImage(index: number) {
  images.value.splice(index, 1)
}

async function submitPost() {
  if (!canSubmit.value || submitting.value) return
  submitting.value = true

  try {
    // Upload images first
    const uploadedUrls: string[] = []
    for (const img of images.value) {
      if (img.url) {
        uploadedUrls.push(img.url)
      } else {
        try {
          const res: any = await communityApi.uploadMedia(img.localPath)
          const url = res?.url || res?.data?.url || res
          if (typeof url === 'string') uploadedUrls.push(url)
        } catch {}
      }
    }

    await communityApi.createPost({
      content: form.content.trim(),
      category: form.category,
      images: uploadedUrls
    })

    uni.showToast({ title: '发布成功', icon: 'success' })
    setTimeout(() => uni.navigateBack(), 1000)
  } catch (e: any) {
    uni.showToast({ title: e?.message || '发布失败', icon: 'none' })
  }
  submitting.value = false
}
</script>

<style lang="scss" scoped>
.create-page {
  min-height: 100vh;
  background: $bg-page;
  padding: $spacing-md $spacing-lg;
}

.category-section {
  margin-bottom: $spacing-md;
}

.label {
  font-size: $font-base;
  font-weight: 500;
  color: $text-primary;
  margin-bottom: $spacing-sm;
  display: block;
}

.cat-scroll { white-space: nowrap; }

.cat-inner {
  display: inline-flex;
  gap: $spacing-sm;
}

.cat-tag {
  display: inline-flex;
  padding: 12rpx 24rpx;
  border-radius: $radius-full;
  font-size: $font-sm;
  color: $text-secondary;
  background: $bg-card;
  white-space: nowrap;
  border: 2rpx solid $border-color;

  &.active {
    background: $primary-50;
    color: $primary;
    border-color: $primary;
  }
}

.content-section {
  position: relative;
  background: $bg-card;
  border-radius: $radius-lg;
  padding: $spacing-md;
  margin-bottom: $spacing-md;
}

.content-textarea {
  width: 100%;
  min-height: 300rpx;
  font-size: $font-base;
  color: $text-primary;
  line-height: 1.7;
}

.char-count {
  text-align: right;
  display: block;
  margin-top: $spacing-xs;
}

.image-grid {
  display: flex;
  flex-wrap: wrap;
  gap: $spacing-sm;
}

.image-item {
  position: relative;
  width: calc(33.33% - 12rpx);
}

.preview-img {
  width: 100%;
  height: 0;
  padding-bottom: 100%;
  border-radius: $radius-md;
  background: $bg-muted;
}

.remove-btn {
  position: absolute;
  top: -12rpx;
  right: -12rpx;
  width: 40rpx;
  height: 40rpx;
  border-radius: 50%;
  background: rgba(0, 0, 0, 0.5);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20rpx;
}

.add-image {
  width: calc(33.33% - 12rpx);
  height: 0;
  padding-bottom: calc(33.33% - 12rpx);
  border-radius: $radius-md;
  border: 2rpx dashed $border-color;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  position: relative;

  .add-icon {
    position: absolute;
    top: 50%;
    left: 50%;
    transform: translate(-50%, -60%);
    font-size: 40rpx;
  }

  .text-xs {
    position: absolute;
    bottom: 16rpx;
    left: 50%;
    transform: translateX(-50%);
  }
}

.submit-section {
  margin-top: $spacing-xl;
}
</style>
