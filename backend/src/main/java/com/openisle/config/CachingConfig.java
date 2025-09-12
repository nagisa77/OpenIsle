package com.openisle.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.hibernate6.Hibernate6Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Redis 缓存配置类
 * @author smallclover
 * @since 2025-09-04
 */
@Configuration
@EnableCaching
public class CachingConfig {

    // 标签缓存名
    public static final String TAG_CACHE_NAME="openisle_tags";
    // 分类缓存名
    public static final String CATEGORY_CACHE_NAME="openisle_categories";
    // 在线人数缓存名
    public static final String ONLINE_CACHE_NAME="openisle_online";
    // 注册验证码
    public static final String VERIFY_CACHE_NAME="openisle_verify";
    // 发帖频率限制
    public static final String LIMIT_CACHE_NAME="openisle_limit";
    // 用户访问统计
    public static final String VISIT_CACHE_NAME="openisle_visit";
    // 文章缓存
    public static final String POST_CACHE_NAME="openisle_posts";

    /**
     * 自定义Redis的序列化器
     * @return
     */
    @Bean()
    @Primary
    public RedisSerializer<Object> redisSerializer() {
        // 注册 JavaTimeModule 來支持 Java 8 的日期和时间 API,否则回报一下错误，同时还要引入jsr310

        // org.springframework.data.redis.serializer.SerializationException: Could not write JSON: Java 8 date/time type `java.time.LocalDateTime` not supported by default:
        // add Module "com.fasterxml.jackson.datatype:jackson-datatype-jsr310" to enable handling
        // (through reference chain: java.util.ArrayList[0]->com.openisle.dto.TagDto["createdAt"])
        // 设置可见性，允许序列化所有元素
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        // Hibernate6Module 可以自动处理懒加载代理对象。
        // Tag对象的creator是FetchType.LAZY
        objectMapper.registerModule(new Hibernate6Module()
                .disable(Hibernate6Module.Feature.USE_TRANSIENT_ANNOTATION)
                // 将 Hibernate 特有的集合类型转换为标准 Java 集合类型
                // 避免序列化时出现 org.hibernate.collection.spi.PersistentSet 这样的类型信息
                .configure(Hibernate6Module.Feature.REPLACE_PERSISTENT_COLLECTIONS, true));
        // service的时候带上类型信息
        // 启用类型信息，避免 LinkedHashMap 问题
        objectMapper.activateDefaultTyping(
                LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY
        );
        objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        return new GenericJackson2JsonRedisSerializer(objectMapper);
    }

    /**
     * 配置 Spring Cache 使用 RedisCacheManager
     */
    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory, RedisSerializer<Object> redisSerializer) {

        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ZERO) // 默认缓存不过期
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(redisSerializer))
                .disableCachingNullValues(); // 禁止缓存 null 值

        // 个别缓存单独设置 TTL 时间
        Map<String, RedisCacheConfiguration> cacheConfigs = new HashMap<>();
        RedisCacheConfiguration oneHourConfig = config.entryTtl(Duration.ofHours(1));
        cacheConfigs.put(TAG_CACHE_NAME, oneHourConfig);
        cacheConfigs.put(CATEGORY_CACHE_NAME, oneHourConfig);

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(config)
                .withInitialCacheConfigurations(cacheConfigs)
                .build();
    }

    /**
     * 配置 RedisTemplate，支持直接操作 Redis
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory,  RedisSerializer<Object> redisSerializer) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // key 和 hashKey 使用 String 序列化
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());

        // value 和 hashValue 使用 JSON 序列化
        template.setValueSerializer(redisSerializer);
        template.setHashValueSerializer(redisSerializer);

        return template;
    }
}
