package com.junshan.service;

import com.junshan.dto.Result;
import com.junshan.entity.ShopType;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author 小君
 * @since 2025-11
 */
public interface IShopTypeService extends IService<ShopType> {

    Result querySort();
}
