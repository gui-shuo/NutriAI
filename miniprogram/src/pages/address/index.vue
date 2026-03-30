<template>
  <view class="address-page">
    <view class="address-list" v-if="addresses.length > 0">
      <view
        class="address-card"
        :class="{ selected: selectMode && selectedId === addr.id }"
        v-for="addr in addresses"
        :key="addr.id"
        @tap="selectMode ? selectAddress(addr) : null"
      >
        <view class="address-main">
          <view class="address-top">
            <text class="receiver-name">{{ addr.receiverName }}</text>
            <text class="receiver-phone">{{ addr.receiverPhone }}</text>
            <view class="default-tag" v-if="addr.isDefault">默认</view>
          </view>
          <view class="address-detail">
            <text v-if="addr.label" class="address-label">{{ addr.label }}</text>
            <text class="address-text">{{ getFullAddress(addr) }}</text>
          </view>
        </view>
        <view class="address-actions">
          <view class="action-btn" @tap.stop="setDefault(addr)" v-if="!addr.isDefault">
            <text class="action-icon">⭐</text>
            <text class="action-text">设为默认</text>
          </view>
          <view class="action-btn" @tap.stop="editAddress(addr)">
            <text class="action-icon">✏️</text>
            <text class="action-text">编辑</text>
          </view>
          <view class="action-btn danger" @tap.stop="deleteAddress(addr)">
            <text class="action-icon">🗑️</text>
            <text class="action-text">删除</text>
          </view>
        </view>
      </view>
    </view>

    <view class="empty-state" v-else-if="!loading">
      <text class="empty-icon">📍</text>
      <text class="empty-text">暂无收货地址</text>
      <text class="empty-hint">添加一个收货地址，方便购物时使用</text>
    </view>

    <view class="loading-state" v-if="loading">
      <text>加载中...</text>
    </view>

    <view class="add-btn-wrap">
      <view class="add-btn" @tap="openAddForm">+ 新增收货地址</view>
    </view>

    <!-- Add/Edit Modal -->
    <view class="modal-mask" v-if="showForm" @tap="showForm = false">
      <view class="modal-content" @tap.stop>
        <view class="modal-header">
          <text class="modal-title">{{ editingId ? '编辑地址' : '新增地址' }}</text>
          <text class="modal-close" @tap="showForm = false">✕</text>
        </view>
        <scroll-view class="modal-body" scroll-y>
          <view class="form-group">
            <text class="form-label">收货人 <text class="required">*</text></text>
            <input class="form-input" v-model="form.receiverName" placeholder="请输入收货人姓名" maxlength="20" />
          </view>
          <view class="form-group">
            <text class="form-label">手机号 <text class="required">*</text></text>
            <input class="form-input" v-model="form.receiverPhone" placeholder="请输入联系电话" type="number" maxlength="11" />
          </view>
          <view class="form-group">
            <text class="form-label">所在地区</text>
            <view class="region-picker" @tap="openRegionPicker">
              <text :class="{ 'placeholder-text': !regionText }">{{ regionText || '选择省/市/区' }}</text>
              <text class="arrow">›</text>
            </view>
          </view>
          <view class="form-group">
            <text class="form-label">详细地址 <text class="required">*</text></text>
            <textarea class="form-textarea" v-model="form.detailAddress" placeholder="街道、楼牌号等" maxlength="200" />
          </view>
          <view class="form-group">
            <text class="form-label">标签</text>
            <view class="label-tags">
              <view
                class="label-tag"
                :class="{ active: form.label === tag }"
                v-for="tag in labelOptions"
                :key="tag"
                @tap="form.label = form.label === tag ? '' : tag"
              >{{ tag }}</view>
            </view>
          </view>
          <view class="form-group switch-group">
            <text class="form-label">设为默认地址</text>
            <switch :checked="form.isDefault" @change="form.isDefault = $event.detail.value" color="#07c160" />
          </view>
        </scroll-view>
        <view class="modal-footer">
          <view class="save-btn" @tap="saveAddress">保存</view>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { onLoad, onShow } from '@dcloudio/uni-app'
import { addressApi } from '@/services/api'

interface AddressItem {
  id: number
  receiverName: string
  receiverPhone: string
  province: string
  city: string
  district: string
  detailAddress: string
  label: string
  isDefault: boolean
}

const addresses = ref<AddressItem[]>([])
const loading = ref(false)
const showForm = ref(false)
const editingId = ref<number | null>(null)
const selectMode = ref(false)
const selectedId = ref<number | null>(null)
const regionText = ref('')

const labelOptions = ['家', '公司', '学校', '其他']

const form = ref({
  receiverName: '',
  receiverPhone: '',
  province: '',
  city: '',
  district: '',
  detailAddress: '',
  label: '',
  isDefault: false
})

function getFullAddress(addr: AddressItem): string {
  let s = ''
  if (addr.province) s += addr.province
  if (addr.city) s += addr.city
  if (addr.district) s += addr.district
  s += addr.detailAddress
  return s
}

async function loadAddresses() {
  loading.value = true
  try {
    const res = await addressApi.getList()
    addresses.value = res.data || []
  } catch {
    uni.showToast({ title: '加载失败', icon: 'none' })
  } finally {
    loading.value = false
  }
}

function openAddForm() {
  editingId.value = null
  form.value = { receiverName: '', receiverPhone: '', province: '', city: '', district: '', detailAddress: '', label: '', isDefault: false }
  regionText.value = ''
  showForm.value = true
}

function editAddress(addr: AddressItem) {
  editingId.value = addr.id
  form.value = {
    receiverName: addr.receiverName,
    receiverPhone: addr.receiverPhone,
    province: addr.province || '',
    city: addr.city || '',
    district: addr.district || '',
    detailAddress: addr.detailAddress,
    label: addr.label || '',
    isDefault: addr.isDefault
  }
  regionText.value = [addr.province, addr.city, addr.district].filter(Boolean).join(' ')
  showForm.value = true
}

function openRegionPicker() {
  // @ts-ignore
  uni.chooseLocation?.({
    success: () => {},
    fail: () => {}
  })
  // Use built-in region picker as fallback
  // We provide a manual text approach since uni.chooseLocation requires map SDK
}

function selectAddress(addr: AddressItem) {
  const pages = getCurrentPages()
  const prevPage = pages[pages.length - 2] as any
  if (prevPage) {
    uni.$emit('address-selected', addr)
  }
  uni.navigateBack()
}

async function saveAddress() {
  if (!form.value.receiverName.trim()) {
    return uni.showToast({ title: '请输入收货人姓名', icon: 'none' })
  }
  if (!form.value.receiverPhone.trim() || form.value.receiverPhone.length < 11) {
    return uni.showToast({ title: '请输入正确的手机号', icon: 'none' })
  }
  if (!form.value.detailAddress.trim()) {
    return uni.showToast({ title: '请输入详细地址', icon: 'none' })
  }

  uni.showLoading({ title: '保存中...' })
  try {
    if (editingId.value) {
      await addressApi.update(editingId.value, form.value)
    } else {
      await addressApi.add(form.value)
    }
    uni.showToast({ title: '保存成功', icon: 'success' })
    showForm.value = false
    loadAddresses()
  } catch (e: any) {
    uni.showToast({ title: e?.message || '保存失败', icon: 'none' })
  } finally {
    uni.hideLoading()
  }
}

async function setDefault(addr: AddressItem) {
  try {
    await addressApi.setDefault(addr.id)
    uni.showToast({ title: '已设为默认', icon: 'success' })
    loadAddresses()
  } catch {
    uni.showToast({ title: '设置失败', icon: 'none' })
  }
}

async function deleteAddress(addr: AddressItem) {
  uni.showModal({
    title: '确认删除',
    content: `确定删除"${addr.receiverName}"的收货地址吗？`,
    success: async (res) => {
      if (res.confirm) {
        try {
          await addressApi.remove(addr.id)
          uni.showToast({ title: '已删除', icon: 'success' })
          loadAddresses()
        } catch {
          uni.showToast({ title: '删除失败', icon: 'none' })
        }
      }
    }
  })
}

onLoad((query) => {
  if (query?.select === '1') {
    selectMode.value = true
  }
})

onShow(() => {
  loadAddresses()
})
</script>

<style scoped>
.address-page {
  min-height: 100vh;
  background: #f5f5f5;
  padding-bottom: 140rpx;
}

.address-card {
  background: #fff;
  margin: 16rpx 24rpx;
  border-radius: 16rpx;
  padding: 28rpx;
  box-shadow: 0 2rpx 8rpx rgba(0,0,0,0.04);
}

.address-card.selected {
  border: 2rpx solid #07c160;
}

.address-top {
  display: flex;
  align-items: center;
  gap: 16rpx;
  margin-bottom: 12rpx;
}

.receiver-name {
  font-size: 32rpx;
  font-weight: 600;
  color: #333;
}

.receiver-phone {
  font-size: 28rpx;
  color: #666;
}

.default-tag {
  font-size: 20rpx;
  color: #fff;
  background: #07c160;
  padding: 4rpx 12rpx;
  border-radius: 8rpx;
}

.address-detail {
  display: flex;
  align-items: flex-start;
  gap: 8rpx;
}

.address-label {
  font-size: 22rpx;
  color: #07c160;
  border: 1rpx solid #07c160;
  padding: 2rpx 10rpx;
  border-radius: 6rpx;
  flex-shrink: 0;
  margin-top: 4rpx;
}

.address-text {
  font-size: 26rpx;
  color: #666;
  line-height: 1.5;
}

.address-actions {
  display: flex;
  gap: 24rpx;
  margin-top: 20rpx;
  padding-top: 20rpx;
  border-top: 1rpx solid #f0f0f0;
}

.action-btn {
  display: flex;
  align-items: center;
  gap: 6rpx;
}

.action-icon {
  font-size: 24rpx;
}

.action-text {
  font-size: 24rpx;
  color: #666;
}

.action-btn.danger .action-text {
  color: #ee0a24;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding-top: 200rpx;
}

.empty-icon {
  font-size: 80rpx;
  margin-bottom: 20rpx;
}

.empty-text {
  font-size: 30rpx;
  color: #333;
  margin-bottom: 8rpx;
}

.empty-hint {
  font-size: 24rpx;
  color: #999;
}

.loading-state {
  text-align: center;
  padding: 40rpx;
  font-size: 28rpx;
  color: #999;
}

.add-btn-wrap {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  padding: 20rpx 28rpx;
  padding-bottom: calc(20rpx + env(safe-area-inset-bottom));
  background: #fff;
  border-top: 1rpx solid #eee;
}

.add-btn {
  text-align: center;
  padding: 24rpx;
  background: linear-gradient(135deg, #07c160, #06ad56);
  color: #fff;
  font-size: 30rpx;
  font-weight: 600;
  border-radius: 44rpx;
}

/* Modal */
.modal-mask {
  position: fixed;
  top: 0; left: 0; right: 0; bottom: 0;
  background: rgba(0,0,0,0.5);
  z-index: 1000;
  display: flex;
  align-items: flex-end;
}

.modal-content {
  width: 100%;
  max-height: 90vh;
  background: #fff;
  border-radius: 24rpx 24rpx 0 0;
  display: flex;
  flex-direction: column;
}

.modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 28rpx;
  border-bottom: 1rpx solid #eee;
}

.modal-title {
  font-size: 32rpx;
  font-weight: 600;
  color: #333;
}

.modal-close {
  font-size: 36rpx;
  color: #999;
  padding: 8rpx;
}

.modal-body {
  flex: 1;
  padding: 28rpx;
  max-height: 65vh;
}

.form-group {
  margin-bottom: 24rpx;
}

.form-label {
  font-size: 28rpx;
  font-weight: 500;
  color: #333;
  margin-bottom: 12rpx;
  display: block;
}

.required {
  color: #ee0a24;
}

.form-input {
  width: 100%;
  height: 80rpx;
  border: 2rpx solid #eee;
  border-radius: 12rpx;
  padding: 0 24rpx;
  font-size: 28rpx;
}

.form-textarea {
  width: 100%;
  height: 160rpx;
  border: 2rpx solid #eee;
  border-radius: 12rpx;
  padding: 16rpx 24rpx;
  font-size: 28rpx;
}

.region-picker {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 80rpx;
  border: 2rpx solid #eee;
  border-radius: 12rpx;
  padding: 0 24rpx;
  font-size: 28rpx;
  color: #333;
}

.placeholder-text {
  color: #c0c4cc;
}

.arrow {
  font-size: 32rpx;
  color: #c0c4cc;
}

.label-tags {
  display: flex;
  gap: 16rpx;
  flex-wrap: wrap;
}

.label-tag {
  padding: 10rpx 28rpx;
  border: 2rpx solid #eee;
  border-radius: 28rpx;
  font-size: 26rpx;
  color: #666;
}

.label-tag.active {
  border-color: #07c160;
  color: #07c160;
  background: rgba(7, 193, 96, 0.06);
}

.switch-group {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.switch-group .form-label {
  margin-bottom: 0;
}

.modal-footer {
  padding: 20rpx 28rpx;
  padding-bottom: calc(20rpx + env(safe-area-inset-bottom));
  border-top: 1rpx solid #eee;
}

.save-btn {
  text-align: center;
  padding: 24rpx;
  background: linear-gradient(135deg, #07c160, #06ad56);
  color: #fff;
  font-size: 30rpx;
  font-weight: 600;
  border-radius: 44rpx;
}
</style>
