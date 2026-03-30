import api from './api'

/**
 * 第三方社交登录 API
 */
export const socialAuthApi = {
  // 获取微信授权URL
  getWechatAuthUrl(state = 'login') {
    return api.get('/auth/social/wechat/auth-url', { params: { state } })
  },

  // 获取QQ授权URL
  getQqAuthUrl(state = 'login') {
    return api.get('/auth/social/qq/auth-url', { params: { state } })
  },

  // 微信登录
  wechatLogin(code) {
    return api.post('/auth/social/wechat/login', { code, provider: 'wechat' })
  },

  // QQ登录
  qqLogin(code) {
    return api.post('/auth/social/qq/login', { code, provider: 'qq' })
  },

  // 获取绑定状态
  getBindInfo() {
    return api.get('/auth/social/bindinfo')
  },

  // 绑定微信
  bindWechat(code) {
    return api.post('/auth/social/bind/wechat', { code })
  },

  // 绑定QQ
  bindQq(code) {
    return api.post('/auth/social/bind/qq', { code })
  },

  // 解绑微信
  unbindWechat() {
    return api.delete('/auth/social/unbind/wechat')
  },

  // 解绑QQ
  unbindQq() {
    return api.delete('/auth/social/unbind/qq')
  }
}
