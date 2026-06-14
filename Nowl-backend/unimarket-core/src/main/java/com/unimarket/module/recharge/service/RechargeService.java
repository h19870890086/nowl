package com.unimarket.module.recharge.service;

import com.unimarket.module.recharge.dto.RechargeCreateDTO;
import com.unimarket.module.recharge.vo.RechargeRecordVO;

import java.util.List;

/**
 * 充值服务接口
 */
public interface RechargeService {

    /**
     * 创建充值并直接到账（模拟充值，无需真实支付）
     * @param userId 用户ID
     * @param dto 充值请求
     * @return 充值后的余额
     */
    RechargeRecordVO recharge(Long userId, RechargeCreateDTO dto);

    /**
     * 查询用户充值记录
     * @param userId 用户ID
     * @return 充值记录列表
     */
    List<RechargeRecordVO> getRechargeHistory(Long userId);
}
