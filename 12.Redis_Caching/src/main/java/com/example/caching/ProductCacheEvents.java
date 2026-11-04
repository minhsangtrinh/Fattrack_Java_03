package com.example.caching;

import com.github.benmanes.caffeine.cache.Cache;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

@Component
public class ProductCacheEvents implements MessageListener {
    private static final Logger LOG = LoggerFactory.getLogger(ProductCacheEvents.class);
    private final Cache<Long, Product> l1;

    public ProductCacheEvents(Cache<Long, Product> l1) {
        this.l1 = l1;
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            long id = Long.parseLong(new String(message.getBody(), StandardCharsets.UTF_8));
            if (id <= 0) {
                LOG.warn("Ignored invalid product deletion notification");
                return;
            }
            l1.invalidate(id);
            LOG.info("Pub/Sub received: L1 evicted product {}", id);
        } catch (NumberFormatException exception) {
            LOG.warn("Ignored malformed product deletion notification");
        }
    }
}
