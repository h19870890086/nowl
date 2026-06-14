package com.unimarket.module.recharge.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 充值记录VO
 */
@Data
public class RechargeRecordVO {

    private Long id;

    /** 充值金额 */
    private BigDecimal amount;

    /** 支付方式中文名 */
    private String paymentMethod;

    /** 充值时间 */
    private LocalDateTime createTime;
}
