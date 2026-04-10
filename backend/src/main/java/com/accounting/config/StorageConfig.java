package com.accounting.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * 存储配置类
 * 根据环境自动选择 Redis 或内存存储
 */
@Slf4j
@Configuration
public class StorageConfig {

    @Value("${redis.host:localhost}")
    private String redisHost;

    @Value("${redis.port:6379}")
    private int redisPort;

    @Value("${redis.password:}")
    private String redisPassword;

    @Value("${redis.database:0}")
    private int redisDatabase;

    /**
     * 尝试创建 Redis 连接，失败则返回 null
     */
    @Bean
    public StringRedisTemplate redisTemplate() {
        try {
            RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
            config.setHostName(redisHost);
            config.setPort(redisPort);
            config.setDatabase(redisDatabase);
            if (redisPassword != null && !redisPassword.isEmpty()) {
                config.setPassword(redisPassword);
            }

            LettuceConnectionFactory connectionFactory = new LettuceConnectionFactory(config);
            connectionFactory.afterPropertiesSet();

            // 测试连接
            connectionFactory.getConnection().ping();

            StringRedisTemplate template = new StringRedisTemplate(connectionFactory);
            log.info("✅ Redis connected successfully at {}:{}/db{}", redisHost, redisPort, redisDatabase);
            return template;
        } catch (Exception e) {
            log.warn("⚠️ Redis not available, will use in-memory storage. Error: {}", e.getMessage());
            return null;
        }
    }
}
