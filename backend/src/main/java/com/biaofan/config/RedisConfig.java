package com.biaofan.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * Redis 配置类
 * <p>配置 StringRedisTemplate Bean，用于 Redis 字符串类型操作
 *
 * @author biaofan
 * @see StringRedisTemplate
 * @see RedisConnectionFactory
 */
@Configuration
public class RedisConfig {

    /**
     * 注册 StringRedisTemplate Bean
     * <p>基于 RedisConnectionFactory 创建，用于字符串的存取操作
     *
     * @param connectionFactory Redis 连接工厂，由 Spring 自动注入
     * @return StringRedisTemplate 实例
     */
    @Bean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory connectionFactory) {
        return new StringRedisTemplate(connectionFactory);
    }
}
