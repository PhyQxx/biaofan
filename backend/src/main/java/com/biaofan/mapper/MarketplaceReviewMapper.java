package com.biaofan.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.biaofan.entity.MarketplaceReview;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface MarketplaceReviewMapper extends BaseMapper<MarketplaceReview> {

    @Select("SELECT AVG(rating) FROM marketplace_review WHERE template_id = #{templateId}")
    Double selectAvgRating(@Param("templateId") String templateId);

    @Select("SELECT COUNT(*) FROM marketplace_review WHERE template_id = #{templateId}")
    Integer selectReviewCount(@Param("templateId") String templateId);
}
