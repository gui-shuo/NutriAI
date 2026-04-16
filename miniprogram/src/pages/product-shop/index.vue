<template>
  <view class="shop-page">
    <!-- Top Action Bar -->
    <view class="top-action-bar">
      <view class="top-bar-left">
        <text class="page-title">🛒 营养商城</text>
      </view>
      <view class="top-bar-right">
        <view class="top-bar-btn" @tap="openOrders">
          <text class="top-bar-icon">📋</text>
          <text class="top-bar-label">订单</text>
        </view>
        <view class="top-bar-btn" @tap="openCart">
          <text class="top-bar-icon">🛒</text>
          <text class="top-bar-label">购物车</text>
          <view class="badge" v-if="cartCount > 0">{{ cartCount > 99 ? '99+' : cartCount }}</view>
        </view>
      </view>
    </view>

    <!-- Disclaimer -->
    <view class="disclaimer-tip" v-if="showDisclaimer">
      <text>📋 商品信息来源于第三方，仅供参考。营养补充剂不能替代正常饮食和药物治疗。</text>
      <text class="dismiss" @tap="showDisclaimer = false">✕</text>
    </view>

    <!-- Search Bar -->
    <view class="search-bar">
      <view class="search-inner">
        <text class="search-icon">🔍</text>
        <input
          class="search-input"
          v-model="keyword"
          placeholder="搜索营养食品..."
          confirm-type="search"
          @confirm="handleSearch"
        />
        <text class="clear-btn" v-if="keyword" @tap="clearSearch">✕</text>
      </view>
    </view>

    <!-- Category Tabs -->
    <scroll-view class="category-tabs" scroll-x :show-scrollbar="false">
      <view class="cat-tab" :class="{ active: currentCategory === '' }" @tap="switchCategory('')">全部</view>
      <view
        v-for="cat in categories"
        :key="cat.id || cat.name"
        class="cat-tab"
        :class="{ active: currentCategory === (cat.id || cat.name) }"
        @tap="switchCategory(cat.id || cat.name)"
      >{{ cat.name }}</view>
    </scroll-view>

    <!-- Recommended Section -->
    <view class="recommend-section" v-if="!keyword && !currentCategory && recommended.length > 0">
      <view class="section-header">
        <text class="section-title">🌟 为你推荐</text>
      </view>
      <scroll-view class="recommend-scroll" scroll-x :show-scrollbar="false">
        <view class="recommend-card" v-for="item in recommended" :key="item.id" @tap="openDetail(item)">
          <image class="recommend-img" :src="item.imageUrl || item.image" mode="aspectFill" />
          <text class="recommend-name">{{ item.name }}</text>
          <text class="recommend-price">¥{{ formatPrice(item.salePrice || item.price) }}</text>
        </view>
      </scroll-view>
    </view>

    <!-- Product Grid -->
    <view class="product-grid" v-if="products.length > 0">
      <view class="product-card" v-for="item in products" :key="item.id">
        <view class="card-tap-area" @tap="openDetail(item)">
          <image class="product-img" :src="item.imageUrl || item.image" mode="aspectFill" />
          <view v-if="item.isRecommended" class="rec-badge">推荐</view>
          <view class="product-info">
            <text class="product-name">{{ item.name }}</text>
            <view class="meta-row">
              <text class="stars">{{ getStars(item.rating) }}</text>
              <text class="sales-count">已售 {{ item.salesCount || 0 }}</text>
            </view>
            <view class="price-row">
              <text class="current-price">¥{{ formatPrice(item.salePrice || item.price) }}</text>
              <text class="original-price" v-if="item.originalPrice && item.originalPrice > (item.salePrice || item.price)">
                ¥{{ formatPrice(item.originalPrice) }}
              </text>
            </view>
          </view>
        </view>
        <view class="add-cart-btn" @tap.stop="quickAddToCart(item)">
          <text class="add-cart-icon">+</text>
        </view>
      </view>
    </view>

    <!-- Empty State -->
    <view class="empty-state" v-else-if="!loading">
      <text class="empty-icon">🛒</text>
      <text class="empty-text">{{ keyword ? '未找到相关商品' : '暂无商品' }}</text>
    </view>

    <!-- Loading & No More -->
    <view class="loading-more" v-if="loading">
      <text>加载中...</text>
    </view>
    <view class="no-more" v-if="!loading && noMore && products.length > 0">
      <text>— 没有更多了 —</text>
    </view>

    <!-- Bottom spacer for floating cart -->
    <view style="height: 20rpx"></view>

    <!-- Floating Cart Button -->
    <view class="floating-cart" v-if="cartCount > 0 && !cartVisible && !detailVisible" @tap="openCart">
      <text class="floating-cart-emoji">🛒</text>
      <view class="floating-cart-badge">{{ cartCount }}</view>
    </view>

    <!-- ===== Product Detail Modal ===== -->
    <view class="modal-mask" v-if="detailVisible" @tap="detailVisible = false">
      <view class="modal-content detail-modal" @tap.stop>
        <view class="modal-header">
          <text class="modal-title">商品详情</text>
          <text class="modal-close" @tap="detailVisible = false">✕</text>
        </view>

        <view class="detail-loading" v-if="detailLoading">
          <text>加载中...</text>
        </view>

        <scroll-view class="modal-body" scroll-y v-if="detailProduct && !detailLoading">
          <image class="detail-main-img" :src="detailProduct.imageUrl || detailProduct.image" mode="aspectFill" />

          <view class="detail-info-area">
            <text class="detail-name">{{ detailProduct.name }}</text>
            <view class="detail-brand-tag" v-if="detailProduct.brand">
              <text>{{ detailProduct.brand }}</text>
            </view>
            <text class="detail-brief" v-if="detailProduct.brief">{{ detailProduct.brief }}</text>

            <view class="detail-rating-row" v-if="detailProduct.rating">
              <text class="stars">{{ getStars(detailProduct.rating) }}</text>
              <text class="review-count">{{ detailProduct.reviewCount || 0 }} 评价</text>
            </view>

            <view class="detail-price-row">
              <text class="detail-sale-price">¥{{ formatPrice(detailProduct.salePrice || detailProduct.price) }}</text>
              <text class="detail-orig-price" v-if="detailProduct.originalPrice && detailProduct.originalPrice > (detailProduct.salePrice || detailProduct.price)">
                ¥{{ formatPrice(detailProduct.originalPrice) }}
              </text>
            </view>

            <view class="detail-stock-row">
              <view class="stock-tag" :class="{ danger: (detailProduct.stock ?? 999) <= 0 }">
                {{ (detailProduct.stock ?? 999) > 0 ? `库存 ${detailProduct.stock ?? '充足'}` : '已售罄' }}
              </view>
              <text class="detail-sales">已售 {{ detailProduct.salesCount || 0 }}件</text>
            </view>

            <view class="detail-tags" v-if="detailProduct.tags && detailProduct.tags.length">
              <view class="tag-chip" v-for="(t, i) in detailProduct.tags" :key="i">{{ t }}</view>
            </view>

            <view class="detail-section" v-if="detailProduct.description">
              <text class="detail-section-title">产品介绍</text>
              <text class="detail-section-body">{{ detailProduct.description }}</text>
            </view>

            <view class="detail-section" v-if="detailProduct.specifications && Object.keys(parseSpecs(detailProduct.specifications)).length">
              <text class="detail-section-title">规格参数</text>
              <view class="spec-table">
                <view class="spec-row" v-for="(v, k) in parseSpecs(detailProduct.specifications)" :key="k">
                  <text class="spec-key">{{ k }}</text>
                  <text class="spec-val">{{ v }}</text>
                </view>
              </view>
            </view>

            <view class="detail-qty-row">
              <text class="qty-label">数量</text>
              <view class="quantity-selector">
                <view class="qty-btn" :class="{ disabled: detailQty <= 1 }" @tap="detailQty > 1 && detailQty--">−</view>
                <text class="qty-value">{{ detailQty }}</text>
                <view class="qty-btn" :class="{ disabled: detailQty >= maxDetailQty }" @tap="detailQty < maxDetailQty && detailQty++">+</view>
              </view>
            </view>
          </view>
        </scroll-view>

        <view class="modal-footer detail-footer" v-if="detailProduct && !detailLoading">
          <view class="detail-view-full" @tap="goFullDetail(detailProduct.id)">查看完整详情</view>
          <view class="detail-cart-btn" :class="{ disabled: (detailProduct.stock ?? 999) <= 0 }" @tap="addToCartFromDetail">
            加入购物车
          </view>
        </view>
      </view>
    </view>

    <!-- ===== Cart Modal ===== -->
    <view class="modal-mask" v-if="cartVisible" @tap="cartVisible = false">
      <view class="modal-content cart-modal" @tap.stop>
        <view class="modal-header">
          <text class="modal-title">🛒 购物车</text>
          <text class="modal-close" @tap="cartVisible = false">✕</text>
        </view>

        <view class="modal-empty" v-if="cart.length === 0">
          <text class="modal-empty-icon">🛒</text>
          <text class="modal-empty-text">购物车是空的</text>
        </view>

        <template v-else>
          <scroll-view class="modal-body" scroll-y>
            <view class="cart-item" v-for="(item, idx) in cart" :key="idx">
              <image class="cart-item-img" :src="item.imageUrl" mode="aspectFill" />
              <view class="cart-item-info">
                <text class="cart-item-name">{{ item.name }}</text>
                <text class="cart-item-price">¥{{ formatPrice(item.salePrice) }}</text>
                <view class="quantity-selector small">
                  <view class="qty-btn" :class="{ disabled: item.quantity <= 1 }" @tap="changeCartQty(idx, -1)">−</view>
                  <text class="qty-value">{{ item.quantity }}</text>
                  <view class="qty-btn" :class="{ disabled: item.quantity >= 10 }" @tap="changeCartQty(idx, 1)">+</view>
                </view>
              </view>
              <view class="cart-item-del" @tap="removeFromCart(idx)">
                <text>✕</text>
              </view>
            </view>
          </scroll-view>

          <view class="cart-summary">
            <view class="cart-total-row">
              <text>合计：</text>
              <text class="cart-total-amount">¥{{ formatPrice(cartTotal) }}</text>
            </view>
            <view class="cart-checkout-btn" @tap="openCheckout">去结算（{{ cartItemCount }}件）</view>
          </view>
        </template>
      </view>
    </view>

    <!-- ===== Checkout Modal ===== -->
    <view class="modal-mask" v-if="checkoutVisible" @tap="checkoutVisible = false">
      <view class="modal-content checkout-modal" @tap.stop>
        <view class="modal-header">
          <text class="modal-title">确认订单</text>
          <text class="modal-close" @tap="checkoutVisible = false">✕</text>
        </view>

        <scroll-view class="modal-body" scroll-y>
          <!-- Address Section -->
          <view class="ck-section">
            <view class="ck-section-head">
              <text class="ck-section-title">📍 收货信息</text>
              <text class="ck-link" @tap="goAddressManage">管理</text>
            </view>

            <view v-if="savedAddresses.length > 0 && !useManualAddress">
              <view
                class="addr-item"
                :class="{ active: selectedAddressId === addr.id }"
                v-for="addr in savedAddresses.slice(0, 3)"
                :key="addr.id"
                @tap="selectAddress(addr)"
              >
                <view class="addr-radio">
                  <view class="radio-dot" :class="{ checked: selectedAddressId === addr.id }"></view>
                </view>
                <view class="addr-body">
                  <view class="addr-top">
                    <text class="addr-name">{{ addr.receiverName }}</text>
                    <text class="addr-phone">{{ addr.receiverPhone }}</text>
                    <view class="addr-default" v-if="addr.isDefault">默认</view>
                  </view>
                  <text class="addr-detail">{{ getFullAddress(addr) }}</text>
                </view>
              </view>
              <view class="addr-new-link" @tap="useManualAddress = true">
                <text>+ 使用新地址</text>
              </view>
            </view>

            <view v-else>
              <view class="addr-back" v-if="savedAddresses.length > 0" @tap="useManualAddress = false">
                <text>← 选择已有地址</text>
              </view>
              <view class="form-group">
                <input class="form-input" v-model="checkoutForm.receiverName" placeholder="收货人姓名" />
              </view>
              <view class="form-group">
                <input class="form-input" v-model="checkoutForm.receiverPhone" placeholder="联系电话" type="number" maxlength="11" />
              </view>
              <view class="form-group">
                <textarea class="form-textarea" v-model="checkoutForm.receiverAddress" placeholder="详细收货地址" :maxlength="200" />
              </view>
            </view>
          </view>

          <!-- Remark -->
          <view class="ck-section">
            <text class="ck-section-title">备注</text>
            <view class="form-group">
              <input class="form-input" v-model="checkoutForm.remark" placeholder="选填" />
            </view>
          </view>

          <!-- Items -->
          <view class="ck-section">
            <text class="ck-section-title">商品清单</text>
            <view class="ck-item" v-for="(item, idx) in cart" :key="idx">
              <text class="ck-item-name">{{ item.name }} × {{ item.quantity }}</text>
              <text class="ck-item-price">¥{{ formatPrice(item.salePrice * item.quantity) }}</text>
            </view>
          </view>

          <!-- Total -->
          <view class="ck-total">
            <text>应付总额：</text>
            <text class="ck-total-amount">¥{{ formatPrice(cartTotal) }}</text>
          </view>

          <view class="ck-alert">
            <text>ℹ️ 模拟支付模式：点击提交后将自动完成支付</text>
          </view>
        </scroll-view>

        <view class="modal-footer">
          <view class="submit-btn" :class="{ disabled: checkoutLoading }" @tap="handleCheckout">
            {{ checkoutLoading ? '提交中...' : '提交订单并支付' }}
          </view>
        </view>
      </view>
    </view>

    <!-- ===== Orders Modal ===== -->
    <view class="modal-mask" v-if="ordersVisible" @tap="ordersVisible = false">
      <view class="modal-content orders-modal" @tap.stop>
        <view class="modal-header">
          <text class="modal-title">📋 我的订单</text>
          <text class="modal-close" @tap="ordersVisible = false">✕</text>
        </view>

        <view class="detail-loading" v-if="ordersLoading">
          <text>加载中...</text>
        </view>

        <view class="modal-empty" v-else-if="orders.length === 0">
          <text class="modal-empty-icon">📋</text>
          <text class="modal-empty-text">暂无订单</text>
        </view>

        <scroll-view class="modal-body" scroll-y v-else>
          <view class="order-card" v-for="order in orders" :key="order.orderNo">
            <view class="order-head">
              <text class="order-no">{{ order.orderNo }}</text>
              <text class="order-status" :class="orderStatusClass(order.orderStatus)">{{ orderStatusText(order.orderStatus) }}</text>
            </view>
            <view class="order-products">
              <view class="order-prod" v-for="(item, i) in (order.items || []).slice(0, 2)" :key="i">
                <text>{{ item.productName }} × {{ item.quantity }}</text>
              </view>
              <text class="order-more" v-if="(order.items || []).length > 2">等{{ order.items.length }}件商品</text>
            </view>
            <view class="order-foot">
              <view class="order-money">
                <text class="pay-status" :class="payStatusClass(order.paymentStatus)">{{ payStatusText(order.paymentStatus) }}</text>
                <text class="order-amount">¥{{ formatPrice(order.totalAmount) }}</text>
              </view>
              <view class="order-actions">
                <view class="action-chip warn" v-if="order.orderStatus === 'PAID'" @tap="handleShip(order)">模拟发货</view>
                <view class="action-chip success" v-if="order.orderStatus === 'SHIPPED'" @tap="handleReceive(order)">确认收货</view>
                <view class="action-chip danger" v-if="['PAID','SHIPPED','DELIVERED'].includes(order.orderStatus)" @tap="handleRefund(order)">退款</view>
              </view>
            </view>
            <view class="order-tracking" v-if="order.trackingNo">
              <text>快递单号：{{ order.trackingNo }}</text>
            </view>
          </view>
        </scroll-view>
      </view>
    </view>

    <!-- ===== Order Result Popup ===== -->
    <view class="modal-mask" v-if="orderResultVisible" @tap="orderResultVisible = false">
      <view class="result-popup" @tap.stop>
        <text class="result-emoji">{{ orderSuccess ? '🎉' : '😢' }}</text>
        <text class="result-title">{{ orderSuccess ? '下单成功' : '下单失败' }}</text>
        <text class="result-desc">{{ orderSuccess ? `订单号：${orderResultNo}` : orderResultError }}</text>
        <view class="result-close-btn" @tap="orderResultVisible = false">{{ orderSuccess ? '完成' : '关闭' }}</view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed, watch, onUnmounted } from 'vue'
import { onPullDownRefresh, onReachBottom, onShow } from '@dcloudio/uni-app'
import { productApi, addressApi } from '@/services/api'
import { request } from '@/utils/request'
import { checkLogin } from '@/utils/common'

// ────── Constants ──────
const CART_KEY = 'nutriai_cart'
const PAGE_SIZE = 10
const categoryNameMap: Record<string, string> = {
  EQUIPMENT: '健身器材',
  HEALTH_FOOD: '营养食品',
  ORGANIC: '有机食品',
  PROTEIN: '蛋白质',
  SUPPLEMENT: '营养补剂',
  VITAMIN: '维生素'
}

// ────── Shared State ──────
const showDisclaimer = ref(true)
const keyword = ref('')
const currentCategory = ref('')
const categories = ref<any[]>([])
const products = ref<any[]>([])
const recommended = ref<any[]>([])
const loading = ref(false)
const noMore = ref(false)
const page = ref(0)

// ────── Utility ──────
function formatPrice(val: number | undefined): string {
  return (val || 0).toFixed(2)
}

function getStars(rating: number | undefined): string {
  const r = Math.round(rating || 0)
  return '★'.repeat(r) + '☆'.repeat(5 - r)
}

function parseSpecs(specs: any): Record<string, string> {
  if (!specs) return {}
  if (typeof specs === 'string') {
    try { return JSON.parse(specs) } catch { return { '规格': specs } }
  }
  if (typeof specs === 'object') return specs
  return {}
}

function extractList(data: any): any[] {
  return data?.content || data?.records || data?.list || (Array.isArray(data) ? data : [])
}

// ────── Data Loading ──────
async function loadCategories() {
  try {
    const res = await productApi.getCategories()
    const raw = res.data || []
    categories.value = raw.map((c: any) => {
      if (typeof c === 'string') return { id: c, name: categoryNameMap[c] || c }
      return c
    })
  } catch { /* ignore */ }
}

async function loadRecommended() {
  try {
    const res = await productApi.getRecommended()
    recommended.value = extractList(res.data)
  } catch { /* ignore */ }
}

async function loadProducts(isRefresh = false) {
  if (loading.value) return
  loading.value = true
  try {
    const params: any = { page: page.value, size: PAGE_SIZE }
    if (currentCategory.value) params.category = currentCategory.value
    if (keyword.value.trim()) params.keyword = keyword.value.trim()

    const res = keyword.value.trim()
      ? await productApi.search(keyword.value.trim())
      : await productApi.getProducts(params)

    const list = extractList(res.data)
    products.value = isRefresh ? list : [...products.value, ...list]
    noMore.value = list.length < PAGE_SIZE
  } catch {
    uni.showToast({ title: '加载失败', icon: 'none' })
  } finally {
    loading.value = false
  }
}

function switchCategory(cat: string) {
  if (currentCategory.value === cat) return
  currentCategory.value = cat
  keyword.value = ''
  refreshData()
}

function handleSearch() {
  currentCategory.value = ''
  refreshData()
}

function clearSearch() {
  keyword.value = ''
  refreshData()
}

function refreshData() {
  page.value = 0
  noMore.value = false
  loadProducts(true)
}

// ────── Product Detail Modal ──────
const detailVisible = ref(false)
const detailLoading = ref(false)
const detailProduct = ref<any>(null)
const detailQty = ref(1)
const maxDetailQty = computed(() => Math.min(detailProduct.value?.stock ?? 10, 10))

async function openDetail(item: any) {
  detailQty.value = 1
  detailProduct.value = item
  detailVisible.value = true
  detailLoading.value = true
  try {
    const res = await productApi.getProduct(item.id)
    if (res.data) {
      const p = res.data
      if (!p.price && p.salePrice) p.price = p.salePrice
      detailProduct.value = p
    }
  } catch { /* keep list data as fallback */ }
  finally { detailLoading.value = false }
}

function goFullDetail(id: number) {
  detailVisible.value = false
  uni.navigateTo({ url: `/pages/product-shop/detail?id=${id}` })
}

// ────── Cart ──────
function loadCartFromStorage(): any[] {
  try {
    const raw = uni.getStorageSync(CART_KEY)
    return raw ? (typeof raw === 'string' ? JSON.parse(raw) : raw) : []
  } catch { return [] }
}

function saveCart() {
  try { uni.setStorageSync(CART_KEY, JSON.stringify(cart.value)) } catch { /* ignore */ }
}

const cart = ref<any[]>(loadCartFromStorage())
const cartVisible = ref(false)

const cartCount = computed(() => cart.value.reduce((s, i) => s + i.quantity, 0))
const cartItemCount = computed(() => cart.value.length)
const cartTotal = computed(() => cart.value.reduce((s, i) => s + i.salePrice * i.quantity, 0))

watch(cart, saveCart, { deep: true })

function quickAddToCart(p: any) {
  if (!checkLogin()) return
  const existing = cart.value.find(c => c.productId === p.id)
  if (existing) {
    if (existing.quantity < 10) existing.quantity++
  } else {
    cart.value.push({
      productId: p.id,
      name: p.name,
      imageUrl: p.imageUrl || p.image || '',
      salePrice: p.salePrice || p.price || 0,
      quantity: 1
    })
  }
  uni.showToast({ title: `已添加 ${p.name}`, icon: 'none', duration: 1200 })
}

function addToCartFromDetail() {
  if (!detailProduct.value || (detailProduct.value.stock ?? 999) <= 0) return
  if (!checkLogin()) return
  const p = detailProduct.value
  const existing = cart.value.find(c => c.productId === p.id)
  if (existing) {
    existing.quantity = Math.min(existing.quantity + detailQty.value, 10)
  } else {
    cart.value.push({
      productId: p.id,
      name: p.name,
      imageUrl: p.imageUrl || p.image || '',
      salePrice: p.salePrice || p.price || 0,
      quantity: detailQty.value
    })
  }
  detailVisible.value = false
  uni.showToast({ title: `已添加 ${p.name} × ${detailQty.value}`, icon: 'none', duration: 1200 })
}

function changeCartQty(idx: number, delta: number) {
  const item = cart.value[idx]
  if (!item) return
  const nv = item.quantity + delta
  if (nv < 1 || nv > 10) return
  item.quantity = nv
}

function removeFromCart(idx: number) {
  cart.value.splice(idx, 1)
}

function openCart() {
  if (!checkLogin()) return
  cartVisible.value = true
}

// ────── Checkout ──────
const checkoutVisible = ref(false)
const checkoutLoading = ref(false)
const checkoutForm = ref({ receiverName: '', receiverPhone: '', receiverAddress: '', remark: '' })
const savedAddresses = ref<any[]>([])
const selectedAddressId = ref<number>(0)
const useManualAddress = ref(false)

const orderResultVisible = ref(false)
const orderSuccess = ref(false)
const orderResultNo = ref('')
const orderResultError = ref('')

function getFullAddress(addr: any): string {
  return `${addr.province || ''}${addr.city || ''}${addr.district || ''}${addr.detailAddress || ''}`
}

function selectAddress(addr: any) {
  selectedAddressId.value = addr.id
}

function goAddressManage() {
  uni.navigateTo({ url: '/pages/address/index' })
}

async function loadSavedAddresses() {
  try {
    const res = await addressApi.getList()
    savedAddresses.value = res.data || []
    if (savedAddresses.value.length > 0) {
      const def = savedAddresses.value.find((a: any) => a.isDefault) || savedAddresses.value[0]
      selectedAddressId.value = def.id
      useManualAddress.value = false
    } else {
      useManualAddress.value = true
    }
  } catch {
    savedAddresses.value = []
    useManualAddress.value = true
  }
}

function openCheckout() {
  if (!cart.value.length) return
  cartVisible.value = false
  checkoutForm.value = { receiverName: '', receiverPhone: '', receiverAddress: '', remark: '' }
  selectedAddressId.value = 0
  useManualAddress.value = false
  checkoutVisible.value = true
  loadSavedAddresses()
}

async function handleCheckout() {
  if (checkoutLoading.value) return
  let receiverName: string, receiverPhone: string, receiverAddress: string
  const { remark } = checkoutForm.value

  if (selectedAddressId.value && !useManualAddress.value) {
    const addr = savedAddresses.value.find(a => a.id === selectedAddressId.value)
    if (!addr) return uni.showToast({ title: '请选择收货地址', icon: 'none' })
    receiverName = addr.receiverName
    receiverPhone = addr.receiverPhone
    receiverAddress = getFullAddress(addr)
  } else {
    receiverName = checkoutForm.value.receiverName
    receiverPhone = checkoutForm.value.receiverPhone
    receiverAddress = checkoutForm.value.receiverAddress
  }

  if (!receiverName || !receiverPhone || !receiverAddress) {
    return uni.showToast({ title: '请填写完整收货信息', icon: 'none' })
  }

  checkoutLoading.value = true
  try {
    const items = cart.value.map(c => ({ productId: c.productId, quantity: c.quantity }))
    const res = await productApi.createOrder({ items, receiverName, receiverPhone, receiverAddress, remark })
    const order = res.data
    const orderNo = order?.orderNo || order

    await productApi.simulatePay(orderNo)

    cart.value = []
    checkoutVisible.value = false
    orderSuccess.value = true
    orderResultNo.value = orderNo
    orderResultVisible.value = true
  } catch (e: any) {
    checkoutVisible.value = false
    orderSuccess.value = false
    orderResultError.value = e?.message || '订单创建失败'
    orderResultVisible.value = true
  } finally {
    checkoutLoading.value = false
  }
}

// Listen for address selection from address manage page
uni.$on('address-selected', (addr: any) => {
  if (!savedAddresses.value.find((a: any) => a.id === addr.id)) {
    savedAddresses.value.unshift(addr)
  }
  selectedAddressId.value = addr.id
  useManualAddress.value = false
})

onUnmounted(() => { uni.$off('address-selected') })

// ────── Orders ──────
const ordersVisible = ref(false)
const ordersLoading = ref(false)
const orders = ref<any[]>([])

function orderStatusText(s: string): string {
  return ({ PENDING_PAYMENT: '待支付', PAID: '已支付', SHIPPED: '已发货', DELIVERED: '已送达', COMPLETED: '已完成', CANCELLED: '已取消', REFUNDED: '已退款' } as any)[s] || s
}
function orderStatusClass(s: string): string {
  return ({ PENDING_PAYMENT: 'warn', PAID: 'success', SHIPPED: 'info', DELIVERED: 'success', COMPLETED: 'info', CANCELLED: 'muted', REFUNDED: 'danger' } as any)[s] || ''
}
function payStatusText(s: string): string {
  return ({ PAID: '已支付', PENDING: '待支付', REFUNDED: '已退款', EXPIRED: '已超时' } as any)[s] || s
}
function payStatusClass(s: string): string {
  return ({ PAID: 'success', PENDING: 'warn', REFUNDED: 'danger', EXPIRED: 'muted' } as any)[s] || ''
}

async function openOrders() {
  if (!checkLogin()) return
  ordersVisible.value = true
  ordersLoading.value = true
  try {
    const res = await productApi.getOrders({ page: 0, size: 20 })
    orders.value = extractList(res.data)
  } catch {
    uni.showToast({ title: '获取订单失败', icon: 'none' })
  } finally {
    ordersLoading.value = false
  }
}

async function handleShip(order: any) {
  try {
    await request({ url: `/products/orders/${order.orderNo}/simulate-ship`, method: 'POST' })
    uni.showToast({ title: '已模拟发货', icon: 'none' })
    openOrders()
  } catch {
    uni.showToast({ title: '操作失败', icon: 'none' })
  }
}

async function handleReceive(order: any) {
  try {
    await productApi.confirmReceive(order.orderNo)
    uni.showToast({ title: '已确认收货', icon: 'none' })
    openOrders()
  } catch {
    uni.showToast({ title: '操作失败', icon: 'none' })
  }
}

async function handleRefund(order: any) {
  try {
    await productApi.simulateRefund(order.orderNo)
    uni.showToast({ title: '退款成功', icon: 'none' })
    openOrders()
  } catch {
    uni.showToast({ title: '退款失败', icon: 'none' })
  }
}

// ────── Lifecycle ──────
onShow(() => {
  loadCategories()
  loadRecommended()
  refreshData()
  // Reload cart from storage (may have changed in detail page)
  cart.value = loadCartFromStorage()
})

onPullDownRefresh(() => {
  page.value = 0
  noMore.value = false
  Promise.all([loadProducts(true), loadRecommended()])
    .then(() => uni.stopPullDownRefresh())
})

onReachBottom(() => {
  if (noMore.value || loading.value) return
  page.value++
  loadProducts()
})
</script>

<style scoped lang="scss">
.shop-page {
  min-height: 100vh;
  background: #F5F7FA;
  padding-bottom: env(safe-area-inset-bottom);
}

/* ===== Top Action Bar ===== */
.top-action-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20rpx 32rpx;
  background: #fff;
  box-shadow: 0 2rpx 12rpx rgba(0, 0, 0, 0.04);
  position: sticky;
  top: 0;
  z-index: 20;
}

.page-title {
  font-size: 34rpx;
  font-weight: 700;
  color: #059669;
}

.top-bar-right {
  display: flex;
  align-items: center;
  gap: 20rpx;
}

.top-bar-btn {
  display: flex;
  align-items: center;
  gap: 6rpx;
  padding: 10rpx 20rpx;
  border-radius: 48rpx;
  background: #F0F4F8;
  position: relative;
}

.top-bar-icon {
  font-size: 28rpx;
}

.top-bar-label {
  font-size: 24rpx;
  color: #1A1A2E;
}

.badge {
  position: absolute;
  top: -8rpx;
  right: -8rpx;
  min-width: 32rpx;
  height: 32rpx;
  line-height: 32rpx;
  text-align: center;
  font-size: 20rpx;
  font-weight: 600;
  color: #fff;
  background: linear-gradient(135deg, #10B981, #059669);
  border-radius: 100rpx;
  padding: 0 8rpx;
}

/* ===== Disclaimer ===== */
.disclaimer-tip {
  background: #ECFDF5;
  color: #059669;
  border-radius: 16rpx;
  padding: 14rpx 48rpx 14rpx 20rpx;
  font-size: 22rpx;
  margin: 12rpx 32rpx;
  position: relative;
}

.disclaimer-tip .dismiss {
  position: absolute;
  right: 16rpx;
  top: 50%;
  transform: translateY(-50%);
  font-size: 28rpx;
  color: #8896AB;
}

/* ===== Search Bar ===== */
.search-bar {
  background: #F5F7FA;
  padding: 12rpx 32rpx;
}

.search-inner {
  display: flex;
  align-items: center;
  background: #fff;
  border-radius: 100rpx;
  padding: 0 28rpx;
  height: 76rpx;
  box-shadow: 0 2rpx 12rpx rgba(0, 0, 0, 0.04);
}

.search-icon {
  font-size: 28rpx;
  margin-right: 12rpx;
}

.search-input {
  flex: 1;
  font-size: 28rpx;
  height: 76rpx;
}

.clear-btn {
  color: #8896AB;
  font-size: 28rpx;
  padding: 8rpx;
}

/* ===== Category Tabs ===== */
.category-tabs {
  white-space: nowrap;
  background: #F5F7FA;
  padding: 16rpx 24rpx;
}

.cat-tab {
  display: inline-block;
  padding: 14rpx 32rpx;
  margin: 0 8rpx;
  border-radius: 100rpx;
  font-size: 26rpx;
  color: #8896AB;
  background: #fff;
  box-shadow: 0 2rpx 8rpx rgba(0, 0, 0, 0.03);
  transition: all 0.2s ease;
}

.cat-tab.active {
  background: linear-gradient(135deg, #10B981, #059669);
  color: #fff;
  box-shadow: 0 4rpx 16rpx rgba(16, 185, 129, 0.3);
}

/* ===== Recommended ===== */
.recommend-section {
  background: #F5F7FA;
  margin-top: 8rpx;
  padding: 24rpx 0;
}

.section-header {
  padding: 0 32rpx;
  margin-bottom: 16rpx;
}

.section-title {
  font-size: 30rpx;
  font-weight: 700;
  color: #1A1A2E;
}

.recommend-scroll {
  white-space: nowrap;
  padding: 0 24rpx;
}

.recommend-card {
  display: inline-block;
  width: 260rpx;
  margin: 0 10rpx;
  vertical-align: top;
  background: #fff;
  border-radius: 24rpx;
  overflow: hidden;
  box-shadow: 0 2rpx 12rpx rgba(0, 0, 0, 0.04);
  padding-bottom: 16rpx;
}

.recommend-img {
  width: 260rpx;
  height: 240rpx;
  border-radius: 24rpx 24rpx 0 0;
}

.recommend-name {
  display: block;
  font-size: 24rpx;
  color: #1A1A2E;
  margin-top: 12rpx;
  padding: 0 16rpx;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.recommend-price {
  font-size: 28rpx;
  color: #EF4444;
  font-weight: 700;
  display: block;
  margin-top: 4rpx;
  padding: 0 16rpx;
}

/* ===== Product Grid ===== */
.product-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  padding: 16rpx 32rpx;
  gap: 24rpx;
  margin-top: 4rpx;
}

.product-card {
  background: #fff;
  border-radius: 24rpx;
  overflow: hidden;
  box-shadow: 0 2rpx 12rpx rgba(0, 0, 0, 0.04);
  position: relative;
  min-width: 0;
}

.card-tap-area {
  position: relative;
}

.product-img {
  width: 100%;
  height: 340rpx;
}

.rec-badge {
  position: absolute;
  top: 12rpx;
  right: 12rpx;
  font-size: 20rpx;
  color: #fff;
  background: linear-gradient(135deg, #10B981, #059669);
  padding: 6rpx 16rpx;
  border-radius: 100rpx;
  font-weight: 600;
}

.product-info {
  padding: 16rpx 20rpx 52rpx;
}

.product-name {
  font-size: 26rpx;
  color: #1A1A2E;
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  min-height: 72rpx;
}

.meta-row {
  display: flex;
  align-items: center;
  gap: 8rpx;
  margin-top: 8rpx;
}

.stars {
  font-size: 22rpx;
  color: #F59E0B;
  letter-spacing: -2rpx;
}

.sales-count {
  font-size: 22rpx;
  color: #8896AB;
}

.price-row {
  display: flex;
  align-items: baseline;
  gap: 10rpx;
  margin-top: 10rpx;
}

.current-price {
  font-size: 32rpx;
  color: #EF4444;
  font-weight: 700;
}

.original-price {
  font-size: 22rpx;
  color: #8896AB;
  text-decoration: line-through;
}

.add-cart-btn {
  position: absolute;
  right: 16rpx;
  bottom: 16rpx;
  width: 56rpx;
  height: 56rpx;
  border-radius: 50%;
  background: linear-gradient(135deg, #10B981, #059669);
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 4rpx 12rpx rgba(16, 185, 129, 0.3);
}

.add-cart-icon {
  color: #fff;
  font-size: 36rpx;
  font-weight: 700;
  line-height: 1;
}

/* ===== Empty / Loading ===== */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 200rpx 0;
}

.empty-icon {
  font-size: 80rpx;
  margin-bottom: 20rpx;
}

.empty-text {
  font-size: 28rpx;
  color: #8896AB;
}

.loading-more,
.no-more {
  text-align: center;
  padding: 30rpx;
  font-size: 24rpx;
  color: #8896AB;
}

/* ===== Floating Cart ===== */
.floating-cart {
  position: fixed;
  right: 32rpx;
  bottom: calc(160rpx + env(safe-area-inset-bottom));
  width: 100rpx;
  height: 100rpx;
  border-radius: 50%;
  background: #fff;
  box-shadow: 0 4rpx 24rpx rgba(0, 0, 0, 0.1);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 50;
}

.floating-cart-emoji {
  font-size: 40rpx;
}

.floating-cart-badge {
  position: absolute;
  top: -4rpx;
  right: -4rpx;
  min-width: 36rpx;
  height: 36rpx;
  line-height: 36rpx;
  text-align: center;
  font-size: 22rpx;
  font-weight: 700;
  color: #fff;
  background: linear-gradient(135deg, #10B981, #059669);
  border-radius: 100rpx;
  padding: 0 8rpx;
}

/* ===== Modal Base ===== */
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
  max-height: 88vh;
  background: #fff;
  border-radius: 32rpx 32rpx 0 0;
  display: flex;
  flex-direction: column;
  box-shadow: 0 -4rpx 24rpx rgba(0, 0, 0, 0.08);
  box-sizing: border-box;
  overflow-x: hidden;
}

.modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 28rpx 32rpx;
  flex-shrink: 0;
}

.modal-title {
  font-size: 32rpx;
  font-weight: 700;
  color: #1A1A2E;
}

.modal-close {
  font-size: 36rpx;
  color: #8896AB;
  padding: 8rpx;
}

.modal-body {
  flex: 1;
  overflow: hidden;
  max-height: 64vh;
}

.modal-footer {
  padding: 20rpx 32rpx;
  padding-bottom: calc(20rpx + env(safe-area-inset-bottom));
  flex-shrink: 0;
}

.modal-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 80rpx 0;
}

.modal-empty-icon {
  font-size: 64rpx;
  margin-bottom: 16rpx;
}

.modal-empty-text {
  font-size: 28rpx;
  color: #8896AB;
}

.detail-loading {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 80rpx 0;
  font-size: 28rpx;
  color: #8896AB;
}

/* ===== Detail Modal ===== */
.detail-main-img {
  width: 100%;
  height: 500rpx;
}

.detail-info-area {
  padding: 24rpx 32rpx;
}

.detail-name {
  font-size: 32rpx;
  font-weight: 700;
  color: #1A1A2E;
  line-height: 1.5;
  display: block;
}

.detail-brand-tag {
  display: inline-block;
  margin-top: 10rpx;
  font-size: 22rpx;
  color: #059669;
  background: #ECFDF5;
  padding: 6rpx 18rpx;
  border-radius: 100rpx;
}

.detail-brief {
  display: block;
  font-size: 26rpx;
  color: #8896AB;
  margin-top: 12rpx;
  line-height: 1.5;
}

.detail-rating-row {
  display: flex;
  align-items: center;
  gap: 12rpx;
  margin-top: 16rpx;
}

.review-count {
  font-size: 24rpx;
  color: #8896AB;
}

.detail-price-row {
  display: flex;
  align-items: baseline;
  gap: 12rpx;
  margin-top: 16rpx;
}

.detail-sale-price {
  font-size: 44rpx;
  color: #EF4444;
  font-weight: 700;
}

.detail-orig-price {
  font-size: 28rpx;
  color: #8896AB;
  text-decoration: line-through;
}

.detail-stock-row {
  display: flex;
  align-items: center;
  gap: 16rpx;
  margin-top: 16rpx;
}

.stock-tag {
  font-size: 22rpx;
  color: #fff;
  background: linear-gradient(135deg, #10B981, #059669);
  padding: 6rpx 18rpx;
  border-radius: 100rpx;

  &.danger {
    background: #EF4444;
  }
}

.detail-sales {
  font-size: 24rpx;
  color: #8896AB;
}

.detail-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 10rpx;
  margin-top: 16rpx;
}

.tag-chip {
  font-size: 22rpx;
  color: #059669;
  background: #ECFDF5;
  padding: 6rpx 18rpx;
  border-radius: 100rpx;
}

.detail-section {
  margin-top: 28rpx;
  padding: 24rpx;
  background: #F0F4F8;
  border-radius: 20rpx;
}

.detail-section-title {
  font-size: 28rpx;
  font-weight: 600;
  color: #1A1A2E;
  margin-bottom: 12rpx;
  display: block;
  padding-left: 16rpx;
  border-left: 6rpx solid #10B981;
}

.detail-section-body {
  font-size: 26rpx;
  color: #8896AB;
  line-height: 1.8;
  display: block;
}

.spec-table {
  margin-top: 8rpx;
}

.spec-row {
  display: flex;
  padding: 14rpx 0;

  & + .spec-row {
    border-top: 1rpx solid #EAEFF5;
  }
}

.spec-key {
  width: 200rpx;
  flex-shrink: 0;
  font-size: 26rpx;
  color: #8896AB;
}

.spec-val {
  flex: 1;
  font-size: 26rpx;
  color: #1A1A2E;
}

.detail-qty-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 28rpx;
  padding-top: 20rpx;
}

.qty-label {
  font-size: 28rpx;
  font-weight: 600;
  color: #1A1A2E;
}

/* Quantity Selector */
.quantity-selector {
  display: flex;
  align-items: center;
  gap: 4rpx;
}

.qty-btn {
  width: 56rpx;
  height: 56rpx;
  background: #F0F4F8;
  border-radius: 16rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 32rpx;
  color: #1A1A2E;

  &.disabled {
    color: #8896AB;
    opacity: 0.4;
  }
}

.qty-value {
  width: 72rpx;
  text-align: center;
  font-size: 28rpx;
  font-weight: 600;
}

.quantity-selector.small {
  .qty-btn {
    width: 48rpx;
    height: 48rpx;
    font-size: 28rpx;
  }

  .qty-value {
    width: 56rpx;
    font-size: 26rpx;
  }
}

.detail-footer {
  display: flex;
  gap: 16rpx;
}

.detail-view-full {
  flex: 1;
  text-align: center;
  padding: 22rpx;
  background: #F0F4F8;
  border-radius: 48rpx;
  font-size: 28rpx;
  color: #1A1A2E;
}

.detail-cart-btn {
  flex: 2;
  text-align: center;
  padding: 22rpx;
  background: linear-gradient(135deg, #10B981, #059669);
  color: #fff;
  font-size: 28rpx;
  font-weight: 600;
  border-radius: 48rpx;
  box-shadow: 0 4rpx 16rpx rgba(16, 185, 129, 0.3);

  &.disabled {
    opacity: 0.5;
  }
}

/* ===== Cart Modal ===== */
.cart-item {
  display: flex;
  align-items: center;
  padding: 20rpx 32rpx;
  gap: 16rpx;

  & + .cart-item {
    border-top: 1rpx solid #EAEFF5;
  }
}

.cart-item-img {
  width: 120rpx;
  height: 120rpx;
  border-radius: 16rpx;
  flex-shrink: 0;
}

.cart-item-info {
  flex: 1;
  min-width: 0;
}

.cart-item-name {
  font-size: 26rpx;
  color: #1A1A2E;
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  margin-bottom: 6rpx;
}

.cart-item-price {
  font-size: 28rpx;
  color: #EF4444;
  font-weight: 600;
  display: block;
  margin-bottom: 8rpx;
}

.cart-item-del {
  padding: 12rpx;
  font-size: 28rpx;
  color: #8896AB;
  flex-shrink: 0;
}

.cart-summary {
  padding: 20rpx 32rpx;
  padding-bottom: calc(20rpx + env(safe-area-inset-bottom));
  flex-shrink: 0;
}

.cart-total-row {
  display: flex;
  align-items: baseline;
  justify-content: flex-end;
  margin-bottom: 16rpx;
  font-size: 28rpx;
  color: #1A1A2E;
}

.cart-total-amount {
  font-size: 40rpx;
  color: #EF4444;
  font-weight: 700;
  margin-left: 8rpx;
}

.cart-checkout-btn {
  text-align: center;
  padding: 24rpx;
  background: linear-gradient(135deg, #10B981, #059669);
  color: #fff;
  font-size: 30rpx;
  font-weight: 600;
  border-radius: 48rpx;
  box-shadow: 0 4rpx 16rpx rgba(16, 185, 129, 0.3);
}

/* ===== Checkout Modal ===== */
.ck-section {
  padding: 20rpx 32rpx;

  & + .ck-section {
    border-top: 1rpx solid #EAEFF5;
  }
}

.ck-section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16rpx;
}

.ck-section-title {
  font-size: 28rpx;
  font-weight: 600;
  color: #1A1A2E;
  display: block;
  margin-bottom: 12rpx;
}

.ck-link {
  font-size: 26rpx;
  color: #10B981;
}

.addr-item {
  display: flex;
  align-items: flex-start;
  gap: 16rpx;
  padding: 18rpx;
  background: #F0F4F8;
  border-radius: 24rpx;
  margin-bottom: 12rpx;
  transition: all 0.2s ease;

  &.active {
    background: #ECFDF5;
  }
}

.addr-radio {
  padding-top: 6rpx;
}

.radio-dot {
  width: 36rpx;
  height: 36rpx;
  border: 2rpx solid #EAEFF5;
  border-radius: 50%;

  &.checked {
    border-color: #10B981;
    background: #10B981;
    position: relative;

    &::after {
      content: '';
      position: absolute;
      top: 50%;
      left: 50%;
      transform: translate(-50%, -50%);
      width: 16rpx;
      height: 16rpx;
      border-radius: 50%;
      background: #fff;
    }
  }
}

.addr-body {
  flex: 1;
  min-width: 0;
}

.addr-top {
  display: flex;
  align-items: center;
  gap: 12rpx;
  margin-bottom: 8rpx;
  flex-wrap: wrap;
}

.addr-name {
  font-size: 28rpx;
  font-weight: 600;
  color: #1A1A2E;
}

.addr-phone {
  font-size: 26rpx;
  color: #8896AB;
}

.addr-default {
  font-size: 18rpx;
  color: #fff;
  background: linear-gradient(135deg, #10B981, #059669);
  padding: 2rpx 12rpx;
  border-radius: 100rpx;
}

.addr-detail {
  font-size: 24rpx;
  color: #8896AB;
  line-height: 1.4;
  display: block;
}

.addr-new-link {
  text-align: center;
  padding: 16rpx;
  font-size: 26rpx;
  color: #10B981;
  border: 1rpx dashed #10B981;
  border-radius: 24rpx;
  margin-bottom: 8rpx;
}

.addr-back {
  font-size: 26rpx;
  color: #10B981;
  margin-bottom: 16rpx;
}

.form-group {
  margin-bottom: 16rpx;
}

.form-input {
  width: 100%;
  max-width: 100%;
  height: 80rpx;
  border: 1rpx solid #EAEFF5;
  border-radius: 16rpx;
  padding: 0 24rpx;
  font-size: 28rpx;
  background: #F0F4F8;
  box-sizing: border-box;
}

.form-textarea {
  width: 100%;
  max-width: 100%;
  height: 140rpx;
  border: 1rpx solid #EAEFF5;
  border-radius: 16rpx;
  padding: 16rpx 24rpx;
  font-size: 28rpx;
  background: #F0F4F8;
  box-sizing: border-box;
}

.ck-item {
  display: flex;
  justify-content: space-between;
  padding: 12rpx 0;
}

.ck-item-name {
  font-size: 26rpx;
  color: #1A1A2E;
  flex: 1;
}

.ck-item-price {
  font-size: 26rpx;
  color: #1A1A2E;
}

.ck-total {
  display: flex;
  align-items: baseline;
  justify-content: flex-end;
  padding: 20rpx 32rpx;
  font-size: 28rpx;
  color: #1A1A2E;
}

.ck-total-amount {
  font-size: 40rpx;
  color: #EF4444;
  font-weight: 700;
  margin-left: 8rpx;
}

.ck-alert {
  margin: 12rpx 32rpx 20rpx;
  padding: 16rpx 20rpx;
  background: #ECFDF5;
  border-radius: 16rpx;
  font-size: 24rpx;
  color: #059669;
}

.submit-btn {
  text-align: center;
  padding: 24rpx;
  background: linear-gradient(135deg, #10B981, #059669);
  color: #fff;
  font-size: 30rpx;
  font-weight: 600;
  border-radius: 48rpx;
  box-shadow: 0 4rpx 16rpx rgba(16, 185, 129, 0.3);

  &.disabled {
    opacity: 0.6;
  }
}

/* ===== Orders Modal ===== */
.order-card {
  margin: 16rpx 24rpx;
  padding: 24rpx;
  background: #fff;
  border-radius: 24rpx;
  box-shadow: 0 2rpx 12rpx rgba(0, 0, 0, 0.04);
}

.order-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12rpx;
}

.order-no {
  font-size: 22rpx;
  color: #8896AB;
}

.order-status {
  font-size: 22rpx;
  padding: 4rpx 14rpx;
  border-radius: 100rpx;
  font-weight: 600;
  background: #F0F4F8;
  color: #8896AB;

  &.success {
    background: rgba(34, 197, 94, 0.1);
    color: #16A34A;
  }

  &.warn {
    background: rgba(245, 158, 11, 0.1);
    color: #D97706;
  }

  &.danger {
    background: rgba(239, 68, 68, 0.1);
    color: #DC2626;
  }

  &.info {
    background: rgba(59, 130, 246, 0.1);
    color: #2563EB;
  }

  &.muted {
    background: #F0F4F8;
    color: #8896AB;
  }
}

.order-products {
  margin-bottom: 12rpx;
}

.order-prod {
  font-size: 26rpx;
  color: #1A1A2E;
  padding: 4rpx 0;
}

.order-more {
  font-size: 24rpx;
  color: #8896AB;
}

.order-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-top: 12rpx;
  border-top: 1rpx solid #EAEFF5;
}

.order-money {
  display: flex;
  align-items: center;
  gap: 12rpx;
}

.pay-status {
  font-size: 22rpx;
  padding: 2rpx 10rpx;
  border-radius: 100rpx;
  background: #F0F4F8;
  color: #8896AB;

  &.success { background: rgba(34, 197, 94, 0.1); color: #16A34A; }
  &.warn { background: rgba(245, 158, 11, 0.1); color: #D97706; }
  &.danger { background: rgba(239, 68, 68, 0.1); color: #DC2626; }
}

.order-amount {
  font-size: 30rpx;
  font-weight: 700;
  color: #EF4444;
}

.order-actions {
  display: flex;
  gap: 8rpx;
}

.action-chip {
  font-size: 22rpx;
  padding: 8rpx 20rpx;
  border-radius: 100rpx;
  font-weight: 600;

  &.success {
    background: rgba(34, 197, 94, 0.1);
    color: #16A34A;
  }

  &.warn {
    background: rgba(245, 158, 11, 0.1);
    color: #D97706;
  }

  &.danger {
    background: rgba(239, 68, 68, 0.1);
    color: #DC2626;
  }
}

.order-tracking {
  margin-top: 12rpx;
  padding-top: 8rpx;
  border-top: 1rpx solid #EAEFF5;
  font-size: 22rpx;
  color: #8896AB;
}

/* ===== Result Popup ===== */
.result-popup {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 560rpx;
  background: #fff;
  border-radius: 32rpx;
  box-shadow: 0 8rpx 40rpx rgba(0, 0, 0, 0.12);
  padding: 60rpx 40rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.result-emoji {
  font-size: 100rpx;
  margin-bottom: 24rpx;
}

.result-title {
  font-size: 36rpx;
  font-weight: 700;
  color: #1A1A2E;
  margin-bottom: 16rpx;
}

.result-desc {
  font-size: 26rpx;
  color: #8896AB;
  text-align: center;
  margin-bottom: 36rpx;
  word-break: break-all;
}

.result-close-btn {
  width: 100%;
  text-align: center;
  padding: 24rpx;
  background: linear-gradient(135deg, #10B981, #059669);
  color: #fff;
  font-size: 30rpx;
  font-weight: 600;
  border-radius: 48rpx;
  box-shadow: 0 4rpx 16rpx rgba(16, 185, 129, 0.3);
}
</style>
