import { ElMessage } from 'element-plus'

/**
 * 统一的消息提示工具
 * CSS全局样式已在 main.scss 中将 .el-message 定位到视口正中间
 * offset 设为 0，完全由 CSS 控制定位
 */

const defaultOptions = {
  offset: 0,
  duration: 3000,
  showClose: true,
  grouping: true,
}

export const message = {
  success: (msg, options = {}) => {
    return ElMessage.success({ message: msg, ...defaultOptions, ...options })
  },
  error: (msg, options = {}) => {
    return ElMessage.error({ message: msg, ...defaultOptions, duration: 4000, ...options })
  },
  warning: (msg, options = {}) => {
    return ElMessage.warning({ message: msg, ...defaultOptions, ...options })
  },
  info: (msg, options = {}) => {
    return ElMessage.info({ message: msg, ...defaultOptions, ...options })
  }
}

export default message
