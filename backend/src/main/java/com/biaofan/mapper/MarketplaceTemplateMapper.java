package com.biaofan.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.biaofan.entity.MarketplaceTemplate;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 商城模板Mapper接口
 *
 * <p>用于访问 marketplace_template 表，提供商城模板的基本CRUD操作</p>
 *
 * @author biaofan
 * @see BaseMapper
 */
@Mapper
public interface MarketplaceTemplateMapper extends BaseMapper<MarketplaceTemplate> {

    /**
     * 分页查询已批准的模板列表
     *
     * <p>支持按分类筛选、关键词搜索（标题或描述），支持按热门、评分、最新排序</p>
     *
     * @param page     分页参数
     * @param category 分类名称（可选，为空或null时不筛选）
     * @param keyword  搜索关键词（可选，为空或null时不搜索）
     * @param sort     排序方式：popular=热门，rated=评分高，recent=最新
     * @return 分页后的已批准模板列表
     */
    @Select("SELECT * FROM marketplace_template WHERE status = 'approved' " +
            "AND (#{category} IS NULL OR #{category} = '' OR category = #{category}) " +
            "AND (#{keyword} IS NULL OR #{keyword} = '' OR title LIKE CONCAT('%', #{keyword}, '%') ESCAPE '\\' OR description LIKE CONCAT('%', #{keyword}, '%') ESCAPE '\\') " +
            "ORDER BY " +
            "  CASE WHEN #{sort} = 'popular' THEN use_count END DESC, " +
            "  CASE WHEN #{sort} = 'rated' THEN avg_rating END DESC, " +
            "  CASE WHEN #{sort} = 'recent' OR #{sort} IS NULL OR #{sort} = '' THEN created_at END DESC")
    IPage<MarketplaceTemplate> selectApprovedList(
            Page<MarketplaceTemplate> page,
            @Param("category") String category,
            @Param("keyword") String keyword,
            @Param("sort") String sort);

    /**
     * 分页查询指定状态的模板列表
     *
     * @param page   分页参数
     * @param status 模板状态
     * @return 分页后的模板列表，按创建时间倒序排列
     */
    @Select("SELECT * FROM marketplace_template WHERE status = #{status} ORDER BY created_at DESC")
    IPage<MarketplaceTemplate> selectByStatus(Page<MarketplaceTemplate> page, @Param("status") String status);
}
