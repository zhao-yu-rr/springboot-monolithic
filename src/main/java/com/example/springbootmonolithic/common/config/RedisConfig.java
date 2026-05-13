package com.example.springbootmonolithic.common.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.Data;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.codec.JsonJacksonCodec;
import org.redisson.config.Config;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Redisson配置类 - 替代原有RedisTemplate配置
 * 使用Redisson作为Redis连接客户端，提供分布式锁、分布式集合等高级功能
 */
@Configuration
@EnableConfigurationProperties(RedisConfig.RedisProperties.class)
public class RedisConfig {

    /**
     * Redisson连接配置属性
     */
    @Data
    @ConfigurationProperties(prefix = "spring.data.redis")
    public static class RedisProperties {
        private String host;
        private int port;
        private String password;
        private int database;
    }

    /**
     * 创建RedissonClient Bean
     * 若spring boot自动配置已创建则不重复创建
     */
    @Bean(destroyMethod = "shutdown")
    @ConditionalOnMissingBean(RedissonClient.class)
    public RedissonClient redissonClient(RedisProperties redisProperties) {
        Config config = new Config();

        // 配置支持Java 8日期时间的Jackson编解码器
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        config.setCodec(new JsonJacksonCodec(objectMapper));

        String address = "redis://" + redisProperties.getHost() + ":" + redisProperties.getPort();

        var serverConfig = config.useSingleServer()
                .setAddress(address)
                .setDatabase(redisProperties.getDatabase())
                .setConnectionPoolSize(20)
                .setConnectionMinimumIdleSize(5)
                .setIdleConnectionTimeout(10000)
                .setConnectTimeout(3000)
                .setTimeout(3000);

        if (redisProperties.getPassword() != null && !redisProperties.getPassword().isEmpty()) {
            serverConfig.setPassword(redisProperties.getPassword());
        }

        return Redisson.create(config);
    }
}
