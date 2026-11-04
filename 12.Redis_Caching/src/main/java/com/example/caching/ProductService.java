package com.example.caching;

import com.github.benmanes.caffeine.cache.Cache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class ProductService {
    private static final Logger LOG = LoggerFactory.getLogger(ProductService.class);
    private final Cache<Long, Product> l1;
    private final ProductLookupService lookup;
    private final ProductRepository repository;
    private final TransactionTemplate transactions;
    private final ProductCacheInvalidator invalidator;

    public ProductService(Cache<Long, Product> l1, ProductLookupService lookup,
            ProductRepository repository, TransactionTemplate transactions,
            ProductCacheInvalidator invalidator) {
        this.l1 = l1;
        this.lookup = lookup;
        this.repository = repository;
        this.transactions = transactions;
        this.invalidator = invalidator;
    }

    public Product getProduct(long id) {
        Product cached = l1.getIfPresent(id);
        if (cached != null) {
            LOG.info("L1 HIT product {}", id);
            return cached;
        }
        LOG.info("L1 MISS product {}", id);
        Product product = lookup.findById(id);
        l1.put(id, product);
        return product;
    }

    public void deleteProduct(long id) {
        transactions.executeWithoutResult(status -> {
            int deleted = repository.deleteById(id);
            LOG.info("DB DELETE product {}, affected rows {}", id, deleted);
        });
        // executeWithoutResult returns only after the DB transaction has committed.
        l1.invalidate(id);
        // Also invalidate for missing rows: retry repairs a previous cache failure.
        invalidator.evictAndPublish(id);
    }
}
