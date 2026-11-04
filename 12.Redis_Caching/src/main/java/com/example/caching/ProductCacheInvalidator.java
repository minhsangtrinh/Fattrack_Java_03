package com.example.caching;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class ProductCacheInvalidator {
    private static final Logger LOG = LoggerFactory.getLogger(ProductCacheInvalidator.class);
    private final StringRedisTemplate redis;

    public ProductCacheInvalidator(StringRedisTemplate redis) {
        this.redis = redis;
    }

    // Called through a separate Spring bean after the DB commit.
    // Evict L2 before publishing so notified peers cannot reload this old L2 value.
    @CacheEvict(cacheNames = CacheConfig.PRODUCTS, cacheManager = "redisCacheManager",
            key = "#id", beforeInvocation = true)
    public void evictAndPublish(long id) {
        Long subscribers = redis.convertAndSend(CacheConfig.DELETION_CHANNEL, Long.toString(id));
        LOG.info("L2 evicted; published product {} deletion to {} subscribers", id, subscribers);
    }
}
