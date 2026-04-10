package com.biaofan.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.biaofan.entity.GamificationUserProduct;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户商品Mapper接口
 *
 * <p>用于访问 gamification_user_product 表，提供用户已兑换商品记录的基本CRUD操作</p>
 *
 * @author biaofan
 * @see BaseMapper
 */
@Mapper
public interface GamificationUserProductMapper extends BaseMapper<GamificationUserProduct> {
}
