package com.biaofan.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.biaofan.entity.MarketplaceReview;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 商城评价Mapper接口
 *
 * <p>用于访问 marketplace_review 表，提供用户对商城商品评价的基本CRUD操作</p>
 *
 * @author biaofan
 * @see BaseMapper
 */
@Mapper
public interface MarketplaceReviewMapper extends BaseMapper<MarketplaceReview> {

    /**
     * 查询指定商品的平均评分
     *
     * @param templateId 模板ID
     * @return 商品的平均评分，无评价时返回null
     */
    @Select("SELECT AVG(rating) FROM marketplace_review WHERE template_id = #{templateId}")
    Double selectAvgRating(@Param("templateId") String templateId);

    /**
     * 查询指定商品的评价数量
     *
     * @param templateId 模板ID
     * @return 商品的评价总数
     */
    @Select("SELECT COUNT(*) FROM marketplace_review WHERE template_id = #{templateId}")
    Integer selectReviewCount(@Param("templateId") String templateId);
}
