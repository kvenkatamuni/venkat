package com.paanini.jiffy.config.cache;

import ai.jiffy.secure.client.service.SentryServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.paanini.jiffy.vfs.api.Persistable;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.cache.support.NoOpCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.jedis.JedisClientConfiguration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import javax.annotation.PostConstruct;
import java.util.concurrent.TimeUnit;

import static com.paanini.jiffy.constants.CacheType.CAFFEINE_CACHE;
import static com.paanini.jiffy.constants.CacheType.REDIS_CACHE;

@Configuration
public class CacheConfig {

    private static Logger logger = LoggerFactory.getLogger(CacheConfig.class);

    @Value("${disable.cache:false}")
    boolean disableCache;

    @Value("${cache.type:redis}")
    String cacheType;

    @Value("${redis.password}")
    String redisPasswordKey;

    @Value("${mongo.ssl.enabled}")
    boolean sslEnabled;

    @Value("${redis.host}")
    String redisHost;

    @Value("${redis.port}")
    int redisPort;

    @Value("${redis.sslEnabled}")
    boolean redisSSlEnabled;

    @Value("${app.sentry.url}")
    String sentryUrl;

    //Cache expire duration in hours
    @Value("${cache.expire.duration:8}")
    int cacheExpireDuration;

    @Value("${cache.max.size:100}")
    int maxCacheSize;

    private String redisPassword;

    @PostConstruct
    public void initCacheConfig() {
        redisPassword = SentryServiceImpl.getSecret(sentryUrl, redisPasswordKey);
    }

    @Bean
    public CacheManager cacheManager(ObjectMapper objectMapper) {
        objectMapper.registerModule(new Jdk8Module());
        CacheManager cacheManager= new NoOpCacheManager();
        if(!disableCache) {
            if (cacheType.equalsIgnoreCase(REDIS_CACHE)) {
                logger.info("Using redis cache");
                cacheManager = RedisCacheManager.builder(redisJedisConnectionFactory())
                        .cacheDefaults(redisConfig(objectMapper))
                        .build();
            } else if (cacheType.equalsIgnoreCase(CAFFEINE_CACHE)) {
                logger.info("Using caffeine cache");
                final CaffeineCacheManager caffeineCacheManager = new CaffeineCacheManager();
                caffeineCacheManager.setCaffeine(caffeineConfig());
                cacheManager = caffeineCacheManager;
            }
        }
        return cacheManager;
    }

    private RedisCacheConfiguration redisConfig(ObjectMapper objectMapper) {
        Jackson2JsonRedisSerializer<? extends Persistable> serializer = new Jackson2JsonRedisSerializer<>(Persistable.class);
        serializer.setObjectMapper(objectMapper);
        return RedisCacheConfiguration.defaultCacheConfig()
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(serializer))
                .disableCachingNullValues();
    }

    private JedisConnectionFactory redisJedisConnectionFactory() {
        RedisStandaloneConfiguration configuration = new RedisStandaloneConfiguration(redisHost, redisPort);
        RedisPassword redisPasswordTemp = RedisPassword.of(redisPassword);
        configuration.setPassword(redisPasswordTemp);
        JedisClientConfiguration jedisClientConfiguration;
        if(redisSSlEnabled) {
            jedisClientConfiguration = JedisClientConfiguration.builder().useSsl().build();
        } else {
            jedisClientConfiguration = JedisClientConfiguration.builder().build();
        }
        JedisConnectionFactory factory = new JedisConnectionFactory(configuration,jedisClientConfiguration);
        GenericObjectPoolConfig poolConfig = factory.getPoolConfig();
        if (poolConfig != null) {
            poolConfig.setTestOnBorrow(true);
            poolConfig.setTestOnReturn(true);
            poolConfig.setTestWhileIdle(true);
        }
        factory.afterPropertiesSet();
        return factory;
    }

    private Caffeine caffeineConfig() {
        return Caffeine.newBuilder().
                maximumSize(maxCacheSize).
                expireAfterWrite(cacheExpireDuration, TimeUnit.HOURS);
    }
}
