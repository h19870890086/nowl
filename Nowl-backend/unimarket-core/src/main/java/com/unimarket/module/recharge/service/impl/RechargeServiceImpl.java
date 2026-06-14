package com.unimarket.module.recharge.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.unimarket.common.enums.NoticeType;
import com.unimarket.common.exception.BusinessException;
import com.unimarket.module.notice.service.NoticeService;
import com.unimarket.module.recharge.dto.RechargeCreateDTO;
import com.unimarket.module.recharge.entity.RechargeRecord;
import com.unimarket.module.recharge.mapper.RechargeRecordMapper;
import com.unimarket.module.recharge.service.RechargeService;
import com.unimarket.module.recharge.vo.RechargeRecordVO;
import com.unimarket.module.user.entity.UserInfo;
import com.unimarket.module.user.mapper.UserInfoMapper;
import com.unimarket.module.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 充值服务实现（模拟充值，无需真实支付）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RechargeServiceImpl implements RechargeService {

    private final RechargeRecordMapper rechargeRecordMapper;
    private final UserInfoMapper userInfoMapper;
    private final UserService userService;
    private final NoticeService noticeService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RechargeRecordVO recharge(Long userId, RechargeCreateDTO dto) {
        // 校验支付方式
        String method = dto.getPaymentMethod().toLowerCase();
        if (!"wechat".equals(method) && !"alipay".equals(method)) {
            throw new BusinessException("支付方式仅支持 wechat-微信 或 alipay-支付宝");
        }

        // 查询用户
        UserInfo user = userInfoMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        // 创建充值记录（直接标记为成功，模拟支付）
        RechargeRecord record = new RechargeRecord();
        record.setUserId(userId);
        record.setAmount(dto.getAmount());
        record.setPaymentMethod(method);
        record.setStatus(1); // 直接成功
        rechargeRecordMapper.insert(record);

        // 原子更新余额（避免并发丢失更新），然后刷新缓存确保前端立即获取最新余额
        userInfoMapper.addMoney(userId, dto.getAmount());
        userService.refreshUserCache(userId);

        // 重新查询用户获取最新余额用于通知
        UserInfo updatedUser = userInfoMapper.selectById(userId);
        java.math.BigDecimal newBalance = updatedUser != null ? updatedUser.getMoney() : user.getMoney().add(dto.getAmount());

        // 发送充值成功通知
        noticeService.sendNotice(
                userId,
                "充值成功",
                "您的账户已成功充值 ￥" + dto.getAmount() + "，当前余额 ￥" + newBalance + "。",
                NoticeType.SYSTEM.getCode(),
                record.getId()
        );

        log.info("充值成功: userId={}, amount={}, method={}, balance={}",
                userId, dto.getAmount(), method, newBalance);

        // 返回结果
        RechargeRecordVO vo = BeanUtil.copyProperties(record, RechargeRecordVO.class);
        vo.setPaymentMethod(getPaymentMethodName(method));
        return vo;
    }

    @Override
    public List<RechargeRecordVO> getRechargeHistory(Long userId) {
        LambdaQueryWrapper<RechargeRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RechargeRecord::getUserId, userId)
               .eq(RechargeRecord::getStatus, 1)
               .orderByDesc(RechargeRecord::getCreateTime);

        List<RechargeRecord> records = rechargeRecordMapper.selectList(wrapper);
        return records.stream().map(r -> {
            RechargeRecordVO vo = BeanUtil.copyProperties(r, RechargeRecordVO.class);
            vo.setPaymentMethod(getPaymentMethodName(r.getPaymentMethod()));
            return vo;
        }).collect(Collectors.toList());
    }

    private String getPaymentMethodName(String method) {
        if ("wechat".equalsIgnoreCase(method)) return "微信支付";
        if ("alipay".equalsIgnoreCase(method)) return "支付宝";
        return method;
    }
}
