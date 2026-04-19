<script setup>
/**
 * 收货地址管理页
 */
import { ref, onMounted } from 'vue'
import NavBar from '../../components/NavBar.vue'
import { userApi } from '../../services/api'

const addresses = ref([])
const loading = ref(false)
const showForm = ref(false)
const form = ref({
  name: '',
  phone: '',
  province: '',
  city: '',
  district: '',
  detail: '',
  isDefault: false,
})
const editingId = ref(null)

async function fetchAddresses() {
  loading.value = true
  try {
    const res = await userApi.getAddresses()
    const list = Array.isArray(res) ? res : []
    addresses.value = list.map(a => ({
      ...a,
      name: a.receiverName || a.name,
      phone: a.receiverPhone || a.phone,
      detail: a.detailAddress || a.detail,
    }))
  } catch (e) {
    addresses.value = []
  } finally {
    loading.value = false
  }
}

function openAdd() {
  editingId.value = null
  form.value = { name: '', phone: '', province: '', city: '', district: '', detail: '', isDefault: false }
  showForm.value = true
}

function openEdit(addr) {
  editingId.value = addr.id
  form.value = { ...addr }
  showForm.value = true
}

function onRegionChange(e) {
  const [province, city, district] = e.detail.value
  form.value.province = province
  form.value.city = city
  form.value.district = district
}

async function saveAddress() {
  if (!form.value.name || !form.value.phone || !form.value.detail) {
    uni.showToast({ title: '请填写完整信息', icon: 'none' })
    return
  }
  const payload = {
    receiverName: form.value.name,
    receiverPhone: form.value.phone,
    province: form.value.province,
    city: form.value.city,
    district: form.value.district,
    detailAddress: form.value.detail,
    isDefault: form.value.isDefault,
  }
  try {
    if (editingId.value) {
      await userApi.updateAddress(editingId.value, payload)
    } else {
      await userApi.addAddress(payload)
    }
    uni.showToast({ title: '保存成功', icon: 'success' })
    showForm.value = false
    fetchAddresses()
  } catch (e) {
    uni.showToast({ title: '保存失败', icon: 'none' })
  }
}

async function deleteAddress(id) {
  uni.showModal({
    title: '提示',
    content: '确定删除该地址吗？',
    success: async (res) => {
      if (res.confirm) {
        try {
          await userApi.deleteAddress(id)
          fetchAddresses()
        } catch (e) {
          uni.showToast({ title: '删除失败', icon: 'none' })
        }
      }
    }
  })
}

onMounted(() => fetchAddresses())
</script>

<template>
  <view class="page">
    <NavBar showBack title="收货地址" />

    <scroll-view v-if="!showForm" scroll-y class="content" :enhanced="true" :show-scrollbar="false">
      <view v-if="loading" class="state-tip">
        <u-loading-icon mode="circle" size="28" color="#0a6e2c" />
        <text style="margin-top: 16rpx;">加载中...</text>
      </view>
      <u-empty v-else-if="addresses.length === 0" text="暂无收货地址" icon="map" mode="address" marginTop="80" />

      <view v-for="addr in addresses" :key="addr.id" class="addr-card">
        <view class="addr-card__main" @tap="openEdit(addr)">
          <view class="addr-card__row">
            <text class="addr-card__name">{{ addr.name }}</text>
            <text class="addr-card__phone">{{ addr.phone }}</text>
            <view v-if="addr.isDefault" class="addr-card__default">
              <text>默认</text>
            </view>
          </view>
          <text class="addr-card__detail">
            {{ addr.province }}{{ addr.city }}{{ addr.district }}{{ addr.detail }}
          </text>
        </view>
        <view class="addr-card__actions">
          <text class="addr-card__edit" @tap="openEdit(addr)">编辑</text>
          <text class="addr-card__delete" @tap="deleteAddress(addr.id)">删除</text>
        </view>
      </view>

      <view style="height: 120rpx;" />
    </scroll-view>

    <!-- 添加/编辑表单 -->
    <scroll-view v-else scroll-y class="content" :enhanced="true" :show-scrollbar="false">
      <view class="form-card">
        <view class="form-item">
          <text class="form-item__label">收货人</text>
          <input class="form-item__input" v-model="form.name" placeholder="请输入姓名" />
        </view>
        <view class="form-item">
          <text class="form-item__label">手机号</text>
          <input class="form-item__input" v-model="form.phone" placeholder="请输入手机号" type="number" maxlength="11" />
        </view>
        <view class="form-item">
          <text class="form-item__label">所在地区</text>
          <picker mode="region" @change="onRegionChange">
            <text class="form-item__value">
              {{ form.province ? `${form.province} ${form.city} ${form.district}` : '请选择' }}
            </text>
          </picker>
        </view>
        <view class="form-item form-item--col">
          <text class="form-item__label">详细地址</text>
          <textarea class="form-item__textarea" v-model="form.detail" placeholder="请输入详细地址" maxlength="200" :auto-height="true" />
        </view>
        <view class="form-item">
          <text class="form-item__label">设为默认</text>
          <switch :checked="form.isDefault" @change="(e) => form.isDefault = e.detail.value" color="#0d631b" />
        </view>
      </view>

      <view class="btn-group">
        <u-button text="取消" shape="circle" color="#f5f5f5" :customStyle="{color: '#333'}" @click="showForm = false" />
        <u-button text="保存" type="primary" shape="circle" color="#0a6e2c" @click="saveAddress" />
      </view>

      <view style="height: 60rpx;" />
    </scroll-view>

    <!-- 新增按钮 -->
    <view v-if="!showForm" class="fab">
      <u-button text="+ 新增地址" type="primary" shape="circle" color="#0a6e2c" @click="openAdd" :customStyle="{boxShadow: '0 8rpx 32rpx rgba(10, 110, 44, 0.3)'}" />
    </view>
  </view>
</template>

<style lang="scss" scoped>
@import '../../styles/design-system.scss';

.page { min-height: 100vh; background: #ffffff; overflow-x: hidden; width: 100%; }
.content { padding: 24rpx; height: calc(100vh - 100px); }

.state-tip {
  display: flex; flex-direction: column; align-items: center;
  gap: 12rpx; padding: 100rpx 0; color: $on-surface-variant;
  &__icon { font-size: 80rpx; }
}

.addr-card {
  background: $surface-container-lowest;
  border-radius: $radius-xl;
  padding: 24rpx;
  margin-bottom: 16rpx;
  box-shadow: $shadow-sm;

  &__main { margin-bottom: 16rpx; }

  &__row {
    display: flex; align-items: center; gap: 16rpx; margin-bottom: 8rpx;
  }

  &__name { font-size: $font-base; font-weight: 600; color: $on-surface; }
  &__phone { font-size: $font-sm; color: $on-surface-variant; }

  &__default {
    background: $primary-container;
    padding: 2rpx 12rpx;
    border-radius: $radius-sm;
    font-size: 20rpx;
    color: $primary;
  }

  &__detail {
    font-size: $font-sm;
    color: $on-surface-variant;
    line-height: $leading-relaxed;
  }

  &__actions {
    display: flex;
    gap: 24rpx;
    justify-content: flex-end;
    border-top: 1rpx solid $surface-container;
    padding-top: 16rpx;
  }

  &__edit { font-size: $font-sm; color: $primary; }
  &__delete { font-size: $font-sm; color: $error; }
}

.form-card {
  background: $surface-container-lowest;
  border-radius: $radius-xl;
  padding: 8rpx 24rpx;
  margin-bottom: 32rpx;
}

.form-item {
  display: flex; align-items: center; padding: 28rpx 0;
  & + & { border-top: 1rpx solid $surface-container; }
  &--col { flex-direction: column; align-items: flex-start; gap: 12rpx; }
  &__label { font-size: $font-base; color: $on-surface; font-weight: 500; width: 160rpx; flex-shrink: 0; }
  &__input { flex: 1; font-size: $font-base; color: $on-surface; text-align: right; }
  &__value { flex: 1; font-size: $font-base; color: $on-surface-variant; text-align: right; }
  &__textarea { width: 100%; font-size: $font-base; color: $on-surface; background: $surface-container-low; border-radius: $radius-lg; padding: 16rpx; box-sizing: border-box; }
}

.btn-group {
  display: flex; gap: 16rpx;
}

.btn {
  flex: 1; padding: 24rpx; text-align: center; border-radius: $radius-xl; font-size: $font-base; font-weight: 600;
  &--primary { background: $primary; color: $on-primary; }
  &--outline { background: $surface-container-low; color: $on-surface; }
  &:active { transform: scale(0.98); }
}

.fab {
  position: fixed;
  bottom: 40rpx;
  left: 24rpx;
  right: 24rpx;
  padding: 24rpx;
  background: $primary;
  color: $on-primary;
  text-align: center;
  border-radius: $radius-xl;
  font-size: $font-base;
  font-weight: 600;
  box-shadow: $shadow-lg;
  z-index: 50;

  &:active { transform: scale(0.98); }
}
</style>
