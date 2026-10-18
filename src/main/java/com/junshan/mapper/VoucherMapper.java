package com.junshan.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.junshan.entity.Voucher;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * <p>
 *  Mapper 接口
 * </p>
 *
 * @author 小君
 * @since 2025-12
 */
public interface VoucherMapper extends BaseMapper<Voucher> {

    List<Voucher> queryVoucherOfShop(@Param("shopId") Long shopId);
}
