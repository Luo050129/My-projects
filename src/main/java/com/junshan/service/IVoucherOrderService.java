package com.junshan.service;

import com.junshan.dto.Result;
import com.junshan.entity.VoucherOrder;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author 小君
 * @since 2025-11
 */
public interface IVoucherOrderService extends IService<VoucherOrder> {

    Result seckillVoucher(Long voucherId);

    void createVoucherOrder(VoucherOrder voucherOrder);

    void handleVoucherOrder(VoucherOrder voucherOrder);
}
