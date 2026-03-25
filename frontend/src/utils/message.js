import { ElMessage } from 'element-plus'

/**
 * 统一的消息提示配置
 * 确保消息显示在页面顶部合适位置，带有优雅的样式
 */

const defaultOptions = {
  offset: 0, // offset设为0，由CSS的top控制位置
  duration: 3000, // 显示3秒
  showClose: true, // 显示关闭按钮
  grouping: true, // 相同消息合并
  customClass: 'custom-message' // 自定义样式类
}

export const message = {
  /**
   * 成功消息
   */
  success: (msg, options = {}) => {
    return ElMessage.success({
      message: msg,
      ...defaultOptions,
      customClass: 'custom-message custom-message-success',
      ...options
    })
  },

  /**
   * 错误消息
   */
  error: (msg, options = {}) => {
    return ElMessage.error({
      message: msg,
      ...defaultOptions,
      duration: 4000, // 错误消息显示4秒
      customClass: 'custom-message custom-message-error',
      ...options
    })
  },

  /**
   * 警告消息
   */
  warning: (msg, options = {}) => {
    return ElMessage.warning({
      message: msg,
      ...defaultOptions,
      customClass: 'custom-message custom-message-warning',
      ...options
    })
  },

  /**
   * 信息消息
   */
  info: (msg, options = {}) => {
    return ElMessage.info({
      message: msg,
      ...defaultOptions,
      customClass: 'custom-message custom-message-info',
      ...options
    })
  }
}

export default message
