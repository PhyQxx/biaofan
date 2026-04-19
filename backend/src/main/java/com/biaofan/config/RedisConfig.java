package com.biaofan.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Redis 配置类
 * <p>配置 StringRedisTemplate Bean，用于 Redis 字符串类型操作
 * <p>配置 RedisCacheManager，支持 @Cacheable 注解实现热点数据缓存
 *
 * @author biaofan
 * @see StringRedisTemplate
 * @see RedisConnectionFactory
 * @see RedisCacheManager
 */
@Configuration
@EnableCaching
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

    /**
     * 注册 RedisCacheManager Bean
     * <p>配置各缓存的名称和过期时间：
     * - aiConfig: AI配置缓存，默认1小时
     * - marketplaceTemplates: 模板市场缓存，默认5分钟
     * - gamificationProfile: 用户积分化档案缓存，默认5分钟
     *
     * @param factory Redis 连接工厂
     * @return RedisCacheManager 实例
     */
    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory factory) {
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(10))
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(
                        new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(
                        new GenericJackson2JsonRedisSerializer()));

        Map<String, RedisCacheConfiguration> cacheConfigs = new HashMap<>();
        cacheConfigs.put("aiConfig", defaultConfig.entryTtl(Duration.ofHours(1)));
        cacheConfigs.put("marketplaceTemplates", defaultConfig.entryTtl(Duration.ofMinutes(5)));
        cacheConfigs.put("gamificationProfile", defaultConfig.entryTtl(Duration.ofMinutes(5)));

        return RedisCacheManager.builder(factory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigs)
                .build();
    }
}
