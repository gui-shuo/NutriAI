<template>
  <div class="product-shop-view">
    <!-- 顶部导航 -->
    <nav class="top-nav">
      <div class="nav-inner">
        <div class="nav-left">
          <el-button text @click="$router.push('/')">
            <el-icon><ArrowLeft /></el-icon>
            返回首页
          </el-button>
          <h2 class="page-title">🛒 营养产品商城</h2>
        </div>
        <div class="nav-right">
          <el-badge :value="cart.length || ''" :hidden="!cart.length">
            <el-button text @click="showCartDrawer">
              <el-icon><ShoppingCart /></el-icon>
              购物车
            </el-button>
          </el-badge>
          <el-button type="primary" text @click="showMyOrders">
            <el-icon><Document /></el-icon>
            我的订单
          </el-button>
        </div>
      </div>
    </nav>

    <main class="main-area">
      <el-alert type="info" :closable="true" show-icon style="margin-bottom: 12px">
        <template #title>
          商城产品信息来源于第三方，仅供参考。营养补充剂不能替代正常饮食和药物治疗。
          <router-link to="/legal/disclaimer" style="color:#409eff">详细声明</router-link>
        </template>
      </el-alert>
      <!-- 搜索栏 -->
      <div class="search-bar">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索营养产品..."
          size="large"
          clearable
          @keydown.enter="doSearch"
          class="search-input"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
          <template #append>
            <el-button @click="doSearch">搜索</el-button>
          </template>
        </el-input>
      </div>

      <!-- 分类 Tabs -->
      <div class="category-tabs">
        <el-radio-group v-model="activeCategory" size="large" @change="onCategoryChange">
          <el-radio-button value="">全部</el-radio-button>
          <el-radio-button v-for="c in categories" :key="c.value" :value="c.value">
            {{ c.label }}
          </el-radio-button>
        </el-radio-group>
      </div>

      <!-- 产品网格 -->
      <el-skeleton :loading="productsLoading" animated :rows="8">
        <el-empty v-if="!products.length" description="暂无相关产品" />
        <div v-else class="product-grid">
          <el-card
            v-for="p in products"
            :key="p.id"
            class="product-card"
            shadow="hover"
            @click="openProductDetail(p)"
          >
            <div class="product-img">
              <img :src="p.imageUrl || defaultImg" :alt="p.name" />
              <el-tag v-if="p.isRecommended" type="danger" size="small" class="rec-tag"
                >推荐</el-tag
              >
            </div>
            <div class="product-body">
              <h4 class="product-name">{{ p.name }}</h4>
              <p class="product-brief">{{ p.brief }}</p>
              <div class="product-meta">
                <el-rate :model-value="p.rating" disabled size="small" />
                <span class="sales">已售 {{ p.salesCount }}</span>
              </div>
              <div class="product-price">
                <span class="sale-price">¥{{ p.salePrice }}</span>
                <span v-if="p.originalPrice > p.salePrice" class="orig-price"
                  >¥{{ p.originalPrice }}</span
                >
              </div>
              <div class="product-actions" @click.stop>
                <el-button type="primary" size="small" @click.stop="addToCart(p)">
                  加入购物车
                </el-button>
              </div>
            </div>
          </el-card>
        </div>
      </el-skeleton>
    </main>

    <!-- 产品详情 Dialog -->
    <el-dialog v-model="detailVisible" width="680px" class="detail-dialog">
      <template v-if="detailProduct">
        <div class="detail-top">
          <div class="detail-img">
            <img :src="detailProduct.imageUrl || defaultImg" :alt="detailProduct.name" />
          </div>
          <div class="detail-info">
            <h2>{{ detailProduct.name }}</h2>
            <el-tag type="info" size="small">{{ detailProduct.brand }}</el-tag>
            <p class="detail-brief">{{ detailProduct.brief }}</p>
            <div class="detail-rating">
              <el-rate :model-value="detailProduct.rating" disabled />
              <span>{{ detailProduct.reviewCount }} 评价</span>
            </div>
            <div class="detail-price">
              <span class="sale-price">¥{{ detailProduct.salePrice }}</span>
              <span v-if="detailProduct.originalPrice > detailProduct.salePrice" class="orig-price"
                >¥{{ detailProduct.originalPrice }}</span
              >
            </div>
            <div class="detail-stock">
              <el-tag :type="detailProduct.stock > 0 ? 'success' : 'danger'" size="small">
                {{ detailProduct.stock > 0 ? `库存 ${detailProduct.stock}` : '已售罄' }}
              </el-tag>
            </div>
            <div class="detail-tags" v-if="detailProduct.tags?.length">
              <el-tag
                v-for="(t, i) in detailProduct.tags"
                :key="i"
                size="small"
                effect="plain"
                type="info"
                >{{ t }}</el-tag
              >
            </div>
            <div class="detail-qty">
              <span>数量：</span>
              <el-input-number
                v-model="detailQty"
                :min="1"
                :max="Math.min(detailProduct.stock, 10)"
                size="default"
              />
            </div>
            <el-button
              type="primary"
              :disabled="detailProduct.stock <= 0"
              @click="addToCartFromDetail"
              style="margin-top: 16px; width: 100%"
            >
              加入购物车
            </el-button>
          </div>
        </div>
        <div v-if="detailProduct.description" class="detail-desc">
          <h3>产品介绍</h3>
          <p>{{ detailProduct.description }}</p>
        </div>
        <div
          v-if="detailProduct.specifications && Object.keys(detailProduct.specifications).length"
          class="detail-specs"
        >
          <h3>规格参数</h3>
          <el-descriptions :column="2" border>
            <el-descriptions-item
              v-for="(v, k) in detailProduct.specifications"
              :key="k"
              :label="k"
            >
              {{ v }}
            </el-descriptions-item>
          </el-descriptions>
        </div>
      </template>
    </el-dialog>

    <!-- 购物车 Drawer -->
    <el-drawer v-model="cartVisible" title="🛒 购物车" size="440px">
      <el-empty v-if="!cart.length" description="购物车是空的" />
      <template v-else>
        <div class="cart-list">
          <div v-for="(item, idx) in cart" :key="idx" class="cart-item">
            <img :src="item.imageUrl || defaultImg" class="cart-img" />
            <div class="cart-info">
              <h4>{{ item.name }}</h4>
              <span class="cart-price">¥{{ item.salePrice }}</span>
              <el-input-number
                v-model="item.quantity"
                :min="1"
                :max="10"
                size="small"
                @change="updateCartTotal"
              />
            </div>
            <el-button type="danger" text size="small" @click="removeFromCart(idx)">
              <el-icon><Delete /></el-icon>
            </el-button>
          </div>
        </div>
        <div class="cart-summary">
          <div class="cart-total">
            <span>合计：</span>
            <span class="total-amount">¥{{ cartTotal.toFixed(2) }}</span>
          </div>
          <el-button type="primary" size="large" @click="openCheckout" style="width: 100%">
            去结算（{{ cart.length }}件）
          </el-button>
        </div>
      </template>
    </el-drawer>

    <!-- 结算 Dialog -->
    <el-dialog v-model="checkoutVisible" title="确认订单" width="560px">
      <!-- 已保存的地址选择 -->
      <div v-if="savedAddresses.length" class="saved-addresses">
        <div class="addr-header">
          <span class="addr-title">📍 我的地址</span>
          <el-button text type="primary" size="small" @click="$router.push('/profile')">管理地址</el-button>
        </div>
        <el-radio-group v-model="selectedAddressId" class="addr-radio-group" @change="onAddressSelect">
          <el-radio
            v-for="addr in savedAddresses"
            :key="addr.id"
            :value="addr.id"
            class="addr-radio"
          >
            <span class="addr-name">{{ addr.receiverName }}</span>
            <span class="addr-phone">{{ addr.receiverPhone }}</span>
            <el-tag v-if="addr.isDefault" type="success" size="small">默认</el-tag>
            <br />
            <span class="addr-detail">{{ addr.province }}{{ addr.city }}{{ addr.district }}{{ addr.detailAddress }}</span>
          </el-radio>
          <el-radio :value="0" class="addr-radio">
            <span class="addr-name">✏️ 手动填写新地址</span>
          </el-radio>
        </el-radio-group>
      </div>

      <!-- 手动填写地址 (无保存地址 或 选择了手动填写) -->
      <el-form
        v-if="!savedAddresses.length || selectedAddressId === 0"
        label-position="top"
        :model="checkoutForm"
        style="margin-top: 12px"
      >
        <el-form-item label="收货人">
          <el-input
            v-model="checkoutForm.receiverName"
            placeholder="请输入收货人姓名"
            maxlength="20"
          />
        </el-form-item>
        <el-form-item label="联系电话">
          <el-input
            v-model="checkoutForm.receiverPhone"
            placeholder="请输入手机号"
            maxlength="11"
          />
        </el-form-item>
        <el-form-item label="收货地址">
          <el-input
            v-model="checkoutForm.receiverAddress"
            type="textarea"
            :rows="2"
            placeholder="请输入详细收货地址"
            maxlength="200"
          />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="checkoutForm.remark" placeholder="选填" maxlength="200" />
        </el-form-item>
      </el-form>
      <!-- 仅备注（选择了已保存地址时显示） -->
      <el-form v-else label-position="top" style="margin-top: 12px">
        <el-form-item label="备注">
          <el-input v-model="checkoutForm.remark" placeholder="选填" maxlength="200" />
        </el-form-item>
      </el-form>
      <div class="checkout-items">
        <h4>商品清单</h4>
        <div v-for="(item, idx) in cart" :key="idx" class="checkout-item">
          <span>{{ item.name }} × {{ item.quantity }}</span>
          <span>¥{{ (item.salePrice * item.quantity).toFixed(2) }}</span>
        </div>
      </div>
      <div class="checkout-total">
        <span>应付总额：</span>
        <span class="total-amount">¥{{ cartTotal.toFixed(2) }}</span>
      </div>
      <el-alert type="info" :closable="false" show-icon style="margin-top: 8px">
        <template #title>模拟支付模式：点击提交后将自动完成支付</template>
      </el-alert>
      <template #footer>
        <el-button @click="checkoutVisible = false">取消</el-button>
        <el-button type="primary" :loading="checkoutLoading" @click="handleCheckout">
          提交订单并支付（模拟）
        </el-button>
      </template>
    </el-dialog>

    <!-- 订单列表 Dialog -->
    <el-dialog v-model="ordersDialogVisible" title="我的订单" width="860px">
      <el-skeleton :loading="ordersLoading" animated :rows="4">
        <el-empty v-if="!orders.length" description="暂无订单" />
        <el-table v-else :data="orders" style="width: 100%">
          <el-table-column label="订单号" prop="orderNo" min-width="150" />
          <el-table-column label="商品" min-width="150">
            <template #default="{ row }">
              <div
                v-for="(item, i) in (row.items || []).slice(0, 2)"
                :key="i"
                class="order-item-name"
              >
                {{ item.productName }} × {{ item.quantity }}
              </div>
              <span v-if="(row.items || []).length > 2">等{{ row.items.length }}件商品</span>
            </template>
          </el-table-column>
          <el-table-column label="金额" width="90" align="center">
            <template #default="{ row }">¥{{ row.totalAmount }}</template>
          </el-table-column>
          <el-table-column label="支付状态" width="90" align="center">
            <template #default="{ row }">
              <el-tag :type="payStatusType(row.paymentStatus)" size="small">{{
                payStatusText(row.paymentStatus)
              }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="订单状态" width="90" align="center">
            <template #default="{ row }">
              <el-tag :type="orderStatusType(row.orderStatus)" size="small">{{
                orderStatusText(row.orderStatus)
              }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="快递单号" width="120">
            <template #default="{ row }">{{ row.trackingNo || '-' }}</template>
          </el-table-column>
          <el-table-column label="操作" width="160" align="center">
            <template #default="{ row }">
              <el-button
                v-if="row.orderStatus === 'PAID'"
                type="warning"
                size="small"
                text
                @click="handleShip(row)"
              >
                模拟发货
              </el-button>
              <el-button
                v-if="row.orderStatus === 'SHIPPED'"
                type="success"
                size="small"
                text
                @click="handleReceive(row)"
              >
                确认收货
              </el-button>
              <el-button
                v-if="['PAID', 'SHIPPED', 'DELIVERED'].includes(row.orderStatus)"
                type="danger"
                size="small"
                text
                @click="handleRefund(row)"
              >
                退款
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-skeleton>
      <template #footer>
        <el-button @click="ordersDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue'
import { ArrowLeft, Document, ShoppingCart, Search, Delete } from '@element-plus/icons-vue'
import {
  getProducts,
  searchProducts,
  getProductDetail,
  createProductOrder,
  simulatePayProduct,
  simulateShipProduct,
  confirmReceiveProduct,
  simulateRefundProduct,
  getProductOrderHistory,
  ProductCategories
} from '@/services/product'
import { getAddresses } from '@/services/address'
import message from '@/utils/message'

const defaultImg =
  'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSIyMDAiIGhlaWdodD0iMjAwIj48cmVjdCB3aWR0aD0iMjAwIiBoZWlnaHQ9IjIwMCIgZmlsbD0iI2VlZSIvPjx0ZXh0IHg9IjUwJSIgeT0iNTAlIiBkb21pbmFudC1iYXNlbGluZT0ibWlkZGxlIiB0ZXh0LWFuY2hvcj0ibWlkZGxlIiBmaWxsPSIjOTk5IiBmb250LXNpemU9IjE0Ij7ml6Dlm77niYc8L3RleHQ+PC9zdmc+'

// --- 状态 ---
const productsLoading = ref(false)
const products = ref([])
const searchKeyword = ref('')
const activeCategory = ref('')
const categories = ref([])

const initCategories = () => {
  categories.value = Object.entries(ProductCategories).map(([, v]) => ({
    value: v.code,
    label: `${v.icon} ${v.name}`
  }))
}

const detailVisible = ref(false)
const detailProduct = ref(null)
const detailQty = ref(1)

const CART_STORAGE_KEY = 'nutriai_cart'

function loadCart() {
  try {
    const raw = localStorage.getItem(CART_STORAGE_KEY)
    return raw ? JSON.parse(raw) : []
  } catch { return [] }
}

const cart = ref(loadCart())
const cartVisible = ref(false)
const cartTotal = ref(0)

watch(cart, val => {
  try { localStorage.setItem(CART_STORAGE_KEY, JSON.stringify(val)) } catch {}
}, { deep: true })

const checkoutVisible = ref(false)
const checkoutLoading = ref(false)
const checkoutForm = ref({ receiverName: '', receiverPhone: '', receiverAddress: '', remark: '' })
const savedAddresses = ref([])
const selectedAddressId = ref(0)

const ordersDialogVisible = ref(false)
const ordersLoading = ref(false)
const orders = ref([])

// --- 初始化 ---
onMounted(() => {
  initCategories()
  fetchProducts()
  updateCartTotal()
})

async function fetchProducts() {
  productsLoading.value = true
  try {
    const res = await getProducts(activeCategory.value || undefined)
    if (res.data.code === 200) {
      products.value = res.data.data?.content || res.data.data || []
    }
  } catch (e) {
    console.error('获取产品列表失败', e)
  } finally {
    productsLoading.value = false
  }
}

async function doSearch() {
  if (!searchKeyword.value.trim()) {
    fetchProducts()
    return
  }
  productsLoading.value = true
  try {
    const res = await searchProducts(searchKeyword.value.trim())
    if (res.data.code === 200) {
      products.value = res.data.data?.content || res.data.data || []
    }
  } catch (e) {
    console.error('搜索失败', e)
  } finally {
    productsLoading.value = false
  }
}

function onCategoryChange() {
  searchKeyword.value = ''
  fetchProducts()
}

async function openProductDetail(p) {
  detailQty.value = 1
  try {
    const res = await getProductDetail(p.id)
    if (res.data.code === 200) {
      detailProduct.value = res.data.data
      detailVisible.value = true
    }
  } catch (e) {
    // fallback to list data
    detailProduct.value = p
    detailVisible.value = true
  }
}

// --- 购物车 ---
function addToCart(p) {
  const existing = cart.value.find(c => c.productId === p.id)
  if (existing) {
    if (existing.quantity < 10) existing.quantity++
  } else {
    cart.value.push({
      productId: p.id,
      name: p.name,
      imageUrl: p.imageUrl,
      salePrice: p.salePrice,
      quantity: 1
    })
  }
  updateCartTotal()
  message.success(`已添加 ${p.name}`)
}

function addToCartFromDetail() {
  if (!detailProduct.value) return
  const p = detailProduct.value
  const existing = cart.value.find(c => c.productId === p.id)
  if (existing) {
    existing.quantity = Math.min(existing.quantity + detailQty.value, 10)
  } else {
    cart.value.push({
      productId: p.id,
      name: p.name,
      imageUrl: p.imageUrl,
      salePrice: p.salePrice,
      quantity: detailQty.value
    })
  }
  updateCartTotal()
  detailVisible.value = false
  message.success(`已添加 ${p.name} × ${detailQty.value}`)
}

function removeFromCart(idx) {
  cart.value.splice(idx, 1)
  updateCartTotal()
}

function updateCartTotal() {
  cartTotal.value = cart.value.reduce((sum, item) => sum + item.salePrice * item.quantity, 0)
}

function showCartDrawer() {
  updateCartTotal()
  cartVisible.value = true
}

// --- 结算 ---
async function openCheckout() {
  if (!cart.value.length) return
  cartVisible.value = false
  checkoutForm.value = { receiverName: '', receiverPhone: '', receiverAddress: '', remark: '' }
  selectedAddressId.value = 0
  checkoutVisible.value = true
  // 加载已保存地址
  try {
    const res = await getAddresses()
    if (res.data.code === 200) {
      savedAddresses.value = res.data.data || []
      // 自动选中默认地址
      const def = savedAddresses.value.find(a => a.isDefault)
      if (def) selectedAddressId.value = def.id
    }
  } catch {
    savedAddresses.value = []
  }
}

function onAddressSelect(id) {
  if (id === 0) {
    checkoutForm.value = { ...checkoutForm.value, receiverName: '', receiverPhone: '', receiverAddress: '' }
  }
}

async function handleCheckout() {
  let receiverName, receiverPhone, receiverAddress
  const { remark } = checkoutForm.value

  if (selectedAddressId.value && selectedAddressId.value !== 0) {
    const addr = savedAddresses.value.find(a => a.id === selectedAddressId.value)
    if (!addr) {
      message.warning('请选择收货地址')
      return
    }
    receiverName = addr.receiverName
    receiverPhone = addr.receiverPhone
    receiverAddress = `${addr.province || ''}${addr.city || ''}${addr.district || ''}${addr.detailAddress || ''}`
  } else {
    receiverName = checkoutForm.value.receiverName
    receiverPhone = checkoutForm.value.receiverPhone
    receiverAddress = checkoutForm.value.receiverAddress
  }

  if (!receiverName || !receiverPhone || !receiverAddress) {
    message.warning('请填写完整的收货信息')
    return
  }

  checkoutLoading.value = true
  try {
    const items = cart.value.map(c => ({ productId: c.productId, quantity: c.quantity }))
    // 1. 创建订单
    const res = await createProductOrder({
      items,
      receiverName,
      receiverPhone,
      receiverAddress,
      remark
    })
    if (res.data.code !== 200) {
      message.error(res.data.message || '创建订单失败')
      return
    }
    const order = res.data.data

    // 2. 模拟支付
    const payRes = await simulatePayProduct(order.orderNo)
    if (payRes.data.code === 200) {
      message.success('🎉 下单成功！支付已完成')
      cart.value = []
      cartTotal.value = 0
      checkoutVisible.value = false
    } else {
      message.error(payRes.data.message || '支付失败')
    }
  } catch (e) {
    console.error('结算失败', e)
    message.error('操作失败，请重试')
  } finally {
    checkoutLoading.value = false
  }
}

// --- 订单管理 ---
async function showMyOrders() {
  ordersDialogVisible.value = true
  ordersLoading.value = true
  try {
    const res = await getProductOrderHistory(0, 20)
    if (res.data.code === 200) {
      orders.value = res.data.data?.content || []
    }
  } catch (e) {
    message.error('获取订单失败')
  } finally {
    ordersLoading.value = false
  }
}

async function handleShip(row) {
  try {
    const res = await simulateShipProduct(row.orderNo)
    if (res.data.code === 200) {
      message.success('已模拟发货')
      showMyOrders()
    } else {
      message.error(res.data.message || '发货失败')
    }
  } catch (e) {
    message.error('操作失败')
  }
}

async function handleReceive(row) {
  try {
    const res = await confirmReceiveProduct(row.orderNo)
    if (res.data.code === 200) {
      message.success('已确认收货')
      showMyOrders()
    } else {
      message.error(res.data.message || '收货确认失败')
    }
  } catch (e) {
    message.error('操作失败')
  }
}

async function handleRefund(row) {
  try {
    const res = await simulateRefundProduct(row.orderNo)
    if (res.data.code === 200) {
      message.success('退款成功')
      showMyOrders()
    } else {
      message.error(res.data.message || '退款失败')
    }
  } catch (e) {
    message.error('退款操作失败')
  }
}

// --- 工具函数 ---
function payStatusType(s) {
  return { PAID: 'success', PENDING: 'warning', REFUNDED: 'danger', EXPIRED: 'info' }[s] || 'info'
}
function payStatusText(s) {
  return { PAID: '已支付', PENDING: '待支付', REFUNDED: '已退款', EXPIRED: '已超时' }[s] || s
}
function orderStatusType(s) {
  return (
    {
      PENDING_PAYMENT: 'warning',
      PAID: 'success',
      SHIPPED: '',
      DELIVERED: 'success',
      COMPLETED: '',
      CANCELLED: 'info',
      REFUNDED: 'danger'
    }[s] || 'info'
  )
}
function orderStatusText(s) {
  return (
    {
      PENDING_PAYMENT: '待支付',
      PAID: '已支付',
      SHIPPED: '已发货',
      DELIVERED: '已送达',
      COMPLETED: '已完成',
      CANCELLED: '已取消',
      REFUNDED: '已退款'
    }[s] || s
  )
}
</script>

<style scoped lang="scss">
.product-shop-view {
  min-height: 100vh;
  background: #FAFAFA;
  font-family: 'Inter', sans-serif;
}

.top-nav {
  background: #FFFFFF;
  border-bottom: 1px solid #E2E8F0;
  box-shadow: 0 1px 3px 0 rgba(0, 0, 0, 0.05);
  position: sticky;
  top: 0;
  z-index: 100;

  .nav-inner {
    max-width: 1280px;
    margin: 0 auto;
    padding: 0 20px;
    display: flex;
    justify-content: space-between;
    align-items: center;
    height: 56px;
  }

  .nav-left {
    display: flex;
    align-items: center;
    gap: 12px;
  }

  .nav-right {
    display: flex;
    align-items: center;
    gap: 8px;
  }

  .page-title {
    font-size: 18px;
    font-family: 'Calistoga', 'Inter', sans-serif;
    font-weight: 600;
    color: #0F172A;
    margin: 0;
  }
}

.main-area {
  max-width: 1280px;
  margin: 0 auto;
  padding: 24px 20px;
}

// === 搜索栏 ===
.search-bar {
  max-width: 600px;
  margin: 0 auto 24px;

  .search-input {
    :deep(.el-input__wrapper) {
      border-radius: 12px;
      border: 1px solid #E2E8F0;
      box-shadow: 0 1px 2px 0 rgba(0, 0, 0, 0.05);

      &:focus-within {
        border-color: #10B981;
        box-shadow: 0 0 0 2px rgba(16, 185, 129, 0.2);
      }
    }
    :deep(.el-input-group__append) {
      background: linear-gradient(135deg, #10B981, #34D399);
      color: #fff;
      border: none;
      border-radius: 0 12px 12px 0;
      font-family: 'Inter', sans-serif;
      font-weight: 500;
    }
  }
}

// === 分类 ===
.category-tabs {
  text-align: center;
  margin-bottom: 24px;
  overflow-x: auto;
}

// === 产品网格 ===
.product-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;

  @media (max-width: 1100px) {
    grid-template-columns: repeat(3, 1fr);
  }
  @media (max-width: 768px) {
    grid-template-columns: repeat(2, 1fr);
  }
  @media (max-width: 480px) {
    grid-template-columns: 1fr;
  }
}

.product-card {
  border-radius: 16px;
  border: 1px solid #E2E8F0;
  box-shadow: 0 1px 3px 0 rgba(0, 0, 0, 0.1), 0 1px 2px -1px rgba(0, 0, 0, 0.1);
  cursor: pointer;
  transition: box-shadow 0.2s, transform 0.2s;
  overflow: hidden;
  background: #FFFFFF;
  position: relative;

  &:hover {
    box-shadow: 0 10px 15px -3px rgba(0, 0, 0, 0.1), 0 4px 6px -4px rgba(0, 0, 0, 0.1);
    transform: translateY(-2px);
  }

  :deep(.el-card__body) {
    padding: 0;
  }

  .product-img {
    position: relative;
    height: 200px;
    overflow: hidden;
    background: #F1F5F9;
    border-bottom: 1px solid #E2E8F0;

    img {
      width: 100%;
      height: 100%;
      object-fit: cover;
    }

    .rec-tag {
      position: absolute;
      top: 8px;
      right: 8px;
    }
  }

  .product-body {
    padding: 14px;

    .product-name {
      font-size: 15px;
      font-family: 'Calistoga', 'Inter', sans-serif;
      font-weight: 600;
      color: #0F172A;
      margin: 0 0 4px;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }

    .product-brief {
      font-size: 13px;
      color: #0F172A;
      opacity: 0.6;
      margin: 0 0 8px;
      display: -webkit-box;
      -webkit-line-clamp: 2;
      -webkit-box-orient: vertical;
      overflow: hidden;
      line-height: 1.4;
    }

    .product-meta {
      display: flex;
      align-items: center;
      gap: 8px;
      margin-bottom: 8px;

      .sales {
        font-size: 12px;
        color: #0F172A;
        opacity: 0.45;
      }
    }

    .product-price {
      margin-bottom: 10px;
      display: inline-block;
      padding: 2px 0;

      .sale-price {
        font-size: 20px;
        font-weight: 700;
        font-family: 'Calistoga', 'Inter', sans-serif;
        color: #10B981;
      }
      .orig-price {
        font-size: 13px;
        color: #0F172A;
        opacity: 0.4;
        text-decoration: line-through;
        margin-left: 8px;
      }
    }

    .product-actions {
      text-align: right;

      :deep(.el-button) {
        border-radius: 10px;
        border: none;
        background: linear-gradient(135deg, #10B981, #34D399);
        box-shadow: 0 2px 4px -1px rgba(16, 185, 129, 0.25);
        font-family: 'Inter', sans-serif;
        font-weight: 500;
        transition: box-shadow 0.2s, transform 0.2s;

        &:hover {
          transform: translateY(-1px);
          box-shadow: 0 4px 6px -1px rgba(16, 185, 129, 0.3);
        }
      }
    }
  }
}

// === 产品详情 ===
.detail-top {
  display: flex;
  gap: 24px;

  @media (max-width: 640px) {
    flex-direction: column;
  }

  .detail-img {
    width: 260px;
    min-width: 260px;
    border-radius: 12px;
    border: 1px solid #E2E8F0;
    overflow: hidden;
    background: #F1F5F9;

    img {
      width: 100%;
      aspect-ratio: 1;
      object-fit: cover;
    }
  }

  .detail-info {
    flex: 1;

    h2 {
      margin: 0 0 8px;
      font-size: 20px;
      font-family: 'Calistoga', 'Inter', sans-serif;
      color: #0F172A;
    }
    .detail-brief {
      color: #0F172A;
      opacity: 0.6;
      font-size: 14px;
      margin: 8px 0 12px;
      line-height: 1.5;
    }
    .detail-rating {
      display: flex;
      align-items: center;
      gap: 8px;
      margin-bottom: 12px;
      color: #0F172A;
      opacity: 0.5;
      font-size: 13px;
    }
    .detail-price {
      margin-bottom: 12px;
      display: inline-block;
      padding: 4px 0;

      .sale-price {
        font-size: 28px;
        font-weight: 700;
        font-family: 'Calistoga', 'Inter', sans-serif;
        color: #10B981;
      }
      .orig-price {
        font-size: 15px;
        color: #0F172A;
        opacity: 0.4;
        text-decoration: line-through;
        margin-left: 8px;
      }
    }
    .detail-stock {
      margin-bottom: 12px;
    }
    .detail-tags {
      display: flex;
      flex-wrap: wrap;
      gap: 4px;
      margin-bottom: 12px;
    }
    .detail-qty {
      display: flex;
      align-items: center;
      gap: 8px;
      color: #0F172A;
      opacity: 0.7;
    }
  }
}

.detail-desc,
.detail-specs {
  margin-top: 24px;

  h3 {
    font-size: 16px;
    font-family: 'Calistoga', 'Inter', sans-serif;
    color: #0F172A;
    margin-bottom: 12px;
    padding-bottom: 8px;
    border-bottom: 1px solid #E2E8F0;
  }
  p {
    color: #0F172A;
    line-height: 1.7;
    white-space: pre-wrap;
  }
}

// === 购物车 ===
.cart-list {
  .cart-item {
    display: flex;
    align-items: center;
    gap: 12px;
    padding: 12px 0;
    border-bottom: 1px solid #E2E8F0;

    .cart-img {
      width: 60px;
      height: 60px;
      border-radius: 10px;
      border: 1px solid #E2E8F0;
      object-fit: cover;
      background: #F1F5F9;
    }

    .cart-info {
      flex: 1;
      h4 {
        margin: 0 0 4px;
        font-size: 14px;
        font-family: 'Inter', sans-serif;
        color: #0F172A;
        font-weight: 600;
      }
      .cart-price {
        color: #10B981;
        font-weight: 600;
        font-family: 'Calistoga', 'Inter', sans-serif;
        display: block;
        margin-bottom: 4px;
      }
    }
  }
}

.cart-summary {
  padding: 16px 0;
  border-top: 1px solid #E2E8F0;
  margin-top: 16px;

  .cart-total {
    display: flex;
    justify-content: space-between;
    margin-bottom: 12px;
    font-size: 16px;
    color: #0F172A;
  }
}

.total-amount {
  font-size: 24px;
  font-weight: 700;
  font-family: 'Calistoga', 'Inter', sans-serif;
  color: #10B981;
}

// === 结算 ===
.checkout-items {
  background: #F1F5F9;
  padding: 12px 16px;
  border-radius: 12px;
  border: 1px solid #E2E8F0;
  box-shadow: 0 1px 2px 0 rgba(0, 0, 0, 0.05);
  margin-top: 16px;

  h4 {
    margin: 0 0 8px;
    font-size: 14px;
    font-family: 'Calistoga', 'Inter', sans-serif;
    color: #0F172A;
  }

  .checkout-item {
    display: flex;
    justify-content: space-between;
    padding: 4px 0;
    font-size: 13px;
    color: #0F172A;
  }
}

.checkout-total {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 8px;
  margin-top: 12px;
  font-size: 15px;
  color: #0F172A;
}

// === 已保存地址 ===
.saved-addresses {
  .addr-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 8px;

    .addr-title {
      font-weight: 600;
      font-family: 'Calistoga', 'Inter', sans-serif;
      font-size: 15px;
      color: #0F172A;
    }
  }

  .addr-radio-group {
    display: flex;
    flex-direction: column;
    gap: 8px;
    width: 100%;

    .addr-radio {
      display: flex;
      align-items: flex-start;
      padding: 10px 12px;
      border: 1px solid #E2E8F0;
      border-radius: 12px;
      margin: 0;
      height: auto;
      width: 100%;
      transition: all 0.2s;

      &:hover {
        border-color: #34D399;
        box-shadow: 0 2px 4px -1px rgba(16, 185, 129, 0.1);
      }

      .addr-name {
        font-weight: 600;
        color: #0F172A;
        margin-right: 8px;
      }

      .addr-phone {
        color: #0F172A;
        opacity: 0.6;
        font-size: 13px;
        margin-right: 6px;
      }

      .addr-detail {
        color: #0F172A;
        opacity: 0.5;
        font-size: 13px;
      }
    }
  }
}

// === 订单 ===
.order-item-name {
  font-size: 13px;
  color: #0F172A;
}
</style>
