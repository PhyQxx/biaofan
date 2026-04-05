package com.biaofan.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.biaofan.entity.PushToken;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface PushTokenMapper extends BaseMapper<PushToken> {

    @Select("SELECT * FROM push_token WHERE user_id = #{userId} LIMIT 1")
    PushToken findByUserId(@Param("userId") Long userId);
}
