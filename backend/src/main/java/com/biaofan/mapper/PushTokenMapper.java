package com.biaofan.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.biaofan.entity.PushToken;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 推送Token Mapper接口
 *
 * <p>用于访问 push_token 表，提供用户推送设备Token的基本CRUD操作</p>
 *
 * @author biaofan
 * @see BaseMapper
 */
@Mapper
public interface PushTokenMapper extends BaseMapper<PushToken> {

    /**
     * 根据用户ID查询推送Token
     *
     * @param userId 用户ID
     * @return 用户的推送Token记录，未找到返回null
     */
    @Select("SELECT * FROM push_token WHERE user_id = #{userId} LIMIT 1")
    PushToken findByUserId(@Param("userId") Long userId);
}
