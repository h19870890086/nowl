import { request } from '../request'
import { RECHARGE_API } from '@/config/apiPaths'

/**
 * 充值相关API
 */

// 创建充值（模拟支付）
export const createRecharge = (data: { amount: number; paymentMethod: string }) => {
  return request.post<{ amount: number; paymentMethod: string; createTime: string }>(RECHARGE_API.CREATE, data)
}

// 查询充值记录
export const getRechargeHistory = () => {
  return request.get<Array<{ id: number; amount: number; paymentMethod: string; createTime: string }>>(RECHARGE_API.LIST)
}
