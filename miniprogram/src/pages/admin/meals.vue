<template>
  <view class="page">
    <!-- Header -->
    <view class="top-bar">
      <text class="top-title">营养餐管理</text>
      <view class="add-btn" @tap="openForm()">+ 添加</view>
    </view>

    <!-- Loading -->
    <view v-if="loading" class="loading-wrap">
      <text class="loading-text">加载中...</text>
    </view>

    <!-- Empty -->
    <view v-else-if="!meals.length" class="empty-wrap">
      <text class="empty-icon">🍱</text>
      <text class="empty-text">暂无营养餐数据</text>
    </view>

    <!-- Meals List -->
    <view v-else class="list">
      <view v-for="meal in meals" :key="meal.id" class="item-card">
        <view class="item-row">
          <image v-if="meal.imageUrl" class="item-img" :src="meal.imageUrl" mode="aspectFill" />
          <view v-else class="item-img-placeholder">🍱</view>
          <view class="item-info">
            <view class="item-name-row">
              <text class="item-name">{{ meal.name }}</text>
              <view class="status-tag" :class="meal.isAvailable ? 'on' : 'off'">
                {{ meal.isAvailable ? '上架' : '下架' }}
              </view>
            </view>
            <text class="item-meta">¥{{ meal.price }} · {{ meal.calories || 0 }}kcal</text>
            <text class="item-desc">{{ meal.category || '未分类' }}</text>
          </view>
        </view>
        <view class="item-actions">
          <view class="act-btn" @tap="openForm(meal)">编辑</view>
          <view class="act-btn" @tap="toggleStatus(meal)">{{ meal.isAvailable ? '下架' : '上架' }}</view>
          <view class="act-btn danger" @tap="handleDelete(meal)">删除</view>
        </view>
      </view>
    </view>

    <!-- Form Sheet -->
    <view v-if="showForm" class="sheet-mask" @tap.self="showForm = false">
      <view class="sheet">
        <view class="sheet-header">
          <text class="sheet-title">{{ editingId ? '编辑营养餐' : '添加营养餐' }}</text>
          <text class="sheet-close" @tap="showForm = false">✕</text>
        </view>
        <scroll-view scroll-y class="sheet-body">
          <view class="form-group">
            <text class="form-label">名称 *</text>
            <input class="form-input" v-model="form.name" placeholder="营养餐名称" />
          </view>
          <view class="form-group">
            <text class="form-label">描述</text>
            <textarea class="form-textarea" v-model="form.description" placeholder="营养餐描述" auto-height />
          </view>
          <view class="form-row">
            <view class="form-group half">
              <text class="form-label">分类</text>
              <input class="form-input" v-model="form.category" placeholder="如：减脂餐" />
            </view>
            <view class="form-group half">
              <text class="form-label">价格 *</text>
              <input class="form-input" v-model="form.price" type="digit" placeholder="0.00" />
            </view>
          </view>
          <view class="form-row">
            <view class="form-group half">
              <text class="form-label">热量(kcal)</text>
              <input class="form-input" v-model="form.calories" type="number" placeholder="0" />
            </view>
            <view class="form-group half">
              <text class="form-label">蛋白质(g)</text>
              <input class="form-input" v-model="form.protein" type="digit" placeholder="0" />
            </view>
          </view>
          <view class="form-row">
            <view class="form-group half">
              <text class="form-label">脂肪(g)</text>
              <input class="form-input" v-model="form.fat" type="digit" placeholder="0" />
            </view>
            <view class="form-group half">
              <text class="form-label">碳水(g)</text>
              <input class="form-input" v-model="form.carbs" type="digit" placeholder="0" />
            </view>
          </view>
          <view class="form-group">
            <text class="form-label">标签（逗号分隔）</text>
            <input class="form-input" v-model="form.tags" placeholder="低脂,高蛋白" />
          </view>
          <view class="form-group">
            <text class="form-label">图片</text>
            <view class="image-upload-area" @tap="chooseImage">
              <image v-if="form.imageUrl" class="upload-preview" :src="form.imageUrl" mode="aspectFill" />
              <view v-else class="upload-placeholder">
                <text class="upload-icon">📷</text>
                <text class="upload-text">拍照/相册上传</text>
              </view>
            </view>
            <view v-if="uploading" class="upload-progress">
              <text class="upload-progress-text">上传中...</text>
            </view>
          </view>
          <view class="form-group">
            <text class="form-label">过敏原信息</text>
            <input class="form-input" v-model="form.allergenInfo" placeholder="如：含坚果" />
          </view>
          <view class="form-group">
            <text class="form-label">食材</text>
            <textarea class="form-textarea" v-model="form.ingredients" placeholder="食材列表" auto-height />
          </view>
          <view class="form-group toggle-row">
            <text class="form-label">上架状态</text>
            <switch :checked="form.isAvailable" @change="form.isAvailable = ($event.detail as any).value" color="#10B981" />
          </view>
          <button class="btn-primary" :loading="saving" @tap="saveMeal">{{ editingId ? '保存修改' : '添加营养餐' }}</button>
        </scroll-view>
      </view>
    </view>

    <view class="safe-bottom" />
  </view>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()

import { adminApi } from '@/services/api'

const loading = ref(false)
const saving = ref(false)
const uploading = ref(false)
const showForm = ref(false)
const editingId = ref<number | null>(null)
const meals = ref<any[]>([])

const defaultForm = {
  name: '', description: '', category: '', price: '',
  calories: '', protein: '', fat: '', carbs: '',
  tags: '', imageUrl: '', allergenInfo: '', ingredients: '',
  isAvailable: true
}
const form = reactive({ ...defaultForm })

function checkPermission() {
  if (!userStore.isAdmin) {
    uni.showToast({ title: '无管理员权限', icon: 'none' })
    setTimeout(() => uni.navigateBack(), 300)
    return false
  }
  return true
}

async function loadMeals() {
  loading.value = true
  try {
    const res = await adminApi.getMeals()
    if (res.code === 200) {
      const data = res.data as any
      meals.value = data?.content || data?.records || data?.list || (Array.isArray(data) ? data : [])
    }
  } catch {} finally {
    loading.value = false
  }
}

function openForm(meal?: any) {
  if (meal) {
    editingId.value = meal.id
    Object.assign(form, {
      name: meal.name || '',
      description: meal.description || '',
      category: meal.category || '',
      price: String(meal.price || ''),
      calories: String(meal.calories || ''),
      protein: String(meal.protein || ''),
      fat: String(meal.fat || ''),
      carbs: String(meal.carbs || ''),
      tags: Array.isArray(meal.tags) ? meal.tags.join(',') : (meal.tags || ''),
      imageUrl: meal.imageUrl || '',
      allergenInfo: meal.allergenInfo || '',
      ingredients: meal.ingredients || '',
      isAvailable: meal.isAvailable !== false
    })
  } else {
    editingId.value = null
    Object.assign(form, defaultForm)
  }
  showForm.value = true
}

async function chooseImage() {
  uni.chooseImage({
    count: 1,
    sizeType: ['compressed'],
    sourceType: ['album', 'camera'],
    success: async (res) => {
      const tempFilePath = res.tempFilePaths[0]
      uploading.value = true
      try {
        const uploadRes = await adminApi.uploadImage(tempFilePath)
        if (uploadRes.code === 200 && uploadRes.data) {
          form.imageUrl = (uploadRes.data as any).url || (uploadRes.data as any)
          uni.showToast({ title: '上传成功', icon: 'success' })
        } else {
          uni.showToast({ title: '上传失败', icon: 'none' })
        }
      } catch {
        uni.showToast({ title: '上传失败', icon: 'none' })
      } finally {
        uploading.value = false
      }
    }
  })
}

async function saveMeal() {
  if (!form.name.trim()) return uni.showToast({ title: '请输入名称', icon: 'none' })
  if (!form.price) return uni.showToast({ title: '请输入价格', icon: 'none' })

  saving.value = true
  try {
    const payload: any = {
      name: form.name.trim(),
      description: form.description.trim(),
      category: form.category.trim(),
      price: parseFloat(form.price) || 0,
      calories: parseInt(form.calories) || 0,
      protein: parseFloat(form.protein) || 0,
      fat: parseFloat(form.fat) || 0,
      carbs: parseFloat(form.carbs) || 0,
      tags: form.tags ? form.tags.split(',').map((t: string) => t.trim()).filter(Boolean) : [],
      imageUrl: form.imageUrl.trim(),
      allergenInfo: form.allergenInfo.trim(),
      ingredients: form.ingredients.trim(),
      isAvailable: form.isAvailable
    }

    let res: any
    if (editingId.value) {
      res = await adminApi.updateMeal(editingId.value, payload)
    } else {
      res = await adminApi.createMeal(payload)
    }

    if (res.code === 200) {
      uni.showToast({ title: editingId.value ? '修改成功' : '添加成功', icon: 'success' })
      showForm.value = false
      loadMeals()
    } else {
      uni.showToast({ title: (res as any).message || '操作失败', icon: 'none' })
    }
  } catch (e: any) {
    uni.showToast({ title: e?.message || '操作失败', icon: 'none' })
  } finally {
    saving.value = false
  }
}

async function toggleStatus(meal: any) {
  try {
    const newStatus = !meal.isAvailable
    const res = await adminApi.toggleMealStatus(meal.id, { isAvailable: newStatus })
    if (res.code === 200) {
      meal.isAvailable = newStatus
      uni.showToast({ title: newStatus ? '已上架' : '已下架', icon: 'success' })
    }
  } catch {}
}

function handleDelete(meal: any) {
  uni.showModal({
    title: '确认删除',
    content: `确定要删除「${meal.name}」吗？`,
    success: async ({ confirm }) => {
      if (!confirm) return
      try {
        const res = await adminApi.deleteMeal(meal.id)
        if (res.code === 200) {
          uni.showToast({ title: '已删除', icon: 'success' })
          loadMeals()
        }
      } catch {}
    }
  })
}

onShow(() => {
  if (checkPermission()) loadMeals()
})
</script>

<style scoped lang="scss">
.page {
  min-height: 100vh;
  background: $background;
}

.top-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 24rpx 32rpx;
  background: $card;
  border-bottom: 1rpx solid $border;
  position: sticky;
  top: 0;
  z-index: 10;
}

.top-title {
  font-size: 34rpx;
  font-weight: 700;
  color: $foreground;
}

.add-btn {
  padding: 12rpx 28rpx;
  background: $gradient-accent;
  color: #fff;
  font-size: 26rpx;
  font-weight: 600;
  border-radius: $radius-full;
}

.loading-wrap, .empty-wrap {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 120rpx 0;
  gap: 16rpx;
}

.loading-text, .empty-text {
  font-size: 28rpx;
  color: $muted-foreground;
}

.empty-icon {
  font-size: 80rpx;
}

.list {
  padding: 24rpx 32rpx;
  display: flex;
  flex-direction: column;
  gap: 20rpx;
}

.item-card {
  background: $card;
  border-radius: $radius-xl;
  padding: 28rpx;
  box-shadow: $shadow-sm;
}

.item-row {
  display: flex;
  gap: 20rpx;
  align-items: flex-start;
}

.item-img {
  width: 120rpx;
  height: 120rpx;
  border-radius: $radius-lg;
  flex-shrink: 0;
  background: $muted;
}

.item-img-placeholder {
  width: 120rpx;
  height: 120rpx;
  border-radius: $radius-lg;
  flex-shrink: 0;
  background: $muted;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 48rpx;
}

.item-info {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 8rpx;
}

.item-name-row {
  display: flex;
  align-items: center;
  gap: 12rpx;
}

.item-name {
  font-size: 30rpx;
  font-weight: 600;
  color: $foreground;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.status-tag {
  font-size: 22rpx;
  padding: 4rpx 14rpx;
  border-radius: $radius-full;
  flex-shrink: 0;

  &.on {
    background: rgba(16, 185, 129, 0.1);
    color: $accent;
  }

  &.off {
    background: rgba(239, 68, 68, 0.1);
    color: #EF4444;
  }
}

.item-meta {
  font-size: 26rpx;
  color: $accent;
  font-weight: 500;
}

.item-desc {
  font-size: 24rpx;
  color: $muted-foreground;
}

.item-actions {
  display: flex;
  gap: 16rpx;
  margin-top: 20rpx;
  padding-top: 20rpx;
  border-top: 1rpx solid $border;
}

.act-btn {
  flex: 1;
  text-align: center;
  padding: 14rpx 0;
  font-size: 26rpx;
  font-weight: 500;
  color: $accent;
  background: rgba(16, 185, 129, 0.06);
  border-radius: $radius-md;

  &.danger {
    color: #EF4444;
    background: rgba(239, 68, 68, 0.06);
  }

  &:active {
    opacity: 0.7;
  }
}

// Sheet
.sheet-mask {
  position: fixed;
  top: 0; left: 0; right: 0; bottom: 0;
  background: rgba(15, 23, 42, 0.5);
  z-index: 999;
  display: flex;
  align-items: flex-end;
}

.sheet {
  width: 100%;
  max-width: 750rpx;
  max-height: 85vh;
  background: $card;
  border-radius: $radius-2xl $radius-2xl 0 0;
  display: flex;
  flex-direction: column;
  box-sizing: border-box;
}

.sheet-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 32rpx;
  border-bottom: 1rpx solid $border;
}

.sheet-title {
  font-size: 32rpx;
  font-weight: 700;
  color: $foreground;
}

.sheet-close {
  font-size: 36rpx;
  color: $muted-foreground;
  padding: 8rpx;
}

.sheet-body {
  flex: 1;
  padding: 32rpx;
  max-height: 70vh;
  box-sizing: border-box;
}

.form-group {
  margin-bottom: 28rpx;
}

.form-label {
  display: block;
  font-size: 26rpx;
  font-weight: 500;
  color: $foreground;
  margin-bottom: 12rpx;
}

.form-input {
  width: 100%;
  height: 80rpx;
  padding: 0 24rpx;
  background: $muted;
  border: 1rpx solid $border;
  border-radius: $radius-md;
  font-size: 28rpx;
  color: $foreground;
  box-sizing: border-box;
}

.form-textarea {
  width: 100%;
  min-height: 120rpx;
  padding: 20rpx 24rpx;
  background: $muted;
  border: 1rpx solid $border;
  border-radius: $radius-md;
  font-size: 28rpx;
  color: $foreground;
  box-sizing: border-box;
}

.form-row {
  display: flex;
  gap: 20rpx;
}

.form-group.half {
  flex: 1;
}

.toggle-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  .form-label { margin-bottom: 0; }
}

.btn-primary {
  width: 100%;
  height: 88rpx;
  background: $gradient-accent;
  color: #fff;
  font-size: 30rpx;
  font-weight: 600;
  border-radius: $radius-lg;
  border: none;
  margin-top: 16rpx;

  &:active {
    opacity: 0.85;
  }
}

.safe-bottom {
  height: env(safe-area-inset-bottom);
  padding-bottom: 40rpx;
}

.image-upload-area {
  width: 200rpx;
  height: 200rpx;
  border-radius: $radius-lg;
  border: 2rpx dashed $border;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  background: $muted;
}

.upload-preview {
  width: 100%;
  height: 100%;
}

.upload-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8rpx;
}

.upload-icon { font-size: 48rpx; }
.upload-text { font-size: 22rpx; color: $muted-foreground; }

.upload-progress {
  margin-top: 8rpx;
}

.upload-progress-text {
  font-size: 22rpx;
  color: $accent;
}
</style>
