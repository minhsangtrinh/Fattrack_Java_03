package com.example.caching;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ProductLookupService {
    private static final Logger LOG = LoggerFactory.getLogger(ProductLookupService.class);
    private final ProductRepository repository;

    public ProductLookupService(ProductRepository repository) {
        this.repository = repository;
    }

    @Cacheable(cacheNames = CacheConfig.PRODUCTS, cacheManager = "redisCacheManager", key = "#id")
    public Product findById(long id) {
        LOG.info("L2 MISS -> DB SELECT product {}", id);
        return repository.findById(id).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));
    }
}
