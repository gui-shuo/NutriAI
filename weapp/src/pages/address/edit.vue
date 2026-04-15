<template>
  <view class="edit-page">
    <view class="form-section">
      <view class="form-item">
        <text class="form-label">收货人</text>
        <input v-model="form.receiverName" placeholder="请输入收货人姓名" class="form-input" />
      </view>

      <view class="form-item">
        <text class="form-label">手机号</text>
        <input v-model="form.receiverPhone" placeholder="请输入手机号" type="number" :maxlength="11" class="form-input" />
      </view>

      <view class="form-item" @tap="chooseRegion">
        <text class="form-label">所在地区</text>
        <text :class="['form-input', { placeholder: !regionText }]">
          {{ regionText || '请选择省/市/区' }}
        </text>
        <text class="form-arrow">›</text>
      </view>

      <view class="form-item">
        <text class="form-label">详细地址</text>
        <input v-model="form.detailAddress" placeholder="街道、楼栋、门牌号等" class="form-input" />
      </view>

      <view class="form-item">
        <text class="form-label">标签</text>
        <view class="label-tags">
          <view
            v-for="label in labels"
            :key="label"
            :class="['tag', { active: form.label === label }]"
            @tap="form.label = form.label === label ? '' : label"
          >
            {{ label }}
          </view>
        </view>
      </view>

      <view class="form-item switch-item">
        <text class="form-label">设为默认地址</text>
        <switch :checked="form.isDefault" color="#10B981" @change="form.isDefault = $event.detail.value" />
      </view>
    </view>

    <view class="submit-section safe-bottom">
      <button class="btn-primary btn-block btn-lg" :disabled="submitting" @tap="handleSave">
        {{ submitting ? '保存中...' : '保存地址' }}
      </button>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { addressApi } from '../../services/api'
import { isValidPhone } from '../../utils'

const editId = ref(0)
const submitting = ref(false)
const labels = ['家', '公司', '学校', '宿舍']

const form = reactive({
  receiverName: '',
  receiverPhone: '',
  province: '',
  city: '',
  district: '',
  detailAddress: '',
  label: '',
  isDefault: false
})

const regionText = computed(() => {
  if (form.province) return `${form.province} ${form.city} ${form.district}`
  return ''
})

onLoad((query: any) => {
  if (query?.id) {
    editId.value = Number(query.id)
    loadAddress()
  }
})

async function loadAddress() {
  try {
    const list: any = await addressApi.getList()
    const addresses = list?.content || list || []
    const addr = addresses.find((a: any) => a.id === editId.value)
    if (addr) {
      form.receiverName = addr.receiverName || ''
      form.receiverPhone = addr.receiverPhone || ''
      form.province = addr.province || ''
      form.city = addr.city || ''
      form.district = addr.district || ''
      form.detailAddress = addr.detailAddress || ''
      form.label = addr.label || ''
      form.isDefault = addr.isDefault || false
    }
  } catch {}
}

function chooseRegion() {
  // Use WeChat region picker
  // #ifdef MP-WEIXIN
  uni.chooseLocation({
    success: () => {},
    fail: () => {}
  })
  // #endif

  // Fallback: manual region picker
  const regions = [['北京市', '上海市', '天津市', '重庆市', '广东省', '浙江省', '江苏省', '四川省', '湖北省', '湖南省', '山东省', '河南省', '福建省', '安徽省', '河北省', '陕西省', '辽宁省', '吉林省', '黑龙江省', '江西省', '山西省', '广西壮族自治区', '云南省', '贵州省', '海南省', '甘肃省', '青海省', '内蒙古自治区', '宁夏回族自治区', '新疆维吾尔自治区', '西藏自治区']]

  uni.showActionSheet({
    itemList: regions[0],
    success: (res) => {
      form.province = regions[0][res.tapIndex]
      form.city = form.province
      form.district = ''
    }
  })
}

async function handleSave() {
  if (!form.receiverName.trim()) {
    return uni.showToast({ title: '请输入收货人', icon: 'none' })
  }
  if (!isValidPhone(form.receiverPhone)) {
    return uni.showToast({ title: '请输入正确的手机号', icon: 'none' })
  }
  if (!form.province) {
    return uni.showToast({ title: '请选择所在地区', icon: 'none' })
  }
  if (!form.detailAddress.trim()) {
    return uni.showToast({ title: '请输入详细地址', icon: 'none' })
  }

  submitting.value = true
  try {
    const data = {
      receiverName: form.receiverName.trim(),
      receiverPhone: form.receiverPhone,
      province: form.province,
      city: form.city || form.province,
      district: form.district,
      detailAddress: form.detailAddress.trim(),
      label: form.label,
      isDefault: form.isDefault
    }

    if (editId.value) {
      await addressApi.update(editId.value, data)
    } else {
      await addressApi.add(data)
    }

    uni.showToast({ title: '保存成功', icon: 'success' })
    setTimeout(() => uni.navigateBack(), 500)
  } catch (e: any) {
    uni.showToast({ title: e?.message || '保存失败', icon: 'none' })
  }
  submitting.value = false
}
</script>

<style lang="scss" scoped>
.edit-page {
  min-height: 100vh;
  background: $bg-page;
  padding: $spacing-md $spacing-lg;
}

.form-section {
  background: $bg-card;
  border-radius: $radius-lg;
  padding: 0 $spacing-lg;
}

.form-item {
  display: flex;
  align-items: center;
  padding: $spacing-lg 0;
  border-bottom: 1rpx solid $border-light;
  &:last-child { border-bottom: none; }
}

.form-label {
  width: 140rpx;
  font-size: $font-base;
  color: $text-primary;
  flex-shrink: 0;
}

.form-input {
  flex: 1;
  font-size: $font-base;
  color: $text-primary;
  &.placeholder { color: $text-muted; }
}

.form-arrow {
  font-size: $font-xl;
  color: $text-muted;
  margin-left: $spacing-sm;
}

.label-tags {
  display: flex;
  flex-wrap: wrap;
  gap: $spacing-sm;
  flex: 1;
}

.switch-item {
  justify-content: space-between;
}

.submit-section {
  margin-top: $spacing-xl;
}
</style>
