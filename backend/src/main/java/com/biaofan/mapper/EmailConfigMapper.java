package com.biaofan.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.biaofan.entity.EmailConfig;
import org.apache.ibatis.annotations.Mapper;

/**
 * 邮件服务配置 Mapper
 */
@Mapper
public interface EmailConfigMapper extends BaseMapper<EmailConfig> {
}
