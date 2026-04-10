package com.biaofan.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.biaofan.entity.SopDraft;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * SOP草稿Mapper接口
 * 
 * 提供SOP草稿数据的数据库访问层操作，继承MyBatis-Plus的BaseMapper。
 * SOP草稿用于保存执行人未完成的SOP执行进度草稿。
 */
@Mapper
public interface SopDraftMapper extends BaseMapper<SopDraft> {

    /**
     * 根据执行人和SOP查找草稿
     * 
     * @param executorId 执行人ID
     * @param sopId SOP ID
     * @return 符合条件的第一条草稿记录
     */
    @Select("SELECT * FROM sop_draft WHERE executor_id = #{executorId} AND sop_id = #{sopId} LIMIT 1")
    SopDraft findByExecutorAndSop(@Param("executorId") Long executorId, @Param("sopId") Long sopId);
}
