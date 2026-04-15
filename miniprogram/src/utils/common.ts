// 检查登录状态，未登录跳转登录页
export function checkLogin(): boolean {
  const token = uni.getStorageSync('accessToken')
  if (!token) {
    uni.navigateTo({
      url: '/pages/auth/login',
      fail: () => {
        uni.reLaunch({ url: '/pages/auth/login' })
      }
    })
    return false
  }
  return true
}

// 格式化时间
export function formatTime(dateStr: string): string {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  const now = new Date()
  const diff = now.getTime() - date.getTime()
  
  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return `${Math.floor(diff / 60000)}分钟前`
  if (diff < 86400000) return `${Math.floor(diff / 3600000)}小时前`
  if (diff < 604800000) return `${Math.floor(diff / 86400000)}天前`
  
  const y = date.getFullYear()
  const m = String(date.getMonth() + 1).padStart(2, '0')
  const d = String(date.getDate()).padStart(2, '0')
  return y === now.getFullYear() ? `${m}-${d}` : `${y}-${m}-${d}`
}

// 格式化日期为 YYYY-MM-DD
export function formatDate(date: Date): string {
  const y = date.getFullYear()
  const m = String(date.getMonth() + 1).padStart(2, '0')
  const d = String(date.getDate()).padStart(2, '0')
  return `${y}-${m}-${d}`
}

// 截断文本
export function truncate(text: string, maxLen: number): string {
  if (!text || text.length <= maxLen) return text
  return text.substring(0, maxLen) + '...'
}

// 默认头像
export function defaultAvatar(avatar?: string): string {
  if (avatar && avatar.trim().length > 0) return avatar
  return '/static/images/default-avatar.png'
}

// rpx 转 px
export function rpx2px(rpx: number): number {
  const { windowWidth } = uni.getWindowInfo()
  return (rpx / 750) * windowWidth
}
