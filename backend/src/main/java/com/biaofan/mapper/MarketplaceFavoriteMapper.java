package com.biaofan.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.biaofan.entity.MarketplaceFavorite;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface MarketplaceFavoriteMapper extends BaseMapper<MarketplaceFavorite> {
    IPage<MarketplaceFavorite> selectByUserId(Page<MarketplaceFavorite> page, @Param("userId") String userId);
}
