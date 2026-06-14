-- =====================================================
-- 充值记录表
-- =====================================================
CREATE TABLE IF NOT EXISTS `recharge_record` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '记录ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `amount` DECIMAL(10,2) NOT NULL COMMENT '充值金额',
  `payment_method` VARCHAR(16) NOT NULL COMMENT '支付方式: wechat-微信, alipay-支付宝',
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '状态: 0-待支付, 1-充值成功',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_recharge_user_time` (`user_id`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='充值记录';
