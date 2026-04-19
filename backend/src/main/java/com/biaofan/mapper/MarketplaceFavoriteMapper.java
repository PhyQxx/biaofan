package com.biaofan.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.biaofan.entity.MarketplaceFavorite;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;

/**
 * 商城收藏Mapper接口
 *
 * <p>用于访问 marketplace_favorite 表，提供用户商品收藏记录的基本CRUD操作</p>
 *
 * @author biaofan
 * @see BaseMapper
 */
@Mapper
public interface MarketplaceFavoriteMapper extends BaseMapper<MarketplaceFavorite> {
    /**
     * 分页查询用户的收藏列表
     *
     * @param page   分页参数
     * @param userId 用户ID
     * @return 分页后的收藏记录列表
     */
    IPage<MarketplaceFavorite> selectByUserId(Page<MarketplaceFavorite> page, @Param("userId") String userId);

    /**
     * 插入收藏记录（如果已存在唯一冲突则忽略）
     * 用于防止收藏重复添加的TOCTOU竞态条件
     */
    @Insert("INSERT IGNORE INTO marketplace_favorite (user_id, template_id, created_at) " +
            "VALUES (#{userId}, #{templateId}, #{createdAt})")
    int insertIgnore(MarketplaceFavorite favorite);
}
