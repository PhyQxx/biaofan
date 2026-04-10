package com.biaofan.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.biaofan.entity.Sop;
import org.apache.ibatis.annotations.Mapper;

/**
 * SOP标准操作规程Mapper接口
 * 
 * 提供SOP主数据的数据库访问层操作，继承MyBatis-Plus的BaseMapper。
 * 可使用BaseMapper提供的标准CRUD方法，以及自定义的查询方法。
 */
@Mapper
public interface SopMapper extends BaseMapper<Sop> {
    /**
     * 根据用户ID分页查询SOP列表
     * 
     * @param page 分页对象
     * @param userId 用户ID
     * @return 分页后的SOP列表
     */
    IPage<Sop> selectByUserId(Page<Sop> page, Long userId);
}
