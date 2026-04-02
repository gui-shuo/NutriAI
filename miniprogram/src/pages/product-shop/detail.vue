<template>
  <view class="detail-page" v-if="product">
    <!-- Image Swiper -->
    <swiper class="product-swiper" indicator-dots indicator-color="rgba(255,255,255,0.5)" indicator-active-color="#fff" autoplay circular>
      <swiper-item v-for="(img, idx) in productImages" :key="idx">
        <image class="swiper-img" :src="img" mode="aspectFill" @tap="previewImage(idx)" />
      </swiper-item>
    </swiper>

    <!-- Price & Name -->
    <view class="price-section">
      <view class="price-row">
        <text class="current-price">¥{{ formatPrice(product.price) }}</text>
        <text class="original-price" v-if="product.originalPrice && product.originalPrice > product.price">
          ¥{{ formatPrice(product.originalPrice) }}
        </text>
        <view class="discount-tag" v-if="product.originalPrice && product.originalPrice > product.price">
          {{ Math.round((1 - product.price / product.originalPrice) * 100) }}% OFF
        </view>
      </view>
      <text class="product-name">{{ product.name }}</text>
      <text class="sales-info">已售 {{ product.salesCount || 0 }}件</text>
    </view>

    <!-- Description -->
    <view class="info-section" v-if="product.description">
      <view class="section-title">商品详情</view>
      <text class="description-text">{{ product.description }}</text>
    </view>

    <!-- Specifications -->
    <view class="info-section" v-if="product.specifications">
      <view class="section-title">规格参数</view>
      <view class="spec-list">
        <view class="spec-item" v-for="(val, key) in parseSpecs(product.specifications)" :key="key">
          <text class="spec-label">{{ key }}</text>
          <text class="spec-value">{{ val }}</text>
        </view>
      </view>
    </view>

    <!-- Spacer -->
    <view style="height: 140rpx"></view>

    <!-- Bottom Buy Bar -->
    <view class="bottom-buy-bar">
      <view class="buy-btn" @tap="openPurchase">立即购买</view>
    </view>

    <!-- Purchase Modal -->
    <view class="modal-mask" v-if="showPurchase" @tap="showPurchase = false">
      <view class="modal-content" @tap.stop>
        <view class="modal-header">
          <text class="modal-title">确认购买</text>
          <text class="modal-close" @tap="showPurchase = false">✕</text>
        </view>

        <scroll-view class="modal-body" scroll-y>
          <!-- Product Summary -->
          <view class="purchase-product">
            <image class="purchase-img" :src="productImages[0]" mode="aspectFill" />
            <view class="purchase-info">
              <text class="purchase-name">{{ product.name }}</text>
              <text class="purchase-price">¥{{ formatPrice(product.price) }}</text>
            </view>
          </view>

          <!-- Quantity -->
          <view class="form-section">
            <text class="form-label">数量</text>
            <view class="quantity-selector">
              <view class="qty-btn" :class="{ disabled: quantity <= 1 }" @tap="quantity > 1 && quantity--">−</view>
              <text class="qty-value">{{ quantity }}</text>
              <view class="qty-btn" @tap="quantity++">+</view>
            </view>
          </view>

          <!-- Address Info -->
          <view class="form-section">
            <text class="form-label">收货信息</text>
            <view class="manage-link" @tap="goAddressManage">管理</view>
          </view>

          <!-- Saved Address Selection -->
          <view v-if="savedAddresses.length > 0 && !useManualAddress">
            <view
              class="saved-address-item"
              :class="{ active: selectedAddressId === addr.id }"
              v-for="addr in savedAddresses.slice(0, 3)"
              :key="addr.id"
              @tap="selectSavedAddress(addr)"
            >
              <view class="saved-addr-radio">
                <view class="radio-dot" :class="{ checked: selectedAddressId === addr.id }"></view>
              </view>
              <view class="saved-addr-info">
                <view class="saved-addr-top">
                  <text class="saved-addr-name">{{ addr.receiverName }}</text>
                  <text class="saved-addr-phone">{{ addr.receiverPhone }}</text>
                  <view class="saved-addr-default" v-if="addr.isDefault">默认</view>
                </view>
                <text class="saved-addr-detail">{{ getAddrFullAddress(addr) }}</text>
              </view>
            </view>
            <view class="manual-entry-link" @tap="useManualAddress = true">
              <text>+ 使用新地址</text>
            </view>
          </view>

          <!-- Manual Address Input -->
          <view v-else>
            <view class="back-to-saved" v-if="savedAddresses.length > 0" @tap="useManualAddress = false">
              <text>← 选择已有地址</text>
            </view>
            <view class="form-group">
              <input class="form-input" v-model="address.name" placeholder="收货人姓名" />
            </view>
            <view class="form-group">
              <input class="form-input" v-model="address.phone" placeholder="联系电话" type="number" maxlength="11" />
            </view>
            <view class="form-group">
              <textarea class="form-textarea" v-model="address.detail" placeholder="详细地址" :maxlength="200" />
            </view>
            <view class="save-address-check" @tap="saveNewAddress = !saveNewAddress">
              <view class="check-box" :class="{ checked: saveNewAddress }">✓</view>
              <text class="check-label">保存到我的地址</text>
            </view>
          </view>

          <!-- Total -->
          <view class="total-row">
            <text>合计：</text>
            <text class="total-price">¥{{ formatPrice(product.price * quantity) }}</text>
          </view>
        </scroll-view>

        <view class="modal-footer">
          <view class="confirm-buy-btn" @tap="confirmOrder">确认下单</view>
        </view>
      </view>
    </view>

    <!-- Order Result Modal -->
    <view class="modal-mask" v-if="showResult" @tap="showResult = false">
      <view class="result-content" @tap.stop>
        <text class="result-icon">{{ orderSuccess ? '🎉' : '😢' }}</text>
        <text class="result-title">{{ orderSuccess ? '购买成功' : '购买失败' }}</text>
        <text class="result-desc" v-if="orderSuccess">订单号：{{ orderNo }}</text>
        <text class="result-desc" v-else>{{ orderError }}</text>
        <view class="result-btn" @tap="showResult = false; showPurchase = false">
          {{ orderSuccess ? '完成' : '关闭' }}
        </view>
      </view>
    </view>
  </view>

  <!-- Loading -->
  <view class="loading-page" v-else-if="pageLoading">
    <text>加载中...</text>
  </view>
</template>

<script setup lang="ts">
import { ref, computed, onUnmounted } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { productApi, addressApi } from '@/services/api'
import { checkLogin } from '@/utils/common'

const product = ref<any>(null)
const pageLoading = ref(true)
const showPurchase = ref(false)
const showResult = ref(false)
const orderSuccess = ref(false)
const orderNo = ref('')
const orderError = ref('')
const quantity = ref(1)
const address = ref({ name: '', phone: '', detail: '' })
const savedAddresses = ref<any[]>([])
const selectedAddressId = ref<number | null>(null)
const useManualAddress = ref(false)

const productImages = computed(() => {
  if (!product.value) return []
  if (product.value.imageUrls && product.value.imageUrls.length > 0) return product.value.imageUrls
  if (product.value.images && product.value.images.length > 0) return product.value.images
  if (product.value.imageUrl) return [product.value.imageUrl]
  if (product.value.image) return [product.value.image]
  return []
})

function formatPrice(val: number | undefined): string {
  return (val || 0).toFixed(2)
}

function parseSpecs(specs: any): Record<string, string> {
  if (typeof specs === 'string') {
    try { return JSON.parse(specs) } catch { return { '规格': specs } }
  }
  if (typeof specs === 'object') return specs
  return {}
}

function previewImage(index: number) {
  uni.previewImage({
    urls: productImages.value,
    current: index
  })
}

function openPurchase() {
  if (!checkLogin()) return
  quantity.value = 1
  loadSavedAddresses()
  showPurchase.value = true
}

async function loadSavedAddresses() {
  try {
    const res = await addressApi.getList()
    savedAddresses.value = res.data || []
    if (savedAddresses.value.length > 0) {
      const defaultAddr = savedAddresses.value.find((a: any) => a.isDefault) || savedAddresses.value[0]
      selectedAddressId.value = defaultAddr.id
      useManualAddress.value = false
    } else {
      useManualAddress.value = true
    }
  } catch {
    savedAddresses.value = []
    useManualAddress.value = true
  }
}

function selectSavedAddress(addr: any) {
  selectedAddressId.value = addr.id
}

function getAddrFullAddress(addr: any): string {
  let s = ''
  if (addr.province) s += addr.province
  if (addr.city) s += addr.city
  if (addr.district) s += addr.district
  s += addr.detailAddress
  return s
}

function goAddressManage() {
  uni.navigateTo({ url: '/pages/address/index' })
}

// Listen for address selection from address page
uni.$on('address-selected', (addr: any) => {
  savedAddresses.value = savedAddresses.value.map((a: any) => a.id === addr.id ? addr : a)
  if (!savedAddresses.value.find((a: any) => a.id === addr.id)) {
    savedAddresses.value.unshift(addr)
  }
  selectedAddressId.value = addr.id
  useManualAddress.value = false
})

onUnmounted(() => {
  uni.$off('address-selected')
})

const saveNewAddress = ref(true)

async function confirmOrder() {
  let receiverName: string, receiverPhone: string, receiverAddress: string

  if (!useManualAddress.value && selectedAddressId.value) {
    const selected = savedAddresses.value.find((a: any) => a.id === selectedAddressId.value)
    if (!selected) {
      return uni.showToast({ title: '请选择收货地址', icon: 'none' })
    }
    receiverName = selected.receiverName
    receiverPhone = selected.receiverPhone
    receiverAddress = getAddrFullAddress(selected)
  } else {
    if (!address.value.name.trim()) {
      return uni.showToast({ title: '请输入收货人姓名', icon: 'none' })
    }
    if (!address.value.phone.trim() || address.value.phone.length < 11) {
      return uni.showToast({ title: '请输入正确的手机号', icon: 'none' })
    }
    if (!address.value.detail.trim()) {
      return uni.showToast({ title: '请输入详细地址', icon: 'none' })
    }
    receiverName = address.value.name
    receiverPhone = address.value.phone
    receiverAddress = address.value.detail

    // Save new address if checkbox is checked
    if (saveNewAddress.value) {
      try {
        await addressApi.add({
          receiverName: address.value.name,
          receiverPhone: address.value.phone,
          detailAddress: address.value.detail,
          isDefault: savedAddresses.value.length === 0
        })
      } catch { /* ignore save failure */ }
    }
  }

  uni.showLoading({ title: '提交订单...' })
  try {
    const orderData = {
      productId: product.value.id,
      quantity: quantity.value,
      receiverName,
      receiverPhone,
      receiverAddress
    }
    const res = await productApi.createOrder(orderData)
    const no = res.data?.orderNo || res.data

    uni.showLoading({ title: '支付中...' })
    await productApi.simulatePay(no)

    orderNo.value = no
    orderSuccess.value = true
    showResult.value = true
  } catch (e: any) {
    orderSuccess.value = false
    orderError.value = e?.message || '订单创建失败'
    showResult.value = true
  } finally {
    uni.hideLoading()
  }
}

onLoad((query) => {
  const id = Number(query?.id || 0)
  if (id) {
    loadProduct(id)
  }
})

async function loadProduct(id: number) {
  pageLoading.value = true
  try {
    const res = await productApi.getProduct(id)
    const p = res.data
    if (p && !p.price && p.salePrice) {
      p.price = p.salePrice
    }
    product.value = p
  } catch {
    uni.showToast({ title: '商品加载失败', icon: 'none' })
  } finally {
    pageLoading.value = false
  }
}
</script>

<style scoped lang="scss">
.detail-page {
  min-height: 100vh;
  background: $background;
  font-family: 'Inter', sans-serif;
}

.loading-page {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  font-size: 28rpx;
  color: $muted-foreground;
  font-family: 'Inter', sans-serif;
}

.product-swiper {
  width: 100%;
  height: 750rpx;
}

.swiper-img {
  width: 100%;
  height: 100%;
}

.price-section {
  background: $card;
  padding: 28rpx;
  border-bottom: 1rpx solid $border;
}

.price-row {
  display: flex;
  align-items: baseline;
  gap: 12rpx;
  margin-bottom: 16rpx;
}

.current-price {
  font-size: 48rpx;
  color: $accent;
  font-weight: 700;
  font-family: 'JetBrains Mono', monospace;
}

.original-price {
  font-size: 28rpx;
  color: $muted-foreground;
  text-decoration: line-through;
}

.discount-tag {
  font-size: 20rpx;
  color: $accent-foreground;
  background: $accent;
  padding: 4rpx 12rpx;
  border-radius: $radius-full;
  font-family: 'JetBrains Mono', monospace;
}

.product-name {
  font-size: 32rpx;
  color: $foreground;
  font-weight: 600;
  line-height: 1.5;
  display: block;
  font-family: 'Calistoga', cursive;
}

.sales-info {
  font-size: 24rpx;
  color: $muted-foreground;
  margin-top: 8rpx;
  display: block;
  font-family: 'Inter', sans-serif;
}

.info-section {
  background: $card;
  margin-top: 16rpx;
  padding: 28rpx;
  border-top: 1rpx solid $border;
}

.section-title {
  font-size: 30rpx;
  font-weight: 600;
  color: $foreground;
  margin-bottom: 20rpx;
  padding-left: 16rpx;
  border-left: 6rpx solid $accent;
  font-family: 'Calistoga', cursive;
}

.description-text {
  font-size: 28rpx;
  color: $muted-foreground;
  line-height: 1.8;
  font-family: 'Inter', sans-serif;
}

.spec-list {
  border-top: 1rpx solid $border;
}

.spec-item {
  display: flex;
  padding: 16rpx 0;
  border-bottom: 1rpx solid $border;
}

.spec-label {
  width: 200rpx;
  font-size: 26rpx;
  color: $muted-foreground;
  flex-shrink: 0;
  font-family: 'Inter', sans-serif;
}

.spec-value {
  font-size: 26rpx;
  color: $foreground;
  flex: 1;
  font-family: 'Inter', sans-serif;
}

/* Bottom Bar */
.bottom-buy-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  background: $card;
  padding: 16rpx 28rpx;
  padding-bottom: calc(16rpx + env(safe-area-inset-bottom));
  border-top: 1rpx solid $border;
  z-index: 100;
}

.buy-btn {
  text-align: center;
  padding: 24rpx;
  background: $gradient-accent;
  color: $accent-foreground;
  font-size: 32rpx;
  font-weight: 600;
  border-radius: $radius-xl;
  box-shadow: $shadow-accent;
  font-family: 'Calistoga', cursive;
}

/* Modal */
.modal-mask {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: $uni-bg-color-mask;
  z-index: 1000;
  display: flex;
  align-items: flex-end;
}

.modal-content {
  width: 100%;
  max-height: 85vh;
  background: $card;
  border-radius: $radius-2xl $radius-2xl 0 0;
  display: flex;
  flex-direction: column;
  box-shadow: $shadow-lg;
}

.modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 28rpx;
  border-bottom: 1rpx solid $border;
}

.modal-title {
  font-size: 32rpx;
  font-weight: 600;
  color: $foreground;
  font-family: 'Calistoga', cursive;
}

.modal-close {
  font-size: 36rpx;
  color: $muted-foreground;
  padding: 8rpx;
}

.modal-body {
  flex: 1;
  padding: 28rpx;
  max-height: 60vh;
}

.purchase-product {
  display: flex;
  margin-bottom: 28rpx;
  padding-bottom: 28rpx;
  border-bottom: 1rpx solid $border;
}

.purchase-img {
  width: 160rpx;
  height: 160rpx;
  border-radius: $radius-xl;
  border: 1rpx solid $border;
  margin-right: 20rpx;
  flex-shrink: 0;
}

.purchase-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.purchase-name {
  font-size: 28rpx;
  color: $foreground;
  margin-bottom: 12rpx;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  font-family: 'Inter', sans-serif;
}

.purchase-price {
  font-size: 36rpx;
  color: $accent;
  font-weight: 600;
  font-family: 'JetBrains Mono', monospace;
}

.form-section {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20rpx;
}

.form-label {
  font-size: 28rpx;
  font-weight: 600;
  color: $foreground;
  font-family: 'Inter', sans-serif;
}

.manage-link {
  font-size: 26rpx;
  color: $accent;
  font-family: 'Inter', sans-serif;
}

/* Saved Address Selection */
.saved-address-item {
  display: flex;
  align-items: flex-start;
  gap: 16rpx;
  padding: 20rpx;
  border: 1rpx solid $border;
  border-radius: $radius-xl;
  margin-bottom: 12rpx;
  transition: all 0.2s ease;
}

.saved-address-item.active {
  border-color: $accent;
  background: rgba(16, 185, 129, 0.04);
}

.saved-addr-radio {
  padding-top: 6rpx;
}

.radio-dot {
  width: 36rpx;
  height: 36rpx;
  border: 2rpx solid $border;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.radio-dot.checked {
  border-color: $accent;
  background: $accent;
}

.radio-dot.checked::after {
  content: '';
  width: 16rpx;
  height: 16rpx;
  border-radius: 50%;
  background: $accent-foreground;
}

.saved-addr-info {
  flex: 1;
}

.saved-addr-top {
  display: flex;
  align-items: center;
  gap: 12rpx;
  margin-bottom: 8rpx;
}

.saved-addr-name {
  font-size: 28rpx;
  font-weight: 600;
  color: $foreground;
  font-family: 'Inter', sans-serif;
}

.saved-addr-phone {
  font-size: 26rpx;
  color: $muted-foreground;
  font-family: 'Inter', sans-serif;
}

.saved-addr-default {
  font-size: 18rpx;
  color: $accent-foreground;
  background: $accent;
  padding: 2rpx 10rpx;
  border-radius: $radius-full;
  font-family: 'JetBrains Mono', monospace;
}

.saved-addr-detail {
  font-size: 24rpx;
  color: $muted-foreground;
  line-height: 1.4;
  font-family: 'Inter', sans-serif;
}

.manual-entry-link {
  text-align: center;
  padding: 16rpx;
  font-size: 26rpx;
  color: $accent;
  border: 1rpx dashed $accent;
  border-radius: $radius-xl;
  margin-bottom: 16rpx;
  font-family: 'Inter', sans-serif;
}

.back-to-saved {
  font-size: 26rpx;
  color: $accent;
  margin-bottom: 16rpx;
  font-family: 'Inter', sans-serif;
}

.save-address-check {
  display: flex;
  align-items: center;
  gap: 12rpx;
  margin-top: 8rpx;
  margin-bottom: 16rpx;
}

.check-box {
  width: 36rpx;
  height: 36rpx;
  border: 2rpx solid $border;
  border-radius: $radius-sm;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 22rpx;
  color: transparent;
}

.check-box.checked {
  background: $accent;
  border-color: $accent;
  color: $accent-foreground;
}

.check-label {
  font-size: 26rpx;
  color: $muted-foreground;
  font-family: 'Inter', sans-serif;
}

.quantity-selector {
  display: flex;
  align-items: center;
  gap: 4rpx;
}

.qty-btn {
  width: 56rpx;
  height: 56rpx;
  border: 1rpx solid $border;
  border-radius: $radius-lg;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 32rpx;
  color: $foreground;
  font-family: 'Inter', sans-serif;
}

.qty-btn.disabled {
  color: $muted-foreground;
  border-color: $border;
  opacity: 0.4;
}

.qty-value {
  width: 80rpx;
  text-align: center;
  font-size: 30rpx;
  font-weight: 600;
  font-family: 'JetBrains Mono', monospace;
}

.form-group {
  margin-bottom: 16rpx;
}

.form-input {
  width: 100%;
  height: 80rpx;
  border: 1rpx solid $border;
  border-radius: $radius-lg;
  padding: 0 24rpx;
  font-size: 28rpx;
  font-family: 'Inter', sans-serif;
  background: $background;
}

.form-textarea {
  width: 100%;
  height: 160rpx;
  border: 1rpx solid $border;
  border-radius: $radius-lg;
  padding: 16rpx 24rpx;
  font-size: 28rpx;
  font-family: 'Inter', sans-serif;
  background: $background;
}

.total-row {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  margin-top: 28rpx;
  padding-top: 20rpx;
  border-top: 1rpx solid $border;
  font-size: 28rpx;
  color: $foreground;
  font-family: 'Inter', sans-serif;
}

.total-price {
  font-size: 40rpx;
  color: $accent;
  font-weight: 700;
  margin-left: 8rpx;
  font-family: 'JetBrains Mono', monospace;
}

.modal-footer {
  padding: 20rpx 28rpx;
  padding-bottom: calc(20rpx + env(safe-area-inset-bottom));
  border-top: 1rpx solid $border;
}

.confirm-buy-btn {
  text-align: center;
  padding: 24rpx;
  background: $gradient-accent;
  color: $accent-foreground;
  font-size: 30rpx;
  font-weight: 600;
  border-radius: $radius-xl;
  box-shadow: $shadow-accent;
  font-family: 'Calistoga', cursive;
}

/* Result Modal */
.result-content {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 560rpx;
  background: $card;
  border-radius: $radius-2xl;
  border: 1rpx solid $border;
  box-shadow: $shadow-lg;
  padding: 60rpx 40rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.result-icon {
  font-size: 100rpx;
  margin-bottom: 24rpx;
}

.result-title {
  font-size: 36rpx;
  font-weight: 600;
  color: $foreground;
  margin-bottom: 16rpx;
  font-family: 'Calistoga', cursive;
}

.result-desc {
  font-size: 26rpx;
  color: $muted-foreground;
  text-align: center;
  margin-bottom: 36rpx;
  font-family: 'Inter', sans-serif;
}

.result-btn {
  width: 100%;
  text-align: center;
  padding: 24rpx;
  background: $gradient-accent;
  color: $accent-foreground;
  font-size: 30rpx;
  border-radius: $radius-xl;
  box-shadow: $shadow-accent;
  font-family: 'Calistoga', cursive;
}
</style>