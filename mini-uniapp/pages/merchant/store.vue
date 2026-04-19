<script setup>
/**
 * 商家端 - 店铺设置
 * 基本信息、营业状态、营业时间
 */
import { ref, onMounted, computed } from 'vue'
import NavBar from '../../components/NavBar.vue'
import MerchantTabBar from '../../components/MerchantTabBar.vue'
import { merchantStoreApi } from '../../services/api'

const store = ref(null)
const loading = ref(true)
const saving = ref(false)
const editing = ref(false)

// 编辑表单
const editForm = ref({
  name: '',
  phone: '',
  address: '',
  description: '',
  businessHours: '',
  type: '',
})

const isActive = computed(() => store.value?.status === 'ACTIVE')

async function fetchStore() {
  loading.value = true
  try {
    const res = await merchantStoreApi.getMyStore()
    store.value = res
    editForm.value = {
      name: res.name || '',
      phone: res.phone || '',
      address: res.address || '',
      description: res.description || '',
      businessHours: res.businessHours || '',
      type: res.type || '',
    }
  } catch (e) {
    uni.showToast({ title: e.message || '获取失败', icon: 'none' })
  } finally {
    loading.value = false
  }
}

function startEdit() {
  editing.value = true
}

function cancelEdit() {
  editing.value = false
  // 恢复原值
  if (store.value) {
    editForm.value = {
      name: store.value.name || '',
      phone: store.value.phone || '',
      address: store.value.address || '',
      description: store.value.description || '',
      businessHours: store.value.businessHours || '',
      type: store.value.type || '',
    }
  }
}

async function saveStore() {
  if (saving.value) return
  saving.value = true
  try {
    const res = await merchantStoreApi.updateStore(editForm.value)
    store.value = res
    editing.value = false
    uni.showToast({ title: '保存成功', icon: 'success' })
  } catch (e) {
    uni.showToast({ title: e.message || '保存失败', icon: 'none' })
  } finally {
    saving.value = false
  }
}

async function toggleBusinessStatus() {
  const newStatus = isActive.value ? 'INACTIVE' : 'ACTIVE'
  try {
    await merchantStoreApi.updateBusinessStatus(newStatus)
    store.value.status = newStatus
    uni.showToast({ title: newStatus === 'ACTIVE' ? '已开店' : '已打烊', icon: 'success' })
  } catch (e) {
    uni.showToast({ title: e.message || '操作失败', icon: 'none' })
  }
}

onMounted(() => {
  fetchStore()
})
</script>

<template>
  <view class="page">
    <NavBar>
      <template #center>
        <text class="nav-title">店铺</text>
      </template>
      <template #right>
        <view v-if="!editing" class="nav-action" @tap="startEdit">
          <u-icon name="edit-pen" size="22" color="#0a6e2c" />
        </view>
      </template>
    </NavBar>

    <scroll-view scroll-y class="content" :enhanced="true" :show-scrollbar="false">
      <!-- 加载中 -->
      <view v-if="loading" class="loading-wrap">
        <u-loading-icon />
      </view>

      <template v-else-if="store">
        <!-- 营业状态卡片 -->
        <view class="status-card">
          <view class="status-card__left">
            <view class="status-card__dot" :class="{ 'status-card__dot--active': isActive }" />
            <text class="status-card__text">{{ isActive ? '营业中' : '已打烊' }}</text>
          </view>
          <u-button
            :text="isActive ? '打烊' : '开店'"
            size="mini"
            shape="circle"
            :color="isActive ? '#ff4d4f' : '#0a6e2c'"
            @click="toggleBusinessStatus"
          />
        </view>

        <!-- 店铺信息 -->
        <view class="section">
          <text class="section__title">基本信息</text>
          <view class="info-card">
            <!-- 店铺名称 -->
            <view class="info-row">
              <text class="info-row__label">店铺名称</text>
              <input
                v-if="editing"
                class="info-row__input"
                v-model="editForm.name"
                placeholder="请输入店名"
              />
              <text v-else class="info-row__value">{{ store.name }}</text>
            </view>

            <!-- 联系电话 -->
            <view class="info-row">
              <text class="info-row__label">联系电话</text>
              <input
                v-if="editing"
                class="info-row__input"
                v-model="editForm.phone"
                placeholder="请输入电话"
                type="number"
              />
              <text v-else class="info-row__value">{{ store.phone || '未设置' }}</text>
            </view>

            <!-- 店铺地址 -->
            <view class="info-row">
              <text class="info-row__label">店铺地址</text>
              <input
                v-if="editing"
                class="info-row__input"
                v-model="editForm.address"
                placeholder="请输入地址"
              />
              <text v-else class="info-row__value">{{ store.address || '未设置' }}</text>
            </view>

            <!-- 店铺类型 -->
            <view class="info-row">
              <text class="info-row__label">店铺类型</text>
              <input
                v-if="editing"
                class="info-row__input"
                v-model="editForm.type"
                placeholder="如：旗舰店、社区店"
              />
              <text v-else class="info-row__value">{{ store.type || '未设置' }}</text>
            </view>

            <!-- 营业时间 -->
            <view class="info-row">
              <text class="info-row__label">营业时间</text>
              <input
                v-if="editing"
                class="info-row__input"
                v-model="editForm.businessHours"
                placeholder="如：08:00-22:00"
              />
              <text v-else class="info-row__value">{{ store.businessHours || '未设置' }}</text>
            </view>

            <!-- 店铺简介 -->
            <view class="info-row info-row--col">
              <text class="info-row__label">店铺简介</text>
              <textarea
                v-if="editing"
                class="info-row__textarea"
                v-model="editForm.description"
                placeholder="请输入店铺简介"
                :maxlength="200"
                auto-height
              />
              <text v-else class="info-row__desc">{{ store.description || '暂无简介' }}</text>
            </view>
          </view>
        </view>

        <!-- 编辑模式操作按钮 -->
        <view v-if="editing" class="edit-actions">
          <u-button
            text="取消"
            shape="circle"
            :customStyle="{ flex: 1 }"
            @click="cancelEdit"
          />
          <u-button
            text="保存"
            type="primary"
            shape="circle"
            color="#0a6e2c"
            :loading="saving"
            :customStyle="{ flex: 1 }"
            @click="saveStore"
          />
        </view>

        <!-- 店铺统计 (只读) -->
        <view class="section">
          <text class="section__title">店铺信息</text>
          <view class="stats-card">
            <view class="stat-item">
              <text class="stat-item__value">{{ store.sortOrder || 0 }}</text>
              <text class="stat-item__label">排序权重</text>
            </view>
            <view class="stat-item">
              <text class="stat-item__value">{{ store.latitude ? '已设置' : '未设置' }}</text>
              <text class="stat-item__label">定位坐标</text>
            </view>
          </view>
        </view>
      </template>

      <view style="height: 160rpx;" />
    </scroll-view>

    <MerchantTabBar :current="2" />
  </view>
</template>

<style lang="scss" scoped>
@import '../../styles/design-system.scss';

.page {
  min-height: 100vh;
  background: $surface-dim;
  overflow-x: hidden;
  width: 100%;
}

.content {
  height: calc(100vh - 88px);
}

.nav-title {
  font-size: 36rpx;
  font-weight: 800;
  color: $on-surface;
}

.nav-action {
  width: 64rpx;
  height: 64rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}

.loading-wrap {
  display: flex;
  justify-content: center;
  padding: 80rpx;
}

// 营业状态
.status-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: 16rpx 24rpx;
  padding: 28rpx 32rpx;
  background: #ffffff;
  border-radius: $radius-xl;
  box-shadow: $shadow-sm;

  &__left {
    display: flex;
    align-items: center;
    gap: 16rpx;
  }

  &__dot {
    width: 16rpx;
    height: 16rpx;
    border-radius: 50%;
    background: #999;

    &--active {
      background: #52c41a;
      box-shadow: 0 0 8rpx rgba(82, 196, 26, 0.5);
    }
  }

  &__text {
    font-size: 30rpx;
    font-weight: 700;
    color: $on-surface;
  }
}

// 分区
.section {
  margin: 24rpx 24rpx 0;

  &__title {
    display: block;
    font-size: 24rpx;
    font-weight: 600;
    color: $on-surface-variant;
    margin-bottom: 12rpx;
    padding-left: 8rpx;
  }
}

// 信息卡片
.info-card {
  background: #ffffff;
  border-radius: $radius-xl;
  overflow: hidden;
  box-shadow: $shadow-sm;
}

.info-row {
  display: flex;
  align-items: center;
  padding: 24rpx 28rpx;
  gap: 16rpx;

  & + & {
    border-top: 1rpx solid rgba(0, 0, 0, 0.04);
  }

  &--col {
    flex-direction: column;
    align-items: flex-start;
  }

  &__label {
    font-size: 26rpx;
    color: $on-surface-variant;
    width: 140rpx;
    flex-shrink: 0;
  }

  &__value {
    flex: 1;
    font-size: 28rpx;
    color: $on-surface;
    text-align: right;
  }

  &__input {
    flex: 1;
    font-size: 28rpx;
    color: $on-surface;
    text-align: right;
    background: $surface-container-low;
    border-radius: $radius-md;
    padding: 8rpx 16rpx;
  }

  &__textarea {
    width: 100%;
    font-size: 26rpx;
    color: $on-surface;
    background: $surface-container-low;
    border-radius: $radius-md;
    padding: 16rpx;
    margin-top: 8rpx;
    min-height: 120rpx;
  }

  &__desc {
    font-size: 26rpx;
    color: $on-surface-variant;
    line-height: 1.6;
    margin-top: 4rpx;
  }
}

// 编辑按钮
.edit-actions {
  display: flex;
  gap: 24rpx;
  padding: 32rpx 24rpx;
}

// 统计
.stats-card {
  display: flex;
  background: #ffffff;
  border-radius: $radius-xl;
  box-shadow: $shadow-sm;
  overflow: hidden;
}

.stat-item {
  flex: 1;
  text-align: center;
  padding: 28rpx 16rpx;

  & + & {
    border-left: 1rpx solid rgba(0, 0, 0, 0.04);
  }

  &__value {
    display: block;
    font-size: 30rpx;
    font-weight: 700;
    color: $primary;
    margin-bottom: 6rpx;
  }

  &__label {
    font-size: 22rpx;
    color: $on-surface-variant;
  }
}
</style>
