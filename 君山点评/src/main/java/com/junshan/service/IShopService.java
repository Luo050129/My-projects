package com.junshan.service;

import com.junshan.dto.Result;
import com.junshan.entity.Shop;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author 小君
 * @since 2025-11
 */
public interface IShopService extends IService<Shop> {
    Result queryById(Long id) throws InterruptedException;

    Result update(Shop shop);

    Result queryShopByType(Integer typeId, Integer current, Double x, Double y);
}
