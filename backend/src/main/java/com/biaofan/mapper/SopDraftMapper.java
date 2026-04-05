package com.biaofan.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.biaofan.entity.SopDraft;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface SopDraftMapper extends BaseMapper<SopDraft> {

    @Select("SELECT * FROM sop_draft WHERE executor_id = #{executorId} AND sop_id = #{sopId} LIMIT 1")
    SopDraft findByExecutorAndSop(@Param("executorId") Long executorId, @Param("sopId") Long sopId);
}
