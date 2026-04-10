package com.biaofan.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.biaofan.entity.GamificationStoreProduct;
import org.apache.ibatis.annotations.Mapper;

/**
 * 积分商城商品Mapper接口
 *
 * <p>用于访问 gamification_store_product 表，提供积分商城商品的基本CRUD操作</p>
 *
 * @author biaofan
 * @see BaseMapper
 */
@Mapper
public interface GamificationStoreProductMapper extends BaseMapper<GamificationStoreProduct> {
}
