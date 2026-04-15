/**
 * Common utility functions
 */

// Format price: fen to yuan, or just format number
export function formatPrice(price: number | string): string {
  const num = typeof price === 'string' ? parseFloat(price) : price
  if (isNaN(num)) return '0.00'
  return num.toFixed(2)
}

// Format date
export function formatDate(dateStr: string, format = 'YYYY-MM-DD'): string {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  const year = d.getFullYear()
  const month = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  const hours = String(d.getHours()).padStart(2, '0')
  const mins = String(d.getMinutes()).padStart(2, '0')

  return format
    .replace('YYYY', String(year))
    .replace('MM', month)
    .replace('DD', day)
    .replace('HH', hours)
    .replace('mm', mins)
}

// Relative time
export function timeAgo(dateStr: string): string {
  const now = Date.now()
  const date = new Date(dateStr).getTime()
  const diff = now - date

  const minutes = Math.floor(diff / 60000)
  const hours = Math.floor(diff / 3600000)
  const days = Math.floor(diff / 86400000)

  if (minutes < 1) return '刚刚'
  if (minutes < 60) return `${minutes}分钟前`
  if (hours < 24) return `${hours}小时前`
  if (days < 7) return `${days}天前`
  if (days < 30) return `${Math.floor(days / 7)}周前`
  return formatDate(dateStr)
}

// Throttle
export function throttle<T extends (...args: any[]) => any>(fn: T, delay = 300): T {
  let last = 0
  return function (this: any, ...args: any[]) {
    const now = Date.now()
    if (now - last >= delay) {
      last = now
      return fn.apply(this, args)
    }
  } as T
}

// Debounce
export function debounce<T extends (...args: any[]) => any>(fn: T, delay = 300): T {
  let timer: ReturnType<typeof setTimeout>
  return function (this: any, ...args: any[]) {
    clearTimeout(timer)
    timer = setTimeout(() => fn.apply(this, args), delay)
  } as T
}

// Show toast
export function showToast(title: string, icon: 'success' | 'none' | 'error' = 'none') {
  uni.showToast({ title, icon, duration: 2000 })
}

// Show loading
export function showLoading(title = '加载中...') {
  uni.showLoading({ title, mask: true })
}

export function hideLoading() {
  uni.hideLoading()
}

// Confirm dialog
export function showConfirm(content: string, title = '提示'): Promise<boolean> {
  return new Promise((resolve) => {
    uni.showModal({
      title,
      content,
      confirmColor: '#10B981',
      success: (res) => resolve(!!res.confirm)
    })
  })
}

// Order status text mapping
export const ORDER_STATUS_MAP: Record<string, { text: string; color: string }> = {
  PENDING: { text: '待付款', color: '#F59E0B' },
  PAID: { text: '已付款', color: '#10B981' },
  SHIPPED: { text: '已发货', color: '#3B82F6' },
  DELIVERED: { text: '已完成', color: '#6B7280' },
  RECEIVED: { text: '已收货', color: '#6B7280' },
  CANCELLED: { text: '已取消', color: '#94A3B8' },
  REFUNDED: { text: '已退款', color: '#EF4444' }
}

// Validate phone number
export function isValidPhone(phone: string): boolean {
  return /^1[3-9]\d{9}$/.test(phone)
}

// Validate email
export function isValidEmail(email: string): boolean {
  return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)
}

// Generate simple ID
export function genId(): string {
  return Date.now().toString(36) + Math.random().toString(36).slice(2, 8)
}
