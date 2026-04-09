package com.biaofan.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.biaofan.entity.MarketplaceTemplate;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface MarketplaceTemplateMapper extends BaseMapper<MarketplaceTemplate> {

    @Select("SELECT * FROM marketplace_template WHERE status = 'approved' " +
            "AND (#{category} IS NULL OR #{category} = '' OR category = #{category}) " +
            "AND (#{keyword} IS NULL OR #{keyword} = '' OR title LIKE CONCAT('%', #{keyword}, '%') OR description LIKE CONCAT('%', #{keyword}, '%')) " +
            "ORDER BY " +
            "  CASE WHEN #{sort} = 'popular' THEN use_count END DESC, " +
            "  CASE WHEN #{sort} = 'rated' THEN avg_rating END DESC, " +
            "  CASE WHEN #{sort} = 'recent' OR #{sort} IS NULL OR #{sort} = '' THEN created_at END DESC")
    IPage<MarketplaceTemplate> selectApprovedList(
            Page<MarketplaceTemplate> page,
            @Param("category") String category,
            @Param("keyword") String keyword,
            @Param("sort") String sort);

    @Select("SELECT * FROM marketplace_template WHERE status = #{status} ORDER BY created_at DESC")
    IPage<MarketplaceTemplate> selectByStatus(Page<MarketplaceTemplate> page, @Param("status") String status);
}
