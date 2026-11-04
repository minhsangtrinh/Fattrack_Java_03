package com.example.caching;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Duration;
import java.util.Set;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

@Configuration
@EnableCaching
public class CacheConfig {
    public static final String PRODUCTS = "products";
    public static final String DELETION_CHANNEL = "product-deletion";

    @Bean
    public Cache<Long, Product> productL1Cache() {
        return Caffeine.newBuilder()
                .maximumSize(1000)
                .expireAfterWrite(Duration.ofSeconds(30))
                .build();
    }

    @Bean
    public RedisCacheManager redisCacheManager(RedisConnectionFactory factory,
            ObjectMapper mapper) {
        var serializer = new Jackson2JsonRedisSerializer<>(mapper, Product.class);
        var configuration = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofSeconds(120))
                .disableCachingNullValues()
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(serializer));
        return RedisCacheManager.builder(factory)
                .cacheDefaults(configuration)
                .initialCacheNames(Set.of(PRODUCTS))
                .build();
    }

    @Bean
    public RedisMessageListenerContainer productDeletionListenerContainer(
            RedisConnectionFactory factory, ProductCacheEvents listener) {
        var container = new RedisMessageListenerContainer();
        container.setConnectionFactory(factory);
        container.addMessageListener(listener, new ChannelTopic(DELETION_CHANNEL));
        return container;
    }
}
