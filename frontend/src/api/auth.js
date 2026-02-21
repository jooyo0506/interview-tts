import { request } from './index'

/**
 * 发送验证码
 */
export function sendCode(phone, type = 'LOGIN') {
  return request.post('/api/auth/send-code', null, {
    params: { phone, type }
  })
}

/**
 * 注册
 */
export function register(data) {
  return request.post('/api/auth/register', data)
}

/**
 * 密码登录
 */
export function login(data) {
  return request.post('/api/auth/login', data)
}

/**
 * 验证码登录
 */
export function loginByCode(data) {
  return request.post('/api/auth/login-by-code', data)
}

/**
 * 获取当前用户信息
 */
export function getUserInfo() {
  return request.get('/api/auth/info')
}

/**
 * 获取VIP套餐列表
 */
export function getVipPlans() {
  return request.get('/api/vip/plans')
}

/**
 * 创建VIP订单
 */
export function createVipOrder(planType) {
  return request.post('/api/vip/create-order', { planType })
}

/**
 * 查询订单状态
 */
export function getOrderStatus(orderNo) {
  return request.get('/api/vip/order-status', { params: { orderNo } })
}

/**
 * Mock支付
 */
export function mockPay(orderNo) {
  return request.post('/api/vip/mock-pay', null, { params: { orderNo } })
}
