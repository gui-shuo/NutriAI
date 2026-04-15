<template>
  <view class="address-page">
    <scroll-view scroll-y class="address-list">
      <view v-for="addr in addresses" :key="addr.id" class="address-card card" @tap="selectOrEdit(addr)">
        <view class="addr-top">
          <text class="text-bold">{{ addr.receiverName }}</text>
          <text class="text-secondary ml-sm">{{ addr.receiverPhone }}</text>
          <view v-if="addr.isDefault" class="badge-primary ml-sm">默认</view>
        </view>
        <text class="addr-detail text-sm text-secondary mt-sm">
          {{ addr.province }}{{ addr.city }}{{ addr.district }}{{ addr.detailAddress }}
        </text>
        <view v-if="addr.label" class="mt-sm">
          <text class="tag">{{ addr.label }}</text>
        </view>

        <view v-if="!isSelectMode" class="addr-actions mt-md">
          <view class="action-group">
            <view class="addr-action" @tap.stop="setDefault(addr)" v-if="!addr.isDefault">
              <text class="text-sm text-primary">设为默认</text>
            </view>
            <view class="addr-action" @tap.stop="editAddr(addr)">
              <text class="text-sm text-secondary">编辑</text>
            </view>
            <view class="addr-action" @tap.stop="deleteAddr(addr)">
              <text class="text-sm text-error">删除</text>
            </view>
          </view>
        </view>
      </view>

      <view v-if="!addresses.length && !loading" class="empty-state">
        <text class="empty-icon">📍</text>
        <text class="empty-text">暂无收货地址</text>
      </view>
    </scroll-view>

    <view class="bottom-action safe-bottom">
      <button class="btn-primary btn-block btn-lg" @tap="addAddress">
        + 新增收货地址
      </button>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { onLoad, onShow } from '@dcloudio/uni-app'
import { addressApi } from '../../services/api'
import { showConfirm } from '../../utils'

const addresses = ref<any[]>([])
const loading = ref(false)
const isSelectMode = ref(false)
let eventChannel: any = null

onLoad((query: any) => {
  if (query?.select === '1') {
    isSelectMode.value = true
  }
})

onShow(() => {
  loadAddresses()
})

// Get the event channel for passing data back
const instance = getCurrentPages()
if (instance.length > 0) {
  const currentPage = instance[instance.length - 1] as any
  eventChannel = currentPage?.getOpenerEventChannel?.()
}

async function loadAddresses() {
  loading.value = true
  try {
    const data: any = await addressApi.getList()
    addresses.value = data?.content || data || []
  } catch {}
  loading.value = false
}

function selectOrEdit(addr: any) {
  if (isSelectMode.value) {
    // Pass selected address back to order confirm page
    const pages = getCurrentPages()
    const prevPage = pages[pages.length - 2] as any
    if (prevPage) {
      const channel = (pages[pages.length - 1] as any)?.getOpenerEventChannel?.()
      channel?.emit('selectAddress', addr)
    }
    uni.navigateBack()
  } else {
    editAddr(addr)
  }
}

function addAddress() {
  uni.navigateTo({ url: '/pages/address/edit' })
}

function editAddr(addr: any) {
  uni.navigateTo({ url: `/pages/address/edit?id=${addr.id}` })
}

async function setDefault(addr: any) {
  try {
    await addressApi.setDefault(addr.id)
    uni.showToast({ title: '已设为默认', icon: 'success' })
    loadAddresses()
  } catch {}
}

async function deleteAddr(addr: any) {
  const ok = await showConfirm('确定删除此地址？')
  if (!ok) return
  try {
    await addressApi.remove(addr.id)
    uni.showToast({ title: '已删除', icon: 'success' })
    loadAddresses()
  } catch {}
}
</script>

<style lang="scss" scoped>
.address-page {
  min-height: 100vh;
  background: $bg-page;
  padding-bottom: 140rpx;
}

.address-list {
  padding: $spacing-md $spacing-lg;
}

.address-card {
  padding: $spacing-lg;
  margin-bottom: $spacing-md;
}

.addr-top {
  display: flex;
  align-items: center;
}

.addr-detail { display: block; }

.addr-actions {
  display: flex;
  justify-content: flex-end;
  border-top: 1rpx solid $border-light;
  padding-top: $spacing-md;
}

.action-group { display: flex; gap: $spacing-lg; }
.addr-action { padding: 4rpx 0; }

.bottom-action {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  padding: $spacing-md $spacing-lg;
  background: $bg-card;
  box-shadow: 0 -2rpx 10rpx rgba(0, 0, 0, 0.05);
}

.empty-state {
  display: flex; flex-direction: column; align-items: center; padding: 120rpx 0;
}
.empty-icon { font-size: 80rpx; margin-bottom: $spacing-md; }
</style>
