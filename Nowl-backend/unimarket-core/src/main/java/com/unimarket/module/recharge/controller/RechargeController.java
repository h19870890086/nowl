package com.unimarket.module.recharge.controller;

import com.unimarket.common.result.Result;
import com.unimarket.module.recharge.dto.RechargeCreateDTO;
import com.unimarket.module.recharge.service.RechargeService;
import com.unimarket.module.recharge.vo.RechargeRecordVO;
import com.unimarket.security.UserContextHolder;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 充值接口
 */
@RestController
@RequestMapping("/recharge")
@RequiredArgsConstructor
public class RechargeController {

    private final RechargeService rechargeService;

    /**
     * 创建充值（模拟支付，直接到账）
     */
    @PostMapping("/create")
    public Result<RechargeRecordVO> recharge(@Valid @RequestBody RechargeCreateDTO dto) {
        Long userId = UserContextHolder.getUserId();
        RechargeRecordVO vo = rechargeService.recharge(userId, dto);
        return Result.success(vo);
    }

    /**
     * 查询充值记录
     */
    @GetMapping("/list")
    public Result<List<RechargeRecordVO>> getRechargeHistory() {
        Long userId = UserContextHolder.getUserId();
        List<RechargeRecordVO> records = rechargeService.getRechargeHistory(userId);
        return Result.success(records);
    }
}
