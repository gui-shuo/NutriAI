<script setup>
/**
 * 意见反馈页
 */
import { ref } from 'vue'
import NavBar from '../../components/NavBar.vue'
import { feedbackApi } from '../../services/api'
import { checkLogin } from '../../utils/common'

const types = ['功能建议', 'Bug反馈', '体验优化', '其他']
const selectedType = ref(0)
const content = ref('')
const contact = ref('')
const submitting = ref(false)

async function submit() {
  if (!checkLogin()) return
  if (!content.value.trim()) {
    uni.showToast({ title: '请输入反馈内容', icon: 'none' })
    return
  }
  submitting.value = true
  try {
    await feedbackApi.submit({
      type: types[selectedType.value],
      title: types[selectedType.value],
      content: content.value,
      contactInfo: contact.value || undefined,
    })
    uni.showToast({ title: '提交成功，感谢反馈', icon: 'success' })
    setTimeout(() => uni.navigateBack(), 1500)
  } catch (e) {
    uni.showToast({ title: '提交失败', icon: 'none' })
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <view class="page">
    <NavBar showBack title="意见反馈" />

    <scroll-view scroll-y class="content" :enhanced="true" :show-scrollbar="false">
      <!-- 反馈类型 -->
      <view class="section">
        <text class="section__title">反馈类型</text>
        <view class="type-tags">
          <u-tag
            v-for="(t, idx) in types"
            :key="idx"
            :text="t"
            :plain="selectedType !== idx"
            :color="selectedType === idx ? '#ffffff' : '#333'"
            :bgColor="selectedType === idx ? '#0a6e2c' : '#f5f5f5'"
            :borderColor="selectedType === idx ? '#0a6e2c' : '#e0e0e0'"
            size="medium"
            shape="circle"
            @click="selectedType = idx"
          />
        </view>
      </view>

      <!-- 反馈内容 -->
      <view class="section">
        <text class="section__title">详细描述</text>
        <textarea
          class="feedback-textarea"
          v-model="content"
          placeholder="请详细描述您遇到的问题或建议..."
          maxlength="500"
        />
        <text class="char-count">{{ content.length }}/500</text>
      </view>

      <!-- 联系方式 -->
      <view class="section">
        <text class="section__title">联系方式（可选）</text>
        <input class="contact-input" v-model="contact" placeholder="手机号或邮箱，方便我们回复您" />
      </view>

      <!-- 提交按钮 -->
      <u-button
        :text="submitting ? '提交中...' : '提交反馈'"
        type="primary"
        shape="circle"
        color="#0a6e2c"
        :loading="submitting"
        :disabled="submitting"
        @click="submit"
        :customStyle="{marginTop: '16rpx'}"
      />
    </scroll-view>
  </view>
</template>

<style lang="scss" scoped>
@import '../../styles/design-system.scss';

.page { min-height: 100vh; background: #ffffff; overflow-x: hidden; width: 100%; }
.content { padding: 24rpx; height: calc(100vh - 100px); }

.section {
  margin-bottom: 28rpx;

  &__title {
    display: block;
    font-size: $font-base;
    font-weight: 600;
    color: $on-surface;
    margin-bottom: 16rpx;
  }
}

.type-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 12rpx;
}

.type-tag {
  padding: 14rpx 28rpx;
  background: $surface-container-low;
  border-radius: $radius-full;
  font-size: $font-sm;
  color: $on-surface;

  &--active {
    background: $primary-container;
    color: $primary;
    font-weight: 600;
  }
}

.feedback-textarea {
  width: 100%;
  height: 280rpx;
  background: $surface-container-lowest;
  border-radius: $radius-xl;
  padding: 20rpx;
  font-size: $font-base;
  color: $on-surface;
  box-sizing: border-box;
  line-height: $leading-relaxed;
}

.char-count {
  display: block;
  text-align: right;
  font-size: $font-xs;
  color: $on-surface-variant;
  margin-top: 8rpx;
}

.contact-input {
  width: 100%;
  background: $surface-container-lowest;
  border-radius: $radius-xl;
  padding: 20rpx;
  font-size: $font-base;
  color: $on-surface;
  box-sizing: border-box;
}

.submit-btn {
  padding: 24rpx;
  background: $primary;
  color: $on-primary;
  text-align: center;
  border-radius: $radius-xl;
  font-size: $font-base;
  font-weight: 600;
  margin-top: 16rpx;

  &--disabled { opacity: 0.5; pointer-events: none; }
  &:active { transform: scale(0.98); }
}
</style>
