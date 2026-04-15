<template>
  <view class="meals-page">
    <!-- Search Bar -->
    <view class="search-bar">
      <view class="search-input">
        <text class="search-icon">🔍</text>
        <input v-model="keyword" placeholder="搜索营养套餐" confirm-type="search" @confirm="onSearch" />
      </view>
    </view>

    <!-- Category Tabs -->
    <scroll-view scroll-x class="category-tabs">
      <view class="tabs-inner">
        <view
          v-for="cat in categories"
          :key="cat.key"
          :class="['tab-item', { active: currentCategory === cat.key }]"
          @tap="switchCategory(cat.key)"
        >
          <text>{{ cat.label }}</text>
        </view>
      </view>
    </scroll-view>

    <!-- Meal List -->
    <scroll-view
      scroll-y
      class="meal-list"
      @scrolltolower="loadMore"
      refresher-enabled
      :refresher-triggered="refreshing"
      @refresherrefresh="onRefresh"
    >
      <view class="list-content">
        <view
          v-for="item in meals"
          :key="item.id"
          class="meal-item card"
          @tap="goDetail(item.id)"
        >
          <image :src="item.imageUrl || '/static/images/meal-default.png'" mode="aspectFill" class="meal-cover" />
          <view class="meal-body">
            <text class="meal-name">{{ item.name }}</text>
            <text class="meal-desc text-line-2 text-sm text-secondary">{{ item.description }}</text>
            <view class="meal-tags" v-if="item.tags">
              <text v-for="tag in (item.tags || '').split(',')" :key="tag" class="tag">{{ tag }}</text>
            </view>
            <view class="meal-footer">
              <view>
                <text class="text-price text-lg">¥{{ item.price }}</text>
                <text v-if="item.originalPrice && item.originalPrice > item.price" class="original-price ml-sm">
                  ¥{{ item.originalPrice }}
                </text>
              </view>
              <view class="flex items-center gap-sm">
                <text class="text-xs text-muted">已售 {{ item.salesCount || 0 }}</text>
                <view class="order-btn" @tap.stop="quickOrder(item)">
                  <text>订购</text>
                </view>
              </view>
            </view>
          </view>
        </view>

        <view v-if="loading" class="loading-hint">
          <text class="text-muted">加载中...</text>
        </view>
        <view v-else-if="!meals.length" class="empty-state">
          <text class="empty-icon">🍽️</text>
          <text class="empty-text">暂无营养套餐</text>
          <text class="text-sm text-muted mt-sm">敬请期待更多抗炎营养餐上线</text>
        </view>
        <view v-else-if="noMore" class="loading-hint">
          <text class="text-muted text-sm">— 没有更多了 —</text>
        </view>
      </view>
    </scroll-view>
  </view>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { productApi, PRODUCT_CATEGORIES } from '../../services/api'
import { useUserStore } from '../../stores/user'

const userStore = useUserStore()
const keyword = ref('')
const currentCategory = ref('')
const meals = ref<any[]>([])
const loading = ref(false)
const refreshing = ref(false)
const noMore = ref(false)
const page = ref(0)
const pageSize = 10

const categories = [
  { key: '', label: '全部' },
  { key: '抗炎套餐', label: '🔥 抗炎套餐' },
  { key: '营养套餐', label: '🥗 营养套餐' },
  { key: '减脂套餐', label: '🏃 减脂套餐' },
  { key: '增肌套餐', label: '💪 增肌套餐' },
  { key: '素食套餐', label: '🌿 素食套餐' }
]

onShow(() => {
  if (!meals.value.length) {
    loadMeals(true)
  }
})

async function loadMeals(reset = false) {
  if (loading.value) return
  if (reset) {
    page.value = 0
    noMore.value = false
  }
  loading.value = true
  try {
    let data: any
    if (keyword.value) {
      data = await productApi.search(keyword.value, page.value, pageSize)
    } else {
      data = await productApi.getList({
        category: currentCategory.value || undefined,
        page: page.value,
        size: pageSize
      })
    }
    const list = data?.content || data?.records || data || []
    if (reset) {
      meals.value = list
    } else {
      meals.value = [...meals.value, ...list]
    }
    if (list.length < pageSize) {
      noMore.value = true
    }
    page.value++
  } catch {}
  loading.value = false
  refreshing.value = false
}

function switchCategory(key: string) {
  currentCategory.value = key
  loadMeals(true)
}

function onSearch() {
  loadMeals(true)
}

function onRefresh() {
  refreshing.value = true
  loadMeals(true)
}

function loadMore() {
  if (!noMore.value) loadMeals()
}

function goDetail(id: number) {
  uni.navigateTo({ url: `/pages/meals/detail?id=${id}` })
}

function quickOrder(item: any) {
  if (!userStore.isLoggedIn) {
    uni.navigateTo({ url: '/pages/auth/login' })
    return
  }
  uni.navigateTo({
    url: `/pages/order/confirm?type=meal&productId=${item.id}&name=${encodeURIComponent(item.name)}&price=${item.price}`
  })
}
</script>

<style lang="scss" scoped>
.meals-page {
  min-height: 100vh;
  background: $bg-page;
  display: flex;
  flex-direction: column;
}

.search-bar {
  padding: $spacing-md $spacing-lg;
  background: $bg-card;
}

.search-input {
  display: flex;
  align-items: center;
  background: $bg-muted;
  border-radius: $radius-full;
  padding: 0 $spacing-md;
  height: 72rpx;

  input {
    flex: 1;
    font-size: $font-base;
    margin-left: $spacing-sm;
  }
}

.search-icon {
  font-size: 28rpx;
}

.category-tabs {
  background: $bg-card;
  white-space: nowrap;
  border-bottom: 1rpx solid $border-light;
}

.tabs-inner {
  display: inline-flex;
  padding: $spacing-sm $spacing-md;
  gap: $spacing-sm;
}

.tab-item {
  display: inline-flex;
  padding: 12rpx 28rpx;
  border-radius: $radius-full;
  font-size: $font-sm;
  color: $text-secondary;
  background: $bg-muted;
  white-space: nowrap;
  flex-shrink: 0;

  &.active {
    background: $primary;
    color: #fff;
  }
}

.meal-list {
  flex: 1;
  height: 0;
}

.list-content {
  padding: $spacing-md $spacing-lg;
}

.meal-item {
  display: flex;
  gap: $spacing-md;
  padding: $spacing-md;
  margin-bottom: $spacing-md;
}

.meal-cover {
  width: 220rpx;
  height: 220rpx;
  border-radius: $radius-md;
  background: $bg-muted;
  flex-shrink: 0;
}

.meal-body {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.meal-name {
  font-size: $font-md;
  font-weight: 600;
  color: $text-primary;
}

.meal-tags {
  display: flex;
  flex-wrap: wrap;
  margin-top: $spacing-xs;
}

.meal-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: auto;
  padding-top: $spacing-sm;
}

.original-price {
  font-size: $font-xs;
  color: $text-muted;
  text-decoration: line-through;
}

.order-btn {
  padding: 8rpx 24rpx;
  background: $gradient-primary;
  color: #fff;
  font-size: $font-sm;
  border-radius: $radius-full;
}

.loading-hint {
  text-align: center;
  padding: $spacing-xl 0;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 120rpx 0;
}

.empty-icon {
  font-size: 80rpx;
  margin-bottom: $spacing-md;
}
</style>
