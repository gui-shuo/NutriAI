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
              <el-tag v-if="p.isRecommended" type="danger" size="small" class="rec-tag">推荐</el-tag>
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
                <span v-if="p.originalPrice > p.salePrice" class="orig-price">¥{{ p.originalPrice }}</span>
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
              <span v-if="detailProduct.originalPrice > detailProduct.salePrice" class="orig-price">¥{{ detailProduct.originalPrice }}</span>
            </div>
            <div class="detail-stock">
              <el-tag :type="detailProduct.stock > 0 ? 'success' : 'danger'" size="small">
                {{ detailProduct.stock > 0 ? `库存 ${detailProduct.stock}` : '已售罄' }}
              </el-tag>
            </div>
            <div class="detail-tags" v-if="detailProduct.tags?.length">
              <el-tag v-for="(t, i) in detailProduct.tags" :key="i" size="small" effect="plain" type="info">{{ t }}</el-tag>
            </div>
            <div class="detail-qty">
              <span>数量：</span>
              <el-input-number v-model="detailQty" :min="1" :max="Math.min(detailProduct.stock, 10)" size="default" />
            </div>
            <el-button type="primary" :disabled="detailProduct.stock <= 0" @click="addToCartFromDetail" style="margin-top: 16px; width: 100%">
              加入购物车
            </el-button>
          </div>
        </div>
        <div v-if="detailProduct.description" class="detail-desc">
          <h3>产品介绍</h3>
          <p>{{ detailProduct.description }}</p>
        </div>
        <div v-if="detailProduct.specifications && Object.keys(detailProduct.specifications).length" class="detail-specs">
          <h3>规格参数</h3>
          <el-descriptions :column="2" border>
            <el-descriptions-item v-for="(v, k) in detailProduct.specifications" :key="k" :label="k">
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
              <el-input-number v-model="item.quantity" :min="1" :max="10" size="small" @change="updateCartTotal" />
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
      <el-form label-position="top" :model="checkoutForm">
        <el-form-item label="收货人">
          <el-input v-model="checkoutForm.receiverName" placeholder="请输入收货人姓名" maxlength="20" />
        </el-form-item>
        <el-form-item label="联系电话">
          <el-input v-model="checkoutForm.receiverPhone" placeholder="请输入手机号" maxlength="11" />
        </el-form-item>
        <el-form-item label="收货地址">
          <el-input v-model="checkoutForm.receiverAddress" type="textarea" :rows="2" placeholder="请输入详细收货地址" maxlength="200" />
        </el-form-item>
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
              <div v-for="(item, i) in (row.items || []).slice(0, 2)" :key="i" class="order-item-name">
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
              <el-tag :type="payStatusType(row.paymentStatus)" size="small">{{ payStatusText(row.paymentStatus) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="订单状态" width="90" align="center">
            <template #default="{ row }">
              <el-tag :type="orderStatusType(row.orderStatus)" size="small">{{ orderStatusText(row.orderStatus) }}</el-tag>
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
                v-if="['PAID','SHIPPED','DELIVERED'].includes(row.orderStatus)"
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
import { ref, computed, onMounted } from 'vue'
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
  ProductCategories,
  getRecommendedProducts
} from '@/services/product'
import message from '@/utils/message'

const defaultImg = 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSIyMDAiIGhlaWdodD0iMjAwIj48cmVjdCB3aWR0aD0iMjAwIiBoZWlnaHQ9IjIwMCIgZmlsbD0iI2VlZSIvPjx0ZXh0IHg9IjUwJSIgeT0iNTAlIiBkb21pbmFudC1iYXNlbGluZT0ibWlkZGxlIiB0ZXh0LWFuY2hvcj0ibWlkZGxlIiBmaWxsPSIjOTk5IiBmb250LXNpemU9IjE0Ij7ml6Dlm77niYc8L3RleHQ+PC9zdmc+'

// --- 状态 ---
const productsLoading = ref(false)
const products = ref([])
const searchKeyword = ref('')
const activeCategory = ref('')
const categories = ref(ProductCategories)

const detailVisible = ref(false)
const detailProduct = ref(null)
const detailQty = ref(1)

const cart = ref([])
const cartVisible = ref(false)
const cartTotal = ref(0)

const checkoutVisible = ref(false)
const checkoutLoading = ref(false)
const checkoutForm = ref({ receiverName: '', receiverPhone: '', receiverAddress: '', remark: '' })

const ordersDialogVisible = ref(false)
const ordersLoading = ref(false)
const orders = ref([])

// --- 初始化 ---
onMounted(() => {
  fetchProducts()
})

async function fetchProducts() {
  productsLoading.value = true
  try {
    const params = {}
    if (activeCategory.value) params.category = activeCategory.value
    const res = await getProducts(params)
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
function openCheckout() {
  if (!cart.value.length) return
  cartVisible.value = false
  checkoutForm.value = { receiverName: '', receiverPhone: '', receiverAddress: '', remark: '' }
  checkoutVisible.value = true
}

async function handleCheckout() {
  const { receiverName, receiverPhone, receiverAddress, remark } = checkoutForm.value
  if (!receiverName || !receiverPhone || !receiverAddress) {
    message.warning('请填写完整的收货信息')
    return
  }

  checkoutLoading.value = true
  try {
    const items = cart.value.map(c => ({ productId: c.productId, quantity: c.quantity }))
    // 1. 创建订单
    const res = await createProductOrder({ items, receiverName, receiverPhone, receiverAddress, remark })
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
  return {
    PENDING_PAYMENT: 'warning', PAID: 'success', SHIPPED: '', DELIVERED: 'success',
    COMPLETED: '', CANCELLED: 'info', REFUNDED: 'danger'
  }[s] || 'info'
}
function orderStatusText(s) {
  return {
    PENDING_PAYMENT: '待支付', PAID: '已支付', SHIPPED: '已发货', DELIVERED: '已送达',
    COMPLETED: '已完成', CANCELLED: '已取消', REFUNDED: '已退款'
  }[s] || s
}
</script>

<style scoped lang="scss">
.product-shop-view {
  min-height: 100vh;
  background: #f5f7fa;
}

.top-nav {
  background: #fff;
  box-shadow: 0 2px 8px rgba(0,0,0,0.06);
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
    font-weight: 600;
    color: #1f2937;
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
    :deep(.el-input-group__append) {
      background: var(--el-color-primary);
      color: #fff;
      border-color: var(--el-color-primary);
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

  @media (max-width: 1100px) { grid-template-columns: repeat(3, 1fr); }
  @media (max-width: 768px) { grid-template-columns: repeat(2, 1fr); }
  @media (max-width: 480px) { grid-template-columns: 1fr; }
}

.product-card {
  border-radius: 12px;
  cursor: pointer;
  transition: transform 0.2s;
  overflow: hidden;

  &:hover { transform: translateY(-4px); }

  :deep(.el-card__body) { padding: 0; }

  .product-img {
    position: relative;
    height: 200px;
    overflow: hidden;
    background: #f9fafb;

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
      font-weight: 600;
      margin: 0 0 4px;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }

    .product-brief {
      font-size: 13px;
      color: #6b7280;
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

      .sales { font-size: 12px; color: #9ca3af; }
    }

    .product-price {
      margin-bottom: 10px;
      .sale-price { font-size: 20px; font-weight: 700; color: #f56c6c; }
      .orig-price { font-size: 13px; color: #c0c4cc; text-decoration: line-through; margin-left: 8px; }
    }

    .product-actions {
      text-align: right;
    }
  }
}

// === 产品详情 ===
.detail-top {
  display: flex;
  gap: 24px;

  @media (max-width: 640px) { flex-direction: column; }

  .detail-img {
    width: 260px;
    min-width: 260px;
    border-radius: 8px;
    overflow: hidden;
    background: #f9fafb;

    img { width: 100%; aspect-ratio: 1; object-fit: cover; }
  }

  .detail-info {
    flex: 1;

    h2 { margin: 0 0 8px; font-size: 20px; }
    .detail-brief { color: #6b7280; font-size: 14px; margin: 8px 0 12px; line-height: 1.5; }
    .detail-rating { display: flex; align-items: center; gap: 8px; margin-bottom: 12px; color: #9ca3af; font-size: 13px; }
    .detail-price {
      margin-bottom: 12px;
      .sale-price { font-size: 28px; font-weight: 700; color: #f56c6c; }
      .orig-price { font-size: 15px; color: #c0c4cc; text-decoration: line-through; margin-left: 8px; }
    }
    .detail-stock { margin-bottom: 12px; }
    .detail-tags { display: flex; flex-wrap: wrap; gap: 4px; margin-bottom: 12px; }
    .detail-qty { display: flex; align-items: center; gap: 8px; color: #6b7280; }
  }
}

.detail-desc, .detail-specs {
  margin-top: 24px;

  h3 { font-size: 16px; margin-bottom: 12px; padding-bottom: 8px; border-bottom: 1px solid #f3f4f6; }
  p { color: #4b5563; line-height: 1.7; white-space: pre-wrap; }
}

// === 购物车 ===
.cart-list {
  .cart-item {
    display: flex;
    align-items: center;
    gap: 12px;
    padding: 12px 0;
    border-bottom: 1px solid #f3f4f6;

    .cart-img {
      width: 60px;
      height: 60px;
      border-radius: 8px;
      object-fit: cover;
      background: #f9fafb;
    }

    .cart-info {
      flex: 1;
      h4 { margin: 0 0 4px; font-size: 14px; }
      .cart-price { color: #f56c6c; font-weight: 600; display: block; margin-bottom: 4px; }
    }
  }
}

.cart-summary {
  padding: 16px 0;
  border-top: 2px solid #f3f4f6;
  margin-top: 16px;

  .cart-total {
    display: flex;
    justify-content: space-between;
    margin-bottom: 12px;
    font-size: 16px;
  }
}

.total-amount {
  font-size: 24px;
  font-weight: 700;
  color: #f56c6c;
}

// === 结算 ===
.checkout-items {
  background: #f9fafb;
  padding: 12px 16px;
  border-radius: 8px;
  margin-top: 16px;

  h4 { margin: 0 0 8px; font-size: 14px; }

  .checkout-item {
    display: flex;
    justify-content: space-between;
    padding: 4px 0;
    font-size: 13px;
    color: #4b5563;
  }
}

.checkout-total {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 8px;
  margin-top: 12px;
  font-size: 15px;
}

// === 订单 ===
.order-item-name {
  font-size: 13px;
}
</style>
