package com.personaowl.oa.common.redis;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.cache.CacheAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.util.Map;

@AutoConfiguration(after = RedisAutoConfiguration.class, before = CacheAutoConfiguration.class)
@EnableCaching
@ConditionalOnClass({RedisConnectionFactory.class, CacheManager.class})
@ConditionalOnProperty(prefix = "oa.cache.redis", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(OaRedisCacheProperties.class)
public class OaRedisCacheAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(CacheManager.class)
    RedisCacheManager redisCacheManager(RedisConnectionFactory connectionFactory,
                                        OaRedisCacheProperties properties) {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        BasicPolymorphicTypeValidator typeValidator = BasicPolymorphicTypeValidator.builder()
                .allowIfSubType("com.personaowl.oa.")
                .allowIfSubType("java.lang.")
                .allowIfSubType("java.time.")
                .allowIfSubType("java.util.")
                .build();
        objectMapper.activateDefaultTyping(
                typeValidator,
                ObjectMapper.DefaultTyping.EVERYTHING,
                JsonTypeInfo.As.PROPERTY);
        GenericJackson2JsonRedisSerializer valueSerializer =
                new GenericJackson2JsonRedisSerializer(objectMapper);

        RedisCacheConfiguration defaults = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(properties.getDefaultTtl())
                .disableCachingNullValues()
                .computePrefixWith(name -> "oa:cache:" + name + ":")
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(valueSerializer));

        Map<String, RedisCacheConfiguration> configurations = Map.of(
                CacheNames.USER_ROLES, defaults.entryTtl(properties.getAuthorizationTtl()),
                CacheNames.USER_PERMISSIONS, defaults.entryTtl(properties.getAuthorizationTtl()),
                CacheNames.DEPARTMENT_LIST, defaults.entryTtl(properties.getOrganizationTtl()),
                CacheNames.DEPARTMENT_DETAIL, defaults.entryTtl(properties.getOrganizationTtl()),
                CacheNames.RBAC_ROLES, defaults.entryTtl(properties.getRbacTtl()),
                CacheNames.RBAC_PERMISSIONS, defaults.entryTtl(properties.getRbacTtl()),
                CacheNames.NOTICE_UNREAD_COUNT, defaults.entryTtl(properties.getNoticeTtl()));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaults)
                .withInitialCacheConfigurations(configurations)
                .transactionAware()
                .build();
    }

    @Bean
    CachingConfigurer oaCachingConfigurer() {
        return new CachingConfigurer() {
            @Override
            public CacheErrorHandler errorHandler() {
                return new LoggingCacheErrorHandler();
            }
        };
    }

    private static final class LoggingCacheErrorHandler implements CacheErrorHandler {
        private static final Logger log = LoggerFactory.getLogger(LoggingCacheErrorHandler.class);

        @Override
        public void handleCacheGetError(RuntimeException exception, Cache cache, Object key) {
            warn(exception, cache, key, "read");
        }

        @Override
        public void handleCachePutError(RuntimeException exception, Cache cache, Object key, Object value) {
            warn(exception, cache, key, "write");
        }

        @Override
        public void handleCacheEvictError(RuntimeException exception, Cache cache, Object key) {
            warn(exception, cache, key, "evict");
        }

        @Override
        public void handleCacheClearError(RuntimeException exception, Cache cache) {
            warn(exception, cache, "*", "clear");
        }

        private void warn(RuntimeException exception, Cache cache, Object key, String operation) {
            log.warn("Redis cache {} failed, cache={}, key={}; falling back to database",
                    operation, cache.getName(), key, exception);
        }
    }
}
