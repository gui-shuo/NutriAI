<script setup>
/**
 * 门店选择页
 * 通过 wx.getLocation 获取用户经纬度，调用后端接口按距离排序返回最近门店
 */
import { ref, onMounted } from 'vue'
import NavBar from '../../components/NavBar.vue'
import { merchantApi } from '../../services/api'
import { useLocation } from '../../composables/useLocation'
import { cosUrl } from '../../utils/common'

const { latitude, longitude, loading: locLoading, authorized, getLocation } = useLocation()

const stores = ref([])
const loading = ref(false)
const locFailed = ref(false)

onMounted(async () => {
  await loadStores()
})

async function loadStores() {
  loading.value = true
  locFailed.value = false
  try {
    // 先获取用户位置
    const loc = await getLocation()
    if (loc) {
      // 按距离排序获取附近门店
      const res = await merchantApi.getNearby(loc.latitude, loc.longitude, 20)
      stores.value = Array.isArray(res) ? res : []
    } else {
      // 定位失败，获取所有营业门店（不排序）
      locFailed.value = true
      const res = await merchantApi.getActive()
      stores.value = Array.isArray(res) ? res : []
    }
  } catch (e) {
    console.error('加载门店失败', e)
    stores.value = []
  } finally {
    loading.value = false
  }
}

/** 重新定位 */
async function retryLocation() {
  await loadStores()
}

/** 选择门店，返回上一页并传参 */
function selectStore(store) {
  const pages = getCurrentPages()
  if (pages.length >= 2) {
    const prevPage = pages[pages.length - 2]
    // 通过 eventChannel 传递选中的门店
    const eventChannel = prevPage.getOpenerEventChannel?.()
  }
  // 使用全局事件传递
  uni.$emit('store-selected', {
    id: store.id,
    name: store.name,
    address: store.address,
    distance: store.distance,
    distanceText: store.distanceText,
    phone: store.phone,
    businessHours: store.businessHours,
  })
  uni.navigateBack()
}
</script>

<template>
  <view class="page">
    <NavBar showBack title="选择门店" />

    <scroll-view scroll-y class="content" :enhanced="true" :show-scrollbar="false">
      <!-- 定位状态栏 -->
      <view class="loc-bar" @tap="retryLocation">
        <view class="loc-bar__left">
          <u-icon :name="authorized ? 'map-fill' : 'map'" size="18" :color="authorized ? '#0a6e2c' : '#999'" />
          <text class="loc-bar__text" v-if="locLoading">定位中...</text>
          <text class="loc-bar__text" v-else-if="authorized">已定位·按距离排序</text>
          <text class="loc-bar__text loc-bar__text--warn" v-else>定位失败，点击重试</text>
        </view>
        <u-icon name="reload" size="16" color="#999" v-if="!locLoading" />
        <u-loading-icon v-else mode="circle" size="16" color="#0a6e2c" />
      </view>

      <!-- 加载状态 -->
      <view v-if="loading" class="state-tip">
        <u-loading-icon mode="circle" size="28" color="#0a6e2c" />
        <text style="margin-top: 16rpx; color: #999;">加载门店中...</text>
      </view>

      <!-- 空状态 -->
      <u-empty v-else-if="stores.length === 0" text="暂无可用门店" icon="map" mode="list" marginTop="100" />

      <!-- 门店列表 -->
      <view v-else class="store-list">
        <view
          v-for="store in stores"
          :key="store.id"
          class="store-card"
          @tap="selectStore(store)"
        >
          <view class="store-card__header">
            <view class="store-card__logo">
              <u-image
                v-if="store.logo"
                :src="cosUrl(store.logo)"
                width="48"
                height="48"
                mode="aspectFill"
                radius="8"
              />
              <view v-else class="store-card__logo-default">
                <u-icon name="home" size="24" color="#0a6e2c" />
              </view>
            </view>
            <view class="store-card__info">
              <text class="store-card__name">{{ store.name }}</text>
              <text class="store-card__address">{{ store.address }}</text>
            </view>
            <view v-if="store.distanceText" class="store-card__distance">
              <u-icon name="map" size="14" color="#0a6e2c" />
              <text class="store-card__dist-text">{{ store.distanceText }}</text>
            </view>
          </view>

          <view class="store-card__footer">
            <view class="store-card__tag" v-if="store.businessHours">
              <u-icon name="clock" size="13" color="#666" />
              <text class="store-card__tag-text">{{ store.businessHours }}</text>
            </view>
            <view class="store-card__tag" v-if="store.phone">
              <u-icon name="phone" size="13" color="#666" />
              <text class="store-card__tag-text">{{ store.phone }}</text>
            </view>
          </view>
        </view>
      </view>

      <view style="height: 40rpx;" />
    </scroll-view>
  </view>
</template>

<style lang="scss" scoped>
@import '../../styles/design-system.scss';

.page {
  min-height: 100vh;
  background: #f5f5f5;
  overflow-x: hidden;
  width: 100%;
}

.content {
  height: calc(100vh - 88px);
}

// 定位状态栏
.loc-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20rpx 28rpx;
  margin: 16rpx 24rpx;
  background: #ffffff;
  border-radius: 12px;
  box-shadow: 0 2rpx 12rpx rgba(0, 0, 0, 0.04);

  &__left {
    display: flex;
    align-items: center;
    gap: 12rpx;
  }

  &__text {
    font-size: 26rpx;
    color: #333;

    &--warn {
      color: #e65100;
    }
  }
}

// 状态提示
.state-tip {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 120rpx 0;
}

// 门店列表
.store-list {
  padding: 0 24rpx;
}

.store-card {
  background: #ffffff;
  border-radius: 16px;
  padding: 28rpx;
  margin-bottom: 16rpx;
  box-shadow: 0 2rpx 16rpx rgba(0, 0, 0, 0.05);
  transition: transform 0.15s;

  &:active {
    transform: scale(0.98);
  }

  &__header {
    display: flex;
    align-items: flex-start;
    gap: 20rpx;
  }

  &__logo {
    flex-shrink: 0;
  }

  &__logo-default {
    width: 96rpx;
    height: 96rpx;
    background: rgba($primary, 0.08);
    border-radius: 16rpx;
    display: flex;
    align-items: center;
    justify-content: center;
  }

  &__info {
    flex: 1;
    min-width: 0;
  }

  &__name {
    display: block;
    font-size: 30rpx;
    font-weight: 700;
    color: #1a1c1a;
    margin-bottom: 6rpx;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  &__address {
    display: block;
    font-size: 24rpx;
    color: #888;
    line-height: 1.4;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  &__distance {
    flex-shrink: 0;
    display: flex;
    align-items: center;
    gap: 4rpx;
    background: rgba($primary, 0.08);
    padding: 6rpx 14rpx;
    border-radius: 20rpx;
  }

  &__dist-text {
    font-size: 24rpx;
    color: $primary;
    font-weight: 600;
  }

  &__footer {
    display: flex;
    gap: 24rpx;
    margin-top: 16rpx;
    padding-top: 16rpx;
    border-top: 1rpx solid rgba(0, 0, 0, 0.04);
  }

  &__tag {
    display: flex;
    align-items: center;
    gap: 6rpx;

    &-text {
      font-size: 22rpx;
      color: #666;
    }
  }
}
</style>
