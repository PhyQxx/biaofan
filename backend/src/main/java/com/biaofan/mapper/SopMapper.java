package com.biaofan.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.biaofan.entity.Sop;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SopMapper extends BaseMapper<Sop> {
    IPage<Sop> selectByUserId(Page<Sop> page, Long userId);
}
