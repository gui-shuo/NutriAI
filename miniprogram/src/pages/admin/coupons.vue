<template>
  <view class="admin-page">
    <!-- Top Add Button -->
    <view class="top-bar">
      <text class="top-title">优惠券列表</text>
      <view class="add-btn" @tap="openCreate">+ 新建</view>
    </view>

    <!-- Coupon List -->
    <scroll-view class="coupon-list" scroll-y>
      <view class="loading-state" v-if="loading && coupons.length === 0">加载中...</view>
      <view class="empty-state" v-if="!loading && coupons.length === 0">暂无优惠券</view>

      <view class="coupon-card" v-for="coupon in coupons" :key="coupon.id">
        <view class="card-header">
          <view class="coupon-amount-badge">
            <text v-if="coupon.couponType === 'REDUCE'">减¥{{ coupon.discountValue }}</text>
            <text v-else>{{ (coupon.discountValue * 10).toFixed(1) }}折</text>
          </view>
          <view class="coupon-status" :class="{ active: coupon.active }">
            {{ coupon.active ? '启用' : '停用' }}
          </view>
        </view>
        <text class="coupon-name">{{ coupon.name }}</text>
        <view class="coupon-meta">
          <text>满 ¥{{ coupon.minOrderAmount }} 可用</text>
          <text v-if="coupon.maxDiscountAmount">最高减¥{{ coupon.maxDiscountAmount }}</text>
          <text>已领 {{ coupon.claimedCount || 0 }}/{{ coupon.totalCount }}</text>
        </view>
        <view class="coupon-time">{{ formatDate(coupon.startTime) }} - {{ formatDate(coupon.endTime) }}</view>
        <view class="card-actions">
          <view class="action-btn" @tap="openEdit(coupon)">编辑</view>
          <view class="action-btn" @tap="toggleStatus(coupon)">{{ coupon.active ? '停用' : '启用' }}</view>
          <view class="action-btn danger" @tap="deleteCoupon(coupon)">删除</view>
        </view>
      </view>
    </scroll-view>

    <!-- Create/Edit Modal -->
    <view class="form-modal" v-if="showModal">
      <view class="modal-mask" @tap="showModal = false"></view>
      <view class="modal-body">
        <view class="modal-header">
          <text class="modal-title">{{ editTarget ? '编辑优惠券' : '新建优惠券' }}</text>
          <text class="modal-close" @tap="showModal = false">✕</text>
        </view>
        <scroll-view class="modal-content" scroll-y>
          <view class="form-item">
            <text class="form-label">名称</text>
            <input class="form-input" v-model="form.name" placeholder="优惠券名称" />
          </view>
          <view class="form-item">
            <text class="form-label">类型</text>
            <picker :value="typeIndex" :range="typeOptions" range-key="label" @change="onTypeChange">
              <view class="picker-value">{{ typeOptions[typeIndex].label }} ›</view>
            </picker>
          </view>
          <view class="form-item">
            <text class="form-label">{{ form.couponType === 'REDUCE' ? '减免金额' : '折扣值(如0.2=八折)' }}</text>
            <input class="form-input" v-model="form.discountValue" type="digit" placeholder="折扣值" />
          </view>
          <view class="form-item" v-if="form.couponType === 'DISCOUNT'">
            <text class="form-label">最高减免</text>
            <input class="form-input" v-model="form.maxDiscountAmount" type="digit" placeholder="不限填0" />
          </view>
          <view class="form-item">
            <text class="form-label">最低消费</text>
            <input class="form-input" v-model="form.minOrderAmount" type="digit" placeholder="满多少元可用" />
          </view>
          <view class="form-item">
            <text class="form-label">发放总量</text>
            <input class="form-input" v-model="form.totalCount" type="number" placeholder="发放总量" />
          </view>
          <view class="form-item">
            <text class="form-label">每人限领</text>
            <input class="form-input" v-model="form.perUserLimit" type="number" placeholder="每人最多领几张" />
          </view>
          <view class="form-item">
            <text class="form-label">开始日期</text>
            <picker mode="date" :value="form.startTime" @change="e => form.startTime = e.detail.value">
              <view class="picker-value">{{ form.startTime || '选择日期' }} ›</view>
            </picker>
          </view>
          <view class="form-item">
            <text class="form-label">结束日期</text>
            <picker mode="date" :value="form.endTime" @change="e => form.endTime = e.detail.value">
              <view class="picker-value">{{ form.endTime || '选择日期' }} ›</view>
            </picker>
          </view>
          <view class="form-item">
            <text class="form-label">描述</text>
            <input class="form-input" v-model="form.description" placeholder="优惠券描述（可选）" />
          </view>
        </scroll-view>
        <view class="modal-footer">
          <view class="btn-cancel" @tap="showModal = false">取消</view>
          <view class="btn-confirm" @tap="saveCoupon">保存</view>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { adminApi } from '@/services/api'

const coupons = ref<any[]>([])
const loading = ref(false)
const showModal = ref(false)
const editTarget = ref<any>(null)

const typeOptions = [
  { label: '满减券', value: 'REDUCE' },
  { label: '折扣券', value: 'DISCOUNT' }
]
const typeIndex = ref(0)

const defaultForm = () => ({
  name: '',
  couponType: 'REDUCE',
  discountValue: '',
  maxDiscountAmount: '',
  minOrderAmount: '0',
  totalCount: '',
  perUserLimit: '1',
  startTime: '',
  endTime: '',
  description: '',
  active: true
})

const form = ref<any>(defaultForm())

async function loadCoupons() {
  loading.value = true
  try {
    const res: any = await adminApi.getCoupons({ page: 0, size: 50 })
    coupons.value = res.data?.content || res.data || []
  } catch (e) {
    console.error('Load coupons error:', e)
  } finally {
    loading.value = false
  }
}

function openCreate() {
  editTarget.value = null
  form.value = defaultForm()
  typeIndex.value = 0
  showModal.value = true
}

function openEdit(coupon: any) {
  editTarget.value = coupon
  form.value = { ...coupon, startTime: coupon.startTime?.substring(0, 10), endTime: coupon.endTime?.substring(0, 10) }
  typeIndex.value = typeOptions.findIndex(t => t.value === coupon.couponType) || 0
  showModal.value = true
}

function onTypeChange(e: any) {
  typeIndex.value = Number(e.detail.value)
  form.value.couponType = typeOptions[typeIndex.value].value
}

async function saveCoupon() {
  if (!form.value.name.trim()) return uni.showToast({ title: '请填写优惠券名称', icon: 'none' })
  if (!form.value.discountValue) return uni.showToast({ title: '请填写折扣值', icon: 'none' })
  if (!form.value.totalCount) return uni.showToast({ title: '请填写发放总量', icon: 'none' })

  const payload = {
    ...form.value,
    discountValue: parseFloat(form.value.discountValue),
    maxDiscountAmount: form.value.maxDiscountAmount ? parseFloat(form.value.maxDiscountAmount) : null,
    minOrderAmount: parseFloat(form.value.minOrderAmount || '0'),
    totalCount: parseInt(form.value.totalCount),
    perUserLimit: parseInt(form.value.perUserLimit || '1'),
    startTime: form.value.startTime ? form.value.startTime + 'T00:00:00' : null,
    endTime: form.value.endTime ? form.value.endTime + 'T23:59:59' : null
  }

  try {
    if (editTarget.value) {
      await adminApi.updateCoupon(editTarget.value.id, payload)
    } else {
      await adminApi.createCoupon(payload)
    }
    showModal.value = false
    uni.showToast({ title: '保存成功', icon: 'success' })
    loadCoupons()
  } catch (e: any) {
    uni.showToast({ title: e?.message || '保存失败', icon: 'none' })
  }
}

async function toggleStatus(coupon: any) {
  try {
    await adminApi.toggleCoupon(coupon.id, { active: !coupon.active })
    coupon.active = !coupon.active
    uni.showToast({ title: coupon.active ? '已启用' : '已停用', icon: 'success' })
  } catch (e: any) {
    uni.showToast({ title: e?.message || '操作失败', icon: 'none' })
  }
}

async function deleteCoupon(coupon: any) {
  uni.showModal({
    title: '确认删除',
    content: `确定删除优惠券「${coupon.name}」？`,
    success: async (res) => {
      if (!res.confirm) return
      try {
        await adminApi.deleteCoupon(coupon.id)
        coupons.value = coupons.value.filter(c => c.id !== coupon.id)
        uni.showToast({ title: '已删除', icon: 'success' })
      } catch (e: any) {
        uni.showToast({ title: e?.message || '删除失败', icon: 'none' })
      }
    }
  })
}

function formatDate(dt: string) {
  if (!dt) return '-'
  return dt.substring(0, 10)
}

onMounted(loadCoupons)
</script>

<style lang="scss" scoped>
.admin-page {
  min-height: 100vh;
  background: $background;
}

.top-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 24rpx 28rpx;
  background: $card;
  border-bottom: 1rpx solid $border;
}

.top-title {
  font-size: 30rpx;
  font-weight: 600;
  color: $foreground;
}

.add-btn {
  background: $accent;
  color: white;
  font-size: 26rpx;
  padding: 14rpx 28rpx;
  border-radius: 30rpx;
}

.coupon-list {
  height: calc(100vh - 100rpx);
  padding: 16rpx 20rpx;
}

.loading-state, .empty-state {
  text-align: center;
  padding: 80rpx;
  color: #94a3b8;
  font-size: 28rpx;
}

.coupon-card {
  background: $card;
  border-radius: 16rpx;
  padding: 24rpx;
  margin-bottom: 20rpx;
  border: 1rpx solid $border;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12rpx;
}

.coupon-amount-badge {
  background: #ef4444;
  color: white;
  font-size: 28rpx;
  font-weight: 700;
  padding: 8rpx 20rpx;
  border-radius: 20rpx;
}

.coupon-status {
  font-size: 24rpx;
  color: #94a3b8;
  padding: 6rpx 16rpx;
  border-radius: 20rpx;
  background: $background;
}

.coupon-status.active {
  color: $accent;
  background: rgba(16,185,129,0.1);
}

.coupon-name {
  font-size: 30rpx;
  font-weight: 600;
  color: $foreground;
  display: block;
  margin-bottom: 10rpx;
}

.coupon-meta {
  display: flex;
  gap: 16rpx;
  flex-wrap: wrap;
  margin-bottom: 8rpx;
}

.coupon-meta text {
  font-size: 24rpx;
  color: #64748b;
}

.coupon-time {
  font-size: 22rpx;
  color: #94a3b8;
  margin-bottom: 16rpx;
}

.card-actions {
  display: flex;
  gap: 12rpx;
  justify-content: flex-end;
}

.action-btn {
  font-size: 24rpx;
  padding: 12rpx 24rpx;
  border-radius: 24rpx;
  background: $accent;
  color: white;
}

.action-btn.danger {
  background: #ef4444;
}

.form-modal {
  position: fixed;
  inset: 0;
  z-index: 1000;
}

.modal-mask {
  position: absolute;
  inset: 0;
  background: rgba(0,0,0,0.5);
}

.modal-body {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  background: white;
  border-radius: 24rpx 24rpx 0 0;
  max-height: 85vh;
  display: flex;
  flex-direction: column;
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 28rpx 28rpx 16rpx;
  border-bottom: 1rpx solid $border;
  flex-shrink: 0;
}

.modal-title {
  font-size: 30rpx;
  font-weight: 600;
  color: $foreground;
}

.modal-close {
  font-size: 28rpx;
  color: #94a3b8;
}

.modal-content {
  flex: 1;
  padding: 16rpx 28rpx;
}

.form-item {
  display: flex;
  align-items: center;
  padding: 18rpx 0;
  border-bottom: 1rpx solid $border;
}

.form-label {
  width: 130rpx;
  font-size: 26rpx;
  color: #64748b;
  flex-shrink: 0;
}

.form-input {
  flex: 1;
  font-size: 26rpx;
  color: $foreground;
  padding: 0 12rpx;
}

.picker-value {
  flex: 1;
  font-size: 26rpx;
  color: $foreground;
  padding: 0 12rpx;
}

.modal-footer {
  display: flex;
  gap: 20rpx;
  padding: 20rpx 28rpx;
  padding-bottom: calc(20rpx + env(safe-area-inset-bottom));
  border-top: 1rpx solid $border;
  flex-shrink: 0;
}

.btn-cancel, .btn-confirm {
  flex: 1;
  text-align: center;
  padding: 22rpx;
  border-radius: 40rpx;
  font-size: 28rpx;
}

.btn-cancel {
  background: $background;
  color: #64748b;
  border: 1rpx solid $border;
}

.btn-confirm {
  background: $accent;
  color: white;
  font-weight: 600;
}
</style>
